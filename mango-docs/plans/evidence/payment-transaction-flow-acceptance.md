# 交易流水列表页验收证据

## 1. 验收范围

- 页面：支付中心 / 交易订单 / 交易流水
- 接口：`GET /payment/transaction-flows/page`、`GET /payment/transaction-flows/detail`
- 权限：`payment:transaction-flow:list`、`payment:transaction-flow:query`
- 数据：`payment_transaction_flow`，关联 `payment_business_order`、`payment_order`、`payment_refund_order`
- 部署形态：本地单体后端 + Mango Admin 单体前端

## 2. 执行环境

- 前端地址：`http://127.0.0.1:7808`
- 后端地址：`http://127.0.0.1:18118`
- 数据库或租户：`mango_dev_e397cd`，租户 `1`
- 测试账号：`admin`
- 浏览器：Playwright Chromium

## 3. 功能验收记录

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| PAY-MENU-010 | `/payment/transaction-flows/page` | 按关键字查询交易流水 | `FLOW-LIST-E2E-*` 业务订单通过真实收银台支付后生成 `PAY_SUCCESS` 流水 | 返回流水号、业务订单号、支付订单号、流水类型、类型中文名、金额、币种和时间字段 | 列表普通业务字段和流水类型均为普通文本；流水号、业务订单号、支付订单号、流水类型未使用边框或标签样式 | 接口业务成功，无 console error、pageerror、交易流水接口 requestfailed | `test-results/payment-transaction-flows.png` | DONE |
| PAY-MENU-010 | `/payment/transaction-flows/detail` | 查询交易流水详情 | `/payment/transaction-flows/page` 返回的真实流水 ID | 返回流水信息、关联订单和时间信息；不存在时使用 `PaymentCode.PAYMENT_READONLY_RESOURCE_NOT_FOUND` | 详情抽屉按流水信息、关联订单、时间信息分组；无文字重叠或控件挤压 | 接口业务成功，无 console error、pageerror、交易流水接口 requestfailed | `test-results/payment-transaction-flows.png` | DONE |
| PAY-MENU-010 | 交易流水页面 | 不可编辑、不可删除 | 真实页面行操作 | 行操作仅显示“详情”，不显示“新增/编辑/删除” | 符合“交易流水不得物理删除”规则 | E2E 使用真实登录态通过 | `PLAYWRIGHT... --grep "交易流水列表"` | DONE |
| PAY-DOMAIN-004 | `/payment/transaction-flows` | 删除接口不可用 | `FLOW-LIST-E2E-*` 真实流水 ID | `DELETE /api/payment/transaction-flows?id={id}` 返回 404 或 405；随后 `GET /payment/transaction-flows/detail` 仍能查询同一流水号 | 后台无删除入口，资金相关历史记录不可物理删除 | E2E 使用真实登录态通过；后端反射测试确认无 `DeleteMapping` | `PaymentReadonlyResourceControllerTest`、`PLAYWRIGHT... --grep "交易流水列表"` | DONE |
| PAY-DOMAIN-004 | 前端 API | 交易流水前端 API 只读 | `paymentTransactionFlowApi` | 仅暴露 `page/detail`，不再从通用资源 API 暴露 `create/update/remove` | 页面不能通过类型层 API 发起删除 | `pnpm -F mango-admin build` 通过 | `mango-ui/packages/payment/src/api/payment.ts` | DONE |
| PAY-MENU-010 | 交易流水页面 | 搜索无结果空态 | `NO-TRANSACTION-FLOW-*` | 表格为空且显示“未查询到匹配的交易流水” | 空态明确区分查询无结果，不显示空白表格 | 接口业务成功，无 console error、pageerror、交易流水接口 requestfailed | `test-results/payment-transaction-flows.png` | DONE |
| PAY-MENU-010 | 权限迁移 | 查询权限入库 | `authorization/V46__payment_transaction_flow_query_permission.sql` | `payment:transaction-flow:query` 绑定 ROLE_ADMIN；列表接口使用 `payment:transaction-flow:list` | 后台登录后菜单进入页面并可打开详情 | E2E 使用真实登录态通过 | `PLAYWRIGHT... --grep "交易流水列表"` | DONE |
| PAY-DOMAIN-004 / PAY-RECON-003 | `payment_transaction_flow`、`payment_difference` | 差异处理备注流水生成和延迟关联追溯 | 差异单 `DIFF-001`，处理动作 `CLOSE` | 差异处理创建 `ADJUST_NOTE` 0 分流水；差异单保存 `adjust_flow_id/adjust_flow_no`，满足“交易流水与支付订单、退款订单可以延迟关联，但必须有差异处理记录”；不新增删除接口，不改变原交易金额 | 后端服务单测，不涉及页面布局 | `PaymentReadonlyResourceServiceTest` 断言流水插入和差异单回填 | `PaymentReadonlyResourceServiceTest` | DONE |
| PAY-DOMAIN-004 / PAY-RECON-001 / PAY-RECON-002 | `PaymentReconciliationService.generateMangoPayVirtualBill`、`PaymentReconciliationService.importReconciliation`、`payment_transaction_flow` | 通道手续费生成 `CHANNEL_FEE` 资金流水 | `tradeType=PAYMENT/REFUND`，账单手续费 `12/8` 分，支付订单 `PO001`，退款订单 `RO001` | 匹配成功的支付账单手续费生成 `CHANNEL_FEE` 流水并关联 `business_order_id/payment_order_id`；匹配成功的退款账单手续费生成 `CHANNEL_FEE` 流水并关联原业务订单、原支付订单和退款订单；流水金额以分为单位保存 | 后端服务单测，不涉及页面布局 | `PaymentReconciliationServiceTest.generateMangoPayVirtualBill_withRealRows_createsMatchedBatch` 断言插入两条 `CHANNEL_FEE` 流水、金额和关联 ID | `PaymentReconciliationServiceTest` | DONE |
| PAY-DOMAIN-004 / PAY-RECON-002 | `PaymentReconciliationService.importReconciliation`、`payment_transaction_flow`、`payment_difference` | 手续费流水幂等和不一致差异识别 | 已存在 `CHANNEL_FEE` 流水金额 `12/10` 分；账单手续费 `12` 分 | 已存在同支付订单或退款订单的 `CHANNEL_FEE` 且金额一致时不重复插入；已存在手续费流水金额与账单手续费不一致时不覆盖原流水，生成 `CHANNEL_FEE_MISMATCH` 差异，差异金额按分计算 | 后端服务单测，不涉及页面布局 | `PaymentReconciliationServiceTest.importReconciliation_existingChannelFeeFlow_doesNotDuplicate`、`PaymentReconciliationServiceTest.importReconciliation_existingChannelFeeFlowAmountMismatch_createsDifference` | `PaymentReconciliationServiceTest` | DONE |
| PAY-DOMAIN-004 / PAY-SETTLE-001 | `PaymentReconciliationService.importReconciliation` | 独立手续费账单项不伪造交易流水 | `tradeType=FEE`，`channelTradeNo=MANGO-FEE-001`，手续费 `12` 分 | 独立 `FEE` 账单项保留在通道账单和结算统计语义内，不生成没有支付订单或退款订单依据的 `CHANNEL_FEE` 假流水，不生成差异单 | 后端服务单测，不涉及页面布局 | `PaymentReconciliationServiceTest.importReconciliation_standaloneFeeBillItem_doesNotCreateFlow` | `PaymentReconciliationServiceTest` | DONE |

