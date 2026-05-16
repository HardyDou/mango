# mango-infra-kv

`mango-infra-kv` 是 Mango 的 KV 技术底座，提供底层 KV store 抽象和可选 capability bean 装配。

## 职责边界

| 概念 | 定义 | 示例 |
|------|------|------|
| `store` | 实际 KV 存储实现，只回答“底层用哪个存储” | `MemoryKvStore`、`RedisKvStore`、`JdbcKvStore` |
| `capability bean` | 基于 `IKvStore` 组装出的通用使用场景能力，只回答“哪些能力启用” | `ICache`、`ILocker`、`IRateLimiter`、`IIdempotent`、`IOutboxStore` |

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
| Capability | `ICache`、`ILocker`、`ICounter`、`IRateLimiter`、`IIdempotent`、`ITokenStore`、`IIdGenerator`、`IOutboxStore`、`IOutboxPublisher` |
| 辅助 | `ISerializer`、`IConverter` |
| 注解 | `@Cacheable`、`@RateLimit`、`@Idempotent`、`@Locker` |
| 枚举 | `KvStoreTypeEnum` |

## 配置前缀

当前统一前缀为 `mango.kv.*`。

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
      prefix: mango:kv
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
      outbox: false
```

配置、日志和文档统一使用 `mango.kv.store.type` 与 `jdbc`。JDBC 当前默认表名为 `infra_kv_entry`。

## Store 选择规则

| 配置 | 条件 | 注入结果 |
|------|------|----------|
| `mango.kv.store.type=memory` | 无额外依赖 | `MemoryKvStore` |
| `mango.kv.store.type=redis` | 存在 `RedissonClient` | `RedisKvStore` |
| `mango.kv.store.type=jdbc` | 存在 `JdbcTemplate` | `JdbcKvStore` |
| `mango.kv.store.type=auto` 或未配置 | 存在 `RedissonClient` | `RedisKvStore` |
| `mango.kv.store.type=auto` 或未配置 | 不存在 `RedissonClient` | `MemoryKvStore` |

说明：

- `auto` 不自动选择 jdbc，避免应用只因存在 DataSource 就把 KV store 切到数据库。
- `JdbcKvStore` 只依赖 `JdbcTemplate`，内部使用本地雪花算法生成表主键，不再依赖 Redis 发号。
- `JdbcKvStore` 会读取 `mango.kv.provider.jdbc.table-name`，默认表名为 `infra_kv_entry`，并校验表名只允许字母、数字和下划线。
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
| `outbox=true` | `IOutboxStore`、`IOutboxPublisher` | `KvOutboxStore`、`KvOutboxPublisher` |

每个 capability bean 都有 `@ConditionalOnMissingBean`，允许业务、测试或应用装配层覆盖。

### Outbox 可靠投递能力

Outbox 是 KV 底座能力，不是 MQ，也不替代 `mango-infra-event` 的事件语义层。它只负责把“要投递的消息”可靠落到当前 KV store，并提供 `claim / ack / nack` 生命周期，让上层事件基础设施或业务 worker 做实际分发。

启用方式：

```yaml
mango:
  kv:
    capability:
      enabled: true
      outbox: true
