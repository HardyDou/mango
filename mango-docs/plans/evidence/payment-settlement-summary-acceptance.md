# 结算汇总验收证据

## 1. 验收范围

- 页面：支付中心 / 对账结算 / 结算汇总
- 接口：`GET /payment/settlement-summaries/page`、`GET /payment/settlement-summaries/detail`、`GET /payment/settlement-summaries/statuses`、`POST /payment/settlement-summaries/generate`、`POST /payment/settlement-summaries/confirm`、`POST /payment/settlement-summaries/void`
- 权限：`payment:settlement-summary:list`、`payment:settlement-summary:query`、`payment:settlement-summary:generate`、`payment:settlement-summary:confirm`、`payment:settlement-summary:void`
- 数据：`payment_settlement_summary`、`payment_channel_bill_detail`、`payment_reconciliation`、`payment_difference`、`payment_operation_audit`
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
| PAY-MENU-015 | `/payment/settlement-summaries/page` | 结算汇总列表、状态筛选和行操作 | `SETTLE-*`、`ORDER_CENTER`、`MANGO_PAY`、账单日 `2026-06-12` | 列表返回结算日期、应用、企业主体、通道、支付金额、退款金额、手续费、净收款、未处理差异、状态和操作；生成态显示确认，已作废显示重新生成 | Element Plus 表单、表格、分页布局稳定；普通业务字段纯文本不加边框；状态使用 `ElTag`；列表区域没有字段重叠 | E2E 中页面接口业务成功，Playwright 用例通过，截图人工查看未发现布局错位 | `mango-ui/apps/mango-admin/test-results/payment-settlement-summaries.png` | DONE |
| PAY-MENU-015 | `/payment/settlement-summaries/generate`、`/confirm`、`/void` | 生成、确认、作废、重新生成结算汇总 | `SETTLE-BO-*`、`SETTLE-PO-*`、`SETTLE-RO-*`、`SETTLE-RC-*` | 首次生成返回 `GENERATED`；相同范围重复生成返回业务码 `3789`；确认后返回 `CONFIRMED`；作废后返回 `VOIDED`；作废后重新生成新汇总 ID，不覆盖原已确认记录 | 页面提供生成汇总按钮、确认提示、作废原因弹窗和重新生成按钮；按钮随状态变化显示 | API E2E 全链路通过，未出现 HTTP 500 或 requestfailed | `PLAYWRIGHT... --grep "结算汇总"` | DONE |
| PAY-SETTLE-001 | `payment_settlement_summary` | 按日、租户、应用、主体、通道生成结算汇总报表 | 支付账单 `128800` 分，退款账单 `38800` 分，手续费 `260` 分 | 汇总金额按分单位返回：支付 `128800`、退款 `38800`、手续费 `260`、净收款 `89740`；支付笔数 `1`，退款笔数 `1`；应用名称通过当前 `payment_application.app_id` 关联回显 | 列表金额列右对齐并格式化为人民币展示；普通字段无框框展示 | 后端单测覆盖金额计算；E2E 通过真实数据库账单明细验证 | `PaymentSettlementSummaryServiceTest`、`.mango/logs/backend.log` | DONE |
| PAY-SETTLE-002 | `/payment/settlement-summaries/confirm`、`/payment/differences/handle` | 未处理差异不允许确认汇总 | 未处理差异 `SETTLE-DF-*`，差异金额 `100` 分 | 存在 `PENDING` 差异时确认返回业务码 `3791`；通过差异处理接口闭环后再次确认成功，确认结果 `unresolvedDifferenceCount=0` | 页面确认动作只对已生成汇总显示；差异处理后回到结算汇总列表可见已生成重建记录 | API E2E 使用真实差异处理接口，无固定成功或临时数据 | `PLAYWRIGHT... --grep "结算汇总"` | DONE |
| PAY-MENU-015 | `payment_operation_audit` | 结算汇总操作审计 | `GENERATE_SETTLEMENT_SUMMARY`、`CONFIRM_SETTLEMENT_SUMMARY`、`PAYMENT_SETTLEMENT_SUMMARY` | 生成和确认操作均写入 `payment_operation_audit`，资源 ID 为真实结算汇总 ID，操作结果为 `SUCCESS` | 操作入口均为受控按钮和接口，不提供直接改状态页面 | E2E 通过操作审计接口查询到审计记录 | `PLAYWRIGHT... --grep "结算汇总"` | DONE |
| PAY-MENU-015 | Flyway、Mapper 和权限数据 | 结算汇总表控制字段、权限和 Mapper 规范 | `payment/V35`、`authorization/V51`、`PaymentSettlementSummaryMapper` | `payment_settlement_summary` 具备应用、笔数、未处理差异、状态、生成、确认、作废和审计字段；Mapper 继承 MyBatis-Plus `BaseMapper`；实体继承 `AuditableEntity`；业务校验使用 `Require` 和 `PaymentCode` | 后台菜单可进入结算汇总页，操作权限随菜单数据入库 | 本地服务重启后 Flyway 已执行到新增 migration，页面和接口均可用 | `.mango/dev-workspace.env` 对应数据库 `mango_dev_e397cd` | DONE |

## 4. 回归抽查记录

| 模块 | 页面 | 功能点 1 | 功能点 2 | UI 细节 | 截图/trace | 结论 |
|---|---|---|---|---|---|---|
| 支付中心 | 结算汇总 | 页面可通过支付中心 / 对账结算 / 结算汇总进入 | 查询、状态筛选、生成、确认、作废、重新生成、审计查询真实可用 | Element Plus 后台布局稳定；普通列表字段不加边框；状态使用标签；搜索区、表格、分页没有重叠 | `mango-ui/apps/mango-admin/test-results/payment-settlement-summaries.png` | DONE |

## 5. 验证命令

```bash
mvn -pl mango-platform/mango-payment/mango-payment-core,mango-platform/mango-payment/mango-payment-starter -am -Dtest=PaymentReadonlyResourceServiceTest,PaymentSettlementSummaryServiceTest -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false
pnpm -F mango-admin build
scripts/dev-workspace.sh stop && scripts/dev-workspace.sh start
PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true PLAYWRIGHT_BASE_URL=http://127.0.0.1:7808 PLAYWRIGHT_API_BASE_URL=http://127.0.0.1:18118 pnpm -F mango-admin exec playwright test e2e/specs/payment-center.spec.ts --project=chromium --workers=1 --grep "结算汇总"
node mango-pmo/tools/acceptance-evidence-check.mjs --evidence mango-docs/plans/evidence/payment-settlement-summary-acceptance.md
```

## 6. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 | 用户确认 |
|---|---|---|---|---|
| 结算汇总导出 | 设计台账 `PAY-SETTLE-001` 原交付物包含导出，但当前设计文档 7.5 只定义汇总报表和确认规则，未定义导出接口字段、格式和权限 | 本轮不声明导出能力已交付；不影响后台生成、确认、作废、重建和财务核对主链路 | 后续如确认导出格式，再补独立台账、接口和 E2E | 不适用 |
| 完整差异识别维度 | 本轮覆盖未处理差异阻断确认，依赖现有 `payment_difference` 处理状态；未扩展全部差异识别场景 | 不把 `PAY-RECON-002` 完整差异识别声明为完成 | 后续继续按对账差异识别台账补齐本地成功通道缺失等场景 | 不适用 |
