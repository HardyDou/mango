# 模块 Flyway 隔离交付台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| MFI-001 | 用户要求 | 模块独立 migration 目录 | 使用 `classpath:db/migration/{module}` 自动发现或显式配置模块 | `PersistenceFlywayAutoConfiguration` | persistence starter 测试 | DONE | `mango/mango-infra/mango-infra-persistence/mango-infra-persistence-starter` |
| MFI-002 | 用户要求 | 模块独立 Flyway history table | 默认 `flyway_schema_history_{module}`，支持配置覆盖 | `PersistenceFlywayProperties`、`PersistenceFlywayAutoConfiguration` | 测试检查 history table | DONE | `mango/mango-infra/mango-infra-persistence/mango-infra-persistence-starter` |
| MFI-003 | 用户要求 | 尝试模块独立数据库 | 模块配置 datasource 后 Flyway 使用独立迁移数据源，不影响主数据源 | `PersistenceFlywayProperties`、`PersistenceFlywayAutoConfiguration` | H2 双库测试 | DONE | `mango/mango-infra/mango-infra-persistence/mango-infra-persistence-starter` |
| MFI-004 | 规范要求 | 验证改动 | 执行相关 Maven 测试和台账检查 | 验证命令输出 | Maven/PMO 检查通过 | DONE | 本文件 |
