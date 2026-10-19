---
documentId: TDD-INFO-COLLECTION-001
documentType: technical-design
pmoVersion: 1.4.7
schemaRevision: 1
riskLevel: L3
riskAssessmentEvidence: "requirement=L3：跨租户公开提交、个人信息和后台处理；solution=L3：新增平台模块、真实数据库、公开入口、服务端 SK 和管理端动态列；final=max(requirement,solution)=L3。"
status: DRAFT
action: WRITE
owner: Mango Tech Lead
approver: 技术负责人（待审批）
approvalEvidence: 用户已明确要求开始研发；正式技术审批尚未补录
upstreamDocumentId: SRS-INFO-COLLECTION-001
upstreamDocumentHash: 2650490f26b349b2f3f6e89da9e740f53245377da4e98ebd19aca3dadbf70525
---

# Mango 信息采集管理技术设计文档

> **先读：这份文档要回答什么**
> - **一句话目标：** 在 Mango 中新增独立信息采集模块，使用真实租户表单和提交记录持久化，提供后台管理、托管提交和服务端提交入口。
> - **真实场景：** 管理员配置一份启用表单；访客通过托管入口提交；运营人员从当前表单进入记录列表，查看动态采集项列并更新处理结果。
> - **关键输入 / 输出：** 输入是表单元数据、采集项 JSON、提交值和处理动作；输出是分页记录、字段快照、处理状态、托管定义和当前表单专属 SK。
> - **成功与失败：** 成功结果必须来自真实数据库和真实权限/租户链路；停用、非法租户、无效字段、失效 SK 或数据库失败不得形成成功记录。
> - **明确不做：** 首版不实现文件上传、多步骤、条件编排、工作流、营销自动化和跨表单提交。
> - **阅读顺序：** 先看决策和模块边界，再看数据、流程、接口、安全和前端映射，最后看测试与追踪矩阵。

## 0. 使用合同与最小填写方式

本设计基于已通过 checker 的 SRS 草案编写。TDD 和 Plan 均保持 `DRAFT/WRITE`，不把 Agent 或用户指令伪装成正式技术审批。

## 1. 设计输入、约束与决策

