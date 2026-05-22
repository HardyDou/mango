INSERT INTO `sys_dict_type` (`id`, `dict_type`, `dict_name`, `status`, `remark`, `create_by`, `update_by`, `create_time`, `update_time`, `created_by`, `created_at`, `updated_by`, `updated_at`, `tenant_id`)
VALUES
(330,'template_source_format','模板源格式',1,'模板服务源文件格式',NULL,NULL,NOW(),NOW(),NULL,NOW(),NULL,NOW(),'default'),
(331,'template_output_format','模板输出格式',1,'模板服务输出格式',NULL,NULL,NOW(),NOW(),NULL,NOW(),NULL,NOW(),'default'),
(332,'template_render_status','模板渲染状态',1,'模板渲染任务状态',NULL,NULL,NOW(),NOW(),NULL,NOW(),NULL,NOW(),'default')
ON DUPLICATE KEY UPDATE
`dict_name` = VALUES(`dict_name`),
`status` = VALUES(`status`),
`remark` = VALUES(`remark`),
`update_time` = NOW(),
`updated_at` = NOW();

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `sort`, `status`, `remark`, `create_by`, `update_by`, `create_time`, `update_time`, `created_by`, `created_at`, `updated_by`, `updated_at`, `tenant_id`)
VALUES
(3300,'template_source_format','纯文本','TEXT',1,1,NULL,NULL,NULL,NOW(),NOW(),NULL,NOW(),NULL,NOW(),'default'),
(3301,'template_source_format','富文本 HTML','HTML',2,1,NULL,NULL,NULL,NOW(),NOW(),NULL,NOW(),NULL,NOW(),'default'),
(3302,'template_source_format','Word DOCX','DOCX',3,1,NULL,NULL,NULL,NOW(),NOW(),NULL,NOW(),NULL,NOW(),'default'),
(3303,'template_source_format','Excel XLSX','XLSX',4,1,NULL,NULL,NULL,NOW(),NOW(),NULL,NOW(),NULL,NOW(),'default'),
(3310,'template_output_format','纯文本','TEXT',1,1,NULL,NULL,NULL,NOW(),NOW(),NULL,NOW(),NULL,NOW(),'default'),
(3311,'template_output_format','HTML','HTML',2,1,NULL,NULL,NULL,NOW(),NOW(),NULL,NOW(),NULL,NOW(),'default'),
(3312,'template_output_format','Word DOCX','DOCX',3,1,NULL,NULL,NULL,NOW(),NOW(),NULL,NOW(),NULL,NOW(),'default'),
(3313,'template_output_format','Excel XLSX','XLSX',4,1,NULL,NULL,NULL,NOW(),NOW(),NULL,NOW(),NULL,NOW(),'default'),
(3314,'template_output_format','PDF','PDF',5,1,NULL,NULL,NULL,NOW(),NOW(),NULL,NOW(),NULL,NOW(),'default'),
(3315,'template_output_format','OFD','OFD',6,1,NULL,NULL,NULL,NOW(),NOW(),NULL,NOW(),NULL,NOW(),'default'),
(3320,'template_render_status','待处理','PENDING',1,1,NULL,NULL,NULL,NOW(),NOW(),NULL,NOW(),NULL,NOW(),'default'),
(3321,'template_render_status','处理中','RUNNING',2,1,NULL,NULL,NULL,NOW(),NOW(),NULL,NOW(),NULL,NOW(),'default'),
(3322,'template_render_status','成功','SUCCESS',3,1,NULL,NULL,NULL,NOW(),NOW(),NULL,NOW(),NULL,NOW(),'default'),
(3323,'template_render_status','失败','FAILED',4,1,NULL,NULL,NULL,NOW(),NOW(),NULL,NOW(),NULL,NOW(),'default')
ON DUPLICATE KEY UPDATE
`dict_label` = VALUES(`dict_label`),
`dict_value` = VALUES(`dict_value`),
`sort` = VALUES(`sort`),
`status` = VALUES(`status`),
`update_time` = NOW(),
`updated_at` = NOW();
