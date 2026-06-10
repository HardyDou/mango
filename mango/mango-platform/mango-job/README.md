# Mango Job 任务管理

Mango Job 是 Mango 原生任务调度能力。它只保留 Mango 自己的任务契约、数据模型、权限菜单、前端页面和日志归档，不再接入 PowerJob 运行时。

任务管理入口：

```text
平台能力 -> 任务管理
├── 任务定义
├── 执行实例
├── Worker 节点
├── 运行状态
└── 告警规则
```

## 1. 能力边界

当前 Mango Job 支持：

- 原生调度引擎：`MANGO_NATIVE`。
- 调度类型：`CRON`、`FIXED_RATE`、`ONE_TIME`、`MANUAL`。
- 部署形态：单体内嵌、单体多实例、独立 JobCenter + 远程 Worker。
- 通信方式：`IN_MEMORY`、`HTTP_INTERNAL`。
- Worker 自动注册、远程 Worker 手动登记、Worker 上下线治理。
- 按 `appCode + ownerService + workerGroup + handlerName + jobCode` 选择可执行 Worker。
- 执行实例、尝试记录、运行日志、处理器返回值统一归档到 `mango_job` 数据库。
- 捕获 `System.out`、`System.err` 和日志框架输出，执行实例行内查看日志。
- 失败实例通过 `mango-notice` 发送系统消息，通知业务域为 `定时任务`。

不再使用：

- PowerJob Server、PowerJob Worker 或 PowerJob 数据表。
- 独立的“执行日志”菜单。日志从“执行实例”行内进入。

## 2. 启动配置

推荐使用部署示例：

[deploy/job/application-job-native.yml](../../../deploy/job/application-job-native.yml)

关键配置：

```yaml
mango:
  persistence:
    datasources:
      primary:
        primary: true
        url: ${MANGO_DB_URL}
        username: ${MANGO_DB_USERNAME}
        password: ${MANGO_DB_PASSWORD}
        driver-class-name: com.mysql.cj.jdbc.Driver
      job:
        url: ${MANGO_JOB_DB_URL:${MANGO_DB_URL}}
        username: ${MANGO_JOB_DB_USERNAME:${MANGO_DB_USERNAME}}
        password: ${MANGO_JOB_DB_PASSWORD:${MANGO_DB_PASSWORD}}
        driver-class-name: com.mysql.cj.jdbc.Driver
    modules:
      mango-job:
        datasource: job
  job:
    enabled: true
    native:
      embedded-worker-enabled: true
      transport: IN_MEMORY
      scheduler-enabled: true
```

本地启动示例：

```bash
SPRING_CONFIG_ADDITIONAL_LOCATION=file:deploy/job/application-job-native.yml \
MANGO_DB_URL=jdbc:mysql://127.0.0.1:3306/mango_dev?useUnicode=true\&characterEncoding=utf8\&useSSL=false\&allowPublicKeyRetrieval=true\&serverTimezone=Asia/Shanghai \
MANGO_DB_USERNAME=root \
MANGO_DB_PASSWORD= \
MANGO_JOB_DB_URL=jdbc:mysql://127.0.0.1:3306/mango_job?useUnicode=true\&characterEncoding=utf8\&useSSL=false\&allowPublicKeyRetrieval=true\&serverTimezone=Asia/Shanghai \
MANGO_JOB_DB_USERNAME=root \
MANGO_JOB_DB_PASSWORD= \
scripts/dev-workspace.sh backend
```

## 3. 部署形态

### 3.1 单体内嵌

适用于 Mango 管理后台和任务处理代码在同一进程内运行。

```yaml
mango:
  job:
    native:
      scheduler-enabled: true
      embedded-worker-enabled: true
      transport: IN_MEMORY
```

特点：

- JobCenter 和 Worker 在同一个 JVM 内。
- Worker 由系统自动注册，地址形如 `in-memory://host/embedded-...`。
- 不需要为 Worker 暴露独立执行端口。
- 管理后台不要手动新增 `IN_MEMORY` Worker。

