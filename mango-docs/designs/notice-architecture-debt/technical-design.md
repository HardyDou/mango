---
documentId: TDD-NOTICE-DEBT
documentType: technical-design
pmoVersion: 1.2.4
schemaRevision: 1
riskLevel: L3
riskAssessmentEvidence: 一次性调整 Notice 公共协议、HTTP 与远程适配、服务事务边界、十七类租户实体、二十张表最终结构、六类渠道 SPI 和资源初始化，属于核心链路高风险重构
status: APPROVED
action: NEXT
owner: Mango Notice Tech Lead
approver: HardyDou
approvalEvidence: review/TDD-NOTICE-DEBT.md
upstreamDocumentId: SRS-NOTICE-DEBT
upstreamDocumentHash: 1233460e4a29a6346ac8f437f099cd165aaa97c6b199080fced46f392e85069a
---

# Notice 历史债务治理技术设计

## 1. 设计输入、约束与决策

| 决策ID | 问题 | 候选方案 | 选择 | 理由 | 来源ID或路径 | 是否推断 | 影响 | 风险 | 回退条件 |
|---|---|---|---|---|---|---|---|---|---|
| DEC-001 | 实际架构扫描 663 条且历史预算 697 条已漂移 | 按旧预算；按本次实际扫描 | 以同一基线提交上的 663 条实际结果为 before，报告同时解释预算差异 | 可复核且不会把已消失问题算入完成量 | NFR-003；architecture report | 否 | 验收统计 | 扫描范围变化导致不可比 | before/after 必须使用同一模块集合和规则版本 |
| DEC-002 | 71 条测试缺少公共契约和最终结构保护 | 直接重构；先补大量参数用例；先补少量高价值特征测试 | 先补 API/HTTP/Feign、权限、迁移和关键服务行为测试，再在生产代码未变时形成增强 before | 控制用例数量并保护最易回归的边界 | SAC-001 至 SAC-005 | 否 | 测试基线 | 测试实现复制生产逻辑 | 断言必须观察公开结果或真实持久化副作用 |
| DEC-003 | NoticeService 3165 行混合配置、发送、记录、账户、站内信和同步 | 保留大类；增加转发包装；按业务事务边界拆分 | 拆为配置、投递、记录运维、接收设置、站内信动作和企业同步等窄服务；Controller 组合接口；不建立无意义包装层 | 降低耦合并保留既有事务和副作用顺序 | FR-001 至 FR-004；core service | 否 | Core 结构 | 拆分时事务或调用顺序改变 | 同组集成测试任一状态/副作用差异即停止 |
| DEC-004 | 大量服务未利用统一 CRUD 能力且命名边界不一致 | 所有服务强制 CRUD；完全不用；只在真实 CRUD 聚合使用 | 业务类型、渠道配置、收件账户和公告等真实 CRUD 服务继承 MangoCrudService；投递、重试、动作和同步保留领域服务 | 统一简单 CRUD 而不把复杂事务伪装成 CRUD | FR-001, FR-003, FR-004；backend/05-module | 否 | Service/Mapper | 过度泛化隐藏业务规则 | 复杂服务不允许只为过规则增加 CRUD 继承 |
| DEC-005 | 历史 HTTP 使用路径变量，Controller/Feign 与页面各自拼接 | 保留旧路径；双路由；唯一固定路径 | 标识使用显式 query，复杂输入使用 body；Controller、Feign 和仓内前端同批切换，不保留双路由 | 与 Payment 已批准政策一致并清除 PATH/ADAPTER 级联债务 | FR-005, SAC-004；backend/03-api | 否 | HTTP 和前端调用 | 外部调用方需整体升级 | 契约目录、Feign parity 和前端请求测试必须全部通过 |
| DEC-006 | Entity、Mapper、协议模型和渠道 SPI 不符合当前边界 | 只做抑制；局部修补；统一最终命名和类型 | 17 个租户实体继承 TenantEntity；Mapper 统一注解和泛型；注解 SQL 入 XML；ChannelSendCommand 改为不冒充公开命令的 NoticeChannelMessage；record 改不可变类 | 消除持久化、模型和 SPI 正式债务，保持表字段和访问器语义 | DR-001 至 DR-003；backend/07-persistence | 否 | API/Core/Support/Channels | 类型迁移遗漏或序列化差异 | 编译、反射契约和渠道测试任一失败即停止 |
| DEC-007 | V1-V17 混合最终结构和 DML | 保留历史；新增 V18；折叠最终态 V1 | 只保留单一 `V1__init_notice.sql`，等价建立 20 张最终表且只含 DDL；历史回填直接体现在最终列定义 | 用户只使用新库，单一最终态最清晰 | FR-006, SAC-005；db/migration/notice | 否 | 新数据库 | 最终结构遗漏索引或约束 | 基线与 after schema 指纹不同即停止 |
| DEC-008 | 管理员联系方式和模板更新留在结构脚本 | 全部转 Demo；全部转正式资源；按数据属性分层 | 个人联系方式不初始化；正式域、菜单、内置站内渠道和模板留在 notice 前缀正式资源；演示内容仅进 Demo；运行态任务、记录、消息、公告不初始化 | 符合 Flyway 仅 DDL、正式必需/Demo/运行态分层 | FR-006, IR-002；META-INF/mango | 否 | 初始化数据 | 必需模板遗漏导致发送失败 | 正式新库启动、资源声明和数据集合验证失败即停止 |
| DEC-009 | Workflow 新 DTO 接口仍被 Payment 以 Map 调用，完整应用当前无法编译 | 忽略到后续；混入 Notice 重构；作为终验前兼容修复 | 在 Notice 完整启动前做最小 Payment 消费者同步并运行 Payment 定向编译/测试，单独提交说明 | 这是前一模块遗留的真实消费者兼容问题，必须恢复主应用可编译 | SAC-005；PaymentRefundApprovalService | 否 | 完整应用启动 | 修复改变退款审批变量 | 只允许等价构造 WorkflowJsonRequest，Payment 定向测试不通过即停止 |

