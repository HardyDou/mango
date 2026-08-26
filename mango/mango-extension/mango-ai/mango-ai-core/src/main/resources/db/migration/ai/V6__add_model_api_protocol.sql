ALTER TABLE `ai_model`
  ADD COLUMN `api_protocol` varchar(32) NOT NULL DEFAULT 'CHAT_COMPLETIONS' AFTER `platform_alias`;
