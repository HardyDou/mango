# Mango Infra Realtime

`mango-infra-realtime` 是面向“服务端向在线客户端实时投递消息”的基础设施模块。它统一承载 SSE、WebSocket、HTTP Polling、连接会话、订阅管理、客户端 transport 协商和跨服务远程发布。

这个模块只解决在线实时投递问题，不负责业务通知、消息中心、已读未读、离线消息、模板、审计持久化，也不承担 MQ/Event 语义。业务通知能力属于 `mango-biz-notification`；未来 MQ/Event 能力应保留给 `mango-infra-messaging`。

## 核心特性

| 特性 | 当前能力 |
|------|----------|
| SSE 单向推送 | 提供 `GET /realtime/subscribe`，服务端向浏览器持续推送消息 |
| WebSocket 双向连接 | 提供 `/realtime/ws`，支持连接建立通知、`ping`/`pong`、客户端业务消息入站分发 |
| HTTP Polling | 提供 `GET /realtime/poll`，支持短轮询和带 hold timeout 的长轮询 |
| Transport 协商 | 提供 `GET /realtime/negotiate`，返回服务端可用 transport 和推荐协议 |
| 用户定向投递 | `RealtimeApi.publishToUser(userId, type, content)` |
| 租户内投递 | `RealtimeApi.publishToTenant(tenantId, type, content)` |
| 全局广播 | `RealtimeApi.broadcast(type, content)` |
| 本地 Polling 队列 | `RealtimePollingService` Java API，支撑 HTTP Polling 的临时在线队列 |
| 远程发布 | `mango-infra-realtime-starter-remote` 通过内部 Feign 调用连接承载服务 |
| 客户端入站 | WebSocket 业务消息经 `@RealtimeListener` 或 `RealtimeSubscriber` 分发给业务模块 |
| 条件化开关 | 支持总开关、SSE、WebSocket、Polling、协商入口、内部远程发布入口独立开关 |

当前实现使用内存会话和内存 Polling 队列。单实例内可用；多实例跨节点会话共享、离线队列和持久化投递不是本模块当前能力。

## 模块结构

| Maven 模块 | 职责 |
|------------|------|
| `mango-infra-realtime-api` | 对业务暴露 `RealtimeApi`、`RealtimeMessage`、`RealtimeListener`、`RealtimeSubscriber`、`RealtimeInboundMessage`、`RealtimeSubscriberApi` 等契约 |
| `mango-infra-realtime-core` | 提供内存订阅管理、内存 Polling 队列、SSE adapter、WebSocket adapter、协议分发、入站监听分发和订阅服务注册表 |
| `mango-infra-realtime-starter` | 自动配置本地 realtime 能力，暴露客户端协议入口、内部发布入口和订阅服务注册入口 |
| `mango-infra-realtime-starter-remote` | 提供远程 `RealtimeApi` 实现、入站监听自动注册和内部入站接收 endpoint |

业务模块只能依赖 `mango-infra-realtime-api`。不要依赖 `core`，也不要直接装配协议实现类。

## 能力边界

`mango-infra-realtime` 负责：

- 建立和维护在线连接。
- 按用户、租户、广播范围投递实时消息。
- 适配 SSE、WebSocket 和 HTTP Polling 协议。
- 为前端提供 transport 协商入口。
- 为不承载连接的服务提供远程发布实现。
- 接收客户端 WebSocket 入站业务消息，并分发给本地或远程服务内的业务监听器。
- 提供本地 Polling Java API 作为 HTTP Polling 的临时在线队列。

`mango-infra-realtime` 不负责：

- 业务消息表、消息模板、通知渠道、站内信列表。
- 已读未读、未读数、消息撤回、消息归档。
- 离线消息持久化、审计级持久化、跨节点可靠投递。
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

业务代码注入 `RealtimeApi`。`RealtimePublisher` 暂时保留为兼容别名，新代码优先使用 `RealtimeApi`：

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

需要自定义 envelope 时直接构造 `RealtimeMessage`：

