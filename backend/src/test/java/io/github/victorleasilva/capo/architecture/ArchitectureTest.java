package io.github.victorleasilva.capo.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Aplica as regras de arquitetura (RNF-004) ao código de produção. */
@Tag("RNF-004")
class ArchitectureTest {

    @Test
    void productionCodeFollowsTheRules() {
        JavaClasses production = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(ArchitectureRules.PRODUCTION_BASE);

        List.of(
                        ArchitectureRules.DOMAIN_IS_FRAMEWORK_FREE,
                        ArchitectureRules.EXPERIMENT_ONLY_FROM_LAB_OR_EXPERIMENT,
                        ArchitectureRules.MODULES_TALK_ONLY_THROUGH_API,
                        ArchitectureRules.NO_FLOATING_POINT_MONEY)
                .forEach(rule -> rule.check(production));
    }
}
