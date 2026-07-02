# 业务审批接入

## 1. 适用场景

业务单据需要发起审批，审批结束后回写业务状态，并能在业务页面查看流程进度。

## 2. 阅读顺序

| 顺序 | 文档 | 关注点 |
|------|------|--------|
| 1 | [Workflow 后端 README](../../../mango/mango-platform/mango-workflow/README.md) | 流程定义、实例、任务、事件和配置 |
| 2 | [@mango/workflow README](../../../mango-ui/packages/workflow/README.md) | 流程页面、设计器、任务 API |
| 3 | [Workflow Example README](../../../mango-ui/packages/workflow-business-example/README.md) | 业务接入示例和页面 key |
| 4 | [能力地图：业务审批闭环](../../capabilities/README.md#3-组合接入入口) | 组合验证入口 |

## 3. 接入检查点

| 环节 | 检查点 |
|------|--------|
| 业务状态 | 业务单据状态区分草稿、审批中、通过、驳回、撤回等业务语义 |
| 流程定义 | 业务类型、流程 key、表单编码和版本关系清晰 |
| 发起审批 | 业务保存和流程发起的事务边界可解释，失败时能回滚或补偿 |
| 审批回调 | 监听流程完成、驳回、撤回等事件并回写业务状态 |
| 页面入口 | 业务详情页展示流程进度、当前任务和审批记录 |
| 返回入口 | 业务跳转审批任务详情时传 `returnPath`，审批完成或点返回能回到业务列表，不回退到 Mango 默认待办 |
| 权限 | 发起、审批、撤回、查看记录按业务角色和流程任务共同判断 |

## 4. 最小闭环

1. 新建业务单据并保存为草稿。
2. 发起审批后业务状态变为审批中。
3. 审批人能在任务列表看到待办。
4. 审批通过后业务状态变为通过。
5. 业务详情页能看到流程实例和审批记录。

## 5. 常见失败

| 现象 | 优先检查 |
|------|----------|
| 发起审批后没有待办 | 流程定义版本、节点办理人表达式、当前租户和组织数据 |
| 业务状态不更新 | 事件监听、回写服务、业务 ID 与流程 businessKey 映射 |
| 审批通过后业务侧仍显示上一节点 | 是否误用 `workflow.task.completed` 同步当前任务；当前任务刷新应使用 `workflow.task.advanced` 或 `complete-result` |
| 审批页打开空白 | 前端 workflow 包是否引入，页面 key 是否注册，接口是否 401/403 |
| 驳回后业务不可再次提交 | 业务状态流转是否覆盖驳回到草稿或重新提交 |
| 退回后业务侧仍显示原审批节点 | 业务侧是否使用 `POST /workflow/tasks/return` 响应或 `workflow.task.advanced` 同步刷新后的 `currentTasks` |
| 多租户流程串数据 | 流程定义、实例、任务和业务表 tenantId 是否一致 |

## 6. 事件接入

业务模块可以通过 workflow 事件异步回写业务状态，也可以在审批页调用任务接口同步拿到刷新结果。选择方式如下：

| 业务目标 | 推荐方式 |
|----------|----------|
| 审批按钮点击后立即刷新当前节点、当前办理人和页面按钮状态 | 调用 `POST /workflow/tasks/complete-result` |
| 审批退回后立即刷新当前节点、当前办理人和页面按钮状态 | 调用 `POST /workflow/tasks/return` |
| 审批中同步下一节点办理人、业务列表当前节点、待办摘要 | 订阅 `workflow.task.advanced` |
| 审计刚完成的任务和办理意见 | 订阅 `workflow.task.completed` |
| 流程通过后回写业务通过状态 | 订阅 `workflow.process.completed` |
| 流程驳回后回写业务驳回状态 | 订阅 `workflow.process.rejected` |

`workflow.task.completed` 和 `workflow.task.advanced` 的差异：

| 事件 | 当前任务表是否已刷新 | 适合用途 |
|------|----------------------|----------|
| `workflow.task.completed` | 否 | 记录当前任务完成动作。 |
| `workflow.task.advanced` | 是 | 同步下一节点或退回目标节点、当前办理人和业务进度。 |

`POST /workflow/tasks/return` 会把当前任务退回到最近一个已完成的不同用户任务节点，或退回到 `targetTaskDefinitionKey` 指定的历史节点。串行流程可以不传目标节点；并行、多实例、重复审批节点或业务语义固定的流程，应在流程节点动作配置或业务审批页中显式传入 `targetTaskDefinitionKey`。接口返回结构与 `complete-result` 一致，业务侧应使用返回的 `currentTasks` 或订阅 `workflow.task.advanced` 刷新业务单据当前节点和当前办理人；退回不会发布 `workflow.task.completed`，也不会把流程状态改为驳回。

单体多实例、微服务或微服务多实例部署时，事件应按至少一次投递处理。业务订阅方使用 `eventId`、`processInstanceId + completedTaskId` 或业务主键构造幂等键，避免重复回写状态、重复发通知或重复生成待办摘要。

## 7. 变更影响记录

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

- PR #356 新增 `WORKFLOW_DEFINITION` 资源声明处理器和 `WorkflowTaskRuntimeApi` 公共任务运行时 API。业务模块可通过资源声明随模块同步流程定义，也可依赖 `mango-workflow-api` 调用待办、已办、抄送、详情、签收、办理、驳回、保存、转办、加签和流程详情能力；既有 `WorkflowBusinessApplyApi.create()` 与 `WorkflowProcessApi.start()` 审批发起方式保持兼容，不改变流程页面 key、菜单、权限码、租户隔离、业务状态回写和页面验收入口。

- PR #295 只治理 Issue #183 后端测试规范、Mockito 审计和 workflow core service 集成测试；不改变业务审批发起、审批回调、状态回写、流程页面 key、后端公开 API、配置、菜单、权限、租户隔离、启动方式和运行时行为。

- Issue #233 明确审批任务完成后的流程推进时序：业务模块同步下一节点待办、当前办理人或业务状态时，使用 `workflow.task.advanced` 或 `POST /workflow/tasks/complete-result`；`workflow.task.completed` 只表示当前任务完成，不承诺当前任务快照已刷新。

- Issue #296 新增审批退回能力：`POST /workflow/tasks/return` 使用 `workflow:task:return` 权限，支持退回到最近历史用户任务节点或 `targetTaskDefinitionKey` 指定历史节点，并返回刷新后的当前任务快照。退回语义和驳回终止不同；业务模块可用接口响应或 `workflow.task.advanced` 同步业务当前节点、当前办理人和待办摘要。

- v2026.06.27-workflow-history-dialog-release 发布 `@mango/workflow@1.0.17`、`@mango/admin-shell@1.0.29`、`@mango/grid-widgets@1.0.6`、`@mango/workflow-business-example@1.0.16`、`@mango/admin@1.0.33` 和 `@mango/cli@1.0.46`，仅对齐工作流历史弹窗标题修复的 npm 物料、聚合包和 CLI/starter 版本锁；不改变业务审批发起、审批回调、状态回写、流程页面 key、后端公开 API、配置、菜单、权限、租户隔离、启动方式和运行时行为。

- 本次 PR 修复 `WorkflowInstanceHistoryDialog` 内部历史申请标题重复显示问题，并为 `WorkflowInstanceHistory` 增加 `showTitle` 展示开关。该开关默认 `true`，业务页面直接使用历史申请组件时标题行为不变；内置弹窗会关闭组件内部标题，只保留弹窗标题。此次变更不改变业务审批发起、审批回调、状态回写、流程页面 key、后端公开 API、配置、菜单、权限、租户隔离、启动方式和运行时行为。

- Issue #275 修复标准审批任务详情页对流程定义管理接口的隐式依赖。办理人打开 `/workflow/task/detail` 或业务包装后的任务详情页时，页面只依赖任务详情、流程实例、业务申请、`formJson`、运行时变量、`renderConfig` 和可选运行时 `designerJson`；不再主动调用流程定义列表、详情或版本管理接口。业务项目不需要给普通审批办理人额外授予流程定义管理权限；若运行时详情未携带 `designerJson`，页面会降级展示审批记录，不阻断业务表单和审批操作。

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
