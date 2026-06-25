ALTER TABLE cms_advertisement
    ADD COLUMN position_type VARCHAR(32) NULL AFTER position,
    ADD COLUMN supported_material_types VARCHAR(255) NULL AFTER position_type,
    ADD COLUMN width INT NULL AFTER supported_material_types,
    ADD COLUMN height INT NULL AFTER width,
    ADD COLUMN remark VARCHAR(512) NULL AFTER height;

UPDATE cms_advertisement
SET position_type = 'CUSTOM',
    supported_material_types = CASE
        WHEN ad_type IS NULL OR ad_type = '' THEN 'TEXT,SINGLE_IMAGE,MULTI_IMAGE,VIDEO,RICH_TEXT,HTML'
        WHEN ad_type = 'IMAGE' THEN 'SINGLE_IMAGE'
        ELSE ad_type
    END
WHERE position_type IS NULL;

CREATE TABLE IF NOT EXISTS cms_ad_delivery (
    id BIGINT PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    org_id BIGINT NULL,
    site_id BIGINT NOT NULL,
    ad_id BIGINT NOT NULL,
    delivery_name VARCHAR(128) NOT NULL,
    material_type VARCHAR(32) NOT NULL,
    title VARCHAR(255) NULL,
    text_content VARCHAR(1024) NULL,
    rich_content TEXT NULL,
    html_content TEXT NULL,
    image_file_id VARCHAR(128) NULL,
    image_file_ids VARCHAR(1024) NULL,
    video_file_id VARCHAR(128) NULL,
    cover_file_id VARCHAR(128) NULL,
    jump_url VARCHAR(512) NULL,
    open_target VARCHAR(32) NULL,
    start_time DATETIME NULL,
    end_time DATETIME NULL,
    sort INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    created_at DATETIME NULL,
    updated_by BIGINT NULL,
    updated_at DATETIME NULL,
    KEY idx_cms_ad_delivery_site (tenant_id, site_id, ad_id, status, deleted),
    KEY idx_cms_ad_delivery_time (tenant_id, start_time, end_time, deleted)
);