### 3.2 单体多实例

适用于多个 Mango 节点同时部署。

要求：

- 所有节点连接同一个 `mango_job` 数据库。
- 每个节点可启用内嵌 Worker。
- 调度扫描通过数据库游标和幂等键控制，不依赖随机抢任务。
- Worker 列表会看到多个内嵌 Worker，每个 JVM 一个 Worker 地址。

### 3.3 独立 JobCenter + 远程 Worker

适用于调度中心和业务服务分开部署。

JobCenter：

```yaml
mango:
  job:
    native:
      scheduler-enabled: true
      embedded-worker-enabled: false
      transport: HTTP_INTERNAL
```

远程 Worker：

```yaml
mango:
  job:
    native:
      scheduler-enabled: false
      embedded-worker-enabled: false
      worker-address: http://worker-a:8080
      job-center-address: http://jobcenter:8080
      worker-heartbeat-interval-millis: 15000
```

远程 Worker 可以自动心跳注册，也可以在管理后台手动登记。手动登记只支持 `HTTP_INTERNAL`，不支持 `IN_MEMORY`。

## 4. 如何定义任务

进入：

```text
平台能力 -> 任务管理 -> 任务定义
```

新增任务时填写：

| 字段 | 说明 |
|---|---|
| 所属应用 `appCode` | 逻辑应用编码，例如 `mango-job`、`order-service`。 |
| 执行服务 `ownerService` | 任务归属的业务服务。为空时使用 `appCode`。 |
| Worker 分组 `workerGroup` | 同一服务下的执行隔离分组。为空时使用 `ownerService`。 |
| 任务编码 `jobCode` | 租户和应用内唯一。建议使用稳定业务编码。 |
| 任务名称 `jobName` | 后台展示名称。 |
| 任务类型 `jobType` | 当前使用 `BUILTIN`。 |
| 调度类型 `scheduleType` | `CRON`、`FIXED_RATE`、`ONE_TIME`、`MANUAL`。 |
| 调度表达式 `scheduleExpression` | 根据调度类型填写。 |
| 处理器名称 `handlerName` | Java 处理器返回的 `handlerName()`。 |
| 参数 Schema `paramSchema` | 可选，JSON Schema，用于前端结构化参数表单。 |
| 默认参数 `paramValue` | 可选，JSON。手动触发时可以覆盖。 |
| 引擎类型 `engineType` | 固定 `MANGO_NATIVE`。 |

调度表达式：

| 调度类型 | 表达式示例 | 说明 |
|---|---|---|
| `CRON` | `0 */1 * * * *` | Spring Cron 表达式，每 1 分钟执行一次。 |
| `FIXED_RATE` | `60000` | 固定频率，单位毫秒。 |
| `ONE_TIME` | `2026-06-08T10:30:00` | `LocalDateTime` 字符串。 |
| `MANUAL` | 留空 | 只允许人工触发。 |

## 5. 如何实现处理器

业务模块实现 `MangoJobHandler` 并注册为 Spring Bean。

```java
package io.mango.example.job;

import io.mango.job.api.handler.MangoJobHandleContext;
import io.mango.job.api.handler.MangoJobHandleResult;
import io.mango.job.api.handler.MangoJobHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class OrderSyncJobHandler implements MangoJobHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderSyncJobHandler.class);

    @Override
    public String appCode() {
        return "order-service";
    }

    @Override
    public String serviceCode() {
        return "order-service";
    }

    @Override
    public String workerGroup() {
        return "order-service";
    }

    @Override
    public Set<String> supportedJobCodes() {
        return Set.of("order_sync_every_minute");
    }

    @Override
    public String handlerName() {
        return "orderSyncJobHandler";
    }

    @Override
    public MangoJobHandleResult handle(MangoJobHandleContext context) {
        LOGGER.info("Order sync job started, tenantId={}, jobCode={}, instanceId={}, parameter={}",
                context.getTenantId(), context.getJobCode(), context.getInstanceId(), context.getParameter());
        System.out.println("Order sync stdout example, instanceId=" + context.getInstanceId());
        return MangoJobHandleResult.success("order sync completed");
    }
}
```

