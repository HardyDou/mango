DELETE FROM `notice_business_channel_template`
WHERE `tenant_id` IN ('1', 'default')
  AND `biz_type` IN (
    'TEST_NOTICE',
    'MANGO_JOB_FAILED',
    'MANGO_JOB_FAILED_TEMPLATE',
    'MANGO_JOB_FAILED_TEMPLATE_E2E'
  );

DELETE FROM `notice_business_config_version`
WHERE `tenant_id` IN ('1', 'default')
  AND `biz_type` IN (
    'TEST_NOTICE',
    'MANGO_JOB_FAILED',
    'MANGO_JOB_FAILED_TEMPLATE',
    'MANGO_JOB_FAILED_TEMPLATE_E2E'
  );

DELETE FROM `notice_business_type`
WHERE `tenant_id` IN ('1', 'default')
  AND `biz_type` IN (
    'TEST_NOTICE',
    'MANGO_JOB_FAILED',
    'MANGO_JOB_FAILED_TEMPLATE',
    'MANGO_JOB_FAILED_TEMPLATE_E2E'
  );

INSERT INTO `notice_business_type`
  (`id`, `biz_type`, `biz_name`, `biz_group`, `domain_code`, `description`, `params_schema`, `enabled`,
   `default_priority`, `idempotent_strategy`, `tenant_id`, `created_by`, `created_at`, `updated_by`, `updated_at`)
VALUES
  (2060000000000014001, 'job.instance.failed', '定时任务执行失败', 'JOB', 'JOB',
   '定时任务实例执行失败后发送给任务负责人或配置接收人。',
   '{"type":"object","properties":{"appCode":{"type":"string","title":"应用编码"},"jobCode":{"type":"string","title":"任务编码"},"jobName":{"type":"string","title":"任务名称"},"handlerName":{"type":"string","title":"处理器"},"instanceId":{"type":"string","title":"执行实例ID"},"triggerType":{"type":"string","title":"触发类型"},"triggerBatchNo":{"type":"string","title":"触发批次"},"traceId":{"type":"string","title":"链路ID"},"scheduledFireTime":{"type":"string","title":"计划触发时间"},"startTime":{"type":"string","title":"开始时间"},"endTime":{"type":"string","title":"结束时间"},"durationMillis":{"type":"number","title":"执行耗时毫秒"},"errorSummary":{"type":"string","title":"失败原因"},"ruleName":{"type":"string","title":"告警规则"}},"required":["jobCode","jobName","instanceId","errorSummary"]}',
   1, 'HIGH', 'BIZ_ID', '1', 1, NOW(), 1, NOW())
ON DUPLICATE KEY UPDATE
  `biz_name` = VALUES(`biz_name`),
  `biz_group` = VALUES(`biz_group`),
  `domain_code` = VALUES(`domain_code`),
  `description` = VALUES(`description`),
  `params_schema` = VALUES(`params_schema`),
  `enabled` = VALUES(`enabled`),
  `default_priority` = VALUES(`default_priority`),
  `idempotent_strategy` = VALUES(`idempotent_strategy`),
  `updated_by` = VALUES(`updated_by`),
  `updated_at` = VALUES(`updated_at`);

INSERT INTO `notice_business_config_version`
  (`id`, `business_type_id`, `biz_type`, `params_schema`, `default_priority`, `idempotent_strategy`, `version`,
   `version_status`, `publish_time`, `publish_by`, `tenant_id`, `created_by`, `created_at`, `updated_by`, `updated_at`)
VALUES
  (2060000000000014002, 2060000000000014001, 'job.instance.failed',
   '{"type":"object","properties":{"appCode":{"type":"string","title":"应用编码"},"jobCode":{"type":"string","title":"任务编码"},"jobName":{"type":"string","title":"任务名称"},"handlerName":{"type":"string","title":"处理器"},"instanceId":{"type":"string","title":"执行实例ID"},"triggerType":{"type":"string","title":"触发类型"},"triggerBatchNo":{"type":"string","title":"触发批次"},"traceId":{"type":"string","title":"链路ID"},"scheduledFireTime":{"type":"string","title":"计划触发时间"},"startTime":{"type":"string","title":"开始时间"},"endTime":{"type":"string","title":"结束时间"},"durationMillis":{"type":"number","title":"执行耗时毫秒"},"errorSummary":{"type":"string","title":"失败原因"},"ruleName":{"type":"string","title":"告警规则"}},"required":["jobCode","jobName","instanceId","errorSummary"]}',
   'HIGH', 'BIZ_ID', 1, 'ACTIVE', NOW(), 1, '1', 1, NOW(), 1, NOW())
ON DUPLICATE KEY UPDATE
  `business_type_id` = VALUES(`business_type_id`),
  `params_schema` = VALUES(`params_schema`),
  `default_priority` = VALUES(`default_priority`),
  `idempotent_strategy` = VALUES(`idempotent_strategy`),
  `version_status` = VALUES(`version_status`),
  `publish_time` = VALUES(`publish_time`),
  `publish_by` = VALUES(`publish_by`),
  `updated_by` = VALUES(`updated_by`),
  `updated_at` = VALUES(`updated_at`);

INSERT INTO `notice_business_channel_template`
  (`id`, `business_type_id`, `biz_type`, `channel_type`, `template_name`, `title_template`, `content_template`,
   `channel_template_id`, `variable_mapping`, `version`, `version_status`, `enabled`, `channel_config_id`,
   `publish_time`, `publish_by`, `tenant_id`, `created_by`, `created_at`, `updated_by`, `updated_at`)
VALUES
  (2060000000000014003, 2060000000000014001, 'job.instance.failed', 'SITE', '定时任务执行失败系统消息',
   '定时任务执行失败：{{jobName}}',
   '定时任务 {{jobName}}（{{jobCode}}）执行失败。实例：{{instanceId}}；处理器：{{handlerName}}；触发批次：{{triggerBatchNo}}；失败原因：{{errorSummary}}。请进入平台能力/任务管理/执行实例查看日志。',
   NULL, NULL, 1, 'ACTIVE', 1, NULL, NOW(), 1, '1', 1, NOW(), 1, NOW())
ON DUPLICATE KEY UPDATE
  `business_type_id` = VALUES(`business_type_id`),
  `template_name` = VALUES(`template_name`),
  `title_template` = VALUES(`title_template`),
  `content_template` = VALUES(`content_template`),
  `version_status` = VALUES(`version_status`),
  `enabled` = VALUES(`enabled`),
  `channel_config_id` = VALUES(`channel_config_id`),
  `publish_time` = VALUES(`publish_time`),
  `publish_by` = VALUES(`publish_by`),
  `updated_by` = VALUES(`updated_by`),
  `updated_at` = VALUES(`updated_at`);
