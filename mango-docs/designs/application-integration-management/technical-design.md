---
documentId: TDD-APPLICATION-INTEGRATION-001
documentType: technical-design
pmoVersion: 1.4.9
schemaRevision: 1
riskLevel: L3
riskAssessmentEvidence: "涉及权限、当前部署上下文、入站密钥、出站凭证、出站网络访问、真实数据库和前端入口，最终风险按 L3 处理；本期不实现多租户运行时和接口级授权。"
status: DRAFT
action: WRITE
owner: Mango Tech Lead
approver: 待技术负责人确认
approvalEvidence: 待补充技术审批记录
upstreamDocumentId: SRS-APPLICATION-INTEGRATION-001
upstreamDocumentHash: c48f8d462249ddb97b6cc726cade471b99271fa257c11d019a6ad6d85274a139
---

# 逻辑应用与集成应用管理技术设计

## 0. 使用合同与最小填写方式

设计输入为现有授权、认证、访问控制、加密、菜单资源和前端能力。逻辑应用复用现有 `authorization_app`，不新增端字段和应用级绑定；集成应用新增独立表、API、连接器和页面。实现保持真实数据库、当前部署上下文、后端权限和真实接口链路；tenantId 仅保留为数据结构字段。

## 1. 设计输入、约束与决策

| 决策ID | 问题 | 候选方案 | 选择 | 理由 | 来源ID或路径 | 是否推断 | 影响 | 风险 | 回退条件 |
|---|---|---|---|---|---|---|---|---|---|
| DEC-001 | 模块归属 | 新模块/扩展 authorization | 扩展 authorization | 应用和权限已有归属 | BS-001、`mango-authorization` | 否 | 复用模块 | 边界扩大 | 独立需求批准 |
| DEC-002 | 对象模型 | 复用逻辑应用表/新表 | 新建集成应用表 | 生命周期、双向配置和敏感字段不同 | BR-001 | 否 | 表和 API 独立 | 数据重复 | 取消本期集成模块 |
| DEC-003 | 入站密钥存储 | 明文/摘要/加密 | Mango Crypto 加密 | 需要轮换和未来读取 | BR-003、`mango-infra-crypto` | 否 | 增加 codec | 密钥托管 | 发布前确认 KMS |
| DEC-004 | 出站凭证存储 | 与入站共用/独立字段 | 独立字段和版本 | 入站由 Mango 签发，出站由合作方提供 | BR-005 | 否 | 双凭证模型 | 字段增多 | 收敛为单向时删除出站字段 |
| DEC-005 | 删除策略 | 删除/停用 | 首版仅启停 | 保留历史和降低误删 | BR-008 | 否 | API 无删除 | 累积停用记录 | 后续归档需求 |
| DEC-006 | 查询方式 | 全量/分页 | 分页 | 数量增长和最小返回 | FR-003 | 否 | 新增分页契约 | 页面复杂度 | 小规模也保留分页 |
| DEC-007 | 逻辑应用多端建模 | 新增端实体/拆为多个逻辑应用 | 拆分多个逻辑应用 | 端不是权限边界，现有应用表已可承载 | BS-006 | 否 | 不新增端字段和端表 | 入口数量增长 | 出现聚合需求时增加可选分组字段 |
| DEC-008 | 出站认证方式范围 | 只做一种/枚举常见方式 | 枚举 NONE、API_KEY、BEARER_TOKEN、BASIC、HMAC_SIGNATURE | 覆盖多数企业集成场景 | BS-004 | 是 | 需要逐项实现签名逻辑 | 认证实现缺陷 | 只保留已验证方式 |
| DEC-009 | 出站实现位置 | 引入 Spring WebClient/使用 JDK HttpClient | 使用 JDK HttpClient 纯对象执行器 | 不新增 Web 依赖且便于单元验证 | `java.net.http` | 否 | core 增加连接器 | 功能受限 | 后续按需替换 |
| DEC-010 | 出站网络边界 | 不限制/默认限制 | 默认拒绝回环和私网目标，可配置放开 | 降低被用于内网探测的风险 | BR-006 | 否 | 需部署配置 | 内网合作方需放开 | 显式配置 `allow-private-network` |
| DEC-011 | 集成应用数据权限 | 应用角色数据范围/只做部署级过滤 | 只做部署级过滤 | 集成应用是平台级凭证配置，不按组织隔离 | BS-011 | 否 | 查询不调用 DataScopeApplier | 后续需要组织隔离时须新增需求 | 新增数据权限需求 |
| DEC-012 | 微应用总览数据来源 | 新建微应用表/复用前端运行单元注册表 | 复用 `authorization_frontend_app_registry` | 注册表已由部署资源声明维护，避免双份事实 | BS-012、BR-009 | 否 | 只读页面，无写接口 | 注册表无数据时页面为空 | 需要人工登记时新增需求 |

