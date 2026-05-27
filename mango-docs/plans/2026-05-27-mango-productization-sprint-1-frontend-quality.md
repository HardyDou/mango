# Mango 产品化 Sprint 1 前端质量修复

## 1. 目标

修复 Issue #26 中前端可独立处理的两个质量问题：`@mango/common` CodeEditor 在 Vite dev 下出现 `CodeMirror is not defined`，以及 `@mango/workflow-business-example` 同一页面包被静态和动态导入导致构建 warning。

## 2. 范围

- 调整 `mango-ui/packages/common/components/CodeEditor` 的 CodeMirror 5 加载方式。
- 调整 `mango-ui/packages/admin-pages/src/defaults.ts` 对 workflow business example 的注册方式。
- 补充或调整前端组件测试和构建验证。
- 更新本 Sprint 交付台账。

## 3. 不做什么

- 不升级到 CodeMirror 6。
- 不实现 `@mango/admin-shell` 产品化壳包。
- 不实现菜单权限资源同步、业务模板或 Initializr。
- 不调整后端接口、数据库、菜单和权限数据。

## 4. 设计输入

- GitHub Issue #26 #4：修复 `@mango/common` CodeEditor。
- GitHub Issue #26 #14：解决 `@mango/workflow-business-example` 构建 warning。
- `mango-docs/plans/2026-05-27-mango-productization-issue-26-plan.md`。
- `mango-pmo/rules/frontend/01-vue-code.md`。
- `mango-pmo/rules/frontend/04-test.md`。
- `mango-pmo/rules/frontend/05-dev-flow.md`。
- `mango-pmo/rules/frontend/06-monorepo-architecture.md`。

## 5. 设计说明

### 5.1 影响模块

- `mango-ui/packages/common/components/CodeEditor`
- `mango-ui/packages/admin-pages`
- `mango-ui/packages/workflow-business-example`
- `mango-docs/plans`

### 5.2 接口变化

无对外业务 API 变化。

前端组件公开 props、emits 和 expose 方法保持兼容。

### 5.3 数据变化

无数据库结构和数据变化。

### 5.4 菜单/页面/权限变化

无菜单、页面路径和权限数据变化。

`workflow/business-form/index` 仍由 `@mango/workflow-business-example` 动态加载页面组件。

### 5.5 测试范围

- CodeEditor 组件单元测试。
- workflow business example 注册测试。
- admin-pages 构建或 mango-admin 构建，确认不再出现静态和动态导入混用 warning。
- 交付台账检查。

## 6. 风险与限制

- CodeMirror 5 是 CommonJS 形态依赖，修复重点是稳定加载，不改变编辑器能力模型。
- 本 Sprint 不处理完整 `@mango/admin-shell` 产品化，因此只消除当前构建 warning，不重构整个页面注册体系。
