# mango-infra-event

`mango-infra-event` 是 Mango 的领域事件基础设施。它提供统一的发布/订阅接口，默认使用进程内事件总线；需要可靠投递时，通过 `mango-infra-kv` 的 Outbox capability 承载，并可选启用 Redis Stream 作为跨服务轻量传输。

## 模块结构

```text
mango-infra-event/
├── mango-infra-event-api      # DomainEvent、发布器、事件总线、订阅者接口
├── mango-infra-event-core     # 进程内事件总线、Outbox dispatcher、Redis Stream transport
└── mango-infra-event-starter  # 自动配置
```

## 当前能力

- `IDomainEventPublisher`：发布领域事件。
- `IDomainEventBus`：发布并订阅领域事件。
- `DomainEventSubscriber`：业务订阅者接口。
- `InMemoryDomainEventBus`：进程内同步分发，作为本地 subscriber dispatcher；handler 异常会向上抛出，供 Outbox 识别失败。
- `OutboxDomainEventDispatcher`：把 KV Outbox 消息可靠投递给本进程订阅者。
- `RedisStreamDomainEventTransport`：可选跨进程传输，使用 Redis Stream consumer group，不使用 Redis Pub/Sub。
- `SystemEventApi`：系统事件运维接口，查询异常事件并发起重新投递。

## 事件模型

```json
{
  "eventId": "uuid",
  "eventType": "workflow.process.completed",
  "businessType": "EXPENSE_REIMBURSEMENT",
  "businessKey": "EXP-202605-001",
  "aggregateId": "APPLY-EXP-202605-001-002",
  "occurredAt": "2026-05-16T07:30:00Z",
  "payload": {},
  "headers": {}
}
```

## 订阅示例

```java
@Component
class ExpenseWorkflowSubscriber implements DomainEventSubscriber {

    @Override
    public String eventType() {
        return "workflow.process.completed";
    }

    @Override
    public void onEvent(DomainEvent event) {
        // 根据 businessType、businessKey、aggregateId 幂等更新业务状态
    }
}
```

## 配置

默认使用进程内同步事件总线：

```yaml
mango:
  event:
    type: memory
```

当前 `type` 仅保留兼容扩展位。需要可靠投递时，开启 `mango.event.outbox.enabled`，并同时开启 KV capability 的 Outbox：

```yaml
mango:
  kv:
    type: redis # memory / redis / jdbc，生产多实例建议 redis 或 jdbc
    capability:
      enabled: true
      outbox: true
  event:
    type: memory
    outbox:
      enabled: true
      worker-id: workflow-event-worker
      batch-size: 50
      retry-delay-seconds: 60
      max-attempts: 5
      dispatch-enabled: true
      dispatch-interval-millis: 1000
      dispatch-initial-delay-millis: 1000
```

开启后：

- `IDomainEventPublisher` 写入 KV Outbox，不直接同步触发业务订阅者。
- `IOutboxDispatcher.dispatchOnce()` 领取待投递事件，发布到 `IDomainEventBus`，成功 `ack`，失败 `nack` 并按 `retry-delay-seconds` 重试。
- 领取投递次数达到 `max-attempts` 后，消息进入 `FAILED` 终态；可通过系统事件页面或 `SystemEventApi.reconsume` 放回 `PENDING`。
- 默认创建进程内调度器，按 `dispatch-interval-millis` 周期调用 `dispatchOnce()`；如果应用需要外部 worker 接管，可设置 `dispatch-enabled=false`。
- 如果只开启 `mango.event.outbox.enabled=true`，但没有开启 KV Outbox capability，应用启动应失败，避免生产环境静默退回进程内发布。

跨服务投递使用 Redis Stream：

```yaml
mango:
  kv:
    store:
      type: redis
    capability:
      enabled: true
      outbox: true
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

开启 `transport=redis-stream` 后，生产者仍先写 KV Outbox；Outbox dispatcher 将事件发布到 Redis Stream 并 `ack` 本地 Outbox。消费方通过 Redis Stream consumer group 拉取事件，再交给本地 `DomainEventSubscriber` 处理，处理成功后确认 stream 消息。Redis Pub/Sub 不具备离线保留和 consumer group ack 语义，本模块不支持。

## 与 Outbox 的关系

- `mango-infra-event` 定义事件语义：事件模型、发布、订阅、业务处理。
- `mango-infra-kv` 的 Outbox 定义可靠投递底座：`enqueue / claim / ack / nack / fail / requeue / query`。
- Workflow 等业务模块只发布领域事件，不直接依赖 Redis、DB 或 MQ。
- 不引入 MQ 时，可用 memory / redis / jdbc store 承载 Outbox；生产多实例优先使用 redis 或 jdbc。
- 需要跨微服务广播或多实例消费时，启用 Redis Stream transport。更重的 MQ 方案后续可通过 `DomainEventTransport` 扩展，不改变业务发布和订阅代码。

## 系统事件运维

开启 Outbox 后会暴露系统事件接口：

- `GET /system/events`：分页查询事件，默认只返回异常事件。
- `GET /system/events/detail?messageId=...`：查询事件详情、错误、payload 和 headers。
- `POST /system/events/reconsume`：将失败或等待重试事件重新放回待投递队列。

管理后台菜单在“系统维护 / 系统事件”，权限码为 `system:event:list`、`system:event:detail`、`system:event:reconsume`。

## 业务接入建议

业务模块只依赖 `IDomainEventPublisher` 发布事件，只实现 `DomainEventSubscriber` 订阅自己关心的事件。事件的 `businessType`、`businessKey`、`aggregateId` 用于幂等更新业务状态；业务详情、审批快照等大对象仍由业务库或工作流业务表维护，不塞进 Outbox。
