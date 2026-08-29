---
documentId: PLAN-WORKFLOW-732
documentType: implementation-plan
pmoVersion: 1.4.2
schemaRevision: 1
riskLevel: L3
riskAssessmentEvidence: requirement=L3（公开查询、历史可见性、任务归属和跨租户边界）；solution=L3（新增投影与 migration、跨身份事实解析、Flowable 同事务更新、数据库行锁并发）；final=max(requirement,solution)=L3
status: APPROVED
action: NEXT
owner: Mango Workflow 交付负责人
approver: HardyDou
approvalEvidence: review/APPROVAL.md
upstreamDocumentId: TDD-WORKFLOW-732
upstreamDocumentHash: 8c9a638e87047fdb02f6da8c3af7e2152253acdd3a245bc2b1370cdbf1206d1b
---

# 工作流历史参与人只读查询与自动派单实施计划

## 1. 实施目标、范围与交付物

| 交付物ID | 技术设计ID | 交付物 | 路径或模块 | 完成状态定义 | 验收来源 | 不处理边界 |
|---|---|---|---|---|---|---|
| DEL-001 | DEC-001, DEC-002, MOD-001, MOD-002, MOD-003, DM-001, API-001 至 API-004, DB-001, DB-003 | 租户参与关系投影、查询/声明 API、remote client 和安全历史回填 | mango-workflow api/core/starter/starter-remote | 单业务、分页、声明、启动和历史回填契约完整并通过权限/租户测试 | SAC-001, SAC-002, SAC-003, SAC-007, SAC-008 | 不修改业务详情数据和业务状态机 |
| DEL-002 | DEC-003, DEC-006, DEC-007, MOD-002, DM-002, DM-003, FLOW-004, DB-002, SEC-003 | 运行时候选用户目录和严格 ROUND_ROBIN 自动派单 | mango-workflow-core | AUTO 节点事务内设置 assignee；空候选回滚；并发游标无丢失更新 | SAC-004, SAC-005, SAC-006 | 不实现 LEAST_TASKS、AFFINITY |
| DEL-003 | MOD-004, API-005, UI-001 | 流程设计器 CLAIM/AUTO 节点配置 | mango-ui/packages/workflow | 旧定义默认 CLAIM，AUTO 保存 ROUND_ROBIN，静态空候选阻断 | SAC-004, SAC-005 | 不新增管理页面 |
| DEL-004 | MOD-005, IMP-001, IMP-002, IMP-003 | Workflow README、能力地图和业务接入指南 | mango-workflow README、mango-docs | API、权限、租户、升级、派单和不处理边界与代码一致 | SAC-001 至 SAC-008 | 不编写业务项目临时 Port 迁移代码 |
| DEL-005 | TC-001 至 TC-008 | 自动化测试、迁移契约和验证证据 | workflow tests、frontend tests、mango-docs/evidence | 需求验收逐项有自动化或明确剩余风险，L3 门禁通过 | SAC-001 至 SAC-008 | 不部署、不发布、不修改生产数据 |
| DEL-006 | DEC-004, API-006, SEC-005, ERR-005, ERR-006, UI-002, TC-009 | Workflow 设计器候选接口、可替换 Provider 和前端去耦 | workflow api/core/starter/remote、@mango/workflow | 五类候选由一个 Workflow 请求返回；自定义 Provider 可替换；缺失/失败显式报错 | SAC-009 | 不给 Workflow 菜单追加跨域权限，不接受客户端 tenantId |

## 2. 工作分解

