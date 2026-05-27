# {{moduleName}} {{aggregatePascal}} 交付契约

## 1. 目标

交付 {{aggregatePascal}} 的后端 API、资源清单、前端页面注册和验证资产。

## 2. 范围

- 后端新增 `{{moduleKebab}}` 模块接口、服务、Controller 和资源清单。
- 前端新增 `{{moduleKebab}}` API client、类型、页面和 page registry。
- 菜单 component 与前端 page registry key 保持一致。

## 3. 不做什么

- 不修改 Mango 框架源码。
- 不绕过后端 owner review。
- 不直接写平台权限表。

## 4. 设计输入

- Mango PMO baseline。
- 当前业务领域规则。
- 本次业务需求说明。

## 5. 设计说明

### 5.1 影响模块

- `backend/modules/{{moduleKebab}}`
- `frontend/packages/{{moduleKebab}}`
- `frontend/packages/{{moduleKebab}}-api`

### 5.2 接口变化

新增 `{{modulePascal}}Api`，返回 `R<T>`。

### 5.3 数据变化

新增 `db/migration/{{moduleKebab}}` 下的 Flyway migration。

### 5.4 菜单/页面/权限变化

新增 `META-INF/mango/resource-manifest.json`，菜单 `component` 使用 `{{moduleKebab}}/{{aggregateKebab}}/index`。

### 5.5 测试范围

覆盖 API 契约、业务服务、资源清单和前端页面注册。

## 6. 风险与限制

变量替换后必须执行生成项目的 Maven、pnpm 和浏览器验证。
