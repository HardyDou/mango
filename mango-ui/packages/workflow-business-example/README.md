# @mango/workflow-business-example

## 1. 概览
`@mango/workflow-business-example` 是工作流业务接入示例包，演示费用报销和合同用印两类业务如何注册自定义申请页、审批详情页、审批变量采集、审批意见采集和动作前校验。

它用于业务开发参考，不是生产业务模块；示例表单中的本地数据和模拟记录不应直接作为真实业务持久化方案。

## 2. 功能清单

| 能力 | 常用入口 |
|------|----------|
| 新业务模块接入工作流前，参考申请页和审批页注册方式 | 前端注册 / 组件 / API 封装 |
| 验证 @mango/workflow 的自定义申请、业务审批和节点字段权限机制 | 前端注册 / 组件 / API 封装 |
| 给业务开发提供费用报销、合同用印这类典型审批页面写法 | 前端注册 / 组件 / API 封装 |
| 回归测试业务组件注册顺序、审批变量采集和审批意见校验 | 前端注册 / 组件 / API 封装 |

## 3. 适用场景
- 新业务模块接入工作流前，参考申请页和审批页注册方式。
- 验证 `@mango/workflow` 的自定义申请、业务审批和节点字段权限机制。
- 给业务开发提供费用报销、合同用印这类典型审批页面写法。
- 回归测试业务组件注册顺序、审批变量采集和审批意见校验。

## 4. 边界说明
- 不作为真实费用报销、合同、用印系统交付。
- 不提供后端业务表、文件归档、印章台账或财务支付能力。
- 不替代业务模块自己的 API、权限、租户和状态机。

## 5. 模块组成
本包只依赖 `@mango/workflow` 和 `@mango/admin-pages`，提供示例 Vue 组件、注册函数、页面注册入口和 Vitest 回归测试。真实业务接入时，应复制模式而不是复制示例数据。

示例包括：

- `src/register.ts`：注册申请和审批组件。
- `src/views/business-form/index.vue`：费用报销、合同用印申请表示例。
- `src/business-components/ExpenseApprovalDetail.vue`：费用报销审批详情示例。
- `src/business-components/DocumentTableApprovalDetail.vue`：合同用印审批详情示例。
- `src/admin-pages.ts`：注册 `workflow/business-form/index` 页面，并保证业务组件先注册。

## 6. 接入方式
只注册业务组件：

```ts
import { registerWorkflowBusinessExampleComponents } from '@mango/workflow-business-example';
import '@mango/workflow-business-example/style.css';

registerWorkflowBusinessExampleComponents();
```

同时注册示例页面：

```ts
import { registerMangoWorkflowBusinessExampleAdminPages } from '@mango/workflow-business-example/admin-pages';

registerMangoWorkflowBusinessExampleAdminPages();
```

真实业务模块建议实现自己的 `registerBusinessComponents()`：

```ts
import { registerBusinessApplyComponents, registerBusinessApprovalComponents } from '@mango/workflow';
import ContractApplyView from './views/ContractApplyView.vue';
import ContractApprovalView from './views/ContractApprovalView.vue';

export function registerContractWorkflowComponents() {
  registerBusinessApplyComponents({
    'contract.apply': { title: '合同申请', component: ContractApplyView },
  });
  registerBusinessApprovalComponents({
    'contract.approve.legal': {
      component: ContractApprovalView,
      collectVariables: context => ({ legalOpinion: context.variables.legalOpinion }),
      collectComment: context => context.variables.legalOpinion,
    },
  });
}
```

## 7. 配置说明
本示例包没有独立运行时配置。关键配置是注册 key 与流程定义中的 key 对齐。

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

流程定义中 `formJson.customConfig.applyPageKey` 和 `approvePageKey` 需要使用这些 key，任务详情才能解析到对应组件。

## 8. API 与扩展
公开导出：

- `registerWorkflowBusinessExampleComponents()`
- `registerWorkflowBusinessExampleApprovalComponents`
- `registerMangoWorkflowBusinessExampleAdminPages()`
- `WorkflowBusinessFormView`

示例审批逻辑：

- 费用报销：只有 `financeReview` 为 `EDITABLE` 时提交 `approvedAmount`。
- 合同用印：按字段权限提交 `managerOpinion`、`legalOpinion`、`financeOpinion`、`approvedSealCount`、`sealKeeperOpinion`。
- 合同用印：`complete` 和 `reject` 动作在当前节点意见可编辑时要求意见非空。
- 合同用印：`commentMode` 使用 `BUSINESS_FORM`，审批意见来自业务表单字段。

## 9. 数据与初始化
本包不包含数据库 migration，也不初始化真实业务数据。

真实业务接入需要自行准备：

- 业务主表和审批快照表。
- 业务单据编号。
- 文件、附件和印章台账。
- 流程定义和模板。
- 菜单、权限和租户授权。

## 10. 管理入口
示例页面注册在模块 `mango-workflow` 下，页面 key 为 `workflow/business-form/index`。如果要在 Admin 菜单中打开，需要 authorization 菜单 component 与该 key 一致。

示例组件本身不做权限真相判断。真实业务必须在后端校验：

- 当前用户是否能创建该业务申请。
- 当前用户是否能查看申请详情和审批记录。
- 当前用户是否能在当前节点提交对应字段。
- 当前租户是否能访问该业务数据。

## 11. 快速开始
1. 复制 `src/register.ts` 的注册模式到业务包。
2. 替换示例申请页为真实业务申请页，并接入业务后端保存接口。
3. 替换示例审批详情为真实业务详情页，并按节点权限控制字段。
4. 在流程定义中配置 `CUSTOM_PAGE`，设置业务自己的 `applyPageKey` 和 `approvePageKey`。
5. 在 Shell 启动时调用业务包注册函数。
6. 保留针对注册 key、`collectVariables`、`collectComment`、`validateBeforeAction` 的单测。

## 12. 问题排查
- 示例页面能打开但真实业务打不开：检查真实业务包是否在 workflow 页面加载前注册。
- 审批组件没有按节点区分：检查流程变量或节点扩展是否输出了正确的审批 key。
- 意见没有进入后端：检查 `commentMode` 和 `collectComment`。
- 变量没有更新：检查字段权限是否为 `EDITABLE`，以及 `collectVariables` 是否返回该字段。

## 13. 相关文档
- [前端模块规范](../../../mango-pmo/rules/frontend/01-vue-code.md)
- [前端测试规范](../../../mango-pmo/rules/frontend/04-test.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 14. 历史资料
- [@mango/workflow README](../workflow/README.md)
- [@mango/workflow 组件 README](../workflow/src/components/README.md)
- [Workflow 后端 README](../../../mango/mango-platform/mango-workflow/README.md)
- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