## 2. 模块与依赖边界

| 模块设计ID | 模块或包 | 职责 | 改动类型 | 依赖方向 | 公开能力 | 系统需求ID | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|
| MOD-001 | authorization-api | 集成应用契约、命令、查询、结果和枚举 | 新增 | 依赖公共 API | 集成应用管理契约 | FR-002~FR-010 | backend/naming | 编译/API 测试 |
| MOD-002 | authorization-core | 实体、Mapper、Service、事务、加密和出站连接器 | 新增 | 依赖 persistence/crypto | 真实数据服务和连接能力 | FR-002~FR-010 | backend/persistence | 集成测试 |
| MOD-003 | authorization-starter | 管理接口、资源声明和配置属性装配 | 新增 | 依赖 core/web | HTTPS 管理入口 | FR-002~FR-010 | backend/module-menu | 资源检查 |
| MOD-004 | rbac | 列表、分步表单、凭证提示和连接测试 | 新增 | 依赖 http/auth | 管理页面 | PG-002~PG-005 | frontend | UI 测试 |
| MOD-005 | rbac | 微应用运行单元总览页面 | 新增 | 依赖 http/auth | 只读总览 | FR-011、PG-006 | frontend | UI 测试 |

## 3. 技术对象与状态模型

| 模型ID | 上游ID | 模型职责 | 标识 | 关系 | 状态编码 | 审计或历史 | 归属或租户 | 一致性约束 |
|---|---|---|---|---|---|---|---|---|
| DM-001 | DR-001 | 集成应用主体 | Long id/applicationCode | 持有入站和出站配置 | 0/1 | created/updated | tenantId/orgId | applicationCode 全局唯一 |
| DM-002 | DR-002 | 入站凭证版本 | applicationId/version | 从属于集成应用 | current/rotated | 轮换时间/版本 | 同应用归属 | 版本递增、clientId 唯一 |
| DM-003 | DR-003 | 出站凭证版本 | applicationId/version | 从属于集成应用 | current/updated | 更新时间/版本 | 同应用归属 | 版本递增 |
| DM-004 | DR-004、DR-005 | 出站连接配置和扩展参数 | applicationId | 从属于集成应用 | 固定配置 | updated | 同应用归属 | 协议和认证方式枚举、地址 http/https |

| 模型ID | 当前状态 | 触发 | 目标状态 | 前置条件 | 副作用 | 失败处理 | 上游ID |
|---|---|---|---|---|---|---|---|
| DM-001 | new | 创建提交 | ENABLED | 方向配置完整、加密成功 | 写入记录 | 事务回滚 | FR-002 |
| DM-001 | ENABLED | 停用 | DISABLED | 当前部署且有权限 | 更新状态 | 原状态保持 | FR-010 |
| DM-001 | DISABLED | 启用 | ENABLED | 未到期且有权限 | 更新状态 | 到期拒绝 | FR-010 |
| DM-002 | current | 轮换入站密钥 | rotated/current | 已开启入站且有权限 | 更新密文和版本 | 旧版本保持 | FR-006 |
| DM-003 | current | 更新出站凭证 | current | 已开启出站且非免认证 | 更新密文和版本 | 旧版本保持 | FR-007 |

