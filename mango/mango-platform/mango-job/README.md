# Mango Job

## 1. 概览
`mango-job` 是 Mango 的原生任务调度能力，负责把业务模块里的 `MangoJobHandler` 注册成可治理任务，并提供任务定义、调度扫描、Worker 注册、实例记录、执行日志、失败告警、菜单权限资源。

它不是单纯的管理页面，也不是外部调度器适配层。当前代码只支持 `MANGO_NATIVE` 原生引擎和 `BUILTIN` Spring Bean 处理器。

模块坐标和子模块：

| 子模块 | 用途 |
|--------|------|
| `io.mango.platform.job:mango-job-api` | API 契约、命令、查询、VO、枚举、`MangoJobHandler` 扩展点 |
| `io.mango.platform.job:mango-job-support` | Handler 注册表、本地执行器、日志捕获、Worker 内部执行接口、内存通信 |
| `io.mango.platform.job:mango-job-core` | 任务定义、调度运行时、实例、日志、Worker、告警、数据库 mapper/service |
| `io.mango.platform.job:mango-job-starter` | JobCenter/单体内嵌 Worker 自动配置、HTTP 管理接口、资源 manifest |
| `io.mango.platform.job:mango-job-starter-remote` | 远程 Worker 自动注册、Feign 调用、HTTP_INTERNAL 执行通道 |

## 2. 功能清单

| 能力 | 常用入口 |
|------|----------|
| 任务定义：创建、修改草稿、删除草稿、启用、暂停、禁用、手动触发 | Maven 依赖 / HTTP API / Java API |
| 调度类型：CRON、FIXED_RATE、ONE_TIME、MANUAL | Maven 依赖 / HTTP API / Java API |
| Worker 模式：单体同 JVM IN_MEMORY、远程 Worker HTTP_INTERNAL | Maven 依赖 / HTTP API / Java API |
| Handler 匹配：按 appCode + ownerService + workerGroup + handlerName + jobCode 查找可执行 Worker | Maven 依赖 / HTTP API / Java API |
| 运行记录：生成 mango_job_instance、mango_job_attempt、mango_job_log_index、mango_job_log_chunk | Maven 依赖 / HTTP API / Java API |
| 日志捕获：捕获 System.out、System.err 和 Logback 事件，写入 native log chunk | Maven 依赖 / HTTP API / Java API |
| 幂等键：调度任务按 jobId + version + scheduledFireTime 生成，手动/API 任务按 jobId + triggerBatchNo 生成 | Maven 依赖 / HTTP API / Java API |
| Worker 治理：自动心跳注册、手动登记远程 Worker、状态调整 | Maven 依赖 / HTTP API / Java API |

## 3. 适用场景
- 任务定义：创建、修改草稿、删除草稿、启用、暂停、禁用、手动触发。
- 调度类型：`CRON`、`FIXED_RATE`、`ONE_TIME`、`MANUAL`。
- Worker 模式：单体同 JVM `IN_MEMORY`、远程 Worker `HTTP_INTERNAL`。
- Handler 匹配：按 `appCode + ownerService + workerGroup + handlerName + jobCode` 查找可执行 Worker。
- 运行记录：生成 `mango_job_instance`、`mango_job_attempt`、`mango_job_log_index`、`mango_job_log_chunk`。
- 日志捕获：捕获 `System.out`、`System.err` 和 Logback 事件，写入 native log chunk。
- 幂等键：调度任务按 `jobId + version + scheduledFireTime` 生成，手动/API 任务按 `jobId + triggerBatchNo` 生成。
- Worker 治理：自动心跳注册、手动登记远程 Worker、状态调整。
- 失败告警：失败实例匹配 `INSTANCE_FAILED` 告警规则后调用 `mango-notice`。
- 菜单权限：通过 `resource-manifest.json` 初始化任务管理菜单和按钮权限。

