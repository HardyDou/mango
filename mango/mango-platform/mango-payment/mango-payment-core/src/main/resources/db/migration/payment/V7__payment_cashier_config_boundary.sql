ALTER TABLE `payment_cashier_config`
  ADD COLUMN `display_config` text DEFAULT NULL COMMENT '基础展示主体配置 JSON';

UPDATE `payment_cashier_config`
SET `display_config` = `theme_config`
WHERE `display_config` IS NULL
  AND `theme_config` IS NOT NULL;
