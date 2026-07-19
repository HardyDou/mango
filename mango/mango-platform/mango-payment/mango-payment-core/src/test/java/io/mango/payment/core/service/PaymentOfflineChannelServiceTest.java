package io.mango.payment.core.service;

import io.mango.infra.context.api.MangoContextHolder;
import io.mango.payment.api.command.SubmitOfflineTransferVoucherCommand;
import io.mango.payment.api.enums.PaymentCode;
import io.mango.payment.core.entity.PaymentOrderEntity;
import io.mango.payment.core.mapper.PaymentApplicationMapper;
import io.mango.payment.core.mapper.PaymentBusinessOrderMapper;
import io.mango.payment.core.mapper.PaymentOfflineBankStatementBatchMapper;
import io.mango.payment.core.mapper.PaymentOfflineBankStatementItemMapper;
import io.mango.payment.core.mapper.PaymentOfflineCollectionMapper;
import io.mango.payment.core.mapper.PaymentOfflineCollectionMatchMapper;
import io.mango.payment.core.mapper.PaymentOfflineCollectionVoucherMapper;
import io.mango.payment.core.mapper.PaymentOfflineRefundMapper;
import io.mango.payment.core.mapper.PaymentOrderMapper;
import io.mango.payment.core.mapper.PaymentRefundOrderMapper;
import io.mango.payment.core.mapper.PaymentTransactionFlowMapper;
import io.mango.payment.core.service.impl.PaymentOfflineChannelService;
import io.mango.payment.core.service.impl.PaymentOperationAuditService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentOfflineChannelServiceTest {

    @InjectMocks
    private PaymentOfflineChannelService offlineChannelService;

    @Mock private PaymentOfflineCollectionMapper offlineCollectionMapper;
    @Mock private PaymentOfflineCollectionVoucherMapper offlineCollectionVoucherMapper;
    @Mock private PaymentOfflineBankStatementBatchMapper offlineBankStatementBatchMapper;
    @Mock private PaymentOfflineBankStatementItemMapper offlineBankStatementItemMapper;
    @Mock private PaymentOfflineCollectionMatchMapper offlineCollectionMatchMapper;
    @Mock private PaymentOfflineRefundMapper offlineRefundMapper;
    @Mock private PaymentOrderMapper paymentOrderMapper;
    @Mock private PaymentBusinessOrderMapper businessOrderMapper;
    @Mock private PaymentApplicationMapper applicationMapper;
    @Mock private PaymentRefundOrderMapper refundOrderMapper;
    @Mock private PaymentTransactionFlowMapper transactionFlowMapper;
    @Mock private PaymentOrderStatusFlowRecorder statusFlowRecorder;
    @Mock private PaymentNotificationDispatcher notificationDispatcher;
    @Mock private PaymentSensitiveValueCodec sensitiveValueCodec;
    @Mock private PaymentOperationAuditService operationAudit;
    @Mock private PaymentNumberGenerator numberGenerator;

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    @DisplayName("public transfer voucher should resolve and restore tenant context from payment order")
    void submitTransferVoucher_withoutTenantContext_usesOrderTenant() {
        SubmitOfflineTransferVoucherCommand command = new SubmitOfflineTransferVoucherCommand();
        command.setPayOrderNo("PO202607180001");
        command.setTransferAmount(128800L);
        command.setVoucherFileIds("2078390000000000001");
        PaymentOrderEntity order = new PaymentOrderEntity();
        order.setPayOrderNo(command.getPayOrderNo());
        order.setTenantId("1");
        when(paymentOrderMapper.selectByPayOrderNo(command.getPayOrderNo())).thenReturn(order);

        assertThatThrownBy(() -> offlineChannelService.submitTransferVoucher(command))
                .extracting("code")
                .isEqualTo(PaymentCode.PAYMENT_OFFLINE_COLLECTION_NOT_FOUND.getCode());

        verify(offlineCollectionMapper).selectByPayOrderNoForUpdate("1", command.getPayOrderNo());
        assertThat(MangoContextHolder.tenantId()).isNull();
    }
}
