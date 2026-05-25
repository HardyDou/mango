INSERT INTO `sys_dict_type` (`id`, `dict_type`, `dict_name`, `status`, `remark`, `create_by`, `update_by`, `create_time`, `update_time`, `created_by`, `created_at`, `updated_by`, `updated_at`, `tenant_id`)
VALUES
(340,'file_access_level','文件访问级别',1,'文件中心访问级别',NULL,NULL,NOW(),NOW(),NULL,NOW(),NULL,NOW(),'default')
ON DUPLICATE KEY UPDATE
`dict_name` = VALUES(`dict_name`),
`status` = VALUES(`status`),
`remark` = VALUES(`remark`),
`update_time` = NOW(),
`updated_at` = NOW();

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `sort`, `status`, `remark`, `create_by`, `update_by`, `create_time`, `update_time`, `created_by`, `created_at`, `updated_by`, `updated_at`, `tenant_id`)
VALUES
(3400,'file_access_level','机构私有','PRIVATE',1,1,NULL,NULL,NULL,NOW(),NOW(),NULL,NOW(),NULL,NOW(),'default'),
(3401,'file_access_level','公开读取','PUBLIC_READ',2,1,NULL,NULL,NULL,NOW(),NOW(),NULL,NOW(),NULL,NOW(),'default'),
(3402,'file_access_level','内部文件','INTERNAL',3,1,NULL,NULL,NULL,NOW(),NOW(),NULL,NOW(),NULL,NOW(),'default')
ON DUPLICATE KEY UPDATE
`dict_label` = VALUES(`dict_label`),
`dict_value` = VALUES(`dict_value`),
`sort` = VALUES(`sort`),
`status` = VALUES(`status`),
`update_time` = NOW(),
`updated_at` = NOW();
