INSERT INTO `sys_org`
    (`id`, `tenant_id`, `pid`, `org_name`, `org_code`, `org_type`, `org_sort`, `org_status`)
SELECT `t`.`id`, `t`.`id`, 0, `t`.`tenant_name`, CONCAT(UPPER(`t`.`tenant_code`), '_ROOT'), 2, 0, '1'
FROM `sys_tenant` `t`
LEFT JOIN `sys_org` `existing`
  ON `existing`.`tenant_id` = `t`.`id`
 AND `existing`.`pid` = 0
WHERE `t`.`id` IN (2, 3, 4)
  AND `existing`.`id` IS NULL
ON DUPLICATE KEY UPDATE
    `org_name` = VALUES(`org_name`),
    `org_type` = VALUES(`org_type`),
    `org_sort` = VALUES(`org_sort`),
    `org_status` = VALUES(`org_status`);

INSERT INTO `org_post`
    (`id`, `tenant_id`, `post_name`, `post_code`, `post_sort`, `post_status`, `remark`)
SELECT CAST(CONV(SUBSTRING(MD5(CONCAT('default-post:', `t`.`id`, ':TENANT_ADMIN')), 1, 15), 16, 10) AS UNSIGNED),
       `t`.`id`, '租户管理员', CONCAT(UPPER(`t`.`tenant_code`), '_TENANT_ADMIN'), 1, '1', '租户默认管理员岗位'
FROM `sys_tenant` `t`
WHERE `t`.`id` IN (2, 3, 4)
ON DUPLICATE KEY UPDATE
    `post_name` = VALUES(`post_name`),
    `post_sort` = VALUES(`post_sort`),
    `post_status` = VALUES(`post_status`),
    `remark` = VALUES(`remark`);

INSERT INTO `org_post`
    (`id`, `tenant_id`, `post_name`, `post_code`, `post_sort`, `post_status`, `remark`)
SELECT CAST(CONV(SUBSTRING(MD5(CONCAT('default-post:', `t`.`id`, ':ORG_MANAGER')), 1, 15), 16, 10) AS UNSIGNED),
       `t`.`id`, '组织负责人', CONCAT(UPPER(`t`.`tenant_code`), '_ORG_MANAGER'), 2, '1', '租户默认组织管理岗位'
FROM `sys_tenant` `t`
WHERE `t`.`id` IN (2, 3, 4)
ON DUPLICATE KEY UPDATE
    `post_name` = VALUES(`post_name`),
    `post_sort` = VALUES(`post_sort`),
    `post_status` = VALUES(`post_status`),
    `remark` = VALUES(`remark`);

INSERT INTO `org_post`
    (`id`, `tenant_id`, `post_name`, `post_code`, `post_sort`, `post_status`, `remark`)
SELECT CAST(CONV(SUBSTRING(MD5(CONCAT('default-post:', `t`.`id`, ':EMPLOYEE')), 1, 15), 16, 10) AS UNSIGNED),
       `t`.`id`, '普通员工', CONCAT(UPPER(`t`.`tenant_code`), '_EMPLOYEE'), 3, '1', '租户默认员工岗位'
FROM `sys_tenant` `t`
WHERE `t`.`id` IN (2, 3, 4)
ON DUPLICATE KEY UPDATE
    `post_name` = VALUES(`post_name`),
    `post_sort` = VALUES(`post_sort`),
    `post_status` = VALUES(`post_status`),
    `remark` = VALUES(`remark`);
