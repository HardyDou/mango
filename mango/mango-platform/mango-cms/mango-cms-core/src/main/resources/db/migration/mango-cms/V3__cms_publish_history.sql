-- 发布改为保留历史记录：同一内容在同一站点栏目下可以存在多次发布/下线记录。
-- 移除唯一约束，改为普通查询索引；前台展示取最新一条有效记录。
ALTER TABLE `cms_content_publish` DROP INDEX `uk_cms_publish_position`;
ALTER TABLE `cms_content_publish` ADD INDEX `idx_cms_publish_position` (`tenant_id`, `content_id`, `site_id`, `category_id`, `deleted`);
ALTER TABLE `cms_content_publish` ADD INDEX `idx_cms_publish_history` (`tenant_id`, `content_id`, `site_id`, `category_id`, `publish_status`, `id`);
