---
documentId: TDD-NOTICE-765
documentType: technical-design
pmoVersion: 1.3.13
schemaRevision: 1
riskLevel: L3
riskAssessmentEvidence: requirement=L3（公网渠道、凭据、持久化、文件与广播）；solution=L3（跨模块、异步一致性、协议验真、供应商能力不确定性）；final=max(requirement,solution)=L3
status: APPROVED
action: NEXT
owner: Mango Notice 技术负责人
approver: HardyDou
approvalEvidence: review/APPROVAL.md
upstreamDocumentId: SRS-NOTICE-765
upstreamDocumentHash: 635c2734e3642a72a318d75b114d2d2417ad2043f6e202f05d326b65a197dca7
---

# Notice 统一消息接收能力技术设计文档

## 1. 设计输入、约束与决策

| 决策ID | 问题 | 候选方案 | 选择 | 理由 | 来源ID或路径 | 是否推断 | 影响 | 风险 | 回退条件 |
|---|---|---|---|---|---|---|---|---|---|
| DEC-001 | 接收逻辑落在哪一层 | 发送渠道类扩展；Notice 独立 inbound core；业务应用各自接收 | Notice 独立 inbound core，渠道模块只做适配 | 接收、持久化、文件和广播属于 Notice 平台能力；保持发送与接收职责分离 | FR-001, FR-005, mango-notice README | 否 | 新增 Notice 入站能力 | 跨模块协调 | 回退到禁用入站 starter，不改变发送链路 |
| DEC-002 | 邮箱协议如何选择 | 同时启用 IMAP/POP3；账号固定一种；统一抽象按配置选择 | 账号配置 `protocol=IMAP|POP3`，适配器统一输出标准消息 | 满足用户“两种方式都支持”且避免双重拉取 | FR-001, IR-001 | 否 | 配置和调度新增 | 游标、服务器能力差异 | 禁用单账号后保留其它账号 |
| DEC-003 | Webhook provider 能力边界 | provider 名称即启用；统一按发送回执解析；能力声明后启用 | `NoticeInboundWebhookProvider` SPI；只有 `supportsInboundReception=true` 且有验真/标准化实现才启用 | 防止把阿里云/腾讯云发送回执或空实现冒充收件；腾讯云 SES 官方回调为递送、退信、打开、点击等发信事件 | FR-003, IR-003, BAC-005 | 否 | 保留阿里云/腾讯云扩展位，当前按官方契约逐家启用 | 供应商文档变化 | provider 不满足契约时配置启动失败并提示不支持 |
| DEC-004 | 企业微信协议实现位置 | Controller 中读取 XML/手写 AES；channel-wecom 协议适配器；公共协议库 | channel-wecom `WecomInboundMessageAdapter`，starter 只接收原始请求和返回 ACK | 复用现有 channel 模块边界，避免公网入口承担验签、解密和业务处理 | FR-002, IR-002, mango-payment 公网回调样本 | 否 | 新增企业微信接收适配 | 加密协议兼容风险 | 无密钥或解密失败只拒绝，不执行发送逻辑 |
| DEC-005 | 回调与业务处理时序 | 同步完成文件、广播；先 Inbox 再异步；先 ACK 后落库 | 验真/解析后调用接收服务；附件在接收尝试中同步写入 file，广播异步可重试 | 外部回调不被广播下游阻塞，同时保留可恢复事实 | BR-001, FR-004, FR-005, NFR-001 | 否 | 引入状态机和异步重试 | 文件调用仍受 provider 响应窗口约束 | Inbox 或附件保存失败不 ACK 成功；广播失败由状态任务恢复 |
| DEC-006 | 附件存储 | 消息表存 URL；对象存储直写；调用 mango-file 保存并存 fileId | 调用 `IFileContentProvider.save(SaveFileCommand)`，消息仅存 fileId | 遵守文件模块公开契约和租户边界，避免 URL/预签名地址持久化 | BR-002, IR-004, mango-file README | 否 | 增加文件服务协作 | 文件保存失败补偿难 | 进入重试/死信，不假装广播完整消息 |
| DEC-007 | 广播可靠性 | 进程内事件；直接 MQ；`IDomainEventPublisher` 复用 Outbox/Redis Stream | `notice.message.received` 通过 `IDomainEventPublisher`，稳定 eventId，Outbox 负责重试 | 复用现有 Mango 能力且满足至少一次；消费者自行幂等 | BR-003, IR-005, mango-infra-event README | 否 | 新增事件载荷和订阅契约 | 消费重复是正常语义 | 依赖事件运维重投，不新建第二套 Outbox |
| DEC-008 | 幂等键 | 仅 provider messageId；内容摘要；租户+账号+来源键组合 | 优先 provider 唯一消息标识；缺失时使用规范化来源元数据+内容摘要；数据库唯一约束 | 覆盖 IMAP UID、POP3 Message-ID、企业微信消息标识和 provider 事件 | BR-003, DR-001 | 否 | 新增唯一索引和重复命中状态 | 来源标识缺失或变化 | 保守拒绝无法形成稳定键的消息，不生成随机业务键 |
| DEC-009 | 凭据来源 | 明文配置 JSON；数据库密文；现有 Secret Resolver SPI | 复用 `NoticeChannelSecretResolver`，只从 `env:`/`property:` 或后续 SPI 解析，运行时对象不进入日志/事件 | 遵守现有 Notice Secret 能力，保留外部 Secret 扩展 | BR-004, NoticeChannelSecretMaterializer | 否 | 接收账号复用现有配置 | Secret 解析失败 | 配置保持不可用并阻止调度/回调接收 |

