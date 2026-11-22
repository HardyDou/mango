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
| data grant              | 按 domain 缓存列表签发的 Grant，自动随详情请求回传 |

## 3. 接入方式

host/runtime 为每个运行实例创建独立客户端：

```ts
import { createMangoHttpClient } from '@mango/http-client';

const client = createMangoHttpClient({
  baseUrl: '/api',
  getAccessToken: () => session.accessToken,
  getTenantId: () => session.tenantId,
  getUserId: () => session.userId,
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
| `getUserId`                                   | 否   | 数据权限 Grant 的身份绑定；不提供时切换账号须手动清空 Grant |
| `refreshAccessToken`                          | 否   | 接收生命周期 `AbortSignal`，不得向子应用公开 refresh token |
| `onUnauthorized`                              | 否   | transport 事件回调，不做 UI 或路由处理                     |
| `maxRetries/retryDelayMs`                     | 否   | 默认不自动重试                                             |

本包不读取环境变量、Session、router、DOM 或微前端全局对象。数据权限 Grant 缓存是唯一例外：它只由响应头写入、由本包自己的 store 保存，host 不参与。

## 5. API 与扩展

- `createMangoHttpClient(options): MangoHttpClient`
- `MangoHttpClient.request()` 实现 `@mango/api-schema` 的公共契约。
- `activate()` 只允许从 inactive 恢复；destroyed 不可恢复。
- `deactivate()` 和 `destroy()` 幂等；destroy 还会 eject 内部 interceptor。
- `MangoHttpError` 是规范化错误实现，公开字段不包含 Axios config、request、response 或 error。
- `clearDataGrants()`、`recordDataGrant()`、`collectDataGrants()`、`applyDataGrantResponse()`、`deleteDataGrant()` 与 `DATA_GRANT_*` 常量用于数据权限 Grant 的显式维护。

## 6. 运行合同

- token、tenant、trace 和 base URL 由 host/provider 注入，业务请求只使用相对 endpoint。
- 每个实例拥有独立 Axios instance、刷新 single-flight、pending request 和生命周期状态。
- 仅 GET/HEAD/OPTIONS 或携带 `idempotencyKey` 的请求可以自动重试。
- `deactivate()` 取消 pending request；`activate()` 恢复接收请求；`destroy()` 取消请求并释放 interceptor，且不可恢复。
- 对外只返回业务数据或 `HttpError`，错误、进度和声明文件不携带 Axios 对象或类型。
- transport 不弹 UI 消息、不跳路由、不读 Session、环境变量或微前端全局对象。

### 6.1 数据权限 Grant 接力

后端在列表响应里用四个响应头描述数据权限凭证，本包据此自动完成「列表 → 详情」的接力：

| 响应头                          | 含义                                                   |
| ------------------------------- | ------------------------------------------------------ |
| `X-Mango-Data-Grant`            | 列表签发的不透明凭证；`absent` 表示本列表没有可用凭证 |
| `X-Mango-Data-Grant-Domain`     | 凭证所属数据域，前端只把它当缓存键，不做 URL 前缀映射 |
| `X-Mango-Data-Grant-Required`   | `1` 表示该请求需要凭证，前端据此在 dev 打运行时断言    |
| `X-Mango-Data-Grant-Skipped`    | `oversize`、`scope-unresolved` 等未签发原因           |

约定：

- 请求头 `X-Mango-Data-Grant` 用逗号分隔携带所有缓存凭证（上限 16 个、序列化后 ≤4096 字符），超限按最近使用优先截断；
- 缓存项包含 `grant`、`seq`、`userId`、`tenantId`；只接受更大的 `seq`，发送前比对身份，不一致直接丢弃并清键；
- 缺少 `X-Mango-Data-Grant-Domain`（旧后端）时按未知域处理：不存、不发，dev 只提示一次；
- `absent`、`oversize`、`scope-unresolved` 以及任何未知的 Skipped 取值都删除该域的键，不做 stale 标记；
- `responseType: 'stream'` 的下载请求不带 Grant；
- `Required: 1` 且没有 Grant 时前端不拦截、不弹 toast，403/404 文案由后端决定；
- 登出、切换租户或切换用户必须调用 `clearDataGrants()`；`@mango/common` 的 `Session.clearToken()` 已经内置这一步。

## 7. 数据与初始化

这个包不包含数据库、菜单、权限或初始化脚本，也不保存 token、tenant 或 refresh token。

唯一的进程内状态是数据权限 Grant 缓存：内存 `Map`，按 domain 键控，不落 Session、localStorage 或 DTO；清空动作（登出、切租户、切用户）由 host 或 `Session.clearToken()` 驱动。

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

**列表能打开但详情 403/404**

确认 `X-Mango-Data-Grant-Domain` 由网关透传，且请求头 `X-Mango-Data-Grant` 在 CORS `Access-Control-Allow-Headers` 与 `Access-Control-Expose-Headers` 中均被放行；浏览器 preflight 缓存会延迟策略生效。

## 10.1 已知限制

- 网关 / WAF 可能过滤 `X-` 前缀响应头或请求头，需要在每个环境确认白名单；被过滤时前端拿不到 domain，接力失效但不会误放行。
- 浏览器 preflight 与 `Access-Control-Expose-Headers` 有缓存窗口，后端已改而部分用户仍 401/403 是预期现象，需要用户刷新或等缓存过期。
- 过渡期内无法识别的旧 Grant 一律按删除处理，不做兼容重放；需要用户重新打开列表。
- 平台侧不得记录 Grant 完整值，日志只能打长度或前 6 位。

## 11. 发布与兼容

当前 `@mango/common/utils/request` 是迁移期旧入口。新业务不得新增对旧单例的依赖；领域 package 将按批次改为 `createXxxApi(client)`。在兼容 facade、真实单体/微前端联调、Nexus candidate、灰度和回退演练完成前，本包不能单独代表前端已生产毕业。

本批次为 `responseType: 'stream'` 使用浏览器 Fetch adapter，并把 pending request 的清理延迟到 `ReadableStream` 完成、报错或取消之后。调用方必须消费或取消流，并继续通过 `AbortSignal` 管理页面生命周期；普通 JSON、Blob 和 ArrayBuffer 请求合同不变。

## 12. 相关文档

- [前端规范落地设计](../../../mango-docs/designs/2026-07-18-frontend-standards-enforcement-design.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
