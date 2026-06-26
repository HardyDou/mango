UPDATE cms_site
SET org_id = 1,
    created_by = 1,
    updated_by = 1
WHERE tenant_id = '1'
  AND site_code = 'demo'
  AND deleted = 0;

UPDATE cms_site_category
SET org_id = 1,
    created_by = 1,
    updated_by = 1
WHERE tenant_id = '1'
  AND site_id = 2070000000000000001
  AND deleted = 0;

UPDATE cms_navigation
SET org_id = 1,
    created_by = 1,
    updated_by = 1
WHERE tenant_id = '1'
  AND site_id = 2070000000000000001
  AND deleted = 0;

UPDATE cms_banner
SET org_id = 1,
    created_by = 1,
    updated_by = 1
WHERE tenant_id = '1'
  AND site_id = 2070000000000000001
  AND deleted = 0;

UPDATE cms_content
SET org_id = 1,
    created_by = 1,
    updated_by = 1
WHERE tenant_id = '1'
  AND id IN (2070000000000000501, 2070000000000000502, 2070000000000000503)
  AND deleted = 0;

UPDATE cms_content_publish
SET org_id = 1,
    created_by = 1,
    updated_by = 1
WHERE tenant_id = '1'
  AND site_id = 2070000000000000001
  AND deleted = 0;

UPDATE cms_advertisement
SET org_id = 1,
    created_by = 1,
    updated_by = 1
WHERE tenant_id = '1'
  AND site_id = 2070000000000000001
  AND deleted = 0;

UPDATE cms_ad_delivery
SET org_id = 1,
    created_by = 1,
    updated_by = 1
WHERE tenant_id = '1'
  AND site_id = 2070000000000000001
  AND deleted = 0;

INSERT INTO cms_site_setting (id, tenant_id, org_id, site_id, seo_title, seo_keywords, seo_description, footer_copyright, icp_record, contact_info, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000000901, s.tenant_id, s.org_id, s.id, s.seo_title, s.seo_keywords, s.seo_description, s.footer_copyright, s.icp_record, s.contact_info, 0, 1, NOW(), 1, NOW()
FROM cms_site s
WHERE s.id = 2070000000000000001
  AND s.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM cms_site_setting ss WHERE ss.tenant_id = s.tenant_id AND ss.site_id = s.id AND ss.deleted = 0);

INSERT INTO cms_site (id, tenant_id, org_id, site_name, site_code, logo_file_id, description, domain, status, default_language, seo_title, seo_keywords, seo_description, footer_copyright, icp_record, contact_info, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000001001, '1', 1, '帮助中心', 'mango-help', NULL, 'Mango 帮助中心', '127.0.0.1:5192', 'ENABLED', 'zh-CN', 'Mango 帮助中心', 'Mango,帮助中心,使用文档', 'Mango 帮助中心提供入门、权限、安全、内容管理与常见问题说明。', '© 2026 Mango 帮助中心', '帮助ICP备2026000001号', '服务支持：help@example.com；服务时间：工作日 09:00-18:00', 0, 1, NOW(), 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM cms_site WHERE tenant_id = '1' AND site_code = 'mango-help' AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM cms_site WHERE domain = '127.0.0.1:5192' AND deleted = 0);

INSERT INTO cms_site_setting (id, tenant_id, org_id, site_id, seo_title, seo_keywords, seo_description, footer_copyright, icp_record, contact_info, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000001901, s.tenant_id, s.org_id, s.id, s.seo_title, s.seo_keywords, s.seo_description, s.footer_copyright, s.icp_record, s.contact_info, 0, 1, NOW(), 1, NOW()
FROM cms_site s
WHERE s.site_code = 'mango-help'
  AND s.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM cms_site_setting ss WHERE ss.tenant_id = s.tenant_id AND ss.site_id = s.id AND ss.deleted = 0);

INSERT INTO cms_site_category (id, tenant_id, org_id, site_id, parent_id, category_name, category_code, category_type, access_path, external_url, sort, visible_status, access_type, role_codes, seo_title, seo_keywords, seo_description, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000001101, s.tenant_id, s.org_id, s.id, 0, '快速开始', 'getting-started', 'LIST', '/getting-started', NULL, 1, 'VISIBLE', 'PUBLIC', NULL, '快速开始', 'Mango,快速开始', 'Mango 快速开始文档。', 0, 1, NOW(), 1, NOW()
FROM cms_site s
WHERE s.site_code = 'mango-help'
  AND s.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM cms_site_category c WHERE c.tenant_id = s.tenant_id AND c.site_id = s.id AND c.category_code = 'getting-started' AND c.deleted = 0);

INSERT INTO cms_site_category (id, tenant_id, org_id, site_id, parent_id, category_name, category_code, category_type, access_path, external_url, sort, visible_status, access_type, role_codes, seo_title, seo_keywords, seo_description, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000001102, s.tenant_id, s.org_id, s.id, 0, '账号与权限', 'account-security', 'LIST', '/account-security', NULL, 2, 'VISIBLE', 'PUBLIC', NULL, '账号与权限', 'Mango,账号,权限,安全', '账号、权限与安全策略说明。', 0, 1, NOW(), 1, NOW()
FROM cms_site s
WHERE s.site_code = 'mango-help'
  AND s.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM cms_site_category c WHERE c.tenant_id = s.tenant_id AND c.site_id = s.id AND c.category_code = 'account-security' AND c.deleted = 0);

