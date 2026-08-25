ALTER TABLE `ai_chat_message`
  ADD COLUMN `content_parts_json` json NULL AFTER `role`;

UPDATE `ai_chat_message`
SET `content_parts_json` = JSON_ARRAY(JSON_OBJECT(
  'type', IF(`role` = 'assistant', 'RICH_TEXT', 'TEXT'),
  'text', `content`
));

ALTER TABLE `ai_chat_message`
  MODIFY COLUMN `content_parts_json` json NOT NULL COMMENT '类型化消息内容块',
  DROP COLUMN `content`;
