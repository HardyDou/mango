UPDATE `authorization_menu`
SET `parent_id` = 2800,
    `menu_type` = 1,
    `menu_name` = '线下支付',
    `menu_code` = 'payment:offline-payment',
    `path` = '/payment/offline',
    `icon` = 'WalletCards',
    `component` = NULL,
    `sort` = 5,
    `status` = 1,
    `visible` = 1,
    `redirect` = '/payment/offline/collections',
    `permissions` = 'payment:offline-payment',
    `remark` = '线下收款通道的收款确认、退款处理、银行流水导入和对账匹配入口',
    `del_flag` = 0,
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 2824;

UPDATE `authorization_menu`
SET `parent_id` = 2824,
    `menu_type` = 2,
    `menu_name` = '线下收款订单',
    `menu_code` = 'payment:offline-collection',
    `path` = '/payment/offline/collections',
    `icon` = 'Money',
    `component` = '@/views/payment/offline-collections/index.vue',
    `sort` = 1,
    `status` = 1,
    `visible` = 1,
    `redirect` = NULL,
    `permissions` = 'payment:offline-collection:list',
    `remark` = '线下转账收款单、对账码、转账备注、凭证和到账确认入口',
    `del_flag` = 0,
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 2825;

UPDATE `authorization_menu`
SET `parent_id` = 2825,
    `menu_type` = 3,
    `menu_name` = '线下收款查询',
    `menu_code` = 'payment:offline-collection:query',
    `permissions` = 'payment:offline-collection:query',
    `status` = 1,
    `visible` = 0,
    `del_flag` = 0,
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 282501;

UPDATE `authorization_menu`
SET `redirect` = '/payment/enterprise-subjects',
    `update_time` = NOW(),
    `updated_at` = NOW()
WHERE `id` = 2823
  AND (`redirect` IS NULL OR `redirect` IN ('/payment/offline-collections', '/payment/offline/collections'));
