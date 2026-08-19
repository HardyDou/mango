---
documentId: TDD-ISSUES-824-826
documentType: technical-design
pmoVersion: 1.3.16
schemaRevision: 1
riskLevel: L3
riskAssessmentEvidence: requirement=L3，身份误认与 WECOM 核心发送阻断同时影响用户主流程、租户和企业主体边界；solution=L3，方案跨 Identity、Notice、Auth 与 RBAC，修改持久化数据、公开同步契约、渠道选择和用户界面；final=max(requirement,solution)
status: APPROVED
action: NEXT
owner: Mango Identity 与 Notice 技术负责人
approver: HardyDou
approvalEvidence: review/APPROVAL.md
upstreamDocumentId: SRS-ISSUES-824-826
upstreamDocumentHash: 21485835f05ff40757861b62fc09d736c9b26705ecb076152032d8b4331fd16f
---

# Issue 824 / 826 第三方身份统一来源技术设计文档

## 1. 设计输入、约束与决策

| 决策ID | 问题 | 候选方案 | 选择 | 理由 | 来源ID或路径 | 是否推断 | 影响 | 风险 | 回退条件 |
|---|---|---|---|---|---|---|---|---|---|
| DEC-001 | 第三方身份如何让用户看懂并在不启用通讯录同步时更新资料 | 回退 Mango 昵称；把掩码外部 ID 当名称；必须管理员同步全通讯录；自助绑定后按需查单成员且提供本人同步按钮 | 持久化只接收第三方完整昵称；自助绑定后尝试读取当前成员资料；个人中心展示 32px 可选头像、完整昵称、账号尾号和单账号同步图标；成功覆盖快照，失败保留已有快照 | 用户能识别绑定对象，且两边组织架构不同时也不会因获取头像昵称而被覆盖 | FR-001、SAC-001、Issue #824、用户批准的方案 A 与单账号同步补充确认 | 否 | Identity core、Auth core/profile、File、历史 WECOM SELF 数据 | 企业微信权限或可见范围不足；头像导入失败；资料同步误调组织流程 | 自助绑定的资料查询失败不影响绑定；手动失败返回脱敏原因；仅 Auth 直接查单成员，不调用 NoticeWecomSyncService；头像替换和昵称展示解耦 |
| DEC-002 | WECOM 发送身份来自何处 | 保留接收账户；Identity 优先并旧账户回退；Identity 唯一来源 | 在具体渠道配置选定后解析 CorpID，按当前租户、userId、WECOM、CorpID、BOUND 查询 Identity；不进入 WECOM 接收账户映射 | 满足单一身份来源和多企业主体隔离，避免无期限兼容 fallback | FR-002、NFR-001、Issue #826 | 否 | Notice delivery、Identity 查询 | 远程身份查询失败或渠道配置错误 | 失败关闭并记录准确发送失败，不回退旧账户 |
| DEC-003 | 通讯录同步是否保留双写开关 | 保留两个开关；默认只写 Identity；删除开关并固定维护 Identity | 删除 bindNoticeAccount、bindLoginIdentity 和旧账户写入；未变化成员仍执行身份绑定 | 产品合同不再允许 WECOM 独立接收账户，开关会继续制造分裂 | FR-002、SAC-002 | 否 | Notice API/core、RBAC UI | 消费者仍提交旧字段 | JSON 多余字段按现有兼容行为忽略；返回字段显式更名并通过构建发现强类型消费者 |
| DEC-004 | 企业微信头像如何保存 | 持久化外部 URL；页面实时读取 WECOM；导入 Mango 文件中心 | Notice 通讯录同步和 Auth 单成员资料刷新均通过 FileImportApi 导入远程头像，Identity 只保存 `avatarFileId`；Auth 页面使用现有私有文件下载接口生成临时 Object URL | 避免长期保存外部头像 URL和页面实时依赖 WECOM，复用文件权限与生命周期能力 | FR-001、FR-002、DR-001、IR-002、IR-003、NFR-003 | 否 | Notice、File、Identity、Auth | 导入失败、旧请求覆盖、Object URL 泄漏、头像删除后残留 | 导入或快照更新失败保留旧快照；新文件绑定失败则删除；成功替换或清空后清理旧文件；页面刷新或卸载时 revoke Object URL |

