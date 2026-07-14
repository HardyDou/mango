---
documentId: PLAN-NOTICE-DEBT
documentType: implementation-plan
pmoVersion: 1.2.4
schemaRevision: 1
riskLevel: L3
riskAssessmentEvidence: requirement=L3，发送、记录、权限租户和初始化属于核心链路；solution=L3，一次性调整十一子模块、仓内调用方、二十表最终结构与完整应用消费者；final=max(requirement,solution)
status: APPROVED
action: NEXT
owner: Mango Notice 实施负责人
approver: HardyDou
approvalEvidence: review/PLAN-NOTICE-DEBT.md
upstreamDocumentId: TDD-NOTICE-DEBT
upstreamDocumentHash: d52c61d00bb99b7fd917b50aa9feaeaf52bb0378a903baf2aedd8b3f333316c8
---

# Notice 历史债务治理实施计划

## 1. 实施目标、范围与交付物

| 交付物ID | 技术设计ID | 交付物 | 路径或模块 | 完成状态定义 | 验收来源 | 不处理边界 |
|---|---|---|---|---|---|---|
| DEL-001 | DEC-002, TC-001 至 TC-005 | 改前/改后统一测试基线 | Notice 十一个子模块 tests 与 evidence | 生产代码未改时补高价值测试并记录增强 before；治理后运行同一入口 | SAC-001 至 SAC-005 | 不堆重复参数用例，不 mock 被测领域对象 |
| DEL-002 | DEC-003 至 DEC-006, API-001 至 API-006 | API/Core/Starter/Remote/Support/Channels 最终架构 | mango-notice 全模块与仓内调用方 | 业务结果保持、唯一固定入口完整、Service/Entity/Mapper/SPI 规范且 663 条债务为 0 | SAC-001 至 SAC-004 | 不新增业务特性或双协议 |
| DEL-003 | DEC-007, DB-001 至 DB-004 | 单一纯 DDL V1 | core migration | 新库 20 表最终结构等价，Flyway 无 DML | SAC-005 | 不支持旧 history 原地升级 |
| DEL-004 | DEC-008, SEC-004 | 正式、Demo 与运行态数据分层 | starter `META-INF/mango` | 正式资源按 notice 模块登记，Demo 显式，默认无个人联系与运行态数据 | SAC-005 | 不初始化任务、记录、消息、公告或用户联系方式 |
| DEL-005 | DEC-009, IMP-001 至 IMP-003 | 完整应用兼容、README、能力说明、证据和 PR | Payment 直接消费者、Notice docs、capability map | Payment 等价消费者修复；新库单体健康；最新 main 同步后 PR required check 通过并合并 | SAC-001 至 SAC-005 | 不扩大 Payment 重构，不执行全仓检查 |

## 2. 工作分解

| 任务ID | 技术设计ID | 交付物ID | 责任角色 | 路径或模块 | 前置任务 | 具体动作 | 完成标准 | 验证ID | 实施批次 | 状态 |
|---|---|---|---|---|---|---|---|---|---|---|
| TASK-001 | DEC-002, TC-001 至 TC-005 | DEL-001 | Dev、QA | Notice tests/evidence | NONE | 补 API/HTTP/Feign/权限、迁移、资源和关键服务行为测试；生产代码未改时运行增强 before | 同一入口全绿，71 条既有测试不删除且新增用例聚焦高风险边界 | VAL-001, VAL-002 | B1 基线 | PLANNED |
| TASK-002 | DEC-005, DEC-006, MOD-001, MOD-004, MOD-005 | DEL-002 | Dev | api/support/channels/remote | TASK-001 | 规范协议校验和不可变模型；渠道 SPI 改准确语义；Feign 使用唯一固定路径并与 API 对齐 | API、MODEL、FEIGN、ADAPTER、channel 债务为 0且契约通过 | VAL-001, VAL-002, VAL-003 | B2 协议与 SPI | PLANNED |
| TASK-003 | DEC-003, DEC-004, DEC-006, MOD-002 | DEL-002 | Dev | notice-core | TASK-002 | 按配置、投递、记录、接收设置、站内信和同步拆服务；真实 CRUD 聚合使用统一 CRUD；实现归入 impl；实体与 Mapper 最终规范 | Core 债务为 0，事务、状态、查询、幂等和副作用测试一致 | VAL-001, VAL-003 | B3 Core | PLANNED |
| TASK-004 | DEC-005, MOD-003, MOD-004, MOD-006, API-001 至 API-006 | DEL-002 | Dev | starter/remote/mango-ui Notice 调用点 | TASK-003 | Controller 只做校验适配和 R.ok；路径变量改固定 path/query/body；Feign 与全部仓内 URL 同批更新 | 路由、binding、权限、响应和仓内调用完整一致，旧路径搜索为 0 | VAL-001, VAL-002, VAL-003 | B4 适配器 | PLANNED |
| TASK-005 | DEC-007, DB-001 至 DB-004 | DEL-003 | Dev、DBA | core migration | TASK-001 | 以 before 最终 schema 为事实固化单一 V1，折入全部最终字段、索引和约束并移除 DML | 单一纯 DDL V1、20 表 schema hash 等价 | VAL-004 | B5 数据库 | PLANNED |
| TASK-006 | DEC-008, SEC-004 | DEL-004 | Dev、QA | starter resources | TASK-005 | 保留 notice 前缀正式域/菜单/消息声明；确认内置渠道和模板归属；个人联系方式不初始化；演示声明只进入 Demo | 默认正式资源完整、Demo 显式、运行态和个人数据为 0 | VAL-004, VAL-005 | B6 资源 | PLANNED |
| TASK-007 | DEC-009, IMP-003 | DEL-005 | Dev、QA | Payment 直接消费者 | TASK-001 | 将旧 Map 等价转换为 WorkflowJsonRequest 并运行 Payment 定向编译/测试 | Payment 编译与定向测试通过，退款审批 JSON 语义不变 | VAL-006 | B7 消费者兼容 | PLANNED |
| TASK-008 | IMP-001 至 IMP-003 | DEL-001 至 DEL-005 | Dev、QA | Notice、Payment、docs | TASK-002 至 TASK-007 | 运行 after、定向架构与静态检查；重建新库启动单体；更新报告/README/能力地图；同步一次最新 main 后提交 PR并盯到合并 | 同组测试全绿、663→0、新静态问题0、schema等价、health UP、最新 required check 通过 | VAL-001 至 VAL-006 | B8 收口 | PLANNED |

