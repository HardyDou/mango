ALTER TABLE `identity_user`
  ADD COLUMN `password_reset_required` tinyint NOT NULL DEFAULT '0' COMMENT '是否要求下次登录修改密码' AFTER `password`,
  ADD COLUMN `password_updated_at` datetime DEFAULT NULL COMMENT '最近密码更新时间' AFTER `password_reset_required`,
  ADD COLUMN `failed_login_count` int NOT NULL DEFAULT '0' COMMENT '连续登录失败次数' AFTER `last_login_time`,
  ADD COLUMN `locked_until` datetime DEFAULT NULL COMMENT '账号锁定截止时间' AFTER `failed_login_count`,
  ADD COLUMN `locked_reason` varchar(200) DEFAULT NULL COMMENT '账号锁定原因' AFTER `locked_until`;

CREATE INDEX `idx_identity_user_locked_until` ON `identity_user` (`locked_until`);
