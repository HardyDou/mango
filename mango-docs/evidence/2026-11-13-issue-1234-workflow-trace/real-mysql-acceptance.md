# Issue #1234 真实 MySQL + 真实审批链路验收证据

## 环境

| 项 | 值 |
|---|---|
| 后端 | `mango dev start backend`（`http://127.0.0.1:18047`，`/actuator/health` = `UP`） |
| 数据库 | MySQL 8.4.8，`mango_dev_mango_issue_1234_047`（worktree slot 47，Flyway + bootstrap 冷启动） |
| 登录 | `admin` / 租户 `1`（`default`）/ 会员 1001 / 角色 `ROLE_ADMIN`（`2121074754737487874`） |
| 流程定义 | `TRACE_ACCEPT_1234`（本次通过 `POST /workflow/definitions` + `deploy` 创建，节点 `first_review` → `second_review`，候选组 `ROLE:2121074754737487874`） |
| 业务申请 | `businessType=TRACE_ACCEPT`，`businessKey=TRACE-KEY-001` |

## 真实链路操作（全部走 HTTP + 真实 Flowable 引擎）

| 步骤 | 请求 | 结果 |
|---|---|---|
| 1 | `POST /workflow/business-applies` | 申请 `2121075642990428161` |
| 2 | `POST /workflow/processes/start` | 实例 `5d799079-df0e-11f1-b82b-7a5ba45963f4`，当前节点 `first_review` |
| 3 | `POST /workflow/tasks/claim` + `complete`（初审，意见「初审通过」） | 成功，进入 `second_review` |
| 4 | `POST /workflow/tasks/claim` + `return`（复审 → `targetTaskDefinitionKey=first_review`，意见「复审退回修改」） | `actionResult=RETURN`，回到 `first_review` |
| 5 | `POST /workflow/tasks/claim` + `complete`（初审，意见「初审再次通过」） | 进入 `second_review` |
| 6 | `POST /workflow/tasks/claim` + `complete`（复审，意见「复审通过」） | 流程结束 |
| 7 | `POST /workflow/tasks/trace-by-business-key` | 见下 |

## 结果 1：单实例完整流转记录（9 条，含多轮退回）

| 动作 | 处理人 | 节点 | 审批意见 | variables |
|---|---|---|---|---|
| START 发起 | admin | — | — | null |
| CLAIM 认领 | admin | 初审 | 认领任务 | null |
| COMPLETE 通过 | admin | 初审 | 初审通过 | null |
| CLAIM 认领 | admin | 复审 | 认领任务 | null |
| RETURN 退回 | admin | 复审 | 复审退回修改 | null |
| CLAIM 认领 | admin | 初审 | 认领任务 | null |
| COMPLETE 通过 | admin | 初审 | 初审再次通过 | null |
| CLAIM 认领 | admin | 复审 | 认领任务 | null |
| COMPLETE 通过 | admin | 复审 | 复审通过 | null |

排序为 `createdTime` 升序，同一时间按 `id` 升序；多轮退回顺序稳定。证据：`real-mysql-trace-single-instance.json`。

## 结果 2：过滤与边界

| 场景 | 请求 | 结果 |
|---|---|---|
| `actions=["START"]` | — | 只返回 1 条 `START` |
| `actions=["RETURN"]` | — | 只返回 1 条，`comment=复审退回修改` |
| `operatorIds=[1]` | 处理人本人 | 返回 9 条 |
| `operatorIds=[2]`（无此人） | — | 返回 0 条 |
| `deptIds=[1]`（申请人部门命中） | — | 返回 9 条 |
| `deptIds=[999]`（未命中） | — | 返回 0 条（整条实例被排除） |
| `businessKey` 未知 | — | `code=200`，空集合 |
| `businessKey` 为空 | — | `code=400`，`业务主键不能为空`（Bean Validation） |
| `actions=["RETRUN"]`（拼写错误） | — | `code=3659`，`未知审批动作：RETRUN` |

## 结果 3：重新申请（多流程实例）

通过 `reapplyFromApplyId` 为同一 `businessKey` 创建第二个申请并启动实例 `ade862a8-df0e-11f1-b82b-7a5ba45963f4` 后再次调用：

- 记录数 10（9 + 新实例 `START`）；
- 返回 2 个不同 `processInstanceId`，2 条 `START`；
- 无 `latestOnly`，历史实例全部返回。

证据：`real-mysql-trace-multi-instance.json`。

## 结果 4：真实 SQL（租户插件 + 字段投影 + 上限探测）

