# Mango Job 任务调度设计说明

## 1. 目标

为 Mango 增加统一任务调度能力。设计结论是 Mango 提供原生 Job 契约、治理数据、权限菜单和统一 UI，底层优先集成 PowerJob 作为调度执行引擎。

目标能力：

- 用 Mango 的租户、权限、菜单、审计、模块机制管理任务。
- 用独立 `mango_job` 数据库存放 Job 模块数据，和 Mango 主库隔离。
- 支持 PowerJob 内部表与 `mango_job` 同库共置，也支持独立 `powerjob` 数据库或 schema。
- 支持单体部署 `Job Center + Worker`、独立 Job Center、远程 Worker 等部署形态。
- 保留底层引擎可替换能力，避免业务代码直接绑定 PowerJob 或 XXL-JOB。
- 前后端同步交付，后台菜单、权限、接口和页面由 Mango 统一承载。

## 2. 设计输入

- 用户确认路线：做 Mango 原生 Job 契约和统一 UI，底层优先集成 PowerJob。
- 用户要求：灵活部署；数据库独立；前后端同步集成。
- 用户确认：PowerJob 使用最新稳定版本，当前实现基线锁定为 `5.1.2`，升版前复核 Maven 坐标、Server 镜像、安全公告和兼容 API。
- 版本复核：2026-06-05 复核 GitHub Release 和 Maven Central，`5.1.2` 是当前最新 Release / Central 版本；公开安全条目提示 PowerJob 5.1.2 OpenAPI Groovy 注入风险，因此 Mango 默认不开放脚本/工作流类高危能力，PowerJob Server 必须置于内网、启用令牌和访问控制。
- 用户确认：优先复用 Mango 内部调用安全机制；PowerJob Server 边界仍保留引擎访问令牌和网络隔离配置。
- 用户确认：菜单入口为 `平台能力 -> 任务管理`。
- 用户确认：告警通知直接接入 `mango-notice`，Job 只配置消息模板和启用策略。
- 前置设计：`mango-docs/designs/mango-multi-datasource-foundation-design.md`。
- 前置实现：`mango-infra-persistence-starter` 已支持多数据源、模块数据源映射、Flyway 隔离和 MyBatis-Plus 真实路由。
- 文档治理 Issue：`https://github.com/HardyDou/mango/issues/98`。
- 外部资料复核日期：2026-06-05。

## 3. 范围

本设计覆盖：

- 主流 Job 组件调研和选型。
- Mango Job 模块边界。
- Job 原生契约。
- PowerJob Adapter 集成边界。
- 数据库和表模型。
- 后端 API。
- 菜单、页面和权限。
- 部署形态。
- 运行态、日志、告警和可观测性。
- 测试和发布策略。

## 4. 不做什么

- 不自研调度算法、时间轮、分片调度和执行心跳。
- 不复制 PowerJob 或 XXL-JOB 的源码到 Mango。
- 不直接改造 PowerJob Server 内部表。
- 不让业务模块直接调用 PowerJob API。
- 不在 Mango 主库保存 Job 治理表。
- 不做跨库外键或跨库 join。
- 不把 PowerJob Console 作为 Mango 交付后台页面。
- 不用 Quartz 作为默认分布式调度引擎。
- 不默认开放脚本任务；HTTP 任务首轮只允许白名单内受控调用。

## 5. 主流 Job 组件调研

| 组件 | 定位 | 优点 | 限制 | Mango 判断 |
|---|---|---|---|---|
| PowerJob | 企业级分布式任务调度和计算中间件 | 支持分布式执行、任务实例、Worker、Server、控制台，适合 Java 微服务调度治理 | 需要独立 Server 和引擎库；原生控制台不承载 Mango 租户、权限、菜单 | 作为优先底层引擎 |
| XXL-JOB | 分布式任务调度平台 | 轻量、使用广、接入简单，已有 `xxl-job-core` 依赖管理 | 模型偏执行器和调度中心，工作流、复杂实例治理和 Mango 统一 UI 适配成本较高 | 作为备选 Adapter |
| Quartz | Java 调度库 | 成熟、嵌入式、支持 JDBC JobStore 和集群 | 不是完整微服务治理平台；缺少 Mango 需要的统一后台、执行器治理、租户权限和跨应用运维模型 | 只适合轻量单体或测试兜底 |
| ElasticJob | 分布式调度生态 | 分片、弹性、Java SPI，适合数据分片任务 | 治理 UI 和 Mango 后台融合仍需自建，生态方向和 Mango 契约需要再适配 | 备选研究项 |
| JobRunr | JVM 后台任务库 | API 轻，支持延迟、周期、重试、持久化和 Dashboard | 更偏应用内后台任务，复杂调度中心、租户治理和统一菜单仍需自建 | 不作为第一引擎 |
| Spring Batch | 批处理框架 | 批处理领域成熟，JobRepository 记录执行元数据 | 不是通用调度平台，需要外部触发和运维层 | 作为任务执行实现，不作为调度引擎 |
| Temporal | 工作流平台 | 长事务、可靠工作流、重试和状态恢复强 | 引入平台复杂度高，领域偏工作流编排，不是后台定时任务的轻量选择 | 不纳入第一阶段 |
| Apache Airflow | 数据工作流平台 | DAG 和数据调度能力强 | Python/Data 平台属性强，和 Mango Java 后台治理不匹配 | 不纳入 Mango 后台 Job |

