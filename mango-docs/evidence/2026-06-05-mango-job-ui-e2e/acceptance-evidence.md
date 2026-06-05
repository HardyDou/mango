# Mango Job UI 与 E2E 验收证据

## 1. 验收范围

- 页面：平台能力 / 任务管理 / 任务定义、执行实例、执行日志、Worker、引擎状态。
- 接口：`/api/job/definitions/page`、`/api/job/definitions`、`/api/job/definitions/status`、`/api/job/definitions/trigger`、`/api/job/instances/page`、`/api/job/logs/page`、`/api/job/workers/page`、`/api/job/engines/status`；`/api/job/handlers` 仅作为任务定义执行动作辅助接口，不作为独立菜单验收项。
- 权限：使用 `admin` 账号通过真实登录、租户选择和页面权限指令访问 Job 菜单与操作按钮。
- 数据：保留 `mango_job_example_manual_builtin`、`mango_job_example_cron_powerjob`、`mango_job_example_fixed_rate_probe` 三条典型示例任务；E2E 临时任务创建后完成删除回收。
- 部署形态：本地 Mango 后端进程内 PowerJob Worker + 外部 PowerJob Server 容器 + 前端 source 模式。

## 2. 执行环境

- 前端地址：`http://127.0.0.1:8347`
- 后端地址：`http://127.0.0.1:18657`
- 主数据库：`mango_dev_a1ce46`
- Job 数据库：`mango_job`
- PowerJob Server：`http://127.0.0.1:7700`
- PowerJob Worker：`appId=1`、`appName=mango-job`、`port=27777`
- 租户：`芒果集团` / `TENANT-ID=1`
- 测试账号：`admin`
- 浏览器：Playwright Chromium Chrome channel

## 3. 关键运行数据

- 示例 Job：`mango_job_example_manual_builtin`
- 最新触发批次：`e2e-job-batch-1780702706896`
- 最新任务 ID：`2063042758866448386`
- 最新实例 ID：`2063042798821388289`
- 最新执行日志 ID：`2063042799362453505`
- Mango `engineJobId`：手动任务 `25`、Cron 任务 `26`、固定频率任务 `27`
- Mango `engineInstanceId`：`943410988542066752`
- PowerJob `instance_info.status`：`5`
- PowerJob `instance_info.result`：`Mango Job runtime probe executed`
- PowerJob `task_tracker_address`：`192.168.31.200:27777`
- 后台日志位置：`mango-job://jobs/2063042758866448386/instances/2063042798821388289`
- 说明：本轮已启用真实 PowerJob Server 和 Mango 进程内 Worker，示例任务通过 `mangoPowerJobProcessor` 派发到 `mangoJobRuntimeProbeHandler` 并执行成功，Mango 侧实例状态为 `SUCCESS`。

## 4. 截图清单

| 截图 | 覆盖功能点 | 核心验收内容 | 结论 |
|---|---|---|---|
| `00-definition-create-manual-schedule.png` | 新增任务 | 手动调度、处理器、超时、并发策略、错过策略、参数 Schema、默认参数、重试策略均可配置 | PASS |
| `01-definition-list-frequency.png` | 任务定义列表 | 调度列可见，展示 Cron `0 */5 * * * ?`、固定频率 `60000`、手动三类频次 | PASS |
| `02-definition-more-filters.png` | 更多筛选 | 低频筛选项按需展开，包含任务类型、调度类型、引擎 | PASS |
| `03-definition-edit-cron-schedule.png` | 编辑 Cron 任务 | 调度类型为 Cron，调度表达式为 `0 */5 * * * ?`，超时和策略可见 | PASS |
| `04-definition-edit-fixed-rate-schedule.png` | 编辑固定频率任务 | 调度类型为固定频率，调度表达式为 `60000` | PASS |
| `05-definition-status-enabled.png` | 状态流转 | 示例手动任务暂停后重新启用，列表显示已启用并可触发 | PASS |
| `06-trigger-dialog-frequency-manual.png` | 手动触发 | 批次号和触发参数可配置，手动任务频次通过触发弹窗执行 | PASS |
| `07-instance-filtered-trigger-batch.png` | 执行实例 | 按任务 ID 和批次号过滤后，能看到刚触发的实例记录 | PASS |
| `08-execution-log-index.png` | 执行日志 | 按任务 ID 和实例 ID 过滤后，能看到后台生成的日志索引 | PASS |
| `09-definition-delete-confirm.png` | 删除任务 | 草稿临时任务删除前有确认弹窗，删除后列表不再出现 | PASS |
| `10-job-instance.png` | 执行实例列表 | 页面真实调用 `/api/job/instances/page`，布局和搜索区可用 | PASS |
| `10-job-log.png` | 执行日志列表 | 页面真实调用 `/api/job/logs/page`，日志列表可用 | PASS |
| `10-job-worker.png` | Worker 列表 | 页面真实调用 `/api/job/workers/page`，展示 PowerJob `task_tracker_address` 回填的真实 Worker 地址 `*:27777` 和在线状态 | PASS |
| `10-job-engine.png` | 引擎状态 | 页面真实调用 `/api/job/engines/status`，引擎状态页可进入 | PASS |

