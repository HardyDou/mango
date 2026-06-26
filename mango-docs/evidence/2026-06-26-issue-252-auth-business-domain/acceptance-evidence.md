# Issue #252 AUTH 业务域注入验收证据

## 1. 验收范围

- 页面：`/#/notice/message-definition`、`/#/notice/receive-setting`
- 接口：`/domain/domains/enabled-tree`、`/notice/business-types`、`/notice/business-types?domainCode=AUTH`、`/notice/receive-preferences`
- 权限：admin 本地测试账号，`internal-admin`
- 数据：`biz_domain.domain_code=AUTH`、认证授权通知业务类型 `auth.login.locked`、`auth.login.success`
- 部署形态：本地 worktree 服务，前后端直连测试库

## 2. 执行环境

- 前端地址：`http://127.0.0.1:7984`
- 后端地址：`http://127.0.0.1:18294`
- 数据库：`127.0.0.1:3306/mango_dev_846baf`
- 测试账号：`admin/admin123`
- 浏览器：Playwright Chromium 1.59.1

## 3. 功能验收记录

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| TASK-001 | Resource YAML | auth starter 声明 `AUTH` 业务域 | `auth-common-domain.yml` | Maven 测试通过，资源可被后端启动同步 | 非 UI 项：通过 Maven 构建验证资源可用 | 非网络项：命令退出码 0 | `logs/build.log` | PASS |
| TASK-002 | 数据库 | Resource Registry 同步写入业务域 | `AUTH` | `biz_domain` 中 `domain_name=认证授权`、`status=1`；`resource_registry` 中 `resource_id=2026061800200000190`、`status=ACTIVE` | 非 UI 项：通过 SQL 查询验证 | 非网络项：SQL 命令退出码 0 | `logs/db-auth-domain.txt` | PASS |
| TASK-003 | `/notice/message-definition` | 业务域树显示并筛选 AUTH | `AUTH` | `/domain/domains/enabled-tree` 返回 `AUTH`；点击业务域树 `认证授权 / AUTH` 后请求 `/notice/business-types?domainCode=AUTH` | 表格展示 `auth.login.locked` 和 `auth.login.success`，业务域列为 `AUTH` | `consoleErrors=[]`，`failedRequests=[]` | `logs/ui-auth-domain.json`, `screenshots/notice-business-config-auth-domain-tree.png`, `screenshots/notice-business-config-auth-filtered.png` | PASS |
| TASK-003 | `/notice/receive-setting` | 接收设置页加载业务类型数据 | admin | 页面加载 `接收规则配置`，接口 `/notice/receive-preferences` 和 `/notice/business-types?pageNum=1&pageSize=200` 返回 200 | 页面可打开，无 404 或空白 | `consoleErrors=[]`，`failedRequests=[]` | `logs/ui-auth-domain.json`, `screenshots/notice-receive-setting-loaded.png` | PASS |
| TASK-004 | 构建/测试 | 后端测试、前端 notice 包构建 | 当前分支 | 命令退出码 0 | 非 UI 项：构建和测试命令验证 | 非网络项：命令退出码 0 | `logs/build.log` | PASS |
| TASK-005 | PMO 检查 | 台账和证据合规 | 当前证据目录 | `delivery-contract-check` 与 `acceptance-evidence-check` 退出码 0 | 非 UI 项：PMO 工具检查 | 非网络项：命令退出码 0 | `logs/pmo-checks.log` | PASS |

## 4. 回归抽查记录

| 模块 | 页面 | 功能点 1 | 功能点 2 | UI 细节 | 截图/trace | 结论 |
|---|---|---|---|---|---|---|
| notice | 消息配置 | 业务域树保留 COMMON/WORKFLOW/NOTICE 等既有域 | AUTH 域点击后按编码筛选 | 表格列、操作按钮保持可见 | `screenshots/notice-business-config-auth-domain-tree.png`, `screenshots/notice-business-config-auth-filtered.png` | PASS |
| notice | 接收设置 | 页面正常加载偏好设置 | 拉取业务类型列表无错误 | 页面无空白和 404 | `screenshots/notice-receive-setting-loaded.png` | PASS |

## 5. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 | 用户确认 |
|---|---|---|---|---|
| 全量 admin 样式依赖构建 | 既有 `@mango/file` 深层导入阻断，和本 issue 无关；本次仅补本地 ignored `dist/style.css` 运行产物以解除 Vite overlay | 不影响提交内容；只影响本地 dev server 验证准备 | 另行处理 `@mango/file` 构建问题 | 不需要 |
