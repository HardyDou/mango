---
documentId: TDD-ISSUE-919
documentType: technical-design
pmoVersion: 1.4.4
schemaRevision: 1
riskLevel: L3
riskAssessmentEvidence: requirement=L3，成员生命周期涉及租户访问、授权撤销和身份归属；solution=L3，方案跨 Identity、Org、Authorization 与 RBAC，并新增持久化审计和受保护恢复契约；final=max(requirement,solution)
status: APPROVED
action: NEXT
owner: Mango Identity 与 Org Tech Lead
approver: HardyDou
approvalEvidence: review/APPROVAL.md
upstreamDocumentId: SRS-ISSUE-919
upstreamDocumentHash: 426f6ff6d3898a5e340a94abe44bc7dafea3f69df828b57a3f6eaee1276b67be
---

# Issue 919 成员生命周期技术设计文档

## 1. 设计输入、约束与决策

| 决策ID | 问题 | 候选方案 | 选择 | 理由 | 来源ID或路径 | 是否推断 | 影响 | 风险 | 回退条件 |
|---|---|---|---|---|---|---|---|---|---|
| DEC-001 | 部门关系移除与成员移出语义混淆 | 继续共用删除；按关系和成员拆分 | 部门动作只按 `relationId` 调用 Org；租户动作只按 `userId` 调用 Identity | 两个稳定标识分别对应两种生命周期对象 | FR-001、FR-002、UC-001、UC-002 | 否 | API、页面动作与文案 | 误传标识会扩大删除范围 | 关系归属无法在服务端验证时停止实现 |
| DEC-002 | 租户成员移出后无法恢复 | 物理删除；新增归档表；原表软移出 | 保留 `identity_user` 与 `tenant_member`，设置 `status=0`、`left_at=now`，清空主组织岗位并撤销当前租户关系和角色 | 复用现有唯一键和稳定身份，认证链路可直接 fail closed | FR-002、DR-001、NFR-002 | 否 | Identity 与 Authorization | 撤权或事务不完整会残留访问 | 无法同事务保证本地状态时停止并补偿远程授权失败 |
| DEC-003 | 新增时如何识别可恢复账号且不泄露跨租户信息 | 直接返回用户；提交后猜测；三态查询 | 受 `system:user:add` 保护的账号可用性查询返回 `AVAILABLE`、`RECOVERABLE`、`UNAVAILABLE`；仅 `RECOVERABLE` 带当前租户脱敏资料 | 页面可提前引导，跨租户或不可恢复账号只有统一结果 | FR-003、FR-004、NFR-001 | 否 | Identity API 与 RBAC | 状态差异可能成为枚举信号 | 任一跨租户分支暴露资料时统一降为 `UNAVAILABLE` |
| DEC-004 | 恢复如何避免客户端接管旧标识 | 客户端提交 userId/memberId；按用户名重查 | Org 校验目标组织后，经 Provider 按当前租户、realm、username 重新查找已移出成员并恢复 | 不信任候选快照或客户端内部标识，恢复时再次校验身份、成员和状态 | FR-003、SAC-003 | 否 | Org/Identity 公共契约 | 并发恢复可能重复建关系 | 状态已变化或关系存在时拒绝并回滚 |
| DEC-005 | 恢复后如何保留移出历史 | 覆盖时间；应用日志；追加表 | Identity V5 新增 `tenant_member_lifecycle_log`，成功创建、移出、恢复分别追加 `CREATED`、`REMOVED`、`RESTORED` | 恢复清空 `left_at` 不会抹去历史事实 | DR-003、SAC-002、SAC-003 | 否 | Flyway、Mapper、事务测试 | 漏记事件会破坏审计链 | 事件写入失败时业务事务回滚 |
| DEC-006 | 是否兼容特性前物理删除成员 | 猜测重建；历史回填；不处理 | 不查询、不重建、不回填特性前已经丢失的成员 | 用户明确“历史数据不用考虑；特性以今天为准” | SC-003、BS-002、review/APPROVAL.md | 否 | 迁移与恢复查询 | 无 | 任何实现要求历史猜测时回到需求阶段 |

## 2. 模块与依赖边界

