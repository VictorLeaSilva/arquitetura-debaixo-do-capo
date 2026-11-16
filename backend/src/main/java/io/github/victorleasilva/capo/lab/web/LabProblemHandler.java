package io.github.victorleasilva.capo.lab.web;

import io.github.victorleasilva.capo.lab.run.InsufficientDataException;
import io.github.victorleasilva.capo.lab.run.InvalidParameterException;
import io.github.victorleasilva.capo.lab.run.ParameterError;
import io.github.victorleasilva.capo.lab.run.RunFailedException;
import io.github.victorleasilva.capo.lab.run.RunInProgressException;
import io.github.victorleasilva.capo.lab.run.SizeExceedsVariantLimitException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Os erros da API do laboratório em Problem Details (RFC 9457; D-12), com {@code type} relativo. Nenhum corpo de erro
 * leva nome de classe nem stack trace (AC-RNF-009.2): só textos fixos e ids.
 */
@RestControllerAdvice
public class LabProblemHandler {

    @ExceptionHandler(ExperimentNotFoundException.class)
    ProblemDetail experimentNotFound(ExperimentNotFoundException e) {
        return problem(
                HttpStatus.NOT_FOUND,
                "experiment-not-found",
                "Experimento não encontrado",
                "Não existe experimento com o id '" + e.experimentId() + "'.");
    }

    @ExceptionHandler(RunNotFoundException.class)
    ProblemDetail runNotFound(RunNotFoundException e) {
        return problem(
                HttpStatus.NOT_FOUND,
                "run-not-found",
                "Execução não encontrada",
                "Não existe execução com o id '" + e.runId() + "'.");
    }

    @ExceptionHandler(InvalidRunIdException.class)
    ProblemDetail invalidRunId(InvalidRunIdException e) {
        ProblemDetail problem = problem(
                HttpStatus.BAD_REQUEST,
                "invalid-parameter",
                "Parâmetro inválido",
                "O id da execução precisa ser um UUID.");
        problem.setProperty("errors", List.of(Map.of("field", "runId", "message", "precisa ser um UUID")));
        return problem;
    }

    @ExceptionHandler(PayloadTooLargeException.class)
    ProblemDetail payloadTooLarge(PayloadTooLargeException e) {
        return problem(
                HttpStatus.CONTENT_TOO_LARGE,
                "payload-too-large",
                "Corpo grande demais",
                "O corpo do pedido passa de 16 KB (16384 bytes).");
    }

    @ExceptionHandler(MalformedRequestException.class)
    ProblemDetail malformedRequest(MalformedRequestException e) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "malformed-request",
                "Corpo inválido",
                "O corpo do pedido não é um JSON válido.");
    }

    @ExceptionHandler(InvalidParameterException.class)
    ProblemDetail invalidParameter(InvalidParameterException e) {
        ProblemDetail problem = problem(
                HttpStatus.BAD_REQUEST,
                "invalid-parameter",
                "Parâmetro inválido",
                "O pedido tem parâmetros inválidos; veja errors.");
        problem.setProperty(
                "errors", e.errors().stream().map(LabProblemHandler::error).toList());
        return problem;
    }

    @ExceptionHandler(SizeExceedsVariantLimitException.class)
    ProblemDetail sizeExceedsVariantLimit(SizeExceedsVariantLimitException e) {
        ProblemDetail problem = problem(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "size-exceeds-variant-limit",
                "Tamanho acima do limite da versão",
                "A versão '" + e.variant() + "' aceita tamanhos até " + e.maxSize() + "; o pedido tem " + e.size()
                        + ".");
        problem.setProperty("variant", e.variant());
        problem.setProperty("maxSize", e.maxSize());
        problem.setProperty("size", e.size());
        return problem;
    }

    @ExceptionHandler(InsufficientDataException.class)
    ProblemDetail insufficientData(InsufficientDataException e) {
        ProblemDetail problem = problem(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "insufficient-data",
                "Dados insuficientes",
                "O tamanho " + e.size() + " é maior que os " + e.seedProducts()
                        + " produtos carregados (LAB_SEED_PRODUCTS).");
        problem.setProperty("size", e.size());
        problem.setProperty("seedProducts", e.seedProducts());
        return problem;
    }

    @ExceptionHandler(RunInProgressException.class)
    ProblemDetail runInProgress(RunInProgressException e) {
        return problem(
                HttpStatus.CONFLICT,
                "run-in-progress",
                "Execução em andamento",
                "Outra execução está em andamento; tente de novo quando ela terminar.");
    }

    @ExceptionHandler(RunFailedException.class)
    ProblemDetail runFailed(RunFailedException e) {
        ProblemDetail problem = problem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "run-failed",
                "A execução falhou",
                "O motivo está em failureReason, consultando a execução.");
        problem.setProperty("runId", e.runId().toString());
        return problem;
    }

    /** Um item de errors, com field antes de message. */
    private static Map<String, String> error(ParameterError error) {
        Map<String, String> item = new LinkedHashMap<>();
        item.put("field", error.field());
        item.put("message", error.message());
        return item;
    }

    private static ProblemDetail problem(HttpStatus status, String type, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("/problems/" + type));
        problem.setTitle(title);
        return problem;
    }
}
