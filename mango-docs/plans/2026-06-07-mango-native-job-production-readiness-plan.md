# Mango 原生 Job Engine 投产就绪计划

## 1. 目标

把 Mango 原生 Job Engine 从“本地可验收”推进到“可投产”：代码、质量门禁、预发验证、部署运维、回滚和生产风险处置均有明确证据。

当前判定：

- 已达到：本地人工验收和预发联调入口。
- 未达到：生产投产门槛。

## 2. 当前已具备能力

- 原生任务定义、结构化参数、频次配置、手动触发和 Cron 每分钟调度。
- 执行实例、attempt、行内日志详情。
- `System.out`、`System.err`、业务 logger 和 handler result 日志采集。
- `IN_MEMORY` 单体内嵌 Worker。
- `HTTP_INTERNAL` 远程 Worker 后端 E2E。
- Worker 重复心跳注册幂等更新。
- Worker 列表、运行状态、告警规则入口和统一菜单。
- 告警规则任务级/应用级 CRUD、启停和结构化通知参数维护。
- 失败执行实例按启用的 `mango_job_alarm_rule` 调用 `mango-notice`，通知模板和通道由 notice 模块负责。
- PowerJob Adapter、PowerJob Worker/Client SDK 依赖和 `POWERJOB` 对外枚举已从当前运行时交付中移除。
- 后端 Maven 测试、后端 Job 模块 checkstyle/PMD 命令、前端 Job 包构建、管理后台构建、管理后台 Playwright E2E、交付台账检查已通过。

## 3. 投产原则

1. 未通过质量门禁，不创建投产 PR。
2. 未通过预发真实部署验证，不发布生产。
3. 未验证数据库 migration、菜单权限和租户隔离，不发布生产。
4. 未完成部署参数、回滚方案和运行监控说明，不发布生产。
5. 未完成长时间调度稳定性验证，不开放生产定时任务。
6. 预发真实通知模板、收件人规则和第三方通道未验证前，不把告警消息声明为生产可用。

## 4. 处理顺序

| 顺序 | 阶段 | 目标 | 主要产出 |
|---:|---|---|---|
| 1 | 质量门禁 | 证明当前代码可以提交评审 | 后端 `mango:check`、前端 lint/build/test/E2E、禁止标记扫描 |
| 2 | 预发验证 | 证明真实部署环境可运行 | 预发验收清单、数据库 migration 记录、菜单权限和租户验证 |
| 3 | 稳定性验证 | 证明调度长时间运行可靠 | 2-4 小时 Cron 稳定性记录、重启恢复、Worker 心跳过期恢复 |
| 4 | 运维资料 | 证明生产可部署、可观测、可回滚 | 部署参数、拓扑样例、监控指标、日志保留、回滚步骤 |
| 5 | 能力边界 | 明确本次可投产能力和不可投产能力 | Worker 治理、告警规则后台维护、预发真实通知通道的投产判定 |
| 6 | PR 与发布 | 完成代码评审和发布物料 | commit、push、PR、PR 描述、发布检查记录 |

## 5. 投产清单

