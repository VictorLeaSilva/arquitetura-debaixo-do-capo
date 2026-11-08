package io.github.victorleasilva.capo.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Aplica as regras de arquitetura (RNF-004) ao código de produção. */
@Tag("RNF-004")
class ArchitectureTest {

    private static final String BASE = "io.github.victorleasilva.capo";

    @Test
    void productionCodeFollowsTheRules() {
        JavaClasses production = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE);

        ArchitectureRules.all(BASE).forEach(rule -> rule.check(production));
    }
}
