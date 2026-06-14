# 工作流前端包

## 1. 能力定位

提供流程管理页面、运行时表单和任务动作组件。

主要使用者：前端开发者、业务开发者和 AI Agent。

## 2. 适用场景

业务前端接入审批、待办或流程配置时使用。

## 3. 不适用场景

不负责后端 Flowable 引擎和业务表单持久化。

## 4. 模块边界

包名：`@mango/workflow`。本包只提供前端运行时、页面、组件、API 封装或页面注册能力，不改变后端接口契约。

## 5. 接入方式

安装并引入：

```ts
import '@mango/workflow/style.css';
import { RuntimeFormRenderer, WorkflowTaskListView, workflowApi } from '@mango/workflow';
import { registerMangoWorkflowAdminPages } from '@mango/workflow/admin-pages';
```

后端需要接入 [Workflow](../../../mango/mango-platform/mango-workflow/README.md)，涉及附件或图标预览时需要 [File](../../../mango/mango-platform/mango-file/README.md)。

## 6. 配置项

配置来自业务应用 Vite、Shell runtimeConfig、后端 API baseURL 和包导出的注册入口；本 README 不复制长期前端规则。

## 7. 对外接口 / 扩展点

公开入口：

- 页面：`WorkflowDefinitionView`、`WorkflowTemplateView`、`WorkflowTaskListView`、`WorkflowTaskDetailView`、`WorkflowStartProcessView`、`WorkflowCustomApplyView`。
- 组件：`RuntimeFormRenderer`、`WorkflowProgressTree`、`WorkflowApprovalTimeline`、`WorkflowNodeTimeline`。
- API 和扩展：`workflowApi`、`registerBusinessApplyComponent`、`registerBusinessApprovalComponent`。
- 页面注册：`registerMangoWorkflowAdminPages()`，页面 key 包括 `workflow/definition/index`、`workflow/template/index`、`workflow/task/todo/index`、`workflow/task/detail/index`、`workflow/start-process/index`。

主要 API 前缀：`/workflow`。

## 8. 数据库 / 初始化数据

无前端数据库。菜单、权限和初始化数据由对应后端模块或 business starter 维护。

## 9. 菜单 / 权限 / 租户

前端只负责页面注册、菜单 component 映射和交互展示；权限、租户和数据归属由后端接口校验。

## 10. 验证方式

```bash
pnpm -F @mango/workflow build
```

## 11. 业务接入最小闭环

业务注册 workflow 页面和业务表单组件，创建流程定义并部署，发起流程后验证待办、审批动作、流程轨迹和业务状态回写。

## 12. 常见问题

流程列表为空优先检查分类、定义状态和部署状态；待办为空优先检查发起人、审批人和任务分配；业务表单不显示优先检查业务组件注册 key 和 form config。

## 13. 关联 PMO 规则

- [前端模块规范](../../../mango-pmo/rules/frontend/01-vue-code.md)
- [前端测试规范](../../../mango-pmo/rules/frontend/04-test.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 14. 历史设计 / 交付记录

- [能力地图](../../../mango-docs/capabilities/README.md)
