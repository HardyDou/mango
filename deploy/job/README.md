# Mango Native Job Deployment

## 1. 概览
`deploy/job` 是 Mango 原生任务调度的部署配置说明，配套后端能力模块 [mango-job](../../mango/mango-platform/mango-job/README.md) 使用。

这里说明三件事：

- JobCenter、内嵌 Worker、远程 Worker 应该怎么部署。
- `deploy/job/application-job-native.yml` 暴露了哪些数据库和运行时配置。
- 生产环境如何监控、告警、保留执行日志和回滚。

业务开发写任务处理器时先看 `mango-job` 模块 README；部署、联调和生产运维再看本文。


## 2. 能力边界
- 不部署 PowerJob Server，不创建 PowerJob 内部表。
- 不替代 `MangoJobHandler` 的业务实现、业务幂等和业务事务设计。
- 不负责通知渠道配置，失败告警真正发送依赖 `mango-notice`。
- 不作为 DBA 审批流程，生产清理 SQL 必须走企业维护任务或 DBA 流程。

## 3. 部署模式

Mango Job 当前只使用 Mango Native Job Engine。活跃交付中已经移除 PowerJob 运行时集成。

支持的运行布局：

| 模式 | 适用 | 关键配置 |
|------|------|----------|
| `monolith-embedded` | 单体、本地开发、小规模私有部署 | JobCenter 和 embedded Worker 在同一个 Mango 应用进程，`embedded-worker-enabled=true`，`transport=IN_MEMORY`。 |
| `monolith-cluster` | 多个 Mango 应用节点共同承载任务 | 所有节点共享同一个 `mango_job` 数据库；调度窗口由数据库游标 CAS 和幂等键保护。 |
| `jobcenter-remote-worker` | 独立调度中心 + 业务 Worker 服务 | JobCenter 开启 scheduler；Worker 配置 `worker-address` 和 `job-center-address` 后通过 Mango 内部接口注册，并由 `HTTP_INTERNAL` 执行。 |

任务定义不随部署布局变化。业务代码只实现 Mango `MangoJobHandler`。

## 4. 接入方式
### 5.1 JobCenter 或单体应用

提供管理接口、调度扫描、数据库治理模型的进程引入：

```xml
<dependency>
    <groupId>io.mango.platform.job</groupId>
    <artifactId>mango-job-starter</artifactId>
</dependency>
```

单体内嵌 Worker 同样使用 `mango-job-starter`，并保持 `mango.job.native.embedded-worker-enabled=true`。

### 5.2 远程 Worker

只执行任务、不承载 Job 管理页面和调度扫描的业务服务引入：

```xml
<dependency>
    <groupId>io.mango.platform.job</groupId>
    <artifactId>mango-job-starter-remote</artifactId>
</dependency>
```

远程 Worker 需要实现 `MangoJobHandler`，并配置：

- `mango.job.native.worker-address`
- `mango.job.native.job-center-address`
- `mango.job.native.job-center-feign-url`

## 5. 配置文件和启动方式

外部配置文件：

```text
deploy/job/application-job-native.yml
```

本地或运维脚本可通过 `SPRING_CONFIG_ADDITIONAL_LOCATION` 加载：

```bash
SPRING_CONFIG_ADDITIONAL_LOCATION=file:deploy/job/application-job-native.yml \
MANGO_DB_URL=jdbc:mysql://127.0.0.1:3306/mango_dev?useUnicode=true\&characterEncoding=utf8\&useSSL=false\&allowPublicKeyRetrieval=true\&serverTimezone=Asia/Shanghai \
MANGO_DB_USERNAME=root \
MANGO_DB_PASSWORD= \
MANGO_JOB_DB_URL=jdbc:mysql://127.0.0.1:3306/mango_job?useUnicode=true\&characterEncoding=utf8\&useSSL=false\&allowPublicKeyRetrieval=true\&serverTimezone=Asia/Shanghai \
MANGO_JOB_DB_USERNAME=root \
MANGO_JOB_DB_PASSWORD= \
MANGO_JOB_PROBE_ENABLED=true \
mango dev start backend
```

`application-job-native.yml` 会把 `mango.persistence.modules.mango-job.datasource` 指向 `job` 数据源。未提供 `MANGO_JOB_DB_*` 时会回退到主库 `MANGO_DB_*`。

