# 标准交付记录

## 1. 元数据

- 任务 ID：Issue #1208
- 交付模式：STANDARD
- 需求影响：L2 - 新增公开 HTTP 接口、公开 VO 字段与权限码，涉及租户隔离和任务数据范围；不改变数据库结构
- 方案风险：L2 - 只读 SQL 直接关联 Flowable 历史任务表，与引擎 schema 耦合；跨 3 个模块
- 最终风险：L2
- 工作区决策：REUSE（`/Users/hardy/Work/mango-issue-1208`，分支 `feat/issue-1208-task-query-by-business`）

## 2. 目标与范围

- 目标：让业务列表能按业务主键批量读取全部任务（在途、已办理、已终止），解决审批流转后初审处理人和处理时间不可见的问题。
- 成功条件：同一申请单返回初审/复审/终审多行且各带处理人与时间；批量查询与单条查询结果一致；`SELF`/`ALL` 范围按约定生效；`createTime`/`endTime` 可排序且 `total` 与过滤结果一致。
- 处理范围：`mango-workflow-api`、`mango-workflow-core`、`mango-workflow-starter`、workflow 模块 README、业务接入指南、能力地图。
- 不处理范围：不新建表、不改表结构、不做存量回填；不支持"仅传 businessType 查全租户全部任务"；不修改 `todo`/`done`/`copied` 语义；不实现 `ORG` 组织范围；不接入角色数据范围配置（`DataScopeApplier`）。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| AC-1 | 业务列表 / `POST /workflow/tasks/page-by-business-key` | `businessType` + 单个 `businessKeys` | 返回该主键下所有申请的全部任务，一行一个任务 | 无命中返回空列表，不报错 | 同一申请单返回多节点任务，各带 `assigneeId`、`createTime`、`endTime` |
| AC-2 | 同上 | `businessKeys` 批量（含多条） | 结果等于逐个查询的并集 | 空集合报 `TASK_QUERY_INVALID` | 批量与单条结果一致，`total` 为过滤后总数 |
| AC-3 | 同上 | `scope=SELF`（默认） | 只返回办理人为当前用户，或候选用户/候选组命中的任务 | 缺用户上下文报错 | 他人且非候选的任务不可见 |
| AC-4 | 同上 | `scope=ALL` 且持有 `workflow:task:all` | 返回该业务主键范围内全部任务 | 无权限返回 `TASK_QUERY_FORBIDDEN`；未安装授权中心同样拒绝 | 越权调用返回 403 类业务失败 |
| AC-5 | 同上 | `scope=ORG` 或传 `deptIds` | 明确拒绝 | `TASK_QUERY_INVALID` | 不会静默忽略后返回全量 |
| AC-6 | 同上 | `sortBy=endTime`，含未办理任务 | `endTime` 为空的任务在 `asc` 和 `desc` 下都排最后 | 白名单外字段报错 | 翻页不重复、不漏行 |
| AC-7 | 同上 | 跨租户数据 | 只返回当前租户数据 | 缺租户上下文报错 | 其他租户任务不可见 |
| AC-8 | 同上 | 申请单 `orgId` 为空 + `scope=SELF` | 正常返回本人任务 | 不因组织为空拒绝 | 与 `done()` 口径一致 |
| AC-9 | 同上 | 撤回/驳回/退回后的申请 | 任务 `status=CANCELED` 带 `cancelCause`（RETURNED/WITHDRAWN/TERMINATED/OTHER）；`applyStatus` 表达整单状态；`returned`+`lastReturnAt`+`lastReturnTarget` 表达退回 | — | 能区分撤销、驳回与"退回待修改"，且不依赖引擎文案 |
| AC-10 | 同上 | 真实 MySQL（`mango_dev_baohan_risk`） | 在 Flowable 7.0.0 实际表结构上返回正确结果 | 列名不匹配时报 `Unknown column` | 同单多节点、分页、排序、SELF 候选组、assigneeIds、returned 全部符合预期 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| D-1 | AC-1/AC-2 | 不新建任务索引表；用一条只读 SQL 关联 `ACT_HI_TASKINST`、`workflow_business_apply`、`workflow_task_record`、`ACT_HI_IDENTITYLINK`/`ACT_RU_IDENTITYLINK`，过滤/排序/分页/统计全部下推 | `WorkflowTaskHistoryMapper` + `mapper/workflow/WorkflowTaskHistoryMapper.xml` | 移除接口与 Mapper，不动数据 |
| D-2 | AC-2 | `businessKeys` 不设数量上限；不传分页时按 `mango.workflow.task-query.unpaged-max-rows`（默认 2000）返回，`total` 恒为真实总数 | `WorkflowTaskBusinessQueryService`、`WorkflowTaskQueryProperties` | 调整配置或回退服务实现 |
| D-3 | AC-3/AC-4 | 数据范围由请求 `scope` 显式声明；`SELF` 的三段 OR 条件由本接口拼装；`ALL` 服务端校验 `workflow:task:all`；不接入 `DataScopeApplier` 与角色数据范围配置 | `WorkflowTaskQueryAuthorizer`、`WorkflowTaskBusinessQueryService` | 回退范围判断，接口签名不变 |
| D-4 | AC-4 | 接口级权限码用 `workflow:task:list`（专员 `SELF` 可用），`ALL` 单独校验 `workflow:task:all` | `WorkflowTaskController`、`WorkflowTaskQueryAuthorizer` | 调整权限码 |
| D-5 | AC-7 | 租户过滤取 `workflow_business_apply.tenant_id`，缺租户上下文直接报错 | Mapper XML、`WorkflowTaskBusinessQueryService` | 回退过滤条件 |
| D-6 | AC-9 | 不新增平台状态枚举；`status` 复用 `TODO`/`DONE` 并新增 `CANCELED`；`returned` 依据 `workflow_task_record.action='RETURN'`；新增返回 `applyId`、`applyStatus`、`claimTime`、`orgId`、`deleteReason` | `WorkflowTaskVO`、`WorkflowTaskRuntimeStatus`、Mapper XML | 回退字段与枚举 |
| D-7 | AC-1 | POST 请求体使用 `WorkflowTaskBusinessPageRequest`；自定义查询 Mapper 继承 `BaseMapper<WorkflowTaskHistoryEntity>`（Flowable 历史任务只读投影实体），满足架构契约 | `WorkflowTaskBusinessPageRequest`、`WorkflowTaskHistoryEntity`、`WorkflowTaskHistoryMapper` | 回退类型与实体 |
| D-8 | AC-3 | 业务申请可见性仅在业务模块注册 `WorkflowBusinessApplyDataPermissionProvider` 时参与收窄；未注册不做"申请人本人"兜底 | `WorkflowBusinessApplyAccessChecker.hasProvider`、`WorkflowTaskBusinessQueryService` | 回退 `hasProvider` 与收窄逻辑 |
| D-9 | AC-9 | 新增 `cancelCause` 枚举，依据"该任务是否有 RETURN 动作"与 `apply_status` 推导，不匹配 `deleteReason` 文本；新增 `lastReturnAt`/`lastReturnTarget`，取自最新 RETURN 记录的 `variables_json` | `WorkflowTaskCancelCause`、`WorkflowTaskVO`、Mapper XML、`WorkflowTaskBusinessQueryService` | 回退字段与推导逻辑 |
| D-10 | AC-10 | 历史任务创建时间使用 `ACT_HI_TASKINST.START_TIME_`；`CREATE_TIME_` 仅存在于 `ACT_RU_TASK`；H2 集成测试建表按真实列名固定 | Mapper XML、`WorkflowTaskHistoryEntity`、集成测试 | 回退列名 |
| D-11 | AC-3 | `assigneeIds` 的用户 ID 到用户名映射强制经 `tenant_member` 校验租户归属，避免跨租户同名用户串号 | Mapper XML、集成测试夹具 | 回退映射子查询 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| I-1 | D-1/D-2 | 1 | `WorkflowTaskHistoryMapper`、`mapper/workflow/WorkflowTaskHistoryMapper.xml` | H2 集成测试覆盖过滤、排序、分页、租户隔离 |
| I-2 | D-2/D-3/D-5 | 2 | `WorkflowTaskBusinessQueryService`、`IWorkflowTaskBusinessQueryService`、`WorkflowTaskQueryProperties` | 范围、分页、租户规则单测通过 |
| I-3 | D-4 | 3 | `WorkflowTaskQueryAuthorizer`、`WorkflowTaskController`、`WorkflowTaskRuntimeApi` | 无权限调 `ALL` 返回业务失败 |
| I-4 | D-6/D-7 | 4 | `WorkflowTaskVO`、`WorkflowTaskRuntimeStatus`、`WorkflowCode`、`WorkflowTaskBusinessPageQuery`、`WorkflowTaskBusinessPageRequest`、`WorkflowTaskHistoryEntity` | 编译与架构门禁通过 |
| I-5 | D-8 | 5 | `WorkflowBusinessApplyAccessChecker` | Provider 缺失时不拒绝、Provider 命中时参与收窄 |
| I-6 | AC-1~AC-9 | 6 | `mango-workflow/README.md`、`mango-docs/guides/business-integration/workflow-business-approval.md`、`mango-docs/capabilities/README.md` | 三条文档门禁通过 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| AC-1/AC-2/AC-5/AC-6/AC-7/AC-9 | H2 集成测试 | `mvn -o -pl mango-platform/mango-workflow/mango-workflow-core -Dtest=WorkflowTaskHistoryMapperIntegrationTest test` | PASS（10 用例） | 多节点返回、批量并集、`ORG` 拒绝、`endTime` 双向 NULL 归尾、租户隔离、`applyStatuses`/`returned`、候选关联 |
| AC-2/AC-3/AC-4/AC-5 | 单元测试 | `mvn -o -pl mango-platform/mango-workflow/mango-workflow-core -Dtest=WorkflowTaskBusinessQueryServiceTest test` | PASS（10 用例） | 空集合与分页成对校验、`SELF` 三段条件、`ALL` 无权限失败、截断上限、状态映射 |
| AC-1~AC-9 | 模块验证 | `mvn -o -pl mango-platform/mango-workflow/mango-workflow-api,mango-platform/mango-workflow/mango-workflow-core,mango-platform/mango-workflow/mango-workflow-starter verify` | PASS | workflow-core 128 tests、workflow-starter 45 tests、`BUILD SUCCESS` |
| AC-1~AC-9 | 架构与质量门禁 | `mvn -o -f mango/pom.xml -pl ":mango-architecture-verification,..." -Dmango.architecture.mode=changed -Dmango.check.gate=no-new-violations verify` | PASS | `newIssues=[]`，`BUILD SUCCESS` |
| AC-10 | 真实 MySQL 联调 | 在 `mango_dev_baohan_risk` 执行等价 SQL（含预编译 `limit ? offset ?`） | PASS | 单键 3 行多节点、分页 2+1、`endTime` 双向 NULL 归尾、SELF 候选组 `ROLE:`/`ORG:` 命中、ALL 94 行、assigneeIds 81 行、`RETURN` 8 条；`DONE` 51 行与 `COMPLETE` 动作 51 条一致 |
| AC-1~AC-9 | 能力文档门禁 | `node mango-pmo/tools/audit-module-readmes.mjs`、`audit-readme-source-facts.mjs`、`check-business-guides.mjs`、`check-capability-docs.mjs`、`risk-verification.mjs` | PASS | 脚本输出 `checks passed` |

