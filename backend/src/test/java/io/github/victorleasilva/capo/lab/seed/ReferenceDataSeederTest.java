package io.github.victorleasilva.capo.lab.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.github.victorleasilva.capo.catalog.api.SeedFailpoint;
import io.github.victorleasilva.capo.support.PostgresTestDatabase;
import io.github.victorleasilva.capo.support.SeedApplication;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.TimeGauge;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.slf4j.event.KeyValuePair;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * Carga dos dados de referência (RF-001, A1), com PostgreSQL real. Cada teste usa um banco novo; "reiniciar" é subir a
 * aplicação de novo no mesmo banco.
 */
@Tag("RF-001")
@ExtendWith(OutputCaptureExtension.class)
class ReferenceDataSeederTest {

    private static final String SIZE = "10000";

    @Test
    void loadsReferenceDataIntoEmptyDatabase() {
        String url = PostgresTestDatabase.createDatabase("ac0011");
        try (ConfigurableApplicationContext ignored = SeedApplication.start(url, SIZE)) {
            assertReferenceCounts(jdbc(url), 10_000);
        }
    }

    @Test
    void restartWithSameSizeSkipsLoad() {
        String url = PostgresTestDatabase.createDatabase("ac0012");
        SeedApplication.start(url, SIZE).close();
        JdbcClient db = jdbc(url);
        Map<String, Object> before = snapshot(db);

        try (ConfigurableApplicationContext context = SeedApplication.start(url, SIZE, SeedEventCapture.class)) {
            assertThat(snapshot(db)).isEqualTo(before);
            SeedEventCapture capture = context.getBean(SeedEventCapture.class);
            assertThat(capture.events("seed.completed")).isEmpty();
            assertThat(capture.events("seed.skipped"))
                    .singleElement()
                    .satisfies(event -> assertThat(value(event, "products")).isEqualTo("10000"));
        }
    }

