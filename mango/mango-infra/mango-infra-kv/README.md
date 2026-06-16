# Mango Infra KV

## 1. 概览
`mango-infra-kv` 提供 Mango 统一 KV 抽象和基于 KV 的通用能力，覆盖 Memory、Redis、JDBC store，以及缓存、锁、计数器、限流、幂等、token store、ID 生成、序列化、转换和 Outbox。

## 2. 功能清单

| 能力 | 常用入口 |
|------|----------|
| 业务需要轻量 key-value 存储，并希望可在 memory、Redis、JDBC 间切换 | Maven 依赖 / starter / Java API |
| 业务方法需要用注解快速接入缓存、分布式锁、限流和幂等 | Maven 依赖 / starter / Java API |
| infra-event、realtime、内部调用 nonce 等基础设施需要共享 KV 能力 | Maven 依赖 / starter / Java API |
| 需要 Outbox store / publisher 支撑可靠投递 | Maven 依赖 / starter / Java API |


## 3. 能力边界
- 不替代业务主数据库，不适合承载强关系、复杂查询和长期业务数据。
- 不替代专业消息队列。
- 不自动保证跨租户隔离；key 设计必须包含业务域和必要上下文。
- store 创建和 capability 创建是两步，选择 Redis store 不代表自动启用缓存、锁、限流等能力。

## 4. 模块入口
- `mango-infra-kv-api`：store、capability、Outbox、注解和上下文契约。
- `mango-infra-kv-core`：Memory/Redis/JDBC store、capability 实现、AOP、key namespace、Outbox store。
- `mango-infra-kv-starter`：Redis、store、capability、Outbox 自动配置。

业务模块负责 key 语义、TTL、幂等窗口、并发冲突处理和降级策略。

## 5. 接入方式
```xml
<dependency>
    <groupId>io.mango.infra.kv</groupId>
    <artifactId>mango-infra-kv-starter</artifactId>
</dependency>
```

只使用契约：

```xml
<dependency>
    <groupId>io.mango.infra.kv</groupId>
    <artifactId>mango-infra-kv-api</artifactId>
</dependency>
```

注解示例：

```java
@Cacheable(key = "user:#{#query.tenantId}:#{#query.userId}", ttl = 300)
public UserVO getUser(UserQuery query) {
    return userRepository.find(query);
}

@Locker(key = "order:pay:#{#orderId}", ttl = 30)
public void pay(String orderId) {
    paymentService.pay(orderId);
}

@Idempotent(key = "callback:#{#request.id}", window = 600)
public void handleCallback(CallbackRequest request) {
    callbackService.handle(request);
}
```

## 6. 配置说明
主配置前缀：`mango.kv`。Redis 连接兼容前缀：`mango.redis`。

### Store

| 配置 | 默认值 | 含义 |
|------|--------|------|
| `store.type` | `auto` | store 类型：`auto`、`redis`、`jdbc`、`memory`。空值按 `auto` 处理。 |
| `provider.memory.cleanup-interval-minutes` | `1` | memory store 过期 key 清理间隔。 |
| `provider.jdbc.table-name` | `infra_kv_entry` | JDBC store 表名。 |
| `provider.jdbc.url` | 回退到 `spring.datasource.url` | JDBC URL。 |
| `provider.jdbc.username` | 回退到 `spring.datasource.username` | JDBC 用户名。 |
| `provider.jdbc.password` | 回退到 `spring.datasource.password` | JDBC 密码。 |
| `provider.jdbc.driver` | 回退到 `spring.datasource.driver-class-name` | JDBC 驱动。 |

`store.type=auto` 时，存在 `RedissonClient` 则使用 Redis，否则使用 Memory。`store.type=jdbc` 需要 `JdbcTemplate`，且表结构必须存在。

### Redis

Redis 配置优先级在代码中按 `mango.dal.provider.redis.*`、`mango.redis.*`、`spring.redis.*`、默认值解析；README 中推荐统一使用 `mango.redis` 或 Spring Boot Redis 配置。

| 配置 | 默认值 | 含义 |
|------|--------|------|
| `mango.redis.enabled` | `true` | 是否启用 Mango Redis 配置。 |
| `mango.redis.host` | `localhost` | Redis host。 |
| `mango.redis.port` | `6379` | Redis port。 |
| `mango.redis.password` | 无 | Redis 密码。 |
| `mango.redis.database` | `0` | Redis database。 |
| `mango.redis.timeout` | `3000` | 连接超时毫秒。 |
| `mango.redis.pool.max-active` | `8` | 连接池最大活跃数。 |
| `mango.redis.pool.max-idle` | `8` | 最大空闲连接。 |
| `mango.redis.pool.min-idle` | `0` | 最小空闲连接。 |
| `mango.redis.pool.max-wait` | `-1` | 连接池最大等待毫秒。 |

### Key Namespace

