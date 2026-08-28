---
documentId: TDD-WORKFLOW-732
documentType: technical-design
pmoVersion: 1.4.2
schemaRevision: 1
riskLevel: L3
riskAssessmentEvidence: requirement=L3（公开查询、历史可见性、任务归属和跨租户边界）；solution=L3（新增投影与 migration、跨身份事实解析、Flowable 同事务更新、数据库行锁并发）；final=max(requirement,solution)=L3
status: APPROVED
action: NEXT
owner: Mango Workflow 技术负责人
approver: HardyDou
approvalEvidence: review/APPROVAL.md
upstreamDocumentId: SRS-WORKFLOW-732
upstreamDocumentHash: 5ec918d4f120e14cfb4fad84c803c4c69f9cfd967d6c068aa5abfff5553ac706
---

# 工作流历史参与人只读查询与自动派单技术设计文档

## 1. 设计输入、约束与决策

| 决策ID | 问题 | 候选方案 | 选择 | 理由 | 来源ID或路径 | 是否推断 | 影响 | 风险 | 回退条件 |
|---|---|---|---|---|---|---|---|---|---|
| DEC-001 | 历史参与资格如何查询 | 查询时拼 Flowable 历史；业务各自维护；Workflow 参与投影 | 新增 Workflow 租户级参与投影 | 能覆盖业务声明、稳定身份、索引分页和显式有效性，避免依赖 Flowable 内部表 | FR-001 至 FR-004, DR-001 | 否 | 新增表、服务和迁移 | 投影与任务事实不一致 | 投影写失败回滚原事务；升级前业务保留现有临时 Port |
| DEC-002 | 参与授权身份 | username；memberId；tenant+userId 并保存 memberId 快照 | tenant+稳定 userId 为授权键，memberId/username/displayName 为快照 | 现有任务记录已有 operatorId；username 可变，memberId 可能因离开再加入变化 | FR-003, FR-004, DR-001 | 否 | 新增统一身份解析 | 旧 username-only 数据不能自动授权 | 只回填可证明 userId，旧文本保留原审计 |
| DEC-003 | 自动派单策略 | 最少任务；亲和；无锁轮询；持久化严格轮询 | 第一版仅 ROUND_ROBIN，节点游标行锁 | Issue 未要求负载或亲和；严格轮询语义清楚且可并发证明 | FR-005, NFR-002 | 否 | 新增节点配置和游标表 | 锁竞争或候选集合变化 | 单节点短事务；候选变化时从大于上次 userId 的首项继续并环回 |
| DEC-004 | 候选用户解析位置 | 继续放运行时服务；直接扩展多模块 Provider；Workflow 目录适配器 | 抽出 WorkflowCandidateDirectory，集中现有跨表读取并 fail-closed | 当前 Workflow 已直接读取租户成员、角色、岗位和组织事实；本次先消除散落 SQL，不扩大身份模块公共面 | IR-001, FR-005, WorkflowCandidateGroupProvider | 否 | 运行时服务只编排 | 上游表结构耦合 | 用集成契约测试锁定查询；后续独立 Issue 再迁移为跨模块 Provider |
| DEC-005 | 参与关系与任务动作一致性 | 异步事件最终一致；查询时修复；同数据库事务同步投影 | 在启动、声明、认领、释放、转办、完成、驳回、退回和自动派单事务内同步投影 | 只读权限不能出现成功动作后长时间缺失或错误残留 | FR-003, FR-004, FR-006 | 否 | 扩展现有事务服务 | 漏掉动作分支 | 由统一 reconcileCurrentAssignees 和 recordCompletedHandler 收口，集成测试覆盖全部动作 |
| DEC-006 | 空候选处理 | 待领取；admin 兜底；明确失败 | AUTO 模式抛 WORKFLOW_AUTO_ASSIGN_NO_CANDIDATE 并回滚 | 满足 Issue 明确失败要求，避免错误处理人 | FR-005, NFR-004 | 否 | 改变仅 AUTO 新配置行为 | 错误阻断流程推进 | 管理员改为 CLAIM 或修复候选后重试原动作 |