```java
RealtimeMessage message = new RealtimeMessage(
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

- `GET /realtime/subscribe`
- `/realtime/ws`
- `GET /realtime/poll`
- `GET /realtime/negotiate`
- `POST /internal/realtime/publish`

不承载前端连接、但部署了会发送消息的业务模块的服务依赖 remote starter：

```xml
<dependency>
    <groupId>io.mango</groupId>
    <artifactId>mango-infra-realtime-starter-remote</artifactId>
</dependency>
```

remote starter 不暴露 `/realtime/subscribe` 或 `/realtime/ws`，只提供远程 `RealtimeApi`，把消息转发到真正承载连接的服务。

典型部署：

```text
c-service: a-module, b-module, mango-infra-realtime-starter-remote
d-service: mango-infra-realtime-starter
frontend: 连接 d-service 的 /realtime/negotiate、/realtime/subscribe、/realtime/ws 或 /realtime/poll
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

WebSocket 支持客户端向服务端发送业务消息。realtime 模块只负责把入站消息路由给业务监听器，不定义具体业务语义。

方法级监听器：

```java
import io.mango.infra.realtime.api.RealtimeInboundMessage;
import io.mango.infra.realtime.api.RealtimeListener;
import org.springframework.stereotype.Component;

@Component
public class TaskRealtimeListener {

    @RealtimeListener(types = "task.cancel")
    public void onTaskCancel(RealtimeInboundMessage message) {
        // message.content() 通常是业务 JSON 字符串，由业务模块自行解析和校验。
    }
}
```

类级订阅者：

```java
import io.mango.infra.realtime.api.RealtimeInboundMessage;
import io.mango.infra.realtime.api.RealtimeListener;
import io.mango.infra.realtime.api.RealtimeSubscriber;
import org.springframework.stereotype.Component;

@Component
@RealtimeListener(types = {"task.cancel", "task.pause"})
public class TaskRealtimeSubscriber implements RealtimeSubscriber {

    @Override
    public void onMessage(RealtimeInboundMessage message) {
        // 按 message.type() 进入业务分支。
    }
}
```

`@RealtimeListener` 可用于方法或类。方法级监听只允许一个参数，类型必须是 `RealtimeInboundMessage`；类级监听必须实现 `RealtimeSubscriber`。同一个 bean 同时声明类级和方法级监听时，方法级优先，避免重复消费。

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

- `d-service` 依赖 `mango-infra-realtime-starter`，配置 `inbound.mode=remote`。
- `c-service` 依赖 `mango-infra-realtime-starter-remote`，配置 `inbound.enabled=true`。
- `c-service` 启动后自动扫描 `@RealtimeListener` / `RealtimeSubscriber`，通过 `POST /internal/realtime/subscribers/register` 注册到 `d-service`。
- `d-service` 收到 WebSocket 入站消息后，向已注册服务的固定 `POST /internal/realtime/inbound` 转发。
- `c-service` 收到内部入站消息后，再本地分发给具体业务监听器。

这个方案暂不依赖 `mango-infra-module`。注册信息由 realtime 自己维护为内存注册表，服务重启后由 remote starter 重新注册；它不是可靠事件总线，也不提供 ack、重试、死信或离线补偿。

## 前端如何使用

### Transport 协商

前端可以先请求协商接口，让服务端返回当前可用 transport，再按推荐结果连接。协商不是鉴权替代品，只用于能力发现和降级选择。

默认入口：

```text
GET /realtime/negotiate
GET /realtime/negotiate?prefer=websocket,sse,polling
```

服务端当前要求 `Authorization: Bearer ...` header；租户上下文应继续由登录态或网关注入管理，协商结果本身不携带业务数据。

响应示例：

