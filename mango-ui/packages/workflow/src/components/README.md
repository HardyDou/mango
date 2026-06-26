# @mango/workflow Components

本入口说明 `@mango/workflow` 的公共组件和业务扩展点。业务开发者主要用它做三件事：渲染动态表单、展示流程轨迹、把业务申请页和审批页注册给 workflow。

## 1. 概览

这里的能力都属于 `business-component`，可以被业务后台页面复用。它不提供菜单注册；菜单和页面注册请使用包根目录 README 中的 `admin-pages` 入口。

## 2. 功能清单

| 能力 | 导出 |
|------|------|
| 动态表单渲染 | `RuntimeFormRenderer` |
| 解析动态表单 JSON | `parseRuntimeForm()` |
| 生成表单默认变量 | `createDefaultVariables()` |
| 解析流程表单配置 | `parseWorkflowFormConfig()` |
| 解析自定义申请路由 | `customApplyRouteOf()` |
| 注册业务申请组件 | `registerBusinessApplyComponent()`、`registerBusinessApplyComponents()` |
| 解析业务申请组件 | `resolveBusinessApplyRegistration()` |
| 注册业务审批组件 | `registerBusinessApprovalComponent()`、`registerBusinessApprovalComponents()` |
| 解析业务审批组件 | `resolveBusinessApprovalRegistration()`、`resolveBusinessApprovalComponent()` |
| 采集审批变量和意见 | `collectBusinessApprovalVariables()`、`collectBusinessApprovalComment()` |
| 解析任务变量 | `businessTypeOf()`、`applyIdOf()`、`businessPermissionsOf()` |
| 展示审批进度 | `WorkflowProgressTree`、`WorkflowDefinitionGraph` |
| 展示审批时间线 | `WorkflowApprovalTimeline`、`WorkflowNodeTimeline`、`WorkflowInstanceProgress` |
| 组合工作流详情布局 | `WorkflowLayout`、`WorkflowSidebar`、`WorkflowInstanceSummary` |
| 展示历史申请记录 | `WorkflowInstanceHistory`、`WorkflowInstanceHistoryDialog` |
| 弹出流程图 | `WorkflowDefinitionGraphDialog` |

## 3. 接入方式

动态表单：

```vue
<script setup lang="ts">
import { reactive } from 'vue';
import { RuntimeFormRenderer, createDefaultVariables, parseRuntimeForm } from '@mango/workflow';

const { fields } = parseRuntimeForm(definition.formJson);
const model = reactive(createDefaultVariables(fields));
</script>

<template>
  <RuntimeFormRenderer
    :fields="fields"
    :model="model"
    :permissions="fieldPermissions"
  />
</template>
```

注册业务申请页：

```ts
import { registerBusinessApplyComponent } from '@mango/workflow';
import ExpenseApplyView from './ExpenseApplyView.vue';

registerBusinessApplyComponent('workflow.expense.apply', {
  title: '费用报销申请',
  component: ExpenseApplyView,
});
```

注册业务审批页：

```ts
import { registerBusinessApprovalComponent } from '@mango/workflow';
import ExpenseApprovalView from './ExpenseApprovalView.vue';

registerBusinessApprovalComponent('workflow.expense.approve.finance', {
  component: ExpenseApprovalView,
  commentMode: 'BUSINESS_FORM',
  collectVariables: context => ({
    approvedAmount: context.variables.approvedAmount,
  }),
  collectComment: context => context.variables.financeOpinion,
  validateBeforeAction: async (context, action) => {
    if (action === 'complete' && !context.variables.financeOpinion) {
      throw new Error('请填写财务意见');
    }
  },
});
```

业务详情布局：

```vue
<script setup lang="ts">
import { WorkflowLayout, WorkflowSidebar } from '@mango/workflow';
</script>

<template>
  <WorkflowLayout title="合同用印详情" @back="$router.back()">
    <template #default>
      <DocumentTableApprovalDetail :context="context" />
    </template>
    <template #sidebar>
      <WorkflowSidebar
        :summary="summary"
        :node="definitionNode"
        :current-node-key="currentNodeKey"
        :visited-node-keys="visitedNodeKeys"
        :status="status"
        :records="records"
        :business-type="businessType"
        :business-key="businessKey"
      />
    </template>
  </WorkflowLayout>
</template>
```

## 4. 工作流业务面板用法

业务模块需要在自己的详情页中承载审批能力时，优先使用 `WorkflowLayout` + `WorkflowSidebar`：

- `WorkflowLayout` 负责页面骨架：顶部 header、右侧返回按钮、header 和 body 之间的灰色分割线、左侧业务内容区、右侧审批区。
- 默认插槽放业务自己的详情、申请表单或动态表单。
- `sidebar` 插槽放 `WorkflowSidebar`，用于展示流程基础信息、当前审批进度，以及打开流程图和历史申请弹窗。
- `WorkflowSidebar` 顶部两个图标按钮分别打开 `WorkflowDefinitionGraphDialog` 和 `WorkflowInstanceHistoryDialog`。流程图弹窗展示流程定义节点图，历史申请弹窗展示同一业务单据的历史申请记录。

