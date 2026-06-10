package io.mango.payment.core.service;

import io.mango.payment.api.enums.PaymentOrderStatusEnum;
import io.mango.payment.api.enums.PaymentRefundOrderStatusEnum;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;

@Service
public class PaymentMangoPayResultMappingService {

    public PaymentChannelResult mapPayment(Map<String, String> values) {
        String scenario = scenario(values, "mangoPayScenario");
        if (scenario == null) {
            return new PaymentChannelResult(null, "PROCESSING", "PROCESSING", PaymentOrderStatusEnum.PAYING.getCode());
        }
        return switch (scenario) {
            case "SUCCESS", "OK" -> payment(scenario, "SUCCESS", PaymentOrderStatusEnum.SUCCESS.getCode());
            case "PROCESSING", "PAYING", "TIMEOUT", "UNKNOWN" ->
                    payment(scenario, resultType(scenario), PaymentOrderStatusEnum.PAYING.getCode());
            case "CLOSED" -> payment(scenario, "CLOSED", PaymentOrderStatusEnum.CLOSED.getCode());
            case "FAIL", "FAILED", "PARAM_ERROR", "PARAMETER_ERROR", "SIGN_ERROR", "SIGNATURE_ERROR", "CHANNEL_UNAVAILABLE" ->
                    payment(scenario, resultType(scenario), PaymentOrderStatusEnum.FAILED.getCode());
            default -> payment(scenario, "UNKNOWN", PaymentOrderStatusEnum.PAYING.getCode());
        };
    }

    public RefundChannelResult mapRefund(Map<String, String> values) {
        String scenario = scenario(values, "mangoPayRefundScenario");
        if (scenario == null) {
            return new RefundChannelResult(null, "PROCESSING", "PROCESSING", PaymentRefundOrderStatusEnum.REFUNDING.getCode());
        }
        return switch (scenario) {
            case "SUCCESS", "OK" -> refund(scenario, "SUCCESS", PaymentRefundOrderStatusEnum.SUCCESS.getCode());
            case "PROCESSING", "REFUNDING", "TIMEOUT", "UNKNOWN" ->
                    refund(scenario, resultType(scenario), PaymentRefundOrderStatusEnum.REFUNDING.getCode());
            case "CLOSED" -> refund(scenario, "CLOSED", PaymentRefundOrderStatusEnum.CLOSED.getCode());
            case "FAIL", "FAILED", "PARAM_ERROR", "PARAMETER_ERROR", "SIGN_ERROR", "SIGNATURE_ERROR", "CHANNEL_UNAVAILABLE" ->
                    refund(scenario, resultType(scenario), PaymentRefundOrderStatusEnum.FAILED.getCode());
            default -> refund(scenario, "UNKNOWN", PaymentRefundOrderStatusEnum.REFUNDING.getCode());
        };
    }

    private PaymentChannelResult payment(String returnCode, String resultType, String status) {
        return new PaymentChannelResult(returnCode, returnCode, resultType, status);
    }

    private RefundChannelResult refund(String returnCode, String resultType, String status) {
        return new RefundChannelResult(returnCode, returnCode, resultType, status);
    }

    private String resultType(String returnCode) {
        return switch (returnCode) {
            case "PROCESSING", "PAYING", "REFUNDING" -> "PROCESSING";
            case "TIMEOUT" -> "TIMEOUT";
            case "PARAM_ERROR", "PARAMETER_ERROR" -> "PARAM_ERROR";
            case "SIGN_ERROR", "SIGNATURE_ERROR" -> "SIGN_ERROR";
            case "CHANNEL_UNAVAILABLE" -> "CHANNEL_UNAVAILABLE";
            case "FAIL", "FAILED" -> "FAILED";
            default -> "UNKNOWN";
        };
    }

    private String scenario(Map<String, String> values, String primaryKey) {
        if (values == null) {
            return null;
        }
        String value = PaymentContextSupport.trimToNull(values.get(primaryKey));
        return value == null ? null : value.toUpperCase(Locale.ROOT);
    }

    public record PaymentChannelResult(
            String scenario,
            String returnCode,
            String resultType,
            String status
    ) {
    }

    public record RefundChannelResult(
            String scenario,
            String returnCode,
            String resultType,
            String status
    ) {
    }
}
