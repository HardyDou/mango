---
documentId: TDD-WORKFLOW-DEBT
documentType: technical-design
pmoVersion: 1.2.2
schemaRevision: 1
riskLevel: L3
riskAssessmentEvidence: 一次性调整 API 校验、领域 Service 返回边界、Entity/Mapper 命名、Controller/Feign 适配、Flowable 初始化和 Demo 装载，技术风险覆盖全部核心审批路径
status: APPROVED
action: NEXT
owner: Mango Workflow 技术负责人
approver: HardyDou
approvalEvidence: review/TDD-WORKFLOW-DEBT.md
upstreamDocumentId: SRS-WORKFLOW-DEBT
upstreamDocumentHash: 78eaec9b6b426f88fa6d84989ca586fed7205337c10e9b17ed54cf98e028c278
---

# Workflow 历史债务治理技术设计

## 1. 设计输入、约束与决策

| 决策ID | 问题 | 候选方案 | 选择 | 理由 | 来源ID或路径 | 是否推断 | 影响 | 风险 | 回退条件 |
|---|---|---|---|---|---|---|---|---|---|
| DEC-001 | 用户要求模块债务一步到位 | 分批保留双实现；单 PR 最终态 | 单 PR 最终态 | 其它模块不依赖内部实现，双实现会扩大行为分叉 | BAC-004, `mango-platform/mango-workflow` | 否 | 四个子模块同时调整 | 改动面大 | 任一公共契约差异立即停止 |
| DEC-002 | 41 条现有测试含 3 条失败 | 带失败重构；先修测试基础设施 | 先修 H2 schema/fixture 并补少量特征测试，生产代码未改时记录 before | 必须先区分既有缺陷与回归 | SAC-001 至 SAC-005 | 否 | 测试和基线证据 | fixture 可能掩盖生产问题 | 只允许修改测试数据/结构且必须证明生产代码未变 |
| DEC-003 | Service 返回和构造 R | 保持现状；领域值+全局异常映射 | `IXxxService` 返回领域值，`XxxService` 使用 `Require + WorkflowCode`，Controller 只 `R.ok` | R 属于 HTTP 边界，可保持原 code/message | FR-001 至 FR-004, backend/03-api | 否 | API/Core/Starter | 错误消息变化 | 错误契约测试不一致即回退当前方法 |
| DEC-004 | 7 个实现类名为 `*ServiceImpl` | 保留；改名并保留 impl 包 | 改为 `XxxService implements IXxxService` 且留在 `service/impl` | 同时满足实现位置和命名规范 | MOD-002, MANGO-ARCH-SVC-005 | 否 | Core 类型引用与测试 | Spring Bean 名变化 | 检查无按字符串 Bean 名消费，否则显式兼容别名 |
| DEC-005 | 12 个 Entity/Mapper 聚合名不规范 | 引入新 DAO；机械规范化类型 | Entity 改 `XxxEntity extends TenantEntity`，Mapper `@Mapper extends BaseMapper<XxxEntity>` | 不改变表、字段、SQL 和查询语义 | MOD-002, MANGO-ARCH-ENTITY-001, MANGO-ARCH-MAPPER-006 | 否 | Core 全部持久化引用 | MyBatis 泛型错配 | 真实 Mapper 集成测试失败即修正引用，不改查询 |
| DEC-006 | Controller/Feign 未完整实现 API | 改路径；逐项对齐 | API 补校验，Controller/Feign 逐项重声明原 endpoint 和 binding | 保持外部协议并消除适配债务 | SAC-004, MANGO-ARCH-ADAPTER-001 | 否 | API/Starter/Remote | 漏 endpoint 或 binding 差异 | 完整指纹差异即停止 |
| DEC-007 | V1-V4 含 DML 和历史兼容 SQL | 保留升级链；新库最终态 V1 | 将最终列/索引折叠入 V1，Flowable metadata 移到引擎前正式初始化 | 用户明确只使用新库且 Flyway 仅 DDL | BAC-005, DB-001, DB-002 | 否 | 数据结构和启动顺序 | ProcessEngine 早于 metadata | 新库启动失败时调整前置依赖，不把 DML 放回 Flyway |
| DEC-008 | 示例流程默认启动写库 | 仅把开关设 false；Demo 资源化 | 删除 sample runner/properties，三套示例转 `META-INF/mango/demo` 声明 | 统一审计、依赖和显式装载 | FR-006, DB-004 | 否 | Core/Starter 初始化 | 复杂 JSON 转换差异 | definitionKey/pageKey/JSON 语义比较失败即停止 |
| DEC-009 | 事件与当前任务快照有严格时序 | 重做事件；冻结既有顺序 | 保留 10 个 WorkflowEventTypes、数据和“刷新快照后发布”顺序 | 业务消费者依赖该事实 | BR-004, FLOW-002, FLOW-003 | 否 | Core 任务和发起逻辑 | 重构改变调用顺序 | 真实 Flowable 事件测试差异即回退 |

