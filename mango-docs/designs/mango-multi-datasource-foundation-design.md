# Mango 多数据源底座设计说明

## 1. 目标

为 Mango 建立统一的多数据源底座，支撑模块独立数据库、运行期数据源路由、模块级 Flyway 迁移和清晰事务边界。该能力是后续 `mango-job` 独立治理库、PowerJob 引擎表共置或独立库以及更多模块独立库的前置能力。

## 2. 背景

Mango 计划新增任务调度能力，路线为 Mango 原生 Job 契约和统一 UI，底层优先集成 PowerJob。该路线要求：

- Job 治理数据使用独立数据库，物理库名按 `mango_{module}` 规则为 `mango_job`。
- PowerJob 引擎数据可与 `mango_job` 共置，也可使用独立库或 schema。
- Mango 主业务库、Job 治理库、PowerJob 引擎表边界清晰。
- 单体和微服务部署形态都能通过配置灵活组合。

当前 `mango-infra-persistence` 已具备模块级 Flyway 迁移和模块独立迁移数据源配置，但运行期 Mapper、事务、模块数据源路由仍未形成统一底座。继续直接开发 `mango-job` 会导致 Job 模块承担持久化基础设施职责。

## 3. 范围

本设计覆盖：

- 多数据源配置模型。
- 数据源注册和命名。
- 模块到数据源映射。
- MyBatis-Plus 运行期数据源路由。
- 事务管理器选择和事务边界。
- Flyway 按数据源和模块隔离迁移。
- 多数据源测试支撑。
- 与后续 `mango-job` 和 PowerJob 集成的衔接方式。

## 4. 不做什么

- 不实现 `mango-job` 业务模块。
- 不集成 PowerJob Server 或 Worker。
- 不实现跨库强一致分布式事务。
- 不允许跨库 join 或跨库外键。
- 不改变现有业务模块默认使用主数据源的行为。
- 不新增前端页面、菜单或权限。

## 5. 影响模块

| 模块 | 影响 |
|---|---|
| `mango-infra-persistence-starter` | 新增多数据源注册、路由、模块映射、事务和 Flyway 扩展能力。 |
| `mango-infra-persistence-web-starter` | 原则上不修改；继续复用基础 persistence 能力。 |
| `mango-tools/mango-maven-plugin` | 后续可增加多数据源配置和 migration 检查，本设计只预留规则输入。 |
| `mango-docs/plans` | 增加开发计划和交付记录。 |
| `mango-platform/mango-job` | 后续消费多数据源能力，不在本次实现。 |

## 6. 核心设计

### 6.1 数据源模型

新增统一配置入口：

```yaml
mango:
  persistence:
    datasources:
      primary:
        primary: true
        url: jdbc:mysql://localhost:3306/mango
        username: mango
        password: ${MANGO_DB_PASSWORD}
      job:
        url: jdbc:mysql://localhost:3306/mango_job
        username: mango_job
        password: ${MANGO_JOB_DB_PASSWORD}
      powerjob:
        url: jdbc:mysql://localhost:3306/mango_job
        username: powerjob
        password: ${POWERJOB_DB_PASSWORD}
    modules:
      mango-system:
        datasource: primary
      mango-job:
        datasource: job
      powerjob:
        datasource: job
```

决策：

- `primary` 是默认数据源。
- 未声明模块映射时，模块使用 `primary`。
- 数据源名称使用小写 kebab 或 lower camel，禁止直接在业务代码中硬编码 JDBC URL。
- 密码只允许来自环境变量、配置中心或部署密钥，不写入仓库。

### 6.2 数据源注册

`mango-infra-persistence-starter` 负责根据 `mango.persistence.datasources` 注册：

- `DataSource` 实例。
- 数据源注册表 `PersistenceDataSourceRegistry`。
- 当前数据源上下文 `PersistenceDataSourceContext`。
- 路由数据源 `MangoRoutingDataSource`。

默认行为：

- 只有一个 Spring `dataSource` Bean 对外暴露，类型为路由数据源。
- 路由数据源根据当前上下文选择目标数据源。
- 无上下文时使用 `primary`。

### 6.3 模块数据源映射

新增模块映射解析器：

```java
public interface PersistenceModuleDataSourceResolver {
    String resolveDataSource(String moduleName);
}
```

运行期路由来源按优先级处理：

1. 显式上下文：例如 `PersistenceDataSourceContext.use("job")`。
2. 显式注解：例如 `@PersistenceDataSource("job")`。
3. 模块名映射：由 Mapper 包、Service 注解或后续模块元数据解析。
4. 默认主数据源：`primary`。

第一阶段必须提供显式上下文和注解能力；模块元数据自动推断可后置，但配置模型必须兼容。

### 6.4 MyBatis-Plus 路由

MyBatis-Plus 保持单套插件装配，底层 DataSource 替换为路由数据源。

约束：

- Mapper 不直接注入具体数据源。
- 业务模块通过模块映射或注解决定执行数据源。
- 租户插件继续由 `PersistenceMybatisPlusAutoConfiguration` 统一装配。
- 技术运行表、外部引擎表可通过 `excludedTables` 或 schema 检查豁免机制排除租户字段要求。

### 6.5 事务边界

默认只支持单数据源本地事务。

决策：

