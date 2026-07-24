---
documentId: PLAN-NOTICE-641-642
documentType: implementation-plan
pmoVersion: 1.3.5
schemaRevision: 1
riskLevel: L3
riskAssessmentEvidence: requirement=L3，附件送达、发件身份和 Secret 安全属于核心业务与安全边界；solution=L3，实施跨 File、Notice 多个 Maven 模块、Resource、数据库迁移和 npm 前端包；final=max(requirement,solution)
status: APPROVED
action: NEXT
owner: Mango Notice 实施负责人
approver: HardyDou
approvalEvidence: review/APPROVAL.md
upstreamDocumentId: TDD-NOTICE-641-642
upstreamDocumentHash: 3eb9fb3b91687546fcdc94cae6de9039e08400d098c166e8d450cd3d48cc88a8
---

# Notice 邮件附件与路由账号组实施计划

## 1. 实施目标、范围与交付物

| 交付物ID | 技术设计ID | 交付物 | 路径或模块 | 完成状态定义 | 验收来源 | 不处理边界 |
|---|---|---|---|---|---|---|
| DEL-001 | DEC-001, DEC-002, MOD-002, MOD-004, FLOW-001, ERR-001, ERR-002, IMP-001 | EMAIL 附件读取、限制、MIME、SMTP 和安全摘要 | mango-notice-support、mango-notice-channel-email | 无附件兼容；允许附件完整进入 MIME；失败不假成功；测试覆盖内容和限制 | SAC-001, SAC-002, TC-001 | 不保证 SMTP 接受后的最终收件箱策略 |
| DEL-002 | DEC-003, DEC-004, DEC-007, MOD-003, DM-002, DM-003, FLOW-002, DB-001, DB-002, DB-003, SEC-002, ERR-003 | 渠道稳定身份、Secret 分层、Resource 合并、标签模型和 migration | mango-notice-core、resource handler、db migration | Resource 无明文、同步不清 Secret、状态可计算、标签关系可审计、V2 可升级 | SAC-003, TC-002, TC-003 | 不建设通用 Secret 平台 |
| DEL-003 | DEC-005, DEC-006, MOD-001, MOD-003, MOD-005, DM-004, FLOW-003, FLOW-004, API-001, API-002, API-003, API-004, API-005, API-006, API-007, DB-004, SEC-003, ERR-004, IMP-002, IMP-003 | 三模式公开契约、引用保护、候选排序和故障切换 | notice-api/core/starter/starter-remote | EXACT/TAG/AUTO 互斥；TAG 不回退；旧数据兼容；实际账号可审计 | SAC-004, SAC-005, TC-003 | 不改变任务调度和其它渠道协议 |
| DEL-004 | MOD-006, UI-001, UI-002, UI-003 | 渠道和消息配置管理交互 | mango-ui/packages/notice | 来源、Secret、标签、影响和三模式可维护；加载/空/失败/权限状态完整 | SAC-006, TC-004 | 不重做 Notice 页面整体视觉体系 |
| DEL-005 | IMP-001, IMP-002, IMP-003, TC-001 至 TC-004 | 测试、README、Resource schema、升级/回退和验证证据 | 模块测试目录、Notice README、mango-docs | 定向验证可重复，公开能力和升级边界可供消费者执行 | SAC-001 至 SAC-006 | 不执行发布、commit、push 或 PR |

## 2. 工作分解

