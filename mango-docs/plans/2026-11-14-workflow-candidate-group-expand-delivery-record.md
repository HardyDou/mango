# 标准交付记录

## 1. 元数据

- 任务 ID：MANGO-1263
- 任务名称：角色/岗位/组织审批人按多人审批方式展开为每人一条任务
- 交付模式：STANDARD
- 需求影响：L2 - 改变工作流审批节点生成任务的方式，影响待办/待领取分布、通知接收人、加签与历史记录，但边界限制在流程引擎任务模型，不涉及数据库结构和对外接口签名。
- 方案风险：L2 - 改动共享引擎的 BPMN 转换与运行时候选解析，涉及多实例、空候选兜底和角色成员解析；失败可通过回退版本和重新发布流程定义恢复。
- 最终风险：L2
- 工作区决策：CREATE - 从 `origin/main` 新建任务 worktree `/data/Mango/mango-1263` 和分支 `feat/workflow-group-expand-1263`；本机未安装 `mango` CLI，`.mango/workspace.json` 按 slot 2（18002/30002/`mango_dev_mango_002`）手工生成并保持与主工作区隔离。
- 要求来源：GitHub Issue #1263（P2，作者 HardyDou）。

## 2. 目标与范围

- 目标：审批节点无论用指定成员还是角色、岗位、组织作为审批人来源，都按节点配置的多人审批方式（或签/会签/依次）为候选成员各生成一条运行时任务。
- 成功条件：角色 3 人 + 或签发起后产生 3 条各自绑定的任务；一人办理后节点通过，其余任务 `MI_END` 自动取消；成员变化对下一次提交生效，不需要重新发布定义；成员为空时按节点“审批人为空”策略处理，不静默通过。
- 处理范围：`WorkflowDesignerBpmnConverter` 的候选组分支、`WorkflowAssigneeCollection` 的运行时集合表达式、`WorkflowCandidateGroupProvider` 的候选组展开、`WorkflowTaskRuntimeService` 的候选组补齐与去重、模块 README、能力地图、单元与集成测试。
- 不处理范围：前端设计器交互与文案、`assignmentMode=AUTO` 派单语义、`FORM_USER` 的表单字段来源类型解析、组织负责人按发起人组织解析的历史缺陷（见第 7 节）、并发抢单与任务上限策略。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| DSR-001 | 审批节点（`SPECIFIED_ROLE` 3 人 + `OR_SIGN`） | 流程发起，角色含 3 名启用成员 | 生成 3 条任务，`assignee` 分别为 3 名成员；任务不再带候选组 | 角色无成员时不生成空多实例 | 任务数 3、办理人正确、候选组链接为空 |
| DSR-002 | 同一节点的待办入口 | 分别以各成员身份查询 | 每个成员命中自己那一条任务，任务均为已派单状态 | 未认领任务数为 0 | `taskAssignee` 命中 1，`taskUnassigned` 命中 0 |
| DSR-003 | 任一成员办理 | 其中一名成员完成审批 | 节点通过、流程结束；其余任务以 `MI_END` 取消 | 其他人不能再办理已取消任务 | 节点剩余任务 0，历史任务 3 条，其中 2 条 `MI_END` |
| DSR-004 | 同一节点的加签入口 | 多实例子任务上执行加签 | 追加一条任务并绑定目标办理人 | 非多实例节点执行加签时报错 | 任务数 4，包含新增办理人 |
| DSR-005 | 审批节点（角色无成员 + `OR_SIGN`） | 角色成员为空 | 按默认 `emptyAssigneeStrategy=TO_ADMIN` 派出 1 条任务给管理员 | 不得生成 0 条任务让节点静默通过 | 任务数 1，办理人为 `admin` |
| DSR-006 | `SPECIFIED_POST`、`SPECIFIED_ORG`、`ORG_LEADER`（指定组织）节点 | 设计器配置对应来源与多人审批方式 | 生成多实例集合表达式，分别带 `POST:`、`ORG:`、`ORG_LEADER:` 前缀 | 前缀无法识别时不发起无意义查询 | 单测断言表达式与完成条件 |
| DSR-007 | `assignmentMode=AUTO` 角色节点 | 自动派单配置 | 保持单任务派单，不展开 | 候选为空时按既有策略失败或回退待领取 | 单测断言不生成多实例 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| ADR-001 | DSR-001~DSR-006 | 候选组在**节点进入时**展开，不在定义发布时固化成员，保证成员变化对下一次提交生效 | `WorkflowDesignerBpmnConverter`、`WorkflowAssigneeCollection` | 回退该提交并重新发布定义 |
| ADR-002 | DSR-001~DSR-006 | 复用既有运行时集合表达式通道（`${mangoWorkflowAssigneeCollection.*}`），不新增 Flowable 监听器或自定义行为 | 多实例 `inputDataItem` | 回退到 `candidateGroups` 单任务 |
| ADR-003 | DSR-005 | 展开结果为空时返回 `EMPTY_ASSIGNEE` 哨兵，交回既有“审批人为空”策略，禁止空集合 | `WorkflowAssigneeCollection.groupMembers` | 回退文案表达式 |
| ADR-004 | DSR-006 | 候选组到成员的唯一解析实现放在 `WorkflowCandidateGroupProvider.memberUserIds`，删除 `WorkflowTaskRuntimeService` 内的重复 SQL；角色、岗位、组织、组织负责人分别按既有租户与启用条件过滤 | `WorkflowCandidateGroupProvider`、`WorkflowTaskRuntimeService` | 恢复原私有方法 |
| ADR-005 | DSR-002 | 已有具体办理人的任务不再补候选组，避免多实例子任务被其他成员认领 | `WorkflowTaskRuntimeService.resolveRuntimeTask` | 移除守卫 |
| ADR-006 | 兼容性 | 不新增 `orSignExpandCandidates` 开关：BPMN 在定义发布时生成，已发布定义不受影响，重新发布后才按新规则生成；开关会长期保留两套任务模型 | 模块 README、升级与兼容表 | 回退版本并保留旧定义 |
| ADR-007 | DSR-003、DSR-004 | 会签默认值下角色节点从“一人处理”变为“全体成员通过”，属于语义对齐而非新增配置；在升级说明中显式登记行为变化与处理建议 | README 升级与兼容 | 节点改选 `OR_SIGN` 或 `AUTO` |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| IMP-001 | ADR-001、ADR-002 | 1 | `mango-workflow-core/.../engine/WorkflowDesignerBpmnConverter.java` | 候选组分支生成多实例，完成条件按 `approvalMode` 复用既有逻辑 |
| IMP-002 | ADR-001、ADR-003 | 2 | `mango-workflow-core/.../engine/WorkflowAssigneeCollection.java` | 新增 `groupMembers`，空集合返回哨兵 |
| IMP-003 | ADR-004 | 3 | `mango-workflow-core/.../engine/WorkflowCandidateGroupProvider.java`、`.../service/impl/WorkflowTaskRuntimeService.java` | 组展开只有一份实现，自动派单复用 |
| IMP-004 | ADR-005 | 4 | `WorkflowTaskRuntimeService.resolveRuntimeTask` | 已有办理人的任务不补候选组 |
| IMP-005 | DSR-001~DSR-007 | 5 | `WorkflowDesignerBpmnConverterTest`、`WorkflowAssigneeCollectionTest`、`WorkflowCandidateGroupProviderTest` | 转换规则与展开规则单测覆盖 |
| IMP-006 | DSR-001~DSR-005 | 6 | `WorkflowTaskRuntimeServiceIntegrationTest` | 真实 Flowable + H2 覆盖展开、办理取消、空候选、加签 |
| IMP-007 | ADR-006、ADR-007 | 7 | `mango-platform/mango-workflow/README.md`、`mango-docs/capabilities/README.md` | 说明展开规则、通知变化和升级动作 |

