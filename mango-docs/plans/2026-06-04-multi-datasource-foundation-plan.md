# Mango 多数据源底座开发计划

## 1. 背景

Mango 任务调度能力确定采用 Mango 原生 Job 契约和统一 UI，底层优先集成 PowerJob。该方案要求 Job 治理数据和 PowerJob 引擎数据使用独立数据库，并支持单体、微服务和共享 Job Center 等灵活部署形态。

为避免 `mango-job` 承担持久化基础设施职责，先建设 `mango-infra-persistence` 多数据源底座。

设计说明：`mango-docs/designs/mango-multi-datasource-foundation-design.md`

## 2. 目标

- 在 `mango-infra-persistence-starter` 中提供统一多数据源注册和路由能力。
- 支持模块到数据源的配置映射。
- 支持 MyBatis-Plus 在运行期路由到指定数据源。
- 支持模块级 Flyway 迁移到不同数据库。
- 明确本地事务边界，禁止事务内静默切换数据源。
- 保持现有单数据源应用兼容。
- 为 `mango-job` 独立 `mango_job` 数据库和 PowerJob 独立 `powerjob` 数据库提供底座。

## 3. 范围

### 3.1 本次做

- 新增多数据源配置属性。
- 新增数据源注册表。
- 新增路由数据源。
- 新增数据源上下文和注解。
- 新增模块数据源映射。
- 扩展 Flyway 按模块选择数据源。
- 增加事务内切换保护。
- 增加单数据源和多数据源测试。
- 更新 persistence README。

### 3.2 本次不做

- 不开发 `mango-job`。
- 不集成 PowerJob。
- 不实现跨库强一致事务。
- 不实现跨库 join。
- 不新增前端页面。
- 不修改业务模块表结构。

## 4. 影响模块

| 模块 | 改动 |
|---|---|
| `mango-infra-persistence-starter` | 核心实现和测试。 |
| `mango-infra-persistence` README | 增加多数据源使用说明。 |
| `mango-docs/designs` | 增加设计说明。 |
| `mango-docs/plans` | 增加开发计划和后续交付记录。 |

## 5. 开发拆分

### Sprint 0：设计与契约

目标：完成多数据源底座设计和开发计划。

交付物：

- `mango-docs/designs/mango-multi-datasource-foundation-design.md`
- `mango-docs/plans/2026-06-04-multi-datasource-foundation-plan.md`

验收：

- 文档写清目标、范围、不做什么、影响模块、接口变化、数据变化、测试范围。
- 用户确认后进入开发。

### Sprint 1：配置和注册表

目标：建立多数据源基础配置和注册能力。

改动项：

- 新增 `PersistenceDataSourceProperties`。
- 新增 `PersistenceDataSourceRegistry`。
- 新增 `PersistenceDataSourceDefinition`。
- 支持 `mango.persistence.datasources` 配置。
- 默认 `primary` 数据源兼容 Spring Boot `DataSource`。

验收：

- 未配置多数据源时现有单数据源测试通过。
- 配置 `primary`、`job` 时可注册两个数据源。
- 密码不出现在日志和异常输出中。

建议验证：

```bash
cd mango
mvn -pl mango-infra/mango-infra-persistence/mango-infra-persistence-starter test
```

### Sprint 2：运行期路由和模块映射

目标：让 MyBatis-Plus 可按上下文、注解或模块配置选择数据源。

改动项：

- 新增 `MangoRoutingDataSource`。
- 新增 `PersistenceDataSourceContext`。
- 新增 `@PersistenceDataSource`。
- 新增 AOP 或拦截器解析注解。
- 新增 `PersistenceModuleDataSourceResolver`。
- 支持 `mango.persistence.modules.<module>.datasource`。

验收：

- 无上下文时走 `primary`。
- 显式上下文可路由到 `job`。
- 注解可路由到 `job`。
- 模块映射配置可解析。
- Mapper 不直接依赖具体数据源 Bean。

建议验证：

```bash
cd mango
mvn -pl mango-infra/mango-infra-persistence/mango-infra-persistence-starter test -Dtest='*DataSource*Test,*Routing*Test'
```

### Sprint 3：事务边界保护

目标：明确并保护多数据源事务边界。

改动项：

- 记录事务开始时的数据源。
- 事务内切换到不同数据源时抛出业务明确异常。
- 文档说明跨库写入必须用 API、事件、outbox 或补偿。

验收：

- 单库事务正常提交和回滚。
- 事务内从 `primary` 切到 `job` 抛出异常。
- 异常包含当前数据源和目标数据源，不包含密码或敏感配置。

建议验证：

```bash
cd mango
mvn -pl mango-infra/mango-infra-persistence/mango-infra-persistence-starter test -Dtest='*Transaction*Test'
```

### Sprint 4：Flyway 多数据源迁移

目标：扩展现有 Flyway 模块迁移能力，接入统一数据源注册表。

改动项：

- Flyway 从 `PersistenceDataSourceRegistry` 取数据源。
- `mango.persistence.modules.<module>.datasource` 控制模块迁移目标库。
- 保留旧 `mango.persistence.flyway.modules.<module>.datasource` 兼容入口。
- 每个模块在所属库内使用独立 history table。

验收：

- `system` 模块迁移到主库。
- `job` 模块迁移到 job 库。
- 两个库分别生成模块 history table。
- 已有 Flyway 测试继续通过。

建议验证：

```bash
cd mango
mvn -pl mango-infra/mango-infra-persistence/mango-infra-persistence-starter test -Dtest='*Flyway*Test'
```

### Sprint 5：文档、回归和准入

目标：完成用户可消费文档和回归验证。

改动项：

- 更新 `mango-infra-persistence/README.md`。
- 补充配置示例：主库、job 库、powerjob 库。
- 补充限制：禁止跨库 join、禁止跨库外键、默认不支持跨库强一致事务。
- 评估是否需要新增 `mango:check` 规则输入，不在本 Sprint 强制实现规则。

验收：

- README 能指导业务模块配置独立数据库。
- 单数据源兼容测试通过。
- 多数据源路由、事务、Flyway 测试通过。
- `mvn mango:check` 无新增违规。

建议验证：

```bash
cd mango
mvn -pl mango-infra/mango-infra-persistence/mango-infra-persistence-starter test
mvn mango:check -Drule=all
```

## 6. 交付台账

本计划的交付台账独立存放，避免计划中的普通表格影响 PMO 台账检查。

台账路径：`mango-docs/plans/2026-06-04-multi-datasource-foundation-ledger.md`

## 7. 风险

| 风险 | 应对 |
|---|---|
| 现有单数据源应用被破坏 | 默认配置保持 `primary`，未配置多数据源时不改变行为。 |
| 事务内跨库写入产生不一致 | 第一版直接禁止事务内切换数据源。 |
| Flyway 旧配置和新配置冲突 | 新配置优先，旧配置作为兼容入口。 |
| PowerJob 表被 Mango Flyway 接管 | 明确 PowerJob 数据库由 PowerJob 管理，Mango 只保存映射关系。 |
| 后续需求要求跨库查询 | 不纳入底座，必须通过 API 或事件同步建模。 |

## 8. 完成标准

- 设计文档和计划已提交。
- 用户确认进入开发。
- 开发阶段每个 Sprint 有独立 worktree 和分支。
- 代码实现后对应测试命令执行并记录结果。