## 2. 模块与依赖边界

| 模块设计ID | 模块或包 | 职责 | 改动类型 | 依赖方向 | 公开能力 | 系统需求ID | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|
| MOD-001 | mango-workflow-api | 5 个公共 API、Command/Query/VO/enum/code/event types | 补校验与协议类型，保持签名/JSON | 只依赖公共结果、校验和文档类型 | Java 契约 | FR-005 | MANGO-ARCH-API-002, MANGO-ARCH-MODEL-002 | API 反射指纹与编译 |
| MOD-002 | mango-workflow-core | Flowable 编排、领域服务、Entity、Mapper、资源处理和数据库结构 | 分层、类型命名、纯 DDL、必需 metadata | api→core；core 不依赖 HTTP | 领域服务 | FR-001 至 FR-004, FR-006 | MANGO-ARCH-SVC-001, MANGO-ARCH-ENTITY-001, MANGO-ARCH-MAPPER-004 | 单元、Mapper、Flowable 集成与架构门禁 |
| MOD-003 | mango-workflow-starter | HTTP、权限、OpenAPI、资源声明、通知订阅 | Controller 纯适配和资源分层 | starter→api/core | `/workflow/**` | FR-001 至 FR-006 | MANGO-ARCH-CTRL-004, MANGO-ARCH-OPENAPI-006 | Controller 契约、MockMvc、架构门禁 |
| MOD-004 | mango-workflow-starter-remote | Feign 远程适配 | 完整实现公共 API 并对齐 binding | remote→api | 远程 Java 契约 | FR-002, FR-004, FR-005 | MANGO-ARCH-FEIGN-008, MANGO-ARCH-ADAPTER-002 | Feign/Controller endpoint 指纹 |
| MOD-005 | workflow 前端与 example | 消费既有 HTTP、pageKey 和表单/设计器模型 | 仅修正 README 事实，无生产代码变化 | 前端→HTTP | 定义、发起、任务与示例页 | FR-005, FR-006 | DOC-CAP-001 | 新库 API 和示例 pageKey 冒烟 |

## 3. 技术对象与状态模型

| 模型ID | 上游ID | 模型职责 | 标识 | 关系 | 状态编码 | 审计或历史 | 归属或租户 | 一致性约束 |
|---|---|---|---|---|---|---|---|---|
| DM-001 | DR-001, FR-001 | 分类、模板、定义和发布版本 | id、definitionKey、versionNo | 版本/模板关联定义与分类 | DRAFT/PUBLISHED/DISABLED；模板状态保持 | 定义版本表与审计字段 | tenantId、domainCode、orgId | 租户内 definitionKey 唯一，发布版本可部署 |
| DM-002 | DR-002, FR-002, FR-003 | Flowable 实例、任务和变量 | processInstanceId、taskId | 任务和变量属于实例 | RUNNING/COMPLETED/REJECTED/ENDED | Flowable 历史表 | tenantId 与发起人/办理人 | 一个有效动作只推进一次 |
| DM-003 | DR-003, FR-002, FR-004 | 业务申请、状态日志和当前任务快照 | applyId、businessType+businessKey | 申请关联实例、日志和当前任务 | DRAFT/SUBMITTED/IN_APPROVAL/APPROVED/REJECTED/WITHDRAWN/CANCELED/TERMINATED | 状态日志与 created/updated | tenantId | apply、instance、current task 状态一致 |
| DM-004 | DR-004, FR-006 | Flowable schema metadata 与模块资源声明 | property key、resource bizKey | metadata 对应引擎版本；资源声明依赖类型 | INIT_ONLY | Flyway history 与资源同步记录 | 系统级/tenant 1 Demo | metadata 在引擎创建前完整，Demo 默认不加载 |

