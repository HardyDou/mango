# Issue #794 本地优先发布流程治理决定

## 1. 决定与状态

- Issue：[HardyDou/mango#794](https://github.com/HardyDou/mango/issues/794)
- 状态：APPROVED，用户于 2026-08-15 明确确认发布范围和本地优先发布方案。
- 交付模式：FULL。
- 需求影响：L3；发布范围遗漏或制品组合不兼容会阻断所有 Mango 消费项目升级、构建或启动。
- 方案风险：L3；修改发布门禁、不可变坐标状态机、PR Workflow 和恢复语义。
- 最终风险：L3。
- 工作区：M01=REUSE；`/Users/hardy/Work/mango-release-admin-compat-107`。
- 启用措施：M01、M07、M08、M09、M10、M11、M14、M15。
- 不适用：M02（无数据库）、M12（无服务 API）、M13（无 UI）、M16（自动化和发布人授权足以观察目标）。

## 2. 目标和边界

目标：让发布人提交 Release PR 前在本地确定发布范围、完成制品封存和候选组合验证；Runner 只做轻量策略检查；正式发布继续在本地执行并使用同一封存制品。

范围：

- Changesets 登记发布意图，源码差异检查防漏，依赖图补齐联动包。
- 本地 `mango release plan`、`prepare`、`publish`、`repair`、`status`。
- 一次构建并封存 tarball/JAR，证据绑定 Git tree、计划摘要和制品 SHA-256。
- 发布前一次候选组合消费者验证，发布后一次纯消费仓验证。
- hosted/group 有限回读、断点恢复、Tag/GitHub Release 后移。
- Release PR 轻量 Runner；普通功能 PR 的受影响门禁保持不变。
- Mango 主仓和业务仓统一使用 `mango-submit-pr` 提交 PR；所有 Runner required checks 都必须有同源本地入口，最终 head 本地全绿后才允许 Push。

非目标：

- 不把正式发布迁移到 GitHub Runner。
- 不关闭 branch protection 或普通功能 PR required checks。
- 不覆盖、删除或重发任何已存在的 npm/Maven 坐标。
- 不修改 Mango 业务运行时接口、数据库、菜单、权限或租户语义。

## 3. 唯一执行路径

新路径固定为：

1. 功能 PR 提交 Changeset；CI 对源码变化和 Changeset 做一致性检查。
2. 发布人从最新主干在本地运行 `mango release plan`，生成 Release PR 版本、依赖、Changelog、CLI 矩阵和机器计划。
3. Release PR 提交前运行 `mango release prepare`，一次构建、封存并完成候选 tuple 验证。
4. Runner 只校验 Release PR 的生成计划、允许变更和本地准备证据绑定关系。
5. Release PR 合并后，`publish` 证明合并 tree 与 prepare tree 一致，再按拓扑发布封存制品。
6. 全部坐标从消费仓完成一次干净消费者验证后，创建 Tag 和 GitHub Release。

PR 提交与发布状态机职责分离：`mango-submit-pr` 只负责本地核验、Commit、Push、创建或更新 PR 和远端回读；`mango-release` 只负责 Mango 制品计划、封存、发布、恢复和收尾。Runner 是最终 head 的独立复核面，不是通过反复 Push 暴露问题的开发环境。

旧 `.mango-release.json` 人工批次列表和十七状态执行路径退出默认执行；不保留未定义期限的 fallback。

## 4. 历史空档迁移

Changesets 接入前的空档只允许迁移一次：

- 最后成功 Release：`v2026.08.14-pmo-1.3.14-cli-1.0.106-historical-document-compat-release`。
- 起点提交：`6ee5334f75b1fe190b5677c684efa6652166cf12`。
- 首批实现前候选提交：`8642714232d5c7e378c38fc136ca9658aec8c9c4`；最终发布 tree 由 Release PR 的机器计划和 prepare manifest 绑定，不沿用该旧 tree。
- 累计业务修复影响：`@mango/admin-shell`、`@mango/admin`、`@mango/cli`；合入最新 `main` 后，Issue #43 / PR #795 的 Job 分页修复新增 `@mango/job@1.0.27`，本次治理修改发布 Skill 和规则新增 `@mango/pmo@1.3.15`。机器范围以完整累计差异为准，不为保持旧包数排除后续已合并变化。
- `@mango/common` 当前源码与已发布 1.0.26 源基线一致，不进入首批发布。

迁移生成带 `legacy-reconciliation` 标识的合成 Changeset，并记录起点 Tag、起止 tree、影响包和恢复基线。首批成功发布后写入新基线并永久关闭该迁移入口。后续缺 Changeset 必须失败，不能再次选择任意 Git base 补登记。

## 5. 质量与效率决定

- 发布范围、Release PR 版本集合、封存集合、发布集合和 Release Notes 的 Published Packages 必须相等。
- 同一批共享门禁只执行一次；逐包只执行构建、发布和精确回读。
- 候选验证使用“本批封存制品 + 消费仓未变化精确版本”；发布后验证只使用消费仓。
- 测试、发布和恢复必须使用同一 SHA-256 制品。
- prepare 在任何远端写入前失败时记录 `FAILED`，同一 plan/source tree 重试前归档原失败目录；不同 tree、不同计划或无法证明无远端写入的失败不自动复用。
- hosted 已存在而 group 暂不可见时进入 `VERIFY_PENDING`，只读重试，不重发。
- npm 批次目标 15-25 分钟；平台混合批次目标 30-45 分钟；单次仓库可见性等待不超过 5 分钟。

## 6. 验收标准

| ID | 可观察结果 |
|---|---|
| REL-PLAN-001 | 多个 PR 的未发布 Changesets 被一次汇总，发布范围不依赖最近一次提交或人工 base。 |
| REL-PLAN-002 | 源码变化缺 Changeset、Changeset 错包、未知归属、版本或拓扑冲突均 fail closed。 |
| REL-LEGACY-001 | 首次迁移从最后成功 Release 累计得到 Admin 三包，并把本次治理新增 PMO、随后合并的 Job 修复纳入最终五包计划；Common 识别为已恢复发布基线。 |
| REL-PREP-001 | 目标制品只构建一次，计划、候选验证和封存清单绑定同一 Git tree；本地 prepare 失败证据可归档后在同一 tree 重试。 |
| REL-CONSUME-001 | 发布前混合 tuple 和发布后纯 group tuple 各只运行一次干净消费者验证。 |
| REL-PUBLISH-001 | publish 使用 prepare 生成的精确制品；tree 或 SHA 不一致时拒绝。 |
| REL-RECOVER-001 | 部分发布后跳过已存在坐标，从首个未发布坐标继续；不重建、不重发。 |
| REL-CLOSE-001 | 全部消费验证通过后才创建 Tag/GitHub Release。 |
| REL-CI-001 | Release PR Runner 只做轻量策略检查，普通功能 PR 门禁不降级。 |
| REL-PR-001 | 每个 required Runner check 都有同源本地入口；最终 head 本地全绿前 Push/创建 PR 被禁止，Runner 失败不得形成盲目补提交循环。 |

## 7. 恢复与退出条件

- 实施合并前可整体回退，不影响任何不可变坐标。
- 首批发布前若新流程发现额外未发布包，更新 Release PR 和候选版本，不允许排除真实影响包。
- 首批发布后若消费者失败，已发布坐标保持不可变，使用新补丁版本修复；不覆盖旧坐标。
- 旧发布配置和兼容迁移入口只有在新计划、prepare、publish、repair、轻量 CI 和首批验证全部通过后删除。