## 6. 验收映射与结果

环境：JDK 21.0.13、Maven 3.9.12、离线本地仓库、H2（集成测试）；模块 `mango-workflow-core`，worktree `/data/Mango/mango-1263`。

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| DSR-001~DSR-005 | M11 集成测试（真实 Flowable + H2） | `mvn -o -pl :mango-workflow-core -am test-compile org.apache.maven.plugins:maven-surefire-plugin:3.2.5:test -Dtest=WorkflowTaskRuntimeServiceIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false` | PASS，14 项通过 | `mango-workflow-core/target/surefire-reports/io.mango.workflow.core.service.impl.WorkflowTaskRuntimeServiceIntegrationTest.txt` |
| DSR-006、DSR-007 | M10 单元测试 | 同上命令，`-Dtest=WorkflowDesignerBpmnConverterTest,WorkflowAssigneeCollectionTest,WorkflowCandidateGroupProviderTest` | PASS，11 + 5 + 4 项通过 | `mango-workflow-core/target/surefire-reports/` |
| 全模块回归 | M10 模块测试 | `mvn -o -pl :mango-workflow-core -am test-compile org.apache.maven.plugins:maven-surefire-plugin:3.2.5:test` | PASS，223 项通过、4 项跳过（存量跳过） | `/tmp/wf-all2.log` |
| 依赖模块回归 | M10 模块测试 | `mvn -o -pl :mango-workflow-starter -am test-compile org.apache.maven.plugins:maven-surefire-plugin:3.2.5:test` | PASS，56 项通过 | `/tmp/wf-starter.log` |
| 静态验证 | M09 模块构建与代码规则 | `mvn -o -pl :mango-workflow-core -am verify -DskipTests` | PASS | `/tmp/wf-verify.log` |
| 静态验证 | M09 Checkstyle / PMD | `mvn -o -pl :mango-workflow-core org.apache.maven.plugins:maven-checkstyle-plugin:3.5.0:check org.apache.maven.plugins:maven-pmd-plugin:3.28.0:check` | PASS，Checkstyle 0 违规；PMD 仅有存量 `MagicNumber` 类警告 | `/tmp/wf-style.log` |
| 能力说明 | M08 文档审计 | `node mango-pmo/tools/audit-module-readmes.mjs`、`node mango-pmo/tools/audit-readme-source-facts.mjs` | PASS，workflow README 均为 OK | 两条审计脚本输出 |
| DSR-001~DSR-005（本地真实场景） | 真实后端 + MySQL + HTTP | `mango dev start backend`（端口 18002、库 `mango_dev_mango_002`）后执行 `mango-docs/evidence/issue-1263/run-local-scenario.sh` | PASS：3 条任务分别派给 3 名角色成员、候选组链接 0；每名成员待办 1／待领取 0、summary `pendingApproval=1`、`pendingHandle=0`；一人办理后 `ended=true`、`applyStatus=APPROVED`、剩余任务 0，其余历史任务 `MI_END` | `mango-docs/evidence/issue-1263/local-scenario.json` |

