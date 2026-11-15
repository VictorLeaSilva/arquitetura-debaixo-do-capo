package io.github.victorleasilva.capo.lab.web;

import java.util.List;
import java.util.Map;

/** O corpo do POST de uma execução (03 §5): as versões e os parâmetros pedidos. */
public record RunRequest(List<String> variants, Map<String, Object> parameters) {

    public RunRequest {
        variants = variants == null ? null : List.copyOf(variants);
        parameters = parameters == null ? null : Map.copyOf(parameters);
    }
}
