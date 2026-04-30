# mango-infra-persistence

> 关系型持久化基础设施，统一封装数据库迁移、数据源、事务、MyBatis-Plus、JdbcTemplate 和仓储基础契约。

## 已实现

- **Flyway 数据库迁移** - 按模块加载 `db/migration/{module}` 下的迁移脚本
- **MyBatis-Plus 集成** - 统一注册分页等基础插件
- **Spring DataSource 集成** - 对外隐藏底层数据源和连接池装配细节
- **事务基础配置** - 统一启用 Spring 事务管理
- **Repository 契约** - 提供不绑定具体实现的仓储基础接口
- **审计字段填充** - 基于 MangoContext 自动填充创建人、创建时间、更新人、更新时间和租户
- **启动期结构校验** - 基于数据库 metadata 校验业务表是否携带审计字段和租户字段
- **分页/排序/查询对象** - 提供持久化层基础分页查询和分页结果对象
- **数据范围扩展点** - 提供租户、数据权限、持久化上下文 SPI
- **多数据库驱动** - MySQL、PostgreSQL、H2 测试数据库

## 依赖

```xml
<dependency>
    <groupId>io.mango</groupId>
    <artifactId>mango-infra-persistence-starter</artifactId>
</dependency>
```

## 配置示例

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/mango?useUnicode=true&characterEncoding=utf-8
    username: root
    password: ${DB_PASSWORD}
    druid:
      initial-size: 5
      min-idle: 5
      max-active: 20

mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  type-aliases-package: io.mango.**.domain
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0

mango:
  persistence:
    flyway:
      enabled: true
      modules:
        authorization:
          enabled: true
    mybatis-plus:
      pagination:
        enabled: true
        max-limit: 500
        overflow: false
    audit:
      enabled: true
    schema-validation:
      enabled: true
      fail-fast: false
      required-columns:
        - created_by
        - created_at
        - updated_by
        - updated_at
        - tenant_id
      excluded-tables:
        - flyway_schema_history
        - databasechangelog
        - databasechangeloglock
        - kv_record
        - infra_kv_entry
        - sys_login_log
        - sys_operation_log
```

## 表结构准入

所有 Mango 业务主数据、配置表、授权表默认必须携带以下字段：

| 字段 | 说明 |
|------|------|
| `created_by` | 创建人 ID |
| `created_at` | 创建时间 |
| `updated_by` | 更新人 ID |
| `updated_at` | 更新时间 |
| `tenant_id` | 租户标识 |

技术基础设施表、追加型日志表、外部系统同步表不强制使用这组字段，但必须显式说明豁免原因。准入由三层机制保证：

- `mango:gen-crud` 默认生成带标准字段的 Flyway 建表脚本。
- `mvn mango:check -Drule=persistence-schema` 在 CI 阶段扫描 `src/main/resources/db/migration` 下的 `CREATE TABLE`。
- 启动期 `schema-validation` 通过 JDBC metadata 校验实际数据库，避免历史库或手工改库漏字段。

确实不应该携带标准字段的表，需要在建表语句前显式标记：

```sql
-- mango-check: disable persistence-audit-fields reason=外部系统同步表或基础设施运行态表
CREATE TABLE external_event (
  `id` bigint NOT NULL,
  PRIMARY KEY (`id`)
);
```

## 待实现

| 功能 | 状态 | 说明 |
|------|------|------|
| 多数据源自动配置 | 待开发 | 动态数据源切换 |
| 分布式事务 | 待开发 | Seata 等能力统一封装 |
| SQL 审计日志 | 待开发 | 记录所有执行的 SQL |
| 分库分表支持 | 待开发 | ShardingSphere 集成 |
| 主从复制读写分离 | 待开发 | 路由策略配置 |

## 设计决策

- 业务模块依赖 `mango-infra-persistence-starter`，不直接感知底层持久化组件
- 底层 MyBatis-Plus、DataSource、事务和迁移组件由本模块统一封装
- H2 仅用于本地和测试场景
- 后续数据权限、乐观锁、软删除等关系型持久化能力继续收敛到本模块
