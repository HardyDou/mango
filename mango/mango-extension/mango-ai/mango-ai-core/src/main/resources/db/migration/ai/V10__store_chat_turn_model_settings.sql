ALTER TABLE `ai_chat_conversation`
  CHANGE COLUMN `model_id` `last_model_id` bigint NOT NULL COMMENT '最近一次成功回复使用的模型 ID',
  CHANGE COLUMN `model_name` `last_model_name` varchar(128) NOT NULL COMMENT '最近一次成功回复使用的模型名称',
  CHANGE COLUMN `provider_code` `last_provider_code` varchar(64) NOT NULL COMMENT '最近一次成功回复使用的供应商编码',
  CHANGE COLUMN `thinking_enabled` `last_thinking_enabled` tinyint NOT NULL DEFAULT 0 COMMENT '最近一次成功回复是否启用深度思考';

ALTER TABLE `ai_chat_message`
  ADD COLUMN `model_id` bigint NULL COMMENT '本轮助手回复实际使用的模型 ID' AFTER `content_parts_json`,
  ADD COLUMN `model_name` varchar(128) NULL COMMENT '本轮助手回复实际使用的模型名称' AFTER `model_id`,
  ADD COLUMN `provider_code` varchar(64) NULL COMMENT '本轮助手回复实际使用的供应商编码' AFTER `model_name`,
  ADD COLUMN `thinking_enabled` tinyint NULL COMMENT '本轮助手回复是否启用深度思考' AFTER `provider_code`;

UPDATE `ai_chat_message` AS `message`
JOIN `ai_chat_conversation` AS `conversation`
  ON `conversation`.`id` = `message`.`conversation_id`
  AND `conversation`.`tenant_id` = `message`.`tenant_id`
SET `message`.`model_id` = `conversation`.`last_model_id`,
    `message`.`model_name` = `conversation`.`last_model_name`,
    `message`.`provider_code` = `conversation`.`last_provider_code`,
    `message`.`thinking_enabled` = `conversation`.`last_thinking_enabled`
WHERE `message`.`role` = 'assistant';
