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
| `mango-infra-excel-starter` | `io.mango.infra.excel:mango-infra-excel-starter` | 基于 Apache POI 的默认 Excel Adapter、title/idx 映射、转换、模板和失败工作簿 |

核心能力：

- 实体基类：`BaseEntity`、`AuditableEntity`、`TenantEntity`。
- CRUD 服务：`MangoCrudService`、`MangoTypedCrudService`、`MangoCrudServiceImpl`，覆盖创建、更新、删除、批量删除、详情、列表、分页，并为业务 Service 提供完整泛型契约。
- 查询条件：`@QueryField`、`@QueryIgnore`、`QueryType`，自动构造 MyBatis-Plus `QueryWrapper`。
- 分页：默认注册 `PaginationInnerInterceptor`，返回 `PersistencePageResult`。
- 租户隔离：默认注册 `TenantLineInnerInterceptor`，从 `MangoContextHolder.tenantId()` 读取租户。
- 审计填充：新增和修改时自动填充创建人、创建时间、更新人、更新时间、租户字段。
- Flyway：按 `db/migration/<module>/V*.sql` 分模块迁移，每个模块独立 history table。
- Flyway MySQL 运行时：Mango 显式管理 `flyway-core` 与 `flyway-mysql` 11.20.3；该版本已测试到 MySQL 9.4，覆盖 Mango 的 MySQL 8.4 基线。
- Flyway 外部 locations：停机升级可按模块加载 `filesystem:` 目录或 `http(s)` 单个 SQL 文件。
- Cold baseline：每个模块维护一份当前 `B*__baseline.sql`，新数据库可跳过 V1...Vn 历史回放。
- 多数据源：支持定义多个数据源、按模块映射、按 `@PersistenceDataSource` 或代码作用域切换。
- Schema 校验：启动时检查业务表主键和审计租户字段。
- Web CRUD：提供标准 `/create`、`/update`、`/delete`、`/batch-delete`、`/detail`、`/page`、`/export`、`/import`、`/import-template` 入口。

## 1.1 数据治理快速判断

业务 Agent 和 Mango 框架开发 Agent 处理 SQL 或大数据物料时，先按下面判断：

| 场景 | 入口 | 关键要求 |
|------|------|----------|
| 新表、改表、索引、约束 | `db/migration/<module>/V*.sql` | 只用 Flyway 管理 DDL。 |
| 停机升级修复历史数据 | `mango.persistence.flyway.modules.<module>.locations` | 使用 `filesystem:` 目录或 `http(s)` 单个 SQL 文件，仍写模块 history table。 |
| 新库当前完整结构 | `db/baseline/<module>/B*__baseline.sql` | 每模块恰好一份，只由 Bootstrap cold 在真空库执行。 |
| 菜单、字典、配置、消息模板、任务、号段等结构化资源 | `mango-resource` | 不写默认 Flyway DML。 |
| demo/sample 数据 | `META-INF/mango/demo/` | 默认不加载，见 `mango-resource` README。 |
| 500MB/1GB 行政区划、年度日历等大数据 | 外部 SQL 包或模块批量导入服务 | 不放 YAML，不打进默认 jar classpath。 |

本模块不提供裸 SQL 执行器，不提供 Data Package/task 编排，不执行任意 bean 方法。需要可追溯的 SQL 升级时，仍使用 Flyway 版本号、模块 history table 和停机升级 runbook。

历史 Flyway migration 中保留的 DML 只代表老库升级历史，不能作为新增字典、菜单、角色、demo 或业务 seed 的当前模板。新增小资源优先看 `mango-resource` README；确实属于结构、大 SQL 或停机升级 SQL 时，才放到本模块管理的 Flyway migration 或外部 locations。

## 2. 功能清单

| 能力 | 常用入口 |
|------|----------|
| 业务模块新增 MySQL 或兼容 JDBC 的业务表 | Maven 依赖 / starter / Java API |
| 业务实体需要统一 Long 雪花主键、审计字段和租户字段 | Maven 依赖 / starter / Java API |
| 单表或薄业务逻辑资源需要快速实现 CRUD API | Maven 依赖 / starter / Java API |
| 模块需要在启动时自动执行自己的 Flyway migration | Maven 依赖 / starter / Java API |
| 模块诊断需要读取本次真实 Flyway 运行状态 | `ModuleDiagnosticContributor` / `mango module doctor` |
| 停机升级时需要按模块执行外部 SQL 包 | YAML 配置 / 运维升级包 |
| 新数据库需要使用当前完整 schema baseline | YAML 配置 / 每模块唯一 `B*__baseline.sql` |
| 应用需要把不同模块路由到不同数据库 | Maven 依赖 / starter / Java API |
| 非 Web 任务、定时任务或测试环境需要显式配置默认租户 | Maven 依赖 / starter / Java API |
| 管理端资源需要复用标准导入导出入口和官方 Excel 解析 | `mango-infra-excel-starter` / Java API |


## 3. 能力边界
- 不替代业务领域建模、复杂聚合查询、跨聚合事务和业务校验。
- `web-starter` 只定义 Excel 公共契约和 Controller 编排；默认 POI 实现由独立的 `mango-infra-excel-starter` 提供。
- 不做全局 SQL 数据范围拦截；平台提供 `DataScopeProvider` 和 `DataScopeApplier`，业务在标准查询入口显式声明资源编码和业务字段。
- 不自动让 `@IgnoreTenant` 生效；当前代码里它只是 API 契约标记，租户拦截器没有读取这个注解。
- 不提供菜单和按钮权限资源；业务模块仍要通过自己的 resource manifest 或 authorization 初始化。
- 不建议在一个 Spring 事务中切换数据源；路由数据源会直接拒绝。

## 4. 模块入口
`api` 只放业务可依赖的轻量契约；`starter` 负责运行时持久化装配；`web-starter` 负责标准 HTTP CRUD 外壳。