| 任务ID | 技术设计ID | 交付物ID | 责任角色 | 路径或模块 | 前置任务 | 具体动作 | 完成标准 | 验证ID | 实施批次 | 状态 |
|---|---|---|---|---|---|---|---|---|---|---|
| TASK-001 | DB-001, DB-002, DB-003, DM-001, DM-002 | DEL-001, DEL-002 | dev | mango-workflow-core persistence | NONE | 按 DB-001、DB-002、DB-003 落实 V3 持久化、并发访问和安全回填 | migration 契约及 V2→V3 升级通过 | VAL-001 | B1 | PLANNED |
| TASK-002 | DEC-002, DEC-007, DM-003 | DEL-001, DEL-002 | dev | mango-workflow-core engine/support | TASK-001 | 实现稳定 userId 解析、启用成员验证和角色/岗位/组织候选展开 | 所有输入归一化、去重、排序且跨租户 fail-closed | VAL-002 | B1 | PLANNED |
| TASK-003 | FLOW-001, FLOW-002, API-001, API-002, API-003, API-004, SEC-001, SEC-002 | DEL-001 | dev | workflow api/core/starter/remote | TASK-001, TASK-002 | 实现 access、my page、声明和启动初始参与人 | API/Feign/权限/分页/原子声明契约通过 | VAL-003 | B2 | PLANNED |
| TASK-004 | DEC-005, FLOW-003, SEC-004 | DEL-001 | dev | WorkflowProcessService、WorkflowTaskRuntimeService | TASK-001, TASK-003 | 在启动和全部任务动作中维护发起、当前办理和完成办理关系 | 投影与任务同事务，历史关系不进入操作授权 | VAL-004 | B2 | PLANNED |
| TASK-005 | DEC-003, DEC-006, FLOW-004, DB-002, ERR-002, ERR-003 | DEL-002 | dev | mango-workflow-core runtime/assignment | TASK-001, TASK-002 | 实现节点游标锁、严格轮询、自动审计、空候选回滚 | 单/多/空候选和并发测试通过 | VAL-005 | B3 | PLANNED |
| TASK-006 | MOD-004, API-005, UI-001 | DEL-003 | dev | mango-ui/packages/workflow | TASK-005 | 增加 assignmentMode 类型、分段控件、默认归一化和节点校验 | Vitest、typecheck、build 通过且旧 JSON 兼容 | VAL-006 | B3 | PLANNED |
| TASK-007 | MOD-005, IMP-001, IMP-002, IMP-003 | DEL-004 | tech-lead | README、capabilities、business integration guide | TASK-003, TASK-005, TASK-006 | 更新公开能力、权限、租户、失败、升级和业务接入说明 | M08、M14 和引用检查通过 | VAL-007 | B4 | PLANNED |
| TASK-008 | TC-001 至 TC-008 | DEL-005 | qa | backend/frontend/tests/evidence | TASK-003, TASK-004, TASK-005, TASK-006, TASK-007 | 运行 L3 最低成本充分验证集并记录命令/结果/残余风险 | M09-M14 和文档集合门禁通过，无未解释失败 | VAL-008 | B4 | PLANNED |
| TASK-009 | DEC-004, API-006, SEC-005, ERR-005, ERR-006, UI-002, TC-009 | DEL-006 | dev/qa | workflow api/core/starter/remote、mango-ui/packages/workflow | TASK-006 | 新增 Provider/默认平台适配器/Workflow 接口，替换五个跨域前端请求，补单元/API/源码边界和浏览器测试 | 仅 Workflow 权限加载五类候选且无 403；自定义 Provider 可替换；失败不为空成功 | VAL-009 | B4 | PLANNED |

## 3. 顺序、依赖与里程碑

| 里程碑ID | 包含任务ID | 进入条件 | 完成条件 | 依赖 | 可并行任务 | 阻塞升级 | 责任人 |
|---|---|---|---|---|---|---|---|
| MS-001 | TASK-001, TASK-002 | TDD 和 Plan 批准 | 数据模型、迁移和身份目录测试通过 | NONE | TASK-001 与目录接口骨架可局部并行 | schema 或身份语义不成立立即停止 B2 | Workflow tech lead |
| MS-002 | TASK-003, TASK-004 | MS-001 完成 | 参与 API、声明和任务投影一致性通过 | MS-001 | API 契约与任务动作测试可并行 | 任一租户或操作权限越界立即停止 B3 | Workflow tech lead |
| MS-003 | TASK-005, TASK-006 | MS-001 完成且 MS-002 无边界缺陷 | 自动派单后端和设计器配置闭环 | MS-001, MS-002 | 后端策略稳定后前端可并行 | 空候选未回滚或并发不严格立即停止 B4 | Workflow tech lead |
| MS-004 | TASK-007, TASK-008, TASK-009 | MS-002、MS-003 完成 | Provider 候选接口、文档、全量定向门禁和证据完成 | MS-002, MS-003 | Provider 后端、前端和文档验证可并行 | 任何跨域请求、403 或 L3 必需门禁失败保持未完成 | Delivery owner |

