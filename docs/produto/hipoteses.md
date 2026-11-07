# Hipóteses

Cada hipótese tem um teste barato e um critério de sucesso definido **antes** do teste.

| ID | Tipo | Hipótese ("acreditamos que…") | Teste | Critério de sucesso | Risco | Estado |
|---|---|---|---|---|---|---|
| H-01 | viabilidade | Uma fatia completa (especificação, planejamento, código, testes, revisão, documentação no padrão DP-05 e post) custa, em média, até 5 semanas | Executar o MVP registrando o custo por tipo de trabalho, com a flash sale à parte (DP-16) | MVP concluído em até 10 semanas e, projetando o custo da flash sale para uma fatia cara completa, a média continua em até 5 semanas (DP-17) | alto | a testar |
| H-02 | valor | Construir e medir as versões ruim, média e boa de um conceito ensina mais do que só ler sobre ele | — | — | médio | aceita como premissa (DP-20) |
| H-04 | uso | Um leitor com Java básico consegue rodar um experimento só seguindo a documentação, no bash ou no PowerShell | Pedir a 3 pessoas, com ambientes diferentes (Windows e Linux/macOS), que sigam a página da fatia 0 sem ajuda | Pelo menos 2 de 3 rodam o experimento sem fazer nenhuma pergunta | médio | a testar |
| H-05 | viabilidade | O cenário de dados distribuídos mais pesado previsto roda numa máquina com 16 GB, subindo só o necessário de cada vez | Nas 2 primeiras semanas, subir o cenário mais pesado e medir a memória (DP-16, T-007) | Sobe e responde usando no máximo a memória reservada ao Docker (~7,6 GB) | médio | a testar |

**Mais arriscada:** H-01. Se o custo de uma fatia ficar muito acima do previsto, o roteiro precisa ser revisto antes de seguir.
