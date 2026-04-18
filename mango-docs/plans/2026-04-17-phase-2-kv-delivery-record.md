# Phase 2 交付记录

## 范围

- 本次处理模块：`mango-infra-kv`。
- 必要下游适配：`mango-captcha-starter` 测试侧移除对 `mango-infra-kv-core` 具体实现的依赖。
- 本次未处理模块：`mango-infra-redis` 中历史 `mango.dal.*` 兼容读取、其它 infra 模块、platform 业务逻辑、app 装配边界。

## 完成项

- [x] Review 改进计划已执行：先收口 Phase 1 遗留 `PageQuery` 序列化，再处理 Phase 2 KV 命名、配置、装配顺序、key 表达式和 core 分包。
- [x] 质检方案已执行：命名残留搜索、配置前缀搜索、业务模块 direct core 依赖搜索、全量 compile、captcha 下游 test-compile。
- [x] 已盘点 `mango-infra-kv-api` 接口、注解与枚举。
- [x] 已盘点 `mango-infra-kv-core` memory / redis / jdbc 三类实现。
- [x] 已盘点 `mango-infra-kv-starter` 自动配置和配置项。
- [x] 已输出 KV 配置与装配规则：`mango-docs/plans/2026-04-17-phase-2-kv-configuration-rules.md`。
- [x] 已统一当前推荐配置前缀为 `mango.kv.store.type`，保留 `mango.kv.type` 作为兼容入口。
- [x] 已将 store 类型口径统一为 `auto` / `memory` / `redis` / `jdbc`，保留 `db` 作为兼容别名。
- [x] 已将 provider 新配置口径统一为 `mango.kv.provider.jdbc.*`，保留 `mango.kv.provider.db.*` 作为兼容入口。
- [x] 已拆分 `IKvStore` 选择逻辑与 capability bean 默认装配逻辑。
- [x] 已为 store 选择增加明确的 `@ConditionalOnBean` / `@ConditionalOnMissingBean` / `@ConditionalOnExpression` 条件。
- [x] 已将 capability bean 改为显式 opt-in：`mango.kv.capability.enabled=true` 且单项能力开关为 true 时才装配。
- [x] 已将 `DalAspect` 重命名为 `KvCapabilityAspect`；Aspect 允许随 starter 加载，具体注解执行时按所需 capability bean 做运行期校验，避免部分能力集场景失效。
- [x] 已增强 `KvCapabilityAspect` key 解析：动态 key 统一使用 SpEL 模板或直接 SpEL 表达式，支持 Spring Bean 解析，并通过 `RequestContextContributor` 消费请求上下文变量；不再扩展 `user:#userId` 这类非标准内联占位写法。
- [x] 已将请求上下文增强协议上提到 `mango-common`，`KvCapabilityAspect` 通过 `RequestContextContributor` 消费扩展属性。
- [x] 已将 JDBC 默认表名统一为 `infra_kv_entry`，并使 `mango.kv.provider.jdbc.table-name` 真正参与 `JdbcKvStore` SQL 构造。
- [x] 已新增 Flyway `V2__rename_kv_record_to_infra_entry.sql`，将历史 `sys_kv_record` 前向迁移到 `infra_kv_entry`。
- [x] 已确认 `infra_` 前缀用于基础设施技术表命名，避免与业务系统表混淆。
- [x] 已将 `mango-infra-kv-core` 按实现方案分包为 `memory` / `redis` / `jdbc` / `support` / `aspect`。
- [x] 已为 `KvStoreAutoConfiguration` 增加自动配置顺序，确保晚于 Redis、DB、Spring JDBC/JdbcTemplate 与 Redisson 自动配置。
- [x] 已更新 `mango-infra-kv/README.md`、相关 POM description 和 `mango-docs/index.md`。
- [x] 已按阶段处理流程补齐模块 README，覆盖职责边界、API 清单、配置前缀、store 选择、capability 装配、参数校验口径和禁止事项。
- [x] 已移除 `mango-captcha-starter` test scope 对 `mango-infra-kv-core` 的直接依赖，测试改用本地 `IKvStore` fake。

## 主动不做项

- [x] 事项：不修改 `mango-infra-redis` 中读取 `mango.dal.provider.redis.*` 的历史兼容逻辑。
  原因：Phase 2 写入范围是 `mango-infra-kv`；`mango-infra-redis` 属其它 infra 模块，后续 Phase 可单独收口。
- [x] 事项：不移动 `mango-infra-kv-core/src/main/resources/db/migration/dal` 下的历史迁移路径。
  原因：移动 Flyway 迁移路径可能影响已有迁移发现规则；本 Phase 只收口当前配置与装配规则。
