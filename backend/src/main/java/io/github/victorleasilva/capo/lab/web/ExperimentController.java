package io.github.victorleasilva.capo.lab.web;

import io.github.victorleasilva.capo.lab.descriptor.ExperimentCatalog;
import io.github.victorleasilva.capo.lab.descriptor.ExperimentDescriptor;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Lista e detalha os experimentos do laboratório (RF-002; contrato em openapi/lab-api.yaml). */
@RestController
@RequestMapping("/api/v1/lab/experiments")
public class ExperimentController {

    private final ExperimentCatalog catalog;

    public ExperimentController(ExperimentCatalog catalog) {
        this.catalog = catalog;
    }

    /** O resumo de cada experimento, em ordem de id. */
    @GetMapping
    public List<ExperimentSummary> list() {
        return catalog.all().stream()
                .map(d -> new ExperimentSummary(d.id(), d.title(), d.summary(), d.studyPage()))
                .toList();
    }

    /** O descritor completo, com o texto exato do arquivo (AC-002.2). */
    @GetMapping("/{experimentId}")
    public ResponseEntity<String> get(@PathVariable String experimentId) {
        ExperimentDescriptor descriptor =
                catalog.find(experimentId).orElseThrow(() -> new ExperimentNotFoundException(experimentId));
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(descriptor.json());
    }
}
