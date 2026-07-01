# Mango Resource Registry 设计交付契约

## 1. 目标

形成并落地 `mango-resource` Resource Registry，明确资源声明协议、稳定 ID、版本、字段类型、消费者注册、启动同步、冲突处理、多实例锁和过程记录，并完成系统字典和授权 API 资源接入验证。

当前能力补充：Issue #184 已在 Resource Registry 基础上补齐 demo 资源目录隔离和 `INIT_ONLY` 同步模式，并把大 SQL、外部 SQL、schema baseline pack 收敛到 `mango-infra-persistence` 的模块化 Flyway。业务 Agent 和框架开发 Agent 判断当前数据初始化边界时，应优先阅读 [Issue #184 数据治理设计](./2026-07-01-issue-184-data-governance-design.md)、`mango-resource` README 和 `mango-infra-persistence` README；本文保留 Resource Registry 原始交付契约和验收台账。

## 2. 范围

- 资源注册中心定位。
- `mango-resource` 模块边界。
- YAML、JSON、Java Provider、自定义 ResourceProvider 声明来源。
- `mango.resource` 声明 schema。
- 雪花资源 ID、`bizKey`、`version` 规则。
- typed fields 和 `classpath:` 文件字段。
- 消费者模块支持资源类型和字段 schema 的公开方式。
- `AUTO`、`MANUAL`、`LOCKED` 覆盖策略。
- 逻辑删除、hash、冲突、启动同步和多实例锁。
- registry、sync log、change log、lock 表设计。
- `mango-system` 字典资源 handler 接入。
- 授权 API 资源通过自定义 `ResourceProvider` 接入，并与旧 runner 数据库结果逐字段对比。

## 3. 不做什么

- 不定义消息模板、工作流、编码规则、打印模板、AI Prompt 的业务 DSL。
- 不把接口权限、菜单等扫描型资源改成配置文件维护。
- 不改变授权 API 资源原有写库逻辑。

## 4. 设计输入

- 用户提供的 Mango Resource Registry 最终方案。
- 用户补充要求：统一 `mango.resource` 前缀、按模块划分、每模块多个资源类型、资源必须有雪花 ID 和 version、支持 typed fields、file 支持 `classpath:`、消费者自行公开或注册支持资源类型和字段内容。
- PMO 规则：`mango-pmo/rules/00-dev-flow.md`、`mango-pmo/rules/01-delivery-contract.md`、`mango-pmo/rules/06-document-assets.md`。

## 5. 交付物

- `mango-docs/designs/mango-resource-registry-design.md`
- `mango-docs/designs/mango-resource-registry-delivery-contract.md`
- `mango/mango-platform/mango-resource/**`
- `mango/mango-platform/mango-system/mango-system-core/src/main/java/io/mango/system/core/resource/**`
- `mango/mango-platform/mango-authorization/**/resource/**`

## 6. 验收方式

- 设计文档覆盖用户列出的定位、目标、架构、模块、Provider、Handler、ResourceType、BizKey、数据库、同步模式、同步规则、Hash、Flyway、后台菜单、启动时序和最终原则。
- 设计文档补充资源声明 schema、ID 生成工具、ready 前启动同步、冲突 fail-fast、多实例抢锁。
- `mango-resource` 模块测试通过。
- `mango-system` 字典资源真实数据库测试通过。
- 授权 API 资源旧 runner 与新 resource 链路真实数据库内容逐字段一致。
- 执行交付台账检查。

## 7. 风险与限制

本次已进入实现。仍有两个需要后续单独治理的风险：既有测试链路中 `-am` 带出的历史 Mockito 警告未在本任务范围内全部清理；页面级资源管理后台只完成后端接口，未做前端菜单页面验收。

