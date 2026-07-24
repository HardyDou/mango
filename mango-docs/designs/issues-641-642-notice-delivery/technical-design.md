---
documentId: TDD-NOTICE-641-642
documentType: technical-design
pmoVersion: 1.3.5
schemaRevision: 1
riskLevel: L3
riskAssessmentEvidence: requirement=L3，附件假成功、错误发件身份和 Secret 泄露影响核心业务与安全；solution=L3，方案跨 File、Notice API/Support/Core/Channel/Starter、Resource、数据库和 npm 前端公共包并包含增量迁移；final=max(requirement,solution)
status: APPROVED
action: NEXT
owner: Mango Notice Tech Lead
approver: HardyDou
approvalEvidence: review/APPROVAL.md
upstreamDocumentId: SRS-NOTICE-641-642
upstreamDocumentHash: bc7ab1c1c12dcbb6532ce4b2f9aac76932bac793d297811724cc56518aca9a41
---

# Notice 邮件附件与路由账号组技术设计文档

## 1. 设计输入、约束与决策

| 决策ID | 问题 | 候选方案 | 选择 | 理由 | 来源ID或路径 | 是否推断 | 影响 | 风险 | 回退条件 |
|---|---|---|---|---|---|---|---|---|---|
| DEC-001 | EMAIL 如何读取附件 | 访问 URL；直接存储；File 服务内读取 | 注入可选 `IFileContentProvider` 并调用 `downloadForService` | 复用租户上下文和文件能力，不传播临时 URL；无附件时不强制装配 File | FR-001, IR-001 | 否 | email 模块增加 file-api 依赖 | 文件能力缺失或流未关闭 | 有附件失败，无附件走原路径 |
| DEC-002 | MIME 与资源限制 | 一次性字符串；流式边界；受限内存模型 | 读取到受总上限约束的字节数组并生成 RFC 兼容 multipart/mixed | 当前原始总上限 25 MiB，可可靠校验内容长度、编码和测试 MIME | FR-001, FR-002, NFR-002 | 否 | EMAIL 配置增加附件策略 | 边界绕过或内存压力 | 硬限制、上限加一读取、受控并发和超时 |
| DEC-003 | Resource 与 Secret 如何隔离 | configJson 掩码合并；全平台 Secret 中心；Notice 分层 | 分离非敏感配置、Secret 引用和人工 Secret，增加 Notice Secret resolver SPI | 防止同步覆盖和查询泄露，且不扩大为通用平台 | FR-003, FR-004, NFR-001 | 否 | DB/API/Resource/运行时合并变化 | 旧 JSON 迁移或引用失败 | 无法解析标记 INCOMPLETE，保留原始数据供修复 |
| DEC-004 | 多账号标签如何建模 | JSON 数组；单字段；标签表与关联表 | 独立标签表和账号多对多关联 | 支持稳定编码、展示名、引用保护、多账号多标签和查询 | FR-004, FR-005, FR-007 | 否 | 新表与标签 API/UI | 迁移和关联复杂度 | 旧账号默认无标签，不影响 EXACT/AUTO |
| DEC-005 | 模板如何表达路由 | 两个可空字段推断；显式模式 | `EXACT/TAG/AUTO` 枚举加互斥字段 | 后端可判定、兼容旧数据、禁止 TAG 隐式回退 | FR-005, FR-006 | 否 | 模板/API/路由算法变化 | 旧客户端未传模式 | 根据旧 channelConfigId 兼容推导 |
| DEC-006 | 候选账号和故障切换 | 仅权重；仅优先级；优先级+权重+健康 | 优先级升序，同组健康优先和稳定权重轮换，可重试失败切换候选 | 满足主备、分流和故障切换且保留失败账号恢复机会 | FR-006, NFR-003 | 否 | Delivery 路由重构 | 错误身份或永久排除 | 候选范围先由模式硬过滤，FAILED 只降序不排除 |
| DEC-007 | 1.0.25 升级如何迁移 | 只改 V1；增量 V2；应用启动回填 | V2 增量迁移并同步 V1 fresh schema；已知 Secret 迁移，异常标记不完整 | 已发布数据库不会重跑 V1，必须有增量 migration | SAC-003, SAC-005 | 否 | DB schema 和升级说明 | JSON 异常或旧数据漂移 | 备份、事务 DDL 边界、失败停止，不自动降级 |