## 2. 模块与依赖边界

| 模块设计ID | 模块或包 | 职责 | 改动类型 | 依赖方向 | 公开能力 | 系统需求ID | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|
| MOD-001 | mango-workflow-api | 参与查询/声明契约、VO、Query、Command、参与类型和派单模式枚举 | 扩展 | api 不依赖 core/starter | WorkflowParticipationApi | FR-001, FR-002, FR-003, FR-005 | rules/backend/03-api.md, rules/backend/05-module.md | M09、M10 |
| MOD-002 | mango-workflow-core | 参与投影服务、身份目录、严格轮询服务、实体和 migration | 扩展/重构 | core -> workflow-api、persistence、Flowable；不依赖其它模块 core | 参与事实和自动派单用例 | FR-001 至 FR-007 | rules/backend/04-db.md, rules/backend/05-module.md, rules/backend/07-persistence.md | M10、M11 |
| MOD-003 | mango-workflow-starter 与 starter-remote | REST Controller、权限声明、Feign client 和装配 | 扩展 | starter/remote -> workflow-api/core | 参与查询和声明远程入口 | FR-001, FR-002, FR-003 | rules/backend/03-api.md, rules/backend/06-security.md | M09、M10、M12 |
| MOD-004 | mango-ui/packages/workflow | 节点派单模式类型、配置控件和 API 类型 | 扩展 | UI -> workflow REST；不反向依赖业务应用 | CLAIM/AUTO 节点配置 | FR-005, PG-001, BT-001 | rules/frontend/01-vue-code.md, rules/frontend/04-test.md, rules/frontend/12-business-api.md | M10、M12、M13 |
| MOD-005 | mango-docs 与 Workflow README | 公开能力、接入、权限、升级和失败语义 | 更新 | 文档引用公开契约 | 参与关系和自动派单接入说明 | FR-001 至 FR-007 | rules/06-document-assets.md, rules/08-capability-docs.md | M08、M14 |

## 3. 技术对象与状态模型

| 模型ID | 上游ID | 模型职责 | 标识 | 关系 | 状态编码 | 审计或历史 | 归属或租户 | 一致性约束 |
|---|---|---|---|---|---|---|---|---|---|
| DM-001 | DR-001, FR-001 至 FR-004 | 一个用户对一个流程实例的参与类型事实 | tenant_id+process_instance_id+user_id+participant_type | 同业务坐标可有多个实例和参与人 | active=true/false；类型 INITIATOR、CURRENT_ASSIGNEE、COMPLETED_HANDLER、BUSINESS_PARTICIPANT | first_participated_at、last_participated_at、身份显示快照及审计字段 | TenantEntity；tenant_id 来自可信上下文 | 唯一键幂等；只以有效类型判断 readable |
| DM-002 | DR-002, FR-005 | AUTO 节点严格轮询游标 | tenant_id+process_definition_id+task_definition_key | 指向上次选中的 userId | last_assigned_user_id 可空 | updated_at 和审计字段 | 流程定义所属租户 | SELECT FOR UPDATE 后选择和更新；与任务 assignee 同事务 |
| DM-003 | FR-003, FR-005 | 标准候选用户值对象 | tenantId+userId | 可携带 memberId、username、displayName | enabled 成员才可返回 | 不独立持久化 | 当前租户 | 输入 username/userId/组标识全部归一化、去重并按 userId 排序 |