## 3. 顺序、依赖与里程碑

| 里程碑ID | 包含任务ID | 进入条件 | 完成条件 | 依赖 | 可并行任务 | 阻塞升级 | 责任人 |
|---|---|---|---|---|---|---|---|
| MS-001 | TASK-001 | 四阶段规格批准 | 生产代码未改的增强 before 全绿且契约可复核 | NONE | NONE | 既有失败先查测试基础设施，不用生产改动掩盖 | QA 负责人 |
| MS-002 | TASK-002, TASK-003, TASK-004 | MS-001 完成 | 十一个模块和仓内调用方形成唯一规范架构，正式债务为 0 | MS-001 | NONE | 任一业务结果、权限或副作用差异立即停止 | Tech Lead |
| MS-003 | TASK-005, TASK-006, TASK-007 | MS-001 完成 | schema、资源边界和 Payment 消费者兼容均通过 | MS-001 | TASK-005, TASK-007 | schema/启动/Payment 测试失败停止 | DBA、Tech Lead |
| MS-004 | TASK-008 | MS-002、MS-003 完成 | after、架构、新库启动、文档和 PR 全部满足 | MS-002, MS-003 | NONE | 只处理最新提交证据，不重复旧提交检查 | Notice 实施负责人 |

## 4. 验证计划

| 验证ID | 测试或验收ID | 任务ID | 验证层级 | 命令或步骤 | 环境 | 测试数据 | 权限或租户边界 | 预期结果 | 证据路径 | 责任人 | 失败处理 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| VAL-001 | TC-001, TC-002 | TASK-001 至 TASK-004, TASK-008 | UNIT/INTEGRATION | Notice 十一个 artifact 的定向 Maven test | Java 21、H2 | 配置、任务、记录、消息、公告和渠道 fixture | tenant 1/2、user A/B | 增强 before 与 after 同组全绿，业务结果一致 | `mango-docs/evidence/baselines/notice-architecture/latest/report.md` | Dev、QA | 查对应 surefire，不删除或弱化测试 |
| VAL-002 | TC-003 | TASK-001, TASK-002, TASK-004, TASK-008 | API | API 反射、MVC/Feign parity、权限和仓内 URL 定向测试 | JUnit/前端 workspace | 两个 API、全部 endpoints 与请求封装 | 全部 permission/INTERNAL 模式 | 唯一 verb/path/binding/权限和响应一致 | 同上 | QA | 任一目录差异阻断 |
| VAL-003 | TC-001, TC-003 | TASK-002 至 TASK-004, TASK-008 | STATIC | Notice 十一个模块加 architecture-verification 定向 verify | 只扫描任务模块 | PR diff | 不适用业务账号 | 正式债务 0，新静态问题 0 | 同上 | Tech Lead | 不使用 baseline 隐藏新增问题 |
| VAL-004 | TC-004 | TASK-005, TASK-006, TASK-008 | DB/STARTUP | 新 MySQL 执行 V1并启动 monolith backend | workspace 独立 MySQL | 空库+正式资源 | 默认不启用 Demo | 20 表 schema hash 等价、Flyway 无 DML、health UP | 同上 | DBA、QA | 删除新库修复后重建 |
| VAL-005 | TC-005 | TASK-006, TASK-008 | RESOURCE | 检查正式/Demo 声明并查询默认新库数据集合 | 默认与显式 Demo 模式 | notice 前缀声明 | 默认正式租户 | 正式资源完整，默认个人联系/任务/记录/消息/公告为0 | 同上 | QA | 归属或 mode 差异阻断 |
| VAL-006 | TC-006 | TASK-007, TASK-008 | INTEGRATION | Payment 直接模块定向 test，再随 monolith 启动 | Java 21、新 MySQL | 退款审批 JSON | tenant 1 测试上下文 | 编译、JSON 等价、Payment 定向测试和完整启动通过 | 同上 | Dev、QA | 不扩大修复；差异即回退 |

