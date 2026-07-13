# 架构债务递减预算

`debt-budget.json` 记录全 Reactor 架构扫描的存量规则计数和稳定问题身份多重集合。它只豁免已经登记的历史身份，不能豁免当前变更新增或替换的问题。

## 更新要求

- 正向：先完成全 Reactor 架构扫描，再执行 `node mango-pmo/tools/check-architecture-debt-budget.mjs --base-ref "$(git merge-base HEAD origin/main)"`；工具同时比较主分支预算、PR 预算和当前全量报告，当前变更仍须独立通过 no-new-violations 门禁。
- 正向：债务减少后执行 `node mango-pmo/tools/check-architecture-debt-budget.mjs --write`，把预算同步降到新值。
- 禁止：为了让新违规通过而直接增加预算，或删除某一规则的历史计数。
- 例外：确需增加预算时，只能在未手工修改原预算的前提下执行 `--write --accept-increase --reason "<审批原因>"`。工具会把原因绑定到原预算 SHA-256；提交后必须使用 `--base-ref` 复验，并由 CODEOWNERS 审核原因和差异。

正例：修复 20 条 `MANGO-ARCH-BEAN-001` 后更新预算，后续检查以更低数量为上限。

反例：新增违规导致 CI 失败后，直接调高 `debt-budget.json` 数量或复用上次的批准原因以恢复绿灯。base/head/current 三方校验必须拒绝这两种做法。

身份判定示例：历史 PMD 问题因文件前面增加注释从第 20 行移到第 21 行，违规源码行内容未变，仍视为同一历史身份；删除 `OldService` 的违规却在 `NewService` 新增同规则违规，会形成一个 identity reduction 和一个 identity increase，必须失败。
