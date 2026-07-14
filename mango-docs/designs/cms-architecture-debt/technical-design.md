---
documentId: TDD-CMS-DEBT
documentType: technical-design
pmoVersion: 1.2.1
schemaRevision: 1
riskLevel: L3
riskAssessmentEvidence: requirement=L3，后台内容状态、公开读取、租户和初始化属于核心链路；solution=L3，一次性调整四层契约、领域服务、错误边界、持久化初始化和演示资源；final=max(requirement,solution)
status: DRAFT
action: WRITE
owner: Mango CMS Tech Lead
approver: HardyDou
approvalEvidence: review/PROPOSAL-CMS-DEBT.md
upstreamDocumentId: SRS-CMS-DEBT
upstreamDocumentHash: 1915ef3a572fa40bd741ac271339f66cee6964a9d6a1150a060bd0bda8af3740
---

# CMS 历史债务治理技术设计文档

## 1. 设计输入、约束与决策

| 决策ID | 问题 | 候选方案 | 选择 | 理由 | 来源ID或路径 | 是否推断 | 影响 | 风险 | 回退条件 |
|---|---|---|---|---|---|---|---|---|---|
| DEC-001 | CMS 四个子模块累计 1,126 条架构债务，问题在 API、Service 和 Controller 间级联 | 只修门禁；数据与代码分批；一次性最终态治理 | 一个任务分支、一个 PR，内部按基线、契约、领域服务、数据和验证设置检查点 | 用户批准与 Payment 相同的一次性策略，避免保留第二套中间边界 | 用户批准；`mango-pmo/baselines/architecture/debt-budget.json` | 否 | CMS API、Core、Starter、Starter Remote、文档和测试 | 改动面大 | 任一契约、业务不变量或初始化结果无法保持时停止，不提交半迁移状态 |
| DEC-002 | 现有测试只有 14 个方法，不能覆盖站点、内容、审核、发布和公开读取 | 直接改后补测；只测编译；先补特征测试并建立 before 基线 | 在生产代码未变化时补 UNIT/API/初始化和定向 UI 用例，记录 before，改造后运行相同入口 | 同一测试集合的前后结果才能证明行为保持 | SAC-001 至 SAC-005；现有测试目录 | 否 | CMS 测试资产与证据基线 | 可能暴露既有真实缺陷 | 不弱化断言；既有缺陷单独记录并依据用户范围处理 |
| DEC-003 | Core Service 返回 `R`、直接构造结果且使用裸消息，造成 339 条 Core 债务 | 放宽门禁；保留旧 Service 增加适配层；直接规范化 | Service 返回 VO、PageResult、ID、Boolean 或文件结果；Controller 统一 `R.ok(...)`；新增 `CmsCode implements BizCode`，所有枚举项保持既有 HTTP 业务码 400 和原消息 | 保持外部 code/message，同时落实 Service 与 Controller 职责 | FR-001 至 FR-004；MANGO-ARCH-SVC/CTRL | 否 | Core、Starter、异常测试 | 错误消息遗漏或 code 改变 | 错误目录快照或接口失败响应存在差异时停止 |
| DEC-004 | `CmsAdminService` 1590 行且承担 11 个聚合，`CmsSiteService` 686 行混合解析、查询、文件授权与转换 | 保留巨型类；增加一个代理层；按业务聚合拆分 | 管理能力拆成内容分类、标签、站点、站点栏目、内容、发布、导航、Banner、广告位、广告投放和站点设置服务；公开能力保留 `ICmsSiteService`，将解析、公开文件策略和转换拆成明确协作者 | 降低修改耦合，不改变 `CmsAdminApi`/`CmsSiteApi` 公开契约 | FR-001 至 FR-004；现有两个实现类 | 否 | Core Service、Starter Controller | 拆分时事务或调用顺序可能遗漏 | 同一特征测试任一状态、查询条件或副作用差异即修正后再继续 |
| DEC-005 | CMS 多数操作名称类似 CRUD，但公共契约使用同一 `Save...Command` 同时承担创建/更新，删除使用简单 ID，并包含数据权限、关联保护和状态动作 | 强制转换为 canonical CRUD；继续一个巨型 Service；使用领域服务显式方法 | 只有精确满足 `MangoTypedCrudService` 六类泛型和输入契约的聚合才使用 canonical CRUD；当前不为继承基类而改变公共输入或暴露通用方法，拆分后的服务保留聚合语义方法 | `MangoCrudServiceImpl` 的反射复制和通用删除不能自动证明 CMS 归属、关联及状态语义；接口保持是本次硬约束 | NFR-001；`rules/backend/03-api.md`；Mango CRUD 实现 | 否 | Core 内部 Service | 可能被误认为未统一 CRUD | 架构门禁与评审证明无 canonical CRUD 违规；若某聚合后续形成标准 C/U/Q 契约再单独迁移 |
| DEC-006 | API 有 487 条字段文档和输入约束债务，Starter 有 300 条适配、OpenAPI 和返回债务 | 修改公共字段或路径；只补最少注解；完整补齐同一契约 | 不改变方法、路径、HTTP verb、参数名、字段名和返回泛型；补齐字段 `@Schema`/校验、Controller `@Operation`/`@Parameter`/`@Valid` 并直接包装对应领域服务调用 | 修复静态契约而不改变消费者可见协议 | FR-001 至 FR-004；NFR-001；MANGO-ARCH-MODEL/CTRL/OPENAPI/ADAPTER | 否 | API、Starter、前端现有调用 | 注解或绑定遗漏导致文档或运行时差异 | API 反射快照、MockMvc 和前端定向消费任一差异时停止 |
| DEC-007 | Flyway V1-V10 混合结构、菜单修正、跨模块文件引用和演示站点数据 | 保留历史链；机械合并全部 SQL；纯 DDL 最终 V1 加资源登记 | 因模块只支持新数据库，以当前最终结构生成一个纯 DDL V1；删除 V2-V10；菜单继续正式 Resource Registry；演示数据迁入模块 `META-INF/mango/demo/` 并由类型化 CMS handler 落库 | 与 Payment 最终态政策一致，消除跨模块 Flyway DML 和默认演示污染 | FR-005, FR-006；用户批准；现有 migration/resources | 否 | Core migration、Starter resources、初始化行为 | 最终 schema、演示依赖或条数可能遗漏 | 新旧最终 schema 指纹、默认/演示资源清单或空库启动任一不一致时停止 |
| DEC-008 | Remote Starter 当前为空，但 README 宣称可远程消费 | 在本次补齐；删除模块；保持现状并明确不新增功能 | 不在本次增加 Feign 实现或新远程能力；公开 Java/HTTP 契约保持，README 仅更新数据初始化和验证说明 | 补齐空模块属于新增功能，超出“行为不变”的债务治理范围 | BS-003；现有 Remote Starter | 否 | Remote Starter 不改生产代码 | 使用者可能继续误解能力 | 不声明新增远程行为；后续如需要按独立能力需求处理 |
| DEC-009 | 公开文件预览是二进制流，当前作为 `CmsSiteController` 的 API 外方法存在，无法满足统一 `R<T>` Controller 契约 | 改成 JSON 下载地址；为架构规则增加例外；迁入函数式二进制 endpoint | 保持 `GET /cms/open/files/public-preview`、query、Content-Type、长度和 Content-Disposition 不变，将协议流适配迁入专用函数式 endpoint，继续调用 `ICmsSiteService.publicFile` 执行业务授权 | 不改变浏览器和站点调用行为，同时避免让标准业务 Controller 暴露 API 外方法或非 R 返回 | FR-004；MANGO-ARCH-CTRL-005、HTTP-001 | 否 | Starter 文件公开入口 | 函数式绑定或响应头可能不同 | 固定请求/响应契约测试任一 verb、path、header、长度或拒绝语义差异即停止 |

