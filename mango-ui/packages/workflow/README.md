# @mango/workflow

## 1. 概览
`@mango/workflow` 是 Mango 工作流前端包，提供流程分类、流程定义、模板、发起流程、待办、已办、抄送、任务详情、运行时表单和业务自定义申请/审批组件注册能力。

它面向后台业务开发者：用现成页面管理流程，用注册扩展点把业务申请单和审批详情接入流程任务。流程引擎、流程实例、任务流转和业务状态回写由后端 `mango-workflow` 承担。

## 2. 功能清单

| 能力 | 常用入口 |
|------|----------|
| 在 Mango Admin 中维护流程分类、流程定义、流程模板和节点配置 | 前端注册 / 组件 / API 封装 |
| 业务后台展示发起流程、待办、已办、我发起的、抄送和任务详情页面 | 前端注册 / 组件 / API 封装 |
| 使用动态表单模式渲染流程表单字段 | 前端注册 / 组件 / API 封装 |
| 使用自定义页面模式，把业务申请页和审批详情页注册到 workflow | 前端注册 / 组件 / API 封装 |
| 在业务列表中查询某条业务数据的最新流程进度或历史流程 | 前端注册 / 组件 / API 封装 |

## 3. 适用场景
- 在 Mango Admin 中维护流程分类、流程定义、流程模板和节点配置。
- 业务后台展示发起流程、待办、已办、我发起的、抄送和任务详情页面。
- 使用动态表单模式渲染流程表单字段。
- 使用自定义页面模式，把业务申请页和审批详情页注册到 workflow。
- 在业务列表中查询某条业务数据的最新流程进度或历史流程。

## 4. 边界说明
- 不实现 Flowable 或后端流程引擎。
- 不保存业务申请单主数据；示例包中的本地数据只用于演示。
- 不替代业务模块自己的领域校验、状态机和数据权限。
- 不负责菜单、权限、租户、流程定义和模板初始化。

## 5. 模块组成
本包只提供 Vue 页面、运行时组件、API 封装、页面注册入口和业务组件注册表。

后端边界：

- `mango-workflow`：流程分类、定义、模板、任务、流程实例、业务申请进度。
- `mango-identity`：用户候选项。
- `mango-system`：租户、业务域相关数据。
- `mango-domain`：流程分类和定义可绑定的业务域树。
- `mango-file`：流程图标、上传字段和附件预览。

业务模块边界：业务申请表、审批详情、业务变量采集、动作前后钩子、业务状态回写由业务包自己实现，并通过本包注册。

## 6. 接入方式
依赖包：

```json
{
  "dependencies": {
    "@mango/workflow": "1.0.9"
  }
}
```

注册工作流页面：

```ts
import { registerMangoWorkflowAdminPages } from '@mango/workflow/admin-pages';
import '@mango/workflow/style.css';

registerMangoWorkflowAdminPages();
```

注册业务申请页和审批页：