## 4. 系统流程、事务与一致性

| 流程设计ID | 系统需求ID | 调用入口 | 参与模块 | 处理顺序 | 事务边界 | 状态变化 | 幂等键 | 并发策略 | 外部失败与补偿 | 用户可见结果 |
|---|---|---|---|---|---|---|---|---|---|---|
| FLOW-001 | FR-002 | POST create | MOD-003→MOD-002→crypto→DB | 校验方向→生成入站密钥→加密出站凭证→写库 | 单事务 | new→enabled | 无，失败重新提交 | applicationCode 唯一约束 | 加密/DB 失败回滚 | 一次性入站凭证 |
| FLOW-002 | FR-003/FR-004 | GET page/detail | MOD-003→MOD-002→DB | 当前部署上下文→查询→脱敏 | 只读 | 无 | 无 | DB 查询 | 查询失败错误反馈 | 列表/详情 |
| FLOW-003 | FR-005 | PUT update | MOD-003→MOD-002→crypto→DB | 校验→方向比对→可选加密新凭证→更新 | 单事务 | 配置更新 | applicationId | 行级更新 | 失败原值保持 | 更新结果 |
| FLOW-004 | FR-006 | POST rotate-inbound-secret | MOD-003→MOD-002→crypto→DB | 查询→生成→加密→版本递增 | 单事务 | version+1 | applicationId | 行级更新 | 失败旧版本保持 | 一次性入站凭证 |
| FLOW-005 | FR-007 | POST outbound-credential | MOD-003→MOD-002→crypto→DB | 查询→校验认证方式→加密→版本递增 | 单事务 | version+1 | applicationId | 行级更新 | 失败旧版本保持 | 保存结果 |
| FLOW-006 | FR-008 | POST test-connection | MOD-003→MOD-002→连接器→外部系统 | 查询→解密出站凭证→地址校验→发起请求 | 无事务 | 无 | 无 | 单次请求，受超时约束 | 失败返回原因 | 状态码和耗时 |

## 5. API 与远程契约设计

