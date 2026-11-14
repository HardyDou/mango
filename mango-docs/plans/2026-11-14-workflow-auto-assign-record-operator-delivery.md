# 标准交付记录

## 1. 元数据

- 任务 ID：MANGO-1269
- 任务名称：自动派单流转记录归因到真实承办人
- 交付模式：STANDARD
- 需求影响：L2 - 改变 `workflow_task_record` 中 `AUTO_ASSIGN` 记录的办理人归因，影响所有使用 `assignmentMode=AUTO`（轮询、任务量最少、亲和、节点亲和）或 `assigneeType=SPECIFIED_USER` 自动派单节点的审批流转记录展示和按办理人过滤；不改变任务派给谁、接口结构、数据库结构和权限码。
- 方案风险：L2 - 修改共享运行时记录写入方法 `WorkflowTaskRuntimeService.saveRecord`，该方法被发起、认领、通过、驳回、退回、转办、加签等动作共同使用；写错会同时污染审核记录和 `COMPLETED_HANDLER` 参与关系，失败需回滚版本并排查已写记录。
- 最终风险：L2
- 工作区决策：CREATE - 从 `origin/main` 新建任务 worktree `/data/Mango/worktrees/mango-1269-auto-assign-record` 和分支 `fix/1269-auto-assign-record-operator`；用项目内 `@mango/cli` 执行 `workspace init`，分配 slot 7（18007/30007/`mango_dev_mango_1269_auto_assign_record_007`）。
- 要求来源：GitHub Issue #1269（P2，`bug`）。

## 2. 目标与范围

- 目标：自动派单成功时，`AUTO_ASSIGN` 流转记录的操作人是本次派单的真实承办人，而不是触发节点进入的发起人或上一步办理人。
- 成功条件：节点通过自动派单派出给用户 B、触发人是用户 A 时，流转记录返回 `actionName=自动派单`、`operatorId/operatorName` 为 B；审批时间线显示「初审 · 自动派单 · B」。
- 处理范围：`WorkflowTaskRuntimeService` 的记录写入（`saveRecord` 与 `autoAssign`）、模块 README 的自动派单审计和流转记录说明、`WorkflowTaskRuntimeServiceIntegrationTest` 回归用例。
- 不处理范围：流转记录 VO 新增承办人字段；`actionName` 文案改为「系统自动派单」；业务项目 `mango-baohan-risk` 自身的待办展示逻辑。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| DSR-001 | 自动派单节点写入的 `AUTO_ASSIGN` 记录 | 用户 B 曾在同亲和键的历史实例办理过该节点，用户 A 重新提交触发节点进入 | 记录 `operator_id`/`operator_name` 为 B，`variables_json.assigneeUserId` 同为 B 的 ID | 记录不写或写入 A 时视为失败 | 数据库行 `operator_id=B`、`operator_name=B` 登录名 |
| DSR-002 | 流转记录读取接口（`traceByBusinessKey` 的 VO 映射） | 上述记录存在 | 返回的 `operatorId`/`operatorName` 为 B，`actionName` 仍为「自动派单」 | 返回 A 时视为失败 | `WorkflowTaskRecordConverter.toVo` 输出 B 与「自动派单」 |
| DSR-003 | 认领、通过、退回、转办等其它动作 | 当前登录用户为 A | 记录操作人仍为 A，参与关系仍按 A 记录 `COMPLETED_HANDLER` | 归因漂移到承办人或系统时视为失败 | 既有集成用例全量通过 |
| DSR-004 | 自动派单候选为空且 `nodeAffinityFallback=CLAIM` | 候选集为空 | 不写 `AUTO_ASSIGN` 记录，只保留待领取候选组 | 写入空承办人记录视为失败 | 既有集成本例通过 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| ADR-001 | DSR-001、DSR-003 | 只给 `AUTO_ASSIGN` 显式传入操作人；`saveRecord` 保留原签名委托到带操作人参数的重载，其它动作继续使用当前登录用户，保证 19 处调用点行为不变 | `WorkflowTaskRuntimeService.saveRecord`、`autoAssign` | 回退该提交，恢复单签名方法 |
| ADR-002 | DSR-001、DSR-002 | `operator_name` 继续写登录名（与其它动作一致），通过 `AuthUserProvider.getByIdForAuth` 从用户 ID 解析，查不到时回退用户 ID 字符串，避免写 null 或空名称 | `WorkflowTaskRuntimeService.assigneeIdentifier` | 同上 |
| ADR-003 | DSR-001 | 被拒绝方案：给 `WorkflowTaskRecordVO` 新增 `assigneeUserId/assigneeName` 并保持 `operatorId=发起人`。放弃原因：当前仓内无消费者，业务页面仍会显示发起人，需业务项目同步升级改造才能修正展示，等于把缺陷转移给业务方 | 流转记录 VO、前端时间线 | 未实施 |
| ADR-004 | DSR-001 | 被拒绝方案：`operator` 写成「系统」。放弃原因：时间线读不出承办人，仍要靠查 `variables_json` 或 `ACT_RU_TASK`，与 Issue 的排障诉求相反 | `WorkflowTaskAction`、前端时间线 | 未实施 |
| ADR-005 | 兼容性 | 无需回填历史 `AUTO_ASSIGN` 记录：自动派单与本次归因修正未投产，仓内只有开发与测试数据，不存在需要纠正的线上审计记录 | `workflow_task_record` | 无需回滚 |
| ADR-006 | 兼容性 | 自动派单亲和选择只读 `CLAIM/COMPLETE/REJECT/RETURN` 的 `operator_id`，不含 `AUTO_ASSIGN`，本次改动不影响后续亲和派单结果；`CURRENT_ASSIGNEE` 参与关系由 `recordCurrentAssignee` 单独写入，同样不受影响 | `WorkflowAutoAssignmentSelector`、`recordCurrentAssignee` | 同上 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| IMP-001 | ADR-001 | 1 | `mango-workflow-core/.../service/impl/WorkflowTaskRuntimeService.java` | 新增带 `operatorId/operatorName` 的 `saveRecord` 重载，原签名委托；`autoAssign` 传入选中承办人 |
| IMP-002 | ADR-002 | 2 | 同上 | `assigneeIdentifier` 用 `AuthUserProvider.getByIdForAuth` 解析登录名并兜底 |
| IMP-003 | DSR-001~DSR-003 | 3 | `mango-workflow-core/src/test/.../WorkflowTaskRuntimeServiceIntegrationTest.java` | 新增真实 Flowable + H2 用例：承办人 1002、触发人 1001，断言行与 VO 归因 |
| IMP-004 | DSR-001 | 4 | `mango-platform/mango-workflow/README.md` | 自动派单审计与流转记录小节写明 `operator_id/operator_name` 语义与历史不回填 |

