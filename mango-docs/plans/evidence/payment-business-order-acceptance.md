# 业务订单列表页验收证据

## 1. 验收范围

- 页面：支付中心 / 交易订单 / 业务订单
- 接口：`GET /payment/business-orders/page`、`GET /payment/business-orders/detail`、`GET /payment/business-orders/statuses`、`GET /payment/cashier/session`
- 权限：`payment:business-order:list`、`payment:business-order:query`
- 数据：`payment_business_order`，关联 `payment_enterprise_subject`、`payment_order`、`payment_refund_order`、`payment_application`、`payment_cashier_config`
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
| PAY-MENU-007 | `/payment/business-orders/statuses` | 业务订单状态筛选项 | 真实登录态请求 | 返回 `TO_PAY`、`PAYING`、`PAID`、`CLOSED`、`REFUNDED` 等状态契约 | 状态筛选下拉来自接口，不使用页面静态数组 | 接口业务成功 | `PLAYWRIGHT... --grep "业务订单列表"` | DONE |
| PAY-MENU-007 | `/payment/business-orders/statuses` | 状态下拉显示回归 | 真实页面打开订单状态下拉 | 下拉弹层可见，显示“支付中”等真实接口选项 | 下拉宽度和弹层可见性稳定，不出现选项不显示 | 接口业务成功，无 console error、pageerror、业务订单接口 requestfailed | `test-results/payment-business-orders.png` | DONE |
| PAY-MENU-007 | `/payment/business-orders/page` | 按关键字和状态查询业务订单 | `BO-LIST-E2E-*`，状态 `PAYING` | 返回业务订单号、AppId、标题、状态、应付金额、已支付金额、已退款金额、匹配收银台配置 ID | 列表展示业务字段为普通文本，订单状态使用 `ElTag`，AppId 未使用标签/边框 | 接口业务成功，无 console error、pageerror、业务订单接口 requestfailed | `test-results/payment-business-orders.png` | DONE |
| PAY-MENU-007 | `/payment/business-orders/detail` | 查询业务订单详情 | `BO-LIST-E2E-*` 的真实订单 ID | 返回通知地址、返回地址、扩展信息、支付订单数、退款订单数 | 抽屉按订单信息、通知与扩展、时间信息分组展示；无文字重叠或控件挤压 | 接口业务成功，无 console error、pageerror、业务订单接口 requestfailed | `test-results/payment-business-orders.png` | DONE |
| PAY-MENU-007 | 业务订单页面 + `/payment/cashier/session` | 列表支付入口打开收银台 | `BO-LIST-E2E-*`，状态 `PAYING`，匹配真实收银台 `350001` | 操作列“支付”位于“详情”前；点击后弹窗请求收银台 session 并回显业务订单号；“确认支付”可用 | 弹窗内嵌真实收银台页面，按钮未遮挡，支付方式来自后端 session | 接口业务成功，无 console error、pageerror、业务订单接口 requestfailed | `test-results/payment-business-orders.png` | DONE |
| PAY-MENU-007 | 业务订单页面 + `/payment/cashier/session` | 已支付订单仍可查看但不可重复发起支付 | `BO-PAID-E2E-*` 通过真实收银台支付后置为 `SUCCESS` | 点击“支付”仍打开收银台；回显订单状态 `SUCCESS`；“确认支付”禁用并显示不可发起支付提示 | 延迟支付结果场景下，已完成订单只展示上下文，不允许二次支付 | 接口业务成功，无 console error、pageerror、业务订单接口 requestfailed | `test-results/payment-business-orders.png` | DONE |
| PAY-MENU-007 | 业务订单页面 | 搜索无结果空态 | `NO-BIZ-ORDER-*` | 表格为空且显示“未查询到匹配的业务订单” | 空态明确区分查询无结果，不显示空白表格 | 接口业务成功，无 console error、pageerror、业务订单接口 requestfailed | `test-results/payment-business-orders.png` | DONE |
| PAY-MENU-007 | 权限迁移 | 查询权限入库 | `authorization/V43__payment_business_order_query_permission.sql` | `payment:business-order:query` 绑定 ROLE_ADMIN；列表接口使用 `payment:business-order:list` | 后台登录后菜单进入页面并可打开详情 | E2E 使用真实登录态通过 | `PLAYWRIGHT... --grep "业务订单列表"` | DONE |

## 4. 回归抽查记录

| 模块 | 页面 | 功能点 1 | 功能点 2 | UI 细节 | 截图/trace | 结论 |
|---|---|---|---|---|---|---|
| 支付中心 | 业务订单 | 页面可通过支付中心 / 交易订单 / 业务订单进入 | 状态筛选、支付弹窗、详情抽屉、空态真实可用 | Element Plus 表单、表格、分页、弹窗、抽屉布局稳定；状态用标签，其余业务字段不加边框 | `test-results/payment-business-orders.png` | DONE |

## 5. 验证命令

```bash
mvn -pl mango-platform/mango-payment/mango-payment-core,mango-platform/mango-payment/mango-payment-starter -am -Dtest=PaymentCashierConfigServiceImplTest,PaymentMethodServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false
pnpm -F mango-admin build
PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true PLAYWRIGHT_BASE_URL=http://127.0.0.1:7808 PLAYWRIGHT_API_BASE_URL=http://127.0.0.1:18118 pnpm -F mango-admin exec playwright test e2e/specs/payment-center.spec.ts --project=chromium --workers=1 --grep "业务订单列表"
```

## 6. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 | 用户确认 |
|---|---|---|---|---|
| 业务订单开放接口、状态机完整性 | 本证据只覆盖后台 `PAY-MENU-007`；开放接口和领域状态机分别归属 `PAY-API-002/003`、`PAY-DOMAIN-001` | 不影响后台列表页完成结论，但不能据此声明业务订单域全部完成 | 后续按台账继续验证和开发开放接口、状态机 | 不适用 |
| 支付订单、退款订单、交易流水后台页 | 属于 `PAY-MENU-008/009/010` 后续台账项 | 不能声明交易订单分组全部完成 | 后续逐菜单推进 | 不适用 |
