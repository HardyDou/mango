# Mango Admin Runtime 产品化 Sprint 6：create-mango-app 和 starter 完整改造

## 1. 背景

Sprint 0-5 已完成完整 Admin 基座、API/Admin 分包、能力依赖解析、菜单集成和基础 local/micro/mixed 运行协议。当前缺口是初始化器生成物仍没有完全对齐最终业务开发模型：业务 Admin 包命名不明确，CLI 缺少 preset 参数，生成项目验收还需要把 preset、features、frontend-mode 和业务 API/Admin 包结构一起固化。

## 2. 目标

初始化出的项目默认就是完整 Mango，可配置裁剪，可扩展业务模块，并且生成物只依赖 Mango 发布物和业务包公开入口。

## 3. 范围

- `create-mango-app` 增加 `--preset full|standard|minimal|custom`，默认 `full`。
- 生成项目记录 `preset`、`features`、`frontendMode` 和 runtime config 路径。
- 业务前端生成 `@<project>/<module>-api` 和 `@<project>/<module>-admin`。
- `*-admin` 表示绑定 Mango Admin Shell 的后台能力包，负责页面注册、菜单入口、权限和后台页面。
- admin app 只通过 `@mango/admin` 启动，不直接依赖或导入 `@mango/admin-shell`。
- `--features` 继续选择 Mango 内置能力包，配合 preset 参与 full/custom 生成验收。
- 模板检查和 CLI 检查覆盖业务 `*-api` / `*-admin` 命名、preset、features、frontend-mode、菜单和 runtime config。
- 生成项目执行 install、typecheck、build、后端测试、dev-start 和浏览器 E2E 截图验收。

## 4. 不做范围

- 不实现通用 `*-ui` 组件层；本 Sprint 只为以后保留命名语义。
- 不做灰度、A/B、远程配置中心、动态 registry、缓存治理、发布平台、监控告警和性能专项。
- 不新增生产微前端发布系统。
- 不把生成项目代码放回 Mango 主仓长期作为业务样例维护。

## 5. 设计决策

### 5.1 CLI 参数

`--preset` 控制 Admin 基座模式：

- `full`：默认完整 Mango Admin。
- `standard`：基础管理能力集合。
- `minimal`：只启用显式业务和能力输入。
- `custom`：按 `--features` 选择能力，并由 `@mango/admin` 自动补齐必需依赖。

### 5.2 业务前端包

生成物使用：

- `@<project>/<module>-api`：业务 API SDK。
- `@<project>/<module>-admin`：业务 Admin Shell 集成包。

`*-admin` 可以依赖 `@mango/admin-pages`、Element Plus、业务 `*-api`；不得被解释为通用 UI 包。非 Admin UI 只依赖 `*-api`，未来需要通用组件时再新增独立 `*-ui`。

### 5.3 验收策略

本 Sprint 不以“文件生成成功”作为完成依据。必须验证生成项目的包结构、依赖、构建、启动、登录、菜单、业务页面和截图布局。

## 6. 验证方式

- `node mango-pmo/tools/delivery-contract-check.mjs --design mango-docs/plans/2026-05-30-create-mango-app-starter-sprint-6.md --ledger mango-docs/plans/2026-05-30-create-mango-app-starter-sprint-6-ledger.md --mode verify`
- `node --check mango-ui/packages/create-mango-app/src/index.mjs`
- `node --check mango-ui/packages/create-mango-app/scripts/check-cli.mjs`
- `node mango-business-starter/scripts/check-template.mjs`
- `node mango-ui/packages/create-mango-app/scripts/check-cli.mjs`
- `cd mango-ui && pnpm package:check`
- `cd mango-ui && pnpm package:build`
- `cd mango-ui && pnpm business-starter:dev-start-e2e -- --evidence-dir ../mango-docs/evidence/2026-05-30-sprint-6/dev-start`
- `cd mango-ui && pnpm admin:full-preset-e2e -- --backend-url <url> --frontend-port <port> --evidence-dir ../mango-docs/evidence/2026-05-30-sprint-6/full-preset --skip-package-build`
- `cd mango-ui && pnpm admin:custom-preset-e2e -- --backend-url <url> --frontend-port <port> --evidence-dir ../mango-docs/evidence/2026-05-30-sprint-6/custom-preset --skip-package-build`
- `cd mango-ui && pnpm frontend-mode:matrix-e2e -- --frontend-port <port> --evidence-dir ../mango-docs/evidence/2026-05-30-sprint-6/mode-matrix`

## 7. 完成标准

- CLI 支持 preset、features、frontend-mode，并把选择写入生成项目配置。
- 生成项目包含业务 `*-api` 和 `*-admin` 包，admin app 依赖业务 `*-admin`。
- 生成项目不包含 Mango 仓内私有源码路径，不直接拼装 `@mango/admin-shell`。
- 生成项目可安装、类型检查、构建、后端测试、dev-start。
- 浏览器 E2E 截图证明完整 Mango Shell、菜单、业务页面、布局、颜色、数据和功能符合预期。
- Sprint 6 台账全部 `DONE` 或明确 `EXCEPTION`。

## 8. 风险与限制

- 生成项目依赖本地或私有 npm 仓库中的 Mango 发布物，E2E 需要先构建或 staging 包。
- 业务页面真实数据依赖生成后端、数据库迁移和登录权限链路，dev-start E2E 必须联动后端验证。
- `*-ui` 是后续独立能力，不得在本 Sprint 临时做半成品。