| 决策ID | 问题 | 候选方案 | 选择 | 理由 | 来源ID或路径 | 是否推断 | 影响 | 风险 | 回退条件 |
|---|---|---|---|---|---|---|---|---|---|
| DEC-001 | 表单和提交记录的模块归属 | CMS 扩展；独立 information 模块 | 独立 `mango-information` 模块 | 需求明确独立于 CMS，不建立站点概念 | BS-007, SC-001 | 否 | 新增 api/core/starter 和前端业务包 | 模块注册和依赖范围扩大 | 首次构建前发现平台模块编号冲突时暂停并重新确认 |
| DEC-002 | 动态采集项如何保存 | 每种控件独立表；表单定义和提交值 JSON | 表单定义 JSON + 提交值 JSON + 快照 JSON | 首版控件可变，避免为每种控件重复建表；快照保证历史可读 | FR-003, DR-002, DR-003 | 否 | Mapper XML 负责 JSON 字段读写，Service 负责校验 | JSON 查询能力有限，导出需在服务层组装 | JSON 字段无法满足字段快照时回到独立明细表设计 |
| DEC-003 | 公开入口和后台入口边界 | 共用后台鉴权；公开入口单独声明 | 后台走授权资源，公开入口按表单状态和提交校验 | 浏览器不携带 SK，服务端决定租户和表单归属 | FR-007, FR-008, FR-009, FR-012 | 否 | 需要公开 Controller 和防滥用校验 | 公开入口暴露面高 | 公开入口无法安全确定表单归属时阻断发布 |
| DEC-004 | 表单专属 SK 保存方式 | 浏览器生成；数据库明文；摘要比对 | 服务端生成随机 SK，保存摘要，后台仅通过服务层读取当前值并支持轮换 | 公开页面不接触 SK；轮换可使旧 SK 失效 | IR-003, NFR-002 | 否 | 需要 SK 轮换和服务端校验 | 现有密钥展示策略需与安全负责人复核 | 安全复核要求不可逆展示时调整为一次性展示 |
| DEC-005 | 管理端页面复用方式 | 新建宿主页面；新增领域 package 注册页面 | 新增 `@mango/information`，通过 `@mango/admin/full` 注册 | 遵循前端 package 边界，单体和微前端复用 | PG-001~005, REDLINE-REUSE-010 | 否 | admin manifest、样式聚合和页面注册同步变更 | 前后端模块需同时接入 | package 构建或样式门禁失败时停止集成 |
| DEC-006 | FE2 领域包如何接入页面注册表 | 依赖 FE3 `@mango/admin-pages`（沿用历史例外）；依赖 FE1 `@mango/admin-extension` | 依赖 FE1 `@mango/admin-extension`（peer + dev） | 架构检查要求 FE2 只能依赖 FE0/FE1/FE2；历史 FE2→admin-pages 例外正在废止，新包不得新增债务 | PG-001, REDLINE-REUSE-010 | 否 | package.json、vite external 和 admin-pages 入口改为 admin-extension | 无 | 平台未发布 admin-extension 时回到 admin-pages 并登记例外 |
| DEC-007 | 托管表单页面的访问身份 | 复用后台登录态；登记为公开路由 | 在宿主应用登记公开路由并加入免登录白名单 | FR-008 要求访客无需登录即可提交；托管页面不应携带后台外壳；分享地址使用绝对地址 | FR-008, FR-009, BT-015 | 否 | 需要宿主应用路由和守卫变更 | 公开路由暴露面增加，需只暴露当前表单定义 | 公开路由被鉴权拒绝时回到登录后访问并登记偏差 |
| DEC-008 | 表单定义与设计器来源 | 自建配置界面；复用平台表单设计器 | 复用 `form-create-designer`（与工作流表单设计器同源），定义以 form-create 规则 JSON 存储 | 自建配置界面缺少拖拽、选中与属性联动，维护成本高；平台已有成熟设计器 | PG-003, BT-004, BT-013, DR-002 | 否 | 前端包新增 form-create 依赖；后端定义解析需兼容规则形状 | 规则结构变化会影响动态列与校验 | 设计器不可用时回退到自建字段表单并登记偏差 |
| DEC-010 | 托管页面如何渲染表单定义 | 用平台通用动态表单组件做类型映射；用设计器同源的 form-create 运行时直接渲染规则 | 托管页面与预览都用 `@form-create/element-ui` 直接渲染规则 | 类型映射会让评分、滑块、上传、子表单等设计器控件退化为文本框，预览与托管页面不一致 | PG-005, FR-008, FR-009, NFR-004 | 否 | 托管页面依赖 form-create 运行时；校验与取值统一由运行时提供 | 设计器新增控件时需确认运行时同样支持 | 运行时无法渲染某控件时在设计器菜单中隐藏该控件并登记偏差 |
| DEC-009 | 提交记录入口 | 表单上下文内页面；全局独立菜单 | 独立菜单入口，按表单筛选，不依赖表单详情 | 产品负责人明确要求独立入口，并去掉返回按钮 | PG-004, BT-006, BT-012 | 否 | 菜单新增一项；提交记录页需要表单筛选器 | 跨表单查询需按所选表单限定范围 | 筛选器缺失时回退到表单详情入口 |

## 2. 模块与依赖边界

| 模块设计ID | 模块或包 | 职责 | 改动类型 | 依赖方向 | 公开能力 | 系统需求ID | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|
| MOD-001 | `mango-information-api` | Command、Query、VO、枚举和 Api 契约 | 新增 | 只依赖 common、validation、swagger | 后台和公开提交契约 | FR-001~012, IR-001~003 | MANGO-ARCH-API-001, MANGO-ARCH-MODEL-002 | API 类型编译和契约测试 |
| MOD-002 | `mango-information-core` | Entity、Mapper、Service、字段规则和 SK 校验 | 新增 | 依赖 api、persistence、context、common | 真实持久化业务能力 | DR-001~005, FR-002~012 | MANGO-ARCH-SVC-003, MANGO-ARCH-MAPPER-005, MANGO-ARCH-ENTITY-003 | 模块集成测试和 migration 检查 |
| MOD-003 | `mango-information-starter` | Controller、自动装配、Resource Registry 菜单和公开资源 | 新增 | 依赖 core、authorization、resource、web、persistence | 后台管理和公开入口 | PG-001~005, FR-001~012 | MANGO-ARCH-CTRL-001, MANGO-ARCH-OPENAPI-002 | Spring 入口测试和资源声明检查 |
| MOD-004 | `@mango/information` | Admin 页面、API factory、页面注册和样式 | 新增 | 依赖 api-schema、common、admin-pages、app-runtime | 表单管理和提交记录页面 | PG-001~005, BT-001~019 | FE API、FE monorepo、Mango UI 规范 | package test/build 和浏览器验收 |