| 模型ID | 当前状态 | 触发 | 目标状态 | 前置条件 | 副作用 | 失败处理 | 上游ID |
|---|---|---|---|---|---|---|---|
| DM-001 | DRAFT | 发布定义 | PUBLISHED | 设计器、表单和编码有效 | 部署 BPMN、写版本、更新定义 | 事务回滚且无错误版本 | FR-001, SAC-001 |
| DM-002 | 无实例 | 发起流程 | RUNNING 或终态 | 已发布定义可用 | 创建实例、任务和变量 | 发起事务回滚 | FR-002, SAC-002 |
| DM-002 | 当前任务待办 | 完成/驳回/退回/转办/加签/认领 | 下一任务、终态或对应办理状态 | 操作者和动作配置有效 | 引擎状态、办理记录和变量改变 | 不推进且返回 WorkflowCode | FR-003, SAC-003 |
| DM-003 | IN_APPROVAL | 实例/任务状态改变 | IN_APPROVAL/APPROVED/REJECTED/TERMINATED | 关联实例存在 | 写状态日志并刷新当前任务 | 与引擎事务共同回滚 | FR-002, FR-003 |

## 4. 系统流程、事务与一致性

| 流程设计ID | 系统需求ID | 调用入口 | 参与模块 | 处理顺序 | 事务边界 | 状态变化 | 幂等键 | 并发策略 | 外部失败与补偿 | 用户可见结果 |
|---|---|---|---|---|---|---|---|---|---|---|
| FLOW-001 | FR-001 | 定义保存/发布 endpoint | MOD-001, MOD-002, MOD-003 | 校验→保存草稿→生成 BPMN→部署→写版本→更新定义 | 现有 Service 事务 | DM-001 DRAFT→PUBLISHED | tenantId+definitionKey+version | 唯一索引和现有更新条件 | 引擎失败整体回滚 | 原定义、版本或 WorkflowCode |
| FLOW-002 | FR-002 | start/startBusinessWorkflow | MOD-001 至 MOD-004 | 解析定义→创建/关联申请→启动实例→刷新 current task→发布 PROCESS_STARTED | 一体化发起单事务 | DM-002/DM-003 进入运行/审批 | 现有 businessKey/实例规则 | 引擎与数据库事务 | 任一步失败整体回滚 | 原启动结果或业务错误 |
| FLOW-003 | FR-003, FR-004 | `/workflow/tasks/**` | MOD-001, MOD-002, MOD-003 | 身份/动作校验→引擎动作→记录/表单→申请状态→刷新快照→发布事件 | 每个任务动作现有事务 | DM-002/DM-003 推进或终止 | taskId+当前运行状态 | Flowable 乐观锁和当前任务判断 | 引擎或写库失败整体回滚 | 原布尔/动作结果、详情和错误 |
| FLOW-004 | FR-006 | 新库应用启动 | MOD-002, MOD-003 | 纯 DDL→metadata initializer→ProcessEngine→正式资源→可选 Demo | 启动阶段各自事务，失败不声明可用 | DM-004 从空到可用 | property key、resource bizKey | insert-if-absent/INIT_ONLY | 失败丢弃新库重建 | health、同步日志和数据集合 |

## 5. API 与远程契约设计