```json
{
  "recommended": "websocket",
  "transports": [
    {
      "type": "websocket",
      "enabled": true,
      "endpoint": "/realtime/ws",
      "bidirectional": true,
      "longPolling": false
    },
    {
      "type": "sse",
      "enabled": true,
      "endpoint": "/realtime/subscribe",
      "bidirectional": false,
      "longPolling": false
    },
    {
      "type": "polling",
      "enabled": true,
      "endpoint": "/realtime/poll",
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

### SSE

SSE 适合服务端到浏览器的单向推送，例如通知角标、任务进度、审批状态刷新。

默认入口：

```text
GET /realtime/subscribe?userId=1001
```

服务端当前会读取：

| 来源 | 字段 | 说明 |
|------|------|------|
| Header | `Authorization` | 必须是 `Bearer ...` 形式 |
| Header | `TENANT-ID` | 推荐租户传递方式；缺省为 `default` |
| Query | `userId` | 可选；用于绑定当前连接所属用户 |

浏览器原生 `EventSource` 不能设置自定义 header，因此生产接入不要直接依赖 URL token。推荐由网关基于 cookie 或登录态补齐鉴权 header，或使用支持 header 的 SSE client。

示例：

```javascript
const events = new EventSource('/realtime/subscribe?userId=1001');

events.onmessage = (event) => {
  const message = JSON.parse(event.data);
  console.log(message.type, message.content);
};

events.onerror = () => {
  events.close();
  // 由前端统一重连策略处理，避免高频重试打满服务端。
};
```

### WebSocket

WebSocket 适合需要双向交互或更频繁实时通信的页面。

默认入口：

```text
/realtime/ws
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

示例：

```javascript
const ticket = encodeURIComponent(connectionTicket);
const socket = new WebSocket(`/realtime/ws?token=${ticket}&userId=1001`);

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
};

socket.onclose = () => {
  // 由前端统一退避重连策略处理。
};
```

当前 WebSocket 入站消息支持：

| 入站 `type` | 行为 |
|-------------|------|
| `ping` | 服务端返回 `type=pong` |
| 业务类型，例如 `task.cancel` | 服务端封装为 `RealtimeInboundMessage`，按 `@RealtimeListener(types = "...")` 分发 |
| 非法 JSON | 服务端返回 `type=error`、`content=Invalid message format` |
| 超过 `mango.infra.realtime.inbound.max-payload-bytes` | 服务端返回 `type=error`、`content=Realtime inbound message too large` |

业务入站消息格式：

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

`tenantId`、`userId`、`sessionId` 由服务端根据连接上下文补齐，前端不应伪造这些字段。

### Polling

Polling 适合浏览器或网络环境无法稳定保持 WebSocket/SSE 的场景。它不是消息中心，也不保证离线可见；只表示前端用 HTTP 循环请求在线临时消息。

默认入口：

```text
GET /realtime/poll?userId=1001
GET /realtime/poll?userId=1001&maxSize=20&timeoutMillis=25000
```

服务端当前会读取：

| 来源 | 字段 | 说明 |
|------|------|------|
| Header | `Authorization` | 必须是 `Bearer ...` 形式 |
| Header | `TENANT-ID` | 推荐租户传递方式；缺省为 `default` |
| Query | `userId` | 必填；用于绑定当前 polling 客户端所属用户 |
| Query | `maxSize` | 可选；本次最多返回多少条 |
| Query | `timeoutMillis` | 可选；`0` 表示短轮询，大于 `0` 表示长轮询 hold 时间 |

短轮询示例：

```javascript
async function pollOnce() {
  const response = await fetch('/realtime/poll?userId=1001&maxSize=20', {
    headers: { Authorization: 'Bearer <access-token>' }
  });
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
      const response = await fetch('/realtime/poll?userId=1001&maxSize=20&timeoutMillis=25000', {
        headers: { Authorization: 'Bearer <access-token>' }
      });
      const messages = await response.json();
      messages.forEach(handleRealtimeMessage);
    } catch (error) {
      await new Promise((resolve) => setTimeout(resolve, 3000));
    }
  }
}

longPoll();
```

服务端 Java API：

```java
realtimePollingService.append("user:1001", RealtimeMessage.toUser(1001L, "task.done", "任务完成"));
List<RealtimeMessage> messages = realtimePollingService.poll("user:1001", 20);
```

## 消息格式

