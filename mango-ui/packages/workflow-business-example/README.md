# @mango/workflow-business-example

## 1. 概览

`@mango/workflow-business-example` 是 Mango 工作流业务接入示例包，演示业务前端如何向 `@mango/workflow` 注册自定义申请页、审批详情页、审批变量采集、审批意见采集和动作前校验。

集成形态：

| 标识 | 说明 |
|------|------|
| `admin-pages` | 注册示例业务申请页 `workflow/business-form/index`。 |
| `business-component` | 注册费用报销和合同用印的申请、审批组件示例。 |
| `example` | 给业务模块复制接入模式，不作为生产业务模块交付。 |

示例只演示前端接入方式，不提供真实费用报销、合同、用印、财务支付、文件归档、印章台账或后端业务表。

## 2. 功能清单

| 能力 | 使用入口 | 说明 |
|------|----------|------|
| 注册自定义申请页 | `registerWorkflowBusinessExampleComponents()` | 把申请组件注册到 workflow 的业务申请组件表。 |
| 注册审批详情页 | `registerWorkflowBusinessExampleComponents()` | 把审批组件注册到 workflow 的业务审批组件表。 |
| 注册示例菜单页面 | `registerMangoWorkflowBusinessExampleAdminPages()` | 注册 `workflow/business-form/index` 页面，并先注册业务组件。 |
| 采集审批变量 | `collectVariables` | 按字段权限把业务表单字段提交给 workflow。 |
| 采集审批意见 | `collectComment` | 合同用印示例从业务字段生成审批意见。 |
| 动作前校验 | `validateBeforeAction` | 合同用印示例在完成/驳回前校验意见和用印份数。 |

## 3. 接入方式

开发依赖：

```bash
pnpm add @mango/workflow-business-example
```

宿主应用需要已经接入 `@mango/workflow`、`@mango/admin-pages`、Vue、Vue Router 和 Element Plus。真实业务项目通常不依赖这个示例包，而是复制注册模式到自己的业务前端包。

只注册示例业务组件：

```ts
import { registerWorkflowBusinessExampleComponents } from '@mango/workflow-business-example';
import '@mango/workflow-business-example/style.css';

registerWorkflowBusinessExampleComponents();
```

同时注册示例页面：

```ts
import { registerMangoWorkflowBusinessExampleAdminPages } from '@mango/workflow-business-example/admin-pages';
import '@mango/workflow-business-example/style.css';

registerMangoWorkflowBusinessExampleAdminPages();
```

业务模块应实现自己的注册函数：

```ts
import { registerBusinessApplyComponents, registerBusinessApprovalComponents } from '@mango/workflow';
import ContractApplyView from './views/ContractApplyView.vue';
import ContractApprovalView from './views/ContractApprovalView.vue';

export function registerContractWorkflowComponents() {
  registerBusinessApplyComponents({
    'contract.apply': {
      title: '合同申请',
      component: ContractApplyView,
    },
  });

  registerBusinessApprovalComponents({
    'contract.approve.legal': {
      component: ContractApprovalView,
      collectVariables: context => ({
        legalOpinion: context.variables.legalOpinion,
      }),
      collectComment: context => context.variables.legalOpinion,
    },
  });
}
```

## 4. 配置说明

本包没有独立运行时配置。关键配置是“注册 key”和流程定义中的 custom page key 保持一致。

| 配置位置 | 示例值 | 含义 |
|----------|--------|------|
| 申请组件 key | `workflow.expense.apply` | 费用报销申请页。 |
| 申请组件 key | `workflow.contractSeal.apply` | 合同用印申请页。 |
| 审批组件 key | `workflow.expense.approve` | 费用报销通用审批详情。 |
| 审批组件 key | `workflow.expense.approve.manager` | 费用报销经理节点。 |
| 审批组件 key | `workflow.expense.approve.finance` | 费用报销财务节点。 |
| 审批组件 key | `workflow.contractSeal.approve` | 合同用印通用审批详情。 |
| 审批组件 key | `workflow.contractSeal.approve.manager` | 合同用印经理节点。 |
| 审批组件 key | `workflow.contractSeal.approve.legal` | 合同用印法务节点。 |
| 审批组件 key | `workflow.contractSeal.approve.finance` | 合同用印财务节点。 |
| 审批组件 key | `workflow.contractSeal.approve.sealKeeper` | 合同用印印章管理员节点。 |
| 页面 key | `workflow/business-form/index` | 示例业务申请页面。 |

流程定义使用自定义页面时，`formJson.customConfig.applyPageKey` 和 `approvePageKey` 要写业务自己的 key。示例 key 只能用于本示例页面。

