# Issues #613/#614 工作流通知接收人交付记录

## 1. 元数据

- 任务 ID：ISSUES-613-614-WORKFLOW-NOTICE
- 交付模式：STANDARD
- 需求影响：L2 - 工作流异步通知可能丢失原申请人或应用上下文，导致终态通知无接收人，或者角色候选人无法解析。
- 方案风险：L2 - 调整 Workflow 事件、Notice 事件上下文和 Identity 角色成员查询，但不改变流程状态机、任务分配规则或数据库结构。
- 最终风险：L2
- 工作区决策：CREATE - `/Users/hardy/Work/mango-issues-613-614`，`fix/issues-613-614-workflow-notice`
- 保障措施：M01、M08、M09、M10、M11、M12

## 2. 目标与范围

- Issue：[HardyDou/mango#613](https://github.com/HardyDou/mango/issues/613)、[HardyDou/mango#614](https://github.com/HardyDou/mango/issues/614)。
- 根因一：流程完成、驳回或结束事件没有携带业务申请中的原申请人，终态 Notice 最终以空接收人发送。
- 根因二：Workflow 事件虽然保留了 `tenantId`，但异步 Notice 只恢复租户，没有恢复 `appCode` 和 `realm`，Identity 无法按原应用与登录域把角色解析成具体用户。
- 目标：通知始终基于对应运行时任务或原申请人解析接收人，并在本地、远程异步消费中恢复完整应用上下文。
- 处理范围：Workflow 事件与通知订阅、Notice 事件上下文、Identity 角色成员查询、定向测试、模块 README、业务接入指南和能力地图。
- 不处理范围：流程任务创建与分配算法、流程定义、数据库、前端、通知撤回，以及 `ORG_LEADER` 的负责人解析能力。

## 3. 可观察系统要求

| ID | 场景 | 预期行为 | 验收标准 |
|---|---|---|---|
| REQ-001 | 流程完成、驳回或结束 | 通知接收人为业务申请中的原申请人 | 事件携带 `applicantId/applicantName`，终态 Notice 只发送给有效 `applicantId` |
| REQ-002 | 一个共享候选任务配置用户、角色、岗位或组织 | 候选用户及目标中的全部有效成员收到指向同一任务的消息 | 只生成一个任务级 Notice 命令，保留该任务 `taskId` 并扩展全部有效接收人 |
| REQ-003 | 并行或多实例已经产生每人一个运行时任务 | 每个办理人只收到自己任务的消息 | 按每条运行时任务及其 `assigneeId` 分别发送，幂等键包含 `eventId + taskId` |
| REQ-004 | Notice 在事务后本地或远程异步消费 | 角色成员按事件产生时的租户、应用和登录域解析 | `tenantId/appCode/realm` 在调用 Notice API 前恢复，完成后恢复消费线程原上下文 |
| REQ-005 | 流程已结束或接收目标无法解析 | 不发送非法空接收人通知 | 事件被跳过，不再产生“接收用户不能为空”错误 |

## 4. 技术决定与边界

- Workflow 事件载荷增加 `appCode`、`realm`、`applicantId` 和 `applicantName`；终态事件在业务申请状态更新后读取申请快照。
- `TASK_ADVANCED` 按 `currentTasks` 逐条处理。任务已有 `assigneeId` 时只使用办理人；未到人时才解析候选用户及 `ROLE`、`POST`、`ORG` 目标。
- 共享候选任务保持一个 `taskId`，角色、岗位或组织只扩展接收人，不复制工作流任务；并行或多实例任务则保持每条运行时任务独立通知。
- Notice 事件命令保存完整应用上下文，本地和远程监听器共用上下文执行器；旧事件缺少 `appCode/realm` 时沿用消费线程已有值。
- Identity 的角色成员查询显式传递 `tenantId/appCode/realm`，不放宽授权查询范围。
- `ORG_LEADER` 不降级为整个组织，防止负责人通知被错误扩大；通知点击时仍以 Workflow 当前权限和任务状态为准。

## 5. 验收结果

| 范围 | 命令或方式 | 结果 |
|---|---|---|
| 定向回归 | Workflow 事件发布、通知订阅，Notice 本地/远程上下文，以及 Identity 角色成员查询相关测试 | PASS - 22 个相关测试通过 |
| 依赖 Reactor | `mvn -f mango/pom.xml -pl mango-platform/mango-workflow/mango-workflow-api,mango-platform/mango-workflow/mango-workflow-core,mango-platform/mango-workflow/mango-workflow-starter,mango-platform/mango-notice/mango-notice-api,mango-platform/mango-notice/mango-notice-support,mango-platform/mango-notice/mango-notice-starter,mango-platform/mango-notice/mango-notice-starter-remote,mango-platform/mango-identity/mango-identity-core -am verify` | PASS - 68 个 Reactor 模块全部成功 |
| 真实 API、数据库与异步通知链路 | `PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true pnpm --dir mango-ui/apps/mango-admin exec playwright test e2e/specs/workflow-management.spec.ts --project=chromium --grep "正式业务流程的角色下一节点通知全部成员且终态通知原申请人" --workers=1 --reporter=list`；同命令增加 `--repeat-each=2` 连续复跑 | PASS - 首轮 `1 passed (9.8s)`，连续复跑 `2 passed (21.3s)`；合入最新 `origin/main` 后再次 `1 passed (11.2s)` |
| 测试质量 | `node mango-pmo/tools/test-quality-check.mjs --base origin/main` | PASS - 6 个测试文件通过质量检查 |
| 能力说明 | `node mango-pmo/tools/audit-module-readmes.mjs`；`node mango-pmo/tools/audit-readme-source-facts.mjs` | PASS |
| 差异格式 | `git diff --check` | PASS |

### 5.1 真实链路证据

- 环境：后端 `http://127.0.0.1:18019`，健康状态 `UP`；数据库 `mango_dev_mango_issues_613_614_019`；租户 `1/default`；应用 `internal-admin`；登录域 `INTERNAL`。
- 正式申请：业务键 `WORKFLOW-NOTICE-E2E-1784629022649`，流程实例 `53ae8e30-84ed-11f1-b753-76e88ab457e9`；通过 `/workflow/processes/start-business` 由 `E2E_NOTICE_APPLICANT_1784629022649`（用户 ID `2079510963923279873`）发起。
- 角色任务：首节点由 `admin` 完成后进入 `ROLE_ADMIN` 角色复核，`admin`（用户 ID `2079504287740551169`）、`E2E_NOTICE_ROLE_ONE_1784629022649`（用户 ID `2079510966087540738`）和 `E2E_NOTICE_ROLE_TWO_1784629022649`（用户 ID `2079510968264384513`）查询到同一共享任务 `544a3209-84ed-11f1-b753-76e88ab457e9`。
- 角色通知：上述三个角色成员分别查询到一条 `workflow.task.assigned` 站内消息；消息均指向 `workflow:task:detail`，且 `target.params.taskId` 均为共享任务 ID。申请人查询不到该角色任务消息。
- 终态通知：共享任务完成后，只有原申请人查询到 `workflow.process.completed`，消息指向 `workflow:task:done` 并携带原流程实例 ID；管理员和两名角色成员均查询不到该终态消息。
- 日志检查：从用例启动前的后端日志偏移量扫描本次执行切片，未发现“接收用户不能为空”、Notice 事件发送失败、接收人解析失败或 `ERROR`。
- 数据清理：用例结束后按唯一业务键删除对应 Notice、Workflow 申请/表单记录、流程定义、分类和三个临时用户；未保留密码或访问令牌。
- M13 状态：未启用。本次声明的是公开 API、真实数据库和异步 Notice 链路行为，没有修改或声明浏览器可见 UI 行为。

## 6. 剩余风险与发布边界

- 本次没有增加通知撤回或失效机制；用户点击消息时，Workflow 的实时任务状态和权限校验仍是最终依据。
- `ORG_LEADER` 仍需专用负责人解析能力或上游提供明确候选用户，本次不将其扩大为组织全员。
- 本任务只完成源码、测试和能力说明，不执行版本发布、部署、提交、Push 或 PR。
