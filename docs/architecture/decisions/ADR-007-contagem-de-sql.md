# ADR-007 · Contagem de SQL pelas estatísticas do Hibernate

- **Status:** aceito
- **Data:** 2026-11-07
- **Origem:** D-07 em 03

## Contexto
O experimento N+1 precisa do número **exato** de comandos SQL de cada versão (51, 3 e 1, para 50 produtos). O Hibernate, a biblioteca que faz o mapeamento entre objetos Java e tabelas, mantém estatísticas internas, entre elas quantos comandos preparou.

## Decisão
Contar pelas estatísticas do Hibernate (`Statistics#getPrepareStatementCount`), zeradas antes de cada medição e lidas no fim.

## Alternativas rejeitadas
| Alternativa | Por que não |
|---|---|
| Contar os spans de SQL da telemetria (ADR-015) | Servem para **ver** as consultas no trace, mas o número do experimento não deve depender da exportação da telemetria |
| Contar linhas de log de SQL | Frágil: depende do formato e do nível do log |

## Consequências
- Positivas: número exato, sem dependência nova.
- Negativas e mitigação: as estatísticas são globais à aplicação; isso é seguro porque só há uma execução por vez (ADR-004).
- Como se prova: AC-006.1 a AC-006.3, com PostgreSQL real (T-014).
