# ADR-017 · Execuções preservadas nas recargas

- **Status:** aceito
- **Data:** 2026-11-07
- **Origem:** D-17 em 03

## Contexto
As execuções registradas em `lab_run` são a prova das medições publicadas: um post cita o `runId`, a data e o ambiente (P7, RNF-011). Trocar o tamanho da carga (`LAB_SEED_PRODUCTS`) recria o catálogo, o estoque e os pedidos. Uma chave estrangeira de `customer_order` para `lab_run` obrigaria a apagar as execuções junto.

## Decisão
`customer_order` guarda `lab_run_id` e `lab_variant` como valores simples, **sem** chave estrangeira para `lab_run`. A recarga apaga o catálogo, o estoque e os pedidos e preserva `lab_run` (Q-011).

## Alternativas rejeitadas
| Alternativa | Por que não |
|---|---|
| Apagar as execuções na recarga | Destruiria a prova das medições publicadas |
| Recusar subir com outro tamanho de carga | Pouco prático para experimentar tamanhos diferentes |

## Consequências
- Positivas: as provas sobrevivem; sem a chave estrangeira, o módulo `ordering` não depende do `lab`.
- Negativas e mitigação: um pedido pode citar uma execução sem que o banco garanta a ligação; aceitável, porque só o laboratório grava esses campos.
- Como se prova: AC-001.6 (T-010).
