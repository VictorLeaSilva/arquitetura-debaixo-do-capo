-- Tabelas do módulo catalog (03 §3; dono: catalog).
CREATE TABLE category (
    id   bigint       PRIMARY KEY,
    name varchar(100) NOT NULL UNIQUE
);

CREATE TABLE product (
    id          bigint         PRIMARY KEY,
    sku         varchar(20)    NOT NULL UNIQUE CHECK (sku ~ '^SKU-[0-9]{7}$'),
    name        varchar(200)   NOT NULL,
    category_id bigint         NOT NULL REFERENCES category (id),
    price       numeric(12, 2) NOT NULL CHECK (price > 0),
    created_at  timestamptz    NOT NULL
);

CREATE INDEX ix_product_category ON product (category_id);

CREATE TABLE product_attribute (
    id         bigint       PRIMARY KEY,
    product_id bigint       NOT NULL REFERENCES product (id),
    name       varchar(50)  NOT NULL,
    value      varchar(200) NOT NULL,
    UNIQUE (product_id, name)
);

CREATE INDEX ix_attribute_product ON product_attribute (product_id);
