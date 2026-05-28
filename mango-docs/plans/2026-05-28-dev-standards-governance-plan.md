# 前后端开发规范治理计划

## 1. 背景

前端页面和后端模块开发在接到需求后，需要先拆解可复用组件、字典、枚举、数据模型、状态和设计模式，避免页面布局、状态样式、接口模型和领域实现各自临时发挥。

## 2. 目标

- 新增基于 Element Plus 的前端 UI 规范。
- 优化后端开发流程，要求开发前先做需求拆解。
- 让 Mango PMO preflight、业务 starter baseline 和 create-mango-app 模板都能加载新规范。
- 将能自动检查的要求接入模板或 preflight 验证。

## 3. 范围

- `mango-pmo/rules/frontend/02-element-plus-ui.md`
- `mango-pmo/rules/backend/10-dev-flow.md`
- `mango-pmo/rules/index.json`
- `mango-business-starter/business-pmo/mango-baseline`
- `mango-ui/packages/create-mango-app/templates/mango-business-starter/business-pmo/mango-baseline`
- starter 模板检查脚本和 create-mango-app 检查脚本

## 4. 不做什么

- 不改现有业务页面。
- 不新增 ESLint 插件或 Maven 插件。
- 不改变运行态接口、数据库、菜单、权限。
- 不把业务 starter baseline 作为长期规范源；长期规范源仍是 `mango-pmo`。

## 5. 设计说明

### 5.1 前端规范

新增 `rules/frontend/02-element-plus-ui.md`，覆盖：

- 需求拆解。
- 选择组件。
- 列表页搜索区、功能区、列表区、分页区。
- 表单、弹窗、抽屉。
- 字体、字号、标题、正文、辅助说明和注释。
- 状态 `ElTag` 语义映射。
- 空、加载、错误状态。
- 样式边界。
- 可检查规则和人工 review 规则。

### 5.2 后端规范

在 `rules/backend/10-dev-flow.md` 的开发前阶段追加需求拆解要求，覆盖：

- 业务对象。
- 字典和枚举。
- 数据模型。
- 接口模型。
- 业务规则。
- 辅助数据。
- 设计模式。
- 复用能力。

### 5.3 入口接入

- `rules/index.json` 增加 `frontend.elementPlusUi`。
- 前端 bundle 加载 Element Plus UI 规范。
- 业务 starter baseline 和 create-mango-app 内置 baseline 同步新增规范。

### 5.4 接口变化

无 HTTP API 变化。

### 5.5 数据变化

无数据库变化。

### 5.6 菜单/页面/权限变化

无运行态菜单、页面和权限变化。

### 5.7 测试范围

- PMO preflight 验证前端任务会加载 Element Plus UI 规范。
- PMO preflight 验证后端任务会加载后端开发流程。
- 业务 starter 模板检查通过。
- create-mango-app 检查通过或记录环境阻塞。
- 交付台账检查通过。

## 6. 风险与限制

- 字体、布局和状态色的部分一致性难以只靠 lint 完整判断，必须保留 PR review 检查。
- 本次只制定规范和模板检查，不批量改造历史页面和历史后端模块。

## 7. 交付台账

交付台账见 `mango-docs/plans/2026-05-28-dev-standards-governance-ledger.md`。
