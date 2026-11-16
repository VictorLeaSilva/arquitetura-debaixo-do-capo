package io.github.victorleasilva.capo.lab.run;

import java.time.Duration;

/** A porta do relógio das medições (D-06; ADR-006): o teste troca a implementação para provar o método (AC-005.3). */
public interface Stopwatch {

    /** Executa a ação e devolve quanto ela levou. */
    Duration time(Runnable action);
}
