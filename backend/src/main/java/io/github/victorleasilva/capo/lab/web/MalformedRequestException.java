package io.github.victorleasilva.capo.lab.web;

/** O corpo do pedido não é um JSON válido; vira um 400 {@code /problems/malformed-request}. */
public class MalformedRequestException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public MalformedRequestException(Throwable cause) {
        super("corpo que não é JSON válido", cause);
    }
}
