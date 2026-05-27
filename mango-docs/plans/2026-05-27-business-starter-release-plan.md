# Mango Business Starter 发布修复计划

## 1. 目标

发布前确认 `create-mango-app` 作为独立 npm 包可直接生成业务项目，并完成 Mango 前后端物料发布验证。

## 2. 范围

- 修复 `create-mango-app` 发布包不携带 `mango-business-starter` 的问题。
- 修复 npm 发布脚本对非 scope `create-*` 包名的识别问题。
- 使用 npm pack/tarball 验证发布包内置 starter 后可以生成项目。
- 发布 npm 包和 Maven snapshot 物料。

## 3. 不做什么

- 不实现 Mango Initializr Web 服务。
- 不调整所有包版本号。
- 不启动完整业务系统 E2E 作为发布前置门禁。

## 4. 设计说明

### 4.1 影响模块

- `mango-ui/packages/create-mango-app`
- `mango-ui/scripts/publish-package.mjs`
- `mango-docs/plans`

### 4.2 接口变化

无 HTTP API 变化。CLI 默认模板路径调整为优先使用包内 `templates/mango-business-starter`，仓内开发态回退到根目录 `mango-business-starter`。

### 4.3 数据变化

无数据库变化。

### 4.4 菜单/页面/权限变化

无 Mango 运行态菜单、页面和权限数据变化。

### 4.5 测试范围

- starter 资产检查。
- CLI 使用显式模板生成项目。
- CLI 使用包内模板生成项目。
- npm pack 后从 tarball 执行 CLI 生成项目。
- 发布后从 Nexus 安装/执行 CLI 验证。

## 5. 完成标准

- `create-mango-app` npm 包包含 `templates/mango-business-starter`。
- `npm create mango-app@latest` 或等价 tarball 执行可生成项目。
- 生成项目携带 Mango PMO baseline。
- 本轮台账检查通过。
