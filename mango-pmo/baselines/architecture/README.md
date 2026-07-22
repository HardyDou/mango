# 架构债务递减预算

`debt-budget.json` 使用 schema v4 记录全 Reactor 架构扫描的全局及逐 Maven 模块规则计数、稳定问题 identity 多重集合。它只豁免已经登记的历史身份，不能豁免当前变更新增、替换或跨模块移动的问题。

schema v4 还可记录 `moduleOnboardings`。它只用于 base 中没有 `module.properties` 的存量模块首次纳管，记录与 Git base、模块身份文件和完整 Reactor inventory 绑定，后续不可修改或删除。

PMO 1.2.0 / Mango Maven 1.0.17 规则升级首次识别出的存量问题，已通过完整 `212/212` Reactor 报告按 `moduleKey` 登记为历史债务。`debt-budget.json` 是数量和 identity 的唯一事实源；每个模块的预算只能下降，不能使用其它模块的减少抵消本模块新增问题。

## 存量接入顺序

| 阶段 | 独立 PR 允许的改动 | 完整 Reactor inventory | 结果 |
|------|-------------------|-------------------------|------|
| 治理升级 | PMO bundle、项目 Skill、PR 模板托管区段、GitHub/Gitea workflow | 必须，且 `inventoryOnly=true` | 证明缺失预算期间只发生治理升级 |
| 预算初始化 | 只新增项目预算文件 | 必须，且 `inventoryOnly=true` | 建立 schema v4 全项目初始上限 |
| 模块身份纳管 | 一个 starter `module.properties` 与预算 | 必须，且 `inventoryOnly=true` | 写入不可变 `moduleOnboardings` 审计 |
| 正常业务开发 | 业务改动；预算只允许随债务下降 | 普通 PR 不要求全量 inventory | 阻断新增、替换、迁移和预算回升 |

四阶段必须按顺序合并，不能压缩成一个 PR。新生成项目已经有空项目预算，跳过前两阶段；base 已有目标 `module.properties` 的模块也跳过身份纳管。业务项目的完整可执行命令见其 `business-pmo/README.md`，Mango 主仓维护者见 `mango-pmo/README.md` 第 8 节。

## 更新要求

- 正向：先完成一次完整 Reactor 架构扫描；schema v2 报告的实际/预期项目数、`modules` 目录和所有问题 `moduleKey` 必须完整。再执行 `node mango-pmo/tools/check-architecture-debt-budget.mjs --base-ref "$(git merge-base HEAD origin/main)"`，同时比较主分支预算、PR 预算和当前报告。
- 正向：查看目录聚合债务时执行 `node mango-pmo/tools/check-architecture-debt-budget.mjs --module mango-platform/mango-system`；查看单模块时可传唯一 artifactId，例如 `--module mango-system-core`。`--module` 可重复，所有查询复用同一份全 Reactor 报告。
- 正向：某个模块债务减少后执行 `node mango-pmo/tools/check-architecture-debt-budget.mjs --module mango-system-core --write`，只降低所选模块并重新计算全局聚合；提交前仍须执行无 `--module` 的全局检查。
- 正向：普通 PR 只执行直接改动模块的质量门禁，并用 `--baseline-only --base-ref <base-sha>` 校验已提交预算未被无授权抬高；完整 212 模块报告只由定时/手工 inventory 或明确的预算迁移生成，不因同一 PR 后续同步而重复扫描。
- 正向：首次纳管先建立只包含目标 starter `module.properties` 的独立 PR，暂存该文件并完成全 Reactor 编译后，以 `-Dmango.architecture.requireFullReactor=true -Dmango.architecture.inventoryOnly=true` 现场生成完整报告，再执行 `node mango-pmo/tools/check-architecture-debt-budget.mjs --onboard-module <moduleKey-prefix> --module-properties <path> --base-ref <base-sha> --reason "<审批原因>" --write`。提交后的 required check 必须用可信 CI 对同一 PR 现场重建报告并执行普通 `--base-ref` 检查；纳管合并后再建立业务 PR。
- 正向：生成业务项目把项目预算放在 baseline 外的 `business-pmo/architecture-debt-budget.json`，并通过 `business-pmo/mango-baseline/tools/check-architecture-debt-budget.mjs` 显式传入 `--report backend/target/mango-architecture-report.json --baseline business-pmo/architecture-debt-budget.json`；GitHub/Gitea workflow 在 governance 模式生成完整报告，在 partial 模式只执行 baseline-only。
- 正向：旧业务仓第一次建立项目预算时，先合并 PMO 与 workflow 升级，再在只新增预算文件的 PR 中用完整报告执行 `--write`；可信 CI 以同一完整报告和 base SHA 复验。首次预算禁止携带纳管记录或其它文件，baseline-only 不能批准。
- 禁止：为了让新违规通过而增加全局或模块预算；禁止把问题转移到其它模块、手工修改聚合、使用无法归属的问题、用部分 Reactor 报告写预算，或为每个模块重复运行全量 Maven 构建。
- 禁止：首次纳管与业务源码、POM、配置、规则升级或其它模块变更共用一个 PR；禁止使用 `--baseline-only`、开发者上传的报告或全局 `--accept-increase` 批准首次纳管。
- 例外：规则升级确需增加存量预算时，只能在未手工修改原预算的前提下使用完整报告执行全局 `--write --accept-increase --reason "<审批原因>"`。工具会把原因绑定到原预算 SHA-256；提交后必须用 `--base-ref` 复验，并取得与仓库 `single-owner` 或 `multi-maintainer` 模式一致的授权。合并后该记录作为不可扩张预算的审计证据保留；它不能授权未来增加，未来增加必须绑定新的 base 摘要。模块模式禁止 `--accept-increase`。

正例：`mango-system-core` 从 233 条降到 223 条后，只写回该模块；其它模块不变，全局预算同步减少 10 条，后续检查以新值为上限。

反例：模块 A 修复一条、模块 B 新增一条后直接写回全局预算。即使总数不变，模块 B 的 identity 增加和模块归属变化也必须失败。

身份判定示例：历史 PMD 问题因文件前面增加注释从第 20 行移到第 21 行，违规源码行内容未变，仍视为同一历史身份；删除 `OldService` 的违规却在 `NewService` 新增同规则违规，会形成一个 identity reduction 和一个 identity increase，必须失败。