## 2. 模块与依赖边界

| 模块设计ID | 模块或包 | 职责 | 改动类型 | 依赖方向 | 公开能力 | 系统需求ID | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|
| MOD-001 | mango-notice-api | 路由模式、Secret 状态、标签、渠道与模板 Command/VO | 扩展 | 无 transport 依赖 | 增量公开契约 | FR-004, FR-005, FR-007 | `rules/backend/03-api.md` | API 契约与编译测试 |
| MOD-002 | mango-notice-support | 渠道消息、结果摘要和 Secret resolver SPI | 扩展 | api、resource-api | 稳定 sender/SPI 合同 | FR-002, FR-003 | `rules/backend/01-code.md` | 单元与消费者编译 |
| MOD-003 | mango-notice-core | 配置分层、Resource 合并、标签、路由、引用保护、持久化 | 扩展/重构 | api/support/resource support | 内部服务与 handler | FR-003 至 FR-007 | `rules/backend/10-dev-flow.md` | 单元、H2 集成、定向 verify |
| MOD-004 | mango-notice-channel-email | 附件加载、限制、MIME 和 SMTP | 扩展 | notice-support、mango-file-api | EMAIL sender | FR-001, FR-002 | `rules/backend/01-code.md` | MIME 单元与 SMTP/File 集成 |
| MOD-005 | mango-notice-starter/remote | 标签与影响 API 适配、公开契约 parity | 扩展 | api/core | HTTP/Feign 入口 | FR-004, FR-005, FR-007 | `rules/backend/03-api.md` | MVC/Feign parity 测试 |
| MOD-006 | @mango/notice | 渠道来源/Secret/标签维护和三模式选择 | 扩展 | Notice HTTP API | npm 页面与类型 | PG-001, PG-002 | `rules/frontend/01-vue-code.md` | typecheck、组件、构建、UI 验证 |

## 3. 技术对象与状态模型

| 模型ID | 上游ID | 模型职责 | 标识 | 关系 | 状态编码 | 审计或历史 | 归属或租户 | 一致性约束 |
|---|---|---|---|---|---|---|---|---|
| DM-001 | DR-001, FR-001 | 内存中的已验证附件 | fileId | 属于单个发送记录 | RESOLVED/FAILED | 只写安全摘要，不持久化内容 | 当前任务租户 | 全部成功才进入 EmailMessage |
| DM-002 | DR-002, FR-003, FR-004 | 渠道稳定身份、配置分层和来源 | tenantId+configCode | 多对多标签；被模板精确引用 | configStatus、secretStatus、healthStatus | Resource id/version/source、受控字段、更新时间 | tenantId | configCode 创建后不可改，Secret 值不出 VO |
| DM-003 | DR-003, FR-004, FR-007 | 稳定路由标签和账号组 | tenantId+channelType+tagCode | 多对多渠道账号；被模板 TAG 引用 | ACTIVE | 来源和更新时间 | tenantId | tagCode 创建后不可改且被引用不可删 |
| DM-004 | DR-004, FR-005, FR-006 | 模板路由范围 | templateId | 可引用一个账号或一个 tagCode | EXACT/TAG/AUTO | 随模板版本和发布记录 | tenantId/bizType | 模式字段组合互斥 |

| 模型ID | 当前状态 | 触发 | 目标状态 | 前置条件 | 副作用 | 失败处理 | 上游ID |
|---|---|---|---|---|---|---|---|
| DM-001 | RESOLVING | 全部附件通过读取和限制 | RESOLVED | 每个 FileDownloadVO 有安全元数据和受限内容 | 进入 MIME 组装 | 关闭已打开流并返回附件失败摘要 | FR-001 |
| DM-002 | INCOMPLETE | 必需非敏感配置和 Secret 均可解析 | COMPLETE | provider schema 校验通过 | 允许参与路由 | 保存缺失键和解析状态，不保存解析值 | FR-003, FR-004 |
| DM-002 | COMPLETE | 必需字段被删除或引用解析失败 | INCOMPLETE | 同步、补录或环境变化后重算 | 从候选账号排除 | 保留可修复配置和审计 | FR-003 |
| DM-004 | AUTO | 管理员保存精确账号 | EXACT | channelConfigId 同租户、同渠道、启用且完整 | routeTagCode 清空 | 事务回滚并返回校验错误 | FR-005 |
| DM-004 | EXACT | 管理员保存路由标签 | TAG | tagCode 同租户、同渠道且有候选 | channelConfigId 清空 | 无候选拒绝，不转 AUTO | FR-005 |

