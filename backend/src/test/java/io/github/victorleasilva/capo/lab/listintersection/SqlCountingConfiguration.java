package io.github.victorleasilva.capo.lab.listintersection;

import io.github.victorleasilva.capo.lab.run.NanoTimeStopwatch;
import io.github.victorleasilva.capo.lab.run.Stopwatch;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import javax.sql.DataSource;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DelegatingDataSource;

/**
 * Conta os comandos SQL da aplicação e quantos deles foram criados dentro de uma medição (AC-005.4). O
 * {@code DataSource} vira um {@link DelegatingDataSource} cujas conexões contam {@code createStatement},
 * {@code prepareStatement} e {@code prepareCall}; o {@code Stopwatch} {@code @Primary} envolve o real e soma o que foi
 * criado entre o início e o fim de cada {@code time}.
 */
@TestConfiguration
class SqlCountingConfiguration {

    private static final Set<String> STATEMENTS = Set.of("createStatement", "prepareStatement", "prepareCall");

    /** Os contadores ficam estáticos porque o {@code BeanPostProcessor} é criado antes dos outros beans. */
    static final AtomicLong STATEMENT_COUNT = new AtomicLong();

    static final AtomicLong INSIDE_MEASUREMENTS = new AtomicLong();

    static final AtomicLong MEASUREMENTS = new AtomicLong();

    @Bean
    static BeanPostProcessor countingDataSource() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof DataSource dataSource && !(bean instanceof DelegatingDataSource)) {
                    return new CountingDataSource(dataSource);
                }
                return bean;
            }
        };
    }

    @Bean
    @Primary
    Stopwatch countingStopwatch() {
        Stopwatch real = new NanoTimeStopwatch();
        return action -> {
            MEASUREMENTS.incrementAndGet();
            long start = STATEMENT_COUNT.get();
            Duration elapsed = real.time(action);
            INSIDE_MEASUREMENTS.addAndGet(STATEMENT_COUNT.get() - start);
            return elapsed;
        };
    }

    static final class CountingDataSource extends DelegatingDataSource {

        CountingDataSource(DataSource target) {
            super(target);
        }

        @Override
        public Connection getConnection() throws SQLException {
            return counting(super.getConnection());
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return counting(super.getConnection(username, password));
        }

        private static Connection counting(Connection connection) {
            return (Connection) Proxy.newProxyInstance(
                    Connection.class.getClassLoader(), new Class<?>[] {Connection.class}, (proxy, method, args) -> {
                        if (STATEMENTS.contains(method.getName())) {
                            STATEMENT_COUNT.incrementAndGet();
                        }
                        try {
                            return method.invoke(connection, args);
                        } catch (InvocationTargetException e) {
                            throw e.getCause();
                        }
                    });
        }
    }
}