## 2. 模块与依赖边界

| 模块设计ID | 模块或包 | 职责 | 改动类型 | 依赖方向 | 公开能力 | 系统需求ID | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|
| MOD-001 | `mango-cms-api` | 传输无关 API、Command、Query、VO、枚举和 CmsCode | 字段文档、校验和错误契约规范化 | 只依赖公共 API，不依赖 core/starter | `CmsAdminApi`、`CmsSiteApi` | FR-001 至 FR-004 | MANGO-ARCH-API/HTTP/MODEL/TYPE-009/010 | API 模型检查、契约快照、架构门禁 |
| MOD-002 | `mango-cms-core` | 领域服务、状态、租户、实体、Mapper、转换和纯 DDL | 拆分巨型 Service、去除 R、Require 错误码、V1 | 依赖本域 api、infra API 和 file API，不依赖 starter | CMS 内部实现 | FR-001 至 FR-005 | MANGO-ARCH-SVC/BEAN/MAPPER/ENTITY/TYPE/DEP-002 | UNIT、数据库集成、架构门禁、schema 对比 |
| MOD-003 | `mango-cms-starter` | Controller、自动装配、公开路径和正式/demo 资源 handler | HTTP 适配与资源登记规范化 | 依赖本域 core 和明确 starter | `/cms`、`/cms/open` | FR-001 至 FR-006 | MANGO-ARCH-CTRL/ADAPTER/BEAN/DEP-007 | MockMvc、装配、资源初始化和启动验证 |
| MOD-004 | `mango-cms-starter-remote` | 保留现有空远程扩展位 | 不新增生产实现 | 继续只依赖本域 api 与 infra-feign | 无新增公开行为 | NFR-001 | MANGO-ARCH-FEIGN/DEP-003/004 | 编译和零新增债务 |
| MOD-005 | `mango-docs/designs/cms-architecture-debt` 与 CMS README | 设计、计划、验证基线和使用说明 | 新增/更新 | 引用 PMO 规范与模块事实 | 开发者交付说明 | FR-005, FR-006, NFR-001, NFR-003 | `rules/06-document-assets.md` | 文档合同和能力文档检查 |