| ID | 类别 | 检查项 | 通过标准 | 当前状态 | 证据 |
|---|---|---|---|---|---|
| JOB-PRD-001 | 台账 | 交付台账完整 | `JOB-ACC-001` 到 `JOB-ACC-023` 全部 `DONE` | 已通过 | `mango-docs/plans/2026-06-07-mango-native-job-engine-acceptance-ledger.md` |
| JOB-PRD-002 | 台账 | 台账 verify 通过 | `Rows=23`、`DONE=23`、`EXCEPTION=0`、`Incomplete=0` | 已通过 | `delivery-contract-check` 输出 |
| JOB-QA-001 | 后端 | Maven 测试 | Job support/api/core/starter-remote/starter 聚合测试通过 | 已通过 | 2026-06-07 清理旧 target 后重跑 Job 聚合 Maven 测试 `BUILD SUCCESS`，25 tests |
| JOB-QA-002 | 后端 | Job 模块静态检查 | 相关后端模块 `checkstyle:check pmd:check` 通过，非本任务工具/规范冲突登记 Issue | Job 目标模块已通过，全仓规则门禁待治理 | 2026-06-07 清理旧 target 后重跑 Job 聚合 `checkstyle:check pmd:check` 为 `BUILD SUCCESS`；Job core 仍有 5 条 `XxxServiceImpl` 命名规则冲突，按 PMO 命名规范不在本轮改类名；全仓 `mvn mango:check -Drule=all` 失败并报告 19672 个跨模块历史规则 issue，已登记 `mango-docs/plans/2026-06-07-job-quality-tooling-rule-alignment-issue.md` |
| JOB-QA-003 | 前端 | Job 包构建 | `pnpm -F @mango/job build` 通过 | 已通过 | Vite build success |
| JOB-QA-004 | 前端 | 管理后台 E2E | `job-management.spec.ts` 通过，截图留存 | 已通过 | 2026-06-07 PR 级补跑完整 E2E `9 passed (2.5m)`；稳定性 E2E `job-scheduler-stability.spec.ts` 本地 3 分钟观察 `1 passed, 2 skipped`；截图和报告证据已留存 |
| JOB-QA-005 | 前端 | 管理后台构建 | `pnpm -F mango-admin build` 通过，Job E2E lint 清零 | 已通过 | 2026-06-07 PR 级补跑 `pnpm -F mango-admin build` 通过；存在既有 dynamic/static import warning；`pnpm --dir apps/mango-admin exec eslint e2e/specs/job-management.spec.ts e2e/specs/job-scheduler-stability.spec.ts` 为 0 errors、0 warnings |
| JOB-QA-006 | 质量 | 红线关键词扫描 | Job 运行时代码、Job 前端、E2E、部署资料和投产文档不得残留交付红线词；PowerJob 只允许出现在历史参考文档 | 已通过 | `rg -n "PowerJob|powerjob|POWERJOB|tech\\.powerjob|TODO|FIXME|mock|fake|dummy|临时|后续优化|未来优化|N/A" ...` 仅命中历史参考文档和非法 Worker 过滤逻辑 |
| JOB-DATA-001 | 数据库 | 独立 Job 数据库 | `mango_job` migration 可在空库执行，主库不保存 Job 治理表 | 本地 MySQL 独立库已验证，预发待验证 | `mango_job.flyway_schema_history_mango_job` 已执行 V1-V4；关键 V4 表存在于 `mango_job`；当前本地旧主库残留 V1 Job 表，预发必须使用干净库或执行历史清理方案 |
| JOB-DATA-002 | 数据库 | migration 升级路径 | 预发库执行 migration 成功，唯一约束和索引生效 | 本地 H2/MySQL 已覆盖，预发待验证 | `MangoJobMultiDataSourceIntegrationTest#flywayAndMybatisPlus_shouldCreateNativeEngineTablesAndIndexesOnJobDatasource`；本地 MySQL `mango_job` 关键唯一约束和索引只读检查通过 |
| JOB-TENANT-001 | 租户 | 租户隔离 | 租户 A/B 的任务定义、实例、日志、Worker 查询隔离 | 后端已覆盖，预发待验证 | `MangoJobMultiDataSourceIntegrationTest` |
| JOB-MENU-001 | 菜单权限 | 菜单入库 | `平台能力/任务管理` 菜单由 migration 入库 | 本地已通过，预发待验证 | `V43__native_job_menu_names.sql`；E2E |
| JOB-MENU-002 | 菜单权限 | 按钮权限 | 触发、暂停、删除、Worker 高风险动作权限码可控 | 本地 E2E 覆盖管理员操作，预发权限矩阵待验证 | `V44__native_job_worker_governance_permissions.sql`；E2E 覆盖 Worker 登记、禁用、恢复；普通用户权限待预发补充 |
| JOB-RUNTIME-001 | 调度 | 每分钟 Cron 稳定性 | 预发连续运行 2-4 小时，无重复窗口、无长时间积压 | 本地 H2 连续窗口和本地 MySQL 3 分钟真实调度观察已覆盖，预发 2-4 小时待验证 | `MangoJobMultiDataSourceIntegrationTest#nativeRuntime_shouldKeepEveryMinuteCronStableAcrossContinuousWindows`；`mango-ui/apps/mango-admin/e2e/specs/job-scheduler-stability.spec.ts`；`mango-docs/evidence/2026-06-07-mango-native-job-e2e/job-scheduler-stability-local.md` |
| JOB-RUNTIME-002 | 调度 | 服务重启恢复 | JobCenter 重启后调度游标继续推进，不补错窗口 | 本地已覆盖，预发待验证 | `MangoJobMultiDataSourceIntegrationTest#nativeRuntime_shouldContinueScheduleCursorAfterJobCenterRestartWithoutDuplicatingCompletedWindow` |
| JOB-RUNTIME-003 | Worker | 内嵌 Worker | 单体 `IN_MEMORY` 不绕本机 HTTP 端口，日志可见 | 已通过，预发待验证 | E2E 截图和后端测试 |
| JOB-RUNTIME-004 | Worker | 远程 Worker | 独立 Worker 通过 `HTTP_INTERNAL` 注册、心跳、执行、回传日志 | 后端已通过，预发待验证 | `MangoJobRemoteDispatchE2ETest` |
| JOB-RUNTIME-005 | Worker | Worker 心跳异常 | Worker 过期、离线、恢复后状态准确 | 本地已覆盖，预发待验证 | `MangoJobMultiDataSourceIntegrationTest#queryService_shouldFilterInvalidExpireStaleWorkersAndRecoverOnHeartbeat` |
| JOB-LOG-001 | 日志 | 执行日志完整 | `System.out`、`System.err`、logger、handler result 均可查询 | 已通过，预发待验证 | E2E 截图 |
| JOB-LOG-002 | 日志 | 日志保留策略 | 单实例读取上限、保留天数、归档策略明确 | 资料已补，预发/运维执行待验证 | `deploy/job/README.md` |
| JOB-OPS-001 | 运维 | 部署参数 | 单体和远程 Worker 的配置项、默认值、样例完整 | 已补本地样例，预发待验证 | `deploy/job/README.md`；`deploy/job/application-job-native.yml`；旧 PowerJob compose/env 样例已从 active delivery 删除 |
| JOB-OPS-002 | 运维 | 监控指标 | 调度延迟、实例积压、失败率、Worker 在线数、日志写入异常可观测 | 资料已补，预发待验证 | `deploy/job/README.md` |
| JOB-OPS-003 | 运维 | 回滚方案 | 代码回滚、菜单回滚、Job migration 回滚风险说明完整 | 资料已补，发布/预发待验证 | `deploy/job/README.md` |
| JOB-ALARM-001 | 告警 | 告警入口 | 告警规则页面入口存在 | 已通过 | E2E 截图 |
| JOB-ALARM-002 | 告警 | 通知发送 | 失败任务调用 `mango-notice` 模板并真实发送 | 本地代码链路已覆盖，预发真实模板/通道待验证 | `MangoJobMultiDataSourceIntegrationTest#nativeRuntime_shouldSendNoticeWhenFailedInstanceMatchesEnabledAlarmRule`；预发需验证 notice 模板、收件人规则和真实通道 |
| JOB-ALARM-003 | 告警 | 规则维护 | 告警规则 CRUD 可通过后台维护 | 本地已通过，预发待验证 | `MangoJobMultiDataSourceIntegrationTest#alarmRuleService_shouldManageCrudStatusAndTenantIsolationOnJobDatasource`；`MangoJobMultiDataSourceIntegrationTest#alarmRuleService_shouldRejectInvalidJsonAndMismatchedJobScope`；E2E 截图 `15-alarm-rule-create-dialog.png` 到 `20-alarm-rule-deleted.png` |
| JOB-WORKER-001 | Worker 治理 | 手动上下线 | 手动添加、禁用、排空、下线完整后台操作可用 | 本地已覆盖，预发待验证 | `MangoJobMultiDataSourceIntegrationTest#workerRegistry_shouldSupportManualCreateStatusGovernanceAndHeartbeatProtection`；`MangoJobMultiDataSourceIntegrationTest#nativeRuntime_shouldNotDispatchToManuallyDisabledEmbeddedWorker`；E2E `11-worker-create-dialog.png` 到 `14-worker-restored-online.png` |
| JOB-REL-001 | 发布 | 分支状态 | commit、push、PR 创建，PR 描述包含台账和验证结果 | 待处理 | 待补充 |
| JOB-REL-002 | 发布 | 发布后验证 | 发布包、业务消费入口和模板依赖结论明确 | 待处理 | 待补充 |