## 2. 模块与依赖边界

| 模块设计ID | 模块或包 | 职责 | 改动类型 | 依赖方向 | 公开能力 | 系统需求ID | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|
| MOD-001 | mango-notice-api | 唯一 Java 协议、Command/Query/VO、错误码和资源声明 | 协议规范化 | 无 Core/Starter 依赖 | NoticeApi、NoticeAnnouncementApi、事件 | FR-001 至 FR-005 | MANGO-ARCH-API/MODEL/OPENAPI | API 反射契约与编译 |
| MOD-002 | mango-notice-core | 领域服务、事务、持久化、投递编排、Outbox 与最终 DDL | 拆分聚合并规范持久化 | 依赖 API、Support 和通用能力 | I*Service 与领域事件处理 | FR-001 至 FR-004, FR-006 | MANGO-ARCH-SVC/ENTITY/MAPPER | H2 集成、迁移和架构测试 |
| MOD-003 | mango-notice-starter | HTTP 适配、权限、自动配置和正式资源 | 纯适配与资源分层 | 依赖 API/Core/渠道实现 | 固定 HTTP 入口 | FR-001 至 FR-006 | MANGO-ARCH-CTRL/ADAPTER/PATH | MVC 契约、资源和启动测试 |
| MOD-004 | mango-notice-starter-remote | 远程声明与事件转发 | 与 API/HTTP 精确对齐 | 只依赖 API | NoticeFeignClient | FR-005 | MANGO-ARCH-FEIGN/ADAPTER | Feign parity 测试 |
| MOD-005 | mango-notice-support 与六个 channel 模块 | 稳定渠道 SPI 和各提供方实现 | SPI 重命名、模型不可变 | Channel 只依赖 API/Support | NoticeChannelSender | FR-002 | MANGO-ARCH-MODEL/ADAPTER | 六类渠道测试 |
| MOD-006 | mango-ui/packages/notice 及实际调用点 | 消费唯一固定 HTTP 入口 | URL 与 query/body 同步 | 只依赖公开 HTTP | 通知管理与个人中心页面 | FR-004, FR-005 | frontend API contract | 定向类型检查、构建和请求契约 |

## 3. 技术对象与状态模型

