package io.github.victorleasilva.capo.lab.web;

import static io.github.victorleasilva.capo.lab.web.RunApiTest.RUNS;
import static io.github.victorleasilva.capo.lab.web.RunApiTest.assertValid;
import static io.github.victorleasilva.capo.lab.web.RunApiTest.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import io.github.victorleasilva.capo.support.PostgresTestDatabase;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Os erros de validação do pedido pela API (RF-003; T-019), cada um conferido contra o lab-api.yaml e sem gravar nada
 * em lab_run.
 */
@Tag("RF-003")
@SpringBootTest
@AutoConfigureMockMvc
class RunValidationApiTest {

    static final int LIMIT = 16_384;
    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        String url = PostgresTestDatabase.createDatabase("validation");
        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.username", PostgresTestDatabase::username);
        registry.add("spring.datasource.password", PostgresTestDatabase::password);
        registry.add("LAB_SEED_PRODUCTS", () -> "10000");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbc;

    /** Um corpo JSON válido completado com espaços até ter exatamente {@code bytes} bytes. */
    static byte[] padded(String json, int bytes) {
        byte[] raw = json.getBytes(StandardCharsets.UTF_8);
        return (json + " ".repeat(bytes - raw.length)).getBytes(StandardCharsets.UTF_8);
    }

    /** {@code bytes} bytes que não são JSON. */
    static byte[] garbage(int bytes) {
        return "x".repeat(bytes).getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void omittedParametersShowTheDefaults() throws Exception {
        MvcResult result = run("test-fictional", "{\"variants\":[\"bad\",\"good\"]}");

        assertThat(result.getResponse().getStatus()).isEqualTo(201);
        JsonNode body = json(result);
        assertValid(RUNS, "post", 201, body);
        assertThat(body.get("parameters")).isEqualTo(MAPPER.readTree("{\"sizes\":[1,2]}"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"variants\":[]}", "{\"variants\":[\"worst\"]}"})
    void invalidVariantsAre400(String request) throws Exception {
        long before = labRuns();

        JsonNode problem = expectProblem(run("list-intersection", request), 400, "invalid-parameter");

        assertThat(fields(problem)).containsExactly("variants");
        assertThat(labRuns()).isEqualTo(before);
    }

    @ParameterizedTest
    @CsvSource(
            delimiter = '|',
            value = {
                "\"sizes\":[1234]     | sizes",
                "\"repetitions\":0    | repetitions",
                "\"repetitions\":11   | repetitions",
                "\"foo\":1            | foo"
            })
    void invalidParametersAre400(String parameter, String field) throws Exception {
        long before = labRuns();

        JsonNode problem = expectProblem(
                run("list-intersection", "{\"variants\":[\"bad\",\"good\"],\"parameters\":{" + parameter + "}}"),
                400,
                "invalid-parameter");

        assertThat(fields(problem)).containsExactly(field);
        assertThat(problem.get("title").asString()).isEqualTo("Parâmetro inválido");
        assertThat(problem.get("detail").asString()).isEqualTo("O pedido tem parâmetros inválidos; veja errors.");
        assertThat(labRuns()).isEqualTo(before);
    }

    @Test
    void sizeAboveTheVariantLimitIs422() throws Exception {
        long before = labRuns();

        JsonNode problem = expectProblem(
                run("list-intersection", "{\"variants\":[\"bad\"],\"parameters\":{\"sizes\":[100000]}}"),
                422,
                "size-exceeds-variant-limit");

        assertThat(problem.get("title").asString()).isEqualTo("Tamanho acima do limite da versão");
        assertThat(problem.get("detail").asString())
                .isEqualTo("A versão 'bad' aceita tamanhos até 20000; o pedido tem 100000.");
        assertThat(problem.get("variant").asString()).isEqualTo("bad");
        assertThat(problem.get("maxSize").asInt()).isEqualTo(20000);
        assertThat(problem.get("size").asInt()).isEqualTo(100000);
        assertThat(labRuns()).isEqualTo(before);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{", "", "{} x", "{\"variants\":[\"bad\"],\"variants\":[\"good\"]}"})
    void malformedBodyIs400(String request) throws Exception {
        long before = labRuns();

        JsonNode problem = expectProblem(run("list-intersection", request), 400, "malformed-request");

        assertThat(problem.get("title").asString()).isEqualTo("Corpo inválido");
        assertThat(problem.get("detail").asString()).isEqualTo("O corpo do pedido não é um JSON válido.");
        assertThat(labRuns()).isEqualTo(before);
    }

    @Test
    void unknownExperimentIs404() throws Exception {
        long before = labRuns();

        expectProblem(run("foo", "{\"variants\":[\"bad\"]}"), 404, "experiment-not-found");

        assertThat(labRuns()).isEqualTo(before);
    }

    @Test
    void bodyAbove16KbIs413() throws Exception {
        long before = labRuns();

        JsonNode problem = expectProblem(
                run("list-intersection", padded("{\"variants\":[\"bad\"]}", LIMIT + 1)), 413, "payload-too-large");

        assertThat(problem.get("title").asString()).isEqualTo("Corpo grande demais");
        assertThat(problem.get("detail").asString()).isEqualTo("O corpo do pedido passa de 16 KB (16384 bytes).");
        assertThat(labRuns()).isEqualTo(before);
    }

    @Test
    void bodyOfExactly16KbIsAccepted() throws Exception {
        byte[] body = padded("{\"variants\":[\"good\"],\"parameters\":{\"sizes\":[1]}}", LIMIT);
        assertThat(body).hasSize(LIMIT);

        MvcResult result = run("test-fictional", body);

        assertThat(result.getResponse().getStatus()).isEqualTo(201);
    }

    /** O Content-Type não é conferido (03 §5.1 não declara 415): o corpo é lido como JSON de qualquer forma. */
    @Test
    void contentTypeIsNotChecked() throws Exception {
        MvcResult result = mockMvc.perform(post(RUNS, "test-fictional")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("{\"variants\":[\"good\"],\"parameters\":{\"sizes\":[1]}}"))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(201);
    }

    private JsonNode expectProblem(MvcResult result, int status, String type) throws Exception {
        assertThat(result.getResponse().getStatus()).isEqualTo(status);
        assertThat(result.getResponse().getContentType()).startsWith("application/problem+json");
        JsonNode problem = json(result);
        assertValid(RUNS, "post", status, problem);
        assertThat(problem.get("type").asString()).isEqualTo("/problems/" + type);
        assertThat(problem.get("status").asInt()).isEqualTo(status);
        return problem;
    }

    private static List<String> fields(JsonNode problem) {
        List<String> fields = new ArrayList<>();
        problem.get("errors").forEach(e -> fields.add(e.get("field").asString()));
        return fields;
    }

    private long labRuns() {
        return jdbc.sql("SELECT count(*) FROM lab_run").query(Long.class).single();
    }

    private MvcResult run(String experimentId, String body) throws Exception {
        return run(experimentId, body.getBytes(StandardCharsets.UTF_8));
    }

    private MvcResult run(String experimentId, byte[] body) throws Exception {
        return mockMvc.perform(post(RUNS, experimentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
    }
}
