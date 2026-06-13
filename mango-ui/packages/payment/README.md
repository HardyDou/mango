# @mango/payment

## 1. 能力定位

`@mango/payment` 提供 Mango 支付中心管理端页面、收银台页面、页面注册入口、支付组件和支付 API 请求封装。主要使用者是 Mango 管理端 Shell、支付运营后台和需要接入支付收银台的业务后台应用。

## 2. 适用场景

- 在管理端注册支付应用、企业主体、支付通道、通道签约、支付方式和收银台配置页面。
- 展示业务订单、支付订单、退款订单、退款审批、交易流水、通知记录、对账、差异、结算和操作审计页面。
- 在业务后台或管理端拉起支付收银台，并展示支付网关结果页。
- 调用后端 payment API 完成支付配置、订单查询、退款、对账和运营治理。

## 3. 不适用场景

- 不实现支付通道后端适配、签名、验签、回调处理或状态推进。
- 不保存支付订单、通道签约和对账数据。
- 不替代业务前台面向 C 端用户的定制化收银台。
- 不负责菜单、权限、租户和支付通道初始化数据。

## 4. 模块边界

本包只提供前端视图、收银台组件、API 封装、样式和 admin page 注册。支付领域规则、数据库、通道适配、权限、租户隔离和状态流由后端 `mango-payment`、`mango-authorization`、`mango-numgen`、`mango-workflow` 和相关模块承担。

## 5. 接入方式

依赖包：

```json
{
  "dependencies": {
    "@mango/payment": "1.0.0"
  }
}
```

注册管理端页面：

```ts
import { registerMangoPaymentAdminPages } from '@mango/payment/admin-pages';

registerMangoPaymentAdminPages();
```

样式入口：

```ts
import '@mango/payment/style.css';
```

收银台组件：

```ts
import { PaymentCashier } from '@mango/payment';
```

## 6. 配置项

本包没有独立运行时配置文件。构建由 `vite build` 完成，宿主应用负责提供请求基地址、认证态、路由、Shell、页面注册上下文和后端 payment API。

## 7. 对外接口 / 扩展点

导出视图：

- `PaymentApplicationView`
- `PaymentEnterpriseSubjectView`
- `PaymentChannelView`
- `PaymentChannelContractView`
- `PaymentMethodView`
- `PaymentCashierConfigView`
- `PaymentBusinessOrderView`
- `PaymentOrderView`
- `PaymentOfflineCollectionView`
- `PaymentOfflineRefundView`
- `PaymentRefundOrderView`
- `PaymentRefundApprovalView`
- `PaymentTransactionFlowView`
- `PaymentExceptionOrderView`
- `PaymentNotificationRecordView`
- `PaymentReconciliationView`
- `PaymentDifferenceView`
- `PaymentSettlementSummaryView`
- `PaymentOperationAuditView`
- `PaymentCashierView`
- `PaymentGatewayResultView`

页面注册：

- `registerMangoPaymentAdminPages()` 注册模块 `mango-payment`。
- 页面 key 覆盖 `payment/applications/index`、`payment/enterprise-subjects/index`、`payment/channel-contracts/index`、`payment/channels/index`、`payment/methods/index`、`payment/cashier-configs/index`、`payment/business-orders/index`、`payment/payment-orders/index`、`payment/offline-collections/index`、`payment/offline-refunds/index`、`payment/refund-orders/index`、`payment/refund-approvals/index`、`payment/transaction-flows/index`、`payment/exception-orders/index`、`payment/notification-records/index`、`payment/reconciliations/index`、`payment/differences/index`、`payment/settlement-summaries/index`、`payment/operation-audits/index`、`payment/cashier/index`、`payment/gateway-result/index`。

API：

- `paymentApi` 覆盖 applications、enterprise-subjects、channels、channel-contracts、methods、cashier-configs、business-orders、payment-orders、refund-orders、refund-approvals、transaction-flows、notification-records、reconciliations、differences、settlement-summaries、operation-audits、offline-collections、offline-refunds 和 gateway result 相关请求。

## 8. 数据库 / 初始化数据

本包不包含数据库 migration。支付表、菜单、权限、支付方式、通道能力、签约字段模板、编号和任务初始化由后端 `mango-payment`、`mango-authorization`、`mango-numgen` 和 `mango-job` 维护。

## 9. 菜单 / 权限 / 租户

前端页面 key 需要与 authorization 中的菜单资源匹配。页面请求后端 payment API，权限、租户、支付应用、企业主体、签约通道和敏感字段边界由后端接口与当前登录态控制。

## 10. 验证方式

```bash
pnpm -F @mango/payment build
```

业务链路验收：

- 管理端菜单能打开支付应用、企业主体、支付通道、支付方式、收银台配置和订单页面。
- 支付配置页面能请求后端 payment API，缺少权限时后端返回受控错误。
- 业务订单能拉起收银台，收银台能展示可用支付方式并返回支付结果。
- 支付成功、退款、通知、对账、差异和结算页面能展示后端状态。

## 11. 业务接入最小闭环

宿主后台接入时引入 `@mango/payment/style.css`，在启动入口调用 `registerMangoPaymentAdminPages()`。后端接入 `mango-payment-starter` 后，authorization 菜单资源的 component key 使用 payment 包注册的页面 key。

验收断言覆盖：支付中心菜单能打开，页面请求 payment API 不返回 404，收银台可按业务订单展示可用支付方式，支付结果页可展示支付状态，缺少权限的用户不能访问治理操作。

## 12. 常见问题

- 页面空白先检查是否调用 `registerMangoPaymentAdminPages()`。
- 接口 404 时检查后端 `mango-payment-starter` 是否启用，以及代理路径是否覆盖 payment API。
- 菜单显示但无权限时检查 authorization 菜单、页面 key 和操作权限码。
- 收银台没有支付方式时检查支付方式状态、收银台配置、签约能力和后端路由规则。

## 13. 关联 PMO 规则

- [前端模块规范](../../../mango-pmo/rules/frontend/01-vue-code.md)
- [前端测试规范](../../../mango-pmo/rules/frontend/04-test.md)
- [模块菜单规范](../../../mango-pmo/rules/backend/11-module-menu.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 14. 历史设计 / 交付记录

- [Mango Payment 后端 README](../../../mango/mango-platform/mango-payment/README.md)
- [支付交付台账](../../../mango-docs/plans/2026-05-25-payment-delivery-ledger.md)
- [支付交付证据汇总](../../../mango-docs/plans/evidence/payment-delivery-evidence-summary.md)
- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
