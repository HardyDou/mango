# Job Views

## 1. 入口定位

本入口说明 `@mango/job` 暴露给 Mango Admin 的任务调度页面入口。它们是页面注册入口，不是复用业务表单组件。

## 2. 公开导出

来自 `@mango/job`：

- `JobAlarmView`
- `JobDefinitionView`
- `JobEngineView`
- `JobInstanceView`
- `JobWorkerView`

来自 `@mango/job/admin-pages`：

- `registerMangoJobAdminPages()`

页面 key：

- `job/alarm/index`
- `job/definition/index`
- `job/instance/index`
- `job/worker/index`
- `job/engine/index`

## 3. 使用场景

- Mango Admin 装配任务定义、执行实例、Worker 节点、告警规则和运行时状态页面。
- 业务项目使用 Mango Job 时，把任务管理页面接入后台菜单。
- 任务运维人员排查任务同步、实例日志、Worker 在线状态和告警配置。

## 4. 接入方式

```ts
import { registerMangoJobAdminPages } from '@mango/job/admin-pages';
import '@mango/job/style.css';

registerMangoJobAdminPages();
```

菜单资源的 `component` 字段使用上面的页面 key。

## 5. Props / 参数 / 事件

这些页面组件不对外定义 props 或事件；数据来源是 `src/api/job.ts` 的 `jobApi`。

任务定义页面内部使用 `JobParamEditor` 渲染任务参数 schema，该组件不作为 package 公开导出。

## 6. 后端依赖

- 后端模块：`mango-platform/mango-job`。
- API 前缀：`/job/definitions`、`/job/instances`、`/job/logs`、`/job/workers`、`/job/handlers`、`/job/alarm-rules`、`/job/engines`。
- Worker 注册和实例同步由后端 JobCenter / Worker 运行时提供。

## 7. 权限 / 租户 / 数据边界

页面内使用的权限码包括：

- `job:engine:list`
- `job:worker:list`
- `job:worker:add`
- `job:definition:*`
- `job:alarm:*`

实际权限、租户和任务数据归属由后端 job 模块校验。前端只负责按按钮权限显示操作入口。

## 8. 验证方式

```bash
pnpm -F @mango/job build
```

页面验收入口：

- 任务定义：`job/definition/index`
- 执行实例：`job/instance/index`
- Worker：`job/worker/index`
- 告警规则：`job/alarm/index`
- 运行状态：`job/engine/index`

最小断言：

- 菜单 component key 能解析到对应页面。
- `/job/handlers` 能返回处理器列表。
- 任务定义、实例、Worker、告警、运行状态页面至少一个列表接口返回成功或明确错误态。

## 9. 常见问题

- 菜单打开空白时，先确认 `registerMangoJobAdminPages()` 已执行。
- Worker 页面为空时，检查后端 Worker 注册和 `/job/workers/page`。
- 触发任务失败时，检查任务状态、handler 名称和后端 JobCenter 日志。

## 10. 关联文档

- [@mango/job README](../../README.md)
- [Job 后端 README](../../../../../mango/mango-platform/mango-job/README.md)
- [能力地图](../../../../../mango-docs/capabilities/README.md)
- [能力说明维护规范](../../../../../mango-pmo/rules/08-capability-docs.md)
