package io.github.victorleasilva.capo.lab.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.victorleasilva.capo.lab.run.Fairness;
import io.github.victorleasilva.capo.lab.run.LabRun;
import io.github.victorleasilva.capo.lab.run.RunResult;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * O corpo de uma execução (03 §5), igual no 201 do POST e no 200 do GET. Em FAILED, {@code results} vem nulo e
 * {@code failureReason} preenchido; fora de FAILED, não há {@code failureReason}.
 */
public record RunResponse(
        UUID id,
        String experimentId,
        String status,
        Instant startedAt,
        Instant finishedAt,
        String traceId,
        Map<String, Object> environment,
        Map<String, Object> parameters,
        List<RunResult> results,
        Fairness fairness,
        @JsonInclude(JsonInclude.Include.NON_NULL) String failureReason) {

    public RunResponse {
        environment = Map.copyOf(environment);
        parameters = Map.copyOf(parameters);
        results = results == null ? null : List.copyOf(results);
    }

    static RunResponse of(LabRun run, String fairnessType) {
        return new RunResponse(
                run.id(),
                run.experimentId(),
                run.status(),
                run.startedAt(),
                run.finishedAt(),
                run.traceId(),
                run.environment(),
                run.parameters(),
                run.results(),
                Fairness.of(fairnessType, run.results()),
                run.failureReason());
    }
}
