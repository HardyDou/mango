# 前端发布物 Tarball 消费 Sprint G

## 1. 背景

Sprint F 已证明生成业务项目可以通过本地 `link:` 覆盖消费 Mango 前端物料，但 `link:` 仍依赖源码工作区，不能代表真实发布物消费。Sprint G 聚焦将验证口径提升到 `pnpm pack` 产出的 tarball。

## 2. 目标

验证业务项目可以安装 Mango 前端包 tarball，完成模板检查、安装、类型检查、生产构建和浏览器冒烟。同时收紧包发布契约，避免 tarball 携带源码、测试和构建配置。

## 3. 范围

- 为可发布的 `@mango/*` 前端包补齐 `files: ["dist"]`。
- 将 Vue、Element Plus、Pinia、Vue I18n peer dependency 从精确版本调整为兼容范围。
- 扩展 `pnpm package:check`，检查发布包只发布 `dist`。
- 使用 `pnpm pack` 生成 15 个 Mango tarball。
- 使用干净生成项目通过 tarball overrides 安装、类型检查、生产构建和浏览器冒烟。

## 4. 不做什么

- 不发布到真实 npm/Nexus registry。
- 不补后端可运行 app、数据库初始化或真实登录权限链路。
- 不处理当前大 chunk 体积优化。

## 5. 设计说明

### 5.1 影响模块

- `mango-ui/packages/*/package.json`
- `mango-ui/scripts/check-package-contracts.mjs`
- `mango-docs/plans`

### 5.2 接口变化

无 HTTP API 变化。npm 包发布契约变化：

- `files` 固定为 `["dist"]`。
- peer dependency 兼容范围放宽为 Vue `^3.5.0`、Element Plus `^2.5.0`、Pinia `^2.0.0`、Vue I18n `^9.2.0`。

### 5.3 数据变化

无数据库变化。

### 5.4 测试范围

- 新特性测试：`pnpm pack`、tarball 内容扫描、外部生成项目 tarball 安装、`pnpm typecheck`、`pnpm build`、浏览器冒烟。
- 回归测试：`pnpm package:check`、starter 模板检查、CLI 自检、Admin Pages/Admin Shell 单测和核心包构建。
- 例外测试：真实 registry 安装、后端测试和真实登录权限链路仍沿用 Sprint F 的例外结论。

## 6. 完成标准

- tarball 内容检查不包含 `src`、测试文件、`tsconfig` 或 `vite.config`。
- tarball 外部项目安装无 Mango peer dependency 警告。
- tarball 外部项目 `typecheck`、`build`、浏览器冒烟通过。
- 交付台账检查通过。

## 7. 风险与限制

- 当前只证明 tarball 发布物可被消费，未证明 Nexus/npm registry 发布和下载链路。
- 当前前端可启动不等于真实后端、真实登录、权限菜单、租户和 CRUD 链路可用。
- 生产构建仍有第三方 PURE 注释和 chunk 体积警告。
