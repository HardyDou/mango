# 业务审批接入

## 1. 适用场景

业务单据需要发起审批，审批结束后回写业务状态，并能在业务页面查看流程进度。

> Issue #983 影响说明：本次文件上传传输能力调整不改变 Workflow API、审批数据、权限或租户规则；工作流中的现有附件调用无需修改。

> Issue #977 影响说明：业务申请 `/page` 与 `/detail` 试点接入数据权限 Grant（默认 `optional`），未回传 `X-Mango-Data-Grant` 时行为与之前一致。

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
| 自动派单 | 需要节点到达即明确办理人时显式配置 `assignmentMode=AUTO`；可选 `autoAssignmentStrategy=ROUND_ROBIN`、`LEAST_TASKS`、`AFFINITY` 或 `NODE_AFFINITY`，缺失时兼容轮询。候选为空时默认使流程事务失败；`NODE_AFFINITY` + `nodeAffinityFallback=CLAIM` 例外，候选为空仍保留角色候选组并进入待领取。`NODE_AFFINITY` 无历史办理人时按 `nodeAffinityFallback` 回退，默认 `CLAIM` 待领取，也可配置轮询、随机或任务量最少 |
| 跨提交复用办理人 | 同一项目修改后重新提交时，需要把“同一个业务实体”告诉平台：在 `CreateWorkflowBusinessApplyCommand` 或 `StartBusinessWorkflowCommand` 传 `affinityKey`（稳定聚合键，例如项目 ID，不要带版本号），并在节点配置 `nodeAffinityScope=...`。`PROCESS_INSTANCE`（默认）只复用当前实例；`BUSINESS_KEY` 再加同一 `businessKey`；`AFFINITY_KEY` 再加同一 `affinityKey`。每层命中有效候选即停，参考当前实例 → 同一业务键 → 同一亲和键的顺序；全部无有效候选才走 `nodeAffinityFallback` |
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
| 并发或重复办理同一任务 | 平台返回 `TASK_ALREADY_COMPLETED` 时按“任务已被其他审批人办理，请刷新后重试”提示；不要捕获 Flowable 外键异常或重试同一任务 |
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
| 承办人释放平台派单任务、管理员改派或回收任务 | 调用 `POST /workflow/tasks/unclaim-result`、`POST /workflow/tasks/reassign`；释放后任务回到节点候选池，同时发出 `workflow.task.unclaimed` |
| 转办或改派时选择目标办理人 | 调用 `GET /workflow/tasks/assignee-options?taskId=` 取当前时候选成员，不要自行拼人；释放/转办可用性以 `WorkflowTaskVO.unclaimable` 为准，不要用 `claimStatus` 推断；业务列表逐行判定时直接用当前任务快照的 `unclaimable`（见 7.4） |
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



## 7.1 审批列表读取全部任务（Issue #1208 / #1223）

业务列表需要"一行一个任务"（初审/复审/终审各占一行）时，用 `POST /workflow/tasks/page-by-business-key` 直接按业务类型读取全部任务，不需要业务侧先枚举业务主键，也不需要建任务副本表。

请求要点：

