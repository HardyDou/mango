---
documentId: PLAN-ISSUE-919
documentType: implementation-plan
pmoVersion: 1.4.4
schemaRevision: 1
riskLevel: L3
riskAssessmentEvidence: requirement=L3，成员生命周期涉及租户访问、授权撤销和身份归属；solution=L3，方案跨 Identity、Org、Authorization 与 RBAC，并新增持久化审计和受保护恢复契约；final=max(requirement,solution)
status: APPROVED
action: NEXT
owner: Issue 919 实施负责人
approver: HardyDou
approvalEvidence: review/APPROVAL.md
upstreamDocumentId: TDD-ISSUE-919
upstreamDocumentHash: c202a5762546374948eeb3f07c2db9675b9de4d36fee8749195f72c914770008
---

# Issue 919 成员生命周期实施计划

## 1. 实施目标、范围与交付物

| 交付物ID | 技术设计ID                                                                                                                                                                                                  | 交付物                                                         | 路径或模块                                                                      | 完成状态定义                                                                   | 验收来源                                    | 不处理边界                                     |
| -------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------- | ------------------------------------------------------------------------------- | ------------------------------------------------------------------------------ | ------------------------------------------- | ---------------------------------------------- |
| DEL-001  | DEC-002、DEC-003、DEC-004、DEC-005、MOD-001、DM-001、DM-003、DM-004、FLOW-002、FLOW-003、FLOW-004、API-001、API-003、API-005、DB-001、DB-002、SEC-002、SEC-003、ERR-001、ERR-002、ERR-003、IMP-001、IMP-002 | Identity 软移出、账号可用性、恢复 Provider、生命周期迁移与测试 | `mango-platform/mango-identity`                                                 | API/remote/local 契约一致，V5 可执行，创建移出恢复行为和安全边界由集成测试证明 | SAC-002、SAC-003、SAC-004、TC-002 至 TC-005 | 不回填或猜测特性前物理删除成员，不永久删除身份 |
| DEL-002  | DEC-001、DEC-004、MOD-002、DM-002、FLOW-001、FLOW-003、API-002、API-004、SEC-001、IMP-002                                                                                                                   | Org 恢复编排与部门移出语义修复                                 | `mango-platform/mango-org`                                                      | Org 校验目标组织岗位后调用恢复契约，主部门最后关系可移除且其它校验保持         | SAC-001、SAC-003、TC-001、TC-003            | 不改变组织树、岗位生命周期或权限码             |
| DEL-003  | DEC-001、DEC-003、MOD-004、UI-001、UI-002、UI-003、IMP-003                                                                                                                                                  | RBAC 独立动作和恢复核对交互                                    | `mango-ui/packages/rbac`                                                        | 部门/租户动作含义和参数独立，可恢复账号展示最小资料并只允许恢复或修改账号      | SAC-001 至 SAC-004、TC-001 至 TC-004        | 不新增成员历史列表或账号释放入口               |
| DEL-004  | MOD-001、MOD-002、MOD-003、MOD-004、IMP-001、IMP-002、IMP-003                                                                                                                                               | 能力说明、永久 E2E 和验收证据                                  | Identity/Org/RBAC README、capability map、mango-admin e2e、`.runtime/issue-919` | 文档反映现行契约，自动化及真实入口证据可复核                                   | TC-001 至 TC-005                            | 不发布、不部署、不关闭 Issue                   |

## 2. 工作分解

