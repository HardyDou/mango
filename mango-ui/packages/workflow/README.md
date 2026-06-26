# @mango/workflow

`@mango/workflow` 是 Mango 审批流前端包。它提供管理后台页面、业务可复用组件和 workflow HTTP API 封装，用于接入后端 `mango-workflow`。

## 1. 概览

这个包包含三类集成形态：

| 标识 | 内容 | 适合谁使用 |
|------|------|------------|
| `admin-pages` | 流程定义、流程模板、任务列表、任务详情、发起流程、自定义申请页面。 | Mango Admin 或业务后台。 |
| `business-component` | 运行时表单、审批进度、审批时间线、业务申请组件注册、业务审批组件注册。 | 需要在业务页面中嵌入审批能力的前端模块。 |
| `api-client` | workflow 后端接口封装和 TypeScript 类型。 | 需要直接调用 workflow 接口的前端代码。 |

`admin-pages` 适配 Mango 管理后台，不适合作为官网、门户站点的通用页面组件。官网类项目如果只需要展示审批进度，应优先使用 `business-component` 中的轨迹组件或自行封装展示层。

## 2. 功能清单

| 能力 | 使用方式 |
|------|----------|
| 注册流程管理页面 | 调用 `registerMangoWorkflowAdminPages()`。 |
| 管理流程定义和流程模板 | 使用注册后的页面 key 和后端 workflow 接口。 |
| 展示待办、已办、抄送和任务详情 | 使用任务列表页、任务详情页或 `workflowApi`。 |
| 展示待办统计 | 使用 `workflowApi.todoSummary()` 读取待审批、待处理、待确认和已超时数量。 |
| 展示我的任务统计 | 使用 `workflowApi.myTaskSummary()` 读取任务总数、待完成、进行中、已完成和已逾期数量。 |
| 展示我的申请统计 | 使用 `workflowApi.businessApplyMySummary()` 读取审核中、已完成、已驳回和已撤回数量。 |
| 发起流程 | 使用发起流程页、自定义申请页或 `workflowApi.startProcess()`。 |
| 渲染动态表单 | 使用 `RuntimeFormRenderer`、`parseRuntimeForm()`、`createDefaultVariables()`。 |
| 接入业务自定义申请页 | 使用 `registerBusinessApplyComponent()`。 |
| 接入业务自定义审批页 | 使用 `registerBusinessApprovalComponent()`。 |
| 展示审批进度和审批记录 | 使用 `WorkflowProgressTree`、`WorkflowApprovalTimeline`、`WorkflowNodeTimeline`。 |

## 3. 集成形态

`admin-pages`：

- 依赖 `@mango/admin-pages` 的页面注册机制。
- 页面 key 需要和后端菜单 `component` 或前端动态路由匹配。
- 页面数据来自后端 `mango-workflow`、`mango-identity`、`mango-domain`、`mango-file` 等接口。

`business-component`：

- 可被业务后台页面复用。
- 业务申请和业务审批组件通过 key 注册，流程定义中的 `applyPageKey`、`approvePageKey` 决定运行时加载哪个组件。
- 组件注册不是权限控制；权限仍由后端任务接口和业务接口校验。

`api-client`：

- 从 `@mango/workflow` 导出 workflow API 类型和请求函数。
- 请求基于 `@mango/common` 的 request 工具。

## 4. 接入方式

安装依赖：

```bash
pnpm add @mango/workflow
```

注册管理后台页面：

```ts
import { registerMangoWorkflowAdminPages } from '@mango/workflow/admin-pages';
import '@mango/workflow/style.css';

registerMangoWorkflowAdminPages();
```

注册业务申请页：

```ts
import { registerBusinessApplyComponent } from '@mango/workflow';
import ContractApplyView from './ContractApplyView.vue';

registerBusinessApplyComponent('contract.apply', {
  title: '合同申请',
  component: ContractApplyView,
});
```

注册业务审批页：

