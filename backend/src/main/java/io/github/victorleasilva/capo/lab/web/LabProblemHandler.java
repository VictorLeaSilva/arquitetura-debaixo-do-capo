package io.github.victorleasilva.capo.lab.web;

import io.github.victorleasilva.capo.lab.run.RunFailedException;
import io.github.victorleasilva.capo.lab.run.RunInProgressException;
import java.net.URI;
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

    private static ProblemDetail problem(HttpStatus status, String type, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("/problems/" + type));
        problem.setTitle(title);
        return problem;
    }
}