- `@Transactional` 默认绑定路由数据源当前选择结果。
- 一个事务内不允许切换数据源。
- 跨库写入必须拆分为 API 调用、事件、outbox 或补偿流程。
- 不引入 Seata 作为本次多数据源底座的默认强一致事务方案。

必须提供运行期保护：

- 事务开启后再次切换到不同数据源时抛出明确异常。
- 异常信息包含当前数据源、目标数据源和调用上下文。

### 6.6 Flyway 迁移

现有 `PersistenceFlywayAutoConfiguration` 已支持模块级迁移和模块独立迁移数据源。本次扩展后调整为：

- Flyway 读取统一 `datasources` 注册表。
- 每个模块通过 `mango.persistence.modules.<module>.datasource` 选择迁移数据源。
- 每个模块保留独立 history table，默认 `flyway_schema_history_{module}`。
- 迁移路径继续使用 `db/migration/{module}/V*.sql`。
- 禁止把不同数据源的 migration 合并到同一个 history table。

兼容策略：

- 保留现有 `mango.persistence.flyway.modules.<module>.datasource` 配置读取，标记为兼容入口。
- 新配置优先级高于旧配置。
- 不修改已执行 migration。

### 6.7 数据库独立策略

后续 Job 相关库建议：

| 数据库 | 用途 | 所属 |
|---|---|---|
| `mango` | Mango 主业务数据、授权、系统、组织等 | Mango 主应用 |
| `mango_job` | Mango Job 治理数据、任务定义、租户快照、执行摘要、引擎映射；可共置 PowerJob 内部表 | `mango-job` |
| `powerjob` | 可选独立 PowerJob Server 引擎内部表库或 schema | PowerJob |

约束：

- `mango_job` 不跨库外键引用 `mango`。
- `powerjob` 表结构由 PowerJob 管理，Mango 不手写 PowerJob 内部表。
- Mango 只保存 PowerJob appId、jobId、instanceId 等映射字段。
- Mango 模块独立数据库命名遵循 `mango-pmo/rules/backend/07-persistence.md`：默认 `mango_{module}`。

## 7. 接口变化

本次底座不新增 HTTP API。

新增 Java 扩展点和注解：

| 类型 | 用途 |
|---|---|
| `PersistenceDataSourceProperties` | 多数据源配置属性。 |
| `PersistenceDataSourceRegistry` | 查询已注册数据源。 |
| `PersistenceDataSourceContext` | 显式设置当前数据源上下文。 |
| `@PersistenceDataSource` | 标记 Service 或方法使用指定数据源。 |
| `PersistenceModuleDataSourceResolver` | 按模块解析数据源。 |
| `MangoRoutingDataSource` | 运行期路由数据源。 |

## 8. 数据变化

本次底座不新增业务表。

数据库变化来自后续消费模块：

- 主库：保持现状。
- 独立库：由模块 migration 写入对应数据库。
- Flyway history table：按模块在对应数据库生成。

## 9. 测试范围

必须覆盖：

- 单数据源兼容：未配置多数据源时现有测试通过。
- 多数据源注册：`primary`、`job` 两个数据源可注册。
- 路由上下文：显式上下文可将写入路由到 `job`。
- 注解路由：`@PersistenceDataSource("job")` 生效。
- 事务保护：事务内切换数据源失败。
- Flyway 隔离：`system` migration 进入主库，`job` migration 进入 job 库。
- 租户插件兼容：主库租户表仍追加租户条件。
- 技术表豁免：独立技术表可不携带租户字段并通过准入检查。

## 10. 验收标准

- `mango-infra-persistence-starter` 在单数据源配置下保持兼容。
- 在测试环境中可启动主库和 job 库两个 DataSource。
- `db/migration/system` 和 `db/migration/job` 可按模块迁移到不同数据库。
- 同一事务内跨数据源切换被阻止并给出明确异常。
- 后续 `mango-job` 可通过配置把自身表放入 `mango_job`。
- 文档说明独立数据库、灵活部署和跨库事务边界。

## 11. 风险与限制

| 风险 | 处理 |
|---|---|
| 多数据源引入后影响现有单数据源应用 | 默认配置必须保持单数据源兼容，未配置模块映射时全部走 `primary`。 |
| 事务内误切数据源导致半提交 | 运行期检测并失败，禁止静默切换。 |
| Flyway 迁移顺序混乱 | 按模块、数据源、history table 隔离，保留确定性排序。 |
| PowerJob 内部表被 Mango 错误接管 | PowerJob 数据库由 PowerJob 管理，Mango 不维护其内部 migration。 |
| 跨库查询诉求扩大范围 | 明确禁止跨库 join，跨库数据通过 API 或事件同步。 |

## 12. 后续衔接

多数据源底座完成后，`mango-job` 的目标配置为：

```yaml
mango:
  persistence:
    modules:
      mango-job:
        datasource: job
```

PowerJob 集成目标配置为：

```yaml
mango:
  job:
    engine:
      type: powerjob
      powerjob:
        server-address: http://powerjob-server:7700
        app-name: mango-job
        datasource: job
```

其中 `datasource: job` 表示 PowerJob 内部表与 Job 模块库共置；高隔离部署可以改为独立 `powerjob` 数据源，但不代表 Mango 直接管理 PowerJob 内部表。
