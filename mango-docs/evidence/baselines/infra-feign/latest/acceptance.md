# Infra Feign 历史债务治理验收证据

## 1. 验收范围

- 模块：`mango-infra-feign-starter`。
- 真实消费者：`mango-resource-starter-remote`、`mango-captcha-starter-remote`。
- 行为：Mango 上下文与 Authorization 透传、Servlet token 生命周期、模块动态目标、内部调用 HMAC、自动配置开关。
- 边界：本模块不定义业务 Feign Client，不拥有数据库、Flyway、初始化数据、菜单或管理页面。

## 2. 治理前基线

| 项目 | 结果 |
|---|---|
| 模块测试 | 4/4 通过，仅覆盖签名顺序、绝对 URL path 签名和两个目标重写场景 |
| Checkstyle | 9 条：2 个 logger 命名、4 个魔法数字、3 个内联条件 |
| SpotBugs | 0 条 |
| 旧 PMD CLI | PMD 6 无法读取 Java 21 class major 65，8 个处理错误；不作为代码通过证据 |
| 自动配置 | 未覆盖总开关、Servlet filter 条件和默认 Bean 装配 |
| 上下文 | 未覆盖 `MEMBER_ID`，未覆盖无 Authorization 时的线程残留 token |
| 入口流程 | 没有真实 Feign 到 HTTP 服务的模块目标、上下文和签名组合验证 |

## 3. 缺陷红灯

首批新增 13 个测试首次运行时 4 个稳定失败、9 个通过；随后新增的空白密钥边界单用例也在旧判断上稳定失败：

| 用例 | 治理前失败事实 |
|---|---|
| 总开关关闭 | `mango.feign.enabled=false` 后 `feignTokenFilter` 仍注册 |
| 无 Authorization 请求 | FilterChain 内仍可读取线程中预存的旧 token |
| 完整上下文透传 | `X-Mango-Member-Id` 缺失 |
| 真实 HTTP 出站流程 | 动态目标、path/query 与 HMAC 均正确，但服务端收到的 member header 缺失 |
| 空白内部密钥 | 纯空白 secret 仍生成整组内部调用签名 Header，而 Web 接收端把空白密钥判为未配置 |

无关 `FilterRegistrationBean` 共存、模块目标解析、绝对动态 URI 保留和现有签名测试均保持通过，因此没有扩大修复范围。

## 4. 修复结果与兼容边界

| 债务类型 | 修复结果 | 兼容边界 |
|---|---|---|
| member 上下文缺失 | `FeignRequestInterceptor` 透传 `MangoContextHeaders.MEMBER_ID` | 已有 Header 名称和其它上下文字段不变 |
| token 线程残留 | 每个 Servlet 请求进入时先清理 token，再按当前 Authorization 设置，退出仍清理 | 当前请求有 token 时的链内可见行为不变 |
| 总开关不完整 | `FeignServletAutoConfiguration` 同步受 `mango.feign.enabled` 控制 | 子开关名称、默认值和默认启用行为不变 |
| 条件装配漂移 | 内部签名拦截器只由自动配置注册，移除 Starter 内组件扫描副入口 | 标准自动配置宿主 Bean 类型和开关不变 |
| 日志暴露 | 内部签名 debug 日志不再输出 timestamp、nonce 和 signature | HTTP Header 与签名算法不变 |
| 空白密钥漂移 | 调用端与接收端统一按“有文本”判定 secret，空白值不再发送无效签名 | 正常非空 secret 的签名结果不变 |
| 静态债务 | 提取默认值和 filter order 常量，规范 logger 和条件表达式 | 配置默认值与 filter order 数值不变 |

## 5. 自动化用例