- `businessType`：按 `workflow_business_apply.business_type` 精确匹配。取业务适配器实际投放的值，例如 `BAOHAN_RC_PROJECT_REVIEW`；平台不校验命名风格，也不做别名映射。
- `businessTypes`：可选（Issue #1294），业务类型集合，最多 20 个，用于一个列表同时展示多个业务类型的任务（项目准入审批 / 合规复核 / 签约审核混排）。集合非空时按集合匹配且 `businessType` 被忽略；两者都不传或都为空时查询以 `业务类型不能为空` 拒绝。空数组、`null`、只传 `businessType` 三种情况生成的 SQL 与 1.12.0 完全相同。
- `processDefinitionKeys`：可选（Issue #1294），按 `workflow_business_apply.process_definition_key` 精确匹配，最多 100 个，传 `null` 或空集合表示不限流程定义。用于同一业务类型下有两条流程定义（例如普通审批与加签审批）时只取其中一条。
- `businessKeys`：可选，不传或为空表示不限业务主键。只有调用方已经明确业务对象范围时才传入，不需要为了列表请求先查业务表取候选业务键。
- `scope`：`SELF`（默认，办理人是当前用户或候选用户/候选组命中）、`ALL`（服务端校验 `workflow:task:all` 权限码，未安装授权中心时拒绝）、`ORG`（本版直接报错并提示改用数据范围配置）。部门档不再由调用方传部门 ID：平台按登录人读取数据范围配置（资源码 `workflow:task:list`）决定处理人可见集，配置为「全部」但当前用户没有 `workflow:task:all` 时退化成本人范围并记 WARN。
- `applyKeyword`：可选，模糊匹配申请编号、申请标题、申请摘要三列中的任意一列，输入里的 `%`、`_`、`\` 按字面匹配。它和 `keyword` 互不替代：`keyword` 只匹配任务名称和节点定义 Key。
- 不传 `page` / `size` 时按 `mango.workflow.task-query.unpaged-max-rows`（默认 2000）一次性返回；`total` 始终是过滤后的真实总数，用 `list.size() < total` 判断是否需要改用分页拉取。业务列表应显式传 `page` / `size`，`total` 按任务数计算，`list` 一行一个任务。

引擎列名事实：历史任务表的创建时间是 `ACT_HI_TASKINST.START_TIME_`，`CREATE_TIME_` 只存在于运行态 `ACT_RU_TASK`。

字段口径：

- `status`：`TODO` 在途、`DONE` 已办理、`CANCELED` 已终止（`deleteReason` 有值）。
- `claimStatus`：`UNCLAIMED` 待领取、`ASSIGNED` 待处理；转办后返回 `ASSIGNED`，与"待处理"一致。
- `returned`：该申请是否发生过退回。退回不改变 `apply_status`，因此"退回待修改"用 `applyStatus=IN_APPROVAL` 加 `returned=true` 表达，不要期望出现 `RETURNED` 状态值。
- `lastReturnAt` / `lastReturnTarget`：最近一次退回的时间和目标节点，用于判断"当前是否处于退回待修改"。
- `cancelCause`：机器可读的终止/退回原因，`REJECTED` / `RETURNED` / `WITHDRAWN` / `TERMINATED` / `OTHER`；`DONE` 也带 `cancelCause`（本人驳回或退回），其余状态为 `null`。业务侧按枚举出中文文案，不要匹配 `deleteReason` 文本——它是引擎内部文案（例如 `Change activity to risk_specialist_review`、`申请人撤回`），不稳定。
- `statuses` 支持分组值与细状态值，多值取并集：`TODO` → `{TODO}`；`DONE` → `{COMPLETED, REJECTED, RETURNED}`；`CANCELED` → `{WITHDRAWN, TERMINATED}`。要「只看被驳回的」用 `statuses=[REJECTED]`，要「只看被退回的」用 `statuses=[RETURNED]`，不需要再拿 `cancelCause` 自己过滤。五个细状态互斥且覆盖全量（`TERMINATED` 兜底「他人驳回导致本任务被作废」），`statuses=[DONE, CANCELED]` 与不限状态的行集一致。
- `statuses=[DONE]` 返回本人办过的任务：正常办结，外加本人驳回或退回、随后流程实例被删除的任务；被他人驳回牵连作废的任务落在 `CANCELED`，不会混入"已办理"。
- `assigneeIds` 直接传业务 `user_id` 即可，平台按 `identity_user.username` 映射到引擎办理人，并用 `tenant_member` 限定该用户属于任务所在租户，跨租户同名用户不会串号。
- 业务字段的多选精确筛选用 `filterAttributes` + `applyAttributeFilters`（Issue #1248）：创建申请时把可筛字段作为键值上报（如 `guaranteeTypeCode=PERF`、`beneficiary=中铁三局`），列表用同名键筛选；版本状态这类提交后会变化的值，在状态变化处调用 `PUT /workflow/business-applies/filter-attributes`（请求体 `{applyId, filterAttributes:[{key,values}]}`）整体覆盖刷新。字段由业务命名，新增或下线筛选字段都不需要平台改表或升级；多值字段按值数组上报（如 `beneficiary=[中铁三局, 中铁四局]`），筛其中任一值即可命中。默认通知的按钮目标也可以由业务决定：在申请快照 `customConfig` 里声明 `viewPath`、`viewQuery`、`viewActionLabel`，该目标对完成/驳回通知和**待办到达通知**都生效，按钮可直接跳到业务自己的待办/领取列表；不需要按钮时把 `viewActionEnabled` 设为 `false`，默认通知只发标题和正文，不注入按钮动作。

同一批属性也会进入工作流领域事件的 `applyAttributes`，默认通知把它们展开成 `applyAttribute_<key>` 消息变量（多值按英文逗号拼接），消息模板可以直接写 `{{applyAttribute_guaranteeTypeCode}}`；没有登记属性时不会注入变量，通知文案保持原样。平台不回读业务表，属性由业务写入，漏更新会直接体现在筛选结果。
- `applyKeyword` 只搜平台已存的申请编号、申请标题、申请摘要，用于模糊搜索。业务希望搜自己的字段（保函种类、项目编号等）文本时，在创建申请单时把可检索文本写进 `applySummary`，推荐 `名称：值，名称：值` 结构，例如 `保函种类：履约保函，项目编号：BH2026001`。摘要创建后不再更新，提交后会变的业务值不能依赖摘要搜索；需要多选精确筛选时用上一行的 `filterAttributes`，不再需要 `businessKeys`。
- 退回后同一 `taskDefinitionKey` 会再次出现（初审 → 复审 → 退回 → 初审），这是正常返回，按 `endTime` / `claimTime` 取最新。
- `applyAttributeFilters`：可选，按业务登记在申请上的可筛属性精确筛选，条件之间为「且」、同一条件的多个值为「或」，最多 10 个条件、单条件最多 50 个值。它替代「先查业务表 → 拼 `businessKeys`」的绕法：没有再枚举上限，`total` 就是过滤后的真实任务数。没有该属性的申请不匹配，存量申请需要先回填。
- `applyTimeBegin` / `applyTimeEnd`：可选，按申请创建时间闭区间过滤，与任务创建/处理时间互不影响。
- 排序：`createTime`、`endTime`、`claimTime`、`applyTime`；`endTime` 与 `claimTime` 为空的任务在升序和降序下都排在最后；时间相同的任务按任务 ID 升序，翻页不重不漏。平台不设默认时间窗，默认时间范围由业务自己决定。
- 该查询直接读取 Flowable 引擎表，租户由 `workflow_business_apply.tenant_id` 收敛；平台租户插件按 `act_*` 前缀跳过引擎表，业务项目不需要再往 `mango.persistence.mybatis-plus.tenant.excluded-tables` 复制引擎表。

权限分工：`workflow:task:list` 控制能否调用接口；`workflow:task:all` 控制能否使用 `ALL` 范围。业务侧按自己的角色矩阵把后一个权限码只授予主管/经理等需要全量视图的角色。

四个过滤维度的粒度不同，不要互相替代：

| 维度 | 字段 | 落库位置 | 一个值代表什么 |
|------|------|----------|----------------|
| 业务类型 | `businessType` / `businessTypes` | `workflow_business_apply.business_type` | 业务域（一套业务数据、一套菜单权限、一套数据权限适配器） |
| 流程定义 | `processDefinitionKeys` | `workflow_business_apply.process_definition_key` | 同一业务域下的一条流程（变体或不同审批链路） |
| 节点定义 | `taskDefinitionKeys` | `ACT_HI_TASKINST.TASK_DEF_KEY_` | 一个审批节点（初审 / 复审 / 终审），一行任务只属于一个节点 |
| 业务主键 | `businessKeys` | `workflow_business_apply.business_key` | 一个业务对象实例（某一份保函） |

多个条件同时出现时按「且」叠加：`businessTypes` 与 `processDefinitionKeys` 都传时取交集。业务类型集合内部是「或」。

展示提示：把多个业务类型归一成域级 `business_type` 会让任务列表失去区分度，只能靠业务键前缀反推，不推荐。混排列表直接用返回里的 `processKey`（取自申请表 `process_definition_key`）做列或标签，`processName` 是流程名称，比枚举值更适合作文案。

Issue #1297 只更新平台侧的公开契约哨兵与 CI 门禁（`mango-workflow-starter` 的 `WorkflowApiSurfaceContractTest` 黄金指纹、`pmo-doc-check` 的 `workflow_contract` job），本节描述的请求字段、响应字段、权限码、错误码与分页语义均无变化，业务仓无需任何动作。

## 7.2 审批流转记录读取（Issue #1234）

审批详情面板需要展示"发起 / 认领 / 通过 / 退回 / 驳回"及多轮退回历史时，用 `POST /workflow/tasks/trace-by-business-key` 或本地注入 `WorkflowTaskRuntimeApi#traceByBusinessKey` 读取，不要直接查 `workflow_task_record` 表，也不需要注册 `WorkflowBusinessApplyDataPermissionProvider`。

