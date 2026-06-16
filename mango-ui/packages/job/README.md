# @mango/job

## 1. 概览
`@mango/job` 是 Mango Admin 的任务调度管理前端包，配套后端 `mango-platform/mango-job` 使用。它提供 admin page 注册入口、任务管理页面和 `/job/**` API 请求封装。

这个包标识为 **admin-pages 配套插件**：

- 适合：Mango Admin、内部运营后台、业务管理后台。
- 不适合：官网、营销站、C 端站点、非 Mango Admin Shell 的独立站点。
- 原因：页面依赖 `@mango/admin-pages` 页面注册机制、`@mango/common` 请求封装、Element Plus、Mango 后端权限和租户上下文。

## 2. 适用场景
- 注册 `mango-job` 模块页面到 Admin Shell。
- 提供任务定义、执行实例、Worker 节点、告警规则、运行状态五个管理页面。
- 封装任务定义、实例、日志、Worker、handler、告警、引擎状态 API。
- 导出前端枚举选项，用于状态展示和表单选择。
- 兼容后端分页结构中的 `list`、`records`、`rows`、`data`。

## 3. 边界说明
- 不提供任务调度后端运行时。
- 不保存任务数据，不包含数据库 migration。
- 不替代业务任务 handler 实现。
- 不负责菜单和权限数据初始化。
- 不作为官网、营销站、C 端站点或非 Mango Admin Shell 的通用组件库使用。

## 4. 模块组成
本包只提供前端视图、API 封装、类型定义和 admin page 注册。任务调度、数据库、菜单入库、按钮权限、租户校验、Worker 注册和失败告警都由后端 `mango-job` 及 authorization/notice 能力承担。

## 5. 接入方式
安装依赖：

```json
{
  "dependencies": {
    "@mango/job": "1.0.1"
  }
}
```

宿主 Admin 入口注册页面：

```ts
import { registerMangoJobAdminPages } from '@mango/job/admin-pages';
import '@mango/job/style.css';

registerMangoJobAdminPages();
```

`registerMangoJobAdminPages()` 内部会调用 `@mango/admin-pages/core` 的 `registerModulePages`，模块编码固定为 `mango-job`。函数带幂等保护，多次调用只会注册一次。

## 6. 功能清单

| 能力 | 常用入口 |
|------|----------|
| 注册 mango-job 模块页面到 Admin Shell | 前端注册 / 组件 / API 封装 |
| 提供任务定义、执行实例、Worker 节点、告警规则、运行状态五个管理页面 | 前端注册 / 组件 / API 封装 |
| 封装任务定义、实例、日志、Worker、handler、告警、引擎状态 API | 前端注册 / 组件 / API 封装 |
| 导出前端枚举选项，用于状态展示和表单选择 | 前端注册 / 组件 / API 封装 |
| 兼容后端分页结构中的 list、records、rows、data | 前端注册 / 组件 / API 封装 |

## 7. 页面 key 和菜单资源

页面 key 来自 `src/admin-pages.ts`：

| 页面 key | 页面组件 | 对应后端 manifest 菜单 |
|----------|----------|------------------------|
| `job/definition/index` | `JobDefinitionView` | 任务定义 |
| `job/instance/index` | `JobInstanceView` | 执行实例 |
| `job/worker/index` | `JobWorkerView` | Worker 节点 |
| `job/alarm/index` | `JobAlarmView` | 告警规则 |
| `job/engine/index` | `JobEngineView` | 运行状态 |

后端 `mango-job-starter` 的 `META-INF/mango/resource-manifest.json` 会把这些 key 写到菜单 `component` 字段。前端只负责注册 key，菜单入库和角色授权由 `mango-authorization` 处理。

## 8. API 与扩展
`jobApi` 统一使用 `@mango/common/utils/request` 的 `get`、`post`、`put`、`del`，接口前缀固定为 `/job`。

任务定义：

