ALTER TABLE `payment_application`
  DROP COLUMN `timestamp_tolerance_seconds`,
  DROP COLUMN `nonce_replay_window_seconds`,
  DROP COLUMN `enabled_subject_ids`,
  DROP COLUMN `enabled_method_codes`,
  DROP COLUMN `refund_enabled`,
  DROP COLUMN `partial_refund_enabled`,
  DROP COLUMN `default_cashier_id`;

ALTER TABLE `payment_cashier_config`
  DROP INDEX `idx_payment_cashier_tenant_app`,
  DROP COLUMN `enterprise_subject_id`,
  DROP COLUMN `terminal_type`,
  DROP COLUMN `method_ids`,
  DROP COLUMN `default_method_id`,
  DROP COLUMN `expire_minutes`,
  DROP COLUMN `app_scope`,
  DROP COLUMN `subject_scope`,
  DROP COLUMN `terminal_scope`,
  DROP COLUMN `theme_config`,
  DROP COLUMN `layout_config`,
  DROP COLUMN `timeout_config`,
  DROP COLUMN `result_config`,
  DROP COLUMN `bank_display_config`,
  DROP COLUMN `offline_transfer_config`,
  ADD KEY `idx_payment_cashier_tenant_app` (`tenant_id`, `application_id`);
