package io.github.victorleasilva.capo.lab.run;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Um pedido de execução que passou na validação: as versões na ordem do pedido e os parâmetros efetivos, na ordem do
 * descritor ({@code Integer} ou {@code List<Integer>}).
 */
public record ValidatedRun(List<String> variants, Map<String, Object> parameters) {

    public ValidatedRun {
        variants = List.copyOf(variants);
        parameters = Collections.unmodifiableMap(new LinkedHashMap<>(parameters));
    }
}