请求要点：

- `businessType` / `businessKey`：必填，按 `workflow_business_apply` 精确匹配。只登记了 `workflow_form_instance`、未走申请中心的流程没有 `business_type`，调用返回空集合。
- `operatorIds`：可选，业务 `user_id`。平台按 `operator_id` 与 `operator_name` 同时匹配，并用 `identity_user.username` 补齐只写入名称的记录。
- `deptIds`：可选，按 `workflow_business_apply.applicant_dept_id` 过滤**整条流程实例**；同一业务键下不同部门的实例会被整体排除。这是与 `operatorIds` 的关键差别。
- `actions`：可选，取值见审批动作枚举；未知动作直接报错，不静默返回空集合。
- `beginTime` / `endTime`：可选，按 `createdTime` 闭区间过滤。
- 三个集合参数传 null 或空集合表示**不过滤**。

返回口径：

- 扁平列表，`processInstanceId` 是分组键。重新申请会让同一业务键对应多个流程实例，平台全部返回，不提供 `latestOnly`；按实例分段展示由调用方 `groupBy` 完成。
- 排序：`createdTime` 升序，同一时间按 `id` 升序，保证多轮退回顺序稳定。
- 返回动作：`START` / `CLAIM` / `UNCLAIM` / `COMPLETE` / `REJECT` / `RETURN` / `TRANSFER` / `ADD_SIGN` / `READ` / `WITHDRAW` / `AUTO_*` / `EVENT_NOTIFY`。撤回不写 `workflow_task_record`：平台把 `workflow_business_apply_status_log` 中该实例的 `WITHDRAW` 行并入返回列表，升级前已撤回的历史申请同样可见；同一实例多条 `WITHDRAW` 只返回最早一条，`id` 为状态流水主键（稳定）；`processDetail` 不含撤回行，行为不变。
- 不返回网关/条件分支节点：它们不在 `workflow_task_record`，改用下方 7.3 的活动时间轴接口读取。
- 不返回 `variables`（字段恒为 null）、审计列、租户列和业务字段。`START` 行的 `taskId` / `taskName` / `taskDefinitionKey` / `comment` 为 null，节点标题用 `actionName`；`CLAIM` 的 `comment` 是系统文案"认领任务"，不是审批意见。
- 记录数超过 `mango.workflow.task-query.unpaged-max-rows`（默认 2000）时以 `TASK_QUERY_INVALID` 失败，不静默截断；调用方需要缩小时间区间。

