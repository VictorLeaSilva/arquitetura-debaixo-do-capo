package io.github.victorleasilva.capo.lab.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Leitura de {@code LAB_SEED_PRODUCTS} (A1, passo 1; AC-001.4). */
@Tag("RF-001")
class SeedSettingsTest {

    @ParameterizedTest
    @ValueSource(strings = {"1", "10000", "1000000"})
    void acceptsIntegersFromOneToOneMillion(String raw) {
        assertThat(SeedSettings.parse(raw)).isEqualTo(Integer.parseInt(raw));
    }

    @Test
    void defaultsToOneMillionWhenAbsent() {
        assertThat(SeedSettings.parse(null)).isEqualTo(1_000_000);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-5", "1000001", "abc", ""})
    void rejectsAnythingElseCitingKeyAndValue(String raw) {
        assertThatThrownBy(() -> SeedSettings.parse(raw))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("LAB_SEED_PRODUCTS inválido: \"" + raw + "\" (use um inteiro de 1 a 1000000)");
    }
}