结论：

- Mango 不应自研调度轮子。
- Mango 不应直接仿制 PowerJob 或 XXL-JOB。
- Mango 应抽象自己的任务治理契约和 UI，把 PowerJob 作为第一引擎 Adapter。
- XXL-JOB 保留为第二 Adapter，避免现有依赖管理浪费。
- Quartz 不能独立满足 Mango 微服务任务治理要求，但可作为轻量内嵌运行模式的候选。

参考资料：

- PowerJob GitHub：`https://github.com/PowerJob/PowerJob`
- XXL-JOB 官方文档：`https://www.xuxueli.com/xxl-job/index.html`
- Quartz 官方文档：`https://www.quartz-scheduler.org/documentation`
- ElasticJob 官方文档：`https://shardingsphere.apache.org/elasticjob/current/en/features/job-type/`
- JobRunr 官方文档：`https://www.jobrunr.io/en/documentation/`
- Spring Batch 官方文档：`https://docs.spring.io/spring-batch/reference/job/configuring-repository.html`
- Temporal 官方文档：`https://docs.temporal.io/`
- Apache Airflow 调度文档：`https://airflow.apache.org/docs/apache-airflow/stable/concepts/scheduler.html`

资料使用边界：

- 外部资料只用于组件能力判断和选型依据。
- Mango 长期规则仍以 `mango-pmo` 为唯一规范源。
- 第三方组件版本、API 和部署方式在进入实现前必须重新确认官方文档和 Maven 坐标。

## 6. Job 应具备的核心指标

| 指标 | 设计要求 |
|---|---|
| 调度能力 | 支持 cron、固定间隔、一次性触发、手动触发、暂停、恢复、禁用。 |
| 分布式能力 | 支持多 Worker、故障转移、实例状态、并发控制和执行超时。 |
| 租户隔离 | 任务定义、执行记录、日志查询和操作权限按租户隔离。 |
| 权限控制 | 页面、按钮、任务操作和执行器管理接入 Mango 权限。 |
| 参数模型 | 支持 JSON 参数、表单化参数 schema、敏感参数脱敏。 |
| 执行记录 | 保存调度实例、运行状态、耗时、触发来源、错误摘要和引擎映射。 |
| 日志 | 支持执行日志索引、日志拉取、日志保留和失败定位。 |
| 告警 | 支持失败、超时、连续失败、恢复通知，接入 Mango Notice。 |
| 幂等 | 任务业务实现必须自己处理幂等，平台记录幂等键和触发批次。 |
| 数据库隔离 | `mango` 主库和 `mango_job` Job 独立库边界清晰；PowerJob 可同库共置或独立库/schema。 |
| 部署弹性 | 单体 `Job Center + Worker`、独立 Job Center、远程 Worker 均能配置。 |
| 可替换性 | 业务只依赖 Mango Job API，不依赖 PowerJob 内部模型。 |
| 可观测性 | 暴露任务数量、实例状态、失败率、耗时、队列和 Worker 健康。 |
| 安全接入 | 引擎地址、认证信息、回调令牌和数据库密码只能来自环境变量、配置中心或部署密钥。 |

## 7. 模块边界

建议新增 `mango-platform/mango-job`，按现有平台模块结构拆分：

| 模块 | 职责 |
|---|---|
| `mango-job-api` | 对业务模块暴露 Job 契约、Command、Query、VO、枚举和 `MangoJobApi`。 |
| `mango-job-core` | 任务定义、任务实例、执行摘要、引擎映射、权限校验和业务服务。 |
| `mango-job-starter` | 自动配置、菜单种子、Flyway migration、PowerJob Adapter 装配。 |
| `mango-job-starter-remote` | 微服务消费侧远程调用适配。 |

建议新增引擎适配层：