权限边界：

- 接口权限只有 `workflow:task:list`，不再校验 `workflow:task:all`，也不做申请级权限判定，因此**不返回 3645**。
- 任何持有该权限码、且知道 `businessType + businessKey` 的账号都能读到该业务键的全部流转记录与审批意见原文。本地注入 `WorkflowTaskRuntimeApi` 直接调用不经过 HTTP 过滤器，平台侧没有行级防线；业务详情页在调用前先完成自身的业务鉴权，由服务端解析 `businessKey`，不接受前端传入的键。权限与鉴权边界见 [后端 API 规范](../../../mango-pmo/rules/backend/03-api.md)。

### 7.3 活动时间轴读取（Issue #1235）

需要还原"任务 + 网关/条件分支"的完整路径时，用 `POST /workflow/tasks/activity-timeline-by-business-key`（本地注入 `WorkflowTaskRuntimeApi#activityTimelineByBusinessKey`）。它与 7.2 的流转记录分工不同：

| 维度 | 流转记录 `trace-by-business-key` | 活动时间轴 `activity-timeline-by-business-key` |
|---|---|---|
| 数据来源 | `workflow_task_record` + 撤回状态流水 | Flowable 历史活动表 |
| 粒度 | 任务级（发起/认领/通过/退回/驳回/撤回） | 活动级（任务 + 网关 + 事件 + 其它） |
| 处理人 | `operatorId` / `operatorName` | `assignee`，非人工活动为 null |
| 典型用途 | 审批详情面板的动作列表 | 流程图路径、条件分支走向、节点耗时 |