| 模型ID | 当前状态 | 触发 | 目标状态 | 前置条件 | 副作用 | 失败处理 | 上游ID |
|---|---|---|---|---|---|---|---|
| DM-001 | 不存在或 INACTIVE | 发起、成为当前 assignee、完成办理或业务声明 | ACTIVE | 稳定 userId 属于当前租户 | 插入或激活并更新时间 | 身份或写入失败回滚原事务 | FR-003, FR-004 |
| DM-001 | ACTIVE/CURRENT_ASSIGNEE | 释放、转办或任务结束 | INACTIVE | 当前任务已不再由该 userId 办理 | 只失活 CURRENT_ASSIGNEE；其它类型不受影响 | 失败回滚任务动作 | FR-004 |
| DM-001 | ACTIVE/BUSINESS_PARTICIPANT | 完整声明集合移除用户 | INACTIVE | 声明请求全部用户验证通过 | 失活该类型，保留审计 | 任一验证失败不改变集合 | FR-003 |
| DM-002 | 首次或已有游标 | AUTO 节点创建 | 游标指向本次 userId | 候选集合非空且游标行已锁定 | 设置任务 assignee、写 AUTO_ASSIGN 记录、激活 CURRENT_ASSIGNEE | 任一步失败全部回滚 | FR-005 |

## 4. 系统流程、事务与一致性

| 流程设计ID | 系统需求ID | 调用入口 | 参与模块 | 处理顺序 | 事务边界 | 状态变化 | 幂等键 | 并发策略 | 外部失败与补偿 | 用户可见结果 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| FLOW-001 | FR-001, FR-002 | access/my-page 查询 | MOD-003, MOD-002 | 读取可信上下文→校验键/分页→按 tenant+userId 查询并按业务坐标聚合→返回 | 只读事务 | 无 | 查询无幂等副作用 | 复合索引和稳定二级排序 | 上下文缺失 fail-closed；非参与统一 readable=false | readable/类型或稳定分页 |
| FLOW-002 | FR-003 | replace business participants | MOD-003, MOD-002 | 校验权限和业务坐标→一次性解析全部 userId→锁定坐标现有声明→upsert 新增/保留→失活移除→返回 | 单数据库事务 | DM-001 原子替换 | tenant+processInstance+userId+type | 坐标行锁或确定性顺序更新避免死锁 | 身份无效或数据库失败整体回滚 | 完整声明摘要或稳定错误 |
| FLOW-003 | FR-004, FR-006 | 启动与任务动作 | MOD-002 | 原任务校验→执行 Flowable 动作→记录完成办理人→推进任务→按实际运行任务 reconcile 当前 assignee | 复用现有 @Transactional 事务 | DM-001 与任务事实同步 | DM-001 唯一键 | 同实例动作沿用 Flowable 乐观锁，投影幂等 upsert | 投影失败回滚任务动作；不使用异步补偿 | 动作行为不变，提交后查询一致 |
| FLOW-004 | FR-005 | advanceRuntimeTasks | MOD-002 | 读取节点配置→解析/过滤候选→确保游标行→FOR UPDATE→按上次 userId 选下一位并环回→setAssignee→审计/投影→更新游标 | 与流程启动或上一任务动作同事务 | DM-002 推进、任务 assignee、DM-001 激活 | tenant+processDefinitionId+nodeKey | 节点级数据库悲观锁；候选按 userId 稳定排序 | 空候选或锁/写失败抛错并整体回滚 | 响应前已有唯一 assignee 或明确配置错误 |
| FLOW-005 | FR-007 | Flyway V3 migration | MOD-002 | 建表/索引→从可证明任务记录和流程表 INSERT SELECT→由唯一键保证重复安全 | Flyway migration 事务能力按数据库执行 | 新增历史 DM-001 | 唯一键+NOT EXISTS | migration 期间单次串行 | 失败阻断应用升级；不回填 username-only | 升级成功后可确认历史记录可查 |

## 5. API 与远程契约设计

