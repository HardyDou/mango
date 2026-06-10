# 操作审计验收证据

## 1. 验收范围

- 页面：支付中心 / 对账结算 / 操作审计
- 接口：`GET /payment/operation-audits/page`
- 权限：`payment:operation-audit:list`
- 数据：`payment_operation_audit`
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
| PAY-MENU-016 | `/payment/operation-audits/page` | 操作审计分页查询 | 通过应用管理新增 `支付E2E操作审计应用*` 产生 `CREATE_APPLICATION` 审计 | 接口返回 `id`、`operatorName`、`operationAction`、`resourceType`、`resourceId`、`operationResult`、`operationTime`、`createTime`；按 `keyword=CREATE_APPLICATION` 和 `statusCode=SUCCESS` 能查询到真实审计记录 | 列表使用 Element Plus 搜索表单、表格和分页；操作人、资源标识、时间为普通文本；操作动作、资源类型、结果使用 `ElTag`；普通字段没有框式展示 | E2E 使用真实登录态和真实后端接口；页面接口业务成功，未出现 4xx/5xx | `mango-ui/apps/mango-admin/test-results/payment-operation-audits.png` | DONE |
| PAY-MENU-016 | 后端 Mapper、VO 和权限 | 专用审计查询契约 | `PaymentOperationAuditVO`、`PaymentOperationAuditMapper`、`authorization/V52` | Controller 返回 `PageResult<PaymentOperationAuditVO>`，不再返回 `Map<String,Object>`；Mapper 继承 MyBatis-Plus `BaseMapper` 并提供专用分页 SQL；接口加 `@ApiAccess(permission = "payment:operation-audit:list")`；菜单权限和运行时配置入库 | 不涉及独立 UI | 后端单测断言租户、关键词、结果筛选均走 Mapper 参数 | `PaymentReadonlyResourceServiceTest#pageOperationAudits_usesMapperAndResultFilter` | DONE |
| PAY-MENU-016 | 前端类型和页面 | 专用操作审计列表页 | `PaymentOperationAudit` 前端类型 | 操作审计页面不再使用 `PaymentResourcePage` 通用 Map 数据；API 使用明确 `PaymentOperationAudit` 类型；列表搜索、结果筛选、空态、错误重试和分页均由真实接口驱动 | 页面无多层卡片边框；搜索区、表格、分页间距稳定；字段没有重叠 | `pnpm -F mango-admin build` 通过后再执行 E2E | `mango-ui/packages/payment/src/views/operation-audits/index.vue` | DONE |

## 4. 回归抽查记录

| 模块 | 页面 | 功能点 1 | 功能点 2 | UI 细节 | 截图/trace | 结论 |
|---|---|---|---|---|---|---|
| 支付中心 | 操作审计 | 页面可通过支付中心 / 对账结算 / 操作审计进入 | 查询、结果筛选、分页和审计记录回显真实可用 | Element Plus 后台布局稳定；普通列表字段不加边框；动作、资源类型、结果使用标签；搜索区、表格、分页没有重叠 | `mango-ui/apps/mango-admin/test-results/payment-operation-audits.png` | DONE |

## 5. 验证命令

```bash
mvn -pl mango-platform/mango-payment/mango-payment-core,mango-platform/mango-payment/mango-payment-starter -am -Dtest=PaymentReadonlyResourceServiceTest -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false
pnpm -F mango-admin build
scripts/dev-workspace.sh stop && scripts/dev-workspace.sh start
PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true PLAYWRIGHT_BASE_URL=http://127.0.0.1:7808 PLAYWRIGHT_API_BASE_URL=http://127.0.0.1:18118 pnpm -F mango-admin exec playwright test e2e/specs/payment-center.spec.ts --project=chromium --workers=1 --grep "操作审计"
node mango-pmo/tools/acceptance-evidence-check.mjs --evidence mango-docs/plans/evidence/payment-operation-audit-acceptance.md
node mango-pmo/tools/delivery-contract-check.mjs --design mango-docs/designs/统一支付系统设计说明书.md --ledger mango-docs/plans/2026-05-25-payment-delivery-ledger.md --mode plan
git diff --check
```

## 6. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 | 用户确认 |
|---|---|---|---|---|
| 审计动作枚举字典维护页 | 设计文档要求操作审计查询，未定义独立审计动作字典维护能力 | 当前页面按真实审计记录编码展示，不声明动作字典管理能力 | 后续如需要中文动作名，需补充设计、数据库字典和权限 | 不适用 |
