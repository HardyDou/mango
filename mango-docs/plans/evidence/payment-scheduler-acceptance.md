# 支付任务触发验收证据

## 1. 验收范围

- 设计项：`PAY-SCHED-001`
- 设计来源：统一支付系统设计说明书 16、17
- 后端接口：
  - `POST /payment/tasks/expire-open-orders`
  - `POST /payment/tasks/query-processing-orders`
  - `POST /payment/notification-records/deliver-due`
  - `POST /payment/reconciliations/import`
  - `POST /payment/reconciliations/mango-pay/virtual/generate`
  - `POST /payment/settlement-summaries/generate`
- 平台触发入口：`PaymentNotificationDispatchScheduler.dispatchOnce()`
- 权限：
  - `payment:task:expire-open-orders`
  - `payment:task:query-processing-orders`
  - `payment:notification-record:deliver-due`
  - `payment:reconciliation:import`
  - `payment:settlement-summary:generate`
- 数据：`payment_order`、`payment_business_order`、`payment_notification_record`、`payment_channel_bill_batch`、`payment_settlement_summary`、`payment_operation_audit`

## 2. 设计结论

支付模块不建设独立调度框架，只提供可被后台人工触发或平台统一调度能力调用的任务入口。订单过期、主动查单、通知重试、对账批次和结算汇总生成均通过受控接口或平台调度入口触发；具体调度编排、频率、告警和统一任务控制台不放在支付模块内。

## 3. 功能验收记录

| 台账 ID | 能力 | 交付物 | 关键断言 | 权限/审计 | 验证方式 | 结论 |
|---|---|---|---|---|---|---|
| PAY-SCHED-001 | 订单过期 | `POST /payment/tasks/expire-open-orders`、`PaymentReadonlyResourceService.expireOpenPaymentOrders`、`PaymentOrderMapper.selectExpiredOpenPaymentOrders` | 只扫描当前租户、已过期、未成功支付、业务订单未收款的 `CREATED/PAYING` 支付订单；逐笔复用受控关单服务，推进支付订单和业务订单关闭；单笔失败计入失败数，不中断整批 | 独立权限 `payment:task:expire-open-orders`；批次审计 `EXPIRE_OPEN_PAYMENT_ORDERS / PAYMENT_ORDER / SUCCESS`；单笔关单仍写 `CLOSE_PAYMENT_ORDER` 审计 | `PaymentReadonlyResourceServiceTest.expireOpenPaymentOrders_closesExpiredOrdersAndRecordsAudit`、`PaymentReadonlyResourceServiceTest.expireOpenPaymentOrders_singleFailure_continuesBatch`、`PaymentReadonlyResourceControllerTest.paymentTaskEndpoints_useIndependentPermissions` | DONE |
| PAY-SCHED-001 | 主动查单 | `POST /payment/tasks/query-processing-orders`、`PaymentReadonlyResourceService.queryProcessingPaymentOrders`、`PaymentOrderMapper.selectProcessingPaymentOrders` | 只扫描当前租户支付中的订单；逐笔复用既有主动查单服务，结果只能由通道查单依据推进；单笔失败计入失败数，不中断整批 | 独立权限 `payment:task:query-processing-orders`；批次审计 `QUERY_PROCESSING_PAYMENT_ORDERS / PAYMENT_ORDER / SUCCESS`；单笔查单记录仍写入 `payment_channel_query_record` | `PaymentReadonlyResourceServiceTest.queryProcessingPaymentOrders_queriesOrdersAndRecordsAudit`、`PaymentReadonlyResourceControllerTest.paymentTaskEndpoints_useIndependentPermissions` | DONE |
| PAY-SCHED-001 | 通知重试 | `POST /payment/notification-records/deliver-due`、`PaymentNotificationDispatchScheduler.dispatchOnce()` | 人工入口投递当前租户到期通知；平台入口按到期通知租户列表设置租户上下文后调用投递服务；不改变资金状态 | 独立权限 `payment:notification-record:deliver-due`；人工触发写 `DELIVER_DUE_NOTIFICATION_RECORDS` 审计 | `PaymentReadonlyResourceServiceTest.deliverDueNotificationRecords_dispatchesAndRecordsAudit`、`PaymentReadonlyResourceControllerTest.notificationDeliverDueEndpoint_usesIndependentPermission`、`PaymentNotificationDispatchSchedulerTest` | DONE |
| PAY-SCHED-001 | 对账批次 | `POST /payment/reconciliations/import`、`POST /payment/reconciliations/mango-pay/virtual/generate` | 后台人工或平台任务可调用接口生成对账批次；同一通道、日期、文件摘要幂等控制；芒果支付账单生成不调用外部机构 | 权限 `payment:reconciliation:import`；服务写导入/生成操作审计 | `payment-reconciliation-acceptance.md`、`payment-mango-pay-acceptance.md` | DONE |
| PAY-SCHED-001 | 结算汇总生成 | `POST /payment/settlement-summaries/generate` | 后台人工或平台任务可调用接口按日期、应用、主体、通道生成汇总；确认前校验对账和差异状态 | 权限 `payment:settlement-summary:generate`；服务写 `GENERATE_SETTLEMENT_SUMMARY` 审计 | `payment-settlement-summary-acceptance.md` | DONE |

## 4. 验证命令

```bash
mvn -pl mango-platform/mango-payment/mango-payment-core -am -Dtest=PaymentReadonlyResourceServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl mango-platform/mango-payment/mango-payment-starter -am -Dtest=PaymentReadonlyResourceControllerTest -Dsurefire.failIfNoSpecifiedTests=false test
```

## 5. 未验证项和风险

| 项目 | 说明 | 归属 |
|---|---|---|
| 外部机构主动查单联调 | 当前批量主动查单入口复用通道适配 SPI；通联、华夏、微信、支付宝、连连等真实查单仍需账号、证书和联调环境验证 | 外部通道台账项 |
| 统一调度平台编排 | 支付模块只暴露任务入口，不建设统一任务平台、任务编排中心或调度 UI | 平台统一调度能力 |
| 监控告警 | 通道异常、订单积压、对账差异、退款失败、证书过期等告警不属于 `PAY-SCHED-001` 的任务入口闭环 | `PAY-OBS-001` |