## 5. API 与扩展

公开导出：

| 导出 | 用途 |
|------|------|
| `registerWorkflowBusinessExampleComponents()` | 注册示例申请和审批组件，幂等执行。 |
| `registerWorkflowBusinessExampleApprovalComponents` | 上面函数的兼容别名。 |
| `registerMangoWorkflowBusinessExampleAdminPages()` | 注册示例页面，并先注册业务组件。 |
| `WorkflowBusinessFormView` | 费用报销、合同用印申请表示例页面。 |

注册入口：

| 文件 | 作用 |
|------|------|
| `src/register.ts` | 注册申请组件、审批组件、变量采集、意见采集和动作前校验。 |
| `src/admin-pages.ts` | 注册 `workflow/business-form/index` 页面。 |
| `src/views/business-form/index.vue` | 示例申请表。 |
| `src/business-components/ExpenseApprovalDetail.vue` | 费用报销审批详情。 |
| `src/business-components/DocumentTableApprovalDetail.vue` | 合同用印审批详情。 |

示例审批规则：

| 示例 | 行为 |
|------|------|
| 费用报销 | 只有 `financeReview` 权限为 `EDITABLE` 时提交 `approvedAmount`。 |
| 合同用印 | 按字段权限提交 `managerOpinion`、`legalOpinion`、`financeOpinion`、`approvedSealCount`、`sealKeeperOpinion`。 |
| 合同用印 | `complete` 和 `reject` 动作在当前节点意见可编辑时要求意见非空。 |
| 合同用印 | `commentMode` 为 `BUSINESS_FORM`，审批意见来自业务表单字段。 |

## 6. 数据与初始化

本包不包含数据库 migration，也不会初始化真实业务数据。真实业务接入工作流时要自行准备：

| 数据 | 由谁提供 |
|------|----------|
| 业务主表、审批快照、业务状态 | 业务后端模块。 |
| 业务单据编号 | 业务模块或 `mango-numgen`。 |
| 文件、附件、印章台账 | 业务模块和 `mango-file`。 |
| 流程模板和流程定义 | `mango-workflow` 管理页面或后端初始化。 |
| 菜单、权限、租户授权 | 业务模块 resource manifest 和 `mango-authorization`。 |

## 7. 管理入口

示例页面注册在模块 `mango-workflow` 下，页面 key 为 `workflow/business-form/index`。如果要从 Admin 菜单打开，authorization 菜单的 component 必须与该 key 一致。

真实业务必须在后端校验：

| 校验点 | 说明 |
|--------|------|
| 创建权限 | 当前用户是否能发起该业务申请。 |
| 查看权限 | 当前用户是否能查看业务详情和审批记录。 |
| 节点权限 | 当前用户是否能在当前节点提交对应字段。 |
| 租户边界 | 当前租户是否能访问该业务数据。 |
| 动作权限 | complete、reject 等动作是否允许执行。 |

## 8. 快速开始

1. 先阅读 `@mango/workflow` 的使用文档，确认业务要用 `CUSTOM_PAGE`。
2. 复制 `src/register.ts` 的注册模式到业务前端包。
3. 替换示例申请页为真实业务申请页，并接入业务后端保存接口。
4. 替换示例审批详情为真实业务详情页，并按 workflow 字段权限控制可编辑字段。
5. 在流程定义中配置业务自己的 `applyPageKey` 和 `approvePageKey`。
6. 在 Shell 启动时先注册业务组件，再打开 workflow 任务详情。

## 9. 问题排查

| 问题 | 常见原因 | 处理方式 |
|------|----------|----------|
| 示例页能打开，真实业务页打不开 | 业务组件注册晚于 workflow 页面加载 | 在应用启动阶段先调用业务注册函数。 |
| 审批组件没有按节点切换 | 流程定义中的 `approvePageKey` 没有区分节点 | 检查流程定义 custom config 和注册 key。 |
| 审批意见没有提交 | `commentMode` 或 `collectComment` 未配置 | 对照合同用印示例实现。 |
| 变量没有更新 | 字段权限不是 `EDITABLE` 或 `collectVariables` 没返回字段 | 检查 workflow 任务上下文 permissions。 |
| 业务数据没保存 | 示例没有真实后端持久化 | 接入业务自己的保存接口和状态机。 |

## 10. 相关文档

- [@mango/workflow README](../workflow/README.md)
- [@mango/workflow 组件 README](../workflow/src/components/README.md)
- [Workflow 后端 README](../../../mango/mango-platform/mango-workflow/README.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
