---
documentId: PLAN-WORKFLOW-DEBT
documentType: implementation-plan
pmoVersion: 1.2.4
schemaRevision: 1
riskLevel: L3
riskAssessmentEvidence: requirement=L3，审批状态、权限、事件和初始化属于核心链路；solution=L3，一次性调整四层契约、Flowable 持久化和资源装载；final=max(requirement,solution)
status: APPROVED
action: NEXT
owner: Mango Workflow 实施负责人
approver: HardyDou
approvalEvidence: review/PLAN-WORKFLOW-DEBT.md
upstreamDocumentId: TDD-WORKFLOW-DEBT
upstreamDocumentHash: 1647aa988aeac6afac9597861b1090bbcdd73ef9579888e2dc2a1edb9efa5c64
---

# Workflow 历史债务治理实施计划

## 1. 实施目标、范围与交付物

| 交付物ID | 技术设计ID | 交付物 | 路径或模块 | 完成状态定义 | 验收来源 | 不处理边界 |
|---|---|---|---|---|---|---|
| DEL-001 | DEC-002, TC-001 至 TC-005 | 改前/改后统一测试基线 | Workflow tests、`mango-docs/evidence/baselines/workflow-architecture/latest` | 先修复测试基础设施并补特征测试，生产代码未改时记录 before；改后同入口全绿 | SAC-001 至 SAC-005 | 不堆重复参数用例，不 mock 被测对象 |
| DEL-002 | DEC-003 至 DEC-006, DEC-009 | API/Core/Starter/Remote 最终架构 | Workflow 四个 Maven 子模块 | Java/HTTP/Feign 契约保持，Service/Entity/Mapper/Controller 规范，845 条正式债务为 0 | SAC-001 至 SAC-003 | 不新增业务接口或改变前端协议 |
| DEL-003 | DEC-007 | 单一纯 DDL V1 与正式必需 metadata | core migration/init | 新库最终 schema 等价，Flyway 无 DML，引擎 metadata 幂等且启动成功 | SAC-004 | 不支持历史库原地升级 |
| DEL-004 | DEC-008 | 正式与 Demo 资源分层 | starter `META-INF/mango` | 正式默认；三套 Demo 仅显式同步且可发布 | SAC-005 | 不初始化运行态申请、实例和任务 |
| DEL-005 | IMP-001 至 IMP-004 | README、设计、能力说明和证据 | Workflow README、前端 README、capability map、docs | 初始化政策、接入契约、验证命令和结果可复核 | SAC-001 至 SAC-005 | 不复制 PMO 长期规范 |

## 2. 工作分解

| 任务ID | 技术设计ID | 交付物ID | 责任角色 | 路径或模块 | 前置任务 | 具体动作 | 完成标准 | 验证ID | 实施批次 | 状态 |
|---|---|---|---|---|---|---|---|---|---|---|
| TASK-001 | DEC-002, TC-001 至 TC-003 | DEL-001 | Dev、QA | Workflow tests | NONE | 修复 H2 claim_status schema 和暂存变量 fixture；补 API 指纹、关键 HTTP/Feign、事件/快照与错误边界测试；生产代码未改时运行 before | 同一测试入口全绿、测试质量检查通过、契约指纹记录 | VAL-001, VAL-002 | B1 基线 | PLANNED |
| TASK-002 | DEC-003, DEC-006 | DEL-002 | Dev | workflow-api | TASK-001 | 为 API 复合入参加 `@Valid`、标量加 constraint，修正协议类型白名单并保持 JSON/Java 语义 | API 债务 0，契约测试通过 | VAL-001, VAL-003 | B2 API | PLANNED |
| TASK-003 | DEC-003 至 DEC-005, DEC-009 | DEL-002 | Dev | workflow-core | TASK-002 | Service 去 R、Require 使用 WorkflowCode、实现类无 Impl；Entity/Mapper 规范命名；更新引用但保持事务/查询/事件 | Core 债务 0，真实 Flowable 集成测试通过 | VAL-001, VAL-003 | B3 Core | PLANNED |
| TASK-004 | DEC-006 | DEL-002 | Dev | starter、starter-remote | TASK-003 | Controller/Feign 完整实现 API，显式 binding/OpenAPI/Validated，Controller 只 `R.ok(service)` | Starter/Remote 债务 0，路由指纹不变 | VAL-001, VAL-003 | B4 Adapter | PLANNED |
| TASK-005 | DEC-007, TC-004 | DEL-003 | Dev、DBA | core migration/init | TASK-001 | 固化 V1-V4 最终 schema；生成单一最终态 V1；将 ACT_GE_PROPERTY 必需值迁入引擎前幂等初始化 | Flyway 仅 DDL，新库 schema 等价，Flowable 启动 | VAL-004 | B5 DB | PLANNED |
| TASK-006 | DEC-008, TC-005 | DEL-004 | Dev、QA | starter resources | TASK-005 | 删除默认 Sample initializer/properties；把三套示例转换为模块 Demo 声明；保留正式 domain/node/menu | 默认零 Demo，显式 Demo 三套声明完整且 INIT_ONLY | VAL-004, VAL-005 | B6 Resource | PLANNED |
| TASK-007 | IMP-001 至 IMP-004 | DEL-001 至 DEL-005 | Dev、QA | Workflow 全模块、docs | TASK-002 至 TASK-006 | 运行 after、定向架构与静态门禁；新库启动和 API/示例冒烟；更新报告与文档；合并最新 main 后 PR | 同组测试全绿、845→0、新增静态 0、服务可用、PR required check 通过 | VAL-001 至 VAL-005 | B7 收口 | PLANNED |

