# Mango Infra Realtime

`mango-infra-realtime` 是面向“服务端向在线客户端实时投递消息”的基础设施模块。它统一承载 SSE、WebSocket、HTTP Polling、连接会话、订阅管理、客户端 transport 协商和跨服务远程发布。

这个模块只解决在线实时投递问题，不负责业务通知、消息中心、已读未读、离线消息、模板、审计持久化，也不承担 MQ/Event 语义。业务通知能力属于 `mango-biz-notification`；未来 MQ/Event 能力应保留给 `mango-infra-messaging`。

## 核心特性

| 特性 | 当前能力 |
|------|----------|
| SSE 单向推送 | 提供 `GET /realtime/transports/sse`，服务端向浏览器持续推送消息 |
| WebSocket 双向连接 | 提供 `/realtime/transports/websocket`，支持连接建立通知、`ping`/`pong`、客户端业务消息入站分发 |
| HTTP Polling | 提供 `GET /realtime/transports/polling`，支持短轮询和带 hold timeout 的长轮询 |
| Transport 协商 | 提供 `GET /realtime/transports/negotiate`，返回服务端可用 transport 和推荐协议 |
| 用户定向投递 | `RealtimeApi.publishToUser(userId, type, content)` |
| 租户内投递 | `RealtimeApi.publishToTenant(tenantId, type, content)` |
| 全局广播 | `RealtimeApi.broadcast(type, content)` |
| 本地 Polling 队列 | `core.polling.RealtimePollingService` 本地能力，支撑 HTTP Polling 的临时在线队列 |
| 远程发布 | `mango-infra-realtime-starter-remote` 通过内部 Feign 调用连接承载服务 |
| 多实例在线投递 | starter 节点维护 presence 路由，跨节点通过 `POST /_realtime/messages/outbound` 转发到连接所在节点 |
| 客户端入站 | WebSocket、SSE、Polling 各自通过独立入口接收业务消息，经 `@RealtimeInboundMessageListener` 分发给业务模块 |
| 条件化开关 | 支持总开关、SSE、WebSocket、Polling、协商入口、内部远程发布入口独立开关 |

当前实现仍使用本地内存会话和本地内存 Polling 队列；不会把真实连接对象放入共享存储。多实例在线投递依赖 presence 路由表：默认提供内存实现，生产多实例应替换为 Redis、DB、注册中心或其它共享实现。离线队列、持久化投递和可靠消息确认不是本模块当前能力。

## 模块结构

| Maven 模块 | 职责 |
|------------|------|
| `mango-infra-realtime-api` | 对业务和 remote adapter 暴露 `RealtimeApi`、`RealtimeOutboundMessage`、`RealtimeInboundMessageListener`、`RealtimeInboundMessage`、`RealtimeInboundApi`、`RealtimeInboundReceiverApi` 等跨模块契约 |
| `mango-infra-realtime-support` | 仅供 `core` / `starter-remote` 复用的内部运行时支撑，例如入站监听器扫描 |
| `mango-infra-realtime-core` | 提供内存订阅管理、连接会话、presence 路由抽象、内存 Polling 队列、SSE/WebSocket/Polling 协议 adapter、协议分发、入站监听分发和订阅服务注册表，不放 HTTP Controller |
| `mango-infra-realtime-starter` | 自动配置本地 realtime 能力，暴露客户端协议入口、正向发布入口、入站注册入口和跨节点出站反向入口，所有 HTTP Controller 都放在这一层 |
| `mango-infra-realtime-starter-remote` | 提供 Feign 版远程 `RealtimeApi`、入站监听自动注册和内部入站接收 endpoint；只能依赖 `api`、`support` 和 Feign 装配，不能依赖 `core` |

业务模块只能依赖 `mango-infra-realtime-api`。不要依赖 `core`，也不要直接装配协议实现类。

分层命名约束：

- `Api`：跨模块 HTTP/Feign 契约，只放在 `api` 模块。
- `XxxController` / `XxxFeignClient`：`Api` 的实现。
- `I...Service`：模块内部服务接口，不能放到 `api` 模块。
- `...Service`：`I...Service` 的实现；`Controller` 只能持有 `I...Service`，不能持有 `Api`。
- `Registry`、`Dispatcher` 这类实现细节不进入 `Api` 命名。

## 能力边界

`mango-infra-realtime` 负责：

- 建立和维护在线连接。
- 按用户、租户、广播范围投递实时消息。
- 适配 SSE、WebSocket 和 HTTP Polling 协议。
- 为前端提供 transport 协商入口。
- 为不承载连接的服务提供远程发布实现。
- 基于 presence 路由把 server-to-client 消息转发到真实承载连接的 realtime 节点。
- 接收客户端 WebSocket、SSE、Polling 入站业务消息，并分发给本地或远程服务内的业务监听器。
- 提供本地 Polling Java API 作为 HTTP Polling 的临时在线队列。

`mango-infra-realtime` 不负责：

