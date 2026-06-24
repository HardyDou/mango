# Element Plus 中文 Locale 修复交付契约

## 1. 目标

修复 Mango 管理端中 Element Plus 内置组件文案部分显示英文的问题，确保分页等内置组件在主后台、admin shell 和微前端应用中统一使用中文 locale。

## 2. 范围

- `mango-admin` 主应用启动入口。
- `admin-shell` 公共启动入口。
- RBAC、workflow、template 三个微前端应用的独立启动入口和 micro 挂载入口。

## 3. 不做什么

- 不调整业务页面自定义文案。
- 不改动 `vue-i18n` 业务语言包结构。
- 不新增依赖。
- 不调整 Element Plus 样式或主题变量。

## 4. 设计输入

- 用户反馈 Element Plus 部分组件文案为英文，典型场景为分页组件。
- 已确认采用方案 1：在全局安装 Element Plus 时传入中文 locale。
- Element Plus 支持通过 `app.use(ElementPlus, { locale })` 配置内置组件语言。

## 5. 设计说明

### 5.1 影响模块

- `mango-ui/apps/mango-admin/src/main.ts`
- `mango-ui/packages/admin-shell/src/appBootstrap.ts`
- `mango-ui/apps/mango-admin-rbac-app/src/main.ts`
- `mango-ui/apps/mango-admin-rbac-app/src/micro.ts`
- `mango-ui/apps/mango-admin-workflow-app/src/main.ts`
- `mango-ui/apps/mango-admin-workflow-app/src/micro.ts`
- `mango-ui/apps/mango-admin-template-app/src/main.ts`
- `mango-ui/apps/mango-admin-template-app/src/micro.ts`

### 5.2 接口变化

无接口变化。

### 5.3 数据变化

无数据库、缓存或持久化数据变化。

### 5.4 菜单/页面/权限变化

无菜单、路由、页面 key 或权限码变化。用户可见变化仅限 Element Plus 内置组件默认文案变为中文。

### 5.5 测试范围

- 管理端样式聚合依赖生成。
- admin 样式治理检查。
- admin 模块样式治理检查。
- `mango-admin` 生产构建。
- 微前端应用构建。
- 检查 Element Plus 注册入口不再保留裸 `app.use(ElementPlus)`。

## 6. 风险与限制

- 本次为全局初始化配置变更，影响所有依赖这些入口安装 Element Plus 的内置组件默认文案。
- 未启动浏览器做真实分页页面截图验收；已通过生产构建和入口代码检查验证配置生效路径。
- 构建过程中出现既有 dynamic import/static import chunk warning 和 chunk size warning，本次未修改打包拆分策略。

## 7. 交付台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| EPL-001 | 用户要求 | Element Plus 分页等内置组件文案使用中文 | 在所有 Element Plus 安装入口引入 `zhCn` 并传入 `locale` | 8 个 Element Plus 初始化入口 | 代码 diff 与入口检索 | DONE | 本文件第 5.1 节列出的源码文件 |
| EPL-002 | 项目规范 | 官方 admin、shell 和微前端构建保持可用 | 不改公共 API 和依赖，仅调整初始化参数 | `mango-ui` 构建产物 | `pnpm.cmd -F mango-admin build`、`pnpm.cmd build:micro` | DONE | 验证命令输出 |
| EPL-003 | PMO 样式门禁 | 前端官方模块相关 PR 需执行样式检查 | 执行 admin 样式聚合与模块样式检查 | `mango-ui` 样式治理检查 | `pnpm.cmd admin:styles:check`、`pnpm.cmd admin:module-styles:check` | DONE | 验证命令输出 |
| EPL-004 | PMO 交付门禁 | PR 前完成交付契约和台账检查 | 使用本文件作为设计说明和交付台账 | `mango-docs/plans/2026-06-24-element-plus-zh-locale-delivery.md` | `delivery-contract-check.mjs --mode verify` | DONE | 本文件 |

## 8. 验收证据记录

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| EPL-001 | Element Plus 初始化入口 | 中文 locale 注入 | 8 个启动入口源码 | `app.use(ElementPlus, { locale: zhCn })` 覆盖主应用、shell 和微前端入口 | 未启动页面，代码检查确认 | 不涉及 | `rg` 入口检索输出 | DONE |
| EPL-002 | 管理端和微前端构建 | 生产构建 | 当前任务分支 | `mango-admin` 与微前端构建通过 | 未启动页面，构建检查确认 | 不涉及 | 构建命令输出 | DONE |
| EPL-003 | 样式治理 | admin 样式检查 | 当前任务分支 | admin 样式检查和模块样式检查通过 | 不涉及 | 不涉及 | 样式检查命令输出 | DONE |
| EPL-004 | PMO 检查 | 交付台账完整性 | 本文件 | 台账列完整且全部 DONE | 不涉及 | 不涉及 | `delivery-contract-check` 输出 | DONE |
