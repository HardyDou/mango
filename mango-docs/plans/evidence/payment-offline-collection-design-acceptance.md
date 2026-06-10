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
| PAY-OFFLINE-002 | 通道、数据库、Mapper | 建立线下收款通道和数据模型 | `OFFLINE_COLLECTION` 通道主数据、签约配置、通道能力、签约能力、订单中心线下转账路由和线下收款相关表均已入库迁移；线下实体继承 `AuditableEntity`，Mapper 继承 MyBatis-Plus `BaseMapper` | `payment_offline_collection`、凭证、银行流水批次、银行流水明细、匹配结果、线下退款处理表均存在；通道编码、签约能力和路由能被收银台真实支付命中 | 不涉及 UI；后台“支付通道”可看到线下收款通道产品，“线下支付/线下收款订单”承载通道运营能力 | 后端测试覆盖通道适配、实体/Mapper/租户边界和 controller 权限；E2E 支付请求返回 `channelCode=OFFLINE_COLLECTION` | `V65/V66/V70/V71/V72/V73/V74/V80/V82/V83` 支付迁移；`PaymentOffline*Entity`；`PaymentOffline*Mapper`；`PaymentOfflineCollectionChannelAdapterTest` | DONE |
| PAY-OFFLINE-003 | 收银台支付接口 | 对公转账走线下收款通道并生成转账备注识别码 | E2E 创建 `PAY-TRANSFER-E2E-*` 业务订单，收银台配置仅启用 `CORPORATE_OFFLINE_ACCOUNT` | 支付初始状态为 `PAYING`，支付结果不固定成功；返回 `TRANSFER_ACCOUNT` 物料、收款户名、账号、开户行和 4-6 位转账备注识别码；创建 `WAITING_TRANSFER` 线下收款记录 | 收银台隐藏多余层级，直接展示线下转账通知单和“已完成转账，回传转账凭证”按钮 | E2E 监听 `/api/payment/cashier/pay`，断言 `channelCode=OFFLINE_COLLECTION`、`materialType=TRANSFER_ACCOUNT`、`transferRemark` 匹配 `[0-9A-Za-z]{4,6}` | `payment-center.spec.ts` 用例 `Web 收银台网银和线下转账物料真实返回`；`PaymentOfflineCollectionChannelAdapterTest` | DONE |
| PAY-OFFLINE-004 | 线下收款后台接口和页面 | 确认到账闭环 | E2E 先提交转账凭证，再调用 `/api/payment/offline-collections/confirm` 单笔确认到账 | 确认后线下收款为 `CONFIRMED`，支付订单为 `SUCCESS` 且 `successFlag=1`，业务订单为 `PAID`，交易流水产生 `PAY_SUCCESS` | 确认到账能力位于“线下支付/线下收款订单”，支付订单列表不承担线下通道运营动作 | E2E 真实调用凭证提交和确认到账接口，并读取支付订单、业务订单和流水分页校验状态 | `PaymentOfflineChannelService`；`PaymentReadonlyResourceControllerTest`；`payment-center.spec.ts` | DONE |
| PAY-OFFLINE-005 | 银行流水导入和匹配 | Excel 导入、批次、明细和匹配确认 | E2E 构造真实 Excel 文件并 multipart 上传到 `/api/payment/offline-collections/bank-statements/import` | 后端解析第一张表，生成 `MATCHED` 批次和 `MATCHED_PENDING_CONFIRM` 明细；财务确认匹配行后批次 `CONFIRMED`，线下收款 `RECONCILED`，支付订单 `SUCCESS`，业务订单 `PAID` | 银行流水导入和确认属于线下收款通道运营能力，不在支付订单列表完成 | E2E 真实上传 Excel；`PaymentOfflineBankStatementExcelParserTest` 验证后端解析银行流水号、交易时间、金额、备注识别码和账号脱敏 | `PaymentOfflineChannelService`；`PaymentOfflineBankStatementExcelParserTest`；`payment-center.spec.ts` | DONE |
| PAY-OFFLINE-006 | 线下退款处理 | 退款登记、凭证和确认结果 | E2E 对已确认到账线下收款发起 38800 分部分退款，提交退款账户、退款金额、退款凭证和原因 | 生成线下退款记录和统一退款订单；退款状态 `REFUNDED`；统一退款流水产生 `REFUND_SUCCESS`；业务订单累计退款金额更新 | 线下退款入口位于“线下支付/线下退款订单”和线下收款订单操作，不复用支付确认入口 | E2E 调用 `/api/payment/offline-collections/refund` 并查询线下退款、统一退款流水、业务订单退款金额 | `PaymentOfflineChannelService`；`PaymentChannelRefundQueryServiceTest`；`PaymentRefundApprovalServiceTest`；`payment-center.spec.ts` | DONE |
| PAY-OFFLINE-007 | 全链路验收 | 线下收款投产验收 | E2E 同一用例覆盖网银物料、线下转账物料、凭证提交、单笔确认、支付成功、部分退款、退款流水、Excel 导入、匹配确认和对账成功 | 线下收款通道本身已满足当前业务范围投产要求；外部真实通道、FTP/FTPS/HTTP 通道账单获取源和用户确认类事项仍按独立台账保持未完成 | 页面布局和交互在收银台、线下收款订单、线下退款订单中完成真实浏览器校验 | Playwright 真实前端、真实后端、真实数据库链路通过；后端聚焦测试覆盖权限、解析、适配器和状态边界 | `payment-center.spec.ts`；`PaymentReadonlyResourceControllerTest`；`PaymentOfflineCollectionChannelAdapterTest`；`PaymentOfflineBankStatementExcelParserTest` | DONE |

## 5. 当前结论

`PAY-OFFLINE-001` 至 `PAY-OFFLINE-007` 已完成：线下收款作为独立完整支付通道，已具备菜单权限、通道主数据、签约能力、收银台转账物料、转账凭证提交、财务确认到账、银行流水 Excel 后端导入解析、匹配确认、部分退款、退款凭证、统一状态流、交易流水和审计边界。该结论只覆盖线下收款通道本身，不替代外部真实通道联调，也不替代 FTP/FTPS/HTTP 通道账单获取源适配。
