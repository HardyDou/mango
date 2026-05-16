# Workflow Outbox Plan

更新时间：2026-05-16

## 目标

把 Outbox 做成 Mango 底座能力，用于工作流及其他业务的可靠事件投递。

## 边界

- Outbox 放在重命名后的底座模块体系内，和 locker / cache / rate limit 同级。
- `infra-event` 保持独立语义层，不和底座能力合并。
- 不引入 MQ。

## 任务拆分

| 优先级 | 任务 | 验收标准 | 状态 |
|---|---|---|---|
| P0 | Outbox API 与基础模型 | 提供 `OutboxMessage`、`OutboxStatus`、`IOutboxStore`、`IOutboxPublisher`、`IOutboxDispatcher` | 已完成 |
| P0 | Outbox 底座实现 | memory / redis / db 三种后端可用，基于现有 `IKvStore` 能力运行 | 已完成 |
| P0 | Outbox 自动装配 | 通过 `mango.kv.capability.outbox` 显式开启 | 已完成 |
| P0 | Outbox 单测 | 覆盖 enqueue / claim / ack / nack / retry | 已完成 |
| P1 | `infra-event` 接入 Outbox | 领域事件可选择通过 Outbox 持久化与分发 | 已完成 |
| P1 | Workflow 接入 Outbox | 工作流完成、驳回等事件改为走 Outbox 可靠投递 | 已完成 |

## 已完成验证

- P0 Outbox 底座：`mvn -pl :mango-infra-test -am -Dtest=OutboxAutoConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test`
- P1 infra-event 接入 Outbox：`mvn -pl :mango-infra-test -am -Dtest=OutboxAutoConfigurationTest,DomainEventOutboxAutoConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test`
- P1 Workflow 标准事件：`mvn -pl :mango-workflow-core -am -Dtest=WorkflowEventPublisherTest -Dsurefire.failIfNoSpecifiedTests=false test`

## 设计原则

1. 先保留事件语义层独立性。
2. Outbox 只做可靠投递底座，不引入 MQ 语义。
3. 存储后端复用现有 KV store，不重复造 memory/redis/db 三套技术栈。
4. 申请历史和审批历史保留快照，Outbox 只负责事件投递与重试。
