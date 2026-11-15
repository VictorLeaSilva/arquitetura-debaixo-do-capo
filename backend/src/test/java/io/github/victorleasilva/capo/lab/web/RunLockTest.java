package io.github.victorleasilva.capo.lab.web;

import static io.github.victorleasilva.capo.lab.web.RunApiTest.RUNS;
import static io.github.victorleasilva.capo.lab.web.RunApiTest.assertValid;
import static io.github.victorleasilva.capo.lab.web.RunApiTest.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import io.github.victorleasilva.capo.lab.run.testrunners.BlockingRunner;
import io.github.victorleasilva.capo.support.PostgresTestDatabase;
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
 * Uma execução por vez (D-04, RNF-003): com o test-blocking segurando a trava, um segundo pedido recebe 409; liberada a
 * trava, o primeiro termina com 201 (AC-003.8). A espera é por latch, nunca por tempo fixo.
 */
@Tag("RF-003")
@Tag("RNF-003")
@SpringBootTest
@AutoConfigureMockMvc
class RunLockTest {

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        String url = PostgresTestDatabase.createDatabase("lock");
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
    void secondRunWhileLockedGets409() throws Exception {
        long before = jdbc.sql("SELECT count(*) FROM lab_run").query(Long.class).single();
        blocking.arm();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<MvcResult> first = executor.submit(() -> run("test-blocking", "{\"variants\":[\"bad\",\"good\"]}"));
            assertThat(blocking.awaitEntered(30))
                    .as("a primeira execução entrou na trava")
                    .isTrue();

            MvcResult second =
                    run("test-fictional", "{\"variants\":[\"bad\",\"good\"],\"parameters\":{\"sizes\":[1]}}");

            assertThat(second.getResponse().getStatus()).isEqualTo(409);
            JsonNode problem = json(second);
            assertValid(RUNS, "post", 409, problem);
            assertThat(problem.get("type").asString()).isEqualTo("/problems/run-in-progress");

            blocking.release();
            MvcResult firstResult = first.get(60, TimeUnit.SECONDS);
            assertThat(firstResult.getResponse().getStatus()).isEqualTo(201);
            assertValid(RUNS, "post", 201, json(firstResult));
        } finally {
            blocking.release();
            executor.shutdownNow();
        }
        assertThat(jdbc.sql("SELECT count(*) FROM lab_run").query(Long.class).single())
                .isEqualTo(before + 1);
    }

    private MvcResult run(String experimentId, String body) throws Exception {
        return mockMvc.perform(post(RUNS, experimentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
    }
}
