# Issue #732 工作流历史参与与自动派单验收证据

## 1. 验收范围

- 页面：工作流定义节点配置（CLAIM/AUTO）；本次 P0 以 Admin API 入口验收运行时结果。
- 接口：参与关系 access/my/declare、流程发起、任务查询/完成。
- 权限：登录、流程启动、任务操作、参与声明临时绑定后恢复；历史参与不授予任务操作权。
- 数据：隔离 MySQL 库 `mango_dev_mango_issue_732_v2_009`，Flowable ACT/业务/参与投影表。
- 部署形态：Mango monolith 本地运行时。

## 2. 执行环境

- 前端地址：`http://127.0.0.1:30009`
- 后端地址：`http://127.0.0.1:18009`
- 数据库或租户：MySQL `mango_dev_mango_issue_732_v2_009`，平台测试租户
- 测试账号：Admin 与本次用例创建的唯一 `E2E_732_<timestamp>` 用户；用户和授权 finally 清理
- 浏览器：Playwright Chromium、Firefox、WebKit（`--workers=1` 串行运行，API request 入口；3/3 通过，未触发失败重试故无新 trace）

## 3. 功能验收记录

| 台账 ID | 用例 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|---|
| TASK-008 | TC-001 | `/workflow/participations/*`, `/workflow/processes/start`, `/workflow/tasks/*` | 历史参与只读、租户/用户隔离、声明、自动派单、完成、空候选回滚 | 两个有效候选用户、一个非参与用户、两个业务键、一个无效候选定义 | access/my 返回预期参与类型；参与人 complete 返回 3651；跨租户/非参与 readable=false；首尾 userId 严格轮询；完成后无 `CURRENT_ASSIGNEE`；空候选返回 3654 且四类流程/投影记录为 0 | 本用例为 API/运行时验收；工作流节点配置的 CLAIM/AUTO 控件由 `WorkflowNodeApprovalConfig.spec.ts` 覆盖，未将接口通过冒充页面视觉通过 | Playwright Chromium、Firefox、WebKit 各 1 次通过（串行 `--workers=1`）；请求断言包含状态码和业务响应；finally 清理未产生未处理 4xx/5xx | `.runtime/playwright/mango-admin/report/index.html`（HTML 报告；本次 3/3 通过，未生成 trace） | PASS |

## 4. 回归抽查记录

| 模块 | 页面 | 功能点 1 | 功能点 2 | UI 细节 | 截图/trace | 结论 |
|---|---|---|---|---|---|---|
| `@mango/workflow` | `workflow/definition/index` | 节点缺失 `assignmentMode` 默认 CLAIM | AUTO 显示固定 ROUND_ROBIN 并保留静态候选校验 | 控件测试使用 `data-testid="workflow-assignment-mode"`，无新增页面布局 | Vitest 报告：2 tests passed | PASS |

## 5. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 | 用户确认 |
|---|---|---|---|---|
| 12 并发 MySQL Testcontainers 专测 | 当前本地验收窗口未启动额外容器套件 | 行锁并发吞吐未量化；单次真实 E2E 与 Mapper/代码契约已覆盖轮询语义 | CI/发布前执行 TC-004 并记录序列摘要 | 未要求本次阻断 |
| 直接 vue-tsc 全包 | workspace 既有 alias、测试类型声明和历史类型债务 | 类型门禁不能作为本次独立通过证据 | 继续由前端 workspace 基线治理；本次包 build 和 Vitest 已通过 | 未要求本次扩展 |

## 6. 业务开发交接输出

| 输出对象 | 交接内容 | 材料路径 | 执行入口 | 数据/账号边界 | 失败/例外处理 | 状态 |
|---|---|---|---|---|---|---|
| 业务开发者 | 参与关系 access/my/declare API、稳定 userId 自动派单、权限和租户边界、空候选错误码 | `mango/mango-platform/mango-workflow/README.md`; `mango-docs/guides/business-integration/workflow-business-approval.md`; `mango-ui/packages/workflow/README.md` | 使用公开 Workflow API；业务只传 process/business 坐标和 userId 集合 | tenant/user 只取可信上下文；不得读 Flowable 或参与投影内部表；测试用户为 E2E 唯一前缀并已清理 | 3651/3653/3654 按公开错误语义处理；空候选事务不落库 | DONE |
