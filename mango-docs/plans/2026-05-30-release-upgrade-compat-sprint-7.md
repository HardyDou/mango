# Mango Admin Runtime 产品化 Sprint 7：发布、升级和兼容策略

## 1. 背景

Sprint 6 已验证业务项目可以通过 `create-mango-app` 初始化完整 Mango Admin，并按 `*-api` / `*-admin` 组织业务前端包。当前缺口是发布物升级和旧项目迁移缺少可执行检查：旧 `@mango/*` 混合包仍存在，但必须被限定为过渡兼容层；新项目必须以 `@mango/admin`、`@mango/*-api` 和 `@mango/*-admin` 为推荐路径。

## 2. 目标

业务项目升级 Mango npm 版本时，可以验证旧兼容入口仍可构建，新推荐入口可安装、构建和启动，并且兼容层不破坏 API/Admin 分层边界。

## 3. 范围

- 包契约检查增加旧混合包兼容层约束。
- 验证 `@mango/*` 兼容包依赖对应 `@mango/*-api`，保留 `./capability` 过渡出口。
- 验证 `@mango/*-admin` 依赖对应 `@mango/*-api`，并在页面仍位于旧包期间包装旧 capability。
- 新增 generated project upgrade E2E：同一个生成项目先走旧兼容入口 install/typecheck/build，再切回新推荐入口 install/typecheck/build 和浏览器截图。
- 补充 release checklist、changelog 模板和 migration 模板，作为本 Sprint 发布/升级交付物。

## 4. 不做范围

- 不做灰度发布、A/B、租户流量策略。
- 不做远程配置中心、动态 registry 下发、CDN 缓存治理。
- 不做远程模块版本回滚、发布平台、监控告警平台。
- 不做生产微前端发布编排。
- 不在本 Sprint 抽离新的通用 `*-ui` 层。

## 5. 设计决策

### 5.1 版本策略

- Mango 内部前端包在同一发布批次保持相同版本。
- 包间依赖发布前必须是明确版本号，不允许 `workspace:*` 出现在发布物。
- `@mango/admin` 是 Admin 应用推荐入口，业务项目不得直接拼装 `@mango/admin-shell`。
- `@mango/*-api` 是非 Admin UI 和业务 Admin 包共同使用的 API SDK。
- `@mango/*-admin` 是绑定 Mango Admin Shell 的后台能力包。

### 5.2 兼容层策略

- 旧 `@mango/*` 混合包在过渡期保留，用于旧项目升级。
- 旧包 API 文件必须转发到对应 `@mango/*-api`。
- 旧包保留 `./capability`，保证旧项目仍可从 `@mango/*/capability` 构建。
- 新 `@mango/*-admin` 包依赖 `@mango/*-api`，并在页面仍位于旧包期间包装旧 capability，公开 `@mango/*-admin/capability`。
- `@mango/admin-pages` 默认能力只引用 `@mango/*-admin/capability`，不再引用旧 `@mango/*/capability`。

### 5.3 破坏性变更检查

- `pnpm package:check` 检查导出路径、发布文件、`workspace:*`、API/Admin 边界、兼容层 re-export 和 Admin wrapper。
- `generated-project:upgrade-e2e` 同时验证旧入口和新入口，避免只测新项目而遗漏升级项目。

### 5.4 Release Checklist

每次发布前至少执行：

```bash
cd mango-ui && pnpm package:check
cd mango-ui && pnpm package:build
cd mango-ui && pnpm generated-project:upgrade-e2e -- --evidence-dir ../mango-docs/evidence/<date>-sprint-7/generated-upgrade
cd mango-ui && pnpm package:registry-e2e -- --evidence-dir ../mango-docs/evidence/<date>-sprint-7/registry
```

发布记录必须包含：

- 发布版本。
- 发布包清单。
- 兼容层状态。
- 破坏性变更列表；没有则写无。
- 升级步骤。
- 回退方式；仅限 npm 版本回退，不包含远程模块回滚编排。
- E2E 截图和 layout report 路径。

### 5.5 Changelog 模板

```md
# Mango UI <version>

## 新增

## 变更

## 修复

## 兼容性

## 迁移说明

## 验证证据
```

### 5.6 Migration 模板

```md
# Mango UI <from> -> <to> 迁移说明

## 适用范围

## 升级前检查

## 依赖调整

## 代码调整

## 验证命令

## 已知限制
```

## 6. 验证方式

- `node --check mango-ui/scripts/check-package-contracts.mjs`
- `node --check mango-ui/scripts/generated-project-upgrade-e2e.mjs`
- `cd mango-ui && pnpm package:check`
- `cd mango-ui && pnpm package:build`
- `cd mango-ui && pnpm generated-project:upgrade-e2e -- --frontend-port <port> --evidence-dir ../mango-docs/evidence/2026-05-30-sprint-7/generated-upgrade --skip-package-build`
- `cd mango-ui && pnpm package:registry-e2e -- --registry-port <port> --frontend-port <port> --evidence-dir ../mango-docs/evidence/2026-05-30-sprint-7/registry`
- `node mango-pmo/tools/delivery-contract-check.mjs --design mango-docs/plans/2026-05-30-release-upgrade-compat-sprint-7.md --ledger mango-docs/plans/2026-05-30-release-upgrade-compat-sprint-7-ledger.md --mode verify`

## 7. 完成标准

- 兼容层契约检查通过。
- 生成项目旧兼容入口 install/typecheck/build 通过。
- 生成项目新推荐入口 install/typecheck/build 通过。
- 新推荐入口浏览器 E2E 截图和 layout report 证明完整 Mango Shell、菜单、业务页面、布局和错误态符合预期。
- registry consumption E2E 回归通过。
- Sprint 7 台账全部 `DONE` 或明确 `EXCEPTION`。

## 8. 风险与限制

- 本 Sprint 的“可回退”只表示业务项目可以回退 npm 版本和依赖声明，不包含远程模块版本回滚平台。
- 旧 `@mango/*` 混合包仍承载部分页面实现，属于受控过渡状态；后续抽离 `*-ui` 或页面迁移时必须另起 Sprint 设计。
- 浏览器升级 E2E 不启动真实后端，因此业务列表接口失败必须以明确错误态记录，不得声明真实业务数据联调完成。