| 接口ID | 系统需求ID | 调用方 | 所属模块 | 入口类型 | 方法与路径 | Command Query或VO | 返回契约 | 校验 | 权限租户或数据权限 | 幂等分页或排序 | 错误码 | 兼容策略 | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| API-001 | FR-002 | rbac | MOD-003 | HTTPS | GET /integration/connectors/page | PageQuery/定义列表 | R<PageResult> | page/size/keyword/category | list + 当前部署 | 分页 | 401/403 | 新增接口 | backend/code | API测试 |
| API-002 | FR-002 | rbac | MOD-003 | HTTPS | GET /integration/connectors/detail | 定义详情 | R<IntegrationConnectorVO> | connectorCode 必填 | query + 当前部署 | 无 | 404 | 新增接口 | backend/code | API测试 |
| API-003 | FR-005 | rbac | MOD-003 | HTTPS | GET /integration/credentials | 凭证列表 | R<List> | connectorCode 可选 | query + 当前租户 | 无 | 401/403 | 新增接口 | backend/code | API测试 |
| API-004 | FR-003 | rbac | MOD-003 | HTTPS | POST /integration/credentials | Command/boolean | R<Boolean> | 字段按认证规范 | add + 当前租户 | 无 | 400/404 | 新增接口 | backend/code | 集成测试 |
| API-005 | FR-004 | rbac | MOD-003 | HTTPS | POST /integration/credentials/rotate | Command/boolean | R<Boolean> | 凭证编码与字段 | rotate + 当前租户 | 字段版本递增 | 400/404 | 新增接口 | backend/code | 集成测试 |
| API-006 | FR-006 | rbac | MOD-003 | HTTPS | GET /integration/instances/page | PageQuery/实例列表 | R<PageResult> | page/size/keyword/status | list + 当前租户 | 分页 | 401/403 | 新增接口 | backend/code | API测试 |
| API-012 | FR-002 | rbac | MOD-003 | HTTPS | POST /integration/connectors | Command/boolean | R<Boolean> | 编码/名称/分类/配置规范/认证规范/能力 | edit + 当前部署 | 编码唯一，覆盖保存 | 400 | 新增接口 | backend/code | API测试 |
| API-007 | FR-010 | rbac | MOD-003 | HTTPS | GET /integration/instances/detail | 实例详情 | R<IntegrationInstanceVO> | instanceCode 必填 | query + 当前租户 | 无 | 404 | 新增接口 | backend/code | API测试 |
| API-008 | FR-006、FR-007、FR-009 | rbac | MOD-003 | HTTPS | POST /integration/instances | Command/boolean | R<Boolean> | 配置规范与能力清单 | add + 当前租户 | 编码唯一 | 400/404 | 新增接口 | backend/code | 集成测试 |
| API-009 | FR-011 | rbac | MOD-003 | HTTPS | GET /authorization/apps | 无入参/运行单元列表 | R<List> | 无 | authorization:app:list | 无 | 401/403 | 复用既有接口 | backend/code | UI 联调 |
| API-011 | FR-008 | rbac | MOD-003 | HTTPS | POST /integration/instances/change-status | Command/boolean | R<Boolean> | instanceCode/状态 | edit + 当前租户 | 无 | 400/404 | 新增接口 | backend/code | API测试 |
| API-010 | FR-012 | rbac | MOD-003 | HTTPS | POST /authorization/frontend-apps/health-check | Command/可达性结果 | R<FrontendAppHealthCheckVO> | runtimeCode 必填 | authorization:app:query | 无 | 400/404 | 新增接口，语义为静态可达性 | backend/code | 单元/集成测试 |

## 6. 持久化与数据迁移设计

| 数据设计ID | 上游或模型ID | 表或实体 | 字段变化 | 约束 | 索引 | 租户审计 | Mapper边界 | 数据来源 | migration或回填 | 回滚或补偿 | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| DB-001 | DM-001、DR-001 | `authorization_integration_application` | 新增 id/tenant/code/name/direction | applicationCode 非空唯一 | tenant+status、tenant+direction | 基类审计 | 只按当前部署上下文查询 | 管理命令 | authorization migration 新表，无回填 | 回滚 migration 前确认无业务数据 | backend/db | migration/集成 |
| DB-002 | DM-002、DR-002 | 同表入站字段 | 新增 authType/clientId/密文/版本/轮换时间/白名单 | 密文非空、版本递增、clientId 唯一 | clientId 唯一 | 同租户 | 不在列表解密 | Mango Crypto 生成 | 创建时写入 | 轮换失败保留旧值 | backend/security | 密文回读 |
| DB-003 | DM-003、DR-003 | 同表出站凭证字段 | 新增密文/版本/更新时间 | 密文非空、版本递增 | 无 | 同租户 | 只有连接测试解密 | 合作方提供 | 创建或更新时写入 | 更新失败保留旧值 | backend/security | 密文回读 |
| DB-004 | DM-004、DR-004、DR-005 | 同表出站配置字段 | 新增协议/地址/认证方式/超时/重试/扩展参数/有效期/状态 | 枚举合法、地址 http/https、JSON 对象 | tenant+status | 同租户 | 管理命令写入 | 管理员输入 | 无回填 | 更新失败原值保持 | backend/db | API测试 |

