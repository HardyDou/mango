INSERT INTO `notice_channel_config` (
  `id`,
  `channel_type`,
  `provider_code`,
  `config_name`,
  `config_json`,
  `enabled`,
  `priority`,
  `weight`,
  `config_status`,
  `last_send_status`,
  `rate_limit_config`,
  `tenant_id`,
  `created_at`,
  `updated_at`
)
SELECT
  270502,
  'SITE',
  'INTERNAL',
  '默认系统消息通道',
  '{"senderName":"系统通知","retentionDays":180,"realtimeEnabled":true,"popupEnabled":true,"soundEnabled":true,"desktopNotificationEnabled":true,"unreadCountEnabled":true,"soundText":"您有新的系统消息，请及时查看"}',
  1,
  0,
  100,
  'COMPLETE',
  'NONE',
  '{"maxPerMinute":0,"timeoutSeconds":10,"concurrentLimit":0}',
  '1',
  NOW(),
  NOW()
WHERE NOT EXISTS (
  SELECT 1
  FROM `notice_channel_config`
  WHERE `tenant_id` = '1'
    AND `channel_type` = 'SITE'
    AND `provider_code` = 'INTERNAL'
);
