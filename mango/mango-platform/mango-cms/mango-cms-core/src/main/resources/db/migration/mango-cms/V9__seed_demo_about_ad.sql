-- 为演示站点"关于我们"页添加广告投放 Banner。
-- 复用文件中心已存在图片作为素材，按文件存在性守卫；无图时跳过。

INSERT INTO cms_advertisement (id, tenant_id, org_id, site_id, ad_code, ad_name, position, position_type, supported_material_types, width, height, remark, ad_type, material_file_id, jump_url, start_time, end_time, sort, status, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000000701, '1', NULL, 2070000000000000001, 'demo-about-hero', '关于我们页 Banner', 'ABOUT_HERO', 'BANNER', 'SINGLE_IMAGE', NULL, NULL, '关于我们页主视觉广告位', NULL, NULL, '/about', NULL, NULL, 1, 'ENABLED', 0, 0, NOW(), 0, NOW()
WHERE EXISTS (SELECT 1 FROM cms_site WHERE id = 2070000000000000001 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM cms_advertisement WHERE tenant_id = '1' AND site_id = 2070000000000000001 AND ad_code = 'demo-about-hero' AND deleted = 0);

INSERT INTO cms_ad_delivery (id, tenant_id, org_id, site_id, ad_id, delivery_name, material_type, title, text_content, rich_content, html_content, image_file_id, image_file_ids, video_file_id, cover_file_id, jump_url, open_target, start_time, end_time, sort, status, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000000801, '1', NULL, 2070000000000000001, 2070000000000000701, '关于我们页 Banner 投放', 'SINGLE_IMAGE', '让每一次发布可信可溯', '演示站点是基于 Mango CMS 的内容管理平台，提供多站点管理、内容发布、审核与数据穿透能力。', NULL, NULL,
    (SELECT f.id FROM file_record f WHERE f.status = 1 AND f.content_type LIKE 'image%' ORDER BY f.id DESC LIMIT 1 OFFSET 2),
    NULL, NULL, NULL, '/about', 'SELF', NULL, NULL, 1, 'ENABLED', 0, 0, NOW(), 0, NOW()
WHERE EXISTS (SELECT 1 FROM cms_advertisement WHERE id = 2070000000000000701 AND deleted = 0)
  AND EXISTS (SELECT 1 FROM file_record f WHERE f.status = 1 AND f.content_type LIKE 'image%' LIMIT 1 OFFSET 2)
  AND NOT EXISTS (SELECT 1 FROM cms_ad_delivery WHERE tenant_id = '1' AND site_id = 2070000000000000001 AND ad_id = 2070000000000000701 AND deleted = 0);
