# 支付 Payment

## 1. 概览

`mango-payment` 是 Mango 的统一支付中心，提供支付应用、企业主体、支付方式、支付通道、通道签约、收银台、业务订单、支付订单、退款、通知、对账、结算、线下收款和异常处理能力。

业务开发接入时分三类：

- 后端只调用支付能力时，依赖 `mango-payment-api`。
- 支付中心应用需要提供管理接口、收银台、开放接口、通道回调和调度能力时，启用 `mango-payment-starter`。
- 管理后台或收银台页面使用 `@mango/payment`。其中管理页面属于 `admin-pages`，收银台组件 `PaymentCashier` 可用于 Mango 后台或业务支付页，但依赖 Mango 支付接口和请求封装。

## 2. 功能清单

| 能力 | 说明 | 常用入口 |
|------|------|----------|
| 支付应用 | 给业务系统分配 `appId`、密钥、签名算法、白名单和通知策略 | `PaymentApplicationApi`、`/payment/applications` |
| 企业主体 | 维护收款主体、银行账户、执照文件和主体状态 | `PaymentEnterpriseSubjectApi`、`/payment/enterprise-subjects` |
| 支付方式 | 维护微信、支付宝、网银、线下转账等方式及收银台展示分组 | `PaymentMethodApi`、`/payment/methods` |
| 支付通道 | 维护通道、能力矩阵、签约字段模板和账单获取方式 | `PaymentChannelApi`、`PaymentChannelContractApi` |
| 收银台 | 按应用、主体、支付方式和路由规则生成付款人可用支付入口 | `PaymentCashierApi`、`PaymentCashier` |
| 业务订单 | 创建业务支付单，承载金额、标题、过期时间、通知地址和返回地址 | `PaymentBusinessOrderApi`、`PaymentOpenApi` |
| 支付订单 | 发起支付、查单、同步状态、生成支付流水 | `PaymentOrderApi` |
| 退款 | 发起退款审批、生成退款订单、查询通道退款状态 | `PaymentRefundApprovalApi`、`PaymentRefundOrderApi` |
| 通知 | 记录并补偿投递支付结果、退款结果等业务通知 | `PaymentNotificationRecordApi` |
| 对账 | 导入通道账单、获取账单、生成本地核对批次和差异 | `PaymentReconciliationApi`、`PaymentDifferenceApi` |
| 结算 | 生成、确认、作废结算汇总 | `PaymentSettlementSummaryApi` |
| 线下收款 | 提交转账凭证、导入银行流水、确认匹配、发起线下退款 | `PaymentOfflineCollectionApi` |
| 异常处理 | 查看重复支付、状态不一致、通知失败等异常单并处理 | `PaymentExceptionOrderApi` |
| 观测和安全 | 查看支付健康快照，重加密敏感字段 | `PaymentObservabilityApi`、`PaymentSecurityApi` |

## 3. 后端接入

业务模块调用支付接口时依赖 API：

```xml
<dependency>
    <groupId>io.mango.platform.payment</groupId>
    <artifactId>mango-payment-api</artifactId>
</dependency>
```

支付中心服务端应用启用 starter：

```xml
<dependency>
    <groupId>io.mango.platform.payment</groupId>
    <artifactId>mango-payment-starter</artifactId>
</dependency>
```

微服务调用方只需要远程调用装配时依赖 remote starter：

```xml
<dependency>
    <groupId>io.mango.platform.payment</groupId>
    <artifactId>mango-payment-starter-remote</artifactId>
</dependency>
```

常用后端 API：

