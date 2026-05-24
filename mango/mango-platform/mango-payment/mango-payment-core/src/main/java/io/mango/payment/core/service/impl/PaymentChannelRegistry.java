package io.mango.payment.core.service.impl;

import io.mango.common.result.Require;
import io.mango.payment.channel.spi.IPaymentChannelProvider;
import io.mango.payment.channel.spi.IPaymentNotifyVerifier;
import io.mango.payment.channel.spi.IRefundChannelProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PaymentChannelRegistry {

    private final List<IPaymentChannelProvider> paymentChannelProviders;
    private final List<IRefundChannelProvider> refundChannelProviders;
    private final List<IPaymentNotifyVerifier> paymentNotifyVerifiers;

    public IPaymentChannelProvider paymentProvider(String payMethod) {
        String channelCode = channelCodeOf(payMethod);
        IPaymentChannelProvider provider = paymentChannelProviders.stream()
                .filter(candidate -> candidate.channelCode().equals(channelCode))
                .findFirst()
                .orElse(null);
        Require.notNull(provider, "支付通道不存在：" + channelCode);
        return provider;
    }

    public IPaymentChannelProvider paymentProviderByChannel(String channelCode) {
        IPaymentChannelProvider provider = paymentChannelProviders.stream()
                .filter(candidate -> candidate.channelCode().equals(channelCode))
                .findFirst()
                .orElse(null);
        Require.notNull(provider, "支付通道不存在：" + channelCode);
        return provider;
    }

    public IRefundChannelProvider refundProvider(String channelCode) {
        IRefundChannelProvider provider = refundChannelProviders.stream()
                .filter(candidate -> candidate.channelCode().equals(channelCode))
                .findFirst()
                .orElse(null);
        Require.notNull(provider, "退款通道不存在：" + channelCode);
        return provider;
    }

    public IPaymentNotifyVerifier notifyVerifier(String channelCode) {
        IPaymentNotifyVerifier verifier = paymentNotifyVerifiers.stream()
                .filter(candidate -> candidate.channelCode().equals(channelCode))
                .findFirst()
                .orElse(null);
        Require.notNull(verifier, "支付回调验签器不存在：" + channelCode);
        return verifier;
    }

    private String channelCodeOf(String payMethod) {
        String value = payMethod == null ? "" : payMethod.trim().toUpperCase();
        int separator = value.indexOf('_');
        return separator > 0 ? value.substring(0, separator) : value;
    }
}