实时消息统一使用 `RealtimeMessage`：

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
      inbound:
        enabled: false
        mode: none
        max-payload-bytes: 65536
        fail-fast: false
        unknown-type-policy: ignore
        remote:
          endpoint-enabled: true
          register-enabled: true
          endpoint: /internal/realtime/inbound
          service-name:
          context-path: /
      sse:
        enabled: true
        endpoint: /realtime/subscribe
        timeout-millis: 300000
      websocket:
        enabled: true
        endpoint: /realtime/ws
        allowed-origins:
          - "*"
      polling:
        enabled: true
        endpoint: /realtime/poll
        default-max-size: 20
        max-size: 100
        default-timeout-millis: 0
        max-timeout-millis: 25000
      negotiate:
        enabled: true
        endpoint: /realtime/negotiate
```

配置说明：

| 配置 | 默认值 | 说明 |
|------|--------|------|
| `mango.infra.realtime.enabled` | `true` | 总开关；关闭后不自动配置 realtime |
| `mango.infra.realtime.mode` | `auto` | 运行模式：`auto`、`sse`、`websocket`、`polling` |
| `mango.infra.realtime.remote.endpoint-enabled` | `true` | 是否启用内部 `POST /internal/realtime/publish` |
| `mango.infra.realtime.inbound.enabled` | `false` | 是否启用客户端到服务端的业务入站消息分发 |
| `mango.infra.realtime.inbound.mode` | `none` | 本地 starter 入站分发模式：`none`、`local`、`remote` |
| `mango.infra.realtime.inbound.max-payload-bytes` | `65536` | 单条 WebSocket 入站文本消息最大字节数 |
| `mango.infra.realtime.inbound.fail-fast` | `false` | 单个 listener 失败时是否停止后续 listener |
| `mango.infra.realtime.inbound.unknown-type-policy` | `ignore` | 未找到 listener 时的处理策略：`ignore`、`warn`、`error` |
| `mango.infra.realtime.inbound.remote.endpoint-enabled` | `true` | remote starter 是否暴露 `POST /internal/realtime/inbound` |
| `mango.infra.realtime.inbound.remote.register-enabled` | `true` | remote starter 是否在启动时自动注册自己 |
| `mango.infra.realtime.inbound.remote.endpoint` | `/internal/realtime/inbound` | remote starter 接收入站消息的内部入口 |
| `mango.infra.realtime.inbound.remote.service-name` | `spring.application.name` | 自动注册时上报的服务名；为空时读取 `spring.application.name` |
| `mango.infra.realtime.inbound.remote.context-path` | `server.servlet.context-path` 或 `/` | 自动注册时上报的服务 context path |
| `mango.infra.realtime.sse.enabled` | `true` | `mode=auto` 时是否启用 SSE adapter |
| `mango.infra.realtime.sse.endpoint` | `/realtime/subscribe` | SSE 订阅入口 |
| `mango.infra.realtime.sse.timeout-millis` | `300000` | SSE 连接超时时间，单位毫秒 |
| `mango.infra.realtime.websocket.enabled` | `true` | `mode=auto` 时是否启用 WebSocket handler |
| `mango.infra.realtime.websocket.endpoint` | `/realtime/ws` | WebSocket 连接入口 |
| `mango.infra.realtime.websocket.allowed-origins` | `["*"]` | WebSocket 允许来源；生产环境应收敛为明确域名 |
| `mango.infra.realtime.polling.enabled` | `true` | `mode=auto` 时是否启用 HTTP Polling adapter 和 endpoint |
| `mango.infra.realtime.polling.endpoint` | `/realtime/poll` | HTTP Polling 入口 |
| `mango.infra.realtime.polling.default-max-size` | `20` | caller 传入 `maxSize <= 0` 时的默认返回条数 |
| `mango.infra.realtime.polling.max-size` | `100` | 单次 polling 最大返回条数上限 |
| `mango.infra.realtime.polling.default-timeout-millis` | `0` | caller 不传 `timeoutMillis` 时的默认 hold 时间；`0` 表示短轮询 |
| `mango.infra.realtime.polling.max-timeout-millis` | `25000` | 长轮询最大 hold 时间，单位毫秒 |
| `mango.infra.realtime.negotiate.enabled` | `true` | 是否启用 `GET /realtime/negotiate` 客户端协商入口 |
| `mango.infra.realtime.negotiate.endpoint` | `/realtime/negotiate` | Transport 协商入口 |

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
        endpoint: /realtime/subscribe
        timeout-millis: 300000
      websocket:
        enabled: true
        endpoint: /realtime/ws
        allowed-origins:
          - "https://app.example.com"
      polling:
        enabled: true
        endpoint: /realtime/poll
        default-max-size: 20
        max-size: 100
        default-timeout-millis: 0
        max-timeout-millis: 25000
      negotiate:
        enabled: true
        endpoint: /realtime/negotiate
```

