package io.mango.payment.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.exception.BizException;
import io.mango.payment.api.enums.PaymentOfflineCollectionStatusEnum;
import io.mango.payment.api.enums.PaymentOrderStatusEnum;
import io.mango.payment.core.entity.PaymentOfflineCollectionEntity;
import io.mango.payment.core.entity.PaymentOrderEntity;
import io.mango.payment.core.mapper.PaymentOfflineCollectionMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentOfflineCollectionChannelAdapterTest {

    private final PaymentOfflineCollectionMapper offlineCollectionMapper = mock(PaymentOfflineCollectionMapper.class);
    private final PaymentSensitiveValueService sensitiveValueService = mock(PaymentSensitiveValueService.class);
    private final PaymentNumberService numberService = mock(PaymentNumberService.class);
    private final PaymentOfflineCollectionChannelAdapter adapter =
            new PaymentOfflineCollectionChannelAdapter(offlineCollectionMapper, sensitiveValueService, new ObjectMapper(), numberService);

    @Test
    @DisplayName("applyPayment should return transfer material and keep payment order paying")
    void applyPayment_returnsTransferMaterial() {
        when(sensitiveValueService.decrypt("enc:offline-account")).thenReturn("6222000000000001");
        when(sensitiveValueService.mask(eq("6222000000000001"), eq(4), eq(4))).thenReturn("6222****0001");

        IPaymentChannelAdapter.PaymentApplyResult result = adapter.applyPayment(paymentCommand());

        assertThat(adapter.channelCode()).isEqualTo("OFFLINE_COLLECTION");
        assertThat(result.status()).isEqualTo("PAYING");
        assertThat(result.channelTradeNo()).isEqualTo("OCPO202606060001");
        assertThat(result.material().getMaterialType()).isEqualTo("TRANSFER_ACCOUNT");
        assertThat(result.material().getAccountName()).isEqualTo("芒果科技有限公司");
        assertThat(result.material().getAccountNo()).isEqualTo("6222000000000001");
        assertThat(result.material().getAccountNoMask()).isEqualTo("6222****0001");
        assertThat(result.material().getBankName()).isEqualTo("招商银行上海分行");
        assertThat(result.material().getReconciliationCode()).isNull();
        assertThat(result.material().getTransferRemark()).matches("[0-9A-Z]{6}");
        assertThat(result.material().getVoucherRequired()).isTrue();
    }

    @Test
    @DisplayName("afterPaymentOrderCreated should create offline collection record")
    void afterPaymentOrderCreated_insertsOfflineCollection() {
        when(sensitiveValueService.decrypt("enc:offline-account")).thenReturn("6222000000000001");
        when(sensitiveValueService.mask(eq("6222000000000001"), eq(4), eq(4))).thenReturn("6222****0001");
        IPaymentChannelAdapter.PaymentApplyCommand command = paymentCommand();
        IPaymentChannelAdapter.PaymentApplyResult result = adapter.applyPayment(command);
        PaymentOrderEntity order = paymentOrder();
        when(numberService.next(PaymentNumberService.PAY_OFFLINE_COLLECTION_NO)).thenReturn("OC2026060600000001");
        ArgumentCaptor<PaymentOfflineCollectionEntity> captor =
                ArgumentCaptor.forClass(PaymentOfflineCollectionEntity.class);

        adapter.afterPaymentOrderCreated(command, result, order);

        verify(offlineCollectionMapper).insert(captor.capture());
        PaymentOfflineCollectionEntity entity = captor.getValue();
        assertThat(entity.getOfflineCollectionNo()).isEqualTo("OC2026060600000001");
        assertThat(entity.getPaymentOrderId()).isEqualTo(370001L);
        assertThat(entity.getPayOrderNo()).isEqualTo("PO202606060001");
        assertThat(entity.getBusinessOrderId()).isEqualTo(360001L);
        assertThat(entity.getBizOrderNo()).isEqualTo("BO202606060001");
        assertThat(entity.getChannelId()).isEqualTo(330004L);
        assertThat(entity.getChannelCode()).isEqualTo("OFFLINE_COLLECTION");
        assertThat(entity.getContractId()).isEqualTo(331004L);
        assertThat(entity.getContractCapabilityId()).isEqualTo(333014L);
        assertThat(entity.getSubjectId()).isEqualTo(320001L);
        assertThat(entity.getSubjectName()).isEqualTo("芒果科技有限公司");
        assertThat(entity.getAccountName()).isEqualTo("芒果科技有限公司");
        assertThat(entity.getAccountNoMask()).isEqualTo("6222****0001");
        assertThat(entity.getBankName()).isEqualTo("招商银行上海分行");
        assertThat(entity.getAmount()).isEqualTo(9900L);
        assertThat(entity.getCurrency()).isEqualTo("CNY");
        assertThat(entity.getReconciliationCode()).isEqualTo(result.material().getTransferRemark());
        assertThat(entity.getTransferRemark()).isEqualTo(result.material().getTransferRemark());
        assertThat(entity.getVoucherCount()).isZero();
        assertThat(entity.getCollectionStatus()).isEqualTo(PaymentOfflineCollectionStatusEnum.WAITING_TRANSFER.getCode());
        assertThat(entity.getExpireTime()).isEqualTo(command.expireTime());
        assertThat(entity.getTenantId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("queryPayment should keep waiting offline collection paying")
    void queryPayment_waitingCollectionReturnsPaying() {
        PaymentOfflineCollectionEntity collection = offlineCollection(PaymentOfflineCollectionStatusEnum.WAITING_TRANSFER.getCode());
        when(offlineCollectionMapper.selectByPayOrderNoForUpdate(1L, "PO202606060001")).thenReturn(collection);

        IPaymentChannelAdapter.PaymentQueryResult result = adapter.queryPayment(
                new IPaymentChannelAdapter.PaymentQueryCommand(1L, paymentOrder()));

        assertThat(result.scenario()).isEqualTo("OFFLINE_COLLECTION_WAITING");
        assertThat(result.returnCode()).isEqualTo(PaymentOfflineCollectionStatusEnum.WAITING_TRANSFER.getCode());
        assertThat(result.resultType()).isEqualTo("ASYNC_PROCESSING");
        assertThat(result.status()).isEqualTo(PaymentOrderStatusEnum.PAYING.getCode());
    }

    @Test
    @DisplayName("queryPayment should map confirmed offline collection to payment success")
    void queryPayment_confirmedCollectionReturnsSuccess() {
        PaymentOfflineCollectionEntity collection = offlineCollection(PaymentOfflineCollectionStatusEnum.CONFIRMED.getCode());
        when(offlineCollectionMapper.selectByPayOrderNoForUpdate(1L, "PO202606060001")).thenReturn(collection);

        IPaymentChannelAdapter.PaymentQueryResult result = adapter.queryPayment(
                new IPaymentChannelAdapter.PaymentQueryCommand(1L, paymentOrder()));

        assertThat(result.scenario()).isEqualTo("OFFLINE_COLLECTION_CONFIRMED");
        assertThat(result.returnCode()).isEqualTo(PaymentOfflineCollectionStatusEnum.CONFIRMED.getCode());
        assertThat(result.resultType()).isEqualTo("SUCCESS");
        assertThat(result.status()).isEqualTo(PaymentOrderStatusEnum.SUCCESS.getCode());
    }

    @Test
    @DisplayName("applyRefund should direct offline refund to offline refund order entry")
    void applyRefund_requiresOfflineRefundEntry() {
        IPaymentChannelAdapter.RefundApplyCommand command = new IPaymentChannelAdapter.RefundApplyCommand(
                1L,
                "OFFLINE_COLLECTION",
                331004L,
                "RO202606060001",
                "BR202606060001",
                "PO202606060001",
                "BO202606060001",
                "OFFLINE_TRANSFER",
                "OCPO202606060001",
                9900L,
                9900L,
                "CNY",
                "线下退款");

        assertThatThrownBy(() -> adapter.applyRefund(command))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("线下退款需要录入退款账户、退款金额和退款凭证");
    }

    @Test
    @DisplayName("generateBill should direct offline reconciliation to bank statement import")
    void generateBill_requiresBankStatementImport() {
        IPaymentChannelAdapter.ChannelBillCommand command = new IPaymentChannelAdapter.ChannelBillCommand(
                1L,
                "OFFLINE_COLLECTION",
                331004L,
                LocalDate.of(2026, 6, 6));

        assertThatThrownBy(() -> adapter.generateBill(command))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("线下收款对账以银行流水 Excel 导入为准");
    }

    private IPaymentChannelAdapter.PaymentApplyCommand paymentCommand() {
        return new IPaymentChannelAdapter.PaymentApplyCommand(
                1L,
                "OFFLINE_COLLECTION",
                331004L,
                "{\"accountName\":\"芒果科技有限公司\",\"accountNo\":\"enc:offline-account\",\"bankName\":\"招商银行上海分行\"}",
                "PO202606060001",
                "BO202606060001",
                "CORPORATE_OFFLINE_ACCOUNT",
                "线下转账",
                "TRANSFER_ACCOUNT",
                9900L,
                "CNY",
                "测试订单",
                LocalDateTime.of(2026, 6, 6, 10, 30),
                320001L,
                "芒果科技有限公司",
                null,
                null,
                null,
                null,
                "127.0.0.1");
    }

    private PaymentOrderEntity paymentOrder() {
        PaymentOrderEntity order = new PaymentOrderEntity();
        order.setId(370001L);
        order.setPayOrderNo("PO202606060001");
        order.setBusinessOrderId(360001L);
        order.setChannelId(330004L);
        order.setChannelCode("OFFLINE_COLLECTION");
        order.setContractId(331004L);
        order.setContractCapabilityId(333014L);
        return order;
    }

    private PaymentOfflineCollectionEntity offlineCollection(String status) {
        PaymentOfflineCollectionEntity entity = new PaymentOfflineCollectionEntity();
        entity.setId(380001L);
        entity.setOfflineCollectionNo("OC202606060001");
        entity.setPayOrderNo("PO202606060001");
        entity.setChannelCode("OFFLINE_COLLECTION");
        entity.setCollectionStatus(status);
        return entity;
    }
}
