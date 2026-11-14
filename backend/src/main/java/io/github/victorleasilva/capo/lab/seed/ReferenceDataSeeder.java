package io.github.victorleasilva.capo.lab.seed;

import io.github.victorleasilva.capo.catalog.api.CatalogSeed;
import io.github.victorleasilva.capo.inventory.api.InventorySeed;
import io.github.victorleasilva.capo.ordering.api.OrderingSeed;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.TimeGauge;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Carga dos dados de referência na inicialização (RF-001, A1), orquestrada pelo {@code lab}: cada módulo limpa e
 * carrega as próprias tabelas pela API dele (D-09), tudo numa única transação. A tabela {@code lab_run} não é tocada
 * (D-17).
 *
 * <p>Roda como {@link ApplicationRunner}: o Spring Boot só marca a aplicação como pronta para receber tráfego
 * (readiness) depois que a carga termina.
 */
@Component
public class ReferenceDataSeeder implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(ReferenceDataSeeder.class);

    private final Environment environment;
    private final TransactionTemplate transaction;
    private final Clock clock;
    private final CatalogSeed catalogSeed;
    private final InventorySeed inventorySeed;
    private final OrderingSeed orderingSeed;
    private final AtomicLong lastDurationMs = new AtomicLong();

    public ReferenceDataSeeder(
            Environment environment,
            TransactionTemplate transaction,
            Clock clock,
            MeterRegistry meterRegistry,
            CatalogSeed catalogSeed,
            InventorySeed inventorySeed,
            OrderingSeed orderingSeed) {
        this.environment = environment;
        this.transaction = transaction;
        this.clock = clock;
        this.catalogSeed = catalogSeed;
        this.inventorySeed = inventorySeed;
        this.orderingSeed = orderingSeed;
        // Exposto pelo Prometheus como lab_seed_duration_seconds; vale 0 até a primeira carga (RNF-008).
        TimeGauge.builder("lab.seed.duration", lastDurationMs, TimeUnit.MILLISECONDS, AtomicLong::doubleValue)
                .register(meterRegistry);
    }

    @Override
    public void run(ApplicationArguments args) {
        int products = SeedSettings.parse(environment.getProperty(SeedSettings.KEY));

        if (catalogSeed.countProducts() == products) {
            LOG.atInfo()
                    .setMessage("seed.skipped")
                    .addKeyValue("products", products)
                    .log();
            return;
        }

        Instant start = clock.instant();
        transaction.executeWithoutResult(status -> {
            orderingSeed.clear();
            inventorySeed.clear();
            catalogSeed.clear();
            catalogSeed.load(products);
            inventorySeed.load();
        });
        long durationMs = Duration.between(start, clock.instant()).toMillis();

        lastDurationMs.set(durationMs);
        LOG.atInfo()
                .setMessage("seed.completed")
                .addKeyValue("products", products)
                .addKeyValue("duration_ms", durationMs)
                .log();
    }
}
