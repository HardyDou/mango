---
documentId: TDD-ACCOUNT-643
documentType: technical-design
pmoVersion: 1.3.8
schemaRevision: 1
riskLevel: L3
riskAssessmentEvidence: requirement=L3，核心登录、账号绑定、联系方式和敏感身份信息同时变化；solution=L3，方案跨 Auth、Identity、Captcha、KV、数据库、公开 API 和登录/个人中心/管理前端，并包含第三方网络交互与兼容入口；final=max(requirement,solution)
status: APPROVED
action: NEXT
owner: Mango Auth Tech Lead
approver: HardyDou
approvalEvidence: review/APPROVAL.md
upstreamDocumentId: SRS-ACCOUNT-643
upstreamDocumentHash: ffbc09c9664cbd34d07d7f729c5fe05019e4c0d3bfabba375ec3377087e56f0f
---

# 账户实名资料与第三方登录技术设计文档

## 1. 设计输入、约束与决策

| 决策ID | 问题 | 候选方案 | 选择 | 理由 | 来源ID或路径 | 是否推断 | 影响 | 风险 | 回退条件 |
|---|---|---|---|---|---|---|---|---|---|
| DEC-001 | 实名资料放置和证件号返回 | 扩展用户表；独立资料表；前端本地值 | 扩展 `identity_user`，证件号持久化原值但所有查询只返回掩码，更新接口不回显原值 | 每账号一份且已有身份资料聚合，避免假保存和双写 | FR-001, DR-001 | 否 | Identity API/core/migration | 原值误回传 | 服务层统一映射掩码，测试 JSON 和日志 |
| DEC-002 | 联系方式如何安全变更 | 复用通用资料更新；独立命令；只校验密码 | 独立当前用户命令，先校验当前密码，再一次性校验 Captcha 的新目标，单事务更新 | 将高风险字段从普通资料更新中隔离 | FR-002, IR-003 | 否 | Identity 依赖 Captcha API 和 PasswordEncoder | 校验后更新失败或验证码重放 | Captcha 成功后仅执行单行事务更新，失败不返回成功 |
| DEC-003 | Provider 配置归属 | Notice 配置；Auth 自有表；静态配置 | Auth 自有 `auth_provider_config`，按 tenantId+appCode+provider 唯一，Secret 使用 Mango Crypto 加密且查询只返回 configured | 登录配置属于认证域，满足应用隔离并消除新耦合 | FR-003, DR-002 | 否 | Auth core 增加持久化和 crypto 依赖 | 密钥不可用或旧配置缺失 | 配置不完整即不参与发现；旧企微入口适配新表 |
| DEC-004 | 多 Provider 扩展 | 为企微/钉钉复制流程；统一适配器 | `ExternalAuthProviderAdapter` 统一授权地址和 code 换身份，WECOM/DINGTALK 各自实现 | 核心编排、票据、绑定规则只实现一次 | FR-004 至 FR-007, NFR-004 | 否 | Auth adapter registry | 厂商字段差异泄漏到编排 | 适配器输出统一 ExternalAuthIdentity |
| DEC-005 | 回调和未绑定流程状态 | 浏览器自编码 state；数据库临时表；KV 一次性记录 | SecureRandom 生成 opaque state 和 bindTicket，KV 保存结构化 JSON，读取后原子 compare-and-delete | 防篡改、防重放且支持多实例 | FR-005 至 FR-007, DR-004 | 否 | Auth KV store | 非原子消费导致重放 | 使用 IKvStore 原子 compareAndDelete；不可用时失败关闭 |
| DEC-006 | 登录与绑定编排 | 厂商回调直接操作表；Auth 调 Identity 公共能力 | Auth 解析外部身份并调用 Identity 内部契约查询/绑定；登录态回调票据绑定当前 userId，匿名票据需账号密码和租户成员验证 | 保持身份关系归 Identity，令牌签发归 Auth | FR-006 至 FR-009 | 否 | 两域 API 契约扩展 | 跨租户或抢占绑定 | 每次调用携带并断言 tenantId/provider/providerTenantId/userId |
| DEC-007 | 旧企微契约 | 删除；继续走 Notice；兼容适配 | 保留 `/auth/wecom/*` 请求响应形状，内部映射 WECOM 统一配置和完成流程，不再新增 Notice 选择能力 | 保护现有消费者同时停止扩大耦合 | FR-010 | 否 | Auth API/starter/frontend 兼容 | 旧调用缺 appCode/state | 默认 internal-admin；只接受对应租户应用的新配置 |