- 业务消息表、消息模板、通知渠道、站内信列表。
- 已读未读、未读数、消息撤回、消息归档。
- 离线消息持久化、审计级持久化、跨节点可靠投递。
- 跨节点共享真实连接对象；真实连接只存在于本机内存。
- MQ、事件总线、领域事件发布订阅。
- 钉钉、短信、邮件、Agent 流式聚合等业务化能力。

涉及消息中心、离线可见、已读未读、审计追踪的场景，应通过 `mango-biz-notification` 处理业务状态，再使用 `mango-infra-realtime` 做在线投递。

## 服务端如何使用

### 业务模块依赖 API

业务模块，例如 `a-module`、`b-module`，只声明编译期契约：

```xml
<dependency>
    <groupId>io.mango</groupId>
    <artifactId>mango-infra-realtime-api</artifactId>
</dependency>
```

业务代码注入 `RealtimeApi`。本模块不保留 `RealtimePublisher` 兼容别名，所有发布入口统一使用 `RealtimeApi`：

```java
import io.mango.infra.realtime.api.RealtimeApi;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ApprovalService {

    private final RealtimeApi realtimeApi;

    public void approve(Long applicantUserId) {
        // 业务处理完成后，向指定在线用户发送临时实时提示。
        realtimeApi.publishToUser(applicantUserId, "approval.done", "审批已完成");
    }
}
```

租户内投递：

```java
realtimeApi.publishToTenant("tenant-a", "import.done", "导入任务已完成");
```

广播：

```java
realtimeApi.broadcast("system.notice", "系统将在 23:00 维护");
```

需要自定义 envelope 时直接构造 `RealtimeOutboundMessage`：

```java
RealtimeOutboundMessage message = new RealtimeOutboundMessage(
        null,
        "task.progress",
        "{\"percent\":80}",
        "tenant-a",
        1001L,
        Map.of("taskId", "T-001"),
        null);

realtimeApi.publish(message);
```

### App 选择本地 starter 或 remote starter

承载前端 SSE/WebSocket 连接的服务依赖本地 starter：

```xml
<dependency>
    <groupId>io.mango</groupId>
    <artifactId>mango-infra-realtime-starter</artifactId>
</dependency>
```

它会暴露：

- `GET /realtime/transports/sse`
- `/realtime/transports/websocket`
- `GET /realtime/transports/polling`
- `GET /realtime/transports/negotiate`
- `POST /realtime/messages/publish`

不承载前端连接、但部署了会发送消息的业务模块的服务依赖 remote starter：

```xml
<dependency>
    <groupId>io.mango</groupId>
    <artifactId>mango-infra-realtime-starter-remote</artifactId>
</dependency>
```

remote starter 不暴露 `/realtime/transports/sse` 或 `/realtime/transports/websocket`，只提供远程 `RealtimeApi`，把消息转发到真正承载连接的服务。该远程 `RealtimeApi` 直接由 `RealtimeFeignClient` 实现，不再额外包装 publisher。

典型部署：

```text
c-service: a-module, b-module, mango-infra-realtime-starter-remote
d-service: mango-infra-realtime-starter
frontend: 连接 d-service 的 /realtime/transports/negotiate、/realtime/transports/sse、/realtime/transports/websocket 或 /realtime/transports/polling
```

远程调用目标通过模块名 `mango-infra-realtime` 解析，可由模块服务映射配置覆盖：

```yaml
mango:
  module:
    module-service:
      modules:
        mango-infra-realtime:
          service-name: d-service
          context-path: /
```

如果 `c-service` 部署了注入 `RealtimeApi` 的业务模块，但既没有 `mango-infra-realtime-starter`，也没有 `mango-infra-realtime-starter-remote`，启动时通常会失败：

```text
NoSuchBeanDefinitionException: No qualifying bean of type 'RealtimeApi'
```

如果 `c-service` 误用了本地 starter，它会暴露自己的连接入口并维护自己的内存连接表。前端如果实际连接在 `d-service`，`c-service` 本地发布的消息不会自动送到 `d-service` 的客户端，形成连接和发布分裂。因此非连接承载服务必须使用 remote starter。

### 业务模块接收客户端入站消息

realtime 模块只负责把客户端入站消息路由给业务监听器，不定义具体业务语义。三种协议分别保留各自入口：

- WebSocket：连接内发送文本消息
- SSE：`POST /realtime/messages/inbound/sse`
- Polling：`POST /realtime/messages/inbound/polling`

三者底层都复用同一套入站转发逻辑，最终统一封装为 `RealtimeInboundMessage` 并按 listener 类型分发。

方法级监听器：

```java
import io.mango.infra.realtime.api.dto.RealtimeInboundMessage;
import io.mango.infra.realtime.api.annotation.RealtimeInboundMessageListener;
import org.springframework.stereotype.Component;

@Component
public class TaskRealtimeListener {

    @RealtimeInboundMessageListener(types = "task.cancel")
    public void onTaskCancel(RealtimeInboundMessage message) {
        // message.content() 通常是业务 JSON 字符串，由业务模块自行解析和校验。
    }
}
```

`@RealtimeInboundMessageListener` 只支持方法级监听。监听方法只允许一个参数，类型必须是 `RealtimeInboundMessage`。