```ts
import { registerBusinessApprovalComponent } from '@mango/workflow';
import ContractApprovalView from './ContractApprovalView.vue';

registerBusinessApprovalComponent('contract.approve', {
  component: ContractApprovalView,
  commentMode: 'BUSINESS_FORM',
  collectVariables: context => ({
    approvedAmount: context.variables.approvedAmount,
  }),
  collectComment: context => context.variables.approvalComment,
});
```

查询业务流程进度：

```ts
import { workflowApi } from '@mango/workflow';

const progress = await workflowApi.businessApplyLatestProgress('contract', contractId);
```

## 5. 快速开始

1. 后端应用启用 `mango-workflow-starter`，并完成 workflow 菜单和权限初始化。
2. 前端应用安装 `@mango/workflow`，在管理后台启动阶段调用 `registerMangoWorkflowAdminPages()`。
3. 在流程定义页面维护流程定义，表单使用动态表单或自定义页面。
4. 动态表单流程直接由 `RuntimeFormRenderer` 渲染。
5. 自定义申请流程在业务包启动阶段调用 `registerBusinessApplyComponent(applyPageKey, registration)`。
6. 自定义审批流程调用 `registerBusinessApprovalComponent(approvePageKey, registration)`。
7. 业务列表需要审批状态时，调用 `workflowApi.businessApplyLatestProgressBatch()` 或后端业务接口聚合结果。

## 6. 配置说明

本包没有独立环境变量。可配置内容来自页面注册、流程定义表单 JSON 和业务组件注册。

| 配置位置 | 字段或参数 | 含义 |
|----------|------------|------|
| `registerMangoWorkflowAdminPages()` | 无入参 | 幂等注册 `mango-workflow` 页面 key 和动态路由。 |
| 流程定义 `startEntryVisible` | `true` / `false` | 是否展示在审批中心发起流程入口；默认 `true`，业务内嵌流程可设为 `false`。 |
| 流程定义 `formJson.mode` | `DYNAMIC_FORM` | 使用动态表单渲染。 |
| 流程定义 `formJson.mode` | `CUSTOM_PAGE` | 使用业务自定义申请页或审批页。 |
| 流程定义 `formJson.customConfig.submitPath` | 路由 path | 自定义申请页路由。 |
| 流程定义 `formJson.customConfig.viewPath` | 路由 path | 自定义查看页路由。 |
| 流程定义 `formJson.customConfig.applyPageKey` | 注册 key | 匹配 `registerBusinessApplyComponent()`。 |
| 流程定义 `formJson.customConfig.approvePageKey` | 注册 key | 匹配 `registerBusinessApprovalComponent()`。 |
| 任务变量 `businessType` 或 `bizType` | 字符串 | 任务详情解析业务类型。 |
| 任务变量 `applyId`、`workflowApplyId`、`businessApplyId`、`snapshotId` | 字符串 | 任务详情解析业务申请记录 ID。 |
| 任务变量 `businessPermissions` | 字段权限对象 | 字段级权限，支持 `HIDDEN`、`READONLY`、`EDITABLE`。 |
| 待办列表查询 | `todoType` | `ASSIGNED` 查询待审批，`CLAIMABLE` 查询待处理，`ALL` 查询全部待办。 |
| 待办列表查询 | `overdue` | 为 `true` 时只查询已超时待办。 |
| 抄送列表查询 | `unread` | 为 `true` 时只查询未读抄送。 |

`parseWorkflowFormConfig()` 支持数组式表单 JSON，也支持包含 `mode`、`rules`、`fields`、`customConfig` 的对象结构。非法 JSON 会按空动态表单处理。

## 7. API 与扩展

页面注册入口：

| 导出 | 来源 | 作用 |
|------|------|------|
| `registerMangoWorkflowAdminPages()` | `@mango/workflow/admin-pages` | 注册 workflow 管理页面和动态路由。 |

页面 key：