## 2. 模块与依赖边界

| 模块设计ID | 模块或包 | 职责 | 改动类型 | 依赖方向 | 公开能力 | 系统需求ID | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|
| MOD-001 | mango-notice-api | 标准接收模型、SPI、枚举、事件载荷契约 | 新增 | api 不依赖 core/starter | 入站消息、provider SPI、状态枚举 | FR-001 至 FR-005, DR-001 至 DR-003 | rules/backend/03-api.md, 05-module.md | M09、M10 |
| MOD-002 | mango-notice-support | 纯协议/渠道适配契约和结构化解析辅助 | 新增 | support 仅依赖 api 与第三方协议库 | 邮件协议、企业微信协议、Webhook SPI 辅助 | FR-001, FR-002, FR-003 | rules/backend/05-module.md, 06-security.md | M10 |
| MOD-003 | mango-notice-core | Inbox 持久化、幂等、附件编排、状态机、事件发布 | 新增 | core -> api/support/file/event/persistence | 入站用例服务和重试任务 | FR-004, FR-005, DR-001 至 DR-003 | rules/backend/04-db.md, 07-persistence.md | M10、M11 |
| MOD-004 | mango-notice-channel-email | IMAP/POP3 拉取和邮箱 Webhook provider 适配 | 扩展 | channel -> support/api | 邮箱接收适配器 | FR-001, FR-003, IR-001, IR-003 | rules/backend/05-module.md, 06-security.md | M10、M11 |
| MOD-005 | mango-notice-channel-wecom | 企业微信签名、AES 解密、XML/JSON 标准化 | 扩展 | channel -> support/api | 企业微信入站适配器 | FR-002, IR-002 | rules/backend/05-module.md, 06-security.md | M10、M12 |
| MOD-006 | mango-notice-starter | 自动配置、调度器、公网函数式路由、ResourceProvider 和管理查询 Controller | 扩展 | starter -> core/channels/resource/web | 公网 GET/POST 回调、账号轮询和受保护的管理查询 | FR-001 至 FR-006 | rules/backend/03-api.md, 05-module.md, 06-security.md | M09、M11、M12 |
| MOD-007 | mango-file 与 mango-infra-event | 提供文件保存和可靠事件传输，不新增平行能力 | 复用 | Notice -> 已发布公共契约 | `IFileContentProvider`、`IDomainEventPublisher` | FR-004, FR-005 | mango-file README, mango-infra-event README | M11 |
| MOD-008 | mango-notice-starter-remote 与 mango-ui/packages/notice | 远程查询契约、管理端页面注册、列表详情和附件下载交互 | 扩展 | UI -> Notice API；Notice remote -> Notice API | `notice/inbound/index` 管理页面与 Feign 查询 | FR-006, PG-001 | rules/backend/03-api.md, rules/frontend/01-vue-code.md, 12-business-api.md | M09、M10、M12 |

## 3. 技术对象与状态模型

