# Mango Infra Realtime

`mango-infra-realtime` 是 Mango 的在线实时连接与投递基础设施。它负责维护用户、终端、连接、房间之间的在线关系，屏蔽 WebSocket / SSE / HTTP Polling 的传输差异，并把统一消息投递到在线目标。

一句话边界：

```text
realtime 负责把在线的人、端、房间连起来，并把消息可靠推过去；不理解业务消息含义。
```

## 职责边界

### 负责

| 职责 | 说明 |
|------|------|
| 连接管理 | 建立、断开、心跳、重连、在线状态维护 |
| Presence | 维护 `tenantId / userId / clientId / sessionId / groupId` 到节点和连接的路由关系 |
| 传输适配 | 统一 WebSocket、SSE、HTTP Polling 的连接、下行、上行差异 |
| 协议协商 | 根据浏览器能力、服务端能力和链路探测结果选择 transport |
| 统一 envelope | 服务端和前端使用同一套消息结构、编解码和解析逻辑 |
| 上行接收 | 接收客户端业务消息，分发给本地或远程业务监听器 |
| 下行投递 | 按 `USER / CLIENT / CONNECTION / GROUP / TENANT / BROADCAST` 路由投递 |
| 投递反馈 | 返回 accepted、delivered、error 等投递层结果，不代表业务处理成功 |
| 可靠投递 | 使用 infra-kv outbox 做发布入队、dispatcher 重试和失败记录 |
| 集群投递 | 使用 infra-kv presence 找到远端节点，并通过内部 endpoint 转发 |

### 不负责

| 非职责 | 应归属 |
|--------|--------|
| 聊天业务：历史消息、已读未读、撤回、禁言、@人 | 业务模块 |
| 通知业务：站内信、模板、短信、邮件、通知偏好 | `mango-biz-notification` |
| 离线收件箱：用户下次上线后拉历史 | message / notification domain |
| 业务事件编排：订单、审批、Agent、workflow 状态流 | 业务服务 / event / MQ |
| MQ 语义：消费组、死信、延迟消息、顺序消费 | MQ / event bus |
| 权限决策、租户鉴权、可信 header 清洗 | access / auth / gateway |

`event.domain = chat` 可以作为演示数据经过 realtime，但 realtime 不应该知道什么是聊天。

## 当前能力

| 能力 | 当前实现 |
|------|----------|
| WebSocket | `/realtime/transports/websocket`，双向通信，支持轻量 ping/pong 控制帧 |
| SSE | `GET /realtime/transports/sse`，服务端下行，客户端上行走 HTTP inbound |
| HTTP Polling | `GET /realtime/transports/polling`，支持短轮询和长轮询 |
| 协商 | `GET /realtime/transports/negotiate`，返回服务端能力、推荐协议和需要探测的链路 |
| 探测 | `/realtime/transports/probe/**`，用于协商阶段检测 WS/SSE/Polling 链路可用性 |
| 上行入口 | WebSocket 连接内发送，或 `POST /realtime/messages/inbound/sse|polling` |
| 下行发布 | `POST /realtime/messages/publish`，写入 reliable publish 链路 |
| 内部转发 | `POST /_realtime/messages/outbound`，节点间转发到连接所在实例 |
| Presence | 默认要求 infra-kv `IKvStore` 同时支持 `IKvSortedSet`，生产建议 Redis KV |
| Outbox | 使用 infra-kv outbox，事件类型 `realtime.message.dispatch` |

## 模块结构

| Maven 模块 | 职责 |
|------------|------|
| `mango-infra-realtime-api` | 对业务暴露 `RealtimeApi`、统一消息 DTO、监听注解和跨模块契约 |
| `mango-infra-realtime-core` | 会话、presence 抽象、协议 adapter、投递服务、协商票据、控制消息处理 |
| `mango-infra-realtime-support` | 入站监听器扫描和分发等内部支撑 |
| `mango-infra-realtime-starter` | 自动配置本地 realtime 节点，暴露 HTTP/WebSocket endpoint，装配 KV presence 和 outbox dispatcher |
| `mango-infra-realtime-starter-remote` | 给不承载连接的业务服务提供远程 `RealtimeApi` 和远程入站接收能力 |