## 3. 顺序、依赖与里程碑

| 里程碑ID | 包含任务ID | 进入条件 | 完成条件 | 依赖 | 可并行任务 | 阻塞升级 | 责任人 |
|---|---|---|---|---|---|---|---|
| MS-001 | TASK-001 | 四阶段规格批准 | 生产代码未改的 before 全绿且指纹可复核 | NONE | NONE | 既有失败先修测试基础设施，不以改生产逻辑掩盖 | QA 负责人 |
| MS-002 | TASK-002 至 TASK-004 | MS-001 完成 | 四层正式架构债务为 0，公共契约与行为测试一致 | MS-001 | 机械类型重命名可与协议注解整理交错，但单一实施者统一收口 | 任一公共指纹或业务结果变化立即停止 | Tech Lead |
| MS-003 | TASK-005, TASK-006 | MS-001 完成 | 新库结构、必需 metadata、正式/Demo 边界和启动全部通过 | MS-001 | Demo 声明可在 V1 固化后整理 | 引擎启动、schema 或资源依赖不一致即丢弃新库并停止 | DBA、Tech Lead |
| MS-004 | TASK-007 | MS-002、MS-003 完成 | after、架构、启动、API、文档和 PR 全部满足 | MS-002, MS-003 | NONE | required check 失败只处理当前最新提交证据，不重复旧提交失败 | Workflow 实施负责人 |

## 4. 验证计划

| 验证ID | 测试或验收ID | 任务ID | 验证层级 | 命令或步骤 | 环境 | 测试数据 | 权限或租户边界 | 预期结果 | 证据路径 | 责任人 | 失败处理 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| VAL-001 | TC-001, TC-002 | TASK-001 至 TASK-004, TASK-007 | UNIT/INTEGRATION | `mvn -f mango/mango-platform/mango-workflow/pom.xml clean test` | Java 21、独立 H2 | 定义、实例、任务、申请 fixture | tenant 1、admin/发起人/审批人 | 同组用例全绿，状态/事件/快照一致 | `mango-docs/evidence/baselines/workflow-architecture/latest/report.md` | Dev、QA | 查对应 surefire，不删除或弱化测试 |
| VAL-002 | TC-003 | TASK-001, TASK-004 | API | API/Controller/Feign 契约测试 | JUnit/MockMvc/反射 | 5 API、全部 endpoints | 保留权限注解与参数 binding | 指纹、路径、verb、泛型、权限不变 | 同上 | QA | 任一差异阻断 |
| VAL-003 | TC-003 | TASK-002 至 TASK-004, TASK-007 | STATIC | 定向 architecture verify；`node mango-pmo/tools/test-quality-check.mjs --base origin/main` | 仅 Workflow 四模块 | PR diff | 不适用业务账号 | 正式债务 0、新增静态/测试问题 0 | 同上 | Tech Lead | 不使用 baseline 抵消新问题 |
| VAL-004 | TC-004 | TASK-005, TASK-007 | DB/STARTUP | workspace 新 MySQL 执行 V1 并启动 backend | 独立新库 | 空库+正式必需 metadata | 默认不启用 Demo | schema 等价、Flyway 无 DML、health UP、Flowable 可创建流程 | 同上 | DBA、QA | 丢弃新库修复后重建 |
| VAL-005 | TC-005 | TASK-006, TASK-007 | RESOURCE/API | 显式启用 Demo 后重启/同步并调用定义、发起、任务接口 | 第二个新库或清理后新库 | 三套示例流程 | tenant 1、admin | 三套定义已发布、重复同步幂等、关键 API 可用 | 同上 | QA | 资源 key/依赖不一致则阻断 |

