# 标准交付记录

> Issue #674 Workflow 空库初始化顺序

## 1. 元数据

- 任务 ID：GitHub Issue #674
- 交付模式：STANDARD
- 需求影响：L2 - 启用 Mango Workflow 的 Spring Boot 直启业务应用无法在空库完成启动，但影响入口限定为未采用 Mango Bootstrap lifecycle 的数据库初始化链路。
- 方案风险：L2 - 修复进入共享 Persistence starter，并改变直启应用的启动期迁移时点；新 lifecycle 的 bootstrap/runtime 边界保持不变。
- 最终风险：L2
- 工作区决策：REUSE - `fix/issue-674-workflow-flyway-order`

## 2. 目标与范围

- 目标：让未配置 `mango.bootstrap.mode` 的兼容直启应用在数据库依赖 Bean 创建前完成模块 Flyway EXPAND migration，保证 Workflow 在查询 `ACT_GE_PROPERTY` 前已创建 Flowable 表。
- 成功条件：空库直启时 migration 先执行；`mango.bootstrap.mode=bootstrap` 或 `runtime` 时不恢复自动 migration；现有模块化 history table、配置和 migration 内容不变。
- 处理范围：Persistence Flyway 自动配置与回归测试、Bootstrap/Persistence/Workflow 能力说明、能力地图。
- 不处理范围：不修改 Workflow V1/V2 SQL，不恢复 Spring Boot 默认的单 history Flyway，不改变 Mango Bootstrap apply/runtime 协议，不发布 Maven 制品。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| SR-674-01 | Spring Boot 兼容直启应用 | 未配置 `mango.bootstrap.mode`，启用 Persistence 与 Workflow Flyway，数据库为空 | Persistence 在数据库依赖 Bean 前执行 EXPAND，随后 Workflow metadata 和 Flowable 正常初始化 | migration 失败时以具体模块、history table、location 和 datasource 上下文阻断启动 | migration history 与 `ACT_GE_PROPERTY` 均存在，应用上下文不再因缺表失败 |
| SR-674-02 | Mango Bootstrap lifecycle | `mango.bootstrap.mode=bootstrap` 或 `runtime` | 不创建兼容自动迁移初始化器；migration 仍只由 Bootstrap step 执行 | runtime 缺回执或 schema 时沿用 lifecycle 的 fail-closed 语义 | 两种 mode 均无 `FlywayMigrationInitializer`，上下文启动不自动建业务表 |
| SR-674-03 | 重复启动 | 目标模块 migration 已执行 | Flyway 按模块 history table 判定为最新，不重复执行 DDL | checksum 或缺失 migration 仍按 Flyway 校验阻断 | 第二次启动无重复 migration，既有数据库行为不变 |
| SR-674-04 | Bootstrap lifecycle schema | classpath 存在 `db/migration/bootstrap` | 该目录只由 `BootstrapSchemaMigrator` 管理，不进入 Persistence 模块发现和声明校验 | Bootstrap lifecycle 自身迁移失败时沿用 Bootstrap 失败语义 | 兼容直启不要求业务声明 `mango.persistence.flyway.modules.bootstrap`，也不创建 Persistence 模块 history |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| TD-674-01 | SR-674-01, SR-674-03 | 恢复 Spring Boot 标准 `FlywayMigrationInitializer` 作为兼容桥，内部只复用 `PersistenceFlywayBootstrapExecutor.migrate(EXPAND)`，不复制 migration 实现 | `mango-infra-persistence-starter` | 删除兼容 Bean 与条件类即可回退，不涉及数据回滚 |
| TD-674-02 | SR-674-02 | 通过条件判定仅在 `mango.bootstrap.mode` 未配置时创建兼容 Bean；任何 lifecycle mode 均保持显式 Bootstrap 所有权 | `mango-infra-persistence-starter` | 同上 |
| TD-674-03 | SR-674-01 | 继续使用 Spring Boot 数据库初始化依赖检测，使 `@DependsOnDatabaseInitialization` 识别兼容 Bean并建立顺序 | Persistence / Workflow Spring 装配 | 同上 |
| TD-674-04 | SR-674-01, SR-674-04 | 从 Persistence classpath 模块发现中排除 Bootstrap 自有 schema；其表仍由 `BootstrapSchemaMigrator` 独立管理 | Persistence / Bootstrap schema 所有权 | 删除排除项即可回退，但会使旧业务直启因未声明 `bootstrap` 而失败 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| IMPL-674-01 | TD-674-01, TD-674-02 | 1 | `PersistenceFlywayAutoConfiguration.java` | 兼容 Bean 条件装配并调用 EXPAND |
| IMPL-674-02 | TD-674-01, TD-674-02 | 2 | `PersistenceFlywayAutoConfigurationTest.java` | 覆盖直启自动迁移、bootstrap/runtime 不自动迁移和禁用开关 |
| IMPL-674-03 | TD-674-03 | 3 | Persistence/Workflow README、能力地图 | 当前初始化模式、顺序与排障入口可直接判断 |
| IMPL-674-04 | TD-674-04 | 4 | Bootstrap README、Persistence 模块发现测试资源 | Bootstrap schema 所有权与排除行为有自动化证据 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| SR-674-01, SR-674-02, SR-674-03 | M11 H2 模块集成测试 | `mvn -q -f mango/mango-infra/mango-infra-persistence/mango-infra-persistence-starter/pom.xml -Dtest=PersistenceFlywayAutoConfigurationTest test` | PASS：26 tests，0 failures，0 errors | `target/surefire-reports/TEST-io.mango.infra.persistence.starter.PersistenceFlywayAutoConfigurationTest.xml` |
| SR-674-01, SR-674-04 | M11 MySQL 空库 Workflow 装配 | 将补丁 starter 以 `1.0.29` 安装到隔离 Maven 仓库，启动宝涵后端连接 0 表 MySQL 库 | PASS（Issue 范围）：生成 289 张表；`flyway_schema_history_workflow` 有 3 条成功记录；`ACT_GE_PROPERTY` 有 13 条 Flowable 7.0 元数据；日志依次出现 Workflow history 创建、ProcessEngine 创建和应用启动，且无 `ACT_GE_PROPERTY` 缺表错误 | 数据库查询与 `.mango/run/logs/baohan-system-service.log`；验证后已执行 `mango dev stop` |
| SR-674-01, SR-674-02, SR-674-03, SR-674-04 | M09 直接修改模块 verify | `mvn -q -f mango/mango-infra/mango-infra-persistence/mango-infra-persistence-starter/pom.xml verify` | PASS：98 tests，0 failures，0 errors，1 skipped | `target/surefire-reports/TEST-*.xml` |
| SR-674-01, SR-674-02, SR-674-03, SR-674-04 | 代码与文档门禁 | `git diff --check`、测试质量、mock audit、模块 README audit、README source facts audit | PASS | 各命令退出码均为 0；mock audit block=0、warn=0 |

## 7. 例外与剩余风险

- 当前尚未发布 Maven 修复版本；消费项目在新版本发布前仍会复现 1.0.29 的问题。
- 宝涵空库回归在 Spring Boot 已报告启动成功后，被业务侧 `ProjectWorkflowDefinitionInitializer` 因缺少内置角色终止；这不影响本 Issue 的 Flyway/Flowable 顺序验收，但该业务初始化器问题需由宝涵项目单独处理，故未取得持续健康态证据。