## 2. 模块与依赖边界

| 模块设计ID | 模块或包 | 职责 | 改动类型 | 依赖方向 | 公开能力 | 系统需求ID | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|
| MOD-001 | mango-identity-api | 当前用户资料、联系方式、外部绑定内部契约 | 扩展 | 不依赖 transport | Java 契约和 VO | FR-001, FR-002, FR-006 至 FR-009 | `rules/backend/03-api.md` | 契约编译和 controller 测试 |
| MOD-002 | mango-identity-core/starter | 资料持久化、密码/Captcha 校验、自助绑定归属与掩码 | 扩展 | api、captcha、context、persistence | `/identity/me/*` | FR-001, FR-002, FR-008, FR-009 | `rules/backend/01-code.md` | 单元、H2 集成、MVC |
| MOD-003 | mango-auth-api | Provider 配置、发现、授权、回调和绑定契约 | 扩展 | 不依赖 core | Java/HTTP 契约 | FR-003 至 FR-007, FR-010 | `rules/backend/03-api.md` | API parity |
| MOD-004 | mango-auth-core | 配置、加密、adapter registry、KV 流程和登录签发 | 扩展/重构 | auth-api、identity-api、authorization、KV、crypto、persistence | 认证编排服务 | FR-003 至 FR-007, FR-010 | `rules/backend/10-dev-flow.md` | 单元、集成、静态检查 |
| MOD-005 | mango-auth-starter/remote | HTTP、权限、Feign 和资源声明 | 扩展 | api/core/resource | 公开与内部入口 | FR-003 至 FR-010 | `rules/backend/03-api.md` | MVC/Feign parity、权限测试 |
| MOD-006 | @mango/auth | 登录、回调绑定、个人资料和授权管理、Provider 配置页 | 扩展 | 实例级 HTTP API | Vue 页面与类型 | PG-001 至 PG-003 | `rules/frontend/01-vue-code.md` | typecheck、Vitest、build、UI 走查 |

## 3. 技术对象与状态模型

| 模型ID | 上游ID | 模型职责 | 标识 | 关系 | 状态编码 | 审计或历史 | 归属或租户 | 一致性约束 |
|---|---|---|---|---|---|---|---|---|
| DM-001 | DR-001, FR-001 | 当前用户资料与实名事实 | userId | 一对一身份账号 | verificationStatus=UNVERIFIED；verificationSource 可空 | 继承用户更新时间 | 全局 userId，租户入口只允许本人 | 证件类型和号码同为空或同非空；API 只返回掩码 |
| DM-002 | DR-002, FR-003 | 第三方登录配置 | tenantId+appCode+provider | 每组合一条 | enabled、secretConfigured、complete | tenant/audit 字段 | 当前 tenantId/appCode | provider 仅 WECOM/DINGTALK；启用必须完整 |
| DM-003 | DR-003, FR-006 | 厂商身份绑定 | tenantId+provider+providerTenantId+externalUserId | 多条绑定归一用户 | BOUND | bind/lastLogin 时间 | 当前租户和 userId | 外部组合唯一；冲突不得覆盖 |
| DM-004 | DR-004, FR-005 至 FR-007 | 授权 state 与绑定票据 | 256-bit opaque token | 关联配置和意图 | ACTIVE/CONSUMED/EXPIRED | KV TTL，不记敏感正文日志 | tenantId+appCode+provider+intent+optional userId | 一次性消费，回调 redirect 必须匹配 |

