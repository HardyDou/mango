# @mango/payment

## 1. 概览
`@mango/payment` 是 Mango 支付中心的管理端前端包，提供支付配置、订单运营、退款、对账、差异、结算、通知和收银台页面，以及支付 API 的 TypeScript 封装。

它面向后台业务开发者和支付运营页面开发者：后台负责展示和操作支付数据，真实支付能力仍由后端 `mango-payment` 和支付通道适配实现。

## 2. 功能清单

| 能力 | 常用入口 |
|------|----------|
| 在 Mango Admin 中注册支付中心菜单页面 | 前端注册 / 组件 / API 封装 |
| 管理支付应用、企业主体、支付通道、通道签约、支付方式和收银台配置 | 前端注册 / 组件 / API 封装 |
| 查询业务订单、支付订单、退款订单、退款审批、交易流水、异常单、通知记录、对账单、差异单和结算单 | 前端注册 / 组件 / API 封装 |
| 在业务后台打开 PaymentCashier 或收银台页面，按业务订单展示可用支付方式并提交支付 | 前端注册 / 组件 / API 封装 |
| 调用 paymentApi 的细分 API 完成运营页面查询、创建、状态同步、退款、通知重试和对账处理 | 前端注册 / 组件 / API 封装 |

## 3. 适用场景
- 在 Mango Admin 中注册支付中心菜单页面。
- 管理支付应用、企业主体、支付通道、通道签约、支付方式和收银台配置。
- 查询业务订单、支付订单、退款订单、退款审批、交易流水、异常单、通知记录、对账单、差异单和结算单。
- 在业务后台打开 `PaymentCashier` 或收银台页面，按业务订单展示可用支付方式并提交支付。
- 调用 `paymentApi` 的细分 API 完成运营页面查询、创建、状态同步、退款、通知重试和对账处理。

## 4. 边界说明
- 不实现支付通道签名、验签、回调处理、账单拉取或状态推进。
- 不保存支付订单、企业主体、签约和对账数据。
- 不替代 C 端官网、商城或 App 的高度定制化收银台。
- 不负责菜单、权限、租户、支付方式和通道能力的初始化。

## 5. 模块组成
本包只包含 Vue 页面、收银台组件、支付 API 封装、页面注册入口和样式。支付领域规则、数据库、通道适配、权限、租户隔离、编号、工作流审批和定时任务分别由后端 `mango-payment`、`mango-authorization`、`mango-numgen`、`mango-workflow` 和 `mango-job` 承担。

`PaymentCashier` 会调用后端收银台接口并使用 `@mango/file` 展示 logo 或上传线下转账凭证；二维码渲染依赖 `qrcode`。

## 6. 接入方式
依赖包：

```json
{
  "dependencies": {
    "@mango/payment": "1.0.0"
  }
}
```

在管理端注册支付页面：

```ts
import { registerMangoPaymentAdminPages } from '@mango/payment/admin-pages';
import '@mango/payment/style.css';

registerMangoPaymentAdminPages();
```

业务后台直接使用收银台组件：

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

调用 API 封装：

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

## 7. 配置说明
本包没有独立 Vite 环境变量。运行时配置来自宿主应用、`@mango/common` 请求实例和后端 payment 配置。

| 配置位置 | 字段 / 参数 | 含义 |
|----------|-------------|------|
| 宿主 Shell | API baseURL / 代理 | 决定 `/payment/**` 请求转发到哪个后端服务。 |
| `registerMangoPaymentAdminPages()` | 无入参 | 幂等注册 `mango-payment` 下的页面 key。 |
| `PaymentCashier` props | `cashierConfigId` | 收银台配置 id；不传业务订单时用于预览。 |
| `PaymentCashier` props | `businessOrderId` | 业务订单 id；支付时必须能解析到后端业务订单。 |
| `PaymentCashier` props | `embedded` | 是否嵌入业务页面；嵌入模式成功后触发 `success`，关闭触发 `close`。 |
| 支付通道管理页面 | `fieldTemplateJson` | 通道字段模板，决定签约配置表单字段。 |
| 通道签约页面 | `configValuesJson` | 按字段模板保存的通道配置值。 |
| 收银台配置页面 | `resultReturnUrl` | 支付完成后结果页或业务回跳地址，由后端保存。 |

通道字段模板支持输入框、数字、开关、URL、日期、JSON、文件 id 等字段类型；字段模板和配置值是后端数据，前端只负责编辑和序列化。

## 8. API 与扩展
页面注册入口：

- `registerMangoPaymentAdminPages()`：注册模块 `mango-payment`。

页面 key：

