package io.github.victorleasilva.capo.lab.web;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Os erros da API do laboratório em Problem Details (RFC 9457; D-12), com {@code type} relativo. */
@RestControllerAdvice
public class LabProblemHandler {

    @ExceptionHandler(ExperimentNotFoundException.class)
    ProblemDetail experimentNotFound(ExperimentNotFoundException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, "Não existe experimento com o id '" + e.experimentId() + "'.");
        problem.setType(URI.create("/problems/experiment-not-found"));
        problem.setTitle("Experimento não encontrado");
        return problem;
    }
}