## 4. 回归抽查记录

| 模块 | 页面 | 功能点 1 | 功能点 2 | UI 细节 | 截图/trace | 结论 |
|---|---|---|---|---|---|---|
| 支付中心 | 交易流水 | 页面可通过支付中心 / 交易订单 / 交易流水进入 | 查询、详情抽屉、空态真实可用 | Element Plus 表单、表格、分页、抽屉布局稳定；列表字段不加边框或标签 | `test-results/payment-transaction-flows.png` | DONE |

## 5. 验证命令

```bash
mvn -pl mango-platform/mango-payment/mango-payment-core,mango-platform/mango-payment/mango-payment-starter -am -Dtest=PaymentReadonlyResourceServiceTest,PaymentReadonlyResourceControllerTest -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false
mvn -f mango/pom.xml -pl mango-platform/mango-payment/mango-payment-core -am -Dtest=PaymentReadonlyResourceServiceTest,PaymentReconciliationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false
mvn -f mango/pom.xml -pl mango-platform/mango-payment/mango-payment-core,mango-platform/mango-payment/mango-payment-starter -am -Dtest=MangoPayVirtualPaymentServiceTest,PaymentMangoPayChannelAdapterTest,PaymentCashierServiceImplTest,PaymentMethodRouteServiceImplTest,PaymentChannelOrderQueryServiceTest,PaymentChannelRefundQueryServiceTest,PaymentReconciliationServiceTest,PaymentSettlementSummaryServiceTest,PaymentReadonlyResourceServiceTest,PaymentMangoPayScenarioControlServiceTest,PaymentDuplicatePaymentServiceTest,PaymentNotificationServiceTest,PaymentOpenApiServiceTest -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false
pnpm -F mango-admin build
PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true PLAYWRIGHT_BASE_URL=http://127.0.0.1:7808 PLAYWRIGHT_API_BASE_URL=http://127.0.0.1:18118 pnpm -F mango-admin exec playwright test e2e/specs/payment-center.spec.ts --project=chromium --workers=1 --grep "支付订单列表|退款订单列表|交易流水列表"
node mango-pmo/tools/acceptance-evidence-check.mjs --evidence mango-docs/plans/evidence/payment-transaction-flow-acceptance.md
```

## 6. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 | 用户确认 |
|---|---|---|---|---|
| 外部通道真实账单解析 | 本证据覆盖通道账单导入和芒果支付账单生成进入 `CHANNEL_FEE` 流水的支付域处理；通联、华夏、微信、支付宝等外部机构账单解析属于对应外部通道交付项 | 不影响 `PAY-DOMAIN-004` 交易流水不可删除、差异追溯和手续费流水生成链路完成；不能据此声明外部通道接入完成 | 后续按 `PAY-CHANNEL-003/005/007/008` 在具备联调资料后逐通道验证 | 不适用 |
