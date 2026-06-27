# Issue 274 Admin Shell 目录菜单权限跳转修复交付契约

## 1. 目标

修复 Admin Shell 目录型菜单使用固定 `redirect` 时未校验当前用户可见菜单权限的问题。目录菜单应按当前用户可见菜单树跳转到可访问页面，避免进入无权限的固定 redirect 页面。

## 2. 范围

- `@mango/admin-shell` 菜单树解析工具。
- Admin Shell 顶部菜单点击跳转。
- Admin Shell 目录 route 直接访问时的 redirect fallback。
- 对应 Vitest 回归用例。

## 3. 不做什么

- 不修改后端菜单、角色、权限接口。
- 不修改业务模块菜单配置。
- 不新增或变更页面组件、路由路径、权限码、数据库数据。
- 不发布 npm 包。

## 4. 设计输入

- GitHub issue #274：Admin Shell 目录型菜单应按当前用户权限跳转到第一个可访问子页面。
- 用户确认的实现设计：在 admin-shell 菜单解析层统一校验 redirect，并复用到顶部菜单点击和目录 route fallback。

## 5. 设计说明

### 5.1 影响模块

- `mango-ui/packages/admin-shell/src/runtime/menuHost.ts`
- `mango-ui/packages/admin-shell/src/layout/navBars/index.vue`
- `mango-ui/packages/admin-shell/src/__tests__/menuHost.spec.ts`

### 5.2 接口变化

无后端 API 变化。前端内部新增 `resolveAccessibleMenuPath` 导出，用于 Admin Shell 内部导航解析。

### 5.3 数据变化

无数据库、默认菜单数据或权限数据变化。

### 5.4 菜单/页面/权限变化

不改变菜单、页面、权限定义。改变 Admin Shell 对当前用户可见菜单树的跳转解析：

- 可运行页面菜单优先进入自身。
- 目录菜单配置的 `redirect` 只有命中当前可见且可运行的菜单时才生效。
- `redirect` 不可访问或不可运行时，按当前可见菜单树深度优先进入第一个可运行子页面。
- 没有可运行子页面时返回空，由既有空状态或无权限路径处理。

### 5.5 测试范围

- 单元测试覆盖 redirect 可访问、redirect 不在可见树、redirect 不可运行、页面菜单自身可运行、目录无可访问子页面。
- 包构建验证 admin-shell 类型和生产构建。

## 6. 风险与限制

- 本次未启动完整 Admin Shell 浏览器验收；改动集中在菜单解析纯逻辑和顶部菜单点击入口，已用单测和包构建覆盖。
- 本次不发布 npm 包；合并后如需业务项目消费，需要按后续 release 流程发布 `@mango/admin-shell`。

## 7. 交付台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| TASK-001 | issue #274 验收标准 1 | 用户点击目录型菜单时，不跳转到当前用户无权限的固定 redirect 页面 | `resolveAccessibleMenuPath` 校验 redirect 是否在当前可见菜单树且可运行 | `menuHost.ts`、`navBars/index.vue` | `pnpm --filter @mango/admin-shell test` | DONE | 本文件第 8 节 |
| TASK-002 | issue #274 验收标准 2 | redirect 目标无权限时自动进入当前用户有权限的第一个子页面 | redirect 不可访问或不可运行时深度优先查找第一个可运行子页面 | `menuHost.ts`、`menuHost.spec.ts` | `pnpm --filter @mango/admin-shell test` | DONE | 本文件第 8 节 |
| TASK-003 | issue #274 验收标准 3 | 顶部菜单、侧边菜单行为一致 | 顶部菜单点击改用 admin-shell 统一解析；侧边目录 path 进入后复用 route fallback | `navBars/index.vue`、`menuHost.ts` | `pnpm --filter @mango/admin-shell test`、`pnpm --filter @mango/admin-shell build` | DONE | 本文件第 8 节 |
| TASK-004 | issue #274 验收标准 4 | 业务模块不需要为不同角色维护不同顶层 redirect | 逻辑下沉到 Admin Shell 菜单解析层，不改业务菜单配置 | `menuHost.ts` | `git diff --check`、代码审阅 | DONE | 本文件第 8 节 |

## 8. 验收证据记录

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| TASK-001 | Admin Shell 菜单解析 | 目录 redirect 不在当前可见菜单树时 fallback | `/guarantee` redirect `/guarantee/overview`，当前树只有 `/guarantee/risk-review` | `resolveAccessibleMenuPath` 和 `resolveDirectoryRouteRedirect` 返回 `/guarantee/risk-review` | 未启动浏览器，顶部点击入口调用同一解析函数 | 不适用，纯单测 | `pnpm --filter @mango/admin-shell test` 通过 | DONE |
| TASK-002 | Admin Shell 菜单解析 | 目录 redirect 目标不可运行时 fallback | `/guarantee/overview` 缺少 component，`/guarantee/risk-review` 可运行 | 返回 `/guarantee/risk-review` | 未启动浏览器，逻辑单测覆盖 | 不适用，纯单测 | `pnpm --filter @mango/admin-shell test` 通过 | DONE |
| TASK-003 | Admin Shell 顶部菜单/目录 route | 顶部菜单和目录 route 复用统一解析 | `navBars/index.vue` 使用 `resolveAccessibleMenuPath`，`resolveDirectoryRouteRedirect` 同源 | 包构建通过，类型有效 | 未启动浏览器 | 不适用，未运行 E2E | `pnpm --filter @mango/admin-shell build` 通过 | DONE |
| TASK-004 | 业务菜单配置 | 不要求业务按角色维护不同 redirect | 无业务配置改动 | diff 仅涉及 admin-shell 代码、测试和本台账 | 不适用 | 不适用 | `git diff --check` 通过 | DONE |
