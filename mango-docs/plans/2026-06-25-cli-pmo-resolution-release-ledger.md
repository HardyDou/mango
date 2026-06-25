# CLI PMO Resolution Release 交付契约

## 1. 目标

修复 `@mango/cli@1.0.35` 在 pnpm 业务项目中无法消费 `@mango/pmo` baseline、回退到 `@mango/cli-template` baseline 的问题，并发布修复版本。

## 2. 范围

- 修改 `@mango/cli` PMO baseline 解析逻辑。
- 补充 CLI 回归测试，覆盖发布后 pnpm 安装布局。
- 升级 `@mango/cli` 到 `1.0.36`。
- 更新 CLI 包日志、README 和平台级发布日志。
- 提交 PR、合并 `main` 后发布 npm 包。

## 3. 不做什么

- 不修改 `@mango/pmo@1.0.0` baseline 内容。
- 不修改业务项目代码或业务 baseline 文件。
- 不发布后端 Maven 物料。

## 4. 设计输入

- 业务项目复现：`@mango/pmo@1.0.0` 已安装，但 `mango pmo check` 报 `Baseline: @mango/cli-template@1.0.35`。
- CLI 源码：`loadPmoPackageBaseline()` 使用固定目录候选，未覆盖 pnpm 发布安装布局。
- 用户要求：提交 PR、合并 main、发布。

## 5. 设计说明

### 5.1 影响模块

- `mango-ui/packages/mango-cli/src/index.mjs`
- `mango-ui/packages/mango-cli/scripts/check-cli.mjs`
- `mango-ui/packages/mango-cli/package.json`
- `mango-ui/packages/mango-cli/README.md`
- `mango-ui/packages/mango-cli/CHANGELOG.md`
- `CHANGELOG.md`

### 5.2 接口变化

无 CLI 命令参数变化。`mango pmo status/check/sync/upgrade` 的 baseline 来源修正为优先通过 Node 包解析读取 `@mango/pmo/baseline.json`。

### 5.3 数据变化

无数据库变化。npm 包版本从 `@mango/cli@1.0.35` 升级到 `@mango/cli@1.0.36`。

### 5.4 菜单/页面/权限变化

无菜单、页面、权限变化。

### 5.5 测试范围

- CLI 全量回归。
- `@mango/pmo` 包检查。
- admin 样式治理检查。
- 业务项目现场 `pmo status` 验证 baseline 来源。
- 发布说明检查。

## 6. 风险与限制

- 现有已用 `@mango/cli@1.0.35` 同步过的业务项目，升级 CLI 后可能看到 baseline 文件与 `@mango/pmo@1.0.0` 有差异，需要重新执行 `mango pmo sync --project-dir .` 对齐。
- 本次只发布 CLI；`@mango/pmo@1.0.0` 不变。

## 7. 交付台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| TASK-001 | 用户要求 | 修复 pnpm 环境 CLI 找不到 `@mango/pmo` baseline 的问题 | 使用 `createRequire(import.meta.url).resolve('@mango/pmo/baseline.json')` 优先按 Node 包解析读取发布包 baseline | `mango-ui/packages/mango-cli/src/index.mjs` | `pnpm --filter @mango/cli test`；业务项目 `pmo status` 输出 `Baseline: @mango/pmo@1.0.0` | DONE | 本文件第 8 节 |
| TASK-002 | 用户要求 | 补回归，防止再次回退 CLI 模板 baseline | 在 CLI 检查脚本中模拟 pnpm 发布安装布局并断言不出现 `@mango/cli-template` | `mango-ui/packages/mango-cli/scripts/check-cli.mjs` | `pnpm --filter @mango/cli test` | DONE | 本文件第 8 节 |
| TASK-003 | 发布门禁 | 升级并发布新的 CLI 版本 | `@mango/cli` 升级为 `1.0.36`，补 CLI README/CHANGELOG 和根 CHANGELOG | `package.json`、`README.md`、`CHANGELOG.md` | `node scripts/check-release-notes.mjs --package=@mango/cli --version=1.0.36`；发布后 registry 验证 | DONE | 本文件第 8 节 |
| TASK-004 | PMO 门禁 | 提交 PR、合并 main、发布前完成验证 | 合并最新 `origin/main` 后执行相关检查 | PR、GitHub Release、npm package | `pnpm --filter @mango/cli test`；`pnpm --filter @mango/pmo check`；admin 样式检查 | DONE | 本文件第 8 节 |

## 8. 验收证据记录

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| TASK-001 | CLI | PMO baseline 解析 | baohan-system-upgrade-mango-latest-20260625 | 修复后的 CLI 输出 `Baseline: @mango/pmo@1.0.0`，不再输出 `@mango/cli-template@1.0.35` | 不涉及 UI | 不涉及 network | 终端输出 | DONE |
| TASK-002 | CLI test | pnpm 发布布局回归 | fake `.pnpm/@mango+cli@1.0.36/node_modules/@mango/{cli,pmo}` | `pnpm --filter @mango/cli test` 通过 | 不涉及 UI | 不涉及 network | 终端输出 | DONE |
| TASK-003 | Release notes | 发布说明 | `@mango/cli@1.0.36` | 根 `CHANGELOG.md` 最新段包含 Published Packages、Upgrade Notes、Verification | 不涉及 UI | 不涉及 network | 终端输出 | DONE |
| TASK-004 | Release validation | 发布前验证 | 当前任务 worktree | CLI、PMO、admin 样式治理检查通过 | 不涉及 UI | 不涉及 network | 终端输出 | DONE |
