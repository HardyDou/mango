# 标准交付记录

> **结论：** `NODE_AFFINITY` 节点显式配置 `nodeAffinityFallback=CLAIM` 时，候选角色没有成员不再阻断审批发起，任务保留角色候选组并进入待领取；其它回退值继续按 `AUTO_ASSIGN_NO_CANDIDATE` 失败。

## 1. 元数据

- 任务 ID：ISSUE-1249
- 交付模式：STANDARD
- 需求影响：L2 - 候选角色为空会阻断审批提交并回滚业务数据，影响审批状态与异常边界，影响面限定在启用 `NODE_AFFINITY` 的节点
- 方案风险：L2 - 修改共享自动派单候选校验路径，新增策略与回退组合分支，模块内可回退但影响 `AUTO` 派单语义
- 最终风险：L2
- 工作区决策：CREATE

## 2. 目标与范围

- 目标：修复 Mango Issue #1249，让 `assignmentMode=AUTO` + `autoAssignmentStrategy=NODE_AFFINITY` + `nodeAffinityFallback=CLAIM` 在候选角色没有成员时按待领取处理。
- 成功条件：流程正常启动；任务 `assignee_` 为空；角色候选组保留；不再抛出 `AUTO_ASSIGN_NO_CANDIDATE`。
- 处理范围：`mango-workflow-core` 自动派单候选校验、对应集成测试、Workflow 模块 README 和业务接入指南。
- 不处理范围：不新增 `nodeAffinityFallback=FAIL` 枚举；非 `CLAIM` 回退仍维持原失败语义；不修改工作流引擎或前端设计器。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| SR-1 | 业务发起审批 | 节点 `assigneeType=SPECIFIED_ROLE`、`roleIds=[999]`、`assignmentMode=AUTO`、`autoAssignmentStrategy=NODE_AFFINITY`、`nodeAffinityFallback=CLAIM`，角色 999 无成员 | 流程启动，任务保持未指派并保留 `ROLE:999` 候选组 | 不抛 `AUTO_ASSIGN_NO_CANDIDATE` | 集成测试断言任务存在、assignee 为空、候选组为 `ROLE:999`、流程实例仍在运行 |
| SR-2 | 业务发起审批 | 同上但 `nodeAffinityFallback=ROUND_ROBIN`，角色 999 无成员 | 自动派单无法选出办理人 | 抛 `AUTO_ASSIGN_NO_CANDIDATE`，流程推进和状态更新回滚 | 集成测试断言业务异常消息包含“自动派单节点没有可用候选人” |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| D-1 | SR-1、SR-2 | 仅当 `strategy=NODE_AFFINITY` 且 `nodeAffinityFallback=CLAIM` 时跳过空候选失败校验；其余组合保持原校验 | `WorkflowTaskRuntimeService#autoAssign` | 恢复无条件调用 `requireCandidates` |
| D-2 | SR-1 | 空候选时复用现有 `Decision(userId=null)` 分支，`candidateLinker.applyGroups` 保留角色候选组，不新增配置或枚举 | `WorkflowTaskRuntimeService#autoAssign`、`WorkflowRuntimeCandidateLinker#applyGroups` | 删除跳过条件 |
| D-3 | SR-1、SR-2 | 同步模块 README 与业务接入指南，说明 `CLAIM` 例外和非 `CLAIM` 的失败语义 | `mango/mango-platform/mango-workflow/README.md`、`mango-docs/guides/business-integration/workflow-business-approval.md` | 回退文档段落 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| IMPL-1 | D-1、D-2 | 1 | `mango/mango-platform/mango-workflow/mango-workflow-core/src/main/java/io/mango/workflow/core/service/impl/WorkflowTaskRuntimeService.java` | 空候选 `CLAIM` 不再失败 |
| IMPL-2 | SR-1、SR-2 | 2 | `mango/mango-platform/mango-workflow/mango-workflow-core/src/test/java/io/mango/workflow/core/service/impl/WorkflowTaskRuntimeServiceIntegrationTest.java` | 两个集成测试分别覆盖待领取和非 `CLAIM` 失败 |
| IMPL-3 | D-3 | 3 | `mango/mango-platform/mango-workflow/README.md`、`mango-docs/guides/business-integration/workflow-business-approval.md` | 文档与运行时语义一致 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| SR-1 | 修复前红灯 | `mvn -pl mango-platform/mango-workflow/mango-workflow-core -Dtest=WorkflowTaskRuntimeServiceIntegrationTest#nodeAffinityClaimKeepsTaskPendingWhenConfiguredRoleHasNoMembers test` | FAIL：复现 `自动派单节点没有可用候选人` | `target/surefire-reports` 中 `BizException` 消息与 Issue #1249 一致 |
| SR-1 | M11 集成测试 | 同上，修复后执行 | PASS：1 test | 任务未指派且保留 `ROLE:999` 候选组 |
| SR-2 | M11 集成测试 | `mvn -pl mango-platform/mango-workflow/mango-workflow-core -Dtest='WorkflowTaskRuntimeServiceIntegrationTest#nodeAffinityClaimKeepsTaskPendingWhenConfiguredRoleHasNoMembers+nodeAffinityNonClaimFallbackStillFailsWhenCandidatesEmpty' test` | PASS：2 tests | 非 `CLAIM` 仍抛 `AUTO_ASSIGN_NO_CANDIDATE` |
| SR-1、SR-2 | 模块质量门禁 | `mvn -pl mango-platform/mango-workflow/mango-workflow-core verify` | PASS：178 tests，0 failures，0 errors，4 skipped | Maven `BUILD SUCCESS` |
| 测试资产 | 测试质量与 Mock 审计 | `node mango-pmo/tools/test-quality-check.mjs --base origin/main`、`node mango-pmo/tools/audit-backend-test-mocks.mjs --report-only --changed-only --base origin/main` | PASS：1 file；block=0，warn=0 | 命令输出 |

## 7. 例外与剩余风险

- 未在业务项目 `mango-baohan-risk` 重跑完整 E2E；空角色候选路径由 `mango-workflow-core` 的 Flowable + H2 集成测试覆盖。
- 未新增 `nodeAffinityFallback=FAIL` 枚举：`ROUND_ROBIN`、`RANDOM`、`LEAST_TASKS` 在候选为空时仍失败，已可区分保留失败语义。
