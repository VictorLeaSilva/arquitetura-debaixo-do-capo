# ADR-008 · Flash sale: UPDATE condicional × versão ingênua

- **Status:** aceito
- **Data:** 2026-11-07
- **Origem:** D-08 em 03

## Contexto
Numa *flash sale*, muitos compradores disputam ao mesmo tempo poucas unidades. Uma **condição de corrida** acontece quando o resultado depende da ordem das operações simultâneas: dois compradores leem "1 unidade" antes de qualquer um gravar a compra, e os dois compram. O experimento precisa mostrar o erro e a correção mais simples.

## Decisão
- **Versão correta:** `UPDATE flash_sale_stock SET available = available - 1 WHERE product_id = ? AND available >= 1`, na mesma transação que grava o pedido. O banco decide de forma atômica: se a linha não foi atualizada, acabou o estoque.
- **Versão ingênua:** ler numa transação curta, esperar a janela de corrida (ADR-024) **sem segurar conexão** e gravar o valor calculado na aplicação em outra transação.
- Os compradores são simulados no mesmo processo, com *virtual threads* (threads leves do Java 21+) liberadas juntas por um *latch* (um ponto de partida comum).

## Alternativas rejeitadas
| Alternativa | Por que não |
|---|---|
| Carga HTTP com k6 | Fica para a fatia 4 (escala horizontal); no MVP, a disputa no mesmo processo é suficiente e mais simples de medir |
| Trava pessimista ou otimista | Ficam para a fatia 3 completa, comparadas com esta |
| Ler e esperar dentro da mesma transação | Prende uma conexão por comprador durante a espera e esgota o pool (achado da revisão da spec, SPEC-REVIEW-001 #1) |

## Consequências
- Positivas: a correção tem uma linha de SQL; a versão ingênua reproduz o padrão errado mais comum.
- Negativas e mitigação: a concorrência real fica limitada pelo tamanho do pool de conexões; ele é registrado em cada execução (`environment.dbPoolSize`).
- Como se prova: AC-007.2 e AC-007.3, com 200 compradores simultâneos, PostgreSQL real e 20 repetições (T-020, T-021).
