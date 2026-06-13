# @mango/job

`@mango/job` 是 Mango 任务管理后台页面包，对应后端 `mango-platform/mango-job`。

## 页面入口

管理后台菜单：

```text
平台能力 -> 任务管理
├── 任务定义
├── 执行实例
├── Worker 节点
├── 运行状态
└── 告警规则
```

页面注册入口：

```ts
import { registerMangoJobAdminPages } from '@mango/job/admin-pages';
import '@mango/job/style.css';

registerMangoJobAdminPages();
```

注册的页面 key：

| 页面 | key |
|---|---|
| 任务定义 | `job/definition/index` |
| 执行实例 | `job/instance/index` |
| Worker 节点 | `job/worker/index` |
| 运行状态 | `job/engine/index` |
| 告警规则 | `job/alarm/index` |

## 使用说明

任务定义页用于维护任务归属、调度频率、处理器和参数：

- `appCode` 表示任务所属逻辑应用。
- `ownerService` 表示任务归属的业务服务，默认等于 `appCode`。
- `workerGroup` 表示 Worker 分组，默认等于 `ownerService`。
- `handlerName` 必须和 Java `MangoJobHandler.handlerName()` 一致。
- `paramSchema` 可生成结构化参数表单。
- `paramValue` 是默认参数 JSON，手动触发时可覆盖。

执行实例页用于查看每一次运行结果。运行日志从实例行内打开，不再单独维护“执行日志”菜单。

Worker 节点页用于查看自动注册 Worker 和手动登记远程 Worker：

- `IN_MEMORY` 内嵌 Worker 由系统自动注册，不能手动新增。
- `HTTP_INTERNAL` 远程 Worker 可以自动心跳注册，也可以手动登记。
- Worker 能力必须和任务定义的 `ownerService`、`workerGroup`、`handlerName`、`jobCode` 匹配。

告警规则页用于维护失败实例告警：

- 告警类型：`INSTANCE_FAILED`
- 通知场景编码：`job.instance.failed`
- 通知模板编码：`job.instance.failed.site`

通知通道和模板归属 `mango-notice`，本页面只维护 Job 告警路由字段。

## 构建

```bash
cd mango-ui/packages/job
pnpm build
```

## 验收建议

1. 打开 `平台能力 -> 任务管理 -> Worker 节点`，确认至少一个在线 Worker。
2. 在 `任务定义` 新增每 1 分钟执行一次的 `CRON` 任务，表达式填写 `0 */1 * * * *`。
3. 启用任务并等待两个调度周期。
4. 在 `执行实例` 按任务名称筛选，确认实例持续产生。
5. 打开实例日志，确认能看到 `System.out`、日志框架输出和处理器返回结果。
6. 配置失败告警，触发失败任务，确认系统消息由 `mango-notice` 生成。