## 4. 系统流程、事务与一致性

| 流程设计ID | 系统需求ID | 调用入口 | 参与模块 | 处理顺序 | 事务边界 | 状态变化 | 幂等键 | 并发策略 | 外部失败与补偿 | 用户可见结果 |
|---|---|---|---|---|---|---|---|---|---|---|
| FLOW-001 | FR-001, FR-002 | Notice worker 发送记录 | MOD-002, MOD-003, MOD-004 | 路由账号→物化 Secret→解析附件→生成 MIME→SMTP→保存结果/健康 | 文件/SMTP 在数据库事务外，结果更新使用现有记录边界 | PENDING/SENDING 到 SUCCESS/RETRY_WAITING/FAILED | sendRecordId/requestId | 单记录 claim；附件受控执行器和每文件超时 | SMTP 前失败不发送；临时 File/SMTP 失败按原重试 | 成功摘要或稳定失败原因 |
| FLOW-002 | FR-003 | Resource upsert | MOD-003 | 校验声明→定位 configCode→分离/合并字段→同步 tag→计算完整性→返回 target | 单 Resource 数据库事务 | INCOMPLETE/COMPLETE | resourceId+version | configCode 唯一索引，更新按主键 | 任一步失败回滚，不清空原 Secret | 同步结果和完整状态 |
| FLOW-003 | FR-004, FR-007 | 渠道/标签管理 API | MOD-001, MOD-003, MOD-005 | 校验租户/权限→查询来源与引用→保存非敏感/只写 Secret或拒绝→返回脱敏 VO | 单管理命令事务 | 配置、Secret 状态、标签关系变化 | config id / tag code | 唯一索引与引用检查 | 冲突回滚并返回影响摘要 | 页面显示状态或不可操作原因 |
| FLOW-004 | FR-005, FR-006 | 模板保存与发送路由 | MOD-001, MOD-003, MOD-005 | 保存时严格互斥校验；发送时按模式查候选→优先级/健康/权重排序→逐账号尝试 | 模板保存单事务；外部发送不包事务 | 模板模式与发送记录实际账号 | template id / sendRecordId | 稳定 seed 轮换；记录 claim | TAG 无候选失败；可重试账号失败切换，消息错误终止 | 模式与实际账号可核对 |

## 5. API 与远程契约设计