## 4. 验证计划

| 验证ID | 测试或验收ID | 任务ID | 验证层级 | 命令或步骤 | 环境 | 测试数据 | 权限或租户边界 | 预期结果 | 证据路径 | 责任人 | 失败处理 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| VAL-001 | TC-006 | TASK-001 | migration/集成 | WorkflowMigrationContractTest、WorkflowMigrationUpgradeIntegrationTest | H2 MySQL mode/隔离库 | V2 START/COMPLETE、username-only 样本 | 双租户历史数据 | V3 schema/索引正确，可证明 userId 回填，未知身份不授权 | target 测试报告 | dev | migration 或回填失败阻断 |
| VAL-002 | TC-002, TC-003 | TASK-002 | 单元/集成 | 候选目录定向测试 | H2 MySQL mode | userId/username/角色/岗位/组织/负责人及禁用成员 | 当前租户与其它租户混合 | 仅返回当前租户启用用户并稳定排序 | target 测试报告 | dev | 任一跨租户或误选阻断 |
| VAL-003 | TC-001, TC-002, TC-005 | TASK-003 | API/集成 | workflow api surface、controller、remote 和 service tests | Spring test/H2 | A/B/C、多业务坐标、声明集合 | LOGIN/PERMISSION、双租户 | access/page/declare/start 契约和分页成立 | target 测试报告 | dev | API 或权限漂移阻断 |
| VAL-004 | TC-001, TC-002 | TASK-004 | 运行时集成 | WorkflowTaskRuntimeServiceIntegrationTest、WorkflowProcessServiceImplIntegrationTest | Spring test/H2 | start/claim/unclaim/transfer/complete/reject/return | 历史参与人无任务处理权 | 每个动作后投影一致且失败回滚 | target 测试报告 | dev | 漏动作或授权扩大阻断 |
| VAL-005 | TC-003, TC-004 | TASK-005 | 运行时/MySQL 并发 | 自动派单定向测试和 Testcontainers 并发测试 | H2+MySQL | A/B/C、空组、禁用成员、12 并发任务 | 单租户节点及跨租户对照 | 严格轮询、游标/assignee/审计同事务、空候选零提交 | target 测试报告与序列摘要 | dev/qa | 伪轮询、死锁或孤儿任务阻断 |
| VAL-006 | TC-007 | TASK-006 | 前端包 | `pnpm --filter @mango/workflow test && pnpm --filter @mango/workflow typecheck && pnpm --filter @mango/workflow build` | 本地 Node/pnpm | 新旧 designerJson | 仅定义编辑权限影响配置 | CLAIM 默认和 AUTO 序列化/校验正确 | Vitest/tsc/build 输出 | dev | 任一失败阻断 |
| VAL-007 | TC-008 | TASK-007 | 文档/静态 | capability、API surface、README 引用和 markdown 检查 | 本地仓库 | 当前代码契约 | 权限与租户说明必须显式 | M08、M09、M14 通过 | checker 输出 | tech-lead | 漏同步阻断 |
| VAL-008 | TC-001 至 TC-008 | TASK-008 | L3 综合 | backend 定向模块测试、frontend 测试/类型/build、四类文档 checker 和 check-document-set | 本地隔离环境 | 全部上述数据 | 双租户、非参与、历史参与、并发 | 无未解释失败；证据逐项映射 | mango-docs/evidence/issue-732-workflow-participation-auto-assignment/verification.md | qa | 保留失败事实并修复后重跑，不弱化门禁 |
| VAL-009 | TC-009 | TASK-009 | API/前端/浏览器 | Provider JUnit、API surface、Workflow Vitest/build；启动隔离服务后用 Chromium 检查网络与五类控件 | Mango CLI 隔离工作区 | 用户、角色、岗位、组织、字典各至少一项 | 用户仅有 `workflow:definition:*`，请求无 tenantId | 只调用 `/workflow/definitions/designer-options`，无 403/4xx/5xx，五类可见；AUTO 保存成功 | target 报告、Vitest 输出、Playwright trace/screenshot | dev/qa | 任一跨域调用、吞错或权限扩大阻断 |