```ts
import {
  registerBusinessApplyComponent,
  registerBusinessApprovalComponent,
} from '@mango/workflow';
import ContractApplyView from './ContractApplyView.vue';
import ContractApprovalView from './ContractApprovalView.vue';

registerBusinessApplyComponent('contract.apply', {
  title: '合同申请',
  component: ContractApplyView,
});

registerBusinessApprovalComponent('contract.approve', {
  component: ContractApprovalView,
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

## 7. 配置说明
本包没有独立 Vite 环境变量。配置来自页面注册、流程定义表单 JSON、节点扩展配置、业务组件注册和后端 API。

| 配置位置 | 字段 / 参数 | 含义 |
|----------|-------------|------|
| `registerMangoWorkflowAdminPages()` | 无入参 | 幂等注册 `mango-workflow` 页面 key 和动态路由。 |
| 流程定义 `formJson` | `mode` | `DYNAMIC_FORM` 使用运行时表单；`CUSTOM_PAGE` 使用业务自定义申请页。 |
| 流程定义 `formJson.customConfig` | `submitPath` | 自定义申请页路由。 |
| 流程定义 `formJson.customConfig` | `viewPath` | 自定义查看页路由。 |
| 流程定义 `formJson.customConfig` | `applyPageKey` | `registerBusinessApplyComponent()` 使用的申请组件 key。 |
| 流程定义 `formJson.customConfig` | `approvePageKey` | `registerBusinessApprovalComponent()` 使用的审批组件 key。 |
| 节点变量 | `businessType` / `bizType` | 任务详情解析业务审批组件的业务类型。 |
| 节点变量 | `applyId` / `workflowApplyId` / `businessApplyId` / `snapshotId` | 任务详情解析业务申请记录 id。 |
| 节点变量 | `businessPermissions` | 字段级权限，支持 `HIDDEN`、`READONLY`、`EDITABLE`。 |

`parseWorkflowFormConfig()` 兼容旧数组式表单 JSON，也支持对象结构；非法 JSON 会退回空的动态表单配置。

## 8. API 与扩展
页面注册入口：

- `registerMangoWorkflowAdminPages()`

页面 key：

| 页面 key | 能力 |
|----------|------|
| `workflow/definition/index` | 流程定义和设计器。 |
| `system/workflow-definition/index` | 流程定义兼容页面 key。 |
| `workflow/template/index` | 流程模板。 |
| `workflow-template/index` | 流程模板兼容页面 key。 |
| `workflow/task/todo/index` | 待办任务。 |
| `workflow/task/initiated/index` | 我发起的流程。 |
| `workflow/task/done/index` | 已办任务。 |
| `workflow/task/copied/index` | 抄送任务。 |
| `workflow/task-list/index` | 任务列表兼容页面 key。 |
| `workflow/task/detail/index` | 任务详情。 |
| `workflow/start-process/index` | 发起流程。 |
| `workflow/custom-apply/index` | 自定义申请容器页。 |

组件导出：

- `RuntimeFormRenderer`
- `WorkflowProgressTree`
- `WorkflowApprovalTimeline`
- `WorkflowNodeTimeline`

业务扩展导出：

- `registerBusinessApplyComponent()`、`registerBusinessApplyComponents()`
- `resolveBusinessApplyRegistration()`
- `registerBusinessApprovalComponent()`、`registerBusinessApprovalComponents()`
- `resolveBusinessApprovalRegistration()`、`resolveBusinessApprovalComponent()`
- `collectBusinessApprovalVariables()`、`collectBusinessApprovalComment()`
- `businessTypeOf()`、`applyIdOf()`、`businessPermissionsOf()`

主要 API：

- 分类和定义：`categoriesPage()`、`categoriesList()`、`definitionsPage()`、`definitionDetail()`、`deployDefinition()`、`nodeCatalog()`。
- 模板：`templatesPage()`、`createTemplateFromDefinition()`、`createDefinitionFromTemplate()`、`importTemplates()`、`pushTemplates()`。
- 任务：`todoTasks()`、`initiatedTasks()`、`doneTasks()`、`copiedTasks()`、`taskDetail()`。
- 动作：`completeTask()`、`rejectTask()`、`saveTask()`、`transferTask()`、`addSignTask()`、`claimTask()`、`unclaimTask()`、`readCopiedTask()`。
- 流程实例：`startProcess()`、`initiatedProcesses()`、`processHistoryByBusinessKey()`、`processDetail()`。
- 业务申请：`businessAppliesPage()`、`businessApplyHistory()`、`businessApplyLatestProgress()`、`businessApplyLatestProgressBatch()`、`businessApplyByProcessInstance()`。
- 候选项：`users()`、`tenants()`、`enabledDomains()`。

## 9. 数据与初始化
本包不包含数据库 migration。

| 数据 | 来源 |
|------|------|
| 流程分类、定义、版本、模板、任务、业务申请记录 | 后端 `mango-workflow`。 |
| 菜单和权限 | 后端 `mango-authorization`。 |
| 用户、组织、角色、岗位候选项 | 后端 `mango-identity`、`mango-org`、`mango-authorization`。 |
| 业务域 | 后端 `mango-domain`。 |
| 流程图标、上传字段、附件 | 后端 `mango-file`。 |

## 10. 管理入口
前端页面 key 必须与 authorization 菜单 component 一致。任务列表、任务详情和动作提交由后端按当前用户、候选人、任务归属、租户和流程实例状态校验。

业务组件注册不是权限控制。即使前端注册了某个审批组件，后端仍必须校验：

- 当前用户是否能查看任务。
- 当前用户是否能执行完成、驳回、转办、加签、签收和退签。
- 当前租户是否能访问流程定义和业务数据。
- 业务变量和业务状态变更是否合法。

## 11. 快速开始
1. 后端启用 `mango-workflow`，准备流程菜单权限。
2. 前端启动时调用 `registerMangoWorkflowAdminPages()`。
3. 业务包实现申请组件和审批组件，调用 `registerBusinessApplyComponent()`、`registerBusinessApprovalComponent()`。
4. 流程定义中把 `formJson.mode` 设置为 `CUSTOM_PAGE`，并配置 `applyPageKey`、`approvePageKey`。
5. 发起流程时业务组件保存业务申请单，并调用后端流程启动接口或让工作流页面提交 `startProcess()`。
6. 审批详情组件通过 `context.variables` 和 `context.permissions` 渲染字段，提交前用 `collectVariables` 和 `collectComment` 回传审批变量和意见。
7. 验证不同节点的字段权限、动作权限和业务状态回写。

## 12. 问题排查
- 发起流程列表为空：检查流程定义状态、发布状态、分类状态和租户权限。
- 自定义申请页打不开：检查 `formJson.customConfig.submitPath` 和 `applyPageKey`，以及业务包是否在 Shell 启动阶段注册。
- 任务详情找不到业务审批组件：检查 `businessType`、`approvePageKey`、注册 key 和业务包加载顺序。
- 字段权限不生效：检查节点变量 `businessPermissions` 是否按任务节点 key 输出 `HIDDEN`、`READONLY`、`EDITABLE`。
- 上传字段不可用：检查 `@mango/file` 样式、后端文件服务和字段类型配置。

## 13. 相关文档
- [前端模块规范](../../../mango-pmo/rules/frontend/01-vue-code.md)
- [前端测试规范](../../../mango-pmo/rules/frontend/04-test.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 14. 历史资料
- [Workflow 后端 README](../../../mango/mango-platform/mango-workflow/README.md)
- [@mango/workflow 组件 README](src/components/README.md)
- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