```

核心接口：

| 接口 | 职责 |
|------|------|
| `IOutboxPublisher` | 业务或事件层写入待投递消息 |
| `IOutboxStore` | 底层持久化与 `enqueue / claim / ack / nack` 生命周期 |
| `IOutboxDispatcher` | 分发 worker 扩展点，由事件层或应用层实现 |

消息字段建议：

| 字段 | 用途 |
|------|------|
| `eventType` | 事件类型，例如 `workflow.process.completed` |
| `businessType` | 业务类型，例如 `EXPENSE_REIMBURSEMENT` |
| `businessKey` | 业务主键，例如报销单号 |
| `aggregateId` | 本次申请、流程实例或聚合根 ID |
| `payload` | 给订阅方处理所需的最小事件数据，不承载完整业务快照 |
| `headers` | 租户、链路、操作者等上下文 |

使用原则：

- 业务数据仍由业务系统持有；Outbox 不保存完整申请快照或审批页面快照。
- 工作流完成、驳回、撤回等状态变化可以写入 Outbox，再由 `mango-infra-event` 或业务 worker 订阅处理。
- 消费方必须按 `eventType + businessType + businessKey + aggregateId` 做幂等处理。
- `nack` 会把消息重新放回待处理索引，并通过 `nextAttemptAt` 控制下一次重试时间。
- 当前实现复用 `IKvStore` 和 `IKvSortedSet`，因此 memory / redis / jdbc 三种 store 都可承载。生产多实例部署建议使用 redis 或 jdbc store。

Capability 实现不再按 memory / redis / jdbc 分裂。具体存储差异必须封装在 `IKvStore`
实现内：

| Store 语义 | 用途 | 要求 |
|-----------|------|------|
| `setIfAbsent(key, value, ttl)` | lock、idempotent、replay 防重 | 原子写入，已过期 key 视为不存在 |
| `set(key, value, ttl)` | cache、token、captcha 等可覆盖值 | 写入或覆盖，并刷新 TTL |
| `incrementBy(key, delta, window)` | counter、rate-limit、id-generator | 单 key 原子递增，支持正负 delta |
| `get/delete/exists` | 通用读取、删除、存在判断 | 必须遵守过期语义 |
| `IKvSortedSet.add(key, member, score, ttl)` | presence、心跳、延迟任务、滑动窗口索引 | member 唯一，按 score 排序，并刷新整个 sorted-set key TTL |
| `IKvSortedSet.rangeByScore/removeByScore/size` | 有序集合范围查询和清理 | score 范围为闭区间；limit <= 0 表示不显式限制返回条数 |

`put(key, value, ttl)` 语义等同于 `setIfAbsent`。新增代码不得使用 `put` 表达缓存覆盖写入，应使用 `set`。

`IKvSortedSet` 是 KV 体系下的通用有序集合能力，不直接暴露 Redis `ZSet` 命名。Redis 实现使用
ZSet，`add` 通过 Lua 保证 `ZADD + EXPIRE` 对同一个 key 原子执行；Memory 使用内存 score map；JDBC
使用 KV 表模拟 member 行。sorted-set 的 TTL 是集合 key 级 TTL，不是 Redis 原生 member TTL。需要 member
级过期时，调用方应使用 `score = expireAtMillis`，查询前用 `removeByScore(key, -inf, nowMillis)` 清理过期 member。

`IRateLimiter` 当前基于 `incrementBy` 实现固定窗口限流，不再宣称 token bucket。

## Redis Key 规范

自动装配出的 KV capability 默认会给业务 key 增加统一命名空间：

```text
mango:kv:{env}:{capability}:{biz-key}
```

如确实需要按应用隔离，可打开 `mango.kv.key.app-enabled=true`，格式变为：

```text
mango:kv:{env}:{app}:{capability}:{biz-key}
```

默认配置：

```yaml
mango:
  kv:
    key:
      enabled: true
      prefix: mango:kv
      env: default
      app-enabled: false
      app: app
```

能力段固定如下：

| 能力 | capability 段 | 示例 |
|------|---------------|------|
| `ICache` | `cache` | `mango:kv:prod:cache:user:10001` |
| `ILocker` | `lock` | `mango:kv:prod:lock:order:202604170001` |
| `ICounter` | `counter` | `mango:kv:prod:counter:sms:18800000000` |
| `IRateLimiter` | `rate-limit` | `mango:kv:prod:rate-limit:login:ip:127.0.0.1` |
| `IIdempotent` | `idempotent` | `mango:kv:prod:idempotent:payment:req-abc` |
| `ITokenStore` | `token` | `mango:kv:prod:token:access:sha256-xxx` |
| `IIdGenerator` | `idgen` | `mango:kv:prod:idgen:global` |
| `IOutboxStore` | `outbox` | `mango:kv:prod:outbox:message:uuid` |

约束：

- 业务只传业务段 key，例如 `user:#{#userId}`，不得手写 `mango:kv` 前缀。
- 默认不加应用名，保证同一环境内 infra KV 可以跨应用共享。
- 环境段用于隔离 dev/test/prod，避免共享 Redis 时串数据。
- `app-enabled=true` 仅用于明确需要应用级隔离的部署。
- 底层 `IKvStore` 只处理最终 key；统一前缀在 capability 层完成。

