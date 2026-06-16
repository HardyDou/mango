# Mango Infra Realtime

## 1. 概览
`mango-infra-realtime` 提供 Mango 服务端实时消息基础设施，覆盖 WebSocket、SSE、HTTP Polling、协议协商、在线 presence、订阅、服务端发布、客户端上行、跨实例转发和基于 KV Outbox 的可靠实时投递。

## 2. 功能清单

| 能力 | 常用入口 |
|------|----------|
| Web 管理端或业务客户端需要实时接收通知、进度、状态变更 | Maven 依赖 / starter / Java API |
| 客户端环境不稳定，需要 WebSocket / SSE / Polling 协商和降级 | Maven 依赖 / starter / Java API |
| 服务端需要按 USER、CLIENT、CONNECTION、GROUP、TENANT、BROADCAST 投递 | Maven 依赖 / starter / Java API |
| 多实例部署下，需要把消息转发到用户所在节点 | Maven 依赖 / starter / Java API |
| 客户端需要向服务端发送业务消息，并由本地或远程 receiver 处理 | Maven 依赖 / starter / Java API |


## 3. 能力边界
- 不理解业务消息语义，不负责业务权限判断。
- 不替代离线消息中心、IM 历史消息库或完整消息队列。
- 不替代业务订阅关系模型；GROUP 只表示实时投递分组。
- 不自动保证消息幂等，业务 listener 和 receiver 必须自己处理重复消息。

## 4. 模块入口
- `mango-infra-realtime-api`：实时消息 DTO、发布 API、上行 API、receiver API、listener 注解。
- `mango-infra-realtime-core`：session、presence、subscription、publish、protocol adapter、inbound forward。
- `mango-infra-realtime-support`：上行 listener 扫描和调用。
- `mango-infra-realtime-starter`：本地实时服务、HTTP/SSE/WebSocket/Polling endpoint、outbox dispatcher。
- `mango-infra-realtime-starter-remote`：业务服务远程接入 realtime 服务，提供 Feign client 和 receiver 自动注册。

## 5. 接入方式
实时服务端：

```xml
<dependency>
    <groupId>io.mango.infra.realtime</groupId>
    <artifactId>mango-infra-realtime-starter</artifactId>
</dependency>
```

业务服务只远程发布或接收上行消息：

```xml
<dependency>
    <groupId>io.mango.infra.realtime</groupId>
    <artifactId>mango-infra-realtime-starter-remote</artifactId>
</dependency>
```

只使用契约：

```xml
<dependency>
    <groupId>io.mango.infra.realtime</groupId>
    <artifactId>mango-infra-realtime-api</artifactId>
</dependency>
```

服务端发布消息：

```java
realtimeApi.publish(RealtimeOutboundMessage.builder()
        .target(RealtimeTarget.user("tenant-a", 1001L))
        .payload(RealtimePayload.json("payment.status", payload))
        .build());
```

处理客户端上行消息：

```java
@RealtimeInboundMessageListener("chat.message")
public void onMessage(RealtimeInboundMessage message) {
    // validate tenant/user permission before business action
}
```

## 6. 配置说明
配置前缀：`mango.infra.realtime`。

### 基础与协议

