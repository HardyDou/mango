# Mango Infra Event

## 1. 能力定位

`mango-infra-event` 提供领域事件发布、订阅、本地分发、Outbox 投递、Redis Stream 传输和系统事件运维接口。主要使用者是需要解耦领域动作、异步投递和失败重试的后端模块。

## 2. 适用场景

- 进程内发布和订阅领域事件。
- 通过 KV Outbox 承载可靠投递。
- 可选使用 Redis Stream 做跨进程轻量传输。
- 查询异常系统事件并触发重新消费。

## 3. 不适用场景

- 不替代完整消息队列治理平台。
- 不负责业务事务边界和业务幂等。
- 不保存独立事件业务表，可靠投递依赖 `mango-infra-kv` Outbox 能力。

## 4. 模块边界

`api` 提供事件契约，`core` 提供本地事件总线、Outbox dispatcher 和 Redis Stream transport，`starter` 提供自动配置和系统事件 Controller。业务模块负责定义事件类型、payload、订阅者和幂等处理。

## 5. 接入方式

```xml
<dependency>
    <groupId>io.mango.infra.event</groupId>
    <artifactId>mango-infra-event-starter</artifactId>
</dependency>
```

只使用契约时可依赖 `mango-infra-event-api`。

## 6. 配置项

配置前缀：`mango.event`。

已发现字段包括 `type`、`transport`、`outbox.*`、`redisStream.*`。默认事件总线类型为 memory，默认 transport 为 none。

## 7. 对外接口 / 扩展点

- `IDomainEventPublisher`
- `IDomainEventBus`
- `DomainEventSubscriber`
- `DomainEventHandler`
- `SystemEventApi`
- `DomainEventTransport`
- `SystemEventController` 路径 `/system/events`，接口包括列表、详情和重新消费；仅在 `mango.event.outbox.enabled=true` 等 outbox 条件满足时装配。

## 8. 数据库 / 初始化数据

未发现本模块独立 SQL migration。Outbox 存储依赖 `mango-infra-kv` 的 Outbox/KV 存储接口。

## 9. 菜单 / 权限 / 租户

系统事件菜单和权限资源目前体现为 authorization 历史迁移 `V47__system_event_menu.sql`；这不是 event 模块新增菜单资产的长期归属方式。事件 payload 中的租户、用户和业务上下文由发布方负责写入并由订阅方校验。

## 10. 验证方式

```bash
mvn -f mango/pom.xml -pl mango-infra/mango-infra-event -am test
```

当前未发现该模块独立 `src/test` 测试类；验收时应覆盖发布、订阅、失败重试和系统事件重新消费链路。

## 11. 业务接入最小闭环

业务模块定义领域事件类型和 payload 后，通过 `IDomainEventPublisher` 发布事件，订阅方实现 `DomainEventSubscriber` 或 handler，并在 handler 内保证幂等和可重试。需要可靠投递时启用 `mango.event.outbox.enabled=true`，并确认 infra-kv store/outbox 能力已接入。

跨进程轻量传输可配置 `mango.event.transport=redis-stream` 及 redis-stream 分组。验收断言覆盖：发布后订阅者收到事件，订阅者异常进入失败/重试链路，`/system/events` 在 outbox 启用场景可查询并 reconsume，重复投递不会破坏业务数据。

## 12. 常见问题

- 订阅者异常会导致投递失败，业务订阅者需要实现幂等和可重试处理。
- 跨服务投递前先确认 Redis Stream 或 Outbox 配置已启用。
- 系统事件重试只能重新投递事件，不会自动修复业务数据错误。

## 13. 关联 PMO 规则

- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [持久化规范](../../../mango-pmo/rules/backend/07-persistence.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 14. 历史设计 / 交付记录

- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
