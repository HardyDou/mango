# 标准交付记录

任务：Issue #835 历史角色数据权限资源停用修复。

## 1. 元数据

- 任务 ID：GitHub Issue #835
- 交付模式：STANDARD
- 需求影响：L2 - 已有数据库升级时，历史 `AUTH_ROLE_DATA_SCOPE` 声明缺失会阻断 Resource Bootstrap FINALIZE 和应用部署
- 方案风险：L2 - 修复涉及授权数据的跨租户稳定身份定位，必须避免默认租户兜底、业务键误匹配和跨租户更新
- 最终风险：L2
- 工作区决策：CREATE（`/Users/hardy/Work/mango-issue-835`，`fix/issue-835-auth-role-data-scope-disable`）
- 启用能力：M01、M08、M09、M10、M11

## 2. 目标与范围

- 目标：让 Resource Registry 在已有数据库中安全停用已从当前声明集合消失的 `AUTH_ROLE_DATA_SCOPE`，不再因缺少原声明字段阻断升级。
- 成功条件：Registry 仅提供 `targetId/targetTable` 时，Handler 从真实目标记录取得租户并在该租户上下文停用；无效或不一致身份明确失败且不更新任何其它记录；Registry 状态在成功后进入 `REMOVED`。
- 处理范围：角色数据权限 Mapper、Resource Handler、真实 Mapper 集成测试、Resource Registry 两轮同步回归、相关能力说明和业务排障指南影响边界。
- 不处理范围：修改 Resource Registry 表结构、恢复历史声明 payload、手工修复业务数据库、审计并修改其它 Resource Handler、Maven 制品发布和业务项目部署。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| REQ-001 | Resource Bootstrap FINALIZE | Registry 中存在 `AUTO` 历史记录及真实 `targetId`，当前模块声明已删除 | 通过目标记录读取真实租户，在正确租户上下文将数据范围状态改为禁用，并把 registry 标记为 `REMOVED` | 不依赖已消失声明中的 `tenantId/roleCode/resourceCode` | 两轮真实 Registry/Mapper 集成测试通过 |
| REQ-002 | `AuthRoleDataScopeResourceHandler.disable` | 仅携带 Registry 可重建的 `targetId/targetTable` | 精确停用该主键目标，不按默认租户或业务键猜测 | 目标不存在时包含 `resourceId/targetId` 明确失败，不回退查询 | Handler 真实 Mapper 集成测试通过 |
| REQ-003 | 多租户授权数据 | 目标属于非默认租户，或输入身份与目标不一致 | 使用目标真实租户；仍携带的租户、应用、资源或目标表身份必须一致 | 任一不一致立即失败，其他租户记录保持不变 | 跨租户正反例集成测试通过 |
| REQ-004 | 现有显式禁用调用方 | 声明没有 `targetId`，但包含完整业务字段 | 保持原有按租户、角色和资源编码停用行为 | 原有字段校验和角色不存在语义不变 | 现有 Handler 测试继续通过 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| DEC-001 | REQ-001、REQ-002、REQ-003 | 增加只按主键、显式忽略租户插件的受限 Mapper 查询；只用于 Registry 历史目标恢复，不开放通用跨租户查询 | `RoleDataScopeMapper` 及 XML | 删除专用查询并恢复旧 Handler 分支 |
| DEC-002 | REQ-001、REQ-002、REQ-003 | `targetId` 路径先跨租户读取身份，再进入目标租户上下文重新查询和更新；失败时禁止回退业务键 | `AuthRoleDataScopeResourceHandler` | 恢复原 `tenantId` 必填路径 |
| DEC-003 | REQ-004 | 无 `targetId` 时保留完整声明的现有停用实现 | `AuthRoleDataScopeResourceHandler` | 不适用，属于兼容保留 |
| DEC-004 | 全部 | 通过 H2 + MyBatis-Plus 真实 Mapper 和真实 Resource Registry 两轮同步验证，不使用 Mapper mock | Authorization Starter 测试 | 删除新增测试，不影响生产代码回滚 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---:|---|---|
| IMP-001 | DEC-001 | 1 | Authorization Core Mapper 和 XML | 可按精确主键读取目标租户和身份，普通 Mapper 仍受租户插件约束 |
| IMP-002 | DEC-002、DEC-003 | 2 | `AuthRoleDataScopeResourceHandler` | Registry 与完整声明两种停用路径均符合失败语义 |
| IMP-003 | DEC-004 | 3 | Authorization Starter 集成测试 | targetId-only、非默认租户、无效/不一致身份和两轮 Registry 同步均有断言 |
| IMP-004 | 全部 | 4 | Resource/Authorization README、能力地图、业务排障指南、定向质量门禁 | 升级行为及无影响边界可发现，M09-M11 全部通过或明确记录阻塞 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| REQ-001 | M11 Registry/Mapper 组合集成 | `mvn -f mango/pom.xml -pl mango-platform/mango-authorization/mango-authorization-core,mango-platform/mango-authorization/mango-authorization-starter -Dtest=AuthRoleDataScopeResourceHandlerIntegrationTest,AuthRoleDataScopeResourceRegistryIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test` | PASS | `AuthRoleDataScopeResourceRegistryIntegrationTest` 1/1：真实 Registry 两轮 FINALIZE 后目标状态为 `0`、Registry 为 `REMOVED`、DISABLE 日志为 `SUCCESS` |
| REQ-002、REQ-003、REQ-004 | M10/M11 Handler 集成 | 同上 | PASS | `AuthRoleDataScopeResourceHandlerIntegrationTest` 13/13；两类测试合计 14/14，无失败、错误或跳过 |
| 全部 | M09 模块质量和静态验证 | `mvn -f mango/pom.xml -pl mango-platform/mango-authorization/mango-authorization-core,mango-platform/mango-authorization/mango-authorization-starter verify`；`git diff --check`；`node mango-pmo/tools/test-quality-check.mjs --base origin/main`；`node mango-pmo/tools/audit-backend-test-mocks.mjs --report-only --changed-only --base origin/main` | PASS | 两个直接修改模块共 136 个测试通过；diff 无空白错误；2 个变更测试文件质量检查通过；Mock 审计 block/warn 均为 0 |
| 全部 | M08 能力说明 | `node mango-pmo/tools/check-standard-delivery-record.mjs mango-docs/plans/2026-08-25-issue-835-auth-role-data-scope-disable-delivery-record.md`；`node mango-pmo/tools/audit-module-readmes.mjs`；`node mango-pmo/tools/audit-readme-source-facts.mjs`；`node mango-pmo/tools/check-business-guides.mjs` | PASS | STANDARD 记录、模块 README 结构、README 源码事实和业务指南均通过 |

## 7. 例外与剩余风险

- 当前无代码验证例外。制品发布和 Baohan 已有数据库部署回归不属于本次代码修复授权，仍需独立发布与消费验证；本次没有启动服务、发布 Maven 制品、连接或修改 Baohan 数据库。
