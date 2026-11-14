-- Tabela do módulo inventory (03 §3; dono: inventory). A FK para product(id) é o acoplamento aceito no ADR-002.
CREATE TABLE flash_sale_stock (
    product_id       bigint  PRIMARY KEY REFERENCES product (id),
    initial_quantity integer NOT NULL CHECK (initial_quantity BETWEEN 1 AND 100),
    available        integer NOT NULL CHECK (available >= 0)
);
