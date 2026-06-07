# Mango 原生 Job Engine 最终特性目标与技术设计

## 1. 目标

Mango Job 的最终目标是提供 Mango 原生任务调度能力，而不是把第三方调度平台包装成 Mango 页面。

最终决策：

- Mango 自己拥有任务定义、调度游标、执行实例、执行尝试、Worker、日志、事件、告警和审计事实。
- PowerJob 只作为能力参考和迁移参考，不作为默认运行时、不作为事实源、不作为第一实现依赖。
- 后台统一入口为 `平台能力 -> 任务管理`。
- 数据库使用独立 `mango_job`，不和 Mango 主库做跨库外键或跨库事务。
- 支持灵活部署：单体内嵌 JobCenter + Worker、单体多实例、独立 JobCenter + 远程 Worker。
- 业务代码只知道 Mango Job 契约，不感知 Worker 在本进程、同集群其它进程或远程微服务。

本设计替代 `mango-docs/designs/mango-job-design.md` 中“底层优先集成 PowerJob 作为调度执行引擎”的路线。旧文档保留为历史设计，不再作为 Job 后续实现依据。

## 2. 设计输入

- 用户确认：按 Mango 原生 Job Engine 方向改造，PowerJob 只作为能力参考。
- 用户要求：不要为了验收写临时代码、虚假代码；日志、实例、Worker、频次执行必须真实可见。
- 用户要求：单体部署必须支持，单体多实例也必须安全；微服务部署时调度器能找到执行器。
- 用户要求：部署模式切换对业务任务定义无感知。
- 用户要求：结构化参数表单，不要求业务用户直接编辑 JSON。
- 用户要求：执行实例后面直接看日志，不保留独立执行日志菜单。
- 用户要求：告警直接接入 `mango-notice`。
- 用户要求：Mango 数据库按 `mango_{moduleName}` 统一前缀，Job 模块使用 `mango_job`。
- PowerJob 资料复核日期：2026-06-06。

PowerJob 官方资料复核：

- PowerJob README 定位为分布式调度与计算框架，提供 Web UI、任务状态监控和在线日志。
- PowerJob 支持 CRON、固定频率、固定延迟、OpenAPI 四类定时策略。
- PowerJob 支持单机、广播、Map、MapReduce 四类执行模式。
- PowerJob 支持 DAG 工作流、Spring Bean/Java/Shell/Python 处理器、失败重试、高可用和横向扩展。
- PowerJob Worker 与 Server 通信不是 UDP；其远程通信层主要支持 `AKKA` 和 `HTTP` 协议，Worker 侧通过 `protocol` 配置选择。老版本 Worker 默认偏 `AKKA`，4.3.0 以后 Server 侧内部通信转向 HTTP，HTTP 是新通信框架方向。
- GitHub Release 页面可见 `PowerJob-V5.1.2`。
- Maven Central 查询 `tech.powerjob:powerjob-worker` 当前返回最新版本 `5.1.1`。两者存在发布介质差异，说明 Mango 原生引擎设计不能绑定 PowerJob 运行版本。

参考资料：

- `https://github.com/PowerJob/PowerJob`
- `https://github.com/PowerJob/PowerJob/releases`
- `https://search.maven.org/search?q=tech.powerjob`
- `https://www.yuque.com/powerjob/guidence/intro`

## 3. 范围

本设计覆盖：

- Mango Job 最终特性目标。
- PowerJob 能力参考矩阵。
- 原生 Job Engine 架构。
- 后端模块边界。
- 前端菜单和页面模型。
- 数据模型。
- 状态机。
- 调度、分发、租约、重试、超时、取消和补偿。
- Worker 注册、心跳、能力上报和手动上下线。
- 日志采集、查询、归档和敏感数据处理。
- 多租户、权限、审计、告警和可观测性。
- 部署模式。
- 从当前 PowerJob Adapter 方案迁移到原生引擎的策略。
- 验收和测试计划。

## 4. 不做什么

- 不把 PowerJob Server、PowerJob Worker 或 PowerJob Console 作为 Mango Job 默认运行时。
- 不让 Mango UI 直接读取 PowerJob 内部表作为事实源。
- 不在 API、VO、Command、前端类型和业务处理器契约中暴露 PowerJob 概念。
- 不承诺严格 exactly-once。Mango Job 提供至少一次执行，业务处理器必须按幂等键处理重复执行。
- 不做跨 `mango` 主库和 `mango_job` 的外键、跨库 join 或跨库强事务。
- 首轮不开放脚本任务、任意 HTTP 任务、DAG 工作流和 MapReduce。
- 首轮不做 XXL-JOB、Quartz、PowerJob 适配器。
- 不保留独立“执行日志”运营菜单。日志作为执行实例详情的一部分。

## 5. PowerJob 能力参考矩阵

