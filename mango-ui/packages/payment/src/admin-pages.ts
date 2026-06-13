import { registerModulePages } from '@mango/admin-pages/core';

let registered = false;

export function registerMangoPaymentAdminPages() {
  if (registered) {
    return;
  }
  registered = true;
  registerModulePages({
    moduleCode: 'mango-payment',
    pages: {
      'payment/applications/index': () => import('./index').then(m => m.PaymentApplicationView),
      'payment/enterprise-subjects/index': () => import('./index').then(m => m.PaymentEnterpriseSubjectView),
      'payment/channel-contracts/index': () => import('./index').then(m => m.PaymentChannelContractView),
      'payment/channels/index': () => import('./index').then(m => m.PaymentChannelView),
      'payment/methods/index': () => import('./index').then(m => m.PaymentMethodView),
      'payment/cashier-configs/index': () => import('./index').then(m => m.PaymentCashierConfigView),
      'payment/business-orders/index': () => import('./index').then(m => m.PaymentBusinessOrderView),
      'payment/payment-orders/index': () => import('./index').then(m => m.PaymentOrderView),
      'payment/offline-collections/index': () => import('./index').then(m => m.PaymentOfflineCollectionView),
      'payment/offline-refunds/index': () => import('./index').then(m => m.PaymentOfflineRefundView),
      'payment/refund-orders/index': () => import('./index').then(m => m.PaymentRefundOrderView),
      'payment/refund-approvals/index': () => import('./index').then(m => m.PaymentRefundApprovalView),
      'payment/transaction-flows/index': () => import('./index').then(m => m.PaymentTransactionFlowView),
      'payment/exception-orders/index': () => import('./index').then(m => m.PaymentExceptionOrderView),
      'payment/notification-records/index': () => import('./index').then(m => m.PaymentNotificationRecordView),
      'payment/reconciliations/index': () => import('./index').then(m => m.PaymentReconciliationView),
      'payment/differences/index': () => import('./index').then(m => m.PaymentDifferenceView),
      'payment/settlement-summaries/index': () => import('./index').then(m => m.PaymentSettlementSummaryView),
      'payment/operation-audits/index': () => import('./index').then(m => m.PaymentOperationAuditView),
      'payment/cashier/index': () => import('./index').then(m => m.PaymentCashierView),
      'payment/gateway-result/index': () => import('./index').then(m => m.PaymentGatewayResultView),
    },
  });
}
