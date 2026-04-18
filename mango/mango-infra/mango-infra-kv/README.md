# mango-infra-kv

`mango-infra-kv` 是 Mango 的 KV 技术底座，提供底层 KV store 抽象和可选 capability bean 装配。

## 职责边界

| 概念 | 定义 | 示例 |
|------|------|------|
| `store` | 实际 KV 存储实现，只回答“底层用哪个存储” | `MemoryKvStore`、`RedisKvStore`、`JdbcKvStore` |
| `capability bean` | 基于 `IKvStore` 组装出的通用使用场景能力，只回答“哪些能力启用” | `ICache`、`ILocker`、`IRateLimiter`、`IIdempotent` |

`store` 选择和 `capability bean` 装配是两个独立职责。选择了 Redis store 不代表自动启用所有 cache/lock/rate-limit/idempotent bean。

## 模块结构

```text
mango-infra-kv/
├── mango-infra-kv-api        # 接口、注解、枚举
├── mango-infra-kv-core       # memory / redis / jdbc / support 实现
└── mango-infra-kv-starter    # 自动配置
```

`mango-infra-kv-core` 按职责分包：

| 包 | 职责 |
|----|------|
| `io.mango.infra.kv.core.memory` | 内存 store 实现 |
| `io.mango.infra.kv.core.redis` | Redis store 实现 |
| `io.mango.infra.kv.core.jdbc` | JDBC store 实现 |
| `io.mango.infra.kv.core.capability` | 基于 `IKvStore` 的通用 capability 实现 |
| `io.mango.infra.kv.core.support` | JSON serializer / converter 等通用支撑 |
| `io.mango.infra.kv.core.aspect` | KV 能力切面与请求上下文消费 |

## API 清单

| 类型 | 清单 |
|------|------|
| Store | `IKvStore` |
| Capability | `ICache`、`ILocker`、`ICounter`、`IRateLimiter`、`IIdempotent`、`ITokenStore`、`IIdGenerator` |
| 辅助 | `ISerializer`、`IConverter` |
| 注解 | `@Cacheable`、`@RateLimit`、`@Idempotent`、`@Locker` |
| 枚举 | `KvStoreTypeEnum` |

## 配置前缀

当前统一前缀为 `mango.kv.*`。不再新增或推荐 `mango.dal.*`。

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
    key:
      enabled: true
      prefix: mango:infra:kv
      env: default
      app-enabled: false
      app: app
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

兼容说明：

- `mango.kv.type` 作为旧配置兼容入口保留。
- `db` 作为 `jdbc` 的旧别名保留。
- `mango.kv.provider.db.*` 作为 `mango.kv.provider.jdbc.*` 的旧配置兼容入口保留。
- 文档、日志和新增配置统一使用 `mango.kv.store.type` 与 `jdbc`。
- JDBC 当前默认表名为 `infra_kv_entry`；老库通过后续迁移从 `sys_kv_record` 前向迁移到新表名。

## Store 选择规则

| 配置 | 条件 | 注入结果 |
|------|------|----------|
| `mango.kv.store.type=memory` | 无额外依赖 | `MemoryKvStore` |
| `mango.kv.store.type=redis` | 存在 `RedissonClient` | `RedisKvStore` |
| `mango.kv.store.type=jdbc` | 存在 `JdbcTemplate` 与 `RedissonClient` | `JdbcKvStore` |
| `mango.kv.store.type=db` | 兼容别名 | `JdbcKvStore` |
| `mango.kv.store.type=auto` 或未配置 | 存在 `RedissonClient` | `RedisKvStore` |
| `mango.kv.store.type=auto` 或未配置 | 不存在 `RedissonClient` | `MemoryKvStore` |

说明：

- `auto` 不自动选择 jdbc，避免应用只因存在 DataSource 就把 KV store 切到数据库。
- `JdbcKvStore` 当前仍依赖 `RedissonClient` 生成递增 ID，Phase 2 不改变该实现。
- `JdbcKvStore` 会读取 `mango.kv.provider.jdbc.table-name`，默认表名为 `infra_kv_entry`，并校验表名只允许字母、数字和下划线。
- Flyway 迁移保持 `V1=sys_kv_record`、`V2=rename to infra_kv_entry`，保证老环境升级时向前兼容。
- 所有 store bean 都有 `@ConditionalOnMissingBean(IKvStore.class)`，应用可覆盖。
- `KvStoreAutoConfiguration` 声明晚于 Redis、DB、Spring JDBC/JdbcTemplate 和 Redisson 自动配置执行，保证 `@ConditionalOnBean` 判断有稳定的 bean definition 基线。

## Capability Bean 装配规则

默认不强启 capability bean。需要显式打开总开关和具体能力开关：

```yaml
mango:
  kv:
    capability:
      enabled: true
      cache: true
      locker: true
```

| 配置 | 注入接口 | 实现选择 |
|------|----------|----------|
| `cache=true` | `ICache` | `KvStoreCache` |
| `locker=true` | `ILocker` | `KvStoreLocker` |
| `counter=true` | `ICounter` | `KvStoreCounter` |
| `rate-limiter=true` | `IRateLimiter` | `KvStoreRateLimiter` |
| `idempotent=true` | `IIdempotent` | `KvStoreIdempotent` |
| `token-store=true` | `ITokenStore` | `KvStoreTokenStore` |
| `id-generator=true` | `IIdGenerator` | `KvStoreIdGenerator` |
| `serializer=true` | `ISerializer` | `JsonSerializer` |
| `converter=true` | `IConverter` | `JsonConverter` |

每个 capability bean 都有 `@ConditionalOnMissingBean`，允许业务、测试或应用装配层覆盖。

