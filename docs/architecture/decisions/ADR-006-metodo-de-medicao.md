# ADR-006 · Método de medição

- **Status:** aceito
- **Data:** 2026-11-07
- **Origem:** D-06 em 03

## Contexto
Medir tempo na JVM é traiçoeiro. As primeiras execuções são mais lentas, porque o compilador *JIT* (que transforma o bytecode em código de máquina enquanto o programa roda) ainda não otimizou o código. E uma medição só esconde o ruído do sistema. O princípio P7 exige que todo número tenha método.

## Decisão
Nos experimentos de lista e de N+1: 2 aquecimentos descartados e depois `repetitions` medidas, publicando a mediana, o mínimo e o máximo. O tempo vem de uma porta `Stopwatch` (implementada com `System.nanoTime`), que pode ser trocada por um relógio falso nos testes. A flash sale roda uma vez por versão, sem aquecimento.

## Alternativas rejeitadas
| Alternativa | Por que não |
|---|---|
| Média de uma única execução | Esconde o ruído e mede o código ainda não otimizado |
| JMH como único método | É o padrão para microbenchmarks, mas não roda dentro de uma API; fica como diferencial |

## Consequências
- Positivas: a dispersão (mínimo e máximo) fica visível; o cálculo é testável com relógio falso.
- Negativas e mitigação: na flash sale não há aquecimento, porque ele gravaria pedidos e gastaria estoque; as repetições são execuções separadas.
- Como se prova: AC-005.3, com um `Stopwatch` falso (T-013).
