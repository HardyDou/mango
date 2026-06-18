# @mango/payment

## 1. 概览

`@mango/payment` 是 Mango 支付中心前端包，提供支付配置、支付运营、退款、线下收款、通知、对账、差异、结算、操作审计和收银台页面，并导出支付 API 封装。

集成形态：

| 标识 | 说明 |
|------|------|
| `admin-pages` | 支付中心管理页面，注册到 Mango Admin 菜单。 |
| `business-component` | `PaymentCashier` 可嵌入后台业务页面发起支付。 |
| `api-client` | 支付配置、订单、退款、收银台、通知、对账、结算等 API 封装。 |

这个包不实现支付通道签名、验签、回调、账单拉取和状态推进；这些由后端 `mango-payment` 和通道适配完成。它也不是 C 端商城或官网收银台的通用皮肤，面向 Mango Admin 和后台业务页面。

## 2. 功能清单

| 能力 | 使用入口 | 说明 |
|------|----------|------|
| 支付基础配置 | 支付应用、企业主体、通道、通道签约、支付方式、路由规则页面 | 给支付管理员维护支付基础数据。 |
| 收银台配置 | 收银台配置页、`PaymentCashier` | 配置可用主体、支付方式、展示和结果回跳。 |
| 业务订单 | 业务订单页、`paymentBusinessOrderApi` | 创建、查询业务支付订单，并拉起收银台。 |
| 支付订单 | 支付订单页、`paymentOrderApi` | 查询支付单，手动同步支付状态。 |
| 线下收款 | 线下收款页、银行流水导入和确认 API | 导入银行流水、匹配和确认线下收款。 |
| 退款 | 退款订单、退款审批、线下退款页面 | 查询退款、创建退款审批、查询通道退款。 |
| 通知 | 通知记录页、`paymentNotificationRecordApi` | 查询、重试和投递到期通知。 |
| 对账和差异 | 对账、差异、结算页面 | 导入账单、拉取账单、处理差异、生成结算。 |
| 操作审计 | 操作审计页 | 查询支付中心高风险操作记录。 |

## 3. 接入方式

开发依赖：

```bash
pnpm add @mango/payment
```

宿主应用还需要提供 Vue、Vue Router、Element Plus，以及 Mango 管理端常用依赖 `@mango/common`、`@mango/admin-pages`、`@mango/file`。部署时后端必须启用 `mango-payment`，并按实际能力启用 authorization、file、numgen、workflow、job 等依赖模块。

注册支付管理页面：

```ts
import { registerMangoPaymentAdminPages } from '@mango/payment/admin-pages';
import '@mango/payment/style.css';

registerMangoPaymentAdminPages();
```

使用 Mango 单体 full preset 时，`@mango/admin/full` 会注册 Payment 管理页面，`@mango/admin/style-full.css` 会带入 Payment 样式；业务项目不需要再单独 import `@mango/payment/style.css`。

嵌入收银台组件：

```vue
<script setup lang="ts">
import { PaymentCashier } from '@mango/payment';
import '@mango/payment/style.css';
</script>

<template>
  <PaymentCashier
    cashier-config-id="1001"
    business-order-id="9001"
    embedded
    @success="handlePaid"
    @close="handleClose"
  />
</template>
```

调用 API：

```ts
import {
  paymentBusinessOrderApi,
  paymentCashierApi,
  paymentOrderApi,
} from '@mango/payment';

const order = await paymentBusinessOrderApi.create(command);
const session = await paymentCashierApi.session(cashierConfigId, order.id);
await paymentOrderApi.syncStatus(payOrderNo);
```

## 4. 配置说明

前端包没有独立 Vite 环境变量。配置来自宿主请求层、页面注册、组件 props 和后端 payment 数据。

| 配置位置 | 字段 | 含义 |
|----------|------|------|
| 宿主应用 | API baseURL / 代理 | 决定 `/payment/**` 请求转发到哪个后端服务。 |
| `registerMangoPaymentAdminPages()` | 无入参 | 幂等注册支付中心页面 key。 |
| `PaymentCashier` | `cashierConfigId` | 收银台配置 id；支付时用于读取可用主体、方式和展示配置。 |
| `PaymentCashier` | `businessOrderId` | 业务订单 id；提交支付时应能关联到后端业务订单。 |
| `PaymentCashier` | `embedded` | 嵌入业务页面时使用，成功触发 `success`，关闭触发 `close`。 |
| 通道管理页 | `fieldTemplateJson` | 通道签约字段模板，决定签约配置表单。 |
| 通道签约页 | `configValuesJson` | 按字段模板保存的签约配置值。 |
| 收银台配置页 | `resultReturnUrl` | 支付完成后的结果页或业务回跳地址。 |

