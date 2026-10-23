# Issue #1106 批次级升级日志条目（待发布时并入）

本文件是 `mango-ui/.changeset/release-notes.txt` 的待并入条目草稿。Issue #1106 的修复为 Maven-only 变更，无 npm 坐标变化，不产 Changeset。

发布批次规划时，把下方各章节内容合并进该批次的 `release-notes.txt`，随 `mango release plan/prepare` 生成 `mango-docs/guides/operations/mango-<旧版本>-to-<新版本>-upgrade.md` 与 `CHANGELOG.md` 最新段。

不得提前把本文件内容写入 `CHANGELOG.md`：批次版本号与同批 PR 清单在发布规划时才确定，提前写入会产生与最终批次不一致的记录。

## Pull Requests

- PR #1114 Fixed: 同一构建版本重复发布时 bootstrap finalize 报 BOOTSTRAP_CANDIDATE_MISSING 并进入人工接管。Packages: Maven `mango-infra-bootstrap-core`, `mango-infra-bootstrap-starter`. Business Adaptation: 升级后同一 generation 重复 finalize 幂等返回 FINALIZED；部署引擎改为按模块 README §7.1 契约调用。

## Fixed

- 同一构建版本重复发布不再失败。finalize 对已是 stable 且 fingerprint 未变的同一 generation 返回 FINALIZED，执行 0 步，不修改 `mango_bootstrap_control` 与执行记录。

## Added

- 新增错误码 `BOOTSTRAP_GENERATION_ALREADY_RELEASED`：同一 generation 已用不同内容发布时显式拒绝，不再与"缺少候选"混淆。

## Changed

- `mango-infra-bootstrap` README 新增 §7.1「部署引擎调用契约」：统一调用序列、幂等保证、只看退出码的成功判定、8 个错误码对应的引擎动作、按状态区分的回滚契约。
- 部署引擎不再需要比对控制表身份来决定是否 finalize；统一 `plan → apply → finalize → verify` 即可收敛。
- 模块 README 新增 §12「升级与兼容」，记录本模块的升级要点与回滚方式。

## Business Impact

- 后端消费者：`mango-infra-bootstrap-core`、`mango-infra-bootstrap-starter` 需升级到本批次版本后生效。
- 运维：已停在 `FAILED_MAINTENANCE` 的环境不会自动恢复，需确认 `state=FINALIZED` 且 `candidate_generation IS NULL` 后重放 commit 并关闭维护。
- 前端消费者：无影响。

## Upgrade Notes

1. 聚合消费者（`mango-admin-starter` 或 app）升级 Maven 版本即可，无需配置变更。
2. 部署引擎按下两项自查：删除基于控制表的"是否需要 finalize"预检；确认失败回滚不对已 `FINALIZED` 的 generation 调用 `abort`。
3. 不重建数据库，不清空 `mango_bootstrap_*` 审计表。

## Verification

- 同一版本连续两次 `bootstrap finalize`：均返回 `FINALIZED`，`mango_bootstrap_control` 与执行表行数不变。
- 正常滚动升级：`apply --strategy=rolling` → `finalize` → `verify` 全部通过，`stable_generation` 正常推进。

## Rollback

- 回退到上一 Maven 版本即可，已写入的 `FINALIZED` 状态无需修复。
- 不要手工修改 `mango_bootstrap_control` 或删除执行记录。

## Upgrade Estimate

- Audience: 使用 `mango-infra-bootstrap` 生命周期入口的部署环境与聚合消费者。
- Engineering Effort: 后端依赖版本升级约 10 分钟；部署引擎契约自查约 30 分钟。
- Execution Window: 与批次内其它 Maven 变更共用同一维护窗口。
- Service Downtime: 依赖引擎切换流程，本变更不额外引入停机。
- Rollback Effort: 回退 Maven 版本约 10 至 15 分钟，不需要数据修复。
- Assumptions: 部署引擎不在本次批次内改造也能恢复；本修复消除的是失败触发源，不替代引擎侧契约对齐。
