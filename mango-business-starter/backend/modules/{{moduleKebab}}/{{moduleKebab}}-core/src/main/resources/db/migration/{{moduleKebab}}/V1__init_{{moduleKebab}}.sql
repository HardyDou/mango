CREATE TABLE {{moduleKebabSnake}}_{{aggregateKebabSnake}} (
    id VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    tenant_id VARCHAR(64) NULL,
    created_time DATETIME NULL,
    updated_time DATETIME NULL,
    PRIMARY KEY (id)
);