## 3. 技术对象与状态模型

| 模型ID | 上游ID | 模型职责 | 标识 | 关系 | 状态编码 | 审计或历史 | 归属或租户 | 一致性约束 |
|---|---|---|---|---|---|---|---|---|
| DM-001 | DR-001, FR-001 | CMS 管理聚合 | 既有 Long 主键与业务编码 | 站点关联栏目、导航、Banner、广告、设置；内容关联分类、标签和发布 | 保持 `CmsStatus`、`CmsContentStatus`、`CmsPublishStatus` 等既有值 | 保持现有审计字段和逻辑删除 | `TenantEntity.tenantId` 与数据权限条件 | 唯一性、关联、删除保护和分页排序不变 |
| DM-002 | DR-002, FR-002, FR-003, FR-004 | 内容公开资格 | 内容、站点、栏目和发布关系主键 | 内容状态与发布关系、有效期、站点和文件引用共同决定公开 | 保持 DRAFT、PENDING_REVIEW、PUBLISHED、REJECTED、OFFLINE 等编码 | 保持发布时间、下线时间和审核意见 | 站点与内容租户必须一致 | 任一公开条件不满足均不可返回或下载 |
| DM-003 | DR-003, FR-005, FR-006 | 初始化资源 | schema 版本、资源 type/version/bizKey | 正式菜单独立；Demo 按站点→设置/栏目→导航/Banner/内容/广告→发布/投放依赖 | 正式默认启用，Demo 显式启用 | Resource Registry 记录登记和应用结果 | Demo 使用明确租户/组织归属 | 默认启动无 Demo；显式启用后对象和引用完整且幂等 |

