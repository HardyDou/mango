# 标准交付记录

## 1. 元数据

- 任务 ID：Issue #1191 工作流退回后指定办理人
- 交付模式：STANDARD
- 需求影响：L2 - 为 `returnTask` 增加可选 `targetAssigneeUserId`，改变退回后的办理人选择语义；不新增访问控制码、不做数据库迁移、不改前端页面，影响所有使用退回动作的业务流程
- 方案风险：L2 - 涉及核心任务运行时与 Flowable 任务创建后的同事务指派、参与人投影、业务当前任务刷新和失败回滚；已用真实 Flowable 引擎和 H2 集成测试覆盖
- 最终风险：L2
- 工作区决策：REUSE（`/Users/hardy/Work/mango-issue-1191-return-assignee`，分支 `feat/issue-1191-return-assignee`，基于 `origin/main`）

## 2. 目标与范围

- 目标：业务退回审批任务时，可以在同一事务内指定退回后目标任务的办理人，用于“复审退回初审时回到当时的初审专员”，不需要额外的 `transfer` 调用、目标节点转办动作或目标节点候选资格。
- 成功条件：
  1. `ReturnWorkflowTaskCommand.targetAssigneeUserId` 支持用户 ID 或用户名，目标用户必须是当前机构启用成员。
  2. 指定成功后，本次退回新建的目标任务 `ASSIGNEE_` 为目标用户，业务当前任务投影、参与人投影和 `workflow.task.advanced` 都反映新办理人。
  3. 目标用户无效、目标节点没有新建任务或新建多条任务时，以 `TASK_INVALID` 失败并整体回滚。
  4. 不传 `targetAssigneeUserId` 时，退回行为与改动前一致。
