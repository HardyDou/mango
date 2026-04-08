# mango-infra-dal

## 职责

DAL（Data Access Layer）抽象层，通过 `IKvStore` 接口提供统一的 KV 存储能力，用于防重复、防抖、限流等场景。

## 技术实现

- 核心框架：Spring Boot 3.x + Redisson / JDBC
- 数据存储：Redis（Redisson）/ Database / Memory 自动选择
- 通信方式：SPI + `@ConditionalOnProperty` 注入

## 模块结构（4 层）

```
mango-infra-dal/
├── mango-infra-dal-api/              ← IKvStore 接口定义
├── mango-infra-dal-core/              ← RedisXivStore / DbXivStore / MemoryXivStore 实现
├── mango-infra-dal-starter/           ← 本地调用启动器（@ConditionalOnBean 注入）
└── mango-infra-dal-starter-remote/    ← Feign Client（微服务时跨进程调用）
```

## 核心接口

### IKvStore

| 方法 | 说明 |
|------|------|
| `put(key, value, expireSeconds)` | 写入 key-value，返回是否新增成功 |
| `get(key)` | 读取 key，过期或不存在返回 null |
| `increment(key, windowSeconds)` | 计数器递增，自动设置滚动窗口过期时间 |
| `delete(key)` | 删除 key |
| `exists(key)` | 检查 key 是否存在（不考虑过期） |

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

| 条件 | 效果 |
|------|------|
| `mango.dal.type=redis` | 强制使用 RedisXivStore（无 RedissonClient 则启动失败） |
| `mango.dal.type=db` | 强制使用 DbXivStore（需同时有 DataSource 和 RedissonClient） |
| `mango.dal.type=memory` | 强制使用 MemoryXivStore |
| `mango.dal.type=auto`（默认） | RedissonClient 存在 → RedisXivStore；否则 → MemoryXivStore |

> **自动检测不会级联到 DbXivStore**。如果需要使用 DbXivStore，必须显式设置 `type=db`。

> 注意：**无运行时降级，无自动级联**。通过 Spring `@ConditionalOnBean` / `@ConditionalOnMissingBean` 在启动时确定用哪个实现，部署拓扑变更需重启服务。

## 约束（强制）

- ✅ 必须通过 SPI 注入使用 `IKvStore`
- ✅ 禁止直接 `new RedisXivStore()` 等实现类
- ✅ TTL 必须显式传入（每个方法参数）
- ✅ `expireSeconds` / `windowSeconds` 必须为正数
- ❌ 禁止硬编码 TTL
- ❌ 禁止在单机部署使用 RedisXivStore（微服务才用）
- ❌ type=db 需同时有 RedissonClient（用于 ID 生成）
