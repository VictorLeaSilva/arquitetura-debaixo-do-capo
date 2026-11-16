package io.github.victorleasilva.capo.lab.run;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * As execuções em {@code lab_run} (03 §3), tabela do módulo {@code lab}. Cada operação é um comando próprio, fora de
 * transação: a linha RUNNING fica visível enquanto a execução roda, e uma queda a deixa para {@link #failOrphans()}.
 */
@Component
public class LabRunRepository {

    static final String ORPHAN_REASON = "interrompida: a aplicação reiniciou durante a execução";
    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {};
    private static final TypeReference<List<RunResult>> RESULTS = new TypeReference<>() {};

    private final JdbcClient jdbc;
    private final JsonMapper mapper;
    private final Clock clock;

    public LabRunRepository(JdbcClient jdbc, JsonMapper mapper, Clock clock) {
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.clock = clock;
    }

    void insertRunning(
            UUID id,
            String experimentId,
            Map<String, Object> parameters,
            Map<String, Object> environment,
            String traceId,
            Instant startedAt) {
        jdbc.sql("""
                        INSERT INTO lab_run (id, experiment_id, status, parameters, environment, trace_id, started_at)
                        VALUES (:id, :experimentId, 'RUNNING', CAST(:parameters AS jsonb), CAST(:environment AS jsonb),
                                :traceId, :startedAt)
                        """)
                .param("id", id)
                .param("experimentId", experimentId)
                .param("parameters", mapper.writeValueAsString(parameters))
                .param("environment", mapper.writeValueAsString(environment))
                .param("traceId", traceId)
                .param("startedAt", utc(startedAt))
                .update();
    }

    void complete(UUID id, List<RunResult> results, Instant finishedAt) {
        jdbc.sql("""
                        UPDATE lab_run SET status = 'COMPLETED', results = CAST(:results AS jsonb), finished_at = :finishedAt
                        WHERE id = :id
                        """)
                .param("id", id)
                .param("results", mapper.writeValueAsString(results))
                .param("finishedAt", utc(finishedAt))
                .update();
    }

    void fail(UUID id, String reason, Instant finishedAt) {
        jdbc.sql("""
                        UPDATE lab_run SET status = 'FAILED', results = NULL, failure_reason = :reason,
                               finished_at = :finishedAt
                        WHERE id = :id
                        """)
                .param("id", id)
                .param("reason", reason)
                .param("finishedAt", utc(finishedAt))
                .update();
    }

    public Optional<LabRun> find(UUID id) {
        return jdbc.sql("SELECT * FROM lab_run WHERE id = :id")
                .param("id", id)
                .query((rs, row) -> toLabRun(rs))
                .optional();
    }

    /** A1, passo 2 (AC-004.4): toda execução RUNNING deixada por uma aplicação que caiu passa a FAILED. */
    public int failOrphans() {
        return jdbc.sql("""
                        UPDATE lab_run SET status = 'FAILED', failure_reason = :reason, finished_at = :finishedAt
                        WHERE status = 'RUNNING'
                        """)
                .param("reason", ORPHAN_REASON)
                .param("finishedAt", utc(clock.instant()))
                .update();
    }

    private LabRun toLabRun(ResultSet rs) throws SQLException {
        String results = rs.getString("results");
        return new LabRun(
                rs.getObject("id", UUID.class),
                rs.getString("experiment_id"),
                rs.getString("status"),
                mapper.readValue(rs.getString("parameters"), MAP),
                results == null ? null : mapper.readValue(results, RESULTS),
                rs.getString("failure_reason"),
                mapper.readValue(rs.getString("environment"), MAP),
                rs.getString("trace_id"),
                instant(rs, "started_at"),
                instant(rs, "finished_at"));
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }

    private static OffsetDateTime utc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }
}