| 模块设计ID | 模块或包 | 职责 | 改动类型 | 依赖方向 | 公开能力 | 系统需求ID | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|
| MOD-001 | `mango-identity-api/core/starter/starter-remote` | 账号可用性、成员软移出、恢复 Provider、生命周期记录 | 修改 | Org 与管理端依赖 Identity 契约，Identity 不反向依赖 Org | availability、restore provider、soft remove | FR-002、FR-003、FR-004、DR-001、DR-003 | BACKEND-MODULE-BOUNDARY、API-CONTRACT | Maven 契约、集成和架构检查 |
| MOD-002 | `mango-org-api/core/starter/starter-remote` | 校验组织并编排创建、恢复、部门关系移除 | 修改 | Org 调用 TenantMemberProvider | restore member account、remove org relation | FR-001、FR-003、DR-002 | BACKEND-MODULE-BOUNDARY、API-CONTRACT | Org 单元与适配器契约测试 |
| MOD-003 | `mango-authorization` adapter | 移出租户时撤销成员角色 | 复用 | Identity 通过既有 adapter 调用 Authorization API | delete subject role bindings | FR-002、NFR-002 | SECURITY-AUTHORIZATION | Identity 集成测试断言命令和残留 |
| MOD-004 | `mango-ui/packages/rbac` | 独立动作、恢复核对和同名引导 | 修改 | RBAC 调用 Identity 与 Org HTTP API | member lifecycle UI | UC-001、UC-002、UC-003、PG-001、BT-001、BT-002、BT-003、BT-004 | FRONTEND-API、FRONTEND-STATE | Vitest、build、Playwright |

## 3. 技术对象与状态模型

| 模型ID | 上游ID | 模型职责 | 标识 | 关系 | 状态编码 | 审计或历史 | 归属或租户 | 一致性约束 |
|---|---|---|---|---|---|---|---|---|
| DM-001 | DR-001、FR-002、FR-003 | 稳定租户成员主体 | `tenant_member.id` | `(tenant_id,user_id)` 唯一关联登录身份 | 有效=`status=1,left_at IS NULL`；已移出=`status=0,left_at IS NOT NULL` | joinedAt/leftAt 加生命周期表 | `tenant_id` | 恢复不得改变 userId/memberId |
| DM-002 | DR-002、FR-001、FR-003 | 成员部门岗位关系 | `tenant_member_org.id` | `(tenant_id,member_id,org_id)` 唯一 | 关系存在或不存在；最多一个主关系 | 既有审计字段 | 当前租户 | 删除主关系后确定性提升最小 ID 关系，否则清空主指针 |
| DM-003 | DR-003 | 追加成员生命周期事件 | `tenant_member_lifecycle_log.id` | 关联 tenantId/userId/memberId | `CREATED`、`REMOVED`、`RESTORED` | occurredAt、operatorUserId、审计字段 | 当前租户 | 业务成功与事件追加同事务；不更新、不删除历史事件 |
| DM-004 | FR-003、FR-004 | 账号可用性响应 | realm+username | 可选当前租户已移出成员 | `AVAILABLE`、`RECOVERABLE`、`UNAVAILABLE` | 无持久化 | 查询时绑定当前租户 | 只有 RECOVERABLE 返回姓名、脱敏联系方式、成员编号和移出时间 |

| 模型ID | 当前状态 | 触发 | 目标状态 | 前置条件 | 副作用 | 失败处理 | 上游ID |
|---|---|---|---|---|---|---|---|
| DM-001 | 有效 | 移出租户成员 | 已移出 | 当前租户成员有效且目标不是当前用户 | 删除当前租户角色与全部部门关系，清空主组织岗位，追加 REMOVED | 任一步失败回滚本地状态；不删除身份或成员 | FR-002、SAC-002 |
| DM-001 | 已移出 | 恢复原成员 | 有效 | Identity 用户启用，成员 leftAt 非空，目标组织有效 | 清空 leftAt、启用成员、只新增目标部门关系、追加 RESTORED | 状态冲突或关系冲突全部回滚 | FR-003、SAC-003 |
| DM-002 | 已归属 | 移出当前部门 | 未分配或仍有其它归属 | relation 属于当前租户 | 删除准确关系并按规则调整主指针 | 关系不存在或跨租户不修改 | FR-001、SAC-001 |

## 4. 系统流程、事务与一致性

