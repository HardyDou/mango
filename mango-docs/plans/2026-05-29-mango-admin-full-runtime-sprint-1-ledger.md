# Mango Admin Runtime 产品化 Sprint 1 交付台账

## 1. 交付台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| S1-001 | 升级计划 Sprint 1 | 新增完整 Admin Runtime 包 `@mango/admin` | 新建 `mango-ui/packages/admin`，作为业务项目唯一 Admin 基座入口 | `mango-ui/packages/admin/package.json`、`src/index.ts` | `pnpm package:build`、`pnpm package:check` | DONE | `pnpm admin:full-preset-e2e -- --backend-url http://127.0.0.1:18492 --frontend-port 7788 --evidence-dir ../mango-docs/evidence/2026-05-29-admin-full-preset-visual-pass`；`pnpm package:check` |
| S1-002 | 升级计划 Sprint 1 | `createMangoAdmin` 支持 full/standard/minimal/custom preset | 由 `@mango/admin` 解析 preset，委托 `@mango/admin-shell` 启动完整主框架 | `mango-ui/packages/admin/src/index.ts`、`src/presets.ts` | `pnpm -F @mango/admin test` | DONE | `@mango/admin` Vitest 5 tests passed |
| S1-003 | 用户要求 | full preset 默认就是完整 Mango Admin，不是仿写主框架 | full preset 默认注册 Mango 内置能力，复用 `admin-shell` 的布局、顶栏、设置、TagsView、菜单和运行时 | `@mango/admin` full preset、full preset E2E | full preset 截图与基准人工识别和脚本布局检查 | DONE | `mango-docs/evidence/2026-05-29-admin-full-preset-visual-pass/`；`visual-review.md`；`layout-report.json` |
| S1-004 | 升级计划 Sprint 1 | 业务 starter 入口改为 `createMangoAdmin` | starter/template 依赖 `@mango/admin`，不直接导入底层 shell | `mango-business-starter/frontend/apps/{{projectKebab}}-admin/src/main.ts`、create-mango-app 模板同名文件 | `node scripts/check-template.mjs`，模板脚本检查 | DONE | `mango-business-starter`: Template check passed, 85 required files, 43 contract checks；create-mango-app 模板同样通过 |
| S1-005 | 用户要求 | 禁止业务项目继续直接拼 `@mango/admin-shell` | 扩展 starter 检查，扫描 import 和 package 依赖 | 两处 `scripts/check-template.mjs` | 注入检查正常通过，违规模式由规则覆盖 | DONE | 两处 `node scripts/check-template.mjs` 均通过 43 contract checks |
| S1-006 | 用户要求 | 每阶段必须做回归测试和新特性测试 | 本阶段执行 package build/check、admin-shell/admin-pages/admin/app-runtime 单测、baseline E2E、full preset E2E | 验证命令和报告 | 命令全部通过或明确记录阻塞 | DONE | Baseline evidence: `mango-docs/evidence/2026-05-29-admin-baseline-rerun/`；full preset evidence: `mango-docs/evidence/2026-05-29-admin-full-preset-visual-pass/`；包级测试全部通过 |
| S1-007 | PMO 规范 | Sprint 1 必须有设计说明和交付台账 | 使用 PMO 台账字段记录来源、要求、决策、交付物、验收和状态 | 本文件和 Sprint 1 设计文件 | `delivery-contract-check --mode verify` | DONE | `node mango-pmo/tools/delivery-contract-check.mjs --design mango-docs/plans/2026-05-29-mango-admin-full-runtime-sprint-1.md --ledger mango-docs/plans/2026-05-29-mango-admin-full-runtime-sprint-1-ledger.md --mode verify` |

## 2. 未完成不得声明完成项

- custom preset 自动补齐依赖、冲突解析和循环依赖检查未进入 Sprint 1 完成范围。
- API SDK / Admin UI 分包未进入 Sprint 1 完成范围。
- full preset 必须通过截图识别和数据/功能断言后才能把 S1-003 标为 DONE。