表字段：id bigint、tenant_id varchar(64)、org_id bigint、application_code varchar(64)、application_name varchar(100)、direction varchar(16)、inbound_auth_type varchar(32)、client_id varchar(96)、inbound_secret_ciphertext varchar(2048)、inbound_secret_version int、inbound_secret_rotated_at datetime、inbound_ip_allowlist text、outbound_protocol varchar(32)、outbound_base_url varchar(500)、outbound_auth_type varchar(32)、outbound_credential_ciphertext varchar(2048)、outbound_credential_version int、outbound_credential_rotated_at datetime、outbound_timeout_ms int、outbound_retry_max int、extension_config_json text、expires_at datetime、status tinyint、remark varchar(500) 及审计字段。唯一键为 applicationCode 和 clientId。

## 7. 安全、权限、租户与数据边界

| 安全设计ID | 系统需求ID | 能力 | 权限资源 | 默认授权 | 后端校验入口 | 租户边界 | 数据归属断言 | 前端反馈 | 审计 | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| SEC-001 | FR-002~FR-010 | 管理接口保护 | list/query/add/edit/rotate/test | 仅平台管理员套餐 | Controller + Service | 当前部署上下文 | 请求 tenant 不可信 | 无权态/403 | 记录动作不记凭证 | backend/security | 安全测试 |
| SEC-002 | FR-006、FR-009 | 入站密钥加密脱敏 | rotate/add | 无默认公开授权 | Crypto codec、VO 映射 | 同租户 | 原文不落库/日志 | 一次性弹窗 | 记录版本不记原文 | backend/security | 脱敏测试 |
| SEC-003 | FR-007、FR-009 | 出站凭证只写不读 | outbound-credential | 无默认公开授权 | Crypto codec、VO 掩码 | 同租户 | 响应不含原文 | 只显示已配置 | 记录版本不记原文 | backend/security | 脱敏测试 |
| SEC-004 | FR-008 | 出站目标限制 | test | 仅平台管理员套餐 | 连接器地址校验 | 当前部署上下文 | 默认拒绝回环和私网 | 明确失败原因 | 记录结果不记凭证 | backend/security | 单元测试 |
| SEC-005 | FR-002、FR-005 | 输入校验 | add/edit | 不适用 | Service Require | 当前部署上下文 | 枚举、地址、JSON 和 IP 校验 | 字段错误 | 不记原文 | backend/security | 集成测试 |

## 8. 错误码、异常与可观测性

| 错误设计ID | 系统需求ID | 失败场景 | 触发条件 | 错误码 | 异常类型 | 用户反馈 | 日志上下文 | 指标或告警 | 重试或补偿 | 敏感信息处理 |
|---|---|---|---|---|---|---|---|---|---|---|
| ERR-001 | FR-002、FR-005 | 参数非法 | 校验失败 | 400 | 业务校验异常 | 字段错误 | traceId/字段名 | 无 | 修改后重试 | 不记凭证 |
| ERR-002 | FR-003~FR-010 | 无权或上下文缺失 | 权限或当前部署上下文不符 | 403 | 访问异常 | 无权/统一拒绝 | traceId/摘要 | 安全告警 | 不重试 | 不泄露数据 |
| ERR-003 | FR-002、FR-006、FR-007 | 加密或数据库失败 | 外部/DB 异常 | 500 | 系统异常 | 操作失败 | traceId/错误类型 | 失败计数 | 事务回滚 | 不记原文 |
| ERR-004 | FR-002 | 编码重复 | 唯一约束 | 400 | 业务校验异常 | 编码已存在 | traceId/编码 | 冲突计数 | 修改编码后重试 | 不记凭证 |
| ERR-005 | FR-008 | 出站连接失败 | 地址受限、超时或目标异常 | 200 业务结果 | 连接结果对象 | 失败原因和耗时 | traceId/异常类型 | 连接失败计数 | 由调用方决定重试 | 不记请求头 |

## 9. 前端结构与交互实现映射

