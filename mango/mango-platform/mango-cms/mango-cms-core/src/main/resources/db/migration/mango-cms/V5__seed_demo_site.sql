INSERT INTO cms_site (id, tenant_id, org_id, site_name, site_code, logo_file_id, description, domain, status, default_language, seo_title, seo_keywords, seo_description, footer_copyright, icp_record, contact_info, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000000001, '1', NULL, '演示站点', 'demo', NULL, 'CMS 演示站点', '127.0.0.1:5193', 'ENABLED', 'zh-CN', '演示站点 · 内容管理平台', 'CMS,内容管理,演示站点', '这是一个基于 Mango CMS 的演示站点，用于展示站点解析、导航、栏目、Banner、广告与内容发布能力。', '© 2026 演示站点 保留所有权利', '演示ICP备2026000001号', '商务咨询：demo@example.com；服务时间：工作日 09:00-18:00', 0, 0, NOW(), 0, NOW()
WHERE NOT EXISTS (SELECT 1 FROM cms_site WHERE tenant_id = '1' AND site_code = 'demo' AND deleted = 0);

INSERT INTO cms_site_category (id, tenant_id, org_id, site_id, parent_id, category_name, category_code, category_type, access_path, external_url, sort, visible_status, access_type, role_codes, seo_title, seo_keywords, seo_description, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000000101, '1', NULL, 2070000000000000001, 0, '首页', 'home', 'PAGE', '/', NULL, 1, 'VISIBLE', 'PUBLIC', NULL, NULL, NULL, NULL, 0, 0, NOW(), 0, NOW()
WHERE EXISTS (SELECT 1 FROM cms_site WHERE id = 2070000000000000001 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM cms_site_category WHERE tenant_id = '1' AND site_id = 2070000000000000001 AND category_code = 'home' AND deleted = 0);

INSERT INTO cms_site_category (id, tenant_id, org_id, site_id, parent_id, category_name, category_code, category_type, access_path, external_url, sort, visible_status, access_type, role_codes, seo_title, seo_keywords, seo_description, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000000102, '1', NULL, 2070000000000000001, 0, '产品能力', 'products', 'LIST', '/products', NULL, 2, 'VISIBLE', 'PUBLIC', NULL, NULL, NULL, NULL, 0, 0, NOW(), 0, NOW()
WHERE EXISTS (SELECT 1 FROM cms_site WHERE id = 2070000000000000001 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM cms_site_category WHERE tenant_id = '1' AND site_id = 2070000000000000001 AND category_code = 'products' AND deleted = 0);

INSERT INTO cms_site_category (id, tenant_id, org_id, site_id, parent_id, category_name, category_code, category_type, access_path, external_url, sort, visible_status, access_type, role_codes, seo_title, seo_keywords, seo_description, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000000103, '1', NULL, 2070000000000000001, 0, '解决方案', 'solutions', 'LIST', '/solutions', NULL, 3, 'VISIBLE', 'PUBLIC', NULL, NULL, NULL, NULL, 0, 0, NOW(), 0, NOW()
WHERE EXISTS (SELECT 1 FROM cms_site WHERE id = 2070000000000000001 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM cms_site_category WHERE tenant_id = '1' AND site_id = 2070000000000000001 AND category_code = 'solutions' AND deleted = 0);

INSERT INTO cms_site_category (id, tenant_id, org_id, site_id, parent_id, category_name, category_code, category_type, access_path, external_url, sort, visible_status, access_type, role_codes, seo_title, seo_keywords, seo_description, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000000104, '1', NULL, 2070000000000000001, 0, '新闻动态', 'news', 'LIST', '/news', NULL, 4, 'VISIBLE', 'PUBLIC', NULL, NULL, NULL, NULL, 0, 0, NOW(), 0, NOW()
WHERE EXISTS (SELECT 1 FROM cms_site WHERE id = 2070000000000000001 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM cms_site_category WHERE tenant_id = '1' AND site_id = 2070000000000000001 AND category_code = 'news' AND deleted = 0);

INSERT INTO cms_site_category (id, tenant_id, org_id, site_id, parent_id, category_name, category_code, category_type, access_path, external_url, sort, visible_status, access_type, role_codes, seo_title, seo_keywords, seo_description, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000000105, '1', NULL, 2070000000000000001, 0, '关于我们', 'about', 'PAGE', '/about', NULL, 5, 'VISIBLE', 'PUBLIC', NULL, NULL, NULL, NULL, 0, 0, NOW(), 0, NOW()
WHERE EXISTS (SELECT 1 FROM cms_site WHERE id = 2070000000000000001 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM cms_site_category WHERE tenant_id = '1' AND site_id = 2070000000000000001 AND category_code = 'about' AND deleted = 0);

INSERT INTO cms_navigation (id, tenant_id, org_id, site_id, nav_type, nav_name, jump_type, category_id, content_id, external_url, open_target, sort, status, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000000201, '1', NULL, 2070000000000000001, 'TOP', '首页', 'URL', NULL, NULL, '/', 'SELF', 1, 'ENABLED', 0, 0, NOW(), 0, NOW()
WHERE EXISTS (SELECT 1 FROM cms_site WHERE id = 2070000000000000001 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM cms_navigation WHERE tenant_id = '1' AND site_id = 2070000000000000001 AND nav_type = 'TOP' AND nav_name = '首页' AND deleted = 0);

INSERT INTO cms_navigation (id, tenant_id, org_id, site_id, nav_type, nav_name, jump_type, category_id, content_id, external_url, open_target, sort, status, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000000202, '1', NULL, 2070000000000000001, 'TOP', '产品能力', 'URL', NULL, NULL, '/products', 'SELF', 2, 'ENABLED', 0, 0, NOW(), 0, NOW()
WHERE EXISTS (SELECT 1 FROM cms_site WHERE id = 2070000000000000001 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM cms_navigation WHERE tenant_id = '1' AND site_id = 2070000000000000001 AND nav_type = 'TOP' AND nav_name = '产品能力' AND deleted = 0);

INSERT INTO cms_navigation (id, tenant_id, org_id, site_id, nav_type, nav_name, jump_type, category_id, content_id, external_url, open_target, sort, status, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000000203, '1', NULL, 2070000000000000001, 'TOP', '解决方案', 'URL', NULL, NULL, '/solutions', 'SELF', 3, 'ENABLED', 0, 0, NOW(), 0, NOW()
WHERE EXISTS (SELECT 1 FROM cms_site WHERE id = 2070000000000000001 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM cms_navigation WHERE tenant_id = '1' AND site_id = 2070000000000000001 AND nav_type = 'TOP' AND nav_name = '解决方案' AND deleted = 0);

INSERT INTO cms_navigation (id, tenant_id, org_id, site_id, nav_type, nav_name, jump_type, category_id, content_id, external_url, open_target, sort, status, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000000204, '1', NULL, 2070000000000000001, 'TOP', '新闻动态', 'URL', NULL, NULL, '/news', 'SELF', 4, 'ENABLED', 0, 0, NOW(), 0, NOW()
WHERE EXISTS (SELECT 1 FROM cms_site WHERE id = 2070000000000000001 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM cms_navigation WHERE tenant_id = '1' AND site_id = 2070000000000000001 AND nav_type = 'TOP' AND nav_name = '新闻动态' AND deleted = 0);

INSERT INTO cms_navigation (id, tenant_id, org_id, site_id, nav_type, nav_name, jump_type, category_id, content_id, external_url, open_target, sort, status, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000000205, '1', NULL, 2070000000000000001, 'TOP', '关于我们', 'URL', NULL, NULL, '/about', 'SELF', 5, 'ENABLED', 0, 0, NOW(), 0, NOW()
WHERE EXISTS (SELECT 1 FROM cms_site WHERE id = 2070000000000000001 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM cms_navigation WHERE tenant_id = '1' AND site_id = 2070000000000000001 AND nav_type = 'TOP' AND nav_name = '关于我们' AND deleted = 0);

INSERT INTO cms_navigation (id, tenant_id, org_id, site_id, nav_type, nav_name, jump_type, category_id, content_id, external_url, open_target, sort, status, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000000301, '1', NULL, 2070000000000000001, 'FOOTER', '产品能力', 'URL', NULL, NULL, '/products', 'SELF', 1, 'ENABLED', 0, 0, NOW(), 0, NOW()
WHERE EXISTS (SELECT 1 FROM cms_site WHERE id = 2070000000000000001 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM cms_navigation WHERE tenant_id = '1' AND site_id = 2070000000000000001 AND nav_type = 'FOOTER' AND nav_name = '产品能力' AND deleted = 0);

INSERT INTO cms_navigation (id, tenant_id, org_id, site_id, nav_type, nav_name, jump_type, category_id, content_id, external_url, open_target, sort, status, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000000302, '1', NULL, 2070000000000000001, 'FOOTER', '解决方案', 'URL', NULL, NULL, '/solutions', 'SELF', 2, 'ENABLED', 0, 0, NOW(), 0, NOW()
WHERE EXISTS (SELECT 1 FROM cms_site WHERE id = 2070000000000000001 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM cms_navigation WHERE tenant_id = '1' AND site_id = 2070000000000000001 AND nav_type = 'FOOTER' AND nav_name = '解决方案' AND deleted = 0);

INSERT INTO cms_navigation (id, tenant_id, org_id, site_id, nav_type, nav_name, jump_type, category_id, content_id, external_url, open_target, sort, status, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000000303, '1', NULL, 2070000000000000001, 'FOOTER', '新闻动态', 'URL', NULL, NULL, '/news', 'SELF', 3, 'ENABLED', 0, 0, NOW(), 0, NOW()
WHERE EXISTS (SELECT 1 FROM cms_site WHERE id = 2070000000000000001 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM cms_navigation WHERE tenant_id = '1' AND site_id = 2070000000000000001 AND nav_type = 'FOOTER' AND nav_name = '新闻动态' AND deleted = 0);

INSERT INTO cms_navigation (id, tenant_id, org_id, site_id, nav_type, nav_name, jump_type, category_id, content_id, external_url, open_target, sort, status, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000000304, '1', NULL, 2070000000000000001, 'FOOTER', '关于我们', 'URL', NULL, NULL, '/about', 'SELF', 4, 'ENABLED', 0, 0, NOW(), 0, NOW()
WHERE EXISTS (SELECT 1 FROM cms_site WHERE id = 2070000000000000001 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM cms_navigation WHERE tenant_id = '1' AND site_id = 2070000000000000001 AND nav_type = 'FOOTER' AND nav_name = '关于我们' AND deleted = 0);

INSERT INTO cms_banner (id, tenant_id, org_id, site_id, position, title, subtitle, media_type, media_file_id, jump_url, start_time, end_time, sort, status, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000000401, '1', NULL, 2070000000000000001, 'HOME_HERO', '内容管理平台 · 让每一次发布可信可溯', '全线上内容发布 · 多站点管理 · 数据穿透', 'IMAGE', NULL, '/products', NULL, NULL, 1, 'ENABLED', 0, 0, NOW(), 0, NOW()
WHERE EXISTS (SELECT 1 FROM cms_site WHERE id = 2070000000000000001 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM cms_banner WHERE tenant_id = '1' AND site_id = 2070000000000000001 AND position = 'HOME_HERO' AND title = '内容管理平台 · 让每一次发布可信可溯' AND deleted = 0);
