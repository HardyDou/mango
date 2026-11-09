# 标准交付记录

> Issue #1179 工作流退回静默成功修复

## 1. 元数据

- 任务 ID：Issue #1179
- 交付模式：STANDARD
- 需求影响：L2：修改公开任务运行时接口 `WorkflowTaskRuntimeApi.returnTask` 的成功语义与响应动作标识。
- 方案风险：L2：在退回事务内新增两次运行时任务校验，失败时回滚整笔退回。
- 最终风险：L2
- 工作区决策：CREATE：`/Users/hardy/Work/mango-issue-1179`，分支 `fix/issue-1179-workflow-return`
- 决策复核：`AGENT_ONLY`；未指定外部同行评审。

## 2. 目标与范围

- 目标：退回不再静默成功。要么目标节点真的产生待办任务，要么接口以 `TASK_INVALID` 失败并回滚；同时修正成功响应里把退回报成 `COMPLETE` 的动作标识。
- 成功条件：退回后 `ACT_RU_TASK` 的当前任务定义 key 等于目标 key；目标节点没有待办任务或流程仍停原节点时接口失败；成功响应 `actionResult=RETURN`。
- 处理范围：`WorkflowTaskRuntimeService.returnTask` 及其返回构造、`WorkflowTaskRuntimeApi.returnTask` 契约注释、业务接入指南与历史变更索引、workflow-core 测试。
- 不处理范围：不新增回退连线或目标节点动作配置；不修改 `reject`、撤回、自动派单、事件类型、权限码、租户隔离、前端页面和数据库结构。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| U1179-01 | `POST /workflow/tasks/return` | 当前任务启用 `returnTask`，`targetTaskDefinitionKey` 为已完成历史用户任务 | 引擎把执行移动到目标节点并生成待办任务 | 目标节点无待办任务时返回 `TASK_INVALID`，事务回滚 | 真实 Flowable 引擎测试通过；服务校验在无待办时抛 `BizException` |
| U1179-02 | 同上 | 目标节点审批人为空被自动通过，流程回到原节点 | 识别为自相矛盾的退回结果并失败 | 返回 `TASK_INVALID`，提示目标节点无待办且流程仍在原节点 | 服务级测试断言失败且不发布 `workflow.task.advanced` |
| U1179-03 | 退回成功响应 | 退回真实生效 | `actionResult` 为 `RETURN` | 不适用 | 服务级测试断言 `WorkflowTaskAction.RETURN` |
| U1179-05 | 业务精确调用形态 | 显式 `targetTaskDefinitionKey` + `variables` 含 `targetTaskDefinitionKey`/`decision` | 真实引擎与真实 Mapper 下任务移动到目标节点 | 失败时 `TASK_INVALID` | `WorkflowTaskRuntimeServiceIntegrationTest.returnTaskWithExplicitTargetAndClientVariablesMovesRuntimeTask` 通过 |
| U1179-06 | 目标为网关或汇聚 | `targetTaskDefinitionKey` 为网关/汇聚节点 | 解析到上游最近的已完成用户任务后退回 | 多个候选或无候选时 `TASK_INVALID` | 真实引擎用例覆盖汇聚目标解析、汇聚直通回原节点、分支网关无上游时失败 |
| U1179-04 | 业务接入方 | 阅读接入指南 | 明确无需回退连线、无需目标节点动作配置、无需历史活动 ID、目标必须是已完成历史用户任务 | 文档与实现一致 | `node mango-pmo/tools/check-business-guides.mjs` 通过 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| D1179-01 | U1179-01 | 保留 Flowable `changeState` 退回方式（不依赖 sequenceFlow）；变更活动状态后立即按 `processInstanceId + taskDefinitionKey` 统计待办任务，为 0 则 `Require.isTrue` 失败并回滚 | `WorkflowTaskRuntimeService.returnTask` | 回退新增的 `ensureReturnTargetTaskCreated` 调用 |
| D1179-02 | U1179-02 | 流程推进后再校验一次：流程未结束、目标节点无任务且原节点有任务时失败，避免与“未移动”混淆 | `WorkflowTaskRuntimeService.returnTask` | 回退 `ensureReturnResultLeftSourceNode` 调用 |
| D1179-03 | U1179-03 | `toCompleteResult` 增加动作参数，`complete` 传 `COMPLETE`、退回传 `RETURN`；枚举值已存在于 `WorkflowTaskAction` 与前端联合类型，无需新增契约 | `WorkflowTaskRuntimeService.toCompleteResult` | 回退方法签名并固定 `COMPLETE` |
| D1179-04 | U1179-04 | 文档只补充既有实现的使用约束与失败语义，不新增配置项、权限码或页面 key | `mango-docs/guides/business-integration/workflow-business-approval.md` | 回退文档章节 |
| D1179-05 | U1179-01/U1179-02 | 退回目标解析、历史校验和生效校验下沉到 `core/support/WorkflowReturnTargetSupport`：`WorkflowTaskRuntimeService` 已接近 checkstyle 2000 行上限，且退回逻辑本身是一个内聚类；不新增依赖，h2 已由现有测试依赖传递引入 | `WorkflowReturnTargetSupport`、`WorkflowTaskRuntimeService` | 回退新类并把方法收回 service |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---:|---|---|
| I1179-01 | D1179-01/D1179-02/D1179-03/D1179-05 | 1 | `mango/mango-platform/mango-workflow/mango-workflow-core/src/main/java/io/mango/workflow/core/support/WorkflowReturnTargetSupport.java`、`.../service/impl/WorkflowTaskRuntimeService.java` | 退回带两处后置校验，响应动作标识可区分，service 仍在 2000 行以内 |
| I1179-02 | D1179-01/D1179-04 | 2 | `mango/mango-platform/mango-workflow/mango-workflow-api/src/main/java/io/mango/workflow/api/WorkflowTaskRuntimeApi.java` | 接口注释写明约束与失败语义 |
| I1179-03 | D1179-01/D1179-03 | 3 | `mango-workflow-core/src/test/java/.../WorkflowTaskRuntimeServiceImplIntegrationTest.java`、`.../engine/WorkflowReturnTaskEngineTest.java` | 覆盖成功、两处失败和真实引擎退回基线 |
| I1179-04 | D1179-04 | 4 | `mango-docs/guides/business-integration/workflow-business-approval.md`、`mango-docs/changelog/business-integration/workflow-business-approval.md` | 接入约束与变更索引更新 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| U1179-01 | 真实引擎集成测试 | `mvn -pl mango-platform/mango-workflow/mango-workflow-core test -Dtest=WorkflowReturnTaskEngineTest` | PASS：排他网关分支汇聚流程中，`moveActivityIdTo` 退回到历史节点后 `ACT_RU_TASK` 定义 key 为 `risk_specialist_review`；移动到非活动节点抛异常 | `WorkflowReturnTaskEngineTest` 2 个用例通过（本会话 surefire 输出） |
| U1179-01/U1179-03 | 服务级集成测试 | `mvn -pl mango-platform/mango-workflow/mango-workflow-core test -Dtest=WorkflowTaskRuntimeServiceImplIntegrationTest` | PASS：15 个用例通过，含 `returnTaskMovesToPreviousHistoricNode...`、`returnTaskFailsWhenTargetNodeKeepsNoPendingTask` | 本会话 surefire 输出 |
| U1179-02 | 服务级集成测试 | 同上 | PASS：`returnTaskFailsWhenProcessStaysOnSourceNode` 断言 `退回未生效` 且不发布 `workflow.task.advanced` | 本会话 surefire 输出 |
| U1179-05 | 端到端集成测试（真实 Flowable 引擎 + 真实 Mapper） | `mvn -pl mango-platform/mango-workflow/mango-workflow-core test -Dtest=WorkflowTaskRuntimeServiceIntegrationTest` | PASS：3 个用例通过，显式目标与业务变量组合下任务从 `finance_approve` 退回到 `manager_approve`，`actionResult=RETURN` | 本会话 surefire 输出 |
| U1179-06 | 真实引擎集成测试 | `-Dtest=WorkflowReturnTaskEngineTest` | PASS：5 个用例，含汇聚网关直通回原节点、汇聚目标解析到初审、分支网关无上游时失败 | 本会话 surefire 输出 |
| 模块回归 | 模块全量测试与质量门禁 | `mvn -pl mango-platform/mango-workflow/mango-workflow-api,mango-platform/mango-workflow/mango-workflow-core,mango-platform/mango-workflow/mango-workflow-starter verify` | PASS：workflow-core 95 个测试通过（4 个既有跳过）、starter 39 个测试通过 | 本会话 Maven 输出 |
| 架构门禁 | changed 模式架构门禁 | `mvn -f mango/pom.xml -pl <api,core> -Dmango.architecture.skip=false -Dmango.architecture.mode=changed -Dmango.architecture.requireFullReactor=false mango:architecture` | PASS：`blocking=0`；同模块存量 `MANGO-ARCH-BEAN-002` 由债务预算承载，未扩大 | 本会话 `mango-architecture-report.json` |
| Mango 专项检查 | `mango:check` 新增违规对比 | `mvn -f mango/pom.xml -pl mango-platform/mango-workflow/mango-workflow-core mango:check` | 新增违规数 443，与 `main` 基线一致（未被本次改动增加）；该目标在 `main` 也因仓库存量违规而 FAIL | 本会话命令输出对比 |
| 测试质量门禁 | 测试改动检查 | `node mango-pmo/tools/test-quality-check.mjs --base origin/main` | PASS：2 个测试文件 | 本会话命令输出 |
| U1179-04 | 文档门禁 | `node mango-pmo/tools/check-business-guides.mjs` | PASS：5 份指南 | 本会话命令输出 |