依赖规则：

- 业务模块只依赖 `mango-infra-realtime-api`。
- 承载浏览器连接的应用依赖 `mango-infra-realtime-starter`。
- 不承载连接但需要发实时消息的应用依赖 `mango-infra-realtime-starter-remote`。
- 业务模块不要依赖 `core`，也不要直接操作 WebSocket/SSE/Polling adapter。

## 核心概念

| 概念 | 说明 |
|------|------|
| `tenantId` | 租户边界，用于 presence 与目标路由隔离 |
| `userId` | 一个业务用户。一个用户可以有多个终端、多个连接 |
| `clientId` | 一个接入端标识，通常表示一个浏览器 Tab、设备或客户端实例 |
| `sessionId` | 一条具体实时连接或 polling 会话 |
| `groupId` | 群组 / 房间 / 协作空间标识，只表达投递分组，不表达业务成员权限 |
| `connectionId` | 可与 `sessionId` 等价使用，表示单条连接目标 |
| `node` | 一个 realtime 服务实例，用于集群路由 |

推荐关系：

```text
tenant
  -> user
      -> client
          -> session / connection
  -> group
      -> session / connection
```

## 统一消息协议

业务消息使用 Unified Realtime Envelope v1。

### 客户端发送

```json
{
  "id": "01JV8A4EJ5N6P7Q8R9S0T1U2V3",
  "version": "1.0",
  "event": {
    "domain": "chat",
    "name": "message.send"
  },
  "source": {
    "platform": "web",
    "clientId": "browser-fe07d3",
    "sessionId": "websocket-browser-fe07d3"
  },
  "context": {
    "tenantId": "default",
    "userId": 1001,
    "traceId": "trace-001",
    "requestId": "req-001"
  },
  "target": {
    "type": "GROUP",
    "id": "room-001"
  },
  "metadata": {
    "roomId": "room-001",
    "roomName": "订单协作群",
    "senderName": "张三"
  },
  "payload": {
    "type": "text",
    "text": "hello"
  },
  "ack": {
    "required": true
  },
  "sequence": 10001,
  "timestamp": "2026-05-21T00:31:58.875Z"
}
```

### 服务端响应

```json
{
  "id": "ecb54afb-9062-421e-9aba-e2099ac051a4",
  "version": "1.0",
  "event": {
    "domain": "chat",
    "name": "message.accepted"
  },
  "context": {
    "tenantId": "default",
    "traceId": "trace-001",
    "requestId": "req-001"
  },
  "payload": {
    "type": "text",
    "text": "我收到你发送的消息“hello”"
  },
  "status": {
    "code": 200,
    "state": "SUCCESS"
  },
  "ack": {
    "messageId": "01JV8A4EJ5N6P7Q8R9S0T1U2V3",
    "accepted": true
  },
  "sequence": 10002,
  "timestamp": "2026-05-21T00:31:58.875Z"
}
```

### 字段说明

| 字段 | 说明 |
|------|------|
| `id` | 消息 ID，业务应保证客户端消息幂等可识别 |
| `version` | 协议版本，当前为 `1.0` |
| `event` | 事件定义，包含 `domain` 和 `name` |
| `source` | 客户端来源，例如平台、clientId、sessionId |
| `context` | 租户、用户、trace、request 等上下文 |
| `target` | 投递目标 |
| `metadata` | 业务元数据，只透传，不由 realtime 解释 |
| `payload` | 业务数据，只透传，不由 realtime 解释 |
| `ack` | ACK 要求或 ACK 结果 |
| `status` | 投递层状态 |
| `sequence` | 顺序号，由发送侧维护 |
| `timestamp` | UTC 时间 |

废弃字段映射：

| 旧字段 | 新字段 |
|--------|--------|
| `kind` | 删除 |
| `type` | `event.name` |
| `content` | `payload` |
| `headers` | `metadata` |

兼容解析可以保留，但新代码不要再生成旧字段。

## 系统控制消息

控制消息用于连接健壮性，不进入业务订阅。

