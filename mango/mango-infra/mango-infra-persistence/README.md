# Mango Infra Persistence

`mango-infra-persistence` 是 Mango 的关系型数据库基础设施模块。业务模块用它接入 MyBatis-Plus、标准实体、分页结果、查询条件构造、审计字段自动填充、租户行级过滤、模块化 Flyway、多数据源路由、启动期表结构校验和基础 CRUD Controller。

## 1. 概览
本模块面向后端业务开发者和基础能力开发者，目标是把“业务表怎么接入 Mango 持久化”这件事标准化。

子模块：

| 子模块 | Maven 坐标 | 用途 |
|--------|------------|------|
| `mango-infra-persistence-api` | `io.mango.infra.persistence:mango-infra-persistence-api` | 实体基类、分页结果、CRUD 契约、查询注解、租户和数据范围扩展契约 |
| `mango-infra-persistence-starter` | `io.mango.infra.persistence:mango-infra-persistence-starter` | MyBatis-Plus、Flyway、多数据源、审计填充、Schema 校验自动配置 |
| `mango-infra-persistence-web-starter` | `io.mango.infra.persistence:mango-infra-persistence-web-starter` | 标准 CRUD Controller、Excel 导入导出扩展入口 |

核心能力：

- 实体基类：`BaseEntity`、`AuditableEntity`、`TenantEntity`。
- CRUD 服务：`MangoCrudService`、`MangoCrudServiceImpl`，覆盖创建、更新、删除、批量删除、详情、列表、分页。
- 查询条件：`@QueryField`、`@QueryIgnore`、`QueryType`，自动构造 MyBatis-Plus `QueryWrapper`。
- 分页：默认注册 `PaginationInnerInterceptor`，返回 `PersistencePageResult`。
- 租户隔离：默认注册 `TenantLineInnerInterceptor`，从 `MangoContextHolder.tenantId()` 读取租户。
- 审计填充：新增和修改时自动填充创建人、创建时间、更新人、更新时间、租户字段。
- Flyway：按 `db/migration/<module>/V*.sql` 分模块迁移，每个模块独立 history table。
- 多数据源：支持定义多个数据源、按模块映射、按 `@PersistenceDataSource` 或代码作用域切换。
- Schema 校验：启动时检查业务表主键和审计租户字段。
- Web CRUD：提供标准 `/create`、`/update`、`/delete`、`/batch-delete`、`/detail`、`/page`、`/export`、`/import`、`/import-template` 入口。

## 2. 功能清单

| 能力 | 常用入口 |
|------|----------|
| 业务模块新增 MySQL 或兼容 JDBC 的业务表 | Maven 依赖 / starter / Java API |
| 业务实体需要统一 Long 雪花主键、审计字段和租户字段 | Maven 依赖 / starter / Java API |
| 单表或薄业务逻辑资源需要快速实现 CRUD API | Maven 依赖 / starter / Java API |
| 模块需要在启动时自动执行自己的 Flyway migration | Maven 依赖 / starter / Java API |
| 应用需要把不同模块路由到不同数据库 | Maven 依赖 / starter / Java API |
| 非 Web 任务、定时任务或测试环境需要显式配置默认租户 | Maven 依赖 / starter / Java API |
| 管理端资源需要复用标准导入导出入口，但 Excel 解析实现由其他模块提供 | Maven 依赖 / starter / Java API |

## 3. 适用场景
- 业务模块新增 MySQL 或兼容 JDBC 的业务表。
- 业务实体需要统一 Long 雪花主键、审计字段和租户字段。
- 单表或薄业务逻辑资源需要快速实现 CRUD API。
- 模块需要在启动时自动执行自己的 Flyway migration。
- 应用需要把不同模块路由到不同数据库。
- 非 Web 任务、定时任务或测试环境需要显式配置默认租户。
- 管理端资源需要复用标准导入导出入口，但 Excel 解析实现由其他模块提供。

