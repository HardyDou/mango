package io.mango.payment.starter.remote;

import java.util.List;

final class PaymentRemoteContractFixtures {

    private static final List<Class<?>> FEIGN_TYPES = List.of(
            MangoPayVirtualPaymentFeignClient.class,
            PaymentApplicationFeignClient.class,
            PaymentBusinessOrderFeignClient.class,
            PaymentCashierConfigFeignClient.class,
            PaymentCashierFeignClient.class,
            PaymentChannelCallbackFeignClient.class,
            PaymentChannelContractFeignClient.class,
            PaymentChannelFeignClient.class,
            PaymentDifferenceFeignClient.class,
            PaymentEnterpriseSubjectFeignClient.class,
            PaymentExceptionOrderFeignClient.class,
            PaymentMethodFeignClient.class,
            PaymentMethodRouteFeignClient.class,
            PaymentNotificationRecordFeignClient.class,
            PaymentObservabilityFeignClient.class,
            PaymentOfflineCollectionFeignClient.class,
            PaymentOfflineRefundFeignClient.class,
            PaymentOpenFeignClient.class,
            PaymentOperationAuditFeignClient.class,
            PaymentOrderFeignClient.class,
            PaymentReconciliationFeignClient.class,
            PaymentRefundApprovalFeignClient.class,
            PaymentRefundOrderFeignClient.class,
            PaymentSecurityFeignClient.class,
            PaymentSettlementSummaryFeignClient.class,
            PaymentTaskFeignClient.class,
            PaymentTransactionFlowFeignClient.class);

    private PaymentRemoteContractFixtures() {
    }

    static List<Class<?>> feignTypes() {
        return FEIGN_TYPES;
    }

    static Class<?> apiType(Class<?> feignType) {
        return feignType.getInterfaces()[0];
    }
}
