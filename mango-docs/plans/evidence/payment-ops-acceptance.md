# 支付后台人工操作验收证据

## 1. 验收范围

- 台账项：`PAY-OPS-002`
- 设计来源：统一支付系统设计说明书 13.2
- 后端接口：`POST /payment/exception-orders/handle`、`POST /payment/differences/handle`、`POST /payment/notification-records/retry`、`POST /payment/notification-records/deliver-due`
- 后端服务：`PaymentReadonlyResourceService`
- 数据：`payment_exception_order`、`payment_difference`、`payment_notification_record`、`payment_transaction_flow`、`payment_operation_audit`

## 2. 功能验收记录

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| PAY-OPS-002 | `POST /payment/exception-orders/handle` | 异常订单人工关闭不直接置成功 | `handleAction=MANUAL_CLOSE`、关联支付单号 `PO2026061000000006` | 只更新异常单处理字段和写 `HANDLE_EXCEPTION_ORDER / PAYMENT_EXCEPTION_ORDER / SUCCESS` 审计；不调用 `PaymentOrderMapper`、`PaymentRefundOrderMapper`、`PaymentBusinessOrderMapper`；不调用查单、关单、查退款服务 | 后端服务单测，不涉及页面布局 | Maven Surefire 通过 | `PaymentReadonlyResourceServiceTest.handleExceptionOrder_manualClose_doesNotTouchOrderSuccessState` | DONE |
| PAY-OPS-002 | `POST /payment/differences/handle` | 差异处理补单动作不直接置成功 | `processAction=SUPPLEMENT_ORDER`、差异类型 `CHANNEL_SUCCESS_LOCAL_MISSING` | 只更新差异处理字段、创建 0 分 `ADJUST_NOTE` 备注流水并写审计；不调用支付单、退款单、业务订单成功更新 mapper；不触发查单、关单、查退款或通知投递 | 后端服务单测，不涉及页面布局 | Maven Surefire 通过 | `PaymentReadonlyResourceServiceTest.handleDifference_supplementOrder_doesNotTouchOrderSuccessState` | DONE |
| PAY-OPS-002 | `PaymentReadonlyResourceController` | 后台人工接口不暴露直接置成功路径 | Controller 所有 `PostMapping` 路径 | 后台 POST 路径不包含 `manual-success`、`mark-success`、`set-success`、`force-success`、`success/manual` 等直接人工置成功语义 | 反射测试，不涉及页面布局 | Maven Surefire 通过 | `PaymentReadonlyResourceControllerTest.manualOperationEndpoints_doNotExposeDirectSuccessMutationPaths` | DONE |
| PAY-OPS-002 | 成功状态推进服务 | 成功状态必须有可信依据 | 收银台支付、通道回调、主动查单、主动查退款、账单对账 | 成功资金状态推进集中在收银台支付、通道回调、主动查单/查退款和账单对账相关服务；后台异常/差异/通知人工操作只登记处理、审计或触发已有通知投递，不新增人工置成功入口 | 不涉及独立 UI | 代码审查 + 既有单测证据 | `PaymentCashierServiceImplTest`、`PaymentChannelCallbackServiceTest`、`PaymentChannelOrderQueryServiceTest`、`PaymentChannelRefundQueryServiceTest`、`PaymentReconciliationMapperIntegrationTest` | DONE |

## 3. 验证命令

```bash
mvn -f mango/pom.xml -pl mango-platform/mango-payment/mango-payment-core,mango-platform/mango-payment/mango-payment-starter -am -Dtest=PaymentReadonlyResourceServiceTest,PaymentReadonlyResourceControllerTest -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false
node mango-pmo/tools/acceptance-evidence-check.mjs --evidence mango-docs/plans/evidence/payment-ops-acceptance.md
```

## 4. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 | 用户确认 |
|---|---|---|---|---|
| 外部真实通道成功状态依据 | 本证据只覆盖后台人工操作不得直接置成功；通联、华夏、微信、支付宝、连连等外部通道回调验签、查单和账单联调仍按各通道台账推进 | 不能据此声明外部真实通道投产完成 | 保持 `PAY-CHANNEL-003/005/007/008` 等外部通道项未完成，取得资料和联调环境后逐通道验收 | 不适用 |
| 全量人工操作权限审计 | 本证据覆盖“不得直接改成功状态”；`PAY-OPS-001` 覆盖更广的人工关单、重推、退款审批、差异确认、汇总确认权限和审计全量面 | 不能据此把全部后台人工操作受控项改为完成 | `PAY-OPS-001` 继续按生产就绪台账补全 | 不适用 |
