# 标准交付记录

> **结论：** 业务列表一次取回的当前任务快照现在带 `unclaimable`、`assignSource`、`claimTime`，逐行控制「释放」按钮不再需要每条调用任务详情；平台派单与本人认领可释放、流程定义指定与退回指定不可释放的口径与任务级视图完全同源。

## 1. 元数据

- 任务 ID：ISSUE-1282-CURRENT-TASK-RELEASE
- 交付模式：STANDARD
- 需求影响：L2 - 公开 VO `WorkflowBusinessApplyCurrentTaskVO` 新增三个字段（同时是 Feign/DTO 契约），并新增 Flyway `V6` 改动 `workflow_business_apply_current_task` 表结构；不新增接口与权限点
- 方案风险：L1 - 只快照写入两列并在读取时复用现有 `WorkflowTaskAssigneeSource.isReleasable()` 口径，不改任务流转、释放、改派与自动派单逻辑
- 最终风险：L2
- 工作区决策：CREATE（`/data/Mango/worktrees/mango-1282-current-task-release-state`，分支 `feat/issue-1282-current-task-release-state`，基线 `origin/main` `bd852c058`）

## 2. 目标与范围

- 目标：解决 Mango Issue #1282，使业务侧仅凭「当前任务」查询结果判断某行是否出现释放按钮，不再逐行调用 `taskRuntimeApi.detail(taskId)`。
- 成功条件：`currentTasks` 带释放资格三字段；`AUTO_ASSIGN`、`SELF_CLAIM` 为 `unclaimable=true`；`DEFINITION`、`RETURN_ASSIGN` 为 `false`；`SELF_CLAIM` 的 `claimTime` 非空；释放被拒错误码不变（`417013` / 平台 `3663`）。
- 处理范围：`mango-workflow-api`（`WorkflowBusinessApplyCurrentTaskVO`）、`mango-workflow-core`（当前任务实体、`WorkflowBusinessApplyService` 快照写入与视图装配、Flyway `V6`）、`mango-workflow-core` 测试、Workflow README、业务审批接入指南、任务释放升级指南、业务审批历史变更索引。
- 不处理范围：不提供批量 `POST /workflow/tasks/unclaimable`（Issue 首选方案已消除 N+1 动机）；不改 `mango-ui/packages/workflow` 的 TypeScript 类型与 `normalizeBusinessApplyCurrentTask`（本仓管理后台未消费当前任务释放资格，业务仓自带接口类型，改前端需引入 `pnpm install` 与构建验证的成本）；不为 `WorkflowTaskRuntimeService` 任务详情补 `claimTime`（存量缺口，与本次业务列表诉求无关）；不给历史任务列表 `WorkflowTaskBusinessQueryService` 增加 `unclaimable`；不做超时回收、自动顺位或弃权语义。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| SR-1 | 业务列表 | 读取 `currentTasks`、`currentTask`（申请分页、进度、办理结果、发起结果、事件 payload） | 每行返回 `unclaimable`、`assignSource`、`claimTime` | 字段缺失时消费方退回逐行详情查询 | VO 字段断言 + 快照写入集成测试 |
| SR-2 | 业务列表 | 当前任务由 `assignmentMode=AUTO` 派出 | `assignSource=AUTO_ASSIGN`，`unclaimable=true`，`claimTime` 非空 | 来源解析失败时按 `DEFINITION` 处理（不可释放） | 集成测试断言 |
| SR-3 | 业务列表 | 当前任务由本人认领（`SELF_CLAIM`） | `unclaimable=true`，`claimTime` 为领取时间 | — | 集成测试断言库列与视图值 |
| SR-4 | 业务列表 | 当前任务由流程定义指定（`DEFINITION`）或退回指定（`RETURN_ASSIGN`） | `unclaimable=false`，业务不显示释放按钮 | 与服务端 `unclaim` 判据不一致时会「按钮可见但被拒」 | 集成测试断言 |
| SR-5 | 业务列表 | 当前任务未指派（待领取） | `assignSource` 为空、`claimTime` 为空、`unclaimable=false` | 不可用 `claimStatus` 推断 | 既有未指派用例断言两列为空 |
| SR-6 | 升级数据库 | 已有 `workflow_business_apply_current_task` 数据 | `V6` 幂等加 `assign_source`、`claim_time` 两列，不回填 | 列已存在时跳过、不中断升级 | 迁移升级集成测试（1.0.20 升级路径与空库路径） |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| D-1 | SR-1 | 在 `WorkflowBusinessApplyCurrentTaskVO` 增加与 `WorkflowTaskVO` 同名同语义的 `Boolean unclaimable`、`WorkflowTaskAssigneeSource assignSource`、`LocalDateTime claimTime`；该 VO 由 `WorkflowBusinessApplyService#toTaskVo` 单点装配，因此申请分页、进度、办理结果、发起结果和事件 payload 全部覆盖 | `mango-workflow-api/.../vo/WorkflowBusinessApplyCurrentTaskVO.java` | 删除三个字段与装配代码 |
| D-2 | SR-2、SR-3、SR-4 | `assign_source`、`claim_time` 随当前任务快照落库：`refreshCurrentTasks` 用 `WorkflowTaskAssigneeSourceResolver.resolve(task)` 写来源，`claimTime` 取 Flowable `task.getClaimTime()`，缺省回落到任务到达时间（对应 Issue 的「办理人获得任务的时间」语义） | `WorkflowBusinessApplyCurrentTaskEntity`、`WorkflowBusinessApplyService#refreshCurrentTasks` | 回退快照写入，视图回退为只读既有列 |
| D-3 | SR-4、SR-5 | `unclaimable` 不落库，读取时按 `isCurrentUserIdentifier(assigneeName) && assignSource.isReleasable()` 计算，判据与 `WorkflowTaskRuntimeService#fillClaimState`、`unclaim` 的 `requireReleasable` 保持一致；来源为空按不可释放处理 | `WorkflowBusinessApplyService#unclaimable`、`#isCurrentUserIdentifier` | 删除读时计算，`unclaimable` 返回 `null` |
| D-4 | SR-6 | 新增 `V6__workflow_business_apply_current_task_claim_state.sql`，沿用 `V2`/`V5` 的 `information_schema` 判重 + `PREPARE/EXECUTE` 条件 DDL，不回填历史行，`AFTER` 定位到两个基线（`workflow-1.0.20` 夹具与当前 `V1`）都存在的 `assignee_name`、`arrived_at` 列 | `mango-workflow-core/src/main/resources/db/migration/workflow/` | 保留列不回滚（可空、无读写副作用）；删代码即失效 |
| D-5 | 兼容 | 不新增 HTTP 接口、权限码、配置项；`claimStatus` 语义不变；释放被拒仍走既有 `TASK_RELEASE_FORBIDDEN`（`417013`/`3663`） | `mango-workflow-api`、`mango-workflow-starter` 无改动 | — |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| IMPL-1 | D-1 | 1 | `mango/mango-platform/mango-workflow/mango-workflow-api/src/main/java/io/mango/workflow/api/vo/WorkflowBusinessApplyCurrentTaskVO.java` | 三字段与中文 `@Schema` 编译通过 |
| IMPL-2 | D-2、D-4 | 2 | `mango-workflow-core/.../entity/WorkflowBusinessApplyCurrentTaskEntity.java`、`mango-workflow-core/src/main/resources/db/migration/workflow/V6__workflow_business_apply_current_task_claim_state.sql` | 实体与迁移列一致，空库与 1.0.20 升级路径均可执行 |
| IMPL-3 | D-2、D-3 | 3 | `mango-workflow-core/.../service/impl/WorkflowBusinessApplyService.java` | 快照写入两列、视图装配三字段、`unclaimable` 按当前用户计算 |
| IMPL-4 | D-2、D-3、D-4 | 4 | `mango-workflow-core/src/test/java/io/mango/workflow/core/service/impl/WorkflowBusinessApplyServiceImplIntegrationTest.java`、`WorkflowTaskRuntimeServiceIntegrationTest.java`、`.../support/WorkflowMigrationUpgradeIntegrationTest.java` | 新增两条释放资格用例、真实引擎场景用例（并修正 `startAutoAssignWorkflow` 夹具未 `markProcessStarted` 导致快照不落库的缺口）、H2 建表补列、迁移计数与列断言更新 |
| IMPL-6 | D-1、D-5 | 6 | `mango-workflow-starter/src/test/java/io/mango/workflow/starter/controller/WorkflowBusinessApplyCurrentTaskHttpContractTest.java` | 业务列表 HTTP 返回体中三字段的字段名、取值与空值语义被锁定 |
| IMPL-5 | D-1、D-2 | 5 | `mango/mango-platform/mango-workflow/README.md`、`mango-docs/guides/business-integration/workflow-business-approval.md`、`mango-docs/guides/operations/workflow-task-release-upgrade.md`、`mango-docs/changelog/business-integration/workflow-business-approval.md` | 字段语义、判定口径、升级影响与实现一致 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| SR-1、SR-2、SR-3 | M11 集成测试（H2 + 真实服务装配） | `mvn -pl mango-platform/mango-workflow/mango-workflow-core -am -Drevision=mango-004 test` → `WorkflowBusinessApplyServiceImplIntegrationTest#refreshCurrentTasksPersistsReleaseEligibilityForSelfClaimedTask` | PASS：`assignSource=SELF_CLAIM`、`unclaimable=true`、`claimTime=10:15`，库列 `assign_source='SELF_CLAIM'` 且 `claim_time` 非空 | Surefire 报告 |
| SR-4 | M11 集成测试 | 同上 → `#refreshCurrentTasksKeepsCurrentTaskUnreleasableWhenSourceOrAssigneeDoesNotAllowRelease` | PASS：`DEFINITION` 与 `AUTO_ASSIGN`（非本人）均为 `unclaimable=false`；`DEFINITION` 的 `claimTime` 回落为 `arrivedAt` | Surefire 报告 |
| SR-5 | M11 集成测试（既有用例增强） | 同上 → `#refreshCurrentTasksPersistsUnclaimedCandidateTaskWhenAssigneeIsNull` | PASS：未指派行 `assign_source`、`claim_time` 均为 `NULL` | Surefire 报告 |
| SR-6 | M11 迁移升级集成测试（真实 MySQL） | 同上 → `WorkflowMigrationUpgradeIntegrationTest`（`migrate_1_0_20SchemaAndChecksum_addsMissingAuditColumns`、`migrate_1_0_21_22Checksum_repairsKnownHistory`、`migrate_currentFreshV1_keepsExistingAuditColumnsAndRecordsV2`） | PASS：1.0.20 升级路径执行 5 个迁移、空库 6 个、重复执行 0 个；`assign_source varchar YES`、`claim_time datetime YES` | Surefire 报告 |
| SR-2、SR-3、SR-4 | M11 真实场景测试（真实 Flowable 引擎 + 真实 `WorkflowBusinessApplyService`） | 同上 → `WorkflowTaskRuntimeServiceIntegrationTest#issue1282CurrentTaskSnapshotExposesReleaseEligibilityOfRealEngineTasks` | PASS：真实自动派单节点 `AUTO_ASSIGN`/`unclaimable=true`/`claimTime` 非空 → 真实 `unclaimWithResult` 后三字段清空且 `UNCLAIMED` → finance 真实认领后本人视角 `SELF_CLAIM`/`true`、admin 视角同 `SELF_CLAIM` 但 `false` → 流程定义指定节点 `DEFINITION`/`false`，真实 `unclaimWithResult` 抛 `BizException` `code=3663` 且消息为 `DEFINITION.releaseBlockedMessage()` | Surefire 报告 |
| SR-1、SR-5 | M12 API 契约验证（业务列表 HTTP 端点） | `mvn -pl mango-platform/mango-workflow/mango-workflow-starter -am -Drevision=mango-004 -Dtest=WorkflowBusinessApplyCurrentTaskHttpContractTest test` | PASS：`POST /workflow/business-applies/page` 返回 `data.list[0].currentTasks[0]` 带 `unclaimable`/`assignSource`/`claimTime`；派单行为 `true`/`AUTO_ASSIGN`/`2026-11-15T10:15:30`；候选池行为 `UNCLAIMED`/`false`/两字段为空（2/2 通过） | Surefire 报告 |
| 全部 | 模块测试基线 | `set -a && . ./.mango/dev-workspace.env && set +a && mvn -pl mango-platform/mango-workflow/mango-workflow-core -am -Drevision=mango-004 test` | PASS：233 tests，0 failures，0 errors，0 skipped（`BUILD SUCCESS`） | 命令输出 |
| 兼容 | `mango-workflow-starter` 模块测试 | 同上，目标换成 `mango-workflow-starter`，`-Dtest='io.mango.workflow.starter.**'`（临时 3.2.5 执行后还原 pom） | 56 PASS、2 存量 FAILURE（`WorkflowApiSurfaceContractTest` 指纹漂移，已在 §7 举证与本次无关） | Surefire 报告 |
| SR-1 | 契约静态核对 | `javap`/源码核对 `WorkflowBusinessApplyCurrentTaskVO` 字段与 `toTaskVo` 装配，确认 `currentTask`/`currentTasks` 全部返回点共用同一装配 | PASS | 源码 |

