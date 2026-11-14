package io.github.victorleasilva.capo.ordering.api;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Limpa os pedidos antes de uma recarga dos dados de referência (D-09), chamado pelo {@code lab}. */
@Component
public class OrderingSeed {

    private final JdbcClient jdbc;

    public OrderingSeed(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void clear() {
        jdbc.sql("DELETE FROM customer_order").update();
    }
}
