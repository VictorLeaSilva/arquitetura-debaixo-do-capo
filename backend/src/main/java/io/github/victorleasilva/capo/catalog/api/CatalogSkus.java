package io.github.victorleasilva.capo.catalog.api;

import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Os SKUs do catálogo para os experimentos do {@code lab} (A3, passo 1): SQL direto, sem portas (P2). */
@Component
public class CatalogSkus {

    private final JdbcClient jdbc;

    public CatalogSkus(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Os SKUs dos {@code n} primeiros produtos, por {@code id}. */
    public List<String> firstSkus(int n) {
        return jdbc.sql("SELECT sku FROM product ORDER BY id LIMIT :n")
                .param("n", n)
                .query(String.class)
                .list();
    }
}
