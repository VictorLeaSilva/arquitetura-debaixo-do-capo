package io.github.victorleasilva.capo.lab.web;

/** Não existe execução com o id pedido; vira um 404 {@code /problems/run-not-found}. */
public class RunNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String runId;

    public RunNotFoundException(String runId) {
        super("execução inexistente: " + runId);
        this.runId = runId;
    }

    public String runId() {
        return runId;
    }
}
