# Workflow 办理人身份契约验收证据

## 1. 验收范围

- 页面：审批管理 / 我的待办、任务详情、我的已办。
- 接口：`POST /workflow/processes/start-business`、`POST /workflow/tasks/complete-result`、`GET /workflow/business-applies/progress/latest`、`GET /workflow/tasks/todo`、`GET /workflow/tasks/{taskId}`。
- 权限：租户 `1` 内真实 `admin` 账号登录和办理；未新增用户、角色或权限，未绕过任务可见性。
- 数据：用例创建并发布两节点 Flowable 定义，数据前缀为 `ASSIGNEE-IDENTITY-E2E-*`；结束时只清理本用例业务快照、Flowable 运行/历史实例和定义，回读残留均为 0。
- 部署形态：Mango Admin 单体本地开发环境，真实后端、真实 MySQL 工作区数据库和真实 Identity 数据。

## 2. 执行环境

- 前端地址：`http://127.0.0.1:30027`
- 后端地址：`http://127.0.0.1:18027`
- 数据库或租户：`mango_dev_mango_workflow_assignee_identity_027`；租户 `1 / default / 芒果集团`
- 测试账号：`admin`（租户成员昵称 `Administrator`）
- 浏览器：Playwright Chromium，1280 x 720

## 3. 功能验收记录

| 台账 ID | 用例 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|---|
| TASK-005 | TC-001 | 我的待办、任务详情、我的已办；`start-business`、`complete-result`、`progress/latest`、`tasks/todo`、`tasks/{taskId}` | 两节点真实审批从发起、首次推进、页面终审到已办闭环 | `ASSIGNEE-IDENTITY-E2E-*`；租户 `1`；办理人 `admin` | `assigneeName=admin`；`assigneeId=2093302750320009218` 且发起、动作结果、进度、待办和详情一致；`assigneeDisplayName=Administrator`；终审后业务进度为 `APPROVED` 且 `currentTasks=[]` | 待办列表显示 `Administrator`；详情页 `data-state=ready` 且摘要办理人显示 `Administrator`；浏览器点击“通过”完成终审；已办列表可查询结果 | 0 console error；0 page error；0 request failure；0 HTTP 5xx | `历史验收图片已清理（可从 Git 历史恢复）`；清理修正后串行重复 `5 passed (43.6s)` | PASS |

## 4. 回归抽查记录

| 模块 | 页面 | 功能点 1 | 功能点 2 | UI 细节 | 截图/trace | 结论 |
|---|---|---|---|---|---|---|
| `@mango/workflow` | 我的待办、任务详情、我的已办 | 列表和详情显示租户成员昵称 | 审批后进度和已办状态一致 | 页面主内容非空，任务详情为 ready，办理人字段无溢出或遮挡 | `历史验收图片已清理（可从 Git 历史恢复）` | PASS |

## 5. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 | 用户确认 |
|---|---|---|---|---|
| Firefox、WebKit 和微前端形态 | 本次 AI 交付按 PMO 日常 UI/E2E 基线只执行 Chromium 单体入口 | 不影响本次真实审批主链路结论；跨浏览器和微前端兼容性未由本证据覆盖 | 在夜间、发布前或兼容性专项执行 | 不适用 |
| 同一用例 5 worker 并发压测 | `--repeat-each=5` 默认并发执行时，多个实例同时部署和清理同一 Flowable 引擎库，出现两次清理死锁及一次发起 500；改为 `--workers=1` 后 5/5 通过 | 标准单用例和同文件串行套件不受影响；本次不声明同库并发压测通过 | 若要将同用例并发作为正式目标，应使用隔离 schema/数据库并设计独立并发用例 | 不适用 |

## 6. 业务开发交接输出

| 输出对象 | 交接内容 | 材料路径 | 执行入口 | 数据/账号边界 | 失败/例外处理 | 状态 |
|---|---|---|---|---|---|---|
| Workflow 维护者和业务开发者 | 可复跑的真实审批身份 E2E，验证原始办理人 key、Identity 用户 ID 和租户成员显示名 | `mango-ui/apps/mango-admin/e2e/specs/workflow-management.spec.ts`；本验收证据目录 | `PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true pnpm exec playwright test e2e/specs/workflow-management.spec.ts --project=chromium --grep '办理人身份在真实审批流' --workers=1` | 使用专用 workspace 数据库、租户 `1` 和现有 `admin`；只清理 `ASSIGNEE-IDENTITY-E2E-*` 数据 | 失败时查看 Playwright artifact/trace、后端 health 和对应 Workflow API 响应；禁止在共享业务库运行 | PASS |