## 3. 技术对象与状态模型

| 模型ID | 上游ID | 模型职责 | 标识 | 关系 | 状态编码 | 审计或历史 | 归属或租户 | 一致性约束 |
|---|---|---|---|---|---|---|---|---|
| DM-001 | DR-001 | 表单属性、定义和当前 SK 摘要 | form id、tenant id、form code | 一个表单拥有多条提交记录 | ENABLED/DISABLED/DELETED | 租户审计字段；定义更新产生更新时间 | TenantEntity，租户由上下文填充 | tenant + code 唯一；有记录时不可删除 |
| DM-002 | DR-003 | 一次提交的固定列、提交值和字段快照 | submission id、form id | 属于一个表单 | SUBMITTED | 原始值和字段快照不可覆盖 | 继承表单租户边界 | 成功提交必须同时保存记录、值和快照 |
| DM-003 | DR-004 | 运营处理状态和备注 | submission id | 一对一处理结果 | PENDING/PROCESSING/COMPLETED/CLOSED | 记录更新时间和处理人 | 与提交记录同租户 | CLOSED 不允许再次处理 |

| 模型ID | 当前状态 | 触发 | 目标状态 | 前置条件 | 副作用 | 失败处理 | 上游ID |
|---|---|---|---|---|---|---|---|
| DM-001 | DISABLED | 启用 | ENABLED | 名称、编码和定义完整 | 接收新提交 | 保持 DISABLED | FR-004, SAC-005 |
| DM-001 | ENABLED | 停用 | DISABLED | 有表单管理权限 | 拒绝新提交，保留历史 | 保持 ENABLED | FR-004, SAC-004 |
| DM-001 | DISABLED | 删除 | DELETED | submission count 为 0 | 删除表单 | 有记录时拒绝 | FR-005, SAC-005 |
| DM-003 | PENDING | 开始处理 | PROCESSING | 有记录处理权限 | 写处理人和时间 | 原状态不变 | FR-011, SAC-003 |
| DM-003 | PROCESSING | 完成 | COMPLETED | 记录未关闭 | 保存备注和处理时间 | 原状态不变 | FR-011, SAC-003 |
| DM-003 | COMPLETED | 关闭 | CLOSED | 有处理权限 | 保存关闭结果 | 原状态不变 | FR-011, SAC-003 |

## 4. 系统流程、事务与一致性

| 流程设计ID | 系统需求ID | 调用入口 | 参与模块 | 处理顺序 | 事务边界 | 状态变化 | 幂等键 | 并发策略 | 外部失败与补偿 | 用户可见结果 |
|---|---|---|---|---|---|---|---|---|---|---|
| FLOW-001 | FR-002, FR-003, FR-004 | 后台表单管理 | MOD-001, MOD-002, MOD-003 | 校验输入→保存表单→保存定义→返回详情 | 表单属性和定义同一写事务 | 表单创建/更新/启停 | 当前租户 + form code | 更新时按 id 和 updatedAt 校验 | 事务回滚，旧定义保留 | 返回保存成功或业务错误 |
| FLOW-002 | FR-008, FR-009, FR-012 | 托管/自建/服务端提交 | MOD-001, MOD-002, MOD-003 | 解析表单→确定租户→校验状态→校验值→写提交和快照 | 提交记录和值快照同一事务 | 生成 SUBMITTED + PENDING | 请求幂等标识（有则复用成功结果） | 同一幂等键唯一约束 | 校验或写入失败整体回滚 | 返回成功记录标识或失败原因 |
| FLOW-003 | FR-010, FR-011 | 当前表单记录页 | MOD-001, MOD-002, MOD-003 | 租户过滤→分页→动态列组装→处理更新 | 查询只读；处理结果单独写事务 | 处理状态变化 | submission id + 当前状态 | 更新条件包含非 CLOSED | 写失败不改变原状态 | 列表和详情刷新 |

