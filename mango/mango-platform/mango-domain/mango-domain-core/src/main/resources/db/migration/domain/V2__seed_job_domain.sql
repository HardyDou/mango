INSERT INTO `biz_domain`
  (`id`, `tenant_id`, `domain_code`, `domain_short_code`, `domain_name`, `parent_id`, `sort`, `status`,
   `remark`, `create_time`, `update_time`, `created_by`, `created_at`, `updated_by`, `updated_at`, `deleted`)
VALUES
  (170, '1', 'JOB', 'JOB', '定时任务', 0, 8, 1, '定时任务业务域',
   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, 0)
ON DUPLICATE KEY UPDATE
  `domain_short_code` = VALUES(`domain_short_code`),
  `domain_name` = VALUES(`domain_name`),
  `parent_id` = VALUES(`parent_id`),
  `sort` = VALUES(`sort`),
  `status` = VALUES(`status`),
  `remark` = VALUES(`remark`),
  `deleted` = 0,
  `update_time` = CURRENT_TIMESTAMP,
  `updated_at` = CURRENT_TIMESTAMP;
