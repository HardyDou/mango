# @mango/workflow Components

## 1. 概览
本入口说明 `@mango/workflow` 的运行时表单、流程轨迹和业务申请/审批注册扩展点。业务开发接入工作流时，核心不是复制页面，而是把业务组件注册到流程定义中的 key。

## 2. 功能清单
来自 `@mango/workflow`：

- `RuntimeFormRenderer`
- `WorkflowProgressTree`
- `WorkflowApprovalTimeline`
- `WorkflowNodeTimeline`
- `parseRuntimeForm`
- `createDefaultVariables`
- `parseWorkflowFormConfig`
- `customApplyRouteOf`
- `registerBusinessApplyComponent`
- `registerBusinessApplyComponents`
- `resolveBusinessApplyRegistration`
- `registerBusinessApprovalComponent`
- `registerBusinessApprovalComponents`
- `resolveBusinessApprovalRegistration`
- `resolveBusinessApprovalComponent`
- `collectBusinessApprovalVariables`
- `collectBusinessApprovalComment`
- `businessTypeOf`
- `applyIdOf`
- `businessPermissionsOf`

页面注册入口来自 `@mango/workflow/admin-pages` 的 `registerMangoWorkflowAdminPages()`。

## 3. 适用场景
- 动态表单模式下，用 `RuntimeFormRenderer` 渲染流程表单。
- 自定义申请模式下，业务包注册申请页，工作流发起页按 `applyPageKey` 打开。
- 任务详情页按业务 key 渲染业务审批组件，并在动作提交前采集变量和意见。
- 业务页面展示流程节点进度、审批记录和轨迹。

## 4. 接入方式
运行时表单：

```vue
<script setup lang="ts">
import { RuntimeFormRenderer, parseRuntimeForm, createDefaultVariables } from '@mango/workflow';

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
registerBusinessApplyComponent('workflow.expense.apply', {
  title: '费用报销申请',
  component: ExpenseApplyView,
});
```

注册业务审批页：

```ts
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

业务申请组件会收到 `BusinessApplyContext`，审批组件会收到 `BusinessApprovalContext`。具体 props 由任务详情容器传入业务组件。

## 5. 参数与事件
`RuntimeFormRenderer` props：

| prop | 含义 |
|------|------|
| `fields` | `RuntimeFormField[]`，由 `parseRuntimeForm()` 或业务方生成。 |
| `model` | 表单变量对象。 |
| `readonly` | 整体只读。 |
| `labelWidth` | 标签宽度。 |
| `permissions` | 字段级权限，支持 `HIDDEN`、`READONLY`、`EDITABLE`。 |

`RuntimeFormField` 支持的主要类型：

- 输入：`input`、`textarea`、`password`、`number`。
- 选择：`select`、`radio`、`checkbox`、`switch`、`cascader`、`treeSelect`、`transfer`。
- 日期时间：`date`、`daterange`、`time`、`timerange`、`datetime`、`datetimerange`。
- 展示和布局：`alert`、`text`、`html`、`divider`、`tag`、`image`、`button`、`container`。
- 业务类型：`systemUser`、`systemOrg`、`systemDept`、`systemPost`、`systemRole`、`systemDict`、`businessType`、`signature`、`serialNo`。
- 文件：`upload`、`imageUpload`。

`BusinessApplyRegistration`：

| 字段 | 含义 |
|------|------|
| `component` | 业务申请页组件。 |
| `title` | 自定义申请容器展示标题。 |

`BusinessApplyContext`：

| 字段 | 含义 |
|------|------|
| `definitionId` | 流程定义 id。 |
| `definitionKey` | 流程定义 key。 |
| `applyPageKey` | 当前申请组件 key。 |
| `definition` | 流程定义详情。 |
| `query` | 路由查询参数。 |

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
| `applyId` | 业务申请记录 id。 |
| `processInstanceId` | 流程实例 id。 |
| `taskId` | 当前任务 id。 |
| `taskDefinitionKey` | 当前节点 key。 |
| `nodeName` | 当前节点名称。 |
| `nodeExtension` | 节点扩展配置。 |
| `readonly` | 是否只读。 |
| `variables` | 当前流程变量。 |
| `permissions` | 字段权限。 |
| `records` | 审批记录。 |

## 6. 后端依赖
- 后端模块：`mango-platform/mango-workflow`。
- 用户候选项：`/identity/users/page`。
- 业务域候选项：`/domain/domains/enabled-tree`。
- 流程接口：`/workflow/categories`、`/workflow/definitions`、`/workflow/templates`、`/workflow/tasks`、`/workflow/processes`、`/workflow/business-applies`。
- 文件字段依赖 `@mango/file` 和后端 `mango-file`。

## 7. 权限与数据边界
- 字段权限只影响前端渲染，后端仍按动作和业务变量做校验。
- 业务组件注册 key 不是权限码，不能作为访问控制依据。
- 待办、已办、抄送、任务详情和动作提交由后端按用户、租户、候选人和流程实例状态校验。
- 自定义申请页保存业务数据时，应由业务后端写入业务主表并校验数据权限。

## 8. 快速开始

1. 动态表单流程使用 `parseRuntimeForm()` 解析定义，再用 `RuntimeFormRenderer` 渲染字段。
2. 自定义申请页在业务包启动时调用 `registerBusinessApplyComponent(applyPageKey, registration)`。
3. 自定义审批页调用 `registerBusinessApprovalComponent(approvalPageKey, registration)`，按节点采集变量和意见。
4. 后端任务动作提交时仍要校验候选人、租户、流程状态和业务数据权限。

## 9. 问题排查
- 任务详情找不到业务组件：检查业务包是否在 Shell 启动前注册，流程变量中的业务 key 是否一致。
- 审批意见没有提交：检查 `commentMode` 和 `collectComment`。
- 字段明明只读但仍被提交：前端只控制交互，后端需要按节点权限过滤或拒绝变量。
- 动态表单字段不显示：检查 `formJson` 是否是合法 JSON，字段类型是否在 `RuntimeFormFieldType` 范围内。

## 10. 相关文档
- [@mango/workflow README](../../README.md)
- [Workflow 后端 README](../../../../../mango/mango-platform/mango-workflow/README.md)
- [@mango/file 组件 README](../../../file/src/components/README.md)
- [能力说明维护规范](../../../../../mango-pmo/rules/08-capability-docs.md)