| 模型ID | 当前状态 | 触发 | 目标状态 | 前置条件 | 副作用 | 失败处理 | 上游ID |
|---|---|---|---|---|---|---|---|
| DM-001 | UNVERIFIED | 用户保存实名字段 | UNVERIFIED | 字段组合合法 | 更新资料和时间 | 回滚且返回字段错误 | FR-001 |
| DM-002 | DISABLED/INCOMPLETE | 管理员保存完整配置并启用 | ENABLED | Secret 已配置、回调合法、Provider 字段完整 | 登录发现可见 | 保存失败或维持不可用 | FR-003, FR-004 |
| DM-003 | UNBOUND | 有效回调并绑定当前/已有账号 | BOUND | 外部身份无其他绑定且账号是租户成员 | 创建唯一绑定 | 唯一冲突返回冲突，不覆盖 | FR-006, FR-007 |
| DM-003 | BOUND | 本人密码校验后解绑 | UNBOUND | binding 归属本人和当前租户 | 删除绑定 | 密码/归属失败不改变 | FR-009 |
| DM-004 | ACTIVE | 完成回调或提交绑定 | CONSUMED | token 存在且上下文匹配 | 删除 KV 值 | 重放返回稳定无效错误 | FR-005 至 FR-007 |

## 4. 系统流程、事务与一致性

| 流程设计ID | 系统需求ID | 调用入口 | 参与模块 | 处理顺序 | 事务边界 | 状态变化 | 幂等键 | 并发策略 | 外部失败与补偿 | 用户可见结果 |
|---|---|---|---|---|---|---|---|---|---|---|
| FLOW-001 | FR-001, FR-002 | `/identity/me/profile`、`/identity/me/contact` | MOD-001, MOD-002 | 取当前 userId→校验本人→资料保存；联系方式则密码→Captcha→单行更新→回读掩码 | 每个更新命令一个数据库事务 | DM-001 更新 | userId | 乐观接受最后一次合法资料写；联系方式唯一约束 | Captcha 或更新失败不改变联系方式 | 真实回读资料或明确错误 |
| FLOW-002 | FR-003, FR-004 | Provider 管理与公开发现 | MOD-003 至 MOD-005 | 后端取当前 tenant/app 权限→校验字段→加密 Secret→保存；发现按显式 tenant/app 查询启用完整项 | 单配置事务 | DM-002 状态变化 | tenant+app+provider | 唯一索引；更新按主键和租户 | crypto 不可用即拒绝，不存明文 | 配置状态或可用 Provider 列表 |
| FLOW-003 | FR-005, FR-006 | authorize/complete | MOD-003 至 MOD-005、厂商 | 解析配置→校验 redirect→签发 state→跳转→原子消费→adapter 换身份→查绑定→登录/绑定/签票 | 外部 HTTP 在 DB 事务外；绑定单事务 | DM-003/DM-004 | state | KV 原子消费和 DB 唯一索引 | 厂商失败不建绑定，可重新授权 | LOGIN_SUCCESS/BIND_SUCCESS/BIND_REQUIRED |
| FLOW-004 | FR-007 | bind-existing | MOD-003 至 MOD-005、MOD-001 | 原子消费票据→账号密码校验→租户成员校验→Identity 建绑定→签发 token | 绑定事务独立；令牌只在成功后签发 | DM-003/DM-004 | bindTicket | 票据一次性和唯一索引 | 绑定失败不签 token，用户重新授权 | 已有账号登录成功 |
| FLOW-005 | FR-008, FR-009 | `/identity/me/external-identities` | MOD-001, MOD-002 | 当前 tenant/user 查询脱敏列表；解绑先验密码再校验归属删除 | 解绑单事务 | DM-003 BOUND→UNBOUND | bindingId+userId | 删除条件包含 tenant/user/id | 失败不删 | 更新后的授权状态 |

## 5. API 与远程契约设计

