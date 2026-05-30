# Mango Admin Runtime 产品化 Sprint 3 交付台账

## 1. 交付台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| S3-001 | 升级计划 Sprint 3 | capability manifest 2.0 支持依赖和冲突声明 | 保留 `requires`/`optional`，新增兼容字段 `conflicts` | `mango-ui/packages/admin-pages/src/core.ts` | `pnpm -F @mango/admin-pages test`、`pnpm package:check` | DONE | `pnpm -F @mango/admin-pages test` 通过；`pnpm package:check` 通过 |
| S3-002 | 用户要求 | custom preset 自动补齐 Mango 基础依赖 | 从默认 Mango catalog 自动补齐 `requires` 强依赖，`optional` 只报告 | `@mango/admin-pages/core` dependency resolver、`@mango/admin` preset 集成 | `pnpm -F @mango/admin test` | DONE | `pnpm -F @mango/admin test` 通过；`mango-docs/evidence/2026-05-30-sprint-3/custom-preset/resolution-report.json` |
| S3-003 | 用户要求 | 缺失、冲突、循环依赖不能假成功 | resolver 失败关闭并输出 diagnostics/report | resolver 单测、preset 单测 | `pnpm -F @mango/admin-pages test`、`pnpm -F @mango/admin test` | DONE | 两组单测均通过，覆盖 missing/conflict/cycle |
| S3-004 | 用户要求 | full preset 继续复用完整 Mango Admin，不回归 | full preset 仍使用完整默认能力集，增加解析报告但不改变用户体验 | `@mango/admin` preset | `pnpm admin:full-preset-e2e` | DONE | `mango-docs/evidence/2026-05-30-sprint-3/full-preset/summary.md`、`layout-report.json`、3 张截图 |
| S3-005 | 用户要求 | custom preset 必须通过真实 E2E 和截图识别验收 | 新增 custom preset E2E，保存截图、layout-report、resolution-report | `mango-ui/scripts/admin-custom-preset-e2e.mjs` | `pnpm admin:custom-preset-e2e` | DONE | `mango-docs/evidence/2026-05-30-sprint-3/custom-preset/summary.md`、`layout-report.json`、`resolution-report.json`、3 张截图 |
| S3-006 | PMO / 用户要求 | 每阶段必须回归测试和新特性测试 | 执行包契约、构建、单测、full/custom E2E 和截图识别报告 | 验证命令和证据报告 | 命令全部通过或记录阻塞 | DONE | `pnpm package:check`、`pnpm package:build`、`pnpm -F @mango/admin-pages test`、`pnpm -F @mango/admin test`、full/custom E2E 均通过；截图识别见 `mango-docs/evidence/2026-05-30-sprint-3/visual-review.md` |
| S3-007 | PMO 规范 | Sprint 3 必须有设计说明和交付台账 | 使用 PMO 台账字段记录原子项、验收和证据 | 本文件和 Sprint 3 设计文件 | `delivery-contract-check --mode verify` | DONE | `node mango-pmo/tools/delivery-contract-check.mjs --design mango-docs/plans/2026-05-30-capability-manifest-dependency-sprint-3.md --ledger mango-docs/plans/2026-05-30-capability-manifest-dependency-sprint-3-ledger.md --mode verify` |

## 2. 未完成不得声明完成项

- 未通过 full/custom E2E 截图验收前，不得声明 Sprint 3 完成。
- 未通过 delivery contract check 前，不得声明交付闭环。
- custom preset 只能使用真实 `@mango/admin`/Admin Shell，不允许仿造 shell。
