CREATE TABLE {{moduleKebabSnake}}_{{aggregateKebabSnake}} (
    id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    tenant_id VARCHAR(64) NULL,
    created_by BIGINT NULL,
    created_at DATETIME NULL,
    updated_by BIGINT NULL,
    updated_at DATETIME NULL,
    PRIMARY KEY (id),
    KEY idx_{{moduleKebabSnake}}_{{aggregateKebabSnake}}_name (name)
);