未启用措施与原因：

- M12：公开 API 的入参和返回结构未变化，本次变化体现在运行时任务集合；已在集成测试中按 `assignee`/`unassigned` 和任务数证明待办与待领取的判定输入，未新增 HTTP 层断言。
- M13：前端设计器已有“多人审批”选项且未改交互与文案，无浏览器可见变化。
- M16：无需要人工确认的视觉或现场结果。

## 7. 例外与剩余风险

- 行为变化：角色/岗位/组织节点重新发布后不再生成待领取共享任务，会签默认值下要求全体成员通过；要维持“一人处理”应改选或签或改为自动派单。已在模块 README「升级与兼容」登记。
- 任务量：展开后每个节点运行时任务数等于候选成员数，组织类节点成员较多时 `ACT_RU_TASK` 增长明显；本次未加入静默上限。
- 成员解析成本：展开时按成员逐个调用 `AuthUserProvider.getByIdForAuth` 换取登录名，成员数量大时是 N 次查询；与既有自动派单候选解析模式一致。
- 历史缺陷（非本次引入）：`ORG_LEADER` 默认 `orgLeaderUseInitiatorOrg=true` 时，定义发布阶段用 `${mangoInitiator}` 查组织必然为空，随后回落到管理员；本次未修改该路径，建议单独登记 Issue 跟踪。
- `FORM_USER` 的表单字段来源类型为角色/组织时，多实例集合仍使用原始值而非展开成员；同样属于独立缺陷，本次未处理。
- 质量门禁观察：当前 Maven 配置下 `mango-workflow-core` 未绑定 PMD/Checkstyle/SpotBugs 执行（仅 `mango-common` 侧绑定），因此 `mvn verify` 不覆盖该模块这些规则；本次用显式插件目标补充执行，未修改构建配置。

