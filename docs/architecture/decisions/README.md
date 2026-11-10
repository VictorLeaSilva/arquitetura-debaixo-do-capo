# Registros de decisões de arquitetura (ADRs)

Um **ADR** (*Architecture Decision Record*) é um documento curto que registra uma decisão de arquitetura: o contexto que obrigou a decidir, o que foi escolhido, as **alternativas rejeitadas** com o motivo e as consequências. Registrar o que foi rejeitado é tão importante quanto registrar o que foi escolhido: é o que responde, meses depois, a "por que não fizemos do outro jeito?".

Cada decisão `D-nn` do design (`docs/specs/03-design-contracts.md`) tem exatamente um ADR; as escolhas de stack da constituição e as perguntas de arquitetura decididas pelo autor também têm. O `scripts/adr-lint.sh` confere isso no CI (RNF-014).

| ADR | Título | Origem | Status |
|---|---|---|---|
| [ADR-001](ADR-001-monolito-modular.md) | Monólito modular num único processo | D-01 | aceito |
| [ADR-002](ADR-002-profundidade-por-modulo.md) | Profundidade da arquitetura por módulo | D-02 | aceito |
| [ADR-003](ADR-003-execucao-sincrona.md) | Execução síncrona dos experimentos | D-03 | aceito |
| [ADR-004](ADR-004-uma-execucao-por-vez.md) | Uma execução por vez com advisory lock | D-04 | aceito |
| [ADR-005](ADR-005-descritores-json.md) | Experimentos descritos por JSON e tela genérica | D-05 | aceito |
| [ADR-006](ADR-006-metodo-de-medicao.md) | Método de medição | D-06 | aceito |
| [ADR-007](ADR-007-contagem-de-sql.md) | Contagem de SQL pelas estatísticas do Hibernate | D-07 | aceito |
| [ADR-008](ADR-008-flash-sale-update-condicional.md) | Flash sale: UPDATE condicional × versão ingênua | D-08 | aceito |
| [ADR-009](ADR-009-dados-de-referencia-sql.md) | Dados de referência por SQL determinístico | D-09 | aceito |
| [ADR-010](ADR-010-observabilidade-container-unico.md) | Observabilidade num container único | D-10 | aceito |
| [ADR-011](ADR-011-aplicacao-em-containers.md) | Aplicação e front-end em containers | D-11 | aceito |
| [ADR-012](ADR-012-problem-details.md) | Erros em Problem Details (RFC 9457) | D-12 | aceito |
| [ADR-013](ADR-013-sem-autenticacao-localhost.md) | Sem autenticação; portas só em 127.0.0.1 | D-13 | aceito |
| [ADR-014](ADR-014-flyway.md) | Migrações com Flyway | D-14 | aceito |
| [ADR-015](ADR-015-sql-como-spans.md) | Consultas SQL como spans no trace | D-15 | aceito |
| [ADR-016](ADR-016-logs-json-e-otlp.md) | Logs em JSON e por OTLP | D-16 | aceito |
| [ADR-017](ADR-017-execucoes-preservadas.md) | Execuções preservadas nas recargas | D-17 | aceito |
| [ADR-018](ADR-018-java-25-spring-boot-4.md) | Java 25 e Spring Boot 4.1 | 00 §3 | aceito |
| [ADR-019](ADR-019-postgresql.md) | PostgreSQL como banco principal | 00 §3 | aceito |
| [ADR-020](ADR-020-react-e-vite.md) | React e Vite no front-end | 00 §3 | aceito |
| [ADR-021](ADR-021-maven-com-wrapper.md) | Maven com wrapper | Q-004 | aceito |
| [ADR-022](ADR-022-docusaurus.md) | Docusaurus no site de documentação | Q-003 | aceito |
| [ADR-023](ADR-023-recharts.md) | Recharts nos gráficos | Q-005 | aceito |
| [ADR-024](ADR-024-janela-de-corrida-injetavel.md) | Janela de corrida injetável na versão ingênua | Q-006 | aceito |
| [ADR-025](ADR-025-archunit.md) | Só ArchUnit para as fronteiras no MVP | Q-007 | aceito |
| [ADR-026](ADR-026-telemetria-nativa-spring-boot.md) | Telemetria pelo suporte nativo do Spring Boot | Q-008 | aceito |
| [ADR-027](ADR-027-spotbugs.md) | SpotBugs na análise estática | Q-015 | aceito |

O modelo para um ADR novo está em [`ADR-000-modelo.md`](ADR-000-modelo.md).

**Um ADR aceito não é reescrito.** Se uma decisão mudar, um ADR novo registra a mudança e o motivo, e o antigo passa a `Status: substituído por ADR-nnn`. Assim, o histórico mostra não só o que vale hoje, mas o caminho até aqui.