| 适配层 | 职责 |
|---|---|
| `MangoJobEngine` | Mango 内部统一引擎 SPI。 |
| `PowerJobEngineAdapter` | 将 Mango Job 契约映射到 PowerJob app、job、instance、worker。 |
| `XxlJobEngineAdapter` | 第二引擎候选，不进入首轮开发范围。 |
| `QuartzJobEngineAdapter` | 轻量内嵌候选，不进入首轮开发范围。 |

依赖方向：

```text
业务模块 -> mango-job-api

mango-job-core -> mango-job-api
               -> mango-infra-persistence
               -> mango-authorization-api / mango-system-api / mango-notice-api

mango-job-starter -> mango-job-api
                  -> mango-job-core
                  -> mango-infra-web-starter
                  -> PowerJob Adapter

mango-job-starter-remote -> mango-job-api
                          -> mango-infra-feign-starter

PowerJob Adapter -> PowerJob Server
```

禁止反向依赖：

- `mango-job-api` 禁止依赖 `mango-job-core`、PowerJob、数据库对象、Controller 或 Feign Client。
- `mango-job-core` 禁止依赖 PowerJob 类型；只能依赖 `MangoJobEngine` SPI。
- `mango-job-starter-remote` 禁止依赖 `mango-job-core`。
- PowerJob Adapter 不得反向污染 `mango-job-api`。
- PowerJob 依赖只能位于 `mango-job-starter` 的 Adapter 实现包或后续独立 adapter 模块。
- PowerJob 类型不得出现在 `api`、`core`、VO、Command、Query、前端类型和数据库表字段类型中。
- Mango 禁止直连 `powerjob` 库读取或修改 PowerJob 内部表。
- 前端页面不得直接访问 PowerJob Server。
- 业务模块不得直接保存 PowerJob jobId 或 instanceId。

## 8. 业务任务处理器契约

业务模块通过 Mango Job 契约注册可执行处理器，不直接注册 PowerJob Processor。

建议契约：

```java
public interface MangoJobHandler {
    String handlerName();

    MangoJobHandleResult handle(MangoJobHandleContext context);
}
```

处理器注册要求：

- `handlerName` 在 `app_code` 内唯一。
- 处理器元数据包含所属模块、参数 schema、是否允许并发、默认超时和默认重试策略。
- 业务处理器只能接收 Mango 上下文，不能接收 PowerJob 内部上下文。
- 处理器执行时必须带租户、操作者、traceId 和触发来源。
- 处理器必须由业务实现保证幂等；平台记录触发批次、实例 ID 和幂等参考键。
- 远程业务模块通过 `mango-job-starter-remote` 注册处理器元数据，并由 Job Center 通过远程 API 触发。

处理器类型：

| 类型 | 说明 |
|---|---|
| `BUILTIN` | 当前 Mango 应用内 Spring Bean 处理器。 |
| `REMOTE_API` | 远程 Mango 服务暴露的任务处理器。 |
| `HTTP` | 受控 HTTP 任务，只允许配置白名单内服务地址。 |
| `SCRIPT` | 脚本任务，首轮不启用，只保留扩展枚举和禁用校验。 |
| `ENGINE_NATIVE` | 引擎原生任务，仅运维可见，不作为业务默认入口。 |

## 9. 原生 Job 契约

### 9.1 任务定义

`MangoJobDefinition` 表达 Mango 内部任务定义：

| 字段 | 说明 |
|---|---|
| `id` | Mango 任务 ID。 |
| `tenant_id` | 租户 ID。 |
| `app_code` | 所属逻辑应用。 |
| `job_code` | 任务编码，租户和应用内唯一。 |
| `job_name` | 任务名称。 |
| `job_type` | `BUILTIN`、`HTTP`、`REMOTE_API`、`SCRIPT`、`ENGINE_NATIVE`。 |
| `schedule_type` | `CRON`、`FIXED_RATE`、`ONE_TIME`、`MANUAL`。 |
| `schedule_expression` | cron 或间隔表达式。 |
| `handler_name` | 内置处理器或远程处理器名称。 |
| `param_schema` | 参数表单 schema。 |
| `param_value` | 默认参数 JSON。 |
| `misfire_strategy` | 错过触发策略。 |
| `concurrency_policy` | 并发策略。 |
| `timeout_seconds` | 执行超时。 |
| `retry_policy` | 重试策略 JSON。 |
| `status` | `DRAFT`、`ENABLED`、`DISABLED`、`PAUSED`。 |
| `engine_type` | `POWERJOB`、`XXL_JOB`、`QUARTZ`。 |
| `engine_app_id` | 引擎应用标识。 |
| `engine_job_id` | 引擎任务标识。 |
| `sync_status` | `PENDING`、`SYNCED`、`FAILED`，表示 Mango 与引擎侧同步状态。 |