| 接口ID | 系统需求ID | 调用方 | 所属模块 | 入口类型 | 方法与路径 | Command Query或VO | 返回契约 | 校验 | 权限租户或数据权限 | 幂等分页或排序 | 错误码 | 兼容策略 | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| API-001 | FR-001, FR-005 | 管理后台/内部模块 | MOD-001, MOD-003, MOD-004 | Java+HTTP+Feign | POST /workflow/definitions | 原 Command/Query/VO，补 `@Valid` 与字段约束 | R<WorkflowDefinitionVO> | Jakarta validation | 保留 `@PreAuthorize`、tenant/data scope | 原分页与版本排序 | 3601-3620 | 全部定义/分类/模板方法、字段、路径和 JSON 不变 | MANGO-ARCH-API-002, MANGO-ARCH-CTRL-005 | API/endpoint 指纹、定义测试 |
| API-002 | FR-002, FR-005 | 发起人/业务模块 | MOD-001 至 MOD-004 | Java+HTTP+Feign | POST /workflow/processes/start-business | Start*Command、WorkflowStartResultVO | R<WorkflowStartResultVO> | body `@Valid` | 当前 tenant/user、启动可见性 | 业务键规则不变 | 3610-3642 | start 与 start-business 全部原协议保持 | MANGO-ARCH-ADAPTER-001 | 真实发起+路由测试 |
| API-003 | FR-003, FR-005 | 审批人 | MOD-001 至 MOD-003 | Java+HTTP | POST /workflow/tasks/complete-result | 任务 Command/Query/VO | R<WorkflowTaskCompleteResultVO> | body/query/标量显式校验 | assignee/candidate/claim/permission | taskId 单次动作 | 3650-3651 | tasks 全部原路径、动作和字段不变 | MANGO-ARCH-CTRL-003, MANGO-ARCH-OPENAPI-006 | 任务集成+完整 endpoint 指纹 |
| API-004 | FR-004, FR-005 | 页面/业务模块 | MOD-001 至 MOD-004 | Java+HTTP+Feign | POST /workflow/business-applies/progress/latest-batch | 原 Query/VO；批量集合改协议 Query 时保持 HTTP body/query 语义 | R<WorkflowBusinessApplyProgressVO> | `@Valid`/标量约束 | tenant/user/data scope | 原分页、排序、latest 语义 | 3640-3651 | 全部查询 JSON 和方法语义保持，必要 Java 重载由指纹审计 | MANGO-ARCH-API-004, MANGO-ARCH-ADAPTER-007 | API、Controller、Feign 指纹 |

## 6. 持久化与数据迁移设计

| 数据设计ID | 上游或模型ID | 表或实体 | 字段变化 | 约束 | 索引 | 租户审计 | Mapper边界 | 数据来源 | migration或回填 | 回滚或补偿 | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| DB-001 | DM-001 至 DM-003 | 12 个 Workflow 业务表及 Flowable 表 | V2-V4 的 domainCode、startEntryVisible、claimStatus/candidates 直接进入 V1 create table | 保持原主键、外键、唯一约束和默认值 | 保持最终索引集合 | 业务 Entity 统一 TenantEntity，字段/表不变 | 12 个 `@Mapper BaseMapper<XxxEntity>`，自定义 XML/方法语义不变 | 现有 V1-V4 最终态 | 仅新库；单一 V1；不回填旧库 | 失败丢弃新库 | MANGO-ARCH-ENTITY-001, MANGO-ARCH-MAPPER-006 | schema 指纹、Mapper 集成、新库启动 |
| DB-002 | DM-004 | ACT_GE_PROPERTY | V1 删除 12 条 INSERT；initializer 写相同 key/value | NAME_ 主键；值与 Flowable 7.0.0 一致 | 主键索引 | 系统级非租户 | 专用 JdbcTemplate initializer，不暴露业务 Mapper | 现有 V1 metadata | ProcessEngine bean 前 insert-if-absent | 新库重建；禁止退回 Flyway DML | DB-MIGRATION-001 | 静态 SQL 检查、metadata 测试、真实启动 |
| DB-003 | DM-004 | 正式 resource declarations | 保留 common domain/node/menu | bizKey 唯一、INIT_ONLY | Resource Registry 自有 | 按声明 target module/tenant | 现有 Workflow handlers | `META-INF/mango/resources` | 启动同步，无历史回填 | 删除新库记录重建 | RESOURCE-REGISTRY-001 | 声明与 handler 集成测试 |
| DB-004 | DM-001, DM-004 | 三套示例定义 | 从 SampleDefinitionInitializer 精确迁为 demo YAML/JSON | definitionKey/pageKey/form/designer 语义不变 | 定义唯一索引 | tenant 1、COMMON domain | 复用 WorkflowDefinition resource handler | `META-INF/mango/demo` | 仅显式 Demo，INIT_ONLY | 新库重建或关闭 Demo | RESOURCE-REGISTRY-001 | 资源契约、同步、发布和 API 冒烟 |

