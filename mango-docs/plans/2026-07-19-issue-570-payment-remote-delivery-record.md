# Issue #570 Payment Remote Starter 交付记录

## 1. 元数据

- 任务 ID：ISSUE-570-PAYMENT-REMOTE
- 交付模式：FULL（资金域公共远程契约修复；复用既有 Payment 架构设计和 Issue，不伪造新的产品需求文档链）
- 需求影响：L3 - 已发布的 Payment remote starter 为空制品，业务服务无法按公开 API 远程消费支付中心。
- 方案风险：L3 - 27 个 API、127 个方法横跨开放签名、内部回调、权限管理和资金交易边界。
- 最终风险：L3
- 工作区决策：CREATE（worktree `/Users/hardy/Work/mango-issue-570-payment-remote`，分支 `fix/issue-570-payment-remote-starter`）
- 适用措施：M01、M08、M09、M10、M11、M12、M14。
- 非降级事实：FUNDS、跨服务公共契约、Maven 发布制品。

## 2. 事实与目标

- Issue：[HardyDou/mango#570](https://github.com/HardyDou/mango/issues/570)。
- 改前事实：`mango-payment-starter-remote` 只有依赖声明，没有 Java 客户端、自动配置或自动配置索引。
- 目标：让微服务调用方仅依赖 remote starter，即可按对应 `XxxApi` 注入 Payment 远程代理。
- 成功条件：全部 Payment HTTP API 有唯一代理；HTTP 契约与 Controller 一致；制品包含运行时代码和自动配置索引；真实 HTTP 查询、请求体、内部签名链路可执行。
- CI 返工事实：PR 首轮只选择 `mango-payment-starter-remote` 时，同域 API 字节码和 starter 模块身份未进入 ArchUnit 契约上下文，产生 154 条 `FEIGN-007/008` 误报；静态质量扫描本身仍只允许覆盖直接修改模块。
- 不处理范围：Payment API、Controller、Service、权限码、签名算法、数据库、前端和业务流程变更；制品发布及版本升级另走发布流程。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| PR-001 | 微服务消费者 | 依赖 remote starter 并启用 OpenFeign | 可按 27 个 `XxxApi` 类型分别注入唯一代理 | 缺失或重复 Bean 导致上下文失败，不静默降级 | Spring 装配和消费者注入测试覆盖全部 API |
| PR-002 | Feign 代理 | 调用任一 Payment API 方法 | verb、根路径、方法路径、参数绑定、请求和响应泛型与 Controller 一致 | 契约漂移由自动测试阻断 | 自动比较 27 个 API、127 个 Controller/Feign 方法 |
| PR-003 | 查询调用方 | 使用继承 `PageQuery` 的 Payment Query | 按 JavaBean 属性编码查询参数，不编码静态字段 | 编码失败直接暴露，不回退到错误请求 | 真实 HTTP 验证 keyword/page/size |
| PR-004 | 开放或内部调用方 | 应用签名请求、标准通道回调、管理和任务调用 | 继续执行既有应用签名、内部 HMAC、权限和租户边界 | 仍按既有鉴权及业务错误返回 | 文档明确边界，代码不修改服务端安全逻辑 |
| PR-005 | Maven 制品消费者 | 获取构建后的 remote starter JAR | JAR 含 27 个客户端、唯一自动配置和 `AutoConfiguration.imports` | 空制品检查失败 | 构件清单回读通过 |
| PR-006 | partial-Reactor Java 门禁 | CI 只选择同域 `starter-remote` 和架构验证模块 | ArchUnit 只补入同域 API 字节码与 starter 模块身份作为 Feign 契约上下文；PMD/Checkstyle/SpotBugs 仍只扫描直接修改模块 | 缺少上下文时合法适配器被误报；扩大静态扫描范围时违反直接模块治理 | 按 GitHub Java job 的四段命令原样复现，架构和静态问题均为 0 |

## 4. 远程契约白名单

Payment 模块元数据声明 `module-name=mango-payment`，服务路径为 `/payment,/openapi/pay`。
API 模块当前 27 个接口都是这两个正式路径下的 HTTP 契约，因此全部纳入远程白名单，没有排除项。

| 分类 | 可远程 API | 边界说明 |
|---|---|---|
| 支付入口与回调 | `MangoPayVirtualPaymentApi`、`PaymentChannelCallbackApi`、`PaymentOpenApi` | 虚拟支付属于正式支付场景；标准回调供适配器验签后调用；开放接口仍要求应用签名 |
| 配置与治理 | `PaymentApplicationApi`、`PaymentCashierConfigApi`、`PaymentChannelApi`、`PaymentChannelContractApi`、`PaymentEnterpriseSubjectApi`、`PaymentMethodApi`、`PaymentMethodRouteApi`、`PaymentSecurityApi` | 远程代理不绕过 Controller 权限、租户和敏感操作保护 |
| 交易与资金处理 | `PaymentBusinessOrderApi`、`PaymentCashierApi`、`PaymentDifferenceApi`、`PaymentOfflineCollectionApi`、`PaymentOfflineRefundApi`、`PaymentOrderApi`、`PaymentReconciliationApi`、`PaymentRefundApprovalApi`、`PaymentRefundOrderApi`、`PaymentSettlementSummaryApi`、`PaymentTransactionFlowApi` | 保持订单状态、幂等、金额和审批规则不变 |
| 运维与任务 | `PaymentExceptionOrderApi`、`PaymentNotificationRecordApi`、`PaymentObservabilityApi`、`PaymentOperationAuditApi`、`PaymentTaskApi` | 管理和任务方法仍受原权限与业务状态约束 |

排除项为“无”。理由不是默认暴露所有 Java 类型，而是 `mango-payment-api` 仅承载已由
Controller 实现的跨模块 HTTP 契约；进程内 SPI 和服务实现位于 core/starter，不在 API 模块，
也没有进入 remote starter。

## 5. 技术决定

| ID | 对应要求 | 决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| TD-001 | PR-001/PR-002 | 每个 Feign 接口直接且只继承一个同名 Payment API，`name=mango-payment`，`contextId` 使用客户端类名 lowerCamel | starter-remote | 回退新增客户端；API 和 Controller 未改 |
| TD-002 | PR-002 | 从 Controller 对齐 HTTP verb、根路径、方法路径和参数绑定；Query 使用 `@SpringQueryMap`，Body/RequestParam 保持原声明 | starter-remote | 回退新增客户端 |
| TD-003 | PR-001/PR-005 | 只提供一个 `PaymentRemoteAutoConfiguration`，按客户端标记类扫描，并通过唯一 `AutoConfiguration.imports` 登记 | starter-remote | 回退自动配置和索引 |
| TD-004 | PR-003 | 自动配置提供 `@ConditionalOnMissingBean` 的 `BeanQueryMapEncoder`，避免无 Spring Data 消费端回落到字段编码器后读取继承层重复静态字段；业务可显式覆盖 | starter-remote | 回退该 Bean；不改 Query 模型 |
| TD-005 | PR-004 | 不提供 fallback，不修改公开应用签名、内部 HMAC、权限、租户或服务端业务逻辑 | 无服务端代码变更 | 回退 remote starter 即可 |
| TD-006 | PR-006 | partial Reactor 出现 `starter-remote` 时，从 Maven `allProjects` 只补同域 API 的已编译类目录，并从同域 starter 源/构建资源读取模块身份；架构检查器导入 API 以解析合约，但只对 selected Reactor 类执行规则；不把上下文目录加入 PMD 源目录、归属报告或依赖规则选择范围 | `mango-architecture-rules`、`mango-maven-plugin` | 回退契约上下文收集；恢复后合法 remote adapter 会再次被误报并由 CI 阻断 |

## 6. 实施清单

| ID | 对应决定 | 改动 | 完成条件 |
|---|---|---|---|
| IMPL-001 | TD-001/TD-002 | 新增 27 个 Feign 客户端，共 127 个声明方法 | 自动契约一致性测试通过 |
| IMPL-002 | TD-003/TD-004 | 新增唯一自动配置、自动配置索引和运行时依赖 | Spring 装配与真实 HTTP 测试通过 |
| IMPL-003 | 全部 | 新增契约、装配、消费者注入和真实 HTTP 测试 | 目标模块及依赖回归通过 |
| IMPL-004 | TD-005 | 更新 Payment README、能力地图和本记录 | 文档门禁通过 |
| IMPL-005 | TD-006 | 修复 partial-Reactor Feign 契约上下文收集并增加单元回归 | 仅选 remote starter 的 CI 原样门禁通过 |

## 7. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 |
|---|---|---|---|
| PR-001/PR-002 | 自动契约、Spring 装配和消费者注入 | `mvn -f mango/pom.xml -pl ':mango-payment-starter-remote' -Dtest=PaymentRemoteAdapterContractTest,PaymentRemoteAutoConfigurationTest,PaymentRemoteHttpIntegrationTest test` | PASS：5/5；覆盖 27 个客户端、127 个方法和全部 API 类型注入 |
| PR-003/PR-004 | 本地真实 HTTP 服务回读查询、请求体和内部签名 Header | `PaymentRemoteHttpIntegrationTest` | PASS：查询参数、请求体、内部签名 Header、HTTP verb 和路径均与预期一致 |
| PR-005 | JAR 构件清单回读 | `jar tf mango/mango-platform/mango-payment/mango-payment-starter-remote/target/mango-payment-starter-remote-1.0.0-SNAPSHOT.jar` | PASS：包含 27 个客户端、自动配置类和 `AutoConfiguration.imports` |
| 全部 | 依赖 Reactor 回归 | `mvn -f mango/pom.xml -pl ':mango-payment-starter-remote' -am test` | PASS：47/47 个 Reactor 模块构建成功，remote starter 5/5 测试通过 |
| 全部 | 目标模块生命周期验证 | `mvn -f mango/pom.xml -pl ':mango-payment-starter-remote' verify -Dmango.architecture.base=origin/main` | PASS：目标模块 `verify` 生命周期构建成功，5/5 测试通过 |
| PR-006 | 架构规则与 Maven 插件单元回归 | `mvn -f mango/pom.xml -pl ':mango-architecture-rules,:mango-maven-plugin' -DskipTests=false install` | PASS：架构规则 175 项、插件 222 项，失败 0、错误 0、跳过 0；新增用例确认 API 合约上下文只参与类型解析、不进入 remote-only 治理报告，且无关业务域的损坏模块身份不会阻断局部门禁；`ArchitectureMojoTest` 19/19 |
| 全部 | GitHub partial-Reactor 架构与静态质量门禁原样复现 | 先对 `mango-payment-starter-remote` 执行 `-am -DskipTests ... install`，再仅选择 `:mango-architecture-verification,mango-platform/mango-payment/mango-payment-starter-remote` 执行 `changed/no-new-violations verify` | PASS：47/47 上游 Reactor 模块构建成功；最终只选 2 个模块时依赖、ArchUnit、PMD、blocking 均为 0，新增静态问题和工具失败均为 0；静态扫描范围只有 `mango-payment-starter-remote` |
| 全部 | Payment 域 module-info 全量检查 | `mvn -f mango/pom.xml -pl ':mango-architecture-verification' io.mango.tools.maven.plugin:mango-maven-plugin:1.0.0-SNAPSHOT:check -Drule=module-info -DbaseDir=/Users/hardy/Work/mango-issue-570-payment-remote/mango/mango-platform/mango-payment -Dmango.check.changedOnly=false -Dmango.check.gate=all -Doutput=json -DreportFile=/Users/hardy/Work/mango-issue-570-payment-remote/.runtime/payment-remote-module-info.json` | PASS：问题 0，工具失败 0 |
| 全部 | 测试资产和 Mock 门禁 | `test-quality-check.mjs --base origin/main`；`audit-backend-test-mocks.mjs --report-only --changed-only --base origin/main` | PASS：检查 6 个测试文件；Mock block=0、warn=0 |
| 全部 | README、源码事实和能力说明门禁 | `audit-module-readmes.mjs`；`audit-readme-source-facts.mjs`；`check-capability-docs.mjs --base origin/main --head HEAD` | PASS：README、源码事实和能力说明均通过 |

说明：独立旧版 Maven PMD 6.42 无法解析 Java 21 class 文件，会报告
`Unsupported class file major version 65`，不作为本任务验收依据。正式 Mango 质量门禁使用
支持 Java 21 的 PMD 执行链，结果为 `pmd=0`、`toolFailureCount=0`。

## 8. 发布与回滚

- 本任务只修复源码和构建产物内容，不在本工作区执行 Maven 发布、版本升级或线上部署。
- 发布时必须按统一 release 流程验证私服 JAR 不再为空，并在干净消费者中回读自动配置。
- 回滚可整体回退本次 remote starter 变更；由于 API、Controller、数据库和业务逻辑未改，不需要数据回滚。