`starter-remote` 在扫描监听器时按 Spring AOP 目标类和可调用方法解析，因此被 `@Transactional`、`@Async` 或其他 advisor 代理的监听 bean 仍然会被识别、注册并正常分发。

承载连接的服务可以本地分发：

```yaml
mango:
  infra:
    realtime:
      inbound:
        enabled: true
        mode: local
```

如果前端连接在 `d-service`，而业务监听器在 `c-service`，则：

- `d-service` 依赖 `mango-infra-realtime-starter`，配置 `inbound.mode=local_remote`。
- `c-service` 依赖 `mango-infra-realtime-starter-remote`，配置 `inbound.enabled=true`。
- `c-service` 启动后自动扫描 `@RealtimeInboundMessageListener`，包括被 Spring AOP 代理的监听 bean，通过 `POST /realtime/receivers/register` 注册到 `d-service`。
- `d-service` 收到客户端入站消息后，向已注册服务的固定 `POST /_realtime/messages/inbound` 转发。
- `c-service` 收到内部入站消息后，再本地分发给具体业务监听器。

这个方案暂不依赖 `mango-infra-module`。注册信息由 realtime 自己维护为内存注册表，服务重启后由 remote starter 重新注册；它不是可靠事件总线，也不提供 ack、重试、死信或离线补偿。

## 前端如何使用

### Transport 协商

前端可以先请求协商接口，让服务端返回当前可用 transport，再按推荐结果连接。协商不是鉴权替代品，只用于能力发现和降级选择。

默认入口：

```text
GET /realtime/transports/negotiate
GET /realtime/transports/negotiate?prefer=websocket,sse,polling
```

协商结果本身不承担认证语义；租户上下文和认证状态应继续由登录态或网关注入管理，协商结果本身不携带业务数据。

响应示例：

```json
{
  "recommended": "websocket",
  "transports": [
    {
      "type": "websocket",
      "enabled": true,
      "endpoint": "/realtime/transports/websocket",
      "bidirectional": true,
      "longPolling": false
    },
    {
      "type": "sse",
      "enabled": true,
      "endpoint": "/realtime/transports/sse",
      "bidirectional": false,
      "longPolling": false
    },
    {
      "type": "polling",
      "enabled": true,
      "endpoint": "/realtime/transports/polling",
      "bidirectional": false,
      "longPolling": true,
      "defaultMaxSize": 20,
      "maxSize": 100,
      "defaultTimeoutMillis": 0,
      "maxTimeoutMillis": 25000
    }
  ]
}
```

推荐前端选择策略：

1. 页面需要双向通信且环境支持 WebSocket：优先 `websocket`。
2. 只需要服务端单向推送且 SSE 可用：使用 `sse`。
3. 代理、网络或浏览器环境不稳定时：降级 `polling`。

三种协议建议统一使用同一个业务消息体：

```json
{
  "id": "client-message-id",
  "type": "task.cancel",
  "content": "{\"taskId\":\"T-001\"}",
  "headers": {
    "source": "task-panel"
  }
}
```

其中 `tenantId`、`userId`、`sessionId` 由服务端根据连接或请求上下文补齐，前端不应伪造这些字段。

### SSE

SSE 适合服务端到浏览器的单向推送，例如通知角标、任务进度、审批状态刷新。

默认入口：

```text
GET /realtime/transports/sse?userId=1001
```

服务端当前会读取：

| 来源 | 字段 | 说明 |
|------|------|------|
| Header | `TENANT-ID` | 推荐租户传递方式；缺省为 `default` |
| Query | `userId` | 可选；用于绑定当前连接所属用户 |

浏览器原生 `EventSource` 不能设置自定义 header，因此生产接入不要直接依赖 URL token。认证、租户、可信上下文等能力应继续由 gateway / security / auth 体系承担。

前端接入示例：

```javascript
const events = new EventSource('/realtime/transports/sse?userId=1001');

events.onmessage = (event) => {
  const message = JSON.parse(event.data);
  console.log(message.type, message.content);
};

events.onerror = () => {
  events.close();
  // 由前端统一重连策略处理，避免高频重试打满服务端。
};
```

SSE 上行消息入口：

```text
POST /realtime/messages/inbound/sse?userId=1001&sessionId=sse-1
```

```javascript
async function sendSseInbound(message) {
  await fetch('/realtime/messages/inbound/sse?userId=1001&sessionId=sse-1', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'TENANT-ID': 'tenant-a' },
    body: JSON.stringify(message)
  });
}
```

### WebSocket

WebSocket 适合需要双向交互或更频繁实时通信的页面。

默认入口：

```text
/realtime/transports/websocket
```

握手阶段当前会读取：

| 来源 | 字段 | 说明 |
|------|------|------|
| Header | `Authorization` | 推荐，值形如 `Bearer ...` |
| Header | `TENANT-ID` | 推荐租户传递方式 |
| Query | `token` | 兼容入口；不推荐生产长期使用 |
| Query | `tenantId` | 兼容入口；不推荐生产长期使用 |
| Query | `userId` | 可选；用于绑定当前连接所属用户 |