| 接口ID | 系统需求ID | 调用方 | 所属模块 | 入口类型 | 方法与路径 | Command Query或VO | 返回契约 | 校验 | 权限租户或数据权限 | 幂等分页或排序 | 错误码 | 兼容策略 | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| API-001 | FR-001 | 业务详情适配器 | MOD-001, MOD-003 | REST/Feign Query | GET /workflow/participations/access | WorkflowParticipationAccessQuery(processKey,businessKey) | R<WorkflowParticipationAccessVO> | processKey/businessKey 非空且最大 128 | LOGIN；tenantId、userId 只取 MangoContextHolder；只返回当前用户事实 | 同输入无副作用；类型稳定排序 | WORKFLOW_PARTICIPATION_CONTEXT_INVALID | 纯新增；现有详情 API 不变 | rules/backend/03-api.md, rules/backend/06-security.md | M09、M10、M12 |
| API-002 | FR-002 | 已办/参与业务列表 | MOD-001, MOD-003 | REST/Feign Query | GET /workflow/participations/my | WorkflowParticipationPageQuery(processKey,startTime,endTime,page,size) | R<PageResult> | page>=1、1<=size<=100、时间范围合法 | LOGIN；仅当前 tenant+userId | lastParticipatedAt desc、processKey asc、businessKey asc；按业务坐标去重 | 统一参数错误 | 纯新增 | rules/backend/03-api.md, rules/backend/06-security.md | M09、M10、M12 |
| API-003 | FR-003 | 业务服务 | MOD-001, MOD-003 | REST/Feign Command | POST /workflow/participations/business | ReplaceWorkflowBusinessParticipantsCommand(processKey,businessKey,processInstanceId,participantUserIds) | R<WorkflowBusinessParticipantsVO> | 集合非空元素、去重后<=200；流程实例必须匹配坐标 | PERMISSION `workflow:participation:declare`；当前租户；全部 userId 必须是启用成员 | 完整集合替换且幂等 | WORKFLOW_PARTICIPANT_INVALID、WORKFLOW_PROCESS_NOT_FOUND | 新权限资源；启动命令可同时传初始集合 | rules/backend/03-api.md, rules/backend/06-security.md | M09、M10、M12 |
| API-004 | FR-003, FR-004 | 业务流程发起方 | MOD-001 | 现有 Command 扩展 | POST /workflow/processes/start-business | StartBusinessWorkflowCommand 增加 participantUserIds | R<WorkflowStartResultVO> | 可选集合去重后<=200；全部用户原子验证 | 沿用 `workflow:process:start`；当前租户 | 与现有业务启动事务幂等边界一致 | WORKFLOW_PARTICIPANT_INVALID | 字段缺失或空集合保持旧行为 | rules/backend/03-api.md | M09、M10 |
| API-005 | FR-005 | 流程定义管理页面 | MOD-004 | 现有 REST Command | PUT /workflow/definitions | SaveWorkflowDefinitionCommand 的 designerJson 保存 assignmentMode | R<Boolean> | AUTO 节点必须有静态候选来源或运行时可解析配置 | 沿用 `workflow:definition:edit` 和定义数据范围 | 重复保存同一 JSON 结果一致 | 复用定义校验错误 | 旧 JSON 缺失字段按 CLAIM | rules/backend/03-api.md | M09、M10 |

## 6. 持久化与数据迁移设计

