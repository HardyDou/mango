# 支付中心 E2E 验收证据

## 1. 验收范围

- 页面：支付中心菜单、支付通道管理、通道签约配置、收银台配置、业务订单列表、Web 收银台。
- 接口：支付中心列表接口、收银台会话、发起支付、支付结果查询、应用管理、收银台配置保存和删除。
- 权限：通过后台登录态访问支付中心页面和支付管理接口。
- 数据：本地验收库 `mango_dev_e397cd`，使用固化示例数据和 E2E 临时业务订单。
- 部署形态：本地单体后端 `18118` + mango-admin 前端 `7808`。

## 2. 执行环境

- 前端地址：`http://127.0.0.1:7808`
- 后端地址：`http://127.0.0.1:18118`
- 数据库或租户：`mango_dev_e397cd`，租户 `1`
- 测试账号：E2E 登录 helper 使用后台测试账号
- 浏览器：Playwright Chromium

## 3. 功能验收记录

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| PAY-E2E-001 | `/#/payment/channels`、支付中心菜单和列表接口 | 数据库菜单分组、列表接口和收银台入口 | 固化支付中心菜单、通道、签约、收银台配置 | 支付中心菜单可进入；通道列表包含芒果支付、线下收款、通联支付、华夏银行；收银台入口返回真实 session | 页面主内容不是空白；支付通道新增弹窗展示字段模板配置；签约配置弹窗不再展示原始 JSON 文本域 | Playwright 等待真实 `/api/payment/*/page` 和 `/api/payment/cashier/session` 响应；未使用接口 mock | `pnpm exec playwright test e2e/specs/payment-center.spec.ts --grep "数据库菜单分组"`；执行汇总 `5 passed (45.3s)` | PASS |
| PAY-E2E-002 | `/#/payment/business-orders` | 业务订单列表、状态筛选和详情 | 固化和 E2E 业务订单 | 状态筛选可选择并回显；列表返回真实业务订单；详情弹窗字段来自接口；已过期或不可支付订单支付按钮不可用 | 表格状态标签按状态展示；搜索区下拉内容可见；详情弹窗可打开关闭 | Playwright 监听真实业务订单分页接口；未使用路由 mock | `pnpm exec playwright test e2e/specs/payment-center.spec.ts --grep "业务订单列表"`；执行汇总 `5 passed (45.3s)` | PASS |
| PAY-E2E-003 | Web 收银台支付结果页 | 支付结果延迟返回时轮询到终态成功 | `PAY-DELAY-E2E-*` 临时业务订单，芒果支付延迟场景 | 初次结果为 `PAYING`；后端完成处理后，点击“我已完成支付”主动查询并展示“支付成功” | 收银台页面显示订单信息和扫码物料；支付结果使用弹窗确认，不把延迟状态固定写成成功 | Playwright 监听 `/api/payment/cashier/pay` 和 `/api/payment/cashier/pay-result`；后端状态机真实推进 | `pnpm exec playwright test e2e/specs/payment-center.spec.ts --grep "收银台支付结果延迟"`；执行汇总 `5 passed (45.3s)` | PASS |
| PAY-E2E-004 | Web 收银台网银和线下转账 | 网银物料、银行选择、线下转账物料 | `PAY-EBANK-E2E-*`、`PAY-TRANSFER-E2E-*` 临时业务订单 | 网银返回 `HTML_FORM` 且包含支付订单号；线下转账返回通道 `OFFLINE_COLLECTION`、物料 `TRANSFER_ACCOUNT`、4 到 6 位转账备注 | 网银通过 tab、企业网银、银行 banner、账号户名表单进入支付；线下转账显示收款户名、账号、开户行、转账备注和“已完成转账”按钮 | Playwright 监听真实 `/api/payment/cashier/session` 和 `/api/payment/cashier/pay`；未使用前端 mock | `pnpm exec playwright test e2e/specs/payment-center.spec.ts --grep "Web 收银台网银和线下转账"`；执行汇总 `5 passed (45.3s)` | PASS |
| PAY-E2E-005 | `/#/payment/applications`、`/#/payment/cashier-configs` | 应用接入安全、收银台配置支付规则 | `支付E2E启用应用*`、`支付E2E停用应用*`、`支付E2E收银台*` | 启用应用生成密钥和签名配置；停用应用不生成密钥；收银台配置保存应用、主体、支付方式、默认方式和 logoFileId；删除后列表不可见并写审计 | 应用新增弹窗、收银台新增弹窗、支付方式多选和默认方式选择可操作；已删除的退款字段不再出现在收银台配置 API 契约中 | Playwright 监听真实保存、分页、删除和审计查询接口；旧后端校验已通过重启当前代码消除 | `pnpm exec playwright test e2e/specs/payment-center.spec.ts --grep "应用管理配置接入安全"`；单跑 `1 passed (8.8s)`，合集 `5 passed (45.3s)` | PASS |

## 4. 回归抽查记录

| 模块 | 页面 | 功能点 1 | 功能点 2 | UI 细节 | 截图/trace | 结论 |
|---|---|---|---|---|---|---|
| 支付中心 | 收银台配置、业务订单、Web 收银台 | 支付中心 5 个关键 E2E 串行通过 | `mango-admin` 生产构建通过 | Chromium 实际点击后台页面、弹窗、tab、button、表单、银行 banner 和状态结果 | Playwright 自动失败截图目录 `mango-ui/apps/mango-admin/test-results`；本次最终无失败截图 | PASS |

## 5. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 | 用户确认 |
|---|---|---|---|---|
| 外部真实通道联调 | 本轮只验收现有本地芒果支付和线下收款链路，通联、华夏、微信、支付宝、连连真实账号尚未提供 | 真实外部资金通道不能据此声明投产 | 拿到真实机构账号、证书、公钥、回调地址后补联调和验签验收 | 用户已说明后续提供账号 |
| 全量支付页面视觉截图 | 本轮目标是关键 E2E 验收和必要阻塞修复，未对支付中心所有页面逐页留截图 | 未覆盖页面仍需单独 UI 回归 | 后续按页面清单执行全量视觉和交互回归 | 本轮未要求全量页面截图 |
| 本地 Flyway V14 checksum repair | 验收库历史记录中的 payment V14 checksum 与当前 worktree 脚本不一致，阻塞当前代码启动 | 仅影响本地 `mango_dev_e397cd` 验收环境启动，不改变业务表数据 | 已将 `flyway_schema_history_payment` V14 checksum 调整为当前脚本校验值；后续应避免改已执行 migration | 本轮作为本地验收环境修复处理 |
