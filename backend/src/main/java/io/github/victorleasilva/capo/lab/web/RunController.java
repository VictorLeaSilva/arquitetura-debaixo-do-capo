package io.github.victorleasilva.capo.lab.web;

import io.github.victorleasilva.capo.lab.run.LabRun;
import io.github.victorleasilva.capo.lab.run.LabRunRepository;
import io.github.victorleasilva.capo.lab.run.RunService;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Executa um experimento e consulta execuções (RF-003, RF-004; contrato em openapi/lab-api.yaml). */
@RestController
public class RunController {

    private final RunService runService;
    private final LabRunRepository repository;

    public RunController(RunService runService, LabRunRepository repository) {
        this.runService = runService;
        this.repository = repository;
    }

    /** Executa, de forma síncrona (D-03), e responde 201 com o corpo da execução e o Location dela. */
    @PostMapping("/api/v1/lab/experiments/{experimentId}/runs")
    public ResponseEntity<RunResponse> run(@PathVariable String experimentId, @RequestBody RunRequest request) {
        List<String> variants = request.variants() == null ? List.of() : request.variants();
        Map<String, Object> parameters = request.parameters() == null ? Map.of() : request.parameters();
        LabRun run = runService.run(experimentId, variants, parameters);
        return ResponseEntity.created(URI.create("/api/v1/lab/runs/" + run.id()))
                .body(RunResponse.of(run, runService.fairnessType(experimentId)));
    }

    @GetMapping("/api/v1/lab/runs/{runId}")
    public RunResponse get(@PathVariable String runId) {
        UUID id;
        try {
            id = UUID.fromString(runId);
        } catch (IllegalArgumentException e) {
            throw new InvalidRunIdException(runId);
        }
        LabRun run = repository.find(id).orElseThrow(() -> new RunNotFoundException(runId));
        return RunResponse.of(run, runService.fairnessType(run.experimentId()));
    }
}