    @Test
    void loadIsDeterministicAcrossDatabases() {
        String first = PostgresTestDatabase.createDatabase("ac0013a");
        String second = PostgresTestDatabase.createDatabase("ac0013b");
        SeedApplication.start(first, SIZE).close();
        SeedApplication.start(second, SIZE).close();

        Map<String, Object> product42 = product42(jdbc(first));
        assertThat(product42)
                .containsEntry("name", "Produto 42")
                .containsEntry("category", "Categoria 43")
                .containsEntry("created_at_ok", true)
                .containsEntry("attributes", "attr-1=valor-302;attr-2=valor-604;attr-3=valor-906");
        assertThat((BigDecimal) product42.get("price")).isEqualByComparingTo("365.98");
        assertThat(product42(jdbc(second))).isEqualTo(product42);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-5", "1000001", "abc"})
    void invalidSeedSizePreventsStartup(String raw, CapturedOutput output) {
        String url = PostgresTestDatabase.createDatabase("ac0014");

        assertThatThrownBy(() -> SeedApplication.start(url, raw)).isInstanceOf(RuntimeException.class);

        assertThat(output.getAll()).contains("LAB_SEED_PRODUCTS inválido: \"" + raw + "\"");
        assertThat(count(jdbc(url), "product")).isZero();
    }

    @Test
    void failureMidLoadCommitsNothingAndRestartCompletes() {
        String url = PostgresTestDatabase.createDatabase("ac0015");

        assertThatThrownBy(() -> SeedApplication.start(url, SIZE, FailingSeedFailpoint.class))
                .hasStackTraceContaining("falha de teste entre produtos e atributos");
        JdbcClient db = jdbc(url);
        assertThat(count(db, "product")).isZero();
        assertThat(count(db, "category")).isZero();

        SeedApplication.start(url, SIZE).close();
        assertReferenceCounts(db, 10_000);
    }

    @Test
    void resizeReloadsCatalogAndKeepsLabRuns() {
        String url = PostgresTestDatabase.createDatabase("ac0016");
        SeedApplication.start(url, SIZE).close();
        JdbcClient db = jdbc(url);
        db.sql("INSERT INTO lab_run (id, experiment_id, status, parameters, results, environment, trace_id,"
                        + " started_at, finished_at) VALUES"
                        + " ('00000000-0000-0000-0000-000000000001', 'n-plus-one', 'COMPLETED', '{\"a\": 1}',"
                        + " '{\"r\": 2}', '{\"seedProducts\": 10000}', 'trace1', TIMESTAMPTZ '2026-02-01T10:00:00Z',"
                        + " TIMESTAMPTZ '2026-02-01T10:00:05Z'),"
                        + " ('00000000-0000-0000-0000-000000000002', 'list-intersection', 'FAILED', '{}', NULL,"
                        + " '{\"seedProducts\": 10000}', 'trace2', TIMESTAMPTZ '2026-02-02T10:00:00Z', NULL)")
                .update();
        db.sql("INSERT INTO customer_order (id, product_id, quantity, lab_run_id, lab_variant, created_at) VALUES"
                        + " (gen_random_uuid(), 1, 1, '00000000-0000-0000-0000-000000000001', 'good', now()),"
                        + " (gen_random_uuid(), 2, 1, NULL, NULL, now())")
                .update();
        List<String> labRuns = labRuns(db);

        try (ConfigurableApplicationContext context = SeedApplication.start(url, "20000", SeedEventCapture.class)) {
            assertThat(count(db, "product")).isEqualTo(20_000);
            assertThat(db.sql("SELECT min(id) || '-' || max(id) FROM product")
                            .query(String.class)
                            .single())
                    .isEqualTo("1-20000");
            assertThat(count(db, "customer_order")).isZero();
            assertThat(labRuns(db)).hasSize(2).isEqualTo(labRuns);
            assertThat(context.getBean(SeedEventCapture.class).events("seed.completed"))
                    .singleElement()
                    .satisfies(event -> assertThat(value(event, "products")).isEqualTo("20000"));
        }
    }

    @Test
    void completedLoadLogsEventAndRecordsGauge() {
        String url = PostgresTestDatabase.createDatabase("t0102");
        try (ConfigurableApplicationContext context = SeedApplication.start(url, SIZE, SeedEventCapture.class)) {
            ILoggingEvent event = context.getBean(SeedEventCapture.class)
                    .events("seed.completed")
                    .getFirst();
            assertThat(value(event, "products")).isEqualTo("10000");
            assertThat(Long.parseLong(value(event, "duration_ms"))).isNotNegative();

            TimeGauge gauge = context.getBean(MeterRegistry.class)
                    .find("lab.seed.duration")
                    .timeGauge();
            assertThat(gauge).isNotNull();
            assertThat(gauge.value(TimeUnit.SECONDS)).isNotNegative();
        }
    }

    // --- apoio ---

    private static JdbcClient jdbc(String url) {
        return JdbcClient.create(
                new DriverManagerDataSource(url, PostgresTestDatabase.username(), PostgresTestDatabase.password()));
    }

    private static long count(JdbcClient db, String table) {
        return db.sql("SELECT count(*) FROM " + table).query(Long.class).single();
    }

    private static void assertReferenceCounts(JdbcClient db, long products) {
        assertThat(count(db, "product")).isEqualTo(products);
        assertThat(count(db, "category")).isEqualTo(50);
        Map<String, Object> perProduct = db.sql("SELECT count(*) AS products, min(c) AS min_attrs, max(c) AS max_attrs"
                        + " FROM (SELECT count(*) AS c FROM product_attribute GROUP BY product_id) AS a")
                .query()
                .singleRow();
        assertThat(((Number) perProduct.get("products")).longValue()).isEqualTo(products);
        assertThat(((Number) perProduct.get("min_attrs")).intValue()).isGreaterThanOrEqualTo(3);
        assertThat(((Number) perProduct.get("max_attrs")).intValue()).isLessThanOrEqualTo(8);
        assertThat(db.sql("SELECT product_id || ':' || available FROM flash_sale_stock")
                        .query(String.class)
                        .list())
                .containsExactly("1:10");
    }

    /** Contagens e o xmin de linhas de cada tabela: uma linha apagada e inserida de novo teria outro xmin. */
    private static Map<String, Object> snapshot(JdbcClient db) {
        return db.sql("SELECT (SELECT count(*) FROM product) AS products,"
                        + " (SELECT count(*) FROM product_attribute) AS attributes,"
                        + " (SELECT count(*) FROM category) AS categories,"
                        + " (SELECT xmin::text FROM product WHERE id = 1) AS product_xmin,"
                        + " (SELECT xmin::text FROM product_attribute WHERE id = 11) AS attribute_xmin,"
                        + " (SELECT xmin::text FROM category WHERE id = 1) AS category_xmin,"
                        + " (SELECT xmin::text FROM flash_sale_stock WHERE product_id = 1) AS stock_xmin")
                .query()
                .singleRow();
    }

    private static Map<String, Object> product42(JdbcClient db) {
        return db.sql("SELECT p.name, c.name AS category, p.price,"
                        + " p.created_at = TIMESTAMPTZ '2026-01-01T00:00:00Z' AS created_at_ok,"
                        + " (SELECT string_agg(a.name || '=' || a.value, ';' ORDER BY a.name)"
                        + " FROM product_attribute a WHERE a.product_id = p.id) AS attributes"
                        + " FROM product p JOIN category c ON c.id = p.category_id WHERE p.sku = 'SKU-0000042'")
                .query()
                .singleRow();
    }

    private static List<String> labRuns(JdbcClient db) {
        return db.sql("SELECT r::text FROM lab_run r ORDER BY id")
                .query(String.class)
                .list();
    }

    private static String value(ILoggingEvent event, String key) {
        List<KeyValuePair> pairs = event.getKeyValuePairs();
        return pairs == null
                ? null
                : pairs.stream()
                        .filter(pair -> pair.key.equals(key))
                        .map(pair -> String.valueOf(pair.value))
                        .findFirst()
                        .orElse(null);
    }

    /**
     * Captura os eventos de log do {@link ReferenceDataSeeder}. Sem {@code @Component}: só entra no contexto quando o
     * teste a passa como fonte (o component scan da aplicação também enxerga as classes de teste).
     */
    static final class SeedEventCapture {

        private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

        SeedEventCapture() {
            appender.start();
            ((Logger) LoggerFactory.getLogger(ReferenceDataSeeder.class)).addAppender(appender);
        }

        List<ILoggingEvent> events(String message) {
            return appender.list.stream()
                    .filter(event -> message.equals(event.getMessage()))
                    .toList();
        }
    }

    /** Ponto de falha que lança exceção entre produtos e atributos (AC-001.5). Sem anotação, pelo mesmo motivo. */
    static final class FailingSeedFailpoint {

        @Bean
        @Primary
        SeedFailpoint failingSeedFailpoint() {
            return () -> {
                throw new IllegalStateException("falha de teste entre produtos e atributos");
            };
        }
    }
}