| 模型ID | 上游ID | 模型职责 | 标识 | 关系 | 状态编码 | 审计或历史 | 归属或租户 | 一致性约束 |
|---|---|---|---|---|---|---|---|---|
| DM-001 | DR-001, FR-001, FR-002, FR-003 | Inbox 消息 | 租户+接收账号+稳定 sourceKey | 一对多附件；一对一事件身份 | RECEIVED、ATTACHMENT_PROCESSING、READY_TO_BROADCAST、BROADCASTED、RETRYABLE_FAILED、DEAD_LETTER | 原始来源摘要、首次接收时间、最近失败 | TenantEntity/tenant_id，账号归属租户 | 唯一键阻止重复 Inbox；状态条件更新 |
| DM-002 | DR-002, FR-004 | 附件关联 | messageId+attachmentIndex | 多对一 Inbox；保存 fileId | SAVED、FAILED | 原文件名、大小、MIME、摘要 | 与 Inbox 同租户 | 同一消息序号唯一；fileId 不为空才可广播 |
| DM-003 | DR-003, FR-005 | 接收游标 | 租户+账号+协议 | 一对一账号游标 | ACTIVE、PAUSED、FAILED | IMAP UIDVALIDITY/UID 或 POP3 UIDL/Message-ID | 与账号同租户 | 仅在 Inbox 持久化成功后推进 |
| DM-004 | FR-005 | 广播身份 | Inbox 一对一稳定 eventId | 事件载荷引用消息和 fileId | PENDING、PUBLISHED、FAILED | eventId、publishAttempt、lastFailure | 与 Inbox 同租户 | eventId 不因重试改变；消费者按 eventId 幂等 |

| 模型ID | 当前状态 | 触发 | 目标状态 | 前置条件 | 副作用 | 失败处理 | 上游ID |
|---|---|---|---|---|---|---|---|
| DM-001 | RECEIVED | 开始附件任务 | ATTACHMENT_PROCESSING | Inbox 已提交 | 创建附件处理任务 | 任务不可用则 RETRYABLE_FAILED | FR-004 |
| DM-001 | ATTACHMENT_PROCESSING | 附件全部保存 | READY_TO_BROADCAST | 无附件或所有 fileId 完整 | 形成广播载荷 | 任一失败 RETRYABLE_FAILED | FR-004 |
| DM-001 | READY_TO_BROADCAST | 发布器受理 | BROADCASTED | 事件载荷完整 | 写入 Outbox/传输层 | 暂时失败 RETRYABLE_FAILED，耗尽 DEAD_LETTER | FR-005 |
| DM-001 | RETRYABLE_FAILED | 退避重试 | ATTACHMENT_PROCESSING 或 READY_TO_BROADCAST | 重试次数未耗尽 | 重做失败步骤 | 达到上限 DEAD_LETTER | FR-004, FR-005 |
| DM-001 | DEAD_LETTER | 运维人工重投 | RECEIVED 或 READY_TO_BROADCAST | 权限和原因已确认 | 记录人工操作 | 失败保持 DEAD_LETTER | FR-005 |

## 4. 系统流程、事务与一致性

| 流程设计ID | 系统需求ID | 调用入口 | 参与模块 | 处理顺序 | 事务边界 | 状态变化 | 幂等键 | 并发策略 | 外部失败与补偿 | 用户可见结果 |
|---|---|---|---|---|---|---|---|---|---|---|
| FLOW-001 | FR-001, FR-004 | 账号轮询调度 | MOD-006, MOD-004, MOD-003 | 读取账号→建立租户上下文→拉取→标准化→Inbox→推进游标 | 每封邮件 Inbox 与游标为短事务；不在事务内保持邮箱连接 | 创建/复用 DM-001，游标按成功记录推进 | tenant+account+protocol+sourceKey | 账号级分布式锁或单实例 claim，批次内顺序处理 | 连接/解析失败退避；未落库不推进游标 | 运维可查处理状态 |
| FLOW-002 | FR-002 | 企业微信公网 GET/POST | MOD-006, MOD-005, MOD-003 | 读取原文→验签/解密→GET 返回或 POST 标准化→Inbox/附件接收→ACK | POST 在接收尝试中保存附件，广播异步 | GET 不变更；POST 创建/复用 DM-001 | provider messageId 或规范化摘要 | 同一 sourceKey 唯一约束；广播不阻塞请求 | 验签或附件失败拒绝；Inbox 失败返回渠道失败码 | GET echostr 原样返回；POST 受理后 ACK |
| FLOW-003 | FR-003 | 邮箱 provider 公网 POST | MOD-006, MOD-004, MOD-003 | provider 验真→标准化→Inbox/附件接收→ACK | 同 FLOW-002 | 创建/复用 DM-001 | provider account+messageId | 唯一约束和受理后返回 | unsupported provider 拒绝；重试由 provider 负责 | 明确 ACK 或拒绝 |
| FLOW-004 | FR-004 | 接收尝试中的附件处理 | MOD-003, MOD-007 | claim 附件→使用当前来源流调用 mango-file save→保存 fileId→状态更新 | 每附件短事务；不把文件调用放在数据库长事务 | ATTACHMENT_PROCESSING→READY/FAILED | messageId+attachmentIndex | 条件更新和附件 claim | 文件失败不 ACK/不推进邮箱游标；来源重投时复用已保存 fileId | Inbox 状态和 fileId 可查 |
| FLOW-005 | FR-005 | 广播 worker/服务 | MOD-003, MOD-007 | claim READY→构造 DomainEvent→publish→状态更新 | 状态更新与发布采用 Outbox 语义，不宣称跨系统原子 | READY→BROADCASTED/FAILED | eventId=稳定 Inbox 事件 ID | 发布幂等由 eventId，消费者去重 | Outbox/Redis Stream 重试和死信 | 事件状态可查 |