## 注解 Key 表达式

`KvCapabilityAspect` 支持以下 key 写法：

| 写法 | 示例 | 说明 |
|------|------|------|
| 静态字符串 | `user:static:key` | 不含 `#` 或 `@`，直接返回，无任何解析开销 |
| SpEL 模板 | `user:#{#query.userId}:#{#req.headers['X-Tenant']}` | 推荐写法，支持方法参数、命名空间变量和容器 bean |
| 直接 SpEL | `#userId`、`@tenantKey.prefix()` | 整个 key 为一个表达式时使用 |

**语法要求：**
- 新增 key 建议统一使用 `#{...}` 模板，例如 `user:#{#userId}:#{@tenantKey.prefix()}`。
- 方法参数直接用参数名访问，不额外加 namespace；对象参数可访问属性，例如 `#{#query.userId}`、`#{#command.tenantId}`。
- 如编译产物缺少参数名，可用 `#args[index]` 兜底，例如 `#{#args[0].userId}`。
- Web header/cookie 访问建议使用命名空间模板表达式，例如 `#{#req.headers['X-Tenant']}`、`#{#req.cookies['SESSION']}`。
- 不支持 `user:#userId` 这类内联占位写法；必须写成 `user:#{#userId}`。
- 编译后的 `SpelExpression` 被缓存，同一 key 表达式只解析一次

可用上下文变量：

- 方法入参：如 `#userId`、`#query.userId`（模板中写为 `#{#userId}`、`#{#query.userId}`）。
- `#args`：方法入参数组。
- `#req`：由 Web contributor 提供；非 Web 场景默认不存在。
- `#req.clientIp`：客户端 IP。
- `#req.headers`：原始请求 headers。
- `#req.cookies`：原始请求 cookies。
- `#req.request`：原始请求对象，仅 Web 场景存在。
- `#user`：预留给认证/安全模块提供，Web contributor 不负责维护。
- `#tenant`：预留给租户模块提供，Web contributor 不负责维护。
- `#dept`：预留给组织/部门模块提供，Web contributor 不负责维护。

KV 表达式上下文扩展规则：

- `mango-infra-kv-api` 定义 `KvContext`、`KvContextContributor`。
- `mango-infra-kv-core` 只消费 KV API 的表达式上下文协议，不直接依赖 Web/Servlet。
- `mango-infra-web` 当前只提供 `req` 命名空间变量，值为 `RequestContextSnapshot`，保留原始 request/header/cookie 与常用 request 字段。
- 用户、租户、部门上下文由对应模块提供，不在 Web contributor 中维护。
- `requestId`、`traceId` 属于观测链路字段，不默认进入 KV key 表达式变量。
- 后续 Security、Trace、RPC 等模块如需扩展 KV 表达式变量，应各自提供 contributor，不得把运行时技术依赖压进 `kv-core`。

## 参数校验口径

- `kv-api` 不新增 `jakarta.validation` 注解，避免扩大 API 依赖。
- 接口 Javadoc 定义 key、ttl、value 等参数契约。
- core 实现统一使用 `Require` 校验参数。
- infra 层不使用 `Require`，避免把业务异常语义带入技术底座。

## 禁止事项

- 业务模块不得直接依赖 `mango-infra-kv-core` 具体实现。
- 业务代码不得通过 if/else 选择 memory / redis / jdbc。
- 不得默认强启全部 capability bean。

## 参考文档

- [Phase 2 配置与装配规则](../../../mango-docs/plans/2026-04-17-phase-2-kv-configuration-rules.md)
- [后端模块级重构计划](../../../mango-docs/plans/2026-04-17-backend-module-by-module-refactor-plan.md)