- [x] 事项：不把 `Require` 用于 KV 参数校验。
  原因：`Require` 抛 `BizException`，属于业务契约断言；KV infra 参数错误继续使用 `IllegalArgumentException`。
- [x] 事项：不在 `kv-api` 方法上新增 `jakarta.validation` 注解。
  原因：避免把 Bean Validation 依赖扩进基础契约；API 通过 Javadoc 定义参数契约，core 实现负责运行时校验。
- [x] 事项：不改变 `JdbcKvStore` 仍依赖 `RedissonClient` 生成递增 ID 的实现。
  原因：该行为属于实现语义调整，Phase 2 只明确条件，不重写 jdbc store 算法。

## 被动适配

- [x] `mango-captcha-starter` 测试从 `MemoryKvStore` 改为测试内联 `IKvStore` fake，避免业务模块测试直接依赖 kv-core 具体实现。
- [x] `mango-captcha-api` 显式声明 `jakarta.validation-api`，承接 Phase 1 从 `mango-common` 移除 validation 依赖后的 API 入参校验注解编译需求。
- [x] Phase 1 review 补丁：`PageQuery` 增加 `Serializable` 与 `serialVersionUID`，保持 query DTO 可跨序列化边界使用；不改为 Command，避免把读模型分页条件误归类为写操作命令。
- [x] `mango-infra-test` 中 KV core 测试包名随 core 实现分包同步调整。
- [x] `JdbcKvStore` 增加表名白名单校验，并补充自定义表名/非法表名测试。
- [x] `mango-infra-web` 提供 servlet request/header/cookie 的请求上下文 contributor，替代 `kv-core` 直接依赖 servlet API。

## 验证结果

- Phase 2 原始验收：`mvn -q -DskipTests compile`：通过。
- `mvn -q -pl mango-platform/mango-captcha/mango-captcha-starter -am -DskipTests test-compile`：通过。
- `mvn -q -pl mango-infra/mango-infra-test -am -DskipTests test-compile`：通过。
- 本次 review 改进回归：`mvn -q -pl mango-common,mango-infra/mango-infra-kv/mango-infra-kv-api,mango-infra/mango-infra-kv/mango-infra-kv-core -am clean compile`：通过。
- 本次 review 改进回归：`mvn -q -pl mango-infra/mango-infra-kv -am clean compile`：待本轮收尾后复核。
- 本次 review 改进回归：`mvn -q -pl mango-infra/mango-infra-web -am -DskipTests test-compile`：通过。
- `mvn test`：未执行；Phase 2 验收命令未要求。
- 当前工作区全量 `mvn -q -DskipTests compile`：被 `mango-gateway-core` 缺失 Spring 相关编译依赖阻断，非本次 `common/kv/web` 重构引入的问题，也不在本次写入范围内。
- 其它搜索/检查命令：
  - `rg -n "mango-infra-kv-core" mango/mango-platform mango/mango-app --glob 'pom.xml' --glob '!**/target/**'`：无命中。
  - `rg -n "io\\.mango\\.infra\\.kv\\.core" mango/mango-platform mango/mango-app --glob '*.java' --glob '!**/target/**'`：无命中。
  - `rg -n "DalAspect|mango\\.dal|DAL store|DAL annotations" mango/mango-infra/mango-infra-kv mango-docs/plans/2026-04-17-phase-2-*.md --glob '!**/target/**'`：仅作为历史兼容、重命名说明或禁止新增说明出现。

## 遗留问题

- [x] `mango-infra-redis` 仍包含 `mango.dal.provider.redis.*` 历史兼容读取逻辑，非 Phase 2 写入范围，后续 infra 配置统一时处理。
- [x] `JdbcKvStore` 当前仍用 `RedissonClient` 生成数据库记录 ID，Phase 2 已明确条件，是否改为 DB 原生 ID 或独立 ID generator 留给后续实现优化。
- [x] `db/migration/dal` 历史目录名未移动，避免迁移路径风险；如后续统一迁移目录，需要单独评估 Flyway 兼容。

## 下一 Phase 前置条件

- [x] Phase 3 可以在 KV store/capability 装配边界已明确的基础上进入 `mango-infra-realtime`。
- [x] 后续业务模块不得直接依赖 `mango-infra-kv-core`；只能依赖 `mango-infra-kv-api` 或通过 starter 装配。
- [x] 后续如启用 cache/lock/rate-limit/idempotent 注解能力，必须显式打开对应 `mango.kv.capability.*` 开关。
