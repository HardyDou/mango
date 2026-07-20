-- Mango Maven 1.0.21 corrected missing audit columns in the published Workflow V1.
-- Normalize only the known 1.0.20 checksum so validation can continue to V2.
SET @workflow_v1_checksum_compatibility_sql = IF(
  EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = '${flyway:table}'
  ),
  'UPDATE `${flyway:table}` SET `checksum` = 1010539203 WHERE `version` = ''1'' AND `script` = ''V1__init_workflow.sql'' AND `checksum` IN (-840523381, -1500222187) AND `success` = 1',
  'DO 0'
);

PREPARE workflow_v1_checksum_compatibility_stmt FROM @workflow_v1_checksum_compatibility_sql;
EXECUTE workflow_v1_checksum_compatibility_stmt;
DEALLOCATE PREPARE workflow_v1_checksum_compatibility_stmt;