| 模型ID | 上游ID | 模型职责 | 标识 | 关系 | 状态编码 | 审计或历史 | 归属或租户 | 一致性约束 |
|---|---|---|---|---|---|---|---|---|
| DM-001 | FR-001, DR-001 | 业务类型、配置版本、渠道模板与账户 | 各表 id、业务编码、版本、渠道类型 | 业务类型一对多版本和模板 | 草稿、发布、启停 | TenantEntity 审计和逻辑删除 | tenant_id、domain_code | 当前发布版本、模板和渠道路由必须相互一致 |
| DM-002 | FR-002, FR-003, DR-002 | 任务、接收快照、发送记录与 Outbox | taskId、recordId、eventId、idempotencyKey | 一任务多目标多记录 | pending、processing、success、failed、ignored | 保留原时间与处理原因 | tenant_id 与业务上下文 | 记录推进一次，任务汇总从记录确定 |
| DM-003 | FR-004, DR-003 | 站内消息、动作、公告、目标与确认 | messageId、actionCode、announcementId、userId | 消息多动作、公告多目标与确认 | unread/read/deleted、draft/published/offline | TenantEntity 审计 | tenant_id、recipient_user_id | 本人归属、动作幂等和公告确认唯一 |

| 模型ID | 当前状态 | 触发 | 目标状态 | 前置条件 | 副作用 | 失败处理 | 上游ID |
|---|---|---|---|---|---|---|---|
| DM-001 | DRAFT | 发布配置 | PUBLISHED | 校验、模板与路由完整 | 激活版本供后续发送 | 事务回滚且旧版本不变 | FR-001, SAC-001 |
| DM-002 | PENDING/FAILED | 发送或重试成功 | SUCCESS | 状态允许且渠道返回成功 | 更新记录、任务汇总和 Outbox | 保留失败原因与既有重试状态 | FR-002, FR-003, SAC-002 |
| DM-003 | UNREAD/PENDING | 阅读或完成动作 | READ/COMPLETED | 当前用户拥有消息且动作可执行 | 更新未读数和动作请求结果 | 不改变状态并返回业务错误 | FR-004, SAC-003 |

## 4. 系统流程、事务与一致性

| 流程设计ID | 系统需求ID | 调用入口 | 参与模块 | 处理顺序 | 事务边界 | 状态变化 | 幂等键 | 并发策略 | 外部失败与补偿 | 用户可见结果 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| FLOW-001 | FR-001, UC-001, SAC-001 | 配置保存/发布 | MOD-001, MOD-002, MOD-003 | 校验→保存草稿→发布/激活→返回遮蔽 VO | 单次配置动作事务 | DM-001 状态推进 | 租户+业务编码+版本 | 唯一约束与条件更新 | 失败整体回滚 | 对象、版本、状态或原错误 |
| FLOW-002 | FR-002, UC-002, SAC-002 | 通知发送 | MOD-001, MOD-002, MOD-003, MOD-005 | 校验→幂等查重→任务/目标/记录→Outbox→渠道发送→汇总 | 任务创建与 Outbox 同事务，外部发送按原边界 | DM-002 推进 | tenant+idempotencyKey | 唯一约束、记录状态条件更新 | 渠道失败落记录并按原策略重试 | 任务标识和发送结果 |
| FLOW-003 | FR-003, FR-004, UC-002, UC-003 | 记录或本人消息动作 | MOD-001, MOD-002, MOD-003 | 归属/状态校验→条件更新→汇总或业务动作→返回 | 每次动作单事务 | DM-002 或 DM-003 推进 | recordId+action、messageId+actionCode | 条件更新防重复 | 外部动作待完成时保留请求状态 | 布尔结果、动作请求或原错误 |
| FLOW-004 | FR-006, UC-004, SAC-005 | 新库启动 | MOD-002, MOD-003 | 纯 DDL V1→正式资源→自动配置→health；Demo 仅显式加载 | 各初始化器保持既有边界 | 建立空正式环境 | 资源 type+key | 资源登记幂等 | 任一步失败停止并重建新库 | 健康 UP 与明确数据集合 |

## 5. API 与远程契约设计

