# 我的待办小组件交付契约

## 1. 目标

新增“我的待办”系统小组件，展示当前登录人的工作流待办概览，并提供跳转到已有待办页面的入口。

## 2. 范围

- 新增工作流待办统计接口。
- 新增 `@mango/workflow` 前端 API 封装。
- 新增 `@mango/grid-widgets` 系统小组件 `system.my-todo`。
- 工作台和任务列表支持小组件统计块携带的路由 query，待确认可进入未读抄送列表，已超时可进入超时待办列表。
- 更新相关 README 与设计文档交付记录。

## 3. 不做什么

- 不修改 `@mango/grid-layout` 布局、拖拽和保存能力。
- 不新增菜单、角色、按钮权限和数据库表。
- 不在工作台页面重复实现待办统计逻辑。
- 不做小组件级权限过滤，数据权限由后端接口控制。

## 4. 设计输入

- `mango-docs/designs/mango-grid-widgets-my-todo-design.md`
- 已确认设计图：标题“我的待办”、右侧“查看全部”、2x2 四个统计块。

## 5. 设计说明

### 5.1 影响模块

- `mango-ui/packages/grid-widgets`
- `mango-ui/packages/workflow`
- `mango/mango-platform/mango-workflow`

### 5.2 接口变化

- 新增 `GET /workflow/tasks/todo/summary`。

### 5.3 数据变化

- 不新增数据库表。
- 后端实时统计当前用户待审批、待处理、待确认和已超时数量。

### 5.4 菜单/页面/权限变化

- 不新增菜单。
- 统计接口复用 `workflow:task:list` 接口权限。
- 抄送列表新增 `unread=true` 查询参数，用于待确认入口筛选未读抄送。

### 5.5 测试范围

- 后端编译和接口方法编译检查。
- `@mango/workflow` 构建。
- `@mango/grid-widgets` 构建。
- `git diff --check`。
- PMO 文档和交付契约检查。

## 6. 风险与限制

- `待确认` 第一版按未读抄送统计。
- `已超时` 依赖 Flowable 任务 due date；未设置 due date 时统计为 0。
- `@mango/grid-widgets` 新增对 `@mango/workflow` 的依赖，需要通过包构建验证依赖边界。
- 本次未启动完整前后端服务做浏览器联调，最终页面数据需在服务运行后再验收。

## 7. 交付台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| TASK-001 | 用户要求 | 严格参照设计图开发我的待办小组件 | 后端统计接口 + 小组件内部消费 | `system.my-todo` 小组件 | 页面构建与样式检查 | DONE | `pnpm.cmd -F @mango/grid-widgets build` |
| TASK-002 | 用户要求 | 小组件能力内聚，不放到工作台页面 | 在 `@mango/grid-widgets` 内部调用 `@mango/workflow` API | 前端 API 与小组件目录 | 包构建 | DONE | `pnpm.cmd -F @mango/workflow build`、`pnpm.cmd -F @mango/grid-widgets build` |
| TASK-003 | 设计方案 | 统计数据来自真实后端接口 | 新增 `/workflow/tasks/todo/summary` | 后端 VO/Service/Controller | 后端编译 | DONE | `mvn.cmd -pl mango-platform/mango-workflow/mango-workflow-api,mango-platform/mango-workflow/mango-workflow-core,mango-platform/mango-workflow/mango-workflow-starter -am -DskipTests compile` |
| TASK-004 | 设计方案 | 点击统计块跳转到已有任务页面 | 小组件通过 `raw.query` 传参，工作台转成路由 query，待确认筛选未读抄送，已超时筛选超时待办 | 工作台导航、任务列表查询 | 前端包构建与后端编译 | DONE | `pnpm.cmd -F @mango/workflow build`、`mvn.cmd -pl mango-platform/mango-workflow/mango-workflow-api,mango-platform/mango-workflow/mango-workflow-core,mango-platform/mango-workflow/mango-workflow-starter -am -DskipTests compile` |

## 8. 验收证据记录

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| TASK-001 | 工作台组件库 | 我的待办小组件注册展示 | 当前用户 | `systemGridWidgets` 包含 `system.my-todo`，包构建通过 | 组件样式随 `@mango/grid-widgets/style.css` 输出 | 不涉及浏览器运行 | `pnpm.cmd -F @mango/grid-widgets build` | 通过 |
| TASK-002 | `@mango/grid-widgets` | 小组件内部请求统计接口 | 构建环境 | `@mango/workflow` 与 `@mango/grid-widgets` 构建通过 | 不涉及页面运行 | 不涉及浏览器运行 | `pnpm.cmd -F @mango/workflow build`、`pnpm.cmd -F @mango/grid-widgets build` | 通过 |
| TASK-003 | `/workflow/tasks/todo/summary` | 返回四类统计 | 当前用户 | 后端相关模块编译通过 | 不涉及 | 未启动服务实测接口 | `mvn.cmd -pl mango-platform/mango-workflow/mango-workflow-api,mango-platform/mango-workflow/mango-workflow-core,mango-platform/mango-workflow/mango-workflow-starter -am -DskipTests compile` | 通过 |
| TASK-004 | 工作台/任务列表 | 统计块跳转、未读抄送筛选和超时待办筛选 | 当前用户 | query 参数可进入路由并被任务列表转换为查询参数 | 未做浏览器运行 | 未启动服务实测接口 | 前端构建、后端编译 | 通过 |
