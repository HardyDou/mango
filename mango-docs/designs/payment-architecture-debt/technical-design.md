---
documentId: TDD-PAYMENT-DEBT
documentType: technical-design
pmoVersion: 1.1.1
schemaRevision: 1
riskLevel: L3
riskAssessmentEvidence: rules/09-test-case-automation-flow.md 中支付、公共契约、租户、持久化、数据一致性和架构门禁变化的 L3 判定
status: APPROVED
action: NEXT
owner: Mango 支付能力负责人
approver: HardyDou
approvalEvidence: review/TDD-PAYMENT-DEBT.md
upstreamDocumentId: SRS-PAYMENT-DEBT
upstreamDocumentHash: 73388ce748045ef01184fb6596a3b58892a3a6fd5920c728c4709a71e38d6671
---

# 支付模块历史架构债务治理技术设计文档

## 1. 设计输入、约束与决策

| 决策ID | 问题 | 候选方案 | 选择 | 理由 | 来源ID或路径 | 是否推断 | 影响 | 风险 | 回退条件 |
|---|---|---|---|---|---|---|---|---|---|
| DEC-001 | 1,869 条问题跨四个子模块并存在级联关系 | 逐文件局部修复；保留兼容层分批迁移；一个分支按根因整体迁移 | 一个任务分支、一个最终交付，内部按测试基线、契约、服务、持久化、适配器和验证设置检查点 | 用户已确认无其它模块依赖并要求一步到位；整体迁移可避免第二套边界 | FR-002, FR-003, FR-005；用户审批记录 | 否 | 支付四子模块、支付前端、文档和正式债务预算 | 改动面大，必须用同一套业务不变量测试约束 | 任一业务不变量、数据值保持或完整架构门禁不能通过时停止交付，不保留半迁移兼容层 |
| DEC-002 | 当前测试数量较多但没有统一改造前后基线 | 直接重构后补测试；只保存覆盖率；先补有效特征测试再改生产代码 | 先盘点并补订单、回调、退款、通知、对账、结算、租户和接口契约测试，生产代码未变时建立 before 基线，改造后运行同一入口 | 测试结果而非代码形态证明支付逻辑保持；符合 BAC-001/BAC-002 | FR-001, FR-002, NFR-001；`mango-payment-*/src/test` | 否 | 测试资产和证据基线 | 历史实现本身可能暴露真实缺陷 | 基线失败按事实记录并先修测试基础设施；不得弱化业务断言 |
| DEC-003 | API、Controller、Service、Mapper、Entity 和 Feign 职责混合产生大部分级联问题 | 调低规则；保留旧类并新增规范类；直接迁移 canonical 结构 | API 只保留传输无关契约和协议模型；Controller 与 Feign 分别适配 HTTP；core 使用 `I*Service` 与 `*Service`；Mapper 只接收持久化类型；Entity 统一命名和基类 | 与 `rules/backend/03-api.md` 的机器门禁一一对应，删除根因可同时消除级联问题 | FR-002, FR-003, FR-005；MANGO-ARCH-TYPE/CTRL/SVC/MAPPER/ENTITY/FEIGN/ADAPTER | 否 | 353 个支付生产 Java 文件中的受影响边界 | 方法签名和转换遗漏会引起行为差异 | 契约目录测试、编译、业务特征测试和完整架构扫描任一失败即停止 |
| DEC-004 | `PaymentCode` 位置与 Service 错误契约不一致，且大量 `Require` 使用裸码/消息 | 放宽规则；增加桥接枚举；直接迁移业务码 | 将 `PaymentCode` 移到 `io.mango.payment.api.enums`，保持现有数值和消息；所有业务前置条件统一使用 `Require + PaymentCode`，Controller 只返回成功结果 | 不改变业务错误语义，避免重复错误契约 | FR-002, FR-003；MANGO-ARCH-SVC-003/004/006、CTRL-004/013 | 否 | core、starter、remote、测试和 README 引用 | 错误码映射遗漏 | 用错误码快照与异常转换测试比对 code/message，禁止保留旧包入口 |
| DEC-005 | 历史 HTTP 使用路径变量，API/Controller/Feign 绑定不一致 | 保留旧路径；同时暴露新旧路径；直接切换查询参数 | 所有简单标识改为显式命名的查询参数；复杂输入收敛为 Command/Query；Controller 和 Feign verb、完整 path、binding 精确一致；支付前端同步唯一新路径 | 用户已批准无兼容负担；单一契约可消除 PATH、CTRL、ADAPTER 和 FEIGN 级联问题 | FR-003, FR-006, SAC-004；MANGO-ARCH-PATH/CTRL/ADAPTER/FEIGN | 否 | 支付 HTTP 路径、签名 canonical request 和前端调用 | OpenAPI HMAC 使用 request URI，路径变化必须按新版契约验签 | 接口目录与 HMAC 固定向量测试必须同时通过；不保留双路由 |
| DEC-006 | 支付实体自定义 `Long tenantId`，不符合平台 String 租户模型 | 规则豁免；继续数值租户；最终态初始化 | 所有租户业务实体继承 canonical `TenantEntity`，移除重复租户字段；支付 V1 直接创建 `VARCHAR(64)` tenant_id 与可空 `BIGINT` org_id | 平台上下文与其它模块已统一 String；项目尚未发布且只面向全新数据库，不需要先建数值列再转换 | FR-004, DR-002, NFR-003；MANGO-ARCH-ENTITY-003 | 否 | 支付实体、Mapper 条件、索引/唯一约束、V1 和测试数据 | 大量表和索引可能遗漏；最终结构可能偏离旧链结果 | 在隔离 MySQL 分别执行旧 V3-V102 链与新 V1，逐表核对列、索引和唯一约束，再执行双租户读写；失败停止 |
| DEC-007 | BEAN-004 等检查可能把纯集合或异常对象误判为托管 Service | 为支付代码绕过；增加名单豁免；修正可证明误判的类型识别 | 先为检查器增加反例/正例回归；只排除明确非 Service 的 JDK/纯 Java 对象，仍阻断业务 Service 手工构造 | 不把检查器误报转嫁为无意义代码，同时不降低容器边界；仅在支付扫描仍命中已证明误判时实施 | FR-005, SAC-003；`mango-tools/mango-architecture-rules` | 是 | 架构规则测试与完整 Reactor 报告 | 过宽判断会放过真实违规 | 正例必须继续失败，反例必须通过；否则不修改检查器 |
| DEC-008 | 未发布支付模块累积 V3-V102，且团队每次使用全新数据库 | 保留历史链；Flyway baseline 到 102；把历史文件机械合并；生成一个最终态 V1 | 以旧链在干净 MySQL 的最终 schema 为参照，生成一个可读、可重复执行于空库的 canonical V1，删除 V3-V102；只保留正式初始化配置，不携带运行态模拟记录或演示私钥 | `rules/backend/04-db.md` 明确允许未发布前重整初始化 SQL；用户确认只使用新数据库并批准调整。单一最终态 V1 消除百次无效中间变换、修补和敏感演示种子 | 用户 2026-07-13 批准；DB-002；PMO 数据库规则 | 否 | payment Flyway history、初始化数据、迁移测试、README、最终服务验证 | 已存在旧 payment Flyway history 的库不能原地升级；错误裁剪配置可能影响本地可用性 | 仅支持全新数据库；新旧 schema 指纹不一致、Flyway 启动失败或支付接口/页面回归失败即停止并恢复 V3-V102 |

