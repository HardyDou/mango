ALTER TABLE `identity_user`
  ADD COLUMN `last_failed_login_at` datetime DEFAULT NULL COMMENT '最近登录失败时间' AFTER `failed_login_count`;

CREATE INDEX `idx_identity_user_last_failed_login_at` ON `identity_user` (`last_failed_login_at`);
