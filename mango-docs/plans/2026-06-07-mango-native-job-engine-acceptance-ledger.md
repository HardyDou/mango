# Mango 原生 Job Engine 验收台账

## 1. 目标

验证 Mango 原生 Job Engine 形成真实任务调度闭环：单体内嵌部署下任务定义、结构化参数、手动触发、Cron 每分钟调度、执行实例、日志、Worker、运行状态、告警入口和菜单一致性均可通过管理后台人工验收；远程 Worker 通过后端跨 Spring Context E2E 验证 `HTTP_INTERNAL` 注册、派发、执行和日志回传。

## 2. 范围

本台账覆盖 2026-06-07 已实现并通过 E2E 的原生 Job 闭环：

- Mango 原生任务定义和状态流转。
- `IN_MEMORY` 单体内嵌 JobCenter + Worker。
- 手动触发和 Cron 每 1 分钟调度。
- 执行实例和行内日志详情。
- `System.out`、业务 logger、handler result 日志归档展示。
- Worker 节点、运行状态、告警规则菜单页面。
- 告警规则任务级/应用级 CRUD、启停和通知参数结构化维护。
- 失败执行实例按启用的 `mango_job_alarm_rule` 调用 `mango-notice`。
- 远程 Worker `HTTP_INTERNAL` 注册、心跳、派发、Java Handler 执行、日志回传。
- Worker 重复心跳注册幂等更新。
- 真实双进程单体多实例下两个 `IN_MEMORY` Worker 自动注册，Cron 每分钟窗口不重复创建。
- 后端单元测试、前端包构建和管理后台 E2E。

不把以下能力标记为本次验收完成：

- 单体多实例并发调度去重的浏览器 E2E。
- 预发环境真实通知模板、收件人规则和第三方通道发送验证。

