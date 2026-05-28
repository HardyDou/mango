UPDATE `notice_channel_config`
SET `config_json` = JSON_SET(
  COALESCE(NULLIF(`config_json`, ''), '{}'),
  '$.soundText',
  '您有新的系统消息，请及时查看'
)
WHERE `channel_type` = 'SITE'
  AND `provider_code` = 'INTERNAL'
  AND JSON_EXTRACT(COALESCE(NULLIF(`config_json`, ''), '{}'), '$.soundText') IS NULL;