## 5. 数据库实施步骤

| 数据步骤ID | 技术设计ID | 环境 | 前置检查 | 动作 | 顺序 | 数据备份或回填 | 验证 | 失败停止条件 | 补偿 | 责任人 |
|---|---|---|---|---|---|---|---|---|---|---|
| DATA-001 | DB-001, DB-002, DB-003 | 开发/测试升级库 | 确认当前 workflow migration 为 V2 且无失败记录 | 应用 V3 新表、索引和安全 INSERT SELECT 回填 | 先建表索引，再回填参与关系，最后记录 Flyway 成功 | 测试库快照；回填只新增可证明行且唯一键幂等 | VAL-001、VAL-008 | DDL、索引、租户或回填数量断言失败 | 修复 migration 前只操作可丢弃测试库；已发布环境只能追加补偿 migration，不改历史 V3 | dev/qa |

## 6. 已启用说明与资产同步计划

| 文档项ID | 技术设计或交付物ID | 目标文档 | 变化 | 责任人 | 完成条件 | 检查命令 | 不适用依据 |
|---|---|---|---|---|---|---|---|
| DOC-001 | DEL-004, IMP-001, IMP-002, IMP-003 | mango/mango-platform/mango-workflow/README.md | 增加参与 API、身份、权限、派单、空候选和升级说明 | Workflow owner | 与 API/配置/migration 一致 | M08、README checker | 适用 |
| DOC-002 | DEL-004, IMP-001 | mango-docs/capabilities/README.md | 近期能力变更和 Workflow 能力入口 | Workflow owner | 可从能力地图定位接入与排障 | M08、M14 | 适用 |
| DOC-003 | DEL-004, IMP-001, IMP-003 | mango-docs/guides/business-integration/workflow-business-approval.md | 业务声明、只读查询、fail-closed 和临时 Port 替换边界 | Workflow owner | 业务消费者无需读取内部表或当前 assignee 猜测 | M08、M14 | 适用 |
| DOC-004 | DEL-003, IMP-002 | mango-ui/packages/workflow/README.md | CLAIM/AUTO 节点配置与兼容默认值 | Frontend owner | 与界面和类型一致 | frontend README/build check | 适用 |
| DOC-005 | DEL-006, IMP-004 | Workflow 后端/前端 README、能力地图、业务审批接入指南 | 增加设计器候选 Provider、接口、权限、可信租户和失败语义 | Workflow owner | 文档明确不得追加跨域菜单权限且自定义 Bean 可替换默认 Provider | M08、M14、document-set | 适用 |

## 7. 风险、阻塞与例外

| 风险ID | 风险等级 | 类型 | 触发条件 | 影响 | 预防 | 应对 | 责任人 | 截止时间 | 状态 | 例外ruleId | 例外批准与到期 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| RISK-001 | L3 | RISK | 参与投影遗漏任务动作或错误保持 active | 合法用户不可读或旧办理人错误可读 | 统一 reconcile/record 服务和全部动作集成测试 | 修复后追加补偿 migration，不放宽任务权限 | Workflow tech lead | 2026-08-28 | CLOSED | NONE | NONE |
| RISK-002 | L3 | RISK | username/userId/memberId 混用 | 跨用户授权或候选误选 | stable userId 归一化、当前租户启用成员断言 | 无法证明的旧 username 不授权 | Workflow tech lead | 2026-08-28 | CLOSED | NONE | NONE |
| RISK-003 | L3 | RISK | 并发游标锁语义在 H2 通过但 MySQL 不成立 | 伪轮询、死锁或孤儿任务 | 必须执行 MySQL 并发测试，短事务和确定性锁顺序 | 失败则不交付 AUTO，保持 CLAIM 默认 | QA owner | 2026-08-28 | CLOSED | NONE | NONE |
| RISK-004 | L2 | RISK | 旧工作流原型被误当实现基线 | 带入过时 SQL 和错误策略语义 | 新 worktree 基于 origin/main，旧 worktree 只作参考 | 验证后再单独清理旧 worktree | Delivery owner | 2026-08-28 | CLOSED | NONE | NONE |
| RISK-005 | L3 | RISK | 通过追加跨域权限或前端回退绕过 403 | Workflow 菜单获得过宽权限或再次耦合平台 REST | Workflow 自有 API、源码 URL 断言和最小权限浏览器验收 | 发现跨域请求立即阻断并回到 Provider 边界 | Workflow tech lead | 2026-08-29 | CLOSED | NONE | NONE |

