# mango-infra-dal

## 职责

DAL（Data Access Layer）抽象层，通过 **IUseCase 接口体系**提供统一的通用能力：缓存、锁、计数器、限流、防重、Token、ID生成、序列化、对象转换。

## 技术实现

- 核心框架：Spring Boot 3.x + Redisson / JDBC
- 数据存储：Redis（Redisson）/ Database / Memory 自动选择
- 通信方式：SPI + `@ConditionalOnMissingBean` 注入

## 模块结构（4 层）

```
mango-infra-dal/
├── mango-infra-dal-api/              ← IUseCase 接口定义（9个）
├── mango-infra-dal-core/              ← MemoryXivStore + 9个 Memory 实现
├── mango-infra-dal-starter/           ← 本地调用启动器（@ConditionalOnMissingBean 注入）
└── mango-infra-dal-starter-remote/    ← Feign Client（微服务时跨进程调用）
```

## 核心接口（IUseCase 体系）

### IKvStore（底层KV存储）

| 方法 | 说明 |
|------|------|
| `put(key, value, expireSeconds)` | 写入 key-value，返回是否新增成功 |
| `get(key)` | 读取 key，过期或不存在返回 null |
| `increment(key, windowSeconds)` | 计数器递增，自动设置滚动窗口过期时间 |
| `delete(key)` | 删除 key |
| `exists(key)` | 检查 key 是否存在（不考虑过期） |

### ICache（通用缓存）

| 方法 | 说明 |
|------|------|
| `set(key, value, ttlSeconds)` | 设置缓存，TTL 必须为正数 |
| `get(key)` | 获取缓存，过期返回 null |
| `exists(key)` | 检查 key 是否存在（考虑过期） |
| `delete(key)` | 删除缓存 |

### ILocker（分布式锁）

| 方法 | 说明 |
|------|------|
| `tryLock(key, ttlSeconds)` | 尝试获取锁，TTL 必须为正数 |
| `unlock(key)` | 释放锁 |

### ICounter（原子计数器）

| 方法 | 说明 |
|------|------|
| `increment(key, delta, windowSeconds)` | 递增/递减计数器，自动设置滚动窗口 |
| `get(key)` | 获取当前值 |

### IRateLimiter（令牌桶限流）

| 方法 | 说明 |
|------|------|
| `tryAcquire(key, permits)` | 尝试获取令牌 |

### IIdempotent（防重复提交）

| 方法 | 说明 |
|------|------|
| `isDuplicate(key, windowSeconds)` | 检查是否重复 |
| `mark(key, windowSeconds)` | 标记为已处理 |

### ITokenStore（Token 存储）

| 方法 | 说明 |
|------|------|
| `store(token, value, ttlSeconds)` | 存储 Token |
| `get(token)` | 获取 Token 值 |
| `remove(token)` | 删除 Token |

### IIdGenerator（ID 生成器）

| 方法 | 说明 |
|------|------|
| `nextId()` | 生成下一个唯一 ID |

### ISerializer（JSON 序列化）

| 方法 | 说明 |
|------|------|
| `serialize(object)` | 对象序列化为 JSON 字符串 |
| `deserialize(content, classType)` | JSON 字符串反序列化 |

### IConverter（对象转换）

| 方法 | 说明 |
|------|------|
| `convert(source, classType)` | 对象类型转换（通过 JSON 中转） |

## 依赖关系

```
本模块依赖：
├── mango-common                    ← 基础对象
├── mango-infra-redis-starter      ← RedissonClient
└── mango-infra-db-starter         ← DataSource / JdbcTemplate

本模块被依赖：
├── mango-*-starter               ← 业务模块的本地 starter
└── mango-*-starter-remote        ← 微服务部署时的远程调用
```

## 配置项

所有配置收敛在 `mango.dal.*` 下，Redis 连接兼容 Spring 标准 `spring.redis.*`。

### 配置结构

```yaml
mango:
  dal:
    type: auto/redis/db/memory
    provider:
      redis:
        host: localhost
        port: 6379
        password:
        database: 0
        timeout: 3000
        pool:
          maxActive: 8
          maxIdle: 8
          minIdle: 0
          maxWait: -1
      db:
        tableName: sys_kv_record
        url: jdbc:postgresql://localhost:5432/mydb
        username:
        password:
        driver:
        druid:
          initialSize: 5
          maxActive: 20
          minIdle: 5
          maxWait: 60000
          validationQuery: SELECT 1
        hikari:
          maxPoolSize: 10
          minIdle: 5
          connectionTimeout: 30000
          idleTimeout: 600000
          maxLifetime: 1800000
      memory:
        cleanupIntervalMinutes: 1
```

### 配置项说明

#### mango.dal — 主配置

| 配置 | 默认值 | 说明 |
|------|--------|------|
| `mango.dal.type` | `auto` | 存储类型：`auto` / `redis` / `db` / `memory` |

#### mango.dal.provider.redis — Redis 连接配置

