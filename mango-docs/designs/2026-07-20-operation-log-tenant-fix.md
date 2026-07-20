# 标准交付记录

## 1. 元数据

- 任务 ID：INC-20260720-operation-log-tenant-null
- 交付模式：STANDARD
- 需求影响：L2 - 修复操作审计日志的租户归属和持久化结果，不改变业务接口契约
- 方案风险：L2 - 变更租户字段的入库语义，但限定在 `mango-system` 操作日志服务
- 最终风险：L2
- 工作区决策：CREATE - `fix/operation-log-tenant`

## 2. 目标与范围

- 目标：修复平台级请求记录操作日志时 `tenant_id` 显式写入 `NULL` 导致的数据库约束异常。
- 成功条件：无租户上下文的操作日志归属平台租户 `default` 并成功入库；普通租户日志保留原租户 ID。
- 处理范围：`mango-system-core` 的操作日志租户解析、定向单元/持久化集成测试。
- 不处理范围：通用持久化租户填充器、登录日志、前端页面和数据库结构。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| SR-001 | `OperationLogAspect` / `ISysLogService.recordOperationLog` | 平台级请求，命令未携带租户 ID | 日志以 `tenant_id=default` 入库 | 不再触发 `tenant_id cannot be null` | 持久化集成测试回读 `default` |
| SR-002 | `ISysLogService.recordOperationLog` | 命令携带普通租户 ID | 原租户 ID 原样归属 | 不得错误归入平台租户 | 持久化集成测试回读原租户 ID |
| SR-003 | 既有操作日志入口 | 任意操作日志记录 | HTTP/API 契约和表结构不变 | 不引入跨模块或前端回归 | 直接修改 Maven 模块 `verify` 通过 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| TD-001 | SR-001, SR-002 | 在操作日志服务将空白租户规范化为既有平台租户标识 `default`；非空租户去除首尾空白后保留 | `SysLogService` | 回退租户解析方法及调用 |
| TD-002 | SR-001, SR-002 | 使用 Spring + MyBatis-Plus + H2 的真实 Mapper 入库测试覆盖非空约束和字段回读 | `SysLogServiceIntegrationTest` | 删除新增测试 |
| TD-003 | SR-003 | 不修改通用 `PersistenceAuditMetaObjectHandler`，避免扩大到所有租户表 | 无跨模块改动 | 无需额外回滚 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| IM-001 | TD-001 | 1 | `mango-system-core/.../SysLogService.java` | 入库前得到非空的有效租户 ID |
| IM-002 | TD-002 | 2 | `mango-system-core/src/test/.../SysLogServiceIntegrationTest.java` | 平台租户和普通租户场景均通过 |
| IM-003 | TD-003 | 3 | `mango-system-core` | 定向 `verify` 通过且无范围外改动 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| SR-001 | M11 持久化集成测试 | `mvn -pl mango-system-core -Dtest=SysLogServiceIntegrationTest test` | 待执行 | 测试输出 |
| SR-002 | M11 持久化集成测试 | `mvn -pl mango-system-core -Dtest=SysLogServiceIntegrationTest test` | 待执行 | 测试输出 |
| SR-003 | M09/M10/M11 模块质量门禁 | `mvn -pl mango-system-core verify` | 待执行 | Maven 输出 |

## 7. 例外与剩余风险

- 不将无租户上下文推广为通用持久化默认值；仅操作日志按已有平台日志语义归属 `default`。
