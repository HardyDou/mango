# 标准交付记录

任务：Issue #725 工作流业务审批撤回公开接口

## 1. 元数据

- 任务 ID：GitHub Issue #725
- 交付模式：STANDARD
- 需求影响：L2 - 新增所有业务审批消费者可调用的公开撤回契约，改变运行中流程实例、当前任务、业务申请终态、权限和领域事件语义，但影响范围限定在 Mango Workflow 能力。
- 方案风险：L2 - 方案横跨 workflow API、core、starter 与 starter-remote，并在同一事务内协作 Flowable、业务申请持久化和事件发布；不新增表或迁移，失败可通过回退代码与发布版本恢复。
- 最终风险：L2
- 工作区决策：CREATE - `fix/issue-725-workflow-withdraw`

## 2. 目标与范围

- 目标：为运行中的业务审批提供公开、幂等、可审计的撤回能力，避免撤回后遗留的旧流程任务继续出现在待领取或待办结果中。
- 成功条件：业务可按申请 ID 或流程实例 ID 撤回；当前租户和申请人校验生效；Flowable 运行实例与当前任务终止；业务申请进入 `WITHDRAWN`；操作人、原因和动作可追溯；发布 `workflow.process.withdrawn` 与 `workflow.process.ended`；重复撤回幂等成功；其它终态返回明确失败；本地与远程契约一致。
- 处理范围：`mango-workflow-api` 公开命令、结果、状态和事件契约；`mango-workflow-core` 撤回编排、并发幂等、状态流水与事件；starter Controller、权限资源、starter-remote Feign；充分单元测试、定向集成/API 契约测试；Workflow README、业务接入指南和能力地图。
- 不处理范围：业务项目自身订单/审核轮次状态机、历史脏数据清理、前端撤回按钮、Baohan 业务代码升级、Maven 发布。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| SR-001 | 业务申请人 / `WorkflowProcessApi.withdraw` | 同租户、本人发起、申请为 `IN_APPROVAL`，传 `applyId` 或 `processInstanceId` 和原因 | 终止 Flowable 实例，使运行任务失效，将申请与表单状态改为撤回并返回撤回结果 | 任一步失败时事务回滚，不返回伪成功 | 运行实例不存在、当前任务为空、申请为 `WITHDRAWN`，结果含申请/实例/状态/原因 |
| SR-002 | 同一业务调用方 | 对已经成功撤回的同一申请重复调用 | 返回幂等成功，不重复删除实例、不重复写状态流水或发布事件 | 不因 Flowable 实例已删除而报不存在 | 结果标记 `idempotent=true`，副作用只发生一次 |
| SR-003 | 非申请人或跨租户调用方 | 标识指向他人或其它租户申请 | 拒绝撤回，不暴露或修改目标流程 | `PROCESS_WITHDRAW_FORBIDDEN` 或租户隔离后的不存在 | 单测与持久化测试证明无 Flowable、状态和事件副作用 |
| SR-004 | 业务调用方 | 申请已 `APPROVED`、`REJECTED`、`CANCELED` 或 `TERMINATED` | 返回包含当前终态的明确业务失败 | `PROCESS_WITHDRAW_NOT_ALLOWED`，消息包含当前状态 | 参数化单测覆盖全部非撤回终态 |
| SR-005 | 事件订阅方 | 撤回事务成功 | 先发布 `workflow.process.withdrawn`，再发布通用 `workflow.process.ended`；载荷含租户、操作人、申请人、业务键、撤回状态和原因 | 事务失败时不形成已提交事件 | 事件单测验证类型、顺序、payload 与业务路由字段 |
| SR-006 | 单体与微服务消费者 | 使用 starter 或 starter-remote 调用撤回 | Java 方法、HTTP verb/path/body/result 完全一致，并受 `workflow:process:withdraw` 保护 | 适配器签名或 binding 不一致时构建失败 | Controller/Feign 契约、架构门禁与消费者编译通过 |
| SR-007 | Workflow 维护者 | 执行定向测试 | 撤回规则、标识解析、权限、状态、幂等、事件和失败副作用有充分自动化覆盖 | 只测透传或调用次数不计完成证据 | core 新增不少于 12 个有行为断言的撤回单元场景，并补真实 Mapper 状态流水集成验证 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| TD-001 | SR-001、SR-002、SR-004 | 新增 `WithdrawWorkflowProcessCommand` 与 `WorkflowProcessWithdrawResultVO`；允许 `applyId`、`processInstanceId` 任一或同时提供，同时提供时必须指向同一申请 | workflow API | 删除新增类型和方法，不影响既有接口 |
| TD-002 | SR-001~SR-004 | 申请记录在事务内按标识加行锁；只允许 `IN_APPROVAL` 首次撤回，`WITHDRAWN` 返回幂等成功，其它状态用稳定业务码拒绝 | business apply/process service | 回退撤回服务方法，无数据库迁移 |
| TD-003 | SR-001、SR-003 | MyBatis 租户过滤与显式上下文前置条件共同保证租户边界；核心服务要求当前用户为原申请人，HTTP 入口另要求 `workflow:process:withdraw`，业务模块仍负责自身单据状态与授权 | core、starter、权限资源 | 删除权限码和入口；既有权限不变 |
| TD-004 | SR-001、SR-005 | Flowable 删除运行实例后，同事务更新表单为 `WITHDRAWN`、清空申请当前任务、写 `WorkflowApplyAction.WITHDRAW` 状态流水，再发布撤回和结束事件 | core service/event | 回退专用分支；不复用驳回语义 |
| TD-005 | SR-006 | Controller 使用 `POST /workflow/processes/withdraw` + JSON body；Feign 重声明相同方法；Bootstrap 延迟代理通过既有 `WorkflowProcessApi` 自动获得新能力 | starter、starter-remote | 删除同名适配方法 |
| TD-006 | SR-007 | 单元测试以真实 `WorkflowProcessService` 为被测对象，仅替换 Flowable、Mapper/业务服务和事件发布等协作者；另用 H2 + 真实 Mapper 验证状态、流水和任务清理 | core tests | 测试随实现回退，不保留无效快照测试 |
| TD-007 | SR-001~SR-007 | 同步 Workflow README、业务审批接入指南、能力地图和本记录；不修改前端公开包，因为本 Issue 没有 UI 行为 | docs | 回退对应能力说明 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---:|---|---|
| IM-001 | TD-001 | 1 | `mango-workflow-api` command/VO/API/event/status/error code | 公开契约可编译且校验语义明确 |
| IM-002 | TD-002~TD-004 | 2 | business apply 与 process core service、事件发布器 | 并发幂等、状态和副作用顺序完整 |
| IM-003 | TD-005 | 3 | starter Controller、starter-remote Feign、菜单/API 权限资源 | 本地与远程 HTTP 合同一致 |
| IM-004 | TD-006 | 4 | core 单元测试、H2 Mapper 集成测试、事件和 API 契约测试 | SR-001~SR-007 自动化覆盖通过 |
| IM-005 | TD-007 | 5 | Workflow README、业务接入指南、能力地图、本记录 | 消费者能找到调用、权限、终态和事件说明 |
| IM-006 | TD-001~TD-007 | 6 | 定向 Maven verify、测试质量/Mock 审计、README 与 diff 门禁 | 所有启用验证结果回填 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| SR-001~SR-005、SR-007 | M10 单元测试 | `mvn -f mango/pom.xml -pl :mango-workflow-core -Drevision=1.0.0-mango-006-SNAPSHOT -Dtest=WorkflowProcessWithdrawalTest,WorkflowBusinessApplyServiceImplIntegrationTest,WorkflowEventPublisherTest test` | PASS | 26 条通过、0 失败、0 跳过；其中撤回编排 17 条，覆盖参数、上下文、申请人、标识一致性、6 种不可撤回状态、幂等、实例缺失、两种标识成功路径和副作用顺序 |
| SR-001、SR-002、SR-005 | M11 集成测试 | `WorkflowBusinessApplyServiceImplIntegrationTest` 使用 H2 MySQL mode、真实 Service/Mapper/事务管理 | PASS | 真实查询和更新验证 `IN_APPROVAL -> WITHDRAWN`、当前任务删除、摘要清空、`WITHDRAW` 流水、租户、操作人、原因和实例 ID；该类 4 条全部通过 |
| SR-006 | M12 API/适配器验证 | starter Controller/API surface 契约测试；starter-remote Feign 契约测试；API 与 remote `verify` | PASS | starter 23 条通过；remote 2 条通过；Java API/Controller 指纹、`POST /withdraw`、`@RequestBody`、返回泛型、Feign 根路径和 `workflow:process:withdraw` 权限一致 |
| SR-001~SR-007 | M09 静态验证 | 四个直接修改模块 `verify`；标准交付记录检查；`test-quality-check.mjs`；`audit-backend-test-mocks.mjs`；两项 README 审计；`git diff --check` | PASS | API、core、starter、remote 均 BUILD SUCCESS；标准交付记录 PASS；core 68 条通过、4 条环境条件跳过；测试质量 8 文件 PASS；Mock 审计 block=0/warn=0；README source facts 全部 OK；diff clean |

## 7. 例外与剩余风险

- 本任务不处理 Issue 中已经产生的历史旧流程和旧任务；只保证能力发布后新发生的撤回走完整公共链路。
- 本任务不发布 Maven 版本，也不修改 Baohan 业务仓；业务回归仍依赖后续独立发布与消费项目升级。
- 业务单据能否撤回、撤回后回到何种业务状态，仍由业务模块在调用前和订阅事件时按自身状态机判断；Workflow 只负责其申请和运行实例终态。
- core 全量 `verify` 中 4 条既有 `WorkflowMigrationUpgradeIntegrationTest` 由 `MANGO_DB_NAME=mango_dev_*` 环境条件控制，本次未连接专用 MySQL，因此按设计跳过；本改动不包含 migration，撤回持久化链路已由隔离 H2 + 真实 Mapper 覆盖。
