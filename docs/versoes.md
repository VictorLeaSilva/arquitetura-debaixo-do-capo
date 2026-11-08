# Matriz de versões

Fonte única das versões do projeto (P10, DP-05). Toda página de estudo cita estas versões na caixa "Testado com"; quando uma versão muda, esta tabela muda no mesmo PR.

| Componente | Versão | Onde está fixada | Fonte oficial | Conferida em |
|---|---|---|---|---|
| Java (JDK de build) | 25 (máquina de referência: 25.0.5) | `backend/pom.xml`: `java.version` e a regra `requireJavaVersion` `[25,)` do enforcer | [JDK 25 no OpenJDK](https://openjdk.org/projects/jdk/25/) (versão LTS); a versão exata vem de `java -version` na máquina de referência (A2) | 2026-11-07 |
| Spring Boot | 4.1.2 | `backend/pom.xml`: `spring-boot-starter-parent` | [Releases do Spring Boot no GitHub](https://github.com/spring-projects/spring-boot/releases) (marcada "Latest") | 2026-11-07 |
| Apache Maven | 3.10.0 | `backend/.mvn/wrapper/maven-wrapper.properties`: `distributionUrl` | [Maven Releases History](https://maven.apache.org/docs/history.html) | 2026-11-07 |
| Maven Wrapper | 3.3.4 | `backend/.mvn/wrapper/maven-wrapper.properties`: `wrapperVersion` | [Releases do Maven Wrapper no GitHub](https://github.com/apache/maven-wrapper/releases) (marcada "Latest") | 2026-11-07 |
| maven-enforcer-plugin | 3.7.0 | `backend/pom.xml`: plugin `maven-enforcer-plugin` | [Download do Maven Enforcer](https://maven.apache.org/enforcer/download.html) ("current stable version") | 2026-11-07 |
| Spotless Maven Plugin | 3.11.0 | `backend/pom.xml`: plugin `spotless-maven-plugin` | [CHANGES do plugin Maven do Spotless](https://github.com/diffplug/spotless/blob/main/plugin-maven/CHANGES.md) (2026-11-02) | 2026-11-07 |
| Palantir Java Format | 2.102.0 | `backend/pom.xml`: `palantirJavaFormat` no Spotless | [Releases do palantir-java-format](https://github.com/palantir/palantir-java-format/releases) (marcada "Latest") | 2026-11-07 |
| SpotBugs (plugin Maven) | 4.10.5 (4.10.5.0) | `backend/pom.xml`: plugin `spotbugs-maven-plugin` | [CHANGELOG do SpotBugs](https://github.com/spotbugs/spotbugs/blob/master/CHANGELOG.md) (2026-10-11) e [releases do plugin](https://github.com/spotbugs/spotbugs-maven-plugin/releases) | 2026-11-07 |
| JaCoCo | 0.8.16 | `backend/pom.xml`: plugin `jacoco-maven-plugin` | [Change history do JaCoCo](https://www.jacoco.org/jacoco/trunk/doc/changes.html) (2026/10/24; Java 25 oficial desde a 0.8.14) | 2026-11-07 |
| ArchUnit | 1.5.1 | `backend/pom.xml`: `archunit-junit5` (test) | [Releases do ArchUnit](https://github.com/TNG/ArchUnit/releases) (marcada "Latest"; Java 25 desde a 1.4.1) | 2026-11-07 |
| actions/checkout | v7.0.1 (`3d3c42e5aac5ba805825da76410c181273ba90b1`) | `.github/workflows/ci.yml` | [Releases do actions/checkout](https://github.com/actions/checkout/releases) | 2026-11-07 |
| actions/setup-java | v6.2.0 (`77dd5b2ce225ea11469928ac317850245a74ae78`) | `.github/workflows/ci.yml` | [Releases do actions/setup-java](https://github.com/actions/setup-java/releases) | 2026-11-07 |
| actions/dependency-review-action | v5.0.0 (`a1d282b36b6f3519aa1f3fc636f609c47dddb294`) | `.github/workflows/ci.yml` | [Releases do dependency-review-action](https://github.com/actions/dependency-review-action/releases) | 2026-11-07 |
| gitleaks/gitleaks-action | v3.0.0 (`e0c47f4f8be36e29cdc102c57e68cb5cbf0e8d1e`) | `.github/workflows/ci.yml` | [Releases do gitleaks-action](https://github.com/gitleaks/gitleaks-action/releases) | 2026-11-07 |

## Próximas versões a acompanhar
- **Spring Boot 4.2.0:** em 2026-11-07 estava em *release candidate* (4.2.0-RC2, marcada "Pre-release" nas [releases do Spring Boot no GitHub](https://github.com/spring-projects/spring-boot/releases)); a atualização entra por uma tarefa própria, depois do lançamento da versão final.
