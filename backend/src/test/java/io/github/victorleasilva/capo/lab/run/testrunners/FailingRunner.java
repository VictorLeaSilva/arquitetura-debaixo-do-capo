package io.github.victorleasilva.capo.lab.run.testrunners;

import io.github.victorleasilva.capo.lab.run.ExperimentRunner;
import io.github.victorleasilva.capo.lab.run.VariantOutcome;
import java.util.Map;
import org.springframework.stereotype.Component;

/** O test-failing (03 §4.1): a execução lança uma exceção (AC-003.9). */
@Component
public class FailingRunner implements ExperimentRunner {

    public static final String MESSAGE = "falha de teste da execução";

    @Override
    public String experimentId() {
        return "test-failing";
    }

    @Override
    public VariantOutcome run(String variant, Integer size, Map<String, Object> parameters) {
        throw new IllegalStateException(MESSAGE);
    }
}