| 接口ID | 系统需求ID | 调用方 | 所属模块 | 入口类型 | 方法与路径 | Command Query或VO | 返回契约 | 校验 | 权限租户或数据权限 | 幂等分页或排序 | 错误码 | 兼容策略 | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| API-001 | FR-001, SAC-001 | 管理前端、Java 调用方 | MOD-001, MOD-003, MOD-004 | 业务配置 | PUT /notice/business-types | `Long id` query + `UpdateNoticeBusinessTypeCommand` body；其它配置动作使用固定子路径 | R<NoticeBusinessTypeVO> | Bean Validation + Service Require | 原 permission、tenant、domain 数据条件 | 版本和唯一性不变 | NoticeCode 保持数值和消息 | 历史 id 路径改显式 query；完整目录由契约测试覆盖 | MANGO-ARCH-API/PATH/CTRL/FEIGN | API/MVC/Feign 指纹和权限测试 |
| API-002 | FR-002, SAC-002 | 业务系统 | MOD-001, MOD-003, MOD-004 | 通知发送 | POST /notice/send | `SendNoticeCommand` body | R<NoticeSendResultVO> | 业务编码、幂等键、目标和内容校验 | 原创建权限、租户和业务域 | 幂等键不变 | NoticeCode | 路径与 body 保持，Controller/Feign 精确一致 | MANGO-ARCH-API/CTRL/FEIGN/ADAPTER | 发送集成与 parity 测试 |
| API-003 | FR-003, SAC-002 | 运维前端、远程调用方 | MOD-001, MOD-003, MOD-004 | 发送记录动作 | POST /notice/records/retry | `Long id` query；批量使用 `RetryNoticeSendRecordsCommand` body | R<Boolean> | id、批量集合和记录状态校验 | 原 retry permission 与租户归属 | 状态条件更新防重复 | NoticeCode | retry/manual-success/ignore 均改固定路径+query/body | MANGO-ARCH-PATH/API/CTRL/FEIGN | 单条批量状态与 parity 测试 |
| API-004 | FR-004, SAC-003 | 个人通知中心 | MOD-001, MOD-003, MOD-004 | 本人消息 | GET /notice/site/my/messages/detail | `Long id` query；动作使用固定 execute/read/delete 路径 | R<NoticeSiteMessageVO> | id、actionCode 和 body 校验 | 原 site permission、当前用户和租户归属 | 分页排序、动作幂等不变 | NoticeCode | 仓内页面同批升级，业务结果不变 | MANGO-ARCH-PATH/API/CTRL/FEIGN | 多用户权限、未读和路由测试 |
| API-005 | FR-004, SAC-003 | 公告管理与个人中心 | MOD-001, MOD-003 | 公告 | POST /notice/announcements/publish | `PublishNoticeAnnouncementCommand` body | R<Boolean> | id、目标和发布时间校验 | 原 announcement/site permission 与租户归属 | 发布/确认幂等 | NoticeCode | 既有固定路径和 query/body 保持 | MANGO-ARCH-API/CTRL | 公告集成和权限测试 |
| API-006 | FR-005, SAC-004 | Notice 前端包 | MOD-006 | TypeScript HTTP client | GET /notice/site/my/messages/detail | `ApiId id` query | R<NoticeSiteMessageVO> | 页面表单与后端约束对齐 | 菜单按钮与接口权限保持 | 分页、排序和重复提交不变 | 展示后端明确错误 | 按 API-001 至 API-005 唯一目录更新，所有主键保持字符串安全 | frontend API contract | 定向类型检查、构建和请求测试 |

## 6. 持久化与数据迁移设计

| 数据设计ID | 上游或模型ID | 表或实体 | 字段变化 | 约束 | 索引 | 租户审计 | Mapper边界 | 数据来源 | migration或回填 | 回滚或补偿 | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| DB-001 | DR-001, DM-001 | 业务配置、版本、模板、渠道与设置实体 | 保持 V17 最终字段 | 原唯一约束与外键语义 | 原查询索引 | 继承 TenantEntity | `@Mapper extends BaseMapper<Entity>`，复杂 SQL 入 XML | 运行时管理和正式资源 | V1 直接建立最终表，无 DML | 仅新库，失败删除重建 | DB-MIGRATION/PERSISTENCE | schema dump 指纹、Mapper 集成测试 |
| DB-002 | DR-002, DM-002 | task、recipient、record、outbox、wecom mapping | 保持业务上下文、domain 和接收快照字段 | 幂等键和状态约束保持 | 原任务、状态、业务查询索引 | 继承 TenantEntity | 条件更新保持原 wrapper/SQL 语义 | 运行时发送 | 历史 ALTER 折入 V1，无回填 | 删除新库重建 | DB-MIGRATION/PERSISTENCE | 发送、重试、Outbox 集成测试 |
| DB-003 | DR-003, DM-003 | site message/action、announcement/target/confirm | 保持 V15-V17 最终字段 | 用户动作与确认唯一性保持 | 原未读、发布和目标索引 | 继承 TenantEntity | 注解 SQL 迁 XML 并保留条件 | 运行时消息与公告 | 最终字段直接进入 V1 | 删除新库重建 | DB-MIGRATION/PERSISTENCE | 本人消息与公告集成测试 |
| DB-004 | FR-006 | `V1__init_notice.sql` 与资源数据 | 17 段合并为单一最终态 | SQL 只允许 DDL | 20 表最终索引一致 | 所有租户表 tenant_id 类型一致 | 不适用 | 空白新 MySQL、正式/演示声明 | 管理员邮箱手机号不迁入任何默认初始化；模板以资源声明为准 | 分支回退或新库重建 | DB-MIGRATION/RESOURCE-REGISTRY | DML 检查、schema hash、行数和启动 |