| PowerJob 能力 | Mango 是否吸收 | Mango 设计结论 |
|---|---:|---|
| Web UI 管理任务、实例、日志 | 是 | Mango 自研统一 UI，接入租户、权限、菜单和审计。 |
| CRON、固定频率、固定延迟、OpenAPI | 是 | Mango 首轮支持 CRON、固定频率、一次性、手动/API 触发；固定延迟按执行完成后再触发进入后续。 |
| 单机执行 | 是 | 作为 `STANDALONE` 执行模式。 |
| 广播执行 | 后续 | 需要 task fan-out 和多 Worker 结果聚合，首轮不做。 |
| Map/MapReduce | 后续 | 需要子任务拆分、聚合状态和大任务治理，首轮不做。 |
| DAG 工作流 | 后续 | 由 Mango 工作流或后续 Job Workflow 统一设计，首轮不做。 |
| Worker 注册和心跳 | 是 | Mango 原生 Worker 注册表、心跳、能力、容量和上下线。 |
| 失败重试 | 是 | 原生 attempt 重试，明确最大次数、退避、失败终态。 |
| 超时和故障转移 | 是 | 通过 task lease、fencing token、Worker 过期回收实现。 |
| 在线日志 | 是 | 原生日志 chunk + index，不读取第三方内部日志表。 |
| 高可用调度中心 | 是 | 多 JobCenter 节点通过数据库行锁/CAS 抢占调度游标和任务租约。 |
| 脚本处理器 | 暂不吸收 | 安全风险高，首轮禁止。 |
| 第三方告警通道 | 部分吸收 | Job 对失败实例提交 `mango-notice` 通知请求，通知模板、接收人规则和第三方通道统一由 `mango-notice` 维护。 |

结论：PowerJob 的能力证明了 Mango Job 应具备哪些企业级调度能力，但当前问题恰恰来自“外部引擎是事实源，Mango 再同步一份”。Mango 后续不再沿用该模型。

## 6. Mango Job 最终特性目标

### 6.1 任务定义

- 支持任务编码、任务名称、所属应用、所属模块、所属租户、处理器、参数 schema、默认参数、频次、并发策略、超时策略、重试策略和告警策略。
- 支持 `CRON`、`FIXED_RATE`、`ONE_TIME`、`MANUAL`。
- 支持启用、暂停、禁用、删除。
- 结构化参数表单由 `param_schema` 驱动，保存仍使用 JSON 对象，不要求业务用户直接编辑 JSON 字符串。
- 任务定义不绑定部署模式，不绑定 Worker 地址，不绑定第三方引擎 ID。

### 6.2 调度与执行

- JobCenter 负责扫描调度游标、创建实例、创建执行尝试、分发任务、回收租约、处理超时、执行重试、取消和补偿。
- Worker 负责注册、心跳、拉取任务、执行业务处理器、上报进度、上报日志、上报结果。
- 执行语义为至少一次执行，平台提供 `idempotency_key`，业务处理器必须基于该键保证业务幂等。
- 多 JobCenter 节点同时部署时，必须通过行锁或 CAS 保证同一调度窗口只创建一个实例。
- Worker 写终态必须携带最新 `fencing_token`，旧 Worker、超时 Worker、失联后恢复的 Worker 不能覆盖新状态。

### 6.3 Worker 治理

- Worker 可自动注册，也可在后台手动登记、停用、下线、排空。
- Worker 上报 `app_code`、环境、实例 ID、地址、协议、标签、版本、容量、当前负载、支持的 handler。
- Worker 状态包括在线、排空中、离线、过期、禁用。
- 单体内嵌 Worker 和远程 Worker 使用同一 Worker 模型；差异只在 transport。

### 6.4 日志与实例

- 执行实例是一次业务运行。
- 执行尝试是一次实际派发到 Worker 的执行动作。
- 一次实例可以有多次尝试，因此实例与日志是 1:N。
- 日志展示放在执行实例详情和行内日志按钮中，不单独作为运营列表菜单。
- 日志必须包含 `System.out`、`System.err`、业务 logger、平台事件和处理器返回结果。
- 日志支持按实例、attempt、时间、级别、关键字分页查询。
- 日志存储支持 chunk、索引、归档、截断和敏感字段脱敏。

### 6.5 UI 与权限

菜单结构：

```text
平台能力
└── 任务管理
    ├── 任务定义
    ├── 执行实例
    ├── Worker 节点
    ├── 运行状态
    └── 告警规则
```

页面要求：

