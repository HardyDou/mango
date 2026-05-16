# mango-infra-event

`mango-infra-event` 是 Mango 的领域事件基础设施。它提供统一的发布/订阅接口，当前内置进程内 memory 实现，后续可扩展 Redis Stream、数据库 outbox 等可靠投递实现。

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

```yaml
mango:
  event:
    type: memory
```

当前 `type` 仅保留扩展位。生产环境如需跨进程可靠通知，应扩展 Redis/DB 实现，增加 ack、retry、dead-letter 和幂等消费表。