| 流程设计ID | 系统需求ID | 调用入口 | 参与模块 | 处理顺序 | 事务边界 | 状态变化 | 幂等键 | 并发策略 | 外部失败与补偿 | 用户可见结果 |
|---|---|---|---|---|---|---|---|---|---|---|
| FLOW-001 | FR-001、SAC-001 | DELETE /org/members | MOD-002、MOD-001 | 查关系与租户；删除关系；若为主关系选择同成员最小 ID 剩余关系并提升，否则清空主指针 | Identity Provider 本地事务 | 只变 DM-002 与主指针 | relationId | 删除后重查；不存在拒绝 | 无外部副作用 | 已移出当前部门 |
| FLOW-002 | FR-002、SAC-002 | Identity 单个/批量移出 | MOD-001、MOD-003 | 排除当前用户；锁定/筛选有效成员；撤销角色；删部门关系；软移出；逐个追加 REMOVED | Identity 服务事务；Authorization 调用失败抛错阻断 | DM-001 有效到已移出 | tenantId+memberId+leftAt-null | 条件更新只接受有效成员，重复请求返回 0 | Authorization 失败不继续本地提交 | 已移出租户成员数量 |
| FLOW-003 | FR-003、FR-004、SAC-003、SAC-004 | account availability 与 Org restore | MOD-004、MOD-001、MOD-002 | 规范化账号；三态查询；展示当前租户候选；Org 校验部门；Provider 重新校验并恢复、建唯一关系、追加 RESTORED | restore 在 Identity 本地事务，Org 只做前置所有权校验 | DM-001 已移出到有效，DM-002 新建一条 | tenantId+realm+username | 唯一键和 leftAt 条件阻止重复恢复 | Provider 失败不产生 Org 本地数据 | 恢复原成员或账号不可用 |
| FLOW-004 | FR-003 | 新成员创建 | MOD-002、MOD-001 | Org 校验部门；Identity 校验账号三态；建 user/member 并追加 CREATED；新增目标关系 | Identity 本地事务 | 新建 DM-001、DM-002、DM-003 | realm+username、tenantId+userId | 唯一键冲突转换为可恢复或不可用错误 | 创建失败整笔回滚 | 新增成功或引导恢复/修改账号 |

## 5. API 与远程契约设计

| 接口ID | 系统需求ID | 调用方 | 所属模块 | 入口类型 | 方法与路径 | Command Query或VO | 返回契约 | 校验 | 权限租户或数据权限 | 幂等分页或排序 | 错误码 | 兼容策略 | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| API-001 | FR-003、FR-004 | RBAC | MOD-001 | HTTP Query | GET /identity/users/account-availability | IdentityAccountAvailabilityQuery(username,realm) | R<IdentityAccountAvailabilityVO> | trim、既有账号长度、默认 INTERNAL | `system:user:add`；只认当前租户 retained member | 无分页；精确匹配 | VALIDATION_ERROR | 新增接口 | API-CONTRACT、SECURITY-TENANT | MVC/适配器与跨租户集成测试 |
| API-002 | FR-003 | RBAC | MOD-002 | HTTP Command | POST /org/member-accounts/restore | RestoreOrgMemberAccountCommand(orgId,postId,username,realm) | R<Long> | orgId、username、realm、postId | `system:user:add`；组织和岗位必须属于当前租户 | 重复恢复拒绝 | ACCOUNT_UNAVAILABLE、MEMBER_NOT_RECOVERABLE | 新增接口 | API-CONTRACT、SECURITY-TENANT | Org service/controller contract test |
| API-003 | FR-003 | Org local/remote | MOD-001 | HTTP/Provider Command | POST /tenant-members/restore-in-org | RestoreTenantMemberInOrgCommand(tenantId,orgId,postId,username,realm,operatorUserId) | R<Long> | tenantId/orgId/username/operator 必填 | INTERNAL adapter；tenantId 必须等于上下文 | 同一成员只恢复一次 | Identity 业务码 | TenantMemberApi、Feign 和 Provider 同步新增 | API-ADAPTER-CONTRACT | adapter contract 与 provider 集成测试 |
| API-004 | FR-001 | RBAC | MOD-002 | HTTP Command | DELETE /org/members | relationId | R<Boolean> | 正数且关系存在 | `system:org:edit`；租户插件与关系归属 | 重复请求失败 | ORG_MEMBER_RELATION_NOT_FOUND | 保持路径，只修正最后主部门语义 | API-CONTRACT | Org service test 与浏览器 network |
| API-005 | FR-002 | RBAC | MOD-001 | HTTP Command | POST /identity/users/delete-batch | BatchDeleteIdentityUserCommand(userIds) | R<Integer> | 非空、去重、自移出排除 | `system:user:delete`；只修改当前租户有效成员 | 重复移出返回 0 | 既有业务失败 | 单个 DELETE 路径保持；批量语义由物理删除改为软移出 | API-CONTRACT | Identity 集成与浏览器测试 |