## 7. 安全、权限、租户与数据边界

| 安全设计ID | 系统需求ID | 能力 | 权限资源 | 默认授权 | 后端校验入口 | 租户边界 | 数据归属断言 | 前端反馈 | 审计 | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| SEC-001 | SA-001, FR-001, FR-003 | 管理配置和记录 | 保留 `notice:*` 现有 permission | 不扩大 | Controller 注解+Service Require | TenantEntity 与租户上下文 | 读取/修改对象 tenant 匹配 | 原权限错误 | 原创建修改审计 | API/SECURITY/PERSISTENCE | 路由权限指纹和跨租户测试 |
| SEC-002 | SA-003, FR-004, SAC-003 | 本人消息与公告 | 保留 `notice:site:*` | 不扩大 | 入口权限+Service 当前用户条件 | 当前 tenant | recipient_user_id/目标范围匹配 | 不存在或不可操作 | 阅读、动作、确认时间 | API/SECURITY | 多用户多租户集成测试 |
| SEC-003 | FR-001, DR-001 | 渠道秘密 | 管理权限 | 不回显明文 | Service 加解密和 VO mask | 当前 tenant 渠道账户 | 只能读取本租户遮蔽结果 | 显示 mask | 修改人和时间 | SECURITY | secret mask 与保存回读测试 |
| SEC-004 | FR-006, SAC-005 | 初始化数据边界 | 部署权限 | Demo 默认关闭 | Resource Registry mode | 声明按模块和租户应用 | 正式无个人联系和运行态数据 | 启动日志 | 资源同步记录 | RESOURCE-REGISTRY | 默认/Demo 数据集合测试 |

## 8. 错误码、异常与可观测性

| 错误设计ID | 系统需求ID | 失败场景 | 触发条件 | 错误码 | 异常类型 | 用户反馈 | 日志上下文 | 指标或告警 | 重试或补偿 | 敏感信息处理 |
|---|---|---|---|---|---|---|---|---|---|---|
| ERR-001 | FR-001, FR-004 | 配置、消息或公告不存在/越权/状态非法 | Require 条件失败 | 现有 NoticeCode 数值与 message | 业务异常 | 原明确原因 | tenant、对象 id、动作，不记密钥 | 保持既有日志指标 | 失败不改状态 | 不记录正文、联系方式和密钥 |
| ERR-002 | FR-002, FR-003 | 渠道失败、记录状态非法或重复处理 | sender 返回失败或条件更新失败 | 现有 NoticeCode/渠道失败语义 | 业务异常或失败结果 | 原发送/运维结果 | taskId、recordId、channel、providerCode | Outbox/发送失败指标保持 | 按原策略重试、人工成功或忽略 | 联系方式遮蔽，密钥不入日志 |
| ERR-003 | FR-006 | DDL、资源或启动失败 | 新库任一阶段失败 | 启动异常 | 基础设施异常 | 明确失败阶段 | migration version、resource type/key、workspace | 启动失败 | 修复后删除新库重建 | 不输出数据库密码和资源秘密 |

## 9. 前端结构与交互实现映射

