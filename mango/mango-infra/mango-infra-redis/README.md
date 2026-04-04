# mango-infra-redis

> Redis 缓存基础设施 - Redisson 分布式客户端、连接池配置

## 已实现

- **RedissonClient 自动配置** - 分布式 Redis 客户端
- **连接池参数配置** - maxActive、maxIdle、minIdle、maxWait
- **密码认证支持** - 安全的密码配置方式
- **超时配置** - connectTimeout、timeout 毫秒级控制
- **`@ConfigurationProperties` 模式** - `mango.redis.*` 前缀

## 依赖

```xml
<dependency>
    <groupId>io.mango</groupId>
    <artifactId>mango-infra-redis-starter</artifactId>
</dependency>
```

## 配置属性

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `mango.redis.enabled` | `true` | 是否启用 |
| `mango.redis.host` | `localhost` | Redis 地址 |
| `mango.redis.port` | `6379` | Redis 端口 |
| `mango.redis.password` | - | 密码（无密码不配置） |
| `mango.redis.database` | `0` | 数据库编号 |
| `mango.redis.timeout` | `3000` | 超时时间（ms） |
| `mango.redis.pool.maxActive` | `8` | 最大连接数 |
| `mango.redis.pool.maxIdle` | `8` | 最大空闲连接 |
| `mango.redis.pool.minIdle` | `0` | 最小空闲连接 |
| `mango.redis.pool.maxWait` | `-1` | 最大等待时间（ms） |

## 使用示例

```yaml
mango:
  redis:
    enabled: true
    host: localhost
    port: 6379
    password: ${REDIS_PASSWORD:}
    database: 0
    timeout: 3000
    pool:
      maxActive: 16
      maxIdle: 8
      minIdle: 2
      maxWait: 3000
```

```java
@Autowired
private RedissonClient redissonClient;

// 获取分布式锁
RLock lock = redissonClient.getLock("my-lock");
lock.lock();
try {
    // 业务逻辑
} finally {
    lock.unlock();
}

// 获取分布式Map
RMap<String, Object> map = redissonClient.getMap("my-map");
```

## 待实现

| 功能 | 状态 | 说明 |
|------|------|------|
| 集群/哨兵模式 | 待开发 | 集群和哨兵配置支持 |
| RedisTemplate 封装 | 待开发 | 简化 String/Hash/List 操作 |
| 缓存注解 | 待开发 | @Cacheable 自动缓存 |
| 分布式计数器 | 待开发 | 统计计数组件 |
| 消息订阅 | 待开发 | Pub/Sub 功能封装 |

## 设计决策

- 使用 Redisson 而非原生 Lettuce，Redisson 提供了丰富的分布式数据结构（Map、Set、Lock 等）
- 所有配置通过 `RedisProperties` 绑定，零硬编码
- 连接池使用 Commons Pool3，与 Spring Boot 3.x 兼容