| 接口ID | 系统需求ID | 调用方 | 所属模块 | 入口类型 | 方法与路径 | Command Query或VO | 返回契约 | 校验 | 权限租户或数据权限 | 幂等分页或排序 | 错误码 | 兼容策略 | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| API-001 | FR-004 | 管理页面 | MOD-001, MOD-005 | HTTP+Java | GET /notice/channels | NoticeChannelConfigPageQuery/NoticeChannelConfigVO | R<PageResult> | 查询条件合法 | 现有 notice:channel 权限和租户上下文 | 现有分页与排序 | CHANNEL_CONFIG_INVALID | configJson 仅返回非敏感值 | `rules/backend/03-api.md` | MVC、Feign、JSON 敏感断言 |
| API-002 | FR-004 | 管理页面 | MOD-001, MOD-005 | HTTP+Java | POST /notice/channels | SaveNoticeChannelConfigCommand | R<NoticeChannelConfigVO> | configCode、routeTagCodes、secretValues | 现有 notice:channel 权限和租户上下文 | configCode 保证幂等身份 | CHANNEL_CONFIG_INVALID/REFERENCE_CONFLICT | 旧字段继续接受 | `rules/backend/03-api.md` | MVC、Feign、JSON 敏感断言 |
| API-003 | FR-004, FR-007 | 管理页面 | MOD-001, MOD-005 | HTTP+Java | GET /notice/channel-route-tags | NoticeRouteTagQuery | R<NoticeRouteTagListVO> | channelType 和筛选条件 | notice:channel 权限和租户 | 按渠道类型、编码排序 | ROUTE_TAG_INVALID | 新增接口 | `rules/backend/03-api.md` | API 与集成测试 |
| API-004 | FR-004, FR-007 | 管理页面 | MOD-001, MOD-005 | HTTP+Java | POST /notice/channel-route-tags | SaveNoticeRouteTagCommand | R<NoticeRouteTagVO> | code/name/channelType，编码不可变 | notice:channel 权限和租户 | tagCode 保证幂等身份 | ROUTE_TAG_INVALID/REFERENCE_CONFLICT | 新增接口 | `rules/backend/03-api.md` | API 与集成测试 |
| API-005 | FR-007 | 管理页面 | MOD-001, MOD-005 | HTTP+Java | DELETE /notice/channel-route-tags | NoticeRouteTagDeleteCommand | R<Boolean> | tagCode 和 channelType | notice:channel 权限和租户 | 重复删除返回不存在错误 | ROUTE_TAG_INVALID/REFERENCE_CONFLICT | 新增接口 | `rules/backend/03-api.md` | API 引用保护测试 |
| API-006 | FR-007 | 管理页面 | MOD-001, MOD-005 | HTTP+Java | GET /notice/channels/reference-impact | NoticeChannelReferenceImpactQuery | R<NoticeChannelReferenceImpactVO> | configId 或 tagCode 二选一 | notice:channel:view 和租户 | 模板名称稳定排序 | NOTICE_BUSINESS_ERROR | 新增接口 | `rules/backend/03-api.md` | API 引用保护测试 |
| API-007 | FR-005 | 管理页面 | MOD-001, MOD-005 | HTTP+Java+Feign | PUT /notice/business-types/channel-templates | SaveNoticeChannelTemplateCommand | R<NoticeChannelTemplateVO> | 三种模式严格互斥与同渠道候选 | 现有 notice:business 权限和租户 | 同模板版本语义 | CHANNEL_ROUTE_INVALID/CHANNEL_ROUTE_TAG_UNAVAILABLE | 未传 mode 按旧字段推导 | `rules/backend/03-api.md` | MVC/Feign parity 和兼容测试 |

## 6. 持久化与数据迁移设计

| 数据设计ID | 上游或模型ID | 表或实体 | 字段变化 | 约束 | 索引 | 租户审计 | Mapper边界 | 数据来源 | migration或回填 | 回滚或补偿 | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| DB-001 | DM-002, DR-002 | notice_channel_config | config_code、secret_refs_json、secret_config_json、resource_id/version/module/source、managed_fields_json、secret_status | tenant+configCode 唯一；Secret 列不映射到查询 VO | uk tenant/config_code；保持 route 索引 | 继承 tenant/audit | mapper 仅 core 使用 | 旧记录和 Resource/admin | V2 回填 LEGACY_加主键并迁移已知敏感键；同步 V1 | 失败停止；旧 JSON 异常标记 INCOMPLETE且保留 | `rules/backend/07-persistence.md` | Flyway 升级/H2或MySQL集成 |
| DB-002 | DM-003, DR-003 | notice_channel_route_tag | 新表 id、channel_type、tag_code、display_name、source/resource 审计 | tenant+channelType+tagCode 唯一 | 唯一和查询索引 | tenant/audit 完整 | 独立 mapper/service | Resource/admin | V2 建表；旧数据为空 | 应用回退前清除 TAG 使用，不自动 drop | `rules/backend/07-persistence.md` | migration 与引用测试 |
| DB-003 | DM-003 | notice_channel_config_route_tag | 新表 config_id、tag_id、source、tenant | tenant+configId+tagId 唯一 | config/tag 双向索引 | tenant/audit | 关联 mapper 仅 service 使用 | Resource/admin | V2 建表 | 删除关系前服务引用保护 | `rules/backend/07-persistence.md` | 多对多同步测试 |
| DB-004 | DM-004, DR-004 | notice_business_channel_template | route_mode、route_tag_code | EXACT/TAG/AUTO 由服务校验 | tenant/channel/route_mode/tag 索引 | 继承 tenant/audit | 现有 mapper | 旧模板和 admin | 有 channel_config_id 回填 EXACT，其余 AUTO；同步 V1 | 旧应用继续读取 channel_config_id；TAG 回退前转换 | `rules/backend/07-persistence.md` | Flyway 数据兼容和路由集成 |