| 类型 | 说明 |
|------|------|
| `PING` | 客户端或服务端发出的轻量心跳帧 |
| `PONG` | 对 `PING` 的轻量响应帧 |
| `connection.connected` | 连接建立通知 |
| `subscription.subscribe` | 订阅 group / room |
| `subscription.unsubscribe` | 取消订阅 |
| `message.accepted` | 投递层接收回执 |
| `message.delivered` | 投递层已投递反馈 |
| `message.failed` | 投递层失败反馈 |

ping/pong 不套完整业务 envelope，避免高频心跳浪费带宽和 CPU。业务 subscribe 不应收到 ping/pong。

## 投递目标

| Target | 说明 |
|--------|------|
| `USER` | 投递给某个用户的所有在线端 |
| `CLIENT` | 投递给某个客户端实例，例如一个浏览器 Tab 或设备 |
| `CONNECTION` | 投递给某条具体连接 |
| `GROUP` | 投递给某个房间 / 群组内的在线连接 |
| `TENANT` | 投递给某个租户内所有在线连接 |
| `BROADCAST` | 投递给所有在线连接 |

`GROUP` 只代表实时投递分组。群成员权限、历史成员、禁言、邀请等是业务职责。

## 服务端使用

业务模块只依赖 API：

```xml
<dependency>
    <groupId>io.mango</groupId>
    <artifactId>mango-infra-realtime-api</artifactId>
</dependency>
```

发布统一 envelope：

```java
import io.mango.infra.realtime.api.RealtimeApi;
import io.mango.infra.realtime.api.dto.RealtimeEvent;
import io.mango.infra.realtime.api.dto.RealtimeOutboundMessage;
import io.mango.infra.realtime.api.dto.RealtimePayload;
import io.mango.infra.realtime.api.dto.RealtimeTarget;
import io.mango.infra.realtime.api.dto.RealtimeTargetType;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OrderService {

    private final RealtimeApi realtimeApi;

    public void notifyRoom(String roomId, String text) {
        RealtimeOutboundMessage message = RealtimeOutboundMessage.builder()
                .event(new RealtimeEvent("chat", "message.delivered"))
                .target(new RealtimeTarget(RealtimeTargetType.GROUP, roomId))
                .payload(new RealtimePayload("text", text, null, null, null, null))
                .build();

        realtimeApi.publish(message);
    }
}
```

承载连接的应用依赖 starter：

```xml
<dependency>
    <groupId>io.mango</groupId>
    <artifactId>mango-infra-realtime-starter</artifactId>
</dependency>
```

非连接承载服务依赖 remote starter：

```xml
<dependency>
    <groupId>io.mango</groupId>
    <artifactId>mango-infra-realtime-starter-remote</artifactId>
</dependency>
```

## 客户端使用

前端推荐使用 `@mango/common` 中的无 UI 工具：

```ts
import { createRealtimeClient } from '@mango/common';

const client = createRealtimeClient({
  mode: 'auto',
  identity: {
    tenantId: 'default',
    userId: 1001,
    clientId: 'browser-fe07d3',
  },
  heartbeat: {
    interval: 30000,
    minInterval: 1000,
    timeout: 5000,
    suppressEvents: true,
  },
  reconnect: {
    enabled: true,
    maxRetries: 6,
    minDelay: 1000,
    maxDelay: 30000,
  },
  transportPolicy: {
    adaptive: true,
    fallbackOrder: ['websocket', 'sse', 'polling'],
    downgrade: {
      enabled: true,
      onConnectFailure: true,
      onHeartbeatTimeout: true,
      consecutiveErrors: 1,
    },
    upgrade: {
      enabled: false,
    },
  },
});

client.subscribe('chat.message.delivered', (message) => {
  renderMessage(message);
});

client.on('heartbeat', (record) => {
  renderHeartbeat(record);
});

await client.connect();

await client.send({
  event: { domain: 'chat', name: 'message.send' },
  target: { type: 'GROUP', id: 'room-001' },
  metadata: { roomName: '订单协作群' },
  payload: { type: 'text', text: 'hello' },
  ack: { required: true },
});
```

`/components/realtime` 是工具能力演示页，不是业务聊天组件。真实业务页面应自行组织 UI，只复用 `createRealtimeClient` 或 `useRealtime`。

## 协商与降级

Auto 模式流程：

