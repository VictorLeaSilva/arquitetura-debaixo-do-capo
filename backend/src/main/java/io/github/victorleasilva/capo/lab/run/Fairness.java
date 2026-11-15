package io.github.victorleasilva.capo.lab.run;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A verificação de comparação justa (P3; A2, passo 8). Não é gravada: é calculada do descritor e dos resultados sempre
 * que a execução é devolvida, para o 201 e o GET saírem iguais.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Fairness(String type, Boolean sameResult) {

    static final String SAME_RESULT = "same-result";

    /**
     * {@code same-result}: {@code sameResult} é verdadeiro quando, em cada tamanho, todas as versões têm o mesmo checksum
     * e nenhum é nulo. Outro tipo (ou execução sem resultados): sem {@code sameResult}.
     */
    public static Fairness of(String type, List<RunResult> results) {
        if (!SAME_RESULT.equals(type) || results == null) {
            return new Fairness(type, null);
        }
        Map<Integer, String> checksumBySize = new HashMap<>();
        boolean same = true;
        for (RunResult result : results) {
            if (result.checksum() == null) {
                same = false;
                break;
            }
            String first = checksumBySize.putIfAbsent(result.size(), result.checksum());
            if (first != null && !Objects.equals(first, result.checksum())) {
                same = false;
                break;
            }
        }
        return new Fairness(type, same);
    }
}
