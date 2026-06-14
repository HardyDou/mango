# Workflow Components

## 1. 入口定位

本入口说明 `@mango/workflow` 的业务接入组件和运行时扩展点，包括运行时表单渲染、业务发起页注册、业务审批页注册和审批轨迹展示。

## 2. 公开导出

来自 `@mango/workflow`：

- `RuntimeFormRenderer`
- `WorkflowProgressTree`
- `WorkflowApprovalTimeline`
- `WorkflowNodeTimeline`
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

## 3. 使用场景

- 业务模块提供自定义申请页，并通过 workflow start process 接入。
- 业务模块提供自定义审批表单、审批变量采集和动作前后钩子。
- 工作流任务详情页渲染运行时表单、审批轨迹和节点进度。
- 模板、定义、待办、已办、抄送等工作流管理页面注册到 Mango Admin。

## 4. 接入方式

```ts
import {
  RuntimeFormRenderer,
  registerBusinessApplyComponent,
  registerBusinessApprovalComponent,
} from '@mango/workflow';
import '@mango/workflow/style.css';
```

注册业务申请页：

```ts
registerBusinessApplyComponent('contract-apply', {
  component: ContractApplyView,
  title: '合同申请',
});
```

注册业务审批页：

```ts
registerBusinessApprovalComponent('contract', {
  component: ContractApprovalView,
  collectVariables: context => ({ amount: context.variables.amount }),
});
```

页面 key 由 `registerMangoWorkflowAdminPages()` 注册，包括 `workflow/task/todo/index`、`workflow/task/detail/index`、`workflow/start-process/index` 和 `workflow/custom-apply/index`。

## 5. Props / 参数 / 事件

`RuntimeFormRenderer` props：

- `fields`：运行时字段数组。
- `model`：表单数据对象。
- `readonly`：整体只读。
- `labelWidth`：表单标签宽度。
- `permissions`：字段级权限，支持 `HIDDEN`、`READONLY`、`EDITABLE`。

业务申请注册参数：

- `key`：申请页 key。
- `component`：Vue 组件。
- `title`：可选标题。

业务审批注册参数：

- `component`：Vue 组件。
- `recordPanelMode`、`recordPanelComponent`：审批记录展示模式。
- `collectVariables`、`collectComment`：动作提交前采集变量和意见。
- `validateBeforeAction`、`beforeAction`、`afterAction`：动作生命周期钩子。
- `getActionOverrides`：按上下文调整动作可见性、禁用态、文案和提示。

## 6. 后端依赖

- 后端模块：`mango-platform/mango-workflow`。
- 组织和用户选择依赖：`mango-platform/mango-identity`、`mango-platform/mango-org`、`mango-platform/mango-system`。
- 文件上传字段依赖：`@mango/file` 和后端 `mango-file`。
- API 前缀：`/workflow/categories`、`/workflow/definitions`、`/workflow/templates`、`/workflow/tasks`、`/workflow/processes`、`/workflow/business-applies`。

## 7. 权限 / 租户 / 数据边界

- 页面 component key 由 workflow 菜单资源绑定。
- 任务列表、任务详情、流程发起和动作提交由后端 workflow 模块按登录用户、租户、任务候选人、任务归属和流程实例校验。
- 字段级权限来自后端流程节点扩展变量，前端只按 `permissions` 渲染隐藏、只读或可编辑状态。
- 业务审批变量由注册方采集，后端仍负责业务数据权限和流程动作合法性。

## 8. 验证方式

```bash
pnpm -F @mango/workflow build
```

页面验收入口：

- 待办：`workflow/task/todo/index`
- 任务详情：`workflow/task/detail/index`
- 发起流程：`workflow/start-process/index`
- 自定义申请：`workflow/custom-apply/index`

最小断言：

- 注册业务申请 key 后，自定义申请页能解析到业务组件。
- 注册业务审批 key 后，任务详情能渲染业务审批组件。
- `RuntimeFormRenderer` 能按字段权限隐藏、只读或编辑。

## 9. 常见问题

- 任务详情找不到业务组件时，检查 `businessType`、注册 key 和业务包是否在 Shell 启动前注册。
- 上传字段不可用时，检查 `@mango/file` 样式、后端文件服务和字段 `type`。
- 审批动作变量缺失时，检查 `collectVariables` 返回值和后端节点扩展配置。

## 10. 关联文档

- [@mango/workflow README](../../README.md)
- [Workflow 后端 README](../../../../../mango/mango-platform/mango-workflow/README.md)
- [@mango/file 组件 README](../../../file/src/components/README.md)
- [能力地图](../../../../../mango-docs/capabilities/README.md)
- [能力说明维护规范](../../../../../mango-pmo/rules/08-capability-docs.md)