浏览器 WebSocket 原生 API 不能自定义 header，所以常见生产方案是：网关注入认证上下文、使用同站 cookie、或先换取短期 connection ticket，再作为兼容 query 参数传入。不要把长期 access token 放在 URL 中。

前端接入示例：

```javascript
const ticket = encodeURIComponent(connectionTicket);
const socket = new WebSocket(`/realtime/transports/websocket?token=${ticket}&userId=1001`);

socket.onmessage = (event) => {
  const message = JSON.parse(event.data);
  switch (message.type) {
    case 'connected':
      break;
    case 'pong':
      break;
    default:
      console.log(message.type, message.content);
  }
};

socket.onopen = () => {
  socket.send(JSON.stringify({ type: 'ping' }));
  socket.send(JSON.stringify({
    id: 'client-message-id',
    type: 'task.cancel',
    content: JSON.stringify({ taskId: 'T-001' }),
    headers: { source: 'task-panel' }
  }));
};

socket.onclose = () => {
  // 由前端统一退避重连策略处理。
};
```

当前 WebSocket 入站消息支持：

| 入站 `type` | 行为 |
|-------------|------|
| `ping` | 服务端返回 `type=pong` |
| 业务类型，例如 `task.cancel` | 服务端封装为 `RealtimeInboundMessage`，按 `@RealtimeInboundMessageListener(types = "...")` 分发 |
| 非法 JSON | 服务端返回 `type=error`、`content=Invalid message format` |
| 超过 `mango.infra.realtime.inbound.max-payload-bytes` | 服务端返回 `type=error`、`content=Realtime inbound message too large` |

### Polling

Polling 适合浏览器或网络环境无法稳定保持 WebSocket/SSE 的场景。它不是消息中心，也不保证离线可见；只表示前端用 HTTP 循环请求在线临时消息。

默认入口：

```text
GET /realtime/transports/polling?userId=1001
GET /realtime/transports/polling?userId=1001&maxSize=20&timeoutMillis=25000
```

服务端当前会读取：

| 来源 | 字段 | 说明 |
|------|------|------|
| Header | `TENANT-ID` | 推荐租户传递方式；缺省为 `default` |
| Query | `userId` | 必填；用于绑定当前 polling 客户端所属用户 |
| Query | `maxSize` | 可选；本次最多返回多少条 |
| Query | `timeoutMillis` | 可选；`0` 表示短轮询，大于 `0` 表示长轮询 hold 时间 |

短轮询示例：

```javascript
async function pollOnce() {
  const response = await fetch('/realtime/transports/polling?userId=1001&maxSize=20');
  return response.json();
}

setInterval(async () => {
  const messages = await pollOnce();
  messages.forEach(handleRealtimeMessage);
}, 1000);
```

长轮询示例：

```javascript
async function longPoll() {
  while (true) {
    try {
      const response = await fetch('/realtime/transports/polling?userId=1001&maxSize=20&timeoutMillis=25000');
      const messages = await response.json();
      messages.forEach(handleRealtimeMessage);
    } catch (error) {
      await new Promise((resolve) => setTimeout(resolve, 3000));
    }
  }
}

longPoll();
```

Polling 上行消息入口：

```text
POST /realtime/messages/inbound/polling?userId=1001&sessionId=poll-1
```

```javascript
async function sendPollingInbound(message) {
  await fetch('/realtime/messages/inbound/polling?userId=1001&sessionId=poll-1', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'TENANT-ID': 'tenant-a' },
    body: JSON.stringify(message)
  });
}
```

服务端 Java API：

```java
realtimePollingService.append("user:1001", RealtimeOutboundMessage.toUser(1001L, "task.done", "任务完成"));
List<RealtimeOutboundMessage> messages = realtimePollingService.poll("user:1001", 20);
```

## 消息格式

服务端向客户端投递使用 `RealtimeOutboundMessage`：

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | `String` | 为空时自动生成 UUID |
| `type` | `String` | 消息类型；为空时默认为 `message` |
| `content` | `String` | 消息内容；可放纯文本或业务 JSON 字符串 |
| `tenantId` | `String` | 租户 ID；为空时默认为 `default` |
| `userId` | `Long` | 用户 ID；为空表示非用户定向消息 |
| `headers` | `Map<String,Object>` | 扩展元数据 |
| `createdAt` | `Instant` | 创建时间；为空时使用当前时间 |

客户端收到的典型 JSON：

```json
{
  "id": "0e8f1842-8e64-42e0-b59b-b74c6080a3c5",
  "type": "approval.done",
  "content": "审批已完成",
  "tenantId": "tenant-a",
  "userId": 1001,
  "headers": {},
  "createdAt": "2026-04-18T10:15:30Z"
}
```

Polling 上行消息入口：

```text
POST /realtime/messages/inbound/polling?userId=1001&sessionId=poll-1
```

请求体格式与 WebSocket / SSE 入站保持一致。

`type` 建议使用稳定的业务事件名，例如 `approval.done`、`task.progress`、`import.failed`。前端基于 `type` 分发 UI 行为，不要解析自然语言 `content` 来判断业务类型。

