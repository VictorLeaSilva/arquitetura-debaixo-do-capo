package io.github.victorleasilva.capo.lab.web;

import static io.github.victorleasilva.capo.lab.web.RunApiTest.RUNS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import io.github.victorleasilva.capo.support.PostgresTestDatabase;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Só a origem da tela do laboratório recebe os headers de CORS (AC-RNF-009.4; Q-045). */
@Tag("RNF-009")
@SpringBootTest
@AutoConfigureMockMvc
class CorsTest {

    private static final String LAB = "http://localhost:5173";
    private static final String ALLOW_ORIGIN = "Access-Control-Allow-Origin";

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        String url = PostgresTestDatabase.createDatabase("cors");
        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.username", PostgresTestDatabase::username);
        registry.add("spring.datasource.password", PostgresTestDatabase::password);
        registry.add("LAB_SEED_PRODUCTS", () -> "10000");
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void onlyTheLabOriginGetsTheCorsHeader() throws Exception {
        MvcResult lab = mockMvc.perform(get("/api/v1/lab/experiments").header("Origin", LAB))
                .andReturn();
        assertThat(lab.getResponse().getStatus()).isEqualTo(200);
        assertThat(lab.getResponse().getHeader(ALLOW_ORIGIN)).isEqualTo(LAB);

        MvcResult evil = mockMvc.perform(get("/api/v1/lab/experiments").header("Origin", "http://evil.example"))
                .andReturn();
        assertThat(evil.getResponse().getStatus()).isEqualTo(403);
        assertThat(evil.getResponse().getHeader(ALLOW_ORIGIN)).isNull();
    }

    @Test
    void preflightAllowsThePostAndLocationIsExposed() throws Exception {
        MvcResult preflight = mockMvc.perform(options(RUNS, "test-fictional")
                        .header("Origin", LAB)
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type"))
                .andReturn();
        assertThat(preflight.getResponse().getStatus()).isEqualTo(200);
        assertThat(preflight.getResponse().getHeader(ALLOW_ORIGIN)).isEqualTo(LAB);
        assertThat(preflight.getResponse().getHeader("Access-Control-Allow-Methods"))
                .contains("POST");

        MvcResult created = mockMvc.perform(post(RUNS, "test-fictional")
                        .header("Origin", LAB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"variants\":[\"good\"],\"parameters\":{\"sizes\":[1]}}"))
                .andReturn();
        assertThat(created.getResponse().getStatus()).isEqualTo(201);
        assertThat(created.getResponse().getHeader(ALLOW_ORIGIN)).isEqualTo(LAB);
        assertThat(created.getResponse().getHeader("Access-Control-Expose-Headers"))
                .contains("Location");
    }
}
