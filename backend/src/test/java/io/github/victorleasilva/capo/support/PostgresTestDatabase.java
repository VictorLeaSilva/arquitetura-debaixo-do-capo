package io.github.victorleasilva.capo.support;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Um PostgreSQL real por JVM de teste (04 §3: nada de banco em memória), com a mesma imagem do compose.yaml.
 *
 * <p>Cada teste pede um banco novo e vazio com {@link #createDatabase(String)}: o container é um só, e os testes não
 * dividem dados.
 */
public final class PostgresTestDatabase {

    private static final DockerImageName IMAGE = DockerImageName.parse("postgres:18.6-trixie");
    private static final AtomicInteger SEQUENCE = new AtomicInteger();
    private static PostgreSQLContainer container;

    private PostgresTestDatabase() {}

    private static synchronized PostgreSQLContainer container() {
        if (container == null) {
            container = new PostgreSQLContainer(IMAGE);
            container.start();
        }
        return container;
    }

    /** Cria um banco vazio com um nome único que começa com {@code prefix} e devolve a URL JDBC dele. */
    public static String createDatabase(String prefix) {
        if (!prefix.matches("[a-z][a-z0-9_]{0,40}")) {
            throw new IllegalArgumentException("prefixo inválido: " + prefix);
        }
        String name = prefix + "_" + SEQUENCE.incrementAndGet();
        PostgreSQLContainer pg = container();
        try (Connection connection = DriverManager.getConnection(pg.getJdbcUrl(), username(), password());
                Statement statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE " + name);
        } catch (SQLException e) {
            throw new IllegalStateException("não foi possível criar o banco " + name, e);
        }
        return "jdbc:postgresql://" + pg.getHost() + ":" + pg.getMappedPort(5432) + "/" + name;
    }

    public static String username() {
        return container().getUsername();
    }

    public static String password() {
        return container().getPassword();
    }
}
