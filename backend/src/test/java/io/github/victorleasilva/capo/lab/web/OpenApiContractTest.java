package io.github.victorleasilva.capo.lab.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Todo status declarado no lab-api.yaml tem exemplo, e todo exemplo passa no próprio schema (03 §5; AC-T011.2). */
@Tag("RF-002")
class OpenApiContractTest {

    private final OpenApiContract contract = new OpenApiContract();

    @Test
    void everyDeclaredResponseHasAValidExample() {
        int checked = 0;
        for (Map.Entry<String, JsonNode> path : contract.document().get("paths").properties()) {
            for (Map.Entry<String, JsonNode> operation : path.getValue().properties()) {
                if (!operation.getValue().has("responses")) {
                    continue;
                }
                for (Map.Entry<String, JsonNode> response :
                        operation.getValue().get("responses").properties()) {
                    for (Map.Entry<String, JsonNode> media :
                            response.getValue().get("content").properties()) {
                        String where = operation.getKey() + " " + path.getKey() + " " + response.getKey() + " "
                                + media.getKey();
                        JsonNode examples = media.getValue().get("examples");
                        assertThat(examples).as("exemplos de " + where).isNotNull();
                        assertThat(examples.size()).as("exemplos de " + where).isPositive();
                        for (JsonNode example : examples) {
                            assertThat(contract.validate(
                                            path.getKey(),
                                            operation.getKey(),
                                            Integer.parseInt(response.getKey()),
                                            media.getKey(),
                                            example.get("value")))
                                    .as(where)
                                    .isEmpty();
                            checked++;
                        }
                    }
                }
            }
        }
        assertThat(checked).isEqualTo(11);
    }

    /** O verificador tem efeito: um corpo fora do contrato é recusado (senão, os testes da API passariam sempre). */
    @Test
    void contractRejectsBodiesOutsideTheSchema() {
        JsonMapper mapper = JsonMapper.builder().build();
        String list = "/api/v1/lab/experiments";
        String detail = list + "/{experimentId}";

        assertThat(contract.validate(list, "get", 200, "application/json", mapper.readTree("[{\"id\": \"x\"}]")))
                .as("item da lista sem title, summary e studyPage")
                .isNotEmpty();
        assertThat(contract.validate(
                        list,
                        "get",
                        200,
                        "application/json",
                        mapper.readTree("[{\"id\": \"x\", \"title\": \"t\", \"summary\": \"s\","
                                + " \"studyPage\": \"p\", \"extra\": 1}]")))
                .as("item da lista com campo extra")
                .isNotEmpty();
        assertThat(contract.validate(detail, "get", 200, "application/json", mapper.readTree("{\"id\": \"x\"}")))
                .as("descritor incompleto")
                .isNotEmpty();
        assertThat(contract.validate(
                        detail, "get", 404, "application/problem+json", mapper.readTree("{\"title\": \"t\"}")))
                .as("problema sem type e status")
                .isNotEmpty();
    }
}