## 配置

默认配置：

```yaml
mango:
  infra:
    realtime:
      enabled: true
      mode: auto
      remote:
        endpoint-enabled: true
      node:
        instance-id:
        service-name:
        context-path:
      outbound:
        endpoint-enabled: true
        endpoint: /_realtime/messages/outbound
      presence:
        prefix: mango:infra:realtime:presence
        ttl-seconds: 120
      inbound:
        enabled: false
        mode: none
        max-payload-bytes: 65536
        fail-fast: false
        unknown-type-policy: ignore
        remote:
          endpoint-enabled: true
          register-enabled: true
          endpoint: /_realtime/messages/inbound
          service-name:
          context-path: /
      sse:
        enabled: true
        endpoint: /realtime/transports/sse
        timeout-millis: 300000
      websocket:
        enabled: true
        endpoint: /realtime/transports/websocket
        allowed-origins:
          - "*"
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

配置说明：

| 配置 | 默认值 | 说明 |
|------|--------|------|
| `mango.infra.realtime.enabled` | `true` | 总开关；关闭后不自动配置 realtime |
| `mango.infra.realtime.mode` | `auto` | 运行模式：`auto`、`sse`、`websocket`、`polling` |
| `mango.infra.realtime.remote.endpoint-enabled` | `true` | 是否启用正向 `POST /realtime/messages/publish` |
| `mango.infra.realtime.node.instance-id` | `spring.application.instance_id` 或 `spring.application.name` | 当前 realtime 节点实例 ID，用于过滤本机 presence，生产多实例应保证同服务不同实例不重复 |
| `mango.infra.realtime.node.service-name` | `spring.application.name` | 当前节点可被其它 realtime 节点访问的服务名；使用注册中心时填服务名，直连测试可填 `localhost:port` |
| `mango.infra.realtime.node.context-path` | `server.servlet.context-path` 或 `/` | 当前服务真实 context path，不建议在 `module.properties` 固定 |
| `mango.infra.realtime.outbound.endpoint-enabled` | `true` | 是否启用反向 `POST /_realtime/messages/outbound`，用于接收其它 realtime 节点转来的 server-to-client 消息 |
| `mango.infra.realtime.outbound.endpoint` | `/_realtime/messages/outbound` | 当前节点接收跨节点出站消息的反向入口 |
| `mango.infra.realtime.presence.prefix` | `mango:infra:realtime:presence` | KV presence 的 key 前缀；仅在运行时存在 `IKvStore` 且实现 `IKvSortedSet` 时使用 |
| `mango.infra.realtime.presence.ttl-seconds` | `120` | KV presence 路由 TTL；长连接会由当前节点定时续约 |
| `mango.infra.realtime.inbound.enabled` | `false` | 是否启用客户端到服务端的业务入站消息分发 |
| `mango.infra.realtime.inbound.mode` | `local_remote` | 本地 starter 入站分发模式：`none`、`local`、`remote`、`local_remote` |
| `mango.infra.realtime.inbound.max-payload-bytes` | `65536` | 单条 WebSocket 入站文本消息最大字节数 |
| `mango.infra.realtime.inbound.fail-fast` | `false` | 单个 listener 失败时是否停止后续 listener |
| `mango.infra.realtime.inbound.unknown-type-policy` | `ignore` | 未找到 listener 时的处理策略：`ignore`、`warn`、`error` |
| `mango.infra.realtime.inbound.remote.endpoint-enabled` | `true` | remote starter 是否暴露 `POST /_realtime/messages/inbound` |
| `mango.infra.realtime.inbound.remote.register-enabled` | `true` | remote starter 是否在启动时自动注册自己 |
| `mango.infra.realtime.inbound.remote.endpoint` | `/_realtime/messages/inbound` | remote starter 接收入站消息的反向入口 |
| `mango.infra.realtime.inbound.remote.service-name` | `spring.application.name` | 自动注册时上报的服务名；为空时读取 `spring.application.name` |
| `mango.infra.realtime.inbound.remote.context-path` | `server.servlet.context-path` 或 `/` | 自动注册时上报的服务 context path |
| `mango.infra.realtime.sse.enabled` | `true` | `mode=auto` 时是否启用 SSE adapter |
| `mango.infra.realtime.sse.endpoint` | `/realtime/transports/sse` | SSE 订阅入口 |
| `mango.infra.realtime.sse.inbound-endpoint` | `/realtime/messages/inbound/sse` | SSE 客户端上行消息入口 |
| `mango.infra.realtime.sse.timeout-millis` | `300000` | SSE 连接超时时间，单位毫秒 |
| `mango.infra.realtime.websocket.enabled` | `true` | `mode=auto` 时是否启用 WebSocket handler |
| `mango.infra.realtime.websocket.endpoint` | `/realtime/transports/websocket` | WebSocket 连接入口 |
| `mango.infra.realtime.websocket.allowed-origins` | `["*"]` | WebSocket 允许来源；生产环境应收敛为明确域名 |
| `mango.infra.realtime.polling.enabled` | `true` | `mode=auto` 时是否启用 HTTP Polling adapter 和 endpoint |
| `mango.infra.realtime.polling.endpoint` | `/realtime/transports/polling` | HTTP Polling 入口 |
| `mango.infra.realtime.polling.inbound-endpoint` | `/realtime/messages/inbound/polling` | Polling 客户端上行消息入口 |
| `mango.infra.realtime.polling.default-max-size` | `20` | caller 传入 `maxSize <= 0` 时的默认返回条数 |
| `mango.infra.realtime.polling.max-size` | `100` | 单次 polling 最大返回条数上限 |
| `mango.infra.realtime.polling.default-timeout-millis` | `0` | caller 不传 `timeoutMillis` 时的默认 hold 时间；`0` 表示短轮询 |
| `mango.infra.realtime.polling.max-timeout-millis` | `25000` | 长轮询最大 hold 时间，单位毫秒 |
| `mango.infra.realtime.negotiate.enabled` | `true` | 是否启用 `GET /realtime/transports/negotiate` 客户端协商入口 |
| `mango.infra.realtime.negotiate.endpoint` | `/realtime/transports/negotiate` | Transport 协商入口 |

`mode` 说明：

| mode | 行为 |
|------|------|
| `auto` | 按 `sse.enabled`、`websocket.enabled`、`polling.enabled` 分别启用 |
| `sse` | 只启用 SSE 发布协议和内部远程发布入口 |
| `websocket` | 只启用 WebSocket 发布协议和内部远程发布入口 |
| `polling` | 只启用 HTTP Polling 发布协议和内部远程发布入口，不暴露 SSE/WebSocket |

生产示例：

```yaml
mango:
  infra:
    realtime:
      enabled: true
      mode: auto
      remote:
        endpoint-enabled: true
      inbound:
        enabled: true
        mode: remote
        max-payload-bytes: 65536
        fail-fast: false
        unknown-type-policy: warn
      sse:
        enabled: true
        endpoint: /realtime/transports/sse
        timeout-millis: 300000
      websocket:
        enabled: true
        endpoint: /realtime/transports/websocket
        allowed-origins:
          - "https://app.example.com"
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

