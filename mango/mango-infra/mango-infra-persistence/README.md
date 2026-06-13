# Mango Infra Persistence

## 1. 能力定位

`mango-infra-persistence` 提供关系型持久化基础设施，覆盖实体基类、CRUD Service、MyBatis-Plus 装配、Flyway、多数据源、审计填充、租户字段、Schema 校验和 Web CRUD 基础 Controller。主要使用者是后端基础设施开发者和业务模块开发者。

代码事实：

- 聚合模块 `io.mango.infra.persistence:mango-infra-persistence`。
- 子模块包括 `mango-infra-persistence-api`、`mango-infra-persistence-starter`、`mango-infra-persistence-web-starter`。
- 配置前缀覆盖 `mango.persistence`、`mango.persistence.flyway`、`mango.persistence.schema-validation`、`mango.persistence.audit`，多数据源定义位于 `mango.persistence.datasources`，模块映射位于 `mango.persistence.modules`。

## 2. 适用场景

- 业务模块需要标准实体基类、分页模型、查询注解和 CRUD Service。
- 应用需要 MyBatis-Plus、Flyway、审计填充和多数据源自动配置。
- Web 模块需要默认 CRUD Controller 和导入导出扩展点。
- 模块数据库需要按 module 映射到不同数据源。

## 3. 不适用场景

- 不负责具体业务表结构设计和领域事务边界。
- 不替代业务模块的复杂查询、复杂聚合和领域权限判断。
- 不提供 Excel 实现本身，Excel 具体实现由相关 Excel starter 提供。
- 不把长期数据库规范复制进 README。

## 4. 模块边界

`api` 提供轻量契约和注解，`starter` 提供运行时持久化装配，`web-starter` 提供 Web CRUD 入口。业务模块负责实体、Mapper、Service、migration 和业务校验。

## 5. 接入方式

只使用契约模型：

```xml
<dependency>
    <groupId>io.mango.infra.persistence</groupId>
    <artifactId>mango-infra-persistence-api</artifactId>
</dependency>
```

使用持久化运行时：

```xml
<dependency>
    <groupId>io.mango.infra.persistence</groupId>
    <artifactId>mango-infra-persistence-starter</artifactId>
</dependency>
```

使用 Web CRUD 基础能力：

```xml
<dependency>
    <groupId>io.mango.infra.persistence</groupId>
    <artifactId>mango-infra-persistence-web-starter</artifactId>
</dependency>
```

## 6. 配置项

已发现配置前缀：

- `mango.persistence`：持久化主配置、多数据源和模块路由。
- `mango.persistence.flyway`：Flyway 迁移配置。
- `mango.persistence.schema-validation`：启动期表结构校验开关。
- `mango.persistence.audit`：审计填充开关。
- `mango.persistence.datasources`：数据源定义。
- `mango.persistence.modules`：模块到数据源映射。
- `mango.persistence.mybatis-plus.tenant`：租户行级隔离配置。
- `mango.persistence.schema-validation.excluded-tables`：启动期结构校验排除表。

字段以 `PersistenceProperties`、`PersistenceFlywayProperties`、`PersistenceDataSourceProperties` 等配置类为准。

最小单库配置关注 `mango.persistence.flyway.enabled`、`mango.persistence.audit.enabled`、`mango.persistence.mybatis-plus.pagination.*`、`mango.persistence.mybatis-plus.tenant.*`。多数据源配置使用 `mango.persistence.datasources.<name>` 定义数据源，并用 `mango.persistence.modules.<module>.datasource` 绑定模块。

## 7. 对外接口 / 扩展点

- 实体基类：`BaseEntity`、`TenantEntity`、`AuditableEntity`。
- Repository / Service：`PersistenceRepository`、`MangoCrudService`。
- 查询注解：`@QueryField`、`@QueryIgnore`、`QueryType`。
- 数据源：`@PersistenceDataSource`、`PersistenceDataSourceAspect`、`PersistenceDataSourceContext`、`PersistenceModuleDataSourceResolver`。
- 上下文：`PersistenceContextProvider`。
- 权限扩展：`DataScopeProvider`、`TenantProvider`。
- Web：`BaseCrudController`、Excel import/export 相关注解和服务扩展点。

## 8. 数据库 / 初始化数据

本模块自身未发现生产 Flyway 表结构；测试资源中存在 persistence 测试 migration。业务模块应在自身 `src/main/resources/db/migration/<module>` 下维护表结构。

## 9. 菜单 / 权限 / 租户

本模块不提供菜单。租户能力体现在实体基类、审计填充、租户 Provider 和查询过滤扩展中；具体菜单权限由业务模块通过 authorization 注册。

## 10. 验证方式

最小验证命令：

```bash
mvn -f mango/pom.xml -pl mango-infra/mango-infra-persistence -am test
```

代表性验收：

- Flyway 能扫描并执行模块 migration。
- 审计字段在新增和修改时自动填充。
- 多数据源配置下模块路由到指定数据源。
- `BaseCrudController` 能完成创建、更新、删除、分页和详情查询。

## 11. 业务接入最小闭环

业务模块应定义实体、Mapper、Service、Controller 和 `src/main/resources/db/migration/<module>` 下的 migration。单表 CRUD 可继承基础 Service/Controller，复杂聚合查询和领域事务仍放在业务模块内部实现。

多数据源场景通过模块映射或 `@PersistenceDataSource` 选择数据源，避免在同一事务中随意切换数据源。验收断言覆盖：migration 执行、审计字段自动填充、租户字段过滤生效、分页返回正确、跨租户查询被隔离。

## 12. 常见问题

- migration 未执行时检查模块路径、Flyway 配置和数据源映射。
- 审计字段为空时检查 Mango 请求上下文和 audit 开关。
- 多数据源事务异常时检查是否在事务内切换数据源。

## 13. 关联 PMO 规则

- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [持久化规范](../../../mango-pmo/rules/backend/07-persistence.md)
- [交付质量门禁](../../../mango-pmo/rules/05-ai-delivery-quality.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 14. 历史设计 / 交付记录

- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
