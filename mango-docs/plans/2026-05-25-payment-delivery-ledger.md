# 统一支付平台重构交付契约与台账

## 1. 目标

基于 `mango-docs/designs/统一支付系统设计说明书.md` 重构支付平台能力，交付可部署、可验证、可投产的统一支付系统。研发不得缩小设计范围，不得用合并页面、前端临时菜单、演示替代或仅主链路跑通替代设计项。

## 2. 范围

本交付覆盖支付平台本期设计范围：支付中心后台菜单、应用管理、企业主体管理、支付通道管理、支付方式管理、收银台配置、业务订单、支付订单、退款订单、交易流水、异常订单、通知记录、对账管理、差异处理、结算汇总、操作审计、开放接口、自建沙箱通道、通联支付通道、华夏银行通道、已确认支付通道、收银台组件、权限、审计、测试和投产就绪检查。

## 3. 不做什么

1. 不建设钱包账户、用户余额、商户余额、充值、提现、内部转账。
2. 不建设财务总账、会计凭证、自动付款。
3. 不承载保函业务模型，不判断保函业务状态。
4. 不提供保证金/担保金业务、监管户、专户入账通知。
5. 不设置独立“租户收银台”后台菜单。
6. 不把支付结果页登记为后台菜单。
7. 不复制历史项目密钥、证书、包名、实体和 Controller 路径。

## 4. 设计输入

1. 设计文档：`mango-docs/designs/统一支付系统设计说明书.md`，作为唯一支付平台落地设计输入。
2. PMO 研发流程：`mango-pmo/rules/00-dev-flow.md`。
3. PMO 交付契约：`mango-pmo/rules/01-delivery-contract.md`。
4. Tech Lead 角色规则：`mango-pmo/agents/02-tech-lead-agent.md`。
5. Sprint 计划：`mango-docs/plans/2026-05-25-payment-sprint-01.md`。
6. 投产清单：`mango-docs/plans/2026-05-25-payment-production-readiness.md`。

## 5. 设计说明

### 5.1 影响模块

后端支付域、后台菜单与权限、开放接口、通道适配、通知、对账、结算、审计、前端支付中心页面、前端收银台组件、E2E 测试、自建沙箱通道环境、通联支付联调、华夏银行通道联调。

### 5.2 接口变化

新增支付开放接口、通道回调接口、后台管理接口、沙箱通道接口、通知重推接口、对账与结算操作接口。开放接口必须包含应用认证、签名、防重放、幂等和多租户隔离。

### 5.3 数据变化

新增支付域核心表、唯一约束、状态机字段、审计字段、租户隔离字段、敏感字段加密存储字段、账单与差异处理字段。所有金额以分为单位，使用整数金额模型。

### 5.4 菜单/页面/权限变化

支付中心菜单由后端菜单数据登记。后台页面逐菜单交付，收银台支付页、支付结果页、二维码展示、网银跳转、线下转账说明、App 唤起页、小程序支付参数页均不作为后台菜单。

### 5.5 测试范围

后端单元测试、后端集成测试、开放接口测试、状态机测试、幂等并发测试、权限与租户隔离测试、前端组件测试、支付中心 E2E、收银台 E2E、沙箱支付流程 E2E、对账与结算验证、投产就绪检查。

## 6. 风险与限制

1. 线下转账第一阶段范围、除自建沙箱/通联/华夏银行外的已知通道清单与接入顺序、支付成功是否触发开票通知、租户定义仍需用户确认；确认前不得由 Agent 自行假设。
2. 通道真实生产配置、证书、密钥不进入仓库；开发和验证使用独立沙箱通道环境，外部通道使用官方或内部联调环境。
3. 支付模块不得直接依赖其他模块内部实现，只能依赖公开 API。
4. 任何未完成项不得在交付报告中声明完成。

