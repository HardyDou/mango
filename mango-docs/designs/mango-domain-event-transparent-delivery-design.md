# Mango 领域事件跨服务透明投递设计

## 1. 背景

Issue #137 要解决的是领域事件在微服务拆分后不能透明投递的问题。业务模块希望继续只依赖 `IDomainEventPublisher` 和 `DomainEventSubscriber`，不在业务代码里感知 Redis、MQ、Feign 或具体目标服务。

现有进程内事件总线只能覆盖单进程同步分发。应用重启、消费者异常、跨服务订阅都会丢失可靠性。最终方案不废弃进程内总线，但将它定位为本地 subscriber dispatcher；可靠发布、重试、失败终态和补偿统一放到 Outbox 模型。

## 2. 目标特性

- 业务发布方透明：仍调用 `IDomainEventPublisher.publish(event)`。
- 业务消费方透明：仍实现 `DomainEventSubscriber`。
- 单应用可靠回放：事件写入 KV Outbox 后再由 dispatcher 投递，失败可重试。
- 跨微服务透明传输：可选 Redis Stream transport，将 Outbox 事件投递到跨进程事件流。
- 运维可见和可补偿：管理后台“系统维护 / 系统事件”展示异常事件并支持重新投递。
- 不支持 Redis Pub/Sub：Pub/Sub 无离线保留、consumer group ack 和可靠回放，不作为本方案传输层。

## 3. 总体架构

```text
业务模块
  |
  | IDomainEventPublisher
  v
KV Outbox
  |
  | IOutboxDispatcher
  +--> transport=none         -> InMemoryDomainEventBus -> DomainEventSubscriber
  |
  +--> transport=redis-stream -> Redis Stream           -> consumer group
                                                         -> InMemoryDomainEventBus
                                                         -> DomainEventSubscriber
```

`InMemoryDomainEventBus` 只负责当前 JVM 内订阅者调用。handler 异常必须向上抛出，Outbox dispatcher 才能把消息标记为重试或失败终态。

## 4. 模块升级

### 4.1 `mango-infra-kv`

`IOutboxStore` 增加兼容扩展方法：

- `fail(messageId, workerId, errorMessage, failedAt)`
- `requeue(messageId, nextAttemptAt, updatedAt)`
- `findById(messageId)`
- `query(OutboxMessageQuery)`
- `count(OutboxMessageQuery)`

`KvOutboxStore` 增加全量索引、失败终态、重新入队、详情和分页查询能力。`ALL` 索引用于新写入和新变更消息，历史旧消息不会自动补全索引。

### 4.2 `mango-infra-event`

- `OutboxDomainEventDispatcher`：本地可靠投递到 `IDomainEventBus`。
- `TransportDomainEventDispatcher`：可靠投递到跨服务 transport。
- `RedisStreamDomainEventTransport`：基于 Redisson `RStream` 的 Redis Stream 实现。
- `DomainEventTransportScheduler`：周期拉取消费 Redis Stream。
- `SystemEventApi / SystemEventController / SystemEventService`：系统事件运维接口。

### 4.3 `mango-platform / mango-ui`

- Flyway migration 新增“系统维护 / 系统事件”菜单和权限。
- `@mango/system` 新增系统事件页面，默认筛选异常事件，支持详情和重新投递。
- `@mango/admin-pages` 注册 `system/event/index` 页面 loader。

## 5. 配置

单应用可靠 Outbox：

```yaml
mango:
  kv:
    capability:
      enabled: true
      outbox: true
  event:
    outbox:
      enabled: true
      worker-id: domain-event-dispatcher
      batch-size: 50
      retry-delay-seconds: 60
      max-attempts: 5
      dispatch-enabled: true
    transport: none
```

跨服务 Redis Stream：

```yaml
mango:
  event:
    outbox:
      enabled: true
    transport: redis-stream
    redis-stream:
      stream-name: mango:domain-event
      group: mango-domain-event
      consumer: ${spring.application.name:${HOSTNAME:domain-event-consumer}}
      batch-size: 50
      read-timeout-millis: 200
      consume-enabled: true
```

## 6. 可靠性语义

- 发布可靠性：开启 Outbox 后，发布方写入 KV Outbox，不直接同步调用业务订阅者。
- 投递重试：dispatcher 领取消息后投递；失败后 `nack` 并按 `retry-delay-seconds` 延迟重试。
- 失败终态：领取投递次数达到 `max-attempts` 后进入 `FAILED`，不再自动重试。
- 人工补偿：系统事件页面或 API 可将异常事件重新放回 `PENDING`。
- 消费幂等：业务消费者必须按 `eventId`、`businessType`、`businessKey` 或业务幂等键自行保证幂等。

## 7. 运维入口

菜单：`系统管理 / 系统维护 / 系统事件`

权限：

- `system:event:list`
- `system:event:detail`
- `system:event:reconsume`

接口：

- `GET /system/events`
- `GET /system/events/detail?messageId=...`
- `POST /system/events/reconsume`

页面只展示真实 Outbox 数据，不提供静态示例数据。默认查询异常事件，运维人员可切换到全部事件。

## 8. 取舍

- 不做默认 HTTP/Feign bridge：广播所有服务需要服务发现、实例过滤、重试、幂等和安全边界，复杂度高于轻量方案目标。
- 不支持 Redis Pub/Sub：无法满足可靠事件的保留、确认和回放。
- 不把 memory 作为可靠传输：memory 只适合进程内分发，进程重启不会保留未消费事件。
- MQ 作为后续扩展：`DomainEventTransport` 已提供扩展点，Kafka/RabbitMQ/RocketMQ 可以按同一 Outbox relay 模型接入。
