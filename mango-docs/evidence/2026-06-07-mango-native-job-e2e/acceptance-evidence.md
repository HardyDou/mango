# Mango 原生 Job Engine 验收证据

## 1. 验收范围

- 页面：平台能力/任务管理/任务定义、执行实例、Worker 节点、运行状态、告警规则。
- 接口：`/api/job/definitions/page`、`/api/job/instances/page`、`/api/job/instances/{id}/logs`、`/api/job/workers/page`、`/api/job/engines/status`、`/api/job/alarm-rules/page`、`/api/job/alarm-rules/detail`、`/api/job/alarm-rules/status`。
- 权限：管理员登录后使用真实菜单和 `job:*` 权限访问，V46 告警规则菜单和按钮权限已入库并授予角色 1。
- 数据：`mango_dev_a1ce46`，`primary` 与 `job` 两个数据源健康检查均为 `UP`。
- 部署形态：本地单体 JobCenter + `IN_MEMORY` Worker，远程 Worker 由后端 E2E 覆盖 `HTTP_INTERNAL`。

## 2. 执行环境

- 前端地址：`http://127.0.0.1:8347`
- 后端地址：`http://127.0.0.1:18657`
- 数据库或租户：`127.0.0.1:3306/mango_dev_a1ce46`，租户 `1`
- 测试账号：`admin`
- 浏览器：Playwright Chromium、Firefox、WebKit

## 3. 功能验收记录

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| JOB-ACC-001 | `/job/definition`、`/api/job/definitions/page` | 任务定义新增、查询、编辑、启停、删除 | `mango_job_example_manual_builtin`、`mango_job_example_cron_native` | 列表能回显任务名称、频次、状态；删除后目标任务行消失 | 搜索区紧凑布局，新增/编辑弹窗字段分组，操作列稳定 | Playwright 收集 `/api/job/` 响应，失败请求数组为空 | `00-definition-create-manual-schedule.png`、`01-definition-list-frequency.png`、`03-definition-edit-cron-schedule.png`、`09-definition-delete-confirm.png` | PASS |
| JOB-ACC-002 | `/job/definition` | 结构化参数表单 | `source`、`assert`、`kind` 参数字段 | 保存 payload 为 JSON 对象，页面通过 schema 字段填写和回显 | 参数字段不要求用户直接编辑 JSON 字符串 | 任务保存、详情和触发请求均返回业务成功体 | `00-definition-create-manual-schedule.png`、`06-trigger-dialog-frequency-manual.png` | PASS |
| JOB-ACC-005 | `/job/definition`、`/job/instance`、`/api/job/instances/page` | 手动触发创建执行实例并完成执行 | `mangoJobRuntimeProbeHandler`、`e2e-job-batch-*` | 执行实例状态为 `SUCCESS`，实例行显示任务名称、批次、Worker 地址 | 触发弹窗显示频次和参数，实例列表可按任务/批次定位 | 手动触发和实例分页请求均返回业务成功体，失败请求数组为空 | `06-trigger-dialog-frequency-manual.png`、`07-instance-filtered-trigger-batch.png` | PASS |
| JOB-ACC-006 | `/job/instance`、`/api/job/instances/page` | Cron 每 1 分钟调度 | 每分钟示例任务 | 至少产生 2 条非运行态调度实例，计划触发窗口连续推进 | 执行实例列表可见调度来源和任务名称 | E2E 等待真实调度窗口完成，未使用页面替身数据 | `08c-scheduled-every-minute-instance.png` | PASS |
| JOB-ACC-020 | `/api/job/definitions/*`、`/api/job/instances/page`、`/api/job/instances/{id}/logs` | 本地每分钟 Cron 连续稳定性观察 | `mango_job_stability_chromium_1780824592232` | 3 分钟观察产生 3 个 `SUCCESS` 调度实例，计划窗口分别为 `17:30:00`、`17:31:00`、`17:32:00`，重复窗口 0，失败实例 0 | 走真实登录和真实 Job API，不使用替身数据；样本实例 Worker 为 `in-memory://...` | `job-scheduler-stability.spec.ts` 执行结果 `1 passed, 2 skipped`；日志详情包含 `System.out` 和 logger 内容 | `job-scheduler-stability-local.md` | PASS |
| JOB-ACC-007 | `/job/instance`、`/api/job/instances/{id}/logs` | 执行实例行内日志详情 | 手动实例和每分钟实例 | 日志详情包含 stdout、logger、handler result 内容 | 独立“执行日志”菜单不存在，日志从实例行按钮进入 | 日志详情接口返回 `nativeLogContent` 和实例上下文 | `08-execution-instance-log-entry.png`、`08b-execution-log-detail.png`、`08d-scheduled-every-minute-log-detail.png` | PASS |
| JOB-ACC-009 | `/job/worker`、`/api/job/workers/page` | Worker 列表显示真实 Worker | `mango-job` 内嵌 Worker | Worker 地址包含 `in-memory://`，状态为在线，不显示非法 Worker 地址 | Worker 搜索区、表格、状态标签和操作列布局可用 | Worker 分页请求返回真实 worker 快照，失败请求数组为空 | `10-job-worker.png` | PASS |
| JOB-ACC-011 | `/job/engine`、`/api/job/engines/status` | 运行状态使用真实 Job API | `MANGO_NATIVE` | 运行时状态展示待同步、已同步、同步失败和最近更新时间 | 页面主内容不是空白、404 或加载失败，刷新按钮可见 | 运行状态接口返回业务成功体，失败请求数组为空 | `10-job-engine.png` | PASS |
| JOB-ACC-023 | `/job/alarm`、`/api/job/alarm-rules/*` | 告警规则创建、编辑、停用、启用、删除 | `E2E 失败告警 *`、`MANGO_JOB_FAILED_TEMPLATE_E2E`、`jobDutyE2E` | 规则落库后列表可查询；编辑后模板和收件规则变化；停用/启用状态准确；删除后列表不再出现 | 告警规则搜索区、结构化收件字段、任务选择器、状态标签和确认操作可用 | V46 授权 migration 入库后，告警规则分页、详情、保存、状态、删除请求均返回业务成功体 | `15-alarm-rule-create-dialog.png`、`16-alarm-rule-created.png`、`17-alarm-rule-edit-dialog.png`、`18-alarm-rule-disabled.png`、`19-alarm-rule-enabled.png`、`20-alarm-rule-deleted.png` | PASS |
| JOB-ACC-021 | `/job/worker`、`/api/job/workers/*` | Worker 手动登记、禁用、恢复 | `127.0.0.1:39080/e2e-*` | 手动 Worker 创建后在线；禁用后状态为停用；恢复后状态为在线 | 新增 Worker 弹窗、状态操作按钮和二次确认可用 | Worker 创建和状态更新请求返回业务成功体，失败请求数组为空 | `11-worker-create-dialog.png`、`12-worker-created-online.png`、`13-worker-disabled.png`、`14-worker-restored-online.png` | PASS |