| API | 用法 |
|-----|------|
| `PaymentOpenApi` | 面向业务系统创建订单、查询订单、获取收银台、支付、退款、查询退款和获取支付凭证。 |
| `PaymentBusinessOrderApi` | 后台创建业务订单、分页查询、查看详情和订单状态字典。 |
| `PaymentCashierApi` | 查询收银台会话、提交支付、查询支付结果、同步支付结果、提交线下转账凭证。 |
| `PaymentOrderApi` | 分页查询支付订单、查看详情、查询状态字典、主动同步通道状态。 |
| `PaymentRefundApprovalApi` | 创建退款审批、查询审批列表和状态。 |
| `PaymentRefundOrderApi` | 查询退款订单、主动查询通道退款状态。 |
| `PaymentReconciliationApi` | 导入对账、生成虚拟通道账单、本地订单核对、账单源和账单获取批次。 |
| `PaymentNotificationRecordApi` | 查询通知记录、重试通知、投递到期通知。 |
| `PaymentTaskApi` | 处理超时未支付订单、查询处理中订单，通常由任务调度调用。 |

## 4. 前端接入

管理后台接入页面和 API：

```ts
import {
  PaymentApplicationView,
  PaymentChannelView,
  PaymentChannelContractView,
  PaymentCashierConfigView,
  PaymentBusinessOrderView,
  PaymentOrderView,
  PaymentRefundOrderView,
  PaymentReconciliationView,
  PaymentDifferenceView,
  PaymentCashier,
  paymentApi,
} from '@mango/payment';
import '@mango/payment/style.css';
```

自动登记支付管理页面时，从 `@mango/payment` 的 `admin-pages` 子入口导入 `registerMangoPaymentAdminPages`，然后在管理后台启动阶段调用一次。

前端能力标识：

| 标识 | 内容 |
|------|------|
| `admin-pages` | 支付应用、主体、通道、签约、支付方式、收银台配置、订单、退款、对账、结算、审计等管理页面。 |
| `business-component` | `PaymentCashier`，用于展示支付方式、二维码、跳转材料、支付结果和线下转账凭证提交。 |
| `api-client` | `paymentApi` 及类型定义，封装 `/payment/**`、`/openapi/pay/**` 和收银台接口。 |

`@mango/payment` 依赖 `@mango/admin-pages`、`@mango/common`、`@mango/file`、`@mango/api-schema`、`element-plus`、`vue`、`vue-router` 和 `qrcode`。

## 5. 快速开始

1. 支付中心应用引入 `mango-payment-starter`，确认 `payment` migration 已执行。
2. 后台创建或确认支付应用，拿到 `appId` 和应用密钥；开放接口调用方使用它做签名。
3. 创建企业主体，并维护主体银行账户、执照文件等资料。
4. 启用支付方式、支付通道、通道能力和签约配置；签约字段里维护商户号、应用号、证书文件、密钥引用、网关地址和回调地址。
5. 配置收银台，绑定应用、主体、可用支付方式、默认支付方式、展示顺序、返回地址和收银台路径。
6. 配置路由规则，按应用、主体、支付方式、终端和金额命中签约能力。
7. 业务系统通过 `PaymentOpenApi.createOrder` 或 `POST /openapi/pay/orders` 创建业务订单，再通过收银台或开放支付接口发起支付。
8. 支付成功后通过通道回调、主动查单或通知补偿推进状态；业务系统接收通知后回写自己的业务单据状态。

## 6. 配置说明

YAML 配置前缀：`mango.payment`。

