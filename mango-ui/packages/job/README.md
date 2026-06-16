# @mango/job

## 1. 概览

`@mango/job` 是 Mango Admin 的任务调度管理前端包，配套后端 `mango-platform/mango-job` 使用。它提供 `/job/**` API 封装、任务管理页面和 Admin Pages 注册入口。

这个包的集成形态是 `admin-pages`：

| 标识 | 说明 |
|------|------|
| 适合 | Mango Admin、内部运营后台、业务管理后台 |
| 不适合 | 官网、营销站、C 端站点、非 Mango Admin Shell 的独立站点 |
| 原因 | 依赖 `@mango/admin-pages` 页面注册、`@mango/common` 请求封装、Element Plus、后端权限和租户上下文 |

## 2. 功能清单

| 能力 | 说明 |
|------|------|
| 页面注册 | 通过 `registerMangoJobAdminPages()` 注册 `mango-job` 模块页面 |
| 任务定义页面 | 新建、编辑、启停、删除和手动触发任务 |
| 执行实例页面 | 查询实例、同步实例、查看实例 native 日志 |
| Worker 页面 | 查询 Worker、手动登记远程 Worker、调整 Worker 状态 |
| 告警规则页面 | 新增、编辑、启停和删除失败告警规则 |
| 运行状态页面 | 查看 native 引擎同步状态汇总 |
| API 封装 | 导出 `jobApi`、请求类型、返回类型和枚举选项 |
| 分页兼容 | 兼容后端分页结构中的 `list`、`records`、`rows`、`data` |

## 3. 接入方式

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

`registerMangoJobAdminPages()` 会调用 `@mango/admin-pages/core` 的 `registerModulePages`，模块编码固定为 `mango-job`。函数带幂等保护，多次调用只注册一次。

宿主应用必须已经具备这些能力：

| 宿主能力 | 来源 |
|----------|------|
| Vue 3 和 Element Plus | peer dependency |
| 请求基地址、鉴权 header、错误处理 | `@mango/common/utils/request` 的宿主配置 |
| 页面注册容器 | `@mango/admin-pages` |
| 菜单数据和 component key | 后端 authorization 菜单资源 |
| `/job/**` 接口 | 后端 `mango-platform/mango-job` |

## 4. 快速开始

1. 后端服务接入 `mango-job-starter` 或 `mango-job-starter-remote`，并实现业务 `MangoJobHandler`。
2. Admin 宿主依赖 `@mango/job`，调用 `registerMangoJobAdminPages()`，并引入 `@mango/job/style.css`。
3. 后端 `mango-authorization` 同步 `mango-job` 的资源 manifest。
4. 给当前用户角色绑定任务管理菜单和按钮权限。
5. 打开任务定义页面，新建任务并启用；手动触发后在执行实例和日志页面查看结果。

## 5. 配置说明

本包没有独立运行时配置文件，不会自己设置后端地址、token、租户或菜单。它读取宿主 Admin 已经配置好的请求封装和页面注册能力。

| 配置来源 | 影响 |
|----------|------|
| `@mango/common/utils/request` | 决定 `/job/**` 请求基地址、鉴权 header、错误处理和租户上下文传递 |
| `@mango/admin-pages` | 决定页面 key 如何挂载到 Admin Shell |
| 后端 authorization 菜单资源 | 决定左侧菜单、按钮权限和 component key |
| `@mango/job/style.css` | 提供本包页面样式 |

## 6. 管理入口

页面 key 来自 `src/admin-pages.ts`：

| 页面 key | 页面组件 | 后端菜单 |
|----------|----------|----------|
| `job/definition/index` | `JobDefinitionView` | 任务定义 |
| `job/instance/index` | `JobInstanceView` | 执行实例 |
| `job/worker/index` | `JobWorkerView` | Worker 节点 |
| `job/alarm/index` | `JobAlarmView` | 告警规则 |
| `job/engine/index` | `JobEngineView` | 运行状态 |

后端 `mango-job-starter/src/main/resources/META-INF/mango/resource-manifest.json` 会把这些 key 写入菜单 `component` 字段。前端只负责注册页面 key，菜单入库和角色授权由 `mango-authorization` 处理。

## 7. API 与扩展

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

`normalizeParams` 会过滤 `''`、`undefined`、`null`，所以页面筛选为空时不会把空值传给后端。

### 7.1 API 清单

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

## 8. 类型和枚举

主要前端类型：

| 类型 | 用途 |
|------|------|
| `JobDefinition`、`SaveJobDefinitionPayload`、`JobDefinitionQuery` | 任务定义 |
| `JobInstance`、`JobInstanceQuery`、`TriggerJobPayload` | 执行实例和手动触发 |
| `JobLogIndex`、`JobLogDetail`、`JobLogQuery` | 执行日志 |
| `JobWorkerSnapshot`、`CreateJobWorkerPayload`、`JobWorkerHandlerPayload` | Worker 管理 |
| `JobHandler`、`JobEngineStatus` | handler 清单和运行状态 |
| `JobAlarmRule`、`SaveJobAlarmRulePayload`、`JobAlarmRuleQuery` | 告警规则 |

枚举值：

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

前端还导出 `jobDefinitionStatusOptions`、`scheduleTypeOptions`、`engineTypeOptions`、`instanceStatusOptions`、`workerStatusOptions`、`transportTypeOptions`、`workerRegisterSourceOptions`、`alarmTypeOptions`、`enabledOptions` 等选项数组，用于表单和状态标签展示。

## 9. 数据与初始化

本包不包含数据库 migration，也不会初始化菜单或权限。

| 数据 | 来源 |
|------|------|
| 任务定义、实例、日志、Worker、告警规则 | 后端 `mango-platform/mango-job` |
| 菜单和按钮权限 | 后端 `mango-job-starter` 的资源 manifest，经 `mango-authorization` 同步入库 |
| 页面 component key | 本包 `registerMangoJobAdminPages()` 注册 |

## 10. 问题排查

| 现象 | 排查点 |
|------|--------|
| 菜单点击后空白 | 检查是否调用 `registerMangoJobAdminPages()`，以及页面 key 是否和后端 manifest 一致 |
| 页面样式缺失 | 检查宿主入口是否引入 `@mango/job/style.css` |
| 接口 404 | 检查后端是否启用 `mango-job-starter`，网关是否转发 `/job` |
| 接口 401/403 | 检查登录态、租户上下文、菜单权限和按钮权限 |
| Worker 页面为空 | 检查后端内嵌 Worker 或远程 Worker 是否注册成功 |
| 固定频率保存失败 | `FIXED_RATE` 的表达式应填毫秒数字符串，例如 `5000` |
| 触发后看不到日志 | 检查实例是否进入执行阶段，Worker 是否在线，handler 是否输出日志 |

## 11. 相关文档

- [Job 页面说明](./src/views/README.md)
- [Mango Job 后端模块](../../../mango/mango-platform/mango-job/README.md)
- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
