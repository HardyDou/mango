# Mango Job UI 与 E2E 验收证据

## 1. 验收范围

- 页面：平台能力 / 任务管理 / 任务定义、执行实例、执行日志、Worker、处理器、引擎状态。
- 接口：`/api/job/definitions/page`、`/api/job/definitions`、`/api/job/definitions/status`、`/api/job/definitions/trigger`、`/api/job/instances/page`、`/api/job/logs/page`、`/api/job/workers/page`、`/api/job/handlers`、`/api/job/engines/status`。
- 权限：使用 `admin` 账号通过真实登录、租户选择和页面权限指令访问 Job 菜单与操作按钮。
- 数据：保留 `mango_job_example_manual_builtin`、`mango_job_example_cron_powerjob`、`mango_job_example_http_callback` 三条典型示例任务；E2E 临时任务创建后完成删除回收。
- 部署形态：本地单体 Mango Admin + 后端服务，前端 source 模式。

## 2. 执行环境

- 前端地址：`http://127.0.0.1:8347`
- 后端地址：`http://127.0.0.1:18657`
- 数据库或租户：`mango_dev_a1ce46`，租户 `芒果集团`
- 测试账号：`admin`
- 浏览器：Playwright Chromium Chrome channel

## 3. 功能验收记录

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| JOB-UI-001 | 任务定义 / `/api/job/definitions/page` | 任务定义列表搜索区紧凑布局和示例任务回显 | 关键字 `mango_job_example_` | 列表回显三条示例任务，分别包含手动内置、Cron PowerJob、HTTP 回调；状态分别可识别为已启用或已暂停 | `.job-toolbar-head` computed display 为 `flex`，主按钮位于标题区右半区；`.job-search` display 为 `flex` 且 `flex-wrap` 为 `wrap`；表格区域首屏可见 | Job API 无 4xx/5xx；E2E 收集到的 console error 为空 | `mango-docs/evidence/2026-06-05-mango-job-ui-e2e/job-definition.png` | PASS |
| JOB-E2E-001 | 任务定义新增/编辑/状态/触发/删除接口链路 | 任务定义完整管理流程 | `mango_job_e2e_tmp_<timestamp>` 和批次号 `e2e-job-batch-<timestamp>` | UI 新增草稿任务后列表出现对应编码；编辑后名称更新为 `E2E 临时任务已编辑`；示例手动任务可暂停后启用；触发后执行实例接口包含同一批次号和 `MANUAL` 触发类型；临时任务最终删除且列表不再出现 | 新增/编辑弹窗展示 `基本信息`、`执行配置`、`参数配置` 分组；保存、状态确认、触发、删除弹窗均完成页面反馈 | Job API 无 4xx/5xx；E2E 收集到的 console error 为空 | Playwright test attachment `job-definition-layout`；截图同 `job-definition.png` | PASS |
| JOB-E2E-002 | 执行实例、执行日志、Worker、处理器、引擎状态 | Job 下全部管理页真实 API 访问 | 菜单入口依次进入五个页面 | URL 分别落到 `/job/instance`、`/job/log`、`/job/worker`、`/job/handler`、`/job/engine`；每页 heading 与 `.job-panel` 可见；搜索型页面 toolbar 高度小于 150px | 无 401、403、拒绝访问、路由加载失败、加载失败文案；搜索型页面使用横向紧凑搜索区 | Job API 无 4xx/5xx；E2E 收集到的 console error 为空 | `mango-docs/evidence/2026-06-05-mango-job-ui-e2e/job-instance.png`、`mango-docs/evidence/2026-06-05-mango-job-ui-e2e/job-engine.png` | PASS |

## 4. 回归抽查记录

| 模块 | 页面 | 功能点 1 | 功能点 2 | UI 细节 | 截图/trace | 结论 |
|---|---|---|---|---|---|---|
| Job | 任务定义 | 搜索 `mango_job_example_` 后回显三条示例任务 | 更多筛选按钮保留，低频筛选项默认收起 | 标题、主操作、搜索区和表格顺序清晰；新增任务按钮不再掉到标题下方 | `job-definition.png` | PASS |
| Job | 执行实例/执行日志/Worker | 菜单切换触发对应分页 API | 搜索区在一行内优先铺开，窄屏可换行 | toolbar 高度受控，列表区域首屏可见 | `job-instance.png` | PASS |
| Job | 处理器/引擎状态 | 页面进入后调用真实接口 | 无权限错误或路由错误文案 | 主内容区域可见，状态信息在 `.job-panel` 内展示 | `job-engine.png` | PASS |

## 5. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 | 用户确认 |
|---|---|---|---|---|
| PowerJob Server 实际调度执行结果 | 当前 Sprint 的 E2E 验证 Mango Job 管理页、Mango 原生契约、触发实例落库和真实 Job API；未接入外部 PowerJob Server 的异步执行闭环 | 只能证明管理链路和 Mango 侧实例记录可用，不能证明外部 Worker 完成真实任务体执行 | 后续接入 PowerJob Server/Worker 联调环境后补充跨进程执行 E2E | 暂无 |
| 多浏览器回归 | 本次按验收重点执行 Chromium；未执行 Firefox/WebKit | 不影响 Chrome/Chromium 管理后台验收结论，跨浏览器细节仍需抽查 | 发布前可补跑 Firefox/WebKit 项目 | 暂无 |
