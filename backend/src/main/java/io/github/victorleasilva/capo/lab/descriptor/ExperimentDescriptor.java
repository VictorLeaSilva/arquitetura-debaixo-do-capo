package io.github.victorleasilva.capo.lab.descriptor;

/**
 * Um descritor de experimento já validado: os campos que a lista usa e o texto original do arquivo, devolvido sem
 * alteração no detalhe (AC-002.2). Guardar o texto, e não uma árvore do Jackson, mantém o registro imutável.
 */
public record ExperimentDescriptor(String id, String title, String summary, String studyPage, String json) {}
