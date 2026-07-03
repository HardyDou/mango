INSERT INTO link_category (
    id, tenant_id, scope, owner_user_id, name, sort_no, status, remark, created_by, updated_by
)
SELECT 2026070103060100001, 1, 'COMPANY', 0, '企业导航', 0, 'ENABLED', '系统内置企业导航分组', 1, 1
WHERE NOT EXISTS (
    SELECT 1 FROM link_category WHERE id = 2026070103060100001
);

INSERT INTO link_item (
    id, tenant_id, category_id, name, url, summary, icon_url, tags,
    visibility_scope, owner_user_id, open_mode, recommended, sort_no, status,
    remark, created_by, updated_by
)
SELECT
    2026070103060200001, 1, 2026070103060100001, 'Mango 管理后台', 'http://127.0.0.1:30002',
    'Mango 内部管理后台', NULL, 'mango,管理后台',
    'COMPANY', 0, 'NEW_WINDOW', 1, 0, 'ENABLED',
    '系统内置企业导航网址', 1, 1
WHERE NOT EXISTS (
    SELECT 1 FROM link_item WHERE id = 2026070103060200001
);

INSERT INTO link_item (
    id, tenant_id, category_id, name, url, summary, icon_url, tags,
    visibility_scope, owner_user_id, open_mode, recommended, sort_no, status,
    remark, created_by, updated_by
)
SELECT
    2026070103060200002, 1, 2026070103060100001, '百度', 'https://www.baidu.com',
    '百度搜索', NULL, '搜索,常用',
    'PUBLIC', 0, 'NEW_WINDOW', 0, 10, 'ENABLED',
    '系统内置公开网址', 1, 1
WHERE NOT EXISTS (
    SELECT 1 FROM link_item WHERE id = 2026070103060200002
);

INSERT INTO link_item (
    id, tenant_id, category_id, name, url, summary, icon_url, tags,
    visibility_scope, owner_user_id, open_mode, recommended, sort_no, status,
    remark, created_by, updated_by
)
SELECT
    2026070103060200003, 1, 2026070103060100001, 'GitHub', 'https://github.com',
    '代码托管与开源协作平台', NULL, '研发,常用',
    'PUBLIC', 0, 'NEW_WINDOW', 0, 20, 'ENABLED',
    '系统内置公开网址', 1, 1
WHERE NOT EXISTS (
    SELECT 1 FROM link_item WHERE id = 2026070103060200003
);

INSERT INTO link_item (
    id, tenant_id, category_id, name, url, summary, icon_url, tags,
    visibility_scope, owner_user_id, open_mode, recommended, sort_no, status,
    remark, created_by, updated_by
)
SELECT
    2026070103060200004, 1, 2026070103060100001, 'Maven Central', 'https://central.sonatype.com',
    'Maven 依赖检索', NULL, '研发,常用',
    'PUBLIC', 0, 'NEW_WINDOW', 0, 30, 'ENABLED',
    '系统内置公开网址', 1, 1
WHERE NOT EXISTS (
    SELECT 1 FROM link_item WHERE id = 2026070103060200004
);

INSERT INTO link_favorite (
    id, tenant_id, user_id, link_id, created_by, updated_by
)
SELECT 2026070103060300001, 1, 1, 2026070103060200002, 1, 1
WHERE NOT EXISTS (
    SELECT 1 FROM link_favorite
    WHERE tenant_id = 1 AND user_id = 1 AND link_id = 2026070103060200002
);

INSERT INTO link_favorite (
    id, tenant_id, user_id, link_id, created_by, updated_by
)
SELECT 2026070103060300002, 1, 1, 2026070103060200003, 1, 1
WHERE NOT EXISTS (
    SELECT 1 FROM link_favorite
    WHERE tenant_id = 1 AND user_id = 1 AND link_id = 2026070103060200003
);

INSERT INTO link_favorite (
    id, tenant_id, user_id, link_id, created_by, updated_by
)
SELECT 2026070103060300003, 1, 1, 2026070103060200004, 1, 1
WHERE NOT EXISTS (
    SELECT 1 FROM link_favorite
    WHERE tenant_id = 1 AND user_id = 1 AND link_id = 2026070103060200004
);
