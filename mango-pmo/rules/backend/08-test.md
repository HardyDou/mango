# 后端测试规范

## 1. 基本要求

- 有代码改动，必须有对应验证。
- 新增业务逻辑，必须补测试。
- 修复缺陷，必须补回归测试。
- 只写 happy path 不算完成。

## 2. 测试范围

至少覆盖：

- 正常场景
- 参数校验
- 边界值
- 异常场景
- 权限或安全约束

## 3. 测试层次

- 单元测试：验证方法和业务分支。
- 集成测试：验证模块协作、数据库、事务、接口契约。
- 只做有效测试：测行为、规则、边界、链路，不测 DTO、getter/setter、常量、简单透传、机械装配。
- 不要求每次都写 E2E。
- 是否需要前端或 E2E 测试，按需求影响面决定。

## 4. 分层归属

- 测试资产目录归属遵循 `mango-pmo/rules/04-test-assets.md`。
- `common`：测试放各模块 `src/test/java`，只测公共规则、通用工具、SPI 契约和高复用基础逻辑，以 `UnitTest` 为主。
- `infra`：模块内 `src/test/java` 只放少量复杂局部逻辑测试；跨 `api/support/core/starter/starter-remote` 的能力链路、单体真实链路、多服务协作链路统一放 `mango-infra-test`。
- `platform`：测试放各模块 `src/test/java`，主要验证业务规则、command/query/usecase/service 链路和模块内部协作，以 `IntegrationTest` 为主。
- `app`：测试放各 app 模块 `src/test/java`，主要验证端到端业务流程和关键用户主链路，以 `E2ETest` 为主。

## 5. 要测与不测

- 要测：行为分支、状态变化、参数校验、边界值、异常场景、权限或安全约束、并发风险、远程调用/注册/转发、多实现一致性、历史回归点。
- 不测：DTO、record、getter/setter、常量类、简单 constructor、简单 controller 透传、简单 delegation wrapper、只有 bean 存在与否的机械装配测试。

## 6. 命名规则

- 测试类名使用 `XxxTest`。
- 测试方法名使用 `方法_场景_结果`。
- 推荐用意图区分测试层次：`*UnitTest`、`*IntegrationTest`、`*E2ETest`。
- 测试类名如果以实现类型开头，测试物料必须和类名一致：
  - `RedisXxxTest` 必须使用 Redis 实现或真实 Redis 集成物料。
  - `JdbcXxxTest` 必须使用 JDBC 实现和数据库物料。
  - `MemoryXxxTest` 必须使用 Memory 实现。
- 通用能力或契约测试不要以实现类型命名，应按能力命名，例如 `CacheTest`、`LockerTest`、`RateLimiterTest`。
- 同一能力需要覆盖多个实现时，使用参数化测试或明确的 fixture 注入，每个参数必须能看出实际实现类型。

## 7. 测试物料

- 测试名声称验证 Redis/JDBC/Memory 时，不得用其他实现替代核心被测物料。
- mock、stub 只能用于隔离外部协作者，不能替代被测实现本身。
- 集成测试必须使用真实集成物料或等价容器/嵌入式环境；无法运行时必须在交付记录说明跳过原因。
- 能力测试应验证能力语义，不只验证一次方法返回值；至少覆盖成功、拒绝或失败、key 隔离、TTL/窗口、非法参数。

## 8. 提交前检查

至少执行与改动范围对应的检查：

- `mvn test`
- `mvn verify`
- `mvn pmd:check`
- `mvn checkstyle:check`
- `mvn mango:check`
- 涉及 KV、缓存、锁、限流、幂等、token、id 等能力时，执行 `mvn mango:check -Drule=test-fixture`

## 9. 交付要求

- 功能可运行
- 测试通过
- 验证结果可说明
- 说明测试覆盖了哪些实现物料，哪些实现因环境原因未执行

## 10. 禁止事项

- 不写测试直接提交业务改动
- 用 mock 覆盖真实集成问题
- 发现回归后不补测试
- `RedisXxxTest` 内实际使用 `MemoryKvStore`、`JdbcKvStore` 等错配物料
- `JdbcXxxTest` 内实际使用 `MemoryKvStore`、`RedisKvStore` 等错配物料
- 为了让测试通过而降低测试名、断言或物料真实性