| 页面 key | 能力 |
|----------|------|
| `payment/applications/index` | 支付应用管理。 |
| `payment/enterprise-subjects/index` | 企业主体管理。 |
| `payment/channels/index` | 支付通道和通道能力管理。 |
| `payment/channel-contracts/index` | 通道签约、签约配置和账单来源管理。 |
| `payment/methods/index` | 支付方式和路由规则管理。 |
| `payment/cashier-configs/index` | 收银台配置管理。 |
| `payment/business-orders/index` | 业务订单创建、查询和拉起收银台。 |
| `payment/payment-orders/index` | 支付订单查询和状态同步。 |
| `payment/offline-collections/index` | 线下收款、银行流水导入和匹配确认。 |
| `payment/offline-refunds/index` | 线下退款查询。 |
| `payment/refund-orders/index` | 退款订单查询和通道查询。 |
| `payment/refund-approvals/index` | 退款审批创建和查询。 |
| `payment/transaction-flows/index` | 交易流水查询。 |
| `payment/exception-orders/index` | 异常订单处理。 |
| `payment/notification-records/index` | 支付通知记录、重试和到期投递。 |
| `payment/reconciliations/index` | 对账导入、虚拟账单生成和通道账单拉取。 |
| `payment/differences/index` | 对账差异处理。 |
| `payment/settlement-summaries/index` | 结算汇总生成、确认和作废。 |
| `payment/operation-audits/index` | 支付操作审计查询。 |
| `payment/cashier/index` | 管理端收银台页面。 |
| `payment/gateway-result/index` | 支付网关结果页。 |

主要 API 封装：

- 配置类：`paymentApplicationApi`、`paymentEnterpriseSubjectApi`、`paymentChannelApi`、`paymentChannelContractApi`、`paymentMethodApi`、`paymentMethodRouteApi`、`paymentCashierConfigApi`。
- 订单类：`paymentBusinessOrderApi`、`paymentOrderApi`、`paymentRefundOrderApi`、`paymentRefundApprovalApi`。
- 线下收款：`paymentOfflineCollectionApi`、`paymentOfflineRefundApi`。
- 运营治理：`paymentTransactionFlowApi`、`paymentExceptionOrderApi`、`paymentNotificationRecordApi`、`paymentReconciliationApi`、`paymentDifferenceApi`、`paymentSettlementSummaryApi`、`paymentOperationAuditApi`。
- 收银台：`paymentCashierApi.session()`、`paymentCashierApi.pay()`、`paymentCashierApi.payResult()`、`paymentCashierApi.syncPayResult()`、`paymentCashierApi.mangoPayVirtualPay()`。

## 9. 数据与初始化
本包不包含数据库 migration。

| 数据 | 来源 |
|------|------|
| 支付应用、主体、通道、签约、支付方式、订单、退款、通知、对账、差异、结算 | 后端 `mango-payment` migration 和业务运行数据。 |
| 菜单和权限 | 后端 `mango-authorization` 菜单资源初始化。 |
| 支付单号、退款单号等编号 | 后端 `mango-numgen` 规则。 |
| 退款审批 | 后端 `mango-workflow` 流程能力。 |
| 通知重试、对账拉取等后台任务 | 后端 `mango-job` 或 payment 内部任务。 |

## 10. 管理入口
前端页面 key 必须与 authorization 菜单的 component 字段一致。页面能否访问、按钮能否操作、数据是否跨租户隔离，全部由后端登录态、租户上下文、角色授权和 payment 接口校验决定。

支付中心常见权限边界：

- 配置页面通常只给支付管理员或平台运营角色。
- 收银台页面需要业务订单可见权限和支付应用可用权限。
- 对账、差异、结算、异常处理和通知重试是高风险操作，应单独授权。
- 企业主体、通道签约和密钥字段属于敏感数据，后端必须控制读写范围。

## 11. 快速开始
1. 后端启用 `mango-payment`，并准备支付应用、企业主体、通道、签约、支付方式和收银台配置。
2. 宿主前端引入 `@mango/payment/style.css`，启动时调用 `registerMangoPaymentAdminPages()`。
3. authorization 菜单的 component 使用上表页面 key。
4. 业务创建订单后拿到 `businessOrderId`，用 `PaymentCashier` 或 `payment/cashier/index` 打开收银台。
5. 支付完成后通过 `paymentCashierApi.payResult()` 或订单页面同步支付结果。
6. 后台验证退款、通知、对账、差异和结算页面都能按权限访问。

## 12. 问题排查
- 页面空白：检查是否调用 `registerMangoPaymentAdminPages()`，菜单 component 是否命中页面 key。
- 接口 404：检查后端是否启用 `mango-payment`，以及宿主代理是否覆盖 `/payment/**`。
- 收银台没有支付方式：检查收银台配置、支付方式状态、通道签约能力和后端路由规则。
- 支付后状态不变：检查通道回调、通知记录、订单同步接口和支付单号。
- 对账列表为空：这不是前端缺陷，先确认后端是否生成或导入了对账数据；测试不应对 master/config 类列表做无目的 `total > 0` 断言。

## 13. 相关文档
- [前端模块规范](../../../mango-pmo/rules/frontend/01-vue-code.md)
- [前端测试规范](../../../mango-pmo/rules/frontend/04-test.md)
- [模块菜单规范](../../../mango-pmo/rules/backend/11-module-menu.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 14. 历史资料
- [Mango Payment 后端 README](../../../mango/mango-platform/mango-payment/README.md)
- [支付交付台账](../../../mango-docs/plans/2026-05-25-payment-delivery-ledger.md)
- [支付交付证据汇总](../../../mango-docs/plans/evidence/payment-delivery-evidence-summary.md)
- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