Capability 实现不再按 memory / redis / jdbc 分裂。具体存储差异必须封装在 `IKvStore`
实现内：

| Store 语义 | 用途 | 要求 |
|-----------|------|------|
| `setIfAbsent(key, value, ttl)` | lock、idempotent、replay 防重 | 原子写入，已过期 key 视为不存在 |
| `set(key, value, ttl)` | cache、token、captcha 等可覆盖值 | 写入或覆盖，并刷新 TTL |
| `incrementBy(key, delta, window)` | counter、rate-limit、id-generator | 单 key 原子递增，支持正负 delta |
| `get/delete/exists` | 通用读取、删除、存在判断 | 必须遵守过期语义 |

`put(key, value, ttl)` 仅作为旧接口兼容入口保留，语义等同于 `setIfAbsent`。新增代码不得使用
`put` 表达缓存覆盖写入，应使用 `set`。

`IRateLimiter` 当前基于 `incrementBy` 实现固定窗口限流，不再宣称 token bucket。

## Redis Key 规范

自动装配出的 KV capability 默认会给业务 key 增加统一命名空间：

```text
mango:infra:kv:{env}:{capability}:{biz-key}
```

如确实需要按应用隔离，可打开 `mango.kv.key.app-enabled=true`，格式变为：

```text
mango:infra:kv:{env}:{app}:{capability}:{biz-key}
```

默认配置：

```yaml
mango:
  kv:
    key:
      enabled: true
      prefix: mango:infra:kv
      env: default
      app-enabled: false
      app: app
```

能力段固定如下：

| 能力 | capability 段 | 示例 |
|------|---------------|------|
| `ICache` | `cache` | `mango:infra:kv:prod:cache:user:10001` |
| `ILocker` | `lock` | `mango:infra:kv:prod:lock:order:202604170001` |
| `ICounter` | `counter` | `mango:infra:kv:prod:counter:sms:18800000000` |
| `IRateLimiter` | `rate-limit` | `mango:infra:kv:prod:rate-limit:login:ip:127.0.0.1` |
| `IIdempotent` | `idempotent` | `mango:infra:kv:prod:idempotent:payment:req-abc` |
| `ITokenStore` | `token` | `mango:infra:kv:prod:token:access:sha256-xxx` |
| `IIdGenerator` | `idgen` | `mango:infra:kv:prod:idgen:global` |
| `JdbcKvStore` 内部 ID | `jdbc-id` | `mango:infra:kv:prod:jdbc-id:infra_kv_entry` |

约束：

- 业务只传业务段 key，例如 `user:#{#userId}`，不得手写 `mango:infra:kv` 前缀。
- 默认不加应用名，保证同一环境内 infra KV 可以跨应用共享。
- 环境段用于隔离 dev/test/prod，避免共享 Redis 时串数据。
- `app-enabled=true` 仅用于明确需要应用级隔离的部署。
- 底层 `IKvStore` 只处理最终 key；统一前缀在 capability 层完成。

## 注解 Key 表达式

`KvCapabilityAspect` 支持以下 key 写法：

| 写法 | 示例 | 说明 |
|------|------|------|
| 静态字符串 | `user:static:key` | 不含 `#` 或 `@`，直接返回，无任何解析开销 |
| SpEL 模板 | `user:#{#userId}:#{#headers['X-Tenant']}` | 推荐写法，支持方法参数、请求上下文变量和容器 bean |
| 直接 SpEL | `#userId`、`@tenantKey.prefix()` | 整个 key 为一个表达式时使用 |

**语法要求：**
- 新增 key 建议统一使用 `#{...}` 模板，例如 `user:#{#userId}:#{@tenantKey.prefix()}`。
- 请求 header/cookie 访问建议使用模板表达式，例如 `#{#headers['X-Tenant']}`、`#{#cookies['SESSION']}`。
- 不支持 `user:#userId` 这类内联占位写法；必须写成 `user:#{#userId}`。
- 编译后的 `SpelExpression` 被缓存，同一 key 表达式只解析一次

可用上下文变量：

- 方法入参：如 `#userId`（模板中写为 `#{#userId}`）。
- `#args`：方法入参数组。
- `#headers`：由 Web contributor 提供；非 Web 场景默认不存在。
- `#cookies`：由 Web contributor 提供；非 Web 场景默认不存在。
- `#request`：由 Web contributor 提供；仅 Web 场景存在。

请求上下文扩展规则：

- `mango-infra-kv-core` 只消费 `mango-common` 中的 `RequestContextContributor` 协议，不直接依赖 Web/Servlet。
- `mango-infra-web` 当前提供 servlet request/header/cookie 变量增强。
- 后续 Security、Trace、RPC 等模块如需扩展表达式变量，应各自提供 contributor，不得把运行时技术依赖压进 `kv-core`。

## 参数校验口径

- `kv-api` 不新增 `jakarta.validation` 注解，避免扩大 API 依赖。
- 接口 Javadoc 定义 key、ttl、value 等参数契约。
- core 实现使用 `IllegalArgumentException` 校验参数。
- infra 层不使用 `Require`，避免把业务异常语义带入技术底座。

## 禁止事项

- 业务模块不得直接依赖 `mango-infra-kv-core` 具体实现。
- 业务代码不得通过 if/else 选择 memory / redis / jdbc。
- 不得默认强启全部 capability bean。
- 不得继续新增 `mango.dal.*` 配置或 DAL 术语。

## 参考文档

- [Phase 2 配置与装配规则](../../../mango-docs/plans/2026-04-17-phase-2-kv-configuration-rules.md)
- [后端模块级重构计划](../../../mango-docs/plans/2026-04-17-backend-module-by-module-refactor-plan.md)
