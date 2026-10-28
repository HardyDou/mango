# 标准交付记录

> authorization_api_resource 逻辑删除残留导致 AUTH_API_RESOURCE 同步启动失败

## 1. 元数据

- 任务 ID：issue-1141
- 交付模式：STANDARD
- 需求影响：L2 - 接口资源同步影响所有声明 `AUTH_API_RESOURCE` 的模块，失败后果是应用启动失败。
- 方案风险：L1 - 改动限定在 `mango-authorization-core`，新增两条 Mapper SQL 和一个注册分支，回退方式明确。
- 最终风险：L2
- 工作区决策：CREATE；任务分支 `fix/issue-1141-api-resource-soft-delete`，工作区 `/Users/hardy/Work/mango-issue-1141`
- 评审选择：`AGENT_ONLY`。用户未指定外部同行评审，按规则保留三视角分析，不将其称为人工评审。

## 2. 目标与范围

- 目标：修复 `authorization_api_resource` 存在逻辑删除残留时，重新声明同一路由触发唯一键冲突导致应用启动失败的问题。
- 成功条件：
  1. 存在软删除行的路由被重新声明时，复活原行而不是插入新行
  2. 同一路由同时存在有效行和软删除行时，优先使用有效行且不产生重复有效记录
  3. 有集成测试真实覆盖 Mapper、SQL 和持久化结果
  4. 真实库上有 23 条软删除残留时后端仍能正常启动
- 处理范围：`mango/mango-platform/mango-authorization/mango-authorization-core`。
- 不处理范围：不修改唯一键结构，不修改 `deleted` 字段语义，不清理其它模块的历史残留数据。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| AC-01 | 应用启动与资源同步 | 路由存在 `deleted = 1` 行 | 复活该行并写入最新声明字段 | 复用时不再抛 `DuplicateKeyException` | 集成测试与真实库启动验证通过 |
| AC-02 | 应用启动与资源同步 | 同一路由同时有 `deleted = 0` 与 `deleted = 1` 行 | 使用有效行合并，不新增有效行 | 不产生第二条有效记录 | 集成测试断言有效行数为 1 |
| AC-03 | 应用启动 | 库中存在多条软删除残留 | 服务正常启动并对过期路由保持删除 | 仍在启动日志暴露具体失败原因 | 真实库重启后 health 返回 200 |

### 三视角复核

| 视角 | 当前来源 | 结论与依据 | 未解决项 | 下一步输入 |
|---|---|---|---|---|
| 事实与用户视角 | Issue #1141、`V1__init_authorization.sql`、`ApiResourceEntity`、`ApiResourceService` | 唯一键 `uk_authorization_api_resource_method_path` 不含 `deleted`；`deleted` 是 `@TableLogic`；`loadExistingIndex` 用普通查询自动过滤软删除行，导致同步走 insert 并撞唯一键。 | 其它模块是否也存在依赖该唯一键的写入路径未逐一排查。 | 以 `mango-authorization-core` 的同步路径为准修复并加回归测试。 |
| 技术与风险视角 | `ApiResourceMapper`、`ApiResourceMapper.xml`、`ApiResourceService` | 采用「读取含软删除行 + 复活原行」方案，不改唯一键结构，避免把 `deleted` 改为时间戳带来的全局影响。 | MyBatis-Plus 对 `@TableLogic` 自动追加条件，普通 update 无法命中软删除行。 | 使用 XML 显式 SQL 完成读取与复活。 |
| 验证与交付视角 | 集成测试、Maven verify、真实库启动 | H2 `MODE=MySQL` + 真实 Mapper 覆盖 Mapper、SQL 和持久化结果；真实库用 23 条残留验证启动。 | 未验证 MySQL 专有行为差异。 | 记录测试物料与剩余风险。 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| D-01 | AC-01 | 新增 `ApiResourceMapper.selectIncludingDeletedByModuleNames`，显式读取包含 `deleted = 1` 的行，不受 `@TableLogic` 过滤。 | `ApiResourceMapper.java`、`ApiResourceMapper.xml` | 删除新增方法并恢复原 `selectList` 查询。 |
| D-02 | AC-01 | 注册流程新增 `isSoftDeleted` 分支，命中时调用 `reviveSoftDeleted` 复活原行，复用原主键。 | `ApiResourceService.java` | 移除该分支。 |
| D-03 | AC-01 | 复活使用 XML 显式 `update ... set deleted = 0`，因为 `updateById` 和 `LambdaUpdateWrapper` 都会被 `@TableLogic` 拦截。 | `ApiResourceMapper.xml`、`ApiResourceService.java` | 删除 `reviveSoftDeleted` 语句。 |
| D-04 | AC-02 | `loadExistingIndex` 同一路由优先保留有效行，仅在当前为软删除行时替换索引项。 | `ApiResourceService.java` | 恢复原 `forEach` 覆盖逻辑。 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---:|---|---|
| I-01 | D-01 | 1 | `ApiResourceMapper.java`、`ApiResourceMapper.xml` | 能读取含软删除行，返回结果包含 `deleted` 字段。 |
| I-02 | D-02 | 2 | `ApiResourceService.java` | 软删除路由被复活而不是插入。 |
| I-03 | D-03 | 3 | `ApiResourceMapper.xml` | 显式 update 能把 `deleted` 写回 0。 |
| I-04 | D-04 | 4 | `ApiResourceService.java` | 同路由有效行优先，不产生重复有效记录。 |
| I-05 | AC-01/AC-02 | 5 | `ApiResourceServiceImplIntegrationTest.java` | 两个回归用例覆盖复活与重复路由场景。 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| AC-01 | M10/M11 | `mvn -f mango/pom.xml -pl :mango-authorization-core -am test -Dtest=ApiResourceServiceImplIntegrationTest` | PASS；23 项通过，含 `registerApiResourcesRevivesSoftDeletedResource`。 | surefire 报告 |
| AC-02 | M10/M11 | 同上，含 `registerApiResourcesPrefersActiveRowWhenRouteIsDuplicated` | PASS；有效行数为 1，访问决策命中。 | surefire 报告 |
| AC-03 | M11/M12 | 在真实库将 `mango-file-preview` 全部 23 行置为 `deleted = 1` 后重启后端 | PASS；health 返回 200，19 行被复活，4 行判定过期保持删除。 | `.mango/run/logs/mango-backend.log` 中 19 条 `Revive soft-deleted API resource` |
| AC-04 | M09 | `mvn -f mango/pom.xml -pl :mango-authorization-core -am verify` | PASS；68 项测试与架构、PMD、Checkstyle 门禁通过。 | Maven 输出 |
| AC-05 | M09 | `node mango-pmo/tools/test-quality-check.mjs --base origin/main` 与 `audit-backend-test-mocks.mjs --report-only --changed-only` | PASS；无新增测试质量与 mock 风险。 | 命令输出 |

## 7. 例外与剩余风险

- 例外：无。
- 剩余风险一：唯一键不含 `deleted` 的结构未变，同一路由最多只能存在一条 `deleted = 1` 行；若同一路由被多次删除，仍需改用删除时间戳语义才能彻底消除冲突。本轮按最小改动修复，避免改动字段语义影响全局查询。
- 剩余风险二：`mango-file-preview` 有 4 条路由本次未复活（判定为过期路由），其残留行仍以 `deleted = 1` 保留，不影响启动。
- 集成测试使用 H2 `MODE=MySQL`，未覆盖 MySQL 专有行为；真实库启动验证已覆盖实际 MySQL。
- 服务已停止；工作区数据库 `mango_dev_mango_issue_1141_005` 在任务收尾时释放。