| 配置项 | 默认值 | 含义 |
|--------|--------|------|
| `enabled` | `true` | 是否启用支付自动配置。关闭后不注册 payment mapper、service、controller 和 workflow 初始化器。 |
| `notification.dispatch.enabled` | `true` | 是否启用支付通知补偿调度器。 |
| `notification.dispatch.interval-millis` | `60000` | 通知补偿调度间隔。 |
| `notification.dispatch.initial-delay-millis` | `30000` | 应用启动后首次调度延迟。 |
| `notification.dispatch.tenant-limit` | `20` | 单轮最多扫描的租户数量。 |
| `notification.dispatch.batch-size` | `20` | 单轮每租户最多处理的通知数量。 |
| `observability.channel-failure-rate-threshold` | `0.1000` | 通道失败率告警阈值。 |
| `observability.payment-success-rate-minimum` | `0.9500` | 支付成功率最低阈值。 |
| `observability.refund-success-rate-minimum` | `0.9500` | 退款成功率最低阈值。 |
| `observability.payment-backlog-threshold` | `100` | 支付积压告警阈值。 |
| `observability.callback-failure-threshold` | `1` | 回调失败告警阈值。 |
| `observability.notification-failure-threshold` | `1` | 通知失败告警阈值。 |
| `observability.refund-failure-threshold` | `1` | 退款失败告警阈值。 |
| `observability.difference-threshold` | `1` | 对账差异告警阈值。 |
| `observability.unhandled-exception-threshold` | `1` | 未处理异常单告警阈值。 |
| `observability.expiring-certificate-threshold` | `1` | 即将过期证书数量告警阈值。 |
| `observability.certificate-warning-days` | `30` | 证书到期提前预警天数。 |
| `workflow.refund-approval.initializer.enabled` | `false` | 是否启动退款审批流程定义初始化。 |
| `workflow.refund-approval.initializer.system-tenant-id` | 空 | 初始化流程定义使用的系统租户 ID。 |
| `workflow.refund-approval.initializer.system-user-id` | 空 | 初始化流程定义使用的系统用户 ID。 |
| `workflow.refund-approval.initializer.principal-name` | 空 | 初始化流程定义使用的操作者名称。 |
| `workflow.refund-approval.initializer.realm` | `INTERNAL` | 初始化上下文登录域。 |
| `workflow.refund-approval.initializer.actor-type` | `INTERNAL_USER` | 初始化上下文操作者类型。 |
| `workflow.refund-approval.initializer.party-type` | `INTERNAL_ORG` | 初始化上下文主体类型。 |
| `workflow.refund-approval.initializer.party-id` | 空 | 初始化上下文主体 ID。 |
| `workflow.refund-approval.initializer.app-code` | 空 | 初始化上下文应用编码。 |

示例：

```yaml
mango:
  payment:
    enabled: true
    notification:
      dispatch:
        enabled: true
        interval-millis: 60000
        batch-size: 20
    workflow:
      refund-approval:
        initializer:
          enabled: true
          system-tenant-id: 1
          system-user-id: 1
          principal-name: system
          app-code: internal-admin
```

运行时配置主要在数据库里维护：支付应用密钥、通道能力、签约配置、收银台配置、路由规则、账单获取源和证书文件引用。

## 7. API 与扩展

管理端常用接口：