推荐结构：

```vue
<script setup lang="ts">
import {
  WorkflowLayout,
  WorkflowSidebar,
  type WorkflowInstanceSummaryInfo,
  type WorkflowInstanceStatus,
} from '@mango/workflow';

const summary: WorkflowInstanceSummaryInfo = {
  currentNodeName: '部门负责人审批',
  status: '运行中',
  initiatorName: 'admin',
  assigneeName: 'admin',
  startTime: '2026-06-26 18:29:21',
};

const status: WorkflowInstanceStatus = 'running';
</script>

<template>
  <WorkflowLayout title="合同用印详情" @back="$router.back()">
    <ContractSealDetailForm />

    <template #sidebar>
      <WorkflowSidebar
        :summary="summary"
        :node="definitionNode"
        :current-node-key="currentNodeKey"
        :visited-node-keys="visitedNodeKeys"
        :status="status"
        :records="approvalRecords"
        business-type="CONTRACT_SEAL"
        :business-key="contractId"
      />
    </template>
  </WorkflowLayout>
</template>
```

如果业务表单来自流程动态表单，可以把 `RuntimeFormRenderer` 放进 `WorkflowLayout` 默认插槽：

```vue
<WorkflowLayout title="审批详情" @back="$router.back()">
  <RuntimeFormRenderer
    :fields="fields"
    :model="variables"
    :permissions="permissions"
    readonly
  />

  <template #sidebar>
    <WorkflowSidebar
      :summary="summary"
      :node="definitionNode"
      :current-node-key="taskDefinitionKey"
      :visited-node-keys="visitedNodeKeys"
      :status="status"
      :records="records"
      :business-type="businessType"
      :business-key="businessKey"
    />
  </template>
</WorkflowLayout>
```

`WorkflowSidebar` 常用 props：

| prop | 含义 |
|------|------|
| `summary` | 顶部流程基础信息，包含当前节点、状态、发起人、办理人、开始时间。 |
| `node` | 流程定义节点树，用于流程图弹窗和节点图渲染。 |
| `currentNodeKey` | 当前节点 key，用于高亮当前节点。 |
| `visitedNodeKeys` | 已流转节点 key 列表，用于标记已完成路径。 |
| `status` | 流程状态，支持 `running`、`completed`、`rejected`、`cancelled`、`pending`。 |
| `records` | 当前流程实例审批记录。 |
| `businessType` | 业务类型，用于查询历史申请。 |
| `businessKey` | 业务主键，用于查询历史申请。 |
| `mode` | 侧栏内容模式：`PROGRESS` 展示节点进度，`APPROVAL_RECORDS` 展示审批记录，`CUSTOM` 使用插槽，`HIDDEN` 隐藏下方内容。 |

扩展插槽：

| 插槽 | 用途 |
|------|------|
| `summary-extra` | 在流程基础信息后追加业务信息。 |
| `default` | `mode="CUSTOM"` 时渲染自定义审批区内容。 |
| `history-summary` | 历史申请弹窗顶部摘要区。 |
| `history-record-extra` | 历史申请单条记录扩展内容。 |

## 5. 参数与事件

`RuntimeFormRenderer` props：

| prop | 类型 | 含义 |
|------|------|------|
| `fields` | `RuntimeFormField[]` | 表单字段，通常来自 `parseRuntimeForm()`。 |
| `model` | `Record<string, any>` | 表单变量对象。 |
| `readonly` | `boolean` | 是否整体只读。 |
| `labelWidth` | `string` | 表单标签宽度，默认 `96px`。 |
| `permissions` | `Record<string, 'HIDDEN' \| 'READONLY' \| 'EDITABLE'>` | 字段级权限。 |

`RuntimeFormField` 常用字段：

| 字段 | 含义 |
|------|------|
| `key` | 字段 key，对应 `model` 中的变量名。 |
| `label` | 字段标题。 |
| `type` | 字段类型。 |
| `placeholder` | 占位提示。 |
| `readonly` | 字段只读。 |
| `options` | select、radio、checkbox 等选项。 |
| `treeOptions` | treeSelect、cascader 等树形选项。 |
| `props` | 透传配置。 |
| `rules` | 表单校验规则。 |
| `defaultValue` | 默认值。 |
| `children` | 容器字段子字段。 |

支持的字段类型：

```text
input
textarea
password
number
select
radio
checkbox
switch
date
daterange
time
timerange
datetime
datetimerange
rate
slider
color
cascader
treeSelect
transfer
upload
imageUpload
editor
systemUser
systemOrg
systemDept
systemPost
systemRole
systemDict
businessType
signature
serialNo
alert
text
html
divider
tag
image
button
container
```