| 用例 ID | 优先级 | 层级 | 稳定契约 | 数据/清理 | 执行入口 | 状态 |
|---|---|---|---|---|---|---|
| TC-FEIGN-001 | P0 | 单元 | 完整 MangoContext 和 Authorization 全量出站，空上下文不产生 Header | 每用例清理 Holder | `FeignRequestInterceptorTest` | AUTOMATED |
| TC-FEIGN-002 | P0 | 单元 | 当前 Authorization 链内可见，缺失时旧 token 不可见，请求后清理 | Mock servlet request；每用例清理 Holder | `FeignTokenFilterTest` | AUTOMATED |
| TC-FEIGN-003 | P0 | 集成 | 总开关关闭后核心与 Servlet Feign 基础设施均不注册 | 独立 Spring Web context | `FeignAutoConfigurationTest` | AUTOMATED |
| TC-FEIGN-004 | P1 | 单元 | 模块信息解析为 service + contextPath，空输入/缺模块返回 empty | 无持久数据 | `ModuleTargetResolverTest` | AUTOMATED |
| TC-FEIGN-005 | P0 | 入口流程 | 真实 Feign 请求经动态目标到随机端口 HTTP 服务，最终 path/query、上下文和 HMAC 一致 | 环回端口；结束关闭 server/Holder | `FeignOutboundFlowTest` | AUTOMATED |
| TC-FEIGN-006 | P1 | 单元 | 空或空白 secret 均不发送内部调用 Header，非空 secret 保持原 HMAC 契约 | 无持久数据 | `InternalCallFeignInterceptorTest` | AUTOMATED |
| TC-FEIGN-007 | P1 | 消费契约 | Resource 动态目标和 Captcha API/Controller/Feign/HTTP 契约使用当前源码 | 无持久数据 | 两个消费者同 reactor 测试 | AUTOMATED |

## 6. 验证结果

| 层级 | 命令/入口 | 结果 | 结论 |
|---|---|---|---|
| 治理前行为基线 | Feign 模块 `clean test` | 4/4，fail/error/skip 0 | PASS |
| 缺陷红灯 | 新增测试运行于旧实现 | 首批 13 个中 4 个稳定失败；追加空白密钥单用例 1/1 失败 | DEFECT CONFIRMED |
| 治理后回归 | Feign 模块 `test` | 14/14，fail/error/skip 0 | PASS |
| 入口流程 | `FeignOutboundFlowTest`，标签 `flow` + `infra-feign` | 真实随机端口 HTTP 1/1 | PASS |
| Resource 消费者 | Feign + `mango-resource-starter-remote` 同 reactor | 9/9 | PASS |
| Captcha 消费者 | Captcha API/Core/Starter/Remote + Feign 同 reactor | 远程契约与 HTTP 3/3 | PASS |
| 直接静态 | Feign `checkstyle:checkstyle`、`spotbugs:spotbugs` | 0/0 | PASS |
| 正式架构 | Feign + `mango-architecture-verification` | dependency、ArchUnit、PMD 7、blocking 均为 0；聚合静态 0，工具失败 0 | PASS |

## 7. Issue #522 防回归

Captcha 首次只选 Remote 时因旧本地 API JAR 缺少当前 DTO 而编译失败；加入当前 API 后，反射测试又暴露旧本地 Starter
Controller。最终验证显式选择 Captcha API、Core、Starter、Remote 与当前 Feign，3/3 通过。该过程证明不能用清缓存、旧 JAR
或单个消费者 selector 冒充当前源码兼容；消费者反射或测试依赖触达的生产者必须进入同一 reactor。

## 8. 数据与未验证项

| 项目 | 结论 |
|---|---|
| 数据库/Flyway/init/demo | N/A；模块不拥有持久化与初始化数据 |
| UI/Chromium | N/A；模块无浏览器或管理页面，随机端口 HTTP 是产品入口边界 |
| 服务注册中心 | 本次使用 `ModuleInfoResolver` 当前契约和真实 HTTP 目标，不重复验证注册中心产品 |
| 全部 remote starter | 用户要求避免全仓重复检查；以 Resource 动态目标和 Captcha HTTP/契约作为代表消费者 |

## 9. 风险分级

- 需求影响：L3。身份上下文和内部签名位于跨服务安全边界，遗漏可能改变下游授权事实。
- 方案风险：L2。修复集中于单个共享 Starter，配置名和 HTTP 协议不变，可通过回退提交恢复。
- 最终风险：L3。由红灯、相同回归集、真实 HTTP 入口、两个当前消费者和正式架构门禁共同覆盖。