| 能力 | 接口 |
|------|------|
| 支付应用 | `GET /payment/applications/page`、`GET /payment/applications/detail`、`POST /payment/applications`、`PUT /payment/applications`、`DELETE /payment/applications` |
| 企业主体 | `GET /payment/enterprise-subjects/page`、`GET /payment/enterprise-subjects/detail`、`POST /payment/enterprise-subjects`、`PUT /payment/enterprise-subjects`、`DELETE /payment/enterprise-subjects` |
| 支付通道 | `GET /payment/channels/page`、`GET /payment/channels/detail`、`POST /payment/channels`、`PUT /payment/channels`、`DELETE /payment/channels`、`GET /payment/channels/capabilities/page` |
| 通道签约 | `GET /payment/channel-contracts/page`、`GET /payment/channel-contracts/detail`、`POST /payment/channel-contracts`、`PUT /payment/channel-contracts`、`DELETE /payment/channel-contracts` |
| 证书管理 | `GET /payment/channel-contracts/certificates/expiring`、`POST /payment/channel-contracts/certificates/rotate` |
| 支付方式 | `GET /payment/methods/page`、`GET /payment/methods/categories`、`GET /payment/methods/detail`、`POST /payment/methods`、`PUT /payment/methods`、`DELETE /payment/methods` |
| 收银台配置 | `GET /payment/cashier-configs/page`、`GET /payment/cashier-configs/detail`、`POST /payment/cashier-configs`、`PUT /payment/cashier-configs`、`DELETE /payment/cashier-configs` |
| 业务订单 | `GET /payment/business-orders/page`、`GET /payment/business-orders/detail`、`GET /payment/business-orders/statuses`、`POST /payment/business-orders` |
| 支付订单 | `GET /payment/payment-orders/page`、`GET /payment/payment-orders/detail`、`GET /payment/payment-orders/statuses`、`POST /payment/payment-orders/sync-status` |
| 退款审批 | `GET /payment/refund-approvals/page`、`GET /payment/refund-approvals/detail`、`GET /payment/refund-approvals/statuses`、`POST /payment/refund-approvals` |
| 退款订单 | `GET /payment/refund-orders/page`、`GET /payment/refund-orders/detail`、`GET /payment/refund-orders/statuses`、`POST /payment/refund-orders/query-channel` |
| 交易流水 | `GET /payment/transaction-flows/page`、`GET /payment/transaction-flows/detail` |
| 通知记录 | `GET /payment/notification-records/page`、`GET /payment/notification-records/detail`、`GET /payment/notification-records/statuses`、`POST /payment/notification-records/retry` |
| 对账 | `GET /payment/reconciliations/page`、`GET /payment/reconciliations/detail`、`GET /payment/reconciliations/statuses`、`POST /payment/reconciliations/import` |
| 差异处理 | `GET /payment/differences/page`、`GET /payment/differences/detail`、`GET /payment/differences/statuses`、`GET /payment/differences/actions`、`POST /payment/differences/handle` |
| 结算汇总 | `GET /payment/settlement-summaries/page`、`GET /payment/settlement-summaries/detail`、`GET /payment/settlement-summaries/statuses`、`POST /payment/settlement-summaries/generate`、`POST /payment/settlement-summaries/confirm`、`POST /payment/settlement-summaries/void` |
| 观测 | `GET /payment/observability/snapshot` |
| 安全 | `POST /payment/security/sensitive-fields/reencrypt` |
| 调度任务 | `POST /payment/tasks/expire-open-orders`、`POST /payment/tasks/query-processing-orders` |

付款人和开放接口：

| 能力 | 接口 |
|------|------|
| 收银台会话 | `GET /payment/cashier/session` |
| 收银台支付 | `POST /payment/cashier/pay` |
| 支付结果 | `GET /payment/cashier/pay-result` |
| 同步支付结果 | `POST /payment/cashier/pay-result/sync` |
| 提交线下转账凭证 | `POST /payment/cashier/offline-collections/transfer-voucher` |
| 创建开放订单 | `POST /openapi/pay/orders` |
| 查询开放订单 | `GET /openapi/pay/orders/{bizOrderNo}` |
| 获取开放收银台 | `POST /openapi/pay/orders/{bizOrderNo}/cashier` |
| 开放支付 | `POST /openapi/pay/orders/{bizOrderNo}/pay` |
| 查询支付订单 | `GET /openapi/pay/payment-orders/{payOrderNo}` |
| 开放退款 | `POST /openapi/pay/refunds` |
| 查询退款 | `GET /openapi/pay/refunds/{bizRefundNo}` |
| 获取支付凭证 | `GET /openapi/pay/receipts/{bizOrderNo}` |

常用入参对象：

| 对象 | 用途 |
|------|------|
| `CreatePaymentBusinessOrderCommand` | 后台创建业务订单。 |
| `PaymentOpenRequestCommand` | 开放接口统一请求壳，承载应用、签名、时间戳、随机串和业务 payload。 |
| `PaymentCashierPayCommand` | 收银台提交支付方式、订单、终端和付款材料。 |
| `CreatePaymentRefundApprovalCommand` | 发起退款审批。 |
| `QueryPaymentRefundOrderCommand` | 主动查询通道退款结果。 |
| `ImportPaymentReconciliationCommand` | 导入对账批次。 |
| `HandlePaymentDifferenceCommand` | 处理对账差异。 |
| `ConfirmOfflineCollectionCommand` | 确认线下收款。 |
| `SubmitOfflineTransferVoucherCommand` | 付款人提交线下转账凭证。 |

接入新支付通道时，通常需要补齐：

