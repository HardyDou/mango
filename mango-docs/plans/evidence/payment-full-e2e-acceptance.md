# 支付中心全量 E2E 验收证据

## 1. 验收范围

- 页面：支付中心菜单、应用接入、支付通道、交易订单、异常通知、对账结算、收银台和支付结果流程。
- 接口：`/api/payment/**`、`/api/openapi/pay/**`。
- 数据：`payment_%` 支付域真实表、授权菜单权限表、文件中心 Logo 上传链路。
- 部署形态：本地单体后端 `http://127.0.0.1:18118`，Mango Admin 前端 `http://127.0.0.1:7808`。

## 2. 执行环境

- 工作区：`/Users/hardy/Work/mango/.claude/worktrees/payment-platform`
- 数据库：`mango_dev_e397cd`
- 浏览器：Playwright Chromium
- 测试账号：`admin`
- 说明：E2E 使用真实前端、真实后端和真实数据库，未使用接口拦截、mock 数据或前端临时数据。
- 本轮复核时间：2026-06-07 17:12 左右，单 worker 完整执行 `payment-center.spec.ts`。

## 3. 功能验收记录

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| PAY-TEST-003 | `e2e/specs/payment-center.spec.ts` | 支付中心菜单、配置、收银台、芒果支付内置虚拟通道付款、结果页、后台查询全量 E2E | E2E 动态数据 `PAY-E2E-*`、`OPENAPI-BO-*`、`RO-E2E-*`、`NT-*`、`RC-*`；通道 `MANGO_PAY`；租户 `1` | Playwright 全量执行 `28 passed`；覆盖支付中心菜单可见、应用管理、企业主体、支付通道、通道签约、支付方式、路由试算、收银台配置、收银台真实支付、支付结果延迟轮询、业务订单支付按钮、支付订单、退款订单、交易流水、异常订单、通知记录、操作审计、对账管理、差异处理、结算汇总和列表 UI 约束 | 主要列表普通字段为纯文本，状态/类型字段使用语义标签；收银台按真实配置渲染 Logo、标题、金额、支付方式和支付物料；支付成功后隐藏确认支付按钮；截图覆盖订单、退款、流水、通知、对账、异常、审计和列表 UI | E2E 监听 console error、pageerror 和支付相关 requestfailed；用例内对关键页面接口做业务断言；全量命令退出码为 0 | `mango-ui/apps/mango-admin/playwright-report/index.html`；`mango-ui/apps/mango-admin/test-results/payment-business-orders.png`；`mango-ui/apps/mango-admin/test-results/payment-orders.png`；`mango-ui/apps/mango-admin/test-results/payment-refund-orders.png`；`mango-ui/apps/mango-admin/test-results/payment-reconciliations-special-bill.png`；`mango-ui/apps/mango-admin/test-results/payment-notification-auto-dispatch.png` | DONE |
| PAY-MENU-015 / PAY-SETTLE-001 | `e2e/specs/payment-center.spec.ts`、`/api/payment/settlement-summaries/*` | 结算汇总生成、差异阻断确认、作废和重新生成回归 | `SETTLE-BO-*`、`SETTLE-PO-*`、`SETTLE-RC-*`、`MANGO_PAY`、账单日 `2026-06-*` | 聚焦 E2E `1 passed`；受影响组合 E2E `4 passed`；完整 E2E `28 passed`；结算金额按真实 `payment_channel_bill_detail` 汇总，支付 `128800` 分、退款 `38800` 分、手续费 `260` 分、净收款 `89740`；历史 `SETTLE-*` 同账期记录不再污染本轮汇总 | 结算汇总页入口、列表、状态、确认、作废、重新生成按钮均由真实接口驱动；普通字段纯文本展示，状态使用标签 | 聚焦、组合和完整 E2E 命令退出码均为 0；未使用接口拦截或前端临时数据 | `PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true pnpm --dir mango-ui/apps/mango-admin exec playwright test e2e/specs/payment-center.spec.ts --project=chromium --reporter=list --workers=1 --grep "结算汇总生成"`；完整命令见第 4 节 | DONE |

## 4. 验证命令

```bash
PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true PLAYWRIGHT_BASE_URL=http://127.0.0.1:7808 PLAYWRIGHT_API_BASE_URL=http://127.0.0.1:18118 pnpm --dir mango-ui/apps/mango-admin exec playwright test e2e/specs/payment-center.spec.ts --project=chromium --workers=1

PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true pnpm --dir mango-ui/apps/mango-admin exec playwright test e2e/specs/payment-center.spec.ts --project=chromium --reporter=list --workers=1 --grep "结算汇总生成"

PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true pnpm --dir mango-ui/apps/mango-admin exec playwright test e2e/specs/payment-center.spec.ts --project=chromium --reporter=list --workers=1 --grep "支付方式路由策略|数据库菜单分组|对账管理导入|结算汇总生成"

curl -s -w '\n%{http_code}\n' http://127.0.0.1:18118/actuator/health

curl -s -o /dev/null -w '%{http_code}\n' http://127.0.0.1:7808/
```

## 5. 本次验证结果

- Playwright Chromium：`28 passed (2.9m)`。
- 结算汇总聚焦 E2E：`1 passed`。
- 路由、菜单/收银台、对账、结算组合回归：`4 passed`。
- 后端服务健康：`http://127.0.0.1:18118/actuator/health` 返回 `UP`。
- 前端服务健康：`http://127.0.0.1:7808` 返回 `200 OK`。

## 6. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 | 用户确认 |
|---|---|---|---|---|
| 外部真实通道联调 | 全量 E2E 使用芒果支付内置虚拟通道 `MANGO_PAY`，不连接通联、华夏、微信、支付宝、连连等外部机构 | 不能据此声明 `PAY-CHANNEL-003/005/007/008` 完成 | 后续拿到官方或内部联调资料、商户参数、证书和回调验签规则后逐通道验收 | 不适用 |
| 用户确认项 | 线下转账范围、其他通道清单和优先级、开票触发边界、租户定义仍在交付台账中标记 `IN_PROGRESS` | 不能据此声明整体投产完成 | 继续按交付台账逐项关闭或登记用户确认例外 | 待确认 |
| 台账整体完成 | 本证据只证明 `PAY-TEST-003` 全量 E2E 和结算回归通过；交付台账仍存在外部通道和用户确认项 | 不能声明支付模块整体投产完成 | 继续按交付台账逐项关闭或登记用户确认例外 | 不适用 |
