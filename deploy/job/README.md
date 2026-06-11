# Mango Native Job Deployment

## Modes

Mango Job uses Mango Native Job Engine only. PowerJob runtime integration has been removed from the active delivery.

Supported runtime layouts:

- `monolith-embedded`: JobCenter and embedded Worker run in the same Mango application process and use `IN_MEMORY` dispatch.
- `monolith-cluster`: multiple Mango application nodes run JobCenter and embedded Worker. Scheduling windows are protected by database cursor CAS and idempotency keys.
- `jobcenter-remote-worker`: JobCenter runs in one Mango service, and worker services register through Mango internal APIs and execute through `HTTP_INTERNAL`.

Task definitions do not change when the deployment layout changes. Business code only implements Mango `MangoJobHandler`.

## Database

Mango Job governance tables are managed by Mango Flyway in `mango_job`.

Recommended layouts:

- `mango` + `mango_job`: default for production and private deployments.
- `primary` fallback: development or explicitly accepted small deployments only.

Mango Job does not create or manage PowerJob internal tables.

## Mango Configuration

Use the native sample as an external config file:

```bash
SPRING_CONFIG_ADDITIONAL_LOCATION=file:deploy/job/application-job-native.yml \
MANGO_DB_URL=jdbc:mysql://127.0.0.1:3306/mango_dev?useUnicode=true\&characterEncoding=utf8\&useSSL=false\&allowPublicKeyRetrieval=true\&serverTimezone=Asia/Shanghai \
MANGO_DB_USERNAME=root \
MANGO_DB_PASSWORD= \
MANGO_JOB_DB_URL=jdbc:mysql://127.0.0.1:3306/mango_job?useUnicode=true\&characterEncoding=utf8\&useSSL=false\&allowPublicKeyRetrieval=true\&serverTimezone=Asia/Shanghai \
MANGO_JOB_DB_USERNAME=root \
MANGO_JOB_DB_PASSWORD= \
MANGO_JOB_PROBE_ENABLED=true \
scripts/dev-workspace.sh backend
```

## Runtime Parameters

| Environment | Property | Default | Applies to | Notes |
|---|---|---:|---|---|
| `MANGO_JOB_EMBEDDED_WORKER_ENABLED` | `mango.job.native.embedded-worker-enabled` | `true` | JobCenter process | Enable in-process `IN_MEMORY` Worker for monolith layouts. Set `false` for pure JobCenter. |
| `MANGO_JOB_TRANSPORT` | `mango.job.native.transport` | `IN_MEMORY` | JobCenter process | Default dispatch transport. Worker address still takes precedence when it is `in-memory://` or `http(s)://`. |
| `MANGO_JOB_SCHEDULER_ENABLED` | `mango.job.native.scheduler-enabled` | `true` | JobCenter process | Disable on pure Worker nodes. |
| `MANGO_JOB_SCAN_INTERVAL_MILLIS` | `mango.job.native.scan-interval-millis` | `5000` | JobCenter process | Scheduler scan interval. |
| `MANGO_JOB_SCHEDULER_TENANT_ID` | `mango.job.native.scheduler-tenant-id` | `1` | JobCenter process | Tenant context used by the scheduler thread. |
| `MANGO_JOB_SCAN_LIMIT` | `mango.job.native.scan-limit` | `50` | JobCenter process | Max due cursors scanned per tick. |
| `MANGO_JOB_LEASE_SECONDS` | `mango.job.native.lease-seconds` | `300` | JobCenter process | Attempt lease seconds. Must be longer than normal dispatch latency. |
| `MANGO_JOB_WORKER_ADDRESS` | `mango.job.native.worker-address` | empty | Remote Worker process | Required for remote Worker registration, for example `http://worker-a:8080`. |
| `MANGO_JOB_CENTER_ADDRESS` | `mango.job.native.job-center-address` | empty | Remote Worker process | Required when Worker must register to an external JobCenter. |
| `MANGO_JOB_WORKER_HEARTBEAT_INTERVAL_MILLIS` | `mango.job.native.worker-heartbeat-interval-millis` | `15000` | Remote Worker process | Worker registration heartbeat interval. |
| `MANGO_JOB_PROBE_ENABLED` | `mango.job.probe.enabled` | `true` | Job process | Enables built-in probe handler so a default embedded worker is visible after startup. Set `false` when the process must only expose business handlers. |

## Cluster Rules

- All JobCenter nodes must share the same `mango_job` database.
- Do not point nodes in one deployment to isolated Job databases.
- Embedded workers are allowed on multiple nodes; the scheduler still uses the shared cursor and idempotency controls.
- Remote workers must register their app code, address, transport and handler capabilities before receiving tasks.
- Production deployments must verify Cron stability, restart recovery, Worker expiration and log retention before enabling critical scheduled tasks.

## Monitoring

Mango Job production monitoring must be based on the `mango_job` database and application logs until dedicated Micrometer meters are added.

Recommended checks:

