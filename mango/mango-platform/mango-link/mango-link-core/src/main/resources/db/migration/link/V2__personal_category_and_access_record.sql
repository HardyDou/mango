ALTER TABLE link_category
    ADD COLUMN scope VARCHAR(32) NOT NULL DEFAULT 'COMPANY' AFTER tenant_id,
    ADD COLUMN owner_user_id BIGINT NOT NULL DEFAULT 0 AFTER scope;

ALTER TABLE link_category
    DROP INDEX uk_link_category_tenant_name,
    ADD UNIQUE KEY uk_link_category_owner_name (tenant_id, scope, owner_user_id, name),
    ADD KEY idx_link_category_owner_status_sort (tenant_id, scope, owner_user_id, status, sort_no);

CREATE TABLE IF NOT EXISTS link_access_record (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    link_id BIGINT NOT NULL,
    user_id BIGINT NULL,
    source VARCHAR(32) NULL,
    client_ip VARCHAR(64) NULL,
    user_agent VARCHAR(512) NULL,
    referer VARCHAR(1024) NULL,
    access_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_link_access_link_time (tenant_id, link_id, access_time),
    KEY idx_link_access_user_time (tenant_id, user_id, access_time)
);
