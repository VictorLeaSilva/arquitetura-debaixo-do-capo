package io.github.victorleasilva.capo.catalog.api;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Limpa e carrega as tabelas do catálogo com os dados de referência (D-09), chamado pelo {@code lab} dentro da
 * transação da carga. A carga é infraestrutura, não regra de negócio: SQL puro, sem domínio nem portas (P2).
 *
 * <p>Os dados são gerados por fórmulas aritméticas sobre {@code generate_series}, sem aleatoriedade: o mesmo tamanho
 * produz os mesmos dados em qualquer banco (AC-001.3).
 */
@Component
public class CatalogSeed {

    private static final String INSERT_CATEGORIES = """
            INSERT INTO category (id, name)
            SELECT c, 'Categoria ' || lpad(c::text, 2, '0') FROM generate_series(1, 50) AS c
            """;

    private static final String INSERT_PRODUCTS = """
            INSERT INTO product (id, sku, name, category_id, price, created_at)
            SELECT i, 'SKU-' || lpad(i::text, 7, '0'), 'Produto ' || i, 1 + (i % 50),
                   10.00 + ((i * 7919) % 99000)::numeric / 100, TIMESTAMPTZ '2026-01-01T00:00:00Z'
            FROM generate_series(1::bigint, :n) AS i
            """;

    // O id do atributo é i * 10 + k: no máximo 8 atributos por produto, então não há colisão, e os ids também são
    // determinísticos.
    private static final String INSERT_ATTRIBUTES = """
            INSERT INTO product_attribute (id, product_id, name, value)
            SELECT i * 10 + k, i, 'attr-' || k, 'valor-' || ((i * k * 31) % 1000)
            FROM generate_series(1::bigint, :n) AS i
            CROSS JOIN LATERAL generate_series(1::bigint, 3 + (i % 6)) AS k
            """;

    private final JdbcClient jdbc;
    private final SeedFailpoint seedFailpoint;

    public CatalogSeed(JdbcClient jdbc, SeedFailpoint seedFailpoint) {
        this.jdbc = jdbc;
        this.seedFailpoint = seedFailpoint;
    }

    /**
     * Apaga atributos, produtos e categorias, nesta ordem. É DELETE, e não TRUNCATE: o PostgreSQL recusa TRUNCATE numa
     * tabela referenciada por chave estrangeira de outra tabela, e só o aceitaria mexendo nas tabelas de outro módulo.
     */
    public void clear() {
        jdbc.sql("DELETE FROM product_attribute").update();
        jdbc.sql("DELETE FROM product").update();
        jdbc.sql("DELETE FROM category").update();
    }

    /** Carrega as 50 categorias, os {@code products} produtos e os atributos de cada um. */
    public void load(int products) {
        long n = products;
        jdbc.sql(INSERT_CATEGORIES).update();
        jdbc.sql(INSERT_PRODUCTS).param("n", n).update();
        seedFailpoint.afterProducts();
        jdbc.sql(INSERT_ATTRIBUTES).param("n", n).update();
    }

    public long countProducts() {
        return jdbc.sql("SELECT count(*) FROM product").query(Long.class).single();
    }
}