## 7. 例外与剩余风险

- 已在真实 MySQL（`mango_dev_baohan_risk`，Flowable 7.0.0）完成只读联调，并因此发现并修复 `CREATE_TIME_` → `START_TIME_` 的列名缺陷（原 H2 夹具列名错误，单测无法暴露）。测试库以外环境仍需在上线前验证一次。
- 真实库中 54 条申请的 `org_id` 与 `applicant_dept_id` 全部为空，`orgId` 恒为 `null`；业务已确认不使用该字段显示部门，`ORG` 范围本期未实现。
- 引擎 schema 耦合：只读 SQL 依赖 `ACT_HI_TASKINST`、`ACT_HI_IDENTITYLINK`、`ACT_RU_IDENTITYLINK` 列名；Flowable 升级必须回归 `WorkflowTaskHistoryMapperIntegrationTest`。
- 契约偏差：冻结版约定的 `truncated` 字段改为 `list.size() < total` 判断（平台分页返回不允许扩字段）；参数错误码由 `3641` 改为 `3659`，越权为 `3660`（`3641` 已被 `APPLY_NOT_FOUND` 占用）。
- 未实现：`ORG` / `deptIds` 组织范围直接报错；未接入角色数据范围配置，业务侧需按 `scope` 显式声明范围。
- 未提交授权项：PR 合并、Review、发布均未执行，需单独授权。
- `cancelCause=OTHER`（驳回）与 `TERMINATED` 在真实库零样本（该库仅有 APPROVED/IN_APPROVAL/WITHDRAWN），仅由单元测试覆盖；建议业务在联调环境用驳回场景实测一次。
- 性能只在 54 申请 / 94 任务上验证：`workflow_business_apply` 在该规模走全表扫描（强制索引验证 `idx_workflow_business_apply_biz_latest` 为覆盖索引可用），`ACT_HI_TASKINST` 走 `ACT_IDX_HI_TASK_INST_PROCINST`，`workflow_task_record` 相关子查询走 `idx_workflow_task_record_proc`；`lastReturnVariables` 仍有 filesort，但排序集合被单个流程实例的动作数限制（实测 4 行），与表规模无关。上线前需在生产规模数据上复核执行计划。
