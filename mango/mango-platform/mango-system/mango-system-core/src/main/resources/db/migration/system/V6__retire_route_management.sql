DELETE FROM `sys_dict_data`
WHERE `dict_type` = 'system_route_type';

DELETE FROM `sys_dict_type`
WHERE `dict_type` = 'system_route_type';

DROP TABLE IF EXISTS `sys_route_conf`;