## 协议入口

| 协议/能力 | 默认入口 | 暴露方 | 默认状态 |
|-----------|----------|--------|----------|
| SSE | `GET /realtime/subscribe` | `mango-infra-realtime-starter` | 启用 |
| WebSocket | `/realtime/ws` | `mango-infra-realtime-starter` | 启用 |
| Polling | `GET /realtime/poll` | `mango-infra-realtime-starter` | 启用 |
| Transport 协商 | `GET /realtime/negotiate` | `mango-infra-realtime-starter` | 启用 |
| 远程发布 | `POST /internal/realtime/publish` | `mango-infra-realtime-starter` | 启用 |
| 入站订阅注册 | `POST /internal/realtime/subscribers/register` | `mango-infra-realtime-starter` | 启用 |
| 入站订阅注销 | `POST /internal/realtime/subscribers/unregister` | `mango-infra-realtime-starter` | 启用 |
| 远程入站接收 | `POST /internal/realtime/inbound` | `mango-infra-realtime-starter-remote` | `inbound.enabled=true` 时启用 |

`/internal/realtime/publish` 是内部远程发布入口，应只允许服务间调用，不应直接暴露给公网或前端。
`/internal/realtime/subscribers/*` 和 `/internal/realtime/inbound` 同样是内部调用入口，不应暴露给公网或前端。

## 租户与认证约定

- `tenantId` 应优先通过 `TENANT-ID` header、登录态上下文或网关注入传递。
- Query 中的 `tenantId` 只作为 WebSocket 兼容入口，不作为长期推荐方案。
- SSE 当前要求 `Authorization: Bearer ...`；原生 `EventSource` 无法设置 header，生产接入需要网关或专用 client 配合。
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
- 当前会话和 Polling 队列为内存实现；多实例下不保证跨实例投递。
- 当前 `RealtimeApi` 会尽量向已注册协议发送；单个协议发送失败不应阻断其它协议。

## 当前不支持

- 不支持离线消息持久化。
- 不支持跨节点会话共享。
- 不支持可靠消息确认、ack、重试、死信。
- 不支持消息中心列表、已读未读、未读数。
- 不支持 MQ/Event 场景。
- 不支持把业务通知渠道逻辑放进 infra realtime。

## 后续规划

### 远程入站注册表

当前 `inbound.mode=remote` 使用 realtime 本地内存注册表维护 remote starter 上报的订阅服务：

```text
starter-remote
  -> POST /internal/realtime/subscribers/register
  -> starter 内存注册表
  -> WebSocket 入站消息转发到 /internal/realtime/inbound
```

该实现适合单实例或研发阶段验证。后续如果要支撑多实例 realtime 服务，需要明确注册表升级路径：

- 基于服务发现或注册中心保存订阅服务。
- 基于 DB / Redis / KV 保存订阅服务和心跳。
- 基于 `mango-infra-module` 的 tag/property 能力发现订阅服务。

在进入该阶段前，不应把当前内存注册表误认为可靠服务目录。

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

当前 realtime 只做协议入口必要的轻量 header/query 读取。认证、鉴权、租户解析、可信 header 注入、外部伪造 header 清洗、内部接口保护、限流、CORS/Origin、WebSocket Upgrade 和长连接超时策略，应由 gateway、security、web 和 feign 体系统一承担。

后续需要冻结以下接入约定：

- `tenantId`、`userId`、认证状态、trace/request id 的统一读取方式。
- SSE 原生 `EventSource` 无法设置 header 时的生产接入方案。
- WebSocket 浏览器原生 API 无法设置 header 时的 cookie / 短期 ticket / 网关注入方案。
- `/internal/realtime/*` 内部接口的访问控制和服务间签名。
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
