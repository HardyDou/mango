# mango-infra-realtime 设计说明

- 日期：2026-04-18
- 状态：当前实现基准
- 规范源：`mango-pmo/rules/backend/03-api.md`、`mango-pmo/rules/backend/05-module.md`

## 1. 定位

`mango-infra-realtime` 是 server-client 实时通信基础设施，只负责连接、订阅、在线投递、协议适配、受控的远程发布适配、跨节点在线投递路由，以及 WebSocket/SSE/Polling 客户端入站业务消息的轻量分发。

不负责：

- 系统消息、通知、已读未读、消息中心列表、审计级持久化。
- MQ、事件总线、服务间异步事件投递。
- Agent 聚合、钉钉、短信、邮件等业务通知渠道。
- 业务消息表或离线消息存储。

业务通知域由 `mango-biz-notification` 承担；未来 MQ/event 能力保留给 `mango-infra-messaging`。

## 2. 模块结构

| 子模块 | 职责 |
|--------|------|
| `mango-infra-realtime-api` | 暴露 `RealtimeApi`、`RealtimeOutboundMessage`、`RealtimeInboundMessage`、`RealtimeInboundMessageListener`、`RealtimeInboundApi`、`RealtimeInboundReceiverApi`、`RealtimeOutboundApi` 等跨模块契约 |
| `mango-infra-realtime-support` | 仅供 `core` / `starter-remote` 复用的内部运行时支撑，例如入站监听器扫描 |
| `mango-infra-realtime-core` | 内存订阅、presence 路由抽象、临时 polling 队列、SSE/WebSocket/Polling adapter、入站监听分发、入站接收服务注册表 |
| `mango-infra-realtime-starter` | 本地装配、客户端协议 endpoint、正向发布 endpoint、入站接收服务注册 endpoint、跨节点出站反向 endpoint |
| `mango-infra-realtime-starter-remote` | Feign adapter，提供远程 `RealtimeApi`，自动注册本服务入站监听器并暴露反向入站接收 endpoint |

`RealtimeOutboundMessage` / `RealtimeInboundMessage` 作为 infra envelope 放在 `api.dto` 包下。`api` 模块只放其它模块直接或间接依赖的契约；模块内部 service、registry、dispatcher 不进入 `api`。

## 3. 当前 API

核心契约：

```java
public interface RealtimeApi {
    void publish(RealtimeOutboundMessage message);
    default void publishToUser(Long userId, String type, String content) { ... }
    default void publishToTenant(String tenantId, String type, String content) { ... }
    default void broadcast(String type, String content) { ... }
}
```

当前 `RealtimeOutboundMessage` 只包含投递所需 envelope 字段：`id`、`type`、`content`、`tenantId`、`userId`、`headers`、`createdAt`。不在 realtime 内定义业务消息状态、模板、渠道或持久化记录。
发布契约统一为 `RealtimeApi`。

客户端入站契约：

```java
@Target(ElementType.METHOD)
public @interface RealtimeInboundMessageListener {
    String[] types();
    int order() default 0;
}

public interface RealtimeInboundReceiverApi {
    void register(RealtimeInboundReceiverRegistration registration);
    void unregister(RealtimeInboundReceiverRegistration registration);
}
```

`RealtimeInboundMessageListener` 只用于方法。监听方法必须只接收一个 `RealtimeInboundMessage` 参数。入站监听扫描和调用位于 `support`，本地 starter 与 remote starter 复用该逻辑。

## 4. Endpoint

| Endpoint | 所属模块 | 状态 | 用途 |
|----------|----------|------|------|
| `GET /realtime/transports/sse` | `starter` | 已实现 | SSE 客户端订阅 |
| `/realtime/transports/websocket` | `starter` | 已实现 | WebSocket 客户端连接 |
| `GET /realtime/transports/polling` | `starter` | 已实现 | HTTP short polling / long polling |
| `GET /realtime/transports/negotiate` | `starter` | 已实现 | 客户端 transport 能力协商 |
| `POST /realtime/messages/inbound/sse` | `starter` | 已实现 | SSE 客户端入站消息 |
| `POST /realtime/messages/inbound/polling` | `starter` | 已实现 | Polling 客户端入站消息 |
| `POST /realtime/messages/publish` | `starter` | 已实现 | `starter-remote` 远程发布 |
| `POST /realtime/receivers/register` | `starter` | 已实现 | `starter-remote` 自动注册入站监听服务 |
| `POST /realtime/receivers/unregister` | `starter` | 已实现 | 服务关闭时注销入站监听服务 |
| `POST /_realtime/messages/inbound` | `starter-remote` | 已实现 | 接收 realtime 服务转发的客户端入站消息 |
| `POST /_realtime/messages/outbound` | `starter` | 已实现 | 接收其它 realtime 节点转发的 server-to-client 消息 |

`RealtimePollingService` 当前是本地内存临时队列，HTTP Polling endpoint 只承担在线长轮询/短轮询，不承担离线消息、消息中心或审计级持久化。

## 5. 部署形态

承载前端连接的服务依赖：

```xml
<dependency>
    <groupId>io.mango</groupId>
    <artifactId>mango-infra-realtime-starter</artifactId>
</dependency>
```

只发送实时消息、不承载前端连接的服务依赖：

