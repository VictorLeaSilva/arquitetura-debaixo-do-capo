# Matriz de versões

Fonte única das versões do projeto (P10, DP-05). Toda página de estudo cita estas versões na caixa "Testado com"; quando uma versão muda, esta tabela muda no mesmo PR.

| Componente | Versão | Onde está fixada | Fonte oficial | Conferida em |
|---|---|---|---|---|
| Java (JDK de build) | 25 (máquina de referência: 25.0.5) | `backend/pom.xml`: `java.version` e a regra `requireJavaVersion` `[25,)` do enforcer | `java -version` na máquina de referência (A2) | 2026-11-07 |
| Spring Boot | 4.1.2 | `backend/pom.xml`: `spring-boot-starter-parent` | [Releases do Spring Boot no GitHub](https://github.com/spring-projects/spring-boot/releases) (marcada "Latest") | 2026-11-07 |
| Apache Maven | 3.10.0 | `backend/.mvn/wrapper/maven-wrapper.properties`: `distributionUrl` | [Maven Releases History](https://maven.apache.org/docs/history.html) | 2026-11-07 |
| Maven Wrapper | 3.3.4 | `backend/.mvn/wrapper/maven-wrapper.properties`: `wrapperVersion` | [Releases do Maven Wrapper no GitHub](https://github.com/apache/maven-wrapper/releases) (marcada "Latest") | 2026-11-07 |
| maven-enforcer-plugin | 3.7.0 | `backend/pom.xml`: plugin `maven-enforcer-plugin` | [Download do Maven Enforcer](https://maven.apache.org/enforcer/download.html) ("current stable version") | 2026-11-07 |

## Próximas versões a acompanhar
- **Spring Boot 4.2.0:** em 2026-11-07 estava em *release candidate* (4.2.0-RC2); a atualização entra por uma tarefa própria, depois do lançamento da versão final.