```text
browser capability
  -> GET /realtime/transports/negotiate
  -> server capability + recommended order
  -> probe required transports
  -> connect selected transport
  -> reconnect same transport on transient failure
  -> after configured failures, downgrade to next transport
```

降级只处理传输层失败，例如连接超时、链路断开、心跳超时。以下情况不是降级理由：

- `401 Unauthorized`
- `403 Forbidden`
- 业务鉴权失败
- 协商接口网络不通
- 服务端明确禁用某协议

固定模式 `websocket / sse / polling` 不走协商，只连接指定协议。

## 认证与上下文

- 普通 HTTP 请求，包括协商、上行、发布接口，认证信息应走 header / cookie / security context。
- WebSocket 和原生 EventSource 不能像 fetch 一样自定义 header，生产可使用同站 cookie、网关注入上下文、短期 connection ticket。
- 不要把长期 access token 放入 URL。
- `clientId` 表示端，不等于用户；是否把端与用户绑定由业务和认证体系决定。
- `tenantId / userId` 的可信来源应由 access/auth/gateway 体系确定，realtime 只消费上下文。

## 集群与可靠投递

### Presence

Presence 保存路由元数据，不保存真实连接对象：

```text
sessionId -> node + tenantId + userId + clientId
userId -> sessionId set
clientId -> sessionId set
groupId -> sessionId set
tenantId -> sessionId set
```

starter 默认要求可用的 infra-kv `IKvStore` 同时实现 `IKvSortedSet`，用于 TTL 索引和过期清理。生产建议 Redis KV。

### Outbox

业务调用发布接口后：

```text
RealtimeApi.publish
  -> IRealtimeReliablePublishService
  -> infra-kv outbox append event: realtime.message.dispatch
  -> RealtimeOutboxDispatcher claim
  -> IRealtimePublishService.publish
  -> local send + remote forward
  -> ack / nack outbox
```

outbox 提升的是“发布到在线投递链路”的可靠性，不等于：

- 离线消息持久化；
- 业务消费成功；
- MQ 消费语义；
- 消息中心历史记录。

将来接入 MQ 后，outbox 仍然有价值：它可以作为本地事务到 MQ / realtime dispatcher 的可靠桥，避免业务事务成功但投递事件丢失。

## 配置

示例：

```yaml
mango:
  infra:
    realtime:
      enabled: true
      mode: auto
      node:
        instance-id: ${HOSTNAME:${spring.application.name}}
        service-name: ${spring.application.name}
        context-path: /
      presence:
        prefix: mango:infra:realtime:presence
        ttl-seconds: 120
      outbox:
        enabled: true
        batch-size: 50
        max-attempts: 5
        initial-delay-millis: 1000
        fixed-delay-millis: 500
        retry-backoff-millis: 1000
      inbound:
        enabled: true
        mode: local_remote
        max-payload-bytes: 65536
        fail-fast: false
        unknown-type-policy: warn
      websocket:
        enabled: true
        endpoint: /realtime/transports/websocket
        allowed-origins:
          - "https://app.example.com"
      sse:
        enabled: true
        endpoint: /realtime/transports/sse
        timeout-millis: 300000
      polling:
        enabled: true
        endpoint: /realtime/transports/polling
        default-max-size: 20
        max-size: 100
        default-timeout-millis: 0
        max-timeout-millis: 25000
      negotiate:
        enabled: true
        endpoint: /realtime/transports/negotiate
```

关键配置：

| 配置 | 说明 |
|------|------|
| `enabled` | realtime 总开关 |
| `mode` | `auto / websocket / sse / polling` |
| `node.instance-id` | 当前实例 ID，同服务多副本必须唯一 |
| `node.service-name` | 远端节点访问当前实例时使用的服务名 |
| `presence.ttl-seconds` | presence 路由 TTL |
| `outbox.enabled` | 是否启用 reliable publish outbox |
| `inbound.enabled` | 是否启用客户端上行业务消息分发 |
| `inbound.mode` | `none / local / remote / local_remote` |
| `websocket.allowed-origins` | 生产必须收敛为明确域名 |

## Endpoint

