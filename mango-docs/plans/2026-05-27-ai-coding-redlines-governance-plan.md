# AI 编码红线规范治理计划

## 1. 背景

业务开发和 Mango 框架开发都需要统一 AI 编码底线，避免伪实现、固定成功、未接真实数据却声明完成等交付风险。

## 2. 目标

新增一条长期有效的 AI 编码红线规范，并让 Mango 内部 PMO preflight 与业务项目 Mango baseline 默认加载。

## 3. 范围

- 在 `mango-pmo` 新增 AI 编码红线规范。
- 将该规范接入 PMO preflight 全局必读。
- 同步到 `mango-business-starter` 的 `business-pmo/mango-baseline`。
- 同步到 `create-mango-app` 内置业务 starter 模板。
- 更新模板检查，确保新业务项目不会漏带该规范。

## 4. 不做什么

- 不修改业务代码。
- 不新增静态扫描器。
- 不改变已有后端、前端、数据库和测试规范内容。

## 5. 设计说明

### 5.1 影响模块

- `mango-pmo/rules`
- `mango-business-starter/business-pmo/mango-baseline`
- `mango-ui/packages/create-mango-app/templates/mango-business-starter/business-pmo/mango-baseline`
- `mango-business-starter/scripts/check-template.mjs`
- `mango-ui/packages/create-mango-app/templates/mango-business-starter/scripts/check-template.mjs`

### 5.2 接口变化

无 HTTP API 变化。PMO preflight 输出会新增 `rules/03-ai-coding-redlines.md`。

### 5.3 数据变化

无数据库变化。

### 5.4 菜单/页面/权限变化

无运行态菜单、页面和权限变化。

### 5.5 测试范围

- Mango PMO preflight 输出包含 AI 编码红线。
- 业务 starter baseline preflight 输出包含 AI 编码红线。
- create-mango-app 生成项目后，生成项目 baseline preflight 输出包含 AI 编码红线。
- starter 模板检查通过。
- 交付台账检查通过。

## 6. 完成标准

- Mango 内部开发和业务项目开发都会默认加载 AI 编码红线。
- 新业务项目模板携带该规范文件。
- 交付台账全部完成且无未完成项。