## 2. 模块与依赖边界

| 模块设计ID | 模块或包 | 职责 | 改动类型 | 依赖方向 | 公开能力 | 系统需求ID | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|
| MOD-001 | `mango-payment-api` | 传输无关 Java 契约、Command/Query/VO、enums 与 `PaymentCode` | 全量契约规范化 | 只依赖公共 API 能力，不依赖 core/starter | 支付领域 Java 契约 | FR-003 | MANGO-ARCH-API/HTTP/MODEL/TYPE-009/010/DEP-001 | API 模型校验测试、编译、完整架构扫描 |
| MOD-002 | `mango-payment-core` | 支付规则、编排、事务、状态、实体、Mapper、转换和渠道适配实现 | Service/Mapper/Entity 根因迁移 | 依赖 api、公共 core 与 infra API，不依赖 starter | 支付内部业务实现 | FR-002, FR-004, FR-005 | MANGO-ARCH-SVC/BEAN/MAPPER/ENTITY/TYPE/DEP-002 | 单元、集成、真实数据库和架构测试 |
| MOD-003 | `mango-payment-starter` | Controller、自动装配、配置与默认适配器注册 | HTTP 与装配迁移 | 依赖本域 api/core 和明确 infra starter | 支付 HTTP 入口 | FR-003 | MANGO-ARCH-CTRL/ADAPTER/BEAN/DEP-007 | Spring MVC 契约、装配、权限与异常转换测试 |
| MOD-004 | `mango-payment-starter-remote` | Feign 远程适配 | 远程契约迁移 | 只依赖本域 api/support 与 infra-feign | 支付远程入口 | FR-003 | MANGO-ARCH-FEIGN/ADAPTER/DEP-003/004 | Controller/Feign parity 与编译测试 |
| MOD-005 | `mango-ui/packages/payment` | 支付管理页面、组件与 HTTP 客户端 | 唯一接口映射同步 | 依赖公开前端包，不依赖 apps 私有实现 | 支付管理前端包 | FR-006 | rules/frontend/01-vue-code.md, rules/frontend/06-monorepo-architecture.md | 类型检查、包构建、API 契约与受影响页面入口验证 |
| MOD-006 | `mango-tools/mango-architecture-rules` | Java/Spring 架构规则准确性 | 条件性检查器修复 | 不依赖业务模块 | 全仓架构门禁 | FR-005 | MANGO-ARCH-BEAN-004 与引擎规则 | 规则正反例测试、完整 Reactor 扫描和预算三方比较 |

