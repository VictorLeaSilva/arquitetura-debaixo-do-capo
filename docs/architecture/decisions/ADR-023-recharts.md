# ADR-023 · Recharts nos gráficos

- **Status:** aceito
- **Data:** 2026-11-07
- **Origem:** Q-005

## Contexto
A tela do laboratório desenha a curva de crescimento: o tempo de cada versão conforme o tamanho da entrada. Com **escala logarítmica**, em que cada passo do eixo multiplica o valor em vez de somar, as curvas O(n) e O(n²) ficam visivelmente diferentes.

## Decisão
Recharts.

## Alternativas rejeitadas
| Alternativa | Por que não |
|---|---|
| Chart.js (com react-chartjs-2) | Desenha num `<canvas>`, o que dificulta testar com o Testing Library |
| SVG feito à mão | Exige escrever eixos, escala e legenda: mais código e mais testes, sem ganho didático para o tema |

## Consequências
- Positivas: componentes React declarativos, com eixos, legenda e escala logarítmica prontos.
- Negativas e mitigação: uma dependência do front-end; a versão fica fixada na T-016.
- Como se prova: AC-008.4 (T-016).