## 5. 数据、升级、发布与回滚步骤

| 发布步骤ID | 技术设计ID | 环境 | 前置检查 | 动作 | 顺序 | 数据备份或回填 | 兼容窗口 | 验证 | 失败停止条件 | 回滚或补偿 | 责任人 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| REL-001 | DB-001 至 DB-004 | before workspace | V1-V17 可顺序执行 | 记录 20 表最终 schema、索引、约束和初始数据行数 | 第1步 | 只读基线，不回填 | 仅任务周期 | schema hash 与行数可复核 | 无法取得最终态事实 | 不修改旧库，停止实施 | DBA |
| REL-002 | DEC-007, DEC-008 | 开发分支 | before 与增强测试完成 | 折叠 V1、资源分层并完成架构重构 | 第2步 | 新库政策，无旧库回填 | 无双版本数据库窗口 | VAL-001 至 VAL-005 | schema/测试/资源失败 | 回退分支改动并重建新库 | Dev |
| REL-003 | DEC-009 | 开发分支 | Workflow 新 DTO 事实确认 | 等价修复 Payment 直接消费者 | 第3步 | 无数据变更 | 同一代码发布 | VAL-006 | Payment 语义或测试差异 | 回退最小修复 | Dev |
| REL-004 | IMP-001, IMP-002 | 默认模式新库 | 定向测试和单一 V1通过 | clean 构建并启动完整单体 | 第4步 | 使用独立新库 | 当前发布版本 | VAL-004 至 VAL-006 | health 非 UP、schema 或数据边界不符 | 停服务并删除新库 | QA |
| REL-005 | IMP-001 至 IMP-003 | PR/main | 所有 after 证据完成 | 同步一次最新 main、解决真实冲突、提交 PR并确认最新 required check 后直接合并 | 第5步 | 不改共享数据库 | required check 期间 | VAL-001 至 VAL-006 | 最新提交出现真实失败 | 未合并则修复；已合并以新提交补偿 | Notice owner |

## 6. 文档与能力同步计划

| 文档项ID | 技术设计或交付物ID | 目标文档 | 变化 | 责任人 | 完成条件 | 检查命令 | 不适用依据 |
|---|---|---|---|---|---|---|---|
| DOC-001 | DEL-002, DEL-003, DEL-004 | `mango/mango-platform/mango-notice/README.md` | 最终模块边界、Service/CRUD政策、唯一调用目录、V1与资源数据边界、验证入口 | Notice owner | 与代码和实际命令一致 | capability docs 定向检查 | 适用 |
| DOC-002 | IMP-001 | Notice 前端 README 或迁移说明 | 固定 path/query/body 和仓内升级事实 | Frontend owner | 不再描述旧路径变量 | 定向文档 diff | 无独立 README 时写入后端 README 的调用迁移章节 |
| DOC-003 | IMP-002, IMP-003 | `mango-docs/capabilities/capability-map.md` | Notice 最终初始化和验证事实、Workflow/Payment 消费者修复 | Notice owner | 能力地图与实现一致 | PMO capability docs check | 适用 |
| DOC-004 | DEL-001 至 DEL-005 | 本设计与 Notice evidence | before/after、债务、schema、资源、启动、兼容和 PR 证据 | Dev、QA | delivery contract、baseline 和 handoff 可复核 | PMO document/delivery checks | 适用 |

## 7. 风险、阻塞与例外