业务模块自己负责：

- 实体类、Mapper、Service、Controller。
- `src/main/resources/db/migration/<module>/V*.sql` 表结构。
- 资源菜单、按钮权限、API 权限的初始化。
- 数据范围、复杂查询、业务唯一性和业务事务。
- 导入导出行模型、业务字典/名称到 code 的特殊转换、数据库关联、去重和业务校验。

## 5. 接入方式
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

使用 Mango 默认 Excel 实现：

```xml
<dependency>
    <groupId>io.mango.infra.excel</groupId>
    <artifactId>mango-infra-excel-starter</artifactId>
</dependency>
```

默认实现通过条件装配注册 `ExcelAdapter`；业务已提供自定义 Adapter 时不会覆盖。

只引入 `api` 不会注册 MyBatis-Plus 插件、Flyway、审计填充、多数据源和 Controller。业务应用要让能力生效，至少需要运行时引入 `mango-infra-persistence-starter`。

## 6. 配置说明
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
| `schema-validation.required-columns` | `created_by`、`created_at`、`updated_by`、`updated_at`、`tenant_id`、`org_id` | 每张非排除租户业务表必须存在的标准字段 |
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
authorization_frontend_app_registry
frontend_menu_runtime_config
authorization_frontend_module_runtime_strategy
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
act_*
flw_*
resource_*
authorization_app_module
authorization_frontend_app_registry
frontend_menu_runtime_config
authorization_frontend_module_runtime_strategy
```

`act_*` 和 `flw_*` 是 Mango 直接集成的 Flowable 引擎表，由 Flowable 数据模型定义，不适用 Mango 业务表的 `id`、审计、租户和组织字段约束。Mango 自有的 `workflow_*` 表仍参与完整 Schema 校验。

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

如果显式配置了 `modules`，starter 会把配置视为当前应用的 Flyway 模块清单，并继续扫描 classpath。classpath 中存在 `db/migration/<module>/V*.sql` 但清单未声明的模块会导致启动失败。有意跳过某个已进入 classpath 的模块时，必须声明该模块 `enabled=false` 并填写 `skip-reason`，避免 starter、Controller 或 Resource 已装配但表结构未初始化的假集成。

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
          locations:
            - classpath:db/migration/mango-system
        mango-job:
          enabled: true
        link:
          enabled: false
          skip-reason: 当前单体不启用网址导航模块
```

| 配置 | 默认值 | 含义 |
|------|--------|------|
| `enabled` | `true` | 全局迁移开关 |
| `upgrade-locations-enabled` | `true` | 是否启用默认外部升级目录；目录存在时追加到未显式配置 locations 的模块 |
| `upgrade-root` | 空 | 默认外部升级根目录；为空时按 `mango.upgrade.root`、`MANGO_UPGRADE_DIR`、`mango.home`/`MANGO_HOME`、`/opt/mango/upgrade` 解析 |
| `cold-baseline.enabled` | `false` | 是否允许 Bootstrap 对真正空库执行每模块当前基线 |
| `modules.<module>.enabled` | `true` | 是否执行当前模块迁移 |
| `modules.<module>.skip-reason` | 空 | `enabled=false` 且 classpath 存在当前模块 migration 时必须填写的跳过原因 |
| `modules.<module>.baseline-on-migrate` | `true` | 存量库无 history table 时是否从 baseline 接管 |
| `modules.<module>.out-of-order` | `false` | 是否允许非顺序版本补跑 |
| `modules.<module>.history-table` | `flyway_schema_history_<module>` | 当前模块 history table，模块名会把非字母数字下划线替换成 `_` |
| `modules.<module>.locations` | `classpath:db/migration/<module>` + 存在时的 `{upgrade-root}/<module>` | 当前模块迁移脚本位置，支持 `classpath:`、`filesystem:` 和 `http(s)` 单个 SQL 文件；显式配置后不再隐式追加默认升级目录 |
| `modules.<module>.validate-on-migrate` | `true` | 迁移前是否校验历史记录 |
| `modules.<module>.ignore-missing-migrations` | `false` | 是否忽略数据库存在但代码已移除的历史迁移 |
| `modules.<module>.datasource.url` | 空 | 当前模块迁移使用独立 JDBC URL |
| `modules.<module>.datasource.driver-class-name` | 空 | 当前模块迁移独立驱动 |
| `modules.<module>.datasource.username` | 空 | 当前模块迁移独立用户名 |
| `modules.<module>.datasource.password` | 空 | 当前模块迁移独立密码 |
| `modules.<module>.baseline.location` | 自动发现 | 当前模块唯一基线；默认要求 `classpath*:db/baseline/<module>/B*__baseline.sql` 恰好一个匹配 |
| `modules.<module>.baseline.version` | 从文件名解析 | 基线包含的最高模块 migration 版本 |

停机升级需要执行不随应用 jar 发布的 SQL 时，不新增裸 SQL 执行器，仍把脚本作为 Flyway migration 管理。默认约定目录为：

```text
${MANGO_HOME:-/opt/mango}/upgrade/<module>/
  V2026070101__fix_channel_data.sql
```

如果没有显式配置当前模块 `locations`，starter 会保留 `classpath:db/migration/<module>`，并在目录存在时自动追加 `filesystem:${MANGO_HOME:-/opt/mango}/upgrade/<module>`。目录不存在时跳过，不创建目录、不报错。

默认升级根目录解析顺序：

```text
1. Java system property: mango.upgrade.root
2. 环境变量: MANGO_UPGRADE_DIR
3. Java system property: mango.home + /upgrade
4. 环境变量: MANGO_HOME + /upgrade
5. /opt/mango/upgrade
```

需要远程 URL 或完全自定义升级目录时，显式配置模块 `locations`，此时配置值表示完整来源清单，不再隐式追加默认升级目录。cold baseline 使用独立的 `modules.<module>.baseline`，不要放进 `locations`：

