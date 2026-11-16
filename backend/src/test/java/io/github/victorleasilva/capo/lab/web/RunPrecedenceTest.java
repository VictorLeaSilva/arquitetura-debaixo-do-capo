package io.github.victorleasilva.capo.lab.web;

import static io.github.victorleasilva.capo.lab.web.RunApiTest.RUNS;
import static io.github.victorleasilva.capo.lab.web.RunApiTest.assertValid;
import static io.github.victorleasilva.capo.lab.web.RunApiTest.json;
import static io.github.victorleasilva.capo.lab.web.RunValidationApiTest.LIMIT;
import static io.github.victorleasilva.capo.lab.web.RunValidationApiTest.garbage;
import static io.github.victorleasilva.capo.lab.web.RunValidationApiTest.padded;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import io.github.victorleasilva.capo.lab.run.testrunners.BlockingRunner;
import io.github.victorleasilva.capo.support.PostgresTestDatabase;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
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

/**
 * A precedência dos erros (AC-003.10): 404 → 413 → 400 → 422 size-exceeds-variant-limit → 422 insufficient-data →
 * 409, provada par a par. Nos pares com o 409, o test-blocking segura a trava de verdade; a espera é por latch.
 */
@Tag("RF-003")
@SpringBootTest
@AutoConfigureMockMvc
class RunPrecedenceTest {

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        String url = PostgresTestDatabase.createDatabase("precedence");
        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.username", PostgresTestDatabase::username);
        registry.add("spring.datasource.password", PostgresTestDatabase::password);
        registry.add("LAB_SEED_PRODUCTS", () -> "10000");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private BlockingRunner blocking;

    @Test
    void notFoundComesBeforePayloadTooLarge() throws Exception {
        withoutRecord(() -> run("foo", padded("{\"variants\":[\"bad\"]}", LIMIT + 1)), 404, "experiment-not-found");
    }

    @Test
    void payloadTooLargeComesBeforeMalformed() throws Exception {
        withoutRecord(() -> run("list-intersection", garbage(LIMIT + 1)), 413, "payload-too-large");
    }

    @Test
    void invalidParameterComesBeforeTheVariantLimit() throws Exception {
        withoutRecord(
                () -> run("list-intersection", "{\"variants\":[\"worst\"],\"parameters\":{\"sizes\":[100000]}}"),
                400,
                "invalid-parameter");
    }

    @Test
    void theVariantLimitComesBeforeInsufficientData() throws Exception {
        withoutRecord(
                () -> run("list-intersection", "{\"variants\":[\"bad\"],\"parameters\":{\"sizes\":[100000]}}"),
                422,
                "size-exceeds-variant-limit");
    }

    @Test
    void insufficientDataAndInvalidParameterComeBeforeRunInProgress() throws Exception {
        long before = labRuns();
        blocking.arm();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<MvcResult> holder = executor.submit(() -> run("test-blocking", "{\"variants\":[\"good\"]}"));
            assertThat(blocking.awaitEntered(30))
                    .as("o test-blocking segura a trava")
                    .isTrue();

            JsonNode insufficient = assertProblem(
                    run("list-intersection", "{\"variants\":[\"good\"],\"parameters\":{\"sizes\":[20000]}}"),
                    422,
                    "insufficient-data");
            assertThat(insufficient.get("title").asString()).isEqualTo("Dados insuficientes");
            assertThat(insufficient.get("detail").asString())
                    .isEqualTo("O tamanho 20000 é maior que os 10000 produtos carregados (LAB_SEED_PRODUCTS).");
            assertThat(insufficient.get("size").asInt()).isEqualTo(20000);
            assertThat(insufficient.get("seedProducts").asInt()).isEqualTo(10000);

            assertProblem(run("list-intersection", "{\"variants\":[]}"), 400, "invalid-parameter");

            // Prova de que a trava estava segura: um pedido válido recebe 409.
            assertProblem(
                    run("test-fictional", "{\"variants\":[\"good\"],\"parameters\":{\"sizes\":[1]}}"),
                    409,
                    "run-in-progress");

            blocking.release();
            assertThat(holder.get(60, TimeUnit.SECONDS).getResponse().getStatus())
                    .isEqualTo(201);
        } finally {
            blocking.release();
            executor.shutdownNow();
        }
        assertThat(labRuns()).as("só a execução do test-blocking foi gravada").isEqualTo(before + 1);
    }

    /** O erro esperado, sem nenhuma linha nova em lab_run. */
    private void withoutRecord(Request request, int status, String type) throws Exception {
        long before = labRuns();
        assertProblem(request.send(), status, type);
        assertThat(labRuns()).isEqualTo(before);
    }

    @FunctionalInterface
    private interface Request {
        MvcResult send() throws Exception;
    }

    private JsonNode assertProblem(MvcResult result, int status, String type) throws Exception {
        assertThat(result.getResponse().getStatus()).isEqualTo(status);
        JsonNode problem = json(result);
        assertValid(RUNS, "post", status, problem);
        assertThat(problem.get("type").asString()).isEqualTo("/problems/" + type);
        return problem;
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
