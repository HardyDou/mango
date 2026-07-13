# 架构债务递减预算

`debt-budget.json` 使用 schema v4 记录全 Reactor 架构扫描的全局及逐 Maven 模块规则计数、稳定问题 identity 多重集合。它只豁免已经登记的历史身份，不能豁免当前变更新增、替换或跨模块移动的问题。

## 更新要求

- 正向：先完成一次完整 Reactor 架构扫描；schema v2 报告的实际/预期项目数、`modules` 目录和所有问题 `moduleKey` 必须完整。再执行 `node mango-pmo/tools/check-architecture-debt-budget.mjs --base-ref "$(git merge-base HEAD origin/main)"`，同时比较主分支预算、PR 预算和当前报告。
- 正向：查看目录聚合债务时执行 `node mango-pmo/tools/check-architecture-debt-budget.mjs --module mango-platform/mango-system`；查看单模块时可传唯一 artifactId，例如 `--module mango-system-core`。`--module` 可重复，所有查询复用同一份全 Reactor 报告。
- 正向：某个模块债务减少后执行 `node mango-pmo/tools/check-architecture-debt-budget.mjs --module mango-system-core --write`，只降低所选模块并重新计算全局聚合；提交前仍须执行无 `--module` 的全局检查。
- 禁止：为了让新违规通过而增加全局或模块预算；禁止把问题转移到其它模块、手工修改聚合、使用无法归属的问题、用部分 Reactor 报告写预算，或为每个模块重复运行全量 Maven 构建。
- 例外：规则升级确需增加存量预算时，只能在未手工修改原预算的前提下使用完整报告执行全局 `--write --accept-increase --reason "<审批原因>"`。工具会把原因绑定到原预算 SHA-256；提交后必须用 `--base-ref` 复验，并取得与仓库 `single-owner` 或 `multi-maintainer` 模式一致的授权。模块模式禁止 `--accept-increase`。

正例：`mango-system-core` 从 233 条降到 223 条后，只写回该模块；其它模块不变，全局预算同步减少 10 条，后续检查以新值为上限。

反例：模块 A 修复一条、模块 B 新增一条后直接写回全局预算。即使总数不变，模块 B 的 identity 增加和模块归属变化也必须失败。

身份判定示例：历史 PMD 问题因文件前面增加注释从第 20 行移到第 21 行，违规源码行内容未变，仍视为同一历史身份；删除 `OldService` 的违规却在 `NewService` 新增同规则违规，会形成一个 identity reduction 和一个 identity increase，必须失败。
