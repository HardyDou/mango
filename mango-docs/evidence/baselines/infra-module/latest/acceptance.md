# Infra Module 历史债务治理验收证据

## 1. 验收范围

- 模块：`mango-infra-module-api`、`mango-infra-module-core`、`mango-infra-module-starter`。
- 真实消费者：`mango-infra-feign-starter`、`mango-authorization-resource-sync-starter`。
- 行为：classpath 元数据加载、显式部署配置覆盖、模块路径归一与反查、Spring 自动配置、Feign 动态目标。
- 边界：本模块不拥有 HTTP Controller、数据库、Flyway、初始化数据、菜单或管理页面。

## 2. 治理前基线

| 项目 | 结果 |
|---|---|
| 模块测试 | 8/8 通过；仅覆盖单一 classpath 或配置来源、常规路径解析和 Bean 身份 |
| Checkstyle | 6 条 `AvoidInlineConditionals` |
| SpotBugs 聚合 | 2 条：`ModuleProperties` Map 内部表示暴露与外部可变 Map 保存 |
| 架构存量 | 完整 Reactor 预算登记 API 1 条 `MANGO-ARCH-TYPE-010`，本地 JVM 契约缺少明确标记 |
| 消费入口 | 没有由 Module 自动配置驱动的真实 Feign HTTP 路由测试 |

## 3. 缺陷红灯

新增边界测试运行于旧实现时形成 4 个稳定失败：

| 用例 | 治理前失败事实 |
|---|---|
| 配置覆盖 classpath | 同名配置已声明 `mango-system-app/system-app`，`resolve()` 仍返回 classpath 的 `mango-platform-app/platform` |
| 尾斜杠归一 | `contextPath=/admin/` 被保留，运行路径形成双斜杠且无法匹配正常请求 |
| 根模块路径 | `modulePath=/` 无法匹配 `/health/readiness` |
| 空线程 ClassLoader | `ModuleMetadataLoader.load()` 直接空指针失败 |

原有 8 个测试保持通过，证明修复目标可以限定在来源优先级、路径边界和加载器回退，不需要改变公开构造器、方法签名或配置名称。

## 4. 修复结果与兼容边界

| 债务类型 | 修复结果 | 兼容边界 |
|---|---|---|
| 部署映射优先级错误 | 显式配置按模块完整替换 classpath 主记录和旧路径集合，首个配置路径仍为主解析值 | 配置前缀、字段名、默认服务名和多路径语法不变 |
| 路径不稳定 | context/module/request path 共用绝对路径归一，根路径支持嵌套请求 | 非根路径、最长模块路径匹配和无 context path 行为不变 |
| 加载器空指针 | TCCL 缺失时回退到 `ModuleMetadataLoader` 定义 ClassLoader | 正常 TCCL 扫描顺序和元数据格式不变 |
| 本地契约边界 | `ModuleInfo`、`ModuleInfoRegistry`、`ModuleInfoResolver` 标记 `@LocalCapabilityContract` | 三个公开类型的名称、签名和构造方式不变 |
| 配置可变状态 | setter 防御性复制，getter 返回只读视图 | Spring 配置绑定和调用方读取方式不变 |
| 静态债务 | 清理 6 条条件表达式、2 条 SpotBugs 和 1 条 API 架构存量 | 默认值、注册顺序和异常类型不变 |

## 5. 自动化用例

| 用例 ID | 优先级 | 层级 | 稳定契约 | 数据/清理 | 执行入口 | 状态 |
|---|---|---|---|---|---|---|
| TC-MODULE-001 | P0 | 单元 | 尾斜杠与根路径形成稳定运行路径并正确匹配 | 无持久数据 | `ModuleInfoTest` | AUTOMATED |
| TC-MODULE-002 | P0 | 集成 | 显式配置完整覆盖同名 classpath 服务与路径 | 独立 Spring context | `ModuleAutoConfigurationTest` | AUTOMATED |
| TC-MODULE-003 | P1 | 单元 | TCCL 为空时使用定义 ClassLoader | 测试后恢复线程 ClassLoader | `ModuleMetadataLoaderTest` | AUTOMATED |
| TC-MODULE-004 | P1 | 单元 | 配置 Map 防御性复制且调用方不可修改 | 无持久数据 | `ModulePropertiesTest` | AUTOMATED |
| TC-MODULE-005 | P0 | 入口流程 | Module 自动配置驱动真实 Feign 请求到配置服务和 context path | 随机环回端口；用例后关闭 server | `ModuleRoutingFlowTest` | AUTOMATED |
| TC-MODULE-006 | P1 | 消费契约 | Feign 与 Authorization 使用当前 Module API 编译 | 无持久数据 | 当前源码同 reactor | AUTOMATED |

## 6. 验证结果

| 层级 | 命令/入口 | 结果 | 结论 |
|---|---|---|---|
| 治理前行为基线 | Module 三子模块 `clean test` | 8/8，fail/error/skip 0 | PASS |
| 缺陷红灯 | 新增测试运行于旧实现 | 4 个边界稳定失败 | DEFECT CONFIRMED |
| 治理后回归 | Module + 当前 Feign 同 reactor 定向套件 | 14/14，fail/error/skip 0 | PASS |
| 入口流程 | `ModuleRoutingFlowTest`，标签 `flow` + `infra-module` | 真实随机端口 HTTP 1/1 | PASS |
| Authorization 消费者 | Module API + Authorization API/resource-sync 当前源码同 reactor | 87 个生产源编译通过 | PASS |
| 直接静态 | Module 三子模块 Checkstyle、SpotBugs | 0/0 | PASS |
| 正式架构 | Module 三子模块 + `mango-architecture-verification` | dependency、ArchUnit、PMD 7、blocking、聚合静态和工具失败均为 0 | PASS |

## 7. Issue #522 防回归

真实路由测试显式把 Module API/Core/Starter 与 Feign 当前源码放在同一 reactor；Authorization 兼容编译也显式选择当前 Module API 与当前消费者。结论不依赖旧本地 JAR，不通过清缓存或单独运行消费者掩盖生产者版本漂移。

## 8. 数据与未验证项

| 项目 | 结论 |
|---|---|
| 数据库/Flyway/init/demo | N/A；模块仅维护应用进程内元数据 |
| UI/Chromium | N/A；模块无浏览器或管理页面，真实 Feign HTTP 是产品入口边界 |
| 服务注册中心 | 不在本模块范围；`serviceName` 只作为上层路由目标，本次验证目标选择和 HTTP 到达事实 |
| 全仓测试 | 未执行；按用户要求仅验证模块、Feign 真实入口和 Authorization 直接消费者 |

## 9. 风险分级

- 需求影响：L3。错误部署映射会把跨服务调用路由到错误服务或旧 context path，影响平台跨模块主流程。
- 方案风险：L2。修复集中在 Module 三子模块，公开类型、配置名和元数据格式不变，可通过单提交回退。
- 最终风险：L3。由旧实现红灯、相同回归集、真实 HTTP、两个当前消费者和正式架构门禁共同覆盖。