| 模型ID | 当前状态 | 触发 | 目标状态 | 前置条件 | 副作用 | 失败处理 | 上游ID |
|---|---|---|---|---|---|---|---|
| DM-001 | 巨型管理实现 | 内部职责迁移 | 按聚合分离且契约保持的领域服务 | before 基线已记录 | 类和内部调用关系变化，数据库结果不变 | 任一用例差异即修复，不保留双实现 | SAC-001, SAC-002 |
| DM-002 | 当前公开读取实现 | 职责拆分与错误规范化 | 解析、读取、文件策略和转换边界清晰 | 公开资格特征测试已建立 | 内部调用变化，公开结果不变 | 泄露或拒绝差异立即停止 | SAC-003, SAC-004 |
| DM-003 | V1-V10 混合 DDL/DML | 新空库初始化 | 纯 DDL V1 加正式/demo 资源 | 当前最终 schema 与 Demo 清单已固化 | Flyway history 重置，只支持全新库 | 丢弃测试库并恢复设计，不支持旧 history 升级 | SAC-005 |

## 4. 系统流程、事务与一致性

| 流程设计ID | 系统需求ID | 调用入口 | 参与模块 | 处理顺序 | 事务边界 | 状态变化 | 幂等键 | 并发策略 | 外部失败与补偿 | 用户可见结果 |
|---|---|---|---|---|---|---|---|---|---|---|
| FLOW-001 | FR-001 至 FR-006 | CMS 测试套件 | MOD-001 至 MOD-005 | 补有效用例→生产代码未变时运行 before→治理→相同入口运行 after→对比报告 | 用例按目标使用无数据库、事务回滚或独立新库 | 只记录结果 | 每个用例使用独立数据标识 | 用例不依赖执行顺序 | 文件能力可用受控替身，CMS 数据与 HTTP 链路真实执行 | 前后业务不变量逐项一致 |
| FLOW-002 | FR-001, FR-002 | `/cms` 管理入口 | MOD-001, MOD-002, MOD-003 | 校验输入和权限→领域前置条件→Mapper 读写→状态/关联更新→转换返回 | 每个写动作保持既有 `@Transactional` 边界 | 按既有内容和发布状态流转 | 既有唯一键及业务编码 | 继续依赖唯一约束和状态前置条件 | 文件校验失败时不写业务数据；事务失败回滚 | 成功、错误码、消息、状态和数据范围不变 |
| FLOW-003 | FR-003, FR-004 | `/cms/open` 公开入口 | MOD-001, MOD-002, MOD-003 | 解析站点→切换目标租户上下文→筛选公开且有效对象→校验文件引用→转换返回 | 只读查询无新增写事务 | 不改变状态 | 不适用 | 请求上下文 finally 恢复，禁止跨请求泄露 | 文件不存在或无授权时拒绝，不降级为任意文件读取 | 只显示目标站点安全公开信息 |
| FLOW-004 | FR-005, FR-006 | Flyway 与 Resource Registry | MOD-002, MOD-003 | 执行纯 DDL V1→登记正式菜单→根据开关决定是否读取 Demo→按依赖类型幂等落库→启动服务 | DDL 与各资源 handler 使用平台既有事务 | 形成正式空白或演示内容集合 | type/version/bizKey 与业务唯一键 | handler 使用幂等 upsert/存在性判断 | 任一依赖失败停止，不吞异常，不形成半套结果 | 环境用途明确且可重复初始化 |

## 5. API 与远程契约设计