## 3. 原子交付项

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| JOB-ACC-001 | 用户要求；设计 6.1 | 支持 Mango 原生任务定义，业务不感知部署模式 | 任务定义保存 `appCode`、`jobCode`、`handlerName`、频次、参数和 `engineType=MANGO_NATIVE`，不绑定 Worker 地址 | `mango-job-api` command/VO/enums；`MangoJobDefinitionService`；任务定义页面 | Playwright 创建、查询、编辑、启用/暂停/删除任务 | DONE | `mango-ui/apps/mango-admin/e2e/specs/job-management.spec.ts`；`mango-docs/evidence/2026-06-07-mango-native-job-e2e/00-definition-create-manual-schedule.png`、`01-definition-list-frequency.png`、`05-definition-status-enabled.png`、`09-definition-delete-confirm.png` |
| JOB-ACC-002 | 用户要求；设计 6.1 | 参数配置使用结构化表单，不要求直接编辑 JSON 字符串 | 前端按 `paramSchema` 生成参数表单，保存仍为 JSON 对象 | `mango-ui/packages/job/src/views/definition/index.vue`；E2E 参数表单断言 | Playwright 在新增和触发弹窗填写 Schema 生成的字段 | DONE | `00-definition-create-manual-schedule.png`、`06-trigger-dialog-frequency-manual.png`；`job-management.spec.ts` 2 passed |
| JOB-ACC-003 | 用户要求；设计 6.1 | 任务频次配置必须可见 | 支持 `MANUAL`、`CRON`、`FIXED_RATE`，页面展示调度类型和表达式 | 任务定义页面编辑弹窗 | Playwright 校验 Cron `0 */5 * * * ?` 和固定频率 `60000` 回显 | DONE | `03-definition-edit-cron-schedule.png`、`04-definition-edit-fixed-rate-schedule.png` |
| JOB-ACC-004 | 用户要求；设计 6.2 | 单体内嵌部署时使用内存通信，不绕 HTTP 端口 | `JobTransportType.IN_MEMORY` + `InMemoryMangoJobWorkerTransport`，Worker 地址显示 `in-memory://...` | `mango-job-core/nativeengine`；Worker 页面；执行实例页面 | E2E 断言实例和 Worker 均显示 `in-memory://` | DONE | `07-instance-filtered-trigger-batch.png`、`10-job-worker.png`；`JobTransportTypeTest` |
| JOB-ACC-005 | 用户要求；设计 6.2 | 手动触发必须真实创建执行实例并完成执行 | JobCenter 创建 instance/attempt 并通过 Worker 执行 handler | `MangoNativeJobRuntime`；`MangoJobWorkerExecutor`；手动触发 UI | Playwright 触发 `mangoJobRuntimeProbeHandler`，等待实例 `SUCCESS` | DONE | `06-trigger-dialog-frequency-manual.png`、`07-instance-filtered-trigger-batch.png`；`job-management.spec.ts` 2 passed |
| JOB-ACC-006 | 用户要求；设计 6.2 | Cron 每 1 分钟任务必须真实按频次执行 | `MangoNativeJobScheduler` 定时扫描调度游标，创建 `SCHEDULED` 实例 | `MangoNativeJobScheduler`；`MangoNativeJobRuntime`；调度游标表 | Playwright 等待每分钟任务至少产生 2 个非运行态调度实例 | DONE | `08c-scheduled-every-minute-instance.png`；E2E 命令输出 `2 passed (2.4m)` |
| JOB-ACC-007 | 用户要求；设计 6.4 | 执行日志从执行实例行内查看，不保留独立执行日志菜单 | 执行实例行提供“日志”按钮，菜单不包含独立“执行日志”项 | `mango-ui/packages/job/src/views/instance/index.vue`；菜单 migration | Playwright 校验无独立“执行日志”菜单，并打开实例行内日志抽屉 | DONE | `08-execution-instance-log-entry.png`、`08b-execution-log-detail.png`、`08d-scheduled-every-minute-log-detail.png` |
| JOB-ACC-008 | 用户要求；设计 6.4 | 日志必须包含 `System.out`、业务 logger 和 handler result | Worker 执行时捕获 console/logback 输出并写入原生日志 chunk | `MangoJobExecutionLogBuffer`；`MangoJobTeePrintStream`；`MangoJobLogbackCapture`；日志详情接口 | E2E 调用 `/api/job/instances/{id}/logs` 并断言日志片段存在 | DONE | `08b-execution-log-detail.png`、`08d-scheduled-every-minute-log-detail.png`；`job-management.spec.ts` 2 passed |
| JOB-ACC-009 | 用户要求；设计 6.3 | Worker 列表必须显示真实 Worker，不能出现 `N/A` | Worker 快照来自原生 Worker 注册和心跳，页面展示真实地址和在线状态 | `MangoNativeJobRuntime` Worker upsert；Worker 页面 | E2E 断言 `mango-job` Worker `ONLINE` 且地址包含 `in-memory://` | DONE | `10-job-worker.png`；`/api/job/workers/page` E2E 断言 |
| JOB-ACC-010 | 用户要求；设计 6.5 | 后台菜单为 `平台能力 -> 任务管理 -> 任务定义/执行实例/Worker 节点/运行状态/告警规则` | 通过授权模块 migration 入库菜单，前端 package 注册页面 | `V44__native_job_menu_names.sql`；`admin-pages.ts` | Playwright 通过菜单进入各页面并截图 | DONE | `10-job-instance.png`、`10-job-worker.png`、`10-job-engine.png`、`10-job-alarm.png` |
| JOB-ACC-011 | 用户要求；设计 6.5 | 运行状态页面使用真实 Job API | 页面访问 `/api/job/engines/status` 展示原生运行状态 | `MangoJobQueryService#listEngineStatus`；运行状态页面 | Playwright 等待真实 API 200 并截图 | DONE | `10-job-engine.png`；`job-management.spec.ts` 2 passed |
| JOB-ACC-012 | 用户要求；设计 6.5 | 告警规则入口遵循接入 `mango-notice` 的决策 | Job 模块保留告警菜单入口，规则维护只配置通知场景、模板编码、收件人规则和启用状态，具体通道归 `mango-notice` | 告警规则页面；菜单权限 | Playwright 打开告警规则页面，完成任务级告警规则创建、编辑、停用、启用和删除 | DONE | `15-alarm-rule-create-dialog.png`、`16-alarm-rule-created.png`、`17-alarm-rule-edit-dialog.png`、`18-alarm-rule-disabled.png`、`19-alarm-rule-enabled.png`、`20-alarm-rule-deleted.png` |
| JOB-ACC-013 | 设计 13 | 远程 Worker transport 契约保留 `HTTP_INTERNAL` | API 类型、transport registry、Feign transport、Worker 自动注册和内部执行 Controller 保留 HTTP 内部调用扩展点；内部接口使用 `@Inner` 和 `ApiAccess.INTERNAL` | `JobTransportType.HTTP_INTERNAL`；`HttpInternalMangoJobWorkerTransport`；`MangoJobRemoteWorkerRegistrar`；`MangoJobWorkerInternalController` | Maven 集成测试启动独立 JobCenter Spring Context 和独立 Worker Spring Context，禁用内嵌 Worker 后通过 HTTP_INTERNAL 注册远程 Worker、派发任务、执行 Java Handler、捕获 `System.out`/logger 并回传日志 | DONE | `MangoJobRemoteDispatchE2ETest#jobCenterShouldDispatchNativeJobToRemoteWorkerOverHttpInternal`；`MangoJobWorkerInternalControllerTest#executeShouldRunJavaHandlerInRemoteWorkerProcessAndReturnCapturedLogs`；`mvn -pl mango-platform/mango-job/mango-job-starter-remote -am test -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false` BUILD SUCCESS |
| JOB-ACC-014 | 设计 6.2；设计 11 | 多 JobCenter/多实例安全基础能力 | 调度游标 CAS 锁、调度窗口幂等键、租约 token 和状态机共同保证同一调度窗口只创建一个实例；Worker 重复心跳注册按唯一键幂等更新 | `MangoJobScheduleCursorEntity`；`MangoJobLeaseService`；`MangoJobIdempotencyKeyService`；`MangoNativeJobRuntime#tick`；`MangoJobWorkerRegistryService` | Maven 集成测试并发调用两个 JobCenter tick，断言只产生一个实例和一个 attempt；重复注册同一 Worker，断言只保留一条 Worker 快照并更新能力清单 | DONE | `MangoJobMultiDataSourceIntegrationTest#nativeRuntime_shouldCreateOnlyOneScheduledInstanceWhenTwoJobCentersTickSameCursor`；`MangoJobMultiDataSourceIntegrationTest#workerRegistry_shouldKeepRegistrationIdempotentWhenWorkerHeartbeatRepeats`；后端聚合 Maven BUILD SUCCESS |
| JOB-ACC-015 | PMO 交付契约 | 交付必须有真实测试、构建、截图和台账检查 | 使用 Maven、前端构建、Playwright E2E、delivery-contract-check | 验证命令和证据目录 | 命令执行通过后更新交付报告 | DONE | `mango-docs/evidence/2026-06-07-mango-native-job-e2e/`；本台账 |
| JOB-ACC-016 | 用户要求；设计 18 | 废弃 PowerJob 集成部分，只保留 Mango 原生 Job Engine | 删除 PowerJob Adapter 包、自动配置测试、SDK 依赖和对外 `POWERJOB` 枚举；历史文档仅保留能力参考 | `mango-job-starter`；`JobEngineType`；`@mango/job` API 类型；本台账和原生设计文档 | 使用 `rg` 分别扫描 PowerJob、powerjob、POWERJOB、tech.powerjob，确认 Job 后端运行时代码、Job 前端和 E2E 无命中；Maven/前端/E2E 重新验证 | DONE | `mango-job-starter/pom.xml`；`JobEngineType`；`packages/job/src/api/job.ts`；本轮验证命令输出 |
| JOB-ACC-017 | 投产计划 `JOB-RUNTIME-005`；设计 6.3 | Worker 心跳过期、查询和恢复状态必须准确 | stale `ONLINE` Worker 查询前持久化为 `EXPIRED`；重新心跳注册恢复为 `ONLINE`；非法 Worker 地址继续过滤 | `MangoJobQueryService#expireStaleWorkers`；`MangoJobWorkerRegistryService#registerWorker`；Worker 集成测试 | Maven 集成测试插入非法、在线、过期 Worker，断言列表过滤、`EXPIRED` 查询和重新心跳恢复 | DONE | `MangoJobMultiDataSourceIntegrationTest#queryService_shouldFilterInvalidExpireStaleWorkersAndRecoverOnHeartbeat`；针对性 Maven BUILD SUCCESS |
| JOB-ACC-018 | 投产计划 `JOB-RUNTIME-002`；设计 6.2 | JobCenter 重启后调度游标继续推进，不重复已完成窗口 | 调度游标、实例幂等键和执行事实全部落库；重启后的 JobCenter 从数据库游标继续扫描下一个窗口 | `MangoNativeJobRuntime#tick`；`mango_job_schedule_cursor`；`mango_job_instance.idempotency_key` | Maven 集成测试第一次 tick 完成首个窗口，第二次 tick 模拟重启后继续下个窗口，断言两个实例的 `scheduledFireTime` 不重复，游标 `lastFireTime` 推进到第二窗口 | DONE | `MangoJobMultiDataSourceIntegrationTest#nativeRuntime_shouldContinueScheduleCursorAfterJobCenterRestartWithoutDuplicatingCompletedWindow`；针对性 Maven BUILD SUCCESS |
| JOB-ACC-019 | 投产计划 `JOB-DATA-002`；设计 8 | 原生 Job migration 必须能在空 Job 库创建 V4 表、关键唯一约束，并能被 MyBatis-Plus Mapper 真实读写 | 使用 Mango Flyway 在 `job` 数据源执行 V1-V4；测试验证 V4 原生表、关键约束和 `MangoJobEventMapper` 读写，且 primary 库不生成 Job 表 | `V1__init_mango_job.sql` 到 `V4__native_job_engine_foundation.sql`；`MangoJobMultiDataSourceIntegrationTest` | Maven targeted 测试验证空 H2 job 库 migration 到 v4 后，`mango_job_schedule_cursor`、`mango_job_attempt`、`mango_job_worker_capability`、`mango_job_log_chunk`、`mango_job_event` 存在，关键约束存在，事件通过 Mapper 插入并从 job 数据源查询 | DONE | `MangoJobMultiDataSourceIntegrationTest#flywayAndMybatisPlus_shouldCreateNativeEngineTablesAndIndexesOnJobDatasource`；Job 聚合 Maven BUILD SUCCESS，20 tests |
| JOB-ACC-020 | 投产计划 `JOB-RUNTIME-001`；设计 6.2 | Cron 每 1 分钟调度必须在连续窗口内稳定推进，不重复、不漏窗口 | 原生调度器以调度游标、调度窗口幂等键和实例事实表推进每个 Cron fire time；连续 tick 必须按分钟窗口精确生成实例 | `MangoNativeJobRuntime#tick`；`mango_job_schedule_cursor`；`mango_job_instance`；`mango_job_attempt`；`mango_job_log_chunk`；`job-scheduler-stability.spec.ts` | Maven 集成测试将每分钟 Cron 游标回拨到 5 个历史窗口，连续执行 5 次 tick，断言 5 个实例全部 `SUCCESS`、计划触发时间连续、批次号无重复、游标推进到下一分钟，并验证 attempt、日志索引和日志 chunk 已落库；Playwright 通过真实登录和真实 Job API 创建每分钟任务，在本地 MySQL 连续观察 3 分钟，断言 3 个实例全部 `SUCCESS`、重复窗口 0、失败实例 0、日志详情可读 | DONE | `MangoJobMultiDataSourceIntegrationTest#nativeRuntime_shouldKeepEveryMinuteCronStableAcrossContinuousWindows`；`mango-ui/apps/mango-admin/e2e/specs/job-scheduler-stability.spec.ts`；`mango-docs/evidence/2026-06-07-mango-native-job-e2e/job-scheduler-stability-local.md`；Job 聚合 Maven BUILD SUCCESS，25 tests；稳定性 E2E `1 passed, 2 skipped` |
| JOB-ACC-021 | 用户要求；设计 6.3 | Worker 必须支持手动登记、禁用、排空、下线和恢复，且状态治理真实影响调度分发 | 新增 Worker 管理 API、按钮权限和统一 UI；手动登记仅支持远程 `HTTP_INTERNAL` Worker；人工 `DISABLED/DRAINING/OFFLINE` 状态不会被心跳自动覆盖；调度只选择 `ONLINE` Worker | `CreateMangoJobWorkerCommand`；`UpdateMangoJobWorkerStatusCommand`；`MangoJobWorkerRegistryService`；`MangoJobController`；`V44__native_job_menu_names.sql`；`V45__native_job_worker_governance_permissions.sql`；Worker 页面 | Maven 集成测试验证手动登记、禁用后心跳不恢复、恢复在线；禁用内嵌 Worker 后手动触发失败并记录实例失败原因；Playwright 通过 UI 登记远程 Worker、禁用、恢复并用 API 断言状态 | DONE | `MangoJobMultiDataSourceIntegrationTest#workerRegistry_shouldSupportManualCreateStatusGovernanceAndHeartbeatProtection`；`MangoJobMultiDataSourceIntegrationTest#nativeRuntime_shouldNotDispatchToManuallyDisabledEmbeddedWorker`；`11-worker-create-dialog.png`；`12-worker-created-online.png`；`13-worker-disabled.png`；`14-worker-restored-online.png`；Job 聚合 Maven BUILD SUCCESS，25 tests；E2E `9 passed (1.9m)` |
| JOB-ACC-022 | 用户要求；设计 8.9、17 | 失败任务应接入 `mango-notice`，由 notice 负责系统消息、短信、邮件、企业微信等通道 | 原生运行时在实例失败终态后查询启用的 `INSTANCE_FAILED` 告警规则，构造 `SendNoticeCommand` 调用可选 `NoticeApi`；规则按任务级或应用级匹配，模板编码和接收人规则从 `mango_job_alarm_rule` 配置读取 | `MangoJobAlarmNotificationService`；`MangoNativeJobRuntime`；`mango_job_alarm_rule`；告警规则页面 | Maven 集成测试创建失败 handler、启用告警规则、触发失败实例，断言 `NoticeApi.send` 收到 `bizType`、`bizId`、`userIds`、`idempotentKey` 和模板参数，实例失败状态和行内日志仍可查询 | DONE | `MangoJobMultiDataSourceIntegrationTest#nativeRuntime_shouldSendNoticeWhenFailedInstanceMatchesEnabledAlarmRule`；Job 聚合 Maven BUILD SUCCESS，25 tests |
| JOB-ACC-023 | 用户要求；投产计划 `JOB-ALARM-003`；设计 6.5、8.9、17 | 告警规则必须可通过 Mango 后台维护，不依赖 DBA 直接写表 | 新增告警规则 API、权限 migration 和统一 UI；支持任务级 `jobId` 规则与应用级默认规则；通知参数通过收件规则、单用户、多用户结构化字段生成 `noticeParams` JSON；后端校验 JSON、租户和任务归属 | `MangoJobAlarmRuleService`；`MangoJobController`；`MangoJobFeignClient`；`V46__native_job_alarm_rule_permissions.sql`；`mango-ui/packages/job/src/views/alarm/index.vue` | Maven 集成测试覆盖 CRUD、启停、删除、租户隔离、非法 JSON、任务归属校验；Playwright E2E 通过后台创建、编辑、停用、启用、删除任务级规则并用 API 断言落库 | DONE | `MangoJobMultiDataSourceIntegrationTest#alarmRuleService_shouldManageCrudStatusAndTenantIsolationOnJobDatasource`；`MangoJobMultiDataSourceIntegrationTest#alarmRuleService_shouldRejectInvalidJsonAndMismatchedJobScope`；`15-alarm-rule-create-dialog.png` 到 `20-alarm-rule-deleted.png`；Job 聚合 Maven BUILD SUCCESS，25 tests |
| JOB-ACC-024 | 用户要求；Worker 归属升级计划 | 调度系统必须知道任务归属，不能把 A 服务的任务随机交给 B 服务 Worker | 任务定义保存 `ownerService`、`workerGroup`；Worker 快照保存 `serviceCode`、`workerGroup`、`transportType`、`registerSource`、`instanceId`、`runtimeAddress`；Worker capability 保存 `serviceCode`、`workerGroup`、`appCode`、`handlerName`、`jobCode`；调度层按归属和能力过滤，Worker 执行前按同一归属二次查 handler | `V5__job_worker_ownership.sql`；`MangoNativeJobRuntime#selectWorker`；`MangoJobWorkerExecutor#execute`；`MangoJobHandlerRegistry#findHandler`；Worker 页面 | Maven 集成测试验证 A 服务任务只有 B Worker 时触发失败且不记录 Worker 地址；同一 HTTP 地址可登记 service A/B 两个 Worker，A 任务只匹配 A 归属；重复注册同一 Worker 只保留一条快照和一条能力；远程 Worker ownership mismatch 被 Controller 拒绝 | DONE | `MangoJobMultiDataSourceIntegrationTest#nativeRuntime_shouldNotDispatchServiceAJobToServiceBWorker`；`MangoJobMultiDataSourceIntegrationTest#workerRegistry_shouldAllowSameAddressForDifferentWorkerGroupsAndDispatchByOwner`；`MangoJobMultiDataSourceIntegrationTest#workerRegistry_shouldKeepRegistrationIdempotentWhenWorkerHeartbeatRepeats`；`MangoJobWorkerInternalControllerTest#executeShouldRejectWhenWorkerDoesNotOwnHandlerCapability`；针对性 Maven BUILD SUCCESS，24 tests；远程 Worker Controller BUILD SUCCESS，2 tests |
| JOB-ACC-025 | 用户要求；灵活部署 | 单体多实例下每个真实进程应自动注册独立内嵌 Worker，且 Cron 不重复调度同一窗口 | 内嵌 Worker 当前地址格式为 `in-memory://{host}/embedded-{pid}@{host}`；JobCenter 使用调度游标 CAS、幂等键、租约 token 防止重复窗口；Worker 心跳过期后查询侧持久化 `EXPIRED` | `MangoNativeJobRuntime#upsertEmbeddedWorkers`；`MangoEmbeddedWorkerRegistrar`；`MangoJobScheduleCursorEntity`；`MangoJobLeaseService`；`MangoJobQueryService#expireStaleWorkers` | 后端集成测试覆盖多内嵌 Worker 自动注册、按 capability 派发、并发 tick 去重、每分钟连续窗口、过期 Worker 过滤；本地真实双进程 monolith `18657/18658` 共用 `mango_dev_a1ce46` 和 `mango_dev_a1ce46_job` 验证两个 `IN_MEMORY` Worker 同时在线，每分钟示例任务最近 12 个窗口均为单实例 `SUCCESS`，重复窗口数 0 | DONE | `MangoJobMultiDataSourceIntegrationTest#embeddedWorkers_shouldRegisterMultipleInMemoryInstancesAndDispatchOnlyByCapability`；`MangoJobMultiDataSourceIntegrationTest#nativeRuntime_shouldCreateOnlyOneScheduledInstanceWhenTwoJobCentersTickSameCursor`；`MangoJobMultiDataSourceIntegrationTest#nativeRuntime_shouldKeepEveryMinuteCronStableAcrossContinuousWindows`；`MangoJobMultiDataSourceIntegrationTest#queryService_shouldFilterInvalidExpireStaleWorkersAndRecoverOnHeartbeat`；本地 DB 证据：`embedded-29094`、`embedded-35634` 同为 `IN_MEMORY/EMBEDDED_AUTO/ONLINE`，`duplicate_windows=0` |

