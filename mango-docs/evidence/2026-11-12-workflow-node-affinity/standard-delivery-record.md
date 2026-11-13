# 标准交付记录

## 1. 元数据

- 任务 ID：Workflow 节点亲和自动派单
- 交付模式：STANDARD
- 需求影响：L2 - 新增公开自动派单策略和审批节点配置，影响任务派单行为、候选权限边界和审批中心设计器；不改变数据库结构
- 方案风险：L2 - 跨 workflow-api/core 和 workflow 前端包，复用任务记录查询和自动派单事务；错误会造成派给错误办理人或任务无法认领
- 最终风险：L2
- 工作区决策：CREATE（`/Users/hardy/Work/mango-workflow-node-affinity`，分支 `feat/workflow-node-affinity`）

## 2. 目标与范围

- 目标：审批节点新增“节点亲和”自动派单。同一流程实例、同一节点有历史办理人且仍在当前候选人中时，自动派回该办理人；没有历史或历史办理人不可用时，按配置回退到待领取、轮询、随机或任务量最少。
- 成功条件：节点重复进入时优先派回同节点最早历史办理人；无历史时默认待领取；回退策略可配置；候选为空仍明确失败并回滚；流程设计器可选择策略和回退方式。
- 处理范围：`mango-workflow-api` 自动派单枚举、`mango-workflow-core` 节点配置/选择器/运行时派单、`mango-ui/packages/workflow` 设计器配置和组件测试、Workflow README、前端 README、业务接入指南、能力地图和变更索引。
- 不处理范围：不新增数据库表或迁移，不改变现有 `ROUND_ROBIN`、`LEAST_TASKS`、`AFFINITY` 语义，不改变候选解析、租户隔离、权限码和审批动作。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| AC-1 | 审批中心自动派单 | 同一流程实例同一节点存在历史办理人，且仍在候选人中 | 自动派给最早历史办理人 | 不在候选范围时不强行派单 | 历史记录按 `created_time asc, id asc` 命中第一人 |
| AC-2 | 同上 | 同节点无历史办理人，`nodeAffinityFallback=CLAIM` | 保持待领取 | 候选为空仍返回 `AUTO_ASSIGN_NO_CANDIDATE` | 不写 assignee，不写自动派单审计 |
| AC-3 | 同上 | 同节点无历史办理人，回退 `ROUND_ROBIN` / `RANDOM` / `LEAST_TASKS` | 分别按轮询、随机、最少任务选择候选人 | 候选为空失败并回滚 | 三种回退均有选择结果 |
| AC-4 | 流程设计器 | 节点 `assignmentMode=AUTO`，策略 `NODE_AFFINITY` | 展示“节点亲和”和“无历史办理人”回退选项 | 旧配置缺失回退字段时保持 `CLAIM` | 组件测试看到“无历史办理人”“随机”等选项 |
| AC-5 | 旧流程定义 | 缺失 `autoAssignmentStrategy` 或 `nodeAffinityFallback` | 继续兼容 `ROUND_ROBIN` 和 `CLAIM` | 非法旧值回落默认值 | BPMN 转换测试通过 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| D-1 | AC-1/AC-2/AC-4 | 新增 `WorkflowAutoAssignmentStrategy.NODE_AFFINITY` 和 `WorkflowNodeAffinityFallback(CLAIM/ROUND_ROBIN/RANDOM/LEAST_TASKS)`；节点配置新增 `nodeAffinityFallback`，默认 `CLAIM` | `mango-workflow-api` 枚举、`WorkflowApprovalNodeConfig`、前端类型和默认配置 | 删除枚举值和配置字段，恢复旧策略 |
| D-2 | AC-1 | 复用 `workflow_task_record`，按租户、流程实例、任务定义 Key 和 `CLAIM/COMPLETE/REJECT/RETURN` 查最早办理人；不新增表，租户隔离沿用现有字段 | `WorkflowAutoAssignmentSelector` | 回退查询方法 |
| D-3 | AC-2/AC-3 | 无历史时按回退策略选择；`CLAIM` 返回空并由运行时保持未指派，同时补齐候选用户/候选组以保证可认领；`ROUND_ROBIN` 使用现有游标，`RANDOM` 使用线程本地随机，`LEAST_TASKS` 复用现有活动任务统计 | `WorkflowAutoAssignmentSelector`、`WorkflowTaskRuntimeService`、`WorkflowRuntimeCandidateLinker` | 回退选择器和服务分支 |
| D-4 | AC-5 | 旧 `autoAssignmentStrategy` 缺失继续 `ROUND_ROBIN`；`nodeAffinityFallback` 缺失或非法继续 `CLAIM` | `WorkflowDesignerBpmnConverter` | 回退兼容解析 |
| D-5 | AC-4 | 设计器在 `AUTO + NODE_AFFINITY` 时展示回退选项，稳定锚点 `workflow.node-affinity-fallback` | `WorkflowNodeApprovalConfig.vue`、workflow API 类型 | 回退前端组件和类型 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| I-1 | D-1/D-4 | 1 | `mango-workflow-api` 枚举、`WorkflowApprovalNodeConfig`、`WorkflowDesignerBpmnConverter` | 配置可序列化、反序列化和兼容旧值 |
| I-2 | D-2/D-3 | 2 | `WorkflowAutoAssignmentSelector`、`WorkflowTaskRuntimeService` | 历史命中、四种回退和无候选失败均按契约执行 |
| I-3 | D-2/D-3 | 3 | `WorkflowAutoAssignmentStrategyTest`、`WorkflowTaskRuntimeServiceImplIntegrationTest`、`WorkflowDesignerBpmnConverterTest` | 单元、H2 集成和转换测试通过 |
| I-4 | D-5 | 4 | workflow 前端类型、设计器组件和组件测试 | 选项可见、默认值正确、构建通过 |
| I-5 | AC-1~AC-5 | 5 | Workflow README、前端 README、业务接入指南、能力地图、业务变更索引 | 对外说明与实现一致，文档 checker 通过 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| AC-1/AC-2/AC-3 | 单元/选择器集成测试 | `mvn -o -pl mango-platform/mango-workflow/mango-workflow-api,mango-platform/mango-workflow/mango-workflow-core -Dtest='WorkflowAutoAssignmentStrategyTest,WorkflowTaskRuntimeServiceImplIntegrationTest,WorkflowDesignerBpmnConverterTest' -Dsurefire.failIfNoSpecifiedTests=false test` | PASS（32 用例） | 节点亲和最早办理人、无候选、随机、回退轮询和配置转换 |
| AC-1/AC-2 | 真实 Flowable 场景测试 | `mvn -o -pl mango-platform/mango-workflow/mango-workflow-api,mango-platform/mango-workflow/mango-workflow-core -Dtest='WorkflowTaskRuntimeServiceIntegrationTest#nodeAffinityRestoresFirstNodeHandlerAfterReturnAndUsesClaimWhenNoHistory' -Dsurefire.failIfNoSpecifiedTests=false test` | PASS（1 用例） | 真实流程：首次节点无历史保持待领取并补齐候选人→admin 认领完成→复审退回→初审自动派回 admin（userId 1001） |
| AC-1~AC-5 | 模块验证 | `mvn -o -pl mango-platform/mango-workflow/mango-workflow-api,mango-platform/mango-workflow/mango-workflow-core,mango-platform/mango-workflow/mango-workflow-starter verify` | PASS | workflow-core 145 tests（4 skipped）、workflow-starter 46 tests，`BUILD SUCCESS` |
| AC-4 | 前端组件测试 | `pnpm --filter @mango/workflow exec vitest run --config vitest.config.ts src/views/workflow-definition/components/workflow-designer/__tests__/WorkflowNodeApprovalConfig.spec.ts` | PASS（2 用例） | “节点亲和”“无历史办理人”“随机”选项可见 |
| AC-4 | 前端构建 | `pnpm --filter @mango/workflow build` | PASS | Vite 112 modules，产物构建成功 |
| AC-4/AC-5 | 前端格式与静态检查 | `pnpm exec prettier --check ...`、`pnpm exec eslint ...` | PASS | Prettier 全部通过；ESLint 0 errors，warning 为存量规则 |
| AC-1~AC-5 | 架构门禁 | 变更模块 `mango:architecture` + `mango:check` | PASS | `newIssues=[]`，`BUILD SUCCESS` |
| AC-5 | 文档门禁 | module README、README source facts、业务指南、能力文档和标准交付记录 checker | PASS | 全部输出 checks passed / PASS |

## 7. 例外与剩余风险

- 前端包全量测试 `pnpm --filter @mango/workflow test` 有 1 个与本改动无关的用例失败：`startProcessIcons.spec.ts` 依赖 `127.0.0.1:3000` 后端，本次环境未启动该服务；本改动相关组件测试和构建均通过。
- 历史办理人取“最早一次 CLAIM/COMPLETE/REJECT/RETURN”，不是“最近一次”。当前产品语义按用户要求的“第一次领取人”实现；后续如需改为最近办理人，应作为独立策略调整。
- 同节点并行多任务、转办/加签后的历史办理人归属未新增专门规则；当前按最早有效办理记录处理。
- 已用真实 Flowable 引擎完成“首次待领取→认领→完成→复审退回→初审自动派回”场景测试；尚未连接真实业务库 MySQL，上线前仍需做一次业务库联调。