## 4. 边界说明
- 不接入 PowerJob Server、PowerJob Worker 或 PowerJob 表结构。
- 不替代业务模块的任务处理器实现、业务事务和业务幂等。
- 不负责通知通道本身，失败告警通知依赖 `mango-notice`。
- 不作为审批流或长流程编排引擎使用，审批流程归属 `mango-workflow`。
- 不适合作为非 Mango 权限体系下的通用任务平台直接暴露给公网。

## 5. 模块组成
`mango-job` 负责平台任务治理模型、原生调度运行时、Worker 注册、实例归档、执行日志、告警规则和菜单权限资源。

业务模块负责实现具体 `MangoJobHandler`、处理业务事务、保证重复触发安全、维护业务参数 schema 的含义。

前端页面由 `mango-ui/packages/job` 提供；菜单、按钮权限和租户边界依赖 `mango-authorization`、`mango-access` 和当前登录上下文。

## 6. 接入方式
JobCenter、单体应用或需要本地调度扫描的服务引入：

```xml
<dependency>
    <groupId>io.mango.platform.job</groupId>
    <artifactId>mango-job-starter</artifactId>
</dependency>
```

只作为远程 Worker 接收 JobCenter 派发任务的服务引入：

```xml
<dependency>
    <groupId>io.mango.platform.job</groupId>
    <artifactId>mango-job-starter-remote</artifactId>
</dependency>
```

只引用命令、VO、枚举或实现 `MangoJobHandler` 契约时引入：

```xml
<dependency>
    <groupId>io.mango.platform.job</groupId>
    <artifactId>mango-job-api</artifactId>
</dependency>
```

`mango-job-starter` 依赖 `mango-infra-web-starter`，仓库内该 web starter 已开启 `@EnableScheduling`。如果业务工程没有引入 web starter，又只引入远程 starter，则远程 starter 自身带 `@EnableScheduling`，用于 Worker 心跳。

## 7. 快速开始
### 4.1 实现处理器

业务模块实现 `io.mango.job.api.handler.MangoJobHandler` 并注册为 Spring Bean：

```java
package com.example.order.job;

import io.mango.job.api.handler.MangoJobHandleContext;
import io.mango.job.api.handler.MangoJobHandleResult;
import io.mango.job.api.handler.MangoJobHandler;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class OrderTimeoutCloseJobHandler implements MangoJobHandler {

    @Override
    public String appCode() {
        return "order";
    }

    @Override
    public String serviceCode() {
        return "order-service";
    }

    @Override
    public String workerGroup() {
        return "order-worker";
    }

    @Override
    public Set<String> supportedJobCodes() {
        return Set.of("order_timeout_close");
    }

    @Override
    public String handlerName() {
        return "orderTimeoutCloseJobHandler";
    }

    @Override
    public MangoJobHandleResult handle(MangoJobHandleContext context) {
        // context.getParameter() 是任务定义或手动触发传入的 JSON 参数。
        // 业务幂等、事务边界和重试安全由业务处理器自己保证。
        return MangoJobHandleResult.success("closed=0");
    }
}
```

Handler 归属字段的默认值来自 `MangoJobHandlerRegistry`：

| 字段 | 解析顺序 | 作用 |
|------|----------|------|
| `handlerName()` | 必填，trim 后不能为空 | 处理器名称，同一 `appCode + serviceCode + workerGroup` 下不能重复 |
| `appCode()` | handler 返回值 -> `mango.job.native.app-code` -> `spring.application.name` -> `local` | 任务所属逻辑应用 |
| `serviceCode()` | handler 返回值 -> `mango.job.native.service-code` -> `appCode` | 调度中心选择执行服务 |
| `workerGroup()` | handler 返回值 -> `mango.job.native.worker-group` -> `serviceCode` | 同一服务下的执行隔离分组 |
| `supportedJobCodes()` | 空集合表示不限制任务编码 | 限制这个 handler 可执行的 `jobCode` |

任务定义里的 `appCode`、`ownerService`、`workerGroup`、`handlerName`、`jobCode` 必须能匹配 Worker 能力，否则执行时会报“未找到可执行任务的 Worker 能力”或“未找到可执行任务的在线 Worker”。

### 4.2 配置单体内嵌 Worker

单体或本地 JobCenter 场景：

