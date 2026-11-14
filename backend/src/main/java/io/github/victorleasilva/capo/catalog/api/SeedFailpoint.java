package io.github.victorleasilva.capo.catalog.api;

/** Ponto de falha de teste da carga (A1, passo 5), chamado entre a carga dos produtos e a dos atributos. */
public interface SeedFailpoint {

    void afterProducts();
}
