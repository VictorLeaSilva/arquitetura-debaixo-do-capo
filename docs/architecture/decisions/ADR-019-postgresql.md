# ADR-019 · PostgreSQL como banco principal

- **Status:** aceito
- **Data:** 2026-11-07
- **Origem:** 00 §3 (Persistência)

## Contexto
O MVP precisa de transações confiáveis (a flash sale), de recursos para os experimentos e de um banco comum em produção. MongoDB e Cassandra entram só na fatia 6, para comparar modelos de consistência.

## Decisão
PostgreSQL como banco principal (18.6 na T-004).

## Alternativas rejeitadas
| Alternativa | Por que não |
|---|---|
| MySQL | O laboratório usa recursos do PostgreSQL: *advisory lock* (ADR-004), `jsonb` (`lab_run`) e índice parcial |

## Consequências
- Positivas: um banco só no MVP, com tudo o que os experimentos precisam.
- Negativas e mitigação: parte do SQL fica específica do PostgreSQL; aceitável, porque o objetivo não é portabilidade entre bancos.
- Como se prova: os testes de integração com PostgreSQL real (Testcontainers, a partir da T-010).