## 7. 安全、权限、租户与数据边界

| 安全设计ID | 系统需求ID | 能力 | 权限资源 | 默认授权 | 后端校验入口 | 租户边界 | 数据归属断言 | 前端反馈 | 审计 | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| SEC-001 | FR-001 | 服务内附件读取 | Notice 发送既有内部权限 | 不新增用户权限 | File provider 的服务内读取与任务租户上下文 | worker 执行前恢复 task tenant | fileId 必须在当前服务上下文可读 | 发送记录显示安全失败，不暴露存储路径 | fileId/文件名/大小/结果 | `rules/backend/01-code.md` | 跨租户/不存在文件集成测试 |
| SEC-002 | FR-003, FR-004 | Secret 声明、补录、查询和解析 | notice:channel:view/edit | 沿用现有授权 | Resource handler、configuration service、secret materializer | 所有配置查询按 Mango tenant context | 账号、标签、模板同租户 | 只显示状态和缺失键 | 来源、版本、字段变化，不记录值 | `rules/03-ai-coding-redlines.md` | Resource/API/log 敏感扫描 |
| SEC-003 | FR-005, FR-006, FR-007 | 标签和模板路由 | notice:business/notice:channel 既有权限 | 沿用现有授权 | template save、route query、impact service | 所有候选带 tenant 条件 | template/config/tag channelType 和 tenant 一致 | 模式冲突与引用影响明确 | 保存模式和最终账号 | `rules/backend/03-api.md` | 跨租户与越界候选测试 |

## 8. 错误码、异常与可观测性

| 错误设计ID | 系统需求ID | 失败场景 | 触发条件 | 错误码 | 异常类型 | 用户反馈 | 日志上下文 | 指标或告警 | 重试或补偿 | 敏感信息处理 |
|---|---|---|---|---|---|---|---|---|---|---|
| ERR-001 | FR-001 | 附件不可用或违反策略 | provider 缺失、文件不存在/无权、超限、类型拒绝、超时、读取失败 | ATTACHMENT_PROVIDER_UNAVAILABLE、ATTACHMENT_NOT_FOUND_OR_FORBIDDEN、ATTACHMENT_LIMIT_EXCEEDED、ATTACHMENT_TYPE_NOT_ALLOWED、ATTACHMENT_READ_TIMEOUT、ATTACHMENT_READ_FAILED | ChannelSendResult 失败 | 发送记录显示阶段和 fileId | taskId、recordId、fileId、阶段、错误码 | 按错误码统计失败 | 暂时性读取/超时可重试，其余不重试 | 不记录内容、URL或底层授权细节 |
| ERR-002 | FR-002 | MIME/SMTP 失败 | header 注入、MIME 组装或 provider 拒绝 | EMAIL_MIME_BUILD_FAILED、PROVIDER_REJECTED、PROVIDER_ERROR | SMTP/业务失败 | 显示稳定原因 | recordId、configId、provider、阶段 | SMTP 失败率 | provider 临时错误按原规则重试 | 不记录密码和完整 SMTP 会话 |
| ERR-003 | FR-003, FR-004 | Secret 或 Resource 无效 | 明文 Secret、引用协议未知、必需键缺失 | CHANNEL_CONFIG_INVALID/CHANNEL_SECRET_INVALID | 业务异常或同步失败 | 同步/页面显示字段和状态 | resourceId、configCode、键名、引用类型 | INCOMPLETE 数量 | 修复声明/补录后重试 | 不记录值或环境内容 |
| ERR-004 | FR-005, FR-006, FR-007 | 路由或引用无效 | 模式冲突、标签无候选、跨渠道、被引用删除 | CHANNEL_ROUTE_INVALID、CHANNEL_ROUTE_TAG_UNAVAILABLE、REFERENCE_CONFLICT | 业务异常/ChannelSendResult | 页面或记录显示明确原因与影响 | templateId、tagCode、configId、候选数 | 标签无候选和切换次数 | 配置修复；禁止 AUTO 补偿 | 不记录 Secret/configJson |

