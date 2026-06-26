# 工作流业务 UI 组件设计

## 背景

当前 `@mango/workflow` 已具备流程定义图、节点进度、审批记录、业务审批组件注册和业务申请历史查询等能力，但这些能力分散在任务详情页和业务示例页中，尚未形成面向业务模块的稳定组件边界。

业务模块接入审批详情页时，仍需要自行拼装页面布局、右侧审批信息、流程图弹窗、历史申请记录和当前实例进度。合同用印详情已经体现出典型业务诉求：左侧展示业务表单或自定义详情，右侧展示当前审批状态，并通过按钮查看流程定义图和历史申请。

## 目标

- 将现有工作流 UI 能力沉淀为 `@mango/workflow` 公共组件。
- 让业务模块优先使用 `WorkflowLayout` 和 `WorkflowSidebar` 完成审批详情页集成。
- 复用现有 `WorkflowProgressTree`、`WorkflowNodeTimeline`、`WorkflowApprovalTimeline` 和 workflow API，不重写已有能力。
- 保持旧组件导出兼容，新增语义更清晰的组件入口。

## 不处理范围

- 不改变后端 workflow API。
- 不改变流程设计器数据结构。
- 不改业务审批动作、字段权限和任务提交协议。
- 不把合同用印示例升级为真实业务模块。
- 不新增流程定义初始化数据。

## 组件交付

### WorkflowLayout

页面级工作流业务布局组件。

职责：

- 提供详情页返回栏。
- Header 右侧展示返回按钮。
- 左侧承载业务主内容。
- 右侧承载工作流侧栏。
- 不依赖 router，返回行为通过事件交给宿主。

业务使用：

```vue
<WorkflowLayout title="合同用印详情" @back="goBack">
  <DocumentTableApprovalDetail :context="context" />

  <template #sidebar>
    <WorkflowSidebar v-bind="workflowSidebarProps" />
  </template>
</WorkflowLayout>
```

### WorkflowSidebar

右侧工作流侧栏组件。

职责：

- 展示流程实例概要。
- 展示当前实例进度。
- 提供流程图弹窗按钮。
- 提供历史申请弹窗按钮。
- 接收业务传入的数据，不直接依赖宿主 store、router 或菜单。

### WorkflowInstanceSummary

流程实例概要组件。

展示字段：

- 当前节点。
- 状态。
- 发起人。
- 办理人。
- 开始时间。

该组件主要由 `WorkflowSidebar` 组合使用，也允许业务单独使用。

### WorkflowInstanceProgress

当前流程实例进度组件。

底层复用 `WorkflowNodeTimeline`，用于展示本次流程实例从开始到结束的节点状态、处理记录和当前停留节点。

### WorkflowDefinitionGraph

流程定义图组件。

底层复用现有 `WorkflowProgressTree`。该名称更准确表达“流程设计器节点拓扑图”，避免业务误解为审批记录时间线。

### WorkflowDefinitionGraphDialog

流程定义图弹窗组件。

用于从 `WorkflowSidebar` 打开完整流程定义图。弹窗内容为 `WorkflowDefinitionGraph`，不是时间线列表。

### WorkflowInstanceHistory

历史申请组件。

基于 `workflowApi.businessApplyHistory(businessType, businessKey)` 展示同一业务单据的多次申请记录。组件支持业务摘要插槽，允许合同用印、费用报销等业务显示不同摘要信息。

### WorkflowInstanceHistoryDialog

历史申请弹窗组件。

用于从 `WorkflowSidebar` 打开历史申请记录。

## 现有代码复用映射

| 现有能力 | 位置 | 改造方式 |
| --- | --- | --- |
| `WorkflowProgressTree` | `mango-ui/packages/workflow/src/components/trace/WorkflowProgressTree.vue` | 保留旧导出，新增 `WorkflowDefinitionGraph` 语义封装 |
| `WorkflowNodeTimeline` | `mango-ui/packages/workflow/src/components/trace/WorkflowNodeTimeline.vue` | 保留旧组件，新增 `WorkflowInstanceProgress` 作为右侧进度封装 |
| `WorkflowApprovalTimeline` | `mango-ui/packages/workflow/src/components/trace/WorkflowApprovalTimeline.vue` | 保留作为某一次实例审批记录组件，不混用为历史申请 |
| 任务详情页右侧概要 | `mango-ui/packages/workflow/src/views/task-detail/index.vue` | 抽出为 `WorkflowInstanceSummary` 和 `WorkflowSidebar` |
| 任务详情页布局 | `mango-ui/packages/workflow/src/views/task-detail/index.vue` | 抽出为 `WorkflowLayout` |
| 示例页历史申请 | `mango-ui/packages/workflow-business-example/src/views/business-form/index.vue` | 沉淀为 `WorkflowInstanceHistory` |
| 历史申请 API | `workflowApi.businessApplyHistory` | 直接复用 |

## 数据边界

业务模块有两种接入方式：

1. 业务已聚合工作流数据时，直接传入 summary、definition node、records、currentNodeKey、visitedNodeKeys 和 status。
2. 业务只知道 `businessType` 和 `businessKey` 时，历史申请组件内部通过 workflow API 查询历史记录。

公共组件不读取宿主路由、菜单、store 或权限上下文。需要返回、跳转或查看详情时，通过事件或 slot 交给业务模块处理。

## 业务使用主路径

大部分业务模块只需要：

```ts
import {
  WorkflowLayout,
  WorkflowSidebar,
} from '@mango/workflow';
```

左侧业务内容通过默认 slot 注入，右侧审批信息通过 `WorkflowSidebar` 接入。

## 兼容性

- 保留 `WorkflowProgressTree`、`WorkflowNodeTimeline`、`WorkflowApprovalTimeline` 原导出。
- 新增组件走包入口统一导出。
- 旧任务详情页可逐步替换为新组件，不要求业务立即迁移。

## 验收标准

- `@mango/workflow` 导出新增公共组件和类型。
- 业务页面可用 `WorkflowLayout + WorkflowSidebar` 拼出审批详情布局。
- 流程图弹窗展示流程定义图，而不是审批时间线。
- 历史申请弹窗展示同一业务单据的多次申请记录。
- 当前流程进度可展示开始、当前节点、后续节点、结束和审批记录。
- 任务详情页复用新组件后原有审批动作、业务组件注册、动态表单渲染保持可用。
- 补充组件测试或等价验证。

## Issue

Issue 地址：https://github.com/HardyDou/mango/issues/266