| 任务ID | 技术设计ID | 交付物ID | 责任角色 | 路径或模块 | 前置任务 | 具体动作 | 完成标准 | 验证ID | 实施批次 | 状态 |
|---|---|---|---|---|---|---|---|---|---|---|
| TASK-001 | DEC-001, DEC-002, MOD-002, MOD-004, DM-001, FLOW-001, ERR-001, ERR-002 | DEL-001 | Dev | notice-support、channel-email | NONE | 扩展安全结果摘要；引入可选 File provider；实现附件策略、受限读取、MIME 和错误映射；补 MIME/SMTP 测试 | TC-001 全部场景通过且无附件行为不变 | VAL-001 | B1 EMAIL 附件 | PLANNED |
| TASK-002 | DEC-003, DEC-004, DEC-007, DB-001, DB-002, DB-003 | DEL-002 | Dev、DBA | notice-core entity/mapper/resource/migration | TASK-001 | 新增稳定编码、Secret 分层、来源、标签实体和 V2；更新 V1；实现 Resource 受控合并和完整性计算 | migration、Resource 与 Secret 集成测试通过 | VAL-002, VAL-003 | B2 数据与 Resource | PLANNED |
| TASK-003 | DEC-005, DEC-006, MOD-001, MOD-003, MOD-005, FLOW-003, FLOW-004, API-001, API-002, API-003, API-004, API-005, API-006, API-007, DB-004, SEC-003, ERR-004 | DEL-003 | Dev | notice-api/core/starter/remote | TASK-002 | 按 API-001、API-002、API-003、API-004、API-005、API-006、API-007 完成公开契约与校验；按 FLOW-003、FLOW-004 完成标签维护、引用影响、模板互斥保存和候选发送 | API parity、三模式、主备、无候选和兼容测试通过 | VAL-002, VAL-004 | B3 路由与 API | PLANNED |
| TASK-004 | MOD-006, UI-001, UI-002, UI-003 | DEL-004 | Dev | mango-ui/packages/notice | TASK-003 | 扩展类型/API；完成渠道来源/标签/Secret 状态和模板三模式；补语义锚点和组件测试 | lint/typecheck/test/build 通过，定向 UI 业务与错误状态可观察 | VAL-005, VAL-006 | B4 管理前端 | PLANNED |
| TASK-005 | IMP-001, IMP-002, IMP-003, TC-001 至 TC-004 | DEL-005 | Dev、QA | README、Resource schema、tests、evidence | TASK-001, TASK-002, TASK-003, TASK-004 | 更新使用/升级/回退说明；执行后端、迁移、前端和 UI 定向验证；记录阻塞与风险 | 所有启用验证有真实结果，无未说明变更和敏感证据 | VAL-001 至 VAL-007 | B5 联合回归 | PLANNED |

## 3. 顺序、依赖与里程碑

| 里程碑ID | 包含任务ID | 进入条件 | 完成条件 | 依赖 | 可并行任务 | 阻塞升级 | 责任人 |
|---|---|---|---|---|---|---|---|
| MS-001 | TASK-001 | FULL 文档链批准 | EMAIL 附件与兼容测试通过 | NONE | NONE | File 契约或 MIME 无法稳定验证时停止并修订 TDD | Notice Dev |
| MS-002 | TASK-002, TASK-003 | MS-001 完成 | V2、Resource/Secret、标签和路由 API/服务测试通过 | MS-001 | TASK-002 内 migration 与模型可按依赖顺序实施 | 旧数据无法无损迁移或 Secret 有泄露风险时停止 | Tech Lead、DBA |
| MS-003 | TASK-004 | MS-002 完成且 API 稳定 | 管理前端类型、组件和构建通过 | MS-002 | NONE | API 契约差异回到 TASK-003，不在页面绕过 | Frontend owner |
| MS-004 | TASK-005 | MS-001 至 MS-003 完成 | 联合回归、能力说明和工作区检查完成 | MS-001, MS-002, MS-003 | 后端/前端定向验证可分别执行 | 任一假成功、越界账号、Secret 泄露或兼容回归阻断交付 | Notice 实施负责人 |

## 4. 验证计划

