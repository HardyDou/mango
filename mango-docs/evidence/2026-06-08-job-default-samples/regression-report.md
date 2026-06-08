# Job 默认示例任务回归测试报告

- 日期：2026-06-08
- 分支：feature/job-default-samples
- 范围：Mango Job 默认示例任务 SQL 固化、任务定义页面可见性、Worker 节点页面回归

## 改动验证

新增 Flyway migration `V6__seed_default_sample_jobs.sql`，向 `mango_job_definition` 幂等写入 2 条默认示例任务：

- `mango_job_default_sample_manual_probe`
- `mango_job_default_sample_cron_probe`

两条任务均为 `DISABLED`，`sync_status=PENDING`，处理器为 `mangoJobRuntimeProbeHandler`。任务默认不自动执行，后续启用时由现有同步流程接入 native job engine。

## 数据库核验

本地库：`mango_dev_3ab00a`

```sql
SELECT job_code, job_name, app_code, owner_service, worker_group, status, sync_status, handler_name
FROM mango_dev_3ab00a.mango_job_definition
WHERE job_code LIKE 'mango_job_default_sample_%'
ORDER BY job_code;
```

结果：2 条默认示例任务均存在，状态为 `DISABLED`，同步状态为 `PENDING`。

## 自动化验证

```bash
mvn -pl :mango-job-starter -am test -Dtest=MangoJobMultiDataSourceIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false
```

结果：通过。`Tests run: 26, Failures: 0, Errors: 0`。

```bash
pnpm exec playwright test /Users/hardy/Work/mango/.runtime/e2e/job-seed-visibility/job-seed-visibility.spec.ts --config=/Users/hardy/Work/mango/.runtime/e2e/job-seed-visibility/playwright.config.ts
```

结果：通过。覆盖登录、任务定义 API、Worker API、任务定义页面筛选、Worker 节点页面筛选、Job 页面失败请求和 console error 检查。

## 截图

- `job-definition-default-samples.png`：任务定义页面筛选默认示例任务。
- `job-worker-embedded.png`：Worker 节点页面筛选内嵌 worker。

## 回归结论

- 示例任务已固化为数据库 migration，后续新环境执行 Flyway 后默认存在。
- 示例任务默认停用，不会产生自动调度副作用。
- 内嵌 Worker 节点在 probe enabled 配置下可见。
- 本次未发现 Job 页面 `/api/job/` 或前端 `/assets/` 失败请求，未发现 console error。

## 未覆盖项

- 未在全新空库完整重建环境执行全量 Flyway，只在当前本地库手动执行新增 SQL 并验证结果。
