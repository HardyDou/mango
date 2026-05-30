# Mango Admin Shell API 产品化 Sprint B 台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| FPB-001 | Sprint B | 固化 Shell options 公开配置面 | 在 `MangoAdminShellOptions` 中声明组件、主题、布局、菜单和 runtime 配置项 | `mango-ui/packages/admin-shell/src/config.ts` | `pnpm --filter @mango/admin-shell test` | DONE | Admin Shell 单测通过，2 个测试文件、6 个用例 passed |
| FPB-002 | Sprint B | 支持关键扩展点替换 | Shell、Layout、Login、Error、Profile 等入口通过 options.components 覆盖 | `mango-ui/packages/admin-shell/src/index.ts`; `mango-ui/packages/admin-shell/src/router.ts`; `mango-ui/packages/admin-shell/src/ShellView.vue` | `pnpm --filter @mango/admin-shell test` | DONE | Admin Shell 单测通过，覆盖组件扩展点替换 |
| FPB-003 | Sprint B | 运行时诊断稳定可见 | 提供运行时决策和 runtime config 诊断读取函数 | `mango-ui/packages/admin-shell/src/runtime/runtimeHost.ts` | `pnpm --filter @mango/admin-shell test` | DONE | Admin Shell 单测通过，覆盖 runtime decision 与 diagnostics getter |
| FPB-004 | Sprint B | 新特性测试通过 | 外部消费方通过 `createMangoAdminApp(options)` 使用扩展点并完成类型检查、构建 | `/tmp/mango-sprint-b-shell-smoke` | `pnpm typecheck && pnpm build` | DONE | 外部消费 smoke 通过，`pnpm typecheck` 与 `pnpm build` exit 0，生产构建完成 |
| FPB-005 | Sprint B | 回归测试通过 | 复跑 Sprint A 包契约检查和核心包构建 | `mango-ui/packages/*/dist` | `pnpm package:check` 和核心包 `pnpm package:build --filter ...` | DONE | `pnpm package:check`、Admin Shell 单测、`@mango/admin-shell`、`@mango/app-runtime`、`@mango/admin-pages`、`@mango/common` 核心包构建均通过 |
| FPB-006 | PMO | 交付台账通过检查 | 本台账记录 Sprint B 原子交付项 | `mango-docs/plans/2026-05-29-admin-shell-api-sprint-b-ledger.md` | `node mango-pmo/tools/delivery-contract-check.mjs --design mango-docs/plans/2026-05-29-admin-shell-api-sprint-b.md --ledger mango-docs/plans/2026-05-29-admin-shell-api-sprint-b-ledger.md --mode verify` | DONE | 交付契约检查通过，6 项 DONE、0 项 EXCEPTION |
