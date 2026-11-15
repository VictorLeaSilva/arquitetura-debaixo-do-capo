package io.github.victorleasilva.capo.lab.run;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** A verificação de comparação justa (P3; A2, passo 8), calculada a partir dos resultados (AC-T012.2). */
@Tag("RF-003")
class FairnessTest {

    private static RunResult result(String variant, int size, String checksum) {
        return new RunResult(variant, size, Map.of("score", 1.0), checksum);
    }

    @Test
    void sameChecksumsPerSizeMeanSameResult() {
        Fairness fairness = Fairness.of(
                "same-result",
                List.of(result("bad", 1, "a"), result("bad", 2, "b"), result("good", 1, "a"), result("good", 2, "b")));
        assertThat(fairness).isEqualTo(new Fairness("same-result", true));
    }

    @Test
    void aDifferentChecksumIsNotSameResult() {
        Fairness fairness = Fairness.of("same-result", List.of(result("bad", 1, "a"), result("good", 1, "x")));
        assertThat(fairness).isEqualTo(new Fairness("same-result", false));
    }

    @Test
    void aMissingChecksumIsNotSameResult() {
        Fairness fairness = Fairness.of("same-result", List.of(result("bad", 1, "a"), result("good", 1, null)));
        assertThat(fairness).isEqualTo(new Fairness("same-result", false));
    }

    @Test
    void demonstratesErrorHasNoSameResult() {
        Fairness fairness = Fairness.of("demonstrates-error", List.of(result("bad", 1, "a"), result("good", 1, "b")));
        assertThat(fairness).isEqualTo(new Fairness("demonstrates-error", null));
    }
}
