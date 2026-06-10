# 差异处理验收证据

## 1. 验收范围

- 页面：支付中心 / 对账结算 / 差异处理
- 接口：`GET /payment/differences/page`、`GET /payment/differences/detail`、`GET /payment/differences/statuses`、`GET /payment/differences/actions`、`POST /payment/differences/handle`
- 权限：`payment:difference:list`、`payment:difference:query`、`payment:difference:handle`
- 数据：`payment_difference`、`payment_reconciliation`、`payment_channel_bill_detail`、`payment_transaction_flow`、`payment_operation_audit`
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
| PAY-MENU-014 | `/payment/differences/page` | 差异处理列表和状态筛选 | `CH-DIFF-E2E-*`、`MANGO_PAY`、`statusCode=PENDING/CLOSED` | 列表返回差异单号、对账批次、关联订单、差异类型、差异金额、处理状态和处理结果；普通字段纯文本；不存在关键字返回空态 | Element Plus 表单、表格、分页布局稳定；状态和差异类型使用 `ElTag`；行操作包含详情和可处理状态的处理按钮；无删除按钮 | console error、pageerror、差异接口 requestfailed 监听数组为空 | `mango-ui/apps/mango-admin/test-results/payment-differences.png` | DONE |
| PAY-MENU-014 | `/payment/differences/detail` | 差异详情抽屉 | `/payment/differences/page` 返回的真实差异 ID | 详情返回差异信息、对账批次、通道、账单日期、金额、处理动作、原因、结果、凭据、处理人和处理时间 | 详情抽屉按差异信息、处理信息、时间信息分区；普通字段纯文本；状态和类型使用 `ElTag`；抽屉内容没有字段重叠 | 详情接口业务成功，页面没有 runtime error | `PLAYWRIGHT... --grep "差异处理"` | DONE |
| PAY-MENU-014 | `/payment/differences/statuses`、`/payment/differences/actions` | 状态和处理动作契约 | 真实登录态 | 状态包含 `PENDING`、`PROCESSING`、`HANDLED`、`IGNORED`、`CLOSED`；动作包含 `ACTIVE_QUERY`、`SUPPLEMENT_ORDER`、`REFUND_COMPENSATE`、`IGNORE`、`CLOSE` | 状态下拉和处理动作下拉均从后端接口加载，不使用页面最终静态数组 | 接口业务成功，数据来自后端契约 | `PLAYWRIGHT... --grep "差异处理"` | DONE |
| PAY-MENU-014 | `/payment/differences/handle` | 查单、补单、退款补偿、忽略、关闭等受控动作入口 | E2E 选择 `CLOSE`，凭据 `mango-file:diff-e2e-*` | 处理后状态变为 `CLOSED / 已关闭`；记录 `processAction/processReason/processResult/processEvidence/processorName/processTime`；终态重复处理返回业务码 `3786` | 处理弹窗校验动作、原因、结果必填；保存后列表刷新，终态行不再显示处理按钮 | POST 处理接口业务成功；重复处理返回业务错误；无 requestfailed | `PLAYWRIGHT... --grep "差异处理"` | DONE |
| PAY-RECON-003 | `payment_difference`、`payment_operation_audit` | 差异处理受控闭环和审计 | `HANDLE_DIFFERENCE`、`PAYMENT_DIFFERENCE`、真实差异单号 | 人工处理记录动作、原因、处理人、处理时间和凭据；审计写入 `HANDLE_DIFFERENCE / PAYMENT_DIFFERENCE / SUCCESS`；处理过程不修改支付订单或退款订单成功状态 | 后台只提供受控处理入口，不提供直接改成功状态的页面入口 | E2E 通过真实审计接口查询到审计记录 | `PLAYWRIGHT... --grep "差异处理"` | DONE |
| PAY-RECON-003 | Flyway 和权限数据 | 差异处理字段、权限和按钮运行时入库 | `payment/V34`、`authorization/V50` | `payment_difference` 增加处理动作、原因、凭据、处理人和处理时间字段；权限 `payment:difference:query/handle` 绑定 `ROLE_ADMIN` 并登记按钮运行时配置 | 登录后可进入差异处理页面并执行详情、处理操作 | 本地服务重启后 Flyway 已执行到新增 migration，页面和接口均可用 | `.mango/dev-workspace.env` 对应数据库 `mango_dev_e397cd` | DONE |
| PAY-DOMAIN-004 / PAY-RECON-003 | `POST /payment/differences/handle`、`payment_transaction_flow` | 差异处理生成 `ADJUST_NOTE` 备注流水并回填差异单 | 差异单 `DIFF-001`，处理动作 `CLOSE`，凭据 `mango-file:900001` | 处理差异时创建 0 分 `ADJUST_NOTE` 交易流水；流水写入 `tenant_id/created_by/created_at/updated_by/updated_at`；差异单回填 `adjust_flow_id/adjust_flow_no`，形成差异处理记录与流水追溯关系；不修改支付订单或退款订单成功状态 | 后端服务单测，不涉及页面布局 | `PaymentReadonlyResourceServiceTest` 断言 `payment_transaction_flow` 插入、流水类型和金额、差异单流水号回填、审计写入 | `PaymentReadonlyResourceServiceTest` | DONE |

## 4. 回归抽查记录

| 模块 | 页面 | 功能点 1 | 功能点 2 | UI 细节 | 截图/trace | 结论 |
|---|---|---|---|---|---|---|
| 支付中心 | 差异处理 | 页面可通过支付中心 / 对账结算 / 差异处理进入 | 查询、状态筛选、详情抽屉、处理弹窗、终态隐藏处理按钮、空态真实可用 | Element Plus 后台布局稳定；普通列表字段不加边框；状态和类型使用标签；搜索区、表格、分页没有重叠 | `mango-ui/apps/mango-admin/test-results/payment-differences.png` | DONE |

## 5. 验证命令

```bash
mvn -pl mango-platform/mango-payment/mango-payment-core,mango-platform/mango-payment/mango-payment-starter -am -Dtest=PaymentReadonlyResourceServiceTest -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false
mvn -f mango/pom.xml -pl mango-platform/mango-payment/mango-payment-core -am -Dtest=PaymentReadonlyResourceServiceTest,PaymentReconciliationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false
pnpm -F mango-admin build
PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true PLAYWRIGHT_BASE_URL=http://127.0.0.1:7808 PLAYWRIGHT_API_BASE_URL=http://127.0.0.1:18118 pnpm -F mango-admin exec playwright test e2e/specs/payment-center.spec.ts --project=chromium --workers=1 --grep "差异处理"
node mango-pmo/tools/acceptance-evidence-check.mjs --evidence mango-docs/plans/evidence/payment-difference-acceptance.md
```

## 6. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 | 用户确认 |
|---|---|---|---|---|
| 完整差异识别 | 本轮完成差异处理闭环，未扩展 `PAY-RECON-002` 的全部差异识别场景；现有 E2E 仍主要覆盖通道成功我方无单 | 不能把 `PAY-RECON-002` 声明为完整完成 | 后续继续按 `PAY-RECON-002` 补本地成功通道账单缺失、金额不一致、状态不一致、退款不一致等完整识别验证 | 不适用 |
| 差异动作的外部通道执行 | 本轮实现后台受控处理记录和审计闭环，未把主动查单、补单、退款补偿接入真实通道执行器 | 不声明完成查单、补单、退款补偿的通道执行能力；本轮完成后台处理闭环 | 后续在通道适配、订单状态机和退款审批模块中接入真实执行动作 | 不适用 |