`BusinessApplyRegistration`：

| 字段 | 含义 |
|------|------|
| `component` | 业务申请页组件。 |
| `title` | 自定义申请容器展示标题。 |

`BusinessApplyContext`：

| 字段 | 含义 |
|------|------|
| `definitionId` | 流程定义 ID。 |
| `definitionKey` | 流程定义 key。 |
| `applyPageKey` | 当前申请组件 key。 |
| `definition` | 流程定义详情。 |
| `query` | 当前路由查询参数。 |

`BusinessApprovalRegistration`：

| 字段 | 含义 |
|------|------|
| `component` | 业务审批详情组件。 |
| `recordPanelMode` | 审批记录面板模式：`DEFAULT`、`HIDDEN`、`CUSTOM`。 |
| `recordPanelComponent` | 自定义审批记录组件。 |
| `commentMode` | 意见输入模式：`ACTION_BAR`、`BUSINESS_FORM`、`NONE`。 |
| `collectVariables` | 动作提交前采集流程变量。 |
| `collectComment` | 动作提交前采集审批意见。 |
| `validateBeforeAction` | 动作前校验，抛错会阻止提交。 |
| `beforeAction` | 动作调用前钩子。 |
| `afterAction` | 动作成功后钩子。 |
| `getActionOverrides` | 按上下文调整动作显隐、禁用、文案和提示。 |

`BusinessApprovalContext`：

| 字段 | 含义 |
|------|------|
| `businessType` | 业务类型。 |
| `businessKey` | 业务主键。 |
| `applyId` | 业务申请记录 ID。 |
| `processInstanceId` | 流程实例 ID。 |
| `taskId` | 当前任务 ID。 |
| `taskDefinitionKey` | 当前节点 key。 |
| `nodeName` | 当前节点名称。 |
| `nodeExtension` | 节点扩展配置。 |
| `readonly` | 是否只读。 |
| `variables` | 当前流程变量。 |
| `permissions` | 字段权限。 |
| `records` | 审批记录。 |

## 6. 后端依赖

| 能力 | 后端依赖 |
|------|----------|
| 流程定义、流程实例、任务、业务申请 | `mango-workflow`。 |
| 用户候选项 | `/identity/users/page`。 |
| 业务域候选项 | `/domain/domains/enabled-tree`。 |
| 流程接口 | `/workflow/categories`、`/workflow/definitions`、`/workflow/templates`、`/workflow/tasks`、`/workflow/processes`、`/workflow/business-applies`。 |
| 文件字段 | `@mango/file` 和后端 `mango-file`。 |

## 7. 权限与数据边界

- `permissions` 只影响前端渲染，不替代后端校验。
- 业务组件注册 key 不是权限码，不能作为访问控制依据。
- 待办、已办、抄送、任务详情和动作提交由后端按当前用户、租户、候选人和流程实例状态校验。
- 自定义申请页保存业务数据时，应调用业务后端写入业务主表，并由业务后端校验数据权限。

## 8. 快速开始

1. 动态表单流程使用 `parseRuntimeForm()` 解析流程定义 `formJson`，再用 `RuntimeFormRenderer` 渲染字段。
2. 需要默认值时，用 `createDefaultVariables(fields)` 创建变量对象。
3. 自定义申请页在业务包启动时调用 `registerBusinessApplyComponent(applyPageKey, registration)`。
4. 自定义审批页调用 `registerBusinessApprovalComponent(approvePageKey, registration)`，按动作采集变量和意见。
5. 审批详情需要字段权限时，从任务变量里读取 `businessPermissions`，再传给 `RuntimeFormRenderer`。

## 9. 问题排查

**动态表单字段不显示**

检查 `formJson` 是否是合法 JSON，字段类型是否在 `RuntimeFormFieldType` 范围内。`parseRuntimeForm()` 会把不支持的字段放进 `unsupported`。

**任务详情找不到业务组件**

检查业务包是否已经注册组件，流程定义或任务变量里的 key 是否和注册 key 一致。

**审批意见没有提交**

检查 `commentMode` 和 `collectComment()`。如果意见在业务表单里，需要设置 `commentMode: 'BUSINESS_FORM'` 并实现 `collectComment()`。

**按钮文案或显隐不符合业务节点**

检查节点动作配置和 `getActionOverrides()`。最终可见按钮由节点动作配置和业务覆盖配置共同决定。

**字段只读但仍被提交**

前端只控制交互。后端排障时检查节点权限过滤和变量变更校验，确认只读字段没有被业务接口写入。

## 10. 相关文档

- [@mango/workflow README](../../README.md)
- [Workflow 后端 README](../../../../../mango/mango-platform/mango-workflow/README.md)
- [@mango/file 组件 README](../../../file/src/components/README.md)
- [能力说明维护规范](../../../../../mango-pmo/rules/08-capability-docs.md)
