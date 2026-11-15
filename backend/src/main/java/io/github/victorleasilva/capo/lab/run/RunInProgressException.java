package io.github.victorleasilva.capo.lab.run;

/** Outra execução segura a trava (D-04); vira um 409 {@code /problems/run-in-progress}. */
public class RunInProgressException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RunInProgressException() {
        super("outra execução está em andamento");
    }
}
