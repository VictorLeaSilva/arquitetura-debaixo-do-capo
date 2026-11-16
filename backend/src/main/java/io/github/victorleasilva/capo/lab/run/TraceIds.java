package io.github.victorleasilva.capo.lab.run;

/** O id do trace da execução corrente, gravado em lab_run.trace_id (A2, passo 6; Q-042). */
public interface TraceIds {

    String currentTraceId();
}