`PaymentCashier` 展示通道 logo 和线下转账凭证时会使用 `@mango/file`；二维码展示依赖 `qrcode`。

## 5. API 与扩展

页面注册入口：

| 导出 | 用途 |
|------|------|
| `registerMangoPaymentAdminPages()` | 注册 `mango-payment` 模块下的所有支付页面。 |

页面 key：

| 页面 key | 管理能力 |
|----------|----------|
| `payment/applications/index` | 支付应用管理。 |
| `payment/enterprise-subjects/index` | 企业主体管理。 |
| `payment/channels/index` | 支付通道和通道能力管理。 |
| `payment/channel-contracts/index` | 通道签约、签约配置、账单来源管理。 |
| `payment/methods/index` | 支付方式和路由规则管理。 |
| `payment/cashier-configs/index` | 收银台配置管理。 |
| `payment/business-orders/index` | 业务订单查询、创建和拉起收银台。 |
| `payment/payment-orders/index` | 支付订单查询和状态同步。 |
| `payment/offline-collections/index` | 线下收款、银行流水导入和匹配确认。 |
| `payment/offline-refunds/index` | 线下退款查询。 |
| `payment/refund-orders/index` | 退款订单查询和通道查询。 |
| `payment/refund-approvals/index` | 退款审批创建和查询。 |
| `payment/transaction-flows/index` | 交易流水查询。 |
| `payment/exception-orders/index` | 异常订单处理。 |
| `payment/notification-records/index` | 通知记录、重试和到期投递。 |
| `payment/reconciliations/index` | 对账导入、虚拟账单生成和通道账单拉取。 |
| `payment/differences/index` | 对账差异处理。 |
| `payment/settlement-summaries/index` | 结算汇总生成、确认和作废。 |
| `payment/operation-audits/index` | 支付操作审计查询。 |
| `payment/cashier/index` | 管理端收银台页面。 |
| `payment/gateway-result/index` | 支付网关结果页。 |

核心 API 封装：

| API | 主要接口 | 能力 |
|-----|----------|------|
| `paymentApplicationApi` | `/payment/applications` | 支付应用 CRUD，创建或更新会返回 appId 和密钥生成结果。 |
| `paymentEnterpriseSubjectApi` | `/payment/enterprise-subjects` | 企业主体 CRUD。 |
| `paymentChannelApi` | `/payment/channels` | 支付通道 CRUD。 |
| `paymentChannelContractApi` | `/payment/channel-contracts` | 通道签约和账单来源维护。 |
| `paymentMethodApi` | `/payment/methods` | 支付方式 CRUD 和分类查询。 |
| `paymentMethodRouteApi` | `/payment/method-routes` | 路由规则 CRUD 和试算。 |
| `paymentCashierConfigApi` | `/payment/cashier-configs` | 收银台配置 CRUD。 |
| `paymentBusinessOrderApi` | `/payment/business-orders` | 业务订单 CRUD 和状态枚举。 |
| `paymentOrderApi` | `/payment/payment-orders` | 支付订单 CRUD、状态枚举和状态同步。 |
| `paymentOfflineCollectionApi` | `/payment/offline-collections` | 线下收款查询、确认、退款、银行流水导入和匹配。 |
| `paymentRefundOrderApi` | `/payment/refund-orders` | 退款订单 CRUD、状态枚举和通道查询。 |
| `paymentRefundApprovalApi` | `/payment/refund-approvals` | 退款审批查询和创建。 |
| `paymentNotificationRecordApi` | `/payment/notification-records` | 通知记录 CRUD、重试和到期投递。 |
| `paymentReconciliationApi` | `/payment/reconciliations` | 对账、账单导入、虚拟账单生成、账单来源和拉取批次。 |
| `paymentDifferenceApi` | `/payment/differences` | 差异查询、动作枚举和处理。 |
| `paymentSettlementSummaryApi` | `/payment/settlement-summaries` | 结算生成、确认和作废。 |
| `paymentCashierApi` | `/payment/cashier/session`、`/payment/cashier/pay` | 收银台会话、支付、结果查询、结果同步、线下凭证提交。 |

