# ADR-027 · SpotBugs na análise estática

- **Status:** aceito
- **Data:** 2026-11-07
- **Origem:** Q-015

## Contexto
A **análise estática** procura bugs comuns sem executar o código, como comparar textos com `==` em Java. Ela entra no build e reprova o PR. As duas opções mais usadas trabalham de jeitos diferentes: uma lê o *bytecode* depois da compilação, a outra roda dentro do próprio compilador.

## Decisão
SpotBugs 4.10.5 (plugin 4.10.5.0), que analisa o bytecode depois da compilação, sem mexer no compilador. Ele roda com o JDK 25 desde a 4.9.7 (changelog oficial, conferido em 2026-11-07).

## Alternativas rejeitadas
| Alternativa | Por que não |
|---|---|
| Error Prone | Exige flags especiais do `javac` e não declarava suporte ao JDK 25 em 2026-11-07 |

## Consequências
- Positivas: um plugin Maven comum, fácil de explicar; prioridade média ou alta reprova o build.
- Negativas e mitigação: as versões ruins dos experimentos podem disparar achados de propósito; a supressão vale só para o pacote `experiment`, com motivo, tarefa e validade (`04 §5`).
- Como se prova: AC-T002.2 (`ES_COMPARING_STRINGS_WITH_EQ` reprova o build).
