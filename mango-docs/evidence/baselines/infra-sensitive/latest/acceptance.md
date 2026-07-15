# Infra Sensitive 历史债务治理验收证据

## 1. 验收范围

- 模块：`mango-infra-sensitive-api`、`mango-infra-sensitive-core`、
  `mango-infra-sensitive-starter`。
- 真实消费者：Authorization 的 `AuthorizationSensitiveRawAccessProvider`、
  `SecurityAutoConfiguration` 和 `DefaultAuthorizationProvider`。
- 行为：15 种脱敏策略、短值和非法值、JSON 递归、Jackson 序列化与反序列化、
  原文权限、临时关闭上下文、敏感词 provider、异常链与日志无原文。
- 边界：Sensitive 没有 Controller、Feign、`starter-remote`、数据库迁移、菜单或页面；
  不为形式验收新增伪造 HTTP/UI 入口。

## 2. 治理前基线

| 项目 | 结果 |
|---|---|
| 生产规模 | API 6 个 Java/283 行；Core 8 个 Java/459 行；Starter 2 个 Java/249 行 |
| 测试规模 | 4 个测试类/286 行；模块测试 15/15，通过；Authorization provider 测试 6/6，通过 |
| 架构阻断 | 4 个：`MANGO-ARCH-BEAN-001`、`MANGO-ARCH-BEAN-006`、`MANGO-ARCH-TYPE-010`、`MANGO-ARCH-TYPE-011` |
| 静态问题 | Checkstyle 26 个、SpotBugs 2 个、PMD 0 个 |
| 已复现缺陷 | Scope 重复关闭会释放外层；短手机号、单字符邮箱等返回原文；静态 Runtime 策略跨上下文共享；provider 返回 null 时 NPE |

## 3. 修复结果

| 债务类型 | 修复结果 | 兼容边界 |
|---|---|---|
| API/模块角色 | 本地 SPI、上下文和默认实现使用 `LocalCapabilityContract`；API 包名和消费者依赖不变 | Authorization 仍只依赖 `mango-infra-sensitive-api`，未被迫依赖 core |
| 静态 Service Locator | 删除 `SensitiveMaskingRuntime`；starter 将 policy 构造注入 Jackson module、modifier 和 serializer | 无参 module 仍默认脱敏；配置前缀、SPI 和注解不变 |
| 上下文生命周期 | Scope 关闭幂等；跨线程关闭明确拒绝；异常后恢复；ThreadLocal 隔离 | `disable`、`getWithoutMasking`、`runWithoutMasking` 签名不变 |
| 短值和非法值 | 确保至少遮蔽一个 Unicode code point；短 key/非法 JSON 不再保留完整原文；非法 IPv4 全遮蔽 | 标准有效输入输出格式保持原基线 |
| 敏感词 provider | null 集合按空集合处理并过滤 null 词条 | provider 顺序和非空词条保持不变 |
| 静态质量 | 重构策略派发、query 参数处理和常量；Spring 配置对象按仓内模式说明 SpotBugs 例外 | 无业务行为变化 |

## 4. 自动化验证

| 层级 | 命令/入口 | 关键结果 | 结论 |
|---|---|---|---|
| 单元与组件 | `mvn -f mango/pom.xml -pl :mango-infra-sensitive-api,:mango-infra-sensitive-core,:mango-infra-sensitive-starter,:mango-authorization-support,:mango-authorization-starter -Dtest='*Sensitive*Test' -Dsurefire.failIfNoSpecifiedTests=false test` | API 5、Core 53、Starter 5、Authorization Support 6、真实装配集成 2；合计 71/71，failure/error/skip 均为 0 | PASS |
| 真实消费链 | `SensitiveAuthorizationIntegrationTest` | Spring 同时装配 Security 与 Sensitive；真实 `DefaultAuthorizationProvider` 有权限返回原文、无权限脱敏；反序列化保持对象原值 | PASS |
| 架构 | Sensitive 三模块加 `mango-architecture-verification` 的 partial reactor full mode | dependency=0、ArchUnit=0、PMD=0、blocking=0 | PASS |
| 静态 | 聚合 static + 各模块 Checkstyle/SpotBugs | total/new/toolFailure=0；API/Core/Starter SpotBugs 均为 0 | PASS |
| 测试质量 | `node mango-pmo/tools/test-quality-check.mjs --base origin/main` | 检查 6 个新增/变更测试文件 | PASS |
| Mock 审计 | `node mango-pmo/tools/audit-backend-test-mocks.mjs --report-only --changed-only --base origin/main` | block=0、warn=0 | PASS |
| 能力文档 | `node mango-pmo/tools/check-capability-docs.mjs --base origin/main --head HEAD` | 检查 25 个变更文件 | PASS |
| 异常保密 | `SensitiveJacksonModuleTest` | policy 失败时完整 cause chain 不包含原手机号 | PASS |
| 并发/上下文 | `SensitiveMaskingContextTest` | 嵌套、重复关闭、异常恢复、双线程隔离、跨线程误关闭全部通过 | PASS |

