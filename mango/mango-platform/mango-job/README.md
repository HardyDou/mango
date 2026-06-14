# Mango Job

## 1. 能力定位

`mango-job` 提供 Mango 原生任务调度、任务定义、执行实例、Worker 节点、运行日志、告警规则和远程 Worker 注册能力。主要使用者是需要平台化定时任务、手动任务和任务治理的 Mango 开发者与业务开发者。

代码事实：

- 聚合模块 `io.mango.platform.job:mango-job`。
- 子模块包括 `mango-job-api`、`mango-job-support`、`mango-job-core`、`mango-job-starter`、`mango-job-starter-remote`。
- 本地 Controller 路径为 `/job`。
- 远程 Feign Client 服务名为 `mango-job`，路径为 `/job`。

## 2. 适用场景

- 管理 CRON、固定频率、一次性、手动触发任务。
- 单体内嵌 Worker、单体多实例 Worker、独立 JobCenter 加远程 Worker。
- 查询任务定义、执行实例、运行日志、Worker 快照和告警规则。
- Worker 自动注册、远程 Worker 手动登记、Worker 上下线治理。
- 需要把任务菜单接入 Mango 管理端。

## 3. 不适用场景

- 不再接入 PowerJob Server、PowerJob Worker 或 PowerJob 表结构。
- 不替代业务模块的任务处理器实现和业务幂等逻辑。
- 不负责通知通道本身，失败告警通知依赖 notice 能力。
- 不作为通用分布式工作流引擎使用，审批流程归属 `mango-workflow`。

## 4. 模块边界

`mango-job` 负责任务治理模型、调度运行时、Worker 注册、执行归档和告警规则。业务模块负责实现具体 handler、业务事务和幂等控制；前端页面由 `mango-ui/packages/job` 提供。

## 5. 接入方式

JobCenter 或本地任务服务接入：

```xml
<dependency>
    <groupId>io.mango.platform.job</groupId>
    <artifactId>mango-job-starter</artifactId>
</dependency>
```

远程 Worker 或远程调用接入：

```xml
<dependency>
    <groupId>io.mango.platform.job</groupId>
    <artifactId>mango-job-starter-remote</artifactId>
</dependency>
```

只使用契约模型时依赖 `mango-job-api`。

部署示例可参考 `deploy/job/application-job-native.yml`。

## 6. 配置项

已发现配置前缀：

- `mango.job`：Job 自动配置开关，`JobAutoConfiguration` 默认匹配启用。
- `mango.job.probe`：任务探测开关。
- `mango.job.native`：原生任务运行时配置，来源 `MangoNativeJobProperties`。

典型配置包括内嵌 Worker、调度器、通信方式、Worker 分组、数据库路由和远程 Worker 地址。远程 Worker 关键字段包括 `job-center-address`、`worker-address`、`worker-heartbeat-interval-millis`、`job-center-feign-url`、`worker-feign-url`。

远程 Worker 注册需同时确认 JobCenter 入口和 Worker 执行入口。仓库代码中存在 `/job/internal/workers/register` 与 `/job/job/internal/workers/register` 兼容处理，接入时应以当前部署暴露的实际路径为准并记录验证结果。

## 7. 对外接口 / 扩展点

- `MangoJobApi`：任务定义、实例、日志、Worker、告警规则、引擎状态 API。
- `MangoJobApi` 中包含 Worker 注册接口。
- `MangoJobWorkerApi`：Worker 执行接口。
- `MangoJobController`：HTTP 路径 `/job`。
- `MangoJobWorkerInternalController`：Worker 内部路径 `/job/internal/workers/execute`。
- `MangoJobFeignClient`、`MangoJobWorkerFeignClient`：远程调用。
- 服务能力覆盖任务定义、查询、Worker 注册、引擎同步、告警规则和操作日志。

## 8. 数据库 / 初始化数据

Flyway 路径：`mango-job-core/src/main/resources/db/migration/mango-job`。

核心表：

- `mango_job_definition`
- `mango_job_instance`
- `mango_job_log_index`
- `mango_job_worker_snapshot`
- `mango_job_alarm_rule`
- `mango_job_engine_mapping`
- `mango_job_operation_log`
- `mango_job_schedule_cursor`
- `mango_job_attempt`
- `mango_job_worker_capability`
- `mango_job_log_chunk`
- `mango_job_event`

迁移 `V6__seed_default_sample_jobs.sql` 包含默认示例任务数据，应按环境确认是否保留或关闭样例。

## 9. 菜单 / 权限 / 租户

任务管理菜单和按钮权限属于 job 能力资产。当前仓库存在 job starter resource manifest；authorization 中 `V40__job_menu.sql`、`V41__retire_job_handler_menu.sql`、`V42__retire_job_log_menu.sql`、`V43__job_instance_sync_permission.sql`、`V44__native_job_menu_names.sql`、`V45__native_job_worker_governance_permissions.sql`、`V46__native_job_alarm_rule_permissions.sql` 是历史兼容迁移事实。

前端入口由 `mango-ui/packages/job` 提供任务定义、执行实例、Worker、运行状态和告警规则页面。

## 10. 验证方式

最小验证命令：

```bash
mvn -f mango/pom.xml -pl mango-platform/mango-job -am test
```

代表性验收：

- 新建任务定义后可分页查询。
- 手动触发任务后生成执行实例和日志。
- Worker 注册后可在 Worker 页面查询。
- 告警规则启用后失败实例进入告警链路。

## 11. 业务接入最小闭环

业务模块接入任务时先实现 handler，并确认 handler 名称、Worker 分组和任务定义一致。单体内嵌 Worker 只接本地 starter；远程 Worker 需要配置 job center 地址、worker 地址、心跳和 Feign URL，并验证 Worker 注册接口可达。

任务 handler 内部负责业务幂等、事务边界和租户上下文恢复。验收断言覆盖：Worker 在线、手动触发生成实例和日志、失败任务按重试策略记录 attempt、重复触发不会破坏业务幂等、告警规则只在匹配条件下进入 notice 链路。

## 12. 常见问题

- 任务不触发时检查 `mango.job.enabled`、`mango.job.native.scheduler-enabled`、Worker 分组和 handler 名称。
- Worker 不在线时检查注册接口、服务名、通信方式和心跳。
- 日志不可见时检查实例是否生成、日志 chunk 是否归档、前端是否跳转到实例详情。

## 13. 关联 PMO 规则

- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [持久化规范](../../../mango-pmo/rules/backend/07-persistence.md)
- [模块菜单规范](../../../mango-pmo/rules/backend/11-module-menu.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 14. 历史设计 / 交付记录

- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
- [Job 部署说明](../../../deploy/job/README.md)
