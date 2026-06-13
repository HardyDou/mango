ALTER TABLE `payment_application`
  ADD COLUMN `ip_whitelist_enabled` tinyint NOT NULL DEFAULT '0' COMMENT 'IP 白名单开关',
  MODIFY COLUMN `sign_algorithm` varchar(32) DEFAULT NULL COMMENT '签名算法';

UPDATE `payment_application`
SET `ip_whitelist_enabled` = CASE WHEN `ip_whitelist` IS NULL OR `ip_whitelist` = '' THEN 0 ELSE 1 END,
    `sign_algorithm` = CASE WHEN `payload_encrypt_enabled` = 1 THEN COALESCE(NULLIF(`sign_algorithm`, ''), 'HMAC_SHA256') ELSE NULL END;

ALTER TABLE `payment_cashier_config`
  MODIFY COLUMN `enterprise_subject_id` bigint DEFAULT NULL COMMENT '历史字段：收银台允许多个企业主体',
  MODIFY COLUMN `terminal_type` varchar(32) DEFAULT NULL COMMENT '历史字段：收银台终端由前端组件能力决定',
  MODIFY COLUMN `expire_minutes` int DEFAULT NULL COMMENT '历史字段：支付超时使用平台统一配置',
  ADD COLUMN `default_cashier` tinyint NOT NULL DEFAULT '0' COMMENT '是否默认收银台',
  ADD COLUMN `enterprise_subject_ids` varchar(1024) DEFAULT NULL COMMENT '允许企业主体 ID，逗号分隔',
  ADD COLUMN `refund_enabled` tinyint NOT NULL DEFAULT '1' COMMENT '是否允许退款',
  ADD COLUMN `partial_refund_enabled` tinyint NOT NULL DEFAULT '1' COMMENT '是否允许部分退款';

UPDATE `payment_cashier_config`
SET `enterprise_subject_ids` = CAST(`enterprise_subject_id` AS CHAR)
WHERE (`enterprise_subject_ids` IS NULL OR `enterprise_subject_ids` = '')
  AND `enterprise_subject_id` IS NOT NULL;

UPDATE `payment_cashier_config`
SET `default_cashier` = CASE WHEN `id` = 350001 THEN 1 ELSE 0 END
WHERE `application_id` = 310001;

UPDATE `payment_cashier_config`
SET `display_config` = COALESCE(`display_config`, `theme_config`),
    `refund_enabled` = 1,
    `partial_refund_enabled` = 1
WHERE `tenant_id` = 1;