## 5. API 与远程契约设计

| 接口ID | 系统需求ID | 调用方 | 所属模块 | 入口类型 | 方法与路径 | Command Query或VO | 返回契约 | 校验 | 权限租户或数据权限 | 幂等分页或排序 | 错误码 | 兼容策略 | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| API-001 | FR-002, SAC-002 | 企业微信 | MOD-006 | PUBLIC function route | GET /notice/inbound-callbacks/public | NoticeInboundCallbackQuery | R<NoticeInboundAckVO> | msg_signature、timestamp、nonce、echostr 必填；适配器验签/解密 | PUBLIC 资源仅路由到已绑定渠道配置；不建立登录用户 | 协议原样返回 echostr；不入库 | NOTICE_INBOUND_SIGNATURE_INVALID、NOTICE_INBOUND_CONFIG_INVALID | 新增固定路径，不修改发送入口 | rules/backend/03-api.md, 06-security.md | M12 |
| API-002 | FR-002, SAC-002 | 企业微信 | MOD-006 | PUBLIC function route | POST /notice/inbound-callbacks/public | NoticeInboundRawCallback | R<NoticeInboundAckVO> | 原文大小、content type、签名、解密和消息类型校验 | PUBLIC 路由后绑定租户/账号；不得以客户端 tenantId 作为隔离条件 | sourceKey 幂等；附件受理后 ACK，广播异步 | NOTICE_INBOUND_SIGNATURE_INVALID、NOTICE_INBOUND_MESSAGE_INVALID、NOTICE_INBOUND_ACCEPT_FAILED | 与 API-001 共路径但方法不同 | rules/backend/03-api.md, 06-security.md | M12 |
| API-003 | FR-003, SAC-005 | 邮箱 provider | MOD-006 | PUBLIC function route | POST /notice/inbound-mail-callbacks/public | NoticeInboundRawCallback | R<NoticeInboundAckVO> | provider SPI 验真、账号映射和能力声明 | PUBLIC 请求只能绑定配置账号；不接受客户端租户字段 | provider sourceKey 幂等；接收链路受理后 ACK | NOTICE_INBOUND_PROVIDER_UNSUPPORTED、NOTICE_INBOUND_SIGNATURE_INVALID | 未注册真实入站适配器时拒绝 | rules/backend/03-api.md, 06-security.md | M12 |
| API-004 | FR-006, SAC-007 | 管理端与内部消费者 | MOD-006, MOD-008 | 管理查询/Feign | GET /notice/inbound-messages | NoticeInboundMessagePageQuery | R<PageResult> | 分页、时间范围、状态和渠道校验 | `notice:inbound:view`；持久化租户拦截保持启用 | 按 receivedAt/id 倒序；列表省略正文和附件 | 复用统一参数错误 | 纯新增查询，不改变个人消息中心 | rules/backend/03-api.md, 06-security.md | M09、M10、M12 |
| API-005 | FR-006, SAC-007 | 管理端与内部消费者 | MOD-006, MOD-008 | 管理查询/Feign | GET /notice/inbound-messages/detail | NoticeInboundMessageVO | R<NoticeInboundMessageVO> | 消息 ID 必填且合法 | `notice:inbound:view`；持久化租户拦截保持启用 | 按 ID 查询本租户消息；详情按需返回正文和附件 fileId | 复用资源不存在错误 | 纯新增查询，不改变个人消息中心 | rules/backend/03-api.md, 06-security.md | M09、M10、M12 |

## 6. 持久化与数据迁移设计

