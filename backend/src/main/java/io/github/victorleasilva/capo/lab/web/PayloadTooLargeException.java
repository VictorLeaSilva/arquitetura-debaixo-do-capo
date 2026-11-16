package io.github.victorleasilva.capo.lab.web;

/** O corpo do pedido passa de 16 KB; vira um 413 {@code /problems/payload-too-large}. */
public class PayloadTooLargeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PayloadTooLargeException() {
        super("corpo acima de 16 KB");
    }
}
