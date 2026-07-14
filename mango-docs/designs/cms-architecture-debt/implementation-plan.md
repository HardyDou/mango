---
documentId: PLAN-CMS-DEBT
documentType: implementation-plan
pmoVersion: 1.2.1
schemaRevision: 1
riskLevel: L3
riskAssessmentEvidence: requirement=L3，后台内容状态、公开读取、租户和初始化属于核心链路；solution=L3，一次性调整四层契约、领域服务、错误边界、持久化初始化和演示资源；final=max(requirement,solution)
status: APPROVED
action: NEXT
owner: Mango CMS 实施负责人
approver: HardyDou
approvalEvidence: review/PLAN-CMS-DEBT.md
upstreamDocumentId: TDD-CMS-DEBT
upstreamDocumentHash: 1ded4b27ba7a42088706b5c1909ae4943a777d11687938512aab47f9c0d572cb
---

# CMS 历史债务治理实施计划

## 1. 实施目标、范围与交付物

| 交付物ID | 技术设计ID | 交付物 | 路径或模块 | 完成状态定义 | 验收来源 | 不处理边界 |
|---|---|---|---|---|---|---|
| DEL-001 | DEC-002, FLOW-001, TC-001 至 TC-005 | CMS 重构前后统一测试套件和最新结果基线 | CMS 各子模块测试目录；`mango-docs/evidence/baselines/cms-architecture/latest` | 生产代码变更前形成 before，变更后相同入口形成 after，命令、环境、数据和差异可追溯 | SAC-001 至 SAC-005 | 不用恒真断言、mock 被测对象或无业务结果的数量堆叠 |
| DEL-002 | DEC-003 至 DEC-006, MOD-001, MOD-002, MOD-003, API-001 至 API-004, ERR-001 | 规范化 CMS API、领域 Service 和 HTTP 适配 | `mango-cms-api`、`mango-cms-core`、`mango-cms-starter` | 公共契约不变，管理与公开逻辑按领域边界拆分，错误 code/message 保持，相关测试通过 | SAC-001 至 SAC-004 | 不新增业务接口，不补 Remote Starter 新能力 |
| DEL-003 | DEC-009, SEC-003 | 保持兼容的公开文件函数式入口 | `mango-cms-starter` | GET 路径、参数、响应头、内容长度和拒绝语义与 before 一致，标准 Controller 无 API 外方法 | SAC-004 | 不改文件存储或下载协议 |
| DEL-004 | DEC-007, DB-001 | CMS 纯 DDL 最终态 V1 | `mango-cms-core/src/main/resources/db/migration/mango-cms` | 仅保留 V1，包含当前最终表、列、约束和索引，不包含 DML 或跨模块表访问 | SAC-005 | 不支持旧 Flyway history 原地升级 |
| DEL-005 | DEC-007, DB-002, DM-003, FLOW-004 | CMS 正式资源与 Demo 类型化登记 | `mango-cms-starter/src/main/resources/META-INF/mango` 与 resource handler | 菜单默认登记；Demo 默认关闭且显式启用后依赖、条数、归属和幂等正确 | SAC-005 | 不初始化用户上传文件和正式运行态内容 |
| DEL-006 | MOD-005, IMP-001, IMP-002 | CMS README、设计、计划和交付证据 | CMS README；CMS 设计与 evidence | 接入契约、初始化政策、测试入口、风险与结果可复核且不复制长期规则 | SAC-001 至 SAC-005 | 不新增第二套长期规范 |

## 2. 工作分解