| 数据设计ID | 上游或模型ID | 表或实体 | 字段变化 | 约束 | 索引 | 租户审计 | Mapper边界 | 数据来源 | migration或回填 | 回滚或补偿 | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| DB-001 | DM-001 | `notice_inbound_message` / NoticeInboundMessageEntity | 新表：id、tenant_id、channel_config_id、channel_type、provider_code、source_key、message_id、subject、from_address、to_addresses、body_text、body_html、raw_headers_json、status、event_id、failure_code、failure_reason、received_at、processed_at、审计字段 | tenant_id 非空；source_key、event_id 业务唯一；正文长度/原始头大小有配置上限 | tenant+channel+source_key 唯一；tenant+status+received_at；tenant+event_id 唯一 | TenantEntity 自动填充/过滤；不跨模块 join | 仅访问本模块表 | 渠道标准化模型 | `V3__notice_inbound.sql` 新增，已有发送表不回填 | 新表可回滚代码；失败消息保留重试/死信，不删除业务事实 | rules/backend/04-db.md, 07-persistence.md | M09、M11 |
| DB-002 | DM-002 | `notice_inbound_attachment` / NoticeInboundAttachmentEntity | 新表：id、tenant_id、message_id、attachment_index、file_id、file_name、content_type、file_size、content_sha256、status、failure_code、failure_reason、审计字段 | message_id+attachment_index 唯一；file_id 成功时非空；同租户 | tenant+message+index；tenant+file_id | TenantEntity；只保存文件 ID，不保存 URL | 仅访问本模块表 | 文件服务返回 FileRecordVO.id | 同一 V3 migration | 文件已保存但消息失败时保留 fileId，避免越权删除；由重试复用 | rules/backend/04-db.md, 07-persistence.md | M11 |
| DB-003 | DM-003 | `notice_inbound_receive_cursor` / NoticeInboundReceiveCursorEntity | 新表：id、tenant_id、channel_config_id、protocol、cursor_value、cursor_version、last_polled_at、last_failure_code、last_failure_reason、审计字段 | tenant+channel_config_id 唯一；protocol 仅 IMAP/POP3 | tenant+next_poll_at；tenant+status | TenantEntity；调度进入租户上下文 | 仅访问本模块表 | 邮箱服务器游标和配置 | 同一 V3 migration，无历史数据回填 | 账号停用后暂停游标，不删除历史 Inbox | rules/backend/04-db.md, 07-persistence.md | M11 |

## 7. 安全、权限、租户与数据边界

| 安全设计ID | 系统需求ID | 能力 | 权限资源 | 默认授权 | 后端校验入口 | 租户边界 | 数据归属断言 | 前端反馈 | 审计 | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| SEC-001 | FR-001, FR-002, FR-003 | 渠道验真与凭据解析 | 公网回调资源由 ResourceProvider 声明为 PUBLIC；管理配置沿用 Notice 权限 | 公网仅开放协议路由，不开放管理查询 | 渠道适配器验签/解密；SecretMaterializer 解析引用；缺失即拒绝 | 从渠道配置映射租户；禁止客户端 tenantId | sourceKey、channelConfigId、tenantId 必须匹配 | 非法回调不进入管理列表 | 记录 provider、配置 ID、请求标识、参数键，不记密钥和正文 | rules/backend/06-security.md | M10、M12 |
| SEC-002 | FR-004, DR-002 | 附件文件归属 | 内部服务调用 mango-file | 非人工公开授权 | `IFileContentProvider.save` 使用当前租户上下文和 PRIVATE | 文件 bizType/bizId 关联 Inbox；读取仅当前租户 | 每个 fileId 属于当前租户且状态完成 | 不适用 | 记录 fileId、大小、类型和摘要，不记 URL | rules/backend/06-security.md, 07-persistence.md | M11 |
| SEC-003 | FR-005, DR-003 | 事件广播 | 订阅方按业务授权 | 由消费者配置决定 | 发布前写入 tenant、businessKey、aggregateId、idempotency headers | 事件 payload 不包含凭据/预签名 URL；租户信息来自上下文 | 消费者必须校验 eventId 与 tenant | 不适用 | 记录 eventId、业务键、重试次数和错误摘要 | rules/backend/06-security.md | M11 |
| SEC-004 | FR-006, SAC-007 | 管理员查询接收消息 | `notice:inbound:view`，附件读取沿用文件服务权限 | 菜单资源不默认绕过角色授权 | NoticeController `@ApiAccess(PERMISSION)`；Mapper 查询不关闭租户拦截；文件下载调用受保护接口 | 列表、详情和附件均由当前租户上下文约束 | 列表不返回正文/附件，详情不返回永久 URL，跨租户 ID 等同不可见 | 无权限不展示入口或返回 403；附件未保存时禁用下载 | 记录管理查询上下文，不记录正文和密钥 | rules/backend/06-security.md, rules/frontend/12-business-api.md | M09、M10、M12 |

## 8. 错误码、异常与可观测性