## 7. 交付台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| PAY-SCOPE-001 | 设计文档 1-2 | 支付系统只提供支付域能力，不承载保函业务模型、钱包、总账、保证金业务 | 支付模块按支付域独立重构，保函业务仅通过开放接口接入 | 后端模块边界说明、依赖关系、代码包结构 | 架构检查、依赖检查、代码审查 | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-sprint-01.md` |
| PAY-DATA-001 | 设计文档 11.1 | 建立支付核心数据模型 | 新增支付域表：租户、应用、主体、通道、方式、收银台、订单、流水、账单、差异、结算、通知、审计、风控 | 数据库迁移、实体、Repository、数据字典 | 迁移执行、集成测试、字段审查 | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-production-readiness.md` |
| PAY-DATA-002 | 设计文档 11.2 | 建立关键唯一约束 | 用数据库唯一约束和业务幂等共同防止重复支付、重复退款、重复通知 | 唯一索引、并发测试 | 并发集成测试、数据库约束检查 | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-production-readiness.md` |
| PAY-DATA-003 | 设计文档 11.3 | 金额以分为单位并统一校验 | 后端以整数金额模型处理金额，禁止散落换算 | 金额值对象、参数校验、金额测试 | 单元测试、接口测试 | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-production-readiness.md` |
| PAY-DATA-004 | 设计文档 12 | 所有核心业务表支持租户隔离 | 表结构包含 tenant_id，后台查询默认带租户上下文 | 表字段、查询条件、权限拦截 | 多租户隔离集成测试、E2E | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-production-readiness.md` |
| PAY-MENU-001 | 设计文档 3.1 | 后台新增支付中心一级菜单 | 菜单由后端菜单数据登记，前端只消费后端菜单 | 菜单迁移、权限点、路由映射 | 数据库菜单检查、E2E 菜单可见性验证 | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-sprint-01.md` |
| PAY-MENU-002 | 设计文档 3.1 | 应用管理列表页 | 管理 appCode、密钥、回调地址、支付配置 | 后台页面、后端接口、权限、审计 | E2E、接口测试、权限测试 | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-sprint-01.md` |
| PAY-MENU-003 | 设计文档 3.1 | 企业主体管理列表页 | 管理收款主体、证照、银行账户、通道商户归属 | 后台页面、后端接口、敏感字段脱敏、权限、审计 | E2E、接口测试、安全检查 | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-sprint-01.md` |
| PAY-MENU-004 | 设计文档 3.1 | 支付通道管理列表页 | 管理通道参数、证书、密钥引用、商户号、启停状态 | 后台页面、后端接口、密钥引用、权限、审计 | E2E、接口测试、敏感字段检查 | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-sprint-01.md` |
| PAY-MENU-005 | 设计文档 3.1 | 支付方式管理列表页 | 管理支付方式展示、通道绑定、优先级、限额和可见范围 | 后台页面、后端接口、权限、审计 | E2E、接口测试 | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-sprint-01.md` |
| PAY-MENU-006 | 设计文档 3.1、6.6 | 收银台配置列表页 | 收银台配置为后台菜单，行操作进入收银台页面 | 后台页面、后端接口、行操作、权限、审计 | E2E 验证列表与行操作 | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-sprint-01.md` |
| PAY-MENU-007 | 设计文档 3.1 | 业务订单列表页 | 查询业务支付意图和状态 | 后台页面、查询接口、详情页、权限 | E2E、接口测试 | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-sprint-01.md` |
| PAY-MENU-008 | 设计文档 3.1 | 支付订单列表页 | 查询支付尝试、通道请求和状态流转 | 后台页面、查询接口、详情页、权限 | E2E、接口测试 | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-sprint-01.md` |
| PAY-MENU-009 | 设计文档 3.1 | 退款订单列表页 | 查询退款申请、退款状态、通道结果 | 后台页面、查询接口、详情页、权限 | E2E、接口测试 | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-sprint-01.md` |
| PAY-MENU-010 | 设计文档 3.1 | 交易流水列表页 | 查询支付、退款、手续费资金流水 | 后台页面、查询接口、详情页、权限 | E2E、接口测试 | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-sprint-01.md` |
| PAY-MENU-011 | 设计文档 3.1、13 | 异常订单列表页 | 处理重复支付、超时未回调、金额不一致、状态不一致等异常 | 后台页面、受控操作接口、权限、审计 | E2E、异常处理集成测试 | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-sprint-01.md` |
| PAY-MENU-012 | 设计文档 3.1、10.5 | 通知记录列表页 | 查看通知结果、失败重试、人工补偿推送 | 后台页面、重推接口、权限、审计 | E2E、通知重试集成测试 | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-sprint-01.md` |
| PAY-MENU-013 | 设计文档 3.1、7.3 | 对账管理列表页 | 管理账单导入、对账任务和批次结果 | 后台页面、导入接口、批次接口、权限 | E2E、账单导入集成测试 | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-sprint-01.md` |
| PAY-MENU-014 | 设计文档 3.1、7.4 | 差异处理列表页 | 查单、补单、退款、忽略、关闭等受控处理 | 后台页面、处理接口、附件凭据、权限、审计 | E2E、差异处理集成测试 | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-sprint-01.md` |
| PAY-MENU-015 | 设计文档 3.1、7.5 | 结算汇总页 | 生成、确认、作废、重新生成结算汇总 | 后台页面、汇总接口、确认接口、权限、审计 | E2E、汇总计算测试 | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-sprint-01.md` |
| PAY-MENU-016 | 设计文档 3.1、13 | 操作审计列表页 | 查询资金相关人工操作、配置变更、审批记录 | 后台页面、查询接口、审计记录写入 | E2E、审计完整性测试 | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-sprint-01.md` |
| PAY-MENU-017 | 设计文档 3.1、6.6 | 不设置独立“租户收银台”菜单 | 租户维度通过收银台配置筛选、详情、行操作承载 | 菜单数据检查、路由检查 | E2E 断言无独立菜单 | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-sprint-01.md` |
| PAY-CASHIER-001 | 设计文档 6.6 | 收银台封装为可复用前端组件 | 建设 `PaymentCashier`，由业务支付页、后台行操作、沙箱支付流程、E2E 复用 | 组件、类型、导出、组件测试 | 组件测试、E2E | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-sprint-01.md` |
| PAY-CASHIER-002 | 设计文档 6.6 | 收银台配置行操作进入收银台页面 | 路由采用 `/payment/cashier-configs/:cashierId/cashier`，不登记为菜单 | 非菜单路由、页面、行按钮 | E2E 点击验证 | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-sprint-01.md` |
| PAY-CASHIER-003 | 设计文档 6.6 | PC 收银台支持二维码、网银跳转、线下转账信息展示 | `PaymentCashier` 支持 PC 终端形态 | PC 收银台视图、支付物料渲染 | E2E、组件测试 | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-sprint-01.md` |
| PAY-CASHIER-004 | 设计文档 6.6 | H5 收银台支持微信/支付宝 H5 支付 | `PaymentCashier` 支持 H5 终端形态 | H5 收银台视图、跳转处理 | E2E、组件测试 | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-sprint-01.md` |
| PAY-CASHIER-005 | 设计文档 6.6 | App 收银台支持唤起微信、支付宝等 App | `PaymentCashier` 支持 App 唤起和回跳 | App 唤起页、回跳状态处理 | E2E、接口测试 | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-sprint-01.md` |
| PAY-CASHIER-006 | 设计文档 6.6 | 小程序收银台支持支付参数获取 | `PaymentCashier` 支持小程序参数模式 | 参数接口、参数页、类型 | 接口测试、E2E | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-sprint-01.md` |
| PAY-CASHIER-007 | 设计文档 6.6 | 支付结果页属于收银台流程，不作为后台菜单 | 结果展示由 `PaymentCashier` 或非菜单页面承载 | 结果页、路由、菜单断言 | E2E 断言结果页不在后台菜单 | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-sprint-01.md` |
| PAY-API-001 | 设计文档 10.1 | 开放接口签名、防重放、防篡改 | 使用 appCode、timestamp、nonce、signature、tenantId 认证 | 认证拦截器、签名工具、重放保护 | 接口安全测试 | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-production-readiness.md` |
| PAY-API-002 | 设计文档 10.2 | 创建业务订单接口 | `POST /openapi/pay/orders`，按 tenantId + appCode + bizOrderNo 幂等 | Controller、DTO、服务、测试 | API 测试、幂等测试 | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-sprint-01.md` |
| PAY-API-003 | 设计文档 10.2 | 查询业务订单接口 | `GET /openapi/pay/orders/{bizOrderNo}` | Controller、DTO、服务、测试 | API 测试 | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-sprint-01.md` |
| PAY-API-004 | 设计文档 10.2 | 获取收银台地址接口 | `POST /openapi/pay/orders/{bizOrderNo}/cashier`，未过期订单返回可用入口 | Controller、DTO、服务、测试 | API 测试、E2E | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-sprint-01.md` |
| PAY-API-005 | 设计文档 10.2 | 发起支付接口 | `POST /openapi/pay/orders/{bizOrderNo}/pay`，每次可生成新支付订单 | Controller、DTO、服务、通道路由 | API 测试、状态机测试 | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-sprint-01.md` |
| PAY-API-006 | 设计文档 10.2 | 查询支付订单接口 | `GET /openapi/pay/payment-orders/{payOrderNo}` | Controller、DTO、服务、测试 | API 测试 | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-sprint-01.md` |
| PAY-API-007 | 设计文档 10.2 | 发起退款接口 | `POST /openapi/pay/refunds`，按 tenantId + appCode + bizRefundNo 幂等 | Controller、DTO、服务、测试 | API 测试、超额退款测试 | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-sprint-01.md` |
| PAY-API-008 | 设计文档 10.2 | 查询退款接口 | `GET /openapi/pay/refunds/{bizRefundNo}` | Controller、DTO、服务、测试 | API 测试 | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-sprint-01.md` |
| PAY-API-009 | 设计文档 10.2 | 获取支付凭证接口 | `GET /openapi/pay/receipts/{bizOrderNo}` | Controller、DTO、服务、测试 | API 测试 | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-sprint-01.md` |
| PAY-API-010 | 设计文档 10.5 | 支付和退款通知业务方 | 支付成功、失败、关闭、退款成功、退款失败时异步通知业务系统 | 通知记录、通知任务、重试、ACK 处理 | 集成测试、E2E | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-sprint-01.md` |
| PAY-DOMAIN-001 | 设计文档 6.7、8.1 | 业务订单状态机 | 业务订单状态只能按允许路径推进 | 状态机、状态变更日志、测试 | 状态机单元测试、集成测试 | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-production-readiness.md` |
| PAY-DOMAIN-002 | 设计文档 6.8、8.2 | 支付订单状态机 | 支付订单状态由回调、查单、对账补偿、受控操作推进 | 状态机、唯一成功约束、测试 | 状态机测试、并发测试 | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-production-readiness.md` |
| PAY-DOMAIN-003 | 设计文档 6.9、8.3 | 退款订单状态机 | 退款订单支持多次部分退款，累计金额不超过可退金额 | 状态机、可退金额锁定、测试 | 状态机测试、并发测试 | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-production-readiness.md` |
| PAY-DOMAIN-004 | 设计文档 6.10 | 交易流水不可删除 | 流水仅表达支付域资金事件，不做会计分录 | 流水服务、唯一约束、审计 | 集成测试、删除权限检查 | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-production-readiness.md` |
| PAY-CHANNEL-001 | 设计文档 6.3、20 | 通道适配层插件化 | 自建沙箱、通联、华夏银行、微信、支付宝、连连等通过统一接口适配 | 通道 SPI、DTO、返回码映射、验签接口 | 单元测试、适配层测试 | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-sprint-01.md` |
| PAY-CHANNEL-002 | 用户追加要求 | 提供自建沙箱通道环境 | 自建沙箱通道作为正式通道类型，支持支付、回调、查单、退款、账单 | 自建沙箱通道服务、后台配置、沙箱控制接口 | 沙箱 E2E、账单核对测试 | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-sprint-01.md` |
| PAY-CHANNEL-003 | 用户追加要求、设计文档 6.3 | 接入通联支付通道 | 通联支付作为明确通道交付项，必须支持支付、回调验签、主动查单、退款、账单和对账 | 通联适配器、配置、回调、查单、退款、账单解析 | 通联联调环境测试、接口测试、账单核对测试 | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-sprint-01.md` |
| PAY-CHANNEL-004 | 设计文档 14、15 | 通道证书和密钥安全 | 只存密钥引用或密文，不在后台明文展示 | 加密存储、脱敏展示、轮换记录 | 安全检查、接口测试 | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-production-readiness.md` |
| PAY-CHANNEL-005 | 用户追加要求、设计文档 6.3 | 接入华夏银行通道 | 华夏银行通道作为明确通道交付项，实施前必须补齐官方或内部对接文档和联调环境，不允许用其他通道替代 | 华夏银行适配器、配置、回调验签、查单、退款、账单解析 | 华夏银行联调环境测试、接口测试、账单核对测试 | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-sprint-01.md` |
| PAY-CHANNEL-006 | 用户追加要求 | 登记其他已知支付通道清单 | “等已知支付通道”必须形成确认清单；确认前不得自行删除、替换或降级为沙箱/通联/华夏银行 | 已知通道清单、用户确认记录、台账更新 | 用户确认后更新设计和台账 | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-sprint-01.md` |
| PAY-CHANNEL-007 | 用户确认 | 微信支付 SDK 参考依赖 | `IJPay-WxPay 2.8.0`、`weixin-java-mp 4.4.0`、`wechatpay-java 0.2.17` 可作为微信支付适配参考，SDK 只放通道适配层 | Maven 依赖、微信适配器、SDK 使用边界说明 | 编译检查、适配层单元测试、联调测试 | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-sprint-01.md` |
| PAY-CHANNEL-008 | 用户确认 | 支付宝 SDK 参考依赖 | `IJPay-AliPay 2.8.0`、`alipay-sdk-java 4.34.0.ALL` 可作为支付宝适配参考，SDK 只放通道适配层 | Maven 依赖、支付宝适配器、SDK 使用边界说明 | 编译检查、适配层单元测试、联调测试 | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-sprint-01.md` |
| PAY-RECON-001 | 设计文档 7.3 | 通道账单导入和批次管理 | 同一通道、日期、文件只能导入一次，重跑保留记录 | 账单批次、账单明细、导入接口 | 集成测试、文件摘要验证 | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-sprint-01.md` |
| PAY-RECON-002 | 设计文档 7.4 | 对账差异识别 | 识别我方成功通道无单、通道成功我方无单、金额不一致等差异 | 对账服务、差异单、处理状态 | 对账集成测试 | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-sprint-01.md` |
| PAY-RECON-003 | 设计文档 7.4、13 | 差异处理受控闭环 | 差异处理必须记录动作、原因、处理人、时间和凭据 | 处理接口、审计、附件引用 | E2E、审计检查 | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-sprint-01.md` |
| PAY-SETTLE-001 | 设计文档 7.5 | 结算汇总报表 | 按日、租户、应用、主体、通道汇总支付、退款、手续费、净收款 | 汇总服务、汇总页、导出 | 汇总计算测试、E2E | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-sprint-01.md` |
| PAY-SETTLE-002 | 设计文档 7.5 | 未处理差异不允许确认汇总 | 汇总确认前校验差异状态 | 确认接口、校验规则、审计 | 集成测试、E2E | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-sprint-01.md` |
| PAY-OPS-001 | 设计文档 13 | 后台人工操作受控 | 人工关单、通知重推、主动查单、异常补单、退款审批、差异确认、汇总确认均需权限和审计 | 操作接口、权限点、审计记录 | 权限测试、审计测试 | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-production-readiness.md` |
| PAY-OPS-002 | 设计文档 13.2 | 禁止后台直接改成功状态 | 所有成功状态必须有通道、查单或账单依据 | 服务校验、受控动作、测试 | 安全测试、代码审查 | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-production-readiness.md` |
| PAY-SEC-001 | 设计文档 15 | 敏感字段加密和脱敏 | appSecret、证书、私钥、银行账号、证件号等加密存储和脱敏展示 | 加密服务接入、脱敏组件、测试 | 安全检查、接口测试 | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-production-readiness.md` |
| PAY-SEC-002 | 设计文档 14、15 | 日志不得打印敏感信息 | 支付日志只记录摘要信息 | 日志规范落地、日志扫描 | 自动扫描、代码审查 | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-production-readiness.md` |
| PAY-OBS-001 | 设计文档 16 | 可观测性 | 记录摘要日志、链路追踪、关键指标和告警 | 指标、日志、链路、告警规则 | 运维检查、压测观察 | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-production-readiness.md` |
| PAY-SCHED-001 | 设计文档 16、17 | 调度由人工触发或平台统一调度能力触发 | 支付模块不自建调度框架，提供可被调度调用的任务入口 | 任务接口、触发记录、权限 | 集成测试、架构检查 | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-production-readiness.md` |
| PAY-TEST-001 | PMO 交付规则 | 单元测试覆盖核心状态机和金额规则 | 状态机、金额、签名、通道路由、返回码映射必须有单元测试 | 单元测试集合 | 测试命令通过 | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-production-readiness.md` |
| PAY-TEST-002 | PMO 交付规则 | 集成测试覆盖 API、数据库约束和并发 | 覆盖创建订单、支付、退款、回调、查单、通知、对账、结算 | 集成测试集合 | 测试命令通过 | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-production-readiness.md` |
| PAY-TEST-003 | 用户要求 | E2E 覆盖支付中心和收银台流程 | 覆盖菜单可见、各列表页、收银台行操作、沙箱支付、结果展示 | Playwright E2E | E2E 命令通过、截图证据 | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-production-readiness.md` |
| PAY-TEST-004 | PMO 交付规则 | 交付前检查台账 | 使用 PMO 脚本检查台账完整性，未完成项不为 0 不声明完成 | 检查命令和输出 | `delivery-contract-check` 通过 | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-production-readiness.md` |
| PAY-CONFIRM-001 | 设计文档 21 | 线下转账是否纳入第一阶段、是否需要自动认款需用户确认 | 未确认前不实现自动认款；可实现为用户确认后的明确范围 | 用户确认记录、设计文档更新 | 用户确认后更新台账状态 | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-sprint-01.md` |
| PAY-CONFIRM-002 | 设计文档 21、用户追加要求 | 除自建沙箱、通联、华夏银行外，其他已知支付通道清单和优先接入顺序需用户确认 | 自建沙箱、通联、华夏银行已明确纳入；微信、支付宝、连连及其他已知通道按确认清单推进 | 用户确认记录、设计文档更新 | 用户确认后更新台账状态 | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-sprint-01.md` |
| PAY-CONFIRM-003 | 设计文档 21 | 支付成功后是否通知业务系统触发开票需用户确认 | 支付系统只发支付结果通知，不处理开票；是否增加事件字段需确认 | 用户确认记录、接口文档更新 | 用户确认后更新台账状态 | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-sprint-01.md` |
| PAY-CONFIRM-004 | 设计文档 21 | 租户定义需用户确认 | 未确认前按设计保留租户隔离模型，不假设仅内部或仅外部 | 用户确认记录、租户模型更新 | 用户确认后更新台账状态 | IN_PROGRESS | `mango-docs/plans/2026-05-25-payment-sprint-01.md` |