## 6. 验收映射与结果

环境：JDK 21.0.13、Maven 3.9.12、离线本地仓库、H2（集成测试）；模块 `mango-workflow-core`，worktree `/data/Mango/worktrees/mango-1269-auto-assign-record`（slot 7）。

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| DSR-001~DSR-003 | M11 集成测试（真实 Flowable + H2） | `mvn -f mango/pom.xml -o -pl :mango-workflow-core -Drevision=mango-007 -Dtest=WorkflowTaskRuntimeServiceIntegrationTest -DfailIfNoTests=false org.apache.maven.plugins:maven-surefire-plugin:3.2.5:test` | PASS，15 项通过（含新增 `autoAssignRecordAttributesRealAssigneeInsteadOfTriggeringUser`） | `mango-workflow-core/target/surefire-reports/TEST-io.mango.workflow.core.service.impl.WorkflowTaskRuntimeServiceIntegrationTest.xml` |
| DSR-001 反证 | M11 回归反证 | 暂存主代码改动后仅运行新增用例 | FAIL（`expected: 1002L but was: 1001L`），证明用例能捕获本缺陷 | 同一 surefire 报告路径；命令输出记录在会话 |
| 模块静态验证 | M09 编译与模块构建 | `mvn -f mango/pom.xml -o -pl :mango-workflow-core -am -DskipTests -Drevision=mango-007 install` | PASS | 构建日志 `BUILD SUCCESS` |
| 能力说明 | M08 README 审计 | `node mango-pmo/tools/audit-module-readmes.mjs`、`node mango-pmo/tools/audit-readme-source-facts.mjs`、`node mango-pmo/tools/check-business-guides.mjs` | 见第 7 节记录 | 命令输出 |

未启用措施与原因：

- M12：`traceByBusinessKey` 入参和返回结构未变，变化只体现在既有 `operatorId/operatorName` 的取值来源；已用 `WorkflowTaskRecordConverter` 的映射断言覆盖接口返回字段，未新增 HTTP 层断言。
- M13：前端时间线组件按 `operatorName` 原样展示，无布局、交互或字段变化。
- M16：无需要人工确认的视觉或现场结果。

## 7. 例外与剩余风险

- 历史数据：无需处理。自动派单与本次归因修正均未投产，只需从本版本开始生效。
- 名称解析成本：自动派单每次写记录增加一次 `AuthUserProvider.getByIdForAuth` 调用，与既有 `recordCurrentAssignee`/`resolveUserId` 的解析方式一致。
- 归因语义边界：`AUTO_ASSIGN` 不再记录「谁触发了节点进入」。该事实由同一实例中相邻的 `START`/`COMPLETE`/`RETURN` 记录表达，本次未新增字段承载。
- 消费方影响：按办理人过滤流转记录的调用方会同时命中该办理人的「自动派单」记录，属于本次预期变化；已在模块 README 流转记录小节写明。
- 构建观察（非本次引入）：`mango-workflow-core` 走 Maven 默认生命周期绑定 `maven-surefire-plugin:2.17`，直接执行 `mvn test` 会显示 `Tests run: 0`；本次用显式插件目标 `maven-surefire-plugin:3.2.5:test` 执行测试，未修改构建配置。

## 8. 决策复核记录

- 事实与用户视角：Issue #1269 给出同一业务键的 `workflow_task_record` 原始行与流转记录接口返回，诉求是「流转记录能看出派给了谁」；用户补充的「待办能看到但处理时提示不是自己的任务」与平台侧事实不符——平台待办查询（`WorkflowTaskHistoryMapper.xml` 的 `selfScope` 按 `ASSIGNEE_` 或候选身份链接匹配）和办理校验（`ensureCurrentUserCanOperate` 按 `assignee`/候选人/候选组判定）都使用真实承办人，因此误判来源是业务页面按流转记录渲染的办理人。
- 技术与风险视角：根因是 `autoAssign` 走通用 `saveRecord`，操作人取自登录上下文；亲和选择 SQL 不读 `AUTO_ASSIGN` 的 `operator_id`，`CURRENT_ASSIGNEE` 参与关系由另一路径写入，因此只改操作人归因不会改变派单结果和待办分布。主要风险是共享方法被其它动作复用，用重载隔离影响面。
- 验证与交付视角：新增真实 Flowable + H2 集成用例，并做了「暂存主代码后用例失败」的反证，证明用例可捕获缺陷；文档同步模块 README 的审计与流转记录语义。
- 评审选择：`AGENT_ONLY`（任务下发时未要求外部同行评审，按规则记录并继续）。该记录不代表人工评审或正式审批。
- 提交授权：尚未获得 Commit、Push、PR 授权；本次仅完成工作区内的实现与验证。
