# Mango Job

## 1. 概览

`mango-job` 是 Mango 的任务调度平台能力。业务模块把任务处理逻辑实现成 `MangoJobHandler`，平台负责把这些处理器登记成可管理任务，并提供任务定义、调度触发、Worker 注册、执行实例、执行日志、失败告警和后台菜单权限。

当前能力只覆盖 Mango 原生调度：

| 项 | 当前支持 |
|----|----------|
| 引擎 | `MANGO_NATIVE` |
| 任务类型 | `BUILTIN` Spring Bean 处理器 |
| 调度类型 | `CRON`、`FIXED_RATE`、`ONE_TIME`、`MANUAL` |
| Worker 通信 | 单体同进程 `IN_MEMORY`、独立 Worker `HTTP_INTERNAL` |
| 管理前端 | `@mango/job`，属于 Mango Admin Pages 插件 |

## 2. 功能清单

| 能力 | 说明 |
|------|------|
| 任务定义 | 创建、修改草稿、删除草稿、启用、暂停、禁用和手动触发任务 |
| 处理器注册 | 扫描 Spring 容器中的 `MangoJobHandler`，生成可执行 handler 清单 |
| Worker 管理 | 支持内嵌 Worker 自动注册、远程 Worker 心跳注册和后台手动登记 |
| 调度触发 | 按 CRON、固定频率、一次性或手动触发生成执行实例 |
| 执行记录 | 记录实例、尝试次数、执行状态、耗时、结果摘要、错误摘要和 traceId |
| 执行日志 | 捕获 handler 执行日志，支持按实例查看 native 日志内容 |
| 失败告警 | 任务失败后匹配启用的告警规则，调用 `mango-notice` 发送通知 |
| 菜单权限 | 通过 Resource Registry `AUTH_MENU` 资源初始化任务管理菜单和按钮权限 |

## 3. 后端接入

业务模块只实现任务处理器、引用命令对象或调用 Job API 时，依赖 API 包：

```xml
<dependency>
    <groupId>io.mango.platform.job</groupId>
    <artifactId>mango-job-api</artifactId>
</dependency>
```

部署 JobCenter、单体应用，或需要在本 JVM 扫描和执行本地 handler 时，依赖 starter：

```xml
<dependency>
    <groupId>io.mango.platform.job</groupId>
    <artifactId>mango-job-starter</artifactId>
</dependency>
```

业务服务作为远程 Worker 接收 JobCenter 派发任务时，依赖 remote starter：

```xml
<dependency>
    <groupId>io.mango.platform.job</groupId>
    <artifactId>mango-job-starter-remote</artifactId>
</dependency>
```

接入形态：

| 形态 | 依赖 | 用法 |
|------|------|------|
| 只写 handler 契约 | `mango-job-api` | 编译期实现 `MangoJobHandler`，不启用调度能力 |
| 单体后台 | `mango-job-starter` | 同一个应用提供管理接口、调度扫描和内嵌 Worker |
| 独立 JobCenter | `mango-job-starter` | 只负责管理接口和调度扫描，通常关闭内嵌 Worker |
| 远程 Worker | `mango-job-starter-remote` | 业务服务实现 handler，向 JobCenter 注册并接收内部执行请求 |

`mango-job-starter` 会带上 `mango-infra-web-starter` 和 `mango-infra-persistence-starter`。`mango-job-starter-remote` 会带上 `mango-infra-feign-starter`，用于远程注册和任务派发调用。

## 4. 前端接入

管理后台使用 `@mango/job`：

```json
{
  "dependencies": {
    "@mango/job": "1.0.1"
  }
}
```

在 Mango Admin Shell 启动入口注册页面：

```ts
import { registerMangoJobAdminPages } from '@mango/job/admin-pages';
import '@mango/job/style.css';

registerMangoJobAdminPages();
```

`@mango/job` 是 admin-pages 配套插件，不是官网、C 端站点或普通 Web 项目的通用组件库。它依赖 `@mango/admin-pages` 的页面注册机制、`@mango/common` 请求封装、Element Plus、后端 `/job/**` 接口、租户上下文和按钮权限。

前端包提供这些页面 key：

