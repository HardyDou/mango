UPDATE `authorization_app_module`
SET `module_name` = '模板管理模块',
    `update_time` = CURRENT_TIMESTAMP
WHERE `app_code` = 'internal-admin'
  AND `module_code` = 'mango-template'
  AND `module_name` = '支付中心';

-- Payment app module and menus are registered by mango-payment-starter AUTH_MENU resource.