## 8. 交付台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| DESIGN-001 | 用户要求 | 明确 Resource Registry 定位 | `mango-resource` 只做注册、同步、审计和追踪，不保存真实资源内容 | `mango-resource-registry-design.md` 第 1、2 节 | 设计评审 | DONE | `mango-resource-registry-design.md` |
| DESIGN-002 | 用户要求 | 定义模块结构和职责 | 采用 `api/support/core/starter/sync-starter/starter-remote` 结构；后台接口归入本地 `starter`，扫描注册归入 `sync-starter`，远程调用归入 `starter-remote` | `mango-resource-registry-design.md` 第 3 节 | 设计评审 | DONE | `mango-resource-registry-design.md` |
| DESIGN-003 | 用户要求 | 支持 YAML、JSON、Java Provider、自定义扫描处理 | 统一以 `ResourceProvider` 作为资源来源接口；声明文件由 `FileResourceProvider` 提供，接口权限、菜单等扫描逻辑由自定义 Provider 内部完成 | `mango-resource-registry-design.md` 第 4、5 节 | 设计评审 | DONE | `mango-resource-registry-design.md` |
| DESIGN-004 | 用户要求 | 统一 `mango.resource` 声明 schema | 按模块划分，每个模块支持多个资源类型，资源字段使用 typed map | `mango-resource-registry-design.md` 第 5、6 节 | 设计评审 | DONE | `mango-resource-registry-design.md` |
| DESIGN-005 | 用户要求 | 每个资源有稳定 ID 和 version | `id` 使用雪花算法，`version` 只增不减，`bizKey` 保留人读唯一键 | `mango-resource-registry-design.md` 第 7 节 | 设计评审 | DONE | `mango-resource-registry-design.md` |
| DESIGN-006 | 用户要求 | 消费者自行公开支持类型和字段 | 支持 `ResourceHandler.spec()` 和 `META-INF/mango/resource-types/*` | `mango-resource-registry-design.md` 第 8 节 | 设计评审 | DONE | `mango-resource-registry-design.md` |
| DESIGN-007 | 用户要求 | 定义覆盖、删除和人工接管 | 采用 `AUTO/MANUAL/LOCKED` 与逻辑删除状态 | `mango-resource-registry-design.md` 第 9、10 节 | 设计评审 | DONE | `mango-resource-registry-design.md` |
| DESIGN-008 | 用户要求 | 定义 hash、冲突和启动同步 | hash 包含文件内容，冲突 fail-fast，ApplicationRunner 在 ready 前同步 | `mango-resource-registry-design.md` 第 11、12、13 节 | 设计评审 | DONE | `mango-resource-registry-design.md` |
| DESIGN-009 | 用户要求 | 支持多实例抢锁 | 使用数据库锁，抢到锁的实例同步，其它实例等待或跳过 | `mango-resource-registry-design.md` 第 14 节 | 设计评审 | DONE | `mango-resource-registry-design.md` |
| DESIGN-010 | 用户要求 | 过程记录到数据库 | 设计 registry、sync log、change log、lock 表 | `mango-resource-registry-design.md` 第 15 节 | 设计评审 | DONE | `mango-resource-registry-design.md` |
| DESIGN-011 | 用户要求 | 考虑所有可被注入资源的模块 | 列出优先接入和谨慎接入模块策略 | `mango-resource-registry-design.md` 第 17 节 | 设计评审 | DONE | `mango-resource-registry-design.md` |
| DESIGN-012 | PMO 规则 | 明确验证范围 | 列出后续实现至少覆盖的解析、同步、锁、日志和后台验证 | `mango-resource-registry-design.md` 第 19 节 | 设计评审 | DONE | `mango-resource-registry-design.md` |
| IMPL-001 | 用户要求 | 实现 `mango-resource` 基础模块 | 新增 api/support/core/starter/sync-starter/starter-remote；使用 infra-persistence/MyBatis-Plus 持久化 | `mango/mango-platform/mango-resource/**` | Maven 测试 | DONE | `mvn -f mango/pom.xml -pl mango-platform/mango-resource/mango-resource-api,mango-platform/mango-resource/mango-resource-support,mango-platform/mango-resource/mango-resource-core,mango-platform/mango-resource/mango-resource-starter,mango-platform/mango-resource/mango-resource-sync-starter,mango-platform/mango-resource/mango-resource-starter-remote -am test -DskipTests=false` |
| IMPL-002 | 用户要求 | 资源来源统一为 `ResourceProvider` | JSON/YAML 文件由 `FileResourceProvider` 提供，自定义扫描也实现 `ResourceProvider`，不保留并列扫描抽象 | `mango-resource-api`、`mango-resource-support` | 旧扫描接口名无残留 | DONE | `rg -n "<旧扫描接口名>" mango mango-docs -S` |
| IMPL-003 | 用户要求 | 字典由资源注册注入 | `mango-system` 提供 `SYSTEM_DICT` handler；其它模块字典声明通过 resource 同步进入系统字典表 | `mango-system-core/src/main/java/io/mango/system/core/resource/**` | Maven 测试 | DONE | `mvn -f mango/pom.xml -pl mango-platform/mango-system/mango-system-core,mango-platform/mango-resource/mango-resource-api,mango-platform/mango-resource/mango-resource-support,mango-platform/mango-resource/mango-resource-core -am test -DskipTests=false` |
| IMPL-004 | 用户要求 | 授权 API 资源对比旧逻辑 | 新增 `ApiAccessResourceProvider`，新链路仍调用原 `IApiResourceService.registerApiResources`；数据库逐字段对比旧 runner 与新 resource 链路 | `ApiAccessResourceProviderDatabaseComparisonTest` | Maven 测试 | DONE | `mvn -f mango/pom.xml -pl mango-platform/mango-authorization/mango-authorization-resource-sync-starter -am test -DskipTests=false` |
| IMPL-005 | 用户要求 | 不改变原数据逻辑 | `ResourceHandler` 支持完整批次；`ApiResourceHandler` 保持授权原批量注册语义，覆盖重复同步和 MANUAL 保护 | `ResourceHandler`、`ResourceRegistrySyncService`、`ApiResourceHandler` | 真实数据库测试 | DONE | `ApiAccessResourceProviderDatabaseComparisonTest` |
