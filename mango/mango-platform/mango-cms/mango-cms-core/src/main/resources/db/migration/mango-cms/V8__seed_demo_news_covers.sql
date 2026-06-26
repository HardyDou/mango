-- 为演示站点新闻内容补充封面图，并新增新闻列表页 HERO Banner。
-- 封面与 Banner 复用文件中心已存在的图片文件，按文件存在性守卫；
-- 环境中无可用图片时跳过，前台回退到渐变占位，不产生脏数据。

UPDATE cms_content
SET cover_file_id = (
    SELECT f.id FROM file_record f
    WHERE f.status = 1 AND f.content_type LIKE 'image%'
    ORDER BY f.id DESC LIMIT 1 OFFSET 0
)
WHERE id = 2070000000000000501
  AND cover_file_id IS NULL
  AND EXISTS (SELECT 1 FROM file_record f WHERE f.status = 1 AND f.content_type LIKE 'image%' LIMIT 1 OFFSET 0);

UPDATE cms_content
SET cover_file_id = (
    SELECT f.id FROM file_record f
    WHERE f.status = 1 AND f.content_type LIKE 'image%'
    ORDER BY f.id DESC LIMIT 1 OFFSET 1
)
WHERE id = 2070000000000000502
  AND cover_file_id IS NULL
  AND EXISTS (SELECT 1 FROM file_record f WHERE f.status = 1 AND f.content_type LIKE 'image%' LIMIT 1 OFFSET 1);

UPDATE cms_content
SET cover_file_id = (
    SELECT f.id FROM file_record f
    WHERE f.status = 1 AND f.content_type LIKE 'image%'
    ORDER BY f.id DESC LIMIT 1 OFFSET 2
)
WHERE id = 2070000000000000503
  AND cover_file_id IS NULL
  AND EXISTS (SELECT 1 FROM file_record f WHERE f.status = 1 AND f.content_type LIKE 'image%' LIMIT 1 OFFSET 2);

INSERT INTO cms_banner (id, tenant_id, org_id, site_id, position, title, subtitle, media_type, media_file_id, jump_url, start_time, end_time, sort, status, deleted, created_by, created_at, updated_by, updated_at)
SELECT 2070000000000000402, '1', NULL, 2070000000000000001, 'NEWS_HERO', '新闻动态', '了解演示站点的最新进展与行业洞察', 'IMAGE',
    (SELECT f.id FROM file_record f WHERE f.status = 1 AND f.content_type LIKE 'image%' ORDER BY f.id DESC LIMIT 1),
    '/news', NULL, NULL, 1, 'ENABLED', 0, 0, NOW(), 0, NOW()
WHERE EXISTS (SELECT 1 FROM cms_site WHERE id = 2070000000000000001 AND deleted = 0)
  AND EXISTS (SELECT 1 FROM file_record f WHERE f.status = 1 AND f.content_type LIKE 'image%' LIMIT 1)
  AND NOT EXISTS (SELECT 1 FROM cms_banner WHERE tenant_id = '1' AND site_id = 2070000000000000001 AND position = 'NEWS_HERO' AND deleted = 0);
