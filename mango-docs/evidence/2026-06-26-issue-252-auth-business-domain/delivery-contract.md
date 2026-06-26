# Issue #252 AUTH 业务域注入交付契约

## 1. 目标

修复认证模块消息模板、认证字典引用 `AUTH` 业务域但 auth starter 未注入 `BUSINESS_DOMAIN` 的缺陷，并让 notice 管理端和客户端列表基于真实业务域数据展示可读名称。

## 2. 范围

- `mango-auth-starter` 默认资源声明。
- `@mango/notice` 业务域筛选、列表和详情展示。
- 受影响包构建、后端资源复制、浏览器 UI 截图和交互验证。

## 3. 不做什么

- 不新增数据库 migration，业务域通过 Resource Registry 初始化。
- 不改变 notice 后端接口协议，筛选值继续使用业务域编码。
- 不处理与 `AUTH` 业务域无关的通知发送、通道、模板业务逻辑。

## 4. 设计输入

- GitHub Issue #252: Auth business domain is referenced by notice resources but not injected。
- `mango-domain` README 对 `BUSINESS_DOMAIN` 资源注入 `biz_domain` 的约定。
- `mango-notice` 中 `bizGroup/domainCode` 写入和前端展示现状。

## 5. 设计说明

### 5.1 影响模块

- 后端：`mango/mango-platform/mango-auth/mango-auth-starter`。
- 前端：`mango-ui/packages/notice`。
- 文档证据：`mango-docs/evidence/2026-06-26-issue-252-auth-business-domain`。

### 5.2 接口变化

- 无新增或变更后端 HTTP API。
- 前端复用既有 `/domain/domains/enabled-tree` 获取业务域树。

### 5.3 数据变化

- 新增 `AUTH` 业务域资源声明，Resource Registry 同步后写入 `biz_domain`：
  - `domainCode=AUTH`
  - `domainName=认证授权`
  - `status=1`
- 无 DDL 变化。

### 5.4 菜单/页面/权限变化

- 无菜单和权限变化。
- 消息中心、发送记录、接收设置、发送任务、失败重试、消息配置详情显示业务域名称和编码，例如 `认证授权（AUTH）`。
- 业务域筛选选项来自业务域树，筛选提交值仍为编码。

### 5.5 测试范围

- 后端资源声明解析和 Maven 测试。
- 前端 notice 包构建。
- 本地数据库导入资源声明，确认 `biz_domain` 存在启用的 `AUTH`。
- 浏览器 UI 截图验证消息配置页业务域树出现 `认证授权 / AUTH`，点击后按 `domainCode=AUTH` 筛选出认证授权消息定义；接收设置页加载正常且无 console/network 错误。

## 6. 风险与限制

- 浏览器 UI 验收依赖本地服务、数据库和可登录账号。
- 已有数据若未重新同步 Resource Registry，需要执行同步或清库重建后才能看到 `AUTH` 业务域。

## 7. 交付台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| TASK-001 | Issue #252 | 认证模块应声明并注入 `AUTH` 业务域 | 在 auth starter 新增 `BUSINESS_DOMAIN` YAML 资源 | `auth-common-domain.yml` | YAML 解析、Maven 资源复制、数据库查询 | DONE | `logs/build.log`, `logs/db-auth-domain.txt` |
| TASK-002 | Issue #252 | Resource Registry 同步后 `biz_domain` 中存在启用的 `AUTH` | 复用 domain 资源处理器，新增资源声明不改 DDL | `biz_domain` 初始化数据 | 本地数据库查询 `domain_code='AUTH'` | DONE | `logs/db-auth-domain.txt` |
| TASK-003 | Issue #252 | notice 页面基于业务域数据展示可读名称，保留编码筛选值 | 新增 `useNoticeDomains`，页面显示可读域名，筛选请求仍传编码 | notice 客户端和管理页面 | 浏览器截图、筛选交互、console/network 检查 | DONE | `logs/ui-auth-domain.json`, `screenshots/*.png` |
| TASK-004 | 交付门禁 | 受影响后端和前端构建/测试通过 | Maven 测试和 notice 包构建 | 构建日志 | 执行命令并记录结果 | DONE | `logs/build.log` |
| TASK-005 | PMO | 提交 PR 前留下验收证据和台账 | 使用 PMO 模板记录证据 | 交付契约、验收证据 | `delivery-contract-check` 和 `acceptance-evidence-check` | DONE | `logs/pmo-checks.log` |

## 8. 验收证据记录

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| TASK-001 | Resource YAML | `AUTH` 业务域声明 | `auth-common-domain.yml` | Maven 模块测试通过，资源可参与同步 | 不涉及 | 不涉及 | `logs/build.log` | PASS |
| TASK-002 | 数据库 | `biz_domain` 和 `resource_registry` 数据 | `AUTH` | `domain_name=认证授权`、`status=1`、resource id `2026061800200000190` 为 ACTIVE | 不涉及 | 不涉及 | `logs/db-auth-domain.txt` | PASS |
| TASK-003 | `/notice/message-definition` | 可读业务域展示与筛选 | `AUTH` | `/domain/domains/enabled-tree` 返回 `AUTH`，业务域树显示认证授权，点击后请求 `/notice/business-types?domainCode=AUTH` 并显示 `auth.login.locked`、`auth.login.success` | 业务域树点击、表格过滤正常；接收设置页加载正常 | `consoleErrors=[]`，`failedRequests=[]` | `logs/ui-auth-domain.json`, `screenshots/notice-business-config-auth-domain-tree.png`, `screenshots/notice-business-config-auth-filtered.png`, `screenshots/notice-receive-setting-loaded.png` | PASS |
| TASK-004 | 构建命令 | 后端测试、前端构建 | 当前分支 | `mvn -pl mango-platform/mango-auth/mango-auth-starter -am test` 与 `pnpm --filter @mango/notice build` 退出码 0 | 不涉及 | 不涉及 | `logs/build.log` | PASS |
| TASK-005 | PMO 检查 | 台账和证据合规 | 当前证据目录 | 检查退出码 0 | 不涉及 | 不涉及 | `logs/pmo-checks.log` | PASS |
