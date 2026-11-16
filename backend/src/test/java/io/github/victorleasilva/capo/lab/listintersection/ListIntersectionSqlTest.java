package io.github.victorleasilva.capo.lab.listintersection;

import static io.github.victorleasilva.capo.lab.listintersection.SqlCountingConfiguration.INSIDE_MEASUREMENTS;
import static io.github.victorleasilva.capo.lab.listintersection.SqlCountingConfiguration.MEASUREMENTS;
import static io.github.victorleasilva.capo.lab.listintersection.SqlCountingConfiguration.STATEMENT_COUNT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import io.github.victorleasilva.capo.support.PostgresTestDatabase;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.json.JsonMapper;

/** A leitura do catálogo acontece antes da medição: nenhum comando SQL dentro do intervalo medido (AC-005.4; P3). */
@Tag("RF-005")
@SpringBootTest
@AutoConfigureMockMvc
@Import(SqlCountingConfiguration.class)
class ListIntersectionSqlTest {

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        String url = PostgresTestDatabase.createDatabase("intersection_sql");
        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.username", PostgresTestDatabase::username);
        registry.add("spring.datasource.password", PostgresTestDatabase::password);
        registry.add("LAB_SEED_PRODUCTS", () -> "10000");
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void noSqlInsideTheMeasurement() throws Exception {
        long statementsBefore = STATEMENT_COUNT.get();
        long measurementsBefore = MEASUREMENTS.get();
        long insideBefore = INSIDE_MEASUREMENTS.get();

        MvcResult result = mockMvc.perform(post(ListIntersectionApiTest.RUNS, "list-intersection")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"variants\":[\"medium\",\"good\"],"
                                + "\"parameters\":{\"sizes\":[10000],\"repetitions\":1}}"))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(201);
        assertThat(JsonMapper.builder()
                        .build()
                        .readTree(result.getResponse().getContentAsString())
                        .get("status")
                        .asString())
                .isEqualTo("COMPLETED");
        assertThat(MEASUREMENTS.get() - measurementsBefore)
                .as("2 versões × (2 aquecimentos + 1 repetição)")
                .isEqualTo(6);
        assertThat(STATEMENT_COUNT.get() - statementsBefore)
                .as("a contagem está ligada: a execução lê o catálogo e grava lab_run")
                .isPositive();
        assertThat(INSIDE_MEASUREMENTS.get() - insideBefore)
                .as("comandos SQL dentro das medições")
                .isZero();
    }
}