## 6. 持久化与数据迁移设计

| 数据设计ID | 上游或模型ID | 表或实体 | 字段变化 | 约束 | 索引 | 租户审计 | Mapper边界 | 数据来源 | migration或回填 | 回滚或补偿 | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| DB-001 | DM-003、DR-003 | `tenant_member_lifecycle_log` | 新表：tenant_id、user_id、member_id、event_type、operator_user_id、occurred_at 与通用审计列 | 必填主体、事件和发生时间；应用仅 insert | `(tenant_id,member_id,occurred_at)`、`(tenant_id,user_id)` | tenant_id 与 created/updated 审计 | 专属 Entity/Mapper，只由 Identity 生命周期服务写 | 特性生效后成功操作 | Identity V5 只建表，不回填历史 | 未发布时删除新表；已写数据不得自动回滚删除 | DB-MIGRATION、PERSISTENCE-MAPPER | Flyway 空库、Mapper 集成、事件计数断言 |
| DB-002 | DM-001、FR-002、FR-003 | 既有 `tenant_member` | 无 DDL；正式使用 status、left_at、primary_org_id、primary_post_id | `(tenant_id,user_id)` 保持唯一 | 复用租户状态索引 | 既有审计自动更新 | 条件查询区分 leftAt null/not null | 今天起的软移出 | 无历史回填 | 事务失败回滚 | DB-TRANSACTION | H2/MySQL 集成测试 |

## 7. 安全、权限、租户与数据边界

| 安全设计ID | 系统需求ID | 能力 | 权限资源 | 默认授权 | 后端校验入口 | 租户边界 | 数据归属断言 | 前端反馈 | 审计 | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| SEC-001 | SA-001、FR-001 | 移出当前部门 | `system:org:edit` | 不新增默认授权 | SysOrgController/Service | relation 和 org 由当前租户过滤 | relationId 必须可见且 member 保留 | 明确“移出当前部门” | 既有关系审计 | SECURITY-AUTHORIZATION、SECURITY-TENANT | 越权关系与最后关系测试 |
| SEC-002 | SA-001、SA-002、FR-002 | 移出租户成员 | `system:user:delete` | 不新增默认授权 | IdentityUserController/Service | 只查当前 tenant_member | current user 永不进入目标集合 | 明确撤销角色部门但保留身份 | REMOVED 事件含 operator | SECURITY-AUTHORIZATION、SECURITY-TENANT | 自移出、跨租户、访问失效测试 |
| SEC-003 | SA-001、FR-003、FR-004 | 查询并恢复旧成员 | `system:user:add` | 不新增默认授权 | Identity availability 与 Org restore | 只有当前租户 leftAt 非空成员可返回候选 | realm+username、tenantId、identity status、member state 全部重查 | 其它租户统一“登录账号不可用” | RESTORED 事件 | SECURITY-DATA-MINIMIZATION、SECURITY-TENANT | 双租户同账号和响应字段测试 |

## 8. 错误码、异常与可观测性

| 错误设计ID | 系统需求ID | 失败场景 | 触发条件 | 错误码 | 异常类型 | 用户反馈 | 日志上下文 | 指标或告警 | 重试或补偿 | 敏感信息处理 |
|---|---|---|---|---|---|---|---|---|---|---|
| ERR-001 | FR-003、FR-004 | 账号不可创建或恢复 | 跨租户存在、身份停用、当前租户无 retained member | `ACCOUNT_UNAVAILABLE` | BizException | 登录账号不可用，请修改登录账号 | tenantId、realm、结果，不记联系方式 | 无新增指标 | 修改账号后重试 | 不返回存在位置或主体资料 |
| ERR-002 | FR-003 | 当前租户账号可恢复却走普通新增 | leftAt 非空 retained member | `RECOVERABLE_ACCOUNT` | BizException | 该账号对应已移出成员，请恢复原成员或修改登录账号 | tenantId、userId、memberId | 无新增指标 | 走恢复流程 | 仅当前租户可见 |
| ERR-003 | FR-001、FR-003 | 关系或恢复状态已变化 | relation 不存在、member 已恢复、组织失效 | 既有 Org not found 或 `MEMBER_NOT_RECOVERABLE` | BizException | 刷新后重试或修改账号 | tenantId 与目标业务键 | 无新增指标 | 不自动重试写操作 | 不显示跨租户信息 |

