# Infra Log 历史债务治理验收证据

## 1. 验收范围

- 模块：`mango-infra-log-api`、`mango-infra-log-starter`。
- 当前源码消费者：`mango-system-core/starter`、`mango-grid-layout-starter`、`mango-home-starter`。
- 行为：日志注解合同、属性绑定、Spring Boot 日志初始化、profile、级别、JSON/操作日志开关、滚动策略、MDC 和文件分流。
- 边界：本模块无数据库、Flyway、init/demo、菜单或浏览器页面；操作日志落库属于 System 消费者。

## 2. 治理前基线

| 项目 | 结果 |
|---|---|
| 模块测试 | 9/9，仅反射验证注解/枚举；测试错误地放在 Starter，而 API 自身为 0 测试 |
| 属性测试 | `mango-infra-test` 有 2 条 POJO 绑定测试，但没有验证 Logback 运行时 |
| 真实入口 | 无独立应用启动、无日志文件/MDC/分流验证 |
| 环境配置 | `LogProperties` 能绑定，但 XML 使用普通 `${mango.log.*}`，Spring 命令行与配置文件未进入 Logback context |
| JSON 开关 | `JSON_OUTPUT_ENABLED` 声明后从未引用，`dev` 设置 true 不产生 JSON 文件 |
| 操作日志开关 | 运行时读不到 `operation.enabled=false`，仍按默认 true 配置 appender |
| 公共合同 | 操作 logger 名称硬编码为注解类名，没有公开稳定常量；API README 包名错误 |
| 静态债务 | Checkstyle 2 条、SpotBugs 8 条 |

## 3. 缺陷红灯

生产 XML 修改前，两个独立子 JVM 启动真实 Spring Boot 应用形成稳定红灯：命令行指定 `root=ERROR` 实际仍为 `INFO`，`io.mango=TRACE` 实际仍为 `DEBUG`；`dev + json.enabled=true` 不创建 JSON 文件，`operation.enabled=false` 未按配置关闭。该流程直接读取生产 `logback-spring.xml`，不 mock Logger、Appender 或 Spring Environment。

## 4. 修复结果与兼容边界

| 债务类型 | 修复结果 | 兼容边界 |
|---|---|---|
| 配置只绑定不生效 | 全部 `mango.log.*` 改用 `springProperty` 从 Spring Environment 读取 | 配置前缀、默认值与 `LogProperties` getter/setter 不变 |
| JSON 开关失效 | `default/dev/local` 为 true 时追加 `FILE_JSON` | `test/prod` 原有 JSON 文件策略保持不变 |
| 操作开关失效 | `operation.enabled` 真实控制独立操作 logger/appender | 默认 true、文件名和 profile 格式不变 |
| 过期条件语法 | 使用 Logback `PropertyEqualityCondition`，移除弃用的 condition attribute 和 Janino 运行时依赖 | true/false 分支语义不变 |
| Logger 魔法字符串 | API 新增 `Loggers.OPERATION`，值保持原注解类 logger 名 | 既有直接使用原字符串的调用仍兼容 |
| API 边界 | 注解、枚举、logger 名声明本地能力合同；注解测试归属 API | Starter 继续传递 API，不改变消费者 import |
| 文档与静态债务 | 修正真实包名、profile/注解边界；Checkstyle 2→0、SpotBugs 8→0 | 不宣称 `@Log` 自动写文本文件或由本模块落库 |

## 5. 自动化用例

| 用例 ID | 优先级 | 层级 | 稳定契约 | 执行入口 | 状态 |
|---|---|---|---|---|---|
| TC-LOG-001 | P0 | 单元 | `@Log` retention/target/value/type 与 logger 名 | `LogAnnotationTest` | AUTOMATED |
| TC-LOG-002 | P1 | 单元 | `LogType` 已发布名称和顺序 | `LogTypeTest` | AUTOMATED |
| TC-LOG-003 | P0 | 接口 | 自动配置默认值与文档配置绑定 | `LogAutoConfigurationTest` | AUTOMATED |
| TC-LOG-004 | P0 | 入口流程 | 独立 dev Spring Boot 进程读取级别/开关/滚动配置并输出带 MDC 的 JSON | `LogbackRuntimeFlowTest` | AUTOMATED |
| TC-LOG-005 | P0 | 入口流程 | 独立 prod Spring Boot 进程分离常规、ERROR、operation JSON 文件 | `LogbackRuntimeFlowTest` | AUTOMATED |

## 6. 验证结果

| 层级 | 命令/入口 | 结果 | 结论 |
|---|---|---|---|
| 治理前行为基线 | API/Starter Maven test | 9/9，但仅注解/枚举 | INSUFFICIENT |
| 缺陷红灯 | 新增子 JVM 流程运行于旧 XML | 2/2 稳定失败 | DEFECT CONFIRMED |
| 治理后回归 | API/Starter 定向套件 | 8/8，fail/error/skip 0 | PASS |
| 入口流程 | `LogbackRuntimeFlowTest`，标签 `flow` + `infra-log` | 两个真实 Spring Boot 子进程、真实 Logback 文件 2/2 | PASS |
| 当前源码契约 | Log API/Starter + System Core/Starter + Grid Layout/Home Starter 同 reactor 编译 | PASS | PASS |
| 正式架构 | Log API/Starter + `mango-architecture-verification` partial reactor | dependency、ArchUnit、PMD 7、blocking 0 | PASS |
| 直接静态 | Checkstyle、SpotBugs | 0/0 | PASS |
| 测试质量 | `test-quality-check`、Mockito changed-only audit | 5 个测试资产 PASS；block=0、warn=0 | PASS |

## 7. Issue #522 防回归

最终生产消费者验证显式把当前 Log API、Starter 与 System、Grid Layout、Home 消费者放在同一 Maven reactor。尝试单独执行历史 `mango-infra-test` 的 LogPropertiesTest 时，测试聚合模块因读取旧 KV API JAR 在无关 KV 测试编译阶段失败；该事实不作为 Log 失败，也不通过改本地 JAR 规避，留待 `mango-infra-test` 自身历史债务治理时拆分测试归属。

## 8. 数据与未验证项

| 项目 | 结论 |
|---|---|
| 数据库/Flyway/init/demo | N/A |
| 文件数据 | JUnit 临时目录隔离，每个流程结束由测试框架清理 |
| 浏览器 UI | N/A；公共产品边界是应用日志初始化与文件输出 |
| 宿主自定义 logging.config | 宿主显式覆盖 Starter 配置属于既有扩展边界，不在本模块强制接管 |
| 全仓测试 | 未执行；按要求只验证 Log、真实入口与当前生产消费者 |

## 9. 风险分级

- 需求影响：L2。配置失效会造成生产日志量、格式、保留与操作日志开关不符合预期，但不直接修改业务数据。
- 方案风险：L2。修改共享日志初始化配置；默认值和 prod/test 既有输出策略保持不变，可按单提交回退。
- 最终风险：L2。由旧 XML 红灯、同一回归集、真实子 JVM 文件输出、当前源码消费者和架构门禁共同覆盖。
