# 支付后台人工操作受控验收证据

## 1. 验收范围

- 台账项：`PAY-OPS-001`
- 设计来源：统一支付系统设计说明书 7.4、7.5、13
- 后端接口：异常订单处理、通知重推、到期通知投递、任务主动查单、退款审批、差异处理、对账导入、结算汇总生成/确认/作废
- 权限来源：`mango-authorization-core/src/main/resources/db/migration/authorization`
- 审计表：`payment_operation_audit`

## 2. 功能验收记录

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| PAY-OPS-001 | `PaymentReadonlyResourceController` | 后台人工操作接口权限 | 反射读取 `queryRefundOrder`、`createRefundApproval`、`reviewRefundApproval`、`handleExceptionOrder`、`retryNotificationRecord`、`deliverDueNotificationRecords`、`expireOpenPaymentOrders`、`queryProcessingPaymentOrders`、`importReconciliation`、`generateMangoPayVirtualBill`、`handleDifference`、`generateSettlementSummary`、`confirmSettlementSummary`、`voidSettlementSummary` | 每个写操作入口均存在 `@ApiAccess`；权限码分别为退款查单、退款审批、异常处理、通知重推、到期投递、批量关单、批量查单、对账导入、差异处理、结算生成、结算确认、结算作废等独立权限 | Controller 契约测试，不涉及页面布局 | Maven Surefire 通过，未出现测试失败 | `PaymentReadonlyResourceControllerTest.payOps001ManualOperationEndpoints_useDedicatedPermissions` | DONE |
| PAY-OPS-001 | 授权 Flyway migration | 人工操作权限初始化 | `payment:refund-order:query-channel`、`payment:refund-approval:create`、`payment:refund-approval:review`、`payment:exception-order:handle`、`payment:notification-record:retry`、`payment:notification-record:deliver-due`、`payment:task:expire-open-orders`、`payment:task:query-processing-orders`、`payment:reconciliation:import`、`payment:difference:handle`、`payment:settlement-summary:generate`、`payment:settlement-summary:confirm`、`payment:settlement-summary:void` | 所有人工操作权限码均能在授权模块 Flyway SQL 中找到初始化记录，避免只有 Controller 注解、没有菜单/权限数据 | SQL 静态契约测试，不涉及页面布局 | Maven Surefire 通过，读取真实 migration 文件 | `PaymentReadonlyResourceControllerTest.payOps001ManualOperationPermissions_areInitializedByAuthorizationMigrations` | DONE |
| PAY-OPS-001 | `PaymentReadonlyResourceService` | 异常订单处理、人工关单、主动查单、通知重推、到期投递、批量关单、批量查单、差异处理审计 | `EX-*`、`NT-*`、`DIFF-*`、`PO20260606120516*` | 异常处理写 `HANDLE_EXCEPTION_ORDER`；单笔关单写 `CLOSE_PAYMENT_ORDER`；通知重推写 `RETRY_NOTIFICATION_RECORD`；到期投递写 `DELIVER_DUE_NOTIFICATION_RECORDS`；批量关单写 `EXPIRE_OPEN_PAYMENT_ORDERS`；批量主动查单写 `QUERY_PROCESSING_PAYMENT_ORDERS`；差异处理写 `HANDLE_DIFFERENCE` | 后端服务单测，不涉及页面布局 | Maven Surefire 通过，批量任务单笔失败只计数不阻断批次 | `PaymentReadonlyResourceServiceTest`、`PaymentChannelOrderCloseServiceTest` | DONE |
| PAY-OPS-001 | `PaymentRefundApprovalService` | 退款审批创建、通过、拒绝审计 | `RFA202606070001`、`MANUAL-REFUND-001` | 创建审批写 `CREATE_REFUND_APPROVAL / SUCCESS`；审核通过走共享退款申请链路并写 `APPROVE_REFUND_APPROVAL / SUCCESS`；审核拒绝只关闭审批单并写 `REJECT_REFUND_APPROVAL / REJECTED` | 后端服务单测，不涉及页面布局 | Maven Surefire 通过，申请人自审被拒绝 | `PaymentRefundApprovalServiceTest` | DONE |
| PAY-OPS-001 | `PaymentSettlementSummaryService` | 结算汇总生成、确认、作废审计 | `500001`、账单日期 `2026-06-01` | 生成写 `GENERATE_SETTLEMENT_SUMMARY`；确认前校验已完成对账且无未处理差异，确认写 `CONFIRM_SETTLEMENT_SUMMARY`；作废只允许已确认汇总并写 `VOID_SETTLEMENT_SUMMARY` | 后端服务单测，不涉及页面布局 | Maven Surefire 通过，跨租户和未处理差异场景被拒绝 | `PaymentSettlementSummaryServiceTest` | DONE |
| PAY-OPS-001 | `PaymentReconciliationService` | 对账导入和芒果支付账单生成审计 | `MANGO_PAY`、账单日期 `2026-06-06` | 对账导入和芒果支付账单生成形成真实对账批次；生成芒果支付账单写 `GENERATE_MANGO_PAY_CHANNEL_BILL / PAYMENT_RECONCILIATION / SUCCESS`；人工导入在服务中写 `IMPORT_RECONCILIATION` 审计 | 后端服务单测，不涉及页面布局 | Maven Surefire 通过，重复账单摘要被拒绝 | `PaymentReconciliationServiceTest`、`PaymentReconciliationService` | DONE |

## 3. 验证命令

```bash
mvn -f mango/pom.xml -pl mango-platform/mango-payment/mango-payment-core,mango-platform/mango-payment/mango-payment-starter -am -Dtest=PaymentReadonlyResourceControllerTest,PaymentReadonlyResourceServiceTest,PaymentRefundApprovalServiceTest,PaymentSettlementSummaryServiceTest,PaymentChannelOrderCloseServiceTest,PaymentReconciliationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
node mango-pmo/tools/acceptance-evidence-check.mjs --evidence mango-docs/plans/evidence/payment-ops-control-acceptance.md --min-rows 6
node mango-pmo/tools/delivery-contract-check.mjs --design mango-docs/designs/统一支付系统设计说明书.md --ledger mango-docs/plans/2026-05-25-payment-delivery-ledger.md --mode verify
```

## 4. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 | 用户确认 |
|---|---|---|---|---|
| 外部真实通道人工查单联调 | 当前人工查单入口复用通道 SPI，芒果支付已可验证；通联、华夏、微信、支付宝、连连等真实机构账号、证书和联调环境尚未提供 | 不能据此声明外部真实通道投产 | 归属 `PAY-CHANNEL-003/005/007/008` 等外部通道台账项 | 不适用 |
| 全量前端 E2E 回归 | 本次收口重点是权限和审计契约，未重新跑支付中心全量 Playwright E2E | 不影响后台权限/审计单元证据，但不能替代最终投产 E2E | 归属 `PAY-TEST-002`、`PAY-TEST-004` 和最终验收 | 不适用 |