唯一约束和索引：

- `uk_job_tenant_app_code`：`tenant_id`、`app_code`、`job_code` 唯一。
- `idx_job_status`：`tenant_id`、`status`、`updated_at`。
- `idx_job_engine`：`engine_type`、`engine_app_id`、`engine_job_id`。

### 9.2 任务实例

`MangoJobInstance` 表达一次调度运行：

| 字段 | 说明 |
|---|---|
| `id` | Mango 实例 ID。 |
| `job_id` | Mango 任务 ID。 |
| `trigger_type` | `SCHEDULED`、`MANUAL`、`RETRY`、`API`。 |
| `trigger_user_id` | 手动触发人。 |
| `trigger_time` | 触发时间。 |
| `start_time` | 开始时间。 |
| `end_time` | 结束时间。 |
| `status` | `WAITING`、`RUNNING`、`SUCCESS`、`FAILED`、`TIMEOUT`、`CANCELED`。 |
| `duration_millis` | 执行耗时。 |
| `engine_instance_id` | 引擎实例 ID。 |
| `error_summary` | 错误摘要。 |
| `trace_id` | 链路追踪 ID。 |
| `trigger_batch_no` | 触发批次号，用于幂等和排障。 |

索引：

- `idx_instance_job_time`：`tenant_id`、`job_id`、`trigger_time`。
- `idx_instance_status_time`：`tenant_id`、`status`、`trigger_time`。
- `idx_instance_engine`：`engine_type`、`engine_instance_id`。

### 9.3 Worker

`MangoJobWorker` 表达执行器运行态快照：

| 字段 | 说明 |
|---|---|
| `id` | Worker 记录 ID。 |
| `app_code` | 所属逻辑应用。 |
| `worker_address` | Worker 地址。 |
| `engine_type` | 引擎类型。 |
| `engine_worker_id` | 引擎 Worker ID。 |
| `last_heartbeat_at` | 最近心跳。 |
| `status` | `ONLINE`、`OFFLINE`、`UNKNOWN`。 |

## 10. 数据库设计

### 10.1 数据库边界

| 数据库 | 所属 | 说明 |
|---|---|---|
| `mango` | Mango 主应用 | 菜单、权限、租户、用户、组织等主数据。 |
| `mango_job` | Mango Job | 按 `mango_{module}` 规则由 `mango-job` / `job` 归一化得到；存放任务定义、实例摘要、日志索引、告警配置、引擎映射；简单部署时也可承载 PowerJob 内部表。 |
| `powerjob` | PowerJob | 可选独立库或 schema，存放 PowerJob Server 内部表，由 PowerJob 管理。 |

约束：

- `mango_job` 使用 `mango.persistence.modules.mango-job.datasource=job`。
- `mango_job` 是 Job 模块物理库名；数据源 key 可继续使用短名 `job`，避免配置过长。
- `mango_job` 不跨库外键引用主库。
- Mango 不维护 `powerjob` migration。
- `powerjob` 数据源只用于部署校验、运维展示和 PowerJob Server 配置说明；如果采用同库共置，则指向同一个物理库但使用 PowerJob 自有表前缀、schema 或数据库账号边界。
- 默认推荐 `mango` 和 `mango_job` 两个物理库；`mango_job` 归属于 Job 模块，并遵循 `mango-pmo/rules/backend/07-persistence.md` 的数据库命名规则。
- 高隔离生产环境可把 PowerJob 内部表放入独立 `powerjob` 库或独立 schema。
- 单库降级只用于本地开发、小规模私有部署或用户明确接受风险的环境；降级不改变模块默认声明，仍由多数据源底座在未注册 `job` 数据源时回退 `primary`。
- Mango Job 表和 PowerJob 内部表即使在同一物理库，也禁止跨表外键、跨表 join、混用 migration 或互相读取内部表。

### 10.1.1 PowerJob 与 `mango_job` 合并边界

PowerJob 可以与 `mango_job` 使用同一个物理数据库，作为 Job 模块的共置部署模式。该模式用于降低单体部署和私有化部署成本，但不改变逻辑边界：

- Mango Flyway 只管理 `mango_job_*` 表。
- PowerJob Server 只管理 PowerJob 内部表。
- 两类表使用不同前缀、schema 或数据库账号权限区分所有权。
- Mango 后端不直接查询或修改 PowerJob 内部表，只通过 `PowerJobEngineAdapter` 访问引擎能力。
- 备份、容量评估和巡检可以按同一个 Job 数据库处理，但发布、迁移和故障恢复记录必须区分 Mango Job 与 PowerJob 两类对象。

