package io.github.victorleasilva.capo.lab.run;

import com.zaxxer.hikari.HikariDataSource;
import io.github.victorleasilva.capo.lab.seed.SeedSettings;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.boot.SpringBootVersion;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * O ambiente em que uma execução rodou (03 §3, {@code lab_run.environment}): sem ele, um número publicado não tem
 * origem (P7).
 */
@Component
public class EnvironmentProbe {

    private static final long MIB = 1024L * 1024L;

    private final JdbcClient jdbc;
    private final DataSource dataSource;
    private final Environment environment;

    public EnvironmentProbe(JdbcClient jdbc, DataSource dataSource, Environment environment) {
        this.jdbc = jdbc;
        this.dataSource = dataSource;
        this.environment = environment;
    }

    public Map<String, Object> describe() {
        Map<String, Object> env = new LinkedHashMap<>();
        env.put("javaVersion", Runtime.version().toString());
        env.put("springBootVersion", SpringBootVersion.getVersion());
        env.put(
                "postgresVersion",
                jdbc.sql("SHOW server_version").query(String.class).single());
        env.put("osName", System.getProperty("os.name"));
        env.put("osArch", System.getProperty("os.arch"));
        env.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        env.put("maxHeapMb", Runtime.getRuntime().maxMemory() / MIB);
        env.put("dbPoolSize", poolSize());
        env.put("seedProducts", SeedSettings.parse(environment.getProperty(SeedSettings.KEY)));
        return env;
    }

    private int poolSize() {
        try {
            return dataSource.unwrap(HikariDataSource.class).getMaximumPoolSize();
        } catch (SQLException e) {
            throw new DataAccessResourceFailureException("o pool de conexões não é o HikariCP", e);
        }
    }
}