```yaml
mango:
  persistence:
    flyway:
      modules:
        payment:
          enabled: true
          locations:
            - classpath:db/migration/payment
            - filesystem:/opt/mango/upgrade/payment
            - https://artifact.example.com/mango/payment/V2026070101__fix_channel_data.sql
```

`filesystem:` 应指向包含 `V*.sql` 的目录。`http(s)` 只支持单个 `.sql` 文件，启动迁移前会下载到临时目录再交给 Flyway；执行结果仍写入该模块的 `flyway_schema_history_<module>`。

停机升级时，一个模块可以同时执行默认 classpath migration 和外部升级目录。Flyway 会按版本号决定执行顺序，已在当前模块 history table 中成功记录的版本不会重复执行。外部 SQL 必须使用高于已发布历史版本的版本号，避免和 jar 内 migration 冲突。

升级执行顺序约定：

```text
1. 模块顺序：显式配置 modules 时按配置顺序执行；未配置 modules 时按 classpath 扫描到的模块名自然排序执行。
2. locations 顺序：未显式配置 locations 时固定为 classpath:db/migration/<module> 在前，存在的约定升级目录 filesystem:${MANGO_HOME:-/opt/mango}/upgrade/<module> 在后；显式 locations 按配置顺序交给 Flyway。
3. SQL 顺序：同一模块的所有 locations 合并后，由 Flyway 按版本号执行，不按文件复制时间、目录遍历顺序或 locations 先后顺序执行。
4. 重复控制：当前模块 history table 中已经成功记录的版本不会重复执行；同一模块不同 location 出现相同版本会按 Flyway 规则校验失败。
```

同一个模块的升级顺序只看文件名前面的版本号，版本号小的先执行：

```text
/opt/mango/upgrade/payment/
  V2026070801__fix_payment_channel.sql
  V2026070802__fix_payment_order.sql
  V2026070803__fix_payment_summary.sql

执行顺序：0801 -> 0802 -> 0803
```

同一个模块内不要出现两个相同版本号。已经执行成功的版本不会重复执行。

多模块升级 SQL 约定：

```text
1. 默认约定：不同模块的升级 SQL 必须按模块边界独立可执行，不依赖另一个模块的外部升级 SQL 已先执行。
2. 有依赖时：必须显式配置 mango.persistence.flyway.modules，并把被依赖模块放在前面；不要依赖未配置 modules 时的模块名自然排序表达业务依赖。
3. 跨模块修复：一条 SQL 同时修复多个模块表、或必须在多个模块完成后执行时，应放入单独的发布编排模块，例如 release-20260708，并在 modules 中排在相关模块之后，使用独立 history table。
4. 事务边界：模块之间没有全局事务；某个模块失败后，已成功模块的 history 不会回滚，恢复时继续由各模块 Flyway history 控制幂等。
```

显式模块顺序示例：

```yaml
mango:
  persistence:
    flyway:
      modules:
        identity:
          enabled: true
        payment:
          enabled: true
        release-20260708:
          enabled: true
          locations:
            - filesystem:/opt/mango/upgrade/release-20260708
```

外部升级 SQL 推荐使用可排序版本号，例如 `V202607081730__fix_payment_channel.sql` 或 `V2026070801__fix_payment_channel.sql`。同一停机窗口内需要多条 SQL 时，版本号必须递增。

推荐升级顺序：

```text
1. 停应用并备份数据库。
2. 把必要的外部升级 SQL 放入约定目录；远程 URL 或特殊目录再显式配置 locations。
3. 启动迁移入口，让 Flyway 写入模块 history table。
4. 启动应用后由 Resource 同步正式资源；INIT_ONLY 不覆盖目标业务表运行时修改。
5. 校验业务关键数据。
```

### 6.5 Cold Baseline

模块历史 migration 很多时，发布候选准备阶段为每个启用模块维护一份当前完整基线：

```text
src/main/resources/db/baseline/<module>/B<version>__baseline.sql
```

目录中必须恰好只有一个 `B*__baseline.sql`。`B<version>` 表示 SQL 已覆盖的最高模块 migration 版本；历史 `db/migration/<module>/V*.sql` 继续保留，供审计、既有库升级和基线后的增量 migration 使用。基线不是在部署现场动态拼接，也不是把所有模块合成一个大 SQL。

基线首行必须声明可重入：

```sql
-- mango:baseline-idempotent
```

启用方式：

```yaml
mango:
  persistence:
    flyway:
      cold-baseline:
        enabled: true
      modules:
        payment:
          baseline:
            # 默认可省略，自动发现 classpath 中唯一文件
            location: classpath:db/baseline/payment/B2026072701__baseline.sql
            version: 2026072701
```

Bootstrap 按逻辑数据源分组，并按数据源 key、模块 code 的稳定顺序执行。每个数据源必须是真正空库；允许存在的只有 Bootstrap 自身控制表。每个模块 SQL 成功后，框架以 `B<version>` 为该模块原有 Flyway history table 建立基线，并记录模块 SQL SHA-256；失败重入只复用同一 fingerprint 已完成的模块。

| 数据库状态 | 执行路径 |
|------------|----------|
| 全新空库且 `cold-baseline.enabled=true` | 先执行每模块唯一 `B*`，再执行高于 `B<version>` 的 expand migration。 |
| 全新空库但未准备完整 `B*` | fail closed；关闭 cold baseline 后才能从 V1 回放。 |
| 已有模块 history 的数据库 | 不执行 cold baseline，继续按模块执行 expand/finalize 增量。 |
| 半初始化且没有完成回执 | MySQL DDL 不可事务回滚；删除并重建该一次性空库后重试。 |

