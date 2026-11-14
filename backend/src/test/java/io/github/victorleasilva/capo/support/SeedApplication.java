package io.github.victorleasilva.capo.support;

import io.github.victorleasilva.capo.CapoApplication;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Sobe a aplicação inteira, sem servidor web, apontando para um banco de teste. Subir de novo no mesmo banco é o
 * "reinício" dos critérios de aceite de RF-001.
 *
 * <p>Os valores vão como argumentos de linha de comando, que vencem as variáveis de ambiente: um
 * {@code LAB_SEED_PRODUCTS} definido no terminal de quem roda os testes não interfere.
 */
public final class SeedApplication {

    private SeedApplication() {}

    public static ConfigurableApplicationContext start(String jdbcUrl, String seedProducts, Class<?>... extraSources) {
        List<Class<?>> sources = new ArrayList<>();
        sources.add(CapoApplication.class);
        sources.addAll(List.of(extraSources));
        return new SpringApplicationBuilder(sources.toArray(Class<?>[]::new))
                .web(WebApplicationType.NONE)
                .run(
                        "--spring.datasource.url=" + jdbcUrl,
                        "--spring.datasource.username=" + PostgresTestDatabase.username(),
                        "--spring.datasource.password=" + PostgresTestDatabase.password(),
                        "--LAB_SEED_PRODUCTS=" + seedProducts);
    }
}