- 任务定义：搜索区使用紧凑表单；任务名称下拉；频次用结构化控件；参数用 schema 表单；提供启用、暂停、触发、编辑、删除。
- 执行实例：列表优先展示任务名称、任务编码、状态、触发来源、计划触发时间、开始/结束时间、耗时、Worker、重试次数；行内提供日志、重试、取消。
- Worker 节点：展示真实 Worker，不能显示 `N/A` 节点；支持手动添加、禁用、排空、下线；显示应用、地址、版本、容量、负载、最近心跳、正在执行数、失败数、支持 handler。
- 运行状态：展示 JobCenter 节点、调度器开关、当前节点角色、数据库 lease、扫描延迟、实例积压、异常游标、失败率和超时数。
- 告警规则：支持任务级规则和应用级默认规则；只配置通知场景、模板编码、启用策略、收件规则、单用户和多用户结构化字段；具体通道由 `mango-notice` 负责。

权限要求：

- 页面、按钮和数据查询接入 Mango 权限。
- 查询按租户隔离。
- 管理 Worker、取消实例、重试实例、修改告警属于高风险操作，需要独立权限码。

## 7. 总体架构

```text
业务模块
  └─ MangoJobHandler / MangoJobApi

mango-job-api
  ├─ Command / Query / VO / enums
  ├─ MangoJobHandler
  └─ MangoJobContext / MangoJobHandleResult

mango-job-core
  ├─ Definition Service
  ├─ Schedule Cursor Service
  ├─ JobCenter Service
  ├─ Dispatch Service
  ├─ Worker Registry Service
  ├─ Attempt State Machine
  ├─ Log Service
  ├─ Alarm Event Service
  └─ Operation Audit Service

mango-job-starter
  ├─ JobCenter auto configuration
  ├─ Embedded Worker auto configuration
  ├─ Transport auto configuration
  ├─ Flyway migration
  └─ Menu / permission seed

mango-job-worker-starter
  ├─ Worker register / heartbeat
  ├─ Task pull / result report
  ├─ Console/log bridge
  └─ Handler registry

mango-ui/packages/job
  ├─ Task Definition
  ├─ Instance Detail + Log
  ├─ Worker Nodes
  ├─ JobCenter Nodes
  └─ Alarm Rules
```

依赖方向：

- 业务模块只依赖 `mango-job-api` 或 `mango-job-worker-starter`。
- `mango-job-api` 不依赖 core、starter、数据库、Controller 和 UI。
- `mango-job-core` 持有领域服务和 MyBatis-Plus Mapper。
- `mango-job-starter` 负责自动配置，不承载领域逻辑。
- 前端页面只访问 Mango Job API，不访问 Worker 私有地址，不访问第三方引擎。

## 8. 核心模型

### 8.1 `mango_job_definition`

任务定义。

关键字段：

- `id`
- `tenant_id`
- `app_code`
- `owner_service`
- `worker_group`
- `module_code`
- `job_code`
- `job_name`
- `handler_name`
- `handler_version`
- `schedule_type`
- `schedule_expression`
- `timezone`
- `param_schema`
- `param_value`
- `concurrency_policy`
- `misfire_policy`
- `timeout_seconds`
- `retry_policy`
- `max_retry_count`
- `status`
- `version`
- `created_by`
- `created_at`
- `updated_by`
- `updated_at`
- `deleted`

约束：

- `uk_job_definition_code`: `tenant_id, app_code, job_code, deleted`。
- `owner_service` 默认等于 `app_code`，表达任务所属服务。
- `worker_group` 默认等于 `owner_service`，表达同一服务下的执行池隔离。
- 任务启用和调度时必须存在可用 handler capability。

### 8.2 `mango_job_schedule_cursor`

调度游标。

关键字段：

- `job_id`
- `tenant_id`
- `schedule_version`
- `last_fire_time`
- `next_fire_time`
- `misfire_policy`
- `lock_owner`
- `lock_until`
- `last_scan_at`

约束：

- `uk_schedule_cursor_job`: `job_id`。
- JobCenter 抢占游标必须使用行级锁或 CAS 更新 `lock_owner/lock_until/schedule_version`。

### 8.3 `mango_job_instance`

执行实例，表示一次业务运行。

关键字段：

- `id`
- `tenant_id`
- `job_id`
- `job_code`
- `job_name_snapshot`
- `trigger_type`
- `trigger_user_id`
- `trigger_batch_no`
- `idempotency_key`
- `scheduled_fire_time`
- `actual_fire_time`
- `start_time`
- `end_time`
- `status`
- `attempt_count`
- `next_retry_time`
- `retry_reason`
- `duration_millis`
- `trace_id`
- `error_summary`
- `result_summary`

约束：

- `uk_instance_idempotency`: `tenant_id, job_id, idempotency_key`。
- 定时触发的 `idempotency_key` 按 `job_id + schedule_version + scheduled_fire_time` 生成。
- 手动/API 触发必须由请求生成批次号，支持重复提交防抖。

### 8.4 `mango_job_attempt`

执行尝试，表示一次派发给 Worker 的实际执行。

关键字段：

