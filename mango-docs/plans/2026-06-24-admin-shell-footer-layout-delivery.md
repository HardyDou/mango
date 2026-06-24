# Admin Shell Footer 布局修复交付契约

## 1. 目标

修复 Mango 管理端布局 1、2、4 中 footer 悬浮在内容 padding 内的问题，使 footer 与上方工作区 tab 导航同属主布局边缘行，并为业务内容区域预留底部安全距离。

## 2. 范围

- `@mango/admin-shell` 主内容区布局组件。
- `@mango/admin-shell` README 中的布局 footer 契约说明。
- 三个业务集成排障指南的无影响说明，用于满足能力文档门禁。
- 本交付契约和验收台账。

## 3. 不做什么

- 不调整布局 3 `transverse` 的 footer 行为。
- 不改菜单、路由、权限、接口或数据结构。
- 不新增依赖。
- 不改业务页面内部布局。

## 4. 设计输入

- 用户要求布局 1、2、4 的 footer 与上方 tab 导航一行，左右和底部贴边，不再悬浮。
- 用户要求内容区域需要留出底部安全距离。
- 已确认采用方案：将 footer 从内容 padding 容器中移出，作为 Shell 布局底部行；内容滚动区保留 padding，并加入底部安全距离。

## 5. 设计说明

### 5.1 影响模块

- `mango-ui/packages/admin-shell/src/layout/component/main.vue`
- `mango-ui/packages/admin-shell/README.md`
- `mango-docs/guides/business-integration/rbac-menu-page-troubleshooting.md`
- `mango-docs/guides/business-integration/permission-button-troubleshooting.md`
- `mango-docs/guides/business-integration/tenant-dict-config-empty.md`
- `mango-docs/plans/2026-06-24-admin-shell-footer-layout-delivery.md`

### 5.2 接口变化

无 HTTP API、前端导出 API、配置字段变化。

### 5.3 数据变化

无数据库、缓存或持久化数据变化。

### 5.4 菜单/页面/权限变化

无菜单、页面 key、路由或权限码变化。用户可见变化仅限布局 1、2、4 的 footer 视觉位置和内容区底部间距。

### 5.5 测试范围

- `@mango/admin-shell` 包构建。
- `@mango/admin-shell` 单测入口。
- admin 样式聚合检查。
- PMO 交付契约检查。
- `git diff --check` 空白检查。

## 6. 风险与限制

- 本次未改布局 3 `transverse`，避免扩大用户指定范围。
- footer 仍使用原有 `--mango-layout-footer-height` 高度，不改变开关语义。
- 浏览器真实截图验收依赖本地管理端服务和登录态；本轮未启动页面，需要由后续人工在布局切换器中确认 1、2、4 三种布局的边缘贴合效果。
- `@mango/admin-shell` 单测当前有 2 个 suite 在收集阶段因 `@wangeditor/editor` 设置 `global.crypto` 失败；本次未修改测试环境配置。
- `admin:styles:check` 已通过；生成脚本会触碰聚合样式文件，本次未纳入无内容差异的生成文件。

## 7. 交付台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| FOOTER-001 | 用户要求 | 布局 1、2、4 footer 不再悬浮，贴合主布局左右和底部 | footer 从内容 padding 容器中移出，作为 Shell 底部行 | `main.vue` | 代码 diff、包构建 | DONE | `pnpm.cmd -F @mango/admin-shell build`、`pnpm.cmd -F mango-admin build` |
| FOOTER-002 | 用户要求 | 内容区域预留底部安全距离 | 内容滚动区 padding 使用 `--layout-content-safe-bottom` | `main.vue` | 代码 diff、包构建 | DONE | `pnpm.cmd -F @mango/admin-shell build` |
| FOOTER-003 | 用户要求 | 修改对应文档 | README 补布局 footer 契约，本文件记录交付台账 | README、本文件 | 文档 diff、PMO 检查 | DONE | 本文件、`mango-ui/packages/admin-shell/README.md` |
| FOOTER-004 | 项目规范 | 交付前完成验证 | 执行构建、测试、样式检查、文档门禁、空白检查和交付契约检查 | 验证命令输出 | 本文件第 8 节 | EXCEPTION | 构建、样式门禁、README 检查、能力文档门禁和空白检查通过；单测存在既有环境问题 |
| FOOTER-005 | 能力文档门禁 | admin-shell 变更需说明业务集成场景影响 | 在菜单页面、按钮权限、租户字典排障指南中补充无影响说明 | 三个业务集成排障指南 | `check-capability-docs` | DONE | `node mango-pmo\tools\check-capability-docs.mjs --base origin/main --head HEAD` |

## 8. 验收证据记录

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| FOOTER-001 | Admin Shell 布局 1/2/4 | footer 贴边 | 当前任务分支 | `defaults`、`classic`、`columns` 使用边缘 footer 路径，布局 3 不进入该路径 | 未做浏览器截图 | 不涉及接口 | `pnpm.cmd -F @mango/admin-shell build`、`pnpm.cmd -F mango-admin build` | 通过 |
| FOOTER-002 | Admin Shell 内容区 | 底部安全距离 | 当前任务分支 | 内容容器底部 padding 包含 `--layout-content-safe-bottom` | 未做浏览器截图 | 不涉及接口 | 代码 diff、包构建输出 | 通过 |
| FOOTER-003 | 文档 | 布局契约说明 | 当前任务分支 | README 和交付台账已更新 | 不涉及 | 不涉及 | 文档 diff | 通过 |
| FOOTER-004 | 工程验证 | 构建、测试、检查 | 当前任务分支 | `@mango/admin-shell build`、`mango-admin build`、`admin:styles:check`、`admin:module-styles:check`、README 检查、能力文档门禁和 `git diff --check` 通过；`@mango/admin-shell test` 记录例外 | 不涉及 | 不涉及 | 命令输出 | 例外 |
| FOOTER-005 | 业务集成指南 | 无影响记录 | 当前任务分支 | 菜单页面、按钮权限、租户字典三个排障指南均记录 admin-shell footer 布局无业务链路影响 | 不涉及 | 不涉及 | 文档 diff、能力文档门禁输出 | 通过 |