```yaml
spring:
  application:
    name: mango-monolith-app

mango:
  job:
    enabled: true
    probe:
      enabled: true
    native:
      app-code: mango-monolith-app
      service-code: mango-monolith-app
      worker-group: mango-monolith-app
      scheduler-enabled: true
      embedded-worker-enabled: true
      transport: IN_MEMORY
      scheduler-tenant-id: "1"
      scan-interval-millis: 5000
      scan-limit: 50
      lease-seconds: 300
```

这种模式下应用启动后会：

1. 扫描 Spring 容器内的 `MangoJobHandler`。
2. 按 `scheduler-tenant-id` 注册内嵌 Worker，注册地址形如 `embedded://<ip>:<port>`。
3. 调度器每 `scan-interval-millis` 扫描到期游标。
4. 使用 `IN_MEMORY` 在同 JVM 内执行 handler。

### 4.3 配置独立 JobCenter + 远程 Worker

JobCenter 引入 `mango-job-starter`，通常开启调度扫描但不一定承载业务 handler：

```yaml
mango:
  job:
    enabled: true
    native:
      scheduler-enabled: true
      embedded-worker-enabled: false
      scheduler-tenant-id: "1"
      scan-interval-millis: 5000
      scan-limit: 50
      lease-seconds: 300
```

远程 Worker 引入 `mango-job-starter-remote`，实现业务 `MangoJobHandler`，并配置自己的对外地址和 JobCenter 地址：

```yaml
spring:
  application:
    name: order-service

mango:
  job:
    native:
      app-code: order
      service-code: order-service
      worker-group: order-worker
      worker-address: http://order-service.internal:8080
      job-center-address: http://mango-job-center.internal:8080
      worker-heartbeat-interval-millis: 15000
      job-center-feign-url: http://mango-job-center.internal:8080
      worker-feign-url: http://127.0.0.1
```

远程 Worker 启动和心跳时会调用 JobCenter 的 `/job/internal/workers/register`；JobCenter 派发任务时会调用 Worker 的 `/job/internal/workers/execute`。

## 8. 配置说明
### 5.1 `mango.job.*`

| 配置 | 默认值 | 代码位置 | 含义 |
|------|--------|----------|------|
| `mango.job.enabled` | `true` | `JobAutoConfiguration` | 是否启用 JobCenter/单体 starter 自动配置。设为 `false` 后不装配 controller、mapper、runtime、调度器和内嵌 Worker 注册器 |
| `mango.job.probe.enabled` | `true` | `JobAutoConfiguration` | 是否注册 `MangoJobRuntimeProbeHandler` 示例探测处理器 |

### 5.2 `mango.job.native.*`

来源：`MangoNativeJobProperties`。