## 4. 验证命令

```bash
cd mango
mvn -pl mango-platform/mango-job/mango-job-support,mango-platform/mango-job/mango-job-api,mango-platform/mango-job/mango-job-core,mango-platform/mango-job/mango-job-starter-remote,mango-platform/mango-job/mango-job-starter -am test -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false

mvn -pl mango-platform/mango-job/mango-job-starter -am test -DskipTests=false -Dtest=MangoJobMultiDataSourceIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false

mvn -pl mango-platform/mango-job/mango-job-starter-remote -am test -DskipTests=false -Dtest=MangoJobWorkerInternalControllerTest -Dsurefire.failIfNoSpecifiedTests=false

mvn -pl mango-platform/mango-job/mango-job-starter -am test -DskipTests=false -Dtest=MangoJobMultiDataSourceIntegrationTest#embeddedWorkers_shouldRegisterMultipleInMemoryInstancesAndDispatchOnlyByCapability -Dsurefire.failIfNoSpecifiedTests=false

curl -sS http://127.0.0.1:18657/actuator/health
curl -sS http://127.0.0.1:18658/actuator/health

mysql -h127.0.0.1 -P3306 -uroot -NBe "
SELECT version, checksum, success
FROM mango_dev_a1ce46.flyway_schema_history_authorization
WHERE version='43';
SELECT version, description, success
FROM mango_dev_a1ce46_job.flyway_schema_history_mango_job
ORDER BY installed_rank;
SELECT worker_address, status, transport_type, register_source, instance_id
FROM mango_dev_a1ce46_job.mango_job_worker_snapshot
WHERE transport_type='IN_MEMORY';
SELECT COUNT(*) duplicate_windows
FROM (
  SELECT i.scheduled_fire_time
  FROM mango_dev_a1ce46_job.mango_job_instance i
  JOIN mango_dev_a1ce46_job.mango_job_definition d ON d.id=i.job_id
  WHERE d.job_code='mango_job_example_chromium_every_minute_cron_probe'
    AND i.trigger_type='SCHEDULED'
  GROUP BY i.scheduled_fire_time
  HAVING COUNT(*) > 1
) x;"

cd mango-ui
pnpm -F @mango/job build

cd mango-ui/apps/mango-admin
E2E_BASE_URL=http://127.0.0.1:8347 pnpm test:e2e -- specs/job-management.spec.ts

cd mango-ui/apps/mango-admin
PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true E2E_BASE_URL=http://127.0.0.1:8347 JOB_STABILITY_MINUTES=3 pnpm test:e2e -- specs/job-scheduler-stability.spec.ts

node mango-pmo/tools/delivery-contract-check.mjs \
  --design mango-docs/designs/mango-native-job-engine-design.md \
  --ledger mango-docs/plans/2026-06-07-mango-native-job-engine-acceptance-ledger.md \
  --mode verify
```

