# ADR-004 · Uma execução por vez com advisory lock

- **Status:** aceito
- **Data:** 2026-11-07
- **Origem:** D-04 em 03

## Contexto
Duas medições rodando ao mesmo tempo disputam CPU, memória e banco, e uma contamina a outra: os números deixam de comparar as versões de forma justa (P3). Um *advisory lock* é uma trava do PostgreSQL identificada por um número escolhido pela aplicação, sem relação com nenhuma tabela.

## Decisão
Antes de executar, a aplicação tenta `pg_try_advisory_lock(<chave fixa do laboratório>)`. Se outra execução segura a trava, a resposta é `409 run-in-progress`. A trava é liberada sempre no fim, inclusive em falha.

## Alternativas rejeitadas
| Alternativa | Por que não |
|---|---|
| Uma flag em memória na aplicação | Quebra assim que houver duas instâncias (fatia 4): cada uma teria a própria flag |
| Uma fila de execuções | Complexidade sem necessidade; recusar com `409` é suficiente para um laboratório |

## Consequências
- Positivas: a trava continua valendo com várias instâncias; não há tabela nem estado extra.
- Negativas e mitigação: uma trava esquecida bloquearia o laboratório; o `finally` libera sempre, e a trava some se a conexão cair.
- Como se prova: AC-003.8 e AC-003.9, com os experimentos de teste `test-blocking` e `test-failing` (T-012).