| 任务ID   | 技术设计ID                                                        | 交付物ID                  | 责任角色      | 路径或模块                            | 前置任务                     | 具体动作                                                                          | 完成标准                                                           | 验证ID             | 实施批次              | 状态    |
| -------- | ----------------------------------------------------------------- | ------------------------- | ------------- | ------------------------------------- | ---------------------------- | --------------------------------------------------------------------------------- | ------------------------------------------------------------------ | ------------------ | --------------------- | ------- |
| TASK-001 | DB-001、DM-003、DEC-005                                           | DEL-001                   | Dev           | identity core migration/entity/mapper | NONE                         | 新增 V5 生命周期表及只追加写入对象                                                | 空库 migration 和 Mapper 编译通过，事件字段完整                    | VAL-001            | B1 Identity data      | PLANNED |
| TASK-002 | DEC-002、FLOW-002、API-005、SEC-002、DB-002、ERR-003              | DEL-001                   | Dev           | IdentityUserService                   | TASK-001                     | 把单个/批量移出改为撤权、删关系、禁用并设置 leftAt，追加 REMOVED                  | 保留 identity_user/tenant_member 与稳定 ID，自移出和重复移出无变化 | VAL-002            | B2 Identity lifecycle | PLANNED |
| TASK-003 | DEC-003、DM-004、API-001、SEC-003、ERR-001、ERR-002               | DEL-001                   | Dev           | identity api/core/starter/remote      | TASK-001                     | 实现账号 AVAILABLE/RECOVERABLE/UNAVAILABLE 查询与脱敏响应                         | 只有当前租户 retained member 返回最小候选资料                      | VAL-003            | B2 Identity lifecycle | PLANNED |
| TASK-004 | DEC-004、FLOW-003、FLOW-004、API-003、SEC-003、DM-001、DB-002     | DEL-001                   | Dev           | TenantMember Provider/API/remote      | TASK-001、TASK-003           | 实现按当前租户 realm+username 恢复，重用 member/user，只建目标关系并追加 RESTORED | 并发/重复恢复拒绝且旧角色部门岗位不恢复                            | VAL-002、VAL-003   | B2 Identity lifecycle | PLANNED |
| TASK-005 | DEC-001、FLOW-001、API-004、SEC-001、DM-002                       | DEL-002                   | Dev           | org core                              | TASK-004                     | 允许移除最后主关系；保留更新降级限制并调用已有确定性提升/清空逻辑                 | relationId 精确移除测试通过                                        | VAL-004            | B3 Org                | PLANNED |
| TASK-006 | DEC-004、FLOW-003、API-002、IMP-002                               | DEL-002                   | Dev           | org api/core/starter/remote           | TASK-004                     | 按 API-002 与 API-003 落地命令、适配器、校验和 Provider 编排                      | 组织岗位租户校验与 adapter contract 通过                           | VAL-004            | B3 Org                | PLANNED |
| TASK-007 | DEC-001、DEC-003、UI-001、UI-002、UI-003、IMP-003                 | DEL-003                   | Frontend Dev  | rbac api/UserView                     | TASK-003、TASK-005、TASK-006 | 按 UI-001 至 UI-003 落地 API 映射、独立动作和恢复核对状态                         | 文案、禁用态、请求参数和陈旧响应处理符合 TDD                       | VAL-005            | B4 Frontend           | PLANNED |
| TASK-008 | TC-001、TC-002、TC-003、TC-004、TC-005                            | DEL-001、DEL-002、DEL-003 | QA/Dev        | Java tests、Vitest、Playwright        | TASK-002 至 TASK-007         | 补齐单元、集成、契约、组件和永久浏览器生命周期用例                                | 五个 TC 均可自动执行且不以 mock 替代安全/数据库关键断言            | VAL-001 至 VAL-006 | B5 Verification       | PLANNED |
| TASK-009 | IMP-001、IMP-002、IMP-003、MOD-001、MOD-002、MOD-003、MOD-004     | DEL-004                   | Dev/Tech Lead | README、capability map、FULL 文档     | TASK-002 至 TASK-008         | 更新能力说明、API 和数据表语义并运行专项检查                                      | 文档与实现一致，文档集合及能力审计通过                             | VAL-007            | B6 Docs               | PLANNED |
| TASK-010 | FLOW-001、FLOW-002、FLOW-003、FLOW-004、SEC-001、SEC-002、SEC-003 | DEL-004                   | QA            | isolated workspace 18006/30006/MySQL  | TASK-008、TASK-009           | 初始化隔离库，执行真实 API/认证/浏览器全流程并保存证据                            | 双租户、ID 保持、授权为空、历史追加、console/network 均满足预期    | VAL-006、VAL-008   | B7 Acceptance         | PLANNED |

## 3. 顺序、依赖与里程碑