| 配置 | 默认值 | 含义 |
|------|--------|------|
| `enabled` | `true` | 是否启用实时基础设施。 |
| `mode` | `AUTO` | 协议模式：`AUTO`、`SSE`、`WEBSOCKET`、`POLLING`。 |
| `sse.enabled` | `true` | AUTO 模式下是否启用 SSE。 |
| `sse.endpoint` | `/realtime/transports/sse` | SSE 建连 endpoint。 |
| `sse.timeout-millis` | `300000` | SSE 连接超时毫秒。 |
| `sse.inbound-endpoint` | `/realtime/messages/inbound/sse` | SSE 客户端上行 HTTP endpoint。 |
| `websocket.enabled` | `true` | AUTO 模式下是否启用 WebSocket。 |
| `websocket.endpoint` | `/realtime/transports/websocket` | WebSocket endpoint。 |
| `websocket.allowed-origins` | `*` | WebSocket allowed origins。 |
| `polling.enabled` | `true` | AUTO 模式下是否启用 HTTP Polling。 |
| `polling.endpoint` | `/realtime/transports/polling` | Polling 拉取 endpoint。 |
| `polling.default-max-size` | `20` | 客户端未传 max size 时单次返回条数。 |
| `polling.max-size` | `100` | 单次 polling 最大返回条数。 |
| `polling.default-timeout-millis` | `0` | 默认 hold timeout；0 表示短轮询。 |
| `polling.max-timeout-millis` | `25000` | 长轮询最大 hold timeout。 |
| `polling.inbound-endpoint` | `/realtime/messages/inbound/polling` | Polling 客户端上行 HTTP endpoint。 |
| `negotiate.enabled` | `true` | 是否启用协议协商 endpoint。 |
| `negotiate.endpoint` | `/realtime/transports/negotiate` | 协议协商 endpoint。 |

### 节点、转发与 Presence

| 配置 | 默认值 | 含义 |
|------|--------|------|
| `remote.endpoint-enabled` | `true` | 是否暴露 `/realtime/messages/publish` 远程发布 endpoint。 |
| `node.instance-id` | 自动生成或环境推导 | 当前 realtime 节点实例 ID。 |
| `node.service-name` | `spring.application.name` | 当前节点可路由服务名。 |
| `node.context-path` | `server.servlet.context-path` 或 `/` | 当前节点 context path。 |
| `outbound.endpoint-enabled` | `true` | 是否暴露跨节点下行转发 endpoint。 |
| `outbound.endpoint` | `/_realtime/messages/outbound` | 接收其他节点转发的下行消息。 |
| `presence.prefix` | `mango:infra:realtime:presence` | presence KV key 前缀。 |
| `presence.ttl-seconds` | `120` | 在线 presence TTL。 |

presence 需要 `IKvStore` 同时实现 `IKvSortedSet`，通常使用 Redis KV store。缺失时 realtime presence 会启动失败。

### Outbox

| 配置 | 默认值 | 含义 |
|------|--------|------|
| `outbox.enabled` | `true` | 是否通过 infra-kv outbox 做可靠实时发布。 |
| `outbox.worker-id` | service name + instance id | Outbox claim worker id。 |
| `outbox.batch-size` | `50` | 每次 claim 消息数。 |
| `outbox.initial-delay-millis` | `1000` | dispatcher 初始延迟。 |
| `outbox.fixed-delay-millis` | `500` | dispatcher 固定执行间隔。 |
| `outbox.max-attempts` | `5` | 最大投递尝试次数。 |
| `outbox.retry-backoff-millis` | `1000` | 失败重试基础 backoff。 |

### 上行消息

| 配置 | 默认值 | 含义 |
|------|--------|------|
| `inbound.enabled` | `false` | 是否启用客户端上行业务消息分发。 |
| `inbound.mode` | `LOCAL_REMOTE` | 上行分发模式：`NONE`、`LOCAL`、`REMOTE`、`LOCAL_REMOTE`。 |
| `inbound.max-payload-bytes` | `65536` | WebSocket 文本上行最大字节数。 |
| `inbound.fail-fast` | `false` | 一个 listener 失败后是否停止后续 listener。 |
| `inbound.unknown-type-policy` | `ignore` | 未知上行类型策略：`ignore`、`warn`、`error`。 |
| `inbound.remote.endpoint-enabled` | `true` | 是否暴露远程上行接收 endpoint。 |
| `inbound.remote.endpoint` | `/_realtime/messages/inbound` | 远程上行接收 endpoint。 |

remote starter 还使用：