## 3. 技术对象与状态模型

| 模型ID | 上游ID | 模型职责 | 标识 | 关系 | 状态编码 | 审计或历史 | 归属或租户 | 一致性约束 |
|---|---|---|---|---|---|---|---|---|
| DM-001 | DR-001, FR-002 | 支付订单、业务订单、退款、通知、对账、差错、结算和交易流水聚合 | 既有数据库主键、业务单号、渠道单号与幂等键 | 沿用订单到退款/通知/流水/对账的既有关联 | 沿用 `Payment*StatusEnum` 与现有状态码 | 继承 canonical 审计字段，历史记录不改写业务状态 | String tenantId；每次查询和唯一性判断保持租户边界 | 金额精度、状态流转、幂等、通知次数和关联结果必须与 before 基线一致 |
| DM-002 | DR-002, FR-004 | 支付租户与各支付实体租户归属 | 原 `tenant_id` 值的十进制字符串 | 所有带租户支付表与请求上下文 | 不新增业务状态 | migration 只改变列类型，不改变审计、业务值或记录数量 | canonical `TenantEntity.tenantId` | 转换前后逐行值相等；双租户相同业务键不得互相可见 |
| DM-003 | FR-003, FR-006 | 支付契约目录 | API 方法签名与 HTTP verb/path/binding | Api ↔ Controller ↔ Feign ↔ 前端调用一对一 | 不适用 | Git 历史与迁移说明保留旧到新映射 | 通过 header/context 与 Service 数据条件执行租户边界 | 一个能力只能有一个当前入口；各适配器返回精确 `R<T>` |

| 模型ID | 当前状态 | 触发 | 目标状态 | 前置条件 | 副作用 | 失败处理 | 上游ID |
|---|---|---|---|---|---|---|---|
| DM-001 | before 基线状态 | 内部职责迁移 | after 已验证状态 | TC-001 至 TC-006 已建立并记录 before | 类名、包、签名和转换路径变化，业务值不变 | 任一不变量断言失败，定位为实现差异并在当前检查点修复 | SAC-001, SAC-002 |
| DM-002 | 空数据库 | payment V1 初始化 | 字符串租户列 | 旧 V3-V102 最终 schema 已在隔离 MySQL 固化为参照 | 直接创建最终列与索引 | Flyway 失败即丢弃测试库并修正 V1；不支持旧 payment history 原地升级 | SAC-005 |
| DM-003 | 历史混合契约 | 唯一映射迁移 | canonical 单一契约 | 用户批准无其它模块兼容要求 | 支付前端与 remote starter 调用同步 | 目录 parity 或接口测试失败即修正，不保留历史路由 | SAC-004 |

## 4. 系统流程、事务与一致性

