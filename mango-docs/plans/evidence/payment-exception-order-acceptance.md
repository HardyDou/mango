# 异常订单列表页验收证据

## 1. 验收范围

- 页面：支付中心 / 交易订单 / 异常订单
- 接口：`GET /payment/exception-orders/page`、`GET /payment/exception-orders/detail`、`GET /payment/exception-orders/statuses`、`GET /payment/exception-orders/actions`、`POST /payment/exception-orders/handle`
- 权限：`payment:exception-order:list`、`payment:exception-order:query`、`payment:exception-order:handle`
- 数据：`payment_exception_order`、`payment_operation_audit`
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
| PAY-MENU-011 | `/payment/exception-orders/page` | 按关键字和状态查询异常订单 | `EX-E2E-*`、`EX-UI-E2E-*` | 返回异常单号、关联订单号、异常类型、级别、处理状态、异常原因、处理结果和时间字段 | 普通单号字段为纯文本；异常类型、级别、处理状态使用 `ElTag`；行操作无删除按钮 | 接口业务成功，无 console error、pageerror、异常订单接口 requestfailed | `test-results/payment-exception-orders.png` | DONE |
| PAY-MENU-011 | `/payment/exception-orders/detail` | 查询异常订单详情 | `/payment/exception-orders/page` 返回的真实异常订单 ID | 返回异常信息、处理信息、时间信息；不存在时使用 `PaymentCode.PAYMENT_EXCEPTION_ORDER_NOT_FOUND` | 详情抽屉按异常信息、处理信息、时间信息分组，无字段重叠 | 接口业务成功，无 console error、pageerror、异常订单接口 requestfailed | `test-results/payment-exception-orders.png` | DONE |
| PAY-MENU-011 | `/payment/exception-orders/statuses`、`/payment/exception-orders/actions` | 查询状态和受控处理动作契约 | 真实登录态 | 状态包含 `PENDING/PROCESSING/HANDLED/IGNORED/CLOSED`；动作包含 `ACTIVE_QUERY/CLOSE_PAYMENT_ORDER/ADD_EVIDENCE/MANUAL_CLOSE`，与当前后端受控动作一致 | 状态筛选和处理动作下拉从接口加载，不使用页面最终静态数据 | 接口业务成功 | `PLAYWRIGHT... --grep "异常订单列表"` | DONE |
| PAY-MENU-011 | `/payment/exception-orders/handle` | 受控处理异常订单 | `EX-E2E-*`、`EX-UI-E2E-*` | 待处理异常订单处理后变为 `HANDLED`；记录处理动作、原因、结果、凭据、处理人、处理时间；终态重复处理返回 `3777` | 处理弹窗必填校验可用，提交后列表刷新且已处理行不再显示处理按钮 | 接口业务成功，无 console error、pageerror、异常订单接口 requestfailed | `test-results/payment-exception-orders.png` | DONE |
| PAY-MENU-011 | `/payment/operation-audits/page` | 处理操作审计 | `HANDLE_EXCEPTION_ORDER` | 写入 `HANDLE_EXCEPTION_ORDER / PAYMENT_EXCEPTION_ORDER / SUCCESS`，资源 ID 为异常单号 | 操作审计可通过接口查询 | E2E 使用真实登录态通过 | `PLAYWRIGHT... --grep "异常订单列表"` | DONE |
| PAY-MENU-011 | 权限迁移 | 查询和处理权限入库 | `authorization/V47__payment_exception_order_operation_permissions.sql` | `payment:exception-order:query`、`payment:exception-order:handle` 绑定 ROLE_ADMIN；列表接口使用 `payment:exception-order:list` | 后台登录后可进入页面、打开详情并处理异常订单 | E2E 使用真实登录态通过 | `PLAYWRIGHT... --grep "异常订单列表"` | DONE |

## 4. 回归抽查记录

| 模块 | 页面 | 功能点 1 | 功能点 2 | UI 细节 | 截图/trace | 结论 |
|---|---|---|---|---|---|---|
| 支付中心 | 异常订单 | 页面可通过支付中心 / 交易订单 / 异常订单进入 | 查询、详情抽屉、处理弹窗、空态真实可用 | Element Plus 表单、表格、分页、抽屉、弹窗布局稳定；普通业务字段不加边框 | `test-results/payment-exception-orders.png` | DONE |

## 5. 验证命令

```bash
mvn -pl mango-platform/mango-payment/mango-payment-core,mango-platform/mango-payment/mango-payment-starter -am -Dtest=PaymentReadonlyResourceServiceTest -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false
pnpm -F mango-admin build
PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true PLAYWRIGHT_BASE_URL=http://127.0.0.1:7808 PLAYWRIGHT_API_BASE_URL=http://127.0.0.1:18118 pnpm -F mango-admin exec playwright test e2e/specs/payment-center.spec.ts --project=chromium --workers=1 --grep "异常订单列表"
node mango-pmo/tools/acceptance-evidence-check.mjs --evidence mango-docs/plans/evidence/payment-exception-order-acceptance.md
```

## 6. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 | 用户确认 |
|---|---|---|---|---|
| 重复支付自动退款、主动查单、通知重推的真实通道执行链路 | 本证据覆盖异常订单后台查询、详情、受控处理、审计闭环；自动退款和通道查询属于退款补偿、通知记录、对账差异后续模块 | 不能据此声明重复支付自动退款、查单补单、通知重推执行链路全部完成 | 后续按 `PAY-MENU-012/014` 和第二阶段退款与异常补偿继续实现 | 不适用 |
| 附件型处理凭据 | 当前异常订单处理凭据为文本引用，文件附件凭据属于差异处理附件能力要求 | 不能据此声明差异处理附件上传链路完成 | 后续在 `PAY-MENU-014` 差异处理按设计补附件凭据 | 不适用 |
