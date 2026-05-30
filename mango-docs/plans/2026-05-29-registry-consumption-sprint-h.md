# 前端发布源 Registry 消费 Sprint H

## 1. 背景

Sprint G 已证明 `pnpm pack` 生成的 Mango 前端发布物可以被干净业务项目消费，但 tarball overrides 仍绕过了 registry 发布、下载和依赖解析链路。业务项目真正依赖 Mango 发布物时，需要通过 npm/Nexus registry 安装。

## 2. 目标

新增可重复的 registry 外部消费验收：在无 Nexus 凭证时，使用临时 Verdaccio registry 模拟真实发布源，自动发布 15 个 `@mango/*` 前端包，再生成干净业务项目并从 registry 安装、类型检查、生产构建和浏览器冒烟。

## 3. 范围

- 新增 `pnpm package:registry-e2e` 验证入口。
- 自动构建并校验 Mango 前端包发布契约。
- 自动启动临时 Verdaccio registry，并用 token 方式非交互发布包。
- 按包间依赖顺序发布 15 个 `@mango/*` 包。
- 生成干净业务项目，使用临时 registry 安装 Mango 发布物。
- 执行业务项目 `check-template`、`pnpm install`、`pnpm typecheck`、`pnpm build` 和浏览器冒烟。

## 4. 不做什么

- 不向内网 Nexus 或公网 npm 发布真实版本。
- 不新增后端 app、根 Maven `pom.xml`、数据库初始化或真实登录权限链路。
- 不处理当前构建 chunk 体积和第三方 PURE 注释警告。

## 5. 设计说明

### 5.1 影响模块

- `mango-ui/scripts/registry-consumption-e2e.mjs`
- `mango-ui/package.json`
- `mango-docs/plans`

### 5.2 接口变化

无 HTTP API 变化。新增前端验证脚本命令：

```bash
pnpm package:registry-e2e
```

### 5.3 数据变化

无数据库变化。脚本只在 `/tmp` 和可选 `--evidence-dir` 中写入临时 registry、临时 npm 配置、生成项目和验证证据。

### 5.4 测试范围

- 新特性测试：临时 registry 启动、token 创建、15 个包发布、registry view 校验、生成项目 registry 安装、类型检查、构建、浏览器冒烟。
- 回归测试：starter 模板检查、CLI 自检、包契约检查、Admin Pages/Admin Shell 单测、核心包构建。
- 例外测试：真实 Nexus 发布安装、后端 Maven 测试、真实登录/权限/租户/CRUD 链路仍不在本 Sprint 证明。

## 6. 完成标准

- `pnpm package:registry-e2e` 通过。
- 安装输出无 Mango `unmet peer` 警告。
- 浏览器冒烟验证首页 `#/home` 和 mixed 微前端缺失诊断。
- 回归测试通过。
- 交付台账检查通过。

## 7. 验证结果

已通过 `pnpm package:registry-e2e -- --evidence-dir /tmp/mango-sprint-h-evidence` 完成 Sprint H 验证。该命令完成 Mango 前端包构建、包契约检查、临时 Verdaccio 发布、registry metadata 校验、外部 mixed 业务项目生成、registry 安装、模板检查、类型检查、生产构建和 Playwright 浏览器冒烟。

本次发布源消费验证共发布 15 个 `@mango/*` 包：`@mango/api-schema`、`@mango/app-runtime`、`@mango/common`、`@mango/auth`、`@mango/calendar`、`@mango/file`、`@mango/numgen`、`@mango/rbac`、`@mango/system`、`@mango/notice`、`@mango/template`、`@mango/workflow`、`@mango/workflow-business-example`、`@mango/admin-pages`、`@mango/admin-shell`。

浏览器冒烟覆盖 `#/home` 首屏、generated business menu、mixed 模式 `MICRO_ROUTE` 渲染，以及微前端 remote entry 缺失时的诊断兜底。验证证据保存在 `/tmp/mango-sprint-h-evidence/summary.md`、`/tmp/mango-sprint-h-evidence/install.out`、`/tmp/mango-sprint-h-evidence/frontend-smoke.png`。

## 8. 风险与限制

- 临时 Verdaccio 能证明 registry 协议级发布、下载和依赖解析，但不能替代内网 Nexus 权限、仓库组同步、网络策略和版本治理验收。
- 当前生成项目仍不能执行后端真实 app 启动、`mvn test` 和真实登录权限链路。
- `pnpm package:registry-e2e` 会启动本地端口，默认使用 registry `4877`、前端 `5198`；端口被占用时应换端口参数重跑。
- 当前构建仍存在第三方 PURE 注释提示和大 chunk 警告，未作为 Sprint H 阻断项。