| 流程设计ID | 系统需求ID | 调用入口 | 参与模块 | 处理顺序 | 事务边界 | 状态变化 | 幂等键 | 并发策略 | 外部失败与补偿 | 用户可见结果 |
|---|---|---|---|---|---|---|---|---|---|---|
| FLOW-001 | FR-001, FR-002 | Maven payment test suite | MOD-001, MOD-002, MOD-003, MOD-004 | 盘点现有用例→补有效缺口→生产代码未变时运行 before→迁移→同入口运行 after | 每个测试按目标使用无数据库、事务回滚或隔离数据库 | 只记录测试结果，不改变业务状态 | 用例独立唯一数据前缀 | 用例不依赖顺序；并发规则使用屏障/唯一约束等真实物料 | 第三方渠道允许受控替身，Mango 内部主链路真实执行 | before/after 业务不变量可逐项比较 |
| FLOW-002 | FR-002 | 下单、支付、渠道回调 | MOD-001, MOD-002, MOD-003 | 校验→创建/查询订单→选择渠道→支付→验签/防重→状态流转→流水/通知 | 沿用当前 Service 事务边界 | 沿用业务订单和支付订单状态机 | 业务单号、支付单号、渠道流水与 callback 唯一键 | 沿用唯一约束、状态条件更新和重复处理规则 | 渠道超时/失败按既有错误、查询和补偿语义 | 金额、状态、支付材料、错误码和通知结果不变 |
| FLOW-003 | FR-002 | 退款申请、审批、执行与查询 | MOD-001, MOD-002, MOD-003 | 校验可退金额/状态→审批→渠道退款→状态/流水/通知 | 沿用退款与审批事务 | 沿用退款订单和审批状态 | 退款业务号与渠道退款号 | 防重复退款与重复完成保持 | 渠道失败、重复回调和查询恢复保持 | 可退金额、状态、错误和通知结果不变 |
| FLOW-004 | FR-002 | 对账、差错、结算与离线收付 | MOD-001, MOD-002, MOD-003 | 账单获取/导入→匹配→差错处理→结算确认/作废；离线凭证/流水匹配 | 沿用各聚合事务 | 沿用对账、差错、结算、离线收退款状态 | 批次号、账单明细键和业务单号 | 批次/明细唯一性与状态条件保持 | 文件/渠道失败与人工处理边界保持 | 汇总金额、差异、状态和审计结果不变 |
| FLOW-005 | FR-003, FR-006 | Java/HTTP/Feign/前端 | MOD-001, MOD-003, MOD-004, MOD-005 | Api 定义→Controller/Feign 重声明绑定→Service 调用→统一异常转换→前端调用 | Controller 无事务，事务留在 Service | 只由 Service 改业务状态 | 写动作沿用业务幂等键 | 适配器无额外并发状态 | 统一异常处理输出失败 `R` | 新版接口绑定正确、成功与失败契约一致 |
| FLOW-006 | FR-004 | Flyway 与支付数据读写 | MOD-002 | 旧链生成结构参照→新空库执行 V1→结构指纹对比→启动服务→双租户读写 | V1 只初始化 schema 与正式配置；测试数据由用例创建并清理 | 空库无业务状态；业务状态机定义不变 | 唯一键与旧链最终态保持 | 索引和唯一约束语义保持 | 失败停止应用启动并丢弃测试库 | 新库结构、租户可见性和业务接口正确；不提供旧库升级路径 |

## 5. API 与远程契约设计