| Metric | Query or source | Alert suggestion |
|---|---|---|
| Due cursor backlog | `select count(*) from mango_job_schedule_cursor where next_fire_time <= now() and (lock_until is null or lock_until < now());` | Greater than `scan-limit * 3` for 5 minutes. |
| Scheduler delay | `select timestampdiff(second, min(next_fire_time), now()) from mango_job_schedule_cursor where next_fire_time <= now();` | Greater than twice the expected schedule interval for critical tasks. |
| Running instance backlog | `select count(*) from mango_job_instance where status in ('WAITING','DISPATCHED','RUNNING');` | Continuous growth for 10 minutes. |
| Failure rate | `select count(*) from mango_job_instance where status = 'FAILED' and trigger_time >= date_sub(now(), interval 10 minute);` | Non-zero for critical tasks, or sudden increase over baseline. |
| Expired Worker count | `select count(*) from mango_job_worker_snapshot where status = 'EXPIRED';` | Any critical app Worker expired for two heartbeat windows. |
| Log write health | `select count(*) from mango_job_log_index where last_fetched_at >= date_sub(now(), interval 10 minute);` plus application error logs | No recent log index for active tasks, or SQL insert errors for `mango_job_log_chunk`. |

Application log alerts should include:

- `Mango native job tick failed`
- dispatch failures containing `未找到可执行任务的 Worker`
- database errors on `mango_job_schedule_cursor`, `mango_job_instance`, `mango_job_attempt`, `mango_job_log_chunk`

## Alarm Integration

Mango Job sends failed execution alarms through `mango-notice` when an enabled rule exists.

Rules can be maintained from Mango Admin:

```text
平台能力 -> 任务管理 -> 告警规则
```

The page supports task-level rules and app-level default rules. It stores only Job alarm routing fields in `mango_job_alarm_rule`; notice templates, recipient rules and third-party channels remain owned by `mango-notice`.

Required `mango_job_alarm_rule` fields:

| Field | Value |
|---|---|
| `tenant_id` | Tenant that owns the job definition. |
| `app_code` | Job application code, for example `mango-job`. |
| `job_id` | Specific job definition ID. Leave `null` for an app-level failed-instance rule. |
| `alarm_type` | `INSTANCE_FAILED`. |
| `notice_scene_code` | Failed-instance notice business key. Current value must be `job.instance.failed`; Mango Job maps this to `SendNoticeCommand.bizType`. |
| `notice_template_code` | Notice template code. Current SITE template is `job.instance.failed.site`; Mango Job passes it in notice params as `noticeTemplateCode`. |
| `notice_params` | Optional JSON. Supported keys are `userId`, `userIds`, and `recipientRuleCode`. |
| `enabled` | `1` to enable the rule. |

Example:

```sql
insert into mango_job_alarm_rule
  (tenant_id, app_code, job_id, rule_name, alarm_type, trigger_condition,
   notice_scene_code, notice_template_code, notice_params, enabled, created_at, updated_at)
values
  ('1', 'mango-job', null, '定时任务失败告警', 'INSTANCE_FAILED', '{"status":"FAILED"}',
   'job.instance.failed', 'job.instance.failed.site',
   '{"recipientRuleCode":"jobDuty"}', 1, now(), now());
```

Notice templates, recipient rules and third-party channels are configured in `mango-notice`. The seeded Mango SITE message template is in `mango/mango-platform/mango-notice/mango-notice-core/src/main/resources/db/migration/notice/V14__seed_job_site_message.sql`, and the notice business domain display name is `定时任务`.

Production readiness requires a pre-production failed-job test that verifies the notice send record and the target system message, SMS, email or enterprise WeCom channel.

## Log Retention

Current native execution logs are stored in:

- `mango_job_log_index`
- `mango_job_log_chunk`
- `mango_job_operation_log`

Retention policy for production:

- The current UI reads the latest 1000 log chunks for one execution instance. Jobs that can produce more lines must also write business evidence to the enterprise log platform or file center.
- Keep execution log chunks for at least 30 days by default.
- Keep operation logs for at least 180 days by default.
- Keep failed-instance logs for at least 90 days when storage budget allows.
- Before deleting chunks, retain `mango_job_log_index` rows with an archive marker in `error_summary` or export logs to the enterprise log platform.
- Run deletion in small batches during low traffic windows. Do not delete by full table scan in business hours.

Suggested cleanup predicates:

```sql
delete from mango_job_log_chunk
where log_time < date_sub(now(), interval 30 day)
limit 1000;

delete from mango_job_operation_log
where created_at < date_sub(now(), interval 180 day)
limit 1000;
```

The above SQL is an operations reference. Production cleanup should be executed through the platform DBA job or an approved maintenance task.

## Rollback

Code rollback:

- Roll back the application image or package to the previous version.
- Set `MANGO_JOB_SCHEDULER_ENABLED=false` before rollback if scheduled tasks are creating incorrect executions.
- Keep `mango_job` database untouched during first rollback so execution facts remain auditable.

Menu rollback:

- Disable the `平台能力/任务管理` menu entries through the authorization module if UI access must be hidden.
- Do not delete menu rows in production unless the release rollback checklist explicitly includes authorization data rollback.

Database rollback:

- Flyway migrations are forward-only in Mango. Do not modify executed migration files.
- If `V4__native_job_engine_foundation.sql` has executed, roll back application code only; keep tables for audit and future forward fix.
- Destructive rollback of Job tables is allowed only before production traffic or after an explicit data retention approval.

Task recovery after rollback:

- Pause critical task definitions before rolling back if the previous version cannot understand the current Job model.
- After forward fix, compare `mango_job_schedule_cursor.last_fire_time`, recent `mango_job_instance` rows, and business idempotency keys before re-enabling critical schedules.
