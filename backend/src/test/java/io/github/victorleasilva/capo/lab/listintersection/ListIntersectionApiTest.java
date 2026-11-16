package io.github.victorleasilva.capo.lab.listintersection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import io.github.victorleasilva.capo.lab.web.OpenApiContract;
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

/** O list-intersection de verdade pela API: o pedido literal do AC-003.1, as medições e a comparação justa. */
@Tag("RF-005")
@SpringBootTest
@AutoConfigureMockMvc
class ListIntersectionApiTest {

    static final String RUNS = "/api/v1/lab/experiments/{experimentId}/runs";
    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final OpenApiContract CONTRACT = new OpenApiContract();

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        String url = PostgresTestDatabase.createDatabase("intersection");
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
    @Tag("RF-003")
    @Tag("RNF-002")
    void runsTheLiteralRequest() throws Exception {
        long before = labRuns();

        MvcResult result =
                run("{\"variants\":[\"bad\",\"good\"],\"parameters\":{\"sizes\":[1000,5000],\"repetitions\":3}}");

        // AC-003.1
        assertThat(result.getResponse().getStatus()).isEqualTo(201);
        JsonNode body = MAPPER.readTree(result.getResponse().getContentAsString());
        assertThat(CONTRACT.validate(RUNS, "post", 201, "application/json", body))
                .isEmpty();
        String id = body.get("id").asString();
        assertThat(result.getResponse().getHeader("Location")).isEqualTo("/api/v1/lab/runs/" + id);
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
        assertThat(body.get("parameters")).isEqualTo(MAPPER.readTree("{\"sizes\":[1000,5000],\"repetitions\":3}"));
        List<String> order = new ArrayList<>();
        body.get("results")
                .forEach(r -> order.add(
                        r.get("variant").asString() + "/" + r.get("size").asInt()));
        assertThat(order).containsExactly("bad/1000", "bad/5000", "good/1000", "good/5000");
        assertThat(labRuns()).isEqualTo(before + 1);
        assertThat(jdbc.sql("SELECT status FROM lab_run WHERE id = :id")
                        .param("id", UUID.fromString(id))
                        .query(String.class)
                        .single())
                .isEqualTo("COMPLETED");

        // AC-005.2
        for (JsonNode r : body.get("results")) {
            JsonNode m = r.get("measurements");
            assertThat(m.propertyNames())
                    .containsExactlyInAnyOrder("durationMsMedian", "durationMsMin", "durationMsMax", "matches");
            assertThat(m.get("durationMsMin").asDouble())
                    .isLessThanOrEqualTo(m.get("durationMsMedian").asDouble());
            assertThat(m.get("durationMsMedian").asDouble())
                    .isLessThanOrEqualTo(m.get("durationMsMax").asDouble());
            assertThat(m.get("matches").asDouble()).isEqualTo(r.get("size").asInt() / 2.0);
        }

        // AC-RNF-002.1
        JsonNode results = body.get("results");
        assertThat(results.get(0).get("checksum").asString()).matches("[0-9a-f]{64}");
        assertThat(results.get(2).get("checksum")).isEqualTo(results.get(0).get("checksum"));
        assertThat(results.get(3).get("checksum")).isEqualTo(results.get(1).get("checksum"));
        assertThat(results.get(1).get("checksum")).isNotEqualTo(results.get(0).get("checksum"));
        assertThat(body.get("fairness")).isEqualTo(MAPPER.readTree("{\"type\":\"same-result\",\"sameResult\":true}"));
    }

    @Test
    void sizeAboveTheSeedIs422() throws Exception {
        long before = labRuns();

        MvcResult result = run("{\"variants\":[\"medium\",\"good\"],\"parameters\":{\"sizes\":[100000]}}");

        assertThat(result.getResponse().getStatus()).isEqualTo(422);
        JsonNode problem = MAPPER.readTree(result.getResponse().getContentAsString());
        assertThat(CONTRACT.validate(RUNS, "post", 422, "application/problem+json", problem))
                .isEmpty();
        assertThat(problem.get("type").asString()).isEqualTo("/problems/insufficient-data");
        assertThat(labRuns()).isEqualTo(before);
    }

    private long labRuns() {
        return jdbc.sql("SELECT count(*) FROM lab_run").query(Long.class).single();
    }

    private MvcResult run(String body) throws Exception {
        return mockMvc.perform(post(RUNS, "list-intersection")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
    }
}