## 6. 必跑命令

后端：

```bash
cd mango
mvn -pl mango-platform/mango-job/mango-job-support,mango-platform/mango-job/mango-job-api,mango-platform/mango-job/mango-job-core,mango-platform/mango-job/mango-job-starter-remote,mango-platform/mango-job/mango-job-starter -am test -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false
mvn -pl mango-platform/mango-job/mango-job-support,mango-platform/mango-job/mango-job-api,mango-platform/mango-job/mango-job-core,mango-platform/mango-job/mango-job-starter-remote,mango-platform/mango-job/mango-job-starter -am checkstyle:check pmd:check -DskipTests
mvn mango:check -Drule=all
```

前端：

```bash
cd mango-ui
pnpm -F @mango/job build
pnpm lint
pnpm build

cd mango-ui/apps/mango-admin
E2E_BASE_URL=http://127.0.0.1:8347 pnpm test:e2e -- specs/job-management.spec.ts
PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true E2E_BASE_URL=http://127.0.0.1:8347 JOB_STABILITY_MINUTES=3 pnpm test:e2e -- specs/job-scheduler-stability.spec.ts
```

台账：

```bash
node mango-pmo/tools/delivery-contract-check.mjs \
  --design mango-docs/designs/mango-native-job-engine-design.md \
  --ledger mango-docs/plans/2026-06-07-mango-native-job-engine-acceptance-ledger.md \
  --mode verify
```

