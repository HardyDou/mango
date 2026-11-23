## 1.6.4 - 2026-11-23

### Added

- `tools/check-admin-aggregate.mjs`：校验官方能力模块的 starter 聚合、Flyway 模块开关与 Resource 声明可达三者一致，并接入 PMO gates。
- `tools/check-migration-duplicates.mjs`：禁止同一迁移模块内两个文件创建同名表，历史重复登记在 `baselines/migration-create-table-duplicates.txt`。

### Fixed

- 架构债预算校验的 `--base-ref` 支持 `HEAD^`、`origin/main~2` 这类 git revision 语法，修复 `Mango Architecture Debt Inventory` 定时任务长期失败。

## 1.6.3 - 2026-11-19

### Added

- `tools/workflow-contract-fingerprint.mjs`：从上一批次标签计算 `workflow-contract-fingerprint`（`api`、`web`、`changed`、`since`）与 `workflow-compatible-additions`，并提供 `--verify-release-notes` 校验发布说明中的契约块；配套用例 `tests/workflow-contract-fingerprint.test.mjs`。
- `mango-docs/guides/operations/workflow-contract-fingerprint.md` 说明指纹算法、`# WARN` 语义与发布门禁用法。

### Changed

- `mango-release` Skill 的 Readiness 要求触碰 `mango-workflow` 的批次携带契约块，`pmo-doc-check` 增加对应校验入口。