| 前端设计ID | 系统需求ID | 页面或动作 | 页面key或路由 | 区域与组件 | 状态来源 | API依赖 | 权限或不可操作 | 空加载或失败态 | 语义测试锚点 | 复用判断 | 适用规范ruleId |
|---|---|---|---|---|---|---|---|---|---|---|---|
| UI-001 | PG-002、FR-003 | 集成应用列表 | `system/integration-application` | 统一搜索区（MangoSearchPanel）、统一列表面板（MangoListPanel）、入站和出站列、主操作与更多操作 | 真实接口 | API-001/002/004/005/006/007/008 | apiCodes | 空/加载/失败/无权 | TC-005 | 复用框架列表与搜索基线 | frontend |
| UI-002 | PG-003、FR-002 | 创建和编辑表单 | 同上 | 基础、入站、出站、扩展参数分区 | 表单+真实接口 | API-003/004 | add/edit | 校验失败/提交失败 | TC-005 | 复用表单基线 | frontend |
| UI-003 | PG-004、FR-006 | 一次性入站凭证提示 | 弹窗组件 | clientId、clientSecret、版本 | 创建或轮换响应内存 | API-003/005 | add/rotate | 成功/关闭 | TC-004/005 | 新增敏感交互 | frontend |
| UI-004 | PG-005、FR-007、FR-008 | 出站凭证与连接测试 | 弹窗、行内更多操作与消息 | 凭证输入、测试结果 | 真实接口 | API-006/007 | rotate/test | 失败提示 | TC-005 | 复用框架弹窗与列表操作基线 | frontend |
| UI-005 | PG-006、FR-011、FR-012 | 微应用总览 | `system/micro-application` | 统一搜索区、总览表格、可达性标签、引用弹层和登记来源提示 | 真实接口 | API-001、API-009、API-010 | apiCodes | 空/加载/失败/无权 | TC-009 | 复用框架列表基线，只读 | frontend |

## 10. 测试设计与验收映射

| 测试用例ID | 系统验收ID | 设计项ID | 场景 | 优先级 | 测试层级 | 自动化判断 | 测试数据 | 权限或租户边界 | 稳定契约 | 执行入口 | 证据 | 失败处理 | 适用规范ruleId |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| TC-001 | SAC-006、SAC-003 | DM-002、DM-003、FLOW-004、FLOW-005 | 入站轮换和出站凭证更新 | P0 | 集成 | AUTO | 当前部署、多版本 | 当前部署上下文 | 旧版本回滚 | `mvn -f mango/pom.xml -pl :mango-authorization-core -Dtest=IntegrationApplicationServiceTest test` | surefire 报告 | 阻断交付 | backend/test |
| TC-002 | SAC-002、SAC-004、SAC-007 | FLOW-001、FLOW-003 | 创建、方向校验和启停 | P0 | 集成/API | AUTO | 当前部署 | add/edit | 真实 DB | `mvn -f mango/pom.xml -pl :mango-authorization-core -Dtest=IntegrationApplicationServiceTest test` | surefire 报告 | 修复后重跑 | backend/test |
| TC-003 | SAC-002、SAC-005 | SEC-005、ERR-001 | 非法 IP、非法扩展参数和非法出站地址 | P0 | 集成 | AUTO | 非法 JSON、非法 IP、ftp 地址 | 当前部署 | 400 且不写库 | `mvn -f mango/pom.xml -pl :mango-authorization-core -Dtest=IntegrationApplicationServiceTest test` | surefire 报告 | 修复后重跑 | backend/test |
| TC-004 | SAC-002、SAC-003 | SEC-002、SEC-003、DM-002、DM-003 | 入站密钥与出站凭证脱敏 | P0 | API/日志 | AUTO | 创建和更新响应 | 全部角色 | 原文仅入站首次返回 | `mvn -f mango/pom.xml -pl :mango-authorization-core -Dtest=IntegrationApplicationServiceTest test` | 输出证据 | 清理原文 | backend/security |
| TC-005 | SAC-002~SAC-005 | UI-001~UI-004 | 页面真实链路 | P1 | UI | AUTO | 当前部署 | 权限角色 | 非 mock API、无 console error、无 4xx/5xx | `pnpm --filter mango-admin test:e2e -- e2e/specs/integration-application-management.spec.ts` | 截图/报告 | 修复后重跑 | frontend |
| TC-006 | SAC-001 | MOD-003 | 菜单与权限资源 | P1 | 资源 | AUTO | 资源注册 | 平台管理员 | apiCodes 一致 | resource checker | 输出证据 | 修复资源 | backend/module-menu |
| TC-007 | SAC-005 | SEC-004、ERR-005 | 受限目标连接测试和出站认证方式 | P0 | 单元 | AUTO | 回环地址、五种认证方式 | 当前部署 | 默认拒绝且不发起外部请求、请求头与签名正确 | `mvn -f mango/pom.xml -pl :mango-authorization-core -Dtest=OutboundHttpInvokerTest test` | surefire 报告 | 修复后重跑 | backend/test |
| TC-008 | SAC-006 | SEC-001、SEC-002 | 未登录和缺权限访问 | P1 | API | MANUAL | 真实账号 | 无权限角色 | 401/403 | 手工请求或 E2E | 验收证据 | 补充请求记录 | backend/security |
| TC-009 | SAC-008、SAC-009 | UI-005、MOD-005、DEC-012 | 微应用总览与健康检查真实链路 | P1 | UI | AUTO | 当前部署运行单元 | 权限角色 | 非 mock API、无 console error、无 4xx/5xx | `pnpm --filter mango-admin test:e2e -- e2e/specs/micro-application.spec.ts` | 截图/报告 | 修复后重跑 | frontend |
| TC-010 | SAC-009 | API-010、SEC-004 | 运行单元可达性探测语义 | P0 | 集成 | AUTO | 本地探测目标、未登记编码 | 当前部署 | 成功/失败/受限目标可区分 | `mvn -f mango/pom.xml -pl :mango-authorization-core -Dtest=FrontendAppHealthServiceTest test` | surefire 报告 | 修复后重跑 | backend/test |