返回口径：

- 请求体字段：`businessType`、`businessKey`（必填），`deptIds`、`beginTime`、`endTime`（可选，`beginTime`/`endTime` 按活动开始时间过滤）。
- 每行字段：`processInstanceId`、`activityId`、`activityName`、`activityType`（Flowable 原值，如 `exclusiveGateway`）、`kind`、`assignee`、`startTime`、`endTime`、`durationMillis`。
- `kind` 取值：网关类活动为 `GATEWAY`，`userTask`/`manualTask`/`receiveTask` 为 `TASK`，开始/结束/边界/中间事件为 `EVENT`，其余（服务任务、脚本任务、调用活动、子流程）为 `OTHER`。
- 返回该业务键下**全部活动**，不只网关：只返回网关会让"任务行 vs 活动行"两套时序对不齐。排序为开始时间升序、同一时间按 `activityId` 升序。
- 活动数超过 `mango.workflow.task-query.unpaged-max-rows`（默认 2000）时以 `TASK_QUERY_INVALID` 失败，不静默截断。
- 权限与流转记录一致，复用 `workflow:task:list`，不额外校验数据范围。

> Issue #1235 影响说明：新增公开端点（minor）。既有 `trace-by-business-key` 的返回内容、字段与语义完全不变；前端渲染不在本次范围。

## 7.3 审批人来源与多人审批方式（Issue #1263）

审批节点的 `assigneeType=SPECIFIED_ROLE|SPECIFIED_POST|SPECIFIED_ORG|ORG_LEADER` 不再生成一条共享待领取任务，改为流程进入节点时把候选组展开为当前租户启用成员，再按 `approvalMode` 生成每人一条任务：

- `OR_SIGN`：每人一条待办，任一人办结后其余任务以 `MI_END` 取消。
- `COUNTERSIGN`：按 `passRatio` 计算所需通过人数，默认 100% 即全部成员通过。这是节点默认值，角色节点重新发布后会从“一人处理”变为“全员通过”。
- `SEQUENTIAL`：按 `userId` 升序依次到达。

成员解析在运行时完成，角色、岗位、组织成员变化对下一次进入节点立即生效，不需要重新发布流程定义。展开结果为空时走节点 `emptyAssigneeStrategy`，不会静默通过。

业务接入影响：

- 这类节点的任务不再进入“待领取”，成员在“待办”直接看到自己的任务；按 `claimStatus` 或 `currentAssigneeName=="待领取"` 判断待领取的业务代码需要改为按任务是否已派单判断。
- 之前用“领取 + 自动领取”实现“复用上一轮办理人”的业务，改为两种方式之一：节点 `approvalMode=OR_SIGN` 并把自动领取换成 `transfer` 转办；或节点改用 `assignmentMode=AUTO` + `autoAssignmentStrategy=AFFINITY/NODE_AFFINITY`，由平台直接把任务派给上一轮办理人。
- 节点启用 `claim` 动作后不再有可领取任务，领取入口应同步移除。
- 通知改为每个成员收到指向自己 `taskId` 的 `workflow.task.assigned`。
- 已发布流程定义不受影响：BPMN 在定义发布时生成，`ensurePublished` 按 `designerJson` 判等，升级或重启不会自动重发布；只有重新发布后才按新规则生成任务。