## 2. 模块与依赖边界

| 模块设计ID | 模块或包 | 职责 | 改动类型 | 依赖方向 | 公开能力 | 系统需求ID | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|
| MOD-001 | mango-identity-api/core | 外部身份绑定、完整昵称与头像文件 ID、有效状态过滤、掩码回读和历史显示名迁移 | 修复与扩展 | Identity core 到本域 Mapper | BindExternalIdentityCommand 与 ExternalIdentityBindingVO 增加头像快照字段 | FR-001、FR-002、DR-001 | rules/backend/04-db.md、rules/backend/07-persistence.md | core test、migration review |
| MOD-002 | mango-notice-api/core/starter | WECOM 身份选择、发送失败语义、通讯录昵称与头像导入、同步职责与结果契约 | 替换旧路径与扩展 | Notice core 通过 IdentityUserApi 和 FileImportApi gateway 访问平台能力 | SyncWecomUsersCommand 与 WecomUserSyncResultVO 语义调整 | FR-002、IR-001、IR-002、NFR-001、NFR-003 | rules/backend/01-code.md、rules/backend/10-dev-flow.md | api/core test、旧标识扫描、头像补偿测试 |
| MOD-003 | mango-auth-api/core/starter/starter-remote 与 @mango/auth | 自助绑定后单成员资料查询、当前用户单账号刷新、头像导入补偿、32px 头像、完整昵称、辅助尾号、资料未同步与解绑确认 | 修复与扩展 | Auth core 通过 WecomLoginClient 读单成员、FileImportApi/FileApi 管理头像快照、IdentityUserApi 更新绑定；Vue 页面调用 Auth 刷新 API 和现有文件下载能力 | POST /auth/providers/wecom/profile/refresh 与 profile 可观察行为 | FR-001、PG-001、BT-001、BT-003、IR-003、NFR-003 | rules/backend/01-code.md、rules/frontend/01-vue-code.md、rules/frontend/04-test.md | Auth core/starter test、vue-tsc、ESLint、Stylelint、工具函数测试、UI 验收 |
| MOD-004 | @mango/rbac 与 @mango/notice | 同步表单和能力说明移除旧双写语义 | 兼容更新 | RBAC 调用现有同步入口；Notice 页面不新增账号入口 | 同步入参与结果类型变化 | FR-002、PG-002、BT-002 | rules/frontend/06-monorepo-architecture.md | @mango/rbac 依赖构建、文本扫描 |

## 3. 技术对象与状态模型

| 模型ID | 上游ID | 模型职责 | 标识 | 关系 | 状态编码 | 审计或历史 | 归属或租户 | 一致性约束 |
|---|---|---|---|---|---|---|---|---|
| DM-001 | DR-001、FR-001 | 表达 Mango 用户的第三方绑定及第三方资料快照 | tenantId+appCode+provider+corpId+externalUserId | 多个绑定归属一个 userId；avatarFileId 引用文件中心记录 | BOUND 或非有效状态 | bindTime、lastLoginTime、bindSource | 当前租户和 userId | displayName 只来自第三方；avatarFileId 为空表示无头像；不保存外部头像 URL；查询发送只接受 BOUND |
| DM-002 | FR-002 | 表达一次 WECOM 发送实际接收身份 | task recipient id | 接收人关联 Mango userId 和发送记录 | 待发送、成功或失败 | 发送记录保存 externalUserId 和失败原因 | 当前租户、所选渠道 CorpID | 不保存 Secret、Token、AESKey；无有效绑定不得发送 |

| 模型ID | 当前状态 | 触发 | 目标状态 | 前置条件 | 副作用 | 失败处理 | 上游ID |
|---|---|---|---|---|---|---|---|
| DM-001 | 非有效或缺失 | 通讯录同步命中成员 | BOUND | 当前租户用户和 WECOM 成员、CorpID 有效 | 创建或更新外部身份 | Identity 返回失败则成员同步计入失败，不写旧账户 | FR-002 |
| DM-001 | BOUND | 自助绑定后自动获取或当前用户手动同步资料 | BOUND | 登录租户、userId、appCode、WECOM、CorpID 和 BOUND 绑定全部匹配 | 成功时替换 displayName/avatarFileId，失败时无数据变化 | 初次查询失败保留绑定；手动失败抛出脱敏业务错误；新头像已导入但绑定更新失败时删除新文件 | FR-001 |
| DM-002 | 待发送 | 渠道配置与外部身份匹配 | 成功或失败 | 当前租户、userId、WECOM、CorpID、BOUND 全部匹配 | 写入实际 wecomUserId 并调用渠道 sender | 配置无效或绑定缺失返回对应失败，不回退 | FR-002 |

