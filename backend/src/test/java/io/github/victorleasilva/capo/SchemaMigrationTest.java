package io.github.victorleasilva.capo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.victorleasilva.capo.support.PostgresTestDatabase;
import io.github.victorleasilva.capo.support.SeedApplication;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Esquema das seis tabelas de 03 §3, criado pelo Flyway (AC-T010.1). */
@Tag("RF-001")
class SchemaMigrationTest {

    private static ConfigurableApplicationContext context;
    private static JdbcClient jdbc;

    @BeforeAll
    static void migrateNewDatabase() {
        context = SeedApplication.start(PostgresTestDatabase.createDatabase("schema"), "10");
        jdbc = context.getBean(JdbcClient.class);
    }

    @AfterAll
    static void close() {
        context.close();
    }

    @Test
    void appliesTheFourMigrations() {
        List<String> versions = jdbc.sql(
                        "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank")
                .query(String.class)
                .list();
        assertThat(versions).containsExactly("1", "2", "3", "4");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "UPDATE product SET price = 0 WHERE id = 1",
                "UPDATE product SET sku = 'X' WHERE id = 1",
                "UPDATE flash_sale_stock SET available = -1 WHERE product_id = 1",
                "UPDATE flash_sale_stock SET initial_quantity = 101 WHERE product_id = 1",
                "INSERT INTO customer_order (id, product_id, quantity, lab_variant, created_at)"
                        + " VALUES (gen_random_uuid(), 1, 1, 'x', now())",
                "INSERT INTO lab_run (id, experiment_id, status, parameters, environment, trace_id, started_at)"
                        + " VALUES (gen_random_uuid(), 'e', 'X', '{}', '{}', 't', now())"
            })
    void constraintsRejectInvalidValues(String sql) {
        assertThatThrownBy(() -> jdbc.sql(sql).update()).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void orderAcceptsLabRunIdWithoutForeignKey() {
        int inserted = jdbc.sql(
                        "INSERT INTO customer_order (id, product_id, quantity, lab_run_id, lab_variant, created_at)"
                                + " VALUES (gen_random_uuid(), 1, 1, gen_random_uuid(), 'good', now())")
                .update();
        assertThat(inserted).isEqualTo(1);
    }
}
