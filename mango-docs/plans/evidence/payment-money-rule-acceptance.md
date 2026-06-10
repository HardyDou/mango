# 支付金额规则验收证据

## 1. 验收范围

- 页面：`/#/payment/cashier-configs/:cashierId/cashier`
- 接口：`/api/payment/cashier/pay`、`/api/payment/channel-contracts`
- 权限：支付中心登录态接口权限链路
- 数据：金额数据库字段以 `bigint` 分为单位；费率字段 `payment_channel_contract_capability.fee_rate decimal(10,10)`
- 部署形态：单体管理后台，前端 `http://127.0.0.1:7808`，后端 `http://127.0.0.1:18118`

## 2. 执行环境

- 前端地址：`http://127.0.0.1:7808`
- 后端地址：`http://127.0.0.1:18118`
- 数据库或租户：`mango_dev_e397cd` / tenant `1`
- 测试账号：`admin`
- 浏览器：Playwright Chromium

## 3. 功能验收记录

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| PAY-DATA-003 | `mango-payment-core` / `Money` | 金额过程计算保留高精度小数，最终按业务语义收敛为整数分 | `12345` 分乘以费率 `0.006789`，得到过程值 `83.81020500` 分；`0.4900` 分和 `0.5000` 分用于验证正整数分边界 | `Money.preciseCents().scale()` 大于等于 `4`；最终 `toNonNegativeCents()` 按 `HALF_UP` 得到 `84` 分；`toPositiveCents("付款金额")` 拒绝四舍五入后为 `0` 分的付款金额并接受 `1` 分；负数和超过 `Long.MAX_VALUE` 的最终金额按 `PaymentCode.PAYMENT_AMOUNT_INVALID` 抛业务异常 | 不涉及独立页面；该值对象用于支付域后台和收银台金额边界，避免页面或接口散落元分换算 | Maven 单测执行真实 Java 代码，未使用 mock 金额计算；异常为业务异常，不是原生溢出异常 | `mvn -pl mango-platform/mango-payment/mango-payment-core -am -Dtest=MoneyTest,PaymentChannelContractServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false` | DONE |
| PAY-DATA-003 | `/api/payment/channel-contracts` | 签约能力限额使用统一 Money 规则校验 | `SavePaymentChannelContractCapabilityCommand.minAmount=-1`、`maxAmount=880000`、`feeRate=0.0060000000` | 后端拒绝负数限额，错误消息为“签约能力最小金额不能小于 0 分”；正常费率允许保留 10 位小数；签约能力删除和更新仍保持路由关系保护 | 通道签约配置页面仍使用 Element Plus 表单和列表；签约能力限额字段没有引入框框展示，状态字段保持语义 tag | 单测和 Playwright 均调用真实服务逻辑；E2E 覆盖签约能力真实保存、回显、删除审计和动态字段模板，不拦截接口响应 | `mango-ui/apps/mango-admin/e2e/specs/payment-center.spec.ts`；`mvn -pl mango-platform/mango-payment/mango-payment-core,mango-platform/mango-payment/mango-payment-starter -am test -DskipTests=false` | DONE |
| PAY-DATA-003 | `/api/payment/cashier/pay` | 芒果支付通道和收银台支付金额在入库前统一收敛为正整数分 | E2E 动态订单 `PAY-DELAY-E2E-*`，金额 `120000` 分；单测覆盖 `0.4900` 分拒绝、`0.5000` 分收敛为 `1` 分 | `MangoPayVirtualPaymentService.pay` 和 `PaymentCashierServiceImpl.pay` 均通过 `Money.cents(...).toPositiveCents("付款金额")` 后写入付款相关表并返回；收银台延迟结果用例支付提交后能轮询到真实终态成功 | 收银台页面展示订单金额、支付方式、二维码、处理中和成功状态；没有金额文本遮挡或布局错位 | Playwright Chromium 走真实前后端和本地数据库，`/payment/cashier/pay`、`/payment/cashier/pay-result` 返回符合业务断言；未出现 console/network 业务断言外异常 | `PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true PLAYWRIGHT_BASE_URL=http://127.0.0.1:7808 PLAYWRIGHT_API_BASE_URL=http://127.0.0.1:18118 pnpm -F mango-admin exec playwright test e2e/specs/payment-center.spec.ts --project=chromium --workers=1` | DONE |
| PAY-DATA-003 | `/openapi/pay/refunds` | 退款可退金额和累计占用金额使用统一 Money 规则校验 | 原支付 `8800` 分，已占用退款 `9000` 分，申请退款 `100` 分 | `PaymentOpenApiService.refund` 使用 `Money.cents(paymentOrder.getAmount()).subtract(Money.cents(occupyingRefundAmount)).toNonNegativeCents()`，占用金额超过原支付金额时按业务异常拒绝，避免裸 `long` 减法产生非法可退金额 | 不涉及独立页面；开放接口仍保持原签名和幂等协议 | Maven 单测调用真实服务逻辑和签名校验，未 mock 金额计算结果 | `mvn -f mango/pom.xml -pl mango-platform/mango-payment/mango-payment-core -am -Dtest=MoneyTest,PaymentOrderStateServiceTest,PaymentReconciliationServiceTest,PaymentSettlementSummaryServiceTest,PaymentOpenApiServiceTest,PaymentCashierServiceImplTest,MangoPayVirtualPaymentServiceTest,PaymentChannelContractServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false` | DONE |
| PAY-DATA-003 | 对账导入 / 芒果支付账单 | 账单金额、手续费汇总、差异金额和异常场景差异调整通过 Money 收敛 | 导入两条账单：`Long.MAX_VALUE` 分和 `1` 分触发汇总溢出；账单差异 `AMOUNT_PLUS` 在 `Long.MAX_VALUE + 1` 时触发溢出 | `PaymentReconciliationService` 对 `totalAmount`、`totalFee`、账单差异绝对值和芒果支付通道账单差异调整使用 `Money`；金额为负或超过 `Long.MAX_VALUE` 时按 `PaymentCode.PAYMENT_AMOUNT_INVALID` 失败 | 对账管理页面不受本次改动影响；列表普通字段仍为纯文本，状态字段使用语义 tag | Maven 单测验证真实 Java 代码，覆盖对账导入、退款账单金额不一致、芒果支付通道账单差异和溢出边界 | `mvn -f mango/pom.xml -pl mango-platform/mango-payment/mango-payment-core -am -Dtest=MoneyTest,PaymentOrderStateServiceTest,PaymentReconciliationServiceTest,PaymentSettlementSummaryServiceTest,PaymentOpenApiServiceTest,PaymentCashierServiceImplTest,MangoPayVirtualPaymentServiceTest,PaymentChannelContractServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false` | DONE |
| PAY-DATA-003 | 结算汇总 | 结算净额入库前通过 Money 收敛为非负整数分 | 交易 `1000` 分、退款 `1200` 分、手续费 `0` 分 | `PaymentSettlementSummaryService` 对交易、退款、手续费、未处理差异和净额统一走 `Money`；净额为负时拒绝生成结算汇总，避免负金额入库 | 结算汇总页接口语义不变；异常数据由后端业务异常阻断 | Maven 单测验证生成、确认、作废和负净额拒绝路径 | `mvn -f mango/pom.xml -pl mango-platform/mango-payment/mango-payment-core -am -Dtest=MoneyTest,PaymentOrderStateServiceTest,PaymentReconciliationServiceTest,PaymentSettlementSummaryServiceTest,PaymentOpenApiServiceTest,PaymentCashierServiceImplTest,MangoPayVirtualPaymentServiceTest,PaymentChannelContractServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false` | DONE |

## 4. 回归抽查记录

| 模块 | 页面 | 功能点 1 | 功能点 2 | UI 细节 | 截图/trace | 结论 |
|---|---|---|---|---|---|---|
| 支付金额规则 | 收银台支付页 | 金额按分传递和返回 | 支付结果延迟轮询到成功 | PC 收银台分类横向展示，金额和结果区未遮挡 | `mango-ui/apps/mango-admin/e2e/specs/payment-center.spec.ts` | DONE |
| 通道签约配置 | 通道签约配置页 | 签约能力限额非负校验 | 费率 `decimal(10,10)` 和 10 位小数校验 | 普通字段纯文本展示，状态使用语义 tag | `mango-docs/plans/evidence/payment-list-ui/channel-contracts-list-desktop.png` | DONE |

## 5. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 | 用户确认 |
|---|---|---|---|---|
| 全量支付中心 E2E 回归 | 本轮改动为后端金额规则收口，未触及页面结构 | 不影响页面可访问性结论；不能替代 `PAY-TEST-003` 的全量 E2E | 后续全量交付前继续执行支付中心完整 Playwright 回归 | 无 |
