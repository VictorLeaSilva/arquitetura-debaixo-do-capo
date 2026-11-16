package io.github.victorleasilva.capo.lab.run;

import io.github.victorleasilva.capo.shared.IdGenerator;
import org.springframework.stereotype.Component;

/**
 * Implementação provisória da porta {@link TraceIds} (Q-042): até a T-015 ligar o tracing, o id é um UUID do gerador
 * injetado sem os hífens (32 hexadecimais, o formato de um trace id). Não há trace correspondente no Grafana.
 */
@Component
class GeneratedTraceIds implements TraceIds {

    private final IdGenerator idGenerator;

    GeneratedTraceIds(IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    @Override
    public String currentTraceId() {
        return idGenerator.newId().toString().replace("-", "");
    }
}