INSERT INTO cms_site_category (id, tenant_id, org_id, site_id, parent_id, category_name, category_code, category_type, access_path, external_url, sort, visible_status, access_type, role_codes, seo_title, seo_keywords, seo_description, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000001103, s.tenant_id, s.org_id, s.id, 0, '内容管理', 'cms-guide', 'LIST', '/cms-guide', NULL, 3, 'VISIBLE', 'PUBLIC', NULL, '内容管理', 'Mango,CMS,内容管理', '站点、栏目、导航、广告与内容发布说明。', 0, 1, NOW(), 1, NOW()
FROM cms_site s
WHERE s.site_code = 'mango-help'
  AND s.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM cms_site_category c WHERE c.tenant_id = s.tenant_id AND c.site_id = s.id AND c.category_code = 'cms-guide' AND c.deleted = 0);

INSERT INTO cms_site_category (id, tenant_id, org_id, site_id, parent_id, category_name, category_code, category_type, access_path, external_url, sort, visible_status, access_type, role_codes, seo_title, seo_keywords, seo_description, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000001104, s.tenant_id, s.org_id, s.id, 0, '常见问题', 'faq', 'LIST', '/faq', NULL, 4, 'VISIBLE', 'PUBLIC', NULL, '常见问题', 'Mango,FAQ,常见问题', 'Mango 常见问题与处理建议。', 0, 1, NOW(), 1, NOW()
FROM cms_site s
WHERE s.site_code = 'mango-help'
  AND s.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM cms_site_category c WHERE c.tenant_id = s.tenant_id AND c.site_id = s.id AND c.category_code = 'faq' AND c.deleted = 0);

INSERT INTO cms_navigation (id, tenant_id, org_id, site_id, nav_type, nav_name, jump_type, category_id, content_id, external_url, open_target, sort, status, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000001201, s.tenant_id, s.org_id, s.id, 'TOP', '快速开始', 'CATEGORY', 2070000000000001101, NULL, NULL, 'SELF', 1, 'ENABLED', 0, 1, NOW(), 1, NOW()
FROM cms_site s
WHERE s.site_code = 'mango-help'
  AND s.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM cms_navigation n WHERE n.tenant_id = s.tenant_id AND n.site_id = s.id AND n.nav_type = 'TOP' AND n.nav_name = '快速开始' AND n.deleted = 0);

INSERT INTO cms_navigation (id, tenant_id, org_id, site_id, nav_type, nav_name, jump_type, category_id, content_id, external_url, open_target, sort, status, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000001202, s.tenant_id, s.org_id, s.id, 'TOP', '账号与权限', 'CATEGORY', 2070000000000001102, NULL, NULL, 'SELF', 2, 'ENABLED', 0, 1, NOW(), 1, NOW()
FROM cms_site s
WHERE s.site_code = 'mango-help'
  AND s.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM cms_navigation n WHERE n.tenant_id = s.tenant_id AND n.site_id = s.id AND n.nav_type = 'TOP' AND n.nav_name = '账号与权限' AND n.deleted = 0);

INSERT INTO cms_navigation (id, tenant_id, org_id, site_id, nav_type, nav_name, jump_type, category_id, content_id, external_url, open_target, sort, status, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000001203, s.tenant_id, s.org_id, s.id, 'TOP', '内容管理', 'CATEGORY', 2070000000000001103, NULL, NULL, 'SELF', 3, 'ENABLED', 0, 1, NOW(), 1, NOW()
FROM cms_site s
WHERE s.site_code = 'mango-help'
  AND s.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM cms_navigation n WHERE n.tenant_id = s.tenant_id AND n.site_id = s.id AND n.nav_type = 'TOP' AND n.nav_name = '内容管理' AND n.deleted = 0);

INSERT INTO cms_navigation (id, tenant_id, org_id, site_id, nav_type, nav_name, jump_type, category_id, content_id, external_url, open_target, sort, status, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000001204, s.tenant_id, s.org_id, s.id, 'TOP', '常见问题', 'CATEGORY', 2070000000000001104, NULL, NULL, 'SELF', 4, 'ENABLED', 0, 1, NOW(), 1, NOW()
FROM cms_site s
WHERE s.site_code = 'mango-help'
  AND s.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM cms_navigation n WHERE n.tenant_id = s.tenant_id AND n.site_id = s.id AND n.nav_type = 'TOP' AND n.nav_name = '常见问题' AND n.deleted = 0);

