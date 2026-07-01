CREATE TABLE IF NOT EXISTS link_category (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    name VARCHAR(64) NOT NULL,
    sort_no INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL,
    remark VARCHAR(256) NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_link_category_tenant_name (tenant_id, name),
    KEY idx_link_category_status_sort (tenant_id, status, sort_no)
);

CREATE TABLE IF NOT EXISTS link_item (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    category_id BIGINT NULL,
    name VARCHAR(128) NOT NULL,
    url VARCHAR(1024) NOT NULL,
    summary VARCHAR(256) NULL,
    icon_url VARCHAR(1024) NULL,
    tags VARCHAR(512) NULL,
    visibility_scope VARCHAR(32) NOT NULL,
    owner_user_id BIGINT NULL,
    open_mode VARCHAR(32) NOT NULL,
    recommended TINYINT(1) NOT NULL DEFAULT 0,
    sort_no INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL,
    remark VARCHAR(256) NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_link_item_category_status_sort (tenant_id, category_id, status, sort_no),
    KEY idx_link_item_scope_status (tenant_id, visibility_scope, status),
    KEY idx_link_item_owner_scope (tenant_id, owner_user_id, visibility_scope),
    KEY idx_link_item_update_time (tenant_id, updated_at)
);

CREATE TABLE IF NOT EXISTS link_visibility_target (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    link_id BIGINT NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_id BIGINT NOT NULL,
    target_name VARCHAR(128) NULL,
    created_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_link_visibility_link (tenant_id, link_id),
    KEY idx_link_visibility_target (tenant_id, target_type, target_id)
);

CREATE TABLE IF NOT EXISTS link_favorite (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    link_id BIGINT NOT NULL,
    created_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_link_favorite_user_link (tenant_id, user_id, link_id),
    KEY idx_link_favorite_user_time (tenant_id, user_id, created_at)
);