| 页面 key | 后台菜单 |
|----------|----------|
| `job/definition/index` | 任务定义 |
| `job/instance/index` | 执行实例 |
| `job/worker/index` | Worker 节点 |
| `job/alarm/index` | 告警规则 |
| `job/engine/index` | 运行状态 |

## 5. 快速开始

### 5.1 实现任务处理器

业务服务实现 `io.mango.job.api.handler.MangoJobHandler` 并注册为 Spring Bean：

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
        String parameter = context.getParameter();
        // 业务事务和重复触发幂等由业务处理器自己保证。
        return MangoJobHandleResult.success("closed=0");
    }
}
```

任务定义里的 `appCode`、`ownerService`、`workerGroup`、`handlerName`、`jobCode` 必须能匹配 Worker 上报的 handler 能力。匹配不上时，触发任务会失败。

处理器字段：

| 字段 | 说明 |
|------|------|
| `handlerName()` | 必填，处理器名称 |
| `appCode()` | 所属逻辑应用；为空时使用 `mango.job.native.app-code`、`spring.application.name` 或 `local` |
| `serviceCode()` | 执行服务编码；为空时默认跟随 `appCode` |
| `workerGroup()` | Worker 分组；为空时默认跟随 `serviceCode` |
| `supportedJobCodes()` | 支持的任务编码；空集合表示不限制 `jobCode` |
| `handle(context)` | 执行业务任务，返回 `SUCCESS` 或 `FAILED` |

`MangoJobHandleContext` 会传入 `tenantId`、`appCode`、`jobCode`、`instanceId`、`operatorId`、`triggerType`、`triggerBatchNo`、`traceId` 和任务参数 JSON。

### 5.2 单体或本地 JobCenter

单体后台启用管理接口、调度扫描和内嵌 Worker：

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

启动后可以在 Worker 页面看到内嵌 Worker，任务触发会在同一个 JVM 内执行 handler。

### 5.3 独立 JobCenter 和远程 Worker

JobCenter 启用调度扫描，通常不承载业务 handler：

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

远程 Worker 业务服务启用 remote starter，并配置自身地址和 JobCenter 地址：

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

远程 Worker 会向 JobCenter 的 `/job/internal/workers/register` 注册心跳；JobCenter 派发任务时会调用 Worker 的 `/job/internal/workers/execute`。

## 6. 配置说明

### 6.1 starter 开关

| 配置 | 默认值 | 含义 |
|------|--------|------|
| `mango.job.enabled` | `true` | 是否启用 JobCenter/单体 starter 自动配置。关闭后不装配 `/job/**` 管理接口、mapper、runtime、调度器和内嵌 Worker 注册器 |
| `mango.job.probe.enabled` | `true` | 是否注册 `mangoJobRuntimeProbeHandler` 示例探测处理器 |

### 6.2 native 运行时配置

`mango.job.native.*` 来自 `MangoNativeJobProperties`：

| 配置 | 默认值 | 含义 |
|------|--------|------|
| `app-code` | 空 | 当前运行时默认应用编码；handler 未覆盖时使用 |
| `service-code` | 空 | 当前运行时默认执行服务编码；为空时跟随 `appCode` |
| `worker-group` | 空 | 当前运行时默认 Worker 分组；为空时跟随 `serviceCode` |
| `scheduler-enabled` | `true` | 是否启用原生调度扫描 |
| `scan-interval-millis` | `5000` | 调度扫描间隔，单位毫秒 |
| `scheduler-tenant-id` | `"1"` | 调度器和自动 Worker 心跳使用的租户上下文 |
| `scan-limit` | `50` | 单次扫描最大任务数 |
| `embedded-worker-enabled` | `true` | 是否启用同进程 Worker 自动注册 |
| `transport` | `IN_MEMORY` | 默认通信方式，支持 `IN_MEMORY`、`HTTP_INTERNAL` |
| `lease-seconds` | `300` | 执行租约秒数 |
| `env-code` | `local` | 当前运行环境编码 |
| `worker-address` | 空 | 当前 Worker 对外执行地址；远程 Worker 必填 |
| `job-center-address` | 空 | 远程 Worker 注册目标 JobCenter 地址；为空时不自动注册 |
| `worker-heartbeat-interval-millis` | `15000` | Worker 注册/心跳间隔，单位毫秒 |

Feign 客户端还读取这两个占位配置：

| 配置 | 默认值 | 用途 |
|------|--------|------|
| `mango.job.native.job-center-feign-url` | `http://127.0.0.1` | JobCenter Feign 默认 URL |
| `mango.job.native.worker-feign-url` | `http://127.0.0.1` | Worker Feign 默认 URL |

## 7. 任务定义怎么填

任务定义保存命令是 `SaveMangoJobDefinitionCommand`。

| 字段 | 是否必填 | 说明 |
|------|----------|------|
| `id` | 修改必填 | 新增为空；只有草稿任务允许修改 |
| `appCode` | 是 | 所属逻辑应用，最长 128 |
| `ownerService` | 否 | 执行服务编码；为空默认 `appCode` |
| `workerGroup` | 否 | Worker 分组；为空默认 `ownerService` |
| `jobCode` | 是 | 任务编码，租户和应用内唯一，最长 128 |
| `jobName` | 是 | 任务名称，最长 128 |
| `jobType` | 是 | 当前填 `BUILTIN` |
| `scheduleType` | 是 | `CRON`、`FIXED_RATE`、`ONE_TIME`、`MANUAL` |
| `scheduleExpression` | 条件必填 | 非 `MANUAL` 必填；`CRON` 用 Spring Cron，`FIXED_RATE` 填毫秒数字符串，`ONE_TIME` 填 `LocalDateTime` 格式 |
| `handlerName` | `BUILTIN` 必填 | 必须等于 `MangoJobHandler.handlerName()` |
| `paramSchema` | 否 | 参数表单 schema JSON，供前端参数编辑使用 |
| `paramValue` | 否 | 默认参数 JSON；调度触发使用它，手动触发可覆盖 |
| `misfireStrategy` | 否 | 错过触发策略字段 |
| `concurrencyPolicy` | 否 | 并发策略字段 |
| `timeoutSeconds` | 否 | 执行超时秒数，必须大于 0 |
| `retryPolicy` | 否 | 重试策略 JSON |
| `engineType` | 是 | 当前填 `MANGO_NATIVE` |

状态流转：

| 当前状态 | 可操作 |
|----------|--------|
| `DRAFT` | 启用、禁用、编辑、删除 |
| `ENABLED` | 暂停、禁用、手动触发 |
| `PAUSED` | 启用、禁用、手动触发 |
| `DISABLED` | 启用、退回草稿 |

固定频率任务的 `scheduleExpression` 必须是 `1000 <= value < 120000` 的毫秒数字符串。一次性任务使用类似 `2026-06-15T10:30:00` 的值。

## 8. 告警规则怎么填

告警规则保存命令是 `SaveMangoJobAlarmRuleCommand`。

| 字段 | 是否必填 | 说明 |
|------|----------|------|
| `id` | 修改必填 | 新增为空 |
| `jobId` | 否 | 为空表示应用级规则；不为空只匹配指定任务 |
| `appCode` | 是 | 所属逻辑应用 |
| `ruleName` | 是 | 规则名称 |
| `alarmType` | 是 | 当前失败告警使用 `INSTANCE_FAILED` |
| `triggerCondition` | 否 | 触发条件 JSON 字段 |
| `noticeSceneCode` | 是 | 通知场景编码 |
| `noticeTemplateCode` | 是 | 作为 `noticeTemplateCode` 参数传给 `mango-notice` |
| `noticeParams` | 否 | JSON，支持 `userId`、`userIds`、`recipientRuleCode` |
| `enabled` | 否 | 是否启用 |

失败实例会调用 `NoticeApi.send`，业务类型是 `job.instance.failed`，优先级是 `HIGH`，幂等键格式是 `mango-job:alarm:<ruleId>:<instanceId>`。如果没有启用 `mango-notice`，任务执行日志会记录跳过告警发送。

## 9. 接口和返回字段

HTTP 前缀是 `/job`。管理页面和前端 `jobApi` 使用这些接口：

| 能力 | 主要接口 | 权限 |
|------|----------|------|
| 任务定义 | `GET /job/definitions/page`、`GET /job/definitions/detail`、`POST /job/definitions`、`PUT /job/definitions`、`DELETE /job/definitions` | `job:definition:*` |
| 任务状态和触发 | `PUT /job/definitions/status`、`POST /job/definitions/trigger` | `job:definition:status`、`job:definition:trigger` |
| 执行实例 | `GET /job/instances/page`、`POST /job/instances/sync`、`GET /job/instances/{instanceId}/logs` | `job:instance:*` |
| 执行日志 | `GET /job/logs/page`、`GET /job/logs/detail` | `job:log:list` |
| Worker | `GET /job/workers/page`、`POST /job/workers`、`PUT /job/workers/status` | `job:worker:*` |
| handler 清单 | `GET /job/handlers` | `job:handler:list` |
| 告警规则 | `GET /job/alarm-rules/page`、`GET /job/alarm-rules/detail`、`POST /job/alarm-rules`、`PUT /job/alarm-rules`、`PUT /job/alarm-rules/status`、`DELETE /job/alarm-rules` | `job:alarm:*` |
| 引擎状态 | `GET /job/engines/status` | `job:engine:list` |
| Worker 内部调用 | `POST /job/internal/workers/register`、`POST /job/internal/workers/execute` | INTERNAL |

常用返回对象：

| 对象 | 关键字段 |
|------|----------|
| `MangoJobDefinitionVO` | `id`、`tenantId`、`appCode`、`ownerService`、`workerGroup`、`jobCode`、`jobName`、`jobType`、`scheduleType`、`handlerName`、`paramSchema`、`paramValue`、`status`、`engineType`、`syncStatus` |
| `MangoJobInstanceVO` | `id`、`jobId`、`jobCode`、`jobName`、`triggerType`、`scheduledFireTime`、`actualFireTime`、`startTime`、`endTime`、`status`、`durationMillis`、`attemptCount`、`resultSummary`、`workerAddress`、`errorSummary`、`traceId`、`triggerBatchNo` |
| `MangoJobLogDetailVO` | `id`、`jobId`、`instanceId`、`instanceStatus`、`logSource`、`nativeLogAvailable`、`nativeLogContent`、`content`、`engineResult`、`errorSummary` |
| `MangoJobWorkerSnapshotVO` | `id`、`tenantId`、`appCode`、`serviceCode`、`workerGroup`、`workerAddress`、`runtimeAddress`、`transportType`、`registerSource`、`instanceId`、`lastHeartbeatAt`、`status` |
| `MangoJobHandlerVO` | `appCode`、`serviceCode`、`workerGroup`、`handlerName`、`supportedJobCodes`、`jobType`、`paramSchema`、`concurrent`、`timeoutSeconds`、`retryPolicy` |
| `MangoJobAlarmRuleVO` | `id`、`jobId`、`jobCode`、`jobName`、`appCode`、`ruleName`、`alarmType`、`noticeSceneCode`、`noticeTemplateCode`、`noticeParams`、`enabled` |
| `MangoJobEngineStatusVO` | `engineType`、`pendingCount`、`failedCount`、`syncedCount`、`lastUpdatedAt` |

## 10. 管理入口

菜单和权限来自：

```text
mango-job-starter/src/main/resources/META-INF/mango/resources/job-common-menu.json
```

`AUTH_MENU` 声明：

| 项 | 值 |
|----|----|
| `appCode` | `internal-admin` |
| `moduleCode` | `mango-job` |
| `moduleName` | `任务调度模块` |
| `packageCodes` | `platform_admin`、`institution_collaboration` |
| `roleCodes` | 空；角色授权由租户套餐绑定同步 |
| 顶级菜单 | `任务管理`，挂到 `parentCode = data`，路径 `/job` |

后台菜单：

| 菜单 | 路径 | component | 主要权限 |
|------|------|-----------|----------|
| 任务定义 | `/job/definition` | `job/definition/index` | `job:definition:list`、`job:definition:query`、`job:definition:add`、`job:definition:edit`、`job:definition:delete`、`job:definition:status`、`job:definition:trigger` |
| 执行实例 | `/job/instance` | `job/instance/index` | `job:instance:list`、`job:instance:sync`、`job:log:list` |
| Worker 节点 | `/job/worker` | `job/worker/index` | `job:worker:list`、`job:worker:add`、`job:worker:status` |
| 告警规则 | `/job/alarm` | `job/alarm/index` | `job:alarm:list`、`job:alarm:query`、`job:alarm:add`、`job:alarm:edit`、`job:alarm:status`、`job:alarm:delete` |
| 运行状态 | `/job/engine` | `job/engine/index` | `job:engine:list` |

资源入库由 Resource Registry 调用 `mango-authorization` 的 `AUTH_MENU` handler 完成。后台看不到菜单时，先检查资源同步、菜单包绑定、角色权限和前端 `registerMangoJobAdminPages()` 是否执行。

## 11. 数据与初始化

Flyway 路径：

```text
mango-job-core/src/main/resources/db/migration/mango-job
```

迁移脚本：

| 脚本 | 内容 |
|------|------|
| `V1__init_mango_job.sql` | 创建任务定义、实例、日志索引、Worker 快照、告警规则、引擎映射和操作日志表 |
| `V2__fix_worker_snapshot_unique_key.sql` | 修正 Worker 快照唯一键 |
| `V3__cleanup_invalid_worker_snapshot.sql` | 清理占位 Worker 地址 |
| `V4__native_job_engine_foundation.sql` | 增加 native 调度游标、执行尝试、Worker 能力、日志分片和事件表 |
| `V5__job_worker_ownership.sql` | 增加 owner service、worker group、transport、register source 等字段 |
| `V6__seed_default_sample_jobs.sql` | 已迁移到 `JOB_DEFINITION` 资源注入 |
| `V7__seed_payment_channel_bill_fetch_job.sql` | 已迁移到 `JOB_DEFINITION` 资源注入 |

核心表：

| 表 | 用途 |
|----|------|
| `mango_job_definition` | 任务定义 |
| `mango_job_schedule_cursor` | native 调度游标 |
| `mango_job_instance` | 执行实例 |
| `mango_job_attempt` | 执行尝试 |
| `mango_job_worker_snapshot` | Worker 快照 |
| `mango_job_worker_capability` | Worker handler 能力 |
| `mango_job_log_index` | 日志索引 |
| `mango_job_log_chunk` | native 执行日志分片 |
| `mango_job_alarm_rule` | 失败告警规则 |
| `mango_job_engine_mapping` | 引擎映射兼容表 |
| `mango_job_operation_log` | 操作日志 |
| `mango_job_event` | Job 事件归档 |

模块属性：

```properties
module-name=mango-job
module-path=/job
persistence-datasource=job
```

如果环境启用了模块化数据源，`persistence-datasource=job` 表示 Job 模块使用 `job` 数据源；否则通常落在默认业务库。

## 12. 资源注入

`mango-job` 作为资源消费者公开 `JOB_DEFINITION`，业务模块通过 `mango-resource-api` 声明任务定义，由 `mango-job-core` 负责写入 `mango_job_definition`。

资源文件：

```text
mango-job-starter/src/main/resources/META-INF/mango/resources/job-common-domain.yml
mango-job-starter/src/main/resources/META-INF/mango/resources/job-common-definition.yml
```

任务通知模板不再使用 YAML 文件，改由 `JobMessageTemplateResourceProvider` 通过 Java Provider 注入。

支持类型：

| 资源类型 | 目标模块 | 说明 |
|----------|----------|------|
| `BUSINESS_DOMAIN` | `domain` | 登记 Job 业务域 |
| `MESSAGE_TEMPLATE` | `notice` | 登记任务失败通知模板 |
| `JOB_DEFINITION` | `job` | 登记任务定义 |

`MESSAGE_TEMPLATE` 包含 `job.instance.failed`、`job.worker.offline`，字段契约以 `mango-notice` 的 `MESSAGE_TEMPLATE` 说明为准。

`JOB_DEFINITION` 字段：

| 字段 | 必填 | 说明 |
|------|------|------|
| `jobId` | 否 | 任务定义稳定 ID；不填使用资源 ID |
| `tenantId` | 否 | 租户 ID，默认 `1` |
| `appCode` | 是 | 所属逻辑应用 |
| `ownerService` | 否 | 任务归属服务，默认跟随 `appCode` |
| `workerGroup` | 否 | Worker 分组，默认跟随 `ownerService` |
| `moduleCode` | 否 | 来源模块编码，默认使用资源 `module-code` |
| `jobCode` | 是 | 任务编码，租户和应用内唯一 |
| `jobName` | 是 | 任务名称 |
| `jobType` | 否 | 任务类型，默认 `BUILTIN` |
| `scheduleType` | 是 | 调度类型：`CRON`、`FIXED_RATE`、`ONE_TIME`、`MANUAL` |
| `scheduleExpression` | 否 | 调度表达式，`MANUAL` 可为空 |
| `handlerName` | 是 | Spring Bean 处理器名称 |
| `handlerVersion` | 否 | 处理器版本 |
| `paramSchema` | 否 | 参数表单 JSON Schema |
| `paramValue` | 否 | 默认参数 JSON，默认 `{}` |
| `misfireStrategy` | 否 | 错过触发策略，默认 `IGNORE` |
| `concurrencyPolicy` | 否 | 并发策略，默认 `SERIAL` |
| `timeoutSeconds` | 否 | 执行超时秒数，默认 `300` |
| `retryPolicy` | 否 | 重试策略 JSON |
| `timezone` | 否 | 调度时区，默认 `Asia/Shanghai` |
| `maxRetryCount` | 否 | 最大重试次数，默认 `0` |
| `definitionVersion` | 否 | 任务定义内部版本，默认 `0` |
| `status` | 否 | 初始状态，默认 `DISABLED`；已有非 `DRAFT` 状态不会被资源覆盖 |
| `engineType` | 否 | 调度引擎类型，默认 `MANGO_NATIVE` |

## 13. 租户和安全

| 项 | 行为 |
|----|------|
| 任务数据 | 任务定义、实例、Worker、告警规则都带 `tenant_id` |
| 管理接口 | 按当前 `MangoContextHolder.tenantId()` 查询当前租户数据 |
| 调度线程 | 使用 `mango.job.native.scheduler-tenant-id` 构造系统上下文 |
| 内部接口 | `/job/internal/workers/register` 和 `/job/internal/workers/execute` 标记为 INTERNAL，只应暴露给 Mango 内部调用链路或内网 |
| handler 执行 | 执行时会设置租户、触发人或系统用户上下文 |

## 14. 问题排查

| 现象 | 排查点 |
|------|--------|
| 后台没有任务菜单 | 检查 Resource Registry `AUTH_MENU` 同步、`platform_admin/institution_collaboration` 套餐绑定、当前角色权限和前端页面注册 |
| `/job/handlers` 为空 | 检查业务 handler 是否是 Spring Bean，是否实现 `MangoJobHandler`，组件扫描是否覆盖 |
| Worker 页面为空 | 单体检查 `embedded-worker-enabled`；远程 Worker 检查 `job-center-address`、`worker-address` 和心跳 |
| 触发失败：未找到 Worker 能力 | 对齐任务定义的 `appCode`、`ownerService`、`workerGroup`、`handlerName`、`jobCode` 和 handler 上报能力 |
| 触发失败：未找到在线 Worker | 检查 Worker 状态、心跳时间、通信方式和 Worker 地址 |
| 固定频率任务保存失败 | `FIXED_RATE` 表达式必须是 `1000` 到 `119999` 之间的毫秒数字符串 |
| 一次性任务不触发 | `ONE_TIME` 表达式必须能被 `LocalDateTime.parse` 解析 |
| 实例有记录但日志为空 | 检查 handler 是否实际输出日志、实例是否进入执行阶段、Worker 是否成功返回日志 |
| 告警未发送 | 检查 `mango-notice` 是否启用、告警规则是否启用、`appCode/jobId` 是否匹配、通知场景和模板是否存在 |

## 15. 相关文档

- [@mango/job 前端包](../../../mango-ui/packages/job/README.md)
- [Job 部署说明](../../../deploy/job/README.md)
- [Mango Authorization](../mango-authorization/README.md)
- [Mango Notice](../mango-notice/README.md)
- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