## 5. API 与远程契约设计

| 接口ID | 系统需求ID | 调用方 | 所属模块 | 入口类型 | 方法与路径 | Command Query或VO | 返回契约 | 校验 | 权限租户或数据权限 | 幂等分页或排序 | 错误码 | 兼容策略 | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| API-001 | FR-001, FR-002, FR-004, FR-005 | Admin | MOD-003 | HTTP | GET /information/forms/page | InformationFormPageQuery | R<InformationFormPageVO> | keyword、status 分页约束 | 后台资源权限 + 当前租户 | page/size；updatedAt desc | INFORMATION_FORM_NOT_FOUND, INFORMATION_FORM_DELETE_FORBIDDEN | 新增接口，无旧契约 | MANGO-ARCH-HTTP-001, MANGO-ARCH-OPENAPI-004 | API/集成测试 |
| API-002 | FR-006, FR-007 | Admin | MOD-003 | HTTP | GET /information/forms/detail | InformationFormQuery | R<InformationFormDetailVO> | id 正数 | 后台资源权限 + 当前租户 | 无 | INFORMATION_FORM_NOT_FOUND | 新增接口 | MANGO-ARCH-PATH-001 | API/集成测试 |
| API-003 | FR-002, FR-003, FR-004, FR-005 | Admin | MOD-003 | HTTP | POST /information/forms/save | SaveInformationFormCommand | R<String> | Bean Validation + Service Require | 后台资源权限 + 当前租户 | form code 唯一 | INFORMATION_FORM_CODE_EXISTS, INFORMATION_FORM_INVALID | 新增接口 | MANGO-ARCH-API-002 | API/集成测试 |
| API-004 | FR-010, FR-011 | Admin | MOD-003 | HTTP | GET /information/submissions/page | InformationSubmissionPageQuery | R<InformationSubmissionPageVO> | formId、分页和状态约束 | 当前租户 + 表单权限 | page/size | INFORMATION_SUBMISSION_NOT_FOUND | 新增接口 | MANGO-ARCH-HTTP-001 | API/集成测试 |
| API-005 | FR-011 | Admin | MOD-003 | HTTP | PUT /information/submissions/process | ProcessInformationSubmissionCommand | R<Boolean> | 状态和备注约束 | 当前租户 + 记录权限 | CLOSED 禁止更新 | INFORMATION_SUBMISSION_CLOSED | 新增接口 | MANGO-ARCH-API-002 | API/集成测试 |
| API-006 | FR-008, FR-009, FR-012 | Public/Server | MOD-003 | HTTP | POST /information/open/forms/submit | SubmitInformationCommand | R<InformationSubmissionResultVO> | 服务端按定义校验 | 公开表单状态；服务端 SK 仅服务端入口 | 幂等标识；不接受 tenantId | INFORMATION_FORM_DISABLED, INFORMATION_SUBMISSION_INVALID, INFORMATION_SK_INVALID | 新增公开入口 | MANGO-ARCH-CTRL-008 | 公开入口测试 |
| API-007 | FR-008, FR-009 | Public/Server | MOD-003 | HTTP | GET /information/open/forms/detail | InformationPublicFormQuery | R<InformationPublicFormVO> | formCode 非空 | 只返回启用定义 | 无 | INFORMATION_FORM_NOT_FOUND | 新增公开入口 | MANGO-ARCH-OPENAPI-002 | 公开入口测试 |

## 6. 持久化与数据迁移设计

