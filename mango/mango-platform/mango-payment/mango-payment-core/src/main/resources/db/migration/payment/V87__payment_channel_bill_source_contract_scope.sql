SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `payment_channel_bill_source` ADD COLUMN `contract_id` bigint NULL COMMENT ''签约通道ID'' AFTER `id`',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'payment_channel_bill_source'
    AND column_name = 'contract_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `payment_channel_bill_source` s
JOIN (
  SELECT c.tenant_id, ch.channel_code, MIN(c.id) AS contract_id
  FROM `payment_channel_contract` c
  JOIN `payment_channel` ch
    ON ch.id = c.channel_id
   AND ch.tenant_id = c.tenant_id
   AND ch.del_flag = 0
  WHERE c.del_flag = 0
    AND c.status = 1
  GROUP BY c.tenant_id, ch.channel_code
) matched
  ON matched.tenant_id = s.tenant_id
 AND matched.channel_code = s.channel_code
SET s.contract_id = matched.contract_id
WHERE s.contract_id IS NULL;

SET @sql := (
  SELECT IF(COUNT(*) > 0,
    'ALTER TABLE `payment_channel_bill_source` DROP INDEX `uk_payment_bill_source_channel_mode`',
    'SELECT 1')
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'payment_channel_bill_source'
    AND index_name = 'uk_payment_bill_source_channel_mode'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'CREATE UNIQUE INDEX `uk_payment_bill_source_contract_mode` ON `payment_channel_bill_source` (`tenant_id`, `contract_id`, `fetch_mode`, `del_flag`)',
    'SELECT 1')
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'payment_channel_bill_source'
    AND index_name = 'uk_payment_bill_source_contract_mode'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'CREATE INDEX `idx_payment_bill_source_contract` ON `payment_channel_bill_source` (`tenant_id`, `contract_id`, `enabled`)',
    'SELECT 1')
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'payment_channel_bill_source'
    AND index_name = 'idx_payment_bill_source_contract'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
