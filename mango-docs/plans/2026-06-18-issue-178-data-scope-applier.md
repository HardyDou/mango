# Issue 178 DataScopeApplier Bean Fix 交付契约

## 1. 目标

修复业务应用同时接入 Mango 持久化 starter 和授权 starter 后，注入 `DataScopeApplier` 时缺少 Bean 导致启动失败的问题。

## 2. 范围

- 调整 `mango-infra-persistence-starter` 中数据权限相关自动配置顺序。
- 增加 `DataScopeProvider` 存在、缺失以及授权自动配置后置导入场景的回归测试。
- 修复验证过程中发现的 `mango-authorization-core` 泛型契约编译问题。

## 3. 不做什么

- 不修改数据权限 API 契约。
- 不修改数据库结构、菜单、页面或权限数据。
- 不调整授权模块业务逻辑和数据范围计算规则。

## 4. 设计输入

- GitHub Issue #178：业务应用注入 `io.mango.infra.persistence.api.scope.DataScopeApplier` 时启动失败。
- 现有实现：`PersistenceAutoConfiguration#dataScopeApplier(...)` 依赖 `DataScopeProvider` Bean。
- 现有授权 starter：`AuthorizationAutoConfiguration` 注册 `AuthorizationDataScopeProvider` 作为 `DataScopeProvider`。

## 5. 设计说明

### 5.1 影响模块

- `mango-infra-persistence-starter`
- `mango-authorization-core`

### 5.2 接口变化

无对外 API、SPI 方法签名或 HTTP 接口变化。

### 5.3 数据变化

无数据库、migration、初始数据或配置项变化。

### 5.4 菜单/页面/权限变化

无菜单、页面或权限码变化。

### 5.5 测试范围

- 持久化 starter 自动配置回归测试。
- 持久化 starter 模块 Maven 单测与 Checkstyle。
- 授权 starter 相关 Maven 单测与 Checkstyle，覆盖新发现的泛型编译问题。

## 6. 风险与限制

- `@AutoConfiguration(afterName = "...AuthorizationAutoConfiguration")` 使用类名字符串，避免新增跨模块依赖；如果未来授权 starter 类名迁移，需要同步更新该排序声明。
- 本次未启动业务应用做运行态验收；变更点已通过 Spring Boot `ApplicationContextRunner` 覆盖自动配置 Bean 注册链路。

## 7. 交付台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| ISSUE-178-001 | GitHub Issue #178 | 业务应用接入持久化与授权 starter 后可注入 `DataScopeApplier` | 持久化自动配置声明在授权自动配置之后处理，保证 `DataScopeProvider` 已可见 | `PersistenceAutoConfiguration` | `mvn -f mango/pom.xml -pl :mango-infra-persistence-starter -am test` | DONE | 本文件第 8 节 |
| ISSUE-178-002 | GitHub Issue #178 | 回归覆盖 provider 存在、缺失和授权自动配置导入顺序场景 | 使用 `ApplicationContextRunner` 验证 Bean 注册结果 | `PersistenceAutoConfigurationTest`、测试用 `AuthorizationAutoConfiguration` | `mvn -f mango/pom.xml -pl :mango-infra-persistence-starter -am test checkstyle:check` | DONE | 本文件第 8 节 |
| ISSUE-178-003 | 任务验证发现 | 授权 starter 聚合测试可编译通过 | 将 `IAuthorizationAppService` 补齐 `MangoCrudService<AuthorizationApp>` 泛型契约 | `IAuthorizationAppService` | `mvn -f mango/pom.xml -pl :mango-authorization-starter -am test` | DONE | 本文件第 8 节 |
| ISSUE-178-004 | PMO 提交门禁 | 提交前完成交付范围验证和格式检查 | 执行 Maven 检查、台账检查和 diff 空白检查 | 验证记录 | `delivery-contract-check`、`git diff --check` | DONE | 本文件第 8 节 |

## 8. 验收证据记录

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| ISSUE-178-001 | Spring Boot 自动配置 | 授权自动配置提供 `DataScopeProvider` 后注册 `DataScopeApplier` | 测试上下文导入持久化和授权自动配置 | 存在单个 `DataScopeProvider` 和单个 `DataScopeApplier` Bean | 不涉及 | 不涉及 | `mvn -f mango/pom.xml -pl :mango-infra-persistence-starter -am test` 通过 | PASS |
| ISSUE-178-002 | Spring Boot 自动配置 | provider 存在和缺失分支 | 测试上下文分别提供或不提供 `DataScopeProvider` | provider 存在时创建 applier，provider 缺失时不创建 applier | 不涉及 | 不涉及 | `mvn -f mango/pom.xml -pl :mango-infra-persistence-starter -am test checkstyle:check` 通过 | PASS |
| ISSUE-178-003 | Maven 编译测试 | 授权 core 服务泛型契约 | 授权 starter 聚合模块 | `mango-authorization-core` 编译通过，授权 starter 测试通过 | 不涉及 | 不涉及 | `mvn -f mango/pom.xml -pl :mango-authorization-starter -am test` 通过 | PASS |
| ISSUE-178-004 | 提交门禁 | 台账和格式检查 | 当前任务改动文件 | 台账状态均为 `DONE`，diff 无尾随空白 | 不涉及 | 不涉及 | `delivery-contract-check` 和 `git diff --check` 通过 | PASS |
