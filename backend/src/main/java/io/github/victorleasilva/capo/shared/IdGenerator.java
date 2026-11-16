package io.github.victorleasilva.capo.shared;

import java.util.UUID;

/** O gerador de IDs injetado (00 §4): os testes podem trocá-lo por um gerador previsível. */
public interface IdGenerator {

    UUID newId();
}
