INSERT INTO `sys_org`
    (`id`, `tenant_id`, `pid`, `org_name`, `org_code`, `org_type`, `org_sort`, `org_status`)
SELECT CAST(CONV(SUBSTRING(MD5(CONCAT('default-root-org:', `t`.`id`)), 1, 15), 16, 10) AS UNSIGNED),
       `t`.`id`,
       0,
       `t`.`tenant_name`,
       CONCAT(UPPER(`t`.`tenant_code`), '_ROOT'),
       2,
       0,
       '1'
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

UPDATE `tenant_member` `m`
JOIN `sys_org` `o`
  ON `o`.`tenant_id` = `m`.`tenant_id`
 AND `o`.`pid` = 0
SET `m`.`primary_org_id` = `o`.`id`
WHERE `m`.`tenant_id` IN (2, 3, 4)
  AND `m`.`primary_org_id` IS NULL;

UPDATE `tenant_member` `m`
JOIN `sys_tenant` `t`
  ON `t`.`id` = `m`.`tenant_id`
JOIN `org_post` `p`
  ON `p`.`tenant_id` = `m`.`tenant_id`
 AND `p`.`post_code` = CONCAT(UPPER(`t`.`tenant_code`), '_TENANT_ADMIN')
SET `m`.`primary_post_id` = `p`.`id`
WHERE `m`.`tenant_id` IN (2, 3, 4)
  AND `m`.`member_type` = 'TENANT_ADMIN'
  AND `m`.`primary_post_id` IS NULL;