## 9. 前端结构与交互实现映射

| 前端设计ID | 系统需求ID | 页面或动作 | 页面key或路由 | 区域与组件 | 状态来源 | API依赖 | 权限或不可操作 | 空加载或失败态 | 语义测试锚点 | 复用判断 | 适用规范ruleId |
|---|---|---|---|---|---|---|---|---|---|---|---|
| UI-001 | PG-001, BT-001, FR-004 | 渠道列表/编辑/Secret 补录 | notice.channel | Element Plus 表格、表单、Secret 状态与来源区域 | channel configs、route tags、impact API | API-001, API-002, API-003, API-004, API-005, API-006 | Resource 锁定只读；无 edit 权限禁用保存 | 加载、空、错误、权限和引用冲突 | data-surface=notice.channel.routing；data-field=notice.channel.secret-status；data-action=notice.channel.secret-supply | 留在 @mango/notice 页面包 | `rules/frontend/01-vue-code.md` |
| UI-002 | PG-002, BT-003, FR-005 | 模板三模式路由 | notice.business-config | 单选模式、精确账号/标签选择、候选预览 | channel configs、route tags、template | API-001, API-003, API-007 | 标签无候选或无 edit 权限不可保存 | 加载、空、错误、权限和无候选 | data-field=notice.channel.route-mode；data-field=notice.channel.route-tag | 复用现有业务配置表单 | `rules/frontend/01-vue-code.md` |
| UI-003 | PG-001, BT-002, FR-007 | 引用影响提示 | notice.channel | ElMessageBox/描述列表 | impact API | API-006 | 有引用时只有取消和跳转调整 | API 失败不执行破坏动作 | data-surface=notice.channel.reference-impact | 页面私有交互，不新增公共组件 | `rules/frontend/04-test.md` |

## 10. 测试设计与验收映射

| 测试用例ID | 系统验收ID | 设计项ID | 场景 | 优先级 | 测试层级 | 自动化判断 | 测试数据 | 权限或租户边界 | 稳定契约 | 执行入口 | 证据 | 失败处理 | 适用规范ruleId |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| TC-001 | SAC-001, SAC-002 | DEC-001, DEC-002, MOD-004, FLOW-001, ERR-001, ERR-002 | 无附件、中文名单双附件、限制和失败 MIME | P0 | 单元/集成 | AUTO | 内存文件 provider、真实临时测试文件、SMTP 测试服务器 | tenant 1/2 和不可读 fileId | MIME headers、part 数量、字节内容、ChannelSendResult | email 模块 JUnit | surefire 与安全摘要 | 任一假成功或泄漏阻断 | `rules/backend/08-test.md` |
| TC-002 | SAC-003 | DEC-003, MOD-003, FLOW-002, DB-001, SEC-002, ERR-003 | Resource 首次同步、补录、重复同步、明文拒绝、引用恢复 | P0 | 集成/API | AUTO | H2/MySQL 账号声明和环境属性 | tenant 1/2、notice:channel 权限 | targetId、状态、Secret 不回传、原值保留 | core/starter tests | surefire/API 报告 | 任何覆盖或泄露阻断 | `rules/09-test-case-automation-flow.md` |
| TC-003 | SAC-004, SAC-005 | DEC-004 至 DEC-007, FLOW-004, DB-002 至 DB-004, SEC-003, ERR-004 | EXACT/TAG/AUTO、主备切换、无候选和旧数据迁移 | P0 | 单元/集成 | AUTO | 多优先级/权重/健康账号、V1.0.25 等价 schema | tenant 1/2、同/跨渠道标签 | 候选集合、顺序、实际 configId、routeMode 回填 | core migration/route tests | surefire 与 migration 报告 | 越界账号或模式漂移阻断 | `rules/backend/08-test.md` |
| TC-004 | SAC-006 | API-001 至 API-007, UI-001 至 UI-003 | 渠道页来源/Secret/标签和模板三模式交互 | P1 | 组件/UI | AUTO | API fixture 与独立 UI 测试账号 | notice:channel/business view/edit 组合 | 语义锚点、禁用状态、请求 payload、错误反馈 | package Vitest + admin Playwright 定向用例 | 报告与截图 | 网络/console/UI 断言失败阻断 | `rules/frontend/04-test.md` |

