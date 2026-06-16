# Mango Infra Event

## 1. 概览
`mango-infra-event` 提供 Mango 领域事件基础设施，覆盖事件契约、进程内发布订阅、KV Outbox 可靠投递、Redis Stream 跨进程传输和系统事件运维接口。

## 2. 功能清单

| 能力 | 常用入口 |
|------|----------|
| 业务模块需要在领域动作完成后发布事件，降低模块间同步调用耦合 | Maven 依赖 / starter / Java API |
| 订阅者失败后需要重试，不希望事件直接丢失 | Maven 依赖 / starter / Java API |
| 微服务部署下需要用 Redis Stream 做轻量跨进程传输 | Maven 依赖 / starter / Java API |
| 运维人员需要查询失败事件并手工触发重新投递 | Maven 依赖 / starter / Java API |

## 3. 适用场景
- 业务模块需要在领域动作完成后发布事件，降低模块间同步调用耦合。
- 订阅者失败后需要重试，不希望事件直接丢失。
- 微服务部署下需要用 Redis Stream 做轻量跨进程传输。
- 运维人员需要查询失败事件并手工触发重新投递。

## 4. 边界说明
- 不替代 Kafka、RocketMQ 等完整消息平台。
- 不负责业务事务边界，发布事件前后的事务一致性由业务模块设计。
- 不负责订阅者幂等，重复投递时业务 handler 必须自己保证幂等。
- 不提供独立事件业务表；可靠投递依赖 `mango-infra-kv` outbox 能力。

## 5. 模块组成
- `mango-infra-event-api`：`DomainEvent`、发布器、总线、订阅者和系统事件 API 契约。
- `mango-infra-event-core`：内存事件总线、Outbox dispatcher、Redis Stream transport、系统事件查询服务。
- `mango-infra-event-starter`：自动装配 publisher、dispatcher、scheduler、transport 和 `/system/events` Controller。

业务模块负责定义事件类型、payload 字段、订阅者逻辑、失败补偿和幂等键。

## 6. 接入方式
只使用事件契约：

```xml
<dependency>
    <groupId>io.mango.infra.event</groupId>
    <artifactId>mango-infra-event-api</artifactId>
</dependency>
```

Spring Boot 应用启用事件运行时：

```xml
<dependency>
    <groupId>io.mango.infra.event</groupId>
    <artifactId>mango-infra-event-starter</artifactId>
</dependency>
```

发布事件：

```java
publisher.publish(DomainEvent.builder()
        .eventType("payment.succeeded")
        .businessType("payment")
        .businessKey(paymentId)
        .aggregateId(orderId)
        .payload("amount", amount)
        .header("tenantId", MangoContextHolder.tenantId())
        .build());
```

订阅事件：

```java
@Component
public class PaymentSucceededSubscriber implements DomainEventSubscriber {
    @Override
    public String eventType() {
        return "payment.succeeded";
    }

    @Override
    public void onEvent(DomainEvent event) {
        // handler must be idempotent
    }
}
```

## 7. 配置说明
配置前缀：`mango.event`。自动配置类为 `DomainEventAutoConfiguration`。

| 配置 | 默认值 | 含义 |
|------|--------|------|
| `type` | `memory` | 事件总线类型；当前实现为内存总线。 |
| `transport` | `none` | 跨进程传输方式；可配置 `none` 或 `redis-stream`。 |
| `outbox.enabled` | `false` | 是否让 `IDomainEventPublisher` 写入 KV Outbox。关闭时直接使用内存总线发布。 |
| `outbox.worker-id` | `domain-event-dispatcher` | Outbox 消息 claim worker id。 |
| `outbox.batch-size` | `50` | 每次 claim 和投递的消息数。 |
| `outbox.retry-delay-seconds` | `60` | 投递失败后的重试延迟秒数。 |
| `outbox.max-attempts` | `5` | 达到后消息进入最终失败状态。 |
| `outbox.dispatch-enabled` | `true` | 是否在当前进程启动 Outbox dispatch scheduler。 |
| `outbox.dispatch-interval-millis` | `1000` | Outbox dispatcher 固定执行间隔。 |
| `outbox.dispatch-initial-delay-millis` | `1000` | Outbox dispatcher 首次执行延迟。 |
| `redis-stream.stream-name` | `mango:domain-event` | Redis Stream key。 |
| `redis-stream.group` | `mango-domain-event` | Redis consumer group。 |
| `redis-stream.consumer` | `domain-event-consumer` | Redis consumer 名称。 |
| `redis-stream.batch-size` | `50` | 每次消费的 Stream 消息数。 |
| `redis-stream.read-timeout-millis` | `200` | Redis Stream 读取超时毫秒。 |
| `redis-stream.pending-idle-timeout-millis` | `60000` | pending 消息可被重新 claim 的 idle 时间。 |
| `redis-stream.consume-enabled` | `true` | 是否在当前进程启动 Redis Stream consumer scheduler。 |
| `redis-stream.consume-interval-millis` | `1000` | Stream consumer 固定执行间隔。 |
| `redis-stream.consume-initial-delay-millis` | `1000` | Stream consumer 首次执行延迟。 |