| 错误设计ID | 系统需求ID | 失败场景 | 触发条件 | 错误码 | 异常类型 | 用户反馈 | 日志上下文 | 指标或告警 | 重试或补偿 | 敏感信息处理 |
|---|---|---|---|---|---|---|---|---|---|---|
| ERR-001 | FR-002, FR-003 | 验签/解密失败 | 签名、时间窗、密钥或密文非法 | NOTICE_INBOUND_SIGNATURE_INVALID | 渠道协议异常 | 公网返回渠道失败 ACK | provider、channelConfigId、requestId、paramKeys | invalid_signature_total | 不重试业务处理；保留安全审计 | 不记录签名值、密钥、密文和完整正文 |
| ERR-002 | FR-001, FR-004 | 邮箱连接/解析/文件失败 | 网络、认证、MIME 或文件服务失败 | NOTICE_INBOUND_RECEIVE_RETRYABLE_FAILED | 可重试接收异常 | 调度状态显示可重试/死信 | tenant、channelConfigId、protocol、sourceKey、stage | receive_retry_total、dead_letter_total | 指数退避；游标不越过未接收消息 | 不记录密码和完整邮件正文 |
| ERR-003 | FR-005 | 广播失败 | Outbox/Redis Stream 暂时不可用或消费者错误 | NOTICE_INBOUND_BROADCAST_RETRYABLE_FAILED | 事件发布异常 | Inbox 状态为可重试/死信 | eventId、businessKey、tenant、attempt | broadcast_retry_total、event_dead_letter_total | 由现有事件运维重投 | 事件载荷不含 Secret、URL |
| ERR-004 | FR-003, SAC-005 | provider 能力不支持 | 仅提供发送回执或未注册入站实现 | NOTICE_INBOUND_PROVIDER_UNSUPPORTED | 配置/能力异常 | 配置校验失败，公网拒绝 | provider、capability、channelConfigId | unsupported_provider_total | 不重试，不创建 Inbox | 不记录 provider 凭据 |

## 9. 前端结构与交互实现映射

| 前端设计ID | 系统需求ID | 页面或动作 | 页面key或路由 | 区域与组件 | 状态来源 | API依赖 | 权限或不可操作 | 空加载或失败态 | 语义测试锚点 | 复用判断 | 适用规范ruleId |
|---|---|---|---|---|---|---|---|---|---|---|---|
| UI-001 | PG-001, BT-001, BT-002, BT-003, SAC-007 | 管理员接收消息列表、详情和附件下载 | `/notice/inbound`；`notice/inbound/index` | MangoListPage/SearchPanel/ListPanel；详情 Dialog/PageSection；附件表格 | API-004、API-005 的租户内 Inbox、广播状态和附件 fileId | API-004, API-005；附件用 `@mango/common` 的受保护文件下载能力 | 页面与 API 均要求 `notice:inbound:view`；无 fileId 禁用下载 | 列表/详情分别显示加载态，空表格明确，无权限由宿主和后端共同拦截 | `data-page="notice.inbound"`、页面注册 key、API/权限契约 | 复用 Notice 业务包与公共组件，不注册到个人消息中心；HTML 正文按文本展示 | rules/frontend/01-vue-code.md, 04-test.md, 12-business-api.md |

## 10. 测试设计与验收映射