- 处理范围：`mango-workflow-api` 的退回命令和 API 说明；`mango-workflow-core` 的退回运行时和退回目标支持；Workflow README、能力地图、业务接入指南、变更记录；Mockito 契约测试和真实 Flowable 集成测试。
- 不处理范围：不新增 `assign` 独立 API；不改变 `transfer` 的授权和审计语义；不实现“自动选择上次处理人”的节点策略；不改数据库结构和前端页面；不覆盖多实例节点指定办理人（首版按失败处理）。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| AC-1 | `WorkflowTaskRuntimeApi.returnTask` | 调用人可办理当前任务，目标用户是当前机构启用成员，目标节点本次新建唯一任务 | 目标任务 `ASSIGNEE_` 为目标用户；`currentTasks` 返回新办理人；`workflow.task.advanced` 携带新办理人 | 目标用户不存在或不是启用成员时 `TASK_INVALID`，不移动流程 | 真实 Flowable 集成测试断言 assignee、当前任务和事件 |
| AC-2 | `WorkflowTaskRuntimeApi.returnTask` | 传入不存在的用户 ID | 退回不执行，流程仍停在源节点 | `TASK_INVALID`，提示目标办理人无效 | 真实 Flowable 集成测试断言源任务仍在、目标任务未创建 |
| AC-3 | `WorkflowTaskRuntimeApi.returnTask` | 目标节点没有新建任务，或本次退回新建多条任务 | 不执行指派 | `TASK_INVALID`，整体回滚 | Mockito 契约测试断言消息和 `setAssignee` 未被调用 |
| AC-4 | 退回审计 | 指定了 `targetAssigneeUserId` | `workflow_task_record` 的 RETURN 记录包含 `targetTaskDefinitionKey` 和规范化后的 `targetAssigneeUserId` | 任一步失败时记录随事务回滚 | Mockito 契约测试断言 `variablesJson` |
| AC-5 | 兼容路径 | 不传 `targetAssigneeUserId` | 退回行为与改动前一致 | 原有失败语义不变 | 既有 5 个退回引擎测试和 19 个 Mockito 契约测试全部通过 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| D-1 | AC-1、AC-5 | `ReturnWorkflowTaskCommand` 新增可选 `targetAssigneeUserId`；不新增访问控制码，调用条件仍是 `workflow:task:return` 加当前任务可办理 | `mango-workflow-api` 命令和 `WorkflowTaskRuntimeApi` 说明 | 删除字段和对应读取逻辑，旧调用不受影响 |
| D-2 | AC-1、AC-2 | 目标用户先通过 `resolveUserId` 解析为 `identity_user.id`，再通过 `TenantMemberProvider` 校验当前机构启用成员；规范化后写入 assignee 和审计变量 | `WorkflowTaskRuntimeService` 的 `resolveReturnTargetAssignee` | 删除校验和字段处理，恢复原退回路径 |
| D-3 | AC-1、AC-3 | 退回前记录目标节点已有任务 ID；退回推进后只接受“本次新建”的唯一任务，避免并行分支下误指派已有任务 | `WorkflowReturnTargetSupport` 的 `targetTaskIds`、`requireNewTargetTask`；`WorkflowTaskRuntimeService.assignReturnedTask` | 删除新参数分支，恢复原退回路径 |
| D-4 | AC-1、AC-4 | 指派后调用 `syncCurrentAssignees` 和 `refreshCurrentTasksAndReturn`，保证参与人投影、业务当前任务和通知载荷一致；失败由现有事务边界整体回滚 | `WorkflowTaskRuntimeService` 退回事务 | 同等回退 |
| D-5 | AC-1、AC-3 | 将可选办理人解析、目标前置任务快照和新生任务指派收敛到 `WorkflowReturnTaskAssigner`，避免 `WorkflowTaskRuntimeService` 类复杂度越过既有基线，同时保持退回主流程可读 | `WorkflowTaskRuntimeService`、`WorkflowReturnTaskAssigner` | 删除新类并把逻辑内联回退回方法 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| IM-01 | D-1 | 1 | `mango-workflow-api` 的 `ReturnWorkflowTaskCommand.java`、`WorkflowTaskRuntimeApi.java` | 契约编译通过，字段有中文文档 |
| IM-02 | D-2、D-4、D-5 | 2 | `WorkflowTaskRuntimeService.java`、`WorkflowReturnTaskAssigner.java` | 无效用户、非成员、成功指派和回滚测试通过；架构门禁 newIssues=0 |
| IM-03 | D-3 | 3 | `WorkflowReturnTargetSupport.java` | 新建唯一任务识别和多任务拒绝测试通过 |
| IM-04 | M08 | 4 | Workflow README、能力地图、业务接入指南、变更记录 | 四类能力文档审计通过 |
| IM-05 | M10、M11 | 5 | `WorkflowTaskRuntimeServiceImplIntegrationTest.java`、`WorkflowTaskRuntimeServiceIntegrationTest.java` | 契约测试和真实 Flowable 集成测试通过 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| AC-1、AC-5 | M11 真实 Flowable + H2 | `mvn -f mango/pom.xml -pl :mango-workflow-api,:mango-workflow-core test` | 通过 | 108 tests, 0 failures；`returnTaskAssignsSpecifiedTargetAssigneeToNewRuntimeTask` 通过 |
| AC-2 | M11 真实 Flowable + H2 | 同上 | 通过 | `returnTaskRejectsInvalidTargetAssigneeWithoutMovingRuntimeTask` 通过 |
| AC-3 | M11 Mockito 契约测试 | 同上 | 通过 | `returnTaskRejectsSpecifiedAssigneeWhenTargetNodeHasMultipleTasks` 通过 |
| AC-4 | M11 Mockito 契约测试 | 同上 | 通过 | `returnTaskAssignsSpecifiedTargetAssigneeInSameMutation` 断言审计变量 |
| AC-5 | 既有回归测试 | 同上 | 通过 | 既有退回引擎测试和契约测试全部通过 |
| 全部 | M09 编译与静态检查 | `mvn -f mango/pom.xml -pl :mango-workflow-api,:mango-workflow-core verify` | 通过 | BUILD SUCCESS；Checkstyle 0 violations；PMD 仅存量 MagicNumber |
| 全部 | M08 能力文档 | `node mango-pmo/tools/audit-module-readmes.mjs`、`node mango-pmo/tools/audit-readme-source-facts.mjs` | 通过 | Workflow README OK |
| 全部 | 测试质量门禁 | `node mango-pmo/tools/test-quality-check.mjs --base origin/main` | 通过 | Test quality PASS: 2 file(s) |

## 7. 例外与剩余风险

- M10 未单独启用：新增校验和事务分支依赖 Flowable 任务创建、参与人投影和工作流事务，由 M11 的真实 Flowable 集成测试和 Mockito 契约测试覆盖，未拆纯单元测试。
- M12 未单独启用：本次没有新增 HTTP 入口，`WorkflowTaskController` 仍只透传 `ReturnWorkflowTaskCommand`；契约由 Java API 编译和 M11 服务入口测试覆盖。
- M13 不适用：无前端页面或浏览器交互变化。
- M15 不适用：PR 远端 head、文件清单和检查状态由提交 Skill 回读，不作为本 PR 的交付验收证据。
- M16 未安排人工验收：服务入口、事务回滚和真实 Flowable 任务创建已由自动化覆盖；未做浏览器或真实业务单据端到端走查。
- 首版只支持“本次退回在目标节点新建唯一任务”；并行、多实例或会签目标节点会以 `TASK_INVALID` 失败并回滚，后续如需支持多任务，需要另定 `targetAssigneeUserIds` 或节点策略语义。
- 退回发起人可以在目标节点候选范围之外指定当前机构启用成员；这是本特性语义，调用条件没有扩大，目标用户仍受当前机构成员校验约束。