## 5. 验收地址

- 后端：`http://127.0.0.1:18657`
- 第二后端实例：`http://127.0.0.1:18658`
- 前端：`http://127.0.0.1:8347`
- 主库：`127.0.0.1:3306/mango_dev_a1ce46`
- Job 独立库：`127.0.0.1:3306/mango_dev_a1ce46_job`

## 6. 截图证据目录

`/Users/hardy/Work/mango/.mango/worktrees/mango-job-sprint-1/mango-docs/evidence/2026-06-07-mango-native-job-e2e`

## 7. 当前阻塞处理结论

- GitHub Issue `#109` 对应旧本地库 `mango_dev_job_runtime_dual_0607` 的授权模块 V43 checksum mismatch；当前 worktree 使用干净主库 `mango_dev_a1ce46`，V43 checksum 为 `-1719360344` 且 `success=1`，与当前源码一致。
- 当前 Job 独立库 `mango_dev_a1ce46_job` 已执行 `flyway_schema_history_mango_job` V1 到 V5，全部 `success=1`。
- 真实双进程单体多实例运行态验收已完成：`18657` 和 `18658` 两个 monolith 健康检查均为 `UP`，两个内嵌 Worker `embedded-29094`、`embedded-35634` 同时在线。
- 每分钟示例任务 `mango_job_example_chromium_every_minute_cron_probe` 最近 12 个调度窗口均为 1 条 `SUCCESS` 实例，重复窗口数 0。
- 本地验收阻塞已解除；生产发布仍需按投产计划完成预发 2-4 小时长跑、真实通知通道、权限矩阵和发布物验证。