## 11. 兼容与已启用能力说明影响

| 影响ID | 设计项ID | 影响对象 | 当前行为 | 目标行为 | 兼容策略 | 升级或补偿 | 已启用能力说明 | 验证 | 责任人 |
|---|---|---|---|---|---|---|---|---|---|
| IMP-001 | MOD-001~MOD-003 | authorization | 已有逻辑应用管理 | 应用管理目录下挂逻辑应用和集成应用两个入口 | 不修改既有逻辑应用契约 | 菜单资源新增目录并提升声明版本 | README 增加菜单结构和集成应用边界 | TC-006 | Tech Lead |
| IMP-002 | UI-001~UI-004 | rbac | 已有应用页面 | 增加集成应用页面和资源 | 不修改现有页面行为 | 新路由/资源 | 能力地图新增管理入口 | TC-005 | Frontend |
| IMP-003 | SEC-004 | 出站运行时 | 本模块只提供定义与声明 | 运行时调用、令牌、验签由业务域实现 | 不改变既有调用代码 | 业务域自行接入 | README 说明职责边界 | TC-007 | Security |
| IMP-004 | DEC-009 | core | 无出站连接依赖 | 使用 JDK HttpClient 实现 | 不新增 Web 依赖 | 后续可替换实现 | 文档说明实现边界 | TC-007 | Tech Lead |

## 12. 技术追踪矩阵

