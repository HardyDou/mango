# 标准交付记录

## 1. 元数据

- 任务 ID：Workflow 节点亲和跨实例复用（Issue #1243）
- 交付模式：STANDARD
- 需求影响：L2 - 新增审批节点配置项和业务申请字段，改变 AUTO + NODE_AFFINITY 的派单结果；涉及跨流程实例读取历史办理人
- 方案风险：L2 - 跨 workflow-api/core、workflow 前端包、Flyway 迁移和接入文档；选错作用域层级会派给错误办理人
- 最终风险：L2
- 工作区决策：CREATE（`/Users/hardy/Work/mango-workflow-affinity-scope`，分支 `feat/workflow-affinity-scope`）

## 2. 目标与范围

- 目标：同一业务实体（项目）修改后重新提交产生新流程实例时，同一审批节点自动派回上次办理人；不同实体之间零串人。
- 成功条件：当前实例 → 同一业务键 → 同一亲和键三层按由近及远命中，命中者必须在当前候选集；三层无效时按 `nodeAffinityFallback` 回退；默认配置行为与 1.7.0 完全一致。
- 处理范围：`mango-workflow-api` 新增作用域枚举和 `affinityKey` 入参，`mango-workflow-core` 节点配置、选择器、运行时派单和审计，Flyway `V4` 迁移，`mango-ui/packages/workflow` 设计器复用范围配置与包 README，Workflow README、业务接入指南、变更索引。
- 不处理范围：不改 `AFFINITY` 语义（无节点维度，仍是实例级）；不合并申请中心按 `businessKey` 的“最新申请”分组；不新增权限码、不改驳回/撤回终止实例的语义；不回填历史 `affinity_key`；不按节点名称或节点类型跨版本匹配节点。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| AC-1 | 审批中心自动派单 | `nodeAffinityScope=PROCESS_INSTANCE`（默认或缺省），同一实例同节点有历史办理人 | 派回该实例同节点最早的历史办理人 | 不在候选集时不强行指派 | 与 1.7.0 结果一致，单测和集成测试通过 |
| AC-2 | 审批中心自动派单 | 作用域 `BUSINESS_KEY`，当前实例无历史，同 `businessType + businessKey` 的其他实例有办理记录 | 派回该业务键下最近实例的最早历史办理人 | 历史人员不在候选集时继续回溯，最终走回退策略 | 选择器集成测试覆盖当前实例、业务键两层 |
| AC-3 | 审批中心自动派单 | 作用域 `AFFINITY_KEY`，业务传了稳定 `affinityKey`，最近实例办理人已失效、更早实例办理人仍有效 | 回溯到更早实例的有效办理人 | 200 条记录内无有效候选时按 `nodeAffinityFallback` | 选择器测试覆盖回溯命中与回退分支 |
| AC-4 | 审批中心自动派单 | 不同 `affinityKey`、不同 `businessType` 或不同租户 | 不复用对方办理人 | 落回待领取或配置的回退策略 | 隔离测试覆盖业务类型、亲和键、租户 |
| AC-5 | 流程设计器 | 节点 `assignmentMode=AUTO`、`autoAssignmentStrategy=NODE_AFFINITY` | 展示“复用范围”三档：仅当前流程 / 同业务单号 / 同亲和键 | 旧配置缺字段时按 `PROCESS_INSTANCE` | 组件测试看到三档文案 |
| AC-6 | 升级已有环境 | 执行 Flyway 迁移 | 存在 `workflow_business_apply` 的库新增可空 `affinity_key` 和组合索引；该表缺失的库跳过，不中断升级；不修改历史数据 | 旧代码路径不读该列，回滚只需撤销配置 | 迁移契约测试和真实 MySQL 升级测试覆盖 |
| AC-7 | 审计回查 | 任一次 AUTO + NODE_AFFINITY 派单 | AUTO_ASSIGN 记录含 `nodeAffinityScope`、`nodeAffinityHit`、`nodeAffinityFallback` | 未命中时 `nodeAffinityHit=FALLBACK` | 集成测试断言审计字段 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| D-1 | AC-1/AC-2/AC-3 | 作用域是“允许的最远层级”：`PROCESS_INSTANCE` → `BUSINESS_KEY` → `AFFINITY_KEY`，运行时逐层尝试、命中即停；作用域只在 `NODE_AFFINITY` 生效 | `WorkflowNodeAffinityScope`、`WorkflowApprovalNodeConfig`、`WorkflowAutoAssignmentSelector` | 删除配置字段，选择器回退单层查询 |
| D-2 | AC-2/AC-3 | 跨实例查询排除当前实例（第一层已覆盖），按 `apply.created_time desc, apply.id desc, record.created_time asc, record.id asc` 排序，`limit 200`；查询同时限定 `tenant_id` 与 `business_type` | `WorkflowAutoAssignmentSelector` | 删除跨实例查询方法 |
| D-3 | AC-3 | 候选集是唯一资格来源：历史办理人必须命中当前候选，否则继续回溯；三层无有效候选才调用现有 `nodeAffinityFallback` | `WorkflowAutoAssignmentSelector`、`WorkflowTaskRuntimeService#autoAssign` | 回退分支保持原实现 |
| D-4 | AC-4 | 亲和键由业务在 `CreateWorkflowBusinessApplyCommand` / `StartBusinessWorkflowCommand` 传入，`varchar(128)`、创建后不可改，随 `WorkflowBusinessApplyVO` 返回；不接受入参指定租户 | `WorkflowBusinessApplyService`、`WorkflowProcessService`、实体和 VO | 删除字段和迁移列 |
| D-5 | AC-6 | 新增 Flyway `V4__workflow_node_affinity_scope.sql`：按 `information_schema` 条件加可空列和 `(tenant_id, business_type, affinity_key, process_instance_id)` 索引，表或列已存在时跳过，不回填；保证部分历史库和手工加列环境可重复执行 | `db/migration/workflow/V4__workflow_node_affinity_scope.sql` | 列可空且无旧读者，保留即无害 |
| D-6 | AC-7 | AUTO_ASSIGN 审计追加 `nodeAffinityScope`、`nodeAffinityHit`、`nodeAffinityFallback` | `WorkflowTaskRuntimeService` | 删除审计键 |
| D-7 | AC-5 | 设计器在 `AUTO + NODE_AFFINITY` 时展示复用范围，稳定锚点 `workflow.node-affinity-scope`，默认 `PROCESS_INSTANCE` | `WorkflowNodeApprovalConfig.vue`、`api/workflow.ts` | 回退前端组件和类型 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| T-1 | D-1/D-2/D-3/D-6 | 1 | `mango-workflow-api` 枚举、`WorkflowApprovalNodeConfig`、`WorkflowAutoAssignmentSelector`、`WorkflowTaskRuntimeService` | 三层选择、回溯、审计编译通过 |
| T-2 | D-4 | 2 | `CreateWorkflowBusinessApplyCommand`、`StartBusinessWorkflowCommand`、`WorkflowBusinessApplyEntity`、`WorkflowBusinessApplyVO`、`WorkflowBusinessApplyService`、`WorkflowProcessService` | 亲和键入参可创建并回读 |
| T-3 | D-5 | 3 | `V4__workflow_node_affinity_scope.sql` | 迁移契约测试通过 |
| T-4 | D-1/D-2/D-3 | 4 | `WorkflowAutoAssignmentStrategyTest`、`WorkflowTaskRuntimeServiceImplIntegrationTest`、`WorkflowTaskRuntimeServiceIntegrationTest` | 选择器、隔离、回溯、端到端用例通过 |
| T-5 | D-7 | 5 | `WorkflowNodeApprovalConfig.vue`、`api/workflow.ts`、组件测试 | 组件测试通过 |
| T-6 | D-1/D-4/D-5 | 6 | Workflow README、`@mango/workflow` README、业务接入指南、变更索引 | 能力说明与实现一致 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| AC-1/AC-2/AC-3/AC-4/AC-7 | M11 集成测试（选择器 + H2） | `mvn -o -pl mango-platform/mango-workflow/mango-workflow-core -Dtest=WorkflowTaskRuntimeServiceImplIntegrationTest test` | PASS（23 tests） | `mango-workflow-core/target/surefire-reports/` |
| AC-1/AC-2/AC-3/AC-4 | M11 端到端集成测试（Flowable + 真实 apply） | `mvn -o -pl mango-platform/mango-workflow/mango-workflow-core -Dtest=WorkflowTaskRuntimeServiceIntegrationTest test` | PASS（8 tests，含 `nodeAffinityReusesHandlerAcrossInstancesOfSameAffinityKeyAndKeepsProjectsIsolated`） | 同上 |
| AC-6 | M11 迁移契约测试 | `mvn -o -pl mango-platform/mango-workflow/mango-workflow-core -Dtest=WorkflowMigrationContractTest test` | PASS（6 tests，含 V4 条件执行断言） | 同上 |
| AC-6 | M11 真实 MySQL 升级 | `MANGO_DB_NAME=mango_dev_workflow_affinity_verify MANGO_DB_USERNAME=root mvn -o -pl …-core -Dtest=WorkflowMigrationUpgradeIntegrationTest test` | PASS（4 tests）：1.0.20/1.0.21-22 基线跳过 V4 不失败；当前 V1 新建库执行 V4 后 `affinity_key` 列存在；重复 migrate 为 0 | 使用一次性库 `mango_dev_506_1vi8njr`，测试前后自动 DROP |
| AC-1～AC-7 | M09 模块测试全量 | `mvn -o -pl mango-platform/mango-workflow/mango-workflow-core test` | PASS（176 tests，4 skipped 为需真实库的环境门禁用例） | 同上 |
| AC-1～AC-7 | M09 模块 verify | `mvn -o -pl mango-platform/mango-workflow/mango-workflow-core -am verify` | BUILD SUCCESS | Maven 输出 |
| AC-1～AC-7 | M09 静态检查 | `mvn -o -pl …-api,…-core pmd:check checkstyle:check` | Checkstyle 0 violations；PMD 仅剩其它文件既有未用 import，本次触及文件已清零 | `target/pmd.xml`、`target/checkstyle-result.xml` |
| AC-1～AC-7 | M09 SpotBugs | `mvn -o -pl …-api,…-core spotbugs:check` | 本次新增类 `WorkflowAutoAssignmentSelector` bugCount=0；模块内 170 条为既有基线 | `target/spotbugsXml.xml` |
| AC-1～AC-7 | M09 架构门禁 | `mango-maven-plugin:1.0.0-SNAPSHOT:architecture`（完整 Reactor） | BUILD SUCCESS | Maven 输出 |
| AC-2 | M11 申请创建持久化 | `-Dtest=WorkflowBusinessApplyServiceImplIntegrationTest` | PASS（10 tests，含亲和键入库、去空格、空值不串单） | `mango-workflow-core/target/surefire-reports/` |
| AC-2 | M11 业务发起透传 | `-Dtest=WorkflowProcessServiceImplIntegrationTest` | PASS（2 tests，`StartBusinessWorkflowCommand.affinityKey` 进入申请创建命令） | 同上 |
| AC-2/AC-4 | M12 HTTP 接口契约 | `mvn -o -pl …/mango-workflow-starter -Dtest=WorkflowAffinityKeyHttpContractTest test` | PASS（3 tests）：`POST /workflow/business-applies` 绑定并回显 `affinityKey`；`POST /workflow/processes/start-business` 透传；129 字符亲和键返回 400 且不调用服务 | `mango-workflow-starter/target/surefire-reports/` |
| AC-5 | M10 组件测试 | `pnpm -C packages/workflow test --run src/views/workflow-definition/components/workflow-designer/__tests__/WorkflowNodeApprovalConfig.spec.ts` | PASS（2 tests） | `mango-ui/packages/workflow` vitest 输出 |
| AC-5 | 前端包构建 | `pnpm -C packages/workflow build` | PASS（vite build + 类型产物生成） | `mango-ui/packages/workflow/dist` |
| AC-5 | 前端包类型检查 | `vue-tsc --noEmit -p tsconfig.json`（`mango-ui/packages/workflow`） | 无新增错误：改动文件错误数 22，与 `main` 基线相同 | 两次输出对比 |
| 全量 | M09 前端包测试 | `pnpm -C packages/workflow test` | 51 passed, 1 failed：`startProcessIcons.spec.ts` 在 `main` 同样失败，属既有基线 | `main` 同用例复现 |