### 10.2 Mango Job 表

首轮建议建表：

| 表 | 说明 |
|---|---|
| `mango_job_definition` | 任务定义。 |
| `mango_job_instance` | 执行实例摘要。 |
| `mango_job_log_index` | 日志索引和拉取位置。 |
| `mango_job_worker_snapshot` | Worker 快照。 |
| `mango_job_alarm_rule` | 告警规则。 |
| `mango_job_engine_mapping` | Mango 对引擎 app/job/instance 的映射。 |
| `mango_job_operation_log` | 启停、触发、修改、删除等操作记录。 |

所有 Mango Job 治理表默认满足 persistence 表结构准入：`id`、审计字段、`tenant_id`。确实属于运行态快照且不适合审计字段的表，需要在 migration 中写明 `mango-check` 豁免原因。

表约束：

- 任务定义必须有租户、应用、任务编码唯一约束。
- 执行实例必须按任务、状态、触发时间建立查询索引。
- 引擎映射必须按引擎类型、引擎 app、引擎 job、引擎 instance 建立查询索引。
- 操作日志不得保存完整敏感参数，只保存脱敏后的摘要。

### 10.3 Flyway

迁移路径：

```text
db/migration/mango-job/V{version}__{description}.sql
```

模块元数据：

```properties
module-name=mango-job
module-path=job
persistence-datasource=job
```

部署默认：

```yaml
mango:
  persistence:
    modules:
      mango-job:
        datasource: job
```

## 11. 后端 API 设计

API 前缀建议使用：

```text
/job
```

| API | 方法 | 说明 | 权限 |
|---|---|---|---|
| `/job/definitions/page` | GET | 分页查询任务定义 | `job:definition:query` |
| `/job/definitions` | POST | 新建任务定义 | `job:definition:create` |
| `/job/definitions/{id}` | GET | 查看任务详情 | `job:definition:view` |
| `/job/definitions/{id}` | PUT | 修改任务定义 | `job:definition:update` |
| `/job/definitions/{id}` | DELETE | 删除任务定义 | `job:definition:delete` |
| `/job/definitions/{id}/enable` | POST | 启用任务 | `job:definition:enable` |
| `/job/definitions/{id}/disable` | POST | 禁用任务 | `job:definition:disable` |
| `/job/definitions/{id}/pause` | POST | 暂停任务 | `job:definition:pause` |
| `/job/definitions/{id}/resume` | POST | 恢复任务 | `job:definition:resume` |
| `/job/definitions/{id}/trigger` | POST | 手动触发 | `job:definition:trigger` |
| `/job/instances/page` | GET | 分页查询执行实例 | `job:instance:query` |
| `/job/instances/{id}` | GET | 查看实例详情 | `job:instance:view` |
| `/job/instances/{id}/cancel` | POST | 终止运行实例 | `job:instance:cancel` |
| `/job/logs/page` | GET | 查询日志索引 | `job:log:query` |
| `/job/logs/{instanceId}` | GET | 查看执行日志 | `job:log:view` |
| `/job/workers/page` | GET | 查询 Worker | `job:worker:query` |
| `/job/handlers` | GET | 查询任务定义可用执行动作 | `job:definition:query` |
| `/job/alarms/page` | GET | 分页查询告警规则 | `job:alarm:query` |
| `/job/alarms` | POST | 新建告警规则 | `job:alarm:create` |
| `/job/alarms/{id}` | PUT | 修改告警规则 | `job:alarm:update` |
| `/job/alarms/{id}` | DELETE | 删除告警规则 | `job:alarm:delete` |
| `/job/engines/status` | GET | 引擎连接状态 | `job:engine:view` |

API 模型命名：

- `CreateJobDefinitionCommand`
- `UpdateJobDefinitionCommand`
- `JobDefinitionPageQuery`
- `JobDefinitionVO`
- `TriggerJobCommand`
- `JobInstancePageQuery`
- `JobInstanceVO`
- `JobWorkerPageQuery`
- `JobWorkerVO`
- `JobAlarmRulePageQuery`
- `JobAlarmRuleVO`
- `JobHandlerVO`

## 12. PowerJob Adapter

### 12.1 集成方式

Mango 不复用 PowerJob Console。Mango 后端通过 Adapter 调用 PowerJob Server 或客户端能力：

```text
Mango UI -> Mango Job API -> Mango Job Core -> PowerJob Adapter -> PowerJob Server
```

Adapter 负责：

- 创建或更新 PowerJob app。
- 创建或更新 PowerJob job。
- 启停任务。
- 手动触发任务。
- 查询实例状态。
- 拉取日志。
- 同步 Worker 状态。
- 把 PowerJob 错误转为 Mango 业务异常。
- 校验 PowerJob Server 认证、版本和连接状态。

