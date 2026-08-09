# Mango PMO Changelog

## 1.3.13 - 2026-08-09

### Fixed

- 撤回 PMO 1.3.10 至 1.3.12 引入且验证失败的治理升级，恢复 PR #737 合并前的开发、文档和交付语义。
- 移除 canonical `business-module` code baseline、L0-L5 精简文档、中文批量交付选择器、Issue 强制证据闭环和 worktree 四阶段完整性门禁。
- 恢复历史生命周期文档的完整合同复验；路径、SHA-256 和历史版本基线不再跳过当前合同结构与审批检查。

### Changed

- 生命周期合同前移到 PMO `1.3.13`，schema revision 保持 `1`；PMO `1.3.6` 至 `1.3.12` 文档仅可通过既有路径、SHA-256 和版本基线进入兼容检查。
- 保留 1.3.10 至 1.3.12、对应 Tag 和 GitHub Release 的不可变发布历史，由 1.3.13 明确替代，不删除或覆盖旧制品。

### Upgrade Notes

1. 安装 `@mango/cli@1.0.103` 后执行 `mango pmo upgrade --project-dir . --to 1.3.13 --dry-run`。
2. 审阅将被移除的 code baseline、精简文档、选择器和 worktree 门禁投影，再执行实际升级和 `mango pmo check --project-dir . --locked`。
3. 已有业务代码、无关业务包和 Mango Maven 版本不回退；仅替换 PMO baseline、Skill、合同和 CLI 模板消费方式。

### Verification

- PMO Node 测试、文档合同测试、Skill eval 与治理意图检查。
- `@mango/pmo` build/check、Business Starter 精确投影和 `@mango/cli` 全量测试。
- 本地 tarball 消费者执行 PMO 升级、full 项目初始化和 CLI 自带业务模块模板生成。

## 1.3.12 - 2026-08-08

### Fixed

- 发布 `check-worktree-delivery-integrity.mjs` 的 `start`、`commit`、`deliver`、`cleanup` 四阶段检查，阻断跨任务 worktree 复用、部分提交、未跟踪文件、未同步 upstream 和未合并清理。
- 收紧 `mango-engineering` 与交付保障 Skill：任务 worktree 身份只能来自当前会话、Issue 或 PR 记录，不能仅按当前分支名称自行推断。
- 保留精确并行脏 worktree 的人工确认入口，同时禁止把已合并但仍有本地改动的 worktree 当作并行任务豁免。

### Changed

- 将 PMO package、Codex plugin、四类生命周期合同和业务 starter 投影同步到 `1.3.12`；合同 schema revision 保持 `1`。
- 人类可读的工程 Skill 和门禁输出统一为中文，命令、字段和机器状态值保持稳定。

### PMO Required Checks

- `check-worktree-delivery-integrity.mjs`
  - Migration: 业务项目升级 PMO baseline 后逐个核对 active worktree；已有项目另行补充正常构建产物的 `.gitignore` 模式。
  - Exception: `start` 只接受用户确认的精确并行 worktree 路径；当前任务 worktree 的未提交、未跟踪、未 Push 或未合并状态不能豁免。
  - Verify: 执行 24 个工具场景、Skill 空白上下文评测、CLI 69 项测试和真实 tarball 业务项目提交/构建/交付链路。

### Upgrade Notes

1. 等待 `@mango/pmo@1.3.12` 从消费仓可见后，执行 `mango pmo upgrade --project-dir . --to 1.3.12 --dry-run`。
2. 审阅规则、Skill、工具和 PR 模板变化后执行实际升级，再运行 `mango pmo check --project-dir . --locked`。
3. 对每个 active worktree 分别执行 `start` 检查；提交前逐项暂存任务文件并运行 `commit`，Push 后运行 `deliver`，合并后运行 `cleanup`。

### Verification

- `node --test mango-pmo/tests/worktree-delivery-integrity.test.mjs`
- `node mango-pmo/tools/check-worktree-delivery-integrity.mjs --mode <start|commit|deliver|cleanup>` 的真实仓库与空白业务仓场景。
- PMO package build/check、business starter 精确投影、Skill eval、治理意图和文档合同门禁。

## 1.3.11 - 2026-08-06

### Fixed

- Stop applying newly added lifecycle document sections and approval-format rules retroactively to immutable historical documents whose path, SHA-256 and historical `pmoVersion` all match the upgrade-generated baseline.
- Keep collection-level duplicate document ID, adjacent lifecycle stage and upstream SHA-256 checks active for those historical snapshots, so compatibility does not weaken the document graph.

