UPDATE `payment_cashier_config`
SET `display_config` = JSON_REMOVE(`display_config`, '$.title')
WHERE `display_config` IS NOT NULL
  AND `display_config` <> ''
  AND JSON_VALID(`display_config`)
  AND JSON_CONTAINS_PATH(`display_config`, 'one', '$.title');

ALTER TABLE `payment_cashier_config`
  DROP COLUMN `refund_enabled`,
  DROP COLUMN `partial_refund_enabled`;