| 验证ID | 测试或验收ID | 任务ID | 验证层级 | 命令或步骤 | 环境 | 测试数据 | 权限或租户边界 | 预期结果 | 证据路径 | 责任人 | 失败处理 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| VAL-001 | TC-001 | TASK-001, TASK-005 | UNIT/INTEGRATION | 对 channel-email 及依赖的直接修改模块执行 Maven test/verify | Java 21、测试 SMTP | 无附件、中文名、单双附件、超限/类型/超时/读取失败 | tenant 1/2 与不可读文件 | MIME 内容一致、限制生效、失败不假成功、无附件兼容 | `.runtime/issues-641-642/backend-email/` 与最终摘要 | Dev、QA | 定位 surefire；不弱化断言 |
| VAL-002 | TC-002, TC-003 | TASK-002, TASK-003, TASK-005 | UNIT/INTEGRATION | 对 notice-core 直接修改模块执行 Maven test/verify | Java 21、H2/测试数据库 | Resource、Secret、标签主备、旧模板 | tenant 1/2 | 重复同步保留 Secret、路由不越界、模式兼容 | `.runtime/issues-641-642/backend-core/` 与最终摘要 | Dev、QA | 任一覆盖/越界阻断 |
| VAL-003 | TC-002, TC-003 | TASK-002, TASK-005 | DB | 在 1.0.25 等价 schema 执行 V2并校验，再验证 fresh V1 | 独立测试数据库 | 旧人工账号、旧精确/空模板、异常 JSON | 独立测试租户 | 编码和 routeMode 正确回填，数据不丢失 | `.runtime/issues-641-642/migration/` 与最终摘要 | DBA、QA | 失败停止，不修改共享库 |
| VAL-004 | TC-002, TC-003 | TASK-003, TASK-005 | API | 执行 Notice MVC/Feign/服务入口定向测试 | Spring test | 渠道/标签/模板 fixture | notice:channel/business 权限与 tenant 1/2 | Secret 不返回、引用保护、API parity 和错误语义正确 | `.runtime/issues-641-642/api/` 与最终摘要 | QA | 修复服务契约，不以前端绕过 |
| VAL-005 | TC-004 | TASK-004, TASK-005 | STATIC/COMPONENT | 在 @mango/notice 执行 lint、typecheck、test、build | Node/pnpm workspace | API fixture | 多权限组合 | 类型、组件和生产构建通过 | `.runtime/issues-641-642/frontend/` 与最终摘要 | Frontend owner | 修复包边界和真实类型 |
| VAL-006 | TC-004 | TASK-004, TASK-005 | UI/E2E | 启动 slot 5 服务，执行渠道与消息配置定向 Playwright，检查 console/network/截图 | backend 18005、frontend 30005、独立数据库 | 独立 E2E 账号、账号/标签/模板数据 | 当前测试租户和 view/edit 权限 | 三模式、Secret 状态、引用影响及错误状态符合 SRS | `mango-docs/evidence/2026-07-24-issues-641-642/` | QA | 环境不可用标 BLOCKED，不伪造通过 |
| VAL-007 | TC-001, TC-002, TC-003, TC-004 | TASK-005 | STATIC/DOC | 运行 capability docs、document lifecycle、diff 和敏感信息检查 | 任务 worktree | 文档和 Git diff | 不适用业务账号 | README、schema、升级/回退与实现一致，证据无 Secret | 同任务 evidence 摘要 | Notice owner | 修正文档或阻断交付 |

## 5. 数据库实施步骤

| 数据步骤ID | 技术设计ID | 环境 | 前置检查 | 动作 | 顺序 | 数据备份或回填 | 验证 | 失败停止条件 | 补偿 | 责任人 |
|---|---|---|---|---|---|---|---|---|---|---|
| DATA-001 | DB-001 至 DB-004, DEC-007 | 开发/测试独立数据库 | 记录旧 schema 和 fixture，不连接共享业务库 | 编写 V2 与 fresh V1，执行旧库升级和新库形成 | TASK-002 后半段 | 旧 configCode、routeMode 和已知 Secret 安全回填；异常 JSON 保留并标记不完整 | VAL-003 | 任一数据丢失、Secret 出现在日志或模式错误 | 丢弃一次性测试库，修复 migration 后重建；正式升级前备份 | DBA |

## 6. 已启用说明与资产同步计划

| 文档项ID | 技术设计或交付物ID | 目标文档 | 变化 | 责任人 | 完成条件 | 检查命令 | 不适用依据 |
|---|---|---|---|---|---|---|---|
| DOC-001 | IMP-001, DEL-001 | `mango/mango-platform/mango-notice/README.md` | EMAIL 附件配置、限制、失败和使用示例 | Notice owner | 字段、默认值和测试入口与代码一致 | capability docs 定向检查 | 适用 |
| DOC-002 | IMP-002, IMP-003, DEL-002, DEL-003 | Notice README 与 Resource schema 说明 | configCode、routeTags、secretRefs、三模式、升级与回退 | Notice owner | 新旧调用和安全边界可直接执行 | capability docs 与 Resource tests | 适用 |
| DOC-003 | DEL-004 | `mango-ui/packages/notice/README.md` | 管理页面三模式和 Secret 补录说明 | Frontend owner | npm 消费者理解新增字段和兼容 | package docs/build check | 适用 |
| DOC-004 | DEL-005 | 本目录和最终 evidence | 生命周期、命令、结果、阻塞和剩余风险 | Dev、QA | checker 与证据索引通过 | document lifecycle/acceptance checker | 适用 |

## 7. 风险、阻塞与例外