| 风险ID | 风险等级 | 类型 | 触发条件 | 影响 | 预防 | 应对 | 责任人 | 截止时间 | 状态 | 例外ruleId | 例外批准与到期 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| RISK-001 | L3 | RISK | 大服务拆分改变事务、状态汇总或渠道副作用顺序 | 漏发、重复或状态失真 | 增强 before 冻结状态和副作用 | 停止并恢复原调用顺序/事务边界 | Tech Lead | 2026-07-14 | OPEN | NONE | 无例外 |
| RISK-002 | L3 | RISK | Entity/Mapper/SPI 重命名遗漏或序列化变化 | 查询、租户或渠道发送失败 | 机械迁移后编译、集成和渠道测试 | 定位引用并恢复字段/访问器语义 | Dev | 2026-07-14 | OPEN | NONE | 无例外 |
| RISK-003 | L3 | RISK | 固定路径未同步全部仓内调用方 | 页面或远程调用失败 | 搜索调用目录并用契约测试覆盖 | 同批修正，禁止保留双路由 | Tech Lead | 2026-07-14 | OPEN | NONE | 无例外 |
| RISK-004 | L3 | RISK | V1 折叠遗漏最终字段、索引或约束 | 新库运行错误 | before schema dump+hash 对比 | 停止并修正 V1后重建 | DBA | 2026-07-14 | OPEN | NONE | 无例外 |
| RISK-005 | L3 | RISK | 正式资源或 Payment 兼容修复不完整 | 通知模板缺失或完整应用不可启动 | 声明测试、Payment 定向测试和 monolith 新库启动 | 只修真实差异，失败不提交 PR | Notice owner | PR前 | OPEN | NONE | 无例外 |
| RISK-006 | L2 | RISK | main 并行变化产生真实冲突 | PR 无法直接合并 | PR前只同步一次最新 main | 按语义解决后只重跑受影响定向测试 | Notice owner | PR前 | OPEN | NONE | 无例外 |

## 8. 实施追踪矩阵

| 上游设计ID | 交付物ID | 任务ID | 验证ID | 里程碑发布文档或风险项ID | 覆盖说明 |
|---|---|---|---|---|---|
| DEC-001, DEC-002, TC-001, TC-002, TC-003, TC-004, TC-005 | DEL-001 | TASK-001, TASK-008 | VAL-001, VAL-002 | MS-001, MS-004, DOC-004, RISK-001 | 覆盖增强 before、同组 after 和高价值测试 |
| DEC-003, DEC-004, DEC-005, DEC-006, MOD-001, MOD-002, MOD-003, MOD-004, MOD-005, MOD-006, DM-001, DM-002, DM-003, FLOW-001, FLOW-002, FLOW-003, API-001, API-002, API-003, API-004, API-005, API-006, SEC-001, SEC-002, SEC-003, ERR-001, ERR-002, UI-001, UI-002, UI-003, IMP-001 | DEL-002 | TASK-002, TASK-003, TASK-004, TASK-008 | VAL-001, VAL-002, VAL-003 | MS-002, MS-004, DOC-001, DOC-002, RISK-001, RISK-002, RISK-003 | 覆盖十一模块、仓内消费者、业务行为和债务清零 |
| DEC-007, DB-001, DB-002, DB-003, DB-004, FLOW-004 | DEL-003 | TASK-005, TASK-008 | VAL-004 | MS-003, MS-004, REL-001, REL-002, REL-004, DOC-001, RISK-004 | 覆盖单一纯 DDL V1、schema 等价和新库启动 |
| DEC-008, SEC-004, ERR-003, IMP-002 | DEL-004 | TASK-006, TASK-008 | VAL-004, VAL-005 | MS-003, REL-002, DOC-001, DOC-003, RISK-005 | 覆盖正式/Demo/运行态分层和默认数据集合 |
| DEC-009, IMP-003, TC-006 | DEL-005 | TASK-007, TASK-008 | VAL-006 | MS-003, MS-004, REL-003, REL-005, DOC-003, DOC-004, RISK-005, RISK-006 | 覆盖 Payment 消费者兼容、完整启动、PR 与合并 |

## 9. 阶段判定与审批

| 检查项 | 结果 | 证据 |
|---|---|---|
| 实施计划 checker | PASS | `node mango-pmo/tools/check-implementation-plan.mjs --document mango-docs/designs/notice-architecture-debt/implementation-plan.md` |
| 生命周期 handoff | PASS | BRD、SRS、TDD、Plan 均 APPROVED/NEXT 且 hash 精确匹配 |
| 依赖图 | PASS | MS-001→MS-002/MS-003→MS-004，TASK-001→TASK-002至TASK-007→TASK-008，无循环 |
| 未关闭阻断数量 | 0 | RISK-001 至 RISK-006 均有停止条件和责任人，不构成实施前阻断 |
| 实施审批 | APPROVED | `review/PLAN-NOTICE-DEBT.md` |