- `id`
- `tenant_id`
- `instance_id`
- `job_id`
- `attempt_no`
- `worker_id`
- `worker_address_snapshot`
- `status`
- `lease_owner`
- `lease_until`
- `fencing_token`
- `dispatch_time`
- `start_time`
- `last_heartbeat_at`
- `end_time`
- `exit_code`
- `error_summary`
- `result_payload`

约束：

- `uk_attempt_no`: `instance_id, attempt_no`。
- Worker 上报结果必须匹配 `attempt_id + fencing_token`。
- Attempt 租约过期后，JobCenter 可标记 `LOST` 并按策略创建新 attempt。

### 8.5 `mango_job_worker_snapshot`

Worker 快照表。

关键字段：

- `id`
- `tenant_id`
- `app_code`
- `service_code`
- `worker_group`
- `worker_address`
- `runtime_address`
- `transport_type`
- `register_source`
- `instance_id`
- `engine_type`
- `engine_worker_id`
- `status`
- `last_heartbeat_at`

约束：

- Worker 快照唯一键为 `tenant_id + service_code + worker_group + engine_type + worker_address`。
- 内嵌 Worker 地址由当前进程生成，当前实现格式为 `in-memory://{host}/embedded-{pid}@{host}`。
- 远程 Worker 地址使用 `http(s)://...`。
- `transport_type` 是调度选择 transport 的事实字段，地址前缀只作为历史兼容回退。
- Worker 不能只靠配置推断在线，必须有真实心跳。

### 8.6 `mango_job_worker_capability`

Worker 能力表。

关键字段：

- `worker_id`
- `service_code`
- `worker_group`
- `app_code`
- `job_code`
- `handler_name`
- `handler_version`
- `param_schema_hash`
- `enabled`

用途：

- JobCenter 分发任务前按 `tenantId + ownerService + workerGroup + appCode + handlerName + jobCode` 选择 Worker。
- UI 展示“哪些 Worker 可以执行这个任务”。
- `job_code` 存储为空串表示该 handler 不限制具体任务编码；指定值时只允许执行对应 `jobCode`。

约束：

- Worker 快照唯一键为 `tenant_id + service_code + worker_group + engine_type + worker_address`。
- Worker 能力唯一键为 `worker_id + service_code + worker_group + app_code + handler_name + job_code`。
- 同一个远程地址可以按不同 `service_code + worker_group` 注册为不同 Worker。

### 8.7 `mango_job_log_index` 和 `mango_job_log_chunk`

日志索引和日志分片。

`mango_job_log_index`：

- `id`
- `tenant_id`
- `instance_id`
- `attempt_id`
- `job_id`
- `status`
- `first_log_time`
- `last_log_time`
- `line_count`
- `size_bytes`
- `truncated`
- `archive_file_id`

`mango_job_log_chunk`：

- `id`
- `tenant_id`
- `instance_id`
- `attempt_id`
- `sequence_no`
- `log_time`
- `level`
- `logger_name`
- `thread_name`
- `content`
- `content_hash`
- `redacted`

约束：

- 业务表不保存文件预览或下载 URL。归档只保存 `file_id`。
- 敏感参数必须在入库前脱敏。

### 8.8 `mango_job_event`

append-only 事件表。

事件类型：

- `JOB_CREATED`
- `JOB_ENABLED`
- `SCHEDULE_FIRED`
- `INSTANCE_CREATED`
- `ATTEMPT_DISPATCHED`
- `ATTEMPT_STARTED`
- `LOG_APPENDED`
- `ATTEMPT_SUCCEEDED`
- `ATTEMPT_FAILED`
- `ATTEMPT_TIMEOUT`
- `INSTANCE_SUCCEEDED`
- `INSTANCE_FAILED`
- `WORKER_REGISTERED`
- `WORKER_HEARTBEAT_LOST`
- `ALARM_TRIGGERED`

用途：

- 排障。
- 审计。
- 告警异步消费。
- 后续指标聚合。

### 8.9 告警和审计

- `mango_job_alarm_rule`: 告警规则。
- `mango_job_alarm_record`: 告警触发记录。
- `mango_job_operation_log`: 用户操作审计。

通知发送由 `mango-notice` 完成。Job 模块在失败实例终态后发出通知场景、模板编码、模板参数和接收方表达式，不直接管理短信、邮件或企业微信通道。

## 9. 状态机

### 9.1 任务定义状态

```text
DRAFT -> ENABLED -> PAUSED -> ENABLED
      -> DISABLED -> DELETED
```

规则：

- `DRAFT` 不参与调度。
- `ENABLED` 必须有有效调度表达式和 handler capability。
- `PAUSED` 保留定义和游标，不产生新实例。
- `DISABLED` 停止调度，允许保留历史实例。
- `DELETED` 软删。删除前必须停止调度，运行中实例按用户选择取消或保留。

### 9.2 实例状态