| 上游ID | 设计项ID | 测试用例ID | 覆盖说明 |
|---|---|---|---|
| FR-001 | DEC-007、MOD-004 | TC-005 | 逻辑应用不引入端分类 |
| FR-002 | API-003、FLOW-001、DB-001、DEC-002 | TC-002、TC-003 | 创建和方向校验 |
| FR-003 | API-001、FLOW-002、SEC-001 | TC-005 | 分页查询 |
| FR-003 | DEC-011 | TC-005 | 分页不应用行级数据权限 |
| FR-004 | API-002、FLOW-002 | TC-005 | 详情脱敏 |
| FR-005 | API-004、FLOW-003、DEC-004 | TC-002 | 更新和凭证保持 |
| FR-006 | API-005、FLOW-004、DM-002、DEC-003 | TC-001、TC-004 | 入站轮换一致性 |
| FR-007 | API-006、FLOW-005、DM-003、SEC-003 | TC-001、TC-004 | 出站凭证保存 |
| FR-008 | API-007、FLOW-006、SEC-004、DEC-009 | TC-007 | 出站连接测试 |
| FR-009 | SEC-002、SEC-003 | TC-004 | 凭证不外泄 |
| FR-010 | API-008、DM-001、DEC-005 | TC-002 | 启停和有效期 |
| FR-002 | ERR-001、ERR-003、ERR-004、SEC-005 | TC-002、TC-003 | 参数、异常和编码冲突 |
| FR-003 | ERR-002 | TC-008 | 越权错误 |
| FR-008 | ERR-005、SEC-004 | TC-007 | 连接失败 |
| FR-002 | MOD-001、MOD-002、MOD-003、DEC-001 | TC-002 | 模块归属和契约 |
| PG-002 | MOD-004、DEC-006、DEC-008 | TC-005 | 页面和出站认证方式 |
| PG-003 | IMP-001、IMP-002 | TC-005、TC-006 | 能力说明同步 |
| PG-005 | IMP-003、IMP-004 | TC-007 | 出站运行时边界 |
| FR-011 | API-009、UI-005、MOD-005、DEC-012 | TC-009 | 微应用总览和引用关系 |
| FR-008 | API-011、DM-001 | TC-002 | 实例启停 |
| FR-002 | API-012 | TC-009 | 连接器定义维护 |
| FR-012 | API-010、SEC-004 | TC-009、TC-010 | 运行单元健康检查 |
| FR-011 | DEC-012 | TC-009 | 微应用不做人工登记 |
| NFR-001 | SEC-001 | TC-008 | 当前部署上下文归属 |
| NFR-002 | SEC-002 | TC-004 | 入站密钥保护 |
| NFR-003 | SEC-003 | TC-004 | 出站凭证保护 |
| NFR-004 | SEC-004、DEC-010 | TC-007 | 出站网络边界 |
| NFR-005 | FLOW-001、FLOW-004、FLOW-005 | TC-001、TC-002 | 事务一致性 |
| NFR-006 | UI-001~UI-004 | TC-005 | 页面状态 |
| DR-001 | DM-001、DB-001 | TC-002 | 集成应用模型 |
| DR-002 | DM-002、DB-002 | TC-001 | 入站字段 |
| DR-003 | DM-003、DB-003 | TC-001 | 出站凭证字段 |
| DR-004 | DM-004、DB-004 | TC-002、TC-007 | 出站配置字段 |
| DR-005 | DB-004 | TC-003 | 白名单和有效期字段 |
| DR-006 | DB-001 | TC-002 | 审计字段 |
| PG-001 | UI-001 | TC-005 | 逻辑应用页面 |
| PG-002 | UI-001 | TC-005 | 集成应用列表 |
| PG-003 | UI-002 | TC-005 | 集成应用表单 |
| PG-004 | UI-003 | TC-004 | 一次性入站凭证 |
| PG-005 | UI-004 | TC-005 | 出站凭证和连接测试 |

## 13. 阶段判定与审批

| 检查项 | 结果 | 证据 |
|---|---|---|
| 技术设计 checker | 待执行 | `node mango-pmo/tools/check-technical-design.mjs --document ...` |
| 专项规范检查计划 | 已列入 | backend、security、module-menu、frontend 检查和测试用例。 |
| 生命周期 handoff | 待完成 | SRS 审批后生成 SHA-256 并回填。 |
| 未关闭阻断数量 | 1 | 生产密钥托管策略待发布前确认。 |
| 例外 | 0 | 无。 |
| Tech Lead 审批 | 待确认 | 当前为研发草案。 |
