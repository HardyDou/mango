# @mango/job

## 1. 能力定位

`@mango/job` 提供 Mango Job 管理端前端页面、页面注册入口和任务 API 请求封装。主要使用者是 Mango 管理端 Shell 和需要展示任务管理能力的业务后台应用。

## 2. 适用场景

- 在管理端注册任务定义、执行实例、Worker、运行状态和告警规则页面。
- 调用后端 `/job` API 管理任务定义、实例、日志、Worker、handler、告警规则和引擎状态。
- 配合 `mango-platform/mango-job` 后端能力形成完整任务管理入口。

## 3. 不适用场景

- 不提供任务调度后端运行时。
- 不保存任务数据。
- 不替代业务任务 handler 实现。
- 不负责菜单和权限数据初始化。

## 4. 模块边界

本包只提供前端视图、API 封装和 admin page 注册。后端任务调度、数据库、权限菜单和告警逻辑由 `mango-job` 后端及 authorization/notice 能力承担。

## 5. 接入方式

依赖包：

```json
{
  "dependencies": {
    "@mango/job": "1.0.1"
  }
}
```

注册管理端页面：

```ts
import { registerMangoJobAdminPages } from '@mango/job/admin-pages';
```

样式入口：

```ts
import '@mango/job/style.css';
```

## 6. 配置项

本包没有独立运行时配置文件。构建由 `vite build` 完成，宿主应用负责提供请求基地址、认证态、Shell 和页面注册上下文。

## 7. 对外接口 / 扩展点

导出视图：

- `JobAlarmView`
- `JobDefinitionView`
- `JobEngineView`
- `JobInstanceView`
- `JobWorkerView`

页面注册：

- `registerMangoJobAdminPages()` 注册模块 `mango-job`。
- 页面 key：`job/alarm/index`、`job/definition/index`、`job/instance/index`、`job/worker/index`、`job/engine/index`。

API：

- `jobApi` 覆盖 definitions、instances、logs、workers、handlers、alarm-rules、engines status。

## 8. 数据库 / 初始化数据

本包不包含数据库 migration。任务表、菜单和权限数据由后端 `mango-job` 与 `mango-authorization` 维护。

## 9. 菜单 / 权限 / 租户

前端页面 key 需要与 authorization 中的菜单资源匹配。页面请求后端 `/job/**` 接口，权限和租户边界由后端接口与当前登录态控制。

## 10. 验证方式

```bash
pnpm -F @mango/job build
```

业务链路验收：

- 管理端菜单能打开任务定义页面。
- 任务定义、执行实例、Worker、运行状态、告警规则页面能请求对应 `/job` API。
- 触发任务后执行实例页面可查看运行结果。
- 打开 `平台能力 -> 任务管理 -> Worker 节点`，确认至少一个在线 Worker。
- 在 `任务定义` 新增每 1 分钟执行一次的 `CRON` 任务，表达式填写 `0 */1 * * * *`。
- 启用任务并等待两个调度周期。
- 在 `执行实例` 按任务名称筛选，确认实例持续产生。
- 打开实例日志，确认能看到 `System.out`、日志框架输出和处理器返回结果。
- 配置失败告警，触发失败任务，确认系统消息由 `mango-notice` 生成。

## 11. 业务接入最小闭环

宿主后台接入时引入 `@mango/job/style.css`，在启动入口调用 `registerMangoJobAdminPages()`。后端接入 `mango-job-starter` 后，authorization 菜单资源的 component key 使用 job 包注册的页面 key。

验收断言覆盖：任务定义菜单能打开，页面请求 `/job/**` 不返回 404/401/403，创建或触发任务后执行实例和日志页面能看到结果，缺少权限的用户不能访问治理操作。

## 12. 常见问题

- 页面空白先检查是否调用 `registerMangoJobAdminPages()`。
- 接口 404 时检查后端 `mango-job-starter` 是否启用，以及代理路径是否包含 `/job`。
- 菜单显示但无权限时检查 authorization 菜单和权限码。

## 13. 关联 PMO 规则

- [前端模块规范](../../../mango-pmo/rules/frontend/01-vue-code.md)
- [前端测试规范](../../../mango-pmo/rules/frontend/04-test.md)
- [模块菜单规范](../../../mango-pmo/rules/backend/11-module-menu.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 14. 历史设计 / 交付记录

- [Mango Job 后端 README](../../../mango/mango-platform/mango-job/README.md)
- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
