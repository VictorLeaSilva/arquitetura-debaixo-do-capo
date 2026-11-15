package io.github.victorleasilva.capo.lab.run;

import java.util.Map;

/** O que uma versão produziu num tamanho: as medições pelo id e o checksum do resultado funcional (pode ser nulo). */
public record VariantOutcome(Map<String, Double> measurements, String checksum) {

    public VariantOutcome {
        measurements = Map.copyOf(measurements);
    }
}