## 9. 前端结构与交互实现映射

| 前端设计ID | 系统需求ID | 页面或动作 | 页面key或路由 | 区域与组件 | 状态来源 | API依赖 | 权限或不可操作 | 空加载或失败态 | 语义测试锚点 | 复用判断 | 适用规范ruleId |
|---|---|---|---|---|---|---|---|---|---|---|
| UI-001 | PG-001、BT-001、FR-001 | 部门行“移出当前部门” | `user.management` | 具体部门筛选下的行操作与确认框 | row.orgRelationId、selectedOrg | API-004 | 仅具体部门且 relationId 存在 | 请求中禁重复，失败保留列表 | `data-action=user.org.remove` | 复用现有表格和确认框 | FRONTEND-ACTION、FRONTEND-STATE |
| UI-002 | PG-001、BT-002、FR-002 | “移出租户成员”单个与批量 | `user.management` | 全部/部门视图行操作与工具栏 | row.userId、当前 session userId | API-005 | 当前用户禁选且动作禁用 | 明确成功数与失败反馈 | `data-action=user.tenant.remove` | 重命名现有删除动作并补说明 | FRONTEND-ACTION、FRONTEND-STATE |
| UI-003 | PG-001、BT-003、BT-004、FR-003、FR-004 | 新增表单账号核对与恢复 | `user.management` | 用户名输入、候选核对区和页脚动作 | availability 三态与请求序号 | API-001、API-002 | RECOVERABLE 禁止普通新增；UNAVAILABLE 禁止提交 | 加载、无候选、查询失败、候选过期均可恢复操作 | `data-state=recoverable-account`、`data-action=user.restore` | 复用新增对话框和组织选择 | FRONTEND-FORM、FRONTEND-ASYNC |

## 10. 测试设计与验收映射

| 测试用例ID | 系统验收ID | 设计项ID | 场景 | 优先级 | 测试层级 | 自动化判断 | 测试数据 | 权限或租户边界 | 稳定契约 | 执行入口 | 证据 | 失败处理 | 适用规范ruleId |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| TC-001 | SAC-001 | DEC-001、DM-002、FLOW-001、API-004、SEC-001、UI-001 | 移除普通、主、最后部门关系 | P1 | Java unit/integration + browser | AUTO | 一个成员三个关系、单关系成员 | 当前租户与伪造 relation | 只删准确关系，确定性主部门或空指针 | Org Maven tests、Playwright | test reports、截图、network | 任一额外关系或成员变化阻断 | TEST-INTEGRATION、TEST-E2E |
| TC-002 | SAC-002 | DEC-002、DM-001、DM-003、FLOW-002、API-005、SEC-002、DB-002 | 单个批量软移出、自移出与撤权 | P0 | Java integration + API/auth | AUTO | 角色、多部门、当前用户、跨租户用户 | 双租户 | user/member 保留，关系角色清空，leftAt/REMOVED 存在，认证失效 | Identity Maven、真实 MySQL/API | report、SQL/API evidence | 残留访问或主体删除阻断 | TEST-SECURITY、TEST-INTEGRATION |
| TC-003 | SAC-003 | DEC-004、DM-001、DM-003、FLOW-003、API-002、API-003、SEC-003、DB-001、UI-003 | 同一人恢复与并发/重复恢复 | P0 | Java integration + API + browser | AUTO | 今天起移出成员和有效组织 | 当前租户管理员 | userId/memberId 不变，只一个目标关系、角色为空、RESTORED 追加 | Maven、Playwright lifecycle spec | report、trace、SQL | 标识变化或旧授权恢复阻断 | TEST-SECURITY、TEST-E2E |
| TC-004 | SAC-004 | DEC-003、DM-004、FLOW-003、API-001、ERR-001、ERR-002、UI-003 | 可新增、可恢复、其它租户占用、同名不同人 | P0 | API contract + component + browser | AUTO | 双租户相同账号、重复姓名 | system:user:add 有权/无权 | 三态正确且跨租户无资料 | controller tests、Vitest、Playwright | report、network payload | 信息泄露或普通同名创建阻断 | TEST-SECURITY、TEST-CONTRACT |
| TC-005 | SAC-002、SAC-003 | DEC-005、DM-003、DB-001 | CREATED/REMOVED/RESTORED 追加历史 | P1 | Flyway + Java integration | AUTO | 新成员完整生命周期 | 当前租户 | 三条事件顺序正确且移出记录保留 | Identity migration/integration | SQL assertion | 漏记或覆盖阻断 | TEST-DATABASE |