| 任务ID | 技术设计ID | 交付物ID | 责任角色 | 路径或模块 | 前置任务 | 具体动作 | 完成标准 | 验证ID | 实施批次 | 状态 |
|---|---|---|---|---|---|---|---|---|---|---|
| TASK-001 | DEC-002, FLOW-001, TC-001 至 TC-005 | DEL-001 | Dev、QA | CMS test 与 evidence | NONE | 盘点当前 14 个测试，补管理聚合、状态机、公开读取、文件授权、契约和初始化测试；生产代码不变时运行 before | before 报告记录命令、Java/Maven、数据库、用例数和所有结果，测试质量检查通过 | VAL-001, VAL-002 | B1 基线 | PLANNED |
| TASK-002 | DEC-003, DEC-006, MOD-001, API-001 至 API-004, ERR-001 | DEL-002 | Dev | `mango-cms-api` | TASK-001 | 补齐协议字段文档与输入约束，新增保持 code 400/message 的 CmsCode，保存 API 反射快照 | API 字段和方法签名不变，模型和错误契约通过测试及架构检查 | VAL-001, VAL-004 | B2 契约 | PLANNED |
| TASK-003 | DEC-003 至 DEC-005, MOD-002, DM-001, DM-002, FLOW-002, FLOW-003, SEC-001 至 SEC-003 | DEL-002, DEL-003 | Dev | `mango-cms-core` | TASK-002 | 按聚合拆分管理 Service，拆分公开解析/文件策略/转换协作者，去除 R 和裸消息，保持事务、查询条件和状态顺序 | 所有领域服务职责单一，现有状态、数据权限、公开资格和错误结果由同一基线证明 | VAL-001, VAL-002, VAL-004 | B3 Core | PLANNED |
| TASK-004 | DEC-006, DEC-009, MOD-003, API-001 至 API-004, UI-001 | DEL-002, DEL-003 | Dev | `mango-cms-starter` | TASK-003 | Controller 直接 `R.ok` 对应领域服务，补 OpenAPI/绑定；文件流迁入函数式 endpoint；保持权限码与公开模式 | HTTP 契约快照和 MockMvc 全部一致，Controller/endpoint 架构债务为 0 | VAL-002, VAL-004 | B4 Starter | PLANNED |
| TASK-005 | DEC-007, MOD-002, DB-001 | DEL-004 | Dev、DBA | CMS migration | TASK-001 | 固化 V1-V10 最终 schema 指纹，生成纯 DDL V1 并删除 V2-V10 | 新 V1 在空 MySQL 成功执行，schema 指纹一致，SQL 无 DML 和跨模块表 | VAL-003, VAL-004 | B5 数据结构 | PLANNED |
| TASK-006 | DEC-007, MOD-003, DM-003, FLOW-004, DB-002 | DEL-005 | Dev、QA | CMS starter resource | TASK-005 | 为站点、设置、栏目、导航、Banner、内容、发布、广告和投放实现类型化 handler 与 Demo 声明，保留正式菜单 | 默认无 Demo；显式启用后完整、幂等且无跨模块文件依赖 | VAL-003, VAL-005 | B6 初始化 | PLANNED |
| TASK-007 | DEC-001, DEC-008, MOD-004, MOD-005, IMP-001, IMP-002 | DEL-001, DEL-006 | Dev、QA、Tech Lead | CMS 全模块、README、evidence | TASK-002, TASK-003, TASK-004, TASK-005, TASK-006 | 运行 after 与架构门禁，启动新库服务，执行公开 API 和 Demo UI 冒烟，更新文档和最终证据 | 前后无未批准行为差异、1,126 债务降至 0、服务与定向 UI/API 验收通过 | VAL-001 至 VAL-005 | B7 收口 | PLANNED |

## 3. 顺序、依赖与里程碑

| 里程碑ID | 包含任务ID | 进入条件 | 完成条件 | 依赖 | 可并行任务 | 阻塞升级 | 责任人 |
|---|---|---|---|---|---|---|---|
| MS-001 | TASK-001 | 书面规格批准 | before 基线和测试质量检查可复核 | NONE | NONE | 基线失败先判断测试基础设施或既有缺陷，不修改生产行为掩盖失败 | QA 负责人 |
| MS-002 | TASK-002, TASK-003, TASK-004 | MS-001 完成 | API/Core/Starter 同一契约测试通过且无双实现 | MS-001 | TASK-002 完成后 TASK-003 与部分协议注解可按文件并行，但由单一实施者顺序提交 | 任一公共签名或失败响应变化立即停止并回到契约快照 | Tech Lead |
| MS-003 | TASK-005, TASK-006 | MS-001 完成且初始化清单固化 | 纯 DDL V1、正式默认和 Demo 显式初始化通过 | MS-001 | TASK-005 与 Demo 声明整理可并行，handler 落库在 V1 稳定后验证 | schema 或资源依赖不一致时丢弃测试库并停止 | DBA、Tech Lead |
| MS-004 | TASK-007 | MS-002、MS-003 完成 | after、架构、启动、API、UI、文档和证据全部满足 | MS-002, MS-003 | NONE | 任一阻断项不得通过例外合并 | Mango CMS 实施负责人 |

## 4. 验证计划

