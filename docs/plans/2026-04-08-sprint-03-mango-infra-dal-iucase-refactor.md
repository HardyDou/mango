# Sprint 03: IUseCase 9 个接口定义

- 起始日期：2026-04-08
- 状态：执行中
- 所属任务：T3
- 关联 plan：`2026-04-07-sprint-00-mango-module-architecture-plan.md`

---

## 背景

T1 完成了 `mango-infra-dal` 的 4 层结构 + `IKvStore` 接口（T1 commit `c0b7df41`）。

T3 需在此基础上扩展为完整的 **IUseCase 体系**（9 个接口），覆盖缓存、锁、计数器、限流、防重、Token、ID 生成等通用能力。

> 参考：`plans/2026-04-07-sprint-00-mango-module-architecture-plan.md` Section 1.6 框架分层原则

---

## IUseCase 9 个接口定义

| # | 接口 | 职责 | 方法签名（初稿） |
|---|------|------|----------------|
| 1 | `ICache` | 通用缓存 | `void set(k, v, ttl) / String get(k) / boolean exists(k) / void del(k)` |
| 2 | `ILocker` | 分布式锁 | `boolean lock(k, ttl) / void unlock(k)` |
| 3 | `ICounter` | 原子计数器 | `long incr(k, delta, windowSec) / long get(k)` |
| 4 | `IRateLimiter` | 令牌桶限流 | `boolean tryAcquire(k, permits)` |
| 5 | `IIdempotent` | 防重复提交 | `boolean isDuplicate(k, windowSec) / void mark(k, windowSec)` |
| 6 | `ITokenStore` | Token 存储 | `void store(t, v, ttl) / String get(t) / void remove(t)` |
| 7 | `IIdGenerator` | ID 生成器 | `long nextId()` |
| 8 | `ISerializer` | JSON/Protobuf 序列化 | `String serialize(o) / <T> T deserialize(s, cl)` |
| 9 | `IConverter` | 对象映射 | `<T> T convert(s, cl)` |

---

## 接口放置规则

- **接口定义**（`*Api`/`*Service`）→ `-api` 子模块
- **业务实现**（`ServiceImpl`）→ `-core` 子模块
- **SPI 注入**（`@Conditional*`）→ `-starter` / `-starter-remote`

---

## 实施步骤

- [x] 1. 在 `mango-infra-dal/mango-infra-dal-api` 中定义 9 个接口
- [x] 2. 在 `mango-infra-dal/mango-infra-dal-core` 中提供 Memory 实现（默认）
- [x] 3. 在 `mango-infra-dal/mango-infra-dal-starter` 中配置 `@ConditionalOnProperty` SPI 注入
- [x] 4. AOP 注解支持（@Cacheable / @RateLimit / @Idempotent / @Locker）
- [ ] 5. 如有 Redis 增强实现，放 `mango-infra-redis` 中（待后续实现）

---

## AOP 注解（已实现）

| 注解 | 功能 | 异常 |
|------|------|------|
| `@Cacheable` | 缓存方法结果 | - |
| `@RateLimit` | 令牌桶限流 | `RateLimitExceededException` |
| `@Idempotent` | 防重复提交 | `DuplicateOperationException` |
| `@Locker` | 分布式锁 | `LockAcquisitionException` |

---

## 约束

- 业务模块禁止 `new RedisXivStoreImpl()` → 必须 `@Autowired ICache`
- TTL 是每个接口的第一等公民，禁止硬编码 TTL
- `IKvStore` 已完成，复用其 4 层结构

---

## 单元测试（已完成）

| 测试类 | 测试数 | 覆盖内容 |
|--------|--------|----------|
| `MemoryCacheTest` | 19 | set/get/exists/delete/过期/异常 |
| `MemoryLockerTest` | 17 | tryLock/unlock/过期/异常 |
| `MemoryCounterTest` | 14 | increment/get/过期/异常 |
| `MemoryRateLimiterTest` | 9 | tryAcquire/令牌桶重填/异常 |
| `MemoryIdempotentTest` | 13 | isDuplicate/mark/过期/异常 |
| `MemoryTokenStoreTest` | 17 | store/get/remove/过期/异常 |
| `MemoryIdGeneratorTest` | 5 | nextId/递增/唯一性 |
| `JsonSerializerTest` | 17 | serialize/deserialize/roundtrip |
| `JsonConverterTest` | 3 | convert/异常 |

**测试结果**: `mvn test` 全部通过 (178 tests in dal-core, 45 tests in dal-starter)

---

## 参考

- T1 commit：`c0b7df41`（已完成 4 层结构 + IKvStore）
- 接口设计参考：`plans/2026-04-07-sprint-00-mango-module-architecture-plan.md` Section 三
