package io.github.victorleasilva.capo.lab.descriptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Validação dos descritores de experimento (RF-002, D-05). Cada cenário monta um classpath temporário com o schema real
 * e os arquivos do cenário, e carrega o catálogo por ele.
 */
@Tag("RF-002")
class ExperimentCatalogTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    @TempDir
    Path classpath;

    @Test
    void loadsAValidDescriptor() {
        ExperimentCatalog catalog = load("valido", base("valido"));
        assertThat(catalog.all()).extracting(ExperimentDescriptor::id).containsExactly("valido");
    }

    @Test
    void descriptorWithoutVariantsIsRejected() {
        assertRejected("sem-versoes", d -> d.remove("variants"), "/variants");
    }

    @Test
    void descriptorWithOneVariantIsRejected() {
        assertRejected("uma-versao", d -> ((ArrayNode) d.get("variants")).remove(1), "/variants");
    }

    @Test
    void descriptorWithTwoQuestionsIsRejected() {
        assertRejected(
                "duas-perguntas",
                d -> ((ArrayNode) d.get("verificationQuestions")).remove(2),
                "/verificationQuestions");
    }

    @Test
    void unknownFieldIsRejected() {
        assertRejected("campo-extra", d -> d.put("foo", "bar"), "/foo");
    }

    @Test
    @Tag("RNF-002")
    void demonstratesErrorRequiresEvidenceMeasurement() {
        assertRejected("sem-evidencia", d -> d.put("fairness", "demonstrates-error"), "/evidenceMeasurement");
    }

    @Test
    @Tag("RNF-002")
    void evidenceMeasurementMustBeAMeasurement() {
        assertRejected(
                "evidencia-desconhecida",
                d -> d.put("fairness", "demonstrates-error").put("evidenceMeasurement", "naoExiste"),
                "/evidenceMeasurement");
    }

    @Test
    void atMostOneSizeParameter() {
        assertRejected(
                "dois-tamanhos", d -> ((ObjectNode) d.get("parameters").get(1)).put("isSize", true), "/parameters");
    }

    @Test
    void chartXMustBeTheSizeParameter() {
        assertRejected("grafico-x", d -> ((ObjectNode) d.get("chart")).put("x", "repetitions"), "/chart/x");
    }

    @Test
    void chartYMustBeAMeasurement() {
        assertRejected("grafico-y", d -> ((ObjectNode) d.get("chart")).put("y", "naoExiste"), "/chart/y");
    }

    @Test
    void idMustMatchFileName() {
        assertRejected("nome-do-arquivo", d -> d.put("id", "outro-id"), "/id");
    }

    @Test
    void variantIdsMustBeUnique() {
        assertRejected("versao-repetida", d -> ((ObjectNode) d.get("variants").get(1)).put("id", "bad"), "/variants");
    }

    @Test
    void realDescriptorsLoad() {
        ExperimentCatalog catalog = new ExperimentCatalog(new PathMatchingResourcePatternResolver(), MAPPER);
        assertThat(catalog.all())
                .extracting(ExperimentDescriptor::id)
                .contains("flash-sale-last-units", "list-intersection", "n-plus-one");
    }

    @Test
    @Tag("RNF-002")
    void flashSaleDeclaresOversold() {
        ExperimentCatalog catalog = new ExperimentCatalog(new PathMatchingResourcePatternResolver(), MAPPER);
        String json = catalog.find("flash-sale-last-units").orElseThrow().json();
        assertThat(MAPPER.readTree(json).get("fairness").asString()).isEqualTo("demonstrates-error");
        assertThat(MAPPER.readTree(json).get("evidenceMeasurement").asString()).isEqualTo("oversold");
    }

    // --- apoio ---

    private void assertRejected(String fileName, Consumer<ObjectNode> change, String field) {
        ObjectNode descriptor = base(fileName);
        change.accept(descriptor);
        assertThatThrownBy(() -> load(fileName, descriptor))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(fileName + ".json")
                .hasMessageContaining(field);
    }

    /** Um descritor válido: duas versões, um parâmetro de tamanho e um comum, uma medição, gráfico e 3 perguntas. */
    private static ObjectNode base(String id) {
        return (ObjectNode) MAPPER.readTree("""
                {
                  "schemaVersion": 1,
                  "id": "%s",
                  "title": "Título",
                  "summary": "Resumo.",
                  "studyPage": "experimentos/%s.mdx",
                  "fairness": "same-result",
                  "variants": [
                    {"id": "bad", "label": "Ruim", "description": "Versão ruim."},
                    {"id": "good", "label": "Boa", "description": "Versão boa."}
                  ],
                  "parameters": [
                    {"name": "sizes", "label": "Tamanhos", "type": "integer-set", "default": [1], "allowed": [1, 2],
                     "isSize": true},
                    {"name": "repetitions", "label": "Repetições", "type": "integer", "default": 1, "min": 1, "max": 3}
                  ],
                  "measurements": [{"id": "durationMs", "label": "Duração", "unit": "ms"}],
                  "chart": {"x": "sizes", "y": "durationMs", "scale": "linear"},
                  "verificationQuestions": ["Um?", "Dois?", "Três?"]
                }
                """.formatted(id, id));
    }

    private ExperimentCatalog load(String fileName, ObjectNode descriptor) {
        Path dir = classpath.resolve("lab/experiments");
        try {
            Files.createDirectories(dir);
            try (InputStream schema = getClass().getResourceAsStream("/lab/experiments/experiment.schema.json")) {
                Files.write(dir.resolve("experiment.schema.json"), schema.readAllBytes());
            }
            Files.writeString(
                    dir.resolve(fileName + ".json"), MAPPER.writeValueAsString(descriptor), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return new ExperimentCatalog(new PathMatchingResourcePatternResolver(isolatedClassLoader()), MAPPER);
    }

    /** Só o diretório temporário, sem o classpath da aplicação (pai {@code null}). */
    private URLClassLoader isolatedClassLoader() {
        try {
            return new URLClassLoader(new URL[] {classpath.toUri().toURL()}, null);
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }
}