| 页面 key | 用途 |
|----------|------|
| `workflow/definition/index` | 流程定义管理。 |
| `system/workflow-definition/index` | 流程定义兼容入口。 |
| `workflow/template/index` | 流程模板管理。 |
| `workflow-template/index` | 流程模板兼容入口。 |
| `workflow/task/todo/index` | 待办任务。 |
| `workflow/task/initiated/index` | 已发起任务入口。 |
| `workflow/task/done/index` | 已办任务。 |
| `workflow/task/copied/index` | 抄送任务。 |
| `workflow/task-list/index` | 通用任务列表。 |
| `workflow/task/detail/index` | 任务详情。 |
| `workflow/start-process/index` | 发起流程。 |
| `workflow/custom-apply/index` | 自定义申请容器页。 |

组件导出：

| 导出 | 标识 | 作用 |
|------|------|------|
| `WorkflowDefinitionView` | `admin-pages` | 流程定义管理页面。 |
| `WorkflowTemplateView` | `admin-pages` | 流程模板管理页面。 |
| `WorkflowTaskListView` | `admin-pages` | 任务列表页面。 |
| `WorkflowTaskDetailView` | `admin-pages` | 任务详情页面。 |
| `WorkflowStartProcessView` | `admin-pages` | 发起流程页面。 |
| `WorkflowCustomApplyView` | `admin-pages` | 自定义申请容器页面。 |
| `RuntimeFormRenderer` | `business-component` | 动态表单渲染组件。 |
| `WorkflowProgressTree` | `business-component` | 审批进度树。 |
| `WorkflowApprovalTimeline` | `business-component` | 审批时间线。 |
| `WorkflowNodeTimeline` | `business-component` | 节点时间线。 |

业务扩展导出：

| 导出 | 作用 |
|------|------|
| `registerBusinessApplyComponent()`、`registerBusinessApplyComponents()` | 注册一个或多个业务申请组件。 |
| `resolveBusinessApplyRegistration()` | 按 key 查询业务申请组件注册信息。 |
| `registerBusinessApprovalComponent()`、`registerBusinessApprovalComponents()` | 注册一个或多个业务审批组件。 |
| `resolveBusinessApprovalRegistration()`、`resolveBusinessApprovalComponent()` | 按 key 查询业务审批组件注册信息。 |
| `collectBusinessApprovalVariables()` | 动作提交前采集业务审批变量。 |
| `collectBusinessApprovalComment()` | 动作提交前采集审批意见。 |
| `businessTypeOf()` | 从任务变量解析业务类型。 |
| `applyIdOf()` | 从任务变量解析申请 ID。 |
| `businessPermissionsOf()` | 从任务变量解析字段权限。 |

主要 API 封装：

| 分类 | 方法 |
|------|------|
| 分类和定义 | `categoriesPage()`、`categoriesList()`、`definitionDetail()`、`definitionsPage()`、`saveDefinition()`、`updateDefinition()`、`deleteDefinition()`、`deployDefinition()`、`nodeCatalog()` |
| 模板 | `templatesPage()`、`templateDetail()`、`saveTemplate()`、`deleteTemplate()`、`createTemplateFromDefinition()`、`createDefinitionFromTemplate()`、`importTemplates()`、`pushTemplates()` |
| 任务 | `todoTasks()`、`todoSummary()`、`myTaskSummary()`、`initiatedTasks()`、`doneTasks()`、`copiedTasks()`、`taskDetail()` |
| 动作 | `completeTask()`、`rejectTask()`、`saveTask()`、`transferTask()`、`addSignTask()`、`claimTask()`、`unclaimTask()`、`readCopiedTask()` |
| 流程实例 | `startProcess()`、`initiatedProcesses()`、`processHistoryByBusinessKey()`、`processDetail()` |
| 业务申请 | `createBusinessApply()`、`businessAppliesPage()`、`businessApplyMySummary()`、`businessApplyDetail()`、`businessApplyHistory()`、`businessApplyLatestProgress()`、`businessApplyLatestProgressBatch()`、`businessApplyByProcessInstance()` |
| 候选项 | `users()`、`tenants()`、`enabledDomains()` |

## 8. 数据与初始化

这个前端包不包含数据库 migration，也不初始化菜单。