```ts
import { jobApi, type SaveJobDefinitionPayload } from '@mango/job';

const payload: SaveJobDefinitionPayload = {
  appCode: 'order',
  ownerService: 'order-service',
  workerGroup: 'order-worker',
  jobCode: 'order_timeout_close',
  jobName: '订单超时关闭',
  jobType: 'BUILTIN',
  scheduleType: 'MANUAL',
  handlerName: 'orderTimeoutCloseJobHandler',
  paramValue: '{"limit":100}',
  engineType: 'MANGO_NATIVE',
};

const jobId = await jobApi.createDefinition(payload);
await jobApi.updateDefinitionStatus(jobId, 'ENABLED');
const instanceId = await jobApi.triggerDefinition({
  jobId,
  triggerBatchNo: 'manual-20260615-001',
  paramValue: '{"limit":20}',
});
const log = await jobApi.detailInstanceLog(instanceId);
```

Worker 登记：

```ts
await jobApi.createWorker({
  appCode: 'order',
  serviceCode: 'order-service',
  workerGroup: 'order-worker',
  workerAddress: 'http://order-service.internal:8080',
  runtimeAddress: 'http://order-service.internal:8080',
  transportType: 'HTTP_INTERNAL',
  handlers: [
    {
      appCode: 'order',
      serviceCode: 'order-service',
      workerGroup: 'order-worker',
      handlerName: 'orderTimeoutCloseJobHandler',
      supportedJobCodes: ['order_timeout_close'],
      jobType: 'BUILTIN',
    },
  ],
});
```

告警规则：

```ts
await jobApi.createAlarmRule({
  appCode: 'order',
  ruleName: '订单任务失败告警',
  alarmType: 'INSTANCE_FAILED',
  noticeSceneCode: 'job.instance.failed',
  noticeTemplateCode: 'job-instance-failed',
  noticeParams: '{"userIds":[1,2]}',
  enabled: true,
});
```

## 9. API 清单

| 方法 | HTTP | 用途 |
|------|------|------|
| `pageDefinitions(params)` | `GET /job/definitions/page` | 任务定义分页 |
| `detailDefinition(id)` | `GET /job/definitions/detail` | 任务定义详情 |
| `createDefinition(data)` | `POST /job/definitions` | 新增任务定义 |
| `updateDefinition(data)` | `PUT /job/definitions` | 修改任务定义 |
| `updateDefinitionStatus(id, status)` | `PUT /job/definitions/status` | 调整任务状态 |
| `deleteDefinition(id)` | `DELETE /job/definitions` | 删除任务 |
| `triggerDefinition(data)` | `POST /job/definitions/trigger` | 手动触发 |
| `pageInstances(params)` | `GET /job/instances/page` | 执行实例分页 |
| `syncInstances(params)` | `POST /job/instances/sync` | 同步实例 |
| `detailInstanceLog(instanceId)` | `GET /job/instances/{id}/logs` | 实例 native 日志 |
| `pageLogs(params)` | `GET /job/logs/page` | 日志索引分页 |
| `detailLog(id)` | `GET /job/logs/detail` | 日志详情 |
| `pageWorkers(params)` | `GET /job/workers/page` | Worker 分页 |
| `createWorker(data)` | `POST /job/workers` | 手动登记 Worker |
| `updateWorkerStatus(data)` | `PUT /job/workers/status` | 调整 Worker 状态 |
| `listHandlers()` | `GET /job/handlers` | 当前应用 handler 清单 |
| `pageAlarmRules(params)` | `GET /job/alarm-rules/page` | 告警规则分页 |
| `detailAlarmRule(id)` | `GET /job/alarm-rules/detail` | 告警详情 |
| `createAlarmRule(data)` | `POST /job/alarm-rules` | 新增告警 |
| `updateAlarmRule(data)` | `PUT /job/alarm-rules` | 修改告警 |
| `updateAlarmRuleStatus(data)` | `PUT /job/alarm-rules/status` | 启停告警 |
| `deleteAlarmRule(id)` | `DELETE /job/alarm-rules` | 删除告警 |
| `listEngineStatus()` | `GET /job/engines/status` | 引擎同步状态 |

`normalizeParams` 会过滤 `''`、`undefined`、`null`，所以页面筛选为空时不会把空值传给后端。

## 10. 类型和枚举

主要前端类型：

- `JobDefinition`、`SaveJobDefinitionPayload`、`JobDefinitionQuery`
- `JobInstance`、`JobInstanceQuery`、`TriggerJobPayload`
- `JobLogIndex`、`JobLogDetail`、`JobLogQuery`
- `JobWorkerSnapshot`、`CreateJobWorkerPayload`、`JobWorkerHandlerPayload`
- `JobHandler`、`JobEngineStatus`
- `JobAlarmRule`、`SaveJobAlarmRulePayload`、`JobAlarmRuleQuery`