| 数据设计ID | 上游或模型ID | 表或实体 | 字段变化 | 约束 | 索引 | 租户审计 | Mapper边界 | 数据来源 | migration或回填 | 回滚或补偿 | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| DB-001 | DM-001, DR-001 | `workflow_process_participant` / WorkflowProcessParticipantEntity | 新表：tenant_id、process_key、business_key、process_instance_id、user_id、member_id、username_snapshot、display_name_snapshot、participant_type、active、first_participated_at、last_participated_at、审计字段 | user_id、流程坐标、type 非空；唯一 tenant+process_instance+user_id+type | 唯一键；tenant+process_key+business_key+user_id+active；tenant+user_id+active+last_participated_at+id | TenantEntity 自动填充/过滤；不允许 InterceptorIgnore | 仅 Workflow mapper 访问 | 运行任务、task record、业务声明 | V3 建表并回填可证明 operator_id；不猜 username | 回滚代码时保留新表不影响旧版本；错误数据用补偿 migration 失活 | rules/backend/04-db.md, rules/backend/07-persistence.md | M09、M11 |
| DB-002 | DM-002, DR-002 | `workflow_auto_assignment_state` / WorkflowAutoAssignmentStateEntity | 新表：tenant_id、process_definition_id、task_definition_key、last_assigned_user_id、created_at、updated_at、审计字段 | 三元组唯一；node key 非空 | 唯一 tenant+process_definition_id+task_definition_key | TenantEntity；按当前定义租户写入 | 专用 mapper 提供 insert-if-absent 和 select-for-update | AUTO 节点运行时 | V3 建空表，不回填旧节点 | 删除 AUTO 配置即可停止更新；保留游标无副作用 | rules/backend/04-db.md, rules/backend/07-persistence.md | M11 |
| DB-003 | FR-007 | V3 历史回填 | INITIATOR 来源 START task record；COMPLETED_HANDLER 来源终结任务动作且 operator_id 非空；CURRENT_ASSIGNEE 在升级后由运行时 reconcile | 只插入存在明确 tenant、流程坐标、processInstance 和 userId 的行 | 复用 DB-001 唯一键 | tenant 来自 Workflow 自有表，不从 username 推断 | migration 只访问 Workflow 自有表 | workflow_task_record、workflow_form_instance、workflow_business_apply | INSERT SELECT+NOT EXISTS，测试升级和重复执行 | 不确定行不写；后续可由业务声明补充 | rules/backend/04-db.md | M11 |

## 7. 安全、权限、租户与数据边界

| 安全设计ID | 系统需求ID | 能力 | 权限资源 | 默认授权 | 后端校验入口 | 租户边界 | 数据归属断言 | 前端反馈 | 审计 | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| SEC-001 | FR-001, FR-002 | 当前用户参与查询 | LOGIN | 所有登录用户仅查询自己 | Controller 登录门禁+服务强制当前 userId/memberId | tenant 只取上下文，mapper 租户拦截启用 | 返回行必须 tenant、userId 与上下文一致 | 非参与返回正常 readable=false 或空页 | 不记录完整参与人集合 | rules/backend/06-security.md | M10、M12 |
| SEC-002 | FR-003 | 业务参与人声明 | `workflow:participation:declare` | 不新增默认角色绕过；由业务接入显式授权 | Controller PERMISSION+流程坐标归属+全部启用成员校验 | 请求不含 tenantId；processInstance 必须属于当前租户坐标 | userId 解析结果 tenant 一致，零部分写入 | 无权限 403；非法成员返回稳定业务错误 | 记录坐标、声明数量和调用 userId，不记录其它租户资料 | rules/backend/06-security.md | M09、M10、M12 |
| SEC-003 | FR-005 | 自动派单 | 流程运行内部能力 | 仅 assignmentMode=AUTO 触发 | 运行时配置、候选目录和游标锁 | 候选查询全部带 current tenant；禁用/离职过滤 | 选中 userId 必须在本次候选快照 | 空候选显示节点配置错误 | AUTO_ASSIGN 记录 processDefinitionId、nodeKey、userId、策略 | rules/backend/06-security.md | M10、M11 |
| SEC-004 | FR-006 | 任务操作 | 沿用 workflow:task:* | 完全保持现有授权 | ensureCurrentUserCanOperate 只使用 assignee/candidate/group | 不读取参与投影 | 历史参与身份不能满足任务操作断言 | 沿用“当前用户不能处理该任务” | 沿用任务动作记录 | rules/backend/06-security.md | M10、M12 |

## 8. 错误码、异常与可观测性

