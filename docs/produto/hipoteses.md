# Hipóteses

Cada hipótese tem um teste barato e um critério de sucesso definido **antes** do teste.

| ID | Tipo | Hipótese ("acreditamos que…") | Teste | Critério de sucesso | Risco | Estado |
|---|---|---|---|---|---|---|
| H-01 | viabilidade | Uma fatia completa (especificação, planejamento, código, testes, revisão, documentação no padrão DP-05 e post) custa, em média, até 5 semanas | Executar o MVP registrando o custo por tipo de trabalho, com a flash sale à parte (DP-16) | MVP concluído em até 10 semanas e, projetando o custo da flash sale para uma fatia cara completa, a média continua em até 5 semanas (DP-17) | alto | a testar |
| H-02 | valor | Construir e medir as versões ruim, média e boa de um conceito ensina mais do que só ler sobre ele | — | — | médio | aceita como premissa (DP-20) |
| H-04 | uso | Um leitor com Java básico consegue rodar um experimento só seguindo a documentação, no bash ou no PowerShell | Pedir a 3 pessoas, com ambientes diferentes (Windows e Linux/macOS), que sigam a página da fatia 0 sem ajuda | Pelo menos 2 de 3 rodam o experimento sem fazer nenhuma pergunta | médio | a testar |
| H-05 | viabilidade | O cenário de dados distribuídos mais pesado previsto roda numa máquina com 16 GB, subindo só o necessário de cada vez | Nas 2 primeiras semanas, subir o cenário mais pesado e medir a memória (DP-16, T-007) | Sobe e responde usando no máximo a memória reservada ao Docker (~7,6 GB) | médio | validada (2026-11-10, T-007): pior pico de cada cenário com a base do MVP no ar e carga de 10.000 inserções, em 3 repetições: Cassandra 3 nós com *heap* de 512 MB, 4.254,8 MiB; com 1 GB, 6.145,1 MiB; MongoDB 3 nós, 1.629,5 MiB; os dois juntos (512 MB), 4.996,9 MiB; todos abaixo do limite de 6.758,4 MiB (`infra/spike-h05/resultados/`) |

**Mais arriscada:** H-01. Se o custo de uma fatia ficar muito acima do previsto, o roteiro precisa ser revisto antes de seguir.

**O que a H-05 ensina para a fatia 6** (medido na T-007, `infra/spike-h05/`):
- Cada nó do Cassandra usa cerca de 1,1 a 1,2 GiB com um *heap* de 512 MB: o *heap* é só uma parte da memória de uma JVM. Com 1 GB de *heap*, o cenário fica em 91% do limite, sem folga para a aplicação crescer; **proposta:** usar 512 MB por nó na fatia 6.
- Os dois clusters juntos cabem (74% do limite), então um experimento de PACELC pode usar o Cassandra e o MongoDB ao mesmo tempo.
- A maior variação entre as repetições veio do container de observabilidade (de 563 a 1.060 MiB), e não dos bancos.
