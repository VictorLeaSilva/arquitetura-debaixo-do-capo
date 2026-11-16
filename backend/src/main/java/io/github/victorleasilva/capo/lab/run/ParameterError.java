package io.github.victorleasilva.capo.lab.run;

/** Um item de {@code errors} no 400 {@code /problems/invalid-parameter}: o campo e o que está errado nele. */
public record ParameterError(String field, String message) {}
