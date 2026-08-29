CREATE TABLE IF NOT EXISTS guarantee_product (
    id bigint NOT NULL,
    biz_code varchar(128) NOT NULL,
    managed_value varchar(255) NOT NULL,
    enabled tinyint NOT NULL DEFAULT 1,
    created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_guarantee_product_biz_code (biz_code)
);

CREATE TABLE IF NOT EXISTS guarantee_env_endpoint (
    id bigint NOT NULL,
    biz_code varchar(128) NOT NULL,
    managed_value varchar(255) NOT NULL,
    enabled tinyint NOT NULL DEFAULT 1,
    created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_guarantee_env_endpoint_biz_code (biz_code)
);

CREATE TABLE IF NOT EXISTS guarantee_business_bootstrap (
    id bigint NOT NULL,
    marker varchar(64) NOT NULL,
    PRIMARY KEY (id)
);