## 协议入口

| 协议/能力 | 默认入口 | 暴露方 | 默认状态 |
|-----------|----------|--------|----------|
| SSE | `GET /realtime/transports/sse` | `mango-infra-realtime-starter` | 启用 |
| WebSocket | `/realtime/transports/websocket` | `mango-infra-realtime-starter` | 启用 |
| Polling | `GET /realtime/transports/polling` | `mango-infra-realtime-starter` | 启用 |
| SSE Inbound | `POST /realtime/messages/inbound/sse` | `mango-infra-realtime-starter` | `inbound.enabled=true` 时启用 |
| Polling Inbound | `POST /realtime/messages/inbound/polling` | `mango-infra-realtime-starter` | `inbound.enabled=true` 时启用 |
| Transport 协商 | `GET /realtime/transports/negotiate` | `mango-infra-realtime-starter` | 启用 |
| 远程发布 | `POST /realtime/messages/publish` | `mango-infra-realtime-starter` | 启用 |
| 跨节点出站接收 | `POST /_realtime/messages/outbound` | `mango-infra-realtime-starter` | 启用 |
| 入站接收器注册 | `POST /realtime/receivers/register` | `mango-infra-realtime-starter` | 启用 |
| 入站接收器注销 | `POST /realtime/receivers/unregister` | `mango-infra-realtime-starter` | 启用 |
| 远程入站接收 | `POST /_realtime/messages/inbound` | `mango-infra-realtime-starter-remote` | `inbound.enabled=true` 时启用 |

`/realtime/messages/publish` 和 `/realtime/receivers/*` 是正向服务调用入口，应只允许服务间调用，不应直接暴露给公网或前端。
`/_realtime/messages/inbound` 与 `/_realtime/messages/outbound` 是反向调用入口，不应暴露给公网或前端。

## URI 规则

realtime 不采用“单一路径 + type 参数”承载所有协议，而是按资源语义统一 URI：

- `/realtime/transports/**`：客户端连接、拉取和协商入口。
- `/realtime/messages/**`：正向消息入口，包括服务发布和客户端上行消息。
- `/realtime/receivers/**`：remote listener 的注册与注销入口。
- `/_realtime/messages/**`：realtime 节点之间的反向服务调用入口。

约束：

- 前端只访问 `/realtime/transports/**`，以及协议要求的 `/realtime/messages/inbound/**`。
- 业务服务正向调用只使用 `/realtime/messages/publish`。
- realtime 节点间回调只使用 `/_realtime/messages/inbound` 与 `/_realtime/messages/outbound`。
- 不再新增 `/realtime/subscribe`、`/realtime/poll`、`/realtime/ws`、`/realtime/negotiate` 这类分散旧式路径。

## 租户与认证约定