## 11. 兼容与已启用能力说明影响

| 影响ID | 设计项ID | 影响对象 | 当前行为 | 目标行为 | 兼容策略 | 升级或补偿 | 已启用能力说明 | 验证 | 责任人 |
|---|---|---|---|---|---|---|---|---|---|
| IMP-001 | DEC-001, DEC-002 | EMAIL 消费业务 | 无附件可发，附件标识被忽略 | 无附件不变；附件全部安全投递或明确失败 | `attachmentFileIds` 字段不变，新增 sender 行为 | 升级 Notice Maven；按限制配置 | 更新 Notice README 附件配置、失败和示例 | TC-001 | Notice owner |
| IMP-002 | DEC-003 至 DEC-006, API-001 至 API-004 | Resource、管理端和业务模板 | providerCode 定位、configJson 混合 Secret、EXACT/AUTO | configCode、Secret 分层、标签和三模式 | 旧精确/空值推导；人工账号不强制 Resource 化 | V2 migration、前后端同批升级 | 更新 MESSAGE_CHANNEL schema、管理与升级说明 | TC-002 至 TC-004 | Notice owner |
| IMP-003 | DEC-007, DB-001 至 DB-004 | 1.0.25 数据库 | 只有旧字段和两种隐式路由 | 增量 V2 和 fresh V1 最终态 | 旧 ID、channelConfigId 和 AUTO 行为保留 | 升级前备份；TAG 回退前转换 | README 写升级/回退顺序 | TC-003 | Notice/DBA |

## 12. 技术追踪矩阵

| 上游ID | 设计项ID | 测试用例ID | 覆盖说明 |
|---|---|---|---|
| SC-001, SA-001, SA-003, FR-001, FR-002, UC-001, DR-001, IR-001, IR-002, NFR-002, NFR-004, SAC-001, SAC-002 | DEC-001, DEC-002, MOD-002, MOD-004, DM-001, FLOW-001, SEC-001, ERR-001, ERR-002, IMP-001 | TC-001 | 覆盖附件读取、限制、MIME、SMTP、审计和无附件兼容 |
| SC-002, SA-002, SA-004, FR-003, FR-004, UC-002, PG-001, BT-001, BT-002, DR-002, IR-003, NFR-001, SAC-003 | DEC-003, MOD-001, MOD-003, MOD-005, DM-002, FLOW-002, FLOW-003, API-001, API-002, API-003, API-004, API-005, API-006, DB-001, SEC-002, ERR-003, UI-001, UI-003, IMP-002 | TC-002, TC-004 | 覆盖 Resource、Secret、管理、来源和引用保护 |
| FR-005, FR-006, FR-007, UC-003, PG-002, BT-003, DR-003, DR-004, NFR-003, SAC-004, SAC-005, SAC-006 | DEC-004, DEC-005, DEC-006, DEC-007, MOD-001, MOD-003, MOD-005, MOD-006, DM-003, DM-004, FLOW-004, API-003, API-004, API-005, API-006, API-007, DB-002, DB-003, DB-004, SEC-003, ERR-004, UI-002, UI-003, IMP-002, IMP-003 | TC-003, TC-004 | 覆盖标签关系、三模式、故障切换、迁移、前端和兼容 |

## 13. 阶段判定与审批

| 检查项 | 结果 | 证据 |
|---|---|---|
| 技术设计 checker | PASS | `node mango-pmo/tools/check-technical-design.mjs --document mango-docs/designs/issues-641-642-notice-delivery/technical-design.md` |
| 生命周期 handoff | PASS | BRD、SRS 已批准且 upstream hash 精确匹配 |
| 专项规范检查计划 | PASS | TC-001 至 TC-004 覆盖后端、数据库、Resource、API、前端与真实 MIME 观察面 |
| 未关闭阻断数量 | 0 | DEC-001 至 DEC-007 均有回退/停止条件；无未决技术选择 |
| Tech Lead 审批 | APPROVED | `review/APPROVAL.md` |