### 7.1 业务项目兼容性检查（`/data/Yunxin/baohan/baohan-system`，只读）

- 版本事实：该业务仓 `baohan-backend/pom.xml` 锁定 `mango.version=1.1.5`，不包含本次变更；升级 Mango 才会受影响。
- 升级不自动生效：`WorkflowDefinitionService.ensurePublished` 用 `isEnsuredDefinitionCurrent` 比对 `designerJson` 等字段，成员解析规则变化不会改变该判等；只要业务仓的 designer JSON 不变，已发布 BPMN 不会被重新生成。
- 重新发布后受影响的节点（`ProjectWorkflowDefinitionPublisher.roleClaimConfig`，共 3 处）：`risk_specialist_review`、`risk_supervisor_review`、`compliance_review`。这些节点为 `assigneeType=SPECIFIED_ROLE`、`approvalMode=COUNTERSIGN`、`emptyAssigneeStrategy=AUTO_REJECT`，并启用了 `claim` 动作。
- 不兼容点 1（语义）：会签由“一人领取办结”变为“角色成员全部通过”；角色内有人长期不处理时节点会卡住。
- 不兼容点 2（待领取池）：`GuaranteeInquiryRiskWorkflowCoordinator` 以 `assigneeDisplayName == "待领取"` 判定待领取并自动领取上一轮风控专员的下一轮任务；`GuaranteeInquiryRiskProjectionService.applyPendingClaimSnapshotGuard` 同样以“待领取”为快照守卫条件。展开后不再存在待领取任务，`INITIAL_PENDING_CLAIM` 不再出现，自动领取会因任务已有办理人而失败并记录告警（不抛业务异常），项目快照显示首位成员。
- 不兼容点 3（交互）：节点启用 `claim` 动作，展开后没有未认领任务，领取入口失效。
- 不受影响的配置：`managerFormUserConfig`、`generalManagerFormUserConfig`、`businessManagerFormUserConfig`（`FORM_USER` + `OR_SIGN`，多值本来就是多实例）；`FeeOrderDiscountWorkflowDefinitionRegistrar` 同样为 `FORM_USER`。
- 处理方式（升级该仓时必须做，二选一）：
  1. 语义为“角色内任一人处理”：`roleClaimConfig` 的 `approvalMode` 改为 `OR_SIGN`；把“待领取”判定改为按任务是否已派单，自动领取改为 `transfer` 转办，或改用 `assignmentMode=AUTO` + `autoAssignmentStrategy=AFFINITY/NODE_AFFINITY` 由平台把任务派给上一轮办理人；删除 `claim` 动作入口。
  2. 语义为“全员会签”：保留 `COUNTERSIGN`，删除领取相关代码、快照守卫和 `claim` 动作，并在页面提示“会签”。
- 处理方式（希望完全保持旧行为）：升级后不要重新发布这 3 个节点；或在升级前把节点改为 `assignmentMode=AUTO`（单任务派单，不展开）。

## 8. 决策复核记录

- 事实与用户视角：Issue #1263 提供 `ACT_RU_TASK`、`ACT_RU_IDENTITYLINK`、`ACT_HI_TASKINST` 对照证据；业务诉求是角色成员动态变化时仍能拿到每人一条待办。
- 技术与风险视角：根因是转换器把候选组写成 `candidateGroups` 并跳过多人审批方式；方案复用既有运行时集合表达式，不改数据库、不改接口签名；主要风险是会签默认值带来的行为变化与空展开。
- 验证与交付视角：以真实 Flowable 集成测试证明任务展开、办理取消、加签与空候选；文档更新到模块 README 与能力地图。
- 评审选择：`AGENT_ONLY`（用户确认“一起改，处理吧”，未要求外部同行评审）。该记录不代表人工评审或正式审批。
- 提交授权：用户明确要求“提交代码 pr 合并 pr”，已授权本任务分支 Commit、Push、创建 PR 和合并该 PR；不包含 Tag、发布或部署。