```text
CREATED -> WAITING -> DISPATCHED -> RUNNING -> SUCCESS
                                      -> RETRY_WAITING -> WAITING
                                      -> FAILED
                                      -> TIMEOUT
                                      -> CANCELED
```

规则：

- 实例代表业务运行，一次实例可有多次 attempt。
- `FAILED/TIMEOUT` 是否进入 `RETRY_WAITING` 由 retry policy 决定。
- `RETRY_WAITING` 到期后回到 `WAITING`，创建下一次 attempt。
- 取消实例时，JobCenter 先停止新 attempt，再通知已租约 Worker 取消。

### 9.3 Attempt 状态

```text
READY -> LEASED -> RUNNING -> SUCCEEDED
                         -> FAILED
                         -> TIMED_OUT
                         -> LOST
                         -> CANCELED
```

规则：

- `LEASED` 必须有 `lease_until` 和 `fencing_token`。
- Worker 开始执行后进入 `RUNNING`。
- 只有最新 `fencing_token` 可以写终态。
- 租约过期且 Worker 心跳丢失时进入 `LOST`。

### 9.4 Worker 状态

```text
REGISTERED -> ONLINE -> DRAINING -> OFFLINE
                         -> EXPIRED
             -> DISABLED
```

规则：

- `ONLINE` 来自真实心跳。
- `DRAINING` 不接新任务，等待当前 attempt 结束。
- 心跳超时进入 `EXPIRED`，其租约由 JobCenter 回收。
- 手动禁用后不能自动接单。

## 10. 调度设计

### 10.1 调度扫描

JobCenter 周期扫描 `mango_job_schedule_cursor`。

流程：

1. 查询 `next_fire_time <= now` 且任务为 `ENABLED` 的游标。
2. 使用数据库行级锁或 CAS 抢占游标；首轮优先数据库 lease/CAS，不引入独立 leader election 服务。
3. 按 `misfire_policy` 计算本次触发窗口。
4. 生成 `idempotency_key`。
5. 插入 `mango_job_instance`，依赖唯一约束防重复。
6. 更新 `last_fire_time` 和 `next_fire_time`。
7. 发布 `SCHEDULE_FIRED` 和 `INSTANCE_CREATED` 事件。

### 10.2 Misfire 策略

首轮支持：

- `SKIP`: 错过的调度窗口跳过，只计算下一次。
- `FIRE_ONCE`: 错过多次只补一次。
- `FIRE_ALL`: 逐个补偿错过窗口，但受最大补偿次数保护。

### 10.3 并发策略

首轮支持：

- `ALLOW`: 允许同一任务多个实例并发。
- `SKIP_IF_RUNNING`: 有运行中实例时跳过本次。
- `QUEUE`: 保留实例，等待前序结束后分发。

后续候选：

- `CANCEL_PREVIOUS`: 新实例取消旧实例。

## 11. 分发与租约设计

JobCenter 分发流程：

1. 查询 `WAITING` 实例。
2. 根据租户、任务归属、应用、处理器、任务编码、能力启用状态和 Worker 在线状态选择 Worker。
3. 创建 `mango_job_attempt`。
4. 写入 `lease_owner`、`lease_until`、`fencing_token`。
5. 将实例置为 `DISPATCHED`。
6. 通过 transport 投递任务或等待 Worker 拉取。
7. Worker 确认后 attempt 进入 `RUNNING`，实例进入 `RUNNING`。

租约规则：

- Worker 执行期间必须定期续租。
- JobCenter 扫描租约过期 attempt。
- attempt 过期时，先标记 `LOST` 或 `TIMED_OUT`，再按 retry policy 创建新 attempt。
- 旧 Worker 即使恢复，也不能用旧 token 写成功。

## 12. Worker 设计

Worker 有两种启动方式：

- 内嵌 Worker：随 Mango 应用启动，适合单体部署。
- 远程 Worker：业务服务引入 `mango-job-worker-starter`，向 JobCenter 注册。

Worker 能力：

- 注册和续约。
- 上报 handler capability。
- 拉取任务或接受推送任务。
- 执行业务 `MangoJobHandler`。
- 捕获 `System.out`、`System.err` 和 logger 输出。
- 批量上报日志 chunk。
- 上报进度和结果。
- 响应取消、排空和下线。

业务处理器契约：

```java
public interface MangoJobHandler {
    default String appCode() {
        return null;
    }

    default String serviceCode() {
        return appCode();
    }

    default String workerGroup() {
        return serviceCode();
    }

    default Set<String> supportedJobCodes() {
        return Set.of();
    }

    String handlerName();

    MangoJobHandleResult handle(MangoJobContext context);
}
```

归属规则：