| 接口ID | 系统需求ID | 调用方 | 所属模块 | 入口类型 | 方法与路径 | Command Query或VO | 返回契约 | 校验 | 权限租户或数据权限 | 幂等分页或排序 | 错误码 | 兼容策略 | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| API-001 | FR-001 | 个人中心 | MOD-001, MOD-002 | HTTP | GET /identity/me/profile | CurrentUserProfileVO | R<CurrentUserProfileVO> | 有登录上下文 | AUTHENTICATED；userId 仅取上下文 | 单对象 | PROFILE_INVALID | 新增 | `rules/backend/03-api.md` | MVC/集成/JSON |
| API-002 | FR-001 | 个人中心 | MOD-001, MOD-002 | HTTP | PUT /identity/me/profile | UpdateCurrentUserProfileCommand | R<CurrentUserProfileVO> | 长度、证件组合 | AUTHENTICATED；userId 仅取上下文 | 单对象更新 | PROFILE_INVALID | 新增 | `rules/backend/03-api.md` | MVC/集成/JSON |
| API-003 | FR-002 | 个人中心 | MOD-001, MOD-002 | HTTP | POST /identity/me/contact-captcha | SendContactCaptchaCommand | R<ContactCaptchaTicketVO> | target、contactType | AUTHENTICATED；本人 | Captcha key 一次性 | CAPTCHA_INVALID | 新增 | `rules/backend/03-api.md` | Captcha 测试 |
| API-004 | FR-002 | 个人中心 | MOD-001, MOD-002 | HTTP | PUT /identity/me/contact | UpdateContactCommand | R<CurrentUserProfileVO> | password、target、key、code、type | AUTHENTICATED；本人 | Captcha 一次性 | PASSWORD_INVALID/CAPTCHA_INVALID/CONTACT_CONFLICT | 新增 | `rules/backend/03-api.md` | 事务测试 |
| API-005 | FR-003 | 管理页 | MOD-003, MOD-004, MOD-005 | HTTP | GET /auth/provider-configs | ProviderConfigQuery/ProviderConfigListVO | R<ProviderConfigListVO> | app/provider 筛选 | `auth:provider-config:view`，tenant 取上下文 | provider 稳定排序 | PROVIDER_CONFIG_INVALID | 新增 | `rules/backend/03-api.md` | MVC、权限、Secret JSON |
| API-006 | FR-003 | 管理页 | MOD-003, MOD-004, MOD-005 | HTTP | POST /auth/provider-configs | SaveProviderConfigCommand | R<ProviderConfigVO> | app/provider/url/厂商字段 | `auth:provider-config:edit`，tenant 取上下文 | 组合唯一 | PROVIDER_CONFIG_INVALID/CONFLICT | 新增 | `rules/backend/03-api.md` | MVC、权限、Secret JSON |
| API-007 | FR-003 | 管理页 | MOD-003, MOD-004, MOD-005 | HTTP | PUT /auth/provider-configs | UpdateProviderConfigCommand | R<ProviderConfigVO> | id/app/provider/url/厂商字段 | `auth:provider-config:edit`，tenant 取上下文 | id+tenant 更新 | PROVIDER_CONFIG_INVALID | 新增 | `rules/backend/03-api.md` | MVC、权限、Secret JSON |
| API-008 | FR-004 | 登录/个人中心 | MOD-003, MOD-004, MOD-005 | HTTP | GET /auth/providers | AvailableProviderQuery/AvailableProviderListVO | R<AvailableProviderListVO> | tenant/app 格式 | PUBLIC，仅返回摘要 | 固定 WECOM/DINGTALK 顺序 | PROVIDER_CONFIG_UNAVAILABLE | 新增 | `rules/backend/03-api.md` | 多租户集成 |
| API-009 | FR-005 | 登录/个人中心 | MOD-003, MOD-004, MOD-005 | HTTP | POST /auth/providers/authorize | StartProviderAuthorizationCommand | R<ProviderAuthorizationVO> | provider/tenant/app/redirect/intent | PUBLIC 或 AUTHENTICATED；BIND_CURRENT 取当前 userId | 每次新 state | PROVIDER_REDIRECT_INVALID | 新增 | `rules/backend/03-api.md` | KV/权限测试 |
| API-010 | FR-006 | 回调页 | MOD-003, MOD-004, MOD-005 | HTTP | POST /auth/providers/complete | CompleteProviderAuthorizationCommand | R<ProviderAuthorizationResultVO> | state/code | PUBLIC；状态内上下文为准 | state 一次性 | PROVIDER_STATE_INVALID/EXTERNAL_AUTH_FAILED/BINDING_CONFLICT | 新增 | `rules/backend/03-api.md` | adapter/重放测试 |
| API-011 | FR-007 | 绑定已有账号页 | MOD-003, MOD-004, MOD-005 | HTTP | POST /auth/providers/bind-existing | BindExistingAccountCommand | R<LoginVO> | ticket/username/password | PUBLIC；账号必须属于票据租户 | ticket 一次性 | BIND_TICKET_INVALID/LOGIN_FAILED | 新增 | `rules/backend/03-api.md` | 账号/租户/重放测试 |
| API-012 | FR-008 | 个人中心 | MOD-001, MOD-002 | HTTP | GET /identity/me/external-identities | CurrentExternalBindingListVO | R<CurrentExternalBindingListVO> | 有登录上下文 | AUTHENTICATED；tenant/user 均取上下文 | provider/绑定时间排序 | BINDING_NOT_FOUND | 新增自助入口，保留管理员入口 | `rules/backend/03-api.md` | 归属测试 |
| API-013 | FR-009 | 个人中心 | MOD-001, MOD-002 | HTTP | DELETE /identity/me/external-identities | UnbindCurrentExternalIdentityCommand | R<Boolean> | bindingId/password | AUTHENTICATED；tenant/user 均取上下文 | bindingId+userId | PASSWORD_INVALID/BINDING_NOT_FOUND | 新增自助入口，保留管理员入口 | `rules/backend/03-api.md` | 归属和密码测试 |
| API-014 | FR-010 | 旧客户端 | MOD-003, MOD-004, MOD-005 | HTTP/Feign | POST /auth/wecom/login | WecomLoginCommand | R<LoginVO> | 映射 tenant/app/WECOM | 与旧入口相同 | 登录命令一次 | WECOM_ACCOUNT_UNBOUND | 形状保留，数据源切换 | `rules/backend/03-api.md` | 兼容契约测试 |
| API-015 | FR-010 | 旧客户端 | MOD-003, MOD-004, MOD-005 | HTTP/Feign | GET /auth/wecom/login-config | WecomLoginConfigQuery/WecomLoginConfigVO | R<WecomLoginConfigVO> | tenant/app/WECOM | 与旧入口相同 | 单对象 | PROVIDER_CONFIG_UNAVAILABLE | 形状保留，数据源切换 | `rules/backend/03-api.md` | 兼容契约测试 |

