# Mango 多数据源底座实现交付台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| MDS-DEV-001 | 开发计划 Sprint 1 | 新增多数据源配置和注册表，未配置时保持单数据源兼容 | 仅在 `mango.persistence.datasources` 存在时启用 Mango 托管多数据源 | `PersistenceDataSourceAutoConfiguration`、`PersistenceDataSourceProperties`、`PersistenceDataSourceRegistry` | `mvn -pl mango-infra/mango-infra-persistence/mango-infra-persistence-starter test` | DONE | `mango-infra-persistence-starter/src/test/java/io/mango/infra/persistence/starter/datasource/PersistenceDataSourceAutoConfigurationTest.java` |
| MDS-DEV-002 | 开发计划 Sprint 2 | 支持运行期路由、注解路由和模块数据源映射 | Mapper 仍只看到一个路由 `DataSource`，通过上下文、注解和模块映射选择目标库 | `MangoRoutingDataSource`、`PersistenceDataSourceContext`、`PersistenceDataSource`、`PersistenceModuleDataSourceResolver` | `mvn -pl mango-infra/mango-infra-persistence/mango-infra-persistence-starter test` | DONE | `mango-infra-persistence-starter/src/test/java/io/mango/infra/persistence/starter/datasource/PersistenceDataSourceAspectTest.java` |
| MDS-DEV-003 | 开发计划 Sprint 3 | 事务内禁止静默切换数据源 | 事务内记录已使用数据源，切换到其它数据源时抛出明确异常 | `MangoRoutingDataSource`、`PersistenceDataSourceContext` | `mvn -pl mango-infra/mango-infra-persistence/mango-infra-persistence-starter test` | DONE | `mango-infra-persistence-starter/src/test/java/io/mango/infra/persistence/starter/datasource/MangoRoutingDataSourceTransactionTest.java` |
| MDS-DEV-004 | 开发计划 Sprint 4 | Flyway 模块迁移支持统一数据源注册表和模块映射，保留旧独立 datasource 配置 | 新模块映射优先，旧 `mango.persistence.flyway.modules.<module>.datasource` 继续可用 | `PersistenceFlywayAutoConfiguration` | `mvn -pl mango-infra/mango-infra-persistence/mango-infra-persistence-starter test` | DONE | `mango-infra-persistence-starter/src/test/java/io/mango/infra/persistence/starter/PersistenceFlywayAutoConfigurationTest.java` |
| MDS-DEV-005 | 开发计划 Sprint 5 | 更新 persistence README，说明 Job 和 PowerJob 独立库、限制和配置方式 | 文档只描述使用方式和边界，不把设计文档作为长期规范源 | `mango-infra-persistence/README.md` | 人工检查 README，执行台账检查 | DONE | `mango/mango-infra/mango-infra-persistence/README.md` |
| MDS-DEV-006 | 用户补充要求 | 模块相关配置内聚，模块可声明默认逻辑数据源，部署配置可覆盖，验证真实 MyBatis-Plus 执行线路 | 解析顺序为部署覆盖、`module.properties` 默认、`primary`；模块默认指向未注册数据源时回退主库；完整 Spring Boot 场景确保 Mango 路由数据源先于 Druid 接管 | `PersistenceModuleDataSourceDefaults`、`DefaultPersistenceModuleDataSourceResolver`、`PersistenceDataSourceAutoConfiguration` | `mvn -pl mango-infra/mango-infra-persistence/mango-infra-persistence-starter -Dtest=PersistenceDataSourceAutoConfigurationTest,PersistenceMybatisPlusMultiDataSourceIntegrationTest test` | DONE | `mango-infra-persistence-starter/src/test/java/io/mango/infra/persistence/starter/datasource/PersistenceMybatisPlusMultiDataSourceIntegrationTest.java` |

## 验收证据记录

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| MDS-DEV-001 | 后端 starter | 多数据源注册和单数据源兼容 | H2 `primary`、`job` | 未配置时无注册表；配置后注册 `primary`、`job` | 不涉及页面 | 不涉及前端网络 | Maven test 输出 | DONE |
| MDS-DEV-002 | 后端 starter | 运行期和注解路由 | H2 `primary_route`、`job_route`、`primary_aspect`、`job_aspect` | 上下文和注解均可路由到 `job` | 不涉及页面 | 不涉及前端网络 | Maven test 输出 | DONE |
| MDS-DEV-003 | 后端 starter | 事务内切换保护 | H2 `primary_tx`、`job_tx` | 事务内从 `primary` 切到 `job` 抛出 `Cannot switch Mango datasource inside one transaction` | 不涉及页面 | 不涉及前端网络 | Maven test 输出 | DONE |
| MDS-DEV-004 | 后端 starter | Flyway 模块迁移到独立库 | H2 `module_flyway_independent`、`flyway_job_registry` | 模块表和 history table 出现在目标库，不出现在默认库 | 不涉及页面 | 不涉及前端网络 | Maven test 输出 | DONE |
| MDS-DEV-005 | 文档 | README 使用说明 | 不适用 | README 包含配置示例、事务限制、Flyway 迁移和 Job/PowerJob 数据库边界 | 不涉及页面 | 不涉及前端网络 | README 文件 | DONE |
| MDS-DEV-006 | 后端 starter | 模块默认数据源和 MyBatis-Plus 真实路由 | H2 `mybatis_primary`、`mybatis_job` | `mango-job` 默认解析到 `job`；`BaseMapper.insert/selectById/deleteById` 分别写入和读取主库、job 库，数据互不覆盖 | 不涉及页面 | 不涉及前端网络 | Maven test 输出 | DONE |