可靠投递示例：

```yaml
mango:
  event:
    outbox:
      enabled: true
      batch-size: 100
      max-attempts: 10
```

Redis Stream 传输示例：

```yaml
mango:
  event:
    transport: redis-stream
    outbox:
      enabled: true
    redis-stream:
      stream-name: mango:domain-event
      group: mango-domain-event
      consumer: ${spring.application.name}
```

## 8. API 与扩展
- `DomainEvent`：通用事件对象，字段包括 event id、event type、business type、business key、aggregate id、occurred at、payload、headers。
- `IDomainEventPublisher`：事件发布入口。
- `IDomainEventBus`：进程内事件总线。
- `DomainEventSubscriber`：订阅者接口，`eventType()` 返回 `*` 可订阅所有事件。
- `DomainEventHandler`：事件处理扩展契约。
- `DomainEventTransport`：跨进程传输扩展契约。
- `SystemEventApi`：系统事件运维 API 契约。
- `SystemEventController`：路径 `/system/events`，仅在 `outbox.enabled` 为 true 时注册。

系统事件接口：

| 方法 | 路径 | 权限码 | 用途 |
|------|------|--------|------|
| `GET` | `/system/events` | `system:event:list` | 分页查询失败、重试中或处理中事件。 |
| `GET` | `/system/events/detail` | `system:event:detail` | 按 message id 查询投递详情和错误信息。 |
| `POST` | `/system/events/reconsume` | `system:event:reconsume` | 把失败或等待重试事件重新放回待投递队列。 |

## 9. 数据与初始化
本模块没有独立 SQL migration、Runner 或 Initializer。可靠投递写入 `mango-infra-kv` 提供的 Outbox store；接入 outbox 前必须确认宿主应用已接入 KV/Outbox 所需存储。

系统事件运维入口依赖 authorization 模块的历史 Flyway migration `V47__system_event_menu.sql` 完成菜单和权限初始化。也就是说：event 模块提供 Controller 和权限标注，菜单、权限数据入库由 authorization migration 承担；新环境启动后要确认该 migration 已执行。

## 10. 管理入口
系统事件页面和权限资源当前在 authorization 历史 Flyway migration `V47__system_event_menu.sql` 中登记，权限码为 `system:event:list`、`system:event:detail`、`system:event:reconsume`。本模块 Controller 通过 `@ApiAccess` 标注这些权限。

事件对象不会自动注入租户。发布方需要把 tenant id、user id、业务幂等键等写入 headers 或 payload；订阅方必须校验租户边界，避免跨租户处理。

## 11. 快速开始
1. 定义事件类型命名规范，例如 `payment.succeeded`、`order.cancelled`。
2. 发布方通过 `IDomainEventPublisher` 发布 `DomainEvent`，写清 business key、aggregate id、tenant id 和幂等键。
3. 订阅方实现 `DomainEventSubscriber`，按 event id 或业务幂等键防重复处理。
4. 需要可靠投递时开启 outbox，并确认 KV store 可用。
5. 需要运维入口时，确认系统事件菜单和权限已初始化到 authorization。

## 12. 问题排查
- 事件没有被订阅：检查订阅者是否是 Spring Bean，`eventType()` 是否和发布事件类型一致。
- outbox 开启后没有投递：检查 `outbox.dispatch-enabled`、KV store、dispatcher 日志和 max attempts。
- reconsume 不能修复业务数据：它只重新投递事件，业务 handler 仍要处理数据状态和幂等。
- 多服务重复消费：订阅者必须按 event id 或业务键幂等。

## 13. 相关文档
- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [持久化规范](../../../mango-pmo/rules/backend/07-persistence.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 14. 历史资料
- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