性能回归入口为仓库根目录 `scripts/tests/bootstrap-performance.sh`。当前 MySQL 8.4 基线使用 5 个模块、375 张表、37,500 行和 16,372,270 SQL 字节，实测 4.805 秒；该数字是开发机证据，不替代目标环境验收。

模块迁移数据源解析顺序：

1. 多数据源 registry 中的模块映射。
2. 当前模块 `modules.<module>.datasource.url` 临时数据源。
3. 应用默认 `DataSource`。

starter 会注册一个 `_noop` Flyway bean，避免 Spring Boot 默认 Flyway 把所有模块脚本合并到一个 history table 中执行。

## 7. API 与扩展
### 7.1 实体和分页模型

| 类型 | 字段 / 行为 |
|------|-------------|
| `BaseEntity` | `Long id`，`@TableId(type = IdType.ASSIGN_ID)`，默认雪花 ID |
| `AuditableEntity` | 继承 `BaseEntity`，增加 `createdBy`、`createdAt`、`updatedBy`、`updatedAt` |
| `TenantEntity` | 继承 `AuditableEntity`，增加 `String tenantId`、`Long orgId` |
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
    org_id BIGINT NULL,
    invoice_no VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_by BIGINT NULL,
    created_at DATETIME NOT NULL,
    updated_by BIGINT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id)
);
```

`id` 必须是 `BIGINT` 或数据库等价类型，不能是 `AUTO_INCREMENT`。当前启动期校验还会要求 `created_by`、`created_at`、`updated_by`、`updated_at`、`tenant_id`、`org_id` 存在，除非表在 `schema-validation.excluded-tables` 中。这里的约束面向租户业务表；全局配置表、历史表、基础设施表和第三方表应明确加入排除清单。

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

### 7.2.1 业务开发常用示例

#### 租户逻辑

租户业务表继承 `TenantEntity`，表结构保留 `tenant_id`。普通查询、分页、`BaseMapper` 和 XML SQL 默认由租户插件追加租户条件；创建时由审计填充器写入租户，不需要业务手工设置。

```java
@Getter
@Setter
@TableName("order_invoice")
public class OrderInvoiceEntity extends TenantEntity {

    private String invoiceNo;
}
```

Service 按 [7.3 CRUD Service](#73-crud-service) 绑定 `MangoTypedCrudService` 六种业务类型，并由实现类继承 `MangoCrudServiceImpl<OrderInvoiceMapper, OrderInvoiceEntity>`。

普通业务代码不要手写 `.eq(OrderInvoiceEntity::getTenantId, ...)`，也不要在创建时 `setTenantId(...)`。跨租户运营、租户初始化或全局表要单独建模，并通过 `mango.persistence.mybatis-plus.tenant.excluded-tables` 明确例外。

#### 数据权限

标准 CRUD 查询通过覆写 `applyDataScope()` 接入数据权限。`tenant_id` 仍由租户插件处理，`created_by` 和 `org_id` 由 `DataScopeApplier` 根据授权模块的数据权限规则追加。

在按 7.3 完成类型绑定的 `PaymentOrderService` 中覆写数据权限钩子：

```java
@Override
protected void applyDataScope(QueryWrapper<PaymentOrderEntity> wrapper, Object query) {
    dataScopeApplier.apply(
            wrapper,
            "payment:order:list",
            DataScopeMapping.builder()
                    .tableName("payment_order")
                    .selfField("created_by")
                    .orgField("org_id")
                    .tenantField("tenant_id")
                    .build()
    );
}
```

普通业务查询不要手写 `created_by = 当前用户`、`org_id in (...)` 或等价条件；这些范围由授权模块的角色数据权限配置和 `DataScopeApplier` 决定。

#### 分页

普通单表分页由类型化 Service 的 `page(query)` 复用 `MangoCrudServiceImpl.pageByQuery()`。Controller 按 [7.4 Web CRUD Controller](#74-web-crud-controller) 显式实现 API 的 `/page` 适配；查询对象继承 `PageQuery`，业务字段用 `@QueryField` 描述条件。

```java
@Getter
@Setter
public class OrderInvoiceQuery extends PageQuery {

    @QueryField(type = QueryType.LIKE)
    private String invoiceNo;

    @QueryField(type = QueryType.IN)
    private List<String> statusList;
}
```

普通 CRUD 不需要在业务 Service 中手写 `new Page<>(...)`、`mapper.selectPage(...)` 和 `PageResult.of(...)`。

#### 联表查询

联表查询用于读模型或报表场景，放在 core 内部查询 Service 和 Mapper XML 中。Service 可继承 `MangoQueryServiceSupport` 复用分页和返回包装；SQL 写在 XML，Mapper 方法不要使用注解 SQL。

```java
@Service
public class OrderInvoiceReadService extends MangoQueryServiceSupport {

    private final OrderInvoiceReadMapper mapper;
    private final DataScopeApplier dataScopeApplier;

    public OrderInvoiceReadService(PersistenceContextProvider contextProvider,
                                   OrderInvoiceReadMapper mapper,
                                   DataScopeApplier dataScopeApplier) {
        super(contextProvider);
        this.mapper = mapper;
        this.dataScopeApplier = dataScopeApplier;
    }

    public PersistencePageResult<OrderInvoiceRowVO> pageRows(OrderInvoiceQuery query) {
        QueryWrapper<OrderInvoiceEntity> scope = new QueryWrapper<>();
        dataScopeApplier.apply(scope, "order:invoice:list", DataScopeMapping.builder()
                .tableName("order_invoice")
                .selfField("i.created_by")
                .orgField("i.org_id")
                .tenantField("i.tenant_id")
                .build());
        return pageResult(mapper.pageRows(page(query), query, scope));
    }
}
```

```java
public interface OrderInvoiceReadMapper {

