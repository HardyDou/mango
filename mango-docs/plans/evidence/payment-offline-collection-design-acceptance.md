# 线下收款独立通道设计确认与交付证据

## 1. 目标

将线下收款从“支付订单人工确认”调整为独立完整支付通道能力。线下收款既是支付方式可路由到的支付通道，也具备该通道自己的后台菜单、收款确认、支付凭证、银行流水导入、对账匹配和退款处理能力。

## 2. 用户确认口径

| 项目 | 确认内容 |
|---|---|
| 领域边界 | 线下收款是支付通道，不是支付订单列表里的人工确认到账操作 |
| 后台菜单 | 支付中心菜单中新增“线下收款”二级菜单，不归属“支付通道”目录 |
| 支付能力 | 收银台选择对公转账后，通过路由命中线下收款通道签约能力，生成支付订单、线下收款记录、随机对账码和转账物料 |
| 收款确认 | 财务在“线下收款”菜单确认到账，确认结果通过通道结果和统一状态机推进支付成功 |
| 凭证能力 | 用户可上传支付凭证；凭证仅作为财务确认辅助依据，不直接推进支付成功 |
| 对账能力 | 财务导入银行流水 Excel，系统按对账码、金额、收款账户、交易时间等自动匹配，并保留异常差异 |
| 退款能力 | 线下收款成功后支持线下退款处理，退款结果通过统一退款状态机推进 |

## 3. 设计决策

| 决策点 | 决定 |
|---|---|
| 通道编码 | `OFFLINE_COLLECTION` |
| 通道类型 | `BUILTIN_OFFLINE` |
| 适配器类型 | `OFFLINE_COLLECTION` |
| 菜单归属 | 支付中心 / 线下收款 |
| 支付订单列表边界 | 只查询支付订单和状态流转，不提供线下收款确认到账或线下退款确认入口 |
| 数据边界 | 新增线下收款记录、支付凭证、银行流水批次、银行流水明细、匹配结果、线下退款处理记录 |
| 状态推进 | 支付成功和退款成功必须先形成线下收款通道结果，再进入统一支付或退款状态机 |
| 审计边界 | 查询、确认到账、凭证查看、银行流水导入、匹配处理和退款处理使用独立权限并记录操作审计 |

