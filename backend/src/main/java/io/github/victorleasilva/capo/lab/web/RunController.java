package io.github.victorleasilva.capo.lab.web;

import io.github.victorleasilva.capo.lab.descriptor.ExperimentCatalog;
import io.github.victorleasilva.capo.lab.descriptor.ExperimentDescriptor;
import io.github.victorleasilva.capo.lab.run.LabRun;
import io.github.victorleasilva.capo.lab.run.LabRunRepository;
import io.github.victorleasilva.capo.lab.run.RunRequestValidator;
import io.github.victorleasilva.capo.lab.run.RunService;
import io.github.victorleasilva.capo.lab.run.ValidatedRun;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.core.JacksonException;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.json.JsonMapper;

/**
 * Executa um experimento e consulta execuções (RF-003, RF-004; contrato em openapi/lab-api.yaml). O corpo do POST é
 * lido aqui, e não por um {@code @RequestBody}, para os erros saírem na precedência do AC-003.10: 404 → 413 → 400 →
 * 422 → 409.
 */
@RestController
public class RunController {

    /** O maior corpo aceito (03 §5.1; API4 de 04): 16 KB. */
    static final int MAX_BODY_BYTES = 16_384;

    private final ExperimentCatalog catalog;
    private final RunRequestValidator validator;
    private final RunService runService;
    private final LabRunRepository repository;
    private final ObjectReader reader;

    public RunController(
            ExperimentCatalog catalog,
            RunRequestValidator validator,
            RunService runService,
            LabRunRepository repository,
            JsonMapper mapper) {
        this.catalog = catalog;
        this.validator = validator;
        this.runService = runService;
        this.repository = repository;
        this.reader = mapper.readerFor(JsonNode.class)
                .with(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    }

    /** Valida, executa de forma síncrona (D-03) e responde 201 com o corpo da execução e o Location dela. */
    @PostMapping("/api/v1/lab/experiments/{experimentId}/runs")
    public ResponseEntity<RunResponse> run(@PathVariable String experimentId, HttpServletRequest request)
            throws IOException {
        ExperimentDescriptor descriptor =
                catalog.find(experimentId).orElseThrow(() -> new ExperimentNotFoundException(experimentId));
        byte[] body = request.getInputStream().readNBytes(MAX_BODY_BYTES + 1);
        if (body.length > MAX_BODY_BYTES) {
            throw new PayloadTooLargeException();
        }
        ValidatedRun validated = validator.validate(descriptor, parse(body));
        LabRun run = runService.run(experimentId, validated.variants(), validated.parameters());
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

    /**
     * O corpo como JSON em UTF-8 estrito (bytes que não são UTF-8 válido, como UTF-16, são 400, sem a detecção
     * automática do Jackson); vazio, inválido, com sobra depois do valor ou com chave repetida também é 400.
     */
    private JsonNode parse(byte[] body) {
        if (body.length == 0) {
            throw new MalformedRequestException(null);
        }
        try {
            String text = StandardCharsets.UTF_8
                    .newDecoder()
                    .decode(ByteBuffer.wrap(body))
                    .toString();
            return reader.readValue(text);
        } catch (CharacterCodingException | JacksonException e) {
            throw new MalformedRequestException(e);
        }
    }
}
