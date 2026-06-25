INSERT INTO cms_content (id, tenant_id, org_id, title, subtitle, summary, content_type, cover_file_id, body, external_url, attachment_file_id, video_file_id, source, author, category_id, seo_title, seo_keywords, seo_description, status, publish_time, offline_time, review_comment, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000000501, '1', NULL, '内容管理平台 3.0 发布，发布时效迈入秒级时代', '全新架构与智能审核上线', '演示站点正式发布内容管理平台 3.0，依托分布式发布引擎与智能审核模型，将平均发布时效从小时级压缩至秒级。', 'ARTICLE', NULL, '<h2>秒级发布成为现实</h2><p>演示站点今日正式发布内容管理平台 3.0。新平台采用分布式发布架构与智能审核引擎，将平均发布时效从小时级压缩至秒级。</p><p>平台已通过安全测评，累计服务内容超 200 万条。</p>', NULL, NULL, NULL, '演示编辑部', '内容运营', 2070000000000000104, NULL, NULL, NULL, 'PUBLISHED', NOW(), NULL, NULL, 0, 0, NOW(), 0, NOW()
WHERE EXISTS (SELECT 1 FROM cms_site_category WHERE id = 2070000000000000104 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM cms_content WHERE tenant_id = '1' AND id = 2070000000000000501 AND deleted = 0);

INSERT INTO cms_content (id, tenant_id, org_id, title, subtitle, summary, content_type, cover_file_id, body, external_url, attachment_file_id, video_file_id, source, author, category_id, seo_title, seo_keywords, seo_description, status, publish_time, offline_time, review_comment, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000000502, '1', NULL, '演示站点中标某省公共服务平台内容系统项目', '省级内容数字化再加速', '演示站点成功中标某省公共服务平台内容数字化建设项目，将提供统一的内容发布、核验与风控服务。', 'ARTICLE', NULL, '<h2>省级内容数字化再加速</h2><p>演示站点成功中标某省公共服务平台内容数字化建设项目。项目将建设覆盖全省的内容发布与核验体系。</p>', NULL, NULL, NULL, '演示编辑部', '内容运营', 2070000000000000104, NULL, NULL, NULL, 'PUBLISHED', NOW(), NULL, NULL, 0, 0, NOW(), 0, NOW()
WHERE EXISTS (SELECT 1 FROM cms_site_category WHERE id = 2070000000000000104 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM cms_content WHERE tenant_id = '1' AND id = 2070000000000000502 AND deleted = 0);

INSERT INTO cms_content (id, tenant_id, org_id, title, subtitle, summary, content_type, cover_file_id, body, external_url, attachment_file_id, video_file_id, source, author, category_id, seo_title, seo_keywords, seo_description, status, publish_time, offline_time, review_comment, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000000503, '1', NULL, '智能审核图谱上线，拦截违规内容准确率达 99.2%', '让违规内容无所遁形', '演示站点自研智能审核图谱正式上线，通过多维关系建模与实时行为分析，对违规内容的拦截准确率达到 99.2%。', 'ARTICLE', NULL, '<h2>让违规内容无所遁形</h2><p>演示站点自研智能审核图谱正式上线。通过多维关系建模与实时行为分析，违规内容拦截准确率达 99.2%。</p>', NULL, NULL, NULL, '演示编辑部', '内容运营', 2070000000000000104, NULL, NULL, NULL, 'PUBLISHED', NOW(), NULL, NULL, 0, 0, NOW(), 0, NOW()
WHERE EXISTS (SELECT 1 FROM cms_site_category WHERE id = 2070000000000000104 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM cms_content WHERE tenant_id = '1' AND id = 2070000000000000503 AND deleted = 0);

INSERT INTO cms_content_publish (id, tenant_id, org_id, content_id, site_id, category_id, publish_status, publish_time, scheduled_publish_time, offline_time, top, top_scope, recommended, recommendation_type, sort, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000000601, '1', NULL, 2070000000000000501, 2070000000000000001, 2070000000000000104, 'PUBLISHED', NOW(), NULL, NULL, 0, NULL, 0, NULL, 1, 0, 0, NOW(), 0, NOW()
WHERE EXISTS (SELECT 1 FROM cms_content WHERE id = 2070000000000000501 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM cms_content_publish WHERE tenant_id = '1' AND content_id = 2070000000000000501 AND site_id = 2070000000000000001 AND deleted = 0);

INSERT INTO cms_content_publish (id, tenant_id, org_id, content_id, site_id, category_id, publish_status, publish_time, scheduled_publish_time, offline_time, top, top_scope, recommended, recommendation_type, sort, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000000602, '1', NULL, 2070000000000000502, 2070000000000000001, 2070000000000000104, 'PUBLISHED', NOW(), NULL, NULL, 0, NULL, 0, NULL, 2, 0, 0, NOW(), 0, NOW()
WHERE EXISTS (SELECT 1 FROM cms_content WHERE id = 2070000000000000502 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM cms_content_publish WHERE tenant_id = '1' AND content_id = 2070000000000000502 AND site_id = 2070000000000000001 AND deleted = 0);

INSERT INTO cms_content_publish (id, tenant_id, org_id, content_id, site_id, category_id, publish_status, publish_time, scheduled_publish_time, offline_time, top, top_scope, recommended, recommendation_type, sort, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000000603, '1', NULL, 2070000000000000503, 2070000000000000001, 2070000000000000104, 'PUBLISHED', NOW(), NULL, NULL, 0, NULL, 0, NULL, 3, 0, 0, NOW(), 0, NOW()
WHERE EXISTS (SELECT 1 FROM cms_content WHERE id = 2070000000000000503 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM cms_content_publish WHERE tenant_id = '1' AND content_id = 2070000000000000503 AND site_id = 2070000000000000001 AND deleted = 0);
