UPDATE `sys_dict_type`
SET `domain_code` = 'TEMPLATE'
WHERE `dict_type` REGEXP '^template_';

UPDATE `sys_dict_type`
SET `domain_code` = 'FILE'
WHERE `dict_type` REGEXP '^file_';

UPDATE `sys_dict_type`
SET `domain_code` = 'WORKFLOW'
WHERE `dict_type` REGEXP '^workflow_';

UPDATE `sys_dict_type`
SET `domain_code` = 'NOTICE'
WHERE `dict_type` REGEXP '^notice_';

UPDATE `sys_dict_type`
SET `domain_code` = 'NUMGEN'
WHERE `dict_type` REGEXP '^numgen_';

UPDATE `sys_dict_type`
SET `domain_code` = 'CALENDAR'
WHERE `dict_type` REGEXP '^calendar_';

UPDATE `sys_dict_type`
SET `domain_code` = 'COMMON'
WHERE `domain_code` IS NULL OR `domain_code` = '';
