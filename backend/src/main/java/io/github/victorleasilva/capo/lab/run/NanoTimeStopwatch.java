package io.github.victorleasilva.capo.lab.run;

import java.time.Duration;
import org.springframework.stereotype.Component;

/** O relógio real das medições: {@code System.nanoTime()} antes e depois da ação (D-06). */
@Component
public class NanoTimeStopwatch implements Stopwatch {

    @Override
    public Duration time(Runnable action) {
        long start = System.nanoTime();
        action.run();
        return Duration.ofNanos(System.nanoTime() - start);
    }
}