| 风险ID | 风险等级 | 类型 | 触发条件 | 影响 | 预防 | 应对 | 责任人 | 截止时间 | 状态 | 例外ruleId | 例外批准与到期 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| RISK-001 | L3 | RISK | 附件限制或读取关闭不正确 | OOM、泄漏或假成功 | 硬上限、上限加一读取、受控超时和失败测试 | 立即停止并回退附件路径 | Notice Dev | 交付前 | CLOSED | NONE | 无例外；控制已写入 TASK-001/VAL-001 |
| RISK-002 | L3 | RISK | Resource 同步清空或泄露 Secret | 全部账号不可用或凭据泄露 | 分层列、受控字段合并、敏感拒绝和重复同步测试 | 停止同步并恢复测试备份，修复后重跑 | Tech Lead | 交付前 | CLOSED | NONE | 无例外；控制已写入 TASK-002/VAL-002 |
| RISK-003 | L3 | RISK | TAG 候选越界或静默回退 | 错误发件身份 | 模式硬过滤、租户/渠道断言和无候选测试 | 阻断交付并修复路由 | Tech Lead | 交付前 | CLOSED | NONE | 无例外；控制已写入 TASK-003/VAL-002 |
| RISK-004 | L3 | RISK | 增量 migration 破坏旧配置 | 升级失败或路由漂移 | 等价旧 schema fixture、备份和兼容回填 | 停止升级；正式环境从备份恢复 | DBA | 交付前 | CLOSED | NONE | 无例外；控制已写入 DATA-001/VAL-003 |

## 8. 实施追踪矩阵

| 上游设计ID | 交付物ID | 任务ID | 验证ID | 里程碑数据文档或风险项ID | 覆盖说明 |
|---|---|---|---|---|---|
| DEC-001, DEC-002, MOD-002, MOD-004, DM-001, FLOW-001, SEC-001, ERR-001, ERR-002, IMP-001, TC-001 | DEL-001 | TASK-001, TASK-005 | VAL-001, VAL-007 | MS-001, MS-004, DOC-001, RISK-001 | 覆盖附件读取、限制、MIME、SMTP、审计、兼容与说明 |
| DEC-003, DEC-004, DEC-007, MOD-003, DM-002, DM-003, FLOW-002, DB-001, DB-002, DB-003, SEC-002, ERR-003, IMP-002, TC-002 | DEL-002 | TASK-002, TASK-005 | VAL-002, VAL-003, VAL-007 | MS-002, MS-004, DATA-001, DOC-002, RISK-002, RISK-004 | 覆盖 Secret、Resource、标签数据和迁移 |
| DEC-005, DEC-006, MOD-001, MOD-005, DM-004, FLOW-003, FLOW-004, API-001, API-002, API-003, API-004, API-005, API-006, API-007, DB-004, SEC-003, ERR-004, IMP-003, TC-003 | DEL-003 | TASK-003, TASK-005 | VAL-002, VAL-003, VAL-004, VAL-007 | MS-002, MS-004, DATA-001, DOC-002, RISK-003, RISK-004 | 覆盖公开契约、三模式、引用保护、切换和兼容 |
| MOD-006, UI-001, UI-002, UI-003, TC-004 | DEL-004 | TASK-004, TASK-005 | VAL-005, VAL-006 | MS-003, MS-004, DOC-003 | 覆盖管理前端、组件、构建和 UI 验收 |
| IMP-001, IMP-002, IMP-003, TC-001, TC-002, TC-003, TC-004 | DEL-005 | TASK-005 | VAL-001 至 VAL-007 | MS-004, DOC-001, DOC-002, DOC-003, DOC-004 | 覆盖联合回归、能力说明、证据和剩余风险 |

## 9. 阶段判定与审批

| 检查项 | 结果 | 证据 |
|---|---|---|
| 实施计划 checker | PASS | `node mango-pmo/tools/check-implementation-plan.mjs --document mango-docs/designs/issues-641-642-notice-delivery/implementation-plan.md` |
| 生命周期 handoff | PASS | BRD、SRS、TDD、Plan 均 APPROVED/NEXT 且 hash 精确匹配 |
| 依赖图 | PASS | TASK-001→TASK-002→TASK-003→TASK-004→TASK-005；MS-001→MS-002→MS-003→MS-004，无循环 |
| 未关闭阻断数量 | 0 | RISK-001 至 RISK-004 均有已确认控制和停止条件，无例外 |
| 实施审批 | APPROVED | `review/APPROVAL.md` |