## 4. 系统流程、事务与一致性

| 流程设计ID | 系统需求ID | 调用入口 | 参与模块 | 处理顺序 | 事务边界 | 状态变化 | 幂等键 | 并发策略 | 外部失败与补偿 | 用户可见结果 |
|---|---|---|---|---|---|---|---|---|---|---|
| FLOW-001 | FR-001、SAC-001 | 自助绑定后资料获取、当前用户外部身份列表、单账号刷新与解绑 | MOD-001、MOD-003 | 绑定成功→Auth 按 userid 读取单成员资料→成功则导入头像并更新快照；列表按 tenant/user/app/BOUND 查询→显示 32px 可选头像、完整昵称、辅助尾号和同步图标；手动刷新时服务端重新从登录上下文定位绑定→成功替换快照并刷新列表→解绑弹窗复用同一资料 | 远程查询、文件导入与 Identity 更新跨服务补偿；解绑沿用既有事务 | 资料快照替换不改变 BOUND；解绑可改变绑定状态 | 当前用户绑定键；前端同步中全局禁用重复点击 | 不接收客户端 userId/CorpID/userid；下载使用 request generation 防旧响应覆盖；页面刷新和卸载清理 Object URL | 自助绑定资料查询失败只保留绑定；手动失败通过统一请求反馈显示原因并保留旧快照；新头像更新失败则删除；不调用 NoticeWecomSyncService | 绑定仍成功；资料成功时显示或更新；失败时显示无权限、成员不可见、可信 IP 或企业微信错误 |
| FLOW-002 | FR-002、SAC-002 | Notice task execution | MOD-001、MOD-002 | 解析候选渠道→读取该配置 CorpID→查询有效外部身份→设置 task recipient wecomUserId→发送→记录结果 | 任务执行沿用现有事务与记录边界 | DM-002 完成 | task/record id | 候选渠道顺序保持现有策略，每个 CorpID 独立匹配 | 身份查询或配置失败继续评估允许的候选；均失败返回最后的准确身份错误 | 成功发送或“缺少有效企业微信绑定”等明确错误 |
| FLOW-003 | FR-002、UC-002 | WECOM directory sync | MOD-001、MOD-002、MOD-004 | 同步部门/成员→创建或更新用户和组织关系→保存映射→导入可用头像→始终绑定完整昵称与头像文件 ID；unchanged 分支同样修复绑定 | 沿用单成员同步边界；文件导入与 Identity 绑定跨服务补偿 | DM-001 创建或恢复 BOUND 并替换头像快照 | corpId+wecom userId | 绑定唯一约束防止抢占；仅显式头像快照允许清空旧头像 | 导入失败继续昵称同步并保留旧头像；绑定失败删除新文件；替换或清空成功后删除旧文件；绝不写旧账户 | boundIdentityCount、成员消息与可选头像 |

## 5. API 与远程契约设计