## 6. 持久化与数据迁移设计

| 数据设计ID | 上游或模型ID | 表或实体 | 字段变化 | 约束 | 索引 | 租户审计 | Mapper边界 | 数据来源 | migration或回填 | 回滚或补偿 | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| DB-001 | DM-001, DR-001 | identity_user | real_name、document_type、document_number、verification_status、verification_source | document type/number 成对；status 非空默认 UNVERIFIED | 保持用户名、手机、邮箱索引 | 继承现有 tenant/audit | Identity core only | 当前用户输入/系统默认 | V2 增量列并同步 fresh V1；存量默认 UNVERIFIED | 测试库失败重建；正式升级前备份 | `rules/backend/07-persistence.md` | migration contract/H2 |
| DB-002 | DM-002, DR-002 | auth_provider_config 新表 | id、tenant_id、app_code、provider、client_id、provider_tenant_id、agent_id、secret_ciphertext、redirect_uris、enabled、audit | tenant+app+provider 唯一；Secret 非明文 | 唯一索引和发现索引 | TenantEntity 审计 | Auth core only | 管理端 | Auth V1 fresh migration | 删除测试表重建；生产仅备份恢复 | `rules/backend/07-persistence.md` | migration/CRUD/泄密检查 |
| DB-003 | DM-003, DR-003 | identity_external_binding | 新增 app_code，corp_id 语义统一为 providerTenantId | tenant+app+provider+providerTenantId+externalUserId 唯一 | 调整唯一索引和 user 查询索引 | 继承 tenant/audit | Identity core only | 厂商回调/存量企微 | V2 增量 appCode 默认 internal-admin 并重建唯一索引；同步 V1 | 旧绑定字段保留，升级失败停止 | `rules/backend/07-persistence.md` | 旧数据升级和冲突测试 |

## 7. 安全、权限、租户与数据边界