| 接口ID | 系统需求ID | 调用方 | 所属模块 | 入口类型 | 方法与路径 | Command Query或VO | 返回契约 | 校验 | 权限租户或数据权限 | 幂等分页或排序 | 错误码 | 兼容策略 | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| API-001 | FR-001 | CMS 管理前端与 Java 消费者 | MOD-001, MOD-003 | 后台管理查询代表；完整目录由契约快照逐项锁定 | GET /cms/sites/detail | `Long id` 与 `CmsSiteVO` | R<CmsSiteVO> | API Bean Validation；Service `Require + CmsCode` | 保持 `cms:site:query`、租户和 DataScope | 对象级查询结果不变 | 既有 HTTP 业务码 400 和消息不变 | 不改变参数名、字段或方法签名 | MANGO-ARCH-API/HTTP/MODEL/CTRL/ADAPTER/OPENAPI | 反射快照、MockMvc、权限和失败响应测试 |
| API-002 | FR-002 | CMS 管理前端与 Java 消费者 | MOD-001, MOD-003 | 内容审核代表；完整目录由契约快照逐项锁定 | POST /cms/contents/approve | `UpdateCmsContentReviewCommand` | R<Boolean> | required JSON body、`@Valid` 与 Service `Require + CmsCode` | 保持 `cms:content:approve`、租户、归属和状态边界 | 重复和非法状态语义不变 | 既有 HTTP 业务码 400 和消息不变 | 不改变 body 字段或返回 | MANGO-ARCH-API/HTTP/MODEL/CTRL/ADAPTER/OPENAPI | MockMvc、状态机和失败响应测试 |
| API-003 | FR-003 | 站点前端与访客 | MOD-001, MOD-003 | 公开站点解析代表；完整目录由契约快照逐项锁定 | GET /cms/open/sites/resolve | `SiteResolveQuery` 与 `SiteResolveVO` | R<SiteResolveVO> | Query 约束和匿名域名边界 | 保持 PUBLIC、域名解析和目标租户上下文 | 解析唯一性不变 | 既有 HTTP 业务码 400 和消息不变 | 不改变 site-shell 调用契约 | MANGO-ARCH-API/HTTP/MODEL/CTRL/ADAPTER/OPENAPI | 双租户域名解析和上下文恢复测试 |
| API-004 | FR-003 | 站点前端与访客 | MOD-001, MOD-003 | 公开内容详情代表；完整目录由契约快照逐项锁定 | GET /cms/open/contents/detail | `SiteContentDetailQuery` 与 `SiteContentVO` | R<SiteContentVO> | Query 约束、内容和发布资格 | 保持 PUBLIC、站点、租户、发布状态和有效期 | 详情公开结果不变 | 既有 HTTP 业务码 400 和消息不变 | 不改变 site-shell 调用契约 | MANGO-ARCH-API/HTTP/MODEL/CTRL/ADAPTER/OPENAPI | 多状态、多有效期和跨站点测试 |

## 6. 持久化与数据迁移设计

| 数据设计ID | 上游或模型ID | 表或实体 | 字段变化 | 约束 | 索引 | 租户审计 | Mapper边界 | 数据来源 | migration或回填 | 回滚或补偿 | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| DB-001 | DM-001, DM-002, DR-001, DR-002 | 现有 13 张 `cms_*` 表及 Entity | 不改变当前 V1-V10 最终字段，仅在新 V1 表达最终结构 | 主键、唯一约束、逻辑删除和关联语义保持 | 当前最终索引名称、列顺序和唯一性保持 | String tenantId、orgId、created/updated 字段保持 | Mapper 只使用 Entity、id、Wrapper、Page 和内部 Row/Criteria | 空数据库 | 生成纯 DDL `V1__init_mango_cms.sql`，无 INSERT/UPDATE/DELETE/跨模块表访问 | 仅支持新库；失败丢弃测试库并修正 V1 | `rules/backend/04-db.md`、MANGO-ARCH-MAPPER/ENTITY/PERSISTENCE_SCHEMA | 旧链最终 schema 与新 V1 指纹、Flyway migrate 和读写验证 |
| DB-002 | DM-003, DR-003 | 正式菜单与 CMS Demo 对象 | 业务表无新增字段 | type/version/bizKey、业务唯一键和依赖键稳定 | 复用业务表现有索引 | Demo 显式 tenantId/orgId；handler 填充审计 | 类型化 handler 调用本域 Mapper，不访问其它模块表 | `META-INF/mango/resources/cms-common-menu.json` 与 `META-INF/mango/demo/cms-demo-*.json` | 由 Resource Registry 默认/显式登记，不做 Flyway 回填 | handler 事务失败停止；禁用 Demo 后新库不加载，已加载数据不承诺自动删除 | Resource Registry 使用说明与 `rules/backend/04-db.md` | 默认/显式初始化集成测试、条数/依赖/幂等断言 |

