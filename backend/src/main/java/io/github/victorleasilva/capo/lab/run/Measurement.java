package io.github.victorleasilva.capo.lab.run;

import java.util.Arrays;
import java.util.function.Supplier;

/**
 * O método de medição D-06 (ADR-006), usado pelos experimentos de tempo: 2 aquecimentos, para o JIT compilar o código,
 * com a duração descartada, e {@code repetitions} medidas, publicadas como mediana, mínimo e máximo (P7: a dispersão
 * fica visível).
 */
public final class Measurement {

    static final int WARMUPS = 2;

    private Measurement() {}

    /** Mede {@code work} pelo {@code stopwatch}; o resultado devolvido é o da última repetição. */
    public static <T> Measured<T> repeat(Stopwatch stopwatch, int repetitions, Supplier<T> work) {
        if (repetitions < 1) {
            throw new IllegalArgumentException("repetitions precisa ser ao menos 1: " + repetitions);
        }
        Holder<T> last = new Holder<>();
        for (int i = 0; i < WARMUPS; i++) {
            stopwatch.time(() -> last.value = work.get());
        }
        double[] millis = new double[repetitions];
        for (int i = 0; i < repetitions; i++) {
            millis[i] = stopwatch.time(() -> last.value = work.get()).toNanos() / 1_000_000.0;
        }
        Arrays.sort(millis);
        int middle = repetitions / 2;
        double median = repetitions % 2 == 0 ? (millis[middle - 1] + millis[middle]) / 2 : millis[middle];
        return new Measured<>(last.value, median, millis[0], millis[repetitions - 1]);
    }

    /** O resultado da última repetição e a mediana, o mínimo e o máximo das durações medidas, em ms. */
    public record Measured<T>(T result, double medianMs, double minMs, double maxMs) {}

    private static final class Holder<T> {
        private T value;
    }
}