INSERT INTO cms_content (id, tenant_id, org_id, title, subtitle, summary, content_type, cover_file_id, body, external_url, attachment_file_id, video_file_id, source, author, category_id, seo_title, seo_keywords, seo_description, status, publish_time, offline_time, review_comment, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000001501, '1', 1, '如何完成首次登录', '从选择租户到进入管理后台', '首次登录时先选择租户，输入账号密码后进入工作台；如启用首次改密策略，系统会引导用户先完成密码修改。', 'ARTICLE', NULL, '<h2>首次登录流程</h2><p>选择租户后输入账号和密码，系统会校验账号状态、安全策略和登录失败次数。</p><p>如果账号被要求首次改密，请按页面提示设置符合规则的新密码。</p>', NULL, NULL, NULL, '帮助中心', 'Mango 团队', 2070000000000001101, '如何完成首次登录', 'Mango,首次登录', '首次登录、租户选择与首次改密说明。', 'PUBLISHED', NOW(), NULL, NULL, 0, 1, NOW(), 1, NOW()
WHERE EXISTS (SELECT 1 FROM cms_site_category WHERE id = 2070000000000001101 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM cms_content WHERE tenant_id = '1' AND id = 2070000000000001501 AND deleted = 0);

INSERT INTO cms_content (id, tenant_id, org_id, title, subtitle, summary, content_type, cover_file_id, body, external_url, attachment_file_id, video_file_id, source, author, category_id, seo_title, seo_keywords, seo_description, status, publish_time, offline_time, review_comment, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000001502, '1', 1, '账号锁定后如何处理', '登录失败锁定与管理员解锁', '连续登录失败达到系统参数配置的阈值后，账号会被锁定；管理员可在用户管理中执行解锁。', 'ARTICLE', NULL, '<h2>账号锁定规则</h2><p>系统会按配置统计近一段时间内的连续登录失败次数，达到阈值后锁定账号。</p><p>真实用户会落库锁定，不存在的用户名只在 KV 中记录过程数据。</p>', NULL, NULL, NULL, '帮助中心', 'Mango 团队', 2070000000000001102, '账号锁定后如何处理', 'Mango,账号锁定,解锁', '登录失败锁定和管理员解锁说明。', 'PUBLISHED', NOW(), NULL, NULL, 0, 1, NOW(), 1, NOW()
WHERE EXISTS (SELECT 1 FROM cms_site_category WHERE id = 2070000000000001102 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM cms_content WHERE tenant_id = '1' AND id = 2070000000000001502 AND deleted = 0);

INSERT INTO cms_content (id, tenant_id, org_id, title, subtitle, summary, content_type, cover_file_id, body, external_url, attachment_file_id, video_file_id, source, author, category_id, seo_title, seo_keywords, seo_description, status, publish_time, offline_time, review_comment, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000001503, '1', 1, '站点内容如何发布', '栏目、导航和内容发布关系', '内容发布前需要准备站点、栏目、导航和内容发布关系；发布后前台站点通过公开接口读取。', 'ARTICLE', NULL, '<h2>内容发布链路</h2><p>站点配置用于管理 SEO、版权和联系信息，栏目决定内容归属，导航决定前台入口。</p><p>内容状态与发布关系都满足公开条件后，前台站点即可读取。</p>', NULL, NULL, NULL, '帮助中心', 'Mango 团队', 2070000000000001103, '站点内容如何发布', 'Mango,CMS,内容发布', 'CMS 站点内容发布链路说明。', 'PUBLISHED', NOW(), NULL, NULL, 0, 1, NOW(), 1, NOW()
WHERE EXISTS (SELECT 1 FROM cms_site_category WHERE id = 2070000000000001103 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM cms_content WHERE tenant_id = '1' AND id = 2070000000000001503 AND deleted = 0);

INSERT INTO cms_content (id, tenant_id, org_id, title, subtitle, summary, content_type, cover_file_id, body, external_url, attachment_file_id, video_file_id, source, author, category_id, seo_title, seo_keywords, seo_description, status, publish_time, offline_time, review_comment, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000001504, '1', 1, '页面没有导航数据怎么办', '排查域名、站点状态和导航状态', '如果帮助中心或演示站点没有导航数据，请先确认域名解析到正确站点，再检查导航是否启用。', 'ARTICLE', NULL, '<h2>导航数据排查</h2><p>前台站点通过当前域名解析站点，再按站点读取启用状态的导航。</p><p>如果域名配置错误、站点停用或导航未启用，页面会显示为空。</p>', NULL, NULL, NULL, '帮助中心', 'Mango 团队', 2070000000000001104, '页面没有导航数据怎么办', 'Mango,导航数据,站点配置', '站点导航数据缺失的排查说明。', 'PUBLISHED', NOW(), NULL, NULL, 0, 1, NOW(), 1, NOW()
WHERE EXISTS (SELECT 1 FROM cms_site_category WHERE id = 2070000000000001104 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM cms_content WHERE tenant_id = '1' AND id = 2070000000000001504 AND deleted = 0);

INSERT INTO cms_content_publish (id, tenant_id, org_id, content_id, site_id, category_id, publish_status, publish_time, scheduled_publish_time, offline_time, top, top_scope, recommended, recommendation_type, sort, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000001601, s.tenant_id, s.org_id, 2070000000000001501, s.id, 2070000000000001101, 'PUBLISHED', NOW(), NULL, NULL, 1, 'SITE', 1, 'HELP_HOME', 1, 0, 1, NOW(), 1, NOW()
FROM cms_site s
WHERE s.site_code = 'mango-help'
  AND s.deleted = 0
  AND EXISTS (SELECT 1 FROM cms_content WHERE id = 2070000000000001501 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM cms_content_publish p WHERE p.tenant_id = s.tenant_id AND p.content_id = 2070000000000001501 AND p.site_id = s.id AND p.category_id = 2070000000000001101 AND p.deleted = 0);

INSERT INTO cms_content_publish (id, tenant_id, org_id, content_id, site_id, category_id, publish_status, publish_time, scheduled_publish_time, offline_time, top, top_scope, recommended, recommendation_type, sort, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000001602, s.tenant_id, s.org_id, 2070000000000001502, s.id, 2070000000000001102, 'PUBLISHED', NOW(), NULL, NULL, 0, NULL, 1, 'HELP_HOME', 2, 0, 1, NOW(), 1, NOW()
FROM cms_site s
WHERE s.site_code = 'mango-help'
  AND s.deleted = 0
  AND EXISTS (SELECT 1 FROM cms_content WHERE id = 2070000000000001502 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM cms_content_publish p WHERE p.tenant_id = s.tenant_id AND p.content_id = 2070000000000001502 AND p.site_id = s.id AND p.category_id = 2070000000000001102 AND p.deleted = 0);

INSERT INTO cms_content_publish (id, tenant_id, org_id, content_id, site_id, category_id, publish_status, publish_time, scheduled_publish_time, offline_time, top, top_scope, recommended, recommendation_type, sort, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000001603, s.tenant_id, s.org_id, 2070000000000001503, s.id, 2070000000000001103, 'PUBLISHED', NOW(), NULL, NULL, 0, NULL, 1, 'HELP_HOME', 3, 0, 1, NOW(), 1, NOW()
FROM cms_site s
WHERE s.site_code = 'mango-help'
  AND s.deleted = 0
  AND EXISTS (SELECT 1 FROM cms_content WHERE id = 2070000000000001503 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM cms_content_publish p WHERE p.tenant_id = s.tenant_id AND p.content_id = 2070000000000001503 AND p.site_id = s.id AND p.category_id = 2070000000000001103 AND p.deleted = 0);

INSERT INTO cms_content_publish (id, tenant_id, org_id, content_id, site_id, category_id, publish_status, publish_time, scheduled_publish_time, offline_time, top, top_scope, recommended, recommendation_type, sort, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000001604, s.tenant_id, s.org_id, 2070000000000001504, s.id, 2070000000000001104, 'PUBLISHED', NOW(), NULL, NULL, 0, NULL, 0, NULL, 4, 0, 1, NOW(), 1, NOW()
FROM cms_site s
WHERE s.site_code = 'mango-help'
  AND s.deleted = 0
  AND EXISTS (SELECT 1 FROM cms_content WHERE id = 2070000000000001504 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM cms_content_publish p WHERE p.tenant_id = s.tenant_id AND p.content_id = 2070000000000001504 AND p.site_id = s.id AND p.category_id = 2070000000000001104 AND p.deleted = 0);

INSERT INTO cms_advertisement (id, tenant_id, org_id, site_id, ad_code, ad_name, position, position_type, supported_material_types, width, height, remark, ad_type, material_file_id, jump_url, start_time, end_time, sort, status, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000001701, s.tenant_id, s.org_id, s.id, 'help-home-hero', '帮助中心首页主视觉', 'HELP_HOME_HERO', 'CUSTOM', 'TEXT', NULL, NULL, '帮助中心首页主视觉文案', 'TEXT', NULL, '#contents', NULL, NULL, 1, 'ENABLED', 0, 1, NOW(), 1, NOW()
FROM cms_site s
WHERE s.site_code = 'mango-help'
  AND s.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM cms_advertisement a WHERE a.tenant_id = s.tenant_id AND a.site_id = s.id AND a.ad_code = 'help-home-hero' AND a.deleted = 0);

INSERT INTO cms_ad_delivery (id, tenant_id, org_id, site_id, ad_id, delivery_name, material_type, title, text_content, rich_content, html_content, image_file_id, image_file_ids, video_file_id, cover_file_id, jump_url, open_target, start_time, end_time, sort, status, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000001801, s.tenant_id, s.org_id, s.id, 2070000000000001701, '帮助中心首页主视觉投放', 'TEXT', '帮助中心', '覆盖账号安全、权限配置、内容管理和常见问题，帮助团队快速完成系统验收。', NULL, NULL, NULL, NULL, NULL, NULL, '#contents', 'SELF', NULL, NULL, 1, 'ENABLED', 0, 1, NOW(), 1, NOW()
FROM cms_site s
WHERE s.site_code = 'mango-help'
  AND s.deleted = 0
  AND EXISTS (SELECT 1 FROM cms_advertisement WHERE id = 2070000000000001701 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM cms_ad_delivery d WHERE d.tenant_id = s.tenant_id AND d.site_id = s.id AND d.ad_id = 2070000000000001701 AND d.deleted = 0);

INSERT INTO cms_site (id, tenant_id, org_id, site_name, site_code, logo_file_id, description, domain, status, default_language, seo_title, seo_keywords, seo_description, footer_copyright, icp_record, contact_info, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000002001, '1', 1, 'Mango 企业官网', 'mango-enterprise', NULL, '面向企业数字化的一体化平台官网演示站点', '127.0.0.1:5191', 'ENABLED', 'zh-CN', 'Mango 企业数字化平台', 'Mango,企业官网,数字化平台', 'Mango 企业官网展示平台能力、解决方案、新闻动态与企业介绍。', '© 2026 Mango 企业官网', '企业ICP备2026000001号', '商务咨询：enterprise@example.com；服务时间：工作日 09:00-18:00', 0, 1, NOW(), 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM cms_site WHERE tenant_id = '1' AND site_code = 'mango-enterprise' AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM cms_site WHERE domain = '127.0.0.1:5191' AND deleted = 0);

INSERT INTO cms_site_setting (id, tenant_id, org_id, site_id, seo_title, seo_keywords, seo_description, footer_copyright, icp_record, contact_info, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000002901, s.tenant_id, s.org_id, s.id, s.seo_title, s.seo_keywords, s.seo_description, s.footer_copyright, s.icp_record, s.contact_info, 0, 1, NOW(), 1, NOW()
FROM cms_site s
WHERE s.site_code = 'mango-enterprise'
  AND s.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM cms_site_setting ss WHERE ss.tenant_id = s.tenant_id AND ss.site_id = s.id AND ss.deleted = 0);

INSERT INTO cms_site_category (id, tenant_id, org_id, site_id, parent_id, category_name, category_code, category_type, access_path, external_url, sort, visible_status, access_type, role_codes, seo_title, seo_keywords, seo_description, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000002101, s.tenant_id, s.org_id, s.id, 0, '产品能力', 'products', 'LIST', '/products', NULL, 1, 'VISIBLE', 'PUBLIC', NULL, '产品能力', 'Mango,产品能力', 'Mango 平台产品能力介绍。', 0, 1, NOW(), 1, NOW()
FROM cms_site s
WHERE s.site_code = 'mango-enterprise'
  AND s.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM cms_site_category c WHERE c.tenant_id = s.tenant_id AND c.site_id = s.id AND c.category_code = 'products' AND c.deleted = 0);

INSERT INTO cms_site_category (id, tenant_id, org_id, site_id, parent_id, category_name, category_code, category_type, access_path, external_url, sort, visible_status, access_type, role_codes, seo_title, seo_keywords, seo_description, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000002102, s.tenant_id, s.org_id, s.id, 0, '解决方案', 'solutions', 'LIST', '/solutions', NULL, 2, 'VISIBLE', 'PUBLIC', NULL, '解决方案', 'Mango,解决方案', '企业数字化解决方案。', 0, 1, NOW(), 1, NOW()
FROM cms_site s
WHERE s.site_code = 'mango-enterprise'
  AND s.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM cms_site_category c WHERE c.tenant_id = s.tenant_id AND c.site_id = s.id AND c.category_code = 'solutions' AND c.deleted = 0);

INSERT INTO cms_site_category (id, tenant_id, org_id, site_id, parent_id, category_name, category_code, category_type, access_path, external_url, sort, visible_status, access_type, role_codes, seo_title, seo_keywords, seo_description, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000002103, s.tenant_id, s.org_id, s.id, 0, '新闻动态', 'news', 'LIST', '/news', NULL, 3, 'VISIBLE', 'PUBLIC', NULL, '新闻动态', 'Mango,新闻动态', '产品进展与企业动态。', 0, 1, NOW(), 1, NOW()
FROM cms_site s
WHERE s.site_code = 'mango-enterprise'
  AND s.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM cms_site_category c WHERE c.tenant_id = s.tenant_id AND c.site_id = s.id AND c.category_code = 'news' AND c.deleted = 0);

INSERT INTO cms_site_category (id, tenant_id, org_id, site_id, parent_id, category_name, category_code, category_type, access_path, external_url, sort, visible_status, access_type, role_codes, seo_title, seo_keywords, seo_description, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000002104, s.tenant_id, s.org_id, s.id, 0, '关于我们', 'about', 'PAGE', '/about', NULL, 4, 'VISIBLE', 'PUBLIC', NULL, '关于我们', 'Mango,关于我们', 'Mango 团队与服务介绍。', 0, 1, NOW(), 1, NOW()
FROM cms_site s
WHERE s.site_code = 'mango-enterprise'
  AND s.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM cms_site_category c WHERE c.tenant_id = s.tenant_id AND c.site_id = s.id AND c.category_code = 'about' AND c.deleted = 0);

INSERT INTO cms_navigation (id, tenant_id, org_id, site_id, nav_type, nav_name, jump_type, category_id, content_id, external_url, open_target, sort, status, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000002201, s.tenant_id, s.org_id, s.id, 'TOP', '首页', 'URL', NULL, NULL, '/', 'SELF', 1, 'ENABLED', 0, 1, NOW(), 1, NOW()
FROM cms_site s
WHERE s.site_code = 'mango-enterprise'
  AND s.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM cms_navigation n WHERE n.tenant_id = s.tenant_id AND n.site_id = s.id AND n.nav_type = 'TOP' AND n.nav_name = '首页' AND n.deleted = 0);

INSERT INTO cms_navigation (id, tenant_id, org_id, site_id, nav_type, nav_name, jump_type, category_id, content_id, external_url, open_target, sort, status, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000002202, s.tenant_id, s.org_id, s.id, 'TOP', '产品能力', 'CATEGORY', 2070000000000002101, NULL, NULL, 'SELF', 2, 'ENABLED', 0, 1, NOW(), 1, NOW()
FROM cms_site s
WHERE s.site_code = 'mango-enterprise'
  AND s.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM cms_navigation n WHERE n.tenant_id = s.tenant_id AND n.site_id = s.id AND n.nav_type = 'TOP' AND n.nav_name = '产品能力' AND n.deleted = 0);

INSERT INTO cms_navigation (id, tenant_id, org_id, site_id, nav_type, nav_name, jump_type, category_id, content_id, external_url, open_target, sort, status, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000002203, s.tenant_id, s.org_id, s.id, 'TOP', '解决方案', 'CATEGORY', 2070000000000002102, NULL, NULL, 'SELF', 3, 'ENABLED', 0, 1, NOW(), 1, NOW()
FROM cms_site s
WHERE s.site_code = 'mango-enterprise'
  AND s.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM cms_navigation n WHERE n.tenant_id = s.tenant_id AND n.site_id = s.id AND n.nav_type = 'TOP' AND n.nav_name = '解决方案' AND n.deleted = 0);

INSERT INTO cms_navigation (id, tenant_id, org_id, site_id, nav_type, nav_name, jump_type, category_id, content_id, external_url, open_target, sort, status, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000002204, s.tenant_id, s.org_id, s.id, 'TOP', '新闻动态', 'CATEGORY', 2070000000000002103, NULL, NULL, 'SELF', 4, 'ENABLED', 0, 1, NOW(), 1, NOW()
FROM cms_site s
WHERE s.site_code = 'mango-enterprise'
  AND s.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM cms_navigation n WHERE n.tenant_id = s.tenant_id AND n.site_id = s.id AND n.nav_type = 'TOP' AND n.nav_name = '新闻动态' AND n.deleted = 0);

INSERT INTO cms_navigation (id, tenant_id, org_id, site_id, nav_type, nav_name, jump_type, category_id, content_id, external_url, open_target, sort, status, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000002205, s.tenant_id, s.org_id, s.id, 'TOP', '关于我们', 'CATEGORY', 2070000000000002104, NULL, NULL, 'SELF', 5, 'ENABLED', 0, 1, NOW(), 1, NOW()
FROM cms_site s
WHERE s.site_code = 'mango-enterprise'
  AND s.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM cms_navigation n WHERE n.tenant_id = s.tenant_id AND n.site_id = s.id AND n.nav_type = 'TOP' AND n.nav_name = '关于我们' AND n.deleted = 0);

INSERT INTO cms_navigation (id, tenant_id, org_id, site_id, nav_type, nav_name, jump_type, category_id, content_id, external_url, open_target, sort, status, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000002301, s.tenant_id, s.org_id, s.id, 'FOOTER', '产品能力', 'CATEGORY', 2070000000000002101, NULL, NULL, 'SELF', 1, 'ENABLED', 0, 1, NOW(), 1, NOW()
FROM cms_site s
WHERE s.site_code = 'mango-enterprise'
  AND s.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM cms_navigation n WHERE n.tenant_id = s.tenant_id AND n.site_id = s.id AND n.nav_type = 'FOOTER' AND n.nav_name = '产品能力' AND n.deleted = 0);

INSERT INTO cms_navigation (id, tenant_id, org_id, site_id, nav_type, nav_name, jump_type, category_id, content_id, external_url, open_target, sort, status, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000002302, s.tenant_id, s.org_id, s.id, 'FOOTER', '解决方案', 'CATEGORY', 2070000000000002102, NULL, NULL, 'SELF', 2, 'ENABLED', 0, 1, NOW(), 1, NOW()
FROM cms_site s
WHERE s.site_code = 'mango-enterprise'
  AND s.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM cms_navigation n WHERE n.tenant_id = s.tenant_id AND n.site_id = s.id AND n.nav_type = 'FOOTER' AND n.nav_name = '解决方案' AND n.deleted = 0);

INSERT INTO cms_navigation (id, tenant_id, org_id, site_id, nav_type, nav_name, jump_type, category_id, content_id, external_url, open_target, sort, status, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000002303, s.tenant_id, s.org_id, s.id, 'FOOTER', '新闻动态', 'CATEGORY', 2070000000000002103, NULL, NULL, 'SELF', 3, 'ENABLED', 0, 1, NOW(), 1, NOW()
FROM cms_site s
WHERE s.site_code = 'mango-enterprise'
  AND s.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM cms_navigation n WHERE n.tenant_id = s.tenant_id AND n.site_id = s.id AND n.nav_type = 'FOOTER' AND n.nav_name = '新闻动态' AND n.deleted = 0);

INSERT INTO cms_navigation (id, tenant_id, org_id, site_id, nav_type, nav_name, jump_type, category_id, content_id, external_url, open_target, sort, status, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000002304, s.tenant_id, s.org_id, s.id, 'FOOTER', '关于我们', 'CATEGORY', 2070000000000002104, NULL, NULL, 'SELF', 4, 'ENABLED', 0, 1, NOW(), 1, NOW()
FROM cms_site s
WHERE s.site_code = 'mango-enterprise'
  AND s.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM cms_navigation n WHERE n.tenant_id = s.tenant_id AND n.site_id = s.id AND n.nav_type = 'FOOTER' AND n.nav_name = '关于我们' AND n.deleted = 0);

INSERT INTO cms_content (id, tenant_id, org_id, title, subtitle, summary, content_type, cover_file_id, body, external_url, attachment_file_id, video_file_id, source, author, category_id, seo_title, seo_keywords, seo_description, status, publish_time, offline_time, review_comment, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000002501, '1', 1, 'Mango Admin 提供统一运营后台', '统一权限、菜单和业务应用入口', 'Mango Admin 将权限、菜单、工作流、内容管理和消息能力整合到统一管理后台。', 'ARTICLE', NULL, '<h2>统一运营后台</h2><p>Mango Admin 提供统一权限、菜单、业务应用入口和可扩展的模块能力。</p>', NULL, NULL, NULL, '企业官网', 'Mango 团队', 2070000000000002101, 'Mango Admin 提供统一运营后台', 'Mango Admin,运营后台', '统一运营后台能力介绍。', 'PUBLISHED', NOW(), NULL, NULL, 0, 1, NOW(), 1, NOW()
WHERE EXISTS (SELECT 1 FROM cms_site_category WHERE id = 2070000000000002101 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM cms_content WHERE tenant_id = '1' AND id = 2070000000000002501 AND deleted = 0);

INSERT INTO cms_content (id, tenant_id, org_id, title, subtitle, summary, content_type, cover_file_id, body, external_url, attachment_file_id, video_file_id, source, author, category_id, seo_title, seo_keywords, seo_description, status, publish_time, offline_time, review_comment, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000002502, '1', 1, '企业内容运营解决方案上线', '多站点、栏目、导航和广告统一管理', '通过 CMS 能力，企业可统一维护官网、帮助中心和活动页面。', 'ARTICLE', NULL, '<h2>内容运营解决方案</h2><p>CMS 支持站点、栏目、导航、广告位和发布内容的统一管理。</p>', NULL, NULL, NULL, '企业官网', 'Mango 团队', 2070000000000002102, '企业内容运营解决方案上线', 'Mango CMS,内容运营', '企业内容运营解决方案。', 'PUBLISHED', NOW(), NULL, NULL, 0, 1, NOW(), 1, NOW()
WHERE EXISTS (SELECT 1 FROM cms_site_category WHERE id = 2070000000000002102 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM cms_content WHERE tenant_id = '1' AND id = 2070000000000002502 AND deleted = 0);

INSERT INTO cms_content (id, tenant_id, org_id, title, subtitle, summary, content_type, cover_file_id, body, external_url, attachment_file_id, video_file_id, source, author, category_id, seo_title, seo_keywords, seo_description, status, publish_time, offline_time, review_comment, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000002503, '1', 1, 'Mango 安全策略基线完成升级', '首次改密、复杂度校验和登录失败锁定', '账号安全策略支持系统参数配置，覆盖首次改密、密码复杂度和登录失败锁定。', 'ARTICLE', NULL, '<h2>安全策略基线</h2><p>系统参数可控制首次改密、密码复杂度规则和登录失败锁定策略。</p>', NULL, NULL, NULL, '企业官网', 'Mango 团队', 2070000000000002103, 'Mango 安全策略基线完成升级', 'Mango,安全策略', '账号安全策略升级动态。', 'PUBLISHED', NOW(), NULL, NULL, 0, 1, NOW(), 1, NOW()
WHERE EXISTS (SELECT 1 FROM cms_site_category WHERE id = 2070000000000002103 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM cms_content WHERE tenant_id = '1' AND id = 2070000000000002503 AND deleted = 0);

INSERT INTO cms_content (id, tenant_id, org_id, title, subtitle, summary, content_type, cover_file_id, body, external_url, attachment_file_id, video_file_id, source, author, category_id, seo_title, seo_keywords, seo_description, status, publish_time, offline_time, review_comment, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000002504, '1', 1, '关于 Mango 团队', '专注企业级应用底座与业务中台', 'Mango 团队持续建设企业级应用底座，提供后台、工作流、内容管理和通知能力。', 'ARTICLE', NULL, '<h2>关于 Mango</h2><p>Mango 专注企业级应用底座与业务中台能力建设。</p>', NULL, NULL, NULL, '企业官网', 'Mango 团队', 2070000000000002104, '关于 Mango 团队', 'Mango,关于我们', 'Mango 团队介绍。', 'PUBLISHED', NOW(), NULL, NULL, 0, 1, NOW(), 1, NOW()
WHERE EXISTS (SELECT 1 FROM cms_site_category WHERE id = 2070000000000002104 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM cms_content WHERE tenant_id = '1' AND id = 2070000000000002504 AND deleted = 0);

INSERT INTO cms_content_publish (id, tenant_id, org_id, content_id, site_id, category_id, publish_status, publish_time, scheduled_publish_time, offline_time, top, top_scope, recommended, recommendation_type, sort, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000002601, s.tenant_id, s.org_id, 2070000000000002501, s.id, 2070000000000002101, 'PUBLISHED', NOW(), NULL, NULL, 1, 'SITE', 1, 'HOME', 1, 0, 1, NOW(), 1, NOW()
FROM cms_site s
WHERE s.site_code = 'mango-enterprise'
  AND s.deleted = 0
  AND EXISTS (SELECT 1 FROM cms_content WHERE id = 2070000000000002501 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM cms_content_publish p WHERE p.tenant_id = s.tenant_id AND p.content_id = 2070000000000002501 AND p.site_id = s.id AND p.category_id = 2070000000000002101 AND p.deleted = 0);

INSERT INTO cms_content_publish (id, tenant_id, org_id, content_id, site_id, category_id, publish_status, publish_time, scheduled_publish_time, offline_time, top, top_scope, recommended, recommendation_type, sort, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000002602, s.tenant_id, s.org_id, 2070000000000002502, s.id, 2070000000000002102, 'PUBLISHED', NOW(), NULL, NULL, 0, NULL, 1, 'HOME', 2, 0, 1, NOW(), 1, NOW()
FROM cms_site s
WHERE s.site_code = 'mango-enterprise'
  AND s.deleted = 0
  AND EXISTS (SELECT 1 FROM cms_content WHERE id = 2070000000000002502 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM cms_content_publish p WHERE p.tenant_id = s.tenant_id AND p.content_id = 2070000000000002502 AND p.site_id = s.id AND p.category_id = 2070000000000002102 AND p.deleted = 0);

INSERT INTO cms_content_publish (id, tenant_id, org_id, content_id, site_id, category_id, publish_status, publish_time, scheduled_publish_time, offline_time, top, top_scope, recommended, recommendation_type, sort, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000002603, s.tenant_id, s.org_id, 2070000000000002503, s.id, 2070000000000002103, 'PUBLISHED', NOW(), NULL, NULL, 0, NULL, 1, 'HOME', 3, 0, 1, NOW(), 1, NOW()
FROM cms_site s
WHERE s.site_code = 'mango-enterprise'
  AND s.deleted = 0
  AND EXISTS (SELECT 1 FROM cms_content WHERE id = 2070000000000002503 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM cms_content_publish p WHERE p.tenant_id = s.tenant_id AND p.content_id = 2070000000000002503 AND p.site_id = s.id AND p.category_id = 2070000000000002103 AND p.deleted = 0);

INSERT INTO cms_content_publish (id, tenant_id, org_id, content_id, site_id, category_id, publish_status, publish_time, scheduled_publish_time, offline_time, top, top_scope, recommended, recommendation_type, sort, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000002604, s.tenant_id, s.org_id, 2070000000000002504, s.id, 2070000000000002104, 'PUBLISHED', NOW(), NULL, NULL, 0, NULL, 0, NULL, 4, 0, 1, NOW(), 1, NOW()
FROM cms_site s
WHERE s.site_code = 'mango-enterprise'
  AND s.deleted = 0
  AND EXISTS (SELECT 1 FROM cms_content WHERE id = 2070000000000002504 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM cms_content_publish p WHERE p.tenant_id = s.tenant_id AND p.content_id = 2070000000000002504 AND p.site_id = s.id AND p.category_id = 2070000000000002104 AND p.deleted = 0);

INSERT INTO cms_advertisement (id, tenant_id, org_id, site_id, ad_code, ad_name, position, position_type, supported_material_types, width, height, remark, ad_type, material_file_id, jump_url, start_time, end_time, sort, status, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000002701, s.tenant_id, s.org_id, s.id, 'enterprise-home-hero', '企业官网首页主视觉', 'HOME_HERO', 'BANNER', 'TEXT', NULL, NULL, '企业官网首页主视觉', 'TEXT', NULL, '#products', NULL, NULL, 1, 'ENABLED', 0, 1, NOW(), 1, NOW()
FROM cms_site s
WHERE s.site_code = 'mango-enterprise'
  AND s.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM cms_advertisement a WHERE a.tenant_id = s.tenant_id AND a.site_id = s.id AND a.ad_code = 'enterprise-home-hero' AND a.deleted = 0);

INSERT INTO cms_ad_delivery (id, tenant_id, org_id, site_id, ad_id, delivery_name, material_type, title, text_content, rich_content, html_content, image_file_id, image_file_ids, video_file_id, cover_file_id, jump_url, open_target, start_time, end_time, sort, status, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000002801, s.tenant_id, s.org_id, s.id, 2070000000000002701, '企业官网首页主视觉投放', 'TEXT', 'Mango 企业数字化平台', '统一后台、流程引擎、内容运营和通知中心，支撑企业应用快速交付。', NULL, NULL, NULL, NULL, NULL, NULL, '#products', 'SELF', NULL, NULL, 1, 'ENABLED', 0, 1, NOW(), 1, NOW()
FROM cms_site s
WHERE s.site_code = 'mango-enterprise'
  AND s.deleted = 0
  AND EXISTS (SELECT 1 FROM cms_advertisement WHERE id = 2070000000000002701 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM cms_ad_delivery d WHERE d.tenant_id = s.tenant_id AND d.site_id = s.id AND d.ad_id = 2070000000000002701 AND d.deleted = 0);

INSERT INTO cms_advertisement (id, tenant_id, org_id, site_id, ad_code, ad_name, position, position_type, supported_material_types, width, height, remark, ad_type, material_file_id, jump_url, start_time, end_time, sort, status, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000002702, s.tenant_id, s.org_id, s.id, 'enterprise-home-float', '企业官网首页运营公告', 'HOME_FLOAT', 'CUSTOM', 'TEXT', NULL, NULL, '企业官网首页运营公告', 'TEXT', NULL, '/news', NULL, NULL, 2, 'ENABLED', 0, 1, NOW(), 1, NOW()
FROM cms_site s
WHERE s.site_code = 'mango-enterprise'
  AND s.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM cms_advertisement a WHERE a.tenant_id = s.tenant_id AND a.site_id = s.id AND a.ad_code = 'enterprise-home-float' AND a.deleted = 0);

INSERT INTO cms_ad_delivery (id, tenant_id, org_id, site_id, ad_id, delivery_name, material_type, title, text_content, rich_content, html_content, image_file_id, image_file_ids, video_file_id, cover_file_id, jump_url, open_target, start_time, end_time, sort, status, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000002802, s.tenant_id, s.org_id, s.id, 2070000000000002702, '企业官网首页运营公告投放', 'TEXT', '安全策略基线已上线', '首次改密、复杂度规则和登录失败锁定已支持系统参数配置。', NULL, NULL, NULL, NULL, NULL, NULL, '/news', 'SELF', NULL, NULL, 1, 'ENABLED', 0, 1, NOW(), 1, NOW()
FROM cms_site s
WHERE s.site_code = 'mango-enterprise'
  AND s.deleted = 0
  AND EXISTS (SELECT 1 FROM cms_advertisement WHERE id = 2070000000000002702 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM cms_ad_delivery d WHERE d.tenant_id = s.tenant_id AND d.site_id = s.id AND d.ad_id = 2070000000000002702 AND d.deleted = 0);
