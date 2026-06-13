package io.mango.payment.core.service;

import io.mango.common.result.Require;
import io.mango.payment.api.PaymentCode;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 支付通道公网回调处理器注册表。
 */
@Service
public class PaymentChannelCallbackHandlerRegistry {

    private final Map<String, IPaymentChannelCallbackHandler> handlers;

    public PaymentChannelCallbackHandlerRegistry(List<IPaymentChannelCallbackHandler> handlers) {
        this.handlers = handlers.stream().collect(Collectors.toUnmodifiableMap(
                handler -> normalize(handler.channelCode()),
                Function.identity(),
                (left, right) -> Require.fail(PaymentCode.PAYMENT_CHANNEL_INVALID.getCode(),
                        "支付通道回调处理器重复接入: " + left.channelCode())));
    }

    public PaymentChannelCallbackHandleResult handle(PaymentChannelRawCallback callback) {
        Require.notNull(callback, PaymentCode.PAYMENT_CHANNEL_INVALID.getCode(), "支付通道回调不能为空");
        return requireHandler(callback.channelCode()).handle(callback);
    }

    public IPaymentChannelCallbackHandler requireHandler(String channelCode) {
        String normalized = normalize(channelCode);
        Require.notBlank(normalized, PaymentCode.PAYMENT_CHANNEL_INVALID.getCode(), "支付通道编码不能为空");
        IPaymentChannelCallbackHandler handler = handlers.get(normalized);
        Require.notNull(handler, PaymentCode.PAYMENT_CHANNEL_INVALID.getCode(), "支付通道回调处理器未接入");
        return handler;
    }

    private String normalize(String channelCode) {
        String normalized = PaymentContextSupport.trimToNull(channelCode);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }
}
