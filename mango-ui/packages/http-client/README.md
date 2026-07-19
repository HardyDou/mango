# @mango/http-client

`@mango/http-client` 是 Mango FE1 HTTP transport adapter。它使用 Axios 1 实现 `@mango/api-schema` 的厂商无关 `HttpClient`，业务 API 和页面不直接依赖 Axios。

## 1. 概览

这个包属于 FE1 基础能力。它负责 transport dispatch、上下文 header、刷新 single-flight、幂等重试、取消和规范化错误，不负责页面状态与 UI 交互。

## 2. 功能清单

| 能力                    | 说明                                                |
| ----------------------- | --------------------------------------------------- |
| `createMangoHttpClient` | 为每个 host/runtime context 创建独立实例            |
| context provider        | 注入 token、tenant、trace 和 base URL               |
| refresh                 | 同一实例内 single-flight，接受标准 `AbortSignal`    |
| retry                   | 仅安全方法或显式 idempotency key                    |
| lifecycle               | active、inactive、destroyed，停用与销毁取消异步工作 |
| normalization           | 只返回业务数据或 Mango `HttpError` / `HttpProgress` |

## 3. 接入方式

host/runtime 为每个运行实例创建独立客户端：

```ts
import { createMangoHttpClient } from '@mango/http-client';

const client = createMangoHttpClient({
  baseUrl: '/api',
  getAccessToken: () => session.accessToken,
  getTenantId: () => session.tenantId,
  getTraceId: () => trace.currentId,
  refreshAccessToken: async () => auth.refresh(),
  onUnauthorized: (error) => auth.handleUnauthorized(error),
});
```

业务 package 只接收 `HttpClient`：

```ts
import type { HttpClient } from '@mango/api-schema';

export function createOrderApi(client: HttpClient) {
  return {
    getOrder(id: string, signal?: AbortSignal) {
      return client.request<Order>({ method: 'GET', url: `/orders/${id}`, signal });
    },
  };
}
```

## 4. 配置说明

| 配置                                          | 必填 | 说明                                                       |
| --------------------------------------------- | ---- | ---------------------------------------------------------- |
| `baseUrl`                                     | 是   | host 拥有的 API base URL                                   |
| `timeoutMs`                                   | 否   | 默认 30 秒                                                 |
| `getAccessToken`、`getTenantId`、`getTraceId` | 否   | 实例级上下文 provider                                      |
| `refreshAccessToken`                          | 否   | 接收生命周期 `AbortSignal`，不得向子应用公开 refresh token |
| `onUnauthorized`                              | 否   | transport 事件回调，不做 UI 或路由处理                     |
| `maxRetries/retryDelayMs`                     | 否   | 默认不自动重试                                             |

本包不读取环境变量、Session、router、DOM 或微前端全局对象。

## 5. API 与扩展

- `createMangoHttpClient(options): MangoHttpClient`
- `MangoHttpClient.request()` 实现 `@mango/api-schema` 的公共契约。
- `activate()` 只允许从 inactive 恢复；destroyed 不可恢复。
- `deactivate()` 和 `destroy()` 幂等；destroy 还会 eject 内部 interceptor。
- `MangoHttpError` 是规范化错误实现，公开字段不包含 Axios config、request、response 或 error。

## 6. 运行合同

- token、tenant、trace 和 base URL 由 host/provider 注入，业务请求只使用相对 endpoint。
- 每个实例拥有独立 Axios instance、刷新 single-flight、pending request 和生命周期状态。
- 仅 GET/HEAD/OPTIONS 或携带 `idempotencyKey` 的请求可以自动重试。
- `deactivate()` 取消 pending request；`activate()` 恢复接收请求；`destroy()` 取消请求并释放 interceptor，且不可恢复。
- 对外只返回业务数据或 `HttpError`，错误、进度和声明文件不携带 Axios 对象或类型。
- transport 不弹 UI 消息、不跳路由、不读 Session、环境变量或微前端全局对象。

## 7. 数据与初始化

这个包不包含数据库、菜单、权限或初始化脚本，也不保存 token、tenant 或 refresh token。

## 8. 管理入口

这个包没有页面、路由或管理入口。

## 9. 快速开始

1. host/runtime 创建实例并注入 context provider。
2. 业务 package 通过 `createXxxApi(client)` 接收 `HttpClient`。
3. Vue composable 负责 loading、错误文案和组件卸载时的取消。
4. runtime deactivate/destroy 时调用客户端同名生命周期方法。

## 10. 问题排查

**请求被判定为 configuration error**

确认 endpoint 是相对地址，且客户端未 inactive/destroyed。

**POST 没有自动重试**

这是默认安全行为。只有 GET/HEAD/OPTIONS 或显式提供 `idempotencyKey` 的请求可重试。

**刷新后仍然 401**

确认 `refreshAccessToken` 返回新 access token，或其完成前已更新 `getAccessToken` 的来源；不要在业务 package 中保存 refresh token。

## 11. 发布与兼容

当前 `@mango/common/utils/request` 是迁移期旧入口。新业务不得新增对旧单例的依赖；领域 package 将按批次改为 `createXxxApi(client)`。在兼容 facade、真实单体/微前端联调、Nexus candidate、灰度和回退演练完成前，本包不能单独代表前端已生产毕业。

## 12. 相关文档

- [前端规范落地设计](../../../mango-docs/designs/2026-07-18-frontend-standards-enforcement-design.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
