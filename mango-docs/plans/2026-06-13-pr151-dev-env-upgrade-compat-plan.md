# PR151 开发环境升级兼容修复设计说明

## 目标

修复 PR #151 复审指出的开发环境升级兼容阻断点，保证业务项目和 Mango 源码仓各自使用正确的 CLI 启动策略，并补齐 Flyway 诊断、端口占用回归和 PMO PR 门禁。

## 范围

- 业务 full 模板 `scripts/dev-workspace.sh`。
- Mango 源码仓根 `scripts/dev-workspace.sh`。
- `@mango/cli` 开发工作区回归脚本。
- persistence starter Flyway 模块迁移异常上下文。
- PMO preflight PR 触发词和回归用例。
- 本 PR 交付台账。

## 不做范围

- 不发布新版本。
- 不合并 PR。
- 不修改业务项目代码。
- 不启动完整业务前后端验收。

## 设计决策

- 业务项目必须使用全局 `mango` CLI。模板脚本不再使用项目内 `node_modules`、`npx` 或 Mango 源码仓路径作为 fallback；缺少全局 CLI 时直接失败并提示固定版本安装命令。
- Mango 源码仓必须优先使用工程内 `mango-ui/packages/mango-cli/src/index.mjs`，再 fallback 全局 `mango`，避免旧全局 CLI 绕过仓内新能力。
- Flyway 模块迁移从数据源解析开始进入模块级异常包装，确保失败信息包含模块、location、historyTable、datasource、outOfOrder。
- occupied-port 回归同时覆盖 `status`、`doctor` 和 `start`。
- PMO PR 相关任务必须触发 `rules/01-delivery-contract.md`。

## 验收方式

- CLI 回归：`pnpm --filter @mango/cli test`。
- Flyway 回归：`mvn -pl mango-infra/mango-infra-persistence/mango-infra-persistence-starter -Dtest=PersistenceFlywayAutoConfigurationTest test`。
- PMO 回归：`node mango-pmo/tools/check-pmo-preflight.mjs`。
- 脚本语法：`bash -n scripts/dev-workspace.sh && bash -n mango-ui/packages/mango-cli/templates/full/scripts/dev-workspace.sh`。
- 交付台账：`node mango-pmo/tools/delivery-contract-check.mjs --design mango-docs/plans/2026-06-13-pr151-dev-env-upgrade-compat-plan.md --ledger mango-docs/plans/2026-06-13-pr151-dev-env-upgrade-compat-ledger.md --mode verify`。

## 风险与限制

- 本次只验证 PR 阻断点和相关自动化回归，不替代发布前完整前后端验收。