- 任务定义必须保存 `ownerService` 和 `workerGroup`；未配置时分别回退到 `appCode` 和 `ownerService`。
- Worker 启动时必须注册 `serviceCode`、`workerGroup`、`appCode`、`handlerName` 和可选 `supportedJobCodes`。
- JobCenter 派发前按 capability 过滤，禁止所有 Worker 共用一个全局队列后只按空闲随机派发。
- Worker 收到命令后必须按同一归属再次查找本进程 handler；归属不匹配时拒绝执行，不能猜测执行。
- 内嵌 Worker 只能由当前 Spring 容器真实 `MangoJobHandler` 自动注册，不允许手动添加。

`MangoJobContext` 至少包含：

- `tenantId`
- `appCode`
- `jobId`
- `jobCode`
- `instanceId`
- `attemptId`
- `triggerType`
- `triggerBatchNo`
- `idempotencyKey`
- `traceId`
- `parameter`
- `scheduledFireTime`
- `log`

业务获取参数：

- 通过 `context.getParameter()` 获取完整参数对象。
- 推荐通过结构化参数 binding 获取类型化字段。
- 禁止业务处理器直接读取 Job 表或第三方引擎上下文。

租户上下文：

- 定时触发没有 Web 请求，JobCenter 必须从任务定义和实例中生成 `tenantId`、`appCode`、system actor 和 `traceId`。
- Worker 执行前由 Mango Job runtime 恢复租户上下文和 trace 上下文。
- 业务处理器不显式传递 tenantId，不在 SQL 中手写租户过滤；应接入 Mango 统一租户能力。
- 用户手动触发时记录触发用户，Worker 执行仍以实例 tenantId 为准。

## 13. Transport 抽象

统一接口：

```text
MangoJobTransport
├── registerWorker
├── heartbeat
├── acquireTask
├── acknowledgeStart
├── appendLogs
├── reportProgress
├── reportResult
└── cancelAttempt
```

首轮必须实现：

- `IN_MEMORY`: 同进程内存调用，用于单体内嵌 JobCenter + Worker。单体部署禁止为了复用远程链路再绕一圈 HTTP、TCP 端口或本机回环地址。
- `HTTP_INTERNAL`: Mango 内部调用安全机制，用于独立 JobCenter 和远程 Worker。微服务部署时 Worker 可以通过独立端口和 JobCenter 通信。

通信选择规则：

- 单体内嵌：默认且必须使用 `IN_MEMORY`。JobCenter 直接调用本进程 Worker runtime，不开放 Worker 独立通信端口。
- 单体多实例：每个进程内的 JobCenter 到本进程 Worker 使用 `IN_MEMORY`；跨进程任务分发可以通过数据库租约选择目标 Worker，由目标进程本地 Worker 执行，必要时再启用 `HTTP_INTERNAL` 做跨进程控制。
- 独立 JobCenter + 远程 Worker：必须使用 `HTTP_INTERNAL`，Worker 通过独立端口注册、心跳、拉取任务、上报日志和结果。
- 部署模式切换只改变 transport 和启动配置，不改变任务定义、handler、参数 schema 和调度策略。
- `IN_MEMORY` 和 `HTTP_INTERNAL` 都必须走同一套实例、attempt、租约、fencing token、日志和状态机，不能因为内存调用绕过治理模型。

后续候选：

- MQ transport。
- gRPC transport。

不采用：

- 不采用 UDP。
- 首轮不采用 Akka，避免引入 ActorSystem、NAT/容器端口暴露和协议栈复杂度。

## 14. 部署模式

### 14.1 单体内嵌

```text
Mango Admin Process
├── JobCenter
└── Embedded Worker
```

适用：

- 本地开发。
- 单体部署。
- 小规模业务。

要求：

- `IN_MEMORY` transport。
- 不启动 Worker 独立端口。
- 不通过 HTTP、本机回环地址或 TCP 远程协议绕行。
- 仍写 `mango_job`。
- 仍使用租约和状态机，不能因为同进程就绕过核心模型。

### 14.2 单体多实例

```text
Mango Admin #1 ─┐
Mango Admin #2 ─┼── mango_job
Mango Admin #3 ─┘
```

每个实例可同时启动 JobCenter 和 Worker。

要求：

- 调度游标抢占防重复触发。
- Worker 注册使用实例 ID 区分。
- 分发使用租约和 fencing token。
- 本进程 JobCenter 调本进程 Worker 使用 `IN_MEMORY`。
- 需要跨进程控制、取消或远程拉取时使用 `HTTP_INTERNAL`，并明确开放端口。
- 多节点同时调度不重复创建同一窗口实例。

### 14.3 独立 JobCenter + 远程 Worker

```text
Mango JobCenter Cluster ─── mango_job
          │
          ├── Worker: mango-system
          ├── Worker: mango-job
          └── Worker: business-service
```

适用：

- 微服务部署。
- 专门的调度中心。
- 执行能力独立扩缩容。

要求：

