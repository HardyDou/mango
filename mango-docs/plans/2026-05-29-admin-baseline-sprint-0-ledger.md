# Mango Admin Runtime 产品化 Sprint 0 交付台账

## 1. 交付台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| S0-001 | 升级计划 Sprint 0 | capability manifest 2.0 类型和检查器骨架必须提前落地 | 在 `admin-pages` 类型中增加依赖、菜单、权限、样式、运行时、后端能力和 E2E 字段 | `mango-ui/packages/admin-pages/src/core.ts` | `node --check scripts/check-package-contracts.mjs` 和 `pnpm package:check` | DONE | `mango-ui/packages/admin-pages/src/core.ts`；`pnpm package:check` 通过 |
| S0-002 | 升级计划 Sprint 0 | 现有能力包必须声明 v2 字段，避免继续按旧 manifest 扩散 | 为内置 capability 补充 `requires`、`backend`、`menus`、`permissions`、`styles`、`runtime`、`e2e` | `mango-ui/packages/*/src/capability.ts` | `pnpm package:check` | DONE | `mango-ui/packages/*/src/capability.ts`；15 个包契约检查通过 |
| S0-003 | 同行评审门禁 | 检查器必须能拒绝缺 v2 字段和菜单权限不一致 | 扩展 `check-package-contracts.mjs` 校验 manifest v2 字段、菜单和权限关系 | `mango-ui/scripts/check-package-contracts.mjs` | `node --check scripts/check-package-contracts.mjs`、`pnpm package:check` | DONE | `mango-ui/scripts/check-package-contracts.mjs`；语法检查和包契约检查通过 |
| S0-004 | 用户要求 | starter 禁止手写 Mango 内置菜单 | 在 starter 模板检查中扫描内置模块和内置菜单码 | `mango-business-starter/scripts/check-template.mjs`、create-mango-app 模板同名脚本 | `node scripts/check-template.mjs` | DONE | `mango-business-starter/scripts/check-template.mjs`；两个 starter 模板检查均通过 |
| S0-005 | 用户要求 | 必须建立原 Mango Admin 基准截图和布局报告 | 新增 `admin-baseline-e2e.mjs`，登录原 Mango Admin 并保存截图、布局、菜单、CSS 和接口报告 | `mango-ui/scripts/admin-baseline-e2e.mjs`、`mango-ui/package.json` | `pnpm admin:baseline-e2e -- --evidence-dir ../mango-docs/evidence/2026-05-29-admin-baseline` | DONE | `mango-docs/evidence/2026-05-29-admin-baseline/home-1440x960.png`、`user-dropdown-1440x960.png`、`settings-drawer-1440x960.png`、`layout-report.json` |
| S0-006 | PMO 规范 | Sprint 0 必须有设计说明和交付台账 | 建立 Sprint 0 设计说明和台账 | 本文件和 Sprint 0 设计文件 | `delivery-contract-check --mode verify` | DONE | `mango-docs/plans/2026-05-29-admin-baseline-sprint-0.md`；`mango-docs/plans/2026-05-29-admin-baseline-sprint-0-ledger.md` |
