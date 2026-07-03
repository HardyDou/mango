ALTER TABLE link_access_record
    MODIFY COLUMN link_id BIGINT NULL,
    ADD COLUMN url VARCHAR(1024) NULL AFTER link_id,
    ADD COLUMN visitor_id VARCHAR(128) NULL AFTER user_id,
    ADD COLUMN extra_params VARCHAR(1024) NULL AFTER source,
    ADD KEY idx_link_access_url_time (tenant_id, url(191), access_time);
