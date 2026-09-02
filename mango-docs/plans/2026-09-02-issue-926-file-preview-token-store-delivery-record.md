# 标准交付记录

## 1. 元数据

- 任务 ID：GitHub Issue #926
- Issue：https://github.com/HardyDou/mango/issues/926
- 交付模式：STANDARD
- 需求影响：L2 - file preview 的短期 token 属于多实例共享状态；静默落到进程内 Map 会造成跨实例随机失效。
- 方案风险：L2 - 删除 starter fallback 后，缺少 `ITokenStore` 的错误从运行期随机失败改为启动期失败，影响既有错误配置的启动语义。
- 最终风险：L2
- 工作区决策：CREATE - `issue-926-file-preview-token-store` / `/Users/hardy/Work/mango-issue-926-file-preview-token-store`
- 启用能力：M01、M08、M09、M11、M12、M15

## 2. 目标与范围

- 目标：让 file preview 只消费 `ITokenStore` 契约，具体实现和 Memory、Redis、JDBC 选择统一归属 `mango-infra-kv-starter`。
- 成功条件：删除 `MemoryPreviewTokenStore`；正确配置时使用 infra-kv Bean；漏配 token capability 时启动失败；模块说明与实际行为一致。
- 处理范围：`mango-file-preview-starter`、`mango-file-preview-app` 装配测试、file preview/KV README、全仓生产 Java 相似实现扫描。
- 不处理范围：HTTP API、token payload、TTL、数据库表、权限、UI，以及扫描发现的其它模块独立问题。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| SR-926-01 | file preview 宿主应用 | `mango.kv.store.type=memory`，开启 token-store capability | `ITokenStore` 由 infra-kv 基于 `MemoryKvStore` 装配 | 不得出现 file preview 私有实现 | Spring app flow 断言 Bean 类型和名称 |
| SR-926-02 | file preview 宿主应用 | 启用 file preview，但关闭 KV capability | 启动期明确报告缺少 `ITokenStore` | 不得静默回退 JVM Map | Spring context 启动失败且根因指向 `ITokenStore` |
| SR-926-03 | Mango 维护者 | 扫描所有生产 Java 模块 | 找出自行实现/构造 infra-kv store/capability 的代码 | 测试替身和 infra-kv 自身实现不计入 | 反向搜索结果记录到本文件 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| TD-926-01 | SR-926-01、SR-926-02 | 删除 file preview 的 `ITokenStore` Bean 与内存实现；保留 core 对 API 契约的构造器注入 | `mango-file-preview-starter` | 恢复删除的 fallback，但会重新引入多实例不一致 |
| TD-926-02 | SR-926-01、SR-926-02 | 不增加 file preview 自有配置；继续使用现有 `mango.kv.*` 选择 store/capability | app 配置、README | 不适用，无新配置 |
| TD-926-03 | SR-926-03 | 扫描接口实现、Bean 工厂、具体实现构造和进程内短期状态；本 Issue 只修复同契约重复实现 | 全仓生产 Java | 不适用，只读扫描 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---:|---|---|
| IM-926-01 | TD-926-01 | 1 | `FilePreviewAutoConfiguration.java` | 私有 fallback 和只服务该实现的代码全部删除 |
| IM-926-02 | TD-926-01、TD-926-02 | 2 | `MangoFilePreviewAppFlowTest.java` | 正常装配和漏配 fail-closed 都有真实 Spring 上下文覆盖 |
| IM-926-03 | TD-926-02 | 3 | file preview/KV README | 接入、选择和排障说明一致 |
| IM-926-04 | TD-926-03 | 4 | 全仓扫描、定向 Maven 验证 | 无未说明同契约生产实现，直接模块门禁通过 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| SR-926-01 | M11 Spring app flow | `mvn -B -ntp -Dmaven.repo.local=.mango/m2/repository -Drevision=1.0.0-mango-009-SNAPSHOT -f mango/pom.xml -pl :mango-file-preview-app -am -Dtest=MangoFilePreviewAppFlowTest -Dsurefire.failIfNoSpecifiedTests=false test` | PASS：5 项，失败/错误/跳过均为 0；日志确认 `MemoryKvStore` 和 infra-kv `ITokenStore (TokenStore)` 完成装配 | `MangoFilePreviewAppFlowTest#filePreviewShouldUseInfraKvTokenStore` |
| SR-926-02 | M11 Spring fail-closed | 同上 | PASS：关闭 capability 的独立 Spring context 启动失败，根因为缺少 `io.mango.infra.kv.api.ITokenStore` | `MangoFilePreviewAppFlowTest#filePreviewShouldFailStartupWithoutInfraKvTokenStore` |
| SR-926-01 | M11/M12 真实宿主启动 | Node 22.23.1 下执行仓库内 `node mango-ui/packages/mango-cli/src/index.mjs dev start backend`，回读 `/actuator/health`、File Preview 登录/临时 token 入口及启动日志，随后执行 `dev stop mango-backend` | PASS：`mango-monolith-app` 在 `18009` 启动，MySQL/JDBC health 为 `UP`；实际装配 `JdbcKvStore` 和 infra-kv `ITokenStore (TokenStore)`；登录入口无凭据返回 401，无效源文件 token 返回业务码 180002；停止后端口已释放 | `.mango/run/logs/mango-backend.log`；数据库 `mango_dev_mango_issue_926_file_preview_token_store_009` |
| SR-926-03 | M09 反向搜索 | 搜索所有生产 Java 的 infra-kv capability 实现、Bean、core 实现 import 和具体 store/capability 构造；反查 `MemoryPreviewTokenStore` / `filePreviewTokenStore` | PASS：infra-kv 外生产实现 0，业务生产构造 0，旧 file preview 实现/Bean 0；仅 Maven 插件集成测试 fixture 构造 Memory store | 本节最终扫描结论 |
| SR-926-01~03 | M09 直接模块质量 | 依赖准备后执行不带 `-am/-amd` 的 `mvn ... -pl :mango-file-preview-starter,:mango-file-preview-app verify`；测试质量、Mock、README、源码事实和 diff 检查 | PASS：starter 14 项、app 5 项；测试质量 1 文件；Mock block/warn 0；两项 README 审计、`git diff --check` 全部通过 | Maven Reactor、PMO 检查器输出 |

