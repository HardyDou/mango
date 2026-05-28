# 业务运行期模块分库设计方案

## 1. 目标

让业务运行期 Mapper 能按模块路由到对应数据源，并和当前模块化 Flyway 配置保持一致。

## 2. 范围

- `mango-infra-persistence-starter` 增加运行期多数据源、模块路由和事务守卫能力。
- `mango-app` 通过配置决定哪些模块使用独立数据库。
- `mango-platform` 业务模块不直接感知数据库连接细节。

## 3. 不做什么

- 不默认启用分库。
- 不默认引入 XA、Seata 或跨库强一致事务。
- 不允许 Mapper 跨域访问其它模块表。
- 不用一次改造要求所有 `@Transactional` 手工指定 transactionManager。

## 4. 关键结论

必须同时设计多数据源和事务边界。只做 Mapper 路由不够，因为 Spring 事务会在方法进入时绑定连接；如果路由 key 设置晚了，事务会绑定到错误数据源。

默认策略：

- 单模块写事务：本地事务，使用模块数据源。
- 同库多模块：兼容旧模式，但仍按模块记录路由。
- 跨模块写事务且数据源不同：默认拒绝，抛出明确异常。
- 跨模块一致性：通过事件、Outbox、Saga 或远程服务编排处理。
- XA/Seata：作为可选增强，不作为默认能力。

## 5. 配置模型

运行期 datasource 和 Flyway 使用同一模块名。

```yaml
mango:
  persistence:
    datasource:
      mode: routing-observe # single | routing-observe | routing-enforce
      modules:
        identity:
          url: jdbc:mysql://127.0.0.1:3306/mango_identity
          username: root
          password:
          driver-class-name: com.mysql.cj.jdbc.Driver
        authorization:
          url: jdbc:mysql://127.0.0.1:3306/mango_authorization
          username: root
          password:
          driver-class-name: com.mysql.cj.jdbc.Driver
    flyway:
      modules:
        identity:
          history-table: flyway_schema_history_identity
          datasource:
            url: jdbc:mysql://127.0.0.1:3306/mango_identity
            username: root
            password:
            driver-class-name: com.mysql.cj.jdbc.Driver
```

`mode` 含义：

- `single`：所有模块使用主数据源。
- `routing-observe`：启用路由但跨数据源事务只记录告警。
- `routing-enforce`：启用路由并拒绝跨数据源本地事务。

## 6. 运行期路由

新增核心组件：

- `MangoDataSourceProperties`：运行期模块数据源配置。
- `ModuleDataSourceRegistry`：维护 `module -> DataSource`。
- `ModuleRoutingDataSource`：基于 `AbstractRoutingDataSource` 选择当前数据源。
- `ModuleDataSourceContext`：保存当前线程模块路由 key。
- `ModuleTransactionGuard`：检查一个本地事务内是否访问多个物理数据源。
- `ModuleMapperRouteInterceptor`：Mapper 执行前根据 mapper namespace 推断模块。
- `ModuleTransactionalRouteAspect`：在 Spring 事务切面之前设置模块路由。

路由来源优先级：

1. 显式上下文：内部框架调用时主动设置。
2. Service 类包名：例如 `io.mango.identity.core.service` 推断 `identity`。
3. Mapper namespace：例如 `io.mango.identity.core.mapper.IdentityUserMapper` 推断 `identity`。
4. 默认主数据源。

## 7. 事务设计

本地事务仍使用 Spring `DataSourceTransactionManager`，底层 DataSource 换成 `ModuleRoutingDataSource`。

事务进入前必须完成路由：

1. `ModuleTransactionalRouteAspect` 以更高优先级运行。
2. 根据 service 所属模块设置 `ModuleDataSourceContext`。
3. Spring `@Transactional` 开启事务并从 routing datasource 获取连接。
4. Mapper 执行时再次校验 mapper 模块和事务绑定模块是否一致。

事务守卫规则：

- 同一事务内第一次访问数据源后，绑定 `transactionId -> datasourceKey`。
- 后续访问同一数据源：允许。
- 后续访问不同数据源：
  - `routing-observe`：记录告警。
  - `routing-enforce`：抛 `CrossDataSourceTransactionException`。

只读查询：

- 无事务的多模块查询允许。
- `@Transactional(readOnly = true)` 默认仍按单数据源事务处理。
- 聚合查询需要跨模块时，通过 API 组合，不做跨库 join。

## 8. 跨库一致性

默认不做跨库 ACID。业务写入跨多个模块数据库时，只允许以下模式：

- 本模块事务提交后写 Outbox，再异步投递领域事件。
- 调用远程模块 API，由目标模块维护自己的本地事务。
- Saga 编排补偿，适用于可补偿业务。

只有强监管、资金账务等确实要求跨库强一致时，单独引入：

- Seata AT/TCC。
- XA/JTA。

这类能力必须显式开启，且需要单独测试和运维方案。

## 9. 模块边界要求

- Mapper 只访问本模块表。
- 跨模块读写必须走 `XxxApi`。
- 单体聚合部署时，本地 starter 实现也视为模块 API 调用，不允许绕过 mapper 边界。
- 平台模块不直接依赖其它模块 core。

## 10. 迁移步骤

1. 增加配置模型和数据源注册表，默认 `single` 模式。
2. 接入 `ModuleRoutingDataSource`，不改变现有业务行为。
3. 接入 Mapper namespace 到模块名的识别。
4. 接入事务前置路由切面。
5. `routing-observe` 灰度，输出跨数据源事务告警。
6. 修正跨模块本地事务，改为 API、事件或 Saga。
7. 对已治理模块开启 `routing-enforce`。
8. 按模块配置独立数据库。

## 11. 验收标准

- 未配置模块数据源时，全仓行为保持单库兼容。
- 配置模块数据源后，该模块 Mapper 写入独立数据库。
- `@Transactional` 方法在事务开启前完成路由。
- 同一事务访问两个不同模块数据源时，`routing-enforce` 模式失败。
- 跨模块查询不使用跨库 join。
- Flyway migration 和运行期 Mapper 使用同一模块数据源配置口径。

## 12. 风险

- 现有单体内可能存在跨模块本地事务，需要先用 observe 模式发现。
- MyBatis/事务切面顺序错误会导致路由失效，必须有集成测试覆盖。
- 多库后无法依赖数据库外键和跨库 join，业务聚合要走 API 或事件。
- 独立数据库增加连接池、监控、备份和发布复杂度。
