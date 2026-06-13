package io.mango.payment.core.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentMangoPayResultMappingServiceTest {

    private final PaymentMangoPayResultMappingService service = new PaymentMangoPayResultMappingService();

    @Test
    @DisplayName("mapPayment should map special return codes to unified payment status")
    void mapPayment_mapsReturnCodes() {
        PaymentMangoPayResultMappingService.PaymentChannelResult defaultResult = service.mapPayment(Map.of());
        assertThat(defaultResult.resultType()).isEqualTo("PROCESSING");
        assertThat(defaultResult.status()).isEqualTo("PAYING");
        assertPayment("SUCCESS", "SUCCESS", "SUCCESS");
        assertPayment("FAILED", "FAILED", "FAILED");
        assertPayment("PARAM_ERROR", "PARAM_ERROR", "FAILED");
        assertPayment("SIGN_ERROR", "SIGN_ERROR", "FAILED");
        assertPayment("CHANNEL_UNAVAILABLE", "CHANNEL_UNAVAILABLE", "FAILED");
        assertPayment("TIMEOUT", "TIMEOUT", "PAYING");
        assertPayment("UNRECOGNIZED", "UNKNOWN", "PAYING");
    }

    @Test
    @DisplayName("mapRefund should map special return codes to unified refund status")
    void mapRefund_mapsReturnCodes() {
        PaymentMangoPayResultMappingService.RefundChannelResult defaultResult = service.mapRefund(Map.of());
        assertThat(defaultResult.resultType()).isEqualTo("PROCESSING");
        assertThat(defaultResult.status()).isEqualTo("REFUNDING");
        assertRefund("SUCCESS", "SUCCESS", "SUCCESS");
        assertRefund("FAILED", "FAILED", "FAILED");
        assertRefund("PARAMETER_ERROR", "PARAM_ERROR", "FAILED");
        assertRefund("SIGNATURE_ERROR", "SIGN_ERROR", "FAILED");
        assertRefund("CHANNEL_UNAVAILABLE", "CHANNEL_UNAVAILABLE", "FAILED");
        assertRefund("TIMEOUT", "TIMEOUT", "REFUNDING");
        assertRefund("UNRECOGNIZED", "UNKNOWN", "REFUNDING");
    }

    private void assertPayment(String scenario, String resultType, String status) {
        PaymentMangoPayResultMappingService.PaymentChannelResult result = service.mapPayment(
                Map.of("mangoPayScenario", scenario));
        assertThat(result.returnCode()).isEqualTo(scenario);
        assertThat(result.resultType()).isEqualTo(resultType);
        assertThat(result.status()).isEqualTo(status);
    }

    private void assertRefund(String scenario, String resultType, String status) {
        PaymentMangoPayResultMappingService.RefundChannelResult result = service.mapRefund(
                Map.of("mangoPayRefundScenario", scenario));
        assertThat(result.returnCode()).isEqualTo(scenario);
        assertThat(result.resultType()).isEqualTo(resultType);
        assertThat(result.status()).isEqualTo(status);
    }
}
