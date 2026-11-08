package io.github.victorleasilva.capo.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import io.github.victorleasilva.archfixtures.catalog.DecimalPrice;
import io.github.victorleasilva.archfixtures.catalog.DoublePrice;
import io.github.victorleasilva.archfixtures.catalog.FloatingMoney;
import io.github.victorleasilva.archfixtures.catalog.UsesOrderingApi;
import io.github.victorleasilva.archfixtures.catalog.UsesOrderingExperiment;
import io.github.victorleasilva.archfixtures.catalog.UsesOrderingInternal;
import io.github.victorleasilva.archfixtures.inventory.domain.PureEntity;
import io.github.victorleasilva.archfixtures.inventory.domain.SpringDependentEntity;
import io.github.victorleasilva.archfixtures.ordering.api.Facade;
import io.github.victorleasilva.archfixtures.ordering.domain.SpringDependentOrder;
import io.github.victorleasilva.archfixtures.ordering.experiment.NaiveThing;
import io.github.victorleasilva.archfixtures.ordering.internal.Hidden;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Prova que cada regra de arquitetura reprova uma violação proposital e aceita um exemplo correto.
 *
 * <p>As classes avaliadas ficam em {@code io.github.victorleasilva.archfixtures}, fora do pacote de
 * produção, para nunca entrarem na verificação real ({@link ArchitectureTest}).
 */
@Tag("RNF-004")
class ArchitectureRulesTest {

    private static final String FIXTURES = "io.github.victorleasilva.archfixtures";

    private static boolean violates(ArchRule rule, Class<?>... classes) {
        JavaClasses imported = new ClassFileImporter().importClasses(classes);
        return rule.evaluate(imported).hasViolation();
    }

    @Test
    void domainMustNotDependOnFrameworks() {
        ArchRule rule = ArchitectureRules.domainIsFrameworkFree(FIXTURES);

        assertThat(violates(rule, SpringDependentEntity.class)).isTrue();
        assertThat(violates(rule, SpringDependentOrder.class)).isTrue();
        assertThat(violates(rule, PureEntity.class)).isFalse();
    }

    @Test
    void experimentPackagesAreOnlyUsedByLabOrExperiment() {
        ArchRule rule = ArchitectureRules.experimentOnlyFromLabOrExperiment(FIXTURES);

        assertThat(violates(rule, UsesOrderingExperiment.class, NaiveThing.class))
                .isTrue();
        assertThat(violates(
                        rule, io.github.victorleasilva.archfixtures.lab.UsesOrderingExperiment.class, NaiveThing.class))
                .isFalse();
    }

    @Test
    void modulesTalkOnlyThroughApi() {
        ArchRule rule = ArchitectureRules.modulesTalkOnlyThroughApi(FIXTURES);

        assertThat(violates(rule, UsesOrderingInternal.class, Hidden.class)).isTrue();
        assertThat(violates(rule, UsesOrderingApi.class, Facade.class)).isFalse();
        assertThat(violates(
                        rule, io.github.victorleasilva.archfixtures.lab.UsesOrderingExperiment.class, NaiveThing.class))
                .isFalse();
    }

    @Test
    void moneyIsNeverFloatingPoint() {
        ArchRule rule = ArchitectureRules.noFloatingPointMoney(FIXTURES);

        assertThat(violates(rule, DoublePrice.class)).isTrue();
        assertThat(violates(rule, DecimalPrice.class)).isFalse();

        // Cada nome (total, amount, cost, maiúsculas) e cada tipo (float, Double, Float, double) é um achado.
        JavaClasses floating = new ClassFileImporter().importClasses(FloatingMoney.class);
        assertThat(rule.evaluate(floating).getFailureReport().getDetails()).hasSize(4);
    }
}
