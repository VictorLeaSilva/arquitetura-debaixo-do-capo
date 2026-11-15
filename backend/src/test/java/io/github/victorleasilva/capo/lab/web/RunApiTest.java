package io.github.victorleasilva.capo.lab.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import io.github.victorleasilva.capo.lab.run.testrunners.FailingRunner;
import io.github.victorleasilva.capo.support.PostgresTestDatabase;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
import tools.jackson.databind.json.JsonMapper;

/** Executar e consultar uma execução (RF-003, RF-004), conferido contra o contrato OpenAPI (AC-T012.1). */
@Tag("RF-003")
@SpringBootTest
@AutoConfigureMockMvc
class RunApiTest {

    static final String RUNS = "/api/v1/lab/experiments/{experimentId}/runs";
    static final String RUN = "/api/v1/lab/runs/{runId}";
    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final OpenApiContract CONTRACT = new OpenApiContract();
    private static final String FICTIONAL = "{\"variants\":[\"bad\",\"good\"],\"parameters\":{\"sizes\":[1,2]}}";
    private static final String TWO_VARIANTS = "{\"variants\":[\"bad\",\"good\"]}";

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        String url = PostgresTestDatabase.createDatabase("runs");
        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.username", PostgresTestDatabase::username);
        registry.add("spring.datasource.password", PostgresTestDatabase::password);
        registry.add("LAB_SEED_PRODUCTS", () -> "10000");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbc;

    @Test
    void runsAnExperimentAndRecordsIt() throws Exception {
        long before = labRuns();

        MvcResult result = run("test-fictional", FICTIONAL);

        assertThat(result.getResponse().getStatus()).isEqualTo(201);
        JsonNode body = json(result);
        assertValid(RUNS, "post", 201, body);
        String id = body.get("id").asString();
        assertThat(UUID.fromString(id)).isNotNull();
        assertThat(result.getResponse().getHeader("Location")).isEqualTo("/api/v1/lab/runs/" + id);
        assertThat(body.get("experimentId").asString()).isEqualTo("test-fictional");
        assertThat(body.get("status").asString()).isEqualTo("COMPLETED");
        assertThat(body.get("traceId").asString()).matches("[0-9a-f]{32}");
        assertThat(body.get("environment").propertyNames())
                .containsExactlyInAnyOrder(
                        "javaVersion",
                        "springBootVersion",
                        "postgresVersion",
                        "osName",
                        "osArch",
                        "availableProcessors",
                        "maxHeapMb",
                        "dbPoolSize",
                        "seedProducts");
        assertThat(body.get("environment").get("seedProducts").asInt()).isEqualTo(10000);
        assertThat(body.get("parameters")).isEqualTo(MAPPER.readTree("{\"sizes\":[1,2]}"));

        List<String> results = new ArrayList<>();
        body.get("results")
                .forEach(r -> results.add(
                        r.get("variant").asString() + ":" + r.get("size").asInt() + ":"
                                + r.get("measurements").get("score").asDouble() + ":"
                                + r.get("checksum").asString()));
        assertThat(results)
                .containsExactly(
                        "bad:1:10.0:fictional-1",
                        "bad:2:20.0:fictional-2",
                        "good:1:10.0:fictional-1",
                        "good:2:20.0:fictional-2");
        assertThat(body.get("fairness")).isEqualTo(MAPPER.readTree("{\"type\":\"same-result\",\"sameResult\":true}"));
        assertThat(body.has("failureReason")).isFalse();

        assertThat(labRuns()).isEqualTo(before + 1);
        assertThat(jdbc.sql("SELECT status FROM lab_run WHERE id = :id")
                        .param("id", UUID.fromString(id))
                        .query(String.class)
                        .single())
                .isEqualTo("COMPLETED");
    }

    /** O valor pedido vence o padrão do descritor (o padrão de sizes no test-fictional é [1, 2]). */
    @Test
    void requestedValueOverridesTheDefault() throws Exception {
        JsonNode body = json(run("test-fictional", "{\"variants\":[\"good\"],\"parameters\":{\"sizes\":[3]}}"));

        assertThat(body.get("parameters")).isEqualTo(MAPPER.readTree("{\"sizes\":[3]}"));
        assertThat(body.get("results")).hasSize(1);
        assertThat(body.get("results").get(0).get("size").asInt()).isEqualTo(3);
    }

