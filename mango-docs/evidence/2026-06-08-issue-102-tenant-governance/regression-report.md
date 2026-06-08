# Issue 102 回归测试报告

## 范围

- Issue: #102 治理 tenantId 自动注入与业务无感知租户隔离规范
- 分支: `feature/issue-102-tenant-governance`
- 日期: 2026-06-08

## 变更验证

| 场景 | 验证方式 | 关键断言 | 结果 |
|---|---|---|---|
| BaseMapper 自动过滤 | `TenantIsolationIntegrationTest` | `selectList` 不手写 tenant 条件，仅返回当前租户数据 | PASS |
| Wrapper 自动过滤 | `TenantIsolationIntegrationTest` | `LambdaQueryWrapper` 只按业务字段查询，不串租户 | PASS |
| 插入自动填充 | `TenantIsolationIntegrationTest` | 未调用 `setTenantId`，插入后 `tenant_id/created_by/updated_by` 来自上下文 | PASS |
| 自定义 XML SQL 自动过滤 | `TenantIsolationIntegrationTest` | XML SQL 不写 tenant 条件，仅返回当前租户数据 | PASS |
| 自定义 XML 分页 | `TenantIsolationIntegrationTest` | 分页总数和记录均按当前租户过滤 | PASS |
| 既有 CRUD/多表查询回归 | Maven 相关测试组合 | CRUD、多表查询、tenant 插件配置、审计填充测试全部通过 | PASS |
| PMO preflight | 本地 preflight 命令 | tenant 自动注入/过滤关键词加载 DB 与持久化规则 | PASS |
| CLI baseline 回归 | `pnpm --filter @mango/cli test` | #104 PMO sync 仍通过，业务 baseline 保留路径别名 | PASS |

## 执行命令

```bash
mvn -q -f mango/pom.xml -pl mango-infra/mango-infra-persistence/mango-infra-persistence-starter -am -Dtest=TenantIsolationIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test -Dcheckstyle.skip=true
mvn -q -f mango/pom.xml -pl mango-infra/mango-infra-persistence/mango-infra-persistence-starter -am -Dtest=TenantIsolationIntegrationTest,MangoCrudServiceImplIntegrationTest,MultiTableQueryServiceIntegrationTest,PersistenceMybatisPlusAutoConfigurationTest,PersistenceAuditAutoConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test -Dcheckstyle.skip=true
pnpm --filter @mango/cli test
node mango-pmo/tools/pmo-preflight.mjs --role dev --phase develop --task "Issue 102: tenantId 自动注入与租户自动过滤" --paths "mango/mango-infra/mango-infra-persistence,mango/mango-platform/mango-job"
```

## 结果摘要

```text
TenantIsolationIntegrationTest: PASS
Persistence related regression set: PASS
mango-cli full/custom/add/module/pmo sync checks passed.
PMO preflight includes rules/backend/04-db.md and rules/backend/07-persistence.md.
```

## UI / 截图识别

- 本次为后端持久化治理和 PMO 规范更新，无新增浏览器业务页面。
- 生成本报告 HTML 并通过 Playwright 截图保存，用作回归测试报告截图证据。

## 未验证项与风险

- 本次未批量重构 system、authorization、calendar、workflow、job 的历史显式 tenantId 代码；已输出迁移清单并固化后续治理顺序。
- 未连接真实 MySQL 执行租户插件验证；已使用 H2 MySQL mode 覆盖 MyBatis-Plus 插件、BaseMapper、Wrapper、XML SQL、分页、自动填充链路。