| 错误设计ID | 系统需求ID | 失败场景 | 触发条件 | 错误码 | 异常类型 | 用户反馈 | 日志上下文 | 指标或告警 | 重试或补偿 | 敏感信息处理 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| ERR-001 | FR-003 | 声明用户非法 | userId 不存在、禁用、离职或不属于当前租户 | WORKFLOW_PARTICIPANT_INVALID | 业务前置条件异常 | “参与用户无效，请刷新人员后重试” | tenant、processKey、businessKey、invalidCount | participant_declare_rejected_total | 修复集合后重试；原事务零写入 | 不回显其它租户用户详情 |
| ERR-002 | FR-005 | AUTO 无候选 | 候选为空、全部禁用或表达式无法归一化 | WORKFLOW_AUTO_ASSIGN_NO_CANDIDATE | 业务配置异常 | 包含流程版本、节点名称/Key和候选来源的配置提示 | tenant、processDefinitionId、nodeKey、assigneeType、groupIds | workflow_auto_assign_failed_total{reason=no_candidate} | 修复节点配置或切回 CLAIM 后重试原动作 | 不记录姓名以外身份敏感字段 |
| ERR-003 | FR-005 | 游标锁或任务写入失败 | 数据库超时、死锁或 Flowable 乐观锁 | 沿用数据库/WORKFLOW_TASK_INVALID | 事务异常 | 统一操作失败，可安全重试 | tenant、processDefinitionId、nodeKey、taskId、attempt | workflow_auto_assign_failed_total{reason=concurrency} | 整体回滚；调用方重试，不做异步补派 | 不记录流程变量全文 |
| ERR-004 | FR-001, FR-002 | 登录上下文不完整 | tenantId、userId 或 memberId 缺失 | WORKFLOW_PARTICIPATION_CONTEXT_INVALID | 安全上下文异常 | 不可查询参与信息 | requestId、缺失字段名 | workflow_participation_context_invalid_total | 不回退默认租户或 anonymous | 不记录凭据和 Token |

## 9. 前端结构与交互实现映射

| 前端设计ID | 系统需求ID | 页面或动作 | 页面key或路由 | 区域与组件 | 状态来源 | API依赖 | 权限或不可操作 | 空加载或失败态 | 语义测试锚点 | 复用判断 | 适用规范ruleId |
|---|---|---|---|---|---|---|---|---|---|---|---|
| UI-001 | PG-001, BT-001, FR-005 | 审批节点派单模式 | workflow/definition/index | WorkflowNodeApprovalConfig 内分段控件；AUTO 下展示固定 ROUND_ROBIN | 节点 properties.assignmentMode，缺失归一化为 CLAIM | API-005 | 只在节点可编辑时可切换；AUTO 静态候选为空时阻止保存 | 复用页面加载/保存状态；错误定位节点 | `data-testid="workflow-assignment-mode"` 和序列化断言 | 复用现有节点审批配置，不新增页面或卡片 | rules/frontend/01-vue-code.md, rules/frontend/04-test.md |

## 10. 测试设计与验收映射