## 4. 边界说明
- 不替代业务领域建模、复杂聚合查询、跨聚合事务和业务校验。
- 不提供 Excel 实现本身；`web-starter` 只定义 `ExcelAdapter` 接口和 Controller 调用流程。
- 不自动实现数据范围过滤；`DataScopeProvider` 是扩展契约，`MangoCrudServiceImpl.applyDataScope()` 需要业务服务覆写。
- 不自动让 `@IgnoreTenant` 生效；当前代码里它只是 API 契约标记，租户拦截器没有读取这个注解。
- 不提供菜单和按钮权限资源；业务模块仍要通过自己的 resource manifest 或 authorization 初始化。
- 不建议在一个 Spring 事务中切换数据源；路由数据源会直接拒绝。

## 5. 模块组成
`api` 只放业务可依赖的轻量契约；`starter` 负责运行时持久化装配；`web-starter` 负责标准 HTTP CRUD 外壳。

业务模块自己负责：

- 实体类、Mapper、Service、Controller。
- `src/main/resources/db/migration/<module>/V*.sql` 表结构。
- 资源菜单、按钮权限、API 权限的初始化。
- 数据范围、复杂查询、业务唯一性和业务事务。
- 导入导出行模型、业务校验和 Excel 具体实现依赖。

## 6. 接入方式
只使用实体、查询注解、分页模型和契约：

```xml
<dependency>
    <groupId>io.mango.infra.persistence</groupId>
    <artifactId>mango-infra-persistence-api</artifactId>
</dependency>
```

业务服务需要 MyBatis-Plus、Flyway、审计、租户和多数据源自动配置：

```xml
<dependency>
    <groupId>io.mango.infra.persistence</groupId>
    <artifactId>mango-infra-persistence-starter</artifactId>
</dependency>
```

需要继承标准 CRUD Controller 或使用导入导出入口：

```xml
<dependency>
    <groupId>io.mango.infra.persistence</groupId>
    <artifactId>mango-infra-persistence-web-starter</artifactId>
</dependency>
```

只引入 `api` 不会注册 MyBatis-Plus 插件、Flyway、审计填充、多数据源和 Controller。业务应用要让能力生效，至少需要运行时引入 `mango-infra-persistence-starter`。

## 7. 配置说明
### 6.1 最小单库配置

如果应用已经用 Spring Boot 或 Druid 提供单个 `DataSource`，可以只配置 Spring 数据源，Mango 会复用这个 `DataSource`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/mango_app?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: mango
    password: mango
    driver-class-name: com.mysql.cj.jdbc.Driver

mango:
  persistence:
    flyway:
      enabled: true
    mybatis-plus:
      tenant:
        enabled: true
    schema-validation:
      enabled: true
      fail-fast: false
