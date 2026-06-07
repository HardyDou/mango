# 业务域管理模块 E2E 验收证据

## 1. 验收范围

- 页面：`/#/system/domain`
- 接口：`/domain/domains/tree`、`/domain/domains/enabled-tree`、`/domain/domains`、`/domain/domains/code`
- 权限：平台管理员 `admin` 访问系统设置下业务域菜单并执行维护操作
- 数据：默认租户 `tenantId=1`，内置业务域 `COMMON`，E2E 临时父子业务域
- 部署形态：本地 worktree 独立端口，前端 `8503`，后端 `18813`

## 2. 执行环境

- 前端地址：`http://127.0.0.1:8503`
- 后端地址：`http://127.0.0.1:18813`
- 数据库或租户：`mango_dev_254a6b` / 默认租户 `1`
- 测试账号：`admin / admin123`
- 浏览器：Playwright Chromium

## 3. 功能验收记录

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| DOMAIN-E2E-001 | `/#/system/domain`、`/domain/domains/enabled-tree` | 业务域管理闭环：登录、访问菜单、查询树列表、新增顶级域、新增下级域、编辑、停用、启用、删除 | `E2E_DOMAIN_<timestamp>`、`E2E_DOMAIN_<timestamp>_PAY`、内置 `COMMON` | 页面树列表展示 `COMMON`；下级域最终编码自动拼接为父域编码加本层编码；编辑弹窗内本层编码禁用；停用后 `enabled-tree` 不再返回父域；启用后 `enabled-tree` 恢复返回父域；删除子域和父域后列表不再展示对应编码 | 业务域菜单可进入；新增业务域弹窗、编辑业务域弹窗、删除确认弹窗均完成真实点击；状态按钮从停用切换到启用后可继续操作 | 监听浏览器 console error 数组为空；创建接口 `POST /domain/domains` 和编辑接口 `PUT /domain/domains` 响应成功；无 `401/403/404/500/未授权/没有权限/拒绝访问/加载失败/登录已过期/请重新登录` 文案残留 | 命令：`PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true pnpm -C mango-ui --filter mango-admin exec playwright test e2e/specs/domain-management.spec.ts --project=chromium --reporter=line`；结果：`1 passed (9.2s)` | PASS |

## 4. 回归抽查记录

| 模块 | 页面 | 功能点 1 | 功能点 2 | UI 细节 | 截图/trace | 结论 |
|---|---|---|---|---|---|---|
| 业务域 | `/#/system/domain` | 默认内置业务域种子数据可见 | 临时 E2E 数据在 finally 中通过接口清理 | 表格、弹窗、状态标签、删除确认框均参与自动化交互 | Playwright Chromium 自动化日志记录 `1 passed (9.2s)` | PASS |

## 5. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 | 用户确认 |
|---|---|---|---|---|
| 多浏览器矩阵、移动端视口、跨租户隔离专项 | 本轮按用户要求启动并执行业务域主流程 E2E，未展开全量兼容矩阵 | 不影响当前 Chromium 主链路验收，但 Safari/Firefox/移动端和跨租户专项仍需后续单独覆盖 | Sprint 后续验收或适配 issue 中补充矩阵用例 | 待确认 |