| 测试用例ID | 系统验收ID | 设计项ID | 场景 | 优先级 | 测试层级 | 自动化判断 | 测试数据 | 权限或租户边界 | 稳定契约 | 执行入口 | 证据 | 失败处理 | 适用规范ruleId |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| TC-001 | SAC-001, SAC-002 | DEC-001, DEC-002, MOD-001, MOD-002, MOD-003, DM-001, FLOW-001, API-001, SEC-001, SEC-004, ERR-004 | 发起人、当前办理人、已完成办理人查询与任务操作隔离；跨租户同键 | P0 | core/starter 集成/API | AUTO | 两租户、A/B/C、同键流程 | 当前 tenant+userId；非参与和跨租户零命中 | readable 类型、404/false 防泄露、complete 仍拒绝 | Maven workflow 定向测试 | JUnit 报告 | 任一权限边界失败阻断 | rules/backend/08-test.md |
| TC-002 | SAC-003 | DEC-002, DEC-005, DM-001, FLOW-002, API-003, API-004, SEC-002, ERR-001 | 初始声明、完整替换、重复请求、非法用户原子失败 | P0 | core/starter 集成/API | AUTO | 当前租户启用/禁用/离职和其它租户 userId | 声明权限、可信租户 | 全量验证、唯一键幂等、零部分写入 | Maven workflow 定向测试 | JUnit 报告和数据库摘要 | 原子性或租户断言失败阻断 | rules/backend/08-test.md |
| TC-003 | SAC-004, SAC-005 | DEC-003, DEC-004, DEC-006, MOD-002, DM-002, DM-003, FLOW-004, DB-002, SEC-003, ERR-002 | 单候选、多候选、角色/岗位/组织展开、空候选回滚 | P0 | core 集成 | AUTO | A/B/C，空组，禁用成员，AUTO/CLAIM | 候选仅当前租户启用成员 | 响应前 assignee、审计、游标；空候选无提交 | WorkflowTaskRuntimeService 集成测试 | JUnit 报告和表断言 | 任一静默兜底或孤儿任务阻断 | rules/backend/08-test.md |
| TC-004 | SAC-006 | DEC-003, DM-002, FLOW-004, DB-002, ERR-003 | 并发创建同节点任务严格轮询 | P0 | MySQL 并发集成 | AUTO | A/B/C 候选，至少 12 个并发任务 | 单租户单流程版本节点 | 节点行锁无丢失更新，序列按游标环回 | Testcontainers/MySQL 定向测试 | JUnit、选中序列和游标摘要 | 死锁未恢复、重复旧游标或孤儿任务阻断 | rules/backend/08-test.md |
| TC-005 | SAC-007 | MOD-002, FLOW-001, API-002, DB-001 | 10 万关系下当前用户分页和索引契约 | P1 | mapper/集成 | AUTO | 合成多租户多业务关系 | 查询始终含 tenant+userId | size<=100、稳定排序、无重复、复合索引存在 | migration contract+mapper integration | SQL 索引断言和 JUnit | 无界查询或缺索引阻断 | rules/backend/04-db.md, rules/backend/08-test.md |
| TC-006 | SAC-008 | DEC-001, DEC-002, FLOW-005, DB-001, DB-003 | V2 到 V3 回填、重复迁移契约、username-only 不授权 | P0 | migration upgrade | AUTO | START/COMPLETE 有 operatorId 与仅 username 样本 | 两租户历史数据 | 可证明行回填，未知身份零授权，schema/version 正确 | WorkflowMigrationUpgradeIntegrationTest | JUnit 和迁移表摘要 | checksum、回填或租户错误阻断 | rules/backend/04-db.md, rules/backend/08-test.md |
| TC-007 | SAC-004, SAC-005 | MOD-004, UI-001 | CLAIM 默认、AUTO 配置、ROUND_ROBIN 展示和序列化 | P1 | Vitest/typecheck | AUTO | 新旧节点 JSON、空候选 AUTO | 仅编辑权限影响控件 | 缺失字段默认 CLAIM，序列化不丢，空候选定位节点 | `pnpm --filter @mango/workflow test && typecheck` | Vitest/tsc 摘要 | 配置漂移或旧定义回归阻断 | rules/frontend/04-test.md |
| TC-008 | SAC-001 至 SAC-008 | MOD-005 | README、能力地图、接入指南和 API surface 同步 | P1 | 文档/静态 | AUTO | 当前代码与 OpenAPI/Feign surface | 明确 LOGIN/PERMISSION 和租户边界 | M08、M09、M14 无漂移 | 能力和 API contract checker | checker 输出 | 公开能力漏文档阻断 | rules/08-capability-docs.md |

## 11. 兼容与已启用能力说明影响

