package io.github.victorleasilva.capo.lab.listintersection;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Cada versão do descritor aponta, por sourceRef, para o arquivo da classe que o runner usa (Q-039; AC-T013.1). */
@Tag("RF-005")
class ListIntersectionDescriptorTest {

    @Test
    void sourceRefPointsToTheVersionClass() throws IOException {
        JsonNode descriptor = JsonMapper.builder()
                .build()
                .readTree(Files.readString(Path.of("src/main/resources/lab/experiments/list-intersection.json")));

        List<String> ids = new ArrayList<>();
        for (JsonNode variant : descriptor.get("variants")) {
            String id = variant.get("id").asString();
            ids.add(id);
            assertThat(variant.has("sourceRef")).as("sourceRef de " + id).isTrue();
            // Os testes rodam em backend/; o sourceRef é relativo à raiz do repositório.
            Path file = Path.of("..").resolve(variant.get("sourceRef").asString());
            assertThat(file).as("arquivo de " + id).isRegularFile();
            assertThat(file.getFileName().toString())
                    .isEqualTo(
                            ListIntersectionRunner.VERSIONS.get(id).getClass().getSimpleName() + ".java");
        }
        assertThat(ids).containsExactlyInAnyOrderElementsOf(ListIntersectionRunner.VERSIONS.keySet());
    }
}
