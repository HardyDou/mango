package io.mango.payment.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.exception.BizException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaymentFuiouPayConfigParserTest {

    private final PaymentSensitiveValueService sensitiveValueService = mock(PaymentSensitiveValueService.class);
    private final PaymentFuiouPayConfigParser parser = new PaymentFuiouPayConfigParser(
            new ObjectMapper(),
            sensitiveValueService);

    @Test
    @DisplayName("parse should accept implemented scanpay signing fields without terminal IP")
    void parse_scanpayConfigWithoutTerminalIp_returnsConfig() {
        when(sensitiveValueService.decrypt(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentFuiouPayConfig config = parser.parse("""
                {
                  "insCd": "08A9999999",
                  "merchantNo": "0002900F0370542",
                  "gatewayBaseUrl": "https://fundwx.payfuiouo2o.com",
                  "notifyUrl": "https://payment.example.com/callback",
                  "privateKey": "merchant-private-key",
                  "fuiouPublicKey": "fuiou-public-key",
                  "operatorId": "mango"
                }
                """);

        assertThat(config.insCd()).isEqualTo("08A9999999");
        assertThat(config.merchantNo()).isEqualTo("0002900F0370542");
        assertThat(config.scanpayGatewayBaseUrl()).isEqualTo("https://fundwx.payfuiouo2o.com");
        assertThat(config.privateKey()).isEqualTo("merchant-private-key");
        assertThat(config.fuiouPublicKey()).isEqualTo("fuiou-public-key");
    }

    @Test
    @DisplayName("parse should accept pc gateway merchant fields without scanpay keys")
    void parse_pcGatewayConfig_returnsConfig() {
        when(sensitiveValueService.decrypt(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentFuiouPayConfig config = parser.parse("""
                {
                  "gatewayMerchantNo": "0001000F0040992",
                  "gatewayMerchantKey": "merchant-key",
                  "gatewayPayUrl": "http://www-2.wg.fuiou.com:13195/smpGate.do",
                  "gatewayQueryUrl": "http://www-2.wg.fuiou.com:13195/smpAQueryGate.do",
                  "gatewayPageNotifyUrl": "http://payment.example.com/fuiou/page",
                  "gatewayBackNotifyUrl": "http://payment.example.com/fuiou/back"
                }
                """);

        assertThat(config.gatewayMerchantNo()).isEqualTo("0001000F0040992");
        assertThat(config.gatewayMerchantKey()).isEqualTo("merchant-key");
        assertThat(config.gatewayPayUrl()).isEqualTo("http://www-2.wg.fuiou.com:13195/smpGate.do");
    }

    @Test
    @DisplayName("parse should reject config without required signing keys")
    void parse_missingRsaKey_rejects() {
        when(sensitiveValueService.decrypt(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatThrownBy(() -> parser.parse("""
                {
                  "insCd": "08A9999999",
                  "merchantNo": "0002900F0370542",
                  "notifyUrl": "https://payment.example.com/callback"
                }
                """))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("富友签约配置缺少商户私钥");
    }
}
