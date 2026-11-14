package io.github.victorleasilva.capo.lab.web;

/** Não existe experimento com o id pedido; vira um 404 {@code /problems/experiment-not-found}. */
public class ExperimentNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String experimentId;

    public ExperimentNotFoundException(String experimentId) {
        super("experimento inexistente: " + experimentId);
        this.experimentId = experimentId;
    }

    public String experimentId() {
        return experimentId;
    }
}
