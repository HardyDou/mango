# Issue #794 本地优先发布流程实施计划

## 1. 基线

- 上游决定：[本地优先发布流程治理决定](../designs/2026-08-15-issue-794-local-first-release-governance.md)
- 状态：IN_PROGRESS
- 风险：L3 / FULL
- 工作区：M01=REUSE
- 不可变动作：实施和首次 `plan/prepare` 不发布；Nexus、Tag、GitHub Release 等待精确坐标形成后的独立授权。

## 2. 工作分解

| 阶段 | 动作 | 完成标准 | 主要验证 |
|---|---|---|---|
| P1 治理与冻结 | 登记 Issue、记录决定、冻结旧批次不可变发布 | Issue #794 和本计划可追踪；旧 manifest 无新增不可变尝试 | Git/manifest 状态回读 |
| P2 范围治理 | 接入 Changesets；实现源码差异防漏、依赖拓扑和版本矩阵生成 | 发布集合由机器生成；集合不一致失败 | 正例、漏登、错包、依赖联动、未知路径单测 |
| P3 历史迁移 | 从最后成功 Release 到当前 tree 生成一次性存量对账 | 得到 Shell/Admin/CLI，并由本次治理真实影响新增 PMO；Common 不发布；迁移入口有退出条件 | Git tag/tree、Nexus 坐标和源基线对账 |
| P4 本地准备 | 实现 `release plan/prepare`、一次构建封存、候选组合验证和无远端写入失败归档重试 | `.runtime` 生成计划、制品和证据；tree/计划/制品摘要绑定；失败证据不覆盖且不阻塞同 tree 修复重试 | 包清单、SHA、失败恢复、混合消费者集成测试 |
| P5 发布恢复 | 精简五状态 publish/repair，发布封存制品，有限双仓回读，Tag/Release 后移 | 支持 `PREPARED/CANDIDATE_VERIFIED/PUBLISHED/CONSUMER_VERIFIED/COMPLETED` 和失败状态 | 状态转换、部分发布、group pending、禁止重发单测 |
| P6 轻量 CI | Release PR 运行轻量策略检查；其它 PR 保持受影响门禁 | release-only PR 不安装依赖或重复构建；源码混入时回到普通门禁 | Workflow 结构和分类器测试 |
| P6A 本地提交门禁 | 新增 `mango-submit-pr`，让 Mango/业务 PR 在 Push 前执行所有 Runner 同源本地入口 | 最终 head 全绿才可提交；Runner 不作为调试循环；远端新失败先补本地复现 | Skill 正反例、边界和 final-head eval |
| P7 文档与清理 | 更新 CLI README、能力地图和唯一发布 Skill；撤销旧人工路径 | 使用说明与实际命令一致；无旧默认 fallback | README/能力文档 checker、旧入口搜索 |
| P8 首批试运行 | 最终四包执行新 `plan/prepare`，保留完整证据 | 精确清单、拓扑、封存 SHA、候选消费者 PASS | 本地 gates、registry doctor 只读检查 |
| P9 正式发布 | 取得当次授权后发布、纯 group 验证、Tag/Release、收尾 | 所有制品可消费且 manifest 完成 | 双仓回读、干净消费者、GitHub 回读 |

## 3. 历史空档具体执行

1. 读取最后成功 Release Tag 指向的 tree 和当时 CLI 版本矩阵。
2. 读取 Nexus 当前已发布精确坐标；不存在或响应异常时 fail closed。
3. 比较最后成功 Release tree 到当前候选 tree 的全部发布相关路径。
4. 使用已发布源基线消除“改动后又恢复”的包；本批已确认 Common 属于该情况。
5. 对剩余直接变化包计算固定运行时依赖和 CLI 矩阵闭包。
6. 生成一次性 `legacy-reconciliation` 记录，内容包含起点 Tag、首批最终 tree、四包版本、Common 恢复基线和 Issue #791/#794。
7. 新流程重算结果必须与合成 Changeset、Release PR 和候选封存清单完全一致。
8. 首批完成后记录新的成功基线，删除迁移开关；以后只能使用正常 Changesets。

## 4. 验证计划

| 验证 | 证明对象 | 入口 |
|---|---|---|
| M09 | 配置、版本集合、拓扑、Workflow 和旧入口清理 | Changeset/plan checker、`git diff --check`、样式与 workspace checks |
| M10 | 范围算法、状态转换、有限重试、恢复规则 | Node 单元测试 |
| M11 | 封存 tarball、混合候选消费者和纯仓库消费者 | 临时消费者集成测试 |
| M14 | 发布系统自修改的完整性、恢复和效率边界 | 独立发布/架构复核，输出阻断项与结论 |
| M15 | Issue/PR/checks、Nexus hosted/group、Tag/Release | `gh`、npm/Maven 精确坐标回读 |

## 5. 停止条件

- 发现当前候选 tree 与最后成功 Release 之间存在未解释制品变化。
- Changesets、自动影响集合、CLI 矩阵或封存集合不相等。
- 目标版本已存在但内容无法证明等同，或 registry 响应不明确。
- prepare 后 Git tree、计划摘要或制品 SHA 变化。
- 候选消费者或纯 group 消费者失败。
- 未取得当前回合的精确发布授权。