### Changed

- Advance lifecycle document contracts to PMO `1.3.11` without changing schema revision `1`; unchanged PMO `1.3.10` documents join the existing historical version set only through the path/SHA/version baseline.
- Ship the synchronized rules, contracts, checker tests, business-starter projection and package-root Codex plugin in `@mango/pmo@1.3.11`.

### Upgrade Notes

1. Publish and verify `@mango/pmo@1.3.11` before installing `@mango/cli@1.0.101`.
2. Run `mango pmo upgrade --project-dir . --to 1.3.11 --dry-run`, review the projected changes, then perform the upgrade and run `mango pmo check --project-dir . --locked`.
3. Do not rewrite approved historical TDD/Plan files merely to add sections introduced by a newer PMO contract. Preserve their generated path/SHA/version entries; modified or new lifecycle documents must use `1.3.11`.
4. If a historical document changes or its upstream digest is stale, repair and reapprove the affected lifecycle chain instead of editing the baseline to hide the drift.

### Verification

- `node --test mango-pmo/tests/document-contract/document-contract.test.mjs`
- `node mango-ui/packages/mango-pmo/scripts/build-package.mjs && node mango-ui/packages/mango-pmo/scripts/check-package.mjs`
- `node mango-business-starter/scripts/sync-pmo-baseline.mjs --check`
- Business document-set regression against the 58-document guarantee project, including seven path/SHA/version-pinned historical documents.

## 1.3.10 - 2026-08-06

### Added

- Add the canonical `business-module` code baseline with backend API/core/starter/starter-remote modules, frontend API/page packages, Flyway/resources, public exports and focused tests.
- Add manifest-driven rendering and evaluation through `code-baseline.mjs` and `evaluate-code-baseline.mjs`, including input/derived-variable validation, path safety, unresolved-placeholder rejection and projection hash checks.
- Add explicit rule coverage, Mango Checkstyle and architecture quality profiles, and concrete template conventions for `XxxCode`, `Require`, typed CRUD, tenant persistence, Mapper/API/resource/migration and frontend patterns.

### Changed

- Route development toward selected code baselines and target repository source instead of mandatory bulk reference reading.
- Require generated code to retain the canonical baseline shape even in legacy repositories, while limiting touched-file cleanup to behavior-preserving violations in the same symbol or local block.
- Advance lifecycle document contracts to PMO `1.3.10` without changing schema revision `1`; unchanged PMO `1.3.9` documents remain supported only through the existing path/SHA/version historical baseline.
- Ship the mechanically synchronized business-starter baseline, package-root Codex plugin and project Skills in `@mango/pmo@1.3.10`.

### Upgrade Notes

1. Publish and verify `@mango/pmo@1.3.10` before installing `@mango/cli@1.0.99`.
2. Run `mango pmo upgrade --project-dir . --to 1.3.10 --dry-run`, review the projected rules, code templates, tools, Agents and Skills, then perform the upgrade and run `mango pmo check --project-dir . --locked`.
3. Use the published `business-module` baseline for new module work. Existing modules are not automatically rewritten and should only receive locally verifiable cleanup when their code is otherwise being changed.
4. Preserve historical PMO `1.3.9` lifecycle documents only when the upgrade-generated path, SHA-256 and version baseline remains valid; new or modified lifecycle documents use `1.3.10`.

### Verification

- `node --test mango-pmo/tests/code-baseline.test.mjs mango-pmo/tests/pmo-preflight.test.mjs mango-pmo/tests/document-contract/document-contract.test.mjs`
- `node mango-ui/packages/mango-pmo/scripts/build-package.mjs && node mango-ui/packages/mango-pmo/scripts/check-package.mjs`
- `node mango-business-starter/scripts/sync-pmo-baseline.mjs --check`
- `node mango-ui/packages/mango-cli/scripts/check-business-module-template.mjs`
- `node mango-ui/packages/mango-cli/scripts/check-cli.mjs`

## 1.3.9 - 2026-08-02

### Fixed

- Resolve terminal Gitea pull-request body edits without constructing an invalid base/head diff after the PR is merged or closed; open PR edits continue to run the full trusted-diff gate.
- Accept typed full-page baseline exceptions for genuinely inapplicable list, detail, form or dialog shells while continuing to reject untyped or explanation-free bypasses.
- Keep generated business PMO scope classification, governance-intent checks and baseline manifests mechanically aligned with the current source bundle.
- Advance lifecycle contracts to PMO `1.3.9` while accepting unchanged, upgrade-pinned PMO `1.3.8` documents as controlled historical inputs.