| 里程碑ID | 包含任务ID                             | 进入条件                        | 完成条件                                      | 依赖   | 可并行任务                                     | 阻塞升级                                       | 责任人            |
| -------- | -------------------------------------- | ------------------------------- | --------------------------------------------- | ------ | ---------------------------------------------- | ---------------------------------------------- | ----------------- |
| MS-001   | TASK-001、TASK-002、TASK-003、TASK-004 | TDD/Plan checker 与生命周期通过 | Identity 数据、软移出、三态和恢复定向测试通过 | NONE   | TASK-002 与 TASK-003 在 migration 后可局部并行 | 稳定 ID 或撤权事务无法成立时停止并修订 TDD     | Identity owner    |
| MS-002   | TASK-005、TASK-006                     | MS-001 Provider 契约稳定        | Org 部门移出和恢复编排测试通过                | MS-001 | TASK-005 与 TASK-006                           | 组织归属无法服务端验证时停止                   | Org owner         |
| MS-003   | TASK-007、TASK-008                     | MS-002 HTTP 契约稳定            | RBAC build/API tests 与永久 E2E 用例可执行    | MS-002 | Java 补测与前端实现可并行                      | 交互需新增未批准状态时回到 SRS/TDD             | Frontend/QA owner |
| MS-004   | TASK-009、TASK-010                     | MS-003 自动化绿灯               | 文档、真实 MySQL/API/UI 与证据门禁全部通过    | MS-003 | 文档更新与环境初始化可并行                     | 隔离环境与自动化结论不一致时以真实入口失败阻断 | Tech Lead/QA      |

## 4. 验证计划

| 验证ID  | 测试或验收ID                           | 任务ID                       | 验证层级                     | 命令或步骤                                                          | 环境                                      | 测试数据                                        | 权限或租户边界             | 预期结果                                        | 证据路径                                      | 责任人        | 失败处理                                  |
| ------- | -------------------------------------- | ---------------------------- | ---------------------------- | ------------------------------------------------------------------- | ----------------------------------------- | ----------------------------------------------- | -------------------------- | ----------------------------------------------- | --------------------------------------------- | ------------- | ----------------------------------------- |
| VAL-001 | TC-005                                 | TASK-001、TASK-008           | Database/Flyway              | identity core migration 与集成测试                                  | Java 21、H2 及隔离 MySQL                  | 空库和一个完整成员生命周期                      | 当前租户                   | V5 一次执行，三类事件追加不覆盖                 | `.runtime/issue-919/tests/identity-migration` | Dev/QA        | migration 或事件断言失败即阻断            |
| VAL-002 | TC-002、TC-003                         | TASK-002、TASK-004、TASK-008 | Java integration             | Maven 定向执行 IdentityUserService/LocalTenantMemberProvider 测试   | Java 21、真实 Mapper                      | 当前用户、目标成员、角色、多部门、已移出成员    | tenant 1/2                 | 软移出保留 ID 并撤权，恢复复用 ID 且最小归属    | `.runtime/issue-919/tests/identity-core`      | Dev/QA        | 不弱化事务、安全或 ID 断言                |
| VAL-003 | TC-004                                 | TASK-003、TASK-004、TASK-008 | API/security contract        | Identity adapter contract 与 availability 集成测试                  | Spring test                               | free/recoverable/cross-tenant/disabled username | 有权、无权、双租户         | 三态和字段最小化正确                            | `.runtime/issue-919/tests/identity-api`       | Dev/QA        | 任一泄露或接管路径阻断                    |
| VAL-004 | TC-001、TC-003                         | TASK-005、TASK-006、TASK-008 | Java unit/contract           | Maven 定向执行 SysOrgService 与 adapter contract                    | Java 21、Mockito/Mapper contract          | 主/非主/最后关系、有效/无效组织岗位             | 当前租户和伪造 ID          | 精确移除与恢复前置校验符合设计                  | `.runtime/issue-919/tests/org`                | Dev/QA        | 额外关系变化或越权阻断                    |
| VAL-005 | TC-001、TC-004                         | TASK-007、TASK-008           | Frontend API/component/build | pnpm 执行 rbac tests、typecheck/build                               | Node/pnpm workspace                       | 三态响应与选中部门行                            | current user 与权限可见性  | API 映射、文案、禁用和状态转换正确              | `.runtime/issue-919/tests/rbac`               | Frontend/QA   | 不以删除断言换名规避语义                  |
| VAL-006 | TC-001、TC-002、TC-003、TC-004         | TASK-008、TASK-010           | Playwright E2E               | 通过真实 Admin 页面完成创建、部门移出、租户移出、恢复及不同人改账号 | backend 18006、frontend 30006、隔离 MySQL | 双租户和两个同名不同账号人员                    | 管理员权限与跨租户直接请求 | UI、network、console、ID 与授权结果全部符合设计 | `.runtime/issue-919/e2e`                      | QA            | 页面或 API 任一失败阻断，不宣称浏览器通过 |
| VAL-007 | TC-001、TC-002、TC-003、TC-004、TC-005 | TASK-009                     | Docs/governance              | capability、document-set、lifecycle 和模块文档检查                  | 当前 worktree                             | FULL 文档链与 README                            | 不适用                     | 文档与代码契约一致且 checker 通过               | `.runtime/issue-919/docs`                     | Dev/Tech Lead | 修正文档或实现漂移后重跑                  |
| VAL-008 | TC-002、TC-003、TC-004                 | TASK-010                     | Real API/database/auth       | SQL 前后对比、登录/受保护接口、重复与跨租户请求                     | 隔离 MySQL 和运行服务                     | 角色、多部门、leftAt、lifecycle log             | 双租户                     | 泄露、残留访问、重复主体和静默授权恢复均为 0    | `.runtime/issue-919/acceptance`               | QA            | 保留现场并定位根因，禁止生产修改          |

