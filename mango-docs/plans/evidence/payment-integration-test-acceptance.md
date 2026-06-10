# 支付全链路集成测试验收证据

## 1. 验收范围

- 台账项：`PAY-TEST-002`
- 目标：证明支付中心已具备可本地复验的 API、数据库约束、并发、通知、对账和结算集成测试证据。
- 覆盖链路：创建业务订单、发起支付、查询支付、退款、标准化通道回调、主动查单、主动查退款、业务通知、对账导入/生成、差异识别、结算汇总生成/确认/作废。
- 数据物料：`payment_%` 真实表、H2 MySQL 模式 JDBC 约束表、生产 MyBatis XML、真实本地 HTTP ACK 接收服务、Playwright 真实前后端和数据库 E2E。
- 边界说明：服务级行为测试可证明状态机分支和副作用边界；本证据声明集成完成时只采信真实数据库、真实 HTTP、生产 XML 或真实前后端数据库 E2E 作为主证据。

## 2. 执行环境

- 工作区：`/Users/hardy/Work/mango/.claude/worktrees/payment-platform`
- 后端地址：`http://127.0.0.1:18118`
- 前端地址：`http://127.0.0.1:7808`
- 数据库：`mango_dev_e397cd`
- 浏览器：Playwright Chromium
- 后端测试：Maven Surefire

