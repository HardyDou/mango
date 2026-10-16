ALTER TABLE cms_content
    ADD COLUMN code VARCHAR(128) NULL AFTER id,
    ADD UNIQUE KEY uk_cms_content_code (tenant_id, code, deleted);
