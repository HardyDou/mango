UPDATE `ai_service_definition`
SET `skill_id` = NULL
WHERE `code` = 'assistant.general'
  AND `skill_id` IS NOT NULL;