| 安全设计ID | 系统需求ID | 能力 | 权限资源 | 默认授权 | 后端校验入口 | 租户边界 | 数据归属断言 | 前端反馈 | 审计 | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| SEC-001 | FR-001, FR-002, FR-008, FR-009 | 当前用户资料和绑定 | AUTHENTICATED | 登录用户 | Identity controller/service | tenantId/userId 只取可信上下文 | 更新/删除 where 同时包含 userId/tenantId | 无登录跳转；验证失败保留表单 | 记录 userId/动作，不记证件/密码/code | `rules/backend/06-security.md` | 越权与日志测试 |
| SEC-002 | FR-003, FR-004 | Provider 配置 | `auth:provider-config:view/edit` | 不默认开放编辑 | Auth controller/service | 管理端 tenantId 只取上下文，公开端显式 tenant/app 严格查 | appCode 必须存在且属于当前租户授权范围 | 无权/不完整明确禁用 | 配置变化记录 provider/app，不记 Secret | `rules/backend/06-security.md` | 权限、多租户、Secret 断言 |
| SEC-003 | FR-005 至 FR-007 | OAuth state/绑定票据 | PUBLIC 或 AUTHENTICATED | 按入口 | Auth service/KV | state 绑定 tenant/app/provider/intent | BIND_CURRENT 的 userId 必须等于回调登录用户；匿名账号需租户成员 | 票据过期要求重新授权 | 只记 token hash 前缀和结果 | `rules/backend/06-security.md` | 篡改、并发、重放测试 |
| SEC-004 | FR-006, FR-009 | 外部绑定唯一与解绑 | AUTHENTICATED/内部调用 | 仅本人或认证编排 | Identity service | 绑定组合带 tenant/app | 冲突不得更新原 userId；解绑同时断言所有权 | 显示已被其他账号绑定或密码错误 | bindingId/provider/userId | `rules/backend/06-security.md` | 并发唯一和抢占测试 |

## 8. 错误码、异常与可观测性

| 错误设计ID | 系统需求ID | 失败场景 | 触发条件 | 错误码 | 异常类型 | 用户反馈 | 日志上下文 | 指标或告警 | 重试或补偿 | 敏感信息处理 |
|---|---|---|---|---|---|---|---|---|---|---|
| ERR-001 | FR-001, FR-002 | 资料或联系方式校验失败 | 字段、密码、验证码或唯一性错误 | PROFILE_INVALID/PASSWORD_INVALID/CAPTCHA_INVALID/CONTACT_CONFLICT | BizException | 定位到资料或验证字段 | requestId/userId/action | 失败计数 | 修正后重试 | 不记录证件、密码、验证码、完整联系方式 |
| ERR-002 | FR-003, FR-004 | 配置不可用 | 缺字段、Secret/crypto、租户应用不匹配 | PROVIDER_CONFIG_INVALID/UNAVAILABLE | BizException | 配置不可用原因 | tenant/app/provider/configId | 不完整配置计数 | 管理员修复 | Secret 永不序列化和记录 |
| ERR-003 | FR-005 至 FR-007 | 授权/票据无效 | state/ticket 过期、篡改、重放 | PROVIDER_STATE_INVALID/BIND_TICKET_INVALID | BizException | 重新授权 | requestId/provider/token hash | 重放计数 | 不复用旧 token | 不记录 code/token 原值 |
| ERR-004 | FR-006, FR-009 | 厂商或绑定失败 | 网络、厂商错误、已有其他绑定、归属不符 | EXTERNAL_AUTH_FAILED/BINDING_CONFLICT/BINDING_NOT_FOUND | BizException | 明确失败与重试入口 | provider/status/requestId | 厂商失败率/冲突计数 | 网络失败重新授权；冲突不补偿 | 外部标识只记 hash/尾部掩码 |

## 9. 前端结构与交互实现映射