## 11. 兼容与已启用能力说明影响

| 影响ID | 设计项ID | 影响对象 | 当前行为 | 目标行为 | 兼容策略 | 升级或补偿 | 已启用能力说明 | 验证 | 责任人 |
|---|---|---|---|---|---|---|---|---|---|
| IMP-001 | API-005、DEC-002 | Identity 删除接口消费者 | 物理删除 tenant_member | 路径与返回类型不变，语义改为可恢复软移出 | 兼容调用形式，更新语义文档和 UI 文案 | 无历史回填 | Identity README | API/集成测试 | Identity owner |
| IMP-002 | API-002、API-003、DEC-004 | Org/Identity Java 与 HTTP 消费者 | 无恢复能力 | 新增可选恢复契约 | 纯新增接口；本地和 remote adapter 同步 | 无 | Org、Identity README 与 capability map | adapter contract | Org/Identity owner |
| IMP-003 | UI-001、UI-002、UI-003 | `@mango/rbac` UserView | 含糊删除、无部门移出和恢复 | 独立动作与可恢复交互 | 保持路由与页面 key | 无 | RBAC README/views README | build、component、E2E | Frontend owner |

## 12. 技术追踪矩阵

| 上游ID | 设计项ID | 测试用例ID | 覆盖说明 |
|---|---|---|---|
| SC-001、FR-001、UC-001、BT-001、DR-002、SAC-001 | DEC-001、MOD-001、MOD-002、DM-002、FLOW-001、API-004、SEC-001、UI-001、IMP-001、IMP-002 | TC-001 | 部门关系精确移除与最后主部门结果 |
| SC-002、SA-002、FR-002、UC-002、BT-002、DR-001、DR-003、NFR-002、SAC-002 | DEC-002、DEC-005、MOD-001、MOD-003、DM-001、DM-003、FLOW-002、API-005、SEC-002、DB-001、DB-002、UI-002、IMP-001 | TC-002、TC-005 | 软移出、撤权、自保护和追加历史 |
| SC-003、FR-003、UC-003、BT-003、DR-001、DR-002、DR-003、NFR-001、NFR-003、SAC-003 | DEC-003、DEC-004、DEC-005、MOD-001、MOD-002、MOD-004、DM-001、DM-003、DM-004、FLOW-003、API-001、API-002、API-003、SEC-003、UI-003、IMP-002、IMP-003 | TC-003、TC-004、TC-005 | 当前租户同身份恢复、最小授权和防接管 |
| SA-001、FR-004、PG-001、BT-004、IR-001、SAC-004 | DEC-001、DEC-003、DEC-006、MOD-004、DM-004、FLOW-003、FLOW-004、API-001、ERR-001、ERR-002、ERR-003、UI-001、UI-002、UI-003、IMP-003 | TC-001、TC-002、TC-003、TC-004 | 页面动作、账号三态、历史边界和跨租户通用反馈 |

## 13. 阶段判定与审批

| 检查项 | 结果 | 证据 |
|---|---|---|
| 技术设计 checker | PASS | `node mango-pmo/tools/check-technical-design.mjs --document mango-docs/designs/issue-919-member-lifecycle/technical-design.md` |
| 生命周期 handoff | PASS | SRS-ISSUE-919 SHA-256 与追踪检查通过 |
| 专项规范检查计划 | PASS | API、数据库、模块、权限、前端、自动化和能力说明均映射到 TC-001 至 TC-005 |
| 未关闭阻断数量 | 0 | 用户已确认同名、恢复、历史数据和执行边界 |
| Tech Lead 审批 | APPROVED | `review/APPROVAL.md` |