    IPage<OrderInvoiceRowVO> pageRows(Page<OrderInvoiceRowVO> page,
                                      @Param("query") OrderInvoiceQuery query,
                                      @Param("scope") Wrapper<OrderInvoiceEntity> scope);
}
```

```xml
<select id="pageRows" resultType="com.example.order.vo.OrderInvoiceRowVO">
    SELECT i.id, i.invoice_no, c.customer_name
    FROM order_invoice i
    LEFT JOIN order_customer c ON c.id = i.customer_id
    WHERE 1 = 1
    ${scope.customSqlSegment}
    <if test="query.invoiceNo != null and query.invoiceNo != ''">
        AND i.invoice_no LIKE CONCAT('%', #{query.invoiceNo}, '%')
    </if>
</select>
```

联表 SQL 仍会经过 MyBatis-Plus 租户插件；数据权限条件由 `DataScopeApplier` 构造。跨域数据不应通过随意 join 解决，只有同一读模型边界内的查询才使用这种方式。

### 7.3 CRUD Service

CRUD 能力分为三层：

| 类型 | 用途 |
|------|------|
| `MangoCrudService<E>` | 框架内部的通用 CRUD 契约，命令、查询和返回值使用宽类型，便于底层复用。 |
| `MangoTypedCrudService<E, C, U, Q, V, ID>` | 业务 Service 接口使用的编译期契约，固定 Entity、创建命令、更新命令、分页查询、VO 和 ID 六种类型。 |
| `MangoCrudServiceImpl<M, E>` | 基于 MyBatis-Plus 的默认实现和扩展钩子；业务实现类继续继承它，并实现自己的 `IXxxService`。 |

`MangoTypedCrudService` 随计划中的 Mango Maven backend `1.0.16` 提供。已发布的 `1.0.15` 无法解析本批次的新 global Entity manifest 契约；在 `1.0.16` 完成仓库回查前，本节描述的是当前源码和发布候选行为。

业务 Service 接口绑定完整类型：

```java
import io.mango.infra.persistence.api.crud.MangoTypedCrudService;

public interface IOrderInvoiceService extends MangoTypedCrudService<
        OrderInvoiceEntity,
        CreateOrderInvoiceCommand,
        UpdateOrderInvoiceCommand,
        OrderInvoicePageQuery,
        OrderInvoiceVO,
        Long> {
}
```

实现类保留 `MangoCrudServiceImpl<Mapper, Entity>`，并实现业务接口：

```java
package com.example.order.service;

import com.example.order.api.command.CreateOrderInvoiceCommand;
import com.example.order.api.command.UpdateOrderInvoiceCommand;
import com.example.order.api.enums.OrderInvoiceCode;
import com.example.order.api.query.OrderInvoicePageQuery;
import com.example.order.api.vo.OrderInvoiceVO;
import com.example.order.entity.OrderInvoiceEntity;
import com.example.order.mapper.OrderInvoiceMapper;
import io.mango.common.result.Require;
import io.mango.infra.persistence.api.crud.DeleteCommand;
import io.mango.infra.persistence.api.crud.MangoCrudServiceImpl;
import io.mango.infra.persistence.api.query.PersistencePageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderInvoiceService
        extends MangoCrudServiceImpl<OrderInvoiceMapper, OrderInvoiceEntity>
        implements IOrderInvoiceService {

    @Override
    protected OrderInvoiceVO toVO(OrderInvoiceEntity entity) {
        if (entity == null) {
            return null;
        }
        OrderInvoiceVO vo = new OrderInvoiceVO();
        vo.setId(entity.getId());
        vo.setInvoiceNo(entity.getInvoiceNo());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(CreateOrderInvoiceCommand command) {
        Require.notNull(command, OrderInvoiceCode.VALIDATION_ERROR);
        Object id = createByCommand(command);
        Require.isTrue(id instanceof Long, OrderInvoiceCode.VALIDATION_ERROR);
        return (Long) id;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(UpdateOrderInvoiceCommand command) {
        Require.notNull(command, OrderInvoiceCode.VALIDATION_ERROR);
        Require.notNull(getById(command.getId()), OrderInvoiceCode.NOT_FOUND);
        return updateByCommand(command);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(DeleteCommand command) {
        Require.notNull(command, OrderInvoiceCode.VALIDATION_ERROR);
        Require.notNull(command.getId(), OrderInvoiceCode.VALIDATION_ERROR);
        Require.notNull(getById(command.getId()), OrderInvoiceCode.NOT_FOUND);
        return deleteById(command.getId());
    }

    @Override
    @SuppressWarnings("unchecked")
    public PersistencePageResult<OrderInvoiceVO> page(OrderInvoicePageQuery query) {
        Require.notNull(query, OrderInvoiceCode.VALIDATION_ERROR);
        return (PersistencePageResult<OrderInvoiceVO>) (PersistencePageResult<?>)
                pageByQuery(query);
    }

    @Override
    public OrderInvoiceVO detail(Long id) {
        Require.notNull(id, OrderInvoiceCode.VALIDATION_ERROR);
        OrderInvoiceEntity entity = getById(id);
        Require.notNull(entity, OrderInvoiceCode.NOT_FOUND);
        return toVO(entity);
    }

    @Override
    protected Class<OrderInvoiceEntity> entityType() {
        return OrderInvoiceEntity.class;
    }
}
```

业务实现类需要显式实现 `create`、`update`、`delete`、`page` 和 `detail`。这些类型化方法可以复用下表中的底层方法，但应在调用前完成 `Require + XxxCode implements BizCode` 业务前置条件校验，并把 Entity 转换为 VO。

#### 7.3.1 从宽类型 CRUD 迁移

1. 把 `IXxxService` 改为继承 `MangoTypedCrudService<Entity, CreateCommand, UpdateCommand, PageQuery, VO, Long>`。
2. 让 `XxxService` 继续继承 `MangoCrudServiceImpl<Mapper, Entity>`，同时实现 `IXxxService`；实现类名称不再使用 `XxxServiceImpl`。
3. 实现五个类型化方法，并分别委托 `createByCommand`、`updateByCommand`、`deleteById`、`pageByQuery`、`getById` 或 `detailById`。
4. 覆写 `toVO(Entity)` 和 `entityType()`；对 `Object`、通配分页结果的转换只保留在 Service 实现内部，不向 Controller/API 扩散。
5. 将 Controller 改为直接实现一个 `XxxApi`，只依赖 `IXxxService`，逐项映射 HTTP 方法并返回 `R.ok(service.xxx(...))`。
6. 迁移后执行 Maven `verify`，确认 Entity/Mapper/Service/Controller/Feign 的类型、模块位置和 HTTP 映射同时通过架构门禁。

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

`applyDataScope(QueryWrapper<E> wrapper, Object query)` 默认是空实现。业务需要数据权限时，在已经实现类型化 CRUD 契约的 Service 中注入 `DataScopeApplier`，并覆写以下钩子：

```java
@Override
protected void applyDataScope(QueryWrapper<PaymentOrderEntity> wrapper, Object query) {
    dataScopeApplier.apply(
            wrapper,
            "payment:order:list",
            DataScopeMapping.builder()
                    .tableName("payment_order")
                    .selfField("created_by")
                    .orgField("org_id")
                    .tenantField("tenant_id")
                    .build()
    );
}
```

规则来自授权模块的角色数据权限：

- `ALL`：当前租户内全部数据，租户隔离仍由租户插件处理。
- `SELF`：追加 `selfField = MangoContextHolder.userId()`。
- `SELF_ORG`：按当前登录成员的主部门动态解析组织范围，追加 `orgField in (...)`。
- `SELF_ORG_AND_CHILDREN`：按当前登录成员的主部门及下级部门动态解析组织范围，追加 `orgField in (...)`。
- `ORG`：追加指定组织 `orgField in (...)`；多角色合并时如果同时有 `SELF`，会生成“组织范围或本人数据”。
- 无配置默认 `SELF`，缺少登录上下文或缺少必要字段会直接抛错，避免放大权限。

`SELF_ORG` 和 `SELF_ORG_AND_CHILDREN` 不需要在角色上为每个部门单独建角色。业务管理员给用户维护主部门，再给用户分配同一个部门管理员角色即可；实际组织范围在查询时按当前成员主部门解析。

如果 `DataScopeMapping.tableName` 不为空，`DataScopeApplier` 会读取数据库元数据校验本次规则需要的字段是否存在；例如角色规则命中 `ORG`、`SELF_ORG` 或 `SELF_ORG_AND_CHILDREN` 时要求 `orgField` 对应列存在，命中 `SELF` 或 `ORG + selfIncluded` 时要求 `selfField` 对应列存在。缺字段会直接 fail-fast。

复杂 SQL 或 XML Mapper 不走 `QueryWrapper` 标准入口时，业务仍应调用 `DataScopeApplier` 构造条件，字段传入带别名的列名，例如 `o.created_by`、`o.org_id`；此时 `tableName` 仍填真实业务表名，用于列存在性校验。

### 7.4 Web CRUD Controller

新建或迁移后的业务 Controller 直接实现一个传输无关的 `XxxApi`，只依赖类型化 `IXxxService`。HTTP mapping 保留在 Controller 和 Feign adapter，不写入 API 契约：

```java
package com.example.order.controller;

import com.example.order.api.OrderInvoiceApi;
import com.example.order.api.command.CreateOrderInvoiceCommand;
import com.example.order.api.command.UpdateOrderInvoiceCommand;
import com.example.order.api.query.OrderInvoicePageQuery;
import com.example.order.api.vo.OrderInvoiceVO;
import com.example.order.service.IOrderInvoiceService;
import io.mango.common.result.R;
import io.mango.infra.persistence.api.crud.DeleteCommand;
import io.mango.infra.persistence.api.query.PersistencePageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@Tag(name = "订单发票", description = "订单发票管理接口")
@RequestMapping("/order/invoices")
public class OrderInvoiceController implements OrderInvoiceApi {

    private final IOrderInvoiceService service;

    public OrderInvoiceController(IOrderInvoiceService service) {
        this.service = service;
    }

    @Override
    @Operation(summary = "创建订单发票", description = "创建一条订单发票业务记录")
    @PostMapping("/create")
    public R<Long> create(@RequestBody @Valid CreateOrderInvoiceCommand command) {
        return R.ok(service.create(command));
    }

    @Override
    @Operation(summary = "修改订单发票", description = "按业务标识修改订单发票")
    @PostMapping("/update")
    public R<Boolean> update(@RequestBody @Valid UpdateOrderInvoiceCommand command) {
        return R.ok(service.update(command));
    }

    @Override
    @Operation(summary = "删除订单发票", description = "按业务标识删除订单发票")
    @PostMapping("/delete")
    public R<Boolean> delete(@RequestBody @Valid DeleteCommand command) {
        return R.ok(service.delete(command));
    }

    @Override
    @Operation(summary = "分页查询订单发票", description = "按查询条件分页获取订单发票")
    @GetMapping("/page")
    public R<PersistencePageResult<OrderInvoiceVO>> page(
            @ParameterObject @Valid OrderInvoicePageQuery query) {
        return R.ok(service.page(query));
    }

    @Override
    @Operation(summary = "查询订单发票详情", description = "按业务标识获取订单发票详情")
    @GetMapping("/detail")
    public R<OrderInvoiceVO> detail(
            @Parameter(description = "业务标识") @RequestParam("id") @NotNull Long id) {
        return R.ok(service.detail(id));
    }
}
```

标准类型化契约包含以下入口，实际前缀由 Controller 的 `@RequestMapping` 和模块 `module-path` 决定：

| 方法 | 路径 | 入参 | 行为 |
|------|------|------|------|
| `POST` | `/create` | `CreateXxxCommand` JSON | 调用 `service.create(command)` |
| `POST` | `/update` | `UpdateXxxCommand` JSON | 调用 `service.update(command)` |
| `POST` | `/delete` | `DeleteCommand` JSON | 调用 `service.delete(command)` |
| `GET` | `/detail?id=` | 受校验的主键 ID | 调用 `service.detail(id)` |
| `GET` | `/page` | `XxxPageQuery` query string | 调用 `service.page(query)` |

`mango-infra-persistence-web-starter` 中的 `BaseCrudController<S, C, U, Q>` 继续作为存量兼容和导入导出编排能力存在，但新的业务 Controller 不再通过继承它生成。存量模块迁移时，把实际使用的 create/update/delete/detail/page/export/import 方法逐项移到实现 `XxxApi` 的 Controller；结构约束以 [后端 API 规范](../../../mango-pmo/rules/backend/03-api.md) 为准。

导入导出限制：

- 没有 `ExcelAdapter` bean 时，导出抛出 `Excel 导出能力未启用`，导入抛出 `Excel 导入能力未启用`。
- `POST /export` 要求 Service 实现 `ExportableService<Q, ROW>`。
- `POST /import` 和 `/import-template` 要求 Service 实现 `ImportableService<ROW>`。
- `POST /import` 的模式可用请求参数 `importMode` 或 `mode` 覆盖，值为 `PARTIAL_SUCCESS` 或 `ALL_SUCCESS`。
- 如果容器里有 `Validator`，导入行会先执行 Bean Validation，再执行业务 `validateImportRows`。
- `PARTIAL_SUCCESS` 会导入校验通过的行；`ALL_SUCCESS` 只要有错误就不导入。

#### 7.4.1 默认 Excel 导入

字段可以按规范化标题或固定零基列号映射，两者必须二选一：

```java
public class TenderLedgerImportRow {

    @ExcelLine
    private Long lineNum;

    @ExcelColumn(title = "渠道", aliases = {"合作渠道"}, required = true,
            dictType = "tender_channel")
    private String channel;

    @ExcelColumn(title = "协议号", required = true,
            converter = AgreementNoConverter.class)
    private String agreementNo;

    @ExcelColumn(idx = 4)
    private BigDecimal amount;
}
```

- title 会执行全半角和连续空白归一化后精确匹配；列乱序不影响结果。
- `idx` 从 `0` 开始，只适用于固定列序模板；title 匹配失败不会按 idx 兜底。
- `converter` 优先于 `dictType`；配置 Converter 后，`dictType` 只作为字段元数据传给 Converter。
- 未配置 Converter 且存在 `dictType` 时，`mango-system-starter` 提供字典 label 到 value 的解析。
- 两者均未配置时使用内置字符串、数字、布尔、枚举、日期和时间转换。
- Converter 可以注册为 Spring Bean；没有 Bean 时必须提供无参构造。

普通 Controller 可以直接使用：

```java
@PostMapping("/import")
public R<ImportResult> importTender(
        @RequestExcel(fileName = "file", sheetName = "投标模板", headRowNumber = 2)
        List<TenderLedgerImportRow> rows) {
    return R.ok(service.importRows(rows));
}
```

`headRowNumber = 2` 表示第一行 title、第二行说明、第三行开始为数据，`@ExcelLine` 的第一条值为 `3`。`sheetName` 非空时按名称选 Sheet，否则使用零基 `sheetIndex`。

校验顺序为工作簿结构、单元格转换、Jakarta Validation、`validateImportRows` 业务批量校验、事务入库。业务批量校验用于文件内重复、跨字段、数据库关联和数据库重复；同一行可以返回多个 `ImportError`。

`BaseCrudController` 存在 Spring 事务管理器时会在事务内调用 `importRows`。`PARTIAL_SUCCESS` 只传入无错误行；`ALL_SUCCESS` 在任一校验错误时不调用入库，入库运行时异常会回滚并进入 `batchErrors`。

存在行级错误且应用装配 `mango-file-starter` 时，默认 Adapter 从原工作簿生成失败文件，只保留失败数据行并追加“失败原因”，通过 Mango File 保存后在 `ImportResult.failureFileId` 返回文件 ID。

模板下载配置 `templateLocation = "classpath:/templates/tender.xlsx"` 时直接复制原始 xlsx，保留说明行、字典 Sheet、数据验证、公式、列宽和冻结窗格；未配置时根据 `@ExcelColumn` 生成空模板。

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

## 8. 数据与初始化
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
| 标准字段 | 默认要求 `tenant_id`、`org_id`、`created_by`、`created_at`、`updated_by`、`updated_at` |
| 租户字段 | `tenant_id` 参与租户行级过滤 |
| 组织字段 | `org_id` 作为组织数据权限默认归属字段 |
| Flyway 路径 | 模块默认脚本放在 `db/migration/<module>/V*.sql`；停机升级可通过模块 `locations` 指向磁盘目录或远程 SQL 文件 |

普通租户业务表建议直接继承 `TenantEntity` 并保留这些标准字段。全局配置表、平台资源表、历史日志表、基础设施表和第三方表不适用时，应加入 `schema-validation.excluded-tables`；如果某个查询声明了数据权限但表缺少对应字段，查询会 fail-fast。

菜单、按钮和 API 权限不是 persistence 初始化的内容。业务模块如果提供管理页面，需要在自己的 resource manifest 或 authorization 初始化逻辑中登记菜单、按钮权限和 API 资源。

## 9. 管理入口
本模块不注册菜单和按钮权限。它只影响数据库层面的租户和审计。

租户行为：

- 默认 `mango.persistence.mybatis-plus.tenant.enabled=true`，所有非排除表 SQL 会自动追加租户条件。
- 租户值优先读取 `MangoContextHolder.tenantId()`，没有时读取 `default-tenant-id`。
- 租户值全是数字时生成数值表达式，否则生成字符串表达式。
- insert 语句如果已经包含租户列，租户拦截器不会重复插入。
- `TenantEntity.tenantId` 在 insert 时由审计填充器从上下文写入，支持实体 setter 类型为 `String` 或 `Long`。
- `TenantEntity.orgId` 在 insert 时默认从当前组织上下文写入；业务已经设置 `orgId` 时保留业务值。

审计行为：

| 场景 | 自动填充字段 |
|------|--------------|
| insert | `createdBy`、`createdAt`、`createTime`、`updatedBy`、`updatedAt`、`updateTime`、`tenantId`、`orgId` |
| update | `updatedBy`、`updatedAt`、`updateTime` |

时间字段支持 `LocalDateTime`、`Instant`、`Date`。`createdBy` 和 `updatedBy` 来自 `MangoContextHolder.userId()` 对应的 persistence context，`orgId` 默认来自 `partyType=org` 时的 `partyId`。

当前注意点：

- `@IgnoreTenant` 目前不是自动绕过租户拦截的开关。需要绕过租户的表，应配置到 `mybatis-plus.tenant.excluded-tables`。
- 平台全局表、字典表、资源表通常应该加入租户排除；普通业务表不应加入。
- 如果业务接口要做按钮权限校验，需要在 Web、安全或 authorization 层声明，不由 persistence 处理。

## 10. 快速开始
1. 引入 `mango-infra-persistence-starter`；需要导入导出、Excel Web 参数解析等能力时再引入 `mango-infra-persistence-web-starter`。
2. 配置应用 `DataSource`，或配置 `mango.persistence.datasources` 多数据源。
3. 为业务模块创建 `db/migration/<module>/V1__init.sql`，租户业务表包含 `id`、`tenant_id`、`org_id` 和审计字段。
4. 实体继承 `TenantEntity` 或按同名字段自定义实体，Mapper 继承 MyBatis-Plus `BaseMapper<E>`。
5. 查询对象继承项目分页查询基类，按需使用 `@QueryField` 和 `@QueryIgnore`。
6. `IXxxService` 继承 `MangoTypedCrudService`，`XxxService` 继承 `MangoCrudServiceImpl` 并实现类型化方法；复杂数据范围覆写 `applyDataScope()`。
7. Controller 直接实现本域 `XxxApi`、注入 `IXxxService` 并逐项声明 HTTP mapping；导入导出能力按需实现 `ExportableService` 或 `ImportableService`。
8. 给管理页面登记菜单、按钮权限和 API 权限；这一步属于业务模块自己的 authorization 资源初始化。
9. 本地验证 Flyway、审计填充、租户隔离、分页查询和 Schema 校验。

## 11. 问题排查
**启动时报 `Missing tenant context for tenant-isolated SQL`**

当前 SQL 命中了租户拦截器，但上下文里没有租户。Web 请求要检查认证和上下文写入；定时任务或测试可配置 `mango.persistence.mybatis-plus.tenant.default-tenant-id`；全局表要加入 `mybatis-plus.tenant.excluded-tables`。

**表结构校验提示缺少 `tenant_id`、`org_id` 或审计字段**

普通业务表应该补字段。平台全局表、历史表或第三方表如果确实不归 Mango 业务规范管理，加入 `mango.persistence.schema-validation.excluded-tables`。

**数据权限查询提示缺少 `org_id` 或 `created_by`**

当前查询已经声明使用组织或本人数据权限，但 `DataScopeMapping.tableName` 对应表没有映射字段。需要给业务表补列，或调整该资源的数据权限模式和字段映射。

**Flyway 没执行业务模块 migration**

检查脚本路径是否是 `db/migration/<module>/V*.sql`，检查 `mango.persistence.flyway.enabled` 和 `mango.persistence.flyway.modules.<module>.enabled`，再检查模块脚本是否已经被当前模块 history table 记录。

启用模块运行态诊断时，Persistence 只报告真实初始化流程记录的 RUNNING/APPLIED/FAILED/DISABLED、current version、pending count 和 history table；诊断调用本身不会执行 migrate、repair 或 validate。启动期 Flyway 失败仍按原行为阻断应用启动。

**多数据源切换在事务里失败**

这是预期保护。一个事务内已经绑定了某个 Mango 数据源后不能切换。把不同数据源写入拆到不同事务边界，或重新设计跨库流程。

**`@IgnoreTenant` 标了但 SQL 仍然追加租户条件**

当前 starter 没有读取这个注解。需要绕过租户的表请配置 `mango.persistence.mybatis-plus.tenant.excluded-tables`。

**导入导出接口存在但调用失败**

`BaseCrudController` 只是提供入口。导出需要 Service 实现 `ExportableService`，导入需要 Service 实现 `ImportableService`。使用官方实现时检查是否依赖 `mango-infra-excel-starter`；字段配置 `dictType` 时还需装配 `mango-system-starter`，需要失败文件 ID 时还需装配 `mango-file-starter`。

**业务只拿到发布 jar，读不到使用说明**

Maven 运行时 jar 不承载 README。持久化能力的使用说明由文档站和源码 README 交付：

- 文档站入口：[Mango 能力地图](../../../mango-docs/capabilities/README.md) -> Persistence 持久化。
- 源码入口：`mango/mango-infra/mango-infra-persistence/README.md`。
- 本地预览：`npm --prefix mango-docs run docs:dev`。

如果业务开发只拿到 Maven 坐标，没有文档仓库或文档站地址，说明交付物缺少文档入口，应补发 Mango 文档站地址或同步对应版本的文档快照，而不是把 README 打进 jar。

## 12. 相关文档
- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [持久化规范](../../../mango-pmo/rules/backend/07-persistence.md)
- [交付质量门禁](../../../mango-pmo/rules/05-ai-delivery-quality.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 13. 补充资料
- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
