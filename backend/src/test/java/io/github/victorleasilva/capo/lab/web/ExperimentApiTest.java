package io.github.victorleasilva.capo.lab.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import io.github.victorleasilva.capo.support.PostgresTestDatabase;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** A API dos experimentos (RF-002), conferida contra o contrato OpenAPI (AC-T011.2). */
@Tag("RF-002")
@SpringBootTest
@AutoConfigureMockMvc
class ExperimentApiTest {

    private static final String BASE = "/api/v1/lab/experiments";
    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final OpenApiContract CONTRACT = new OpenApiContract();

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        String url = PostgresTestDatabase.createDatabase("api");
        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.username", PostgresTestDatabase::username);
        registry.add("spring.datasource.password", PostgresTestDatabase::password);
        registry.add("LAB_SEED_PRODUCTS", () -> "10");
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listsTheMvpExperiments() throws Exception {
        MvcResult result = mockMvc.perform(get(BASE)).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getContentType()).startsWith("application/json");
        JsonNode body = MAPPER.readTree(result.getResponse().getContentAsString());
        assertThat(CONTRACT.validate(BASE, "get", 200, "application/json", body))
                .isEmpty();

        List<String> ids = new ArrayList<>();
        body.forEach(item -> ids.add(item.get("id").asString()));
        // No classpath de teste, o test-fictional (03 §4.1) também aparece, depois dos três do MVP.
        assertThat(ids).containsExactly("flash-sale-last-units", "list-intersection", "n-plus-one", "test-fictional");
        for (JsonNode item : body) {
            assertThat(item.propertyNames()).containsExactlyInAnyOrder("id", "title", "summary", "studyPage");
            String id = item.get("id").asString();
            if (!id.equals("test-fictional")) {
                JsonNode file = descriptorFile(id);
                assertThat(item.get("title")).isEqualTo(file.get("title"));
                assertThat(item.get("summary")).isEqualTo(file.get("summary"));
                assertThat(item.get("studyPage")).isEqualTo(file.get("studyPage"));
            }
        }
    }

    @Test
    void testDescriptorsLiveOnlyInTestResources() throws IOException {
        assertThat(fileNames(Path.of("src/main/resources/lab/experiments")))
                .containsExactlyInAnyOrder(
                        "experiment.schema.json",
                        "flash-sale-last-units.json",
                        "list-intersection.json",
                        "n-plus-one.json");
        assertThat(fileNames(Path.of("src/test/resources/lab/experiments"))).contains("test-fictional.json");
    }

    @Test
    void returnsTheFullDescriptor() throws Exception {
        MvcResult result = mockMvc.perform(get(BASE + "/n-plus-one")).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getContentType()).startsWith("application/json");
        JsonNode body = MAPPER.readTree(result.getResponse().getContentAsString());
        assertThat(body).isEqualTo(descriptorFile("n-plus-one"));
        assertThat(CONTRACT.validate(BASE + "/{experimentId}", "get", 200, "application/json", body))
                .isEmpty();
    }

    @Test
    void unknownExperimentIsProblem404() throws Exception {
        MvcResult result = mockMvc.perform(get(BASE + "/foo")).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(404);
        assertThat(result.getResponse().getContentType()).startsWith("application/problem+json");
        JsonNode body = MAPPER.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("type").asString()).isEqualTo("/problems/experiment-not-found");
        assertThat(body.get("status").asInt()).isEqualTo(404);
        assertThat(body.get("title").asString()).isEqualTo("Experimento não encontrado");
        assertThat(body.get("detail").asString()).isEqualTo("Não existe experimento com o id 'foo'.");
        assertThat(CONTRACT.validate(BASE + "/{experimentId}", "get", 404, "application/problem+json", body))
                .isEmpty();
    }

    private static JsonNode descriptorFile(String id) throws IOException {
        return MAPPER.readTree(Files.readString(Path.of("src/main/resources/lab/experiments", id + ".json")));
    }

    private static List<String> fileNames(Path dir) throws IOException {
        try (Stream<Path> files = Files.list(dir)) {
            return files.map(file -> file.getFileName().toString()).toList();
        }
    }
}
