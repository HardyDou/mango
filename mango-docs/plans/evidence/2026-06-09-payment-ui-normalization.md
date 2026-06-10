# 支付中心页面 UI 精细化调整验收证据

## 1. 验收范围

- 页面：业务订单、支付订单、退款订单、对账管理、操作审计、结算汇总、线下收款订单、线下退款订单。
- 接口：本次不新增接口；页面通过既有支付中心分页接口加载真实数据。
- 权限：使用 `admin` 管理员登录态访问支付中心菜单。
- 数据：本地工作区数据库 `mango_dev_e397cd` 既有支付测试数据。
- 部署形态：本地单体前端 `mango-admin`，支付业务包 `@mango/payment`。

## 2. 执行环境

- 前端地址：`http://127.0.0.1:7808`
- 后端地址：`http://127.0.0.1:18118`
- 数据库或租户：`mango_dev_e397cd` / `default`
- 测试账号：`admin`
- 浏览器：Playwright Chromium

## 3. 功能验收记录

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| PAYMENT-UI-001 | `/payment/business-orders` | 搜索区、列表区、分页区结构 | 真实分页接口数据 | 标题、`.payment-business-orders__toolbar`、`.payment-business-orders__table`、`.payment-business-orders__pagination` 可见 | 搜索控件、表格、分页位于统一支付页面结构内 | 无 console error；支付接口无 requestfailed | `mango-ui/apps/mango-admin/test-results/payment-ui-business-orders.png` | 通过 |
| PAYMENT-UI-002 | `/payment/payment-orders` | 搜索区、列表区、分页区结构 | 真实分页接口数据 | 标题、`.payment-orders__toolbar`、`.payment-orders__table`、`.payment-orders__pagination` 可见 | 搜索控件、表格、分页位于统一支付页面结构内 | 无 console error；支付接口无 requestfailed | `mango-ui/apps/mango-admin/test-results/payment-ui-payment-orders.png` | 通过 |
| PAYMENT-UI-003 | `/payment/refund-orders` | 搜索区、列表区、分页区结构 | 真实分页接口数据 | 标题、`.payment-refund-orders__toolbar`、`.payment-refund-orders__table`、`.payment-refund-orders__pagination` 可见 | 搜索控件、表格、分页位于统一支付页面结构内 | 无 console error；支付接口无 requestfailed | `mango-ui/apps/mango-admin/test-results/payment-ui-refund-orders.png` | 通过 |
| PAYMENT-UI-004 | `/payment/reconciliations` | 搜索区、列表区、分页区结构 | 真实分页接口数据 | 标题、`.payment-reconciliations__toolbar`、`.payment-reconciliations__table`、`.payment-reconciliations__pagination` 可见 | 已移除列表外层卡片；对账页跟其他支付列表页一致 | 无 console error；支付接口无 requestfailed | `mango-ui/apps/mango-admin/test-results/payment-ui-reconciliations.png` | 通过 |
| PAYMENT-UI-005 | `/payment/operation-audits` | 搜索区、列表区、分页区结构 | 真实分页接口数据 | 标题、`.payment-operation-audits__toolbar`、`.payment-operation-audits__table`、`.payment-operation-audits__pagination` 可见 | 搜索区由栅格改为统一 inline form | 无 console error；支付接口无 requestfailed | `mango-ui/apps/mango-admin/test-results/payment-ui-operation-audits.png` | 通过 |
| PAYMENT-UI-006 | `/payment/settlement-summaries` | 搜索区、列表区、分页区结构 | 真实分页接口数据 | 标题、`.payment-settlement-summaries__toolbar`、`.payment-settlement-summaries__table`、`.payment-settlement-summaries__pagination` 可见 | 搜索区由栅格改为统一 inline form | 无 console error；支付接口无 requestfailed | `mango-ui/apps/mango-admin/test-results/payment-ui-settlement-summaries.png` | 通过 |
| PAYMENT-UI-007 | `/payment/offline/collections` | 搜索区、列表区、分页区结构 | 真实分页接口数据 | 标题、`.payment-offline-collections__toolbar`、`.payment-offline-collections__table`、`.payment-offline-collections__pagination` 可见 | 线下收款页 tab、搜索、表格和分页可见 | 无 console error；支付接口无 requestfailed | `mango-ui/apps/mango-admin/test-results/payment-ui-offline-collections.png` | 通过 |
| PAYMENT-UI-008 | `/payment/offline/refunds` | 搜索区、列表区、分页区结构 | 真实分页接口数据 | 标题、`.payment-offline-refunds__toolbar`、`.payment-offline-refunds__table`、`.payment-offline-refunds__pagination` 可见 | 搜索控件、表格、分页位于统一支付页面结构内 | 无 console error；支付接口无 requestfailed | `mango-ui/apps/mango-admin/test-results/payment-ui-offline-refunds.png` | 通过 |

## 4. 回归抽查记录

| 模块 | 页面 | 功能点 1 | 功能点 2 | UI 细节 | 截图/trace | 结论 |
|---|---|---|---|---|---|---|
| 支付中心 | 对账管理 | 页面可通过菜单路由访问 | 真实分页表格加载完成 | header、toolbar、table、pagination 间距和结构与其他支付页面一致 | `mango-ui/apps/mango-admin/test-results/payment-ui-reconciliations.png` | 通过 |
| 支付中心 | 操作审计 | 页面可通过菜单路由访问 | 真实分页表格加载完成 | 搜索区不再使用栅格卡片残留 | `mango-ui/apps/mango-admin/test-results/payment-ui-operation-audits.png` | 通过 |
| 支付中心 | 结算汇总 | 页面可通过菜单路由访问 | 真实分页表格加载完成 | 搜索区不再使用栅格卡片残留，分页区域统一 | `mango-ui/apps/mango-admin/test-results/payment-ui-settlement-summaries.png` | 通过 |

## 5. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 | 用户确认 |
|---|---|---|---|---|
| 全量支付业务闭环 E2E | 本次任务范围是页面搜索区、列表区、表单区 UI 精细化调整 | 不影响本次 UI 结构验收结论；支付业务链路需由既有专项 E2E 覆盖 | 后续按支付闭环任务继续跑对应 E2E | 无需额外确认 |
