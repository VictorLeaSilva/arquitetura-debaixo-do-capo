# ADR-014 · Migrações com Flyway

- **Status:** aceito
- **Data:** 2026-11-07
- **Origem:** D-14 em 03

## Contexto
O esquema do banco muda ao longo das fatias. Uma **migração** é um script versionado que leva o banco de uma versão do esquema para a próxima, aplicado na ordem e uma vez só.

## Decisão
Flyway, com arquivos SQL versionados (`V1__...sql`).

## Alternativas rejeitadas
| Alternativa | Por que não |
|---|---|
| Liquibase | Mais poderoso (XML ou YAML, *rollback*), mas acrescenta uma linguagem a aprender sem ganho no MVP |

## Consequências
- Positivas: SQL puro, o mais fácil de ler e de explicar; o Spring Boot integra o Flyway sem configuração.
- Negativas e mitigação: sem *rollback* automático; a correção de uma migração errada é uma migração nova.
- Como se prova: as migrações aplicadas pelos testes de integração com PostgreSQL real (T-010 em diante).