## 5. 数据、升级、发布与回滚步骤

| 发布步骤ID | 技术设计ID | 环境 | 前置检查 | 动作 | 顺序 | 数据备份或回填 | 兼容窗口 | 验证 | 失败停止条件 | 回滚或补偿 | 责任人 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| REL-001 | DB-001, DB-002 | before 独立 workspace | V1-V4 可读，旧服务未修改 | 记录最终 schema、索引和 ACT_GE_PROPERTY key/value | 第 1 步 | 只读基线，不回填 | 仅任务周期 | schema/metadata 指纹可复核 | 无法取得最终态事实 | 不修改旧库，停止实施 | DBA |
| REL-002 | DB-001, DB-002, DB-003, DB-004 | 开发分支 | before 已完成 | 折叠 V1、实现 metadata initializer 和 Demo declaration | 第 2 步 | 新库政策，无旧库回填 | 无双版本窗口 | TC-004, TC-005 | 静态契约或单测失败 | 丢弃分支新库并修复 | Dev |
| REL-003 | DB-001, DB-002, DB-003 | 默认模式新库 | 单一 V1 和正式资源编译通过 | 启动 backend 并核对正式模式 | 第 3 步 | 使用独立新库，不备份共享数据 | 当前发布版本 | VAL-004 | health 非 UP、metadata 缺失或出现 Demo | 停服务并删除新库 | QA |
| REL-004 | DB-004 | 显式 Demo 新库 | 默认模式通过 | 启用 Demo、启动、重复同步并调用示例 API | 第 4 步 | 使用独立新库 | 当前发布版本 | VAL-005 | 三套定义不完整、未发布或重复 | 停服务并删除新库 | QA |
| REL-005 | IMP-001, IMP-002, IMP-003, IMP-004 | PR/main | after、定向架构和新库验证通过 | 同步最新 main、解决真实冲突、提交 PR 并自动合并 | 第 5 步 | 不改共享数据库 | PR required check 期间 | VAL-001 至 VAL-005 | 最新提交 required check 失败 | PR 未合并可关闭；已合并用新提交修复 | Workflow owner |

## 6. 文档与能力同步计划

| 文档项ID | 技术设计或交付物ID | 目标文档 | 变化 | 责任人 | 完成条件 | 检查命令 | 不适用依据 |
|---|---|---|---|---|---|---|---|
| DOC-001 | DEL-002, DEL-003, DEL-004 | `mango/mango-platform/mango-workflow/README.md` | 最终模块边界、V1/必需数据/Demo、测试和接入说明 | Workflow owner | 与实现、路径和命令一致 | capability docs 与定向测试 | 适用 |
| DOC-002 | IMP-003, IMP-004 | `mango-ui/packages/workflow/README.md` | 修正 migration 写菜单等过期事实，指向 Resource Registry 与实际 pageKey | Frontend owner | 不再描述错误初始化来源 | 文档 diff 复核 | 前端生产代码不变，仅更新事实 |
| DOC-003 | IMP-002 至 IMP-004 | `mango-docs/capabilities/capability-map.md` | Workflow 初始化、验证和兼容事实 | Workflow owner | capability-docs check 通过 | PMO capability docs check | 适用 |
| DOC-004 | DEL-001 至 DEL-005 | 本设计与 `mango-docs/evidence/baselines/workflow-architecture/latest` | before/after、债务、启动/API/例外和交接证据 | Dev、QA | delivery contract、baseline、handoff 可复核 | PMO document/delivery checks | 适用 |

## 7. 风险、阻塞与例外