## 7. 安全、权限、租户与数据边界

| 安全设计ID | 系统需求ID | 能力 | 权限资源 | 默认授权 | 后端校验入口 | 租户边界 | 数据归属断言 | 前端反馈 | 审计 | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| SEC-001 | FR-001, FR-002 | CMS 后台管理 | 保持现有 `cms:*` 资源 | 保持 `cms-common-menu.json` 套餐授权 | Controller `@ApiAccess` 加 Service 归属/状态 `Require` | MangoContext tenantId 与 Entity tenantId 一致 | 查询应用 DataScope；详情/更新/删除先按租户和数据范围取对象 | 保持现有权限或业务原因 | 保持审计字段 | 授权、租户、数据权限规范 | 双租户、无权限、对象级数据权限测试 |
| SEC-002 | FR-003 | CMS 公开读取 | PUBLIC，仅 `/cms/open/**` | 无需登录但不等于无限数据访问 | `CmsSiteController` 与公开 Service 资格判断 | 匿名只能按域名解析；目标租户上下文仅在请求范围内生效并恢复 | 所有下游查询绑定解析站点 tenantId/siteId | 空、不存在或不可访问，不泄露内部状态 | 公开读取不新增审计写入 | 授权、上下文和租户规范 | 匿名 siteCode 拒绝、域名解析、跨租户和上下文恢复测试 |
| SEC-003 | FR-004 | 公开文件读取 | PUBLIC 入口加业务引用授权 | 不默认公开任意文件 | `CmsPublicFilePolicy` 校验站点 Logo、Banner、已发布内容或有效广告引用 | 文件引用对象与目标站点租户一致 | 文件 ID 必须可在公开对象引用关系中证明 | 无授权时不可访问 | 不记录文件敏感元数据 | 文件引用规则与授权规范 | 被引用/未引用/未发布/跨站点文件矩阵测试 |

## 8. 错误码、异常与可观测性

| 错误设计ID | 系统需求ID | 失败场景 | 触发条件 | 错误码 | 异常类型 | 用户反馈 | 日志上下文 | 指标或告警 | 重试或补偿 | 敏感信息处理 |
|---|---|---|---|---|---|---|---|---|---|---|
| ERR-001 | FR-001 至 FR-006 | 输入、对象存在性、唯一性、关联、状态、归属、公开资格、文件和初始化失败 | 任一业务前置条件不满足或资源处理失败 | `CmsCode` 各项 code 均保持 400，message 保持当前字符串；初始化系统异常继续统一 500 | `BizException` 或明确基础设施异常 | 与治理前一致的业务消息；初始化失败显示具体阶段 | module、operation、tenantId、siteId、objectId、resourceType，不记录正文、密钥或文件内容 | 本次不新增指标；保留应用错误日志 | 业务失败不重试且事务回滚；幂等初始化可修正后重跑新库 | 日志不输出内容正文、令牌、文件内容或隐私信息 |

## 9. 前端结构与交互实现映射