| Endpoint | 用途 | 暴露方 |
|----------|------|--------|
| `GET /realtime/transports/negotiate` | 协商 transport | starter |
| `/realtime/transports/websocket` | WebSocket 连接 | starter |
| `GET /realtime/transports/sse` | SSE 连接 | starter |
| `GET /realtime/transports/polling` | Polling 拉取 | starter |
| `/realtime/transports/probe/websocket` | WebSocket 探测 | starter |
| `GET /realtime/transports/probe/sse` | SSE 探测 | starter |
| `GET /realtime/transports/probe/polling` | Polling 探测 | starter |
| `POST /realtime/messages/inbound/sse` | SSE 模式客户端上行 | starter |
| `POST /realtime/messages/inbound/polling` | Polling 模式客户端上行 | starter |
| `POST /realtime/messages/publish` | 服务端正向发布 | starter |
| `POST /_realtime/messages/outbound` | 节点间出站转发 | starter |
| `POST /_realtime/messages/inbound` | remote starter 接收入站转发 | starter-remote |

`/_realtime/**` 是内部接口，不应暴露给公网。

## 与 notification / MQ 的关系

| 模块 | 负责 | 不负责 |
|------|------|--------|
| `mango-infra-realtime` | 在线连接、presence、传输适配、在线投递、投递结果 | 消息业务语义、历史、已读未读 |
| `mango-biz-notification` | 业务通知、消息记录、模板、已读未读、离线可见 | WebSocket/SSE/Polling 连接 |
| MQ / event bus | 跨服务异步事件、削峰、消费组、死信、延迟、顺序 | 浏览器在线连接状态 |
| infra-kv outbox | 本地可靠入队、重试、失败记录 | 业务消息中心或完整 MQ |

推荐调用链：

```text
business service
  -> notification/message domain records business state
  -> realtime publish online delivery envelope
  -> realtime transport sends to connected clients
```

只需要临时在线提示、不需要业务记录时，可以直接调用 `RealtimeApi`。

## 运维注意事项

- 网关必须支持 WebSocket Upgrade。
- SSE 需要关闭代理缓冲，保持流式响应。
- 浏览器、网关、LB、容器的 idle timeout 要和心跳配置匹配。
- 前端重连必须退避，不要毫秒级固定重连。
- 心跳默认建议 30s；高实时场景可降到 1s 或 500ms，但必须评估连接数、带宽、CPU 和网关压力。
- `allowed-origins` 生产不要使用 `*`。
- presence TTL 要大于心跳间隔和短暂网络抖动窗口。
- outbox dispatcher 要监控积压、失败次数和重试延迟。

## 当前不做

- 不做聊天系统。
- 不做消息中心。
- 不做离线收件箱。
- 不做已读未读。
- 不做模板通知。
- 不做完整 MQ。
- 不做业务权限判断。

## 验收命令

后端 realtime 相关测试：

```bash
mvn -pl mango-infra/mango-infra-test -am \
  -Dtest=MangoRealtimeProtocolIntegrationTest,MangoRealtimeOutboundMultiInstanceE2ETest,RealtimePresenceAutoConfigurationTest,RealtimeOutboxAutoConfigurationTest,OutboxAutoConfigurationTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

前端工具单测：

```bash
pnpm -F mango-admin exec vitest run src/__tests__/realtime-client.spec.ts --environment happy-dom
```

前端构建：

```bash
pnpm -F mango-admin build
```

真实页面 E2E：

```bash
REALTIME_E2E_BASE_URL=http://127.0.0.1:7791 node apps/mango-admin/e2e/realtime-real-check.mjs
REALTIME_E2E_BASE_URL=http://127.0.0.1:7791 REALTIME_E2E_MODE=websocket node apps/mango-admin/e2e/realtime-real-check.mjs
REALTIME_E2E_BASE_URL=http://127.0.0.1:7791 REALTIME_E2E_MODE=sse node apps/mango-admin/e2e/realtime-real-check.mjs
REALTIME_E2E_BASE_URL=http://127.0.0.1:7791 REALTIME_E2E_MODE=polling node apps/mango-admin/e2e/realtime-real-check.mjs
REALTIME_E2E_BASE_URL=http://127.0.0.1:7791 node apps/mango-admin/e2e/realtime-group-real-check.mjs
```
