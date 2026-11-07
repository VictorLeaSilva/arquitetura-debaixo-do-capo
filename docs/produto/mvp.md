# MVP

## Hipótese que o MVP testa
H-01: uma fatia completa (especificação, planejamento, código, testes, revisão, documentação no padrão DP-05 e post) custa, em média, até 5 semanas (DP-17). Para medir uma fatia barata e uma cara, o MVP junta a fundação, a fatia 1 reduzida e um experimento da flash sale (DP-16). De carona, testa também H-04 e H-05.

## Dentro
| # | Funcionalidade (o que o usuário consegue fazer) | Hipótese que apoia |
|---|---|---|
| F1 | Subir o ambiente local com poucos comandos e carregar um volume grande de dados (até 1 milhão de produtos) | H-01, H-04 |
| F2 | Rodar o experimento "buscar itens numa lista": versões ruim, média e boa, com tempo medido e a curva de crescimento | H-01 |
| F3 | Rodar o experimento "N+1": ver quantas consultas ao banco cada versão faz para a mesma tela | H-01 |
| F4 | Rodar o experimento "últimas unidades" da flash sale: ver a versão ingênua vender mais do que tem e a correta vender exatamente o estoque | H-01 |
| F5 | Ver a telemetria de cada experimento (tempo de cada etapa, consultas, logs ligados à mesma execução) | H-01 |
| F6 | Ler, para cada experimento, uma página de estudo no padrão de explicação (DP-05), com comandos em bash e PowerShell | H-04 |
| F7 | Publicar 2 posts com resultado medido, um por fatia do MVP | H-01 |

## Fora
- Experimento de paginação e o restante da fatia 1 (índices, cache, top-K): voltam na fatia 1 completa (DP-16).
- O restante da fatia 3 (níveis de lock, idempotência, rate limiting): volta na fatia 3 completa (DP-16).
- Fatia 2 e fatias 4 a 9: voltam na ordem do roteiro (DP-13).
- Login, progresso por pessoa e execução isolada por pessoa (DP-21).
- Qualquer uso de nuvem paga (DP-11).

## Métrica de sucesso
[meta] F1 a F7 concluídas em até 10 semanas (2 fatias × média de 5 semanas), com o custo de cada tipo de trabalho registrado (DP-17).

## Restrições
- Custo zero: só local (DP-11).
- Uma máquina com 16 GB de RAM, com cerca de 7,6 GB disponíveis para containers [fato: medido em 2026-11-07].
- Repositório público desde o início (DP-06).
- Código em inglês e documentação em português (DP-04), no padrão DP-05.
