package io.github.victorleasilva.capo.lab.run;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** O método de medição D-06: 2 aquecimentos descartados e {@code repetitions} medidas (AC-005.3). */
@Tag("RF-005")
class MeasurementTest {

    /** Executa a ação de verdade e devolve, em ordem, as durações dadas (em ms). */
    private static Stopwatch fake(long... millis) {
        Deque<Duration> durations = new ArrayDeque<>();
        for (long ms : millis) {
            durations.add(Duration.ofMillis(ms));
        }
        return action -> {
            action.run();
            return durations.removeFirst();
        };
    }

    @Test
    void warmupsAreDiscarded() {
        Measurement.Measured<String> measured = Measurement.repeat(fake(1000, 1000, 10, 20, 30), 3, () -> "x");

        assertThat(measured.medianMs()).isEqualTo(20.0);
        assertThat(measured.minMs()).isEqualTo(10.0);
        assertThat(measured.maxMs()).isEqualTo(30.0);
    }

    @Test
    void medianOfAnEvenCountIsTheMeanOfTheTwoInTheMiddle() {
        Measurement.Measured<String> measured = Measurement.repeat(fake(1000, 1000, 40, 10, 30, 20), 4, () -> "x");

        assertThat(measured.medianMs()).isEqualTo(25.0);
        assertThat(measured.minMs()).isEqualTo(10.0);
        assertThat(measured.maxMs()).isEqualTo(40.0);
    }

    @Test
    void durationsKeepTheFractionOfAMillisecond() {
        Stopwatch stopwatch = action -> {
            action.run();
            return Duration.ofNanos(1_500_000);
        };

        assertThat(Measurement.repeat(stopwatch, 1, () -> "x").medianMs()).isEqualTo(1.5);
    }

    @Test
    void workRunsTwoWarmupsPlusTheRepetitionsAndTheLastResultIsKept() {
        AtomicInteger calls = new AtomicInteger();

        Measurement.Measured<Integer> measured = Measurement.repeat(fake(1, 1, 1, 1, 1), 3, calls::incrementAndGet);

        assertThat(calls).hasValue(5);
        assertThat(measured.result()).isEqualTo(5);
    }

    @Test
    void atLeastOneRepetition() {
        assertThatThrownBy(() -> Measurement.repeat(fake(), 0, () -> "x")).isInstanceOf(IllegalArgumentException.class);
    }
}