| 接口ID | 系统需求ID | 调用方 | 所属模块 | 入口类型 | 方法与路径 | Command Query或VO | 返回契约 | 校验 | 权限租户或数据权限 | 幂等分页或排序 | 错误码 | 兼容策略 | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| API-001 | FR-001、FR-002 | Auth、Notice | MOD-001 | HTTP | GET /identity/users/external-identity | BindExternalIdentityCommand、ExternalIdentityQuery、ExternalIdentityBindingVO | R<ExternalIdentityBindingVO> | provider/corpId/externalUserId 归一化，只返回 BOUND；头像文件 ID 必须为正数；显式 replaceAvatarFile 才允许清空 | tenant 从可信上下文，userId/appCode 按入口限定 | 单对象限制或当前列表稳定排序 | 沿用 IdentityCode | 新增可选 avatarFileId 与 replaceAvatarFile，旧调用方不传时保留已有头像；失效绑定从结果移除 | rules/backend/01-code.md | core 集成测试与前端类型检查 |
| API-002 | FR-002 | RBAC 管理页 | MOD-002 | HTTP | POST /notice/wecom/users/sync | SyncWecomUsersCommand、WecomUserSyncResultVO | R<WecomUserSyncResultVO> | 必填同步选项沿用现有校验 | system:user:add；租户来自上下文 | 重复同步以映射 hash 判断资料是否变化，但身份始终修复 | Notice 业务错误与成员消息 | 旧 JSON 多余字段不恢复旧行为；强类型消费者同步升级 | rules/backend/10-dev-flow.md | api/core compile、RBAC build |
| API-003 | FR-001 | @mango/auth 当前登录用户 | MOD-003 | HTTP | POST /auth/providers/wecom/profile/refresh | 无客户端身份参数 | R<Boolean> | 要求已认证且当前 tenantId/userId/appCode 完整；按当前配置 CorpID 查找 WECOM BOUND 绑定 | @ApiAccess(LOGIN)；所有身份键来自 SecurityContext 和服务端配置 | 同步中前端禁止重复请求；服务端以当前绑定快照替换 | WECOM_PROFILE_SYNC_FAILED(1505)；区分48002权限、60020可信 IP、60111成员不可见及其他企业微信错误 | 新增入口，不更改已有绑定、登录或通讯录同步契约 | rules/backend/01-code.md | AuthSecurityFlowTest、ExternalAuthorizationServiceTest、AuthApi/Feign 编译 |

## 6. 持久化与数据迁移设计

| 数据设计ID | 上游或模型ID | 表或实体 | 字段变化 | 约束 | 索引 | 租户审计 | Mapper边界 | 数据来源 | migration或回填 | 回滚或补偿 | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| DB-001 | DM-001、DR-001 | identity_external_binding | V3 清理错误 display_name；V4 新增可空 bigint `avatar_file_id` | V3 仅 provider=WECOM、bind_source=SELF 且 display_name 精确等于 identity_user.nickname；头像只存 Mango 文件 ID | 不新增索引 | 现有 tenant/audit 不变 | Identity core 本域表由 Entity/Mapper 访问，文件内容通过 File API | 第三方昵称与 Notice 导入后的 FileRecord ID | V3 清理历史误值；V4 追加字段，不修改已执行迁移 | 迁移前备份；应用补偿清理替换文件；真实第三方名称不满足 V3 条件时不处理 | rules/backend/04-db.md | SQL 条件、H2 schema 和 Identity 集成测试；真实库未执行 |
| DB-002 | DM-002、FR-002 | notice_recipient 与 notice_send_record | 不新增列；发送前把匹配 externalUserId 写入现有 recipient 字段并按现有记录链保存 | 不写 notice_recipient_account 的 WECOM 行 | 不变 | 现有租户和审计 | Notice core 仅访问本域表，Identity 通过 API gateway | Identity 外部身份 | 无 migration | 发送失败保留准确记录；不回退旧账户 | rules/backend/07-persistence.md | 集成测试资产与旧写入路径扫描 |

## 7. 安全、权限、租户与数据边界

| 安全设计ID | 系统需求ID | 能力 | 权限资源 | 默认授权 | 后端校验入口 | 租户边界 | 数据归属断言 | 前端反馈 | 审计 | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| SEC-001 | FR-001 | 当前用户绑定展示、单账号资料刷新与解绑 | AUTHENTICATED 与既有解绑密码校验 | 登录用户仅本人 | AuthController @ApiAccess(LOGIN)；ExternalAuthorizationService 从 SecurityContext 取 tenantId/userId/appCode；IdentityUserService current user methods 与现有 File 下载授权 | tenantId、userId、appCode 来自当前上下文，CorpID 来自当前应用服务端配置 | 刷新不接收客户端身份键；只查询 WECOM+CorpID+BOUND 且归属本人的单条绑定；头像只返回内部文件 ID | 外部标识掩码且仅作尾号；刷新失败显示脱敏原因；解绑确认明确目标 | 自助查询失败只记录 tenantId/appCode/userId/errorCode/type，不记录 Secret、Token、完整 externalUserId 或外部头像 URL | rules/03-ai-coding-redlines.md | 未登录接口 401、登录用户当前绑定查询、失败保留快照与脱敏日志复核 |
| SEC-002 | FR-002 | WECOM 发送身份选择 | 沿用 Notice send 和 sync 权限 | 不新增默认授权 | NoticeDeliveryService 与 IdentityUserService | Notice 和 Identity 共享当前可信 tenant context | userId、provider=WECOM、CorpID、BOUND 同时匹配 | 缺少有效绑定时准确失败 | 发送记录可含 externalUserId，严禁 Secret/Token/AESKey | rules/backend/01-code.md | CorpID 隔离、失效绑定集成测试资产 |