```

默认租户拦截是开启的。Web 请求必须由认证上下文写入 `MangoContextHolder.tenantId()`；定时任务、测试或离线脚本如果没有上下文，要配置 `mango.persistence.mybatis-plus.tenant.default-tenant-id`，否则租户 SQL 会抛出 `Missing tenant context for tenant-isolated SQL`。

### 6.2 `mango.persistence.*`

来源：`PersistenceProperties`，配置前缀为 `mango.persistence`。

| 配置 | 默认值 | 含义 |
|------|--------|------|
| `flyway.enabled` | `true` | 是否启用 Mango 管理的模块化 Flyway 迁移 |
| `mybatis-plus.pagination.enabled` | `true` | 是否注册默认分页插件 |
| `mybatis-plus.pagination.overflow` | `false` | 页码溢出时是否回到首页 |
| `mybatis-plus.pagination.max-limit` | `500` | 单页最大条数 |
| `mybatis-plus.pagination.db-type` | 空 | 数据库类型；为空时 MyBatis-Plus 自动判断 |
| `mybatis-plus.tenant.enabled` | `true` | 是否注册租户行级拦截器 |
| `mybatis-plus.tenant.column` | `tenant_id` | 租户字段名 |
| `mybatis-plus.tenant.default-tenant-id` | 空 | 非 Web 场景无上下文时使用的默认租户 |
| `mybatis-plus.tenant.excluded-tables` | 见下表 | 不追加租户条件的表，支持以 `*` 结尾的前缀匹配 |
| `audit.enabled` | `true` | 是否注册 MyBatis-Plus `MetaObjectHandler` 自动填充审计字段 |
| `schema-validation.enabled` | `true` | 是否在应用启动时校验数据库表结构 |
| `schema-validation.fail-fast` | `false` | 发现结构问题时是否启动失败 |
| `schema-validation.required-columns` | `created_by`、`created_at`、`updated_by`、`updated_at`、`tenant_id` | 每张非排除业务表必须存在的字段 |
| `schema-validation.excluded-tables` | 见下表 | 不参与启动期结构校验的表，支持以 `*` 结尾的前缀匹配 |

租户默认排除表：

```text
flyway_schema_history*
databasechangelog
databasechangeloglock
kv_record
infra_kv_entry
sys_tenant
sys_config
sys_dict_type
sys_dict_data
sys_area
authorization_api_resource
authorization_permission
authorization_menu
authorization_app
authorization_app_login_context
authorization_app_module
frontend_app_registry
frontend_menu_runtime_config
frontend_module_runtime_strategy
identity_user
tenant_member
tenant_member_org
```

Schema 校验默认排除表：

```text
flyway_schema_history*
databasechangelog
databasechangeloglock
kv_record
infra_kv_entry
sys_login_log
sys_operation_log
authorization_app_module
frontend_app_registry
frontend_menu_runtime_config
frontend_module_runtime_strategy
```

如果某张平台字典表、全局配置表或历史表不应该有 `tenant_id`，必须显式加入两个排除列表中对应的那个列表。只加到租户排除不会跳过 Schema 校验；只加到 Schema 排除也不会跳过租户 SQL 追加。

### 6.3 多数据源配置

来源：`PersistenceDataSourceProperties`，配置前缀同样是 `mango.persistence`。只有配置了 `mango.persistence.datasources` 时，Mango 管理的数据源自动配置才会激活；否则走应用已有的 `DataSource`。

```yaml
mango:
  persistence:
    datasources:
      primary:
        primary: true
        url: jdbc:mysql://127.0.0.1:3306/mango_primary
        username: mango
        password: mango
        driver-class-name: com.mysql.cj.jdbc.Driver
      job:
        url: jdbc:mysql://127.0.0.1:3306/mango_job
        username: mango
        password: mango
        driver-class-name: com.mysql.cj.jdbc.Driver
    modules:
      mango-job:
        datasource: job
