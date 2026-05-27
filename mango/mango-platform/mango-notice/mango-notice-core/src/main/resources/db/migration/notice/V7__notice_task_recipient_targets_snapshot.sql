ALTER TABLE `notice_task`
  ADD COLUMN `recipient_targets_snapshot` text DEFAULT NULL COMMENT '接收目标快照 JSON' AFTER `params_snapshot`;