| 接口ID | 系统需求ID | 调用方 | 所属模块 | 入口类型 | 方法与路径 | Command Query或VO | 返回契约 | 校验 | 权限租户或数据权限 | 幂等分页或排序 | 错误码 | 兼容策略 | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| API-001 | FR-003 | 支付管理前端、Java 消费者 | MOD-001, MOD-003, MOD-004 | 管理 API | GET /payment/payment-orders/detail | `Long id`；其它管理能力分别使用规范 Command/Query/VO | R<PaymentOrderVO> | Bean Validation + Service `Require` | 保留 `@SaCheckPermission`、租户上下文和数据条件 | 分页排序语义、写动作幂等键不变 | `PaymentCode` 数值和消息保持 | 详情/动作使用固定子路径和显式 query；完整目录由契约测试覆盖，不保留历史路径 | MANGO-ARCH-API/HTTP/MODEL/PATH/CTRL/FEIGN/ADAPTER | 接口目录、MVC 契约、Feign parity、权限与错误转换测试 |
| API-002 | FR-003 | 外部支付应用 | MOD-001, MOD-003 | OpenAPI | POST /openapi/pay/orders/detail | `PaymentOpenRequestCommand` body 中显式 `bizOrderNo`；其它能力使用八个固定 create/detail POST 路径 | R<PaymentOpenBusinessOrderVO> | appId、tenantId、timestamp、nonce、signature、body 与业务字段校验 | 应用启用状态、IP 白名单、签名、防重放和租户归属 | nonce 唯一、业务单号幂等 | 原 `PaymentCode` 与 HTTP 失败语义保持 | 所有签名字段与业务标识统一放入请求 body；请求签名使用实际新版 URI，完整目录同步迁移说明 | MANGO-ARCH-PATH/API/MODEL/ADAPTER/SVC | 固定签名向量、过期/重放/IP/String tenant、成功与失败接口测试 |
| API-003 | FR-003 | 支付渠道 | MOD-001, MOD-003 | 标准化渠道回调 | POST /payment/channel-callbacks | `PaymentChannelCallbackCommand` | R<PaymentChannelCallbackResultVO> | 渠道标识、签名、订单号、金额和状态校验 | INTERNAL 标准入口校验渠道配置、合同、租户与订单归属 | 渠道流水/事件防重 | 保持渠道签名失败、订单不存在和重复回调语义 | 公网 GET/POST `/payment/channel-callbacks/public?channelCode=...` 由函数式 endpoint 保留原始协议与纯文本 ACK，再转换为同一 canonical callback 处理模型 | MANGO-ARCH-PATH/API/MODEL/CTRL/SVC | Fuiou/MangoPay 签名、重复、金额不一致、GET/POST 原始 body/来源地址、ACK 与 PUBLIC 资源声明测试 |
| API-004 | FR-003 | 支付任务与内部能力 | MOD-001, MOD-003, MOD-004 | 内部 API | POST /payment/tasks/expire-open-orders | 无客户端业务参数 | R<PaymentTaskDispatchResultVO> | 接口访问与任务上下文校验 | `@Inner`/权限、租户和数据归属 | 任务/通知/账单批次幂等 | 保持既有业务错误码 | 其它内部方法同样使用固定路径与显式 query/header/body，Controller 与 Feign 精确一致 | MANGO-ARCH-FEIGN/ADAPTER/API/CTRL | Controller/Feign 自动 parity 与内部访问测试 |
| API-005 | FR-006 | 支付前端包 | MOD-005 | TypeScript HTTP client | GET /payment/payment-orders/detail | `ApiId id` | R<PaymentOrderVO> | 页面表单与后端约束对齐 | 保持菜单、按钮和接口权限映射 | 分页、排序和重复提交行为不变 | 显示后端明确错误 | 按 API-001 的唯一目录更新 URL 与 query/body，并保持所有主键字符串语义 | frontend rules | 包类型检查、构建、API 请求契约测试和页面入口回归 |

## 6. 持久化与数据迁移设计

| 数据设计ID | 上游或模型ID | 表或实体 | 字段变化 | 约束 | 索引 | 租户审计 | Mapper边界 | 数据来源 | migration或回填 | 回滚或补偿 | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| DB-001 | DM-001, DR-001, FR-002 | 全部 payment 实体与 Mapper | 非 `*Entity` 类按聚合重命名；重复审计/租户字段移除并继承 canonical 基类；业务列和值不变 | 保留表名、主键、业务唯一约束、金额精度和状态列 | 保留现有索引语义 | 审计字段继承 canonical 基类；租户见 DB-002 | Mapper 直接继承 `BaseMapper<XxxEntity>`，参数/返回仅 Entity、id、Wrapper、Page 或 core Row/Criteria | 现有 payment migration 与业务写入 | 不为类重命名改表；XML namespace/result type 同步 | 代码回滚不改变数据；任何表/列不匹配由 schema checker 阻断 | MANGO-ARCH-MAPPER/ENTITY 与 persistence schema | H2 基础映射、MySQL migration/复杂 SQL、Mapper XML 与 manifest 检查 |
| DB-002 | DM-002, DR-002, FR-004 | 所有 payment 表与初始化配置 | V1 直接声明最终列；tenant_id 为 `VARCHAR(64)`，org_id 为可空 `BIGINT` | 主键、业务唯一约束、金额精度、空性和默认值与旧链最终态保持 | 索引名、列顺序与唯一性保持 | Entity 统一继承 `TenantEntity`，查询从 `MangoContextHolder.tenantId()` 取得 String | Wrapper、XML 与 core Criteria 使用 String tenantId | 旧 V3-V102 在一次性临时库的最终 schema；正式内建通道配置 | 删除 V3-V102，以 `V1__payment_platform.sql` 直接创建最终态；不初始化订单、退款、流水、异常、通知、对账等运行数据，不写演示私钥 | 仅支持丢弃未发布的新库重建；旧库如需保留必须继续使用重整前版本，本任务不提供升级兼容 | MANGO-ARCH-ENTITY-003, rules/backend/04-db.md, rules/backend/07-persistence.md | 新旧 information_schema 指纹对比、敏感/运行态种子扫描、Flyway 新库启动、双租户真实读写和唯一约束测试 |