1. migration 固化通道、通道能力、签约字段模板、默认签约、路由规则和必要权限。
2. 通道适配器实现支付、查单、退款、查退款、关单、账单获取或账单解析。
3. 回调入口接入 `PaymentChannelCallbackApi`，提交通道回调命令。
4. 对账接入账单获取源或文件导入。
5. 记录请求摘要、响应摘要、通道交易号、通道退款号和错误原因，保证管理端能定位问题。

不要用固定成功、mock 报文或手工改库替代真实支付、回调、查单、退款和对账链路。

## 8. 数据与初始化

Flyway 路径：`mango-payment-core/src/main/resources/db/migration/payment`。

核心表按用途分组：

| 分组 | 表 |
|------|----|
| 基础配置 | `payment_application`、`payment_enterprise_subject`、`payment_subject_bank_account`、`payment_method`、`payment_method_category` |
| 通道和签约 | `payment_channel`、`payment_channel_capability`、`payment_channel_field_template`、`payment_channel_contract`、`payment_channel_contract_value`、`payment_channel_contract_capability`、`payment_channel_certificate_rotation_record` |
| 收银台和路由 | `payment_cashier_config`、`payment_method_route_rule`、`payment_method_route_rule_item` |
| 订单和流水 | `payment_business_order`、`payment_order`、`payment_refund_order`、`payment_transaction_flow`、`payment_order_status_flow` |
| 通知和异常 | `payment_notification_record`、`payment_exception_order`、`payment_operation_audit` |
| 对账和结算 | `payment_reconciliation`、`payment_difference`、`payment_settlement_summary`、`payment_channel_bill_detail`、`payment_channel_bill_source`、`payment_channel_bill_fetch_batch` |
| 线下收款 | `payment_offline_collection`、`payment_offline_collection_voucher`、`payment_offline_bank_statement_batch`、`payment_offline_bank_statement_item`、`payment_offline_collection_match`、`payment_offline_refund_process` |
| 开放接口和虚拟通道 | `payment_openapi_nonce`、`payment_virtual_channel_payment`、`payment_mango_pay_scenario_control` |

初始化内容包括：

- 支付基础表、通道能力、支付方式分类、收银台配置、路由规则、虚拟通道、富友通道配置和线下收款相关表。
- 支付编号规则依赖 `mango-numgen`，通过 `SEQUENCE_RULE` 资源注入；不要在业务代码或前端拼接订单号。
- 支付昨日账单拉取任务依赖 `mango-job`，通过 `JOB_DEFINITION` 资源注入。
- 支付菜单、页面资源和按钮权限由 `mango-authorization` 的资源能力维护。
- `module.properties` 登记 `module-name=mango-payment`、`module-path=/payment`，供模块资源扫描使用。
- `PaymentRefundApprovalWorkflowDefinitionInitializer` 是应用启动时的退款审批流程定义初始化入口；只有开启 `workflow.refund-approval.initializer.enabled` 并补齐系统租户、用户和操作者配置后才会执行。

资源文件：

```text
mango-payment-starter/src/main/resources/META-INF/mango/resources/payment-common-domain.yml
mango-payment-starter/src/main/resources/META-INF/mango/resources/payment-common-numgen.yml
mango-payment-starter/src/main/resources/META-INF/mango/resources/payment-common-job.yml
```

支持类型：

| 资源类型 | 目标模块 | 说明 |
|----------|----------|------|
| `BUSINESS_DOMAIN` | `domain` | 登记支付业务域 |
| `SEQUENCE_RULE` | `numgen` | 登记支付编号规则 |
| `JOB_DEFINITION` | `job` | 登记支付账单拉取任务 |

平台自生成编号统一通过 `mango-numgen`：