## 6. 配置说明
### 7.1 数据库配置

| 环境变量 | 配置项 | 默认值 | 含义 |
|----------|--------|--------|------|
| `MANGO_DB_URL` | `mango.persistence.datasources.primary.url` | 无 | Mango 主库 JDBC URL。 |
| `MANGO_DB_USERNAME` | `mango.persistence.datasources.primary.username` | 无 | Mango 主库用户名。 |
| `MANGO_DB_PASSWORD` | `mango.persistence.datasources.primary.password` | 无 | Mango 主库密码。 |
| `MANGO_JOB_DB_URL` | `mango.persistence.datasources.job.url` | `${MANGO_DB_URL}` | Job 独立库 JDBC URL。 |
| `MANGO_JOB_DB_USERNAME` | `mango.persistence.datasources.job.username` | `${MANGO_DB_USERNAME}` | Job 独立库用户名。 |
| `MANGO_JOB_DB_PASSWORD` | `mango.persistence.datasources.job.password` | `${MANGO_DB_PASSWORD}` | Job 独立库密码。 |

推荐数据库布局：

- `mango` + `mango_job`：生产和私有部署默认方案。
- `primary` fallback：仅用于开发环境，或明确接受的小型部署。

Mango Job 治理表由 Flyway 在 `mango_job` 模块路径下维护。Mango Job 不创建、不管理 PowerJob 内部表。

### 7.2 Job 运行参数

| 环境变量 | 配置项 | 默认值 | 作用进程 | 含义 |
|----------|--------|--------|----------|------|
| `MANGO_JOB_EMBEDDED_WORKER_ENABLED` | `mango.job.native.embedded-worker-enabled` | `true` | JobCenter | 是否启用同进程 `IN_MEMORY` Worker。纯 JobCenter 设为 `false`。 |
| `MANGO_JOB_TRANSPORT` | `mango.job.native.transport` | `IN_MEMORY` | JobCenter | 默认派发通道。Worker 地址为 `embedded://`、历史 `in-memory://` 或 `http(s)://` 时地址会优先生效。 |
| `MANGO_JOB_SCHEDULER_ENABLED` | `mango.job.native.scheduler-enabled` | `true` | JobCenter | 是否开启调度扫描。纯 Worker 节点设为 `false`。 |
| `MANGO_JOB_SCAN_INTERVAL_MILLIS` | `mango.job.native.scan-interval-millis` | `5000` | JobCenter | 调度扫描间隔，单位毫秒。 |
| `MANGO_JOB_SCHEDULER_TENANT_ID` | `mango.job.native.scheduler-tenant-id` | `1` | JobCenter | 调度线程使用的租户上下文。 |
| `MANGO_JOB_SCAN_LIMIT` | `mango.job.native.scan-limit` | `50` | JobCenter | 每次最多扫描的到期游标数。 |
| `MANGO_JOB_LEASE_SECONDS` | `mango.job.native.lease-seconds` | `300` | JobCenter | attempt 租约秒数，必须长于正常派发耗时。 |
| `MANGO_JOB_WORKER_ADDRESS` | `mango.job.native.worker-address` | 空 | Worker | 当前 Worker 对外执行地址。内嵌 Worker 可为空；远程 Worker 必填，例如 `http://worker-a:8080`。 |
| `MANGO_JOB_CENTER_ADDRESS` | `mango.job.native.job-center-address` | 空 | 远程 Worker | Worker 注册目标 JobCenter 地址。 |
| `MANGO_JOB_WORKER_HEARTBEAT_INTERVAL_MILLIS` | `mango.job.native.worker-heartbeat-interval-millis` | `15000` | Worker | Worker 注册和心跳间隔。 |
| `MANGO_JOB_PROBE_ENABLED` | `mango.job.probe.enabled` | `true` | Job 进程 | 是否注册内置探测 handler。生产只暴露业务 handler 时设为 `false`。 |