## 7. 安全、权限、租户与数据边界

| 安全设计ID | 系统需求ID | 能力 | 权限资源 | 默认授权 | 后端校验入口 | 租户边界 | 数据归属断言 | 前端反馈 | 审计 | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| SEC-001 | FR-003, SAC-004 | 管理接口权限 | 保留现有 `payment:*` 资源 | 不新增默认授权 | Controller `@SaCheckPermission` 与 Service 业务归属 `Require` | 当前 String tenantId | 同租户可见，异租户同业务键不可见 | 403/业务失败按统一错误展示 | 保留操作审计 | security/API rules | 反射权限目录、MVC 授权/拒绝与双租户接口测试 |
| SEC-002 | FR-002, FR-003 | OpenAPI 与渠道回调安全 | 应用/渠道配置决定访问 | 无匿名管理授权 | HMAC/渠道验签、timestamp、nonce、IP、订单/金额归属 | 应用、渠道合同和订单映射到唯一租户 | 签名正确仍不得跨应用/租户访问 | 明确签名、过期、重放或归属错误 | 记录请求标识和失败原因，不记录密钥/完整敏感数据 | security rules | 固定签名向量、篡改、过期、重放、IP 与跨租户测试 |
| SEC-003 | FR-004, SAC-005 | 持久化租户隔离 | 数据层租户条件 | 不允许共享默认租户 | Service 输入与 MyBatis 租户能力 | canonical String tenantId | 两租户相同业务键分别创建、查询、更新，结果互不影响 | 不泄露其它租户存在性 | 审计 tenantId 与用户保持 | MANGO-ARCH-ENTITY-003, persistence rules | 隔离 MySQL 双租户集成测试与 SQL 条件检查 |

## 8. 错误码、异常与可观测性

| 错误设计ID | 系统需求ID | 失败场景 | 触发条件 | 错误码 | 异常类型 | 用户反馈 | 日志上下文 | 指标或告警 | 重试或补偿 | 敏感信息处理 |
|---|---|---|---|---|---|---|---|---|---|---|
| ERR-001 | FR-002, FR-003 | 业务前置条件失败 | 参数合法但对象不存在、状态/金额/归属/重复条件不满足 | 迁移后的 `PaymentCode`，数值与消息保持 | `Require` 抛 canonical 业务异常 | 统一异常处理器转换失败 `R` | 记录业务单号、支付单号、租户和动作，不记录密钥 | 沿用现有支付指标/告警 | 沿用可重试与不可重试语义 | 敏感字段脱敏 |
| ERR-002 | FR-002, FR-003 | 外部渠道或通知失败 | 超时、网络、第三方错误、验签失败或重试耗尽 | 既有渠道/通知 PaymentCode | 明确边界异常转换为业务异常 | 保持原失败、处理中、查询恢复或重试反馈 | 渠道、请求标识、订单和错误类别 | 保持可观测性快照和告警 | 沿用查询、重试、补偿与死信/记录策略 | 不记录凭据、签名密钥和完整报文敏感值 |
| ERR-003 | FR-005 | 架构检查失败 | 支付残留或其它模块新增问题指纹 | 架构 ruleId | 构建失败 | 命令输出 moduleKey/ruleId/path/message | 完整报告与预算差异 | CI required check | 修复代码或有回归证明的检查器准确性，不允许豁免 | 报告不含业务密钥 |

## 9. 前端结构与交互实现映射

| 前端设计ID | 系统需求ID | 页面或动作 | 页面key或路由 | 区域与组件 | 状态来源 | API依赖 | 权限或不可操作 | 空加载或失败态 | 语义测试锚点 | 复用判断 | 适用规范ruleId |
|---|---|---|---|---|---|---|---|---|---|---|---|
| UI-001 | FR-006, PG-001, BT-001, SAC-004 | 现有支付管理页面与动作 | 现有 payment admin page keys/routes | `@mango/payment` 现有 views/components，不新增页面 | API-005 与现有页面状态 | API-001, API-005 | 保持现有菜单/按钮权限与后端拒绝反馈 | 保持正常、空、加载、失败、不可操作状态 | 复用现有 `data-page/surface/action/field/state`；缺失的受影响入口补稳定锚点 | 只更新支付包唯一 API client，不复制到 app | frontend test/monorepo/component rules |