迁移路径、需要同步调整的业务代码、发布验证和回滚步骤见 [Workflow 审批人候选组展开升级指南](../operations/workflow-approval-candidate-expand-upgrade.md)。

## 7.4 业务列表判定释放资格（Issue #1282）

业务列表从 `WorkflowBusinessApplyVO.currentTasks`、`WorkflowBusinessApplyProgressVO.currentTasks`、`WorkflowTaskActionResultVO.currentTask/nextTasks`、`WorkflowStartResultVO.currentTask`、`WorkflowEventPayloadVO.currentTask` 拿到的 `WorkflowBusinessApplyCurrentTaskVO` 已带释放资格字段，逐行控制「释放」按钮不再需要为每行调用 `taskRuntimeApi.detail(taskId)`：

| 字段 | 类型 | 语义 |
|------|------|------|
| `unclaimable` | `Boolean` | 当前登录用户能否释放该任务，口径与 `WorkflowTaskVO.unclaimable` 同源：任务有承办人、承办人是本人、来源为 `SELF_CLAIM` 或 `AUTO_ASSIGN`。 |
| `assignSource` | `WorkflowTaskAssigneeSource` | 承办人来源，用于按钮分组和文案；未指派时为 `null`。 |
| `claimTime` | `LocalDateTime` | 办理人获得任务的时间：本人认领为领取时间，平台派单为派单到达时间；未指派时为 `null`。 |

判定口径：

- `assignmentMode=AUTO` 派出的任务来源为 `AUTO_ASSIGN`，`unclaimable=true`。
- 审批节点按办理人指定（`DEFINITION`）或退回后按目标人新建（`RETURN_ASSIGN`）的任务为 `unclaimable=false`，业务侧不显示释放按钮，不再依赖「点错后收到 417013」。
- 本人认领（`SELF_CLAIM`）任务为 `unclaimable=true`，`claimTime` 为领取时间且非空。
- `claimStatus` 不能替代 `unclaimable`：`SELF_CLAIM`、`AUTO_ASSIGN`、`DEFINITION`、`RETURN_ASSIGN` 都是 `ASSIGNED`。
- 字段随当前任务表一并读取，不产生额外 RPC；释放、改派、流转后按 `workflow.task.unclaimed`、`workflow.task.advanced` 事件或重新查询刷新。
- 按钮渲染后任务可能已被改派，服务端仍会拒绝：按 `TASK_RELEASE_FORBIDDEN`（业务码 `417013`、平台码 `3663`）提示刷新，不要按成功处理。

升级影响：Flyway `V6__workflow_business_apply_current_task_claim_state.sql` 为 `workflow_business_apply_current_task` 新增 `assign_source`、`claim_time` 两列，不需要回填。升级前已存在的当前任务行两列为 `NULL`，`unclaimable` 对它们返回 `false`（来源未知按不可释放处理）；任务再次刷新或流转后自动补齐。不新增接口、权限码、配置项和页面入口。

## 8. 验证命令

后端验证使用 Java 21。legacy migration fixture 和 API surface fingerprint 是测试资产；本次校准仅跟随既有 V1/V2/V3 迁移与 API 声明版本，未修改生产历史 SQL、公开 API、配置、菜单、权限、租户、页面、启动或审批运行时行为。业务审批接入步骤保持不变。

