package io.github.victorleasilva.capo.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import io.github.victorleasilva.capo.CapoApplication;
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

        // Guarda contra uma verificação vazia: se o pacote base estiver errado, nenhuma classe é importada
        // e as regras (que aceitam "nenhuma classe") passariam sem verificar nada.
        assertThat(production.contain(CapoApplication.class)).isTrue();

        List.of(
                        ArchitectureRules.DOMAIN_IS_FRAMEWORK_FREE,
                        ArchitectureRules.EXPERIMENT_ONLY_FROM_LAB_OR_EXPERIMENT,
                        ArchitectureRules.MODULES_TALK_ONLY_THROUGH_API,
                        ArchitectureRules.NO_FLOATING_POINT_MONEY)
                .forEach(rule -> rule.check(production));
    }
}
