# Workflow：让业务单据发起审批并展示进度

## 先说人话

Mango Workflow 负责审批流程的定义、发起、任务办理和审批进度。它不替业务模块保存业务单据，也不替业务模块决定业务状态。

业务模块拥有自己的单据和状态；Workflow 只保存并返回审批过程事实。业务模块通过公开 API 查询进度，再决定页面如何展示和何时更新业务状态。

本文中的 Workflow 特指 Mango 审批能力。普通状态机、CI/CD workflow、PMO 流程或数据编排不要自动路由到 `mango-workflow`。

## 一个具体场景

合同申请页面提交一张合同单据：

1. 业务模块先保存合同单据。
2. 通过 `WorkflowBusinessApplyApi` 创建业务申请，或使用已有申请。
3. 通过 `WorkflowProcessApi.start()` 发起一个已发布流程。
4. 审批人通过 Workflow 任务接口办理。
5. 业务模块通过 `latestProgress()` 或批量接口读取当前进度。
6. 审批完成后，业务模块根据公开事件或查询结果更新自己的合同状态。

最小流程：

```text
业务单据 -> 业务申请 -> Workflow 流程 -> 审批任务
    └──────────────────────────────-> 业务模块更新状态
```

失败先看：租户、权限、流程是否已发布、业务主键和状态回写。

## 最小接入路径

### 后端

业务模块只依赖 API 契约：

```xml
<dependency>
    <groupId>io.mango.platform.workflow</groupId>
    <artifactId>mango-workflow-api</artifactId>
</dependency>
```

只有承载 Workflow 服务的应用才引入 `mango-workflow-starter`。业务模块不要调用 `mango-workflow-core` 的内部 Service，也不要直接查询 Workflow 表。

### 前端

安装并引入样式：

```bash
pnpm add @mango/workflow
```

如果使用管理页面，在启动阶段注册：

```ts
import { registerMangoWorkflowAdminPages } from '@mango/workflow/admin-pages';
import '@mango/workflow/style.css';

registerMangoWorkflowAdminPages();
```

业务页面展示进度时使用公开 API：

```ts
import { workflowApi } from '@mango/workflow';

const progress = await workflowApi.businessApplyLatestProgress('contract', contractId);
```

如果是自定义业务申请页或审批页，在启动阶段注册对应的 `applyPageKey` 或 `approvePageKey`，不要把注册表当成权限控制。

## 谁负责什么

| 部分 | 负责什么 |
|---|---|
| 业务模块 | 业务单据、业务状态、业务数据权限和状态回写 |
| `mango-workflow-api` | 业务可依赖的命令、查询、VO、枚举和事件契约 |
| `mango-workflow-starter` | 承载 Workflow 服务能力的应用装配 |
| `@mango/workflow` | 管理页面、业务组件和前端 API |
| 后端权限/租户上下文 | 校验当前用户、租户、任务和业务数据访问 |

## 容易混淆的词

| 词 | 直白解释 | 不代表什么 |
|---|---|---|
| 业务申请 | Workflow 关联的一条业务审批事实 | 不是业务模块的原始单据 |
| 流程定义 | 描述审批节点和表单入口的可发布配置 | 不是业务状态机全部规则 |
| 任务办理 | 审批人对当前待办执行动作 | 不自动替业务模块更新业务表 |
| `designerJson` / `formJson` | 流程设计和表单配置的快照/配置 | 不是让业务直接依赖 Workflow core |

## 失败时先看哪里

| 现象 | 先检查 |
|---|---|
| 管理页面没有显示 | `registerMangoWorkflowAdminPages()`、菜单 component、页面 key 和样式 |
| 不能发起流程 | 当前租户是否存在已发布定义，是否有流程定义/发起权限 |
| 业务进度为空 | `businessType`、业务主键、申请记录和当前租户上下文 |
| 审批后业务状态不变 | 业务模块自己的事件订阅或状态回写，不要只查 Workflow 页面 |
| 设计器候选项 403 | 使用 `workflow:definition:query` 和后端统一候选接口，不要追加跨域权限 |

## 权威来源

- [Workflow 后端 README](../../../mango/mango-platform/mango-workflow/README.md)
- [@mango/workflow README](../../../mango-ui/packages/workflow/README.md)
- [业务审批接入](../business-integration/workflow-business-approval.md)
- [Workflow 术语边界规则](../../../mango-pmo/rules/08-capability-docs.md)
