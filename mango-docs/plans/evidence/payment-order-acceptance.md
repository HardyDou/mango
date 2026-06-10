# 支付订单列表页验收证据

## 1. 验收范围

- 页面：支付中心 / 交易订单 / 支付订单
- 接口：`GET /payment/payment-orders/page`、`GET /payment/payment-orders/detail`、`GET /payment/payment-orders/statuses`
- 权限：`payment:payment-order:list`、`payment:payment-order:query`
- 数据：`payment_order`，关联 `payment_business_order`、`payment_method`、`payment_channel`、`payment_cashier_config`、`payment_channel_contract`、`payment_contract_capability`、`payment_route_rule`、`payment_transaction_flow`
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
| PAY-MENU-008 | `/payment/payment-orders/statuses` | 支付订单状态筛选项 | 真实登录态请求 | 返回 `CREATED`、`PAYING`、`SUCCESS`、`FAILED`、`CLOSED`、`DUPLICATE_REFUNDED` 等状态契约 | 状态筛选下拉来自接口，不使用页面静态数组 | 接口业务成功 | `PLAYWRIGHT... --grep "支付订单列表"` | DONE |
| PAY-MENU-008 | `/payment/payment-orders/page` | 按关键字和状态查询支付订单 | `PO-LIST-E2E-*` 真实支付订单，状态 `PAYING` | 返回支付订单号、业务订单号、支付方式、通道、通道商户号、通道交易号、金额、状态、有效成功标识和时间字段 | 列表普通业务字段为普通文本，支付状态和有效成功标识使用 `ElTag`；订单号和通道商户号未使用边框或标签样式 | 接口业务成功，无 console error、pageerror、支付订单接口 requestfailed | `test-results/payment-orders.png` | DONE |
| PAY-MENU-008 | `/payment/payment-orders/detail` | 查询支付订单详情 | `PO-LIST-E2E-*` 的真实支付订单 ID | 返回通道请求字段、路由规则、签约能力、支付时间、过期时间和状态流转记录 | 详情抽屉按订单信息、通道请求、时间信息、状态流转分组；状态流转用时间线展示，无文字重叠或控件挤压 | 接口业务成功，无 console error、pageerror、支付订单接口 requestfailed | `test-results/payment-orders.png` | DONE |
| PAY-MENU-008 | 支付订单页面 | 搜索无结果空态 | `NO-PAYMENT-ORDER-*` | 表格为空且显示“未查询到匹配的支付订单” | 空态明确区分查询无结果，不显示空白表格 | 接口业务成功，无 console error、pageerror、支付订单接口 requestfailed | `test-results/payment-orders.png` | DONE |
| PAY-MENU-008 | 权限迁移 | 查询权限入库 | `authorization/V44__payment_order_query_permission.sql` | `payment:payment-order:query` 绑定 ROLE_ADMIN；列表接口使用 `payment:payment-order:list` | 后台登录后菜单进入页面并可打开详情 | E2E 使用真实登录态通过 | `PLAYWRIGHT... --grep "支付订单列表"` | DONE |

## 4. 回归抽查记录

| 模块 | 页面 | 功能点 1 | 功能点 2 | UI 细节 | 截图/trace | 结论 |
|---|---|---|---|---|---|---|
| 支付中心 | 支付订单 | 页面可通过支付中心 / 交易订单 / 支付订单进入 | 状态筛选、详情抽屉、空态真实可用 | Element Plus 表单、表格、分页、抽屉、时间线布局稳定；状态使用标签，其余业务字段不加边框 | `test-results/payment-orders.png` | DONE |

## 5. 验证命令

```bash
mvn -pl mango-platform/mango-payment/mango-payment-core,mango-platform/mango-payment/mango-payment-starter -am -Dtest=PaymentReadonlyResourceServiceTest -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false
pnpm -F mango-admin build
PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true PLAYWRIGHT_BASE_URL=http://127.0.0.1:7808 PLAYWRIGHT_API_BASE_URL=http://127.0.0.1:18118 pnpm -F mango-admin exec playwright test e2e/specs/payment-center.spec.ts --project=chromium --workers=1 --grep "支付订单列表|退款订单列表"
node mango-pmo/tools/acceptance-evidence-check.mjs --evidence mango-docs/plans/evidence/payment-order-acceptance.md
```

## 6. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 | 用户确认 |
|---|---|---|---|---|
| 支付订单状态机完整性 | 本证据只覆盖后台 `PAY-MENU-008` 查询列表、详情和状态筛选；完整状态推进归属 `PAY-DOMAIN-002` | 不影响后台列表页完成结论，但不能据此声明支付订单域全部完成 | 后续按台账继续验证状态机、并发成功约束、重复支付退款和补偿推进 | 不适用 |
| 发起支付和查询支付订单开放接口 | 本证据不覆盖 `PAY-API-005/006` 开放接口完整契约 | 不能据此声明业务系统支付接入接口全部完成 | 后续按台账继续开发和验收开放接口 | 不适用 |