| 验证ID | 测试或验收ID | 任务ID | 验证层级 | 命令或步骤 | 环境 | 测试数据 | 权限或租户边界 | 预期结果 | 证据路径 | 责任人 | 失败处理 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| VAL-001 | TC-001, TC-002 | TASK-001, TASK-002, TASK-003, TASK-007 | UNIT | `mvn -f mango/mango-platform/mango-cms/pom.xml clean test` | Java 21、当前 Maven、worktree | `IT_CMS_*` 独立数据；Mango 内部被测链路不 mock | 双租户、数据权限、多状态和公开/非公开矩阵 | before/after 同一套用例全部通过且数量、失败、错误、跳过可比较 | `mango-docs/evidence/baselines/cms-architecture/latest/report.md` | QA | 禁止删用例或弱化断言；定位真实差异后修复 |
| VAL-002 | TC-001, TC-002 | TASK-001, TASK-004, TASK-007 | API | 执行 CMS MockMvc、数据库集成和公开文件契约测试 | Spring 测试上下文与隔离数据库 | 管理、公开、文件引用、无引用和跨站点数据 | 保持权限码、PUBLIC、租户上下文和 DataScope | verb/path/binding/R 泛型、code/message、状态和响应头一致 | 同上 | QA | 任一 2xx 但业务断言不符仍判失败 |
| VAL-003 | TC-003 | TASK-005, TASK-006, TASK-007 | API | 在独立空 MySQL 执行 Flyway、默认资源、Demo 资源与幂等验证 | workspace 新数据库 | 固定 CMS Demo 声明，不依赖共享数据 | Demo tenantId/orgId 明确；正式模式无 Demo | schema 指纹一致、V1 纯 DDL、默认/显式结果符合清单 | 同上 | DBA、QA | 丢弃失败测试库，修正 V1/handler 后从空库重跑 |
| VAL-004 | TC-004 | TASK-002 至 TASK-005, TASK-007 | STATIC | CMS 直接修改模块 `mvn verify` 加 architecture changed 模式及 `test-quality-check --base origin/main` | CMS partial reactor，不执行全仓债务盘点 | Git diff、compiled classes 和正式 CMS module selectors | 不适用 | CMS dependency/archunit/pmd 从 1,126 到 0，新增问题 0，测试无恒真/同值/mock 被测对象 | 同上 | Dev、Tech Lead | 逐条修复，不抬高正式债务预算，不用 partial 报告写全局预算 |
| VAL-005 | TC-003, TC-005 | TASK-006, TASK-007 | UI、API | `mango dev start backend` 后调用健康检查和 CMS 公开接口，再启动 Demo 站点执行定向浏览器用例 | workspace `mango_182`；端口 18182/38182；数据库 `mango_dev_mango_cms_architecture_debt_182` | 显式 Demo 资源创建的唯一站点 | 仅演示租户；不记录密码、token 或密钥 | 服务健康，公开站点关键内容可见，未发布内容不可见，console/network 无未解释错误 | 同上 | QA | 停止服务，保留日志于 `.runtime`，修复后用新库重跑 |

## 5. 数据、升级、发布与回滚步骤

| 发布步骤ID | 技术设计ID | 环境 | 前置检查 | 动作 | 顺序 | 数据备份或回填 | 兼容窗口 | 验证 | 失败停止条件 | 回滚或补偿 | 责任人 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| REL-001 | DEC-007, DB-001, DB-002 | 本地/CI 新数据库 | 确认数据库为空、模块未发布且用户批准新库政策 | 执行纯 DDL V1，再登记正式资源；显式开关为真时登记 Demo | 先结构、再正式资源、最后可选 Demo | 不迁移旧库、不回填运行态数据；当前 Demo 清单由版本控制资源表达 | 无旧 history 兼容窗口 | VAL-003、VAL-005 | 任一旧 history 数据库、schema 不一致、DML 混入或 Demo 默认出现 | 丢弃新库并回退任务分支；不得将 V1 应用于旧 history | DBA |
| REL-002 | IMP-001, IMP-002, DEC-001 | PR 与下一平台发布批次 | after、架构、CI、README 和证据满足 | 合并最新 main 后复测，提交/Push/PR，通过 required check 后 squash 合并 | 验证→合并 main→复测→PR→CI→合并 | 无生产数据操作 | 公共 HTTP/Java 契约保持，无消费者迁移窗口 | VAL-001 至 VAL-005 与 PR CI | 任一 required check、契约或运行态失败 | 不合并 PR；已合并前可整体回退提交 | Mango CMS 能力负责人 |

## 6. 文档与能力同步计划

| 文档项ID | 技术设计或交付物ID | 目标文档 | 变化 | 责任人 | 完成条件 | 检查命令 | 不适用依据 |
|---|---|---|---|---|---|---|---|
| DOC-001 | IMP-001, IMP-002, DEL-006, TASK-007 | `mango/mango-platform/mango-cms/README.md` 与能力地图 | 说明纯 DDL V1、正式/demo 分离、新库边界、测试入口和行为保持 | Dev | 内容与代码/资源事实一致，只链接 PMO 长期规范 | README 事实与能力文档 checker | 无 |
| DOC-002 | DEL-001, DEL-006, TASK-001, TASK-007 | `mango-docs/evidence/baselines/cms-architecture/latest/report.md` | 记录 before/after 命令、环境、数据库、用例结果、架构计数、启动/API/UI 证据和差异 | QA | 最新基线可复核，无临时日志、密钥或 token | 文档合同、链接检查和人工证据审查 | 无 |

