INSERT INTO link_category (
    id, tenant_id, scope, owner_user_id, name, sort_no, status, remark, created_by, updated_by
)
SELECT 2026070103060100001, 1, 'COMPANY', 0, '企业导航', 0, 'ENABLED', '系统内置企业导航分组', 1, 1
WHERE NOT EXISTS (
    SELECT 1 FROM link_category
    WHERE tenant_id = 1 AND scope = 'COMPANY' AND owner_user_id = 0 AND name = '企业导航'
);
