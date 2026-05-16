# mango-infra-event

`mango-infra-event` 是 Mango 的领域事件基础设施。它提供统一的发布/订阅接口，当前内置进程内 memory 实现，后续可靠投递通过 `mango-infra-kv` 的 Outbox capability 承载，不引入 MQ。

## 模块结构

```text
mango-infra-event/
├── mango-infra-event-api      # DomainEvent、发布器、事件总线、订阅者接口
├── mango-infra-event-core     # 内存实现和后续 Redis/DB 实现
└── mango-infra-event-starter  # 自动配置
```

## 当前能力

- `IDomainEventPublisher`：发布领域事件。
- `IDomainEventBus`：发布并订阅领域事件。
- `DomainEventSubscriber`：业务订阅者接口。
- `InMemoryDomainEventBus`：进程内同步分发，适合单体、本地开发和测试。

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

当前 `type` 仅保留扩展位。需要可靠投递时，开启 `mango.event.outbox.enabled`，并同时开启 KV capability 的 Outbox：

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
```

开启后：

- `IDomainEventPublisher` 写入 KV Outbox，不直接同步触发业务订阅者。
- `IOutboxDispatcher.dispatchOnce()` 领取待投递事件，发布到 `IDomainEventBus`，成功 `ack`，失败 `nack` 并按 `retry-delay-seconds` 重试。
- 如果只开启 `mango.event.outbox.enabled=true`，但没有开启 KV Outbox capability，应用启动应失败，避免生产环境静默退回进程内发布。

## 与 Outbox 的关系

- `mango-infra-event` 定义事件语义：事件模型、发布、订阅、业务处理。
- `mango-infra-kv` 的 Outbox 定义可靠投递底座：`enqueue / claim / ack / nack`。
- Workflow 等业务模块只发布领域事件，不直接依赖 Redis、DB 或 MQ。
- 不引入 MQ 时，可用 memory / redis / jdbc store 承载 Outbox；生产多实例优先使用 redis 或 jdbc。

## 业务接入建议

业务模块只依赖 `IDomainEventPublisher` 发布事件，只实现 `DomainEventSubscriber` 订阅自己关心的事件。事件的 `businessType`、`businessKey`、`aggregateId` 用于幂等更新业务状态；业务详情、审批快照等大对象仍由业务库或工作流业务表维护，不塞进 Outbox。
