# 标准交付记录

> **结论：** `mango-ui` 全量静态门禁恢复通过；Prettier/stylelint 基线清零，TypeScript 诊断从 25 个 workspace / 787 条降到 21 个 / 426 条，`quality-baseline.json` 已按实际下降后的债务受控收敛。

## 1. 元数据

- 任务 ID：ISSUE-1214
- 交付模式：STANDARD
- 需求影响：L2 - main 的 Frontend Quality 全量静态门禁持续失败，阻塞前端交付验证，并涉及多处共享类型契约
- 方案风险：L2 - 修改 admin-shell 共享路由类型、Pinia 类型增强和多包视图类型，需要跨包回归和基线收敛
- 最终风险：L2
- 工作区决策：CREATE

## 2. 目标与范围

- 目标：修复 Mango Issue #1214，使 `pnpm -C mango-ui check:static` 通过，main push Frontend Quality 可恢复绿色。
- 成功条件：Prettier、stylelint、ESLint、typecheck 的 ratchet 全部通过；质量基线只能下降或保持。
- 处理范围：`mango-ui` 的格式、类型定义、未使用配置、前端静态质量基线和相关包测试。
- 不处理范围：不修改业务流程、页面交互、接口协议、后端代码；不处理与本 Issue 无关的既有 workflow 测试失败。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| SR-1 | 前端开发者 / CI | 在 `mango-ui` 执行 `pnpm check:static` | Prettier、stylelint、ESLint、typecheck 全部通过 | 任一 ratchet 新增即失败 | 命令退出码 0 |
| SR-2 | 前端构建 | 在 `mango-ui` 执行 `pnpm build` | 所有 workspace 正常构建 | 构建失败即阻断 | 命令退出码 0 |
| SR-3 | 共享类型消费者 | admin-shell、rbac、cms、file 的视图和测试使用共享类型 | 类型定义与真实字段一致，不再产生新增诊断 | 新增诊断被 ratchet 阻断 | typecheck PASS，且 current identities 是 baseline 子集 |
| SR-4 | main push CI | 合并后 main push Frontend Quality | `frontend-quality` 与 `frontend-e2e-p0` 通过 | 失败则不能声明 Issue 完成 | GitHub Actions run 为 success |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| D-1 | SR-1、SR-3 | 修正 Pinia `DefineStoreOptionsBase` 增强的类型参数名，使 `persist` 与 actions 推断恢复；保留原持久化选项 | `mango-ui/packages/admin-shell/src/types/pinia.d.ts` | 恢复 `_S`/`_Store` 参数名 |
| D-2 | SR-3 | 将 `ShellRouteMenu` 从 union 继承改为显式路由字段接口，保留 `sourceMenu` 和递归 children，统一 routesList 类型 | `menuHost.ts`、`routesList.ts`、相关布局和测试 | 恢复原接口和 `RouteRecordRaw[]` |
| D-3 | SR-3 | 修正 rbac `AppModuleRuntimeStrategy` 的本地字段定义和 `ApiResourceVO` 历史字段兼容；修正 cms/file 的类型与未使用配置 | `packages/rbac`、`packages/cms`、`packages/file` | 逐文件恢复 |
| D-4 | SR-1 | 使用 `update-static-baseline.mjs --write` 将质量基线收敛到实际下降后的指标和 identity 集合 | `mango-ui/quality-baseline.json` | 从 Git 恢复旧基线 |
| D-5 | SR-1 | 对 `packages/workflow/README.md` 执行 Prettier 格式化，清除最后一个格式差异 | `mango-ui/packages/workflow/README.md` | 恢复原文档 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| IMPL-1 | D-5 | 1 | `mango-ui/packages/workflow/README.md` | Prettier 通过 |
| IMPL-2 | D-1、D-2 | 2 | `mango-ui/packages/admin-shell/src/**` | admin-shell 类型与测试通过 |
| IMPL-3 | D-3 | 3 | `mango-ui/packages/rbac/**`、`mango-ui/packages/cms/**`、`mango-ui/packages/file/**`、`mango-ui/apps/**` | 新增 typecheck identity 清零 |
| IMPL-4 | D-4 | 4 | `mango-ui/quality-baseline.json` | `check:static` ratchet 通过 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| SR-1 | M09 全量静态门禁 | `pnpm -C mango-ui check:static` | PASS：eslint 159/754，prettier 0，stylelint 32，typecheck 21/426 | 命令输出 |
| SR-2 | M09 构建 | `pnpm -C mango-ui build` | PASS：全部 workspace 构建完成 | 命令输出 |
| SR-3 | M10 定向测试 | `pnpm -F @mango/admin-shell test`、`pnpm -F @mango/rbac test`、`pnpm -F @mango/cms test`、`pnpm -F @mango/file test` | PASS：100 + 13 + 6 + 58 tests | Vitest 输出 |
| SR-3 | 基线收敛 | `node scripts/quality/update-static-baseline.mjs --write --base-ref=origin/main` | PASS：指标下降，无新增 identity | `quality-baseline.json` |
| SR-4 | 合并后回读 | GitHub Actions main push Frontend Quality | 待合并后回读 | PR 合并记录 |

## 7. 例外与剩余风险

- `pnpm -C mango-ui test:unit` 中 `packages/workflow` 的 `startProcessIcons.spec.ts` 在 clean main 同样失败，属于既有失败，不由本次改动引入；未在本任务修复。
- `frontend-e2e-p0` 未在本地启动完整微前端环境执行，依赖合并后 main push Actions 回读。
- 当前 typecheck 仍有 21 个 workspace / 426 条历史诊断，已由 `quality-baseline.json` 锁定为下降后的债务，后续继续递减，不回填。
