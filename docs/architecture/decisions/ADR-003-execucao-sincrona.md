# ADR-003 · Execução síncrona dos experimentos

- **Status:** aceito
- **Data:** 2026-11-07
- **Origem:** D-03 em 03

## Contexto
Um experimento roda as versões ruim, média e boa e devolve as medições. Há duas formas de expor isso numa API: **síncrona** (a requisição espera até o fim e já volta com o resultado) ou **assíncrona** (a API responde logo `202 Accepted` e o cliente consulta o resultado depois).

## Decisão
Execução síncrona e no mesmo processo: o `POST /api/v1/lab/experiments/{id}/runs` só responde quando o experimento termina, com `201` e o resultado completo.

## Alternativas rejeitadas
| Alternativa | Por que não |
|---|---|
| `202 Accepted` e consulta periódica | Mais estado (fila, status, consultas) e mais código, sem nenhuma execução longa no MVP que justifique |

## Consequências
- Positivas: simples de entender, de testar e de mostrar; o resultado chega numa resposta só.
- Negativas e mitigação: uma execução longa prende a requisição; os limites de parâmetros nos descritores e o tempo máximo de 120 s na tela (AC-008.8) mantêm as execuções em segundos.
- Como se prova: os testes de RF-003 (T-012).
