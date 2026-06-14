# Mango Infra KV

## 1. 能力定位

`mango-infra-kv` 提供 KV 抽象层、Memory/Redis/JDBC store、缓存、锁、计数、限流、幂等、token store、ID 生成和 Outbox 能力。主要使用者是 infra 模块和需要轻量通用 KV capability 的业务模块。

## 2. 适用场景

- 需要统一 KV store 抽象并可在 memory、Redis、JDBC 间切换。
- 需要缓存、分布式锁、限流、幂等、计数器、token store。
- 需要 Outbox 发布、存储能力，或基于 `IOutboxDispatcher` 契约接入自定义 dispatcher。
- 需要通过注解给方法接入 cache、lock、rate limit 或 idempotent 能力。

## 3. 不适用场景

- 不替代业务数据库主存储。
- 不替代复杂消息队列、搜索引擎或分析型存储。
- 不自动启用所有 capability，store 选择和 capability 装配是两件事。

## 4. 模块边界

`api` 提供接口、注解和上下文，`core` 提供 memory/redis/jdbc 实现和 capability 组合，`starter` 提供自动配置。业务模块负责 key 设计、过期策略、幂等语义和故障处理。

## 5. 接入方式

```xml
<dependency>
    <groupId>io.mango.infra.kv</groupId>
    <artifactId>mango-infra-kv-starter</artifactId>
</dependency>
```

只使用契约时依赖 `mango-infra-kv-api`。

## 6. 配置项

配置前缀：

- `mango.kv`：KV store 和 capability 配置，来源 `KvStoreProperties`。
- `mango.redis`：Redis 连接配置，来源 `KvRedisProperties`。

自动配置包括 `KvRedisAutoConfiguration`、`KvStoreAutoConfiguration`、`KvCapabilityAutoConfiguration`、`OutboxAutoConfiguration`。

关键字段包括 `mango.kv.store.type`、`mango.kv.provider.*`、`mango.kv.key.*`、`mango.kv.capability.enabled` 和 `mango.kv.capability.outbox`。

## 7. 对外接口 / 扩展点

- Store：`IKvStore`、`IKvSortedSet`
- Capability：`ICache`、`ILocker`、`ICounter`、`IRateLimiter`、`IIdempotent`、`ITokenStore`、`IIdGenerator`
- 序列化：`ISerializer`、`IConverter`
- Outbox：`IOutboxPublisher`、`IOutboxStore`；`IOutboxDispatcher` 为 dispatcher 契约，当前 starter 默认装配 publisher / store。
- 上下文：`KvContext`、`KvContextContributor`
- 注解：`@Cacheable`、`@Locker`、`@RateLimit`、`@Idempotent`

## 8. 数据库 / 初始化数据

Flyway 路径：`mango-infra-kv-core/src/main/resources/db/migration/kv`。

`V1__init_kv.sql` 创建 `infra_kv_entry`，包含唯一键 `uk_kv_key` 和过期时间索引 `idx_kv_record_expire_time`。

## 9. 菜单 / 权限 / 租户

本模块不提供菜单或权限资源。租户维度通常通过 key 前缀、上下文 contributor 或业务调用方约定实现。

KV capability key 命名空间默认形态为 `{prefix}:{env}[:{app}]:{capability}:{biz-key}`，调用方不应手写完整底层前缀。

## 10. 验证方式

```bash
mvn -f mango/pom.xml -pl mango-infra/mango-infra-kv -am test
```

当前未发现该模块独立 `src/test` 测试类；验收应覆盖 store 切换、注解能力、过期清理和 Outbox 投递。

## 11. 业务接入最小闭环

业务应用先选择 store：`auto`、`redis`、`jdbc` 或 `memory`，再按需开启 capability，例如 cache、locker、rateLimiter、idempotent、tokenStore、idGenerator 或 outbox。JDBC store 需要确认 `infra_kv_entry` migration 已执行。

业务代码只传业务段 key，让 capability 层按 `{prefix}:{env}[:{app}]:{capability}:{biz-key}` 生成完整命名空间。验收断言覆盖：不同 env/app key 隔离，过期 key 被清理，锁和幂等在并发下生效，Outbox 重试不会重复执行业务副作用。

## 12. 常见问题

- 选择 Redis store 不代表自动开启所有 capability，需要检查 `mango.kv` 配置。
- key 设计应包含业务域和必要上下文，避免跨租户或跨模块冲突。
- Outbox 失败重试需要业务消费者保证幂等。

## 13. 关联 PMO 规则

- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [持久化规范](../../../mango-pmo/rules/backend/07-persistence.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 14. 历史设计 / 交付记录

- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
