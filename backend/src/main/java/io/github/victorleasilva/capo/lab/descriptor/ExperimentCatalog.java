package io.github.victorleasilva.capo.lab.descriptor;

import com.networknt.schema.Error;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SchemaRegistryConfig;
import com.networknt.schema.SpecificationVersion;
import com.networknt.schema.path.PathType;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Os descritores de experimento (D-05): cada arquivo JSON em {@code lab/experiments/} descreve um experimento e é
 * validado contra o JSON Schema na inicialização. Um descritor inválido impede a aplicação de subir (AC-002.4), com
 * uma linha por erro citando o arquivo e o campo.
 */
@Component
public final class ExperimentCatalog {

    static final String DESCRIPTORS = "classpath*:lab/experiments/*.json";
    static final String SCHEMA = "classpath:lab/experiments/experiment.schema.json";
    private static final String SCHEMA_FILE = "experiment.schema.json";

    private final Map<String, ExperimentDescriptor> descriptors;

    public ExperimentCatalog(ResourcePatternResolver resolver, JsonMapper mapper) {
        // Os caminhos dos erros saem como JSON Pointer (/variants/1/id), o formato citado nas mensagens.
        SchemaRegistryConfig config =
                SchemaRegistryConfig.builder().pathType(PathType.JSON_POINTER).build();
        Schema schema = SchemaRegistry.withDefaultDialect(
                        SpecificationVersion.DRAFT_2020_12, builder -> builder.schemaRegistryConfig(config))
                .getSchema(read(resolver.getResource(SCHEMA)), InputFormat.JSON);
        List<String> errors = new ArrayList<>();
        Map<String, ExperimentDescriptor> loaded = new LinkedHashMap<>();

        for (Resource resource : descriptorFiles(resolver)) {
            String file = resource.getFilename();
            String json = read(resource);
            List<Error> schemaErrors = schema.validate(json, InputFormat.JSON);
            if (!schemaErrors.isEmpty()) {
                schemaErrors.forEach(error -> errors.add(file + ": " + pointer(error) + ": " + error.getMessage()));
                continue;
            }
            JsonNode descriptor = mapper.readTree(json);
            List<String> ruleErrors = checkRules(file, descriptor);
            if (!ruleErrors.isEmpty()) {
                errors.addAll(ruleErrors);
                continue;
            }
            String id = descriptor.get("id").asString();
            if (loaded.containsKey(id)) {
                errors.add(file + ": /id: o id '" + id + "' já existe em outro descritor");
                continue;
            }
            loaded.put(
                    id,
                    new ExperimentDescriptor(
                            id,
                            descriptor.get("title").asString(),
                            descriptor.get("summary").asString(),
                            descriptor.get("studyPage").asString(),
                            json));
        }

        if (!errors.isEmpty()) {
            throw new IllegalStateException("Descritores de experimento inválidos:\n" + String.join("\n", errors));
        }
        this.descriptors = loaded;
    }

    /** Todos os descritores, em ordem crescente de id. */
    public List<ExperimentDescriptor> all() {
        return descriptors.values().stream()
                .sorted(Comparator.comparing(ExperimentDescriptor::id))
                .toList();
    }

    public Optional<ExperimentDescriptor> find(String id) {
        return Optional.ofNullable(descriptors.get(id));
    }

    /** As regras que o JSON Schema não expressa (03 §4). */
    private static List<String> checkRules(String file, JsonNode descriptor) {
        List<String> errors = new ArrayList<>();
        String id = descriptor.get("id").asString();
        if (!file.equals(id + ".json")) {
            errors.add(file + ": /id: o id '" + id + "' precisa ser igual ao nome do arquivo");
        }
        Set<String> measurements = uniqueValues(file, descriptor.get("measurements"), "id", "/measurements", errors);
        uniqueValues(file, descriptor.get("variants"), "id", "/variants", errors);
        uniqueValues(file, descriptor.get("parameters"), "name", "/parameters", errors);

        JsonNode evidence = descriptor.get("evidenceMeasurement");
        if (evidence != null && !measurements.contains(evidence.asString())) {
            errors.add(file + ": /evidenceMeasurement: '" + evidence.asString() + "' não é o id de uma medição");
        }
        JsonNode chart = descriptor.get("chart");
        if (chart != null) {
            String sizeParameter = null;
            for (JsonNode parameter : descriptor.get("parameters")) {
                if (parameter.path("isSize").asBoolean(false)) {
                    sizeParameter = parameter.get("name").asString();
                }
            }
            String x = chart.get("x").asString();
            if (!x.equals(sizeParameter)) {
                errors.add(file + ": /chart/x: '" + x + "' precisa ser o parâmetro com isSize = true");
            }
            String y = chart.get("y").asString();
            if (!measurements.contains(y)) {
                errors.add(file + ": /chart/y: '" + y + "' não é o id de uma medição");
            }
        }
        return errors;
    }

    private static Set<String> uniqueValues(
            String file, JsonNode items, String key, String field, List<String> errors) {
        Set<String> seen = new HashSet<>();
        for (JsonNode item : items) {
            String value = item.get(key).asString();
            if (!seen.add(value)) {
                errors.add(file + ": " + field + ": '" + value + "' aparece mais de uma vez");
            }
        }
        return seen;
    }

    /** O campo do erro como JSON Pointer; num campo obrigatório ausente, o nome dele vem em {@code getProperty()}. */
    private static String pointer(Error error) {
        String base = error.getInstanceLocation().toString();
        if (error.getProperty() != null) {
            base = base + "/" + error.getProperty();
        }
        return base.isEmpty() ? "/" : base;
    }

    private static List<Resource> descriptorFiles(ResourcePatternResolver resolver) {
        try {
            List<Resource> files = new ArrayList<>();
            for (Resource resource : resolver.getResources(DESCRIPTORS)) {
                if (!SCHEMA_FILE.equals(resource.getFilename())) {
                    files.add(resource);
                }
            }
            files.sort(Comparator.comparing(Resource::getFilename));
            return files;
        } catch (IOException e) {
            throw new UncheckedIOException("não foi possível listar " + DESCRIPTORS, e);
        }
    }

    private static String read(Resource resource) {
        try (InputStream in = resource.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("não foi possível ler " + resource.getDescription(), e);
        }
    }
}