## 4. 原子交付项

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| PAY-CONFIRM-001 | 设计文档、交付台账 | 用户确认线下收款范围 | 用户最新要求：“线下收款是完整的支付通道”“新增线下收款菜单”“不要直接用支付订单确认到账” | 已将原待确认项改为用户已确认，并拆分 `PAY-OFFLINE-001` 至 `PAY-OFFLINE-007` | 不涉及 UI | 本项为设计确认，不发起网络请求 | 本文件第 2、3 节 | DONE |
| PAY-OFFLINE-001 | 后台菜单、权限、路由、只读接口 | 新增线下收款独立菜单 | 当前本地库 `mango_dev_e397cd`；线下收款记录允许为空表，数据来自真实 `payment_offline_collection` 表而非前端静态数据 | 后端菜单包含“支付中心 / 线下收款”；支付通道管理展示线下收款通道产品定义，但不承载确认到账、凭证、银行流水或退款处理入口；`/api/payment/offline-collections/statuses` 返回 `WAITING_TRANSFER/PENDING_CONFIRM/CONFIRMED/RECONCILED/EXPIRED/CLOSED`；`/api/payment/offline-collections/page` 返回真实分页结构；支付订单页面无“确认到账”或“线下退款确认”入口 | Playwright 已验证菜单入口、查询栏、列表、支付订单入口边界；列表普通字段纯文本，状态使用 Tag | `PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true pnpm --dir mango-ui/apps/mango-admin exec playwright test e2e/specs/payment-center.spec.ts --project=chromium --grep "线下收款独立菜单"` 通过；后端聚焦测试和 admin build 已通过 | `mango/mango-platform/mango-authorization/mango-authorization-core/src/main/resources/db/migration/authorization/V61__payment_offline_collection_menu_permissions.sql`；`mango/mango-platform/mango-authorization/mango-authorization-core/src/main/resources/db/migration/authorization/V62__payment_offline_collection_second_level_menu.sql`；`mango/mango-platform/mango-payment/mango-payment-starter/src/main/java/io/mango/payment/starter/controller/PaymentReadonlyResourceController.java`；`mango/mango-platform/mango-payment/mango-payment-core/src/main/java/io/mango/payment/core/service/PaymentReadonlyResourceService.java`；`mango-ui/apps/mango-admin/test-results/payment-offline-collections.png` | DONE |
| PAY-OFFLINE-002 | 通道、数据库、Mapper | 建立线下收款通道和数据模型 | 已新增基础 `payment_offline_collection` 表、实体和 Mapper，但尚未完成 `OFFLINE_COLLECTION` 通道产品、签约模板、签约能力、凭证、银行流水批次、流水明细、匹配结果和线下退款处理表 | 原因：当前只完成 `PAY-OFFLINE-001` 及线下收款基础只读表；完整通道产品、签约能力和全部数据模型仍未交付，不能声明通道数据模型完成 | 不涉及 UI | 已有基础表和只读 Mapper 被 `PAY-OFFLINE-001` 使用；完整通道注册、路由试算和支付下单集成仍未验证 | `mango/mango-platform/mango-payment/mango-payment-core/src/main/resources/db/migration/payment/V65__payment_offline_collection_base.sql`；`PaymentOfflineCollectionEntity`；`PaymentOfflineCollectionMapper` | 未验证 |
| PAY-OFFLINE-003 | 收银台支付接口 | 对公转账走线下收款通道并生成对账码 | 待创建支付订单和线下收款记录 | 原因：本轮只完成设计和台账，尚未改造收银台支付接口；实现后必须断言支付初始状态为支付中、不固定成功、转账备注含随机对账码 | 待收银台 E2E | 待实现后验证收银台支付接口请求 | 待补充 | 未验证 |
| PAY-OFFLINE-004 | 线下收款后台接口和页面 | 确认到账闭环 | 待创建支付中线下收款记录 | 原因：本轮只完成设计和台账，尚未新增确认到账接口和页面；实现后必须断言确认到账通过通道结果推进支付成功、流水和通知，重复确认幂等或拒绝 | 待 E2E | 待实现后验证确认到账接口请求 | 待补充 | 未验证 |
| PAY-OFFLINE-005 | 银行流水导入和匹配 | Excel 导入、批次、明细和匹配 | 待上传银行流水 Excel | 原因：本轮只完成设计和台账，尚未新增银行流水导入和匹配服务；实现后必须断言文件防重、对账码和金额唯一匹配，异常进入差异或待处理 | 待 E2E | 待实现后验证导入和匹配接口请求 | 待补充 | 未验证 |
| PAY-OFFLINE-006 | 线下退款处理 | 退款登记、凭证和确认结果 | 待创建已成功线下收款和退款订单 | 原因：本轮只完成设计和台账，尚未新增线下退款处理接口和页面；实现后必须断言线下退款确认通过退款状态机推进退款成功和流水 | 待 E2E | 待实现后验证线下退款接口请求 | 待补充 | 未验证 |
| PAY-OFFLINE-007 | 全链路验收 | 线下收款投产验收 | 待准备完整测试数据 | 原因：本轮只完成设计和台账，线下收款菜单、数据、支付、确认、导入、退款、对账、权限、审计和 E2E 尚未全链路实现和验证 | 待 E2E | 待完整实现后执行全链路网络验证 | 待补充 | 未验证 |

## 5. 当前结论

`PAY-OFFLINE-001` 已完成：线下收款作为“支付中心 / 线下收款”独立二级菜单、权限、只读接口和页面入口已落地，并通过后端聚焦测试、admin 构建和窄范围 Playwright E2E 验证。`PAY-OFFLINE-002` 至 `PAY-OFFLINE-007` 仍未完成，线下收款通道能力、收银台生成对账码、确认到账、银行流水导入匹配、退款处理和投产验收不能声明完成。