Worker 桥接负责：

- Mango 应用按配置启动 PowerJob Worker。
- PowerJob job 的 `processorInfo` 固定映射为 Mango 统一 processor 名称。
- 统一 processor 从 PowerJob `jobParams` 和 `instanceParams` 解析 Mango 元数据，再派发给业务 `MangoJobHandler`。
- 业务处理器只感知 `MangoJobHandleContext`，不感知 PowerJob `TaskContext`。
- Worker 默认关闭，只有 `mango.job.powerjob.worker.enabled=true` 时才启动，避免普通 Mango 应用因缺少 PowerJob Server 配置启动失败。

完整 PowerJob 集成的判定标准：

- PowerJob Server 真实启动并使用 `mango_job` 共置库或独立 `powerjob` 库/schema。
- Mango Worker 真实注册到 PowerJob Server。
- 创建任务后 Mango 任务有真实 `engine_job_id`，PowerJob Server 有对应任务。
- 手动或定时触发后 Mango 实例有真实 `engine_instance_id`。
- 实例状态从 `WAITING` 同步到 `RUNNING` 或终态，不允许长期停留在待执行。
- Mango 执行日志页面能查看本次执行的日志索引、摘要或引擎日志。
- 前端 E2E 覆盖任务定义、频次配置、触发、实例、日志、Worker、引擎状态，并保留截图证据。

当前实现验证：

- external PowerJob Server + Mango 进程内 Worker 已完成真实联调。
- PowerJob Server 使用 `powerjob/powerjob-server:v5.1.2`，PowerJob 内部表与 Mango Job 治理表共置在 `mango_job` 物理库，但由 PowerJob Server 自行管理内部表。
- Mango Worker 以 `appId=1`、`appName=mango-job`、`port=27777` 注册并上报心跳。
- 三个示例任务已同步到 PowerJob，手动任务 `engineJobId=17`、Cron 任务 `engineJobId=18`、固定频率任务 `engineJobId=19`。
- 手动示例任务触发后，Mango 实例 `engineInstanceId=943410988542066752`，状态为 `SUCCESS`；PowerJob `instance_info.status=5`，执行结果为 `Mango Job runtime probe executed`。
- Mango UI 执行实例和执行日志页面可看到同一次执行的任务、批次、实例和引擎实例信息。
- 验收截图保存在 `mango-docs/evidence/2026-06-05-mango-job-ui-e2e`。

### 12.2 映射规则

| Mango | PowerJob |
|---|---|
| `app_code` | PowerJob appName 或 appId 映射。 |
| `job_code` | PowerJob jobName 或 external key。 |
| `MangoJobDefinition.id` | `engine_job_id` 关联。 |
| `MangoJobInstance.id` | `engine_instance_id` 关联。 |
| `tenant_id` | Mango 自有隔离字段，不写入 PowerJob 内部表作为强依赖。 |

PowerJob 内部状态不直接暴露给前端，必须转换为 Mango 状态枚举。

### 12.3 配置模型

```yaml
mango:
  job:
    enabled: true
    engine:
      type: powerjob
      powerjob:
        server-address: http://powerjob-server:7700
        app-name: mango-job
        datasource: job
        access-token: ${POWERJOB_ACCESS_TOKEN}
    worker:
      enabled: true
      app-code: internal-admin
```

`datasource: job` 表示 PowerJob 内部表与 Job 模块数据库共置。独立部署时可改为 `powerjob`，但仍不代表 Mango 直接管理 PowerJob 内部表。

认证要求：

- Mango UI 只访问 Mango Job API，复用 Mango 登录态、租户、菜单、按钮权限和审计能力。
- Mango 服务间内部调用优先复用 Mango 现有内部调用安全机制；`mango-job-api` 如暴露内部接口，可使用 `@Inner` 或 infra-web 内部访问能力约束调用方。
- PowerJob Server 仍按引擎边界配置 `access-token`、内网 ACL 或部署网络隔离。
- 远程 Worker 回调 Job Center 时必须携带 Mango 内部调用凭证或等效服务身份，禁止裸露公网无鉴权接口。

### 12.4 一致性和补偿

Mango Job 操作会同时影响 `mango_job` 治理数据和 PowerJob 引擎数据，不能使用跨库事务。处理策略：

- 先写 Mango 操作意图和 `sync_status=PENDING`。
- Adapter 调用 PowerJob 成功后更新映射和 `sync_status=SYNCED`。
- Adapter 调用失败时记录 `sync_status=FAILED`、错误摘要和可重试操作。
- 后台提供引擎状态和同步失败列表，允许运维重试同步。
- 删除任务采用先停用再删除的保护流程，避免 Mango 删除成功但引擎仍在调度。
- 手动触发使用 `trigger_batch_no` 防止重复提交造成多次不可识别触发。

