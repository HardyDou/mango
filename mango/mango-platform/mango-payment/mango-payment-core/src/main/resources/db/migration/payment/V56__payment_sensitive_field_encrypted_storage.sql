ALTER TABLE `payment_application`
  MODIFY COLUMN `app_secret` varchar(512) DEFAULT NULL COMMENT '应用密钥密文';

ALTER TABLE `payment_enterprise_subject`
  ADD COLUMN `credit_code_hash` varchar(64) DEFAULT NULL COMMENT '统一社会信用代码规范化哈希' AFTER `credit_code`,
  MODIFY COLUMN `credit_code` varchar(512) NOT NULL COMMENT '统一社会信用代码密文',
  MODIFY COLUMN `bank_account_no` varchar(512) NOT NULL COMMENT '银行账户密文';

UPDATE `payment_enterprise_subject`
SET `credit_code_hash` = SHA2(UPPER(TRIM(`credit_code`)), 256)
WHERE `credit_code_hash` IS NULL;

ALTER TABLE `payment_enterprise_subject`
  MODIFY COLUMN `credit_code_hash` varchar(64) NOT NULL COMMENT '统一社会信用代码规范化哈希',
  DROP INDEX `uk_payment_subject_tenant_credit`,
  ADD UNIQUE KEY `uk_payment_subject_tenant_credit_hash` (`tenant_id`, `credit_code_hash`, `del_flag`);

ALTER TABLE `payment_subject_bank_account`
  MODIFY COLUMN `account_no` varchar(512) NOT NULL COMMENT '银行账号密文或受控值';