- `tenantId` 应优先通过 `TENANT-ID` header、登录态上下文或网关注入传递。
- Query 中的 `tenantId` 只作为 WebSocket 兼容入口，不作为长期推荐方案。
- WebSocket 浏览器原生 API 无法设置 header，生产接入应使用 cookie、短期 ticket 或网关注入方式。
- 不要把长期 access token 放进 URL。URL 会进入浏览器历史、代理日志和网关访问日志。

## 部署归属约定

同一系统群下，一个业务模块应只有一个逻辑服务归属。例如 `a-module` 部署在 `c-service`，就不应同时部署在 `d-service`。

允许的是同一逻辑服务多副本：

```text
c-service replica-1
c-service replica-2
c-service replica-3
```

禁止的是同一业务模块被多个不同逻辑服务同时装配：

```text
c-service: a-module, b-module
d-service: a-module, realtime endpoint
```

这种部署会造成模块归属漂移，同一个模块的 controller、定时任务、事件监听、数据库写入逻辑可能被重复装配。跨服务复用应通过 `*-api`、`*-starter-remote`、Feign/RPC 或事件机制完成，不能把同一个业务模块源码同时塞进多个逻辑服务。

对 realtime 来说：

| 服务类型 | 应依赖 | 结果 |
|----------|--------|------|
| 承载前端连接的服务 | `mango-infra-realtime-starter` | 暴露 SSE/WebSocket endpoint，维护本地连接 |
| 不承载连接但需要发消息的服务 | `mango-infra-realtime-starter-remote` | 提供远程 `RealtimeApi`，转发到连接承载服务 |
| 只声明编译期契约的业务模块 | `mango-infra-realtime-api` | 可编译，不提供运行时实现 |

多实例 realtime 节点的 server-to-client 投递路径：

```text
node-a RealtimeApi.publishToUser
  -> node-a 本地连接分发
  -> presence 路由表查找 userId 所在远端节点
  -> POST http://{serviceName}{contextPath}/_realtime/messages/outbound
  -> node-b publishLocal
  -> node-b 本地 WebSocket/SSE/Polling 分发
```

presence 只保存路由信息，不保存 `RealtimeSession`、WebSocket session、SSE emitter 等本地连接对象。默认内存实现仅适合单进程或测试；如果应用同时装配 `mango-infra-kv-starter`，且底层 `IKvStore` 实现了 `IKvSortedSet`，starter 会自动启用 KV presence。生产多实例推荐 Redis KV，底层使用 sorted set 维护 user/tenant/all 到 sessionId 的 TTL 索引。

依赖规则：

- 业务模块只能依赖 `mango-infra-realtime-api`。
- 连接承载服务使用 `mango-infra-realtime-starter`，由 starter 间接装配 core。
- 非连接承载服务使用 `mango-infra-realtime-starter-remote`，该模块不得依赖 `mango-infra-realtime-core`。
- `starter-remote` 里的远程发布必须是 Feign/RPC adapter，不允许再包装为调用自身的本地实现。

## 与 `mango-biz-notification` 的关系

| 模块 | 负责什么 | 不负责什么 |
|------|----------|------------|
| `mango-infra-realtime` | 在线连接、协议适配、实时投递、远程发布 | 业务通知、消息中心、已读未读、离线持久化 |
| `mango-biz-notification` | 业务通知、消息记录、状态流转、业务 API | SSE/WebSocket 连接管理、协议细节 |

推荐调用链：

```text
a-module / b-module
  -> mango-biz-notification 记录业务消息和状态
  -> mango-infra-realtime-api 实时投递在线用户
  -> mango-infra-realtime-starter 暴露 SSE/WebSocket/Polling 入口
  -> 前端接收消息并刷新 UI
```

只需要临时在线提示、不需要入库的场景，可以直接使用 `RealtimeApi`。只要涉及消息中心、离线可见、已读未读、审计追踪，就应经过 `mango-biz-notification`。

## 运维注意事项

- 网关和反向代理必须支持 WebSocket HTTP Upgrade。
- SSE 需要关闭响应缓冲，或配置为流式转发。
- 网关、LB、容器和浏览器端超时时间要相互匹配，避免频繁断线重连。
- 前端重连必须使用退避策略，不要固定毫秒级高频重连。
- 生产环境应收敛 `mango.infra.realtime.websocket.allowed-origins`，不要长期使用默认 `*`。
- 当前会话和 Polling 队列为内存实现；跨实例只转发在线出站消息，不共享真实连接对象。
- 生产多实例必须提供共享 presence 实现，并保证 `node.instance-id` 在同一服务多副本间唯一。
- 当前 `RealtimeApi` 会尽量向已注册协议发送；单个协议发送失败不应阻断其它协议。

## 当前不支持

- 不支持离线消息持久化。
- 不支持跨节点共享真实连接对象。
- 不支持可靠消息确认、ack、重试、死信。
- 不支持消息中心列表、已读未读、未读数。
- 不支持 MQ/Event 场景。
- 不支持把业务通知渠道逻辑放进 infra realtime。

## 后续规划

### 远程入站注册表

当前 `inbound.mode=remote` 或 `local_remote` 使用 realtime 本地内存注册表维护 remote starter 上报的订阅服务：

