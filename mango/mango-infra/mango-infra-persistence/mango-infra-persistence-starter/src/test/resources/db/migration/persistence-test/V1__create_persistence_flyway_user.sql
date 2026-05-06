CREATE TABLE persistence_flyway_user (
    id BIGINT NOT NULL,
    username VARCHAR(64) NOT NULL,
    created_by BIGINT,
    created_at TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP,
    tenant_id VARCHAR(64),
    PRIMARY KEY (id)
);

INSERT INTO persistence_flyway_user (
    id, username, created_by, created_at, updated_by, updated_at, tenant_id
) VALUES (
    1, 'migrated', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 'tenant-a'
);
