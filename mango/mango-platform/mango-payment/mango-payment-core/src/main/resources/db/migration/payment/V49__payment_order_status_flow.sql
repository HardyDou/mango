CREATE TABLE IF NOT EXISTS `payment_order_status_flow` (
  `id` bigint NOT NULL COMMENT '主键',
  `order_type` varchar(32) NOT NULL COMMENT '订单类型：BUSINESS_ORDER、PAYMENT_ORDER、REFUND_ORDER',
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `order_no` varchar(64) NOT NULL COMMENT '订单号',
  `from_status` varchar(32) DEFAULT NULL COMMENT '变更前状态',
  `to_status` varchar(32) NOT NULL COMMENT '变更后状态',
  `trigger_source` varchar(64) NOT NULL COMMENT '触发来源',
  `trigger_no` varchar(128) DEFAULT NULL COMMENT '触发单号',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人ID',
  `operator_name` varchar(128) DEFAULT NULL COMMENT '操作人名称',
  `happen_time` datetime NOT NULL COMMENT '发生时间',
  `remark` varchar(512) DEFAULT NULL COMMENT '说明',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记：0-正常，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_payment_order_status_flow_order` (`tenant_id`, `order_type`, `order_id`, `happen_time`),
  KEY `idx_payment_order_status_flow_no` (`tenant_id`, `order_type`, `order_no`, `happen_time`),
  KEY `idx_payment_order_status_flow_trigger` (`tenant_id`, `trigger_source`, `trigger_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付订单状态流转记录';

INSERT INTO `payment_order_status_flow` (
  `id`, `order_type`, `order_id`, `order_no`, `from_status`, `to_status`,
  `trigger_source`, `trigger_no`, `operator_id`, `operator_name`, `happen_time`,
  `remark`, `tenant_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`
)
SELECT
  (cast(conv(substr(md5(concat('BUSINESS_ORDER:', bo.tenant_id, ':', bo.id)), 1, 15), 16, 10) as unsigned) & 9223372036854775807),
  'BUSINESS_ORDER',
  bo.id,
  bo.biz_order_no,
  NULL,
  bo.status,
  'HISTORY_BACKFILL',
  bo.biz_order_no,
  bo.created_by,
  'system',
  coalesce(bo.created_at, now()),
  '历史业务订单状态初始化',
  bo.tenant_id,
  bo.created_by,
  coalesce(bo.created_at, now()),
  bo.updated_by,
  coalesce(bo.updated_at, bo.created_at, now()),
  0
FROM payment_business_order bo
WHERE bo.del_flag = 0
  AND NOT EXISTS (
    SELECT 1 FROM payment_order_status_flow sf
    WHERE sf.tenant_id = bo.tenant_id
      AND sf.order_type = 'BUSINESS_ORDER'
      AND sf.order_id = bo.id
      AND sf.trigger_source = 'HISTORY_BACKFILL'
  );

INSERT INTO `payment_order_status_flow` (
  `id`, `order_type`, `order_id`, `order_no`, `from_status`, `to_status`,
  `trigger_source`, `trigger_no`, `operator_id`, `operator_name`, `happen_time`,
  `remark`, `tenant_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`
)
SELECT
  (cast(conv(substr(md5(concat('PAYMENT_ORDER:', po.tenant_id, ':', po.id)), 1, 15), 16, 10) as unsigned) & 9223372036854775807),
  'PAYMENT_ORDER',
  po.id,
  po.pay_order_no,
  NULL,
  po.status,
  'HISTORY_BACKFILL',
  po.pay_order_no,
  po.created_by,
  'system',
  coalesce(po.created_at, now()),
  '历史支付订单状态初始化',
  po.tenant_id,
  po.created_by,
  coalesce(po.created_at, now()),
  po.updated_by,
  coalesce(po.updated_at, po.created_at, now()),
  0
FROM payment_order po
WHERE NOT EXISTS (
    SELECT 1 FROM payment_order_status_flow sf
    WHERE sf.tenant_id = po.tenant_id
      AND sf.order_type = 'PAYMENT_ORDER'
      AND sf.order_id = po.id
      AND sf.trigger_source = 'HISTORY_BACKFILL'
  );

INSERT INTO `payment_order_status_flow` (
  `id`, `order_type`, `order_id`, `order_no`, `from_status`, `to_status`,
  `trigger_source`, `trigger_no`, `operator_id`, `operator_name`, `happen_time`,
  `remark`, `tenant_id`, `created_by`, `created_at`, `updated_by`, `updated_at`, `del_flag`
)
SELECT
  (cast(conv(substr(md5(concat('REFUND_ORDER:', ro.tenant_id, ':', ro.id)), 1, 15), 16, 10) as unsigned) & 9223372036854775807),
  'REFUND_ORDER',
  ro.id,
  ro.refund_order_no,
  NULL,
  ro.status,
  'HISTORY_BACKFILL',
  ro.refund_order_no,
  ro.created_by,
  'system',
  coalesce(ro.created_at, now()),
  '历史退款订单状态初始化',
  ro.tenant_id,
  ro.created_by,
  coalesce(ro.created_at, now()),
  ro.updated_by,
  coalesce(ro.updated_at, ro.created_at, now()),
  0
FROM payment_refund_order ro
WHERE NOT EXISTS (
    SELECT 1 FROM payment_order_status_flow sf
    WHERE sf.tenant_id = ro.tenant_id
      AND sf.order_type = 'REFUND_ORDER'
      AND sf.order_id = ro.id
      AND sf.trigger_source = 'HISTORY_BACKFILL'
  );