| 配置 | 默认值 | 含义 | 影响 |
|------|--------|------|------|
| `app-code` | 空 | 当前运行时默认应用编码 | handler 未覆盖 `appCode()` 时使用；仍为空再用 `spring.application.name`，最后退到 `local` |
| `service-code` | 空 | 当前运行时默认执行服务编码 | handler 未覆盖 `serviceCode()` 时使用；仍为空退到 `appCode` |
| `worker-group` | 空 | 当前运行时默认 Worker 分组 | handler 未覆盖 `workerGroup()` 时使用；仍为空退到 `serviceCode` |
| `scheduler-enabled` | `true` | 是否启用原生调度扫描 | `MangoNativeJobScheduler.tick()` 只有为 true 才调用 runtime 扫描到期任务 |
| `scan-interval-millis` | `5000` | 调度扫描间隔，单位毫秒 | `@Scheduled(fixedDelayString = "${mango.job.native.scan-interval-millis:5000}")` |
| `scheduler-tenant-id` | `"1"` | 调度器和内嵌 Worker 心跳使用的租户上下文 | 当前版本没有租户枚举，调度扫描只使用这个租户 |
| `scan-limit` | `50` | 单次扫描最大游标数 | 查询 `mango_job_schedule_cursor` 时作为 limit，最小按 1 处理 |
| `embedded-worker-enabled` | `true` | 是否启用同进程 Worker 自动注册 | 为 true 时注册 Spring 容器里的 handler，并允许 `IN_MEMORY` 派发 |
| `transport` | `IN_MEMORY` | 默认通信方式 | Worker 快照无 `transport_type` 且地址无法判断时使用 |
| `lease-seconds` | `300` | 执行尝试租约秒数 | 创建 attempt 时写入 `lease_until` 和 fencing token，必须大于 0 |
| `env-code` | `local` | 当前运行环境编码 | 当前代码只持有该字段，尚未参与调度决策 |
| `worker-address` | 空 | 当前 Worker 对外执行地址 | 远程 Worker 必填；内嵌 Worker 未填时自动生成 `embedded://<localIp>:<port>` |
| `job-center-address` | 空 | 远程 Worker 注册目标 JobCenter 地址 | 为空时 `MangoJobRemoteWorkerRegistrar` 不自动注册 |
| `worker-heartbeat-interval-millis` | `15000` | Worker 注册/心跳间隔，单位毫秒 | 内嵌 Worker 和远程 Worker 的 `@Scheduled` 心跳都使用这个配置 |
| `job-center-feign-url` | `http://127.0.0.1` | JobCenter Feign 默认 URL | `MangoJobFeignClient` 的 url；实际注册时也会用 `job-center-address` 作为 URI 覆盖 |
| `worker-feign-url` | `http://127.0.0.1` | Worker Feign 默认 URL | `MangoJobWorkerFeignClient` 的 url；实际派发时用 Worker 快照地址作为 URI 覆盖 |

## 9. 任务定义字段怎么填

接口命令：`SaveMangoJobDefinitionCommand`。

| 字段 | 是否必填 | 当前支持值/规则 | 说明 |
|------|----------|----------------|------|
| `id` | 修改必填 | 新增为空 | 只有 `DRAFT` 草稿任务允许修改 |
| `appCode` | 是 | 最长 128 | 所属逻辑应用，必须与 handler/Worker 能力匹配 |
| `ownerService` | 否 | 最长 128；为空默认 `appCode` | 执行服务编码 |
| `workerGroup` | 否 | 最长 128；为空默认 `ownerService` | Worker 分组 |
| `jobCode` | 是 | 租户 + 应用内唯一，最长 128 | 任务编码；会参与 handler `supportedJobCodes()` 匹配 |
| `jobName` | 是 | 最长 128 | 任务名称 |
| `jobType` | 是 | `BUILTIN` | 当前只支持 Spring Bean handler |
| `scheduleType` | 是 | `CRON`、`FIXED_RATE`、`ONE_TIME`、`MANUAL` | 调度类型 |
| `scheduleExpression` | 条件必填 | 非 `MANUAL` 必填 | `CRON` 使用 Spring `CronExpression`；`FIXED_RATE` 是毫秒数，保存校验要求 `>= 1000` 且 `< 120000`；`ONE_TIME` 使用 `LocalDateTime.parse` |
| `handlerName` | `BUILTIN` 必填 | 最长 128 | 处理器名称，必须等于 `MangoJobHandler.handlerName()` |
| `paramSchema` | 否 | JSON 字符串 | 前端参数编辑器 schema |
| `paramValue` | 否 | JSON 字符串 | 默认参数；调度触发使用它，手动触发可覆盖 |
| `misfireStrategy` | 否 | 最长 64 | 写入调度游标 `misfire_policy`，当前 native runtime 未实现复杂错过补偿策略 |
| `concurrencyPolicy` | 否 | 最长 64 | 当前保存归档字段，native runtime 未按该字段做并发隔离 |
| `timeoutSeconds` | 否 | `> 0` | 保存校验执行超时；当前 native runtime 未按该字段中断 handler |
| `retryPolicy` | 否 | JSON 字符串 | 当前保存归档字段，native runtime 未实现自动重试调度 |
| `engineType` | 是 | `MANGO_NATIVE` | 当前唯一引擎 |

状态流转来自 `UpdateMangoJobDefinitionStatusCommand`：

