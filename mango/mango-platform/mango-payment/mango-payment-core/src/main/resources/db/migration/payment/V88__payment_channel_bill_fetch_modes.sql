SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `payment_channel` ADD COLUMN `bill_fetch_modes` varchar(128) DEFAULT NULL COMMENT ''支持的账单获取方式：MANUAL/FTP/FTPS/HTTP'' AFTER `capability_summary`',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'payment_channel'
    AND column_name = 'bill_fetch_modes'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `payment_channel`
SET `bill_fetch_modes` = 'MANUAL,HTTP',
    `updated_at` = NOW()
WHERE `tenant_id` = 1
  AND `channel_code` = 'MANGO_PAY'
  AND `del_flag` = 0;

UPDATE `payment_channel`
SET `bill_fetch_modes` = 'MANUAL,FTP,FTPS,HTTP',
    `updated_at` = NOW()
WHERE `tenant_id` = 1
  AND `channel_code` IN ('ALLINPAY', 'HUAXIA_BANK')
  AND `del_flag` = 0;

UPDATE `payment_channel`
SET `bill_fetch_modes` = 'MANUAL',
    `updated_at` = NOW()
WHERE `tenant_id` = 1
  AND `channel_code` = 'OFFLINE_COLLECTION'
  AND `del_flag` = 0;