## 3. 功能验收记录

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| PAY-TEST-002 | `/openapi/pay/orders`、`/openapi/pay/orders/{bizOrderNo}/pay`、`/openapi/pay/refunds` | 开放接口创建订单、发起支付、退款和查询链路 | `OPENAPI-BO-*`、`OPENAPI-RF-*`、`app_openapi_e2e`、`MANGO_PAY`、金额 `128800/38800` 分 | Playwright 通过真实后端、真实签名、真实数据库完成创建订单、幂等、签名失败、nonce 防重放、发起支付、查询支付、退款、查询退款和凭证；支付和退款均落库真实订单、流水和通知记录 | API-only：不涉及页面布局；收银台和后台查询由全量 E2E 另行覆盖 | Playwright 请求走 `http://127.0.0.1:18118` 后端；业务断言覆盖成功和业务错误码分支；未使用接口拦截 | `payment-openapi-acceptance.md`；`payment-full-e2e-acceptance.md`；`payment-center.spec.ts` 开放接口用例 | DONE |
| PAY-TEST-002 | `payment_order`、`payment_mango_pay_virtual_payment` | 数据库唯一约束和并发成功保护 | H2 MySQL 模式 JDBC 数据：同一业务订单两笔支付、同一通道交易号、同一芒果支付订单号 | `PaymentOrderUniqueConstraintMigrationTest` 使用真实 JDBC 验证通道交易号唯一、同业务订单只允许一笔有效成功支付，并发更新时只有一笔可成为 `success_flag=1`；`PaymentVirtualChannelPaymentUniqueConstraintMigrationTest` 验证芒果支付虚拟通道支付号唯一 | 数据库集成测试：不涉及页面布局，直接验证 DDL 约束行为 | Maven Surefire 执行 H2 MySQL 模式测试；断言来自数据库唯一索引异常和并发更新结果 | `payment-data-unique-constraints-acceptance.md`；`PaymentOrderUniqueConstraintMigrationTest`；`PaymentVirtualChannelPaymentUniqueConstraintMigrationTest` | DONE |
| PAY-TEST-002 | `POST /payment/channel-callbacks`、主动查单/查退款服务 | 标准化回调、查单、查退款状态推进和竞争保护 | 支付订单 `PO202606060001`、退款订单 `RO202606060001`、通道交易号 `CH202606060001`、`MANGO_PAY` | 标准化回调校验通道、商户号、金额和本地订单状态；支付回调推进 `PAYING -> SUCCESS/FAILED`；退款回调推进 `REFUNDING/PROCESSING -> SUCCESS/FAILED`；主动查单和主动查退款复用通道适配器结果，CAS 失败时不重复写成功流水、不重复通知 | 后端行为测试：不涉及页面布局；异常订单和退款订单页面操作由全量 E2E 覆盖 | Maven Surefire 覆盖支付回调、退款回调、主动查单、主动查退款和竞争分支；Playwright 覆盖页面触发真实查单/查退款 | `payment-openapi-acceptance.md`；`payment-mango-pay-acceptance.md`；`PaymentChannelCallbackServiceTest`；`PaymentChannelOrderQueryServiceTest`；`PaymentChannelRefundQueryServiceTest` | DONE |
| PAY-TEST-002 | `PaymentNotificationService`、`/payment/notification-records/deliver-due` | 业务通知真实 HTTP ACK、失败重试和到期投递 | 本地 `HttpServer` ACK 接收器、通知事件 `PAYMENT_SUCCESS/PAYMENT_FAILED/PAYMENT_CLOSED/REFUND_SUCCESS/REFUND_FAILED` | 通知服务向本地真实 HTTP 服务发送签名后的业务通知；ACK 为 `SUCCESS` 时通知记录更新成功；ACK 非成功时按应用重试策略推进，耗尽后保留人工补偿入口；到期投递入口只处理 `next_retry_time <= now` 的真实记录 | 通知记录页面 E2E 覆盖状态筛选、成功/失败记录回显和重推按钮展示规则 | Maven 使用真实本地 HTTP 服务收包并断言请求体；Playwright 覆盖通知记录页面和自动调度投递回显 | `payment-openapi-acceptance.md`；`payment-mango-pay-acceptance.md`；`PaymentNotificationServiceTest`；`PaymentNotificationDispatchSchedulerTest`；`payment-notification-auto-dispatch.png` | DONE |
| PAY-TEST-002 | `PaymentReconciliationMapper.xml`、`/payment/reconciliations/import`、`/payment/reconciliations/mango-pay/virtual/generate` | 对账导入、芒果支付账单生成和差异识别 | `RC-BIZ-E2E-*`、`special-recon-*.csv`、`PO-MISSING`、`RO-MISSING`、通道 `MANGO_PAY` | 对账导入生成真实批次和账单明细；重复文件摘要拒绝；本地无单、金额不一致、本地成功账单缺失生成差异；生产 MyBatis XML 在 H2 MySQL 模式下只返回同租户、同通道、同账单日、成功且账单缺失的支付/退款记录 | 对账管理页面 E2E 覆盖搜索、状态下拉、导入弹窗、生成芒果支付账单、详情抽屉和列表字段展示 | Playwright 使用真实接口和数据库；Maven 加载生产 Mapper XML 验证 SQL 行为 | `payment-reconciliation-acceptance.md`；`PaymentReconciliationServiceTest`；`PaymentReconciliationMapperIntegrationTest`；`payment-reconciliations-special-bill.png` | DONE |
| PAY-TEST-002 | `/payment/settlement-summaries/generate`、`/confirm`、`/void` | 结算汇总生成、确认、作废和未处理差异拦截 | `SETTLE-BO-*`、`SETTLE-PO-*`、`SETTLE-RO-*`、`SETTLE-RC-*`、支付 `128800` 分、退款 `38800` 分、手续费 `260` 分 | 结算按日、租户、应用、主体、通道汇总支付、退款、手续费和净收款；重复生成拒绝；未处理差异阻断确认；差异处理后可确认；作废后可重新生成新汇总 | 结算汇总页面 E2E 覆盖列表、状态筛选、生成按钮、确认提示、作废原因弹窗和重新生成按钮 | Playwright 调用真实后端接口并查询页面回显；Maven 覆盖汇总金额计算和状态分支 | `payment-settlement-summary-acceptance.md`；`PaymentSettlementSummaryServiceTest`；`payment-settlement-summaries.png` | DONE |
| PAY-TEST-002 | `e2e/specs/payment-center.spec.ts` | 支付中心全量真实前后端数据库 E2E | `PAY-E2E-*`、`OPENAPI-BO-*`、`RO-E2E-*`、`NT-*`、`RC-*`、`MANGO_PAY` | 全量 E2E `28 passed`，覆盖支付中心菜单、应用、主体、通道、签约、支付方式、路由、收银台、芒果支付付款、支付结果延迟轮询、业务订单支付按钮、订单、退款、流水、异常、通知、审计、对账、差异、结算和列表 UI 约束 | Chromium 截图覆盖订单、退款、流水、通知、对账、异常、审计和列表 UI；普通字段纯文本，语义字段用 Tag | Playwright 监听 console error、pageerror 和支付相关 requestfailed；关键页面接口均有业务断言 | `payment-full-e2e-acceptance.md`；`mango-ui/apps/mango-admin/playwright-report/index.html`；`test-results/payment-*.png` | DONE |
| PAY-TEST-002 | `PaymentMangoPayChannelAdapter`、`PaymentChannelAdapterRegistry` | 芒果支付内置虚拟通道通过统一通道 SPI 接入主链路 | `channelCode=MANGO_PAY`、签约配置 `333001`、支付/退款/账单场景控制 | 收银台支付、OpenAPI 退款、主动查单、主动查退款和对账账单生成均通过通道适配器注册表分发；未注册外部通道按业务码拒绝，不用占位实现伪装完成；账单生成从真实成功支付/退款订单读取数据 | 后端 SPI 测试不涉及页面布局；收银台和对账页面由 E2E 验证真实交互 | Maven 验证适配器分发、芒果支付支付/退款/查单/账单行为；Playwright 验证收银台和对账页面触发真实链路 | `payment-mango-pay-acceptance.md`；`PaymentChannelAdapterRegistryTest`；`PaymentMangoPayChannelAdapterTest`；`PaymentCashierServiceImplTest` | DONE |

