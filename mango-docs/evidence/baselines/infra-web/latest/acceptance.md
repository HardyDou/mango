# Infra Web 历史债务治理验收证据

## 1. 验收范围

- 模块：`mango-infra-web-api`、`mango-infra-web-support`、`mango-infra-web-starter`。
- 真实消费者：`mango-infra-feign-starter` 的内部调用拦截器，以及随机端口 Spring Boot 测试应用。
- 行为：请求上下文快照、Jackson 契约、异常响应、内部路径发现、HMAC 签名、时间戳与 nonce 防重放。
- 边界：本模块不拥有登录授权、业务权限、数据库、Flyway、初始化数据、菜单或管理页面。

## 2. 治理前基线

| 项目 | 结果 |
|---|---|
| 模块测试 | API 0、Support 0、Starter 16；16/16 通过 |
| 架构债务 | API 1 条 `MANGO-ARCH-TYPE-010`，本地 JVM 快照契约未声明 |
| Checkstyle | API 3、Support 1、Starter 24，共 28 条 |
| SpotBugs | Starter 15 条：配置绑定 Bean 暴露、Spring 协作者、路径条件空值及宽泛异常捕获 |
| 消费链 | 没有 Feign 与 Web 多值查询签名一致性、真实 HTTP 内部调用或异常边界入口流程测试 |
| 可复现缺陷 | 原子 nonce 占位返回 false 仍放行；两个并发同 nonce 请求可同时通过 |

## 3. 修复结果与兼容边界

| 债务类型 | 修复结果 | 兼容边界 |
|---|---|---|
| 签名漂移 | Web Support 提供唯一签名与查询规范化实现，Feign/Web 共同使用 | Header、HMAC-SHA256 算法和 payload 字段顺序不变 |
| 重复查询参数 | 按完整 `key=value` 项排序，客户端结构化查询与服务端原始查询结果一致 | 单值查询签名结果不变，重复参数由错误结果修正为一致结果 |
| 重放竞态 | 签名通过后以 `setIfAbsent` 原子占用 nonce，false 或异常均拒绝 | 正常首次请求仍放行，重复请求仍返回 403 |
| 启动安全窗口 | 路径未加载时 fail-closed；首次定时刷新延迟，路径集合原子替换 | 加载成功后的公开/内部路径行为不变；加载失败不再误放行 |
| 快照契约 | 标记 `LocalCapabilityContract`，Map 防御性复制 | record 组件、包名和调用方式不变 |
| 静态债务 | 精确处理 Spring 配置绑定/协作者告警，收窄反射异常，消除路径条件空值风险 | 配置前缀、默认值、Bean 条件和 HTTP 响应不变 |

## 4. 自动化验证

| 用例 ID | 优先级 | 层级 | 场景与稳定契约 | 数据/清理 | 执行入口 | 状态 |
|---|---|---|---|---|---|---|
| TC-WEB-001 | P0 | 单元 | 首次 nonce 原子占位成功，重复/失败/异常拒绝 | 每用例独立内存 KV | `InternalCallFilterTest` | AUTOMATED |
| TC-WEB-002 | P0 | 单元 | 两个并发同 nonce 请求只允许一个 | 唯一 nonce，执行器退出 | `InternalCallFilterTest` | AUTOMATED |
| TC-WEB-003 | P0 | 入口流程 | `@Inner` 路径合法签名放行一次、重放 403 | 独立 Spring 上下文内存 KV | `WebBoundaryIntegrationTest` | AUTOMATED |
| TC-WEB-004 | P1 | API/入口流程 | 业务、校验、404、405、数据库和系统异常保持 HTTP/R 契约 | 无持久数据 | `WebBoundaryIntegrationTest` | AUTOMATED |
| TC-WEB-005 | P1 | 集成 | Long/Java Time 经真实 MVC/Jackson 输出字符串 | 固定边界值 | `WebBoundaryIntegrationTest` | AUTOMATED |
| TC-WEB-006 | P1 | 单元/消费契约 | 原始查询与 Feign 重复参数得到相同签名 | 无持久数据 | `InternalCallSignatureTest`、`InternalCallFeignInterceptorTest` | AUTOMATED |