## 4. 回归抽查记录

| 模块 | 页面 | 功能点 1 | 功能点 2 | UI 细节 | 截图/trace | 结论 |
|---|---|---|---|---|---|---|
| Job 管理 | 任务管理五个页面 | 菜单路径 `平台能力/任务管理` 可访问 | 独立“执行日志”菜单已移除 | 页面标题、搜索区、工具栏、表格和空/错误态区域可见 | `10-job-instance.png`、`10-job-worker.png`、`10-job-engine.png`、`10-job-alarm.png` | PASS |
| Job 管理 | 告警规则 | V46 权限补执行后接口可访问 | 规则 CRUD 后清理测试数据 | 新增/编辑弹窗字段按业务聚合，不直接暴露 JSON 文本区作为主要输入 | `15-alarm-rule-create-dialog.png` 到 `20-alarm-rule-deleted.png` | PASS |

## 5. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 | 用户确认 |
|---|---|---|---|---|
| 预发真实部署 | 当前只在本地 worktree 验证，未连接预发环境 | 不能直接发布生产 | 按投产就绪计划执行预发数据库、菜单权限、单体和远程 Worker 验证 | 未确认 |
| 2-4 小时调度稳定性 | 本轮新增本地 3 分钟真实连续窗口观察，未做预发 2-4 小时观察 | 不能开放生产长周期定时任务 | 预发连续运行每分钟任务并记录实例数、失败率和积压情况 | 未确认 |
| 真实通知通道 | 本地覆盖 Job 到 `mango-notice` 调用，未验证短信、邮件、企业微信等第三方通道 | 不能声明第三方通道生产可用 | 预发配置 notice 模板和收件人规则，触发失败任务后核验 notice 发送记录和目标通道 | 未确认 |
