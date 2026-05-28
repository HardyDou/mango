INSERT INTO `notice_recipient_account`
(`id`, `user_id`, `account_type`, `account_value`, `display_name`, `verified_status`, `default_account`, `enabled`,
 `tenant_id`, `created_by`, `created_at`, `updated_by`, `updated_at`)
VALUES
(2060000000000004001, 1, 'EMAIL', '1012404303@qq.com', 'admin邮箱', 'VERIFIED', 1, 1,
 '1', 1, NOW(), 1, NOW()),
(2060000000000004002, 1, 'MOBILE', '18701445644', 'admin手机号', 'VERIFIED', 1, 1,
 '1', 1, NOW(), 1, NOW())
ON DUPLICATE KEY UPDATE
  `display_name` = VALUES(`display_name`),
  `verified_status` = VALUES(`verified_status`),
  `default_account` = VALUES(`default_account`),
  `enabled` = VALUES(`enabled`),
  `updated_by` = VALUES(`updated_by`),
  `updated_at` = VALUES(`updated_at`);
