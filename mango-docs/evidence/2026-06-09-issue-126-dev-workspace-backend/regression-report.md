# Issue 126 Regression Report

## Scope

- Issue: `#126` 生成业务项目使用 dev-workspace 后端入口真实启动失败
- Worktree: `.mango/worktrees/issue-126-dev-workspace-backend`
- Generated project: `.runtime/projects/issue-126-dev-workspace-backend`
- Backend URL: `http://127.0.0.1:18587`
- Database: `127.0.0.1:3306/mango_dev_issue126`

## Change

- The full project backend template now enables Flyway migrations for `domain` and `mango-job`.
- The CLI regression check asserts that generated full backend projects enable `domain`, `workflow`, and `mango-job` migrations together.

## Acceptance

| Check | Result |
| --- | --- |
| `pnpm -F @mango/cli test` | PASS |
| Generate full/monolith business project with `mango-cli` | PASS |
| Start generated project with `scripts/dev-workspace.sh backend` | PASS |
| `GET /actuator/health` | PASS, status `UP` |
| `biz_domain` table exists | PASS |
| `mango_job_schedule_cursor` table exists | PASS |
| `mango_job_definition` table exists | PASS |
| Default sample jobs exist | PASS, `2` rows |
| Business domain seed exists | PASS, `8` rows |

## Acceptance Table

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| ISSUE-126-001 | `scripts/dev-workspace.sh backend`, `/actuator/health` | full/monolith 生成项目默认后端入口真实启动 | generated project `.runtime/projects/issue-126-dev-workspace-backend`, DB `mango_dev_issue126`, port `18587` | Spring Boot 启动完成；health `UP`；`biz_domain`、`mango_job_schedule_cursor`、`mango_job_definition` 表存在；默认 Job 示例任务 `2` 条；业务域 seed `8` 条 | 后端启动链路，无业务 UI 页面；使用 HTML 验收报告截图留证 | health 请求返回 200/UP；未复现缺表启动失败；无前端 console/network 项 | `regression-report.png`；生成项目启动日志保留在 `.runtime` 临时目录 | PASS |

## Evidence

Health response:

```json
{"status":"UP","components":{"db":{"status":"UP","details":{"database":"MySQL","validationQuery":"isValid()"}}}}
```

Database assertion:

```text
biz_domain
mango_job_definition
mango_job_schedule_cursor
mango_job_definition count = 2
biz_domain count = 8
```

Generated `application.yml` contains:

```text
domain:
  enabled: true
workflow:
  enabled: true
mango-job:
  enabled: true
```

## Notes

- The foreground Maven `spring-boot:run` process was stopped after validation, so Maven reported exit code `143` during shutdown. This happened after Spring Boot had started successfully and health/database assertions had passed.
- Runtime generated project, logs, and build output remain under `.runtime/` and are not committed.