| 数据设计ID | 上游或模型ID | 表或实体 | 字段变化 | 约束 | 索引 | 租户审计 | Mapper边界 | 数据来源 | migration或回填 | 回滚或补偿 | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| DB-001 | DR-001, DM-001 | mango_information_form / InformationFormEntity | name、code、remark、status、definition_json、sk_digest、sk_value、submission_count | tenant_id + code 唯一；状态枚举 | tenant_code、tenant_status | TenantEntity 自动填充 | InformationFormMapper XML 只访问本模块表 | 管理员配置和服务端生成 | V1__init_information_collection.sql | migration 失败停止；不做历史回填 | MANGO-ARCH-ENTITY-003, PERSISTENCE_SCHEMA | migration/集成测试 |
| DB-002 | DR-003, DM-002 | mango_information_submission / InformationSubmissionEntity | form_id、submission_no、source_page、values_json、field_snapshot_json、idempotency_key | tenant_id + submission_no 唯一；幂等键唯一 | tenant_form、tenant_status、tenant_created_at | TenantEntity 自动填充 | Mapper XML 只访问本模块表 | 公开提交入口 | 同一 V1 migration | 写入失败整体回滚 | PERSISTENCE_SCHEMA | 真实数据库测试 |
| DB-003 | DR-004, DM-003 | mango_information_submission_process / InformationSubmissionProcessEntity | submission_id、status、remark、processed_by、processed_at | submission_id 唯一 | tenant_submission_status | TenantEntity 自动填充 | Mapper XML 只访问本模块表 | 运营人员处理 | 同一 V1 migration | 处理失败保持原状态 | PERSISTENCE_SCHEMA | 事务集成测试 |

## 7. 安全、权限、租户与数据边界

| 安全设计ID | 系统需求ID | 能力 | 权限资源 | 默认授权 | 后端校验入口 | 租户边界 | 数据归属断言 | 前端反馈 | 审计 | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| SEC-001 | SA-001, FR-001~007 | 后台表单管理 | information:form:* | 随管理菜单授权 | Controller + Service Require | 当前租户自动过滤 | form.tenantId 等于上下文租户 | 无权页或按钮不可用 | 创建、更新、删除和启停审计 | TenantEntity、MANGO-ARCH-CTRL-002 | 两租户 API 测试 |
| SEC-002 | SA-003, FR-010~011 | 提交记录处理 | information:submission:* | 随表单管理授权 | Service 按 formId 和租户校验 | 当前租户自动过滤 | submission/form/process 同租户 | 无权拒绝且不泄露存在性 | 处理人和时间 | MANGO-ARCH-SVC-003 | 两租户 API 测试 |
| SEC-003 | SA-004, SA-005, FR-008~009 | 公开提交和 SK | information:public-submit | 匿名提交按表单状态 | Public Service 校验 formCode、状态、SK 和值 | 服务端确定表单租户；拒绝 tenantId | 客户端不能覆盖租户归属 | 只返回可识别失败原因 | 记录提交来源和失败类别，不记录 SK | NFR-001, NFR-002 | 浏览器、API 和日志检查 |

## 8. 错误码、异常与可观测性

| 错误设计ID | 系统需求ID | 失败场景 | 触发条件 | 错误码 | 异常类型 | 用户反馈 | 日志上下文 | 指标或告警 | 重试或补偿 | 敏感信息处理 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| ERR-001 | FR-002, FR-003 | 表单定义不合法 | 名称、编码或采集项不满足规则 | INFORMATION_FORM_INVALID | Mango 业务异常 | 标出具体错误 | form code、tenant context | 校验失败计数 | 修正后重试 | 不记录提交值全文 |
| ERR-002 | FR-004, FR-008 | 表单停用 | 新提交时状态为 DISABLED | INFORMATION_FORM_DISABLED | Mango 业务异常 | 表单暂不可提交 | form code、source | 停止重试 | 不写提交记录 | 不记录个人信息 |
| ERR-003 | FR-005 | 禁止删除 | 表单已有提交记录 | INFORMATION_FORM_DELETE_FORBIDDEN | Mango 业务异常 | 只能停用 | form id、count | 删除拒绝计数 | 无 | 不记录提交值 |
| ERR-004 | FR-008, FR-009 | 提交值校验失败 | 必填、格式或选项不合法 | INFORMATION_SUBMISSION_INVALID | Mango 业务异常 | 返回字段错误 | form code、field codes | 校验失败计数 | 修正后重试 | 日志不记录个人值 |
| ERR-005 | FR-007, FR-009 | SK 无效 | 缺失、失效或不匹配 | INFORMATION_SK_INVALID | Mango 业务异常 | 拒绝服务端提交 | form code、request id | SK 失败计数 | 重新生成 SK | 禁止记录 SK |