| 前端设计ID | 系统需求ID | 页面或动作 | 页面key或路由 | 区域与组件 | 状态来源 | API依赖 | 权限或不可操作 | 空加载或失败态 | 语义测试锚点 | 复用判断 | 适用规范ruleId |
|---|---|---|---|---|---|---|---|---|---|---|---|
| UI-001 | PG-001, FR-001, FR-003 | 业务配置、渠道、任务、记录和公告管理 | 保持现有 Notice 管理路由 | 现有列表、表单和动作组件 | API-001, API-003, API-005 | API-001, API-003, API-005 | 权限码和状态禁用保持 | 保持既有空/加载/错误组件 | URL/query/body、permission、id string | 复用现有页面，只迁请求目录 | frontend API contract |
| UI-002 | PG-002, FR-004 | 个人消息、公告、动作和偏好 | 保持现有个人通知路由 | 现有消息列表、详情、未读与动作组件 | API-004, API-005 | API-004, API-005 | 本人归属和动作状态 | 保持既有五态 | 未读数、本人详情、动作完成、公告确认 | 复用现有页面 | frontend API contract |
| UI-003 | FR-005, SAC-004 | Notice 请求封装 | mango-ui Notice package | 统一固定 path 与 query/body | API-006 | API-006 | 不改变权限声明 | 后端错误透传 | 全部旧路径搜索为 0、类型检查 | 收敛重复 URL，不新增组件 | frontend API contract |

## 10. 测试设计与验收映射

| 测试用例ID | 系统验收ID | 设计项ID | 场景 | 优先级 | 测试层级 | 自动化判断 | 测试数据 | 权限或租户边界 | 稳定契约 | 执行入口 | 证据 | 失败处理 | 适用规范ruleId |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| TC-001 | SAC-001, SAC-002 | DEC-002 至 DEC-006, FLOW-001, FLOW-002, API-001 至 API-003, DM-001, DM-002 | 配置、发送、记录、Outbox、渠道请求和错误 | P0 | UNIT/INTEGRATION | AUTO | 现有 H2 fixture 与渠道替身 | tenant 1 与跨租户 | 返回、状态、版本、幂等和副作用 | Notice 十一个模块 Maven test | baseline report | 不删除或弱化断言，定位真实差异 | TEST-QUALITY-001 |
| TC-002 | SAC-003 | FLOW-003, API-004, API-005, DM-003, SEC-002 | 本人消息动作、未读数、公告发布/确认 | P0 | INTEGRATION | AUTO | 多用户消息与公告 fixture | tenant 1/2、user A/B | 归属、动作、确认和状态 | 同一 Maven test | baseline report | 任一越权或状态差异阻断 | TEST-QUALITY-001 |
| TC-003 | SAC-004 | MOD-001, MOD-003, MOD-004, MOD-006, API-001 至 API-006, UI-001 至 UI-003 | 两个 Java API、全部 Controller/Feign 和仓内 URL | P0 | API | AUTO | 反射、路由、权限和请求 fixture | 全部访问模式与 permission | 方法、字段、verb/path/binding/权限 | Maven test + Notice 前端定向检查 | baseline report | 任一目录或调用方遗漏阻断 | TEST-QUALITY-001 |
| TC-004 | SAC-005 | DEC-007, DB-001 至 DB-004, FLOW-004 | 单一纯 DDL V1、20 表 schema 和正式新库启动 | P0 | DB/API | AUTO | 独立新 MySQL | 默认正式模式 | schema hash、零 Flyway DML、health | Maven migration test + CLI backend start | baseline report | 丢弃新库修复后重建 | DB-MIGRATION-001 |
| TC-005 | SAC-005 | DEC-008, SEC-004, UI-003 | 正式/Demo 声明和默认数据集合 | P1 | RESOURCE/API | AUTO | 默认与显式 Demo 声明 | 默认无个人联系与运行态数据 | 目录、类型、mode、数据行数 | Maven resource test + 新库查询 | baseline report | 声明或边界差异阻断 | RESOURCE-REGISTRY-001 |
| TC-006 | SAC-005 | DEC-009 | Workflow→Payment 消费者兼容和完整应用启动 | P0 | INTEGRATION | AUTO | 退款审批 JSON 与独立新库 | 租户 1 测试上下文 | Payment 编译测试、JSON 等价、health UP | Payment 定向 test + monolith start | baseline report | 仅等价修复，否则停止 | TEST-QUALITY-001 |