## 7. 例外与剩余风险

- 节点维度按 `taskDefinitionKey` 匹配：流程定义升级时节点被删除后重建，亲和自然失效，不做名称或类型猜测。已写入 README 和接入指南。
- 回溯上限固定为 200 条记录，超过上限按回退策略处理；上限不是配置项，避免新增无消费者配置。
- L2 业务键层对“业务键每版本变化”的业务恒等于 L1；保留该层是为了业务键稳定且不打算传亲和键的接入方。
- `startProcessIcons.spec.ts` 失败为 `main` 既有问题，与本次改动无关，未纳入本次修复范围。
- 真实 MySQL 升级已执行（一次性库 `mango_dev_506_1vi8njr`，测试结束自动 DROP）。测试夹具 `src/test/resources/db/migration/workflow-1.0.20/V1__init_workflow.sql` 是仅含 5 张受影响表的精简快照，因此该路径下 V4 跳过；经 `git show v2026.07.14-maven-1.0.20-…:V1__init_workflow.sql` 核对，Maven 1.0.20 发布版 V1 已创建 `workflow_business_apply`，真实 1.0.20 库升级会正常新增列和索引，无产品影响。
- `mango:check` 全仓执行返回 BUILD FAILURE，命中项均为既有债务（`mango-infra-bootstrap`、`mango-file`、`mango-integration`、`mango-identity`、`WorkflowTaskHistoryMapper.xml` 跨模块 SQL 等），本次改动文件未出现在违规列表，故不作为本次新增门禁结论。
- HTTP 接口测试使用 standalone MockMvc，覆盖入参绑定、长度校验和响应回显，不启动完整应用上下文；权限拦截、`@ApiAccess` 资源声明和 Feign 远程契约不在本次范围。
- 外部同行评审：用户未指定，按 `AGENT_ONLY` 记录本次三视角分析，不称为人工评审或审批。