## 9. 前端结构与交互实现映射

| 前端设计ID | 系统需求ID | 页面或动作 | 页面key或路由 | 区域与组件 | 状态来源 | API依赖 | 权限或不可操作 | 空加载或失败态 | 语义测试锚点 | 复用判断 | 适用规范ruleId |
|---|---|---|---|---|---|---|---|---|---|---|---|
| UI-001 | PG-001, BT-001~008 | 表单列表 | information/forms/index | MangoListPage、MangoSearchPanel、MangoListPanel、el-table | API-001 | API-001~005 | 菜单和按钮权限 | 空、加载、错误、无权 | data-page=information.forms、data-action=information.form.create | 复用现有 CMS 列表骨架，不复制业务实现 | FE-API、FE-TEST |
| UI-002 | PG-002, BT-003, BT-005, BT-006, BT-009~012 | 表单详情 | information/forms/detail | el-card、el-descriptions、el-table、el-drawer | API-002 | API-002 | 当前表单权限 | 加载、错误、无权 | data-page=information.form.detail | 页面私有结构 | FE-VUE |
| UI-003 | PG-003, BT-004, BT-010, BT-013~014 | 表单设计 | information/forms/config | form-create-designer、MangoPageBackBar、el-form、el-card | API-002/003 | API-002/003 | 编辑权限 | 校验、保存中、错误 | data-page=information.form.config、data-surface=information.form.designer、data-action=information.form.save | 复用平台表单设计器，不自建拖拽与属性面板 | FE-VUE |
| UI-004 | PG-004, BT-006, BT-012, BT-015~018 | 提交记录 | information/submissions/index | MangoSearchPanel、MangoListPanel、el-table、el-drawer、Pagination | API-004 | API-004/005 | 当前表单记录权限 | 空、加载、错误、无权 | data-page=information.form.submissions、data-filter=form、data-action=information.submission.detail | 复用现有列表骨架，动态列由 API 字段定义驱动 | FE-API、FE-TEST |
| UI-005 | PG-005, BT-019 | 托管表单 | information/open/forms/detail | el-form、动态控件、成功结果 | API-007 | API-006/007 | 仅启用表单可提交 | 停用、校验失败、提交失败 | data-page=information.hosted-form、data-action=information.hosted.submit | 页面私有结构，不携带 SK | FE-API、FE-TEST |

## 10. 测试设计与验收映射

| 测试用例ID | 系统验收ID | 设计项ID | 场景 | 优先级 | 测试层级 | 自动化判断 | 测试数据 | 权限或租户边界 | 稳定契约 | 执行入口 | 证据 | 失败处理 | 适用规范ruleId |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| TC-001 | SAC-001 | API-001, API-003, DB-001, UI-001, UI-003 | 创建表单并保存定义 | P0 | 集成/API/UI | AUTO | 专用测试租户 + `IT_INFO_001` | 管理员 | API-001~003 | 后端集成测试、Playwright | evidence/issue-1055 | 阻断后端和页面验收 | backend-test、frontend-test |
| TC-002 | SAC-002 | FLOW-002, API-006, DB-002, UI-004 | 两份不同定义的表单提交和动态列 | P0 | 入口流程/API/UI | AUTO | 两份表单、不同字段快照 | 两租户 | API-004/006 | API 流程 + Playwright | evidence/issue-1055 | 记录错列则阻断 | backend-test、frontend-test |
| TC-003 | SAC-003 | DM-003, API-004, API-005, UI-004 | 查询、详情、处理状态和备注 | P0 | 集成/API/UI | AUTO | 一条测试提交记录 | 当前租户授权账号 | API-004/005 | 后端 flow + Playwright | evidence/issue-1055 | 状态不一致则阻断 | backend-test、frontend-test |
| TC-004 | SAC-004 | FLOW-002, API-006, ERR-002, ERR-004 | 停用、缺失和非法值提交失败 | P0 | API/入口流程 | AUTO | 停用表单、非法值 | 匿名和服务端入口 | API-006 | 公开入口测试 | evidence/issue-1055 | 生成成功记录则阻断 | backend-test |
| TC-005 | SAC-005 | DM-001, API-001, API-003, DB-001 | 删除无记录表单，拒绝删除有记录表单 | P1 | 集成/API | AUTO | 两份测试表单 | 当前租户 | API-001/003 | 后端集成测试 | evidence/issue-1055 | 删除边界失败阻断 | backend-test |
| TC-006 | SAC-006 | DB-002, UI-004 | 修改定义后查看历史字段快照 | P0 | 集成/API/UI | AUTO | 修改字段标签和顺序 | 当前租户 | API-004 | 数据断言和截图 | evidence/issue-1055 | 历史错列阻断 | backend-test、frontend-test |
| TC-007 | SAC-007, SAC-009 | SEC-001~003, ERR-005, UI-005 | 跨租户拒绝和 SK 不暴露 | P0 | 入口流程/API/UI | AUTO | 两租户、两账号、一个 SK | 交叉租户和匿名浏览器 | data-page/data-action | API 测试、Playwright 网络检查 | evidence/issue-1055 | 任何泄露阻断 | backend-test、frontend-test |
| TC-008 | SAC-010~012 | UI-001~005, API-004 | 导出、空/错误状态、返回路径和截图 | P1 | UI/E2E/手工 | MANUAL | 正常、空和错误数据 | 管理员和无权账号 | 语义 data-* 锚点 | Playwright + 人工验收 | evidence/issue-1055 | 补截图并修复后重测 | frontend-test |

