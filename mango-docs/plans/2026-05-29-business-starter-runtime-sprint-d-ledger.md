# Mango Business Starter 可启动模板 Sprint D 台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| FSD-001 | Sprint D | starter 生成后具备前端 workspace | 增加根级 pnpm workspace、package scripts、tsconfig、registry 配置说明 | `mango-business-starter/package.json`、`pnpm-workspace.yaml`、`tsconfig.base.json`、`.npmrc` | `node mango-business-starter/scripts/check-template.mjs` | DONE | 模板自检通过：78 个必需文件、25 个契约检查 |
| FSD-002 | Sprint D | admin app 可通过 Mango发布物启动 | admin app 依赖 `@mango/admin-shell`、注册默认能力与业务页面，不复制 Mango app 源码 | `frontend/apps/{{projectKebab}}-admin/src/main.ts` | 生成项目 `pnpm typecheck`、`pnpm build` | DONE | `/tmp/mango-sprint-d-starter-smoke/guarantee-platform` 中 `pnpm typecheck`、`pnpm build` 通过 |
| FSD-003 | Sprint D | 支持单体、微前端、混合部署配置 | 增加 runtime config 与环境变量，默认本地模块，支持模块切到 micro | `runtimeConfig.ts`、`.env.example`、`public/mango-runtime-config.json` | 生成项目 `pnpm typecheck`、`pnpm build` | DONE | 生成项目 runtime config 类型检查通过，Vite 构建 3441 个模块 |
| FSD-004 | Sprint D | 模板自检覆盖新增可启动资产 | check-template 校验 workspace、脚本、runtime config、菜单 fallback 和无 `workspace:*` | `scripts/check-template.mjs` | `node mango-business-starter/scripts/check-template.mjs` | DONE | 模板自检通过：78 个必需文件、25 个契约检查 |
| FSD-005 | Sprint D | create-mango-app 内置模板同步 | 将根 starter 新资产同步到 CLI package 内置模板 | `mango-ui/packages/create-mango-app/templates/mango-business-starter` | `node mango-ui/packages/create-mango-app/scripts/check-cli.mjs` | DONE | CLI 自检通过，生成项目包含 workspace、runtime config、starter menus、Vite 类型文件 |
| FSD-006 | Sprint D | 新特性测试通过 | 临时生成业务项目并执行 install、typecheck、build | `/tmp/mango-sprint-d-starter-smoke` | `pnpm install && pnpm typecheck && pnpm build` | DONE | `pnpm install --ignore-scripts`、`pnpm typecheck`、`pnpm build` 通过 |
| FSD-007 | Sprint D | 回归测试通过 | 复跑 Sprint A/B/C 包契约、Admin Shell/Admin Pages 单测、核心构建 | `mango-ui` | `pnpm package:check`、相关测试与构建命令 | DONE | `pnpm package:check` 15 个包通过；`@mango/admin-pages` 1 个测试文件 4 个用例通过；`@mango/admin-shell` 2 个测试文件 6 个用例通过；核心包构建通过 |
| FSD-008 | PMO | 交付台账通过检查 | 本台账记录 Sprint D 原子交付项 | 本文件 | `node mango-pmo/tools/delivery-contract-check.mjs --design mango-docs/plans/2026-05-29-business-starter-runtime-sprint-d.md --ledger mango-docs/plans/2026-05-29-business-starter-runtime-sprint-d-ledger.md --mode verify` | DONE | Sprint D 台账 8 项均为 DONE，delivery contract verify 通过 |