## 13. 灵活部署形态

| 形态 | App 依赖 | 调用路径 | 适用场景 |
|---|---|---|---|
| 单体部署 `Job Center + Worker` | 应用依赖 `mango-job-starter` 和业务模块 starter | 本地 Controller 路径 `/{module-path}/...`，业务处理器本地执行 | 开发、轻量项目、私有化单体 |
| 独立 Job Center | Job Center 应用依赖 `mango-job-starter`；业务应用依赖 `mango-job-starter-remote` 或 `mango-job-api` | 业务应用通过 Feign 访问 Job Center；Job Center 通过远程处理器回调业务应用 | 多业务模块共用任务治理 |
| 远程 Worker | Worker 应用依赖业务模块 starter 和 Job Worker 装配 | Worker 注册处理器元数据，接收 Job Center 或 PowerJob 触发 | 高负载、网络隔离或资源隔离执行环境 |
| 微服务 + 共享 PowerJob Server | Job 服务依赖 `mango-job-starter`；业务服务依赖 `mango-job-starter-remote` | 正向调用路径使用 `/job/...`；远程处理器回调使用 `/_job/...` 或目标模块反向路径 | 企业级部署 |
| 单库降级部署 | 应用依赖 `mango-job-starter`，只注册 `primary` 数据源 | `mango-job` 默认数据源未注册时回退 `primary` | 本地开发、小规模或用户接受风险的环境 |

部署要求：

- 不同部署形态不改变后端菜单、权限和前端页面。
- 前端只调用 Mango Job API。
- 业务应用只依赖 `mango-job-api` 或远程 starter。
- PowerJob Server 地址、认证和数据库由部署配置提供。
- `mango-job-starter` 提供 `META-INF/mango/module.properties`，`module-name=mango-job`，`module-path=job`。
- `mango-job-starter-remote` 不声明 `module.properties`，远程目标通过 `mango-infra-module` 解析。
- Feign adapter 必须放在 `mango-job-starter-remote`，并实现 `MangoJobApi`。
- 单体 `Job Center + Worker` 不表示把 PowerJob Server 源码导入 Mango Boot 3 应用上下文；PowerJob Server 使用官方镜像、发行包、子进程或 sidecar，由 Mango 启动脚本和配置统一管理。
- 多节点单体允许每个 Mango 节点启动 Worker；JobCenter 必须是同一个 PowerJob Server 集群或同一组外部化 Server 节点，避免多个互不感知的调度中心重复调度。
- 当前实跑形态是 external PowerJob Server + Mango Worker；embedded-single 和 embedded-cluster 已有配置样例，人工验收时按同一 UI/API 和同一 PowerJob Server 集群原则验证。

## 14. 前端统一 UI

菜单建议：

```text
平台能力
  任务管理
    任务定义
    执行实例
    执行日志
    Worker
    引擎状态
```

页面：

| 页面 | 主要能力 |
|---|---|
| 任务定义 | 查询、新建、编辑、启用、禁用、暂停、恢复、手动触发、删除。 |
| 执行实例 | 查询状态、查看详情、终止运行、重试入口。 |
| 执行日志 | 按任务、实例、时间、状态查询日志。 |
| Worker | 查看 Worker 在线状态、地址、最近心跳、所属应用。 |
| 引擎状态 | 展示 PowerJob 连接、Server、数据库和 Adapter 状态。 |

说明：业务处理器是任务定义的执行动作字段和 Mango Job 后端契约，不作为 `平台能力 / 任务管理` 下的独立菜单。失败、超时、连续失败和恢复通知接入 `mango-notice`，不在 Job 模块单独提供通信平台配置页面。

UI 原则：

- 使用 Mango 现有后台菜单和权限体系。
- 页面注册到 `mango-ui/packages/admin-pages/src/defaults.ts`。
- 后端菜单 `component` 必须能命中页面注册表。
- 单体、Shell、微前端形态下菜单、权限和页面一致。
- 不显示 PowerJob 原生页面 iframe 作为正式交付。

## 15. 权限和租户

权限码按资源和动作拆分：

| 资源 | 权限码 |
|---|---|
| 任务定义 | `job:definition:query/create/view/update/delete/enable/disable/pause/resume/trigger` |
| 执行实例 | `job:instance:query/view/cancel/retry` |
| 日志 | `job:log:query/view` |
| Worker | `job:worker:query` |
| 告警 | `job:alarm:query/create/update/delete` |
| 引擎 | `job:engine:view` |