## 8. 实施追踪矩阵

| 上游设计ID | 交付物ID | 任务ID | 验证ID | 里程碑数据文档或风险项ID | 覆盖说明 |
|---|---|---|---|---|---|
| DEC-001, DEC-002, DEC-005, MOD-001, MOD-002, MOD-003, DM-001, FLOW-001, FLOW-002, FLOW-003, API-001, API-002, API-003, API-004, DB-001, DB-003, SEC-001, SEC-002, SEC-004, ERR-001, ERR-004, IMP-001, IMP-003 | DEL-001 | TASK-001, TASK-002, TASK-003, TASK-004 | VAL-001, VAL-002, VAL-003, VAL-004 | MS-001, MS-002, DATA-001, DOC-001, DOC-002, DOC-003, RISK-001, RISK-002 | 覆盖参与投影、公开契约、声明、任务一致性、租户和历史回填 |
| DEC-003, DEC-006, DEC-007, MOD-002, DM-002, DM-003, FLOW-004, DB-002, SEC-003, ERR-002, ERR-003, IMP-002 | DEL-002 | TASK-001, TASK-002, TASK-005 | VAL-001, VAL-002, VAL-005 | MS-001, MS-003, DATA-001, DOC-001, RISK-002, RISK-003 | 覆盖运行时候选目录、严格轮询、空候选回滚和并发 |
| MOD-004, API-005, UI-001 | DEL-003 | TASK-006 | VAL-006 | MS-003, DOC-004, RISK-004 | 覆盖设计器配置、默认兼容和序列化 |
| MOD-005, IMP-001, IMP-002, IMP-003 | DEL-004 | TASK-007 | VAL-007 | MS-004, DOC-001, DOC-002, DOC-003, DOC-004 | 覆盖已启用能力与接入文档同步 |
| TC-001, TC-002, TC-003, TC-004, TC-005, TC-006, TC-007, TC-008 | DEL-005 | TASK-008 | VAL-008 | MS-004, RISK-001, RISK-002, RISK-003, RISK-004 | 覆盖全部系统验收和 L3 证据闭环 |
| DEC-004, API-006, SEC-005, ERR-005, ERR-006, UI-002, IMP-004, TC-009 | DEL-006 | TASK-009 | VAL-009 | MS-004, DOC-005, RISK-005 | 覆盖设计器候选 Provider、Workflow 权限、显式失败、前端去耦和浏览器验收 |

## 9. 阶段判定与审批

| 检查项 | 结果 | 证据 |
|---|---|---|
| 实施计划 checker | PASS | `node mango-pmo/tools/check-implementation-plan.mjs --document mango-docs/designs/issue-732-workflow-participation-auto-assignment/implementation-plan.md` |
| 生命周期 handoff | PASS | TDD-WORKFLOW-732 已审批且 upstreamDocumentHash 与文件 SHA-256 一致 |
| 依赖图 | PASS | MS-001→MS-002→MS-003→MS-004；数据库/身份先于 API/运行时，后端策略先于前端和最终文档 |
| 未关闭阻断数量 | 0 | RISK-001 至 RISK-004 均有预防和失败停止条件，无例外 |
| 实施审批 | APPROVED | `review/APPROVAL.md` |
