# Workflow Return Path Release Delivery

## 1. 目标

处理 GitHub Issue #240，让 Mango Workflow 标准任务详情页支持业务来源返回，并发布业务项目可升级的前端包版本。

## 2. 范围

- `@mango/workflow` 标准任务详情页顶部返回按钮。
- `@mango/workflow` 标准任务详情页非暂存动作完成后的默认跳转。
- `@mango/workflow-business-example`、`@mango/grid-widgets`、`@mango/admin-shell`、`@mango/admin` 和 `@mango/cli` 的发布锁，确保业务项目升级 Mango 后获得新版 Workflow。
- 平台 `CHANGELOG.md` 发布说明。

## 3. 不做什么

- 不修改 Workflow 后端接口、数据库、流程定义模型或权限模型。
- 不修改保函业务项目代码。
- 不引入跨站外链跳转能力。

## 4. 设计输入

- GitHub Issue #240：Workflow task detail should support business return path。
- Issue 补充评论：动作成功后的默认跳转也应支持业务来源返回。

## 5. 设计说明

### 5.1 影响模块

- `mango-ui/packages/workflow/src/views/task-detail/index.vue`
- `mango-ui/packages/workflow/src/views/task-detail/__tests__/taskDetail.spec.ts`
- `mango-ui/packages/workflow/package.json`
- `mango-ui/packages/workflow-business-example/package.json`
- `mango-ui/packages/grid-widgets/package.json`
- `mango-ui/packages/admin-shell/package.json`
- `mango-ui/packages/admin/package.json`
- `mango-ui/packages/mango-cli/package.json`
- `mango-ui/packages/mango-cli/release-versions.json`
- `CHANGELOG.md`

### 5.2 接口变化

无后端接口变化。前端路由 query 新增消费：

- `returnPath`：站内绝对路径，例如 `/guarantee/risk/reviews`。
- `returnQuery`：可选 query 字符串，例如 `scope=TODO`。

### 5.3 数据变化

无数据库、migration 或持久化数据变化。

### 5.4 菜单/页面/权限变化

无菜单和权限变化。标准任务详情页保留原 `from=initiated/done/todo` 兼容逻辑。

### 5.5 测试范围

- 单测覆盖安全业务返回路径、外链和协议相对 URL 拒绝、空值拒绝、原 `from` 回退、动作完成后返回业务来源。
- 包构建覆盖 `@mango/workflow`、`@mango/workflow-business-example`、`@mango/grid-widgets`、`@mango/admin-shell`、`@mango/admin`。
- CLI release lock 测试覆盖 `@mango/cli`。

## 6. 风险与限制

- 业务项目需要在入口跳转任务详情时传入 `returnPath`，否则继续使用原 Workflow 列表回退逻辑。
- `returnPath` 只允许站内绝对路径；需要保留查询参数时使用 `returnQuery`。

## 7. 交付台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| WF-RET-001 | Issue #240 | 支持 `returnPath=/guarantee/risk/reviews` 返回业务来源页 | 标准任务详情页优先解析安全 `returnPath`，未传时回退原 `from` 逻辑 | `task-detail/index.vue` | task-detail 单测覆盖业务返回路径 | DONE | `node_modules/.pnpm/node_modules/.bin/vitest run packages/workflow/src/views/task-detail/__tests__/taskDetail.spec.ts --config .runtime/vitest-workflow-task-detail.config.ts` |
| WF-RET-002 | Issue #240 评论 | 动作完成后也回业务来源页 | 非 `save` 动作成功后复用同一业务返回地址解析 | `task-detail/index.vue` | task-detail 单测覆盖通过动作后返回业务来源 | DONE | `node_modules/.pnpm/node_modules/.bin/vitest run packages/workflow/src/views/task-detail/__tests__/taskDetail.spec.ts --config .runtime/vitest-workflow-task-detail.config.ts` |
| WF-RET-003 | Issue #240 | 对 `returnPath` 做站内路径安全校验 | 仅允许单斜杠开头的站内绝对路径，拒绝外链、协议相对 URL、空值、query/hash-in-path、反斜杠和控制字符 | `task-detail/index.vue` | task-detail 单测覆盖外链、协议相对 URL 和空值回退 | DONE | `node_modules/.pnpm/node_modules/.bin/vitest run packages/workflow/src/views/task-detail/__tests__/taskDetail.spec.ts --config .runtime/vitest-workflow-task-detail.config.ts` |
| WF-RET-004 | 发布要求 | 业务项目升级 Mango 后获得新版 Workflow | 发布 `@mango/workflow`，同步 `@mango/workflow-business-example`、`@mango/grid-widgets`、`@mango/admin-shell`、`@mango/admin` 和 `@mango/cli` 发布锁 | package versions、release lock、CHANGELOG | 包构建、CLI 测试、release notes 检查和 publish 脚本 | DONE | `pnpm -F @mango/workflow build`; `pnpm -F @mango/workflow-business-example build`; `pnpm -F @mango/grid-widgets build`; `pnpm -F @mango/admin-shell build`; `pnpm -F @mango/admin build`; `pnpm --filter @mango/cli test` |

## 8. 验收证据记录

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| WF-RET-001 | `/workflow/task/detail` | 顶部返回业务来源页 | `returnPath=/guarantee/risk/reviews`, `returnQuery=scope=TODO&tab=pending` | router 跳转 `{ path: '/guarantee/risk/reviews', query: { scope: 'TODO', tab: 'pending' } }` | 单测触发“返回”按钮 | 单测无接口错误断言 | Vitest 输出 | DONE |
| WF-RET-002 | `/workflow/task/detail` | 通过动作后返回业务来源页 | `returnPath=/guarantee/risk/reviews`, `returnQuery=scope=TODO` | 调用 `completeTask` 后 router 跳转业务来源页 | 单测触发“通过”按钮 | 单测无接口错误断言 | Vitest 输出 | DONE |
| WF-RET-003 | `/workflow/task/detail` | 不安全返回路径回退 | `https://example.com/workflow`, `//example.com/workflow`, 空值 | router 回退 `/workflow/task/done` | 单测触发“返回”按钮 | 单测无接口错误断言 | Vitest 输出 | DONE |
| WF-RET-004 | npm package | 发布包可构建 | `@mango/workflow@1.0.12`, `@mango/workflow-business-example@1.0.12`, `@mango/grid-widgets@1.0.1`, `@mango/admin-shell@1.0.22`, `@mango/admin@1.0.25`, `@mango/cli@1.0.37` | 构建和 release lock 检查通过 | 不涉及页面截图 | 不涉及浏览器网络 | 命令输出 | DONE |