租户要求：

- 所有治理表默认带 `tenant_id`。
- 查询任务、实例、日志和告警规则必须按租户过滤。
- 手动触发任务时写入当前租户和操作者。
- Worker 属于运行态资源，可按应用和部署环境展示；涉及租户任务执行时实例记录必须有租户。
- PowerJob Server 访问令牌、回调令牌和 HTTP 任务敏感参数必须脱敏存储和脱敏展示。

## 16. 运行态和可观测性

必须暴露：

- 任务总数、启用数、失败数。
- 实例运行中、成功、失败、超时数量。
- Worker 在线、离线数量。
- PowerJob Server 连接状态。
- 最近失败任务列表。
- 执行耗时分布。
- 同步失败任务数量。
- 执行动作契约注册数量和不可用数量。

说明：`MangoJobHandler` 是任务定义的执行契约，属于“任务定义”表单中的执行动作字段，不作为 `平台能力 / 任务管理` 下的独立菜单。运行态运营页面展示 Worker、实例、日志和引擎状态，避免把 PowerJob Processor 等底层概念直接暴露给业务用户。

日志要求：

- Mango 保存日志索引和错误摘要。
- 详细执行日志可按 Adapter 从引擎侧拉取。
- 日志接口不得输出敏感参数。

告警要求：

- 接入 `mango-notice`，不在 Job 模块内直接适配短信、邮件、企业微信等通信平台。
- Job 模块只维护通知场景、模板编码、模板参数和启用策略。
- 支持失败、超时、连续失败和恢复通知；具体通道由 `mango-notice` 模板和租户配置决定。
- 告警记录写入 `mango_job`。

## 17. 测试范围

后端测试：

- Job 定义 CRUD。
- 状态流转：草稿、启用、禁用、暂停、恢复、删除。
- 手动触发。
- PowerJob Adapter 映射。
- 处理器注册和触发上下文。
- 引擎同步失败补偿。
- 多数据源：`mango-job` 表进入 `job` 数据源。
- 租户过滤和权限校验。
- 实例状态同步。
- 日志索引和日志拉取。
- 告警规则触发。

前端测试：

- 菜单可见性和权限按钮。
- 任务定义列表、表单、详情。
- 启停、暂停、恢复、触发操作。
- 实例和日志查询。
- Worker 和引擎状态页面。
- 任务定义表单可配置并回显执行动作。
- 单体和 Shell 菜单一致性。

集成测试：

- 启动 Mango 应用、`mango_job` 数据库和 PowerJob Server；独立 PowerJob 库/schema 部署时额外启动 `powerjob`。
- 创建任务后 PowerJob 侧存在对应任务。
- 手动触发后 Mango 实例记录和 PowerJob 实例状态一致。
- Worker 下线后页面状态更新。
- Adapter 同步失败后可查询失败状态并重试。

## 18. 验收标准

- 设计文档写清目标、范围、不做范围、影响模块、接口变化、数据变化、菜单/页面/权限变化、测试范围。
- 组件调研覆盖 PowerJob、XXL-JOB、Quartz、ElasticJob、JobRunr、Spring Batch、Temporal、Airflow。
- 选型结论明确：Mango 原生契约和统一 UI，PowerJob 优先 Adapter。
- 数据库独立策略明确：`mango` 主库与 `mango_job` Job 库隔离；PowerJob 内部表支持同库共置或独立库/schema。
- 业务处理器注册、触发上下文和引擎映射边界明确。
- 开发计划拆分到可执行 Sprint。
- 交付台账覆盖用户要求和设计原子项。

## 19. 风险与限制

| 风险 | 处理 |
|---|---|
| PowerJob API 或部署模型变化 | Adapter 隔离 PowerJob，Mango API 不暴露 PowerJob 内部模型。 |
| 统一 UI 覆盖 PowerJob 全部能力成本高 | 只纳入 Mango 任务治理必须能力，低频引擎内部能力只展示状态或链接运维说明。 |
| 跨库一致性诉求扩大 | Job 治理数据和业务数据通过 API、事件和实例记录协同，不做跨库事务。 |
| Worker 运行态与租户模型不完全一致 | 任务实例记录租户；Worker 以应用和部署维度展示。 |
| 旧 XXL-JOB 依赖造成路线误导 | 首轮以 PowerJob Adapter 为准，XXL-JOB 仅保留备选适配位。 |
| PowerJob Server 认证和网络边界不清 | 复用 Mango 内部调用安全机制保护 Mango 内部 API，PowerJob Server 边界保留访问令牌、内网 ACL 和健康检查。 |
