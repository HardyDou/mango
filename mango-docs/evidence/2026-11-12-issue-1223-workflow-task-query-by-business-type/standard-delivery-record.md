# 标准交付记录

## 1. 元数据

- 任务 ID：Issue #1223
- 交付模式：STANDARD
- 需求影响：L2 - 改变公开任务查询契约的必填约束，影响业务列表分页语义、租户隔离和权限范围；不改变数据库结构
- 方案风险：L2 - 查询条件同时影响 `mango-workflow-api`、`mango-workflow-core`、`mango-workflow-starter` 和业务接入指南，失败会返回越权或不完整的分页结果
- 最终风险：L2
- 工作区决策：CREATE（`/Users/hardy/Work/mango-issue-1223`，分支 `feat/issue-1223-task-query-by-business-type`）

## 2. 目标与范围

- 目标：允许业务列表只按 `businessType`、`scope`、任务阶段、任务状态、处理人、时间和分页查询任务，`businessKeys` 不传或为空表示不限业务主键。
- 成功条件：`total` 按任务数统计，`list` 一行一个任务；传入 `businessKeys` 时保持原有过滤行为；业务侧不再需要先查业务表取候选业务键。
- 处理范围：`mango-workflow-api` 查询模型与接口说明、`mango-workflow-core` 服务/Mapper/测试、`mango-workflow-starter` Controller 文案、Workflow README、业务接入指南、能力地图和业务变更索引。
- 不处理范围：不新建任务索引表，不改数据库结构，不实现 `ORG` 组织范围，不接入角色数据范围配置，不修改 `todo` / `done` / `copied` 语义，不发布 Maven 或 npm 版本。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| AC-1 | 业务列表 / `POST /workflow/tasks/page-by-business-key` | `businessType` + 不传 `businessKeys` + `page` / `size` | 返回该业务类型下当前租户的全部任务，`total` 为任务总数 | `businessType` 为空仍报 `TASK_QUERY_INVALID` | `list` 一行一个任务，翻页不重复、不漏行 |
| AC-2 | 同上 | `businessKeys=[]` | 与不传等价，不限业务主键 | 不因空集合报错 | 空集合与 `null` 查询结果一致 |
| AC-3 | 同上 | 传入非空 `businessKeys` | 仍按业务主键集合过滤 | 单个业务主键为空或超长时按既有校验失败 | 与 Issue #1208 查询结果一致 |
| AC-4 | 同上 | `scope=SELF` 或 `scope=ALL` | `SELF` 仅返回本人办理或候选任务；`ALL` 仍校验 `workflow:task:all` | 缺权限返回 `TASK_QUERY_FORBIDDEN` | 范围语义与 #1208 一致 |
| AC-5 | 同上 | 跨租户数据 | 只返回当前租户任务 | 缺租户上下文报错 | 其他租户任务不可见 |
| AC-6 | 业务接入文档 | 业务列表按业务类型查询 | 文档明确不需要先枚举业务键，也不需要 2000 条业务键上限 | — | 文档与接口实际行为一致 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| D-1 | AC-1/AC-2 | 移除 `WorkflowTaskBusinessPageQuery.businessKeys` 的 `@NotEmpty`；保留字段级 `@Size(max = Integer.MAX_VALUE)` 满足协议模型校验契约，不设置实际数量上限；`null` 和空集合在服务层统一归一为空列表 | `WorkflowTaskBusinessPageQuery`、`WorkflowTaskBusinessQueryService` | 恢复 `@NotEmpty` 和服务层校验 |
| D-2 | AC-1/AC-2/AC-3 | Mapper 仅在 `businessKeys` 非空时追加 `a.business_key in (...)`；`businessType` 和租户条件始终生效 | `WorkflowTaskHistoryMapper.xml` | 回退 Mapper XML |
| D-3 | AC-4 | 保留 `scope=SELF` 的办理人/候选人/候选组过滤和 `scope=ALL` 的 `workflow:task:all` 校验 | `WorkflowTaskBusinessQueryService`、`WorkflowTaskQueryAuthorizer` | 回退范围判断 |
| D-4 | AC-1/AC-5 | 不传 `businessKeys` 时跳过逐申请 `WorkflowBusinessApplyDataPermissionProvider` 收窄，避免为分页枚举全部业务申请；范围由租户、`scope` 和权限码决定。传入 `businessKeys` 时保持原 Provider 收窄 | `WorkflowTaskBusinessQueryService`、`WorkflowBusinessApplyAccessChecker` | 恢复对全部申请执行 Provider 收窄，但会引入全量枚举成本 |
| D-5 | AC-1/AC-3 | 保留现有 `pageByBusinessKey` 方法和 HTTP 路径，避免业务调用方改路径；只把 `businessKeys` 从必填改为可选 | `WorkflowTaskRuntimeApi`、`WorkflowTaskController` | 回退接口说明和 DTO 校验 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| I-1 | D-1/D-5 | 1 | `mango-workflow-api` 查询模型、请求模型、接口说明 | `businessKeys` 不再强制非空，Swagger 描述与实际一致 |
| I-2 | D-2/D-3/D-4 | 2 | `mango-workflow-core` 服务、Mapper XML | 无业务键查询生成合法 SQL，保留租户和范围过滤 |
| I-3 | D-1/D-2 | 3 | `WorkflowTaskBusinessQueryServiceTest`、`WorkflowTaskHistoryMapperIntegrationTest` | 单测和 H2 集成测试覆盖 `null`、空集合、非空集合 |
| I-4 | D-5 | 4 | `WorkflowTaskController`、Workflow README、业务接入指南、能力地图、业务变更索引 | 接口文案和业务接入说明一致 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| AC-1/AC-2/AC-3/AC-4 | 单元测试 | `mvn -o -pl mango-platform/mango-workflow/mango-workflow-core -Dtest=WorkflowTaskBusinessQueryServiceTest test` | PASS（16 用例） | `WorkflowTaskBusinessQueryServiceTest`：无业务键与空集合不限、有业务键时 Provider 收窄、`SELF`/`ALL` 范围、分页参数校验 |
| AC-1/AC-2 | API 参数校验 | `mvn -o -pl mango-platform/mango-workflow/mango-workflow-starter -am -Dtest=WorkflowControllerValidationInheritanceTest -Dsurefire.failIfNoSpecifiedTests=false test` | PASS（2 用例） | `WorkflowControllerValidationInheritanceTest.businessTaskPageRequest_allowsMissingBusinessKeys`：只传 `businessType` 无校验错误，空 `businessType` 仍报错 |
| AC-1/AC-2/AC-3/AC-5 | H2 集成测试 | `mvn -o -pl mango-platform/mango-workflow/mango-workflow-core -Dtest=WorkflowTaskHistoryMapperIntegrationTest test` | PASS（12 用例） | 无业务键返回当前租户 3 条任务，分页 2 条，空集合同样生效，租户隔离保持 |
| AC-1~AC-5 | 模块验证 | `mvn -o -pl mango-platform/mango-workflow/mango-workflow-api,mango-platform/mango-workflow/mango-workflow-core,mango-platform/mango-workflow/mango-workflow-starter verify` | PASS | workflow-api、workflow-core 136 tests（4 skipped）、workflow-starter 46 tests，`BUILD SUCCESS` |
| AC-6 | 文档检查 | 文档审查 + PMO 文档 checker | PASS | Workflow README、业务接入指南、能力地图和业务变更索引已改为业务键可选 |

## 7. 例外与剩余风险

- 不传 `businessKeys` 时不做逐申请 `WorkflowBusinessApplyDataPermissionProvider` 收窄；需要该 Provider 级可见性时，调用方必须传 `businessKeys`。该限制已写入 Workflow README 和业务接入指南。
- 未执行真实业务库（`mango_dev_baohan_risk`）联调；本次验证使用 H2 真实列名夹具和模块测试。
- 未发布 Maven/npm 物料；业务侧在平台发版前仍需保留原有取业务键绕法。
- 业务键数量上限由业务侧自行控制；平台不新增业务键数量上限，但传入超大批量业务键仍会带来 SQL 参数和查询成本。
