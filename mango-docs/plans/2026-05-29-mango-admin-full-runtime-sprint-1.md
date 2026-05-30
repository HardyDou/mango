# Mango Admin Runtime 产品化 Sprint 1 设计说明

## 1. 目标

Sprint 1 交付 `@mango/admin` 完整 Admin 基座入口，让业务项目默认通过 `createMangoAdmin({ preset: 'full' })` 启动 Mango Admin，不再直接依赖和拼装底层 `@mango/admin-shell`。

本阶段必须保留 Mango 现有主框架能力：登录、后端菜单、侧栏、顶栏、用户区、设置抽屉、主题色、TagsView、页面注册和本地/微前端运行协议。

## 2. 范围

### 2.1 本阶段范围

- 新增 `mango-ui/packages/admin`，发布包名 `@mango/admin`。
- 暴露 `createMangoAdmin`、preset 类型、能力清单类型和底层实例类型。
- `createMangoAdmin` 支持 `full`、`standard`、`minimal`、`custom` 四类 preset。
- full preset 默认注册 Mango 内置能力页面，并复用 `@mango/admin-shell` 的完整布局和运行时。
- standard/minimal/custom 在本阶段建立真实契约和可执行行为，不声明已完成最终能力依赖解析。
- 业务 starter 和 create-mango-app 模板入口改为依赖 `@mango/admin`。
- 模板检查阻止 starter 继续直接导入或依赖 `@mango/admin-shell`。
- 新增 full preset E2E，保留截图和布局报告。

### 2.2 非本阶段范围

- 不在 Sprint 1 拆分 `@mango/*-api` 与 `@mango/*-admin`，该事项进入 Sprint 2。
- 不在 Sprint 1 完成 custom preset 的自动依赖补齐和冲突解析，该事项进入 Sprint 3。
- 不改变 Mango 后端菜单和权限接口契约。

## 3. 设计决策

### 3.1 包层级

```text
@mango/admin          面向业务项目的完整 Admin Runtime API
@mango/admin-shell    底层 Shell Runtime，保留布局、菜单、路由、运行时
@mango/admin-pages    页面注册和 capability manifest 协议
@mango/app-runtime    local / micro / iframe / link 运行协议
```

业务项目只能依赖 `@mango/admin`、业务能力包和必要 API 包；不得直接拼装 `@mango/admin-shell`。

### 3.2 Preset 语义

- `full`：默认完整 Mango Admin，注册全部 Mango 默认能力页面，菜单优先来自后端。
- `standard`：完整主框架加基础管理能力集合，保留可扩展模块和后端菜单能力。
- `minimal`：只注册 Shell 基础页面和调用方显式传入的能力，适合极简后台。
- `custom`：注册调用方传入能力和 registry。本阶段不自动补齐依赖，发现缺失依赖时给出显式错误。

### 3.3 Starter 接入

starter 入口由：

```ts
createMangoAdminApp(...)
```

改为：

```ts
createMangoAdmin({
  preset: 'full',
  ...
})
```

starter 仍可注册业务页面和业务菜单，但不能手写 Mango 内置菜单，也不能直接导入底层 shell。

## 4. 测试策略

### 4.1 新特性测试

- `@mango/admin` 包构建、类型导出和 package contract 通过。
- `createMangoAdmin` preset 单测覆盖默认注册、custom 注册和缺依赖失败。
- starter/template 检查能拒绝直接 `@mango/admin-shell`。
- full preset E2E 保存首页、用户下拉、设置抽屉截图和布局报告。

### 4.2 回归测试

- Admin Shell 单测。
- Admin Pages 单测。
- `pnpm package:build`。
- `pnpm package:check`。
- 原 Mango Admin baseline E2E。
- full preset E2E。

## 5. 完成标准

- 业务 starter 不再直接依赖 `@mango/admin-shell`。
- `@mango/admin` 可被发布、安装、类型检查和构建。
- full preset 截图经脚本断言和人工截图识别，确认主框架布局、颜色、菜单、顶栏、用户区、设置和 TagsView 符合基准。
- 未完成的 custom 自动依赖解析必须在台账中标为后续 Sprint，不得声明已完成。