| 业务对象 | 字段 | genKey | 前缀 |
|----------|------|--------|------|
| 业务订单 | `biz_order_no` | `PAY_BIZ_ORDER_NO` | `BO` |
| 支付订单 | `pay_order_no` | `PAY_ORDER_NO` | `PO` |
| 退款订单 | `refund_order_no` | `PAY_REFUND_ORDER_NO` | `RO` |
| 业务退款号 | `biz_refund_no` | `PAY_BIZ_REFUND_NO` | `BR` |
| 退款审批 | `approval_no` | `PAY_REFUND_APPROVAL_NO` | `RA` |
| 支付流水 | `flow_no` | `PAY_FLOW_NO` | `PF` |
| 退款流水 | `flow_no` | `PAY_REFUND_FLOW_NO` | `RF` |
| 通知记录 | `notification_no` | `PAY_NOTIFY_NO` | `NT` |
| 对账批次 | `reconciliation_no` / `batch_no` | `PAY_RECON_BATCH_NO` | `RC` |
| 对账差异 | `difference_no` | `PAY_DIFF_NO` | `DF` |
| 异常订单 | `exception_no` | `PAY_EXCEPTION_NO` | `EX` |
| 线下收款单 | `offline_collection_no` | `PAY_OFFLINE_COLLECTION_NO` | `OC` |
| 线下退款单 | `offline_refund_no` | `PAY_OFFLINE_REFUND_NO` | `OF` |
| 芒果支付虚拟付款单 | `virtual_payment_no` | `PAY_MANGO_VIRTUAL_NO` | `MP` |

`channel_trade_no` 和 `channel_refund_no` 以外部通道返回为准，不由平台生成。

## 9. 管理入口

前端页面 key 由支付管理页面注册函数 `registerMangoPaymentAdminPages` 注册：

| 页面 | page key |
|------|----------|
| 支付应用 | `payment/applications/index` |
| 企业主体 | `payment/enterprise-subjects/index` |
| 通道签约 | `payment/channel-contracts/index` |
| 支付通道 | `payment/channels/index` |
| 支付方式 | `payment/methods/index` |
| 收银台配置 | `payment/cashier-configs/index` |
| 业务订单 | `payment/business-orders/index` |
| 支付订单 | `payment/payment-orders/index` |
| 线下收款 | `payment/offline-collections/index` |
| 线下退款 | `payment/offline-refunds/index` |
| 退款订单 | `payment/refund-orders/index` |
| 退款审批 | `payment/refund-approvals/index` |
| 交易流水 | `payment/transaction-flows/index` |
| 异常订单 | `payment/exception-orders/index` |
| 通知记录 | `payment/notification-records/index` |
| 对账批次 | `payment/reconciliations/index` |
| 对账差异 | `payment/differences/index` |
| 结算汇总 | `payment/settlement-summaries/index` |
| 操作审计 | `payment/operation-audits/index` |
| 收银台 | `payment/cashier/index` |
| 支付结果 | `payment/gateway-result/index` |

权限码按资源命名，例如 `payment:application:list`、`payment:channel-contract:add`、`payment:payment-order:sync-status`、`payment:difference:handle`、`payment:task:expire-open-orders`。新增页面或按钮时，菜单、页面 key 和权限码需要在授权资源中保持一致。

## 10. 问题排查

- 收银台没有支付方式：检查收银台配置的 `methodCodes`、企业主体、签约能力、路由规则、金额区间和支付方式状态。
- 支付通道调用失败：检查签约字段、商户号、应用号、证书文件、网关地址、回调地址和适配器错误摘要。
- 回调没有推进订单状态：检查公网回调地址、通道回调路由、验签结果、通道交易号、幂等记录和支付订单状态。
- 业务系统没收到通知：检查 `payment_notification_record` 状态、通知地址、应用通知策略和 `mango.payment.notification.dispatch.*` 是否启用。
- 退款发起不了：检查支付订单是否可退、可退金额、退款审批状态、通道是否支持退款。
- 对账没有数据：检查账单获取源、账单获取批次、导入文件、通道交易号和支付流水。
- 编号异常：检查对应 `genKey` 是否初始化、租户上下文是否正确，不要在业务侧自行拼接编号。
- 证书即将过期没有提醒：检查签约能力的 `certificateExpireTime` 和 `observability.certificate-warning-days`。

## 11. 相关文档

- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
- [文档资产边界](../../../mango-pmo/rules/06-document-assets.md)
