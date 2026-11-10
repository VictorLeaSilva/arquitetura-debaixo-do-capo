# ADR-020 · React e Vite no front-end

- **Status:** aceito
- **Data:** 2026-11-07
- **Origem:** 00 §3 (Front-end)

## Contexto
O front-end é uma tela genérica que monta qualquer experimento a partir do descritor dele (ADR-005). **Vite** é a ferramenta que compila e serve o código do front-end durante o desenvolvimento.

## Decisão
React com TypeScript e Vite.

## Alternativas rejeitadas
| Alternativa | Por que não |
|---|---|
| Angular | React é o mais pedido em vagas e o mais documentado (`00 §3`) |
| Vue | Pelo mesmo motivo (`00 §3`) |

## Consequências
- Positivas: muito material de referência; o Vite dá um ciclo de desenvolvimento rápido.
- Negativas e mitigação: o ecossistema muda rápido; as versões ficam fixadas na tarefa que cria o front-end (T-016).
- Como se prova: os testes Vitest de RF-008 (T-016).
