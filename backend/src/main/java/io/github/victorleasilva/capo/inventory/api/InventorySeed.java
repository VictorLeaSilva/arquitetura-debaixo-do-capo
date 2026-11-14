package io.github.victorleasilva.capo.inventory.api;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Limpa e carrega o estoque da flash sale com os dados de referência (D-09), chamado pelo {@code lab} dentro da
 * transação da carga. O domínio do estoque nasce na T-020; aqui é só SQL de carga.
 */
@Component
public class InventorySeed {

    private final JdbcClient jdbc;

    public InventorySeed(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void clear() {
        jdbc.sql("DELETE FROM flash_sale_stock").update();
    }

    /** O produto 1 com 10 unidades, iniciais e disponíveis. */
    public void load() {
        jdbc.sql("INSERT INTO flash_sale_stock (product_id, initial_quantity, available) VALUES (1, 10, 10)")
                .update();
    }
}
