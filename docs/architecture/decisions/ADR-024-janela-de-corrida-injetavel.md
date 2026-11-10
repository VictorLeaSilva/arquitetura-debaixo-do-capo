# ADR-024 · Janela de corrida injetável na versão ingênua

- **Status:** aceito
- **Data:** 2026-11-07
- **Origem:** Q-006

## Contexto
A versão ingênua da flash sale (ADR-008) só vende acima do estoque se dois compradores lerem o estoque antes de qualquer um gravar. A **janela de corrida** é esse intervalo entre ler e gravar. Sem controle sobre ela, o erro aparece às vezes e às vezes não: o teste ficaria instável, e a demonstração dependeria da sorte.

## Decisão
Uma porta `RaceWindow` com duas implementações: no teste, uma barreira que só libera as gravações depois que todos os compradores leram (o resultado fica garantido); no laboratório, uma pausa de `raceWindowMs` ∈ {0, 1, 5} (padrão 1) que alarga a janela, explicada na página de estudo.

## Alternativas rejeitadas
| Alternativa | Por que não |
|---|---|
| Só a barreira no teste; no laboratório, a corrida natural | O laboratório às vezes não mostraria o erro |
| Nada artificial: depender do volume de compradores | Até o teste ficaria instável |

## Consequências
- Positivas: o teste é determinístico e a demonstração é estável.
- Negativas e mitigação: é o único ponto do código de produção com pausa proposital; ele fica restrito à versão ruim e é explicado: em sistemas reais, a janela existe naturalmente (rede, GC, uma consulta lenta), e o parâmetro só a torna visível.
- Como se prova: AC-007.2 (20 de 20 com venda acima do estoque) e AC-007.7 (T-021).
