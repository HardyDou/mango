# 工作流业务示例包

## 1. 能力定位

提供费用报销、用印等工作流业务组件示例。

主要使用者：前端开发者、业务开发者和 AI Agent。

## 2. 适用场景

开发新的业务审批模块时参考组件注册和测试方式。

## 3. 不适用场景

不作为真实业务模块直接交付。

## 4. 模块边界

包名：`@mango/workflow-business-example`。本包只提供前端运行时、页面、组件、API 封装或页面注册能力，不改变后端接口契约。

## 5. 接入方式

在业务前端或 Mango 前端包中引入 `@mango/workflow-business-example`：

```ts
import { registerWorkflowBusinessExampleComponents } from '@mango/workflow-business-example';

registerWorkflowBusinessExampleComponents();
```

后台页面注册入口：

```ts
import { registerMangoWorkflowBusinessExampleAdminPages } from '@mango/workflow-business-example/admin-pages';

registerMangoWorkflowBusinessExampleAdminPages();
```

## 6. 配置项

配置来自业务应用 Vite、Shell runtimeConfig、后端 API baseURL 和包导出的注册入口；本 README 不复制长期前端规则。

## 7. 对外接口 / 扩展点

公开入口以 `package.json` exports 和 `src/index.ts` 为准。

关键文件：

- `src/register.ts`：注册业务申请组件和业务审批组件。
- `src/admin-pages.ts`：注册 `workflow/business-form/index` 页面，并调用业务组件注册。
- `src/views/business-form/index.vue`：费用报销、合同用印申请表单示例。
- `src/business-components/ExpenseApprovalDetail.vue`：费用报销审批详情示例。
- `src/business-components/DocumentTableApprovalDetail.vue`：合同用印审批详情示例。
- `src/__tests__/adminPages.spec.ts`：页面注册回归。
- `src/__tests__/contractSealApproval.spec.ts`：合同用印审批组件回归。

示例业务 key：

- 申请：`workflow.expense.apply`、`workflow.contractSeal.apply`。
- 审批：`workflow.expense.approve`、`workflow.contractSeal.approve` 及 `manager`、`finance`、`legal`、`sealKeeper` 节点后缀。

## 8. 数据库 / 初始化数据

无前端数据库。菜单、权限和初始化数据由对应后端模块或 business starter 维护。

## 9. 菜单 / 权限 / 租户

前端只负责页面注册、菜单 component 映射和交互展示；权限、租户和数据归属由后端接口校验。

## 10. 验证方式

```bash
pnpm -F @mango/workflow-business-example test
```

## 11. 业务接入最小闭环

复制示例模式到业务模块，替换真实 API 和表单：

1. 在业务包实现申请表单和审批详情组件。
2. 在业务包 `register.ts` 调用 `registerBusinessApplyComponents()` 和 `registerBusinessApprovalComponents()`。
3. 让业务流程模板中的业务 key 与 `register.ts` 中的 key 对齐。
4. 在业务应用启动阶段调用业务包注册函数。
5. 保留页面注册和审批变量采集测试，覆盖 `collectVariables`、`collectComment`、`validateBeforeAction`。

## 12. 常见问题

如果页面打不开，先检查包是否构建、样式是否引入、菜单 component 是否注册、后端 API 是否返回真实数据。

## 13. 关联 PMO 规则

- [前端模块规范](../../../mango-pmo/rules/frontend/01-vue-code.md)
- [前端测试规范](../../../mango-pmo/rules/frontend/04-test.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 14. 历史设计 / 交付记录

- [能力地图](../../../mango-docs/capabilities/README.md)
