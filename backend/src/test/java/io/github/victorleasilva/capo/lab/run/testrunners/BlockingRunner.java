package io.github.victorleasilva.capo.lab.run.testrunners;

import io.github.victorleasilva.capo.lab.run.ExperimentRunner;
import io.github.victorleasilva.capo.lab.run.VariantOutcome;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

/**
 * O test-blocking (03 §4.1): a execução, já com a trava segura, avisa que entrou e espera o teste mandar sair
 * (AC-003.8). A espera tem limite de 30 s, para um teste com defeito não travar o build.
 */
@Component
public class BlockingRunner implements ExperimentRunner {

    private volatile CountDownLatch entered = new CountDownLatch(1);
    private volatile CountDownLatch release = new CountDownLatch(1);

    @Override
    public String experimentId() {
        return "test-blocking";
    }

    /** Prepara uma nova rodada: o próximo run avisa "entrou" e espera {@link #release()}. */
    public void arm() {
        entered = new CountDownLatch(1);
        release = new CountDownLatch(1);
    }

    public boolean awaitEntered(long seconds) throws InterruptedException {
        return entered.await(seconds, TimeUnit.SECONDS);
    }

    public void release() {
        release.countDown();
    }

    @Override
    public VariantOutcome run(String variant, Integer size, Map<String, Object> parameters) {
        entered.countDown();
        try {
            if (!release.await(30, TimeUnit.SECONDS)) {
                throw new IllegalStateException("o teste não liberou a execução em 30 s");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("execução interrompida", e);
        }
        return new VariantOutcome(Map.of("score", 1.0), "blocking");
    }
}
