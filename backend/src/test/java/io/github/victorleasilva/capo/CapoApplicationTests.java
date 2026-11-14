package io.github.victorleasilva.capo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.victorleasilva.capo.support.PostgresTestDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Teste de fumaça da T-001: a aplicação sobe e o health check do Actuator responde.
 *
 * <p>AC-T001.3: {@code GET /actuator/health} devolve 200 com {@code "status":"UP"}. Desde a T-010, a aplicação precisa
 * de um banco: o teste usa um PostgreSQL real e a carga pequena dos testes ({@code LAB_SEED_PRODUCTS=10000}).
 */
@SpringBootTest
@AutoConfigureMockMvc
class CapoApplicationTests {

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        String url = PostgresTestDatabase.createDatabase("app");
        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.username", PostgresTestDatabase::username);
        registry.add("spring.datasource.password", PostgresTestDatabase::password);
        registry.add("LAB_SEED_PRODUCTS", () -> "10000");
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthEndpointReportsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
