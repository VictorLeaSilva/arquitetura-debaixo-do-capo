# ADR-012 · Erros em Problem Details (RFC 9457)

- **Status:** aceito
- **Data:** 2026-11-07
- **Origem:** D-12 em 03

## Contexto
A tela genérica precisa entender os erros da API sem conhecer cada um. A **RFC 9457**, *Problem Details for HTTP APIs*, é o padrão da IETF para o corpo de erro: um JSON com `type`, `title`, `status`, `detail` e campos extras, servido como `application/problem+json`.

## Decisão
Todos os erros em Problem Details, com `type` relativo no formato `/problems/<código>` e, nos erros de validação, a lista `errors` com `{field, message}`.

## Alternativas rejeitadas
| Alternativa | Por que não |
|---|---|
| Um formato próprio de erro | Reinventa um padrão que o Spring já implementa (`ProblemDetail`) e que qualquer cliente HTTP reconhece |

## Consequências
- Positivas: a tela mostra o `detail` e o campo citado para qualquer erro; os testes conferem o `type`.
- Negativas e mitigação: os `type` relativos não são URLs navegáveis; isso é aceitável num laboratório local.
- Como se prova: os testes de erro de RF-003 e RF-004 (T-012).
