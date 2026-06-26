# 我的申请小组件交付台账

## 1. 目标

新增“我的申请”系统小组件，展示当前登录人发起的工作流申请状态概览，并支持跳转到已有我的申请页面按状态筛选。

## 2. 范围

- 新增 `GET /workflow/business-applies/my/summary` 统计接口。
- 新增 `@mango/workflow` 前端 API 封装。
- 新增 `@mango/grid-widgets` 系统小组件 `system.my-process`。
- 工作台默认布局加入“我的申请”卡片。
- `/workflow/task/initiated` 使用业务申请分页数据并支持状态筛选。
- 更新设计文档、包 README 和业务审批接入说明。

## 3. 不做什么

- 不修改 `@mango/grid-layout`。
- 不新增数据库表、菜单、角色或按钮权限。
- 不改变工作流发起、审批、撤回和回调主流程。
- 不做小组件级权限过滤。

## 4. 设计输入

- 用户确认“我的申请”小组件数据来自工作流。
- 统计口径：审核中 = `SUBMITTED + IN_APPROVAL`。
- 设计文档：`mango-docs/designs/mango-grid-widgets-my-process-design.md`。

## 5. 交付台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| TASK-001 | 用户要求 | 新增我的申请小组件 | 使用 `system.my-process`，组件内部消费工作流统计 API | `mango-ui/packages/grid-widgets/src/system/my-process` | `pnpm.cmd -F @mango/grid-widgets build` | DONE | 构建日志 |
| TASK-002 | 用户要求 | 统计当前用户发起的申请 | 后端按 `applicantId` 和申请状态聚合 | `/workflow/business-applies/my/summary` | `WorkflowBusinessApplyServiceImplTest` | DONE | 单测日志 |
| TASK-003 | 用户要求 | 支持审核中、已完成、已驳回、已撤回 | 统一基于 `workflow_business_apply.apply_status` | `WorkflowBusinessApplySummaryVO` | `WorkflowBusinessApplyServiceImplTest` | DONE | 单测日志 |
| TASK-004 | 用户要求 | 点击统计项跳到对应模块 | 小组件通过 `runtime.navigate` 传递 `raw.query.statuses` | `MyProcessWidget.vue`、`task-list/index.vue` | `pnpm.cmd -F @mango/workflow test -- --run src/views/task-list/__tests__/taskList.spec.ts` | DONE | 单测日志 |
| TASK-005 | 项目规范 | 文档同步 | 新增设计文档和交付台账，更新 README | `mango-docs`、`README.md` | PMO 文档检查 | DONE | PMO 检查日志 |

## 6. 验收证据记录

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/日志 | 结论 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| TASK-001 | 工作台组件库 | 我的申请小组件可展示 | 当前登录人 | 组件库包含 `system.my-process` | 构建通过 | 不涉及 | `pnpm.cmd -F @mango/grid-widgets build` | 通过 |
| TASK-002 | `/workflow/business-applies/my/summary` | 当前用户统计 | 当前登录人 | 返回四个数字字段 | 不涉及 | 后端单测通过 | `WorkflowBusinessApplyServiceImplTest` | 通过 |
| TASK-003 | 统计口径 | 审核中/完成/驳回/撤回 | 单测数据 | 四类字段映射正确 | 不涉及 | 不涉及 | `WorkflowBusinessApplyServiceImplTest` | 通过 |
| TASK-004 | `/workflow/task/initiated` | 状态筛选跳转 | `statuses=APPROVED` | 调用业务申请分页并传入状态 | 前端单测通过 | 不涉及 | `taskList.spec.ts` | 通过 |

## 7. 风险与限制

- 历史申请如果没有 `applicantId`，本次统计不会计入。
- 小组件依赖 `@mango/workflow` 的统计接口，业务系统如果未接入工作流模块，需要不要注册该系统小组件或自行处理接口权限。
- 我的申请页面改用业务申请分页数据后，字段展示会更贴近申请视角，但可能与原流程实例列表的部分字段含义不同。
- 本轮已完成构建和单测验证；页面运行态验证需要启动前后端服务后在工作台补充确认。