- 新建任务固定为 `DRAFT`。
- `DRAFT` 可启用、禁用。
- `ENABLED` 可暂停、禁用。
- `PAUSED` 可启用、禁用。
- `DISABLED` 可启用、退回草稿。
- 只有 `DRAFT` 可编辑、删除；`DRAFT` 和 `DISABLED` 不允许手动触发。

## 10. API 与扩展
HTTP controller 前缀：`/job`。所有管理接口由 `MangoJobController` 提供。

| 方法 | 路径 | 权限 | 用途 |
|------|------|------|------|
| `GET` | `/job/definitions/page` | `job:definition:list` | 分页查询任务定义 |
| `GET` | `/job/definitions/detail?id=` | `job:definition:query` | 查询任务定义详情 |
| `POST` | `/job/definitions` | `job:definition:add` | 新增任务定义 |
| `PUT` | `/job/definitions` | `job:definition:edit` | 修改草稿任务定义 |
| `PUT` | `/job/definitions/status` | `job:definition:status` | 调整任务状态 |
| `DELETE` | `/job/definitions?id=` | `job:definition:delete` | 删除草稿任务 |
| `POST` | `/job/definitions/trigger` | `job:definition:trigger` | 手动触发任务 |
| `GET` | `/job/instances/page` | `job:instance:list` | 分页查询执行实例 |
| `POST` | `/job/instances/sync` | `job:instance:sync` | 同步实例；native 引擎下主要用于兼容外部引擎语义 |
| `GET` | `/job/logs/page` | `job:log:list` | 分页查询日志索引 |
| `GET` | `/job/logs/detail?id=` | `job:log:list` | 查询日志详情 |
| `GET` | `/job/instances/{instanceId}/logs` | `job:instance:list` | 按实例查询 native 日志 |
| `GET` | `/job/workers/page` | `job:worker:list` | 分页查询 Worker 快照 |
| `POST` | `/job/workers` | `job:worker:add` | 手动登记远程 Worker |
| `PUT` | `/job/workers/status` | `job:worker:status` | 调整 Worker 状态 |
| `POST` | `/job/internal/workers/register` | `INTERNAL` | Worker 启动或心跳注册 |
| `GET` | `/job/handlers` | `job:handler:list` | 查询当前应用已注册 handler |
| `GET` | `/job/alarm-rules/page` | `job:alarm:list` | 分页查询告警规则 |
| `GET` | `/job/alarm-rules/detail?id=` | `job:alarm:query` | 查询告警规则详情 |
| `POST` | `/job/alarm-rules` | `job:alarm:add` | 新增告警规则 |
| `PUT` | `/job/alarm-rules` | `job:alarm:edit` | 修改告警规则 |
| `PUT` | `/job/alarm-rules/status` | `job:alarm:status` | 启停告警规则 |
| `DELETE` | `/job/alarm-rules?id=` | `job:alarm:delete` | 删除告警规则 |
| `GET` | `/job/engines/status` | `job:engine:list` | 查询引擎同步状态 |

Worker 内部执行接口由 `MangoJobWorkerInternalController` 提供：

| 方法 | 路径 | 权限 | 用途 |
|------|------|------|------|
| `POST` | `/job/internal/workers/execute` | `INTERNAL` | JobCenter 派发任务到 Worker |

`MangoJobWorkerFeignClient` 的方法映射包含 `/job/job/internal/workers/execute`，这是因为 FeignClient 自身 `path = "/job"`，方法上又声明了 `/job/internal/workers/execute`。部署远程 Worker 前要用 E2E 验证实际网关路径，必要时修正 Feign 方法路径，避免双 `/job`。

## 11. 告警怎么配置

接口命令：`SaveMangoJobAlarmRuleCommand`。