| 前端设计ID | 系统需求ID | 页面或动作 | 页面key或路由 | 区域与组件 | 状态来源 | API依赖 | 权限或不可操作 | 空加载或失败态 | 语义测试锚点 | 复用判断 | 适用规范ruleId |
|---|---|---|---|---|---|---|---|---|---|---|---|
| UI-001 | PG-001, PG-002, BT-001, BT-002, BT-003, FR-001 至 FR-004 | 现有 CMS 管理页面与 Demo 公开站点 | 保持现有 `/cms/**` 和站点路由 | 不改组件、布局和交互 | 继续来自 API-001、API-002 | API-001, API-002 | 保持现有权限和不可操作反馈 | 保持现有加载、空、失败状态 | 复用现有 `data-page/data-action/data-state`，仅做定向冒烟 | 无前端生产代码变更；UI 只证明消费者契约和 Demo 数据可用 | 前端测试与语义锚点规范 |

## 10. 测试设计与验收映射

| 测试用例ID | 系统验收ID | 设计项ID | 场景 | 优先级 | 测试层级 | 自动化判断 | 测试数据 | 权限或租户边界 | 稳定契约 | 执行入口 | 证据 | 失败处理 | 适用规范ruleId |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| TC-001 | SAC-001, SAC-002 | DEC-002 至 DEC-006, DM-001, FLOW-002, API-001, SEC-001, ERR-001 | 各管理聚合 CRUD、数据权限、关联保护及内容状态流转 | P0 | UNIT、API | AUTO | `IT_CMS_ADMIN_*` 双租户独立数据，事务回滚 | 管理权限、DataScope、对象归属和状态 | 方法签名、错误 code/message、数据库前后状态 | CMS core/starter JUnit 与 MockMvc | `mango-docs/evidence/baselines/cms-architecture/latest/` | 任一差异定位到具体聚合并停止迁移 | 后端测试规范、`rules/09-test-case-automation-flow.md` |
| TC-002 | SAC-003, SAC-004 | DM-002, FLOW-003, API-002, SEC-002, SEC-003, UI-001 | 域名解析、公开可见性、有效期、跨租户和文件引用授权 | P0 | UNIT、API | AUTO | `IT_CMS_PUBLIC_*` 两站点、多状态、多有效期和文件引用矩阵 | 匿名域名访问、目标租户上下文和文件授权 | 公开结果集合、上下文恢复和失败消息 | CMS core/starter JUnit 与 MockMvc | 同上 | 任一非公开数据泄露立即阻断 | 后端测试规范、文件引用规则 |
| TC-003 | SAC-005 | DEC-007, DM-003, FLOW-004, DB-001, DB-002 | 新旧最终 schema、纯 DDL、正式默认和 Demo 显式初始化 | P0 | API、集成 | AUTO | 独立空 MySQL 数据库与固定 Demo 声明 | Demo 明确 tenantId/orgId，正式模式无 Demo | schema 指纹、资源 type/version/bizKey、条数、依赖和幂等 | Flyway/Resource Registry 集成测试与 Mango CLI 启动 | 同上 | DDL/DML 混入、缺表索引或 Demo 误加载均阻断 | 数据库规则、资源说明 |
| TC-004 | SAC-001 至 SAC-005 | DEC-001, MOD-001 至 MOD-004 | CMS 四子模块架构与静态质量 | P0 | STATIC | AUTO | 当前 Git diff 与完整 CMS 模块 classes | 不适用 | dependency/archunit/pmd 从 1126 降至 0，无新增静态问题 | 定向 Maven architecture/verify | 同上 | 任一债务或门禁失败继续修复 | `rules/05-ai-delivery-quality.md` |
| TC-005 | SAC-003, SAC-005 | MOD-003, MOD-005, UI-001, IMP-001, IMP-002 | 新库服务启动、公开 API 和 Demo 站点定向冒烟 | P1 | UI、API | AUTO | workspace `mango_182` 的正式/演示新库和唯一 Demo 站点 | 只使用演示租户，不写共享库 | 健康检查、公开站点关键内容、console/network 无未解释错误 | Mango CLI backend 与定向 CMS Demo UI 测试 | 同上 | 服务、API 或页面任一业务断言失败即阻断 | 开发环境和 UI/E2E 规范 |