## 4. 验证命令

```bash
mvn -f mango/pom.xml -pl mango-platform/mango-payment/mango-payment-core,mango-platform/mango-payment/mango-payment-starter -am -Dtest=PaymentOpenApiServiceTest,PaymentCashierServiceImplTest,PaymentChannelOrderQueryServiceTest,PaymentChannelCallbackServiceTest,PaymentChannelRefundQueryServiceTest,PaymentNotificationServiceTest,PaymentReconciliationServiceTest,PaymentReconciliationMapperIntegrationTest,PaymentSettlementSummaryServiceTest,PaymentOrderUniqueConstraintMigrationTest,PaymentVirtualChannelPaymentUniqueConstraintMigrationTest,PaymentReadonlyResourceServiceTest,PaymentNotificationDispatchSchedulerTest -Dsurefire.failIfNoSpecifiedTests=false test
PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true PLAYWRIGHT_BASE_URL=http://127.0.0.1:7808 PLAYWRIGHT_API_BASE_URL=http://127.0.0.1:18118 pnpm --dir mango-ui/apps/mango-admin exec playwright test e2e/specs/payment-center.spec.ts --project=chromium --workers=1
node mango-pmo/tools/acceptance-evidence-check.mjs --evidence mango-docs/plans/evidence/payment-integration-test-acceptance.md --min-rows 8
```

## 5. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 | 用户确认 |
|---|---|---|---|---|
| 外部真实通道集成 | 本证据使用芒果支付内置虚拟通道和标准化回调入口，不包含通联、华夏、微信、支付宝等机构公网回调验签、真实查单、真实退款和真实账单解析 | 不能据此关闭 `PAY-CHANNEL-003/005/007/008` | 后续拿到通道账号、证书、验签规则和联调环境后逐通道补集成证据 | 不适用 |
| 长周期压力和生产监控样本 | 本证据覆盖本地可复验链路、到期通知和一次性 E2E，不覆盖多日长周期压力、生产监控样本和外部机构真实告警 | 不影响 `PAY-TEST-002` 本地集成测试关闭；不能替代生产运维验收 | 外部通道联调和生产试运行时补充监控样本 | 不适用 |
| 测试替身边界 | 部分服务级测试为隔离 Mapper、审计服务或通道协作者使用测试替身，不能单独证明数据库或外部机构联调 | 本证据已将真实数据库、真实 HTTP、生产 XML 和真实前后端数据库 E2E 作为集成主证据，服务级测试只作为状态机补充 | 后续新增集成能力时优先使用真实集成物料或等价嵌入式环境 | 不适用 |