新增测试未使用 Mockito。Authorization provider 原有 6 个测试使用真实 VO 和函数式测试替身；
新增的消费链测试使用真实 `DefaultAuthorizationProvider`、真实 Spring 自动配置和真实 Jackson module。

## 5. Fresh MySQL 与 Monolith 验收

| 项目 | 结果 |
|---|---|
| 工作区 | slot 189；后端 `18189`；独立数据库 `mango_dev_mango_infra_sensitive_debt_189`；独立 Maven 本地仓库 |
| 空库前置 | 数据库首次创建后表数量为 0 |
| 启动方式 | 只安装 Sensitive 精确模块，然后直接运行 monolith Spring Boot plugin；未执行根仓全量 install |
| 启动结果 | `Started MangoMonolithApplication in 16.87 seconds` |
| 健康检查 | `/actuator/health` HTTP 200，整体、MySQL、ping、SSL 均为 `UP` |
| 接口装配 | `/v3/api-docs` HTTP 200 |
| 数据库 | 222 张表、21 张 Flyway history 表、失败迁移 0 |
| 日志 | `ERROR`/启动失败 0；真实 stack trace 0；测试原手机号/邮箱命中 0；非空 SM4 key 命中 0；数据库密码为空，检查 N/A |
| 清理 | 验收后后端端口已停止监听 |

第一次直接 Maven 启动没有映射 CLI 使用的 Office 环境别名，因 Office 组件缺失退出。
确认 workspace 已配置关闭插件后，将 `MANGO_OFFICE_PLUGIN_ENABLED=false` 等值映射为组件实际读取的
`KK_OFFICE_PLUGIN_ENABLED=false`，删除并重建隔离数据库，从 0 表重新启动并通过上述验收。
该问题属于本地启动命令环境映射，不是 Sensitive 代码或迁移缺陷。

## 6. 未验证项和边界

| 项目 | 原因 | 结论 |
|---|---|---|
| Chromium 页面 E2E | 全仓生产 DTO 没有 `@Sensitive` 使用点，Sensitive 本身也没有页面/Controller；新增伪页面会制造错误能力边界 | N/A，以真实 Authorization + Spring + Jackson 消费链和 fresh monolith 代替 |
| Remote 回归（#522） | 模块没有 Feign、Controller 或 `starter-remote` | N/A，扫描确认无远程协议入口 |
| 数据库敏感字段 | 模块不持久化数据、不提供迁移 | N/A，数据库只用于 monolith 全量装配验证 |
| 非直接消费者模块 | 用户明确要求不重复执行全仓检查 | 不用本证据宣称其它模块回归；仅证明 Sensitive 与真实 Authorization 消费链 |

## 7. 业务开发交接

标准回归入口是 71 个定向测试、Sensitive partial architecture/static 门禁和 fresh monolith
健康检查。新增策略时必须同步扩展全枚举策略矩阵；任何响应、异常 cause chain 或运行日志出现
完整原文均阻断交付。`SensitiveMaskingContext` 只允许 try-with-resources 受控使用，Scope 必须由
创建它的线程关闭。