```xml
<dependency>
    <groupId>io.mango</groupId>
    <artifactId>mango-infra-realtime-starter-remote</artifactId>
</dependency>
```

remote starter 通过 `@FeignClient(name = "mango-infra-realtime", path = "/realtime/messages")` 声明目标 realtime 服务。当前入站方案先不依赖 `mango-infra-module`：remote starter 启动时扫描本服务的 `@RealtimeInboundMessageListener`，如果存在监听器，则向 realtime 服务的 `POST /realtime/receivers/register` 上报 `serviceName`、`contextPath` 和固定反向接收 path。
`mango-infra-realtime-starter-remote` 只能依赖 `mango-infra-realtime-api` 和 Feign 装配，不允许依赖 `mango-infra-realtime-core`；远程发布的 `RealtimeApi` 直接由 Feign client 实现，不再额外包装本地 publisher。

入站部署形态：

```text
frontend
  -> d-service /realtime/transports/websocket
  -> mango-infra-realtime-starter 收到 WebSocket/SSE/Polling 入站消息
  -> d-service 内存注册表查找已注册订阅服务
  -> POST http://{serviceName}{contextPath}/_realtime/messages/inbound
  -> c-service mango-infra-realtime-starter-remote
  -> 本地 @RealtimeInboundMessageListener
```

本地 starter 入站模式：

| mode | 行为 |
|------|------|
| `none` | 不处理客户端业务入站消息 |
| `local` | 直接分发给本服务内的监听器 |
| `remote` | 转发给已注册的 remote starter 服务 |
| `local_remote` | 先本地分发，再转发给已注册的 remote starter 服务 |

该注册表是内存实现，只解决当前连接承载服务向业务服务转发入站消息的问题；不是服务目录、事件总线或可靠订阅系统。服务重启后由 remote starter 重新注册。

出站多实例投递形态：

```text
node-a RealtimeApi.publishToUser
  -> node-a 本地连接分发
  -> presence 路由表查找用户所在远端节点
  -> POST http://{serviceName}{contextPath}/_realtime/messages/outbound
  -> node-b publishLocal
  -> node-b 本地 WebSocket/SSE/Polling 分发
```

presence 只保存 session 到节点的路由信息，不保存 WebSocket session、SSE emitter 或其它本地连接对象。默认 `InMemoryRealtimePresenceService` 适合单实例和测试；如果运行时存在 `IKvStore` 且实现了 `IKvSortedSet`，starter 可自动切到 KV presence。生产多实例推荐 Redis KV：`IKvStore` 存 session 详情，`IKvSortedSet` 维护 user/tenant/all 到 sessionId 的 TTL 索引，并补充 TTL、心跳续约、节点下线清理和服务发现解析。

## 6. 安全和接入

- `/realtime/messages/publish` 和 `/realtime/receivers/*` 是正向服务调用入口，应配合 `mango-infra-web` 内部路径保护和 `mango-infra-feign-starter` 内部调用签名使用。
- `/_realtime/messages/inbound` 与 `/_realtime/messages/outbound` 是反向服务调用入口，只允许服务间访问。
- 认证、鉴权、租户解析、可信 header 注入、外部伪造 header 清洗、限流、CORS/Origin、WebSocket Upgrade 和连接超时策略属于 `mango-infra-web/security/context` 与 `mango-gateway`，不属于 realtime。
- `mango-infra-realtime` 当前只读取协议入口必要 header/query 并做轻量校验；Phase 4/5 冻结统一上下文与 gateway 接入规则后，应通过 resolver/provider 被动适配，不自行解析 token 或实现权限规则。
- `tenantId` 应优先通过统一上下文、可信 header 或网关注入传递；query 参数只作为 WebSocket 场景的临时兼容入口。
- WebSocket query token 仅为兼容入口，不作为生产推荐。生产接入应使用 cookie、短期连接票据或网关统一认证方案。
- SSE 原生 `EventSource` 不能设置自定义 header，生产接入需通过 cookie、支持 header 的客户端或网关统一认证。
- 前端必须处理断线重连、重复消息和降级。

## 7. 与 mango-biz-notification 的关系

| 模块 | 负责 | 不负责 |
|------|------|--------|
| `mango-infra-realtime` | 在线连接、协议投递、临时 polling、远程发布适配 | 通知业务、消息中心、离线消息、已读未读 |
| `mango-biz-notification` | 通知记录、业务状态、消息中心 API、是否触发实时投递 | SSE/WebSocket 会话、协议实现 |

推荐链路：

```text
a-module / b-module
  -> mango-biz-notification 记录通知和状态
  -> RealtimeApi 投递在线用户
  -> mango-infra-realtime-starter 承载 SSE/WebSocket
  -> 前端接收并刷新 UI
```

只需要在线临时提示且不需要入库时，可以直接调用 `RealtimeApi`。

## 8. 当前不做

- 不新增 `RealtimeStore`。跨实例 presence 或短暂队列以后只能通过 KV/注册中心抽象引入，并且不得承担业务消息持久化。
- 不把真实 `RealtimeSession`、WebSocket session、SSE emitter 等本地连接对象放入共享存储。
- 不在 infra realtime 内实现 Agent、钉钉、短信、邮件等业务渠道。
- 不把 Polling 当成业务消息中心、离线队列或可靠消息通道。
- 不把同一个业务模块部署到同一系统群下的多个逻辑服务。