更多 handler 归属字段、API 字段和管理接口见 [mango-job 配置说明](../../mango/mango-platform/mango-job/README.md#8-配置说明)。

## 7. 集群规则

- 所有 JobCenter 节点必须共享同一个 `mango_job` 数据库。
- 不要让同一套部署的节点指向彼此隔离的 Job 数据库。
- 多个节点可以同时启用 embedded Worker；稳定身份地址为 `embedded://{ip}:{server.port}`，派发仍在当前 JVM 执行。
- 调度器使用共享数据库游标和幂等控制，一个任务实例只能被一个运行时租约持有。
- 远程 Worker 必须先注册应用编码、地址、通信方式和 handler 能力，才能接收任务。
- 生产启用关键定时任务前，必须验证 Cron 稳定性、重启恢复、Worker 过期和日志保留。

## 8. 管理入口
菜单和按钮资源由 `mango-job-starter/src/main/resources/META-INF/mango/resource-manifest.json` 描述，并通过 Mango 模块资源初始化流程入库。

管理入口：

```text
平台能力 -> 任务管理
```

部署层不直接创建菜单。菜单不可见时先检查 `mango-job-starter` 是否引入、资源 manifest 是否被扫描、当前角色是否绑定任务管理权限。

调度器和 Worker 自动注册使用 `mango.job.native.scheduler-tenant-id` 构造系统上下文，默认租户是 `1`。业务任务执行时仍要由 handler 自己保证业务数据的租户边界和幂等。

## 9. 告警集成

Mango Job 在存在启用规则时，通过 `mango-notice` 发送失败执行告警。

规则可在 Mango Admin 维护：

```text
平台能力 -> 任务管理 -> 告警规则
```

页面支持任务级规则和应用级默认规则。`mango_job_alarm_rule` 只保存 Job 告警路由字段；通知模板、接收人规则和第三方渠道仍由 `mango-notice` 负责。

`mango_job_alarm_rule` 关键字段：

| 字段 | 值 |
|------|----|
| `tenant_id` | 任务定义所属租户。 |
| `app_code` | Job 应用编码，例如 `mango-job`。 |
| `job_id` | 指定任务定义 ID；应用级失败规则可留空。 |
| `alarm_type` | 当前使用 `INSTANCE_FAILED`。 |
| `notice_scene_code` | 失败实例通知业务键。当前值为 `job.instance.failed`，Mango Job 会映射为 `SendNoticeCommand.bizType`。 |
| `notice_template_code` | 通知模板编码。当前 SITE 模板为 `job.instance.failed.site`，Mango Job 会放入通知参数 `noticeTemplateCode`。 |
| `notice_params` | 可选 JSON，支持 `userId`、`userIds`、`recipientRuleCode`。 |
| `enabled` | `1` 表示启用。 |

示例：

```sql
insert into mango_job_alarm_rule
  (tenant_id, app_code, job_id, rule_name, alarm_type, trigger_condition,
   notice_scene_code, notice_template_code, notice_params, enabled, created_at, updated_at)
values
  ('1', 'mango-job', null, '定时任务失败告警', 'INSTANCE_FAILED', '{"status":"FAILED"}',
   'job.instance.failed', 'job.instance.failed.site',
   '{"recipientRuleCode":"jobDuty"}', 1, now(), now());
```

通知业务类型、模板、接收人规则和第三方渠道在 `mango-notice` 配置。Job 失败站内信模板种子位于 `mango/mango-platform/mango-notice/mango-notice-core/src/main/resources/db/migration/notice/V14__seed_job_site_message.sql`，通知业务域显示名为 `定时任务`。

生产就绪必须做一次预发失败任务测试，确认通知发送记录以及目标站内信、短信、邮件或企业微信通道。

## 10. 监控

在专用 Micrometer 指标补齐前，生产监控基于 `mango_job` 数据库和应用日志。

推荐检查：

| 指标 | 查询或来源 | 告警建议 |
|------|------------|----------|
| 到期游标积压 | `select count(*) from mango_job_schedule_cursor where next_fire_time <= now() and (lock_until is null or lock_until < now());` | 连续 5 分钟大于 `scan-limit * 3`。 |
| 调度延迟 | `select timestampdiff(second, min(next_fire_time), now()) from mango_job_schedule_cursor where next_fire_time <= now();` | 关键任务超过预期调度间隔的 2 倍。 |
| 运行实例积压 | `select count(*) from mango_job_instance where status in ('WAITING','DISPATCHED','RUNNING');` | 连续 10 分钟增长。 |
| 失败率 | `select count(*) from mango_job_instance where status = 'FAILED' and trigger_time >= date_sub(now(), interval 10 minute);` | 关键任务非零，或较基线突增。 |
| 过期 Worker | `select count(*) from mango_job_worker_snapshot where status = 'EXPIRED';` | 关键应用 Worker 连续两个心跳窗口过期。 |
| 日志写入健康 | `select count(*) from mango_job_log_index where last_fetched_at >= date_sub(now(), interval 10 minute);` 加应用错误日志 | 活跃任务没有近期日志索引，或 `mango_job_log_chunk` 写入报错。 |

应用日志告警应覆盖：

- `Mango native job tick failed`
- 包含 `未找到可执行任务的 Worker` 的派发失败
- `mango_job_schedule_cursor`、`mango_job_instance`、`mango_job_attempt`、`mango_job_log_chunk` 相关数据库错误

## 11. 日志保留

当前原生执行日志存储在：

- `mango_job_log_index`
- `mango_job_log_chunk`
- `mango_job_operation_log`

生产保留策略：

- 当前 UI 读取单个执行实例最新 1000 个 log chunk。输出更多日志的任务必须同时把业务证据写入企业日志平台或文件中心。
- 默认至少保留执行 log chunk 30 天。
- 默认至少保留操作日志 180 天。
- 存储预算允许时，失败实例日志至少保留 90 天。
- 删除 chunk 前，应保留 `mango_job_log_index` 行并在 `error_summary` 写入归档标记，或把日志导出到企业日志平台。
- 低峰小批量删除，业务高峰期不要做全表扫描删除。

参考清理谓词：

```sql
delete from mango_job_log_chunk
where log_time < date_sub(now(), interval 30 day)
limit 1000;

delete from mango_job_operation_log
where created_at < date_sub(now(), interval 180 day)
limit 1000;
```

以上 SQL 只是运维参考。生产清理必须通过平台 DBA 任务或已审批维护任务执行。

## 12. 回滚

代码回滚：

- 回滚应用镜像或发布包到上一版本。
- 如果调度任务正在产生错误执行，先设置 `MANGO_JOB_SCHEDULER_ENABLED=false`。
- 首次回滚时保留 `mango_job` 数据库，保证执行事实可审计。

菜单回滚：

- 如果只需要隐藏 UI 入口，通过授权模块禁用 `平台能力/任务管理` 菜单。
- 生产环境不要直接删除菜单行，除非发布回滚清单明确包含授权数据回滚。

数据库回滚：

- Mango Flyway migration 是前进式。不要修改已经执行过的 migration 文件。
- 如果 `V4__native_job_engine_foundation.sql` 已执行，优先只回滚应用代码，保留表用于审计和后续前进修复。
- 只有在生产流量进入前，或获得明确数据保留审批后，才允许破坏性回滚 Job 表。

任务恢复：

- 如果旧版本不能理解当前 Job 模型，回滚前先暂停关键任务定义。
- 前进修复后，对比 `mango_job_schedule_cursor.last_fire_time`、近期 `mango_job_instance` 和业务幂等键，再恢复关键调度。

## 13. 发布前确认
后端模块测试：

```bash
mvn -f mango/pom.xml -pl mango-platform/mango-job -am test
```

部署链路确认：

- 使用 `deploy/job/application-job-native.yml` 启动后，`mango.persistence.modules.mango-job.datasource` 指向预期数据源。
- `mango_job` 数据库完成 Flyway migration，核心表 `mango_job_definition`、`mango_job_schedule_cursor`、`mango_job_worker_snapshot`、`mango_job_instance`、`mango_job_log_chunk` 存在。
- 单体内嵌模式下，Worker 页面能看到 `embedded://{ip}:{port}` Worker。
- 远程 Worker 模式下，Worker 调用 JobCenter 注册成功，JobCenter 能派发到 `/job/internal/workers/execute`。
- 新建 `MANUAL` 或示例 probe 任务后，手动触发能生成 instance、attempt、log index 和 log chunk。
- 关闭 `MANGO_JOB_SCHEDULER_ENABLED=false` 后，不再自动扫描到期任务。
- 配置失败告警后，失败任务能在 `mango-notice` 产生发送记录。

## 14. 相关文档
- [Mango Job 模块 README](../../mango/mango-platform/mango-job/README.md)
- [Job 前端 README](../../mango-ui/packages/job/README.md)
- [Notice 模块 README](../../mango/mango-platform/mango-notice/README.md)
- [能力说明维护规范](../../mango-pmo/rules/08-capability-docs.md)
