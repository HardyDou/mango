package io.mango.payment.channel.sandbox;

import io.mango.payment.api.enums.PaymentMaterialType;
import io.mango.payment.api.enums.PaymentOrderStatus;
import io.mango.payment.api.enums.RefundOrderStatus;
import io.mango.payment.channel.spi.IPaymentChannelProvider;
import io.mango.payment.channel.spi.IPaymentNotifyVerifier;
import io.mango.payment.channel.spi.IRefundChannelProvider;
import io.mango.payment.channel.spi.model.PaymentChannelResponse;
import io.mango.payment.channel.spi.model.PaymentChannelStatus;
import io.mango.payment.channel.spi.model.RefundChannelStatus;
import org.springframework.stereotype.Component;

@Component
public class SandboxPaymentChannelProvider implements IPaymentChannelProvider, IRefundChannelProvider, IPaymentNotifyVerifier {

    public static final String CHANNEL_CODE = "SANDBOX";

    @Override
    public String channelCode() {
        return CHANNEL_CODE;
    }

    @Override
    public PaymentChannelResponse pay(Long paymentOrderId, Long amount, String subject) {
        String channelOrderNo = sandboxPaymentOrderNo(paymentOrderId);
        return new PaymentChannelResponse(CHANNEL_CODE, channelOrderNo, PaymentMaterialType.SANDBOX_TOKEN,
                "sandbox://pay/" + paymentOrderId);
    }

    @Override
    public PaymentChannelStatus queryPayment(Long paymentOrderId, String channelOrderNo) {
        return new PaymentChannelStatus(PaymentOrderStatus.SUCCESS, channelOrderNo);
    }

    @Override
    public boolean closePayment(Long paymentOrderId, String channelOrderNo) {
        return true;
    }

    @Override
    public RefundChannelStatus refund(Long refundOrderId, Long paymentOrderId, Long refundAmount) {
        return new RefundChannelStatus(RefundOrderStatus.SUCCESS, sandboxRefundOrderNo(refundOrderId));
    }

    @Override
    public RefundChannelStatus queryRefund(Long refundOrderId, String channelRefundNo) {
        return new RefundChannelStatus(RefundOrderStatus.SUCCESS, channelRefundNo);
    }

    @Override
    public boolean verifyPayment(Long paymentOrderId, String channelOrderNo, String notifyEventId, String signature) {
        return signature != null && signature.equals(signatureOf(paymentOrderId, channelOrderNo, notifyEventId));
    }

    public static String signatureOf(Long paymentOrderId, String channelOrderNo, String notifyEventId) {
        return CHANNEL_CODE + ':' + paymentOrderId + ':' + channelOrderNo + ':' + notifyEventId;
    }

    public static String sandboxPaymentOrderNo(Long paymentOrderId) {
        return "SANDBOX-PAY-" + paymentOrderId;
    }

    private static String sandboxRefundOrderNo(Long refundOrderId) {
        return "SANDBOX-REFUND-" + refundOrderId;
    }
}