- Worker 使用 `HTTP_INTERNAL` 注册、心跳和任务交互。
- Worker 使用独立端口对 JobCenter 暴露内部通信接口，端口来自部署配置或服务注册。
- 服务身份、租户上下文和权限边界明确。
- Worker 不直接写实例终态，必须通过 JobCenter API 且携带 token。

## 15. API 设计

API 分组：

- 任务定义 API。
- 触发 API。
- 执行实例 API。
- Worker API。
- JobCenter API。
- 告警规则 API。
- 日志 API。

关键接口：

| 接口 | 用途 |
|---|---|
| `POST /job/definitions` | 创建任务定义。 |
| `PUT /job/definitions/{id}` | 修改任务定义。 |
| `POST /job/definitions/{id}/enable` | 启用。 |
| `POST /job/definitions/{id}/pause` | 暂停。 |
| `POST /job/definitions/{id}/disable` | 禁用。 |
| `POST /job/definitions/{id}/trigger` | 手动触发。 |
| `GET /job/instances` | 查询执行实例。 |
| `GET /job/instances/{id}` | 实例详情。 |
| `GET /job/instances/{id}/attempts` | 实例尝试列表。 |
| `GET /job/instances/{id}/logs` | 实例日志。 |
| `POST /job/instances/{id}/cancel` | 取消实例。 |
| `POST /job/instances/{id}/retry` | 重试实例。 |
| `GET /job/workers` | Worker 列表。 |
| `POST /job/workers/{id}/drain` | Worker 排空。 |
| `POST /job/workers/{id}/disable` | Worker 禁用。 |
| `GET /job/runtime-status` | 运行状态，包含 JobCenter、数据库 lease、积压、失败率和 Worker 总览。 |

内部 Worker API：

| 接口 | 用途 |
|---|---|
| `POST /job/internal/workers/register` | Worker 注册。 |
| `POST /job/internal/workers/heartbeat` | 心跳。 |
| `POST /job/internal/tasks/acquire` | 拉取任务。 |
| `POST /job/internal/attempts/{id}/start` | 开始执行。 |
| `POST /job/internal/attempts/{id}/logs` | 上报日志。 |
| `POST /job/internal/attempts/{id}/result` | 上报结果。 |

## 16. 多租户、安全和审计

- 所有用户 API 按 Mango 登录态和租户上下文过滤。
- Worker 内部 API 使用 Mango 内部调用安全机制。
- Worker 注册必须带服务身份、appCode、envCode 和签名。
- Worker 上报结果必须带 `attempt_id + fencing_token`。
- 参数 schema 可标记敏感字段，日志和审计入库前脱敏。
- 操作审计记录创建、编辑、启停、触发、取消、重试、Worker 上下线、告警变更。
- Job 模块不保存文件访问 URL；日志归档保存 `file_id`。

## 17. 告警与可观测性

告警场景：

- 任务失败。
- 任务超时。
- 连续失败。
- Worker 离线。
- 调度延迟超过阈值。
- 实例积压超过阈值。
- 失败后恢复。

告警降噪：

- 支持连续失败阈值。
- 支持同一任务同一错误窗口聚合。
- 支持静默窗口。
- 支持恢复通知。
- 接收人来源可以是固定用户、角色、部门、任务负责人或表达式。

指标：

- 任务总数、启用数、暂停数。
- 实例成功率、失败率、超时率。
- 调度延迟。
- 队列积压。
- Worker 在线数、容量、负载。
- 日志写入延迟。

通知：

- Job 写告警事件和告警记录，失败实例会按启用的告警规则提交 `mango-notice`。
- `mango-notice` 根据模板编码和模板参数发送系统消息、短信、邮件、企业微信等。

## 18. 迁移策略

当前仓库曾存在 PowerJob Adapter 方案的代码、DDL、E2E 和截图证据。当前交付已废弃 PowerJob 运行时集成，迁移必须避免新旧模型混跑。

迁移步骤：

1. 标记旧设计和旧交付台账为历史路线。
2. 新增原生引擎 DDL，避免继续扩展 PowerJob 映射字段。
3. 删除 `PowerJobEngineAdapter`、PowerJob 自动配置、PowerJob SDK 依赖和对应测试包。
4. API 和 UI 改为原生实例、attempt、Worker、日志事实。
5. 示例任务改为 `IN_MEMORY` 和 `HTTP_INTERNAL` 两种 transport 验证。
6. E2E 重新覆盖每 1 分钟 CRON、手动触发、日志输出、失败重试、Worker 下线恢复。
7. PowerJob 相关运行时代码不进入默认发布路径；历史调研资料只保留为能力参考。

兼容策略：

- 如果已有测试环境产生 PowerJob 映射数据，不做线上自动迁移。
- 原生引擎上线前清理测试数据或提供一次性 migration 工具，工具必须单独评审。
- 不把第三方 engine ID 继续放到新 API 和 UI。

