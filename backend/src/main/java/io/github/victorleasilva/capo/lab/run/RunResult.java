package io.github.victorleasilva.capo.lab.run;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

/** Um item de {@code results} (03 §5): a versão, o tamanho (se houver), as medições e o checksum (se houver). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RunResult(String variant, Integer size, Map<String, Double> measurements, String checksum) {

    public RunResult {
        measurements = Map.copyOf(measurements);
    }
}
