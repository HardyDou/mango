# 标准交付记录

> **结论：** 平台派单任务可以交回节点候选池，业务不再被「已被认领」和「非认领获得」两段判定夹死；释放后同一任务不再自动派单，承接人需要的「改派、可选办理人、承办人来源」一并提供，Flowable `ASSIGNEE_` 取值统一为登录用户名。

## 1. 元数据

- 任务 ID：ISSUE-1275-TASK-RELEASE
- 交付模式：STANDARD
- 需求影响：L2 - 新增公开接口 `reassign`、`assignee-options`，改变 `unclaim` 授权口径、自动派单写入值和转办目标校验，影响审批任务容错动作与审计口径；不改变数据库结构
- 方案风险：L2 - 触及任务运行时、自动派单推进、台账写入与 Flowable 办理人取值；错误会导致任务被派回原承办人或无人可领
- 最终风险：L2
- 工作区决策：CREATE（`/data/Mango/worktrees/mango-issue-1275`，分支 `fix/workflow-task-release-1275`）

## 2. 目标与范围

- 目标：让「本人认领」和「平台派单」两类任务都能由承办人释放回候选池，并给管理侧提供改派/回收与可选办理人能力，解除 Mango Issue #1275 的阻塞。
- 成功条件：平台派单任务 `unclaim` 成功并回池；释放后推进流程不再派回原承办人；不可释放来源返回带原因的明确提示；管理员能改派或回收；引擎 `ASSIGNEE_` 只写登录用户名。
- 处理范围：`mango-workflow-api`（枚举、命令、VO、接口）、`mango-workflow-core`（运行时服务、来源解析、候选补齐、自动派单抑制、审批人解析规范化）、`mango-workflow-starter`（控制器与权限码）、`mango-ui/packages/workflow`（类型、可选办理人与设计器动作开关）、Workflow README、升级指南。
- 不处理范围：不改 `WorkflowTaskClaimStatus` 枚举；不新增等价 `unassign` 接口；不做超时回收、自动顺位/跳过、弃权语义、跨实例派单抑制；不引入节点主管角色；不改造 `isAdminUser` 的 `admin` 用户名判定。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| SR-1 | 承办人 | 任务由平台自动派单（轮询、最少任务、亲和、节点亲和任一策略） | `unclaim` 成功，`ASSIGNEE_` 清空，候选人重建 | 无候选人时失败并保留原承办人 | 真实 Flowable：派单任务释放后 assignee 为空且候选链含原成员 |
| SR-2 | 承办人 | 任务已释放 | 后续 `advanceRuntimeTasks` 不再自动派单 | 不重复写入 assignee | 真实 Flowable：推进后 assignee 仍为空 |
| SR-3 | 候选人或管理员 | 释放后的池任务 | 可以被领取 | — | 真实 Flowable：另一候选人 `claim` 成功 |
| SR-4 | 承办人 | 任务来源为转办、改派、退回指定、按人展开、流程定义指定、发起人/表单指定、空办理人兜底 | `unclaim` 拒绝，提示包含具体来源原因 | 业务码 3663；节点启用转办时追加转办提示 | 单元测试断言提示包含「当前任务不支持释放：任务由他人转办给你」 |
| SR-5 | 承办人 | 任务没有承办人 | 返回 3662「任务未被指派」 | 不再返回含糊的「只能释放自己认领的任务」 | 单元测试 |
| SR-6 | 管理员 | `POST /reassign` 传 `targetUserId` | 任务改派给目标人，台账记录原承办人与目标人 | 目标非当前租户启用成员返回 3664 | 集成测试与控制器契约 |
| SR-7 | 管理员 | `POST /reassign` 传 `pool=true` | 任务回收进候选池 | 非可释放来源返回 3663 及原因 | 核心服务分支 |
| SR-8 | 业务页面 | `GET /assignee-options?taskId=` | 返回当前时候选成员（用户 ID、用户名、显示名、是否当前承办人） | 无候选返回空集合；非承办人/候选人/管理员返回 3665 | 控制器契约测试 |
| SR-9 | 消费方 | 读取任务视图 | `assignSource` 返回承办人来源 | 未指派为空 | VO 字段断言 |
| SR-10 | 平台 | 任意写入 `ASSIGNEE_` | 统一写登录用户名，解析不出回落用户 ID | — | 真实 Flowable：`admin`、`finance` 而非 `1001`、`1002` |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| D-1 | SR-1、SR-4、SR-9 | 新增 `WorkflowTaskAssigneeSource`，`releasable` 只对 `SELF_CLAIM`、`AUTO_ASSIGN` 为真，不可释放来源带可读原因；补齐 `claim`、`autoAssign`、`transfer`、`returnTask`、`claimIfUnassigned`、`resolveEmptyRuntimeAssignee`、`applyResolvedRuntimeUsers` 的写入点 | `WorkflowTaskAssigneeSource`、`WorkflowTaskRuntimeService` | 删除枚举与写入点 |
| D-2 | SR-1、SR-4 | 新增 `WorkflowTaskAssigneeSourceResolver`：任务级变量 `mangoAssignSource` 为准，缺失时按 `workflow_task_record` 该任务最近一条 `CLAIM`/`AUTO_ASSIGN` 推导，推不出按 `DEFINITION` | `WorkflowTaskAssigneeSourceResolver` | 删除集中解析 |
| D-3 | SR-1、SR-2 | `unclaim` 释放只清 assignee、重建候选、写 `mangoReleasedToPool`；`resolveRuntimeTask` 对已释放任务只补候选不派单 | `WorkflowTaskRuntimeService#releaseToPool`、`#resolveRuntimeTask` | 恢复原 `unclaim` |
| D-4 | SR-1 | 释放前 `linkNodeCandidates` 按节点配置解析候选并补身份链，解析为空且无既有候选时 fail-closed | `WorkflowTaskRuntimeService#ensureReleaseCandidates` | 去掉候选检查 |
| D-5 | SR-6、SR-7 | 新增 `POST /workflow/tasks/reassign`（`workflow:task:reassign`），`targetUserId` 与 `pool` 二选一，改派要求当前租户启用成员，`pool` 沿用可释放来源白名单 | `ReassignWorkflowTaskCommand`、`IWorkflowTaskRuntimeService`、控制器 | 下线接口 |
| D-6 | SR-8 | 新增 `GET /workflow/tasks/assignee-options`（`workflow:task:list`），复用 `WorkflowAssigneeResolver` 与租户成员过滤 | 同上 | 下线接口 |
| D-7 | SR-10 | `WorkflowAssigneeResolver` 把办理人标识规范成登录用户名（运行时表达式原样保留），`autoAssign`、空办理人兜底、运行时单人解析、转办、退回指定统一走同一 key | `WorkflowAssigneeResolver`、`WorkflowReturnTaskAssigner` | 恢复原标识写入 |
| D-8 | SR-3 | `WorkflowRuntimeCandidateLinker` 只把 `CANDIDATE` 身份当候选，避免 assignee 身份被误判为已有候选导致承办人释放后拿不到候选身份 | `WorkflowRuntimeCandidateLinker` | 恢复原判定 |
| D-9 | 兼容 | `mangoClaimedFromCandidate` 保留写入但不再参与授权；`WorkflowTaskVO.unclaimable` 与 `unclaim` 共用 `requireReleasable`；节点动作新增 `unclaim`（默认开启） | `WorkflowNodeActionConfigResolver`、`fillClaimState` | 恢复原字段语义 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| IMPL-1 | D-1、D-5、D-6 | 1 | `mango/mango-platform/mango-workflow/mango-workflow-api/**`（`WorkflowTaskAssigneeSource`、`ReassignWorkflowTaskCommand`、`WorkflowTaskAssigneeVO`、`WorkflowCode`、`WorkflowTaskAction`、`WorkflowTaskVO`、`WorkflowTaskRuntimeApi`） | 新枚举、命令、VO、业务码与接口声明编译通过 |
| IMPL-2 | D-2 | 2 | `mango/mango-platform/mango-workflow/mango-workflow-core/src/main/java/io/mango/workflow/core/support/WorkflowTaskAssigneeSourceResolver.java` | 来源解析支持任务级变量与台账推导 |
| IMPL-3 | D-3、D-4、D-9 | 3 | `mango/mango-platform/mango-workflow/mango-workflow-core/src/main/java/io/mango/workflow/core/service/impl/WorkflowTaskRuntimeService.java` | 释放、抑制再派单、改派、可选办理人、`unclaimable` 口径完成 |
| IMPL-4 | D-7、D-8 | 4 | `WorkflowAssigneeResolver.java`、`WorkflowReturnTaskAssigner.java`、`WorkflowRuntimeCandidateLinker.java`、`WorkflowNodeActionConfigResolver.java` | 办理人 key 统一、候选判定与节点动作开关完成 |
| IMPL-5 | D-5、D-6 | 5 | `mango/mango-platform/mango-workflow/mango-workflow-starter/**/WorkflowTaskController.java` | 两个新接口与权限码生效 |
| IMPL-6 | SR-1~SR-10 | 6 | `mango-workflow-core/src/test/**`（`WorkflowTaskRuntimeServiceIntegrationTest`、`WorkflowTaskRuntimeServiceImplIntegrationTest`、`WorkflowNodeActionConfigResolverTest`、引用变更的转换器/退回引擎测试） | 单元、集成与真实 Flowable 用例通过 |
| IMPL-7 | D-5、D-6、SR-9 | 7 | `mango-ui/packages/workflow/src/api/workflow.ts`、`WorkflowNodeCapabilityConfig.vue` | 类型、接口封装与设计器动作开关可用 |
| IMPL-8 | SR-1~SR-10 | 8 | `mango/mango-platform/mango-workflow/README.md`、`mango-docs/guides/operations/workflow-task-release-upgrade.md`、`mango-docs/guides/business-integration/workflow-business-approval.md` | 能力、失败语义、升级口径与业务接入建议与实现一致 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| SR-1、SR-2、SR-3 | M11 真实 Flowable 集成测试 | `mvn -o -pl mango-platform/mango-workflow/mango-workflow-core org.apache.maven.plugins:maven-surefire-plugin:3.2.5:test -Dtest=WorkflowTaskRuntimeServiceIntegrationTest#autoAssignedTaskCanBeReleasedToPoolAndIsNotDispatchedAgain` | PASS：释放后 assignee 为空、候选链含 `admin`/`finance`、推进不再派单、另一候选人领取成功 | Surefire 报告 |
| SR-4 | M11 真实 Flowable 集成测试 | 同上，`#transferredTaskCannotBeReleasedByItsNewOwner` | PASS：提示包含「当前任务不支持释放：任务由他人转办给你」与「如需移交请使用转办」，assignee 保持 `finance` | Surefire 报告 |
| SR-4、SR-5 | M11 单元/集成测试 | `... -Dtest=WorkflowTaskRuntimeServiceImplIntegrationTest#unclaimRejectsNonReleasableSourceWithExplicitReason` | PASS | Surefire 报告 |
| SR-1、SR-9 | M11 单元/集成测试 | 同上，`#claimMarksSelfClaimAndUnclaimReleasesPlatformAssignedTask` | PASS：认领写 `SELF_CLAIM`，释放写 `mangoReleasedToPool` | Surefire 报告 |
| SR-10 | M11 真实 Flowable 集成测试 | 同上，`#nodeAffinityRestoresFirstNodeHandlerAfterReturnAndUsesClaimWhenNoHistory`、`#autoAssignRecordAttributesRealAssigneeInsteadOfTriggeringUser` | PASS：`ASSIGNEE_` 为 `admin`/`finance`，台账仍记录用户 ID 1002 | Surefire 报告 |
| 全部 | 模块测试基线 | `mvn -o -pl mango-platform/mango-workflow/mango-workflow-api,mango-platform/mango-workflow/mango-workflow-core -am test` 后执行 `maven-surefire-plugin:3.2.5:test` | PASS：228 tests，0 failures，0 errors，4 skipped（均为存量跳过） | `BUILD SUCCESS` |
| 前端 | 组件构建与类型检查 | `pnpm --filter @mango/workflow build` | 待执行：当前环境 Node 26.13.1 与仓库要求 22.23.1 不符，pnpm 拒绝安装依赖，worktree 无 `node_modules` | 需在 Node 22.23.1 环境补跑 |

## 7. 例外与剩余风险

- 前端验证未执行：当前环境 Node 版本被 pnpm 拦截，worktree 未安装依赖；`assignSource` 类型、`reassign`/`assignee-options` 封装和设计器动作开关只做了源码改动，需在 Node 22.23.1 环境跑 `pnpm --filter @mango/workflow build` 与组件测试。
- `mvn test` 在 `mango-workflow-core` 上因模块继承的 Surefire 2.17 不会发现 JUnit 5 用例（存量问题），本次用 `maven-surefire-plugin:3.2.5:test` 显式执行并记录结果；未改动仓库构建配置。
- 会签、依次审批节点释放后可能停在池中等待新候选人，平台不做超时回收，运维按升级指南由管理员代办或改派。
- `reassign` 的管理员判定沿用 `isAdminUser()`（`username == admin`），按节点主管或流程管理员角色授权登记为后续独立任务。
- 存量任务的来源按台账推导，无法推导时按不可释放处理；升级前已卡住的派单任务在升级后可直接释放。
