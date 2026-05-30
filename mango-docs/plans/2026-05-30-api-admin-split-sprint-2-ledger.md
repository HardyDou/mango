# Mango Admin Runtime 产品化 Sprint 2 交付台账

## 1. 交付台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| S2-001 | 升级计划 Sprint 2 | 内置能力拆成 API SDK 包和 Admin UI 包 | 为 9 个内置能力新增 `@mango/*-api` 与 `@mango/*-admin` 包 | `mango-ui/packages/*-api`、`mango-ui/packages/*-admin` | `pnpm package:check`、`pnpm package:build` | DONE | `pnpm package:check` 通过；`pnpm admin:full-preset-e2e -- --backend-url http://127.0.0.1:18492 --frontend-port 7788 --evidence-dir ../mango-docs/evidence/2026-05-30-sprint-2/full-preset` 内置执行 `pnpm package:build` 通过 |
| S2-002 | 用户要求 | API SDK 可被非管理系统 UI 使用 | `*-api` 不包含 views、capability、Admin Shell 和 Element Plus 依赖 | `@mango/*-api` 包 | `pnpm api-sdk:consumer-e2e` | DONE | `mango-docs/evidence/2026-05-30-sprint-2/api-sdk-consumer/summary.md`、`layout-report.json`、`api-sdk-consumer-1280x820.png` |
| S2-003 | 用户要求 | Admin 包一依赖就自动集成页面、菜单、权限和局部样式 | `*-admin` 导出 capability，并依赖对应 `*-api` | `@mango/*-admin/capability`、`@mango/admin-pages` 默认能力 | `pnpm -F @mango/admin-pages test`、full preset E2E | DONE | `pnpm -F @mango/admin-pages test` 通过；`mango-docs/evidence/2026-05-30-sprint-2/full-preset/summary.md`、`layout-report.json`、`home-1440x960.png`、`user-dropdown-1440x960.png`、`settings-drawer-1440x960.png` |
| S2-004 | 升级计划 Sprint 2 | 旧混合包保留兼容出口但不作为新开发入口 | 旧 `@mango/*` 包继续导出原能力，默认集成改为新 Admin 包 | 旧包 package 和构建 | `pnpm package:build`、兼容 import 构建检查 | DONE | `pnpm admin:full-preset-e2e` 内置 `pnpm package:build` 全包构建通过；`pnpm -F @mango/admin test` 通过 |
| S2-005 | PMO / 用户要求 | 每阶段必须回归测试和新特性测试 | 执行包契约、拆包构建、API SDK 独立消费、full preset E2E 和模板检查 | 验证命令和证据报告 | 命令全部通过或记录阻塞 | DONE | `pnpm package:check`、`pnpm -F @mango/admin-pages test`、`pnpm -F @mango/admin test`、`pnpm api-sdk:consumer-e2e -- --skip-package-build --evidence-dir ../mango-docs/evidence/2026-05-30-sprint-2/api-sdk-consumer`、`pnpm admin:full-preset-e2e -- --backend-url http://127.0.0.1:18492 --frontend-port 7788 --evidence-dir ../mango-docs/evidence/2026-05-30-sprint-2/full-preset`、两份 template check 均通过；截图识别记录见 `mango-docs/evidence/2026-05-30-sprint-2/visual-review.md` |
| S2-006 | PMO 规范 | Sprint 2 必须有设计说明和交付台账 | 使用 PMO 台账字段记录原子项、验收和证据 | 本文件和 Sprint 2 设计文件 | `delivery-contract-check --mode verify` | DONE | 本文件；`node mango-pmo/tools/delivery-contract-check.mjs --design mango-docs/plans/2026-05-30-api-admin-split-sprint-2.md --ledger mango-docs/plans/2026-05-30-api-admin-split-sprint-2-ledger.md --mode verify` |

## 2. 未完成不得声明完成项

- custom preset 自动补齐依赖、冲突解析和循环依赖检查不属于 Sprint 2 完成范围。
- 旧混合包删除和迁移期限不属于 Sprint 2 完成范围。
- 未通过 API SDK 独立消费 E2E 前，不得声明 API SDK 可用于非管理 UI。