## 5. 数据库实施步骤

| 数据步骤ID | 技术设计ID      | 环境            | 前置检查                                      | 动作                         | 顺序                    | 数据备份或回填     | 验证                                | 失败停止条件                       | 补偿                                         | 责任人 |
| ---------- | --------------- | --------------- | --------------------------------------------- | ---------------------------- | ----------------------- | ------------------ | ----------------------------------- | ---------------------------------- | -------------------------------------------- | ------ |
| DATA-001   | DB-001、DEC-005 | H2 与隔离 MySQL | 确认 Identity 当前最高 V4、目标库为任务隔离库 | 增加并执行 V5 创建生命周期表 | 先 migration 后应用验证 | 不备份、不回填历史 | Flyway history、表结构和事件 insert | 非隔离库、checksum 冲突或 DDL 失败 | 停止；未写业务数据时在隔离库重建，不修改生产 | Dev/QA |

## 6. 已启用说明与资产同步计划

| 文档项ID | 技术设计或交付物ID                 | 目标文档                            | 变化                                                 | 责任人         | 完成条件                      | 检查命令                                                                                           | 不适用依据 |
| -------- | ---------------------------------- | ----------------------------------- | ---------------------------------------------------- | -------------- | ----------------------------- | -------------------------------------------------------------------------------------------------- | ---------- |
| DOC-001  | IMP-001、IMP-002、DEL-001、DEL-002 | Identity/Org README、capability map | 记录软移出、三态、恢复 Provider、V5 表和部门移出语义 | Backend owner  | API/数据/权限表与实现一致     | capability docs checker/audit                                                                      | NONE       |
| DOC-002  | IMP-003、DEL-003                   | RBAC README 与 views README         | 记录独立动作和新增恢复交互                           | Frontend owner | 导出、页面动作和 API 说明一致 | frontend capability audit                                                                          | NONE       |
| DOC-003  | DEL-004                            | Issue 919 FULL 文档集               | 保持 BRD/SRS/TDD/Plan 哈希追踪和验证记录             | Tech Lead/QA   | document-set/lifecycle 全通过 | `node mango-pmo/tools/check-document-set.mjs --root mango-docs/designs/issue-919-member-lifecycle` | NONE       |

## 7. 风险、阻塞与例外