### Changed

- Ship the current Gitea event-mode resolver, frontend page-baseline behavior and synchronized generated-project workflow/tool projection in `@mango/pmo@1.3.9`.
- Preserve stable `pr-contract-check` and `pmo-doc-check` identities; this release adds no new required-check filename beyond the PMO `1.3.8` set.

### Upgrade Notes

1. Install `@mango/pmo@1.3.9` before `@mango/cli@1.0.96`.
2. Run `mango pmo upgrade --project-dir . --to 1.3.9 --sync-shell`, then review changes to GitHub/Gitea workflows, PMO tools, rules and the generated baseline manifest.
3. Run `mango pmo check --project-dir . --locked`; for Gitea, verify both an open-PR body edit and a terminal merged/closed-PR body edit.
4. Existing typed frontend page-baseline exceptions remain valid when their page kind and concrete reason still match the detected page; do not replace them with generic suppression text.
5. Existing PMO `1.3.8` lifecycle documents are preserved only when the upgrade-generated path/SHA-256/version baseline still matches; new or modified lifecycle documents use `1.3.9`.

### Verification

- `node --test mango-pmo/tests/pmo-check-event-mode.test.mjs mango-pmo/tests/frontend-page-baseline.test.mjs mango-pmo/tests/pmo-check-scope.test.mjs`
- `node mango-ui/packages/mango-pmo/scripts/build-package.mjs && node mango-ui/packages/mango-pmo/scripts/check-package.mjs`
- `node mango-business-starter/scripts/sync-pmo-baseline.mjs --check`
- `node mango-ui/packages/mango-cli/scripts/check-cli.mjs`

## 1.3.8 - 2026-08-01

### Fixed

- Preserve unchanged lifecycle documents created under PMO `1.3.6` or `1.3.7` when their schema-compatible contracts remain supported, using a path, SHA-256 and PMO-version baseline created by `mango pmo upgrade`.

### Changed

- Require new or modified management list pages to use `MangoListPage`, `MangoSearchPanel`, `MangoListPanel` and `Pagination`.
- Require new or modified independent detail/form pages to use the current Mango page shells and standard dialogs to use `MangoDialog`.
- Add the incremental frontend page-baseline checker to the PMO bundle and generated business baseline.
- Add frontend page-baseline execution to generated GitHub and Gitea workflows and aggregate it into the stable `pmo-doc-check` required check.

### PMO Required Checks

- `check-frontend-page-baseline.mjs`
  - Migration: Before upgrading, inspect changed `views/**/*.vue` files and migrate management lists, independent detail/form pages and standard dialogs to their current Mango components.
  - Exception: Use a reviewable typed comment such as `<!-- mango-page-baseline-exception form: embedded vendor form cannot use the independent page shell -->` only when the detected pattern is not the governed page type; supported kinds are `list`, `detail`, `form` and `dialog`.
  - Verify: `node business-pmo/mango-baseline/tools/check-frontend-page-baseline.mjs --base <base-sha> --head <head-sha> --frontend-root <frontend-root>`.

### Upgrade Notes

1. Inspect open and planned frontend changes before syncing PMO 1.3.8; migrate affected pages or add a typed exception with a concrete reason.
2. Run `mango pmo upgrade --project-dir . --to 1.3.8 --sync-shell` and review the PMO rule, tool and GitHub/Gitea workflow diff.
3. Review `.mango-pmo-legacy-documents.json`, run `mango pmo check --project-dir . --locked`, then run the frontend page-baseline checker against the intended PR base/head.
4. New or migrated lifecycle documents must use PMO `1.3.8`; only unchanged documents on contract-declared historical versions can use the generated compatibility baseline.

### Verification

- `node --test mango-pmo/tests/document-contract/document-contract.test.mjs`
- `node --test mango-pmo/tests/frontend-page-baseline.test.mjs`
- `node mango-ui/packages/mango-pmo/scripts/build-package.mjs && node mango-ui/packages/mango-pmo/scripts/check-package.mjs`
- `node mango-ui/packages/mango-cli/scripts/check-cli.mjs`
- `node mango-business-starter/scripts/sync-pmo-baseline.mjs --check`
