-- Tabela do módulo ordering (03 §3; dono: ordering). "order" é palavra reservada do SQL.
-- lab_run_id fica SEM chave estrangeira para lab_run (D-17): ordering não depende do lab.
CREATE TABLE customer_order (
    id          uuid        PRIMARY KEY,
    product_id  bigint      NOT NULL REFERENCES product (id),
    quantity    integer     NOT NULL CHECK (quantity > 0),
    lab_run_id  uuid,
    lab_variant varchar(10) CHECK (lab_variant IN ('bad', 'medium', 'good')),
    created_at  timestamptz NOT NULL
);

CREATE INDEX ix_order_lab_run ON customer_order (lab_run_id, lab_variant);