```text
starter-remote
  -> POST /realtime/receivers/register
  -> starter 内存注册表
  -> WebSocket 入站消息转发到 /_realtime/messages/inbound
```

该实现适合单实例或研发阶段验证。生产多实例如果要稳定支撑 remote starter 入站监听服务发现，需要明确注册表升级路径：

- 基于服务发现或注册中心保存订阅服务。
- 基于 DB / Redis / KV 保存订阅服务和心跳。
- 基于 `mango-infra-module` 的 tag/property 能力发现订阅服务。

在进入该阶段前，不应把当前内存注册表误认为可靠服务目录。

### 多实例在线 presence

当前 server-to-client 多实例投递已经抽象为 `IRealtimePresenceService`：

- `subscribe` 时登记 session 到节点路由。
- `unsubscribe` 时删除 session 路由。
- `RealtimeApi.publish*` 先投递本机，再根据 presence 路由转发到远端节点的 `/_realtime/messages/outbound`。

默认 `InMemoryRealtimePresenceService` 用于单实例和测试。生产多实例建议按环境选择 Redis、DB、注册中心或 KV 实现，并补充 TTL、心跳续约、节点下线清理和服务发现解析。不要把 `RealtimeSession` 或协议连接对象写入共享存储。

### Redis Presence 生产验证

如果应用同时装配 `mango-infra-kv-starter`，并选择 Redis 作为底层 `IKvStore`，且该实现支持 `IKvSortedSet`，starter 会自动启用 `KvRealtimePresenceService`。推荐生产配置：

```yaml
mango:
  kv:
    store:
      type: redis
  infra:
    realtime:
      node:
        instance-id: ${HOSTNAME}
      presence:
        prefix: mango:infra:realtime:presence
        ttl-seconds: 120
```

Redis presence 当前验证的关键点：

- session 路由详情写入 KV，user / tenant / all 索引写入 sorted set。
- `publish*` 先投递本机，再只向远端节点转发，不重复回送本机。
- TTL 到期前会由当前节点定时续约；节点异常下线后，过期路由会依赖 TTL 和 score 清理逐步剔除。
- presence 只保存路由元数据，不保存 WebSocket session、SSE emitter、Polling 队列或其它本地连接对象。

建议至少保留一组真实 Redis 集成测试，覆盖：

- online / find / offline 的完整生命周期；
- TTL 续约在长连接存活期间持续生效；
- 双节点 publish 时只向远端实例转发。

### 可靠入站投递

当前 WebSocket 客户端入站分发是尽力转发：

- 不保证消息必达。
- 不提供 ack。
- 不做失败重试。
- 不做死信。
- 不保证跨服务顺序。

后续如果业务需要“客户端上行事件可靠处理”，应单独设计：

- 客户端消息幂等 ID。
- 服务端消费幂等。
- ack / nack 协议。
- 重试策略和最大重试次数。
- 死信或失败记录。
- 限流和 payload 校验策略。

可靠入站不应混入 `RealtimeApi.publishToUser` 这类 server-to-client 投递接口。

### 安全与网关接入

当前 realtime 只做协议入口必要的轻量上下文读取和消息转发。认证、鉴权、租户解析、可信 header 注入、外部伪造 header 清洗、内部接口保护、限流、CORS/Origin、WebSocket Upgrade 和长连接超时策略，应由 gateway、security、web 和 feign 体系统一承担。

后续需要冻结以下接入约定：

- `tenantId`、`userId`、认证状态、trace/request id 的统一读取方式。
- SSE 原生 `EventSource` 无法设置 header 时的生产接入方案。
- WebSocket 浏览器原生 API 无法设置 header 时的 cookie / 短期 ticket / 网关注入方案。
- `/realtime/*` 与 `/_realtime/*` 服务间接口的访问控制和签名。
- 网关、LB、容器、浏览器之间的长连接超时配置基线。

### 可选 module 集成

当前方案先不依赖 `mango-infra-module`。如果后续确认需要复用 module 的服务归属能力，应保持边界简单：

- realtime 只读取“哪些服务订阅了 realtime 入站消息”这类最小信息。
- module 可以提供 tag/property 能力，例如 `SUB_REALTIME=true`。
- starter-remote 可在启动时向 module 注册 tag/property。
- starter 可通过 `ModuleApi.getServiceByTag(...)` 或等价查询发现订阅服务。

不应为了 realtime 反向扩张 `mango-infra-module` 的核心模型，也不应让 module 承担消息投递、ack、重试等 realtime 职责。

## 验收命令

编译 realtime 与 infra-test：

```bash
mvn -q -DskipTests -pl mango-infra/mango-infra-realtime,mango-infra/mango-infra-test -am compile
```

执行 realtime 相关单元测试：

```bash
mvn -q -pl mango-infra/mango-infra-test -am -Dtest='io.mango.infra.realtime.**.*Test' -Dsurefire.failIfNoSpecifiedTests=false test
```

执行静态检查：

```bash
mvn -q -DskipTests -pl mango-infra/mango-infra-realtime,mango-infra/mango-infra-test -am checkstyle:check pmd:check
```