## 8. 错误码、异常与可观测性

| 错误设计ID | 系统需求ID | 失败场景 | 触发条件 | 错误码 | 异常类型 | 用户反馈 | 日志上下文 | 指标或告警 | 重试或补偿 | 敏感信息处理 |
|---|---|---|---|---|---|---|---|---|---|---|
| ERR-001 | FR-002 | 所选渠道配置缺少或无法解析 CorpID | configJson 无有效 corpId | CHANNEL_CONFIG_INVALID | ChannelSendResult failed | 企业微信渠道缺少或配置无效的 CorpID | taskId、channelConfigId、channelType | 沿用发送失败统计 | 修正渠道配置后重试 | 不输出 configJson 或 Secret |
| ERR-002 | FR-002、SAC-002 | 无匹配有效外部身份 | 查询失败、无数据、非 BOUND 或 externalUserId 空 | RECIPIENT_INVALID | ChannelSendResult failed | 用户缺少与渠道 CorpID 匹配的有效企业微信绑定 | taskId、recipientId、userId、channelConfigId | 沿用发送失败统计 | 修复绑定后重试 | 不记录 Secret；外部用户标识按现有安全口径处理 |
| ERR-003 | FR-001、FR-002 | 企业微信头像导入或页面下载失败 | 远程图片不可达、文件接口失败或私有文件不可读 | WECOM_PROFILE_SYNC_FAILED(1505) 或页面头像隐藏 | BizException/运行异常与页面请求异常 | 手动同步提示头像导入失败；初次自动获取不阻断绑定；页面下载失败只隐藏头像 | 仅记录内部 userId 或文件 ID，不记录完整 externalUserId 和外部头像 URL | warning 日志 | 下次单账号或通讯录同步重试；绑定更新失败清理新导入文件；保留旧快照 | 不持久化外部 URL，不在 warning 中输出完整外部用户标识 |
| ERR-004 | FR-001、SAC-001 | 企业微信单成员资料查询失败 | 应用无成员资料读取权限、服务器可信 IP 未配置、成员不存在或不在应用可见范围、企业微信其他错误 | WECOM_PROFILE_SYNC_FAILED(1505) | BizException | 48002 提示无成员资料读取权限；60020 提示未配置当前服务器可信 IP；60111 提示成员不存在或不在可见范围；其它返回脱敏 errcode/errmsg | 自助绑定日志只含 tenantId/appCode/userId/code/type；手动请求沿用统一错误响应 | 业务错误日志 | 修复权限、可见范围或可信 IP 后由用户再次点击；失败不修改快照 | 不返回 Secret、Token或完整 externalUserId |

## 9. 前端结构与交互实现映射

| 前端设计ID | 系统需求ID | 页面或动作 | 页面key或路由 | 区域与组件 | 状态来源 | API依赖 | 权限或不可操作 | 空加载或失败态 | 语义测试锚点 | 复用判断 | 适用规范ruleId |
|---|---|---|---|---|---|---|---|---|---|---|---|
| UI-001 | PG-001、BT-001、BT-003、FR-001 | 第三方绑定列表、WECOM 单账号资料同步和解绑确认 | profile?tab=authorization | 现有 el-table、32px el-avatar、link 同步按钮、Refresh 图标与 MangoDialog；包内 identity label/hint/sync 工具 | listCurrentExternalIdentities、bindingAvatarUrls、syncingExternalIdentityId 与 selectedBinding | API-001、API-003 与现有 downloadUploadedFile | 沿用登录和解绑密码校验；同步中禁用所有同步按钮防重入 | 无头像不渲染 el-avatar；昵称缺失显示点击右侧按钮指引；手动失败由统一 request interceptor 展示后端原因，页面不清空快照 | data-action=sync-wecom-profile、data-field=unbind-external-identity、external-identity-summary 与工具函数单测 | 复用现有页面和统一消息拦截器，不新增组件抽象 | rules/frontend/01-vue-code.md |
| UI-002 | PG-002、BT-002、FR-002 | WECOM 同步表单和结果 | system/user | 现有同步 MangoDialog 与 el-descriptions | syncWecomUsers 返回值 | API-002 | system:user:add；同步中禁重复提交 | 失败沿用消息列表；删除旧双写开关 | 现有弹窗语义与结果字段 | 只调整类型和字段，不新增 Notice 账号入口 | rules/frontend/04-test.md |

