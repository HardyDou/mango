# 业务审批接入

## 1. 适用场景

业务单据需要发起审批，审批结束后回写业务状态，并能在业务页面查看流程进度。

> Issue #983 影响说明：本次文件上传传输能力调整不改变 Workflow API、审批数据、权限或租户规则；工作流中的现有附件调用无需修改。

## 2. 阅读顺序

| 顺序 | 文档 | 关注点 |
|------|------|--------|
| 1 | [Workflow 后端 README](../../../mango/mango-platform/mango-workflow/README.md) | 流程定义、实例、任务、事件和配置 |
| 2 | [@mango/workflow README](../../../mango-ui/packages/workflow/README.md) | 流程页面、设计器、任务 API |
| 3 | [Workflow Example README](../../../mango-ui/packages/workflow-business-example/README.md) | 业务接入示例和页面 key |
| 4 | [能力地图：业务审批闭环](../../capabilities/README.md#3-组合接入入口) | 组合验证入口 |

办理人身份特性升级请先阅读[Workflow 办理人身份特性升级指南](../operations/workflow-assignee-identity-upgrade.md)。该指南固定版本升级、接口权限、DTO/事件透传、前端去重查询和历史数据边界。

## 3. 接入检查点

| 环节 | 检查点 |
|------|--------|
| 业务状态 | 业务单据状态区分草稿、审批中、通过、驳回、撤回等业务语义 |
| 流程定义 | 业务类型、流程 key、表单编码和版本关系清晰 |
| 发起审批 | 业务保存和流程发起的事务边界可解释，失败时能回滚或补偿；业务后端通过 `WorkflowProcessApi.startBusinessWorkflow()` 或 `WorkflowBusinessApplyApi` + `WorkflowProcessApi` 组合入口接入，不直接调用 workflow core service |
| 审批回调 | 监听流程完成、驳回、撤回等事件并回写业务状态；事件类型和 payload 使用 `mango-workflow-api` 的 `WorkflowEventTypes`、`WorkflowEventPayloadVO` |
| 页面入口 | 业务详情页展示流程进度、当前任务和审批记录 |
| 办理人身份 | 使用返回的 `assigneeName` 作为原始 Flowable key；`assigneeId`、`assigneeDisplayName` 仅作为当前租户身份增强，候选组未认领时保持为空 |
| 返回入口 | 业务跳转审批任务详情时传 `returnPath`，审批完成或点返回能回到业务列表，不回退到 Mango 默认待办 |
| 权限 | 发起、审批、撤回、查看记录按业务角色和流程任务共同判断 |
| 历史只读 | 业务详情需要历史参与可见性时，消费 `WorkflowParticipationApi.access()`；该事实只表示可读，不代表可办理当前任务 |
| 自动派单 | 需要节点到达即明确办理人时显式配置 `assignmentMode=AUTO`；可选 `autoAssignmentStrategy=ROUND_ROBIN`、`LEAST_TASKS` 或 `AFFINITY`，缺失时兼容轮询，候选为空会使流程事务失败 |
| 设计器候选项 | 流程定义页面只调用 Workflow 的 `designer-options` 接口；业务承载应用需要自定义目录时注册 `WorkflowDesignerOptionProvider`，不要给菜单追加跨域平台权限 |
| 模板推送机构 | 流程模板页面只在打开推送弹窗后调用 Workflow 的 `tenant-options`；自定义机构目录注册 `WorkflowTemplateTenantOptionProvider`，不要追加 `system:tenant:list` |

## 4. 最小闭环

1. 新建业务单据并保存为草稿。
2. 发起审批后业务状态变为审批中。
3. 审批人能在任务列表看到待办。
4. 申请人可以在业务允许时撤回本人运行中的申请，业务状态同步变为已撤回。
5. 审批通过后业务状态变为通过。
6. 业务详情页能看到流程实例和审批记录。

## 4.1 历史参与关系

业务后端可以在 `StartWorkflowProcessCommand`、`StartBusinessWorkflowCommand` 中传入完整 `participantUserIds`，或通过 `WorkflowParticipationApi.replaceBusinessParticipants()` 原子替换业务声明参与人。用户标识使用稳定 `userId`；租户一律来自运行时登录上下文。Workflow 会验证账号、租户成员状态和离职状态，任一用户无效时整次声明零写入。

详情授权适配器调用 `WorkflowParticipationApi.access(processKey, businessKey)`，列表可调用 `my` 分页。`INITIATOR`、`CURRENT_ASSIGNEE`、`COMPLETED_HANDLER`、`BUSINESS_PARTICIPANT` 都是只读参与事实；审批、认领、释放、驳回、退回和转办仍使用当前 Flowable assignee/candidate 权限，业务页面不能把 `readable=true` 当作可操作依据。

V3 升级只回填具有稳定 `operator_id` 或 `assignee_id` 的历史记录。只有 username 的旧记录不会猜测为用户授权；这类历史可见性由业务确认后通过声明 API 补齐。

## 5. 常见失败

| 现象 | 优先检查 |
|------|----------|
| 发起审批后没有待办 | 流程定义版本、节点办理人表达式、当前租户和组织数据 |
| 业务状态不更新 | 事件监听、回写服务、业务 ID 与流程 businessKey 映射 |
| 审批通过后业务侧仍显示上一节点 | 是否误用 `workflow.task.completed` 同步当前任务；当前任务刷新应使用 `workflow.task.advanced` 或 `complete-result` |
| 审批页打开空白 | 前端 workflow 包是否引入，页面 key 是否注册，接口是否 401/403 |
| 驳回后业务不可再次提交 | 业务状态流转是否覆盖驳回到草稿或重新提交 |
| 退回后业务侧仍显示原审批节点 | 业务侧是否使用 `POST /workflow/tasks/return` 响应或 `workflow.task.advanced` 同步刷新后的 `currentTasks` |
| 撤回返回无权限或状态校验失败 | 当前登录人是否为原申请人、租户是否一致、申请是否仍为 `IN_APPROVAL`、业务菜单是否声明 `workflow:process:withdraw` |
| 多租户流程串数据 | 流程定义、实例、任务和业务表 tenantId 是否一致 |
| 空库 `bootstrap apply` 在 migration 前查询 `ACT_GE_PROPERTY` | 调用链是否由业务 Bean 注入 `WorkflowTaskRuntimeApi` 等公开接口后提前创建 Controller；升级到包含 Bootstrap API 延迟代理的 Maven 版本，不要手工建 Flowable 表或恢复业务 `forceSync()` 兼容 |
| AUTO 节点返回 `AUTO_ASSIGN_NO_CANDIDATE` | 检查指定用户、角色、岗位、组织或组织主管是否能展开为当前租户启用且未离职的用户；该错误不会转 admin 或退化为待领取 |
| 流程设计器候选项 403 | 确认角色有 `workflow:definition:query` 且前端只调用 `/workflow/definitions/designer-options`；不要追加 `system:*`、`authorization:*`、Identity 或 Org 权限 |
| 流程设计器提示 Provider 缺失或加载失败 | 承载 Workflow 的应用应提供默认平台公共 API Bean，或注册自定义 `WorkflowDesignerOptionProvider`；Provider 的可信上下文与失败处理遵循 [安全规范](../../../mango-pmo/rules/backend/06-security.md) |
| 流程模板页面打开即出现机构列表 403 | 当前前端只在打开推送弹窗后调用 `/workflow/templates/tenant-options`；页面挂载时加载机构或调用 `/system/tenant/list` 表示仍在使用旧调用路径，同时确认当前角色拥有 `workflow:template:push` |

## 6. 事件接入

业务模块可以通过 workflow 事件异步回写业务状态，也可以在审批页调用任务接口同步拿到刷新结果。选择方式如下：

| 业务目标 | 推荐方式 |
|----------|----------|
| 审批按钮点击后立即刷新当前节点、当前办理人和页面按钮状态 | 调用 `POST /workflow/tasks/complete-result` 或 `WorkflowTaskRuntimeApi.completeWithResult()` |
| 审批退回后立即刷新当前节点、当前办理人和页面按钮状态 | 调用 `POST /workflow/tasks/return` |
| 保存审批草稿、认领、取消认领后立即刷新按钮状态 | 调用 `save-result`、`claim-result`、`unclaim-result` 或对应 `WorkflowTaskRuntimeApi` result 方法 |
| 审批中同步下一节点办理人、业务列表当前节点、待办摘要 | 订阅 `workflow.task.advanced` |
| 保存草稿、认领、取消认领后异步刷新业务侧状态 | 订阅 `workflow.task.saved`、`workflow.task.claimed`、`workflow.task.unclaimed` |
| 审计刚完成的任务和办理意见 | 订阅 `workflow.task.completed` |
| 流程通过后回写业务通过状态 | 订阅 `workflow.process.completed` |
| 流程驳回后回写业务驳回状态 | 订阅 `workflow.process.rejected` |
| 申请人撤回后回写业务撤回状态 | 调用 `WorkflowProcessApi.withdraw()` 后同步处理结果，并订阅 `workflow.process.withdrawn` 做异步幂等回写 |

`workflow.task.completed` 和 `workflow.task.advanced` 的差异：

| 事件 | 当前任务表是否已刷新 | 适合用途 |
|------|----------------------|----------|
| `workflow.task.completed` | 否 | 记录当前任务完成动作。 |
| `workflow.task.advanced` | 是 | 同步下一节点或退回目标节点、当前办理人和业务进度。 |

### 6.1 默认审批通知

Mango 的默认审批通知只消费 `workflow.task.advanced`、`workflow.process.completed` 和
`workflow.process.rejected`：首次提交及后续节点到达时通知实际办理人，最终通过或驳回时只通知原申请人一次。
默认文案使用 `processName` 和 `applyTitle` 展示流程名称与业务标题；`businessType`、`definitionKey`、
`taskDefinitionKey`、`businessKey` 仍可用于内部关联和路由，但不会回退为默认用户文案。业务应用发起审批时应提供
可读的申请标题，无需为每个业务类型复制默认模板或事件订阅器。

默认模板启用系统消息和企业微信，关闭邮件和短信。没有可用外部通道或用户没有匹配当前企业微信 CorpID 的有效绑定时，
对应发送记录收敛为取消且不重试；渠道配置解析失败、Identity 查询异常等真实运行故障仍按发送失败处理。业务状态回写仍按
本节事件接入方式独立处理，默认通知不替代业务状态机，也不改变流程 API、权限或租户边界。

终态消息（通过、驳回、结束）的「查看申请」按钮目标在业务没有提供 `customConfig.viewPath` 时是工作流的办理人视角页面（`workflow:task:done`、`workflow:task:initiated`）。收件人没有审批中心菜单授权时，平台会下发 `fallbackTargetKey=notice:site-message`，前端退到消息中心，因此按钮始终可用。要让按钮直达业务申请详情，业务侧在申请快照的 `customConfig` 中配置三个可选字段：

| 字段 | 作用 | 约束 |
|------|------|------|
| `viewPath` | 站内绝对路径，例如 `/guarantee/apply/detail` | 以 `/` 开头，不含 `?`、`#`、反斜杠和控制字符；外部 URL 被丢弃。 |
| `viewQuery` | 点开页面时要带上的查询参数 | 只保留单层标量值；声明后站内信目标只携带这些参数。用于“点开列表并定位到目标行”这类场景。 |
| `viewActionLabel` | 按钮文案，例如「去确认」 | 最长 32 个字符；缺省时使用默认文案。 |

只有终态通知读取这三个字段；待办到达通知始终指向 `workflow:task:detail`。业务如果不需要 Mango 默认通知，把它关掉再自己发：设置 `mango.workflow.notice.enabled=false`，然后订阅 `workflow.task.advanced`、`workflow.process.completed`、`workflow.process.rejected` 公开事件，用业务自己的 `bizType` 和模板调用 `NoticeApi`。不要通过删除 Spring Bean 定义停用默认通知，那是升级就会失效的做法。

`POST /workflow/tasks/return` 会把当前任务退回到最近一个已完成的不同用户任务节点，或退回到 `targetTaskDefinitionKey` 指定的历史节点。串行流程可以不传目标节点；并行、多实例、重复审批节点或业务语义固定的流程，应在流程节点动作配置或业务审批页中显式传入 `targetTaskDefinitionKey`。接口返回结构与 `complete-result` 一致，业务侧应使用返回的 `currentTasks` 或订阅 `workflow.task.advanced` 刷新业务单据当前节点和当前办理人；退回不会发布 `workflow.task.completed`，也不会把流程状态改为驳回。

退回的使用约束：

- 退回由流程引擎的变更活动状态能力完成，源节点与目标节点之间**不需要**回退连线（`sequenceFlow`），也不需要目标节点声明退回动作。
- 只有**当前任务节点**需要启用 `returnTask` 动作；动作配置中的 `targetTaskDefinitionKey` 是默认退回目标。
- `targetTaskDefinitionKey` 指向**当前流程实例中已完成的用户任务节点**；退回不需要历史活动 ID。
- 把网关或汇聚节点当作退回目标时（设计器里上一个节点常常就是网关），平台按流程定义向上游解析到最近的已完成用户任务：唯一时直接退回该节点，多个时报错并列出候选，分支网关上游没有用户任务时报错。不要依赖“退回网关”本身，汇聚网关会把执行直接带回原节点。
- 目标节点、失败语义和返回结构的完整定义见[Workflow 后端 README](../../../mango/mango-platform/mango-workflow/README.md#3-后端接入)。
- 退回是失败可见的操作：引擎变更活动状态后，若目标节点没有产生待办任务，或当前任务仍停在原节点，接口以 `TASK_INVALID`（`流程任务参数非法`）失败并回滚整笔事务，不会返回成功。业务侧按错误提示处理，不要先写“已退回”。
- 成功响应中的 `actionResult` 是 `RETURN`，不是 `COMPLETE`；业务侧可用于区分通过和退回，但不要用 `applyStatus` 单独判断退回，退回不会改申请状态。
- 需要退回后直接回到指定处理人时，传 `targetAssigneeUserId`（`identity_user.id` 或 `username`）。退回人不需要是目标节点候选人，也不需要目标节点启用 `transfer`；目标用户需为当前租户启用成员，且本次退回只能在目标节点新建一条任务，否则退回整体失败并回滚。

`POST /workflow/processes/withdraw` 与 `WorkflowProcessApi.withdraw()` 支持使用 `applyId` 或 `processInstanceId` 定位申请，`reason` 必填。后端同时校验 `workflow:process:withdraw` 权限、租户上下文和原申请人身份；仅运行中的 `IN_APPROVAL` 可首次撤回，已撤回请求按幂等成功返回，其它终态不会被改写。成功响应包含撤回前后状态、`withdrawn`、`idempotent`、`ended` 和原因，并发布 `workflow.process.withdrawn` 后再发布 `workflow.process.ended`。业务模块仍需先判断业务单据是否允许撤回，并用事件 ID 或业务主键幂等维护自身状态机、快照和通知；Workflow 不替代业务状态机。当前改动不提供新的前端撤回按钮，业务页面应按自身权限和状态决定是否展示操作入口。

办理人字段约定：`assigneeName`/兼容字段 `assignee` 保留 Flowable 原始 key；`assigneeId` 和 `assigneeDisplayName` 由 Workflow 在当前租户内批量解析，解析失败开放为空。候选组 key 不代表具体用户；身份服务暂不可用时审批结果仍会返回，页面回退原始 key。

单体多实例、微服务或微服务多实例部署时，事件应按至少一次投递处理。业务订阅方使用 `eventId`、`processInstanceId + completedTaskId` 或业务主键构造幂等键，避免重复回写状态、重复发通知或重复生成待办摘要。

业务订阅事件时，依赖边界应停留在 `mango-workflow-api`：事件类型使用 `WorkflowEventTypes`，`event.payload` 使用 `WorkflowEventPayloadVO` 反序列化。不要在业务模块中引用 `io.mango.workflow.core.event.WorkflowDomainEvents`、`WorkflowEventPublisher` 或 `io.mango.workflow.core.service.*`。业务列表需要展示当前节点、当前办理人、认领状态或候选人时，使用 `WorkflowBusinessApplyApi.latestProgress()`、批量进度 API 或任务动作 result 返回值，不要直接查询 workflow 运行表。

## 7. 运行时配置

部署配置示例：

```yaml
mango:
  event:
    outbox:
      enabled: true
    transport: redis-stream
```



## 7.1 审批列表读取全部任务（Issue #1208）

业务列表需要"一行一个任务"（初审/复审/终审各占一行）时，用 `POST /workflow/tasks/page-by-business-key` 一次取回该批业务主键下所有申请的全部任务，不需要业务侧建任务副本表。

请求要点：

- `businessType`：按 `workflow_business_apply.business_type` 精确匹配。取业务适配器实际投放的值，例如 `BAOHAN_RC_PROJECT_REVIEW`；平台不校验命名风格，也不做别名映射。
- `businessKeys`：必填、数量不设上限。业务表先分页，再把当前页的业务主键批量传入即可。
- `scope`：`SELF`（默认，办理人是当前用户或候选用户/候选组命中）、`ALL`（服务端校验 `workflow:task:all` 权限码，未安装授权中心时拒绝）、`ORG`（本版直接报错）。数据范围由调用方显式声明，平台不读取角色数据范围配置。
- 不传 `page` / `size` 时按 `mango.workflow.task-query.unpaged-max-rows`（默认 2000）一次性返回；`total` 始终是过滤后的真实总数，用 `list.size() < total` 判断是否需要改用分页拉取。

引擎列名事实：历史任务表的创建时间是 `ACT_HI_TASKINST.START_TIME_`，`CREATE_TIME_` 只存在于运行态 `ACT_RU_TASK`。

字段口径：

- `status`：`TODO` 在途、`DONE` 已办理、`CANCELED` 已终止（`deleteReason` 有值）。
- `claimStatus`：`UNCLAIMED` 待领取、`ASSIGNED` 待处理；转办后返回 `ASSIGNED`，与"待处理"一致。
- `returned`：该申请是否发生过退回。退回不改变 `apply_status`，因此"退回待修改"用 `applyStatus=IN_APPROVAL` 加 `returned=true` 表达，不要期望出现 `RETURNED` 状态值。
- `lastReturnAt` / `lastReturnTarget`：最近一次退回的时间和目标节点，用于判断"当前是否处于退回待修改"。
- `cancelCause`：已终止任务的机器可读原因，`RETURNED` / `WITHDRAWN` / `TERMINATED` / `OTHER`。业务侧按枚举出中文文案，不要匹配 `deleteReason` 文本——它是引擎内部文案（例如 `Change activity to risk_specialist_review`、`申请人撤回`），不稳定。
- `statuses=[DONE]` 只返回真正办结的任务，被退回或撤回终止的任务不会混入"已办理"。
- `assigneeIds` 直接传业务 `user_id` 即可，平台按 `identity_user.username` 映射到引擎办理人。
- 退回后同一 `taskDefinitionKey` 会再次出现（初审 → 复审 → 退回 → 初审），这是正常返回，按 `endTime` / `claimTime` 取最新。
- 排序：`createTime`、`endTime`、`claimTime`；`endTime` 与 `claimTime` 为空的任务在升序和降序下都排在最后。

权限分工：`workflow:task:list` 控制能否调用接口；`workflow:task:all` 控制能否使用 `ALL` 范围。业务侧按自己的角色矩阵把后一个权限码只授予主管/经理等需要全量视图的角色。

## 8. 验证命令

后端验证使用 Java 21。legacy migration fixture 和 API surface fingerprint 是测试资产；本次校准仅跟随既有 V1/V2/V3 迁移与 API 声明版本，未修改生产历史 SQL、公开 API、配置、菜单、权限、租户、页面、启动或审批运行时行为。业务审批接入步骤保持不变。

```bash
mvn -f mango/pom.xml -pl mango-platform/mango-workflow -am test
# 按业务主键查询任务的 H2 集成测试与范围规则单测
mvn -f mango/pom.xml -pl mango-platform/mango-workflow/mango-workflow-core \
  -Dtest='WorkflowTaskHistoryMapperIntegrationTest,WorkflowTaskBusinessQueryServiceTest' test
pnpm -F @mango/workflow build
pnpm -F @mango/workflow-business-example build
```

模块验证入口：

- [Workflow 验证方式](../../../mango/mango-platform/mango-workflow/README.md)
- [Workflow Frontend 验证方式](../../../mango-ui/packages/workflow/README.md)
- [Workflow Example 验证方式](../../../mango-ui/packages/workflow-business-example/README.md)


## 9. 关联规则

- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
- [AI 交付质量规则](../../../mango-pmo/rules/05-ai-delivery-quality.md)

## 10. 历史变更

详细版本影响见[业务审批历史变更索引](../../changelog/business-integration/workflow-business-approval.md)。

> Issue #1208 影响说明：新增 `POST /workflow/tasks/page-by-business-key`，业务列表可读取业务主键下的全部任务（在途、已办理、已终止）。不修改数据库结构，不改变 `todo` / `done` / `copied` 语义；新增配置项 `mango.workflow.task-query.unpaged-max-rows`。

> Issue #1098 影响说明：资源声明的 `sync-mode` 收敛为 `SYNC_ALWAYS` / `SYNC_ONCE`（旧值 `AUTO`、`INIT_ONLY`、`MANUAL`、`LOCKED` 仍可读取并映射）。本场景涉及的数据初始化、租户边界、权限判定与页面行为不变。
