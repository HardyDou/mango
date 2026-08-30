# 业务审批接入

> 日期范围查询兼容：业务申请分页和历史查询的日期-only `startedAtBegin/startedAtEnd` 会分别按当天 `00:00:00` 和 `23:59:59` 发送；其它字段、权限和业务数据范围不变。

> 2026-07-22 菜单显示文案调整说明：审批中心更名为审批管理；流程 API、页面 key、权限码、业务接入方式和本指南步骤均不受影响，历史记录保留原名称。

## 1. 适用场景

业务单据需要发起审批，审批结束后回写业务状态，并能在业务页面查看流程进度。

## 2. 阅读顺序

| 顺序 | 文档 | 关注点 |
|------|------|--------|
| 1 | [Workflow 后端 README](../../../mango/mango-platform/mango-workflow/README.md) | 流程定义、实例、任务、事件和配置 |
| 2 | [@mango/workflow README](../../../mango-ui/packages/workflow/README.md) | 流程页面、设计器、任务 API |
| 3 | [Workflow Example README](../../../mango-ui/packages/workflow-business-example/README.md) | 业务接入示例和页面 key |
| 4 | [能力地图：业务审批闭环](../../capabilities/README.md#3-组合接入入口) | 组合验证入口 |

办理人身份特性升级请先阅读[Workflow 办理人身份特性升级指南](./workflow-assignee-identity-upgrade.md)。该指南固定版本升级、接口权限、DTO/事件透传、前端去重查询和历史数据边界。

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
| 流程模板页面打开即出现机构列表 403 | 前端不应在页面挂载时加载目标机构，也不应调用 `/system/tenant/list`；打开推送弹窗后调用 `/workflow/templates/tenant-options`，并确认拥有 `workflow:template:push` |

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

`POST /workflow/tasks/return` 会把当前任务退回到最近一个已完成的不同用户任务节点，或退回到 `targetTaskDefinitionKey` 指定的历史节点。串行流程可以不传目标节点；并行、多实例、重复审批节点或业务语义固定的流程，应在流程节点动作配置或业务审批页中显式传入 `targetTaskDefinitionKey`。接口返回结构与 `complete-result` 一致，业务侧应使用返回的 `currentTasks` 或订阅 `workflow.task.advanced` 刷新业务单据当前节点和当前办理人；退回不会发布 `workflow.task.completed`，也不会把流程状态改为驳回。

`POST /workflow/processes/withdraw` 与 `WorkflowProcessApi.withdraw()` 支持使用 `applyId` 或 `processInstanceId` 定位申请，`reason` 必填。后端同时校验 `workflow:process:withdraw` 权限、租户上下文和原申请人身份；仅运行中的 `IN_APPROVAL` 可首次撤回，已撤回请求按幂等成功返回，其它终态不会被改写。成功响应包含撤回前后状态、`withdrawn`、`idempotent`、`ended` 和原因，并发布 `workflow.process.withdrawn` 后再发布 `workflow.process.ended`。业务模块仍需先判断业务单据是否允许撤回，并用事件 ID 或业务主键幂等维护自身状态机、快照和通知；Workflow 不替代业务状态机。当前改动不提供新的前端撤回按钮，业务页面应按自身权限和状态决定是否展示操作入口。

办理人字段约定：`assigneeName`/兼容字段 `assignee` 保留 Flowable 原始 key；`assigneeId` 和 `assigneeDisplayName` 由 Workflow 在当前租户内批量解析，解析失败开放为空。候选组 key 不代表具体用户；身份服务暂不可用时审批结果仍会返回，页面回退原始 key。

单体多实例、微服务或微服务多实例部署时，事件应按至少一次投递处理。业务订阅方使用 `eventId`、`processInstanceId + completedTaskId` 或业务主键构造幂等键，避免重复回写状态、重复发通知或重复生成待办摘要。

业务订阅事件时，依赖边界应停留在 `mango-workflow-api`：事件类型使用 `WorkflowEventTypes`，`event.payload` 使用 `WorkflowEventPayloadVO` 反序列化。不要在业务模块中引用 `io.mango.workflow.core.event.WorkflowDomainEvents`、`WorkflowEventPublisher` 或 `io.mango.workflow.core.service.*`。业务列表需要展示当前节点、当前办理人、认领状态或候选人时，使用 `WorkflowBusinessApplyApi.latestProgress()`、批量进度 API 或任务动作 result 返回值，不要直接查询 workflow 运行表。

## 7. 变更影响记录

- Issue #732 新增租户隔离的 Workflow 参与关系 API 和审批节点自动派单。业务可通过 `participantUserIds` 原子声明只读参与人，历史参与关系与任务操作权限保持分离；流程设计器中旧节点缺少 `assignmentMode` 时按 `CLAIM`，显式 `AUTO` 时可使用数据库游标保护的 `ROUND_ROBIN`、按活动任务数选择的 `LEAST_TASKS` 或流程实例最近处理人优先的 `AFFINITY`（未命中回退 `LEAST_TASKS`），空候选返回 `AUTO_ASSIGN_NO_CANDIDATE` 并回滚。设计器候选项改由 Workflow 自有接口和可替换 `WorkflowDesignerOptionProvider` 提供，只使用 Workflow 定义权限并从可信上下文派生租户。V3 仅回填可证明的稳定用户 ID，username-only 历史不授权。

- Issue #870 为业务申请详情、历史、最新进度和流程详情增加 `WorkflowBusinessApplyDataPermissionProvider` 扩展点。业务模块在同一 Workflow 运行时中注册 Provider，并按 `businessType` 从自己的业务表校验 owner、组织和租户；普通业务员不再依赖全局 `workflow:business-apply:detail`。Workflow 使用持久化申请事实构造权限上下文，无权时返回 `APPLY_ACCESS_DENIED`，批量进度过滤无权记录。Provider 运行在实际承载 Workflow 服务的应用中，业务查询通过公开 Workflow API 完成；完整接入边界见 [Workflow README](../../../mango/mango-platform/mango-workflow/README.md#业务申请数据权限)，长期能力文档边界见 [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)。升级验证覆盖业务详情正反权限用例和业务代理兼容路径移除。

- 2026-08-03 修复空库 Bootstrap 装配业务 starter 时公开 Workflow API 注入提前创建 Flowable 的问题。Bootstrap 只注入延迟解析的公开 API 代理，migration 后的 Resource step 仍按需发布定义，Runtime 仍使用原 Controller。业务继续依赖 `mango-workflow-api`，不需要改为 core service、恢复 `forceSync()`、手工建表或开启 Flowable 自动建表。

- Issue #606 为开启 demo 资源的租户 `1` 默认 `ROLE_ADMIN` 初始化 `workflow:definition:list = ALL`，使全新数据库中的流程定义管理分页可读取租户内已发布定义；`INIT_ONLY` 首次遇到已有数据范围时保留人工配置。该修复不改变业务模块按 `definitionKey` 发起审批、任务办理、回调、状态回写或租户隔离；`startEntryVisible=false` 仍只隐藏审批中心发起入口，不隐藏流程定义管理记录。

- Issue #613/#614 修复工作流通知接收人和异步应用上下文：流程完成、驳回和结束事件携带原申请人，终态通知只发送给 `applicantId`；`workflow.task.advanced` 对共享候选任务按候选用户、角色、岗位、组织扩展同一个 `taskId`，对并行或多实例的已分配任务按每条运行时任务及其 `assigneeId` 分开发送。事件同时携带 `tenantId`、`appCode`、`realm`，Notice 本地和远程监听器发送前恢复这些字段，角色成员按原应用与登录域解析。没有有效接收人的事件直接跳过；`ORG_LEADER` 仍不扩大为整个组织。

- Issue #506 为 Maven `1.0.20` 已执行 Workflow V1 的数据库增加安全前向迁移。升级版本只兼容已知的 `1.0.20` V1 checksum，并通过 V2 幂等补齐 7 个审计列；`1.0.21`/`1.0.22` 创建的新数据库已有列时不会重复添加，未知 checksum 继续由 Flyway 阻断。业务审批 API、流程状态、权限、租户和页面行为不变；升级后检查 `flyway_schema_history_workflow` 的 V1/V2 状态和 Workflow README 列出的审计列。

- PR #502 统一由 `mango-workflow-api` 提供参数校验约束，starter Controller 继承契约而不重复声明。业务审批的 HTTP/Java API、请求与响应结构、校验规则、流程状态流转、权限和事件行为不变，业务调用方无需改造。

- PR #497 只把 Workflow 发布 Notice 事件时的租户标识显式写入事件契约，并同步 Payment 直接消费者；工作流定义、发起/审批/退回接口、业务状态回写、菜单权限和页面入口不变。业务审批通知继续使用既有业务类型与模板；未配置外部渠道账户时按 Notice 原有重试或取消策略处理。

- v2026.06.30-maven-1.0.1-admin-branding-cli-release 只对齐固定后端 Maven `1.0.1`、前端 npm 批次和 CLI/starter 版本锁；不改变业务审批发起、审批回调、状态回写、流程页面 key、后端公开 API、配置、菜单、权限、租户隔离、启动方式和运行时行为。业务项目应成组升级本发布批次的后端 `<mango.version>` 和前端 `@mango/*` 包，避免新旧前端依赖混装。

- v2026.06.29-workflow-return-cli-db-release 发布工作流退回能力和前端聚合版本锁。既有审批发起、通过、驳回、撤回和事件订阅流程保持兼容；使用退回动作前，先完成资源同步并给角色授予 `workflow:task:return` 权限。

部署配置示例：

```yaml
mango:
  event:
    outbox:
      enabled: true
    transport: redis-stream
```

## 7. 验证命令

```bash
mvn -f mango/pom.xml -pl mango-platform/mango-workflow -am test
pnpm -F @mango/workflow build
pnpm -F @mango/workflow-business-example build
```

模块验证入口：

- [Workflow 验证方式](../../../mango/mango-platform/mango-workflow/README.md#10-验证方式)
- [Workflow Frontend 验证方式](../../../mango-ui/packages/workflow/README.md#10-验证方式)
- [Workflow Example 验证方式](../../../mango-ui/packages/workflow-business-example/README.md#10-验证方式)

## 8. 关联规则

- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
- [AI 交付质量规则](../../../mango-pmo/rules/05-ai-delivery-quality.md)

## 9. 变更影响记录

- 2026-07-22 富文本托管图片基础能力因前端固定依赖传播，将 `@mango/workflow` 升至 `1.0.34`、`@mango/workflow-business-example` 升至 `1.0.33`；本次仅对齐 npm 版本矩阵，不新增或修改审批组件、审批发起、任务办理、回调、状态回写、流程页面 key、API、菜单、权限、租户隔离或本场景验收步骤。

- MySQL 8.4 告警治理将 Workflow 空库基线改为 `utf8mb4` 和无整数显示宽度 DDL，并把生成后端的 Flyway 基线对齐到 11.20.3；审批发起、任务办理、回调、状态回写、页面 key 和公开 API 均不改变。既有生产库不由本次基线改写转换字符集，新建数据库直接采用新基线。

- PR #490 一次性治理 Workflow 后端架构债务并重建空白数据库初始化边界：公开 HTTP 路径、Java API、任务流转、事件类型和业务审批接入方式保持不变；Flyway 只保留纯 DDL 的 V1，Flowable 必需元数据由启动初始化器按缺失项补齐，三条示例流程改为 `META-INF/mango/demo/` 下的 `INIT_ONLY` 声明且默认不加载。业务项目继续只依赖 `mango-workflow-api` 或 remote starter；本次数据库基线只支持新数据库，演示环境需显式设置 `mango.resource.registry.demo-enabled=true`。

- v2026.07.11-maven-1.0.14-cli-release 仅将当前后端实现向前发布为 Maven `1.0.14` 并更新 CLI 后端版本锁；不改变流程定义、业务申请/审批 API、任务操作、工作流事件、数据权限或验收步骤。

- v2026.07.11-npm-readme-forward-release 仅向前发布已更正的 package README 并传播精确 npm 依赖版本；不改变流程定义、业务申请/审批 API、任务操作、工作流事件、数据权限或本场景验收步骤。

- v2026.07.08-admin-page-layout-release 只发布后台统一页面骨架组件、运营列表页 CLI/starter 模板和前端 npm 版本锁；不改变业务审批发起、审批回调、状态回写、流程页面 key、后端公开 API、配置、菜单、权限、租户隔离、启动方式和运行时行为。业务项目升级时按发布说明成组升级前端 `@mango/*` 包和 `@mango/cli`。

- 本次 PR 将业务审批页面需要复用的 `workflow:*` 接口权限放到业务菜单自身的 `apiCodes` 中，而不是挂到 workflow 菜单或按钮节点下。业务员、业务经理、总经理等非风控角色可以因业务菜单获得审批接口权限，但不会被自动带出审批中心、风控审批或风控工作台菜单。新业务接入审批时，应在本业务菜单声明需要的 workflow 接口权限，并由业务后端继续校验单据状态、办理人、租户和幂等。

- v2026.07.07-maven-1.0.9-api-contract-release 发布 PR #400 的 workflow API 边界治理物料，并对齐 `@mango/workflow@1.0.23`、`@mango/workflow-business-example@1.0.22`、`@mango/admin-shell@1.0.36`、`@mango/admin@1.0.41` 和 `@mango/cli@1.0.60`。业务模块发起审批、任务办理、事件订阅和进度查询应继续只依赖 `mango-workflow-api` 或 remote starter，不直接引用 workflow core service/event，也不直接读取 workflow 表；既有业务审批发起、审批回调、状态回写、流程页面 key、菜单、权限码、租户隔离和组合入口保持兼容。

- PR #400 处理 workflow API 边界治理。业务模块发起业务审批可使用 `WorkflowProcessApi.startBusinessWorkflow()` 一次性创建业务申请并启动流程；审批、保存、认领和取消认领需要同步刷新页面时，使用任务 result API 读取 `progress.currentTask`、`claimStatus`、`candidateUsers` 和 `candidateGroups`。业务后端和事件订阅方只依赖 `mango-workflow-api` 或 remote starter，不依赖 workflow core service/event，也不直接读取 workflow 表。流程页面 key、菜单、权限码、租户隔离和既有 `WorkflowBusinessApplyApi.create()` + `WorkflowProcessApi.start()` 组合入口保持兼容。

- PR #388 支持站内消息动作的 `FLOW` 目标类型，业务模块可在消息中携带流程或任务入口动作；不改变业务审批发起、审批回调、状态回写、流程页面 key、后端公开 API、配置、菜单、权限、租户隔离、启动方式和运行时行为。排查审批消息动作无法打开流程时，额外确认消息动作 `targetKey`、隐藏业务参数、流程/任务权限和对应 workflow 页面注册。

- v2026.07.02-maven-1.0.6-home-widgets-cli-release 将工作流首页小组件归属到 `@mango/workflow@1.0.20` 并更新 CLI 版本锁；不改变业务审批发起、审批回调、状态回写、流程页面 key、后端公开 API、配置、菜单、权限、租户隔离、启动方式和运行时行为。业务项目升级时按发布说明成组升级后端 `<mango.version>`、前端 `@mango/*` 包和 `@mango/cli`。

- PR #356 新增 `WORKFLOW_DEFINITION` 资源声明处理器和 `WorkflowTaskRuntimeApi` 公共任务运行时 API。业务模块可通过资源声明随模块同步流程定义，也可依赖 `mango-workflow-api` 调用待办、已办、抄送、详情、签收、办理、驳回、保存、转办、加签和流程详情能力；既有 `WorkflowBusinessApplyApi.create()` 与 `WorkflowProcessApi.start()` 审批发起方式保持兼容，不改变流程页面 key、菜单、权限码、租户隔离、业务状态回写和页面验收入口。

- PR #295 只治理 Issue #183 后端测试规范、Mockito 审计和 workflow core service 集成测试；不改变业务审批发起、审批回调、状态回写、流程页面 key、后端公开 API、配置、菜单、权限、租户隔离、启动方式和运行时行为。

- Issue #233 明确审批任务完成后的流程推进时序：业务模块同步下一节点待办、当前办理人或业务状态时，使用 `workflow.task.advanced` 或 `POST /workflow/tasks/complete-result`；`workflow.task.completed` 只表示当前任务完成，不承诺当前任务快照已刷新。

- Issue #296 新增审批退回能力：`POST /workflow/tasks/return` 使用 `workflow:task:return` 权限，支持退回到最近历史用户任务节点或 `targetTaskDefinitionKey` 指定历史节点，并返回刷新后的当前任务快照。退回语义和驳回终止不同；业务模块可用接口响应或 `workflow.task.advanced` 同步业务当前节点、当前办理人和待办摘要。

- v2026.06.27-workflow-history-dialog-release 发布 `@mango/workflow@1.0.17`、`@mango/admin-shell@1.0.29`、`@mango/grid-widgets@1.0.6`、`@mango/workflow-business-example@1.0.16`、`@mango/admin@1.0.33` 和 `@mango/cli@1.0.46`，仅对齐工作流历史弹窗标题修复的 npm 物料、聚合包和 CLI/starter 版本锁；不改变业务审批发起、审批回调、状态回写、流程页面 key、后端公开 API、配置、菜单、权限、租户隔离、启动方式和运行时行为。

- 本次 PR 修复 `WorkflowInstanceHistoryDialog` 内部历史申请标题重复显示问题，并为 `WorkflowInstanceHistory` 增加 `showTitle` 展示开关。该开关默认 `true`，业务页面直接使用历史申请组件时标题行为不变；内置弹窗会关闭组件内部标题，只保留弹窗标题。此次变更不改变业务审批发起、审批回调、状态回写、流程页面 key、后端公开 API、配置、菜单、权限、租户隔离、启动方式和运行时行为。

- Issue #275 修复标准审批任务详情页对流程定义管理接口的隐式依赖。办理人打开 `/workflow/task/detail` 或业务包装后的任务详情页时，页面只依赖任务详情、流程实例、业务申请、`formJson`、运行时变量、`renderConfig` 和可选运行时 `designerJson`；不再主动调用流程定义列表、详情或版本管理接口。业务项目不需要给普通审批办理人额外授予流程定义管理权限；若运行时详情未携带 `designerJson`，页面会降级展示审批记录，不阻断业务表单和审批操作。

- Issue #650 补齐业务运行时详情的流程图快照。`GET /workflow/tasks/detail`、`GET /workflow/processes/detail` 和兼容入口 `GET /workflow/tasks/process-detail` 使用 `LOGIN` 访问模式，已登录业务用户不需要任务详情、流程详情或流程定义管理权限；`designerJson` 按实例 `processDefinitionId` 读取不可变发布版本，历史实例不会漂移到最新定义。业务页面直接把该字段交给 `WorkflowProgressTree` 或 `WorkflowSidebar`，不要调用定义管理 API 绕行。

- v2026.06.27-system-component-release 同步发布 `@mango/workflow@1.0.16`、`@mango/workflow-business-example@1.0.15` 及其前端依赖批次，仅对齐 npm 物料和 CLI/starter 版本锁；不改变业务审批发起、审批回调、状态回写、流程页面 key、后端公开 API、配置、菜单、权限、租户隔离、启动方式和运行时行为。业务项目排查审批页面异常时，仍先确认前端包批次一致、页面 key 已注册、流程定义和任务数据有效。

- 本次 PR 调整内置审批任务详情页操作按钮栏布局：按钮栏只显示在左侧业务内容列下方并居中，右侧流程摘要栏下方不再显示操作区；内容较长时按钮栏仍在内容列底部粘性悬浮。此次变更不改变业务审批发起、审批回调、状态回写、流程页面 key、后端公开 API、配置、菜单、权限、租户隔离、启动方式和运行时行为。

- PR #268 新增 `@mango/workflow` 可复用审批详情 UI 组件：`WorkflowLayout`、`WorkflowSidebar`、`WorkflowInstanceSummary`、`WorkflowInstanceProgress`、`WorkflowDefinitionGraph`、`WorkflowDefinitionGraphDialog`、`WorkflowInstanceHistory` 和 `WorkflowInstanceHistoryDialog`。业务审批详情页可以优先使用 `WorkflowLayout + WorkflowSidebar` 组合左侧业务内容和右侧流程信息；任务详情页已复用该组件组。此次变更不改变业务审批发起、审批回调、状态回写、流程页面 key、后端公开 API、配置、菜单、权限、租户隔离、启动方式和运行时行为。业务项目继续通过 `WorkflowBusinessApplyApi.create()` 与 `WorkflowProcessApi.start()` 发起流程，并在业务后端自行校验权限、快照和幂等。

- 本次 PR 新增流程定义 `startEntryVisible` 启动入口可见性。业务内嵌流程可声明为“仅业务内嵌”，从审批中心发起流程列表隐藏；业务审批发起、审批回调、状态回写、流程页面 key、权限、租户隔离和业务上下文启动方式不变。业务模块仍应通过 `WorkflowBusinessApplyApi.create()` 与 `WorkflowProcessApi.start()` 按 `definitionKey`、`businessType`、`businessKey` 发起，并继续自行校验业务权限、快照和幂等。

- 本次 PR 新增 `@mango/grid-widgets` 我的申请系统小组件，并新增 `GET /workflow/business-applies/my/summary` 当前登录人申请统计接口；我的申请列表默认同时展示业务申请记录和直接发起的流程实例，并按流程实例 ID 去重，带 `statuses` 状态筛选时使用业务申请分页数据源。业务项目接入工作台后，可通过 `system.my-process` 展示审核中、已完成、已驳回和已撤回申请概览，列表跳转复用现有 `/workflow/task/initiated` 页面。

- Issue #264 发布 `@mango/workflow@1.0.14`、`@mango/workflow-business-example@1.0.13` 并随前端发布批次对齐 `@mango/admin-pages@1.0.11`、`@mango/system@1.0.10`；不改变业务审批发起、审批回调、状态回写、流程页面 key、公开 API、配置、权限、租户隔离、页面验收步骤、启动方式和运行时行为。本次仅同步发布锁和 package 边界，业务项目应成组升级同一批次前端包。
- PR #241 支持业务回传路径：业务系统跳转审批任务详情时可通过 `returnPath`（可选 `returnQuery`）指定审批完成后或点“返回”的落点，任务详情页顶部返回按钮按 `returnPath` 回到业务列表，不带则兜底回 Mango 默认待办/已办；同时精简审批任务详情页布局（流程信息右移、操作按钮条贴底固定）。不改变业务审批发起、审批回调、状态回写、流程页面 key、权限、租户隔离、启动方式和运行时行为。业务项目接入时，跳转 `/workflow/task/detail?taskId=xxx&returnPath=/业务列表` 即可让审批人返回业务上下文。

- 本次 PR 新增 `@mango/grid-widgets` 我的待办系统小组件，并新增 `GET /workflow/tasks/todo/summary` 待办统计接口；业务审批发起、审批回调、状态回写、流程页面 key、流程定义配置、租户隔离和页面验收步骤不变。业务项目接入工作台后，可通过 `system.my-todo` 展示待审批、待处理、待确认和已超时任务概览，列表跳转仍复用现有待办和抄送页面。

- PR #222 对齐 `@mango/numgen`、`@mango/template`、`@mango/workflow` 内部依赖的 `@mango/system` 版本到本地最新发布物料集合；不改变业务审批发起、审批回调、状态回写、流程页面 key、公开 API、配置、权限、租户隔离、页面验收步骤、启动方式和运行时行为。业务项目应成组安装同一批次的本地 `@mango/*` tarball，避免新旧内部依赖混装导致安装解析失败。

- PR #216 加固前端 `@mango/*` npm 包发布边界，非 CLI 包不再发布 `src` 等源码目录，并补充发布包 tarball 和业务消费 typecheck 基线；不改变业务审批发起、审批回调、状态回写、流程页面 key、权限、租户隔离、页面验收步骤、启动方式和运行时行为。业务项目应继续使用公开 package 入口和样式入口，升级到后续发布的新包版本后重新运行前端 typecheck。

- PR #199 将工作流菜单、接口权限和默认资源声明纳入 Resource Registry 注入链路，并修正菜单码/权限码复用风险；不改变业务审批发起、审批回调、状态回写、流程页面 key、权限判断、租户隔离和页面验收步骤。清库重建或 1.0 rebase 升级后，排查工作流菜单、待办接口 403、流程定义或节点定义缺失时，需要同时确认 `AUTH_MENU`、`API_RESOURCE`、`WORKFLOW_CATEGORY`、`WORKFLOW_TEMPLATE_CATEGORY` 和 `WORKFLOW_NODE_DEFINITION` 声明同步成功。
- PR #195 加固前端 `@mango/*` 包的 `exports`、`types` 和生成声明文件，使业务项目通过发布后的 `dist` 产物独立消费；不改变业务审批发起、审批回调、状态回写、流程页面 key、权限、租户隔离、页面验收步骤、启动方式和运行时行为。业务项目应继续使用公开 package 入口和 `./style.css`，不要依赖包内 `src` 路径。
- PR #194 发布资源注册中心版本并升级 `@mango/workflow@1.0.11`、`@mango/workflow-business-example@1.0.11`、`@mango/admin@1.0.23`、`@mango/common@1.0.10`、`@mango/cli@1.0.34` 等前端包；不改变业务审批发起、审批回调、状态回写、流程页面 key、权限、租户隔离、页面验收步骤和运行时行为。业务升级时应成组升级前端 `@mango/*` 包并刷新后端 Mango `1.0.0-SNAPSHOT` 依赖。
- PR #193 新增 `mango-resource` 注册中心并将工作流分类、模板分类、节点定义和消息模板默认数据迁移为资源声明同步；不改变业务审批发起、审批回调、状态回写、流程页面 key、权限、租户隔离和页面验收步骤。排查流程定义或节点定义缺失时，需要同时确认 `WORKFLOW_CATEGORY`、`WORKFLOW_TEMPLATE_CATEGORY` 和 `WORKFLOW_NODE_DEFINITION` 声明是否已同步。
- PR #171 将流程定义列表作为角色数据权限业务接入样例，`workflow:definition:list` 可按显式数据权限过滤；不改变业务审批发起、审批回调、状态回写、页面 key 和租户隔离方式。
- PR #153 Maven revision 支持只调整构建和发布版本解析，不改变业务审批的公开 API、配置、权限、租户、页面和运行时行为。
- PR #157 支付异常单依赖环修复和 workflow API/core 边界收敛只调整内部 Bean 依赖，不改变业务审批接入的公开 API、配置、权限、租户、页面和运行时行为。
- PR 本次持久化基线与 README 发布物料治理只补充业务开发查看 Mango 能力文档的入口，并让 npm 包携带 package README；不改变业务审批发起、审批回调、状态回写、流程页面 key、权限、租户隔离、启动和运行时行为。

- Issue #354 为 Resource Registry 增加资源类型依赖排序，仅改变同一同步批次内 handler 执行顺序，例如 `WORKFLOW_CATEGORY` 和 `WORKFLOW_NODE_DEFINITION` 先于 `WORKFLOW_DEFINITION` 同步；不改变业务审批发起、审批回调、状态回写、流程页面 key、后端公开 API、配置、菜单、权限、租户隔离、启动方式和运行时行为。排查流程定义或节点定义缺失时仍确认对应资源声明同步成功、目标 handler 消费成功以及流程定义发布结果。

- Issue #322 仅放宽 Mango 前端包在当前已认证主版本内的 `peerDependencies` 范围，并明确 `pinia@3`、`vue-i18n@10+`、`vue-router@5` 暂未纳入当前认证范围；不改变业务审批发起、审批回调、状态回写、流程页面 key、后端公开 API、配置、菜单、权限、租户隔离、启动方式和运行时行为。业务项目安装依赖时如出现 peer warning，应先按 `mango-ui/README.md` 的认证范围对齐前端包批次，审批业务异常仍按流程定义、任务运行时接口、权限和业务回调链路排查。

- v2026.07.04-maven-1.0.8-platform-release 未改动工作流业务审批接入链路，不改变业务审批发起、审批回调、状态回写、流程页面 key、后端公开 API、配置、菜单、权限、租户隔离、启动方式和运行时行为。本次首页、通知和文件发布批次不会影响审批业务排障路径，审批异常仍按流程定义、任务运行时接口、权限和业务回调链路排查。

- PR #414 仅收口首页工作台的 `我的待办`、`我的任务`、`我的申请` 小组件权限：卡片同时校验 `workflow:task:list` 和对应工作流页面入口，缺少任一项时首页显示“缺少权限”并禁用查看交互；不改变业务审批发起、审批回调、状态回写、流程页面 key、后端公开 API、配置、菜单、权限、租户隔离、启动方式和运行时行为。业务审批异常仍按流程定义、任务运行时接口、权限和业务回调链路排查。

- v2026.07.07-maven-1.0.13-menu-api-codes-release 仅发布 menuCode/apiCodes 权限模型的 Maven、npm 和 CLI 版本批次；不改变业务审批发起、审批回调、状态回写、流程页面 key、后端公开 API、配置、菜单、权限、租户隔离、启动方式和运行时行为。业务菜单仍应在自身 `apiCodes` 中声明需要复用的 workflow 接口权限，避免因接口权限带出 workflow 或风控菜单。

- v2026.07.11-npm-lock-sync-release 仅同步 `@mango/*` npm 发布批次、CLI/starter 版本锁和包消费者验证；不改变业务审批发起、审批回调、状态回写、流程页面 key、后端公开 API、配置、菜单、权限、租户隔离、启动方式和运行时行为。业务项目应将相关前端包与 `@mango/cli` 成组升级。

- Resource 历史债务治理仅迁移 Workflow Resource Handler/Provider 的本地 SPI 依赖，资源依赖排序和声明内容不变；不改变审批发起、任务处理、回调、状态回写、页面 key、API、菜单、权限、租户隔离和本场景排障步骤。

## 2026-07-20 工作流通知与菜单影响

- 审批中心的菜单入口调整为 `平台能力 / 审批中心`，原 `/workflow` 及子路由、权限码、审批 API、状态回调保持不变。
- `workflow.task.assigned` 通知会按每条当前运行时任务解析直接办理人、候选用户、`ROLE`、`POST`、`ORG` 接收目标；流程已结束或没有有效接收目标时不再投递空接收人通知。`ORG_LEADER` 不会被扩大为整个组织，仍需上游提供明确候选用户或后续增加专用接收目标能力。

## 2026-07-19 前端规范候选影响

- 本次前端规范候选只统一该前端包的组件合同、显式样式入口、Host 请求客户端注入和质量门禁；不改变审批发起、任务处理、回调、状态回写、流程页面 key、后端 API、菜单、权限和租户隔离。业务项目主动升级完整前端包矩阵后，重新执行本指南既有审批闭环即可。

## 2026-07-31 工作流通知跳转影响

- `v2026.07.31-maven-1.0.29-pmo-1.3.7-cli-1.0.93-notice-file-dialog-release` 让工作流通知携带可查看目标：待处理状态进入对应办理页，已完成、驳回或结束状态优先使用流程 `customConfig.viewPath`，缺失或无效时回退到通用任务或申请详情页。该变化只影响通知详情和实时提醒中的主操作，不改变审批发起、任务处理、回调、状态回写、流程页面 key、权限或租户隔离。业务流程如配置自定义查看页，应确认 `viewPath` 已在 Host 注册，并分别验证进行中与终态通知的跳转。

## 2026-08-03 Mango 1.0.31 发布影响

- Mango `1.0.31` 不改变审批发起、任务处理、回调、状态回写、流程页面 key、公开 API、菜单、权限或租户隔离语义。使用 `1.0.30` 或其它 `1.0.3x` 版本的业务项目应成组升级完整 release tuple，先通过独立 Bootstrap 验证 Workflow 相关 Resource 声明，再复验“发起 -> 办理 -> 终态回写 -> 通知跳转”闭环；直接读取 workflow 表或混装旧版单包都不属于该发布矩阵的受支持消费方式。

## 2026-08-06 前端依赖批次发布影响

- 本发布候选成组更新 `@mango/workflow@1.0.40`、`@mango/workflow-business-example@1.0.39` 及其固定前端依赖，但不发布 Maven，也不改变审批发起、任务处理、回调、状态回写、流程页面 key、API、菜单、权限或租户隔离语义。业务项目升级完整 npm 批次后，继续按本指南复验既有审批闭环，不能混装本批次的新旧单包。