| 配置 | 默认值 | 说明 |
|------|--------|------|
| `host` | `localhost` | 服务器地址 |
| `port` | `6379` | 端口 |
| `password` | - | 密码 |
| `database` | `0` | DB 索引 |
| `timeout` | `3000` | 连接超时（ms） |
| `pool.maxActive` | `8` | 最大连接数 |
| `pool.maxIdle` | `8` | 最大空闲连接 |
| `pool.minIdle` | `0` | 最小空闲连接 |
| `pool.maxWait` | `-1` | 等待时间（ms） |

#### mango.dal.provider.db — 数据库配置

| 配置 | 默认值 | 说明 |
|------|--------|------|
| `tableName` | `sys_kv_record` | KV 存储表名 |
| `url` | - | JDBC URL（fallback 到 spring.datasource.url） |
| `username` | - | 用户名（fallback 到 spring.datasource.username） |
| `password` | - | 密码（fallback 到 spring.datasource.password） |
| `driver` | - | 驱动类（fallback 到 spring.datasource.driver-class-name） |
| `druid.initialSize` | `5` | 初始连接数 |
| `druid.maxActive` | `20` | 最大连接数 |
| `druid.minIdle` | `5` | 最小空闲连接 |
| `druid.maxWait` | `60000` | 最大等待时间（ms） |
| `druid.validationQuery` | `SELECT 1` | 验证查询 |
| `druid.testWhileIdle` | `true` | 空闲时检测 |
| `hikari.maxPoolSize` | `10` | 最大连接数 |
| `hikari.minIdle` | `5` | 最小空闲连接 |
| `hikari.connectionTimeout` | `30000` | 连接超时（ms） |
| `hikari.idleTimeout` | `600000` | 空闲超时（ms） |
| `hikari.maxLifetime` | `1800000` | 最大生命周期（ms） |

#### mango.dal.provider.memory — 内存配置

| 配置 | 默认值 | 说明 |
|------|--------|------|
| `cleanupIntervalMinutes` | `1` | 过期 key 清理间隔（分钟） |

---

### 兼容规则

**Redis 配置优先级（从高到低）：**
```
mango.dal.provider.redis.*  >  mango.redis.*  >  spring.redis.*  >  内置默认值
```

**数据库配置：**
- 优先使用 `mango.dal.provider.db.*`
- Druid 参数 fallback 到 `spring.datasource.druid.*`
- Hikari 参数 fallback 到 `spring.datasource.hikari.*`
- 基础参数 fallback 到 `spring.datasource.*`
- 天然兼容 `spring.datasource.*` 和 `dynamic-datasource` 多数据源

---

## 配置示例

### RedisXivStore — 默认配置（零配置接入 Spring 标准）

```yaml
# 使用 Spring 标准前缀，DAL 自动兼容
spring:
  redis:
    host: 10.0.0.1
    port: 6379
    password: redis123
    jedis:
      pool:
        max-active: 16
        min-idle: 4

mango:
  dal:
    type: redis
```

### RedisXivStore — 使用 DAL 专属前缀

```yaml
# mango.dal.provider.redis.* 优先级最高
mango:
  dal:
    type: redis
    provider:
      redis:
        host: 10.0.0.1
        port: 6379
        pool:
          maxActive: 32
```

### DbXivStore — 使用 DAL 专属配置（ Druid 连接池）

```yaml
mango:
  dal:
    type: db
    provider:
      redis:
        host: 10.0.0.1
        port: 6379
      db:
        url: jdbc:postgresql://localhost:5432/mydb
        username: postgres
        password: postgres
        druid:
          initialSize: 5
          maxActive: 20
          minIdle: 5
```

### DbXivStore — 使用 HikariCP 连接池

```yaml
mango:
  dal:
    type: db
    provider:
      redis:
        host: 10.0.0.1
        port: 6379
      db:
        url: jdbc:postgresql://localhost:5432/mydb
        username: postgres
        password: postgres
        hikari:
          maxPoolSize: 20
          minIdle: 5
```

### DbXivStore — 兼容 dynamic-datasource

```yaml
# 使用 spring.datasource.dynamic.* 配置多数据源
spring:
  datasource:
    dynamic:
      primary: master
      datasource:
        master:
          url: jdbc:postgresql://localhost:5432/mydb
          username: postgres
          password: postgres
  redis:
    host: 10.0.0.1
    port: 6379

mango:
  dal:
    type: db
```

### MemoryXivStore — 零配置

```yaml
mango:
  dal:
    type: memory
```

### 显式指定类型

```yaml
# 强制 RedisXivStore（无 RedissonClient 则启动失败）
mango:
  dal:
    type: redis

# 强制 DbXivStore（需 DataSource + RedissonClient）
mango:
  dal:
    type: db

# 强制 MemoryXivStore（单机 / 测试环境）
mango:
  dal:
    type: memory
```

## SPI 注入机制

### IKvStore 注入

| 条件 | 效果 |
|------|------|
| `mango.dal.type=redis` | 强制使用 RedisXivStore（无 RedissonClient 则启动失败） |
| `mango.dal.type=db` | 强制使用 DbXivStore（需同时有 DataSource 和 RedissonClient） |
| `mango.dal.type=memory` | 强制使用 MemoryXivStore |
| `mango.dal.type=auto`（默认） | RedissonClient 存在 → RedisXivStore；否则 → MemoryXivStore |