## 11. 兼容、发布与能力文档影响

| 影响ID | 设计项ID | 影响对象 | 当前行为 | 目标行为 | 兼容策略 | 升级或回滚 | README或能力地图 | 发布批次 | 验证 | 责任人 |
|---|---|---|---|---|---|---|---|---|---|---|
| IMP-001 | DEC-003 至 DEC-006, API-001 至 API-006 | Java/HTTP/Feign/前端调用方 | 大服务、路径变量和分散 URL | 窄服务与唯一固定路径，业务语义不变 | 仓内调用方同批升级；无双协议 | 整体升级或整体回退该 PR | Notice README、前端 README、capability map | 单 PR | TC-001 至 TC-003 | Notice owner |
| IMP-002 | DEC-007, DEC-008, DB-004 | 新环境数据库和资源 | V1-V17 且含 DML | 单一纯 DDL V1、正式/Demo/运行态分层 | 只支持新库 | 删除新库并用旧版本重建 | Notice README、capability map | 同批 | TC-004, TC-005 | DBA/Notice owner |
| IMP-003 | DEC-009 | Payment 退款审批调用方 | 使用旧 Map 调用新 Workflow DTO | 等价 DTO 构造并恢复单体编译 | 只修直接消费者，不扩 Payment 重构 | 回退该最小提交 | Payment/Workflow 兼容说明写入报告 | Notice 终验前 | TC-006 | Payment owner |

## 12. 技术追踪矩阵

| 上游ID | 设计项ID | 测试用例ID | 覆盖说明 |
|---|---|---|---|
| SC-001, SA-001, FR-001, UC-001, PG-001, BT-001, DR-001, NFR-001, SAC-001 | DEC-001, DEC-002, DEC-003, DEC-004, DEC-006, MOD-001, MOD-002, MOD-003, DM-001, FLOW-001, API-001, DB-001, SEC-001, SEC-003, ERR-001, UI-001, IMP-001 | TC-001, TC-003 | 覆盖配置、服务、持久化、权限和调用契约 |
| SC-002, SA-002, FR-002, FR-003, UC-002, BT-002, DR-002, IR-001, NFR-003, SAC-002 | DEC-003, DEC-004, DEC-006, MOD-002, MOD-005, DM-002, FLOW-002, FLOW-003, API-002, API-003, DB-002, SEC-001, ERR-002, UI-001 | TC-001, TC-003 | 覆盖发送、记录、渠道、幂等与运维动作 |
| SA-003, FR-004, UC-003, PG-002, BT-003, DR-003, NFR-002, SAC-003 | MOD-001, MOD-002, MOD-003, DM-003, FLOW-003, API-004, API-005, DB-003, SEC-002, ERR-001, UI-002 | TC-002, TC-003 | 覆盖本人消息、公告、动作和权限租户 |
| FR-005, SAC-004 | DEC-005, MOD-003, MOD-004, MOD-006, API-001, API-003, API-004, API-006, UI-001, UI-002, UI-003, IMP-001 | TC-003 | 覆盖唯一 HTTP/Feign/前端目录及仓内消费者同步 |
| SC-003, SA-004, FR-006, UC-004, IR-002, NFR-004, SAC-005 | DEC-007, DEC-008, DEC-009, MOD-002, MOD-003, DB-004, FLOW-004, SEC-004, ERR-003, IMP-002, IMP-003 | TC-004, TC-005, TC-006 | 覆盖单一 V1、资源分层、兼容修复和完整启动 |

## 13. 阶段判定与审批

| 检查项 | 结果 | 证据 |
|---|---|---|
| 技术设计 checker | PASS | `node mango-pmo/tools/check-technical-design.mjs --document mango-docs/designs/notice-architecture-debt/technical-design.md` |
| 生命周期 handoff | PASS | BRD、SRS 已批准且 upstream hash 精确匹配 |
| 专项规范检查计划 | PASS | TC-003 覆盖 API/HTTP/Feign/前端；TC-004 覆盖数据库迁移；TC-005 覆盖资源登记；仅做 Notice 与必要 Payment 定向验证 |
| 未关闭阻断数量 | 0 | DEC-001 至 DEC-009 均定义风险和停止条件 |
| Tech Lead 审批 | APPROVED | `review/TDD-NOTICE-DEBT.md` |