截图目录绝对路径：

```text
/Users/hardy/Work/mango/.mango/worktrees/mango-job-sprint-1/mango-docs/evidence/2026-06-05-mango-job-ui-e2e
```

## 5. 功能验收记录

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| JOB-UI-001 | 任务定义 / `/api/job/definitions/page` | 列表、搜索、调度频次展示 | `mango_job_example_` | 三条示例任务回显；Cron、固定频率、手动调度均在调度列可见 | 搜索区紧凑展示，更多筛选默认收起且可展开 | Job API 无 4xx/5xx；console error 为空 | `01-definition-list-frequency.png`、`02-definition-more-filters.png` | PASS |
| JOB-UI-002 | 任务定义新增/编辑 | 频次和执行配置 | Cron `0 */5 * * * ?`、固定频率 `60000`、手动空表达式 | 新增和编辑弹窗展示调度类型、调度表达式、超时、并发、错过、参数和重试配置 | 弹窗分为基本信息、执行配置、参数配置；保存按钮可用 | Job API 无 4xx/5xx；console error 为空 | `00-definition-create-manual-schedule.png`、`03-definition-edit-cron-schedule.png`、`04-definition-edit-fixed-rate-schedule.png` | PASS |
| JOB-E2E-001 | `/api/job/definitions/trigger` + `/api/job/instances/page` | 示例 Job 触发并产生实例 | `e2e-job-batch-1780702706896` | 触发接口成功；实例接口包含同一批次号、`MANUAL` 触发类型、`engineInstanceId=943410988542066752` 和 `SUCCESS` 状态 | 触发弹窗可输入批次号和触发参数；实例页可按批次号过滤 | Job API 无 4xx/5xx；console error 为空 | `06-trigger-dialog-frequency-manual.png`、`07-instance-filtered-trigger-batch.png` | PASS |
| JOB-E2E-002 | `/api/job/logs/page` | 后台执行日志索引可见 | `jobId=2063042758866448386`、`instanceId=2063042798821388289` | 日志接口返回一条 POWERJOB 日志索引，`engineInstanceId=943410988542066752`，`logLocation` 为 `mango-job://jobs/.../instances/...` | 执行日志页可按任务 ID 和实例 ID 过滤，表格展示日志位置和引擎实例 | Job API 无 4xx/5xx；console error 为空 | `08-execution-log-index.png` | PASS |
| JOB-E2E-003 | 任务定义 CRUD 和状态流转 | 新增、编辑、暂停、启用、删除 | `mango_job_e2e_tmp_<timestamp>` | 临时任务创建为草稿，编辑后名称更新；示例任务可暂停再启用；临时任务可删除 | 状态操作和删除均有确认反馈；删除后列表不再出现临时任务 | Job API 无 4xx/5xx；console error 为空 | `05-definition-status-enabled.png`、`09-definition-delete-confirm.png` | PASS |
| JOB-E2E-004 | 执行实例、执行日志、Worker、引擎状态 | Job 下全部管理页真实 API 访问 | 菜单逐个进入 | URL 和 heading 正确；`.job-panel` 可见；Worker 表格包含 `:27777` 地址和在线状态；无 401、403、拒绝访问、路由加载失败、加载失败文案 | 搜索型页面 toolbar 高度小于 150px；非搜索页主内容可见 | Job API 无 4xx/5xx；console error 为空 | `10-job-instance.png`、`10-job-log.png`、`10-job-worker.png`、`10-job-engine.png` | PASS |

## 6. 验证命令

```bash
mvn -pl mango-platform/mango-job/mango-job-starter -am test
pnpm --dir mango-ui admin:styles:check
pnpm --dir mango-ui --filter @mango/job build
pnpm --dir mango-ui --filter mango-admin build
PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true pnpm --dir mango-ui --filter mango-admin exec playwright test e2e/specs/job-management.spec.ts --project=chromium --reporter=line
```

## 7. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 |
|---|---|---|---|
| 多浏览器回归 | 本次按验收重点执行 Chromium | 不影响 Chrome/Chromium 管理后台验收，Firefox/WebKit 仍需发布前抽查 | 发布前补跑 Firefox/WebKit 项目 |
