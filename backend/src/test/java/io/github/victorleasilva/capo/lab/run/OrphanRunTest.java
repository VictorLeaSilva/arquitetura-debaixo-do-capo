package io.github.victorleasilva.capo.lab.run;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.victorleasilva.capo.support.PostgresTestDatabase;
import io.github.victorleasilva.capo.support.SeedApplication;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/** Uma execução RUNNING deixada por uma aplicação que caiu vira FAILED quando a aplicação reinicia (AC-004.4). */
@Tag("RF-004")
class OrphanRunTest {

    @Test
    void runningRowsBecomeFailedOnRestart() {
        String url = PostgresTestDatabase.createDatabase("orphan");
        SeedApplication.start(url, "10000").close();
        JdbcClient db = JdbcClient.create(
                new DriverManagerDataSource(url, PostgresTestDatabase.username(), PostgresTestDatabase.password()));
        db.sql("INSERT INTO lab_run (id, experiment_id, status, parameters, environment, trace_id, started_at)"
                        + " VALUES ('00000000-0000-0000-0000-0000000000aa', 'test-blocking', 'RUNNING', '{}', '{}',"
                        + " 'trace', TIMESTAMPTZ '2026-03-01T10:00:00Z')")
                .update();

        SeedApplication.start(url, "10000").close();

        Map<String, Object> row = db.sql("SELECT status, failure_reason, finished_at IS NOT NULL AS finished"
                        + " FROM lab_run WHERE id = '00000000-0000-0000-0000-0000000000aa'")
                .query()
                .singleRow();
        assertThat(row)
                .containsEntry("status", "FAILED")
                .containsEntry("failure_reason", "interrompida: a aplicação reiniciou durante a execução")
                .containsEntry("finished", true);
    }
}