## 7. 风险、阻塞与例外

| 风险ID | 风险等级 | 类型 | 触发条件 | 影响 | 预防 | 应对 | 责任人 | 截止时间 | 状态 | 例外ruleId | 例外批准与到期 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| RISK-001 | L3 | RISK | 领域拆分遗漏查询条件、事务或状态副作用 | 管理或公开结果变化 | before 特征测试、状态/权限/双租户矩阵和小检查点 | 停止后续批次，定位差异并恢复既有语义 | Tech Lead | 2026-07-14 | CLOSED | NONE | NONE |
| RISK-002 | L3 | RISK | 新 V1 与当前最终 schema 或 Demo 清单不一致 | 新环境不可用或内容污染 | schema 指纹、DML 扫描、默认/显式资源和幂等验证 | 丢弃新库，修正后从空库重跑 | DBA | 2026-07-14 | CLOSED | NONE | NONE |
| RISK-003 | L2 | RISK | 文件二进制入口迁移后响应头或授权结果变化 | 站点素材无法展示或越权访问 | 固定 verb/path/query/header/body 与引用授权测试 | 恢复原入口并重新设计；不以规则例外放行 | Dev、QA | 2026-07-14 | CLOSED | NONE | NONE |

## 8. 实施追踪矩阵

| 上游设计ID | 交付物ID | 任务ID | 验证ID | 里程碑发布文档或风险项ID | 覆盖说明 |
|---|---|---|---|---|---|
| DEC-001, DEC-002, FLOW-001, TC-001, TC-002, TC-003, TC-004, TC-005 | DEL-001 | TASK-001, TASK-007 | VAL-001, VAL-002, VAL-003, VAL-004, VAL-005 | MS-001, MS-004, REL-002, DOC-002, RISK-001 | 覆盖统一 before/after 基线与最终证据 |
| DEC-003, DEC-004, DEC-005, DEC-006, MOD-001, MOD-002, MOD-003, DM-001, DM-002, FLOW-002, FLOW-003, API-001, API-002, API-003, API-004, SEC-001, SEC-002, ERR-001, UI-001, IMP-001 | DEL-002 | TASK-002, TASK-003, TASK-004, TASK-007 | VAL-001, VAL-002, VAL-004, VAL-005 | MS-002, MS-004, REL-002, DOC-001, RISK-001 | 覆盖 API、领域拆分、权限、错误和前端消费者兼容 |
| DEC-009, SEC-003 | DEL-003 | TASK-003, TASK-004, TASK-007 | VAL-002, VAL-005 | MS-002, MS-004, RISK-003 | 覆盖公开文件流与授权兼容 |
| DEC-007, MOD-002, DM-003, FLOW-004, DB-001 | DEL-004 | TASK-005, TASK-007 | VAL-003, VAL-004, VAL-005 | MS-003, MS-004, REL-001, RISK-002 | 覆盖纯 DDL V1 与新库启动 |
| DEC-007, MOD-003, DM-003, FLOW-004, DB-002 | DEL-005 | TASK-006, TASK-007 | VAL-003, VAL-005 | MS-003, MS-004, REL-001, RISK-002 | 覆盖正式和 Demo 资源分离 |
| DEC-008, MOD-004, MOD-005, IMP-002 | DEL-006 | TASK-007 | VAL-004, VAL-005 | MS-004, REL-002, DOC-001, DOC-002 | 覆盖 Remote 不扩展、README 和能力文档影响 |

## 9. 阶段判定与审批

| 检查项 | 结果 | 证据 |
|---|---|---|
| 实施计划 checker | PASS | `node mango-pmo/tools/check-implementation-plan.mjs --document mango-docs/designs/cms-architecture-debt/implementation-plan.md`，2026-07-14 通过 |
| 生命周期 handoff | PASS | `node mango-pmo/tools/check-lifecycle-handoff.mjs --brd mango-docs/designs/cms-architecture-debt/business-requirements.md --srs mango-docs/designs/cms-architecture-debt/system-requirements.md --tdd mango-docs/designs/cms-architecture-debt/technical-design.md --plan mango-docs/designs/cms-architecture-debt/implementation-plan.md --risk L3` |
| 依赖图 | PASS | TASK-001→TASK-002→TASK-003→TASK-004；TASK-001→TASK-005→TASK-006；最后 TASK-007，无环 |
| 未关闭阻断数量 | 0 | RISK-001 至 RISK-003 均有预防和应对，无开放阻断 |
| 实施审批 | APPROVED | `review/PLAN-CMS-DEBT.md` |