| 字段 | 是否必填 | 说明 |
|------|----------|------|
| `id` | 修改必填 | 新增为空 |
| `jobId` | 否 | 为空表示应用级默认规则；不为空只匹配指定任务 |
| `appCode` | 是 | 匹配任务定义的应用 |
| `ruleName` | 是 | 规则名称 |
| `alarmType` | 是 | 当前失败告警固定使用 `INSTANCE_FAILED` |
| `triggerCondition` | 否 | 当前保存字段；失败规则实际由 `alarmType + enabled + appCode + jobId` 匹配 |
| `noticeSceneCode` | 是 | 通知场景编码；命令字段必填 |
| `noticeTemplateCode` | 是 | 写入 notice params 的 `noticeTemplateCode` |
| `noticeParams` | 否 | JSON，支持 `userId`、`userIds`、`recipientRuleCode` |
| `enabled` | 否 | 是否启用 |

失败实例会调用 `NoticeApi.send`，固定设置：

- `bizType = MangoJobNoticeBizTypes.JOB_INSTANCE_FAILED`，值为 `job.instance.failed`。
- `bizId = instanceId`。
- `priority = HIGH`。
- `idempotentKey = mango-job:alarm:<ruleId>:<instanceId>`。
- params 包含任务、实例、handler、trace、耗时和错误摘要。

如果未启用 `mango-notice`，执行日志会记录“mango-notice 未启用，跳过 Job 失败告警发送”。

## 12. 数据与初始化
Flyway 路径：`mango-job-core/src/main/resources/db/migration/mango-job`。

| 迁移 | 内容 |
|------|------|
| `V1__init_mango_job.sql` | 创建任务定义、实例、日志索引、Worker 快照、告警规则、引擎映射、操作日志 |
| `V2__fix_worker_snapshot_unique_key.sql` | 修正 Worker 快照唯一键，支持多租户多应用 |
| `V3__cleanup_invalid_worker_snapshot.sql` | 清理占位 Worker 地址 |
| `V4__native_job_engine_foundation.sql` | 增加 native 调度游标、attempt、Worker 能力、日志分片、事件表 |
| `V5__job_worker_ownership.sql` | 增加 owner service、worker group、transport、register source 等归属字段 |
| `V6__seed_default_sample_jobs.sql` | 初始化两个禁用的默认 Probe 示例任务 |
| `V7__seed_payment_channel_bill_fetch_job.sql` | 初始化禁用的支付昨日账单拉取任务 |

核心表：

| 表 | 用途 |
|----|------|
| `mango_job_definition` | 任务定义，含调度表达式、handler、状态、引擎同步状态 |
| `mango_job_schedule_cursor` | native 调度游标，记录下一次触发时间和扫描锁 |
| `mango_job_instance` | 执行实例摘要，记录触发、状态、耗时、幂等键、结果 |
| `mango_job_attempt` | 单次执行尝试，记录 Worker、租约、防陈旧 token、开始结束状态 |
| `mango_job_worker_snapshot` | Worker 快照，记录地址、通信方式、注册来源、在线状态 |
| `mango_job_worker_capability` | Worker 能力，记录可执行 handler 和 jobCode |
| `mango_job_log_index` | 日志索引 |
| `mango_job_log_chunk` | native 执行日志分片 |
| `mango_job_alarm_rule` | 失败实例告警规则 |
| `mango_job_engine_mapping` | 引擎映射兼容表 |
| `mango_job_operation_log` | 任务定义和触发操作日志 |
| `mango_job_event` | Job 事件归档 |

`mango-job-starter/src/main/resources/META-INF/mango/module.properties` 声明：

```properties
module-name=mango-job
module-path=/job
persistence-datasource=job
```

如果环境启用了模块化数据源，`persistence-datasource=job` 表示 Job 模块走 `job` 数据源；否则通常落在默认业务库。

## 13. 管理入口
资源 manifest：`mango-job-starter/src/main/resources/META-INF/mango/resource-manifest.json`。

manifest 声明：

- `appCode`: `internal-admin`
- `moduleCode`: `mango-job`
- `moduleName`: `任务调度模块`
- `packageCodes`: `internal-admin-default`、`internal-admin-ops`
- `roleCodes`: `ROLE_ADMIN`
- 顶级菜单挂到 `parentCode = data`，路径 `/job`，重定向 `/job/definition`。

菜单和页面 key：