来源为本次用户确认的 Payment 历史债务政策：逻辑/接口兼容、单元与接口基线、真实服务入口验证和直接消费者
同 reactor 验证。测试不依赖执行顺序，不写共享数据库，Spring 上下文关闭后内存数据自动释放。

| 层级 | 命令/入口 | 结果 | 结论 |
|---|---|---|---|
| 治理前行为基线 | Web 三模块定向 `test` | API 0、Support 0、Starter 16；16/16 | PASS |
| 缺陷红灯 | 新增 nonce 原子占位与并发重放测试运行于旧实现 | 2/2 稳定失败：错误放行、并发放行 2 次 | 缺陷已复现 |
| 治理后回归 | Web 三模块与 Feign 消费模块同 reactor `test` | API 3、Support 5、Starter 24、Feign 4；36/36 | PASS |
| Web 边界入口流程 | `WebBoundaryIntegrationTest` | Java 21、随机端口 Tomcat 4/4；真实 Filter、MVC、Advice、Jackson、路径扫描和 KV 原子语义 | PASS |
| Context 消费入口流程 | `MangoContextPropagationE2ETest` | Java 21、随机端口 Tomcat 2/2；HTTP 上下文传播与线程复用隔离 | PASS |
| 直接静态 | Web 三模块 `checkstyle:checkstyle pmd:pmd spotbugs:spotbugs` | Checkstyle、PMD、SpotBugs 均为 0 | PASS |
| Partial reactor 架构门禁 | Web 三模块、Feign 直接消费者与 `mango-architecture-verification` | dependency、ArchUnit、PMD、blocking 均为 0；新增静态问题 0，工具失败 0 | PASS |
| 测试质量/Mock 审计 | `test-quality-check`、`audit-backend-test-mocks --report-only --changed-only` | 质量检查 PASS；block 0、warn 0 | PASS |

入口流程测试不 mock 被测 Web 组件，启动真实嵌入式 Tomcat，并通过真实 HTTP 客户端进入过滤器、Spring MVC、
全局异常处理和 Jackson。测试内 KV 是实现 `IKvStore` 原子契约的进程内存储，不代替外部 Redis/JDBC 的产品验收。

## 5. 数据、发布物与 Issue #522 防回归

| 项目 | 结果 |
|---|---|
| 数据库 | N/A；模块没有表、实体或 Flyway migration |
| 正式/demo 初始化 | N/A；模块没有资源注册、Runner 或初始化数据 |
| Controller/API 校验 | 随机端口 E2E 已验证 MVC 请求体异常、404、405 和业务/系统异常契约 |
| 真实消费者 | Feign 拦截器与 Web 服务端在同一 Maven reactor 使用当前 `InternalCallSignature` 源码 |
| 缓存隔离 | 不以旧版 Web Support JAR 替代当前修改；生产者和消费者同时编译、测试 |

## 6. 未验证项和边界

| 项目 | 原因 | 结论 |
|---|---|---|
| Fresh MySQL | Web 无数据库能力或迁移 | N/A，不创建伪表或伪 migration |
| Chromium 页面 | Web 是基础 HTTP starter，无独立管理页面 | N/A，随机端口 HTTP 是该能力的产品入口边界 |
| Redis/JDBC KV | KV 存储由 `mango-infra-kv` 所有，已在其模块单独治理 | 本批次验证 `IKvStore` 原子调用契约，不重复外部设施验收 |
| 全仓消费者 | 用户要求不重复全仓检查 | 只对直接 Feign/Context 消费链负责，不外推全仓通过 |

## 7. 业务开发交接

后续修改内部调用 Header、payload、查询编码、时间戳或 nonce 语义时，必须同时运行 Web 与 Feign 测试，且保留
多值查询和并发重放用例。新增 `@Inner` 路径时需验证宿主存在 `IKvStore`、收发两端 secret 一致，并分别测试
无签名、错误签名、首次合法请求和重复 nonce。修改异常处理或 Jackson 配置时，必须更新随机端口边界入口流程测试。

## 8. 风险分级

- 需求影响：L3。内部调用签名、防重放和内部路径识别位于跨服务安全边界。
- 方案风险：L3。修复涉及客户端/服务端共同签名契约以及并发原子语义。
- 交付结论：L3。已由缺陷红灯、相同回归集、真实 HTTP 入口流程、直接消费者同 reactor 和正式架构门禁共同覆盖。