## 7. 安全、权限、租户与数据边界

| 安全设计ID | 系统需求ID | 能力 | 权限资源 | 默认授权 | 后端校验入口 | 租户边界 | 数据归属断言 | 前端反馈 | 审计 | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| SEC-001 | FR-001, FR-005 | 定义/分类/模板管理 | 保留全部 workflow 权限码 | 默认不扩大 | Controller 注解+Service Require | MangoContext tenantId | definition/category/template tenant/domain 匹配 | 原无权限或业务错误 | 保留审计字段 | AUTHORIZATION-001 | 注解指纹、跨租户测试 |
| SEC-002 | FR-002, FR-004 | 发起和申请查询 | 启动可见性、登录身份和数据范围 | 保持原策略 | Process/BusinessApply Service | 当前 tenant/user | apply/definition/business key 同租户 | 原不可发起/不可见反馈 | 申请状态日志 | DATA-PERMISSION-001 | 发起与进度测试 |
| SEC-003 | FR-003 | 任务办理 | assignee/candidate/claim 和节点动作配置 | 非办理人默认拒绝 | TaskRuntime Service 每个写动作 | 任务实例 tenant 与上下文 | task、instance、apply 关联一致 | 原任务错误 | task record/event | AUTHORIZATION-001 | 候选、认领、越权、重复动作测试 |
| SEC-004 | FR-006 | 初始化资源 | 正式默认、Demo 显式 | Demo 默认关闭 | Resource Registry 开关和 handler 校验 | Demo tenant 1，不影响其它 tenant | moduleCode/targetModule/bizKey 明确 | 启动日志 | 同步记录 | RESOURCE-REGISTRY-001 | 默认/显式两次新库验证 |

## 8. 错误码、异常与可观测性

| 错误设计ID | 系统需求ID | 失败场景 | 触发条件 | 错误码 | 异常类型 | 用户反馈 | 日志上下文 | 指标或告警 | 重试或补偿 | 敏感信息处理 |
|---|---|---|---|---|---|---|---|---|---|---|
| ERR-001 | FR-001 | 分类、模板、定义不存在或非法 | 查询、保存、发布条件不满足 | 保持 3601-3620 | Require 统一业务异常 | 保持 WorkflowCode message 和必要动态信息 | tenant、definition/category/template id/key | 现有日志 | 修正输入后重试 | 不输出设计器敏感变量 |
| ERR-002 | FR-002, FR-004 | 实例或申请不存在/非法 | 发起或查询目标无效 | 保持 3640-3642 | Require 统一业务异常 | 保持原消息 | tenant、businessType/key、instance/apply id | 现有日志 | 修正输入或刷新 | 不跨租户泄露存在性 |
| ERR-003 | FR-003 | 任务不存在、越权或动作非法 | 任务动作校验失败 | 保持 3650-3651 | Require 统一业务异常 | 保持原任务反馈 | taskId、processInstanceId、action、operator | 现有日志 | 刷新任务后按可用动作 | 不记录表单敏感变量全文 |
| ERR-004 | FR-006 | schema、metadata 或资源初始化失败 | 新库启动任一阶段失败 | 启动失败，不构造业务成功码 | 配置/初始化异常 | 日志指出阶段和 resource key/property key | workspace/database/module/resource | 启动失败可见 | 丢弃新库修复后重建 | 声明禁止密码/token |

## 9. 前端结构与交互实现映射