| 配置 | 默认值 | 含义 |
|------|--------|------|
| `inbound.remote.register-enabled` | `true` | 业务服务启动时是否向 realtime 服务注册 receiver。 |
| `inbound.remote.service-name` | 无 | 业务 receiver 所在服务名。 |
| `inbound.remote.context-path` | `/` | 业务 receiver context path。 |

最小生产示例：

```yaml
mango:
  kv:
    store:
      type: redis
    capability:
      enabled: true
      outbox: true
  infra:
    realtime:
      enabled: true
      mode: AUTO
      websocket:
        allowed-origins:
          - https://admin.example.com
      presence:
        ttl-seconds: 120
      inbound:
        enabled: true
        mode: LOCAL_REMOTE
```

## 7. API 与扩展
- API：`RealtimeApi`、`RealtimeOutboundApi`、`RealtimeInboundApi`、`RealtimeInboundReceiverApi`。
- 注解：`@RealtimeInboundMessageListener`。
- 发布服务：`IRealtimePublishService`、`IRealtimeReliablePublishService`。
- Presence：`IRealtimePresenceService`。
- Receiver：`IRealtimeInboundReceiverService`。
- Feign：`RealtimeFeignClient`、`RealtimeInboundReceiverFeignClient`，服务名为 `mango-infra-realtime`。

主要 endpoint：

| 路径 | 用途 |
|------|------|
| `/realtime/messages/publish` | 远程发布实时消息。 |
| `/realtime/receivers/register` | 注册上行 receiver。 |
| `/realtime/receivers/unregister` | 注销上行 receiver。 |
| `/realtime/transports/websocket` | WebSocket 连接。 |
| `/realtime/transports/sse` | SSE 连接。 |
| `/realtime/transports/polling` | HTTP Polling 拉取。 |
| `/realtime/transports/negotiate` | 协议协商。 |
| `/realtime/messages/inbound/sse` | SSE 客户端上行。 |
| `/realtime/messages/inbound/polling` | Polling 客户端上行。 |
| `/_realtime/messages/outbound` | 节点间下行转发。 |
| `/_realtime/messages/inbound` | 远程业务服务接收上行消息。 |

## 8. 数据与初始化
本模块没有独立 SQL migration、Runner 或 Initializer。presence 和可靠投递依赖 `mango-infra-kv`：

- presence 需要支持 sorted set 的 KV store。
- realtime outbox 需要 `mango.kv.capability.outbox=true` 注册 `IOutboxStore` 和 `IOutboxPublisher`。
- JDBC KV store 场景需要执行 KV 模块的 `infra_kv_entry` migration。

## 9. 管理入口
本模块不创建管理菜单和权限。实时消息 DTO 支持 tenant 上下文和 TENANT target，但这只是投递维度；业务发布方和上行 listener 必须校验当前用户是否有权向目标用户、群组或租户发送消息。

## 10. 快速开始
1. realtime 服务接入 starter，并配置 Redis KV store。
2. 前端先调用 negotiate，再按返回能力选择 WebSocket、SSE 或 Polling。
3. 服务端发布消息时明确 target type、tenant id、user id 或 group id。
4. 需要上行消息时，业务服务接入 remote starter，声明 `@RealtimeInboundMessageListener`。
5. 所有 listener 入口先校验租户、用户和业务权限，再处理业务动作。

## 11. 问题排查
- 启动失败提示 presence requires sorted set：改用 Redis KV store，Memory/JDBC 不一定满足 sorted set 需求。
- 客户端连不上：先调 negotiate，再检查 CORS、allowed origins、网关是否支持 WebSocket/SSE。
- 消息只在本节点可见：检查 presence KV、node service name、outbound endpoint 和跨节点网络。
- 上行消息没处理：检查 `inbound.enabled`、`inbound.mode`、listener 注解类型和 receiver 注册。
- 可靠投递不生效：检查 realtime outbox 和 KV outbox 两层开关。

## 12. 相关文档
- [后端 API 规范](../../../mango-pmo/rules/backend/03-api.md)
- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 13. 补充资料
- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
