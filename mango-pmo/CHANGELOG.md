## 1.6.3 - 2026-11-19

### Added

- `tools/workflow-contract-fingerprint.mjs`：从上一批次标签计算 `workflow-contract-fingerprint`（`api`、`web`、`changed`、`since`）与 `workflow-compatible-additions`，并提供 `--verify-release-notes` 校验发布说明中的契约块；配套用例 `tests/workflow-contract-fingerprint.test.mjs`。
- `mango-docs/guides/operations/workflow-contract-fingerprint.md` 说明指纹算法、`# WARN` 语义与发布门禁用法。

### Changed

- `mango-release` Skill 的 Readiness 要求触碰 `mango-workflow` 的批次携带契约块，`pmo-doc-check` 增加对应校验入口。