处理器归属规则：

- `appCode()` 为空时，优先使用 `mango.job.native.app-code`，再使用 `spring.application.name`，最后使用 `local`。
- `serviceCode()` 为空时，优先使用 `mango.job.native.service-code`，再使用 `appCode`。
- `workerGroup()` 为空时，优先使用 `mango.job.native.worker-group`，再使用 `serviceCode`。
- `supportedJobCodes()` 为空表示该处理器不限制 `jobCode`；生产任务建议显式声明。

## 6. 参数如何获取

执行上下文为 `MangoJobHandleContext`。

| 字段 | 含义 |
|---|---|
| `tenantId` | 当前租户。 |
| `appCode` | 任务所属逻辑应用。 |
| `jobCode` | 任务编码。 |
| `instanceId` | 当前执行实例 ID。 |
| `operatorId` | 人工触发时的操作人。 |
| `triggerType` | `MANUAL` 或 `SCHEDULED`。 |
| `triggerBatchNo` | 触发批次号。 |
| `traceId` | 链路 ID。 |
| `parameter` | 本次执行参数 JSON。 |

参数取值规则：

- 手动触发传入参数时，使用本次触发参数。
- 手动触发未传参数、定时触发时，使用任务定义里的默认参数 `paramValue`。
- 前端支持按 `paramSchema` 渲染结构化表单；后端处理器收到的仍是 JSON 字符串。

参数 Schema 示例：

```json
{
  "type": "object",
  "properties": {
    "batchSize": {
      "type": "number",
      "title": "批量大小",
      "default": 100
    },
    "dryRun": {
      "type": "boolean",
      "title": "只试运行",
      "default": false
    }
  }
}
```

默认参数示例：

```json
{
  "batchSize": 100,
  "dryRun": false
}
```

## 7. Worker 如何发现和派发

Worker 来源：

| 来源 | 通信方式 | 是否可手动添加 | 说明 |
|---|---|---|---|
| `EMBEDDED_AUTO` | `IN_MEMORY` | 否 | 单体内嵌 Worker，由当前 JVM 的真实处理器自动注册。 |
| `REMOTE_AUTO` | `HTTP_INTERNAL` | 是 | 远程 Worker 通过心跳注册到 JobCenter。 |
| `MANUAL` | `HTTP_INTERNAL` | 是 | 管理后台手动登记远程 Worker。 |

派发规则：

1. 任务定义绑定 `appCode`、`ownerService`、`workerGroup`、`handlerName`、`jobCode`。
2. Worker 注册自己的 `serviceCode`、`workerGroup` 和处理器能力。
3. JobCenter 只选择在线、同租户、同服务、同分组、同应用、同处理器且支持该 `jobCode` 的 Worker。
4. Worker 执行前再次校验处理器归属；归属不匹配会拒绝执行。

因此，A 服务的任务不会被随机派发给 B 服务 Worker。前提是任务定义和处理器归属字段按业务服务真实填写。

## 8. 执行实例与日志

进入：

```text
平台能力 -> 任务管理 -> 执行实例
```

执行实例表示一次任务运行。实例行内可以查看：

- 任务名称和任务编码。
- 触发类型、触发时间、开始时间、结束时间、耗时。
- 执行状态和失败摘要。
- Worker 地址。
- 处理器返回结果。
- 运行日志。

日志来源：

- `System.out`
- `System.err`
- 日志框架输出
- Mango Job 运行时事件
- 处理器返回值摘要
- 告警发送结果

