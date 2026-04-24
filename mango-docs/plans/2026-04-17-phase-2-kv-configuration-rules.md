# Phase 2 mango-infra-kv 配置与装配规则

- 盘点日期：2026-04-17
- 范围：`mango/mango-infra/mango-infra-kv`
- 目标：统一 KV 配置术语，拆清 `store` 选择与 `capability bean` 默认装配职责。

## P2-T1 API 盘点

`mango-infra-kv-api` 当前包含：

| 类型 | 清单 | Phase 2 处理 |
|------|------|--------------|
| Store 接口 | `IKvStore` | 保留，作为底层 KV store 唯一抽象 |
| Capability 接口 | `ICache`、`ILocker`、`ICounter`、`IRateLimiter`、`IIdempotent`、`ITokenStore`、`IIdGenerator` | 保留，作为基于 store 的使用场景能力 |
| 辅助接口 | `ISerializer`、`IConverter` | 保留，作为注解/AOP 或对象处理辅助能力 |
| 注解 | `@Cacheable`、`@Idempotent`、`@Locker`、`@RateLimit` | 保留；Phase 2 不扩展注解语义 |
| 枚举 | `KvStoreTypeEnum` | 保留并补齐 `AUTO` / `JDBC` 命名口径 |

API 入参规则：

- 不在 `kv-api` 方法上新增 `jakarta.validation` 注解，避免把 Bean Validation 依赖扩进基础契约。
- 接口 Javadoc 继续定义 key、ttl、value 等参数契约。
- 运行时参数校验由 core 实现负责，infra 层不使用 `Require`；`Require` 抛出 `BizException`，不适合作为 infra 参数错误语义。

## P2-T2 Core 实现盘点

| Store | 底层实现 | Capability 实现 |
|-------|----------|---------------|
| `memory` | `MemoryKvStore` | `MemoryCache`、`MemoryLocker`、`MemoryCounter`、`MemoryRateLimiter`、`MemoryIdempotent`、`MemoryTokenStore`、`MemoryIdGenerator` |
| `redis` | `RedisKvStore` | `RedisCache`、`RedisLocker`、`RedisCounter`、`RedisRateLimiter`、`RedisIdempotent`、`RedisTokenStore`、`RedisIdGenerator` |
| `jdbc` | `JdbcKvStore` | `JdbcCache`、`JdbcLocker`、`JdbcCounter`、`JdbcRateLimiter`、`JdbcIdempotent`、`JdbcTokenStore`、`JdbcIdGenerator` |
| common | - | `JsonSerializer`、`JsonConverter` |

Core 结论：

- memory/redis/jdbc 三类实现都保留。
- `mango-infra-kv-core` 按实现方案分包：`memory`、`redis`、`jdbc`、`support`，注解切面保留在 `aspect`。
- 注解切面命名已统一为 `KvCapabilityAspect`。
- `KvCapabilityAspect` 的动态 key 解析统一使用 SpEL 模板或直接 SpEL 表达式，支持 Spring Bean 解析，并通过 `mango-common` 的 `RequestContextContributor` 协议消费外部请求上下文变量；不扩展 `user:#userId` 这类非标准内联占位写法。
- Web 请求 header/cookie/request 变量不再由 `kv-core` 直接提供，而是由 `mango-infra-web` 作为 contributor 增强。
- Flyway 迁移脚本使用 `db/migration/kv` 路径。

## P2-T3 Starter 盘点

当前 starter 自动配置：

| 类 | 当前职责 | 问题 |
|----|----------|------|
| `KvStoreAutoConfiguration` | 创建 `IKvStore` | jdbc 条件不够明确；store 选择文档与实际代码不一致 |
| `KvCapabilityAutoConfiguration` | 创建 capability beans | 默认强启所有 memory capability，且没有随 store 选择 redis/jdbc 实现 |
| `KvStoreProperties` | `mango.kv` 配置属性 | 没有独立 capability 默认装配开关 |

## 统一配置前缀

Phase 2 后唯一当前前缀：

```yaml
mango:
  kv:
    store:
      type: auto # auto | memory | redis | jdbc
    provider:
      jdbc:
        table-name: infra_kv_entry
      memory:
        cleanup-interval-minutes: 1
    capability:
      enabled: false
      cache: false
      locker: false
      counter: false
      rate-limiter: false
      idempotent: false
      token-store: false
      id-generator: false
      serializer: false
      converter: false
```

发布前配置口径：

- store 选择只使用 `mango.kv.store.type`。
- store 类型只使用 `auto` / `memory` / `redis` / `jdbc`。
- JDBC provider 只使用 `mango.kv.provider.jdbc.*`。
- JDBC 默认表名统一为 `infra_kv_entry`；采用 `infra_` 前缀表达基础设施技术表归属。
- Flyway 只保留当前 `db/migration/kv/V1__init_kv_record.sql` 初始化脚本。

## Store 选择规则

`store` 只负责创建 `IKvStore`。

| 配置 | 条件 | 结果 |
|------|------|------|
| `mango.kv.store.type=memory` | 无额外依赖 | `MemoryKvStore` |
| `mango.kv.store.type=redis` | 存在 `RedissonClient` | `RedisKvStore` |
| `mango.kv.store.type=jdbc` | 存在 `JdbcTemplate` | `JdbcKvStore` |
| `mango.kv.store.type=auto` 或未配置 | 存在 `RedissonClient` | `RedisKvStore` |
| `mango.kv.store.type=auto` 或未配置 | 不存在 `RedissonClient` | `MemoryKvStore` |

说明：

- `jdbc` 只依赖 `JdbcTemplate`，数据库记录 ID 使用本地雪花算法生成。
- `JdbcKvStore` 读取 `mango.kv.provider.jdbc.table-name`，默认表名为 `infra_kv_entry`，并对表名执行白名单校验，避免动态 SQL 注入风险。
- `auto` 不自动选择 jdbc，避免应用只因存在 DataSource 就把 KV store 切到数据库。
- 所有 store bean 都必须有 `@ConditionalOnMissingBean(IKvStore.class)`，允许应用覆盖。
- `KvStoreAutoConfiguration` 必须晚于 Redis、DB、Spring JDBC/JdbcTemplate 与 Redisson 自动配置执行，确保 `@ConditionalOnBean` 的判断基线稳定。

## Capability Bean 装配规则

`capability bean` 只负责创建 cache/lock/rate-limit/idempotent/token/id-generator 等能力。

默认规则：

- `mango.kv.capability.enabled=false`，不默认强启所有能力。
- 开启总开关后，各 capability 仍需单独启用。
- 每个 bean 都必须有 `@ConditionalOnMissingBean`，允许业务或测试覆盖。
- capability bean 使用已选择的 `IKvStore` 创建对应实现；serializer/converter 只在显式开启时创建。

选择规则：

| Store 类型 | Capability 实现 |
|------------|---------------|
| `MemoryKvStore` | memory capability |
| `RedisKvStore` | redis capability |
| `JdbcKvStore` | jdbc capability |

## 禁止事项确认

- 业务模块不得直接依赖 `mango-infra-kv-core` 具体实现。
- 业务代码不得通过 if/else 选择 memory/redis/jdbc。
- 默认不强启全部 capability beans。
- `mango-infra-kv` 不使用 `Require` 做参数校验，不把业务异常语义带入 infra。
