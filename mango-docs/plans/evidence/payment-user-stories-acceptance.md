# 支付方式与业务接入用户故事验收证据

## 1. 交付项

- 台账项：`PAY-STORY-001`
- 设计来源：`统一支付系统设计说明书.md` 第 6.5.1、6.5.2 节
- 验收目标：确认支付方式、通道能力、签约能力、路由策略和业务接入用户故事均有明确场景、验收口径和现有台账证据映射。

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| PAY-STORY-001 | 设计文档 6.5.1 | 8 条支付方式、通道能力、签约能力和路由策略用户故事确认 | 设计文档用户故事表 | 每条故事均有参与人、场景描述、业务结果和验收口径，并映射到当前交付台账证据 | 故事覆盖后台运营、商户运营、付款人、业务系统、财务和技术支持角色 | 文档和台账检查通过 | 本文件第 2 节 | DONE |
| PAY-STORY-001 | 设计文档 6.5.1 | 8 个典型业务场景确认 | 芒果支付微信扫码、通联优先、通联停用切换、企业网银、对公转账、金额超限、应用可见范围、多主体签约 | 典型场景已拆到通道、签约、路由、收银台、开放接口、对账结算等台账；外部真实通道联调仍归属通道专项台账 | 收银台聚合展示、路由试算、订单详情、对账结算页面已有对应证据 | 现有 E2E/接口/单测证据覆盖当前可验收部分 | 本文件第 3 节 | DONE |
| PAY-STORY-001 | 设计文档 6.5.2 | 7 条业务接入与开通服务场景确认 | 应用开通、芒果支付验证、真实收款开通、创建订单跳转收银台、付款人支付、业务通知、排障 | 应用、收银台、开放接口、芒果支付、通知、路由试算和后台排障证据均已建立；剩余投产能力继续由对应台账跟踪 | 业务订单支付弹窗、收银台、通知记录、操作审计、对账结算页面已有真实页面证据 | 现有 Playwright/Maven/MySQL 证据可追溯 | 本文件第 4 节 | DONE |

## 2. 用户故事覆盖矩阵

| 用户故事 | 验收口径 | 对应台账 | 证据文件 | 结论 |
|---|---|---|---|---|
| 平台运营维护统一支付方式字典 | 后台可新增、编辑、启停标准支付方式；列表展示三级路径和收银台展示名 | `PAY-MENU-006`、`PAY-METHOD-DICT-001` | `payment-method-acceptance.md` | 已覆盖 |
| 平台运营声明每个支付通道支持哪些能力 | 通道详情可维护支持能力；同一标准支付方式可被多个通道支持 | `PAY-MENU-005`、`PAY-CHANNEL-CAP-001` | `payment-channel-acceptance.md` | 已覆盖 |
| 商户运营登记真实签约资料 | 通道签约配置关联企业主体；签约能力只能来自该通道声明的能力 | `PAY-MENU-004`、`PAY-CONTRACT-CAP-001` | `payment-channel-contract-acceptance.md` | 已覆盖 |
| 平台运营配置同一个支付方式走哪个签约能力 | 路由规则项指向签约能力；可验证优先级、手工指定、失败降级和通道隔离 | `PAY-ROUTE-001`、`PAY-ROUTE-TEST-001`、`PAY-E2E-ROUTE-001` | `payment-method-route-acceptance.md` | 已覆盖 |
| 付款人只看到容易理解的付款方式 | 多个签约能力支持同一标准支付方式时，收银台只展示一个聚合支付方式 | `PAY-CASHIER-001`、`PAY-CASHIER-003`、`PAY-E2E-ROUTE-001` | `payment-cashier-config-acceptance.md`、`payment-method-route-acceptance.md` | 已覆盖 |
| 业务系统发起订单但不关心底层通道 | 开放接口返回收银台地址或支付参数；支付订单记录最终命中的通道和签约能力 | `PAY-API-002`、`PAY-API-004`、`PAY-API-005` | `payment-openapi-acceptance.md` | 已覆盖 |
| 财务按企业主体核对收款 | 订单、流水、对账、结算均可按租户、应用、企业主体、通道和签约配置查询 | `PAY-MENU-007`、`PAY-MENU-010`、`PAY-MENU-013`、`PAY-MENU-015` | `payment-business-order-acceptance.md`、`payment-transaction-flow-acceptance.md`、`payment-reconciliation-acceptance.md`、`payment-settlement-summary-acceptance.md` | 已覆盖 |
| 技术支持定位一笔支付为何走某个通道 | 支付订单详情展示 method_code、route_rule_id、contract_capability_id、channel_code 和过滤/命中结果 | `PAY-MENU-008`、`PAY-ROUTE-TEST-001`、`PAY-MENU-016` | `payment-order-acceptance.md`、`payment-method-route-acceptance.md`、`payment-operation-audit-acceptance.md` | 已覆盖 |

## 3. 典型场景覆盖矩阵