| 配置 | 默认值 | 含义 |
|------|--------|------|
| `key.enabled` | `true` | capability 是否自动加 Mango KV namespace。 |
| `key.prefix` | `mango:kv` | namespace 根前缀。 |
| `key.env` | `default` | 环境段，建议 dev/test/prod 分开。 |
| `key.app-enabled` | `false` | 是否加入 app 段。 |
| `key.app` | `app` | app 段值。 |

最终 key 形态：`{prefix}:{env}[:{app}]:{capability}:{biz-key}`。

### Capability

所有 capability 默认关闭。先开启 `capability.enabled`，再按需打开单项。

| 配置 | 默认值 | 注册 Bean |
|------|--------|-----------|
| `capability.enabled` | `false` | 启用 capability 自动配置和 AOP。 |
| `capability.cache` | `false` | `ICache`。 |
| `capability.locker` | `false` | `ILocker`。 |
| `capability.counter` | `false` | `ICounter`。 |
| `capability.rate-limiter` | `false` | `IRateLimiter`。 |
| `capability.idempotent` | `false` | `IIdempotent`。 |
| `capability.token-store` | `false` | `ITokenStore`。 |
| `capability.id-generator` | `false` | `IIdGenerator`。 |
| `capability.serializer` | `false` | `ISerializer`。 |
| `capability.converter` | `false` | `IConverter`。 |
| `capability.outbox` | `false` | `IOutboxStore`、`IOutboxPublisher`。 |

示例：

```yaml
mango:
  kv:
    store:
      type: redis
    key:
      prefix: mango:kv
      env: prod
      app-enabled: true
      app: mango-admin
    capability:
      enabled: true
      cache: true
      locker: true
      rate-limiter: true
      idempotent: true
      serializer: true
      converter: true
      outbox: true
  redis:
    host: redis.internal
    port: 6379
```

## 7. API 与扩展
- Store：`IKvStore`、`IKvSortedSet`。
- Capability：`ICache`、`ILocker`、`ICounter`、`IRateLimiter`、`IIdempotent`、`ITokenStore`、`IIdGenerator`。
- 支撑：`ISerializer`、`IConverter`、`KvContext`、`KvContextContributor`。
- Outbox：`IOutboxPublisher`、`IOutboxStore`、`IOutboxDispatcher`、`OutboxMessage`、`OutboxStatus`。
- 注解：`@Cacheable`、`@Locker`、`@RateLimit`、`@Idempotent`。

注解字段：

- `@Cacheable(key, ttl, cacheValue)`：缓存方法返回值，默认 TTL 3600 秒。
- `@Locker(key, ttl)`：方法执行前加锁，默认 TTL 30 秒。
- `@RateLimit(key, permits)`：按 key 获取令牌，超限返回 false。
- `@Idempotent(key, window)`：执行前先标记幂等 key，默认窗口 60 秒；方法抛异常后同 key 也会被视为重复，直到窗口过期。

key 表达式支持 SpEL 模板，例如 `order:#{#orderId}`、`user:#{#req.headers['X-Tenant']}:#{#userId}`。

## 8. 数据与初始化
Flyway 路径：`mango-infra-kv-core/src/main/resources/db/migration/kv`。

`V1__init_kv.sql` 创建 `infra_kv_entry`，包含：

- `kv_key` 唯一索引 `uk_kv_key`。
- `expire_time` 索引 `idx_kv_record_expire_time`。

JDBC store 依赖该表。Redis 和 Memory store 不需要 SQL 初始化。

## 9. 管理入口
本模块不创建菜单和权限。租户隔离靠 key 设计、`KvContextContributor` 或业务调用方传入 tenant id 实现。不要把多租户共享 key 写成全局常量。

## 10. 快速开始
1. 选择 store：本地开发可 memory，生产优先 Redis；需要数据库持久化时使用 JDBC。
2. 按需开启 capability，不要为了“方便”一次性打开全部能力。
3. 设计 key：包含业务域、租户、用户或请求幂等键。
4. 对写接口使用幂等时，明确 mark-before 语义是否符合业务重试要求。
5. 需要可靠事件或 realtime outbox 时，同时开启 KV outbox 和对应上层模块 outbox。

## 11. 问题排查
- 已配置 Redis 但仍是 memory：检查是否存在 `RedissonClient`，以及 `store.type` 是否为 `redis` 或 `auto`。
- 注解不生效：检查 `capability.enabled` 和对应单项开关是否开启。
- 幂等后不能重试失败请求：当前 `@Idempotent` 是执行前标记，需要业务侧按窗口过期或新 key 重试。
- JDBC store 报表不存在：执行 `db/migration/kv` 下的 Flyway migration。
- Outbox 重复投递：消费者必须按 message id 或业务键幂等。

## 12. 相关文档
- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [持久化规范](../../../mango-pmo/rules/backend/07-persistence.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 13. 补充资料
- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
