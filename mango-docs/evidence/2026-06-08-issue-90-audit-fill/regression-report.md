# Issue #90 回归测试报告

- 日期：2026-06-08
- 分支：feature/issue-90-audit-fill
- Issue：#90 企业 CRUD 生成模块审计字段未自动填充
- 范围：Mango 持久化审计基类、CRUD 集成测试、CLI 生成业务模块消费验证

## 改动验证

`AuditableEntity` 的 `createdBy`、`createdAt`、`updatedBy`、`updatedAt` 已声明 MyBatis-Plus `@TableField(fill = ...)`：

- `createdBy`：`FieldFill.INSERT`
- `createdAt`：`FieldFill.INSERT`
- `updatedBy`：`FieldFill.INSERT_UPDATE`
- `updatedAt`：`FieldFill.INSERT_UPDATE`

CLI 生成的业务实体继续继承 `TenantEntity`，因此自动继承审计字段填充策略，不需要在每个生成实体重复声明审计字段。

## 自动化验证

```bash
mvn -pl mango-infra/mango-infra-persistence/mango-infra-persistence-starter -am test -Dtest=MangoCrudServiceImplIntegrationTest,PersistenceAuditAutoConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false
```

结果：通过。`Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`。

该测试直接创建 H2 表 `demo_user`，通过 `MangoCrudServiceImpl#createByCommand` 和 `updateByCommand` 走真实 MyBatis-Plus Mapper 链路，并直接查询数据库断言：

- `tenant_id = tenant-a`
- `created_by = 1001`
- `created_at IS NOT NULL`
- `updated_by = 1001`
- `updated_at IS NOT NULL`

```bash
pnpm --dir mango-ui/packages/mango-cli test
```

结果：通过。`mango-cli full/custom/add/module checks passed`。

```bash
mvn -pl mango-infra/mango-infra-persistence/mango-infra-persistence-starter -am install -DskipTests
```

结果：通过。用于让生成项目消费本次修复后的本地 `1.0.0-SNAPSHOT`。

```bash
node mango-ui/packages/mango-cli/src/index.mjs init issue90-audit-app --preset custom --modules none --topology monolith --package com.example.issue90 --group-id com.example --mango-version 1.0.0-SNAPSHOT --force
node mango-ui/packages/mango-cli/src/index.mjs module add procurement --module-name 采购管理 --aggregate purchase-order --aggregate-name 采购订单 --project-dir .runtime/projects/issue-90-audit-fill/issue90-audit-app
```

结果：通过。生成项目中的 `PurchaseOrderEntity` 继承 `TenantEntity`，migration 包含 `tenant_id/created_by/created_at/updated_by/updated_at`。

```bash
mvn -f backend/pom.xml -pl modules/procurement/procurement-starter -am -DskipTests compile
mvn -f backend/pom.xml -DskipTests compile
```

结果：通过。采购模块和完整生成后端均可编译。

```bash
mvn -pl mango-infra/mango-infra-persistence/mango-infra-persistence-starter -am mango:check -Drule=all -DskipTests
```

结果：未通过，判断为当前仓库已知治理/工具链问题，不属于本轮审计字段修复回归失败。失败点包括根聚合静态分析解析无关模块 `mango-domain-core -> mango-domain-api:1.0.0-SNAPSHOT` 时无法从远端仓库解析 SNAPSHOT，以及 `aktStatus is NULL: maximum Iterations exceeded` 重复输出；该问题与 open issue #107 的 `mango:check` reactor/根聚合支持不足方向一致。

## 截图

- `audit-integration-test.png`：框架 CRUD 集成测试截图。
- `generated-purchase-order-page.png`：CLI 生成采购订单页面源码截图，覆盖新增、查询、详情、编辑、删除 UI 识别。

## 回归结论

- 企业 CRUD 生成实体继承 `TenantEntity` 后，审计字段填充策略由 `AuditableEntity` 统一提供。
- 框架真实 Mapper insert/update 链路已证明审计字段和租户字段会写入数据库。
- CLI 生成项目和采购订单模块可编译，生成页面/API/后端 CRUD 链路存在。

## 未覆盖项

- 未启动完整生成企业项目前后端做浏览器新增/编辑真实联调；本轮用框架集成测试直接查库和生成项目编译替代数据库核验。
- 尝试在生成项目内临时新增 JUnit 验证时失败，原因是生成业务模块默认不带测试依赖；该临时文件已删除。
- 两次 Maven 初始命令因在仓库根而非 `mango/` Maven 根执行，模块选择失败；随后已在正确目录重跑并通过。
- `mango:check` 因当前根聚合/依赖解析治理问题未通过，已单独记录为非本轮功能回归失败。
