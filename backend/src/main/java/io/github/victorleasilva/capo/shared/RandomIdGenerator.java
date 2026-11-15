package io.github.victorleasilva.capo.shared;

import java.util.UUID;
import org.springframework.stereotype.Component;

/** O gerador de IDs de produção: UUID aleatório (versão 4). */
@Component
class RandomIdGenerator implements IdGenerator {

    @Override
    public UUID newId() {
        return UUID.randomUUID();
    }
}