| 前端设计ID | 系统需求ID | 页面或动作 | 页面key或路由 | 区域与组件 | 状态来源 | API依赖 | 权限或不可操作 | 空加载或失败态 | 语义测试锚点 | 复用判断 | 适用规范ruleId |
|---|---|---|---|---|---|---|---|---|---|---|---|
| UI-001 | FR-001, FR-005 | 流程定义管理 | 保持现有 workflow 定义路由 | 分类、列表、设计器、表单、版本 | 定义/版本 API | API-001 | 保持现有权限码和按钮状态 | 保持现有 loading/empty/error | endpoint 指纹、定义发布测试 | 复用现有 `@mango/workflow`，无生产改动 | UI-COMPAT-001 |
| UI-002 | FR-002, FR-004, FR-005 | 发起与我的申请 | 保持现有发起/申请路由 | 流程卡片、申请表单、进度 | Process/BusinessApply API | API-002, API-004 | 启动可见性与当前用户范围 | 保持原空态/错误 | 发起、progress、render mode 测试 | 复用现有页面，无生产改动 | UI-COMPAT-001 |
| UI-003 | FR-003, FR-004, FR-005 | 我的任务与全部动作 | 保持现有 tasks 路由 | 列表、详情、表单、动作栏 | Task API 和 node action config | API-003, API-004 | assignee/candidate 与动作 enabled | 保持原空态/已办理/失败 | 任务动作和 endpoint 指纹 | 复用现有页面，无生产改动 | UI-COMPAT-001 |
| UI-004 | FR-006 | 三套业务示例申请/审批 | 保持 expense/contractSeal/leave pageKey | example 包既有自定义页 | Demo definition resource | API-002, API-003 | tenant 1/admin Demo | 资源缺失时启动/同步失败 | pageKey、definitionKey、API 冒烟 | 复用 `workflow-business-example`，无生产改动 | UI-COMPAT-001 |

## 10. 测试设计与验收映射

| 测试用例ID | 系统验收ID | 设计项ID | 场景 | 优先级 | 测试层级 | 自动化判断 | 测试数据 | 权限或租户边界 | 稳定契约 | 执行入口 | 证据 | 失败处理 | 适用规范ruleId |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| TC-001 | SAC-001 | DEC-002 至 DEC-006, FLOW-001, API-001, DM-001 | 定义保存、发布、版本和错误 | P0 | UNIT/INTEGRATION | AUTO | 现有定义 fixture+非法/重复输入 | tenant 1 与跨租户 | 状态、版本、WorkflowCode、API 指纹 | Workflow Maven test | baseline report | 不删除/弱化测试，定位真实差异 | TEST-QUALITY-001 |
| TC-002 | SAC-002, SAC-003 | DEC-002, DEC-003, DEC-009, FLOW-002, FLOW-003, DM-002, DM-003 | 发起、完成、驳回、退回、暂存、转办、加签、认领、事件和快照 | P0 | INTEGRATION | AUTO | 真实 H2 Flowable 流程 | admin/发起人/审批人/候选人 | 返回、引擎状态、记录、变量、快照、事件顺序 | Workflow Maven test | baseline report | 查 surefire 和状态，不改断言迎合实现 | TEST-QUALITY-001 |
| TC-003 | SAC-004 | DEC-006, API-001 至 API-004, MOD-001, MOD-003, MOD-004 | 5 API、全部 Controller/Feign | P0 | API | AUTO | 反射/MockMvc endpoint fixture | 权限注解和显式 binding | 方法、字段、泛型、verb/path/binding/权限 | Workflow Maven test | baseline report | 任一指纹差异阻断 | TEST-QUALITY-001 |
| TC-004 | SAC-005 | DEC-007, DB-001, DB-002, FLOW-004, DM-004 | 单一纯 DDL V1、metadata 和新库启动 | P0 | DB/API | AUTO | 独立新 MySQL | 正式默认 | schema 指纹、零 Flyway DML、12 metadata、health | Maven test+CLI backend start | baseline report | 丢弃新库修复后重建 | DB-MIGRATION-001 |
| TC-005 | SAC-005 | DEC-008, DB-003, DB-004, SEC-004, UI-004 | 正式/Demo 声明、幂等同步和三套示例 API | P1 | RESOURCE/API | AUTO | 默认和显式 Demo 新库 | tenant 1/admin，默认无 Demo | 目录、类型、JSON/pageKey、已发布定义、重复 0 | Maven test+CLI/API | baseline report | resource key/依赖差异阻断 | RESOURCE-REGISTRY-001 |

## 11. 兼容、发布与能力文档影响