```bash
mvn -f mango/pom.xml -pl mango-platform/mango-workflow -am test
# 按业务类型查询任务的 H2 集成测试与范围规则单测
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

> Issue #1282 影响说明：业务列表取回的当前任务快照 `WorkflowBusinessApplyCurrentTaskVO` 增加 `unclaimable`、`assignSource`、`claimTime`，与 `WorkflowTaskVO` 同名同语义，业务侧不需要再逐行调用任务详情接口即可判断是否展示「释放」按钮。新增 Flyway `V6` 为 `workflow_business_apply_current_task` 加 `assign_source`、`claim_time` 两列，不回填历史行（旧行 `unclaimable=false`，任务刷新后补齐）；不新增接口、权限码和配置项，释放被拒错误码 `417013`/`3663` 不变。

> Issue #1269 影响说明：自动派单的 `AUTO_ASSIGN` 流转记录改为把 `operator_id`/`operator_name` 写成真实承办人，不再写成触发节点进入的发起人或上一步办理人。审批详情时间线对自动派单节点显示“自动派单 · 承办人”；按办理人过滤流转记录的调用方会同时命中该办理人的自动派单记录。接口入参与返回结构、配置项、数据库表结构、权限码、租户隔离和任务派单结果不变；未投产、无需回填历史记录。

> Issue #1263 影响说明：角色、岗位、组织、组织负责人审批人按节点 `approvalMode` 展开为每人一条任务，不再生成待领取共享任务。会签默认值下角色节点重新发布后要求全体成员通过；依赖“待领取”或自动领取的业务代码需要迁移到按办理人或自动派单判断。接口、配置项、数据库结构和权限码不变，已发布定义在重新发布前行为不变。

> Issue #1231 影响说明：`POST /workflow/tasks/page-by-business-key` 新增可选参数 `applyKeyword`，模糊匹配申请编号、申请标题、申请摘要，输入中的 `%`、`_` 按字面匹配；`keyword` 语义不变，仍只匹配任务名称和节点定义 Key。业务需要搜自己的字段时，在创建申请单时把可检索文本写进 `applySummary`，推荐 `名称：值，名称：值` 结构；摘要创建后不再更新。变量白名单过滤和平台侧业务列本次不提供，业务字段的多选精确筛选继续使用 `businessKeys`。数据库结构、权限码、租户隔离和分页口径不变。

> Issue #1223 影响说明：`POST /workflow/tasks/page-by-business-key` 的 `businessKeys` 改为可选；不传或为空表示不限业务主键，`total` 按任务数统计。业务列表不再需要先查业务表取候选业务键，也不会因为业务键数量上限丢数据。传入 `businessKeys` 时行为与 #1208 兼容；不传时业务申请可见性不做逐申请 Provider 收窄，由租户、`scope` 和权限码决定。

> Issue #1220 影响说明：修复 `page-by-business-key` 在启用默认租户插件时 500（`Unknown column 't.tenant_id'`）。平台租户插件改按 `act_*`、`flw_*` 前缀跳过 Flowable 引擎表，业务项目可以删除 `mango.persistence.mybatis-plus.tenant.excluded-tables` 中自行追加的 `ACT_*` 例外，并恢复使用平台默认清单。查询字段、权限码、申请状态、数据库表结构和页面行为不变。

> Issue #1208 影响说明：新增 `POST /workflow/tasks/page-by-business-key`，业务列表可读取业务主键下的全部任务（在途、已办理、已终止）。不修改数据库结构，不改变 `todo` / `done` / `copied` 语义；新增配置项 `mango.workflow.task-query.unpaged-max-rows`。

> Issue #1098 影响说明：资源声明的 `sync-mode` 收敛为 `SYNC_ALWAYS` / `SYNC_ONCE`（旧值 `AUTO`、`INIT_ONLY`、`MANUAL`、`LOCKED` 仍可读取并映射）。本场景涉及的数据初始化、租户边界、权限判定与页面行为不变。
> Issue #1346 影响说明：`WORKFLOW_DEFINITION` 声明现在按租户与流程编码派生分类、流程定义与流程版本主键，空库回放得到相同主键；本指南描述的接口入参、返回字段、权限码与任务语义均无变化，已发布流程定义的主键保持不变。