| 前端设计ID | 系统需求ID | 页面或动作 | 页面key或路由 | 区域与组件 | 状态来源 | API依赖 | 权限或不可操作 | 空加载或失败态 | 语义测试锚点 | 复用判断 | 适用规范ruleId |
|---|---|---|---|---|---|---|---|---|---|---|---|
| UI-001 | PG-001, BT-003 | 登录页 Provider 与回调绑定 | login/provider-callback | Provider 图标按钮、回调状态、已有账号表单 | API-004 至 API-007 | API-004 至 API-007 | 无可用配置不显示；处理中防重复 | 加载、空、过期、厂商失败 | `data-provider`、`data-surface=provider-bind-existing` | 扩展现有 login flow，统一 provider action | `rules/frontend/01-vue-code.md` |
| UI-002 | PG-002, BT-001, BT-002 | 真实个人资料 | profile | 基本/实名资料、联系方式弹窗 | API-001, API-002 | API-001, API-002 | 认证状态/来源只读；验证码发送倒计时 | skeleton、保存失败保留输入 | `data-field=real-name/document-number/contact` | 重写现有本地假保存为 API 状态 | `rules/frontend/04-test.md` |
| UI-003 | PG-002, BT-003, BT-004 | 第三方授权管理 | profile authorization tab | 企微/钉钉行、绑定/解绑密码弹窗 | API-004 至 API-006, API-008 | API-004 至 API-006, API-008 | 未配置禁用并显示原因 | 空、绑定回调、解绑失败 | `data-provider`、`data-action=bind/unbind` | 页面内业务区域，不做嵌套卡片 | `rules/frontend/01-vue-code.md` |
| UI-004 | PG-003, BT-005 | Provider 配置页 | auth/provider-config | 应用筛选、两 Provider 表格、编辑抽屉 | API-003 | API-003 | view/edit 权限分别控制；Secret 留空表示保留 | 加载、空、请求失败 | `data-surface=provider-config` | @mango/auth 导出独立管理页 | `rules/frontend/12-business-api.md` |

## 10. 测试设计与验收映射

| 测试用例ID | 系统验收ID | 设计项ID | 场景 | 优先级 | 测试层级 | 自动化判断 | 测试数据 | 权限或租户边界 | 稳定契约 | 执行入口 | 证据 | 失败处理 | 适用规范ruleId |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| TC-001 | SAC-001, SAC-002 | DEC-001, DEC-002, MOD-001, MOD-002, FLOW-001, API-001, API-002, DB-001, SEC-001 | 资料回读/掩码、默认状态、密码+新联系方式验证码成功和失败 | P0 | 单元/集成/MVC | AUTO | 两用户、重复手机邮箱、有效/无效 captcha | 当前/他人 userId、tenant 1/2 | DB、API、JSON 与错误码 | Identity 模块 Maven test/verify | surefire 与定向摘要 | 泄密或越权阻断 | `rules/backend/08-test.md` |
| TC-002 | SAC-003, SAC-006 | DEC-003, MOD-003 至 MOD-005, FLOW-002, API-003, API-004, DB-002, SEC-002 | 两租户两应用配置、Secret 加密/保留/不回传、发现隔离 | P0 | 集成/API | AUTO | WECOM/DINGTALK 配置矩阵 | view/edit 权限和 tenant 1/2 | 唯一组合、configured、公开摘要 | Auth core/starter tests | surefire/JSON 断言 | 任一明文或越界阻断 | `rules/backend/08-test.md` |
| TC-003 | SAC-004, SAC-005, SAC-006 | DEC-004 至 DEC-007, FLOW-003 至 FLOW-005, API-005 至 API-009, DB-003, SEC-003, SEC-004 | 两 Provider 授权、已绑定登录、未绑定账号验证、登录态绑定、冲突、解绑、重放和旧企微兼容 | P0 | 单元/集成/API | AUTO | adapter 测试服务器、两账号两租户、一次性票据 | 匿名/登录态、当前/其他用户 | 三种结果、token、binding 唯一和旧响应形状 | Auth/Identity tests | surefire/API 报告 | 账号接管或重放阻断 | `rules/09-test-case-automation-flow.md` |
| TC-004 | SAC-001 至 SAC-006 | UI-001 至 UI-004 | 登录、回调绑定、资料、联系方式、授权和配置全状态 | P1 | COMPONENT/UI | AUTO | API fixture 和独立 E2E 账号 | 权限组合、两租户 | 语义锚点、请求 payload、错误/空态 | @mango/auth Vitest + Playwright | 报告与截图 | console/network/交互失败阻断 | `rules/frontend/04-test.md` |