| 测试用例ID | 系统验收ID | 设计项ID | 场景 | 优先级 | 测试层级 | 自动化判断 | 测试数据 | 权限或租户边界 | 稳定契约 | 执行入口 | 证据 | 失败处理 | 适用规范ruleId |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| TC-001 | SAC-001 | DEC-002, MOD-002, MOD-004, DM-003, FLOW-001, ERR-002 | IMAP/POP3 结构化邮件解析、游标推进和连接失败 | P0 | 单元/组件 | AUTO | RFC 邮件 fixture，唯一 `IT_765_EMAIL_`，不接真实邮箱 | 专用测试租户；不写共享库 | 协议、附件顺序、sourceKey、游标仅在 Inbox 成功后推进 | `mvn -pl mango-platform/mango-notice/mango-notice-channel-email test` | JUnit 报告 | 任一协议或游标断言失败阻断 | rules/backend/08-test.md |
| TC-002 | SAC-002 | DEC-004, MOD-002, MOD-005, FLOW-002, ERR-001 | 企业微信 GET 验证、POST 解密、篡改和时间窗 | P0 | 单元/组件 | AUTO | 固定签名向量和加密 XML；密钥只注入测试进程 | 专用测试租户；无真实公网写入 | echostr、明文消息、非法签名和不入库边界 | `mvn -pl mango-platform/mango-notice/mango-notice-channel-wecom test` | JUnit 报告 | 协议或安全断言失败阻断 | rules/backend/06-security.md, 08-test.md |
| TC-003 | SAC-003, SAC-004 | DEC-005, DEC-008, MOD-003, DM-001, DM-002, DB-001, DB-002, FLOW-002, FLOW-004 | 真实数据库 Inbox 唯一键、并发重复、附件关联 | P0 | 集成 | AUTO | Testcontainers MySQL/隔离库，`IT_765_INBOUND_`，测试后清理 | 双测试租户，断言跨租户不可见 | 真实 Mapper、事务、唯一约束和状态转换 | Notice core 集成测试定向标签 `flow inbound` | 测试报告和数据库摘要 | 任一重复或租户断言失败阻断 | rules/backend/04-db.md, 07-persistence.md, 08-test.md |
| TC-004 | SAC-003, SAC-004 | DEC-006, DEC-007, MOD-003, MOD-007, FLOW-004, FLOW-005, SEC-002, SEC-003 | 附件保存到 mango-file、只广播 fileId、Outbox eventId 稳定 | P0 | 集成 | AUTO | 隔离文件存储/测试租户，`IT_765_FILE_`，显式清理消息和文件 | 文件与事件同租户 | 真实 IFileContentProvider 和 IDomainEventPublisher；只替换外部存储传输 | Notice core 集成测试定向标签 `flow inbound broadcast` | 事件与文件审计摘要 | 文件或事件失败进入重试，不弱化断言 | rules/backend/08-test.md |
| TC-005 | SAC-002, SAC-005, SAC-006 | DEC-003, DEC-004, MOD-005, MOD-006, API-001, API-002, API-003, SEC-001, ERR-001, ERR-004 | 公网 GET/POST 路由、ResourceProvider、ACK、非法 provider | P0 | API/入口流程 | AUTO | MockMvc/真实 Spring 装配；固定签名向量；不记录完整正文 | PUBLIC 仅绑定已配置测试账号；不使用客户端租户 | 函数式 endpoint、GET/POST resource 声明、纯文本 ACK 和安全失败 | Starter 测试 `@Tag("flow") @Tag("notice-inbound")` | MockMvc、资源声明和日志安全断言 | 任一路由、ACK、PUBLIC 资源或敏感信息断言失败阻断 | rules/backend/03-api.md, 06-security.md, 08-test.md |
| TC-006 | SAC-001, SAC-003 | DEC-002, DEC-006, MOD-004, FLOW-001, FLOW-004 | 126 邮箱真实自发自收，含正文和附件 | P1 | 手工/外部状态回读 | MANUAL | 用户提供的 126 邮箱、授权码、SMTP/IMAP/POP3 配置；唯一主题 `IT_765_SELF_MAIL_<id>` | 仅测试租户和该邮箱；凭据从安全环境注入 | SMTP 接受、IMAP/POP3 回读、Inbox、fileId 和广播日志 | 用户提供凭据后按步骤执行，不把密码写入仓库 | `mango-docs/evidence/issue-765-notice-inbound/mail-test.md` | 缺凭据保持 BLOCKED，不宣称真实链路通过 | rules/backend/08-test.md |
| TC-007 | SAC-005 | DEC-003, MOD-004, API-003, ERR-004 | 腾讯云 SES 发送回执和未支持入站能力边界 | P1 | 单元/API | AUTO | 官方事件样本或固定 JSON；不向厂商发送真实邮件 | provider 配置无 Secret 回显 | delivered/bounce/open/click 被识别为发送回执并拒绝收件适配 | channel-email provider capability test | 报告和样本摘要 | 若误判为收件能力则阻断 | rules/backend/06-security.md, 08-test.md |
| TC-008 | SAC-007 | MOD-003, MOD-006, MOD-008, API-004, API-005, SEC-004, UI-001 | 接收后管理员分页、详情、权限、租户和附件下载契约 | P0 | 集成/API/前端包 | AUTO | H2 接收消息和附件 fileId；前端源码与页面注册 | `notice:inbound:view`；租户拦截不关闭；个人消息中心注册不变 | 列表无正文附件；详情有正文和 fileId；Feign/API/前端类型一致；HTML 不执行 | Notice core/starter/starter-remote 定向测试；`pnpm --filter @mango/notice build && test` | JUnit、Vitest 和构建摘要 | 任一查询、权限、注册、类型或构建失败阻断 | rules/backend/06-security.md, 08-test.md, rules/frontend/04-test.md |

## 11. 兼容与已启用能力说明影响

