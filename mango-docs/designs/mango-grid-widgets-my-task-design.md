# Mango 我的任务小组件设计方案

## 1. 背景

后台工作台已经通过 `@mango/grid-layout` 承载自定义布局，通过 `@mango/grid-widgets` 聚合系统小组件和业务小组件。本次新增系统预制小组件“我的任务”，用于展示当前登录人的工作流任务执行概览。

本次仍遵循小组件设计原则：

- 不修改 `@mango/grid-layout`，布局组件只负责布局和拖拽。
- 消费页面只负责传入 `runtime`、默认布局和个性化配置。
- 小组件内部负责自己的数据读取、展示、错误态和跳转意图。
- 跳转通过 `runtime.navigate` 交给宿主处理，兼容单体和微前端场景。

## 2. 目标

- 新增 `system.my-task` 系统小组件。
- 小组件展示标题“我的任务”、右侧“查看全部”、任务总数、状态占比条和 2x2 状态卡片。
- 状态卡片包括：待完成、进行中、已完成、已逾期。
- 新增后端统计接口，返回当前登录人的任务总览。
- 工作台默认布局展示“我的任务”卡片，组件库也能选择该小组件。

## 3. 不做范围

- 不新增数据库表。
- 不新增菜单、角色或按钮权限配置。
- 不修改自定义栅格布局组件。
- 不把任务统计逻辑写入工作台页面。
- 不新增独立“我的任务”列表页，点击后跳转到已有任务列表或已办列表。

## 4. 数据口径

后端新增接口：

```http
GET /workflow/tasks/my/summary
```

权限：

```java
@ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "workflow:task:list")
```

响应字段：

| 字段 | 含义 | 统计口径 |
|------|------|----------|
| `total` | 任务总数 | `pending + processing + completed + overdue` |
| `pending` | 待完成 | 当前用户可认领或候选处理的运行任务，复用 `todoType=CLAIMABLE` 口径 |
| `processing` | 进行中 | 当前用户已分配待处理的运行任务，复用 `todoType=ASSIGNED` 口径 |
| `completed` | 已完成 | 当前用户已处理完成的历史任务 |
| `overdue` | 已逾期 | 当前用户相关运行任务中超过到期时间的任务，按任务 ID 去重 |

## 5. 前端设计

新增目录：

```text
mango-ui/packages/grid-widgets/src/system/my-task/
├─ MyTaskWidget.vue
├─ index.ts
└─ my-task.ts
```

职责：

- `MyTaskWidget.vue`：展示卡片、加载统计、错误重试和状态跳转。
- `my-task.ts`：注册 `system.my-task` 的 `MangoGridWidgetDefinition`。
- `index.ts`：提供独立子路径导出。

默认注册：

```ts
{
  type: 'system.my-task',
  title: '我的任务',
  category: '系统组件',
  source: 'mango',
  moduleCode: 'workflow',
  defaultLayout: { w: 3, h: 10, minW: 3, minH: 10 },
  showTitle: false,
  padding: false,
}
```

点击行为：

| 点击区域 | 跳转目标 |
|----------|----------|
| 查看全部 | `/workflow/task/todo` |
| 待完成 | `/workflow/task/todo?todoType=CLAIMABLE` |
| 进行中 | `/workflow/task/todo?todoType=ASSIGNED` |
| 已完成 | `/workflow/task/done` |
| 已逾期 | `/workflow/task/todo?todoType=ALL&overdue=true` |

小组件只发出 `runtime.navigate({ path, raw: { query } })`，真实路由转换由宿主页面处理。

## 6. 后端设计

新增 VO：

```java
public class WorkflowMyTaskSummaryVO {
    private Long total;
    private Long pending;
    private Long processing;
    private Long completed;
    private Long overdue;
}
```

服务层：

- 在 `IWorkflowTaskRuntimeService` 增加 `myTaskSummary()`。
- 在 `WorkflowTaskRuntimeServiceImpl` 聚合当前用户待完成、进行中、已完成和已逾期任务。
- 已逾期统计复用当前运行任务查询，按任务 ID 去重，避免同一个任务同时命中候选和已分配时重复计数。

控制器：

- 在 `WorkflowTaskController` 暴露 `GET /workflow/tasks/my/summary`。

## 7. 影响范围

- `@mango/workflow` 增加 `WorkflowMyTaskSummary` 类型和 `workflowApi.myTaskSummary()`。
- `@mango/grid-widgets` 增加 `system.my-task` 小组件、样式、导出入口和包导出配置。
- `admin-shell` 工作台默认布局新增一张“我的任务”卡片。
- `mango-workflow` 后端新增统计接口。

## 8. 验证计划

- 后端单测覆盖 `myTaskSummary()` 聚合口径。
- 构建 `@mango/workflow`。
- 构建 `@mango/grid-widgets`。
- 执行 `git diff --check`。
- 启动前后端后，在工作台验证：
  - 默认布局出现“我的任务”卡片。
  - 加载成功时显示总数和四个状态。
  - 接口失败时显示局部错误和重试按钮。
  - 点击查看全部和状态项能跳转到对应已有页面。

## 9. 风险与处理

| 风险 | 说明 | 处理 |
|------|------|------|
| 统计口径和已有“我的待办”存在重叠 | “我的任务”是总览，“我的待办”是待办细分，两者展示目的不同 | 通过标题、总数和状态文案区分 |
| 新接口未同步资源表 | 本地后端如果仍加载旧 workflow jar，可能报资源不存在 | 需要 `mvn install` workflow 模块并重启后端 |
| 已完成数量较大 | 历史任务累计可能明显大于运行任务 | 本次按当前用户历史完成任务统计，后续如需时间范围再扩展接口 |
| 小组件无接口权限 | 无 `workflow:task:list` 时接口可能 403 | 小组件显示局部错误，不影响工作台其它卡片 |
