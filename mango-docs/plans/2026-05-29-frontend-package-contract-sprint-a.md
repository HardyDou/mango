# Mango 前端发布物契约收口 Sprint A

## 1. 背景

业务项目消费 Mango 前端 npm 包时，`@mango/admin-shell`、`@mango/admin-pages` 等包仍以 `src` 作为入口，导致外部项目类型检查和构建会穿透 Mango monorepo 源码。本 Sprint 收口发布契约基础设施，让对外包入口指向构建产物。

## 2. 目标

所有 Mango 对外前端包声明稳定的 `dist` 入口、类型入口和子路径导出，并提供统一构建和契约检查命令。

## 3. 范围

- 新增前端包统一构建脚本。
- 新增前端包发布契约检查脚本。
- 更新对外包 `package.json`，将 `main`、`module`、`types`、`exports` 指向 `dist`。
- 保留 `create-mango-app` 的 CLI 发布形态，不改为库构建入口。
- 保留业务 starter 模板包的源码入口，后续 Sprint D 处理生成项目模板。

## 4. 不做什么

- 不改业务页面行为。
- 不重构 Admin Shell 扩展点。
- 不把内置能力改造成微前端远程入口。
- 不发布 npm 包。
- 不修复所有历史类型错误；本 Sprint 先建立发布契约和检查门禁。

## 5. 设计说明

### 5.1 影响模块

- `mango-ui/package.json`
- `mango-ui/scripts/build-package.mjs`
- `mango-ui/scripts/check-package-contracts.mjs`
- `mango-ui/packages/*/package.json`
- `mango-docs/plans`

### 5.2 接口变化

前端 npm 包发布入口变化：

- 包根入口从源码入口切换到 `dist/index.js` 和 `dist/index.d.ts`。
- 公开子路径从源码路径切换到对应 `dist/**/*.js` 和 `dist/**/*.d.ts`。

### 5.3 数据变化

无数据库变化。

### 5.4 菜单/页面/权限变化

无菜单、页面、权限数据变化。

### 5.5 测试范围

- 发布契约扫描。
- 对外消费方 smoke：在 Mango monorepo 外部创建临时 Vite/Vue 项目，通过 `link:` 依赖消费 Mango 发布物料，执行类型检查和生产构建。
- 15 个对外前端包构建回归。
- `git diff --check`。
- 交付台账检查。

## 6. 完成标准

- 对外包 `package.json` 不再把包根入口指向 `src`。
- 对外包声明 `types`。
- 包契约检查脚本可发现源码入口、`workspace:*` 和 app 私有路径依赖。
- 核心包至少覆盖 `@mango/admin-pages`、`@mango/app-runtime`、`@mango/admin-shell` 的构建验证。
- 外部消费方 smoke 必须能从 `dist` 入口消费 `@mango/admin-shell`、`@mango/admin-pages`、`@mango/app-runtime`、`@mango/common` 和主要能力包，并通过 `vue-tsc --noEmit` 与 `vite build`。
- 本 Sprint 完成后必须执行新增能力测试和回归测试，通过后才能进入 Sprint B。

## 7. 风险与限制

- 如果历史源码类型错误阻断声明生成，必须记录为阻断，不能回退到源码入口。
- 本地 smoke 使用 `link:` 依赖验证包契约，和真实 npm registry 安装仍有差异；后续发布前还需要执行 pack/install 级验收。
- 外部 smoke 的 Vite 构建存在大 chunk 警告，不阻断 Sprint A 包契约验收，后续在 Shell API 与路由拆包阶段治理。
