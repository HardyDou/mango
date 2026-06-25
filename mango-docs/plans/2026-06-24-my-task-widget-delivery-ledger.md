# 我的任务小组件交付台账

## 1. 目标

新增“我的任务”系统小组件，展示当前登录人的工作流任务总数、待完成、进行中、已完成和已逾期数量，并支持跳转到已有任务列表页面。

## 2. 范围

- 新增 `GET /workflow/tasks/my/summary` 统计接口。
- 新增 `@mango/workflow` 前端 API 封装。
- 新增 `@mango/grid-widgets` 系统小组件 `system.my-task`。
- 工作台默认布局加入“我的任务”卡片。
- 更新设计文档、包 README 和 workflow README。

## 3. 不做什么

- 不修改 `@mango/grid-layout`。
- 不新增数据库表、菜单、角色或按钮权限。
- 不新增独立任务列表页面。
- 不把任务统计逻辑写入工作台页面。

## 4. 设计输入

- 用户确认样式采用设计稿：标题、查看全部、任务总数、状态占比条、2x2 状态卡片。
- 用户确认数据来源优先使用后端新增统计接口，接口和数据能力收敛在小组件及其业务包内部。
- 设计文档：`mango-docs/designs/mango-grid-widgets-my-task-design.md`。

## 5. 交付台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|----|------|------|----------|--------|----------|------|----------|
| TASK-001 | 用户要求 | 新增我的任务小组件 | 使用 `system.my-task`，组件内部消费工作流统计 API | `mango-ui/packages/grid-widgets/src/system/my-task` | `pnpm.cmd -F @mango/grid-widgets build` | DONE | 构建日志 |
| TASK-002 | 用户要求 | 展示任务总数和四个状态 | 后端返回 `total/pending/processing/completed/overdue` | `WorkflowMyTaskSummaryVO` | `WorkflowTaskRuntimeServiceImplTest` | DONE | 单测日志 |
| TASK-003 | 用户要求 | 点击可跳转到对应模块 | 小组件通过 `runtime.navigate` 发出跳转意图 | `MyTaskWidget.vue` | `pnpm.cmd -F @mango/grid-widgets build` | DONE | 构建日志 |
| TASK-004 | 项目规范 | 工作台只写个性化布局 | 工作台仅新增默认布局项，不写业务查询逻辑 | `admin-shell/src/views/home/index.vue` | `pnpm.cmd -F @mango/admin-shell build` | DONE | 构建日志 |
| TASK-005 | 项目规范 | 文档同步 | 新增设计文档和交付台账，更新 README | `mango-docs`、README | `check-capability-docs.mjs` | DONE | PMO 检查日志 |

## 6. 验收证据记录

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---------|-----------|--------|----------|----------|-------------|----------------------|----------------|------|
| TASK-001 | 组件库 | 我的任务小组件可构建 | 本地源码 | 构建产物包含 `my-task.js` | 小组件结构、注册入口和样式通过构建校验 | 不涉及真实网络请求 | `pnpm.cmd -F @mango/grid-widgets build` | 通过 |
| TASK-002 | `/workflow/tasks/my/summary` | 当前用户任务统计 | 单测 mock 数据 | `total/pending/processing/completed/overdue` 聚合正确 | 不涉及页面交互 | 后端单测通过，无接口运行态网络请求 | `WorkflowTaskRuntimeServiceImplTest` | 通过 |
| TASK-003 | 工作台 | 查看全部和状态跳转 | 本地源码 | `runtime.navigate` 参数包含 `path` 和 `raw.query` | 组件构建通过，页面运行态待启动服务后人工确认 | 不涉及真实网络请求，跳转意图由代码发出 | `pnpm.cmd -F @mango/grid-widgets build` | 通过 |
| TASK-004 | 工作台默认布局 | 默认展示我的任务卡片 | 本地源码 | 默认布局包含 `widgetType=system.my-task` | admin-shell 构建通过，页面运行态待启动服务后人工确认 | 不涉及真实网络请求 | `pnpm.cmd -F @mango/admin-shell build` | 通过 |
| TASK-005 | 文档检查 | 文档与能力说明同步 | 本地改动文件 | PMO 能力文档检查通过 | 不涉及页面交互 | 不涉及真实网络请求 | `node mango-pmo/tools/check-capability-docs.mjs` | 通过 |

## 7. 风险与限制

- 本地服务如果仍加载旧 workflow jar，新接口可能报资源不存在，需要重新安装 workflow 模块并重启后端。
- 已完成数量来自历史任务，可能与当前运行任务数量差异较大，这是统计口径决定的结果。
- 小组件依赖 `workflow:task:list` 接口权限；无权限时展示局部错误，不影响工作台其它卡片。
- 本轮已完成构建和单测验证，页面运行态验证需要启动前后端后由用户在工作台继续确认。