前端支持的枚举值与后端枚举对应：

| 类型 | 值 |
|------|----|
| `JobDefinitionStatus` | `DRAFT`、`ENABLED`、`DISABLED`、`PAUSED` |
| `JobEngineType` | `MANGO_NATIVE` |
| `JobType` | `BUILTIN` |
| `JobScheduleType` | `CRON`、`FIXED_RATE`、`ONE_TIME`、`MANUAL` |
| `JobSyncStatus` | `PENDING`、`SYNCED`、`FAILED` |
| `JobTransportType` | `IN_MEMORY`、`HTTP_INTERNAL` |
| `JobWorkerRegisterSource` | `EMBEDDED_AUTO`、`REMOTE_AUTO`、`MANUAL` |
| `JobInstanceStatus` | `CREATED`、`WAITING`、`DISPATCHED`、`RUNNING`、`RETRY_WAITING`、`SUCCESS`、`FAILED`、`TIMEOUT`、`CANCELED` |
| `JobTriggerType` | `SCHEDULED`、`MANUAL`、`RETRY`、`API` |
| `JobAlarmType` | `INSTANCE_FAILED` |
| `JobWorkerStatus` | `REGISTERED`、`ONLINE`、`DRAINING`、`OFFLINE`、`EXPIRED`、`DISABLED`、`UNKNOWN` |

## 11. 配置说明
本包没有独立配置文件，也不会自己配置后端地址、token、菜单、租户。宿主应用必须提供：

| 宿主能力 | 来源 |
|----------|------|
| 请求基地址、鉴权 header、错误处理 | `@mango/common/utils/request` 的宿主配置 |
| 菜单数据和 component key | 后端 authorization 菜单资源 |
| 页面注册容器 | `@mango/admin-pages` |
| Element Plus 和 Vue | peer dependency |
| `/job/**` 后端接口 | `mango-platform/mango-job` |

## 12. 数据与初始化
本包不包含数据库 migration，也不会初始化菜单。任务表、示例任务、Worker 表、告警规则表和菜单权限资源由后端 `mango-platform/mango-job` 提供。

## 13. 管理入口
页面 key 必须与后端 manifest 中的菜单 `component` 一致。按钮权限和租户边界由后端 `/job/**` 接口校验，前端只负责展示页面和触发请求。

关键页面 key：

- `job/definition/index`
- `job/instance/index`
- `job/worker/index`
- `job/alarm/index`
- `job/engine/index`

## 14. 快速开始
1. 后端业务服务接入 `mango-job-starter` 或 `mango-job-starter-remote`，并实现 `MangoJobHandler`。
2. Admin 宿主依赖 `@mango/job`，调用 `registerMangoJobAdminPages()` 并引入样式。
3. authorization 同步 `mango-job` manifest，当前用户获得任务管理菜单和按钮权限。
4. Worker 页面能看到在线 Worker。
5. 任务定义页面新建并启用任务，手动触发后执行实例和日志页面能看到结果。

## 15. 问题排查
| 现象 | 原因 | 处理 |
|------|------|------|
| 菜单点击后空白 | 没调用 `registerMangoJobAdminPages()` 或没引入页面包 | 在 Admin 启动入口注册 `@mango/job/admin-pages` |
| 页面样式缺失 | 没引入 `@mango/job/style.css` | 在宿主入口引入样式 |
| 接口 404 | 后端未启用 `mango-job-starter`，或代理没有转发 `/job` | 检查后端依赖和网关路径 |
| 接口 401/403 | 登录态或权限缺失 | 检查 token、角色和 manifest 权限同步 |
| Worker 页面为空 | 后端没有内嵌 Worker 或远程 Worker 注册 | 看后端 `mango.job.native.*` 配置 |
| 固定频率保存失败 | 表达式不是后端要求的毫秒数字符串 | 填 `1000` 到 `119999` 之间的值 |

## 16. 相关文档
- [Job 页面说明](./src/views/README.md)
- [Mango Job 后端模块](../../../mango/mango-platform/mango-job/README.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 17. 历史资料
- [Job 页面说明](./src/views/README.md)
- [Mango Job 后端模块](../../../mango/mango-platform/mango-job/README.md)