## 7. 例外与剩余风险

- `mango-workflow-core` 继承的 Surefire 2.17 不发现 JUnit 5 用例（存量问题，模块 `Tests run: 0`），本次验证临时在模块 `pom.xml` 加 `maven-surefire-plugin` 3.2.5 执行后已还原，仓库构建配置无改动。
- 存量失败（与本次无关）：`mango-workflow-starter` 的 `WorkflowApiSurfaceContractTest` 在本次改动前已失败。把本次全部改动 `git stash` 后在 `origin/main` `bd852c058` 上重跑仍为同一对比（api 指纹预期 `649612f3…`、实际 `053297d7…`；http 指纹预期 `32e55a00…`、实际 `277ab3b8…`），说明基线指纹与当前 JDK 21.0.13 计算结果已不一致（该文件上次变动为 `55b351394` #1259）。本次不修改该基线指纹，也不新增断言，避免用手工重算掩盖存量漂移；PR 合同只检查文档契约，不跑该测试，不阻塞本次交付。
- 迁移升级测试需要 `MANGO_DB_*` 环境变量（用隔离库 `mango_dev_506_<hash>`）；未注入时按 `@EnabledIfEnvironmentVariable` 跳过，因此本次验证显式注入 `.mango/dev-workspace.env` 后执行，不以跳过视为通过。
- 升级前已存在的当前任务行 `assign_source`、`claim_time` 为 `NULL`，`unclaimable` 返回 `false`（来源未知按不可释放），任务再次刷新或流转后补齐；不回填历史行，避免用猜测值决定按钮可见性。
- `mango-ui/packages/workflow` 的 `WorkflowBusinessApplyCurrentTask` 类型与 `normalizeBusinessApplyCurrentTask` 未同步三字段：本仓管理后台不消费该字段，需要前端类型的消费方按 README/指南自建类型，或后续单独立项同步 npm 包。
- 本次无 UI 改动，未执行 E2E；无新增接口与权限点，未新增独立端点，API 契约验证复用既有 `POST /workflow/business-applies/page`（`WorkflowBusinessApplyCurrentTaskHttpContractTest`）；该测试用 `standaloneSetup` 装配 MockMvc，需显式注入 `MappingJackson2HttpMessageConverter` 才能与运行态一致地序列化 `LocalDateTime`（否则默认输出时间戳数组）。
- 真实引擎套件 `WorkflowTaskRuntimeServiceIntegrationTest` 的 `startAutoAssignWorkflow` 夹具原先未调用 `markProcessStarted`，申请与流程实例未关联，`refreshCurrentTasks` 按 `process_instance_id` 查不到申请而不落库；本次补齐该关联（与 `startNodeAffinityWorkflow` 一致），使快照写入在真实引擎下被覆盖，既有 2 处调用方在全量跑中仍通过。
