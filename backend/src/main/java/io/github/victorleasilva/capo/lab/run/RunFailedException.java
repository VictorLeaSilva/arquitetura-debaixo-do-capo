package io.github.victorleasilva.capo.lab.run;

import java.util.UUID;

/** A execução falhou e ficou FAILED em lab_run; vira um 500 {@code /problems/run-failed} com o {@code runId}. */
public class RunFailedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient UUID runId;

    public RunFailedException(UUID runId, Throwable cause) {
        super("a execução " + runId + " falhou", cause);
        this.runId = runId;
    }

    public UUID runId() {
        return runId;
    }
}
