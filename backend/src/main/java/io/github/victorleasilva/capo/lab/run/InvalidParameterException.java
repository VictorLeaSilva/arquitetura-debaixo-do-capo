package io.github.victorleasilva.capo.lab.run;

import java.util.List;

/** O pedido não passou na validação contra o descritor; vira um 400 {@code /problems/invalid-parameter}. */
public class InvalidParameterException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient List<ParameterError> errors;

    public InvalidParameterException(List<ParameterError> errors) {
        super("pedido de execução inválido");
        this.errors = List.copyOf(errors);
    }

    public List<ParameterError> errors() {
        return errors;
    }
}
