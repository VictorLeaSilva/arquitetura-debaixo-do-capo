package io.github.victorleasilva.capo.architecture;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.CompositeArchRule;
import java.util.ArrayList;
import java.util.List;

/**
 * Regras de arquitetura do projeto (RNF-004, {@code 01 §3}).
 *
 * <p>Cada regra recebe o pacote base, para valer tanto no código de produção
 * ({@code io.github.victorleasilva.capo}) quanto nas fixtures de teste
 * ({@code io.github.victorleasilva.archfixtures}). Todas aceitam "nenhuma classe encontrada"
 * ({@code allowEmptyShould}), porque os módulos ainda não existem; {@link ArchitectureRulesTest}
 * prova que cada uma reprova uma violação de verdade.
 */
public final class ArchitectureRules {

    /** Os módulos de negócio e o laboratório: o segmento logo abaixo do pacote base. */
    static final List<String> MODULES = List.of("catalog", "inventory", "ordering", "lab");

    private static final String LAB = "lab";

    /** Pacote base do código de produção (Q-012). */
    static final String PRODUCTION_BASE = "io.github.victorleasilva.capo";

    /** AC-RNF-004.1, aplicada ao código de produção. */
    public static final ArchRule DOMAIN_IS_FRAMEWORK_FREE = domainIsFrameworkFree(PRODUCTION_BASE);

    /** AC-RNF-004.2, aplicada ao código de produção. */
    public static final ArchRule EXPERIMENT_ONLY_FROM_LAB_OR_EXPERIMENT =
            experimentOnlyFromLabOrExperiment(PRODUCTION_BASE);

    /** AC-RNF-004.3, aplicada ao código de produção. */
    public static final ArchRule MODULES_TALK_ONLY_THROUGH_API = modulesTalkOnlyThroughApi(PRODUCTION_BASE);

    /** AC-RNF-004.4, aplicada ao código de produção. */
    public static final ArchRule NO_FLOATING_POINT_MONEY = noFloatingPointMoney(PRODUCTION_BASE);

    private ArchitectureRules() {}

    /** AC-RNF-004.1: o domínio do estoque e do pedido não depende de framework. */
    public static ArchRule domainIsFrameworkFree(String base) {
        return noClasses()
                .that()
                .resideInAnyPackage(base + ".inventory.domain..", base + ".ordering.domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("org.springframework..", "jakarta.persistence..", "org.hibernate..")
                .allowEmptyShould(true)
                .as("o domínio de inventory e ordering não depende de Spring, JPA nem Hibernate");
    }

    /** AC-RNF-004.2: as versões ruins só são usadas pelo laboratório ou por outro pacote experiment. */
    public static ArchRule experimentOnlyFromLabOrExperiment(String base) {
        return classes()
                .that()
                .resideInAPackage(base + "..experiment..")
                .should()
                .onlyHaveDependentClassesThat()
                .resideInAnyPackage(base + "." + LAB + "..", base + "..experiment..")
                .allowEmptyShould(true)
                .as("pacotes experiment só são usados por lab ou por outro pacote experiment");
    }

    /**
     * AC-RNF-004.3: um módulo só usa outro pela API pública dele; o pacote experiment do outro só
     * pode ser usado pelo lab ou por um pacote experiment.
     */
    public static ArchRule modulesTalkOnlyThroughApi(String base) {
        List<ArchRule> rules = new ArrayList<>();
        for (String origin : MODULES) {
            for (String target : MODULES) {
                if (origin.equals(target)) {
                    continue;
                }
                String targetRoot = base + "." + target + "..";
                String targetApi = base + "." + target + ".api..";
                String targetExperiment = base + "." + target + ".experiment..";

                // Classes comuns do módulo de origem: só a API do outro módulo.
                // O lab também pode usar o pacote experiment dos outros.
                String[] allowedForOrigin =
                        LAB.equals(origin) ? new String[] {targetApi, targetExperiment} : new String[] {targetApi};
                rules.add(noClasses()
                        .that()
                        .resideInAPackage(base + "." + origin + "..")
                        .and()
                        .resideOutsideOfPackage(base + "." + origin + ".experiment..")
                        .should()
                        .dependOnClassesThat(
                                resideInAPackage(targetRoot).and(not(resideInAnyPackage(allowedForOrigin))))
                        .allowEmptyShould(true));

                // O pacote experiment do módulo de origem: API ou experiment do outro módulo.
                rules.add(noClasses()
                        .that()
                        .resideInAPackage(base + "." + origin + ".experiment..")
                        .should()
                        .dependOnClassesThat(
                                resideInAPackage(targetRoot).and(not(resideInAnyPackage(targetApi, targetExperiment))))
                        .allowEmptyShould(true));
            }
        }
        return CompositeArchRule.of(rules)
                .as("módulos só se comunicam pela API pública (ou pelo experiment, a partir de lab ou experiment)");
    }

    /** AC-RNF-004.4: nenhum campo de dinheiro em ponto flutuante. */
    public static ArchRule noFloatingPointMoney(String base) {
        return noFields()
                .that()
                .areDeclaredInClassesThat()
                .resideInAPackage(base + "..")
                .and()
                .haveNameMatching("(?i).*(price|amount|total|cost).*")
                .should()
                .haveRawType(double.class)
                .orShould()
                .haveRawType(float.class)
                .orShould()
                .haveRawType(Double.class)
                .orShould()
                .haveRawType(Float.class)
                .allowEmptyShould(true)
                .as("nenhum campo de dinheiro (price, amount, total, cost) é double ou float");
    }

    /** Todas as regras, na ordem dos critérios de aceite. */
    public static List<ArchRule> all(String base) {
        return List.of(
                domainIsFrameworkFree(base),
                experimentOnlyFromLabOrExperiment(base),
                modulesTalkOnlyThroughApi(base),
                noFloatingPointMoney(base));
    }
}