| 风险ID   | 风险等级 | 类型 | 触发条件                                                     | 影响                   | 预防                                                                        | 应对                                               | 责任人                  | 截止时间   | 状态   | 例外ruleId | 例外批准与到期 |
| -------- | -------- | ---- | ------------------------------------------------------------ | ---------------------- | --------------------------------------------------------------------------- | -------------------------------------------------- | ----------------------- | ---------- | ------ | ---------- | -------------- |
| RISK-001 | L3       | RISK | Authorization 远程撤权成功后 Identity 本地事务失败           | 成员仍有效但角色已撤销 | 定向测试错误路径并保留操作可重试性；当前组合部署优先使用本地 adapter        | 失败时不标记已移出，记录上下文并允许管理员重新授权 | Identity owner          | 2026-09-01 | CLOSED | NONE       | NONE           |
| RISK-002 | L3       | RISK | availability 候选在确认前被恢复或状态改变                    | 页面使用陈旧资料提交   | restore 按 username/realm/current tenant 服务端重查，不提交内部主体 ID      | 返回状态冲突并保留表单刷新候选                     | Identity/Frontend owner | 2026-09-01 | CLOSED | NONE       | NONE           |
| RISK-003 | L3       | RISK | 部门范围查询返回下级组织成员但行 relationId 不属于所选根节点 | “当前部门”含义不准确   | 行动作只在准确 relationId 对应的实际 orgId 上执行，并在 UI 展示实际所属部门 | relationId 缺失时不显示动作；网络断言目标关系      | Org/Frontend owner      | 2026-09-01 | CLOSED | NONE       | NONE           |

## 8. 实施追踪矩阵

| 上游设计ID                                                                                                                       | 交付物ID         | 任务ID                       | 验证ID                             | 里程碑数据文档或风险项ID            | 覆盖说明                                           |
| -------------------------------------------------------------------------------------------------------------------------------- | ---------------- | ---------------------------- | ---------------------------------- | ----------------------------------- | -------------------------------------------------- |
| DEC-002、DEC-005、MOD-001、MOD-003、DM-001、DM-003、FLOW-002、API-005、DB-001、DB-002、SEC-002、ERR-003、TC-002、TC-005、IMP-001 | DEL-001          | TASK-001、TASK-002、TASK-008 | VAL-001、VAL-002、VAL-008          | MS-001、DATA-001、DOC-001、RISK-001 | Identity 软移出、撤权与追加历史                    |
| DEC-003、DM-004、API-001、ERR-001、ERR-002、TC-004                                                                               | DEL-001、DEL-003 | TASK-003、TASK-007、TASK-008 | VAL-003、VAL-005、VAL-006          | MS-001、MS-003、RISK-002            | 账号三态、脱敏和同名不同人交互                     |
| DEC-004、FLOW-003、FLOW-004、API-002、API-003、SEC-003、TC-003、IMP-002                                                          | DEL-001、DEL-002 | TASK-004、TASK-006、TASK-008 | VAL-002、VAL-003、VAL-004、VAL-006 | MS-001、MS-002、DOC-001、RISK-002   | 按账号重查并恢复稳定身份和最小关系                 |
| DEC-001、MOD-002、DM-002、FLOW-001、API-004、SEC-001、TC-001                                                                     | DEL-002、DEL-003 | TASK-005、TASK-007、TASK-008 | VAL-004、VAL-005、VAL-006          | MS-002、MS-003、RISK-003            | 部门关系移除与租户移出彻底分离                     |
| MOD-004、UI-001、UI-002、UI-003、IMP-003                                                                                         | DEL-003、DEL-004 | TASK-007、TASK-009、TASK-010 | VAL-005、VAL-006、VAL-007          | MS-003、MS-004、DOC-002、DOC-003    | RBAC 行为、文案、状态和能力说明                    |
| DEC-006                                                                                                                          | DEL-001、DEL-004 | TASK-001、TASK-004、TASK-009 | VAL-001、VAL-003、VAL-007          | DATA-001、DOC-003                   | 固定不回填、不猜测和不恢复特性前物理删除成员的边界 |

## 9. 阶段判定与审批

| 检查项           | 结果     | 证据                                                                                                                                 |
| ---------------- | -------- | ------------------------------------------------------------------------------------------------------------------------------------ |
| 实施计划 checker | PASS     | `node mango-pmo/tools/check-implementation-plan.mjs --document mango-docs/designs/issue-919-member-lifecycle/implementation-plan.md` |
| 生命周期 handoff | PASS     | TDD-ISSUE-919 SHA-256 与全链追踪检查通过                                                                                             |
| 依赖图           | PASS     | TASK-001 至 TASK-010 无环，契约和数据先于消费者                                                                                      |
| 未关闭阻断数量   | 0        | RISK-001 至 RISK-003 均有现行预防与失败停止策略                                                                                      |
| 实施审批         | APPROVED | `review/APPROVAL.md`                                                                                                                 |
