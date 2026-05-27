# Mango 外部发布物消费阻断修复计划

## 1. 目标

修复业务项目直接消费 Mango 发布物时遇到的阻断问题，使前端 `@mango/admin-pages` 和相关 npm 包可以被外部项目稳定依赖，并避免 `mango-file-preview-starter` 覆盖宿主应用端口。

## 2. 范围

- 修复 `@mango/admin-pages` 根入口引用 monorepo 内部 `apps/*` 路径的问题。
- 为 `@mango/admin-pages` 提供稳定子路径导出。
- 清理已发布前端包中的 `workspace:*` 依赖语义。
- 将 `mango-file-preview-engine` 配置中的端口配置迁移到独立命名空间，避免 starter 污染宿主 `server.port`。
- 记录 `@mango/admin-shell`、Mango Initializr、初始化数据、菜单/权限/资源同步规范为后续 Sprint 项。

## 3. 不做什么

- 本次不实现完整 `@mango/admin-shell` 壳包。
- 本次不实现 Mango Initializr 服务。
- 本次不实现初始化数据种子能力。
- 本次不实现菜单/权限/资源同步生成器。
- 本次不停止正在运行的业务服务。

## 4. 设计输入

- 用户反馈的 8 个外部消费问题。
- `mango-pmo/rules/frontend/06-monorepo-architecture.md`。
- `mango-pmo/rules/frontend/01-vue-code.md`。
- `mango-pmo/rules/backend/05-module.md`。
- `mango-pmo/rules/backend/08-test.md`。

## 5. 设计说明

### 5.1 影响模块

- `mango-ui/packages/admin-pages`
- `mango-ui/packages/*/package.json`
- `mango-ui/apps/mango-admin-shell`
- `mango-ui/packages/workflow-business-example`
- `mango/mango-platform/mango-file-preview/mango-file-preview-engine`
- `mango-docs/plans`

### 5.2 接口变化

前端 npm 包导出契约变化：

- `@mango/admin-pages`
- `@mango/admin-pages/core`
- `@mango/admin-pages/defaults`
- `@mango/admin-pages/dev-component-pages`

后端业务 API 无变化。

### 5.3 数据变化

无数据库结构变化。

### 5.4 菜单/页面/权限变化

无菜单、页面、权限数据变化。

### 5.5 测试范围

- 前端包构建。
- `workspace:*` 依赖扫描。
- `@mango/admin-pages` 内部路径扫描。
- 后端 file-preview engine 测试。
- file-preview 配置扫描，确认不再声明 `server.port`。
- 交付台账检查。

## 6. 风险与限制

- `@mango/admin-shell` 是较大的壳包产品化工作，本次只修复当前外部消费阻断和内部源码路径依赖。
- npm 包当前仍以源码入口发布，完整 dist/types 发布策略需要单独 Sprint 收敛。
- Initializr、初始化数据、资源同步规范需要 PMO/Tech Lead 评审后进入后续 Sprint。

