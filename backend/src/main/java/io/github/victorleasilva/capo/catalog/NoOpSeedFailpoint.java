package io.github.victorleasilva.capo.catalog;

import io.github.victorleasilva.capo.catalog.api.SeedFailpoint;
import org.springframework.stereotype.Component;

/** O ponto de falha da carga em produção: não faz nada. Só os testes trocam por uma versão que lança exceção. */
@Component
class NoOpSeedFailpoint implements SeedFailpoint {

    @Override
    public void afterProducts() {
        // de propósito vazio
    }
}
