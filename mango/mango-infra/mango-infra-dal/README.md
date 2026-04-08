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

| 配置 | 默认值 | 说明 |
|------|--------|------|
| `mango.dal.kvstore.type` | `auto` | 存储类型：auto/redis/db/memory |

### 配置示例

```yaml
# 强制使用 RedisXivStore（无 RedissonClient 则启动失败）
mango:
  dal:
    kvstore:
      type: redis

# 强制使用 DbXivStore（无 DataSource 则启动失败）
mango:
  dal:
    kvstore:
      type: db

# 强制使用 MemoryXivStore（无依赖）
mango:
  dal:
    kvstore:
      type: memory

# 自动检测（默认）：RedissonClient → DataSource → MemoryXivStore
mango:
  dal:
    kvstore:
      type: auto
```

## 使用示例

### Backend

```java
@Autowired
private IKvStore kvStore;

// 防重复提交
if (!kvStore.put("idempotent:" + requestId, "1", 3600)) {
    throw new BizException("请求已提交，请勿重复提交");
}

// 限流
long count = kvStore.increment("rate:" + userId, 60);
if (count > 100) {
    throw new BizException("请求过于频繁，请稍后再试");
}
```

## SPI 注入机制

| 条件 | 效果 |
|------|------|
| `mango.dal.kvstore.type=redis` | 强制使用 RedisXivStore（无 RedissonClient 则启动失败） |
| `mango.dal.kvstore.type=db` | 强制使用 DbXivStore（无 DataSource 则启动失败） |
| `mango.dal.kvstore.type=memory` | 强制使用 MemoryXivStore |
| 不配置 type | 自动检测：RedissonClient → DataSource → MemoryXivStore |

> 注意：**无运行时降级**。通过 Spring `@ConditionalOnBean` / `@ConditionalOnMissingBean` 在启动时确定用哪个实现，部署拓扑变更需重启服务。

## 约束（强制）

- ✅ 必须通过 SPI 注入使用 `IKvStore`
- ✅ 禁止直接 `new RedisXivStore()` 等实现类
- ✅ TTL 必须显式传入（每个方法参数）
- ❌ 禁止硬编码 TTL
- ❌ 禁止在单机部署使用 RedisXivStore（微服务才用）