| 风险ID | 风险等级 | 类型 | 触发条件 | 影响 | 预防 | 应对 | 责任人 | 截止时间 | 状态 | 例外ruleId | 例外批准与到期 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| RISK-001 | L3 | RISK | Service 去 R 后契约或错误测试差异 | code/message 或响应变化 | 先冻结 WorkflowCode、R 泛型和错误断言 | 停止并修正 Require/Controller 映射 | Tech Lead | 2026-07-14 | OPEN | NONE | 无例外 |
| RISK-002 | L3 | RISK | Entity/Mapper 重命名后 SQL 或结果差异 | 状态、租户或逻辑删除变化 | 仅机械改类型，表/字段/Wrapper 不变 | 定位引用并恢复原查询语义 | Dev | 2026-07-14 | OPEN | NONE | 无例外 |
| RISK-003 | L3 | RISK | ACT_GE_PROPERTY 初始化晚于 ProcessEngine | 新库服务不可启动 | 引擎前置 initializer+真实新库验证 | 调整 Bean dependency，禁止把 DML 放回 Flyway | Tech Lead | 2026-07-14 | OPEN | NONE | 无例外 |
| RISK-004 | L3 | RISK | Demo 声明与原 designer/form JSON 不一致 | 示例页面或审批节点失效 | 从原 initializer 精确转换并比较 definitionKey/pageKey/JSON | 修正 handler/声明并重建新库 | Workflow owner | 2026-07-14 | OPEN | NONE | 无例外 |
| RISK-005 | L2 | RISK | main 并行变化产生真实冲突 | PR 无法直接合并 | PR 前只同步一次最新 main | 按语义解决后只重跑定向测试 | Workflow owner | PR 前 | OPEN | NONE | 无例外 |

## 8. 实施追踪矩阵

| 上游设计ID | 交付物ID | 任务ID | 验证ID | 里程碑发布文档或风险项ID | 覆盖说明 |
|---|---|---|---|---|---|
| DEC-001, DEC-002, MOD-002, MOD-003, DM-001, FLOW-001, SEC-001, ERR-001, UI-001, TC-001, TC-002, TC-003 | DEL-001 | TASK-001, TASK-007 | VAL-001, VAL-002 | MS-001, MS-004, DOC-004, RISK-001 | 覆盖测试基础设施、特征测试、before/after 和交付证据 |
| DEC-003, DEC-004, DEC-005, DEC-006, DEC-009, API-001, API-002, API-003, API-004, MOD-001, MOD-004, DM-002, DM-003, FLOW-002, FLOW-003, SEC-002, SEC-003, ERR-002, ERR-003, UI-002, UI-003 | DEL-002 | TASK-002, TASK-003, TASK-004, TASK-007 | VAL-001, VAL-002, VAL-003 | MS-002, MS-004, DOC-001, DOC-002, DOC-003, RISK-001, RISK-002 | 覆盖 API/Core/Starter/Remote 最终架构和兼容 |
| DEC-007, DB-001, DB-002, DM-004, FLOW-004, TC-004 | DEL-003 | TASK-005, TASK-007 | VAL-004 | MS-003, MS-004, REL-001, REL-002, REL-003, DOC-001, RISK-003 | 覆盖单一 V1、metadata 和默认新库启动 |
| DEC-008, DB-003, DB-004, MOD-005, SEC-004, ERR-004, UI-004, TC-005 | DEL-004 | TASK-006, TASK-007 | VAL-004, VAL-005 | MS-003, MS-004, REL-004, DOC-001 至 DOC-004, RISK-004 | 覆盖正式/Demo 资源分层和三套示例 |
| IMP-001, IMP-002, IMP-003, IMP-004 | DEL-005 | TASK-007 | VAL-001 至 VAL-005 | MS-004, REL-005, DOC-001 至 DOC-004, RISK-005 | 覆盖文档、main 同步、PR 和发布交接 |

## 9. 阶段判定与审批

| 检查项 | 结果 | 证据 |
|---|---|---|
| 实施计划 checker | PASS | `node mango-pmo/tools/check-implementation-plan.mjs --document mango-docs/designs/workflow-architecture-debt/implementation-plan.md` |
| 生命周期 handoff | PASS | `node mango-pmo/tools/check-lifecycle-handoff.mjs --brd ... --srs ... --tdd ... --plan ... --risk L3 --through plan` |
| 依赖图 | PASS | MS-001→MS-002/MS-003→MS-004，TASK-001→TASK-002 至 TASK-006→TASK-007，无循环 |
| 未关闭阻断数量 | 0 | RISK-001 至 RISK-005 均有停止条件和责任人，不构成实施前阻断 |
| 实施审批 | APPROVED | `review/PLAN-WORKFLOW-DEBT.md` |