    @Test
    @Tag("RF-004")
    void getReturnsTheSameBodyAsThePost() throws Exception {
        MvcResult created = run("test-fictional", FICTIONAL);

        MvcResult fetched = mockMvc.perform(get(created.getResponse().getHeader("Location")))
                .andReturn();

        assertThat(fetched.getResponse().getStatus()).isEqualTo(200);
        assertValid(RUN, "get", 200, json(fetched));
        assertThat(json(fetched)).isEqualTo(json(created));
    }

    @Test
    @Tag("RNF-003")
    void failingRunReturns500AndReleasesTheLock() throws Exception {
        MvcResult failed = run("test-failing", TWO_VARIANTS);

        assertThat(failed.getResponse().getStatus()).isEqualTo(500);
        assertThat(failed.getResponse().getContentType()).startsWith("application/problem+json");
        JsonNode problem = json(failed);
        assertValid(RUNS, "post", 500, problem);
        assertThat(problem.get("type").asString()).isEqualTo("/problems/run-failed");
        UUID runId = UUID.fromString(problem.get("runId").asString());
        assertThat(jdbc.sql("SELECT status FROM lab_run WHERE id = :id")
                        .param("id", runId)
                        .query(String.class)
                        .single())
                .isEqualTo("FAILED");

        assertThat(run("test-fictional", FICTIONAL).getResponse().getStatus()).isEqualTo(201);
    }

    @Test
    @Tag("RF-004")
    void failedRunShowsReasonAndNoResults() throws Exception {
        String runId = json(run("test-failing", TWO_VARIANTS)).get("runId").asString();

        MvcResult fetched = mockMvc.perform(get(RUN, runId)).andReturn();

        assertThat(fetched.getResponse().getStatus()).isEqualTo(200);
        JsonNode body = json(fetched);
        assertValid(RUN, "get", 200, body);
        assertThat(body.get("status").asString()).isEqualTo("FAILED");
        assertThat(body.has("results")).isTrue();
        assertThat(body.get("results").isNull()).isTrue();
        assertThat(body.get("failureReason").asString()).isEqualTo(FailingRunner.MESSAGE);
    }

    @Test
    @Tag("RF-004")
    void unknownRunIs404AndBadIdIs400() throws Exception {
        MvcResult notFound = mockMvc.perform(get(RUN, UUID.randomUUID())).andReturn();
        assertThat(notFound.getResponse().getStatus()).isEqualTo(404);
        assertValid(RUN, "get", 404, json(notFound));
        assertThat(json(notFound).get("type").asString()).isEqualTo("/problems/run-not-found");

        MvcResult badId = mockMvc.perform(get(RUN, "abc")).andReturn();
        assertThat(badId.getResponse().getStatus()).isEqualTo(400);
        JsonNode problem = json(badId);
        assertValid(RUN, "get", 400, problem);
        assertThat(problem.get("type").asString()).isEqualTo("/problems/invalid-parameter");
        assertThat(problem.get("errors").get(0).get("field").asString()).isEqualTo("runId");
    }

    @Test
    @Tag("RNF-009")
    void serverErrorHasNoStackTrace() throws Exception {
        String body = run("test-failing", TWO_VARIANTS).getResponse().getContentAsString();

        assertThat(body).doesNotContain("Exception", "java.", "io.github.", "\tat ");
    }

    // --- apoio ---

    private MvcResult run(String experimentId, String body) throws Exception {
        return mockMvc.perform(post(RUNS, experimentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
    }

    private long labRuns() {
        return jdbc.sql("SELECT count(*) FROM lab_run").query(Long.class).single();
    }

    static JsonNode json(MvcResult result) throws Exception {
        return MAPPER.readTree(result.getResponse().getContentAsString());
    }

    static void assertValid(String path, String method, int status, JsonNode body) {
        String media = status >= 400 ? "application/problem+json" : "application/json";
        assertThat(CONTRACT.validate(path, method, status, media, body))
                .as(method + " " + path + " " + status)
                .isEmpty();
    }
}