当前 UI 从实例详情读取 `mango_job_log_chunk`，一次最多展示最新 1000 条日志。

## 9. 告警与通知

进入：

```text
平台能力 -> 任务管理 -> 告警规则
```

当前支持失败实例告警：

| 字段 | 值 |
|---|---|
| 告警类型 | `INSTANCE_FAILED` |
| 通知场景编码 | `job.instance.failed` |
| 通知模板编码 | `job.instance.failed.site` |
| 通知参数 | JSON，支持 `userId`、`userIds`、`recipientRuleCode` |

Mango Job 只负责在任务失败时向 `mango-notice` 提交发送命令。系统消息、短信、邮件、企业微信等通道由 `mango-notice` 根据模板和收件规则处理。

初始化 SQL：

- 业务域：`mango/mango-platform/mango-domain/mango-domain-core/src/main/resources/db/migration/domain/V2__seed_job_domain.sql`
- 系统消息模板：`mango/mango-platform/mango-notice/mango-notice-core/src/main/resources/db/migration/notice/V14__seed_job_site_message.sql`

通知业务域显示名为 `定时任务`，业务 Key 为 `job.instance.failed`。

## 10. 数据库

Mango Job 使用模块独立数据库约定：

```yaml
mango:
  persistence:
    modules:
      mango-job:
        datasource: job
```

生产推荐数据库名：

```text
mango_job
```

核心表：

| 表 | 说明 |
|---|---|
| `mango_job_definition` | 任务定义。 |
| `mango_job_schedule_cursor` | 调度游标。 |
| `mango_job_instance` | 执行实例。 |
| `mango_job_attempt` | 执行尝试。 |
| `mango_job_worker_snapshot` | Worker 快照。 |
| `mango_job_worker_capability` | Worker 处理器能力。 |
| `mango_job_log_index` | 日志索引。 |
| `mango_job_log_chunk` | 日志内容。 |
| `mango_job_alarm_rule` | 告警规则。 |

## 11. 常用验收步骤

1. 启动后端和前端。
2. 打开管理后台。
3. 进入 `平台能力 -> 任务管理 -> Worker 节点`，确认内嵌 Worker 在线。
4. 进入 `任务定义`，创建 `CRON` 任务，表达式填写 `0 */1 * * * *`。
5. 启用任务，等待至少 2 个调度周期。
6. 进入 `执行实例`，按任务名称筛选，确认每分钟产生实例。
7. 打开实例日志，确认能看到 `System.out`、日志框架输出和处理器返回结果。
8. 将探针参数设置为 `{"fail": true}` 或使用会失败的业务处理器，验证失败实例和系统消息。

## 12. 排查清单

任务不执行：

- 任务状态是否已启用。
- `scheduleType` 和 `scheduleExpression` 是否有效。
- `mango.job.native.scheduler-enabled` 是否为 `true`。
- `mango_job_schedule_cursor.next_fire_time` 是否到期。
- 多节点是否连接同一个 `mango_job` 数据库。

找不到 Worker：

- `Worker 节点` 是否在线。
- 任务 `ownerService`、`workerGroup` 是否和 Worker 能力一致。
- 任务 `handlerName` 是否和处理器 `handlerName()` 一致。
- 处理器 `supportedJobCodes()` 是否包含当前 `jobCode`。
- 内嵌 Worker 是否启用，远程 Worker 地址是否是 `http(s)://`。

日志为空：

- 实例是否已经进入 `RUNNING`、`SUCCESS` 或 `FAILED`。
- 处理器是否实际输出 `System.out`、`System.err` 或日志框架日志。
- `mango_job_log_chunk` 是否有对应 `instance_id`。

告警未发送：

- 是否存在启用的 `INSTANCE_FAILED` 告警规则。
- `notice_scene_code` 是否为 `job.instance.failed`。
- `mango-notice` 是否启用并存在对应模板。
- 通知参数中的用户或收件规则是否有效。
