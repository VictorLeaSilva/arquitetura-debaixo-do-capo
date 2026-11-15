package io.github.victorleasilva.capo.lab.run;

import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Component;

/**
 * Uma execução por vez (D-04): um advisory lock do PostgreSQL, de sessão, numa conexão dedicada do pool. A trava fica
 * no banco, e não na memória, para continuar valendo com várias instâncias da aplicação (fatia 4).
 */
@Component
public class RunLock {

    /** A chave fixa do laboratório; qualquer número serve, desde que todas as instâncias usem o mesmo. */
    public static final long KEY = 4_242_012L;

    private final DataSource dataSource;

    public RunLock(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** Tenta pegar a trava sem esperar; vazio se outra sessão já a segura. */
    public Optional<Held> tryAcquire() {
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            if (call(connection, "SELECT pg_try_advisory_lock(?)")) {
                return Optional.of(new Held(connection, dataSource));
            }
            connection.close();
            return Optional.empty();
        } catch (SQLException e) {
            closeQuietly(connection);
            throw new DataAccessResourceFailureException("não foi possível tentar a trava de execução", e);
        }
    }

    private static boolean call(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, KEY);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        }
    }

    private static void closeQuietly(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
                // a falha original é a que importa
            }
        }
    }

    /** A trava segura. {@link #close()} a libera na mesma conexão e devolve a conexão ao pool, sempre. */
    public static final class Held implements AutoCloseable {

        private final Connection connection;
        private final DataSource dataSource;

        private Held(Connection connection, DataSource dataSource) {
            this.connection = connection;
            this.dataSource = dataSource;
        }

        @Override
        public void close() {
            try {
                call(connection, "SELECT pg_advisory_unlock(?)");
            } catch (SQLException e) {
                // Se voltasse ao pool, a conexão continuaria segurando a trava. Retirada do pool (evict), ela é
                // fechada de verdade, e o PostgreSQL solta a trava ao encerrar a sessão.
                evictQuietly();
                throw new DataAccessResourceFailureException("não foi possível liberar a trava de execução", e);
            } finally {
                closeQuietly(connection);
            }
        }

        private void evictQuietly() {
            try {
                dataSource.unwrap(HikariDataSource.class).evictConnection(connection);
            } catch (SQLException ignored) {
                // a falha original é a que importa
            }
        }
    }
}
