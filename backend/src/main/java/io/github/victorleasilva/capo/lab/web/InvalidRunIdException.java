package io.github.victorleasilva.capo.lab.web;

/** O id de execução pedido não é um UUID; vira um 400 {@code /problems/invalid-parameter} com o campo runId. */
public class InvalidRunIdException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidRunIdException(String runId) {
        super("id de execução que não é UUID: " + runId);
    }
}
