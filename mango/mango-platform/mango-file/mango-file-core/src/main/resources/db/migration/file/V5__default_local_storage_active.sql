UPDATE `file_storage_config`
SET `active` = CASE WHEN `storage_type` = 'LOCAL' THEN 1 ELSE 0 END,
    `updated_time` = NOW(),
    `updated_at` = NOW()
WHERE `storage_type` IN ('LOCAL', 'MINIO');
