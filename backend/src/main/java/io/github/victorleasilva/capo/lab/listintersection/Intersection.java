package io.github.victorleasilva.capo.lab.listintersection;

import java.util.List;

/**
 * Uma versão da interseção de listas (A3). Cada versão fica num arquivo, para o {@code sourceRef} do descritor apontar
 * para ela (Q-039).
 */
interface Intersection {

    /** Os SKUs do lote que existem no catálogo, na ordem do lote. */
    List<String> found(List<String> catalog, List<String> batch);
}