| 影响ID | 设计项ID | 影响对象 | 当前行为 | 目标行为 | 兼容策略 | 升级或回滚 | README或能力地图 | 发布批次 | 验证 | 责任人 |
|---|---|---|---|---|---|---|---|---|---|---|
| IMP-001 | DEC-003 至 DEC-006 | Java/HTTP/Feign 调用方 | 使用既有方法、字段、路径和 R 响应 | 外部可观察行为不变，内部边界规范 | 指纹和同组测试保护 | PR 未合并可关闭；已合并用新提交修复 | Workflow README | 单 PR | TC-001 至 TC-003 | Tech Lead |
| IMP-002 | DEC-007 | 新环境数据库 | V1-V4 且 Flyway 含 DML | 单一纯 DDL V1+正式必需 metadata initializer | 只支持新库，无旧 history 兼容窗口 | 丢弃新库重建 | Workflow README、capability map | 同批 | TC-004 | DBA |
| IMP-003 | DEC-008 | 示例流程和部署人员 | sample runner 默认开启 | Demo 资源显式启用 | definitionKey/pageKey/JSON 语义保持 | 关闭 Demo 或新库重建 | Workflow README、前端 README | 同批 | TC-005 | Workflow owner |
| IMP-004 | MOD-005, UI-001 至 UI-004 | Workflow 前端 | 消费现有 HTTP 与 pageKey | 生产代码不变，只修正文档事实 | 后端 endpoint 和示例冒烟 | 无前端发布回滚 | 前端 README、capability map | 同批 | TC-003, TC-005 | Frontend owner |

## 12. 技术追踪矩阵

| 上游ID | 设计项ID | 测试用例ID | 覆盖说明 |
|---|---|---|---|
| SC-001, SA-001, FR-001, UC-001, PG-001, BT-001, DR-001, NFR-001, SAC-001 | DEC-001, DEC-002, DEC-003, DEC-004, DEC-005, MOD-001, MOD-002, MOD-003, DM-001, FLOW-001, API-001, DB-001, SEC-001, ERR-001, UI-001, IMP-001 | TC-001, TC-003 | 覆盖定义、服务、持久化、权限和接口兼容 |
| SC-002, SA-002, SA-003, SA-004, FR-002, FR-003, FR-004, UC-002, UC-003, PG-002, PG-003, BT-002, BT-003, DR-002, DR-003, IR-001, IR-003, NFR-002, NFR-003, SAC-002, SAC-003 | DEC-006, DEC-009, MOD-004, DM-002, DM-003, FLOW-002, FLOW-003, API-002, API-003, API-004, SEC-002, SEC-003, ERR-002, ERR-003, UI-002, UI-003 | TC-002, TC-003 | 覆盖发起、查询、全部任务动作、事件和快照 |
| FR-005, SAC-004 | MOD-001, MOD-003, MOD-004, API-001, API-002, API-003, API-004, UI-001, UI-002, UI-003, IMP-001 | TC-003 | 覆盖 Java、HTTP、Feign、权限和前端消费兼容 |
| SC-003, SA-005, FR-006, UC-004, DR-004, IR-002, NFR-004, SAC-005 | DEC-007, DEC-008, MOD-002, MOD-003, MOD-005, DM-004, FLOW-004, DB-002, DB-003, DB-004, SEC-004, ERR-004, UI-004, IMP-002, IMP-003, IMP-004 | TC-004, TC-005 | 覆盖单一 V1、必需 metadata、正式/Demo 分层和示例页 |

## 13. 阶段判定与审批

| 检查项 | 结果 | 证据 |
|---|---|---|
| 技术设计 checker | PASS | `node mango-pmo/tools/check-technical-design.mjs --document mango-docs/designs/workflow-architecture-debt/technical-design.md` |
| 生命周期 handoff | PASS | `node mango-pmo/tools/check-lifecycle-handoff.mjs --brd ... --srs ... --tdd ... --risk L3 --through tdd` |
| 专项规范检查计划 | PASS | TC-003 覆盖 backend/03-api；TC-004 覆盖数据库迁移；TC-005 覆盖资源登记；只做 Workflow 定向验证 |
| 未关闭阻断数量 | 0 | DEC-001 至 DEC-009 均有风险与回退条件 |
| Tech Lead 审批 | APPROVED | `review/TDD-WORKFLOW-DEBT.md` |