| 典型场景 | 当前验收结论 | 对应证据 | 边界说明 |
|---|---|---|---|
| 芒果支付微信扫码支付 | 芒果支付作为正式通道模型可完成下单、支付、流水、通知、账单和对账相关验证 | `payment-mango-pay-acceptance.md`、`payment-cashier-config-acceptance.md` | 不代表外部机构通道联调完成 |
| 生产微信扫码优先走通联 | 路由模型支持同一标准支付方式按优先级路由到签约能力 | `payment-method-route-acceptance.md`、`payment-channel-acceptance.md`、`payment-channel-contract-acceptance.md` | 通联真实适配和联调归属 `PAY-CHANNEL-003` |
| 通联能力停用后切换微信直连 | 路由模型支持能力状态过滤、优先级和失败降级表达 | `payment-method-route-acceptance.md` | 微信直连真实适配归属 `PAY-CHANNEL-007` |
| 企业网银支付 | Web/PC 收银台支持网银/银联跳转类支付物料，路由模型支持企业网银签约能力 | `payment-cashier-config-acceptance.md`、`payment-method-route-acceptance.md` | 华夏银行真实适配和联调归属 `PAY-CHANNEL-005` |
| 对公转账 | Web 收银台可展示线下转账支付物料；主体银行账户模型已独立补齐 | `payment-cashier-config-acceptance.md`、`payment-core-data-model-acceptance.md` | 自动认款是否纳入第一阶段归属 `PAY-CONFIRM-001` |
| 支付方式金额超限 | 路由试算和签约能力限额模型可表达金额过滤原因 | `payment-method-route-acceptance.md`、`payment-channel-contract-acceptance.md` | 更细的投产级限流/风控归属 `PAY-OBS-001`、`PAY-SEC-*` 等专项 |
| 应用只开放部分支付方式 | 收银台配置可限定允许支付方式，开放接口获取收银台时按应用和配置返回 | `payment-cashier-config-acceptance.md`、`payment-openapi-acceptance.md` | 不改变应用管理与收银台配置职责边界 |
| 多主体不同签约资料 | 通道签约配置按企业主体保存商户号、AppId、证书、签约能力和限额 | `payment-channel-contract-acceptance.md` | 外部真实商户参数和证书联调归属对应通道专项 |

## 4. 业务接入与开通服务场景覆盖矩阵

| 服务场景 | 当前验收结论 | 对应证据 | 边界说明 |
|---|---|---|---|
| 为业务应用开通支付服务 | 应用管理、接入安全、默认收银台、Logo 文件 ID、支付规则配置已有真实页面和接口证据 | `payment-application-delete-acceptance.md`、`payment-cashier-config-acceptance.md` | 应用不维护企业主体范围、支付方式范围或退款规则 |
| 先用芒果支付内置虚拟通道验证应用接入 | 芒果支付可支撑支付、查单、关单、退款、退款查询、账单、对账、异常场景和延迟通知验证 | `payment-mango-pay-acceptance.md` | 长时间运行稳定性和全量 E2E 仍在测试专项台账 |
| 给应用开通真实收款能力 | 通道、签约和路由模型已具备真实通道配置入口和签约能力承载 | `payment-channel-acceptance.md`、`payment-channel-contract-acceptance.md`、`payment-method-route-acceptance.md` | 通联、华夏、微信、支付宝、连连等外部通道联调仍由对应通道台账推进 |
| 创建支付订单并跳转收银台 | 开放接口创建业务订单、获取收银台、发起支付、查询订单已有签名和幂等证据 | `payment-openapi-acceptance.md` | 业务系统自身状态推进不属于支付模块 |
| 付款人在业务页面完成付款 | Web/H5 收银台组件可展示订单、支付方式、支付物料和结果轮询 | `payment-cashier-config-acceptance.md`、`payment-cashier-delay-acceptance.md` | App 和小程序页面不纳入本期收银台页面交付 |
| 业务系统接收支付结果并推进业务 | 支付成功、失败、关闭、退款成功、退款失败通知已有真实 HTTP 回调和 ACK 证据 | `payment-openapi-acceptance.md`、`payment-notification-record-acceptance.md` | 多租户长周期调度和外部通道回调通知仍由 `PAY-API-010` 跟踪 |
| 排查业务应用为何不能支付 | 路由试算、支付订单详情、异常订单、通知记录和操作审计提供排查入口 | `payment-method-route-acceptance.md`、`payment-order-acceptance.md`、`payment-exception-order-acceptance.md`、`payment-operation-audit-acceptance.md` | 可观测性指标和告警归属 `PAY-OBS-001` |

## 5. 结论

`PAY-STORY-001` 当前验收结论为通过：

- 设计文档第 6.5.1、6.5.2 节已给出完整用户故事、典型场景、业务接入流程和最小配置。
- 每条故事均已映射到当前台账中的页面、接口、数据模型或 E2E 证据。
- 本项只确认故事和验收口径已经完整建立并可追溯，不替代通联、华夏、微信、支付宝、连连等外部通道联调，不替代通知长周期调度、状态机并发和全量 E2E 投产验收。

全量支付模块仍有其他 `IN_PROGRESS` 台账项，不能据此声明支付模块整体完成。