## 7. 预发验收清单

| 场景 | 操作 | 通过标准 |
|---|---|---|
| 数据库初始化 | 在预发空库执行 migration | `mango_job` 表、索引、唯一约束和菜单 migration 成功 |
| 单体部署 | 启动 JobCenter + embedded Worker | Worker 地址为 `in-memory://`，每分钟任务执行成功 |
| 远程 Worker | 启动独立 JobCenter 和业务 Worker | Worker 在线，远程 Java Handler 执行成功，日志回传 |
| 调度稳定性 | 每分钟任务连续运行 2-4 小时 | 无重复窗口、无卡死、无异常积压 |
| 重启恢复 | 重启 JobCenter | 游标继续推进，已完成实例不重复执行 |
| 租户隔离 | 租户 A/B 分别创建任务 | 定义、实例、日志互不可见 |
| 菜单权限 | 普通用户和管理员分别访问 | 菜单、按钮、数据权限符合预期 |
| 日志 | 任务输出 stdout/stderr/logger | 实例行内日志完整展示 |
| 告警发送 | 创建启用的失败告警规则并触发失败任务 | notice 发送记录可查询，配置的系统消息/短信/邮件/企业微信通道按模板收到消息 |

## 8. 当前阻断项

| ID | 阻断项 | 影响 | 处理方式 |
|---|---|---|---|
| BLOCK-001 | 预发真实部署未验证 | 不能发布生产 | 按第 7 节逐项验收 |
| BLOCK-002 | 预发长时间调度稳定性未验证 | 不能开放生产定时任务 | 本地 3 分钟真实调度观察已通过；预发仍需连续运行 2-4 小时并记录实例数、重复窗口、失败数和积压数 |
| BLOCK-003 | 监控、日志保留、回滚资料已补，执行未验证 | 不能直接交给运维投产 | 在预发按 `deploy/job/README.md` 执行监控、清理和回滚演练 |
| BLOCK-004 | 告警预发真实通道未验证 | 不能声明真实第三方通道生产可用 | 预发通过后台创建 `INSTANCE_FAILED` 告警规则，配置 notice 模板和接收人规则，触发失败任务后核验 notice 发送记录和目标通道 |
| BLOCK-005 | PR、发布包和业务消费入口未验证 | 不能发布生产 | 合并最新 main 后重跑受影响验证，创建 PR 并完成发布验证 |
| BLOCK-006 | 本地开发主库存在早期 Mango Job V1 残留表 | 不影响当前独立 `mango_job` 运行，但说明旧环境升级到独立库时需要历史表清理/迁移口径 | 预发使用干净主库验证；如从旧版本升级，追加 DBA 清理脚本或历史数据迁移方案，禁止自动删除业务未知数据 |
| BLOCK-007 | 全仓 `mvn mango:check -Drule=all` 存在跨模块历史规则债务 | 不能把“整仓质量门禁完全通过”作为 PR 前结论 | 本轮 Job 不扩大修复 infra/notice/workflow/system/authorization 等模块；按 `mango-docs/plans/2026-06-07-job-quality-tooling-rule-alignment-issue.md` 单独治理，或由 PMO 确认基线差异模式 |