```

| 配置 | 默认值 | 含义 |
|------|--------|------|
| `datasources.<name>.primary` | `false` | 是否为默认数据源；没有任何 `primary=true` 时默认名是 `primary` |
| `datasources.<name>.url` | 空 | JDBC URL；为空的定义不会注册 |
| `datasources.<name>.driver-class-name` | 空 | JDBC 驱动类名 |
| `datasources.<name>.username` | 空 | 数据库用户名 |
| `datasources.<name>.password` | 空 | 数据库密码 |
| `modules.<module>.datasource` | 空 | 模块名到数据源名的映射 |
| `datasource-routing.annotation-enabled` | `true` | 是否启用 `@PersistenceDataSource` AOP 路由 |

模块默认数据源还可以由 jar 包里的 `META-INF/mango/module.properties` 声明：

```properties
module-name=mango-job
persistence-datasource=job
```

配置项 `mango.persistence.modules.<module>.datasource` 优先级高于 `module.properties` 默认值。

### 6.4 Flyway 配置

来源：`PersistenceFlywayProperties`，配置前缀为 `mango.persistence.flyway`。

如果不配置 `modules`，starter 会扫描所有 `classpath*:db/migration/*/V*.sql`，把中间目录名当作模块名，并按模块名排序执行。每个模块默认使用独立 history table，例如模块 `mango-job` 的默认表是 `flyway_schema_history_mango_job`。

显式配置示例：

```yaml
mango:
  persistence:
    flyway:
      enabled: true
      modules:
        mango-system:
          enabled: true
          baseline-on-migrate: true
          out-of-order: false
          validate-on-migrate: true
          ignore-missing-migrations: false
          history-table: flyway_schema_history_mango_system
        mango-job:
          enabled: true
```

| 配置 | 默认值 | 含义 |
|------|--------|------|
| `enabled` | `true` | 全局迁移开关 |
| `modules.<module>.enabled` | `true` | 是否执行当前模块迁移 |
| `modules.<module>.baseline-on-migrate` | `true` | 存量库无 history table 时是否从 baseline 接管 |
| `modules.<module>.out-of-order` | `false` | 是否允许非顺序版本补跑 |
| `modules.<module>.history-table` | `flyway_schema_history_<module>` | 当前模块 history table，模块名会把非字母数字下划线替换成 `_` |
| `modules.<module>.validate-on-migrate` | `true` | 迁移前是否校验历史记录 |
| `modules.<module>.ignore-missing-migrations` | `false` | 是否忽略数据库存在但代码已移除的历史迁移 |
| `modules.<module>.datasource.url` | 空 | 当前模块迁移使用独立 JDBC URL |
| `modules.<module>.datasource.driver-class-name` | 空 | 当前模块迁移独立驱动 |
| `modules.<module>.datasource.username` | 空 | 当前模块迁移独立用户名 |
| `modules.<module>.datasource.password` | 空 | 当前模块迁移独立密码 |

模块迁移数据源解析顺序：

1. 多数据源 registry 中的模块映射。
2. 当前模块 `modules.<module>.datasource.url` 临时数据源。
3. 应用默认 `DataSource`。

starter 会注册一个 `_noop` Flyway bean，避免 Spring Boot 默认 Flyway 把所有模块脚本合并到一个 history table 中执行。

## 8. API 与扩展
### 7.1 实体和分页模型

| 类型 | 字段 / 行为 |
|------|-------------|
| `BaseEntity` | `Long id`，`@TableId(type = IdType.ASSIGN_ID)`，默认雪花 ID |
| `AuditableEntity` | 继承 `BaseEntity`，增加 `createdBy`、`createdAt`、`updatedBy`、`updatedAt` |
| `TenantEntity` | 继承 `AuditableEntity`，增加 `String tenantId` |
| `PersistencePageResult<T>` | `records`、`total`、`page`、`size`、`pages` |

实体示例：

```java
package com.example.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("order_invoice")
public class OrderInvoiceEntity extends TenantEntity {

    private String invoiceNo;

    private String status;
}
```

对应表结构至少要满足 Schema 校验：

```sql
CREATE TABLE order_invoice (
    id BIGINT NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    invoice_no VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_by BIGINT NULL,
    created_at DATETIME NOT NULL,
    updated_by BIGINT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id)
);
```

`id` 必须是 `BIGINT` 或数据库等价类型，不能是 `AUTO_INCREMENT`。当前校验还会要求 `created_by`、`created_at`、`updated_by`、`updated_at`、`tenant_id` 存在，除非表在 `schema-validation.excluded-tables` 中。

### 7.2 查询注解

`QueryWrapperBuilder` 会把查询对象字段转换成数据库条件：

| 类型 | 行为 |
|------|------|
| 无注解字段 | 默认 `EQ`，字段名驼峰转下划线 |
| `@QueryField(column = "xxx")` | 指定数据库列名 |
| `@QueryField(type = QueryType.LIKE)` | 生成 like 条件 |
| `@QueryIgnore` | 跳过这个字段 |
| `PageQuery` 声明字段 | 跳过分页基类字段 |
| `Map` 查询 | 除 `page`、`size`、`sorts` 外都按 `EQ` 处理 |

支持的 `QueryType`：

```text
EQ
NE
LIKE
LEFT_LIKE
RIGHT_LIKE
IN
BETWEEN
GE
GT
LE
LT
```

查询对象示例：

```java
package com.example.order.query;

import io.mango.common.po.PageQuery;
import io.mango.infra.persistence.api.crud.QueryField;
import io.mango.infra.persistence.api.crud.QueryIgnore;
import io.mango.infra.persistence.api.crud.QueryType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class OrderInvoiceQuery extends PageQuery {

    @QueryField(type = QueryType.LIKE)
    private String invoiceNo;

    @QueryField(type = QueryType.IN)
    private List<String> statusList;

    @QueryField(column = "created_at", type = QueryType.BETWEEN)
    private List<LocalDateTime> createdAtRange;

    @QueryIgnore
    private String frontendOnly;
}
```

注意：

- 空字符串、空集合和 `null` 不生成条件。
- `BETWEEN` 只处理 `List` 且至少两个元素。
- `IN` 支持集合；对象数组会转为列表。
- `Map` 查询不会读取 `@QueryField`，只按 key 驼峰转下划线。

### 7.3 CRUD Service

业务 Service 可以继承 `MangoCrudServiceImpl<M, E>`：

```java
package com.example.order.service;

import com.example.order.entity.OrderInvoiceEntity;
import com.example.order.mapper.OrderInvoiceMapper;
import io.mango.infra.persistence.starter.crud.MangoCrudServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class OrderInvoiceService
        extends MangoCrudServiceImpl<OrderInvoiceMapper, OrderInvoiceEntity> {

    @Override
    protected Class<OrderInvoiceEntity> entityType() {
        return OrderInvoiceEntity.class;
    }
}
```

默认行为：

| 方法 | 行为 |
|------|------|
| `createByCommand(command)` | BeanUtils 复制命令到实体，调用 `save(entity)`，返回实体 ID |
| `updateByCommand(command)` | BeanUtils 复制命令到实体，调用 `updateById(entity)` |
| `deleteById(id)` | 按实体 ID 类型转换 `String`、`Number` 到 `Long` 或 `Integer` 后删除 |
| `batchDeleteByIds(ids)` | 空列表直接返回 `true`，非空调用 `removeBatchByIds` |
| `detailById(id)` | 查询实体并调用 `toVO(entity)`，没有记录返回 `null` |
| `listByQuery(query)` | 使用 `QueryWrapperBuilder` 查询并转换 VO |
| `pageByQuery(query)` | 读取 `page`、`size` 字段，默认 `1` 和 `10` |

可覆写钩子：

```text
beforeCreate
afterCreate
beforeUpdate
afterUpdate
beforeDelete
afterDelete
beforeBatchDelete
afterBatchDelete
toVO
applyDataScope
entityType
```

`applyDataScope(QueryWrapper<E> wrapper, Object query)` 默认是空实现。如果要接入部门、岗位、用户等数据范围过滤，业务 Service 要在这里追加条件，或显式调用自己的 `DataScopeProvider`。

### 7.4 Web CRUD Controller

`mango-infra-persistence-web-starter` 提供 `BaseCrudController<S, C, U, Q>`：

```java
package com.example.order.controller;

import com.example.order.command.CreateOrderInvoiceCommand;
import com.example.order.command.UpdateOrderInvoiceCommand;
import com.example.order.query.OrderInvoiceQuery;
import com.example.order.service.OrderInvoiceService;
import io.mango.infra.persistence.web.starter.controller.BaseCrudController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/order/invoices")
public class OrderInvoiceController extends BaseCrudController<
        OrderInvoiceService,
        CreateOrderInvoiceCommand,
        UpdateOrderInvoiceCommand,
        OrderInvoiceQuery> {

    public OrderInvoiceController(OrderInvoiceService service) {
        super(service);
    }

    @Override
    protected Class<OrderInvoiceQuery> queryType() {
        return OrderInvoiceQuery.class;
    }
}
```

继承后自动具备以下接口，实际前缀由 Controller 上的 `@RequestMapping` 决定：

| 方法 | 路径 | 入参 | 行为 |
|------|------|------|------|
| `POST` | `/create` | 创建命令 JSON | 调用 `service.createByCommand` |
| `POST` | `/update` | 更新命令 JSON | 调用 `service.updateByCommand` |
| `POST` | `/delete` | `DeleteCommand{id}` | 调用 `service.deleteById` |
| `POST` | `/batch-delete` | `BatchDeleteCommand{ids}` | 调用 `service.batchDeleteByIds` |
| `GET` | `/detail?id=` | 主键 ID | 调用 `service.detailById` |
| `GET` | `/page` | 查询对象 query string | 调用 `service.pageByQuery` |
| `POST` | `/export` | 查询条件 JSON | 需要 Service 实现 `ExportableService` 且存在 `ExcelAdapter` |
| `POST` | `/import` | multipart 文件 | 需要 Service 实现 `ImportableService` 且存在 `ExcelAdapter` |
| `GET` | `/import-template` | 无 | 需要 Service 实现 `ImportableService` 且存在 `ExcelAdapter` |

导入导出限制：

- 没有 `ExcelAdapter` bean 时，导出抛出 `Excel 导出能力未启用`，导入抛出 `Excel 导入能力未启用`。
- `POST /export` 要求 Service 实现 `ExportableService<Q, ROW>`。
- `POST /import` 和 `/import-template` 要求 Service 实现 `ImportableService<ROW>`。
- `POST /import` 的模式可用请求参数 `importMode` 或 `mode` 覆盖，值为 `PARTIAL_SUCCESS` 或 `ALL_SUCCESS`。
- 如果容器里有 `Validator`，导入行会先执行 Bean Validation，再执行业务 `validateImportRows`。
- `PARTIAL_SUCCESS` 会导入校验通过的行；`ALL_SUCCESS` 只要有错误就不导入。

### 7.5 多数据源路由 API

注解方式：

```java
package com.example.order.service;

import io.mango.infra.persistence.starter.datasource.PersistenceDataSource;
import org.springframework.stereotype.Service;

@Service
@PersistenceDataSource("order")
public class OrderReportService {
}
```

方法上的 `@PersistenceDataSource` 优先于类上的注解。AOP 顺序是 `Ordered.HIGHEST_PRECEDENCE`，会尽量在事务切面之前设置当前数据源。

代码作用域方式：

```java
import io.mango.infra.persistence.api.datasource.PersistenceDataSourceContext;

try (PersistenceDataSourceContext.Scope ignored = PersistenceDataSourceContext.use("archive")) {
    // 这里执行的 Mapper 调用路由到 archive 数据源。
}
```

限制：

- 数据源名不存在会抛出 `Mango datasource does not exist: <name>`。
- 同一个实际 Spring 事务内第一次拿到连接后，不能再切换到别的数据源；否则抛出 `Cannot switch Mango datasource inside one transaction`。
- 需要跨库写入时，不要依赖这个模块提供分布式事务；应拆分事务边界或使用业务补偿。

## 9. 数据与初始化
本模块自身没有生产业务表 migration；业务模块必须在自己的模块下维护 migration。

推荐路径：

```text
src/main/resources/db/migration/<module>/V1__init.sql
src/main/resources/db/migration/<module>/V2__add_xxx.sql
```

启动期初始化器：

| 初始化器 | 触发条件 | 幂等边界 | 排查入口 |
|----------|----------|----------|----------|
| `persistenceFlywayMigrationInitializer` | 存在 `DataSource`、classpath 有 Flyway、`mango.persistence.flyway.enabled=true` | Flyway 按模块 history table 记录已执行版本；重复启动不会重复执行同一版本 | 检查对应 `flyway_schema_history_<module>` 表和应用启动日志 |
| `SchemaValidationRunner` | 存在 `DataSource`、`mango.persistence.schema-validation.enabled=true` | 只读取数据库元数据，不写业务数据 | 启动日志出现 `数据库结构校验通过`，或在发现问题时按 `fail-fast` 决定告警或启动失败 |

业务表最小要求：

| 要求 | 说明 |
|------|------|
| `id` | 必须存在，类型是 BIGINT 或数据库等价类型 |
| 主键 | 必须以 `id` 作为 primary key |
| 非自增 | `id` 不能是 `AUTO_INCREMENT`，实体默认使用 MyBatis-Plus 雪花 ID |
| 审计字段 | 默认要求 `created_by`、`created_at`、`updated_by`、`updated_at` |
| 租户字段 | 默认要求 `tenant_id`，并参与租户行级过滤 |
| Flyway 路径 | 模块脚本放在 `db/migration/<module>/V*.sql` |

菜单、按钮和 API 权限不是 persistence 初始化的内容。业务模块如果提供管理页面，需要在自己的 resource manifest 或 authorization 初始化逻辑中登记菜单、按钮权限和 API 资源。

## 10. 管理入口
本模块不注册菜单和按钮权限。它只影响数据库层面的租户和审计。

租户行为：

- 默认 `mango.persistence.mybatis-plus.tenant.enabled=true`，所有非排除表 SQL 会自动追加租户条件。
- 租户值优先读取 `MangoContextHolder.tenantId()`，没有时读取 `default-tenant-id`。
- 租户值全是数字时生成数值表达式，否则生成字符串表达式。
- insert 语句如果已经包含租户列，租户拦截器不会重复插入。
- `TenantEntity.tenantId` 在 insert 时由审计填充器从上下文写入，支持实体 setter 类型为 `String` 或 `Long`。

审计行为：

| 场景 | 自动填充字段 |
|------|--------------|
| insert | `createdBy`、`createdAt`、`createTime`、`updatedBy`、`updatedAt`、`updateTime`、`tenantId` |
| update | `updatedBy`、`updatedAt`、`updateTime` |

时间字段支持 `LocalDateTime`、`Instant`、`Date`。`createdBy` 和 `updatedBy` 来自 `MangoContextHolder.userId()` 对应的 persistence context。

当前注意点：

- `@IgnoreTenant` 目前不是自动绕过租户拦截的开关。需要绕过租户的表，应配置到 `mybatis-plus.tenant.excluded-tables`。
- 平台全局表、字典表、资源表通常应该加入租户排除；普通业务表不应加入。
- 如果业务接口要做按钮权限校验，需要在 Web、安全或 authorization 层声明，不由 persistence 处理。

## 11. 快速开始
1. 引入 `mango-infra-persistence-starter`；需要标准 Controller 时再引入 `mango-infra-persistence-web-starter`。
2. 配置应用 `DataSource`，或配置 `mango.persistence.datasources` 多数据源。
3. 为业务模块创建 `db/migration/<module>/V1__init.sql`，表里包含 `id`、审计字段和 `tenant_id`。
4. 实体继承 `TenantEntity` 或按同名字段自定义实体，Mapper 继承 MyBatis-Plus `BaseMapper<E>`。
5. 查询对象继承项目分页查询基类，按需使用 `@QueryField` 和 `@QueryIgnore`。
6. Service 继承 `MangoCrudServiceImpl`，覆写 `entityType()`；复杂数据范围覆写 `applyDataScope()`。
7. Controller 继承 `BaseCrudController` 并声明资源路径；导入导出能力按需实现 `ExportableService` 或 `ImportableService`。
8. 给管理页面登记菜单、按钮权限和 API 权限；这一步属于业务模块自己的 authorization 资源初始化。
9. 本地验证 Flyway、审计填充、租户隔离、分页查询和 Schema 校验。

## 12. 问题排查
**启动时报 `Missing tenant context for tenant-isolated SQL`**

当前 SQL 命中了租户拦截器，但上下文里没有租户。Web 请求要检查认证和上下文写入；定时任务或测试可配置 `mango.persistence.mybatis-plus.tenant.default-tenant-id`；全局表要加入 `mybatis-plus.tenant.excluded-tables`。

**表结构校验提示缺少 `tenant_id` 或审计字段**

普通业务表应该补字段。平台全局表、历史表或第三方表如果确实不归 Mango 业务规范管理，加入 `mango.persistence.schema-validation.excluded-tables`。

**Flyway 没执行业务模块 migration**

检查脚本路径是否是 `db/migration/<module>/V*.sql`，检查 `mango.persistence.flyway.enabled` 和 `mango.persistence.flyway.modules.<module>.enabled`，再检查模块脚本是否已经被当前模块 history table 记录。

**多数据源切换在事务里失败**

这是预期保护。一个事务内已经绑定了某个 Mango 数据源后不能切换。把不同数据源写入拆到不同事务边界，或重新设计跨库流程。

**`@IgnoreTenant` 标了但 SQL 仍然追加租户条件**

当前 starter 没有读取这个注解。需要绕过租户的表请配置 `mango.persistence.mybatis-plus.tenant.excluded-tables`。

**导入导出接口存在但调用失败**

`BaseCrudController` 只是提供入口。导出需要 Service 实现 `ExportableService`，导入需要 Service 实现 `ImportableService`，并且容器里必须有 `ExcelAdapter` bean。

## 13. 相关文档
- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [持久化规范](../../../mango-pmo/rules/backend/07-persistence.md)
- [交付质量门禁](../../../mango-pmo/rules/05-ai-delivery-quality.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 14. 历史资料
- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
