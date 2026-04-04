# mango-infra-db

> 数据库基础设施 - MyBatis-Plus、Druid 连接池、多数据源支持

## 已实现

- **MyBatis-Plus 集成** - CRUD增强、分页插件、逻辑删除
- **Druid 连接池** - 阿里巴巴高性能数据库连接池（Spring Boot 3.x 兼容版本）
- **多数据库驱动** - MySQL、PostgreSQL、H2（测试环境）
- **Spring Boot 3.x 兼容** - 使用 `mybatis-spring` 3.0.4 解决兼容性问题

## 依赖

```xml
<dependency>
    <groupId>io.mango</groupId>
    <artifactId>mango-infra-db-starter</artifactId>
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
```

## 待实现

| 功能 | 状态 | 说明 |
|------|------|------|
| 多数据源自动配置 | 待开发 | 动态数据源切换 |
| 数据源健康检查 | 待开发 | Druid StatView |
| SQL 审计日志 | 待开发 | 记录所有执行的 SQL |
| 分库分表支持 | 待开发 | ShardingSphere 集成 |
| 主从复制读写分离 | 待开发 | 路由策略配置 |

## 设计决策

- Druid 使用 `druid-spring-boot-3-starter` 而非默认版本，确保 Spring Boot 3.x 兼容
- MyBatis-Spring 版本锁定为 3.0.4，避免 Spring 6.1 兼容性冲突
- H2 仅在 runtime 范围，用于本地测试
- 支持 MySQL 和 PostgreSQL 双驱动，适配不同业务场景
