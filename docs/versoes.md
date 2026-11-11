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
| actions/setup-node | v7.1.0 (`949feb2413d6458794dcd2491c4babbbce0c15c1`) | `.github/workflows/ci.yml` | [Releases do actions/setup-node](https://github.com/actions/setup-node/releases) (marcada "Latest") | 2026-11-10 |
| Node.js (site) | 26 (máquina de referência: 26.13.0, com npm 11.20.0) | `site/.nvmrc` e `engines.node` (`>=26`) em `site/package.json` | [Versões do Node.js](https://nodejs.org/en/about/previous-releases) (26 é a LTS "Lithium"; 26.13.0 de 2026-10-28) | 2026-11-10 |
| Docusaurus | 3.10.2 (`core`, `preset-classic`, `theme-mermaid`, `tsconfig`, `types`, `module-type-aliases`) | `site/package.json` | [Releases do Docusaurus](https://github.com/facebook/docusaurus/releases) e `latest` no registro do npm (exige Node `>=20.0`) | 2026-11-10 |
| React | 19.3.1 (`react`, `react-dom`) e `@mdx-js/react` 3.1.1 | `site/package.json` | [Versões do React](https://react.dev/versions) e `latest` no registro do npm | 2026-11-10 |
| TypeScript (site) | 5.9.3 | `site/package.json` | [Releases do TypeScript](https://github.com/microsoft/TypeScript/releases) (ver as particularidades abaixo; Q-033) | 2026-11-11 |
| ESLint | 10.14.0 e `@eslint/js` 10.0.1 | `site/package.json` | [Releases do ESLint](https://github.com/eslint/eslint/releases) | 2026-11-10 |
| typescript-eslint | 8.72.2 | `site/package.json` | [Releases do typescript-eslint](https://github.com/typescript-eslint/typescript-eslint/releases) (aceita TypeScript `>=4.8.4 <6.1.0`) | 2026-11-10 |
| Prettier | 3.9.10 | `site/package.json` | [Changelog do Prettier](https://github.com/prettier/prettier/blob/main/CHANGELOG.md) | 2026-11-10 |
| PostgreSQL (imagem Docker) | 18.6 (`postgres:18.6-trixie`) | `compose.yaml`: serviço `postgres` | [Política de versões do PostgreSQL](https://www.postgresql.org/support/versioning/) e [imagem oficial no Docker Hub](https://hub.docker.com/_/postgres) | 2026-11-09 |
| Observabilidade (imagem Docker) | 0.40.0 (`grafana/otel-lgtm:0.40.0`) | `compose.yaml`: serviço `observability` | [Releases do docker-otel-lgtm](https://github.com/grafana/docker-otel-lgtm/releases) (marcada "Latest") | 2026-11-09 |
| Apache Cassandra (imagem Docker, spike H-05) | 5.0.9 (`cassandra:5.0.9-trixie`) | `infra/spike-h05/compose.cassandra.yaml` | [Download do Apache Cassandra](https://cassandra.apache.org/_/download.html) (5.0.9 é a GA mais recente; a 6.0 está em alpha) e [imagem oficial](https://hub.docker.com/_/cassandra) | 2026-11-09 |
| MongoDB (imagem Docker, spike H-05) | 9.0.4 (`mongo:9.0.4`) | `infra/spike-h05/compose.mongo.yaml` | [Notas de versão do MongoDB](https://www.mongodb.com/docs/manual/release-notes/) (série estável 9.0; 9.0.4 de 2026-10-13) e [documentação da imagem oficial](https://github.com/docker-library/docs/blob/master/mongo/README.md) | 2026-11-09 |

## Particularidades de versão já registradas
- **PostgreSQL 18:** a imagem oficial passou a guardar os dados em `/var/lib/postgresql/18/docker`, e o volume é montado em `/var/lib/postgresql`. Até a 17, o ponto de montagem era `/var/lib/postgresql/data`; um `compose.yaml` feito para a 17 e usado com a 18 perde os dados a cada recriação do container ([imagem oficial no Docker Hub](https://hub.docker.com/_/postgres)).
- **Cassandra 5:** o coletor de memória padrão é o G1 (`-XX:+UseG1GC`), e com ele só o `MAX_HEAP_SIZE` vale; o `HEAP_NEWSIZE` dos tutoriais antigos era do coletor CMS e agora é ignorado, com aviso (`cassandra-env.sh` da imagem; Q-027).
- **TypeScript no site (Q-033):** o `@docusaurus/tsconfig` 3.10.2 define `baseUrl`, e o TypeScript 6 reprova essa opção (`error TS5101: Option 'baseUrl' is deprecated and will stop functioning in TypeScript 7.0`); o TypeScript 7 não é aceito pelo `typescript-eslint` 8.72.2. O site fica no 5.9.3 até o Docusaurus deixar de usar o `baseUrl`.
- **Mermaid no Docusaurus 3.10.2 (Q-034):** o `@docusaurus/theme-mermaid` importa o pacote opcional `@mermaid-js/layout-elk`, e o webpack tenta resolvê-lo mesmo sem ele instalado (`Can't resolve '@mermaid-js/layout-elk'`). Um plugin local no `site/docusaurus.config.ts` o troca por um módulo vazio. Instalar o pacote não resolve: a faixa `^0.1.9` que o tema declara exige o Mermaid 11, e o npm instala o Mermaid 12.
- **Dependências internas do Docusaurus 3.10.2 (Q-035):** o `@docusaurus/bundler` traz `tinypool` 1.1.1 (falha crítica, GHSA-5gmw-xhrv-c9v3) e `serialize-javascript` 6.0.2 (falha grave, GHSA-5c6j-r48x-rmvq). O `site/package.json` força as versões corrigidas por `overrides` (`tinypool` 2.2.0 e `serialize-javascript` 7.1.4); retirar quando o Docusaurus atualizar. Restam alertas moderados e baixos, abaixo do limite do `dependency-review` (*high*).
- **Node 26:** o build do Docusaurus imprime `ExperimentalWarning: localStorage is not available because --localstorage-file was not provided.` É o `localStorage` embutido no Node, que o build não usa; não é erro.

## Próximas versões a acompanhar
- **Spring Boot 4.2.0:** em 2026-11-07 estava em *release candidate* (4.2.0-RC2, marcada "Pre-release" nas [releases do Spring Boot no GitHub](https://github.com/spring-projects/spring-boot/releases)); a atualização entra por uma tarefa própria, depois do lançamento da versão final.
- **PostgreSQL 19:** em 2026-11-09 estava em RC1 ("PostgreSQL 19 RC 1 Released!", postgresql.org); a imagem `postgres:19` ainda não tinha tag GA.
