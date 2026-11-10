# ADR-009 · Dados de referência por SQL determinístico

- **Status:** aceito
- **Data:** 2026-11-07
- **Origem:** D-09 em 03

## Contexto
Os experimentos precisam de volume (até 1 milhão de produtos) e de dados **reprodutíveis**: a mesma carga em qualquer máquina, para os números serem comparáveis. *Determinístico* quer dizer que a mesma entrada gera sempre a mesma saída, sem sorteio.

## Decisão
Os dados são gerados por SQL (`generate_series` e fórmulas aritméticas com tipos explícitos, sem aleatoriedade), com a limpeza e a carga na mesma transação, quando o banco está vazio ou quando o tamanho configurado muda. Cada módulo carrega e limpa as próprias tabelas pela API dele, e o `lab` orquestra.

## Alternativas rejeitadas
| Alternativa | Por que não |
|---|---|
| Um CSV versionado | Centenas de megabytes no git |
| Um gerador em Java com dados aleatórios | Lento e não determinístico: cada carga seria diferente |
| Um script único que escreve em todas as tabelas | Um módulo mexeria nas tabelas dos outros, contra `01 §3` |

## Consequências
- Positivas: rápido, reprodutível e sem arquivo grande no repositório; uma carga que falha no meio não deixa dados pela metade.
- Negativas e mitigação: fórmulas aritméticas podem estourar tipos inteiros (achado da revisão da spec); a mitigação são os tipos explícitos (`bigint`, `numeric`) e um valor exato conferido em teste (AC-001.3).
- Como se prova: AC-001.1 a AC-001.6 (T-010) e o tempo da carga de 1 milhão (RNF-008).