> **自动检测不会级联到 DbXivStore**。如果需要使用 DbXivStore，必须显式设置 `type=db`。

### IUseCase 接口注入

所有 9 个 IUseCase 接口通过 `@ConditionalOnMissingBean` 自动注入 **Memory 实现**：

| 接口 | 默认实现 |
|------|---------|
| `ICache` | MemoryCache |
| `ILocker` | MemoryLocker |
| `ICounter` | MemoryCounter |
| `IRateLimiter` | MemoryRateLimiter |
| `IIdempotent` | MemoryIdempotent |
| `ITokenStore` | MemoryTokenStore |
| `IIdGenerator` | MemoryIdGenerator |
| `ISerializer` | JsonSerializer |
| `IConverter` | JsonConverter |

> 注意：**无运行时降级，无自动级联**。通过 Spring `@ConditionalOnMissingBean` 在启动时确定用哪个实现，部署拓扑变更需重启服务。

## AOP 注解支持

除了编程式调用（`@Autowired ICache`），还支持注解方式使用：

### 注解列表

| 注解 | 功能 | 切面 |
|------|------|------|
| `@Cacheable` | 缓存方法结果 | CacheAspect |
| `@RateLimit` | 令牌桶限流 | RateLimitAspect |
| `@Idempotent` | 防重复提交 | IdempotentAspect |
| `@Locker` | 分布式锁 | LockerAspect |

### @Cacheable

```java
@Cacheable(key = "user:#userId", ttl = 3600)
public User getUser(String userId) {
    // 首次调用执行方法，结果缓存
    // 后续调用直接返回缓存结果
    return userRepository.findById(userId);
}
```

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `key` | String | - | 缓存 key，支持 SpEL `#参数名` |
| `ttl` | long | 3600 | 过期时间（秒） |
| `cacheValue` | boolean | true | 是否缓存返回值 |

### @RateLimit

```java
@RateLimit(key = "api:#userId", permits = 100)
public Result apiEndpoint(String userId) {
    // 超过 100 次/秒 限制时抛出 RateLimitExceededException
    return doSomething();
}
```

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `key` | String | - | 限流 key，支持 SpEL |
| `permits` | int | 1 | 每次请求消耗的令牌数 |

### @Idempotent

```java
@Idempotent(key = "order:#orderNo", window = 60)
public void createOrder(String orderNo) {
    // 60 秒内相同 orderNo 只允许执行一次
    // 重复调用抛出 DuplicateOperationException
    orderService.submit(orderNo);
}
```

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `key` | String | - | 防重 key，支持 SpEL |
| `window` | long | 60 | 有效时间窗口（秒） |

### @Locker

```java
@Locker(key = "order:#orderId", ttl = 30)
public void processOrder(Long orderId) {
    // 获取分布式锁后执行，30 秒自动释放
    // 锁获取失败抛出 LockAcquisitionException
    orderService.process(orderId);
}
```

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `key` | String | - | 锁 key，支持 SpEL |
| `ttl` | long | 30 | 锁 TTL（秒） |

### SpEL 表达式

注解的 `key` 属性支持 SpEL 表达式，使用 `#参数名` 引用方法参数：

```java
// 单参数
@Cacheable(key = "user:#userId")
public User getUser(String userId) { ... }

// 多参数
@Locker(key = "order:#userId:#orderId")
public void updateOrder(String userId, Long orderId) { ... }

// 直接值（无 SpEL）
@RateLimit(key = "api:global", permits = 1000)
public Result globalLimit() { ... }
```

### 异常处理

| 注解 | 异常类型 | 说明 |
|------|---------|------|
| `@RateLimit` | `RateLimitExceededException` | 超过限流阈值 |
| `@Idempotent` | `DuplicateOperationException` | 检测到重复操作 |
| `@Locker` | `LockAcquisitionException` | 锁获取失败 |

## 约束（强制）

- ✅ 必须通过 SPI 注入使用 `IKvStore` / `ICache` 等 IUseCase 接口
- ✅ 禁止直接 `new MemoryCache()` / `new RedisXivStore()` 等实现类
- ✅ TTL 必须显式传入（每个方法参数）
- ✅ TTL / windowSeconds 必须为正数（不得为 0 或负数）
- ❌ 禁止硬编码 TTL
- ❌ 禁止在单机部署使用 RedisXivStore（微服务才用）
- ❌ type=db 需同时有 RedissonClient（用于 ID 生成）

## 使用示例

```java
@Autowired
private ICache cache;

@Autowired
private ILocker locker;

@Autowired
private ICounter counter;

// 缓存
cache.set("user:1", "John", 3600);
String user = cache.get("user:1");

// 分布式锁
if (locker.tryLock("order:create", 30)) {
    try {
        // 业务逻辑
    } finally {
        locker.unlock("order:create");
    }
}

// 计数器（限流）
long count = counter.increment("api:rate:user1", 1, 60);
if (count > 100) {
    throw new RateLimitException();
}
```