## 11. 兼容与已启用能力说明影响

| 影响ID | 设计项ID | 影响对象 | 当前行为 | 目标行为 | 兼容策略 | 升级或补偿 | 已启用能力说明 | 验证 | 责任人 |
|---|---|---|---|---|---|---|---|---|---|
| IMP-001 | MOD-001~004 | Mango 平台模块和 Admin full | 没有信息采集模块 | 新模块以平台 starter 和前端 package 方式接入 | 不修改 CMS 数据和接口 | 新增 Maven/前端模块依赖及菜单资源 | 增加模块 README 和前端 package README | 模块构建、样式检查和页面验收 | Tech Lead |

## 12. 技术追踪矩阵

| 上游ID | 设计项ID | 测试用例ID | 覆盖说明 |
|---|---|---|---|
| FR-001 | MOD-003, API-001, UI-001 | TC-001, TC-008 | 表单列表和查询。 |
| FR-002 | FLOW-001, API-003, DB-001, UI-003 | TC-001 | 新增表单。 |
| FR-003 | DEC-002, API-003, DB-001, UI-003 | TC-001, TC-006 | 表单定义。 |
| FR-004 | DM-001, API-003, UI-001 | TC-005 | 启停。 |
| FR-005 | DM-001, API-003, DB-001 | TC-005 | 删除限制。 |
| FR-006 | API-002, UI-002 | TC-003 | 详情。 |
| FR-007 | DEC-004, API-002, SEC-003, UI-002 | TC-007 | 接入和 SK。 |
| FR-008 | FLOW-002, API-006, UI-005 | TC-002, TC-004, TC-007 | 托管提交。 |
| FR-009 | FLOW-002, API-006 | TC-002, TC-004, TC-007 | 自建/服务端提交。 |
| FR-010 | API-004, UI-004 | TC-002, TC-003, TC-008 | 动态列表。 |
| FR-011 | DM-003, API-005, UI-004 | TC-003 | 处理结果。 |
| FR-012 | SEC-001~003, ERR-005 | TC-007 | 租户和公开安全。 |
| PG-001 | UI-001 | TC-001, TC-008 | 表单列表页面。 |
| PG-002 | UI-002 | TC-003, TC-007 | 表单详情页面。 |
| PG-003 | UI-003 | TC-001, TC-006 | 配置页面。 |
| PG-004 | UI-004 | TC-002, TC-003, TC-006, TC-008 | 提交记录页面。 |
| PG-005 | UI-005 | TC-004, TC-007 | 托管表单页面。 |
| DR-001 | DM-001, DB-001 | TC-001, TC-005 | 表单数据。 |
| DR-002 | DEC-002, DB-001, DB-002 | TC-001, TC-006 | 定义和快照。 |
| DR-003 | DM-002, DB-002 | TC-002, TC-004 | 提交记录。 |
| DR-004 | DM-003, DB-003 | TC-003 | 处理结果。 |
| DR-005 | SEC-001~003 | TC-007 | 租户边界。 |
| IR-001 | API-006, UI-005 | TC-004, TC-007 | 托管入口。 |
| IR-002 | API-006 | TC-002, TC-004 | 自建入口。 |
| IR-003 | DEC-004, API-006, SEC-003 | TC-007 | 服务端 SK。 |
| NFR-001 | SEC-001~003 | TC-007 | 租户隔离。 |
| NFR-002 | DEC-004, SEC-003 | TC-007 | SK 不暴露。 |
| NFR-003 | FLOW-002, ERR-002, ERR-004 | TC-004 | 失败一致性。 |
| NFR-004 | DEC-002, DB-002, UI-004 | TC-002, TC-006 | 动态列和历史快照。 |
| NFR-005 | ERR-001~005, UI-001~005, DEC-006, DEC-007, DEC-008, DEC-009, DEC-010 | TC-008 | 失败反馈，含 FE2 包边界、托管表单匿名路由、表单设计器来源、提交记录入口和托管渲染来源决策。 |
| SAC-001 | API-001~003, UI-001, UI-003 | TC-001 | 创建配置验收。 |
| SAC-002 | FLOW-002, API-006, API-004 | TC-002 | 多表单动态列。 |
| SAC-003 | DM-003, API-004~005, UI-004 | TC-003 | 记录处理。 |
| SAC-004 | API-006, ERR-002, ERR-004 | TC-004 | 失败提交。 |
| SAC-005 | DM-001, API-003 | TC-005 | 删除停用。 |
| SAC-006 | DB-002, UI-004 | TC-006 | 历史快照。 |
| SAC-007 | SEC-001~003 | TC-007 | 跨租户拒绝。 |
| SAC-008 | API-006, UI-005 | TC-002, TC-004 | 双入口。 |
| SAC-009 | DEC-004, SEC-003, ERR-005 | TC-007 | SK 安全。 |
| SAC-010 | UI-004 | TC-008 | 导出。 |
| SAC-011 | UI-001~005 | TC-008 | 页面状态。 |
| SAC-012 | UI-001~005 | TC-008 | 返回交互。 |
| FR-001 | DEC-001 | TC-001 | 独立模块决策覆盖。 |
| FR-002 | DEC-003 | TC-001 | 公开入口边界决策覆盖。 |
| FR-003 | DEC-005 | TC-001 | 前端 package 决策覆盖。 |
| FR-004 | MOD-001 | TC-001 | API 模块边界覆盖。 |
| FR-005 | MOD-002 | TC-001 | Core 模块边界覆盖。 |
| FR-006 | MOD-004 | TC-008 | 前端包边界覆盖。 |
| FR-007 | FLOW-003 | TC-003 | 提交处理流程覆盖。 |
| FR-008 | API-007 | TC-002, TC-004 | 公开定义查询覆盖。 |
| FR-009 | SEC-002 | TC-007 | 后台提交安全覆盖。 |
| FR-010 | ERR-003 | TC-005 | 删除错误契约覆盖。 |
| FR-011 | IMP-001 | TC-008 | 能力说明影响覆盖。 |

## 13. 阶段判定与审批

| 检查项 | 结果 | 证据 |
|---|---|---|
| 技术设计 checker | 待执行 | `node mango-pmo/tools/check-technical-design.mjs --document mango-docs/designs/issue-1055-information-collection/technical-design.md` |
| 生命周期 handoff | 待执行 | BRD/SRS 审批证据和 TDD hash 校验待执行。 |
| 专项规范检查计划 | WRITE | Maven 模块架构、Flyway 表归属、前端 boundaries、Admin 样式检查和真实入口测试列入 Plan。 |
| 未关闭阻断数量 | 0 | 当前无 Agent 设计阻断；正式技术审批仍未完成。 |
| Tech Lead 审批 | 待技术负责人审批 | 用户已授权开始研发，不等同于 Tech Lead 签批。 |