| 数据 | 来源 |
|------|------|
| 流程分类、定义、版本、模板、任务、业务申请记录 | 后端 `mango-workflow`。 |
| workflow 菜单和权限 | 后端 migration 写入 `authorization_menu`。 |
| 用户候选项 | 后端 `/identity/users/page`。 |
| 业务域候选项 | 后端 `/domain/domains/enabled-tree`。 |
| 文件上传、图片上传、附件预览 | `@mango/file` 和后端 `mango-file`。 |

## 9. 管理入口

后端默认写入的菜单 component 是：

| 菜单 | 后端菜单 component | 前端页面 key |
|------|-------------------|--------------|
| 流程模板 | `@/views/workflow/template/index.vue` | `workflow/template/index` |
| 流程定义 | `@/views/workflow/definition/index.vue` | `workflow/definition/index` |

`registerMangoWorkflowAdminPages()` 还注册两个动态路由：

| 路由 | 页面 key | 权限码 |
|------|----------|--------|
| `/workflow/task/detail` | `workflow/task/detail/index` | `workflow:task:detail` |
| `/workflow/custom-apply` | `workflow/custom-apply/index` | `workflow:custom-apply` |

常用权限码来自后端 workflow 菜单和按钮权限，例如 `workflow:definition:list`、`workflow:definition:deploy`、`workflow:template:list`、`workflow:task:list`、`workflow:task:complete`。

待办任务相关公开 API：

| API | 后端接口 | 说明 |
|-----|----------|------|
| `workflowApi.todoTasks(params)` | `GET /workflow/tasks/todo` | 支持 `todoType` 和 `overdue` 筛选。 |
| `workflowApi.todoSummary()` | `GET /workflow/tasks/todo/summary` | 返回 `WorkflowTaskSummary`，字段为 `pendingApproval`、`pendingHandle`、`pendingConfirm`、`overdue`。 |
| `workflowApi.myTaskSummary()` | `GET /workflow/tasks/my/summary` | 返回 `WorkflowMyTaskSummary`，字段为 `total`、`pending`、`processing`、`completed`、`overdue`。 |
| `workflowApi.copiedTasks(params)` | `GET /workflow/tasks/copied` | 支持 `unread` 筛选未读抄送。 |
| `workflowApi.businessApplyMySummary()` | `GET /workflow/business-applies/my/summary` | 返回 `WorkflowBusinessApplySummary`，字段为 `inReview`、`completed`、`rejected`、`withdrawn`。 |
| `workflowApi.businessAppliesPage(params)` | `POST /workflow/business-applies/page` | 我的申请页面复用该接口，支持 `statuses` 筛选申请状态。 |

## 10. 问题排查

**菜单打开后提示页面不存在**

确认已经调用 `registerMangoWorkflowAdminPages()`，并引入 `@mango/workflow/style.css`。

**发起流程列表为空**

检查后端流程定义是否已发布，当前租户是否有可用定义，当前账号是否有流程定义或发起流程权限。声明为“仅业务内嵌”的流程不会显示在审批中心发起流程列表中，需要从业务页面按业务上下文发起。

**自定义申请页打不开**

检查流程定义 `formJson.mode` 是否为 `CUSTOM_PAGE`，`customConfig.applyPageKey` 是否和 `registerBusinessApplyComponent()` 的 key 一致。

**任务详情找不到业务审批组件**

检查 `approvePageKey`、任务变量中的 `businessType`，以及业务包是否已经执行 `registerBusinessApprovalComponent()`。

**字段权限不生效**

检查任务变量 `businessPermissions` 是否按字段 key 输出 `HIDDEN`、`READONLY`、`EDITABLE`。前端字段权限只控制渲染，提交后的业务校验仍应由后端完成。

**上传或图片字段不可用**

确认前端安装并打包了 `@mango/file`，后端启用了 `mango-file`，表单字段类型为 `upload` 或 `imageUpload`。

## 11. 相关文档

- [Workflow 后端 README](../../../mango/mango-platform/mango-workflow/README.md)
- [@mango/workflow 组件 README](src/components/README.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