## 7. 例外与剩余风险

- 未用业务仓 `mango-baohan/mango-baohan-risk` 的真实库和真实流程定义复现原始现场；本次用同等流程形态（排他网关 + 空默认分支 + 汇聚 → 复审）在真实 Flowable 7.0.0 引擎上验证。原始现场若另有数据或版本差异，仍以业务侧重跑退回为准。
- 原始现场“静默成功”的触发条件未直接命中：真实引擎实验证明移动到非活动节点会抛异常，因此原因只可能是退回后被自动推进或调用方读取了旧快照（含调用方事务回滚）。本次按 fail-closed 修复根因类别；业务侧复现时接口现在会明确失败并给出目标节点与原节点。
- 业务在同版本发布前的构建上仍会看到静默成功：修复未发布前无法观察到新错误语义，需升级到包含本 PR 的版本后重跑。
- `mvn -pl ... mango:check` 在 `main` 也存在 443 条存量违规而整体 FAIL，属于仓库既有状态；本次只保证未新增违规。
- 描述统计型 SpotBugs 存量告警（`EI_EXPOSE_REP*`、`BX_UNBOXING_IMMEDIATELY_REBOXED` 等）在本次改动前已存在，未由本任务扩大。
- 前端未改动：`@mango/workflow` 已包含 `RETURN` 动作类型，任务详情页失败时只提示错误、不跳转，无需新增界面。
