package io.mango.payment.core.service;

import io.mango.common.result.Require;
import io.mango.payment.api.PaymentCode;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PaymentChannelAdapterRegistry {

    private final Map<String, IPaymentChannelAdapter> adapters;

    public PaymentChannelAdapterRegistry(List<IPaymentChannelAdapter> adapters) {
        this.adapters = adapters.stream().collect(Collectors.toUnmodifiableMap(
                adapter -> normalize(adapter.channelCode()),
                Function.identity()));
    }

    public IPaymentChannelAdapter requireAdapter(String channelCode) {
        String normalized = normalize(channelCode);
        Require.notBlank(normalized, PaymentCode.PAYMENT_CHANNEL_INVALID.getCode(), "支付通道编码不能为空");
        IPaymentChannelAdapter adapter = adapters.get(normalized);
        Require.notNull(adapter, PaymentCode.PAYMENT_CHANNEL_INVALID.getCode(), "支付通道适配器未接入");
        return adapter;
    }

    private String normalize(String channelCode) {
        String normalized = PaymentContextSupport.trimToNull(channelCode);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }
}
