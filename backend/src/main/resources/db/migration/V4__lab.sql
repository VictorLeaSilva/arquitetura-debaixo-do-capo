-- Tabela do módulo lab (03 §3; dono: lab). Uma recarga dos dados de referência não toca nela (D-17).
CREATE TABLE lab_run (
    id             uuid         PRIMARY KEY,
    experiment_id  varchar(50)  NOT NULL,
    status         varchar(20)  NOT NULL CHECK (status IN ('RUNNING', 'COMPLETED', 'FAILED')),
    parameters     jsonb        NOT NULL,
    results        jsonb,
    failure_reason varchar(500),
    environment    jsonb        NOT NULL,
    trace_id       varchar(32)  NOT NULL,
    started_at     timestamptz  NOT NULL,
    finished_at    timestamptz
);
