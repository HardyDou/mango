# 前端组件开发规范治理计划

## 1. 背景

Mango 是业务开发脚手架，前端组件会被发布到私有或公开 npm 仓库，并被其它业务项目作为独立组件消费。组件需要同时服务单体部署、微前端部署、npm 独立消费和业务复用。现有规范已有 Vue、Element Plus UI 和 Monorepo 边界，但缺少针对组件开发、包边界、发布、独立消费可靠性和复用的专项规则。

## 2. 目标

- 新增前端组件开发规范。
- 明确组件必须遵守 Mango 单体、微前端和 npm 发布特性。
- 将规范接入 PMO preflight、业务 starter baseline 和 create-mango-app 模板。
- 用模板检查防止业务 baseline 漏带组件规范。

## 3. 范围

- `mango-pmo/rules/frontend/03-component-development.md`
- `mango-pmo/rules/index.json`
- `mango-business-starter/business-pmo/mango-baseline`
- `mango-ui/packages/create-mango-app/templates/mango-business-starter/business-pmo/mango-baseline`
- starter 模板检查脚本

## 4. 不做什么

- 不改现有组件实现。
- 不新增 npm 发布流水线。
- 不新增 ESLint 插件。
- 不改变运行态接口、数据库、菜单和权限。

## 5. 设计说明

### 5.1 组件规范内容

新增 `rules/frontend/03-component-development.md`，覆盖：

- Mango 单体部署、微前端部署和 npm 独立消费特性。
- 组件开发前的职责、依赖数据、输入输出、部署边界、发布边界拆解。
- 页面私有、业务包复用、Mango 公共复用的放置规则。
- 组件包边界、API 设计、样式主题、资源副作用和独立消费可靠性。
- npm 发布要求、示例文档、测试与检查。

### 5.2 入口接入

- `rules/index.json` 增加 `frontend.componentDevelopment`。
- 前端 bundle 默认加载组件开发规范。
- 业务 starter baseline 和 create-mango-app 内置 baseline 同步新增规范。

### 5.3 接口变化

无 HTTP API 变化。

### 5.4 数据变化

无数据库变化。

### 5.5 菜单/页面/权限变化

无运行态菜单、页面和权限变化。

### 5.6 测试范围

- Mango PMO preflight 验证前端组件任务会加载组件开发规范。
- 业务 starter baseline preflight 验证前端组件任务会加载组件开发规范。
- 业务 starter 模板检查通过。
- create-mango-app 模板检查和 CLI 检查通过。
- 交付台账检查通过。

## 6. 风险与限制

- 本次只制定规范和模板检查，不会自动判断所有组件是否满足 npm 发布边界。
- npm 发布流水线和包构建策略需要后续按真实发布需求单独设计。

## 7. 交付台账

交付台账见 `mango-docs/plans/2026-05-28-frontend-component-standards-governance-ledger.md`。
