package io.github.victorleasilva.capo.lab.run;

import io.github.victorleasilva.capo.lab.descriptor.ExperimentCatalog;
import io.github.victorleasilva.capo.lab.descriptor.ExperimentDescriptor;
import io.github.victorleasilva.capo.lab.web.ExperimentNotFoundException;
import io.github.victorleasilva.capo.shared.IdGenerator;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Executa um experimento (A2; D-03: síncrono, no mesmo processo), uma execução por vez (D-04). Recebe o pedido já
 * validado pelo {@link RunRequestValidator} (A2, passos 2 a 4); os spans são da T-015.
 */
@Component
public final class RunService {

    private static final int MAX_REASON = 500;

    private final ExperimentCatalog catalog;
    private final RunLock lock;
    private final LabRunRepository repository;
    private final EnvironmentProbe environmentProbe;
    private final TraceIds traceIds;
    private final IdGenerator idGenerator;
    private final Clock clock;
    private final JsonMapper mapper;
    private final Map<String, ExperimentRunner> runners = new HashMap<>();

    public RunService(
            ExperimentCatalog catalog,
            RunLock lock,
            LabRunRepository repository,
            EnvironmentProbe environmentProbe,
            TraceIds traceIds,
            IdGenerator idGenerator,
            Clock clock,
            JsonMapper mapper,
            List<ExperimentRunner> runners) {
        this.catalog = catalog;
        this.lock = lock;
        this.repository = repository;
        this.environmentProbe = environmentProbe;
        this.traceIds = traceIds;
        this.idGenerator = idGenerator;
        this.clock = clock;
        this.mapper = mapper;
        for (ExperimentRunner runner : runners) {
            if (this.runners.putIfAbsent(runner.experimentId(), runner) != null) {
                throw new IllegalStateException("duas implementações para o experimento " + runner.experimentId());
            }
        }
    }

    /** Executa, com as versões e os parâmetros efetivos já validados, e devolve a execução como ficou gravada. */
    public LabRun run(String experimentId, List<String> variants, Map<String, Object> parameters) {
        ExperimentDescriptor descriptor =
                catalog.find(experimentId).orElseThrow(() -> new ExperimentNotFoundException(experimentId));
        JsonNode document = mapper.readTree(descriptor.json());

        RunLock.Held held = lock.tryAcquire().orElseThrow(RunInProgressException::new);
        try (held) {
            UUID id = idGenerator.newId();
            repository.insertRunning(
                    id,
                    experimentId,
                    parameters,
                    environmentProbe.describe(),
                    traceIds.currentTraceId(),
                    clock.instant());
            try {
                ExperimentRunner runner = runners.get(experimentId);
                if (runner == null) {
                    throw new IllegalStateException(
                            "nenhuma implementação registrada para o experimento " + experimentId);
                }
                List<RunResult> results = new ArrayList<>();
                List<Integer> sizes = sizes(document, parameters);
                for (String variant : variants) {
                    for (Integer size : sizes) {
                        VariantOutcome outcome = runner.run(variant, size, parameters);
                        results.add(new RunResult(variant, size, outcome.measurements(), outcome.checksum()));
                    }
                }
                repository.complete(id, results, clock.instant());
            } catch (RuntimeException e) {
                repository.fail(id, reason(e), clock.instant());
                throw new RunFailedException(id, e);
            }
            return repository.find(id).orElseThrow();
        }
    }

    /** O tipo de {@code fairness} do descritor de um experimento. */
    public String fairnessType(String experimentId) {
        return catalog.find(experimentId)
                .map(d -> mapper.readTree(d.json()).get("fairness").asString())
                .orElseThrow(() -> new ExperimentNotFoundException(experimentId));
    }

    /** Os valores do parâmetro com {@code isSize}, na ordem do pedido; sem esse parâmetro, uma rodada sem tamanho. */
    private static List<Integer> sizes(JsonNode document, Map<String, Object> parameters) {
        for (JsonNode parameter : document.get("parameters")) {
            if (parameter.path("isSize").asBoolean(false)) {
                List<Integer> sizes = new ArrayList<>();
                for (Object value :
                        (List<?>) parameters.get(parameter.get("name").asString())) {
                    sizes.add(((Number) value).intValue());
                }
                return sizes;
            }
        }
        List<Integer> once = new ArrayList<>();
        once.add(null);
        return once;
    }

    /** A mensagem da exceção, nunca o nome da classe (AC-RNF-009.2). */
    private static String reason(RuntimeException e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            return "erro sem mensagem";
        }
        return message.length() > MAX_REASON ? message.substring(0, MAX_REASON) : message;
    }
}