| 影响ID | 设计项ID | 影响对象 | 当前行为 | 目标行为 | 兼容策略 | 升级或补偿 | 已启用能力说明 | 验证 | 责任人 |
|---|---|---|---|---|---|---|---|---|---|
| IMP-001 | API-001, API-002, API-003, API-004, DB-001 | 业务工作流消费者 | 依赖当前 assignee 或业务日志临时判断历史可见性 | 使用公开参与关系 access/my/declare 契约 | 全部为新增 API；业务可逐步替换临时 Port | V3 回填可证明旧记录，未知身份由业务声明补充 | 更新 Workflow README、业务审批接入指南、能力地图 | TC-001, TC-002, TC-006, TC-008 | Workflow owner |
| IMP-002 | DEC-003, DEC-006, UI-001, DB-002 | 已发布流程定义和流程管理员 | 旧节点无 assignmentMode，候选任务保持领取语义 | 新配置 AUTO 严格轮询；空候选失败 | 缺失 assignmentMode 一律 CLAIM；不改历史发布版本 | 管理员只对新版本显式启用 AUTO；停用可切回 CLAIM | 更新节点配置和失败语义说明 | TC-003, TC-004, TC-007, TC-008 | Workflow owner |
| IMP-003 | SEC-004, FLOW-003 | 当前任务消费者 | 任务动作按 assignee/candidate/group 校验 | 完全保持 | 不读取参与表，不增加 fallback | 无 | README 明确只读关系不等于办理权 | TC-001, TC-008 | Workflow owner |

## 12. 技术追踪矩阵

| 上游ID | 设计项ID | 测试用例ID | 覆盖说明 |
|---|---|---|---|
| SC-001, SA-001, FR-001, FR-002, UC-001, UC-002, DR-001, IR-002, NFR-001, NFR-003, SAC-001, SAC-002, SAC-007 | DEC-001, DEC-002, MOD-001, MOD-002, MOD-003, MOD-005, DM-001, FLOW-001, API-001, API-002, DB-001, SEC-001, SEC-004, ERR-004, IMP-001, IMP-003 | TC-001, TC-005, TC-008 | 覆盖参与查询、分页、稳定身份、租户、能力说明与办理权隔离 |
| SC-002, SA-002, FR-003, UC-003, IR-001, SAC-003 | DEC-002, DEC-005, MOD-001, MOD-002, MOD-003, DM-001, DM-003, FLOW-002, API-003, API-004, DB-001, SEC-002, ERR-001, IMP-001 | TC-002, TC-008 | 覆盖启动声明和运行中原子替换 |
| SC-003, SA-003, FR-005, UC-004, PG-001, BT-001, DR-002, NFR-002, NFR-004, SAC-004, SAC-005, SAC-006 | DEC-003, DEC-004, DEC-006, MOD-002, MOD-004, DM-002, DM-003, FLOW-004, API-005, DB-002, SEC-003, ERR-002, ERR-003, UI-001, IMP-002 | TC-003, TC-004, TC-007, TC-008 | 覆盖节点配置、候选目录、严格轮询和空候选回滚 |
| SC-004, SA-004, FR-004, FR-006, UC-001 | DEC-005, MOD-002, DM-001, FLOW-003, SEC-004, IMP-003 | TC-001, TC-002 | 覆盖任务动作与参与投影同事务及权限隔离 |
| FR-007, DR-003, SAC-008 | DEC-001, DEC-002, MOD-002, FLOW-005, DB-001, DB-003, IMP-001 | TC-006, TC-008 | 覆盖安全历史回填与升级兼容 |

## 13. 阶段判定与审批

| 检查项 | 结果 | 证据 |
|---|---|---|
| 技术设计 checker | PASS | `node mango-pmo/tools/check-technical-design.mjs --document mango-docs/designs/issue-732-workflow-participation-auto-assignment/technical-design.md` |
| 生命周期 handoff | PASS | SRS-WORKFLOW-732 已审批且 upstreamDocumentHash 与文件 SHA-256 一致 |
| 专项规范检查计划 | PASS | M08、M09、M10、M11、M12、M13、M14 已映射到 API、数据库、权限、并发、前端和能力文档测试 |
| 未关闭阻断数量 | 0 | 无 |
| Tech Lead 审批 | APPROVED | `review/APPROVAL.md` |
