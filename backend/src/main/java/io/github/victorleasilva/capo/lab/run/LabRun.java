package io.github.victorleasilva.capo.lab.run;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Uma execução lida de {@code lab_run} (03 §3), com os campos jsonb já convertidos. */
public record LabRun(
        UUID id,
        String experimentId,
        String status,
        Map<String, Object> parameters,
        List<RunResult> results,
        String failureReason,
        Map<String, Object> environment,
        String traceId,
        Instant startedAt,
        Instant finishedAt) {

    public LabRun {
        parameters = Map.copyOf(parameters);
        results = results == null ? null : List.copyOf(results);
        environment = Map.copyOf(environment);
    }
}
