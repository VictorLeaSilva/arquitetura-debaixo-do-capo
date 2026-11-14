package io.github.victorleasilva.capo.lab.web;

/** Um experimento na lista: só o que a tela precisa para mostrar o cartão e o link da página de estudo. */
public record ExperimentSummary(String id, String title, String summary, String studyPage) {}