## 10. 测试设计与验收映射

| 测试用例ID | 系统验收ID | 设计项ID | 场景 | 优先级 | 测试层级 | 自动化判断 | 测试数据 | 权限或租户边界 | 稳定契约 | 执行入口 | 证据 | 失败处理 | 适用规范ruleId |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| TC-001 | SAC-001 | DEC-001、DEC-004、MOD-001、MOD-003、FLOW-001、API-003、SEC-001、ERR-003、ERR-004、UI-001 | 自助绑定自动获取单成员昵称头像；无权限或传输异常仍绑定成功；手动同步成功覆盖快照和删除旧头像；失败保留旧快照并清理未绑定新头像；未登录 API 拒绝；页面展示与空资料指引 | P1 | 单元/集成/组件 | AUTO | corp-a/external-a，新旧昵称头像，48002 权限错误，传输异常，externalUserId 尾号 4826，BOUND/UNBOUND | tenant-a、当前 userId=7、admin-app；刷新入口不传身份键 | WecomLoginClient 单成员契约、Auth API、Identity 快照替换和 external identity 工具函数 | DefaultWecomLoginClientTest、ExternalIdentityAvatarServiceTest、ExternalAuthorizationServiceTest、AuthSecurityFlowTest、externalIdentity.spec.ts、vue-tsc/ESLint/Stylelint | Auth Core 定向测试、Auth Starter 45/45、Auth 前端 14/14 和静态检查通过 | 失败时修复实现或断言，不弱化“绑定成功/资料失败”隔离、当前账号定位和失败保留规则 | rules/09-test-case-automation-flow.md |
| TC-002 | SAC-002 | DEC-002、DEC-003、DEC-004、FLOW-002、FLOW-003、API-002、SEC-002、ERR-003 | 无旧账户发送、CorpID 隔离、失效绑定、未变化成员修复绑定、头像导入与移除 | P0 | 集成 | AUTO | corp-a/corp-b、BOUND/UNBOUND、空 notice_recipient_account、WECOM 头像 URL stub 与无头像快照 | 当前 tenant 与 Mango userId | userId→Identity externalUserId→WECOM sender；远程头像→File ID→Identity | NoticeServiceIntegrationTest | Notice 20/20 通过；测试 stub 不访问真实 WECOM；无渠道配置路径保留 | 任一 fallback、错选或头像阻断昵称同步则阻断交付 | rules/09-test-case-automation-flow.md |
| TC-003 | SAC-001、SAC-002 | MOD-001 至 MOD-004、API-001、API-002 | Java 和 TypeScript 契约、导入、迁移与旧标识静态一致性 | P0 | 静态/编译 | AUTO | 当前 worktree 源码 | 不使用真实凭据；租户条件代码复核 | direct module compile、package build、rg、diff check | Maven compile/test-compile 与 pnpm build | 命令输出 | 编译或残留失败时修复后重跑 | rules/11-delivery-assurance.md |
| TC-004 | SAC-001、SAC-002 | UI-001、UI-002、FLOW-001、FLOW-002 | 个人中心 32px 头像、昵称、尾号、无头像、资料未同步、单账号同步图标/loading/成功/失败保留、解绑确认、管理员同步弹窗和企业微信投递 | P0 | UI/API/人工 | MANUAL | 隔离数据库绑定用户、真实文件中心图片；浏览器路由拦截 profile refresh 成功和 1505 失败响应 | 当前 tenant、CorpID、登录与同步权限；不发出真实企业微信网络请求 | 浏览器页面、数据库、私有文件下载和拦截的 Auth API 响应 | 按 SRS 场景执行 | 本轮重启后验收个人中心、单账号同步交互和解绑确认；真实 WECOM 资料查询与投递仍不执行 | 真实企业微信权限、可信 IP、可见范围和投递仍由 QA 在授权测试企业验收 | rules/frontend/04-test.md |