## 11. 兼容、发布与能力文档影响

| 影响ID | 设计项ID | 影响对象 | 当前行为 | 目标行为 | 兼容策略 | 升级或回滚 | README或能力地图 | 发布批次 | 验证 | 责任人 |
|---|---|---|---|---|---|---|---|---|---|---|
| IMP-001 | API-001, API-002, DEC-003 至 DEC-006 | CMS Java/HTTP 消费者和现有前端 | 既有方法、路径、字段、R 返回、权限和消息 | 消费者可见契约完全保持，内部职责规范化 | 不提供双路由或兼容壳；直接保持原契约 | 未发布新版本前可回退整个 PR；发布后按平台批次升级 | README 补充验证入口，不改变接入 API | CMS 后端平台物料随下一批发布 | 反射、MockMvc、前端消费和 UI 冒烟 | Mango CMS 能力负责人 |
| IMP-002 | DB-001, DB-002, DEC-007 | 新数据库使用者 | V1-V10 默认带演示内容 | 纯 DDL V1；正式资源默认；Demo 显式 | 明确只支持新数据库，不支持旧 Flyway history 原地升级 | 失败时丢弃新库并回退 PR；不对旧库执行 V1 | README 和能力地图说明最终态初始化政策 | 与 CMS Starter 同批 | schema、正式/demo 初始化和空库启动 | Mango CMS 能力负责人 |

## 12. 技术追踪矩阵

| 上游ID | 设计项ID | 测试用例ID | 覆盖说明 |
|---|---|---|---|
| SC-001, SA-001, SA-002, FR-001, FR-002, UC-001, PG-001, BT-001, BT-002, DR-001, NFR-001, SAC-001, SAC-002 | DEC-001, DEC-002, DEC-003, DEC-004, DEC-005, DEC-006, MOD-001, MOD-002, MOD-003, DM-001, FLOW-001, FLOW-002, API-001, API-002, SEC-001, ERR-001, UI-001, IMP-001 | TC-001, TC-004, TC-005 | 覆盖管理契约、领域拆分、状态、权限、错误与前后基线 |
| SC-002, SA-003, FR-003, FR-004, UC-002, PG-002, BT-003, DR-002, IR-001, NFR-002, SAC-003, SAC-004 | DEC-003, DEC-004, DEC-006, DEC-009, MOD-001, MOD-002, MOD-003, DM-002, FLOW-003, API-003, API-004, SEC-002, SEC-003, ERR-001, UI-001, IMP-001 | TC-002, TC-004, TC-005 | 覆盖公开内容、二进制文件入口、租户上下文与文件授权 |
| SC-003, SA-004, FR-005, FR-006, UC-003, DR-003, NFR-003, SAC-005 | DEC-007, DEC-008, MOD-002, MOD-003, MOD-004, MOD-005, DM-003, FLOW-004, DB-001, DB-002, IMP-002 | TC-003, TC-004, TC-005 | 覆盖纯 DDL、新库、正式/demo 分离、Remote 不扩展和文档影响 |

## 13. 阶段判定与审批

| 检查项 | 结果 | 证据 |
|---|---|---|
| 技术设计 checker | PASS | `node mango-pmo/tools/check-technical-design.mjs --document mango-docs/designs/cms-architecture-debt/technical-design.md`，2026-07-14 通过 |
| 生命周期 handoff | 待书面复核后执行 | 上游摘要已记录，待文档批准后执行阶段移交 |
| 专项规范检查计划 | 已设计 | TC-001 至 TC-005 覆盖 STATIC、UNIT、API、UI；四类均有具体观察对象 |
| 未关闭阻断数量 | 0 | 无未关闭阻断 |
| Tech Lead 审批 | 待书面复核 | `review/PROPOSAL-CMS-DEBT.md` 记录方案方向批准，书面规格待用户复核 |
