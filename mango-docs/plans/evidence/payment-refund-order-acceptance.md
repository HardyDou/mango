# 退款订单列表页验收证据

## 1. 验收范围

- 页面：支付中心 / 交易订单 / 退款订单
- 接口：`GET /payment/refund-orders/page`、`GET /payment/refund-orders/detail`、`GET /payment/refund-orders/statuses`
- 权限：`payment:refund-order:list`、`payment:refund-order:query`
- 数据：`payment_refund_order`，关联 `payment_order`、`payment_business_order`、`payment_method`、`payment_channel`、`payment_channel_contract`、`payment_transaction_flow`
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
| PAY-MENU-009 | `/payment/refund-orders/statuses` | 退款订单状态筛选项 | 真实登录态请求 | 返回 `CREATED`、`REFUNDING`、`SUCCESS`、`FAILED`、`CLOSED` 状态契约 | 状态筛选下拉来自接口，不使用页面静态数组 | 接口业务成功 | `PLAYWRIGHT... --grep "退款订单列表"` | DONE |
| PAY-MENU-009 | `/payment/refund-orders/page` | 按关键字和状态查询退款订单 | `RO-E2E-*` 真实退款订单，状态 `REFUNDING` | 返回退款订单号、业务退款号、原支付订单号、业务订单号、支付方式、通道、退款金额、状态和时间字段 | 列表普通业务字段为普通文本，退款状态使用 `ElTag`；退款单号、支付单号未使用边框或标签样式 | 接口业务成功，无 console error、pageerror、退款订单接口 requestfailed | `test-results/payment-refund-orders.png` | DONE |
| PAY-MENU-009 | `/payment/refund-orders/detail` | 查询退款订单详情 | `RO-E2E-*` 的真实退款订单 ID | 返回退款申请、原支付订单、通道结果、时间信息和状态流转记录 | 详情抽屉按退款申请、原支付订单、通道结果、时间信息、状态流转分组；状态流转用时间线展示，无文字重叠或控件挤压 | 接口业务成功，无 console error、pageerror、退款订单接口 requestfailed | `test-results/payment-refund-orders.png` | DONE |
| PAY-MENU-009 | 退款订单页面 | 搜索无结果空态 | `NO-REFUND-ORDER-*` | 表格为空且显示“未查询到匹配的退款订单” | 空态明确区分查询无结果，不显示空白表格 | 接口业务成功，无 console error、pageerror、退款订单接口 requestfailed | `test-results/payment-refund-orders.png` | DONE |
| PAY-MENU-009 | 权限迁移 | 查询权限入库 | `authorization/V45__payment_refund_order_query_permission.sql` | `payment:refund-order:query` 绑定 ROLE_ADMIN；列表接口使用 `payment:refund-order:list` | 后台登录后菜单进入页面并可打开详情 | E2E 使用真实登录态通过 | `PLAYWRIGHT... --grep "退款订单列表"` | DONE |

## 4. 回归抽查记录

| 模块 | 页面 | 功能点 1 | 功能点 2 | UI 细节 | 截图/trace | 结论 |
|---|---|---|---|---|---|---|
| 支付中心 | 退款订单 | 页面可通过支付中心 / 交易订单 / 退款订单进入 | 状态筛选、详情抽屉、空态真实可用 | Element Plus 表单、表格、分页、抽屉、时间线布局稳定；状态使用标签，其余业务字段不加边框 | `test-results/payment-refund-orders.png` | DONE |

## 5. 验证命令

```bash
mvn -pl mango-platform/mango-payment/mango-payment-core,mango-platform/mango-payment/mango-payment-starter -am -Dtest=PaymentReadonlyResourceServiceTest -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false
pnpm -F mango-admin build
PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true PLAYWRIGHT_BASE_URL=http://127.0.0.1:7808 PLAYWRIGHT_API_BASE_URL=http://127.0.0.1:18118 pnpm -F mango-admin exec playwright test e2e/specs/payment-center.spec.ts --project=chromium --workers=1 --grep "支付订单列表|退款订单列表"
node mango-pmo/tools/acceptance-evidence-check.mjs --evidence mango-docs/plans/evidence/payment-refund-order-acceptance.md
```

## 6. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 | 用户确认 |
|---|---|---|---|---|
| 退款订单状态机完整性 | 本证据只覆盖后台 `PAY-MENU-009` 查询列表、详情和状态筛选；退款状态推进、可退金额锁定、业务通知和并发保护证据归属 `payment-openapi-acceptance.md` 的 `PAY-DOMAIN-003` | 不能仅凭本页面证据声明退款状态机完整完成 | 后续仍按 `PAY-DOMAIN-003`、`PAY-TEST-002/003` 补外部通道联调和全量 E2E | 不适用 |
| 发起退款和查询退款开放接口 | 本证据不覆盖 `PAY-API-007/008` 开放接口完整契约 | 不能据此声明业务系统退款接入接口全部完成 | 后续按台账继续开发和验收开放接口 | 不适用 |
