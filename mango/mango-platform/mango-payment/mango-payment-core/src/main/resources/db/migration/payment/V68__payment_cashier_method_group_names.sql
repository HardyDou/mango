UPDATE `payment_method`
SET `method_name` = '线下转账',
    `description` = '展示收款户名、账号、开户行、转账备注和认款说明',
    `route_strategy` = '路由到线下收款通道签约能力',
    `updated_at` = NOW()
WHERE `method_code` = 'CORPORATE_OFFLINE_ACCOUNT'
  AND `del_flag` = 0;
