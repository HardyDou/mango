# @mango/job Views

## 1. 概览
`src/views` 是 `@mango/job` 提供给 Mango Admin Shell 的任务调度管理页面集合。它们是 **admin-pages 配套页面**，不是公共业务组件，也不适合官网、C 端站点或非 Mango Admin 的页面复用。

页面依赖：

- `@mango/admin-pages` 注册和菜单 component key。
- `@mango/common/utils/request` 请求封装。
- Element Plus 表格、表单、弹窗、抽屉和消息组件。
- 后端 `mango-platform/mango-job` 的 `/job/**` API。
- 后端 authorization 菜单和按钮权限。

## 2. 功能清单
页面 key 来自 `src/admin-pages.ts`，菜单入库来自后端 `resource-manifest.json`。

| 页面 key | 文件 | 导出 | 管理能力 |
|----------|------|------|----------|
| `job/definition/index` | `views/definition/index.vue` | `JobDefinitionView` | 任务定义列表、筛选、新增、编辑草稿、删除草稿、状态调整、手动触发、参数 JSON 编辑 |
| `job/instance/index` | `views/instance/index.vue` | `JobInstanceView` | 执行实例列表、任务筛选、实例同步、实例日志抽屉、日志自动刷新 |
| `job/worker/index` | `views/worker/index.vue` | `JobWorkerView` | Worker 快照列表、手动登记 HTTP Worker、Worker 状态治理 |
| `job/alarm/index` | `views/alarm/index.vue` | `JobAlarmView` | 失败实例告警规则列表、新增、编辑、启停、删除、任务范围选择、通知参数编辑 |
| `job/engine/index` | `views/engine/index.vue` | `JobEngineView` | Mango 原生引擎同步状态统计 |

页面组件不定义对外 props 或事件。业务项目按菜单 component key 接入，不把这些页面当作二次开发表单嵌入。

## 3. 页面入口
宿主 Admin 启动入口：

```ts
import { registerMangoJobAdminPages } from '@mango/job/admin-pages';
import '@mango/job/style.css';

registerMangoJobAdminPages();
```

菜单资源的 `component` 字段使用页面 key，例如：

```json
{
  "menuName": "任务定义",
  "path": "/job/definition",
  "component": "job/definition/index",
  "permissions": ["job:definition:list"]
}
```

后端 `mango-job-starter` 已在 `META-INF/mango/resource-manifest.json` 声明这些菜单。通常不需要业务项目手写菜单 SQL，只需要确保 authorization 的 manifest 同步和角色授权生效。

## 4. 后端依赖
| 页面 | 主要接口 |
|------|----------|
| 任务定义 | `GET /job/definitions/page`、`POST /job/definitions`、`PUT /job/definitions`、`PUT /job/definitions/status`、`DELETE /job/definitions`、`POST /job/definitions/trigger`、`GET /job/handlers` |
| 执行实例 | `GET /job/instances/page`、`POST /job/instances/sync`、`GET /job/instances/{instanceId}/logs`、`GET /job/definitions/page` |
| Worker 节点 | `GET /job/workers/page`、`POST /job/workers`、`PUT /job/workers/status` |
| 告警规则 | `GET /job/alarm-rules/page`、`GET /job/alarm-rules/detail`、`POST /job/alarm-rules`、`PUT /job/alarm-rules`、`PUT /job/alarm-rules/status`、`DELETE /job/alarm-rules`、`GET /job/definitions/page` |
| 运行状态 | `GET /job/engines/status` |

接口封装统一在 `../api/job.ts` 的 `jobApi` 中。页面只依赖 `jobApi`，不直接拼接底层 request。

## 5. 页面里的关键字段

任务定义页面保存 `SaveJobDefinitionPayload`：

| 字段 | 页面含义 |
|------|----------|
| `appCode` | 所属应用 |
| `ownerService` | 执行服务编码；为空时后端按 `appCode` 处理 |
| `workerGroup` | Worker 分组；为空时后端按 `ownerService` 处理 |
| `jobCode` | 任务编码 |
| `jobName` | 任务名称 |
| `jobType` | 当前只支持 `BUILTIN` |
| `scheduleType` | `CRON`、`FIXED_RATE`、`ONE_TIME`、`MANUAL` |
| `scheduleExpression` | 调度表达式；`MANUAL` 可为空 |
| `handlerName` | 处理器名称 |
| `paramSchema` | 参数表单 schema JSON |
| `paramValue` | 默认参数 JSON |
| `engineType` | 当前只支持 `MANGO_NATIVE` |

`views/definition/JobParamEditor.vue` 只在任务定义页面内部使用，用于编辑参数 schema 和默认参数；它没有从 package 主入口导出，不是公共组件。

Worker 页面登记 `CreateJobWorkerPayload`，只支持手动登记 `HTTP_INTERNAL` Worker。内嵌 Worker 和远程 Worker 自动注册由后端运行时完成。

告警页面保存 `SaveJobAlarmRulePayload`，告警类型当前只支持 `INSTANCE_FAILED`。通知参数 JSON 支持 `userId`、`userIds`、`recipientRuleCode`，最终由后端 `mango-notice` 发送。

## 6. 管理入口
页面入口权限来自后端 manifest：

| 页面 | 入口权限 | 操作权限 |
|------|----------|----------|
| 任务定义 | `job:definition:list` | `job:definition:query`、`job:definition:add`、`job:definition:edit`、`job:definition:delete`、`job:definition:status`、`job:definition:trigger` |
| 执行实例 | `job:instance:list` | `job:instance:sync`、`job:log:list` |
| Worker 节点 | `job:worker:list` | `job:worker:add`、`job:worker:status` |
| 告警规则 | `job:alarm:list` | `job:alarm:query`、`job:alarm:add`、`job:alarm:edit`、`job:alarm:status`、`job:alarm:delete` |
| 运行状态 | `job:engine:list` | 无额外写操作 |

前端页面可以按按钮权限展示操作入口，但最终权限、租户和数据边界由后端 `/job/**` 接口校验。

## 7. 问题排查
| 现象 | 原因 | 处理 |
|------|------|------|
| 菜单打开空白 | 页面 key 未注册 | 确认调用 `registerMangoJobAdminPages()` |
| 页面能打开但样式异常 | 未引入样式 | 引入 `@mango/job/style.css` |
| 列表 404 | 后端没有启用 `mango-job-starter` 或代理未转发 `/job` | 检查后端依赖和网关 |
| 创建任务后触发失败 | handler 归属字段与 Worker 能力不匹配 | 对齐 `appCode`、`ownerService`、`workerGroup`、`handlerName`、`jobCode` |
| 日志抽屉一直刷新但无内容 | 实例未执行完成或日志尚未归档 | 检查实例状态、Worker 在线状态和后端日志 |
| 告警规则保存成功但无通知 | `mango-notice` 未启用或通知接收人配置不对 | 检查后端实例日志和 `noticeParams` |

## 8. 相关文档
- [@mango/job](../../README.md)
- [Mango Job 后端模块](../../../../../mango/mango-platform/mango-job/README.md)
- [能力说明维护规范](../../../../../mango-pmo/rules/08-capability-docs.md)