## 19. 测试和验收

### 19.1 单元测试

- Cron/fixed rate/one-time 下一次触发计算。
- Misfire 策略。
- 并发策略。
- 状态机合法/非法迁移。
- 幂等键生成。
- attempt 租约续期、过期、fencing token。
- 日志脱敏。
- Worker capability 匹配。

### 19.2 集成测试

- MyBatis-Plus 真实访问 `mango_job` 数据源。
- 多 JobCenter 并发扫描同一游标只创建一个实例。
- Worker 注册、心跳、拉取、执行、日志上报、结果上报。
- Worker 重复心跳注册按租户、应用、引擎和地址幂等更新，不能产生重复 Worker 快照。
- Worker 失联后 attempt 回收和重试。
- 单体内嵌模式和 HTTP 远程模式都使用同一任务定义。
- 独立 JobCenter Spring Context 与独立 Worker Spring Context 通过 `HTTP_INTERNAL` 完成注册、派发、Java Handler 执行和日志回传。

### 19.3 E2E 验收场景

必须保留截图和后台日志证据：

1. 创建每 1 分钟 CRON 任务，结构化表单配置频次和参数。
2. 等待至少 3 个调度窗口，执行实例列表出现 3 次调度。
3. 打开实例日志，看到 `System.out`、`System.err`、logger 和返回结果。
4. 手动触发任务，实例行展示触发来源为手动。
5. 停用 Worker 后任务不再分发到该 Worker。
6. Worker 心跳过期，运行中 attempt 被标记 LOST 并按策略重试。
7. 单体多实例并发启动，不重复创建同一调度窗口实例。
8. 权限不足用户不能触发、取消、禁用 Worker。
9. 失败任务触发 `mango-notice` 告警记录。
10. 租户 A 创建任务后，租户 B 看不到定义、实例、日志和租户任务上下文。
11. 远程 Worker 注册、心跳、拉取任务、上报日志和结果都经过 Mango 内部调用安全机制。
12. `mango_job` 独立库可迁移、可备份、可清理，主库不保存 Job 治理表。

## 20. 分阶段交付建议

### Sprint A：原生模型和旧方案下线设计

- 新原生引擎 DDL。
- API 契约和状态机。
- 旧 PowerJob Adapter 运行时代码下线方案。
- 单元测试覆盖状态机、游标、幂等键。

### Sprint B：JobCenter 和内嵌 Worker

- 调度扫描。
- 实例和 attempt。
- `IN_MEMORY` transport。
- 不开放 Worker 独立端口。
- 日志捕获和查询。
- 单体内嵌 E2E。

### Sprint C：多实例安全

- 游标 CAS/行锁。
- 租约续期。
- fencing token。
- Worker 过期回收。
- 单体多实例集成测试。

### Sprint D：远程 Worker

- `HTTP_INTERNAL` transport。
- Worker starter。
- 内部调用安全。
- 远程 Worker E2E。

当前状态：`HTTP_INTERNAL` transport、远程 Worker starter 自动装配、内部执行 Controller、Worker 注册器和后端跨 Spring Context E2E 已完成。生产级内部调用鉴权策略仍按安全组件统一评审，不作为浏览器人工验收前置项。

### Sprint E：统一 UI 和告警

- 任务定义、执行实例、Worker、运行状态、告警规则。
- 实例行内日志。
- `mango-notice` 接入。
- 权限和租户 E2E。

### Sprint F：生产验收

- 压测。
- 长时间 Cron 稳定性。
- 日志保留和归档。
- 发布文档。
- 回滚方案。

## 21. 设计红线

- 禁止把 PowerJob、XXL-JOB、Quartz 作为 Mango Job 第一实现引擎。
- 禁止调度事实分裂为 Mango 一份、第三方引擎一份。
- 禁止 UI 显示第三方引擎 ID、N/A Worker 或不可解释状态。
- 禁止没有 attempt、租约、fencing token 就宣称支持多实例和故障转移。
- 禁止没有日志 chunk/index 就宣称支持执行日志。
- 禁止没有真实 Worker 心跳就宣称 Worker 在线。
- 禁止部署模式改变时要求业务任务定义换类型。
- 禁止用 mock、临时代码、固定成功结果作为验收。

## 22. 开放问题

以下问题不阻塞设计，但进入 Sprint 前必须逐项确认：

- Cron 解析库选型：优先复用 Spring CronExpression 还是引入独立库。
- 日志 chunk 单行最大长度、单次上报最大大小、归档阈值。
- `IN_MEMORY` transport 中 `System.out/System.err` 捕获的线程隔离策略。
- Worker 手动添加和自动注册冲突时的优先级。
- `QUEUE` 并发策略的最大排队数量和过期策略。
- 日志保留天数、单实例最大日志量、单 chunk 大小和归档阈值。
