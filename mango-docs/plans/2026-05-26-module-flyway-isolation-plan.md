# 模块 Flyway 隔离设计说明

## 目标

实现模块独立 migration 目录、模块独立 Flyway history table，并为模块独立数据库预留可配置能力。

## 范围

- `mango-infra-persistence-starter` 的 Flyway 自动配置。
- `mango.persistence.flyway.modules.*` 配置模型。
- Flyway 模块隔离测试。

## 不做什么

- 不迁移或清理现有数据库。
- 不默认把现有模块切到独立数据库。
- 不修改已有 migration SQL 内容。
- 不调整业务 Mapper 的运行时数据源路由。

## 设计决策

- migration 目录继续使用 `classpath:db/migration/{module}`。
- history table 默认使用 `flyway_schema_history_{module}`，模块名中的非字母数字下划线转为 `_`。
- 模块未配置 datasource 时复用主 `DataSource`，保持现有单库部署。
- 模块配置 `datasource.url` 后，为该模块 Flyway 单独创建迁移数据源，支持模块独立数据库。
- `baseline-on-migrate` 使用模块级配置，不再硬编码开启。
- 支持模块级 `history-table` 覆盖默认表名。

## 接口变化

新增配置项：

```yaml
mango:
  persistence:
    flyway:
      modules:
        identity:
          enabled: true
          baseline-on-migrate: false
          history-table: flyway_schema_history_identity
          datasource:
            url: jdbc:mysql://127.0.0.1:3306/mango_identity
            username: root
            password:
            driver-class-name: com.mysql.cj.jdbc.Driver
```

## 数据变化

- 不新增业务表。
- Flyway history table 按模块独立创建。
- 如果配置独立数据库，migration 在对应模块数据库内执行。

## 测试范围

- 同一数据库内多个模块使用相同 `V1` 不冲突。
- 模块级 history table 可自定义。
- 模块级 `baseline-on-migrate` 生效。
- 模块级 datasource 可把 migration 执行到独立数据库。