## 11. 兼容与已启用能力说明影响

| 影响ID | 设计项ID | 影响对象 | 当前行为 | 目标行为 | 兼容策略 | 升级或补偿 | 已启用能力说明 | 验证 | 责任人 |
|---|---|---|---|---|---|---|---|---|---|
| IMP-001 | DEC-001, DEC-002, DB-001 | Identity 消费者和个人中心 | 基础资料可读，个人中心是假保存 | 当前用户真实资料和安全联系方式 API | 既有管理 API 保留；新增字段向后兼容 | Identity V2 migration | 更新 Identity/Auth README 与前端 profile 契约 | TC-001, TC-004 | Identity owner |
| IMP-002 | DEC-003 至 DEC-007, DB-002, DB-003 | Auth/Notice/旧企微消费者 | 企微从 Notice 配置直接登录 | Auth 配置和统一 Provider 流程 | `/auth/wecom/*` 形状保留；存量 binding appCode 回填 internal-admin；Notice 不再作为新配置源 | Auth V1 + Identity V2，同批升级 | 更新 Auth README、配置和迁移说明 | TC-002, TC-003 | Auth owner |
| IMP-003 | UI-001 至 UI-004 | @mango/auth 消费应用 | 企微弹窗和静态个人中心 | 两 Provider、回调绑定、真实资料和配置页 | 保留现有导出，新增统一 API/页面导出 | 消费应用注册回调与管理路由 | 更新 npm README 和路由示例 | TC-004 | Frontend owner |

## 12. 技术追踪矩阵

| 上游ID | 设计项ID | 测试用例ID | 覆盖说明 |
|---|---|---|---|
| SC-001, SA-001, FR-001, FR-002, UC-001, PG-002, BT-001, BT-002, DR-001, IR-003, NFR-002, SAC-001, SAC-002 | DEC-001, DEC-002, MOD-001, MOD-002, DM-001, FLOW-001, API-001, API-002, API-003, API-004, DB-001, SEC-001, ERR-001, UI-002, IMP-001 | TC-001, TC-004 | 覆盖实名资料、脱敏和联系方式双重校验 |
| SC-003, SA-003, FR-003, FR-004, UC-004, PG-003, BT-005, DR-002, NFR-001, NFR-002, SAC-003 | DEC-003, MOD-003, MOD-004, MOD-005, DM-002, FLOW-002, API-005, API-006, API-007, API-008, DB-002, SEC-002, ERR-002, UI-004, IMP-002 | TC-002, TC-004 | 覆盖租户应用配置、加密 Secret 和发现 |
| SC-002, SA-002, FR-005, FR-006, FR-007, FR-008, FR-009, FR-010, UC-002, UC-003, PG-001, BT-003, BT-004, DR-003, DR-004, IR-001, IR-002, NFR-003, NFR-004, SAC-004, SAC-005, SAC-006 | DEC-004, DEC-005, DEC-006, DEC-007, MOD-001, MOD-002, MOD-003, MOD-004, MOD-005, MOD-006, DM-003, DM-004, FLOW-003, FLOW-004, FLOW-005, API-009, API-010, API-011, API-012, API-013, API-014, API-015, DB-003, SEC-003, SEC-004, ERR-003, ERR-004, UI-001, UI-003, IMP-002, IMP-003 | TC-003, TC-004 | 覆盖两 Provider、匿名/登录态绑定、登录、解绑、防重放和兼容 |

## 13. 阶段判定与审批

| 检查项 | 结果 | 证据 |
|---|---|---|
| 技术设计 checker | PASS | 已执行仓库技术设计检查器，结果为 PASS |
| 生命周期 handoff | PASS | BRD、SRS 已批准且 upstream hash 精确匹配 |
| 专项规范检查计划 | PASS | TC-001 至 TC-004 覆盖数据库、API、安全、Provider 协议、前端和真实 UI 观察面 |
| 未关闭阻断数量 | 0 | DEC-001 至 DEC-007 均有失败关闭和兼容边界 |
| Tech Lead 审批 | APPROVED | `review/APPROVAL.md` |
