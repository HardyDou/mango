ALTER TABLE `payment_business_order`
  ADD COLUMN `title` varchar(128) NOT NULL DEFAULT '' COMMENT '支付标题' AFTER `app_code`,
  ADD COLUMN `paid_amount` bigint NOT NULL DEFAULT '0' COMMENT '已支付金额，单位分' AFTER `amount`,
  ADD COLUMN `refunded_amount` bigint NOT NULL DEFAULT '0' COMMENT '已退款金额，单位分' AFTER `paid_amount`,
  ADD COLUMN `extend_info` json DEFAULT NULL COMMENT '业务扩展信息' AFTER `return_url`;

UPDATE `payment_business_order`
SET `title` = CONCAT('业务订单 ', `biz_order_no`)
WHERE `title` = '';

UPDATE `payment_business_order` `bo`
LEFT JOIN (
  SELECT `business_order_id`, `tenant_id`, SUM(`amount`) AS `paid_amount`
  FROM `payment_order`
  WHERE `status` IN ('SUCCESS', 'PAID')
  GROUP BY `business_order_id`, `tenant_id`
) `po`
  ON `po`.`business_order_id` = `bo`.`id`
 AND `po`.`tenant_id` = `bo`.`tenant_id`
LEFT JOIN (
  SELECT `po`.`business_order_id`, `ro`.`tenant_id`, SUM(`ro`.`refund_amount`) AS `refunded_amount`
  FROM `payment_refund_order` `ro`
  JOIN `payment_order` `po`
    ON `po`.`id` = `ro`.`payment_order_id`
   AND `po`.`tenant_id` = `ro`.`tenant_id`
  WHERE `ro`.`status` IN ('SUCCESS', 'REFUNDED')
  GROUP BY `po`.`business_order_id`, `ro`.`tenant_id`
) `ro`
  ON `ro`.`business_order_id` = `bo`.`id`
 AND `ro`.`tenant_id` = `bo`.`tenant_id`
SET `bo`.`paid_amount` = COALESCE(`po`.`paid_amount`, 0),
    `bo`.`refunded_amount` = COALESCE(`ro`.`refunded_amount`, 0),
    `bo`.`update_time` = NOW()
WHERE `bo`.`del_flag` = 0;