常用返回字段：

| 数据 | 字段 |
|------|------|
| 分页结果 | `list`、`total`、`pageNum`、`pageSize` |
| 支付应用保存结果 | `id`、`appId`、`appSecret`、`secretGenerated` |
| 业务订单 | `id`、`bizOrderNo`、`amount`、`paidAmount`、`status`、`payable`、`payDisabledReason` |
| 支付方式 | `methodCode`、`methodName`、`interactionType`、`terminalScope`、`iconFileId`、`status` |
| 收银台会话 | 收银台配置、业务订单、可用支付方式和展示数据 |

## 6. 数据与初始化

`@mango/payment` 不包含数据库 migration。页面有数据的前提是后端已经完成初始化和业务运行写入。

| 数据 | 来源 | 前端消费 |
|------|------|----------|
| 支付应用、主体、通道、签约、方式、收银台配置 | `mango-payment` migration 和管理页面维护 | 配置类页面、收银台会话。 |
| 业务订单、支付订单、退款、通知、对账、差异、结算 | `mango-payment` 运行数据 | 运营查询和处理页面。 |
| 菜单和权限 | `mango-authorization` | 支付中心菜单、按钮和接口权限。 |
| 文件和图片 | `mango-file` | 通道 logo、主体证照、线下凭证。 |
| 单号规则 | `mango-numgen` | 支付单、退款单、对账单、结算单编号。 |
| 退款审批 | `mango-workflow` | 退款审批创建和流转。 |
| 定时任务 | `mango-job` 或 payment 后端任务 | 通知重试、对账拉取、状态同步。 |

## 7. 管理入口

菜单的 component 必须使用上面的页面 key。前端只负责页面注册和展示，最终访问控制由后端登录态、租户、角色、按钮权限和 payment 接口校验。

建议单独授权的高风险入口：

| 入口 | 原因 |
|------|------|
| 企业主体、通道签约 | 可能涉及商户号、密钥、证照和银行账户。 |
| 收银台配置、支付路由 | 会影响真实支付链路。 |
| 退款审批、退款订单 | 影响资金流出。 |
| 通知重试、差异处理、结算确认 | 会改变业务账务状态。 |
| 操作审计 | 可能暴露敏感操作记录。 |

## 8. 快速开始

1. 后端启用 `mango-payment`，准备支付应用、企业主体、通道、签约、支付方式和收银台配置。
2. 前端安装 `@mango/payment`，引入样式，并调用 `registerMangoPaymentAdminPages()`。
3. 在 authorization 中把支付菜单 component 配成对应页面 key。
4. 业务创建订单后拿到 `businessOrderId`，用 `PaymentCashier` 或 `payment/cashier/index` 打开收银台。
5. 支付完成后调用 `paymentCashierApi.payResult()` 或 `paymentOrderApi.syncStatus()` 查询状态。
6. 按权限验证退款、通知、对账、差异和结算页面。

## 9. 问题排查

| 问题 | 常见原因 | 处理方式 |
|------|----------|----------|
| 支付菜单打开空白 | 没调用 `registerMangoPaymentAdminPages()` 或 component key 不一致 | 对照页面 key 和注册入口。 |
| `/payment/**` 404 | 后端未启用 payment 或代理没转发 | 检查后端服务和宿主 API baseURL。 |
| 收银台没有支付方式 | 收银台配置、支付方式、签约能力或路由规则不可用 | 查收银台会话返回和后端 payment 日志。 |
| 支付后状态不变 | 通道回调未到、通知失败或同步接口失败 | 查支付订单、通知记录和通道查询。 |
| 线下凭证上传失败 | file 后端、文件权限或大小限制不满足 | 查 `@mango/file` 和 `mango-file` 配置。 |
| 对账列表为空 | 当前库没有导入或生成对账数据 | 不要用无目的 `total > 0` 判断支付能力可用，改测具体导入/生成动作。 |

## 10. 相关文档

- [Mango Payment 后端 README](../../../mango/mango-platform/mango-payment/README.md)
- [@mango/file](../file/README.md)
- [后端 Workflow](../../../mango/mango-platform/mango-workflow/README.md)
- [后端 Numgen](../../../mango/mango-platform/mango-numgen/README.md)
- [后端 Job](../../../mango/mango-platform/mango-job/README.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