| 菜单 | 路径 | component | 主要权限 |
|------|------|-----------|----------|
| 任务定义 | `/job/definition` | `job/definition/index` | `job:definition:list`、`job:definition:query`、`job:definition:add`、`job:definition:edit`、`job:definition:delete`、`job:definition:status`、`job:definition:trigger` |
| 执行实例 | `/job/instance` | `job/instance/index` | `job:instance:list`、`job:instance:sync`、`job:log:list` |
| Worker 节点 | `/job/worker` | `job/worker/index` | `job:worker:list`、`job:worker:add`、`job:worker:status` |
| 告警规则 | `/job/alarm` | `job/alarm/index` | `job:alarm:list`、`job:alarm:query`、`job:alarm:add`、`job:alarm:edit`、`job:alarm:status`、`job:alarm:delete` |
| 运行状态 | `/job/engine` | `job/engine/index` | `job:engine:list` |

manifest 由 `mango-authorization` 的资源同步能力入库。业务环境看不到菜单时，先检查 authorization 的 manifest 同步是否开启、`internal-admin-default/internal-admin-ops` 包是否绑定给当前用户、`ROLE_ADMIN` 或对应角色是否拥有权限。

## 14. 租户和安全边界

- 所有任务定义、实例、Worker、告警规则都带 `tenant_id`。
- 管理接口按 `MangoContextHolder.tenantId()` 查询当前租户数据。
- 调度器和 Worker 自动注册使用 `mango.job.native.scheduler-tenant-id` 构造系统上下文，默认 `"1"`。
- `/job/internal/workers/register` 和 `/job/internal/workers/execute` 标记 `@Inner` 与 `ApiResourceAccessMode.INTERNAL`，应只暴露给 Mango 内部调用链路或内网。
- Handler 执行时会设置 `MangoContextHolder`，其中用户为触发人或系统用户，租户来自执行命令。

## 15. 问题排查
| 现象 | 原因 | 处理 |
|------|------|------|
| 管理页面看不到任务菜单 | manifest 未同步或角色未绑定包/权限 | 检查 authorization 资源同步、`internal-admin-default/internal-admin-ops`、当前角色权限 |
| `/job/handlers` 为空 | 没有 Spring Bean 实现 `MangoJobHandler`，或远程 Worker 没有接入 starter | 确认 handler 带 `@Component`，确认依赖和组件扫描范围 |
| Worker 页面为空 | `embedded-worker-enabled=false` 且没有远程 Worker 注册，或 `job-center-address/worker-address` 未配置 | 单体开启内嵌 Worker；远程 Worker 配置两个地址并检查心跳 |
| 手动触发失败：未找到 Worker 能力 | 任务定义归属字段与 handler 注册字段不一致 | 对齐 `appCode`、`ownerService`、`workerGroup`、`handlerName`、`jobCode` |
| 手动触发失败：未找到在线 Worker | Worker 状态不是 `ONLINE`，或 `IN_MEMORY` Worker 不在当前 JobCenter JVM | 检查心跳、状态、通信方式；远程部署使用 `HTTP_INTERNAL` |
| 固定频率保存失败 | `FIXED_RATE` 表达式不是毫秒数或不在保存校验范围 | 使用 `1000 <= scheduleExpression < 120000` 的数字字符串 |
| 一次性任务不触发 | `ONE_TIME` 表达式不能被 `LocalDateTime.parse` 解析 | 使用形如 `2026-06-15T10:30:00` 的值 |
| 日志为空 | handler 没有输出，或实例未进入执行阶段 | 查询实例状态和 attempt；确认 Worker 可达 |
| 告警未发送 | 未启用 `mango-notice`、规则未启用、`appCode/jobId` 不匹配 | 检查 `mango_job_alarm_rule` 和实例日志中的告警结果 |

## 16. 相关文档
- [@mango/job 前端包](../../../mango-ui/packages/job/README.md)
- [Job 部署说明](../../../deploy/job/README.md)
- [能力地图](../../../mango-docs/capabilities/README.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 17. 历史资料
- [Job 部署说明](../../../deploy/job/README.md)
- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
