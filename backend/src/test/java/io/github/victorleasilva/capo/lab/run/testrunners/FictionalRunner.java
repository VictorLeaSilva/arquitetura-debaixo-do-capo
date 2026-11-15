package io.github.victorleasilva.capo.lab.run.testrunners;

import io.github.victorleasilva.capo.lab.run.ExperimentRunner;
import io.github.victorleasilva.capo.lab.run.VariantOutcome;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Execução determinística do test-fictional, só no classpath de teste (Q-043). */
@Component
public class FictionalRunner implements ExperimentRunner {

    @Override
    public String experimentId() {
        return "test-fictional";
    }

    @Override
    public VariantOutcome run(String variant, Integer size, Map<String, Object> parameters) {
        return new VariantOutcome(Map.of("score", size * 10.0), "fictional-" + size);
    }
}