| 影响ID | 设计项ID | 影响对象 | 当前行为 | 目标行为 | 兼容策略 | 升级或补偿 | 已启用能力说明 | 验证 | 责任人 |
|---|---|---|---|---|---|---|---|---|---|
| IMP-001 | MOD-001, MOD-003, MOD-006, API-001, API-002, API-003 | mango-notice 消费者与运维 | 无统一入站能力；企业微信验证 404 | 新增固定公网回调、Inbox、事件和配置说明 | 不改变现有发送入口；新增路径单独注册 | 回退 starter 入站装配不影响发送 | 更新 mango-notice README、能力地图和 Issue 765 记录 | TC-005 | Notice owner |
| IMP-002 | DEC-003, MOD-004 | 阿里云、腾讯云 provider 接入者 | 发送产品和发送回执不能直接作为收件 | 以 SPI 能力声明和官方契约为准，未支持即拒绝 | 不保留发送回执误判 fallback | 供应商入站契约核实后新增独立适配器 | README 记录 provider 能力矩阵和不适用原因 | TC-007 | Notice owner |
| IMP-003 | DEC-006, MOD-007, DB-002 | 文件服务与业务消费者 | 消息模型无入站附件关系 | 附件写入 mango-file，业务只消费 fileId | 不保存 URL/预签名地址 | 文件失败保留重试/死信，不自动删除已保存文件 | 更新 file 接入说明引用和事件载荷说明 | TC-004 | Notice owner |
| IMP-004 | API-004, API-005, SEC-004, UI-001 | Notice 管理人员和前端消费者 | 无组织级接收消息查询页面 | 新增租户隔离的列表、详情和附件下载，个人消息中心不变 | 新增菜单、API 和页面 key；不改变既有个人消息路由 | 移除新菜单和查询装配即可回退，不影响接收持久化与广播 | 更新 Notice README、菜单资源和能力地图 | TC-008 | Notice owner |

## 12. 技术追踪矩阵

| 上游ID | 设计项ID | 测试用例ID | 覆盖说明 |
|---|---|---|---|
| SC-001, SA-001, SA-002, FR-001, UC-001, PG-001, BT-001, DR-003, IR-001, NFR-001, SAC-001 | DEC-002, MOD-002, MOD-004, MOD-006, DM-003, FLOW-001, API-003, DB-003, SEC-001, ERR-002, UI-001, IMP-001 | TC-001, TC-006 | 覆盖 IMAP/POP3 配置、调度、游标、入口边界和真实邮箱观察 |
| SC-002, SA-001, FR-002, UC-002, DR-001, IR-002, NFR-001, SAC-002, SAC-006 | DEC-004, DEC-005, MOD-005, MOD-006, DM-001, FLOW-002, API-001, API-002, SEC-001, ERR-001, UI-001, IMP-001 | TC-002, TC-005 | 覆盖企业微信验签解密、GET/POST ACK、幂等和安全 |
| SC-002, SA-001, FR-003, UC-003, IR-003, NFR-001, SAC-005 | DEC-003, MOD-004, MOD-006, FLOW-003, API-003, ERR-004, IMP-002 | TC-005, TC-007 | 覆盖邮箱 Webhook SPI、腾讯云发送回执边界和未知 provider 拒绝 |
| SC-003, SA-003, SA-004, FR-004, FR-005, DR-001, DR-002, DR-003, IR-004, IR-005, NFR-002, NFR-003, SAC-003, SAC-004 | DEC-001, DEC-005, DEC-006, DEC-007, DEC-008, DEC-009, MOD-001, MOD-003, MOD-007, DM-001, DM-002, DM-004, FLOW-004, FLOW-005, DB-001, DB-002, SEC-002, SEC-003, ERR-002, ERR-003, IMP-003 | TC-003, TC-004 | 覆盖 Inbox、附件 fileId、租户、Outbox、稳定 eventId、重试和死信 |
| SC-004, SA-005, FR-006, UC-004, PG-001, BT-001, BT-002, BT-003, SAC-007 | MOD-003, MOD-006, MOD-008, API-004, API-005, SEC-004, UI-001, IMP-004 | TC-008 | 覆盖管理员接收消息列表、详情、租户权限、附件 File 下载和个人消息中心隔离 |

## 13. 阶段判定与审批

| 检查项 | 结果 | 证据 |
|---|---|---|
| 技术设计 checker | PASS | `node mango-pmo/tools/check-technical-design.mjs --document mango-docs/designs/issue-765-notice-inbound/technical-design.md` |
| 生命周期 handoff | PASS | SRS-NOTICE-765 已批准并通过 checker；上游摘要在交接时写入 |
| 专项规范检查计划 | PASS | API、DB、模块、安全、持久化、测试和能力文档规则已映射到设计/测试表 |
| 未关闭阻断数量 | 0 | 无 |
| Tech Lead 审批 | APPROVED | `review/APPROVAL.md` |
