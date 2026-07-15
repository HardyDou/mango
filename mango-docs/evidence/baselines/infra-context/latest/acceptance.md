# Infra Context 历史债务治理验收证据

## 1. 验收范围

- 模块：`mango-infra-context-api`、`mango-infra-context-support`、
  `mango-infra-context-starter`。
- 真实消费者：`mango-infra-web-starter` 的 `MangoContextWebFilter` 和测试应用中的
  `@TtlAsync` 服务。
- 行为：快照规范化与不可变合并、Holder 与 token 生命周期、Runnable/Callable/定时任务传播、
  工作线程恢复、自动配置开关、自定义 Bean 回退和线程池配置。
- 边界：Context 不拥有 Controller、Feign、数据库、Flyway、正式资源、demo 数据、菜单或页面。

## 2. 治理前基线

| 项目 | 结果 |
|---|---|
| 模块测试 | API、Support、Starter 均为 `No tests to run`，合计 0 个 |
| Maven 编译 | 三个模块及上游依赖编译成功，只能证明可编译 |
| 架构债务 | API 3 条 `MANGO-ARCH-TYPE-010`；三个具体 JVM 本地能力类型没有正式契约标记 |
| Checkstyle | 13 条：Snapshot 7、Holder 1、Properties 5 |
| SpotBugs | Properties 1 条 `EI_EXPOSE_REP` |
| 消费链 | 没有 HTTP 入口到异步执行器的自动化验收，也没有线程复用防泄漏证据 |

## 3. 修复结果与兼容边界

| 债务类型 | 修复结果 | 兼容边界 |
|---|---|---|
| JVM 本地契约 | `MangoContextHeaders`、`MangoContextHolder`、`MangoContextSnapshot` 标记 `LocalCapabilityContract` | 包名、类名、字段、方法签名和 HTTP 头常量不变 |
| 快照复杂度 | 拆分首选值逻辑，使用全字段空值匹配，消除复杂条件和行内三元表达式 | 空白转 null、文本 trim、非空新值覆盖、空新值保留旧值的语义不变 |
| Holder | 用显式分支返回空快照 | `set(null/empty)` 同时清理上下文和 token、`clearToken` 只清 token 的语义不变 |
| 配置默认值 | 将数字默认值提取为命名常量 | 线程数、队列、存活、停机等待和线程名前缀默认值不变 |
| Spring 配置 Bean | 仅在 `getExecutor()` 精确说明可变嵌套 Bean 的绑定契约 | 继续返回同一个实例，Spring Boot 配置绑定方式不变 |
| 测试缺口 | 新增模块单元/组件测试和 Web 真实消费链 E2E | 不新增生产 Controller 或伪造页面来满足形式验收 |

## 4. 自动化验证

| 层级 | 命令/入口 | 结果 | 结论 |
|---|---|---|---|
| 治理前行为基线 | Context 三模块定向 `test` | API 15、Support 7、Starter 6；28/28 | PASS |
| 治理后等价回归 | 同一批 Context 三模块测试 | 28/28，failure/error/skip 均为 0 | PASS |
| 真实 HTTP/异步 E2E | `MangoContextPropagationE2ETest` | 随机端口 Tomcat 2/2；真实 Filter、`@TtlAsync`、命名线程池和连续请求隔离 | PASS |
| 架构 | Context 三模块、Web 消费模块与 `mango-architecture-verification` 的 partial reactor | dependency=0、ArchUnit=0、PMD=0、blocking=0 | PASS |
| Context 直接静态 | 三模块 `checkstyle:checkstyle pmd:pmd spotbugs:spotbugs` | API/Support/Starter 的 Checkstyle、PMD、SpotBugs 均为 0 | PASS |
| 聚合静态 | affected partial reactor，`no-new-violations` | new=0、tool failure=0 | PASS |

新增测试不使用 Mockito。端到端测试启动真实嵌入式 Tomcat，通过真实 HTTP 客户端进入
`MangoContextWebFilter`，再由 Spring `@Async` 代理提交到真实 `mangoContextExecutor`；第二次请求复用同一
异步线程，并断言上一次 tenant/app 上下文没有残留。

## 5. 数据、发布物与 Issue #522 防回归

| 项目 | 结果 |
|---|---|
| 数据库 | N/A；模块不读写数据库，不发布 Flyway migration |
| 正式/demo 初始化 | N/A；模块没有资源注册、Runner 或初始化数据 |
| Controller/API 校验 | N/A；Context 仅提供 JVM 本地能力，不声明 HTTP Controller/Feign |
| 真实消费者 | Web Starter 使用 Context Starter，随机端口 E2E 在同一 Maven reactor 构建并运行修改后的 Context 代码 |
| 缓存隔离 | E2E 使用 reactor 上游模块，不以公共 Maven 仓库中的旧 Context JAR 代替当前修改 |

## 6. 未验证项和边界

| 项目 | 原因 | 结论 |
|---|---|---|
| Fresh MySQL | Context 没有数据库能力或迁移 | N/A，不为形式验收创建空表或伪 migration |
| Chromium 页面 | Context 没有独立管理页面 | N/A，真实 HTTP 服务端到异步线程链路是该能力的端到端边界 |
| Feign 下游传播 | 由 `mango-infra-feign` 拥有，Context 只提供头常量和进程内快照 | 本批次不外推 Feign 模块已完成治理；后续治理 Feign 时单独验证 |
| 非直接消费者模块 | 用户要求不重复全仓检查 | 本证据只证明 Context 与 Web 真实消费者，不宣称其它模块回归 |

## 7. 业务开发交接

Context 的定向回归入口是 28 个模块测试和 2 个真实 HTTP/异步 E2E。排查异步上下文时先确认任务是否使用
`@TtlAsync`、`mangoContextExecutor` 或经 `TtlExecutorDecorator` 包装的自有线程池；手工设置上下文的调用方
应在结束时清理。后续修改快照字段、请求头、Holder 清理语义或线程池装配时，需要同步扩展模块测试和 Web
消费链 E2E。
