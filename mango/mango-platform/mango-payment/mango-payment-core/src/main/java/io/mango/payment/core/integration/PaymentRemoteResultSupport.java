package io.mango.payment.core.integration;

import io.mango.common.result.BizCode;
import io.mango.common.result.R;
import io.mango.common.result.Require;

/** 支付域远程响应适配器，将传输层包装隔离在业务服务之外。 */
public final class PaymentRemoteResultSupport {

    private PaymentRemoteResultSupport() {}

    public static <T> PaymentRemoteOutcome<T> outcome(R<T> response) {
        if (response == null) {
            return new PaymentRemoteOutcome<>(false, null, null);
        }
        return new PaymentRemoteOutcome<>(response.isSuccess(), response.getData(), response.getMsg());
    }

    public static <T> T requireData(R<T> response, BizCode bizCode, String fallbackMessage) {
        PaymentRemoteOutcome<T> outcome = outcome(response);
        Require.isTrue(
                outcome.success(),
                bizCode,
                outcome.message() == null || outcome.message().isBlank()
                        ? fallbackMessage
                        : outcome.message());
        return outcome.data();
    }
}
