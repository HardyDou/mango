# create-mango-app 能力选择 Sprint E 台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| FSE-001 | Sprint E | CLI 支持能力选择 | 增加 `--features`，渲染能力依赖、默认能力注册和配置 | `create-mango-app/src/index.mjs`、starter 模板 | `node mango-ui/packages/create-mango-app/scripts/check-cli.mjs` | DONE | CLI 自测通过，验证 `base,system,workflow,file` 渲染到依赖、capability import 和 `mango.config.json` |
| FSE-002 | Sprint E | CLI 支持前端部署模式选择 | 增加 `--frontend-mode`，渲染 local/micro/mixed runtime config | `runtimeConfig.ts`、`mango-runtime-config.json`、`.env.example` | CLI 自测和生成项目 build | DONE | CLI 自测验证 `mixed` 渲染为 runtime profile `hybrid`，生成项目 typecheck/build 通过 |
| FSE-003 | Sprint E | 生成后验证提示闭环 | next steps 输出模板检查、install、typecheck、build | `create-mango-app/src/index.mjs` | CLI 自测检查 stdout | DONE | CLI 自测验证 stdout 包含 `pnpm typecheck` 和 selection summary |
| FSE-004 | Sprint E | 新特性测试通过 | 临时生成项目并执行 install、typecheck、build | `/tmp/mango-sprint-e-cli-smoke` | `pnpm install && pnpm typecheck && pnpm build` | DONE | `/tmp/mango-sprint-e-cli-smoke/guarantee-platform` 中 `pnpm install --ignore-scripts`、`pnpm typecheck`、`pnpm build` 通过 |
| FSE-005 | Sprint E | 回归测试通过 | 复跑 Sprint A-D 关键检查 | `mango-ui`、`mango-business-starter` | 模板自检、包契约、单测、核心构建 | DONE | `check-template`、`check-cli`、`pnpm package:check`、Admin Pages/Admin Shell 单测、核心包构建均通过 |
| FSE-006 | PMO | 交付台账通过检查 | 本台账记录 Sprint E 原子交付项 | 本文件 | `delivery-contract-check` | DONE | Sprint E 台账 6 项均为 DONE，delivery contract verify 通过 |
