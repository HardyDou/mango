# Issue 275 Workflow 标准任务详情运行时数据修复交付契约

## 1. 目标

修复标准任务详情页依赖流程定义管理接口的问题。审批办理人打开业务审批任务详情时，应只依赖任务、流程实例、业务申请和运行时渲染数据，不因为缺少流程定义管理数据或权限导致页面异常。

## 2. 范围

- `@mango/workflow` 任务详情页数据加载。
- Workflow 任务/流程运行时详情类型归一化。
- 任务详情页 Vitest 回归用例。
- 业务审批接入文档影响记录。

## 3. 不做什么

- 不修改后端流程定义、任务、实例和业务申请接口。
- 不修改业务流程定义、菜单、权限码或数据库数据。
- 不改变任务审批、认领、退回、转办和委派等动作接口。
- 不发布 npm 包。

## 4. 设计输入

- GitHub issue #275：Workflow 标准任务详情不应让办理人依赖流程定义管理接口。
- 问题现场：`GET /workflow/tasks/detail?taskId=...` 正常，但任务详情页继续调用 `GET /workflow/definitions/versions`、`GET /workflow/definitions/page`，当定义不可见或办理人无定义管理权限时影响页面加载。

## 5. 设计说明

### 5.1 影响模块

- `mango-ui/packages/workflow/src/api/workflow.ts`
- `mango-ui/packages/workflow/src/views/task-detail/index.vue`
- `mango-ui/packages/workflow/src/views/task-detail/__tests__/taskDetail.spec.ts`
- `mango-ui/packages/workflow/package.json`
- `mango-ui/packages/workflow/vitest.config.ts`

### 5.2 接口变化

无后端 API 变化。前端运行时详情模型允许接收可选 `designerJson` 字段；若后端任务详情或流程详情携带设计器快照，任务详情页可直接用于右侧流程图渲染。

### 5.3 数据变化

无数据库变化。前端不再为了任务详情页主动查询流程定义管理列表、详情或版本接口。

### 5.4 页面/权限变化

- 标准任务详情页不再调用流程定义管理接口。
- 办理人只需要具备任务详情和任务办理相关权限。
- 有运行时 `designerJson` 时，右侧流程图使用运行时快照渲染。
- 无运行时 `designerJson` 时，页面继续展示业务表单、操作区和审批记录，不因流程定义管理接口失败阻断办理。

### 5.5 测试范围

- 单元测试覆盖业务风险审批任务详情可用运行时 `formJson`、变量和渲染配置完成渲染。
- 单元测试断言任务详情页不会调用 `definitionVersions`、`definitionDetail`、`definitionsPage`。
- 包构建验证 `@mango/workflow` 生产构建和类型生成。

## 6. 风险与限制

- 本次不补后端运行时快照字段持久化逻辑；若运行时详情未携带 `designerJson`，任务详情页会降级到审批记录侧栏。
- 本次未启动浏览器 E2E；改动集中在数据加载依赖和已有任务详情页单测，已用回归用例和包构建覆盖。
- 本次不发布 npm 包；合并后如需业务项目消费，需要按后续 release 流程发布 `@mango/workflow`。

## 7. 交付台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| TASK-001 | issue #275 问题描述 | 任务详情页不依赖流程定义管理接口 | 删除任务详情页定义版本、定义详情、定义分页查询链路 | `task-detail/index.vue` | Vitest 回归用例 | DONE | 本文件第 8 节 |
| TASK-002 | issue #275 预期行为 | 办理人能基于运行时数据查看和办理任务 | 使用 `taskDetail` 的 `formJson`、变量、`renderConfig` 和可选 `designerJson` 渲染 | `workflow.ts`、`task-detail/index.vue` | Vitest 回归用例、包构建 | DONE | 本文件第 8 节 |
| TASK-003 | issue #275 权限边界 | 缺少定义管理权限不阻断任务办理页 | 测试中让定义管理 API mock 失败，并断言未被调用 | `taskDetail.spec.ts` | `pnpm exec vitest run --config vitest.config.ts src/views/task-detail/__tests__/taskDetail.spec.ts` | DONE | 本文件第 8 节 |
| TASK-004 | PMO 文档要求 | 记录业务接入影响 | 在业务审批接入文档补充影响说明 | `workflow-business-approval.md` | 文档审阅、`git diff --check` | DONE | 本文件第 8 节 |

## 8. 验收证据记录

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| TASK-001 | `/workflow/task/detail` | 不调用定义管理接口 | taskId `task-risk-1`，processKey `WF_GUARANTEE_RISK_REVIEW` | `definitionVersions`、`definitionDetail`、`definitionsPage` 调用次数为 0 | 未启动浏览器，组件测试覆盖 | 测试中定义管理 API mock 为 rejected，页面仍渲染 | 任务详情 Vitest 通过 | DONE |
| TASK-002 | 标准任务详情 | 运行时表单渲染 | `formJson` 包含 `riskTitle` 字段，变量包含业务类型和标题 | 页面渲染 `保函风控审批` 和 `风控标题` | 组件测试覆盖表单渲染 | 不依赖定义管理接口 | 任务详情 Vitest 通过 | DONE |
| TASK-003 | `@mango/workflow` 包 | 生产构建 | 当前 workflow 包源码 | Vite 构建和类型生成成功 | 不适用 | 不适用 | `pnpm --filter @mango/workflow build` 通过 | DONE |
| TASK-004 | 业务接入文档 | 影响说明 | Issue #275 | 文档说明办理人无需定义管理接口权限 | 不适用 | 不适用 | `node mango-pmo/tools/check-business-guides.mjs`、`git diff --check` 通过 | DONE |