## 9. 本轮修正记录

- Worker 治理按钮权限从 `V43__native_job_menu_names.sql` 拆分到 `V44__native_job_worker_governance_permissions.sql`。原因是本地验证库已执行过早期 V43，继续修改同一 migration 会触发 Flyway checksum 校验失败。
- 本地 `mango_dev_a1ce46` 仅对 `flyway_schema_history_authorization` 的 V43 checksum 做一次验证环境修正，以恢复本 worktree 启动；正式交付以 V43 菜单命名、V44 Worker 治理权限两个增量 migration 为准。
- 重启后本地后端 `http://127.0.0.1:18657` 健康检查通过，`primary` 和 `job` 两个 MySQL 数据源均为 `UP`，授权 V44 已执行成功。
- 新增 `job-scheduler-stability.spec.ts` 作为可重复的每分钟 Cron 稳定性 E2E。2026-06-07 本地以 `JOB_STABILITY_MINUTES=3` 跑通真实登录、任务创建、启用、调度实例查询、重复窗口断言、失败实例断言和日志详情断言，报告见 `mango-docs/evidence/2026-06-07-mango-native-job-e2e/job-scheduler-stability-local.md`。该结果不替代预发 2-4 小时长跑。
- 2026-06-07 补跑全仓 `mvn mango:check -Drule=all`，结果在根模块失败并报告 19672 个跨模块历史规则 issue；本轮按用户要求不扩大到 infra/common/其它平台模块，已追加到质量工具规则对齐 Issue。
- 2026-06-07 清理 `job-management.spec.ts` 的 `@typescript-eslint/no-non-null-assertion` 告警，改为显式断言辅助函数。随后 `job-management.spec.ts` 与 `job-scheduler-stability.spec.ts` ESLint 无告警，完整 Job 管理 E2E 重新执行通过 `9 passed (2.2m)`。
- 2026-06-07 清理 Job 模块旧 `target` 目录后重新确认 PowerJob 源码/产物路径扫描为空；后端 Job 聚合测试再次通过 `BUILD SUCCESS`，25 tests；Job 聚合 checkstyle/PMD 再次返回 `BUILD SUCCESS`。
- 2026-06-07 PR #101 更新后补跑 `pnpm -F mango-admin build` 通过，完整 Job 管理 E2E 再次通过 `9 passed (2.5m)`。
