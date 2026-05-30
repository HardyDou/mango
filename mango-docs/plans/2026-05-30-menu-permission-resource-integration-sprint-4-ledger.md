# Mango Admin Runtime 产品化 Sprint 4 交付台账

## 1. 交付台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| S4-001 | 升级计划 Sprint 4 | 菜单合并策略：后端菜单优先、能力菜单补充、业务菜单追加 | Admin Shell 提供共享合并工具并输出报告 | `mango-ui/packages/admin-shell/src/runtime/menuMerge.ts` | `pnpm -F @mango/admin-shell test` | DONE | `pnpm -F @mango/admin-shell test` 通过；`mango-docs/evidence/2026-05-30-sprint-4/full-preset/menu-merge-report.json`、`mango-docs/evidence/2026-05-30-sprint-4/custom-preset/menu-merge-report.json` |
| S4-002 | 用户要求 | starter 不再手写 Mango 内置菜单 | `@mango/admin` 自动把 capability menus 注入 Shell，starter 仅声明业务菜单 | `@mango/admin` preset 集成、starter 入口检查 | `pnpm -F @mango/admin test`、template check | DONE | `pnpm -F @mango/admin test` 通过；`node mango-business-starter/scripts/check-template.mjs` 通过 |
| S4-003 | 升级计划 Sprint 4 | 菜单冲突和权限过滤规则可验证 | menu merge report 记录冲突、来源、过滤项 | Admin Shell 单测和 report | `pnpm -F @mango/admin-shell test` | DONE | `pnpm -F @mango/admin-shell test` 通过；full/custom `menu-merge-report.json` 已生成 |
| S4-004 | 升级计划 Sprint 4 | full preset 显示原 Mango 完整菜单 | 后端菜单仍为权威来源，capability 不覆盖后端菜单 | full preset E2E | `pnpm admin:full-preset-e2e` | DONE | `mango-docs/evidence/2026-05-30-sprint-4/full-preset/summary.md`、`layout-report.json`、`menu-sampling-report.json`、截图集 |
| S4-005 | 升级计划 Sprint 4 | custom preset 只显示所选能力菜单并支持业务菜单追加 | custom E2E 验证 selected capabilities、auto dependencies、业务菜单追加 | custom preset E2E | `pnpm admin:custom-preset-e2e` | DONE | `mango-docs/evidence/2026-05-30-sprint-4/custom-preset/summary.md`、`resolution-report.json`、`layout-report.json`、`menu-sampling-report.json`、截图集 |
| S4-006 | 前端测试规范 | 每阶段必须回归、新特性、截图识别、一级菜单和一级功能抽查 | E2E 输出 screenshots、layout-report、menu-sampling-report、visual-review | E2E 证据 | full/custom E2E 和人工截图识别 | DONE | `mango-docs/evidence/2026-05-30-sprint-4/visual-review.md`；full 抽查 4 个一级菜单 12 个子页面；custom 抽查 5 个一级菜单 13 个子页面；功能抽查用户菜单和设置抽屉 |
| S4-007 | PMO 规范 | Sprint 4 必须有设计说明和交付台账 | 使用 PMO 台账字段记录原子项、验收和证据 | 本文件和 Sprint 4 设计文件 | `delivery-contract-check --mode verify` | DONE | `node mango-pmo/tools/delivery-contract-check.mjs --design mango-docs/plans/2026-05-30-menu-permission-resource-integration-sprint-4.md --ledger mango-docs/plans/2026-05-30-menu-permission-resource-integration-sprint-4-ledger.md --mode verify` |

## 2. 未完成不得声明完成项

- 未验证真实后端菜单和权限链路前，不得声明菜单复用完成。
- 未完成一级菜单和子页面抽查前，不得声明阶段完成。
- 不允许把业务 starter 静态 Mango 内置菜单作为最终方案。