## 7. 例外与剩余风险

- 根因：`MemoryPreviewTokenStore` 在 2026-05-25 的初始接入提交 `c3d33c46b` 中作为本地 fallback 引入；次日提交 `f80b6fbad` 已补齐 infra-kv 配置，但没有删除 fallback。后续设计已要求共享 KV，代码和设计因此长期不一致。正常配置的应用此前已因自动配置顺序优先使用 infra-kv Bean，实际缺陷是漏配 capability 时被 file preview 静默兜底。
- 同契约扫描：除 `mango-infra-kv` 自身外，所有生产 Java 均未实现 `IKvStore`、`ITokenStore`、`ICache`、`ILocker`、`ILeaseLocker`、`ICounter`、`IRateLimiter` 或 `IIdempotent`，也未直接构造 Memory/Redis/JDBC store 或 capability；`mango-maven-plugin/src/it/resource-baseline-package` 的命中属于插件集成测试 fixture，不是产品运行代码。
- 相似状态分类：`LoginAttemptTracker` 保留测试使用的内存构造器，但仓内生产装配强制注入 `IKvStore`，不构成本问题；微信/企业微信 access token 是可重新向上游获取的本地缓存；Realtime connection ticket 显式标记为 `@LocalCapabilityContract`。`CaptchaInterceptor.failedAttempts`、第三方 token 缓存和 Realtime ticket 的多实例语义需要各自单独评估，不能用本 Issue 的 `ITokenStore` 结论直接修改。
- 能力说明：已更新 File Preview README、KV README 和能力地图；前端入口、业务指南、PMO 规则及 `rules/index.json` 不受影响。
- 例外：未新增其它模块 Issue；扫描候选尚未完成各自入口、部署拓扑和兼容性分析，避免把独立问题错误并入 #926。
- 剩余风险：本任务不发布 Maven 新版本；真实宿主已验证 JDBC store，未连接 Redis 外部服务，也未准备登录账号和真实文件执行完整有效 token 预览。Memory 装配由 app flow 覆盖，store 多实现语义由 `mango-infra-test` 既有参数化与自动装配测试覆盖。