## 11. 兼容与已启用能力说明影响

| 影响ID | 设计项ID | 影响对象 | 当前行为 | 目标行为 | 兼容策略 | 升级或补偿 | 已启用能力说明 | 验证 | 责任人 |
|---|---|---|---|---|---|---|---|---|---|
| IMP-001 | DEC-001、DEC-004、DB-001、API-003、UI-001 | Identity、Notice、Auth、File 与 @mango/auth 消费者 | 缺失显示名可能保存 Mango 昵称，页面只显示掩码 ID，无第三方头像快照，个人补资料只能依赖通讯录同步 | displayName 只存第三方完整昵称，头像只存 Mango 文件 ID；自助绑定后按需查单成员；个人中心显示 32px 可选头像、昵称、辅助尾号、单账号同步图标或资料未同步 | Identity API 增加可选头像字段；Auth API 新增当前用户 WECOM 资料刷新入口；旧调用不传 replaceAvatarFile 时保留头像；V3/V4 处理历史误值和头像字段 | 同批升级 Identity migration、Auth、Notice 与前端包；不需启用通讯录同步 | 更新 mango-auth、mango-identity、mango-notice README | TC-001、TC-003、TC-004 | Identity/Notice/Auth owner |
| IMP-002 | DEC-002、DEC-003、API-002、UI-002 | Notice、RBAC 和 WECOM 消费者 | 同步可双写，发送读取旧账户 | Identity 是 WECOM 用户身份唯一来源 | 删除旧开关和结果字段，消费者需同批更新；不保留 fallback | 后端与 @mango/rbac 同批发布时升级 | 更新 mango-notice、@mango/notice、@mango/rbac README | TC-002 至 TC-004 | Notice/RBAC owner |

## 12. 技术追踪矩阵

| 上游ID | 设计项ID | 测试用例ID | 覆盖说明 |
|---|---|---|---|
| SC-001、SA-001、FR-001、UC-001、PG-001、BT-001、BT-003、DR-001、IR-002、IR-003、NFR-002、NFR-003、SAC-001 | DEC-001、DEC-004、MOD-001、MOD-003、DM-001、FLOW-001、API-001、API-003、DB-001、SEC-001、ERR-003、ERR-004、UI-001、IMP-001 | TC-001、TC-003、TC-004 | 覆盖自助绑定后单成员资料获取、当前用户单账号同步、成功覆盖、失败保留、权限错误、头像补偿、辅助尾号、解绑识别和组织架构隔离 |
| SC-002、SA-002、FR-002、UC-002、PG-002、BT-002、DR-001、IR-001、NFR-001、SAC-002 | DEC-002、DEC-003、MOD-001、MOD-002、MOD-004、DM-001、DM-002、FLOW-002、FLOW-003、API-001、API-002、DB-002、SEC-002、ERR-001、ERR-002、UI-002、IMP-002 | TC-002、TC-003、TC-004 | 覆盖租户和 CorpID 身份隔离、发送与同步单一来源、错误语义和消费者更新 |

## 13. 阶段判定与审批

| 检查项 | 结果 | 证据 |
|---|---|---|
| 技术设计 checker | PASS | `node mango-pmo/tools/check-technical-design.mjs --document mango-docs/designs/issues-824-826/technical-design.md` |
| 生命周期 handoff | PASS | BRD、SRS 已批准且 upstream ID 与 SHA-256 精确匹配 |
| 专项规范检查计划 | PASS | TC-001 至 TC-004 映射 M09-M13 与人工验收；本轮执行限制明确 |
| 未关闭阻断数量 | 0 | 真实 WECOM、数据库和浏览器属于未执行验收，不冒充通过 |
| Tech Lead 审批 | APPROVED | `review/APPROVAL.md` |