## 10. 测试设计与验收映射

| 测试用例ID | 系统验收ID | 设计项ID | 场景 | 优先级 | 测试层级 | 自动化判断 | 测试数据 | 权限或租户边界 | 稳定契约 | 执行入口 | 证据 | 失败处理 | 适用规范ruleId |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| TC-001 | SAC-001, SAC-002 | DEC-002, FLOW-001 | 现有支付 59 个测试类、268 条基线用例与新增有效用例组成统一 suite | P0 | 单元/组件/API/集成 | AUTO | 用例自有 `TEST_/IT_` 数据，无执行顺序依赖 | 明确租户或无租户纯规则 | 同一 Maven 命令、Surefire 用例集合与业务断言 | payment 四子模块 Maven test suite | `mango-docs/evidence/baselines/payment-architecture/latest/` | 任一失败阻断；before/after 记录精确差异 | test flow/backend test rules |
| TC-002 | SAC-002 | DM-001, FLOW-002, FLOW-003 | 金额、订单/退款状态、重复支付/退款完成、渠道回调与错误码 | P0 | 单元/组件 | AUTO | 固定金额边界、合法/非法状态、重复事件 | 固定测试租户和业务键 | Money 精度、状态表、idempotency key、PaymentCode code/message | 定向 JUnit 与 TC-001 | 同上 | 禁止只断言调用次数；业务结果不一致即阻断 | backend test rules |
| TC-003 | SAC-002 | FLOW-004, ERR-002 | 通知、对账、差错、结算、离线收款/退款正常与失败 | P1 | 组件/集成 | AUTO | 唯一批次、账单、凭证和通知记录 | 同租户与异租户样本 | 状态、金额汇总、差异、重试次数和审计结果 | 定向 JUnit 与 TC-001 | 同上 | 外部系统仅替换边界；内部 Service/Mapper 主链路真实执行 | backend test rules |
| TC-004 | SAC-004 | DM-003, API-001, API-003, API-004, SEC-001, ERR-001 | 全部 Api/Controller/Feign 方法目录与关键 MVC 成功、校验、权限、失败转换 | P0 | API/集成 | AUTO | 代表性合法/非法 Command/Query 和用户权限 | 有权/无权、同租户/异租户 | verb、path、binding、泛型、validation、permission、R/error body | starter/remote tests 与 TC-001 | 同上 | 任何未映射或三方不一致阻断 | API/architecture/backend test rules |
| TC-005 | SAC-004 | API-002, SEC-002 | OpenAPI HMAC、timestamp、nonce、IP、URI、body 篡改与防重放 | P0 | 单元/API | AUTO | 固定 app secret/请求向量，仅测试资源 | 应用绑定租户与跨租户订单 | canonical request 与 signature 固定向量 | OpenAPI tests 与 TC-001 | 同上 | 不把密钥写入日志或证据；任一安全边界失败阻断 | security/backend test rules |
| TC-006 | SAC-005 | DM-002, DB-001, DB-002, SEC-003, FLOW-006 | 空库 V1、旧链最终 schema 等价、无运行态/敏感种子、Mapper 与双租户读写 | P0 | 数据库集成 | AUTO | 两个隔离 `mango_dev_*` MySQL；两个 `IT_PAY_` 租户和相同业务键 | 两租户完全隔离 | information_schema 列/索引/约束指纹一致；V1 仅一条 Flyway history；运行态表为空；无演示私钥；唯一约束与查询结果正确 | migration/Mapper integration tests + 完整服务启动 | 同上 | 数据库名不匹配安全前缀、结构差异、敏感种子或任一读写差异即停止 | persistence/backend test rules |
| TC-007 | SAC-003 | DEC-003, DEC-007, MOD-006, ERR-003 | 检查器正反例与完整架构扫描/预算比较 | P0 | 规则单元/架构门禁 | AUTO | Java fixture 与完整 Reactor | 不涉及业务账号 | 正例继续阻断、反例通过、payment 1,869→0、其它新增 0 | architecture rule tests + full verify/report/budget | 同上 | 不允许排除、降级或接受预算增加 | architecture budget rules |
| TC-008 | SAC-004 | MOD-005, API-005, UI-001 | 支付前端类型、构建、API 请求和受影响页面入口 | P1 | 单元/组件/UI入口 | AUTO | 测试账号/租户与可清理支付数据或只读现有数据集 | 按页面权限和租户 | URL/query/body、页面正常/空/失败/无权限状态 | payment package tests/build + payment UI/E2E tag | 同上 | 真实入口环境不可用则记录 BLOCKED，不能用接口 200 代替页面结果 | frontend test rules |