`real-mysql-trace-sql.txt` 记录了两条真实语句：

- 新接口：`SELECT id, process_instance_id, task_id, task_name, task_definition_key, action, action_name, operator_id, operator_name, comment, created_time FROM workflow_task_record WHERE (process_instance_id IN (?, ?)) AND tenant_id = 1 ORDER BY created_time ASC, id ASC LIMIT 2001`
  - 字段投影未包含 `variables_json`；
  - `tenant_id = 1` 由 MyBatis-Plus 租户插件注入；
  - `LIMIT 2001` 是上限探测（`unpaged-max-rows + 1`）。
- `process-detail`：仍为全字段查询，`variables_json` 在列表内，行为未变。

## 结果 5：既有入口未被破坏

- `GET /workflow/tasks/process-detail` 返回 9 条记录，动作序列与流转记录一致；审批页数据源（`processDetail.records`）未受影响。
- `application/apply` 访问校验路径未改动：本流程发起人即当前用户，未触发 `3645`；新接口完全不做申请级判定。

## 未验证项

- **前端面板**：本仓没有消费 `traceByBusinessKey` 的页面；业务面板在 `mango-baohan-risk`。平台审批页 `mango-ui/packages/workflow/src/views/task-detail` 使用 `processDetail`，已在结果 5 验证其数据源未变。当前环境没有浏览器工具，未做 UI 截图或点击验收。
- **超限失败**：真实 MySQL 未把 `unpaged-max-rows` 调到极小值（需改配置重启）；该行为由 `WorkflowTaskTraceIntegrationTest#failsInsteadOfTruncatingWhenRecordCountExceedsLimit` 覆盖。
- **跨租户**：本实例只有租户 1 数据；跨租户由 `WorkflowTaskTraceIntegrationTest#hidesRecordsOfOtherTenant` 与真实 SQL 的 `tenant_id = 1` 注入共同证明，未在真实库造第二租户数据。

## 结果 6：《风险审批报告.docx》字段覆盖核对

核对对象：`/Users/hardy/Desktop/风险审批报告.docx`（业务侧风险审批报告导出件）。报告结构为「项目信息 / 尽调信息 / 风控意见」，其中风控意见按 `终审 → 复审 → 初审` 输出，每个节点含：是否承保、收费标准、风险加收、补充、审批人、审批时间；说明栏写明「生效顺序：终审、复审、初审，没有时不显示对应一行」。

| 报告字段 | 数据来源 | 新接口是否提供 | 依据 |
|---|---|---|---|
| 审批人（初审/复审/终审） | `COMPLETE` 记录的 `operatorName` | 提供 | 真实库 `mango_dev_baohan_risk` 中 `COMPLETE` 行有 `operator_name` |
| 审批时间 | 同记录的 `createdTime` | 提供 | 同上 |
| 补充 | 同记录的 `comment` | 提供 | 真实数据为「E2E 初审通过」「退回补充」等业务文案 |
| 节点归属（初审/复审/终审、是否缺行） | `taskName` / `taskDefinitionKey` | 提供 | 真实节点键 `risk_specialist_review` / `risk_supervisor_review` / `risk_manager_review` |
| 是否承保、收费标准、风险加收 | 业务自有数据 | **不提供，也不需要** | 真实库 `workflow_task_record.variables_json` 对 `COMPLETE` 行均为 `{}`，审批只提交意见；平台从未保存这些字段 |
| 生效顺序 终审→复审→初审 | — | 不提供，由业务排序 | 接口按 `createdTime` 升序返回（初审→复审→终审），报告需要的顺序相反 |

消费规则（业务侧必须实现，否则报告会错）：

1. **每个节点取最后一次 `COMPLETE`**：退回后同一节点会再次出现 `COMPLETE`；真实库存在同一实例 `RETURN` 后同节点二次办理的数据。按 `taskDefinitionKey` 取最新一条，不能取第一条。
2. **缺失节点不显示**：报告「没有时不显示对应一行」等于该节点键没有任何 `COMPLETE`。
3. **按节点等级排序，不按时间排序**：`终审 → 复审 → 初审` 与接口时间顺序相反。
4. **退回记录不进入报告表头**：`RETURN` 只用于判断"是否发生过退回"，报告只消费 `COMPLETE`；`CLAIM` 的 `comment` 是系统文案「认领任务」，不能当审批意见。

结论：报告需要的审批人与审批时间可由本接口提供；三个风控结论字段属于业务数据，平台记录表从未保存，与「不返回 `variables`」的决定一致，不构成缺口。