## 11. 兼容、发布与能力文档影响

| 影响ID | 设计项ID | 影响对象 | 当前行为 | 目标行为 | 兼容策略 | 升级或回滚 | README或能力地图 | 发布批次 | 验证 | 责任人 |
|---|---|---|---|---|---|---|---|---|---|---|
| IMP-001 | DEC-003, DEC-004, DEC-005, API-001, API-002, API-003, API-004 | Java/HTTP/Feign 消费者 | 历史混合契约、路径变量和错误码旧包 | canonical 单一契约、固定路径/query/body、PaymentCode 在 api.enums | 用户批准直接切换；无其它模块消费者；支付前端和 remote 同步 | 新版本整体升级；回滚代码与调用方必须同批，禁止协议混用 | 更新 payment 后端 README 与统一支付设计说明的接入/迁移章节 | 后续 Maven/npm 平台发布批次，本任务不发布 | TC-004, TC-005, TC-008 | 支付负责人 |
| IMP-002 | DEC-006, DEC-008, DB-002 | payment 数据库消费者 | V3-V102 历史初始化链 | 单一 `V1__payment_platform.sql`，直接创建 `VARCHAR(64)` String tenantId 最终态 | 仅支持全新数据库，不提供旧 Flyway history 兼容 | 未发布环境丢弃数据库后重建；若必须保留旧库则继续使用重整前版本 | README 明确新库前提、V1 和禁止旧库原地升级 | 后续后端发布批次 | TC-006 | 支付负责人 |
| IMP-003 | DEC-001, DEC-002, DEC-007, MOD-006, ERR-003 | 维护与测试资产 | 支付债务预算 1,869、无统一 before/after 基线 | 支付预算 0、长期 suite 与 latest 结果基线 | 不接受历史预算回升 | Git 回滚仅用于任务未交付；债务下降不得在后续回升 | 更新设计、计划、交付台账、测试交接与架构基线 | 当前 PR | TC-001 至 TC-008 | 支付负责人 |

## 12. 技术追踪矩阵

| 上游ID | 设计项ID | 测试用例ID | 覆盖说明 |
|---|---|---|---|
| SC-001, SA-001, SA-002, FR-001, FR-002, FR-003, FR-004, FR-005, FR-006, UC-001, UC-002, PG-001, BT-001, DR-001, DR-002, IR-001, NFR-001, NFR-002, NFR-003, SAC-001, SAC-002, SAC-003, SAC-004, SAC-005 | DEC-001, DEC-002, DEC-003, DEC-004, DEC-005, DEC-006, DEC-007, DEC-008, MOD-001, MOD-002, MOD-003, MOD-004, MOD-005, MOD-006, DM-001, DM-002, DM-003, FLOW-001, FLOW-002, FLOW-003, FLOW-004, FLOW-005, FLOW-006, API-001, API-002, API-003, API-004, API-005, DB-001, DB-002, SEC-001, SEC-002, SEC-003, ERR-001, ERR-002, ERR-003, UI-001, IMP-001, IMP-002, IMP-003 | TC-001, TC-002, TC-003, TC-004, TC-005, TC-006, TC-007, TC-008 | 全部系统需求映射到根因迁移、支付业务不变量、接口与安全、租户数据、前端入口和完整架构预算验证 |

## 13. 阶段判定与审批

| 检查项 | 结果 | 证据 |
|---|---|---|
| 技术设计 checker | PASS | `node mango-pmo/tools/check-technical-design.mjs --document mango-docs/designs/payment-architecture-debt/technical-design.md` |
| 生命周期 handoff | PASS | `node mango-pmo/tools/check-lifecycle-handoff.mjs --brd mango-docs/designs/payment-architecture-debt/business-requirements.md --srs mango-docs/designs/payment-architecture-debt/system-requirements.md --tdd mango-docs/designs/payment-architecture-debt/technical-design.md --risk L3 --through tdd` |
| 专项规范检查计划 | PASS | TC-001 至 TC-008 覆盖后端测试质量、API/安全、真实数据库、前端入口、架构规则和预算门禁 |
| 未关闭阻断数量 | 0 | 当前设计无开放阻断或例外 |
| Tech Lead 审批 | APPROVED | `review/TDD-PAYMENT-DEBT.md` |
