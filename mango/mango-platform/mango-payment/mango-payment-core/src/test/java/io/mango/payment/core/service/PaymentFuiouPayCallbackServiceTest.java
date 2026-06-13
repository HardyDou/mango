package io.mango.payment.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.payment.api.command.PaymentChannelCallbackCommand;
import io.mango.payment.api.enums.PaymentChannelCode;
import io.mango.payment.api.enums.PaymentOrderStatusEnum;
import io.mango.payment.core.entity.PaymentOrderEntity;
import io.mango.payment.core.mapper.PaymentChannelContractMapper;
import io.mango.payment.core.mapper.PaymentOrderMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentFuiouPayCallbackServiceTest {

    private static final String SCANPAY_PUBLIC_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCBv9K+"
            + "jiuHqXIehX81oyNSD2RfVn+KTPb7NRT5HDPFE35CjZJd7Fu40r0U2Cp7Eyhayv/mRS6ZqvBT/8tQqwp"
            + "UExTQQBbdZjfk+efb9bF9a+uCnAg0RsuqxeJ2r/rRTsORzVLJy+4GKcv06/p6CcBc5BI1gqSKmyyNBlgfkxLYewIDAQAB";

    private final PaymentFuiouXmlCodec xmlCodec = new PaymentFuiouXmlCodec();
    private final PaymentFuiouSignService signService = new PaymentFuiouSignService();
    private final PaymentOrderMapper paymentOrderMapper = mock(PaymentOrderMapper.class);
    private final PaymentChannelContractMapper channelContractMapper = mock(PaymentChannelContractMapper.class);
    private final PaymentChannelCallbackService callbackService = mock(PaymentChannelCallbackService.class);
    private final PaymentFuiouPayCallbackService service = new PaymentFuiouPayCallbackService(
            xmlCodec,
            signService,
            new PaymentFuiouGatewaySignService(),
            new PaymentFuiouPayConfigParser(new ObjectMapper(), new PaymentSensitiveValueService(null)),
            paymentOrderMapper,
            channelContractMapper,
            callbackService);

    @Test
    @DisplayName("parseScanpayCallback should parse real fuiou success callback req")
    void parseScanpayCallback_realSuccessReq_parsesCommandFields() {
        PaymentFuiouPayCallbackService.FuiouScanpayCallback callback =
                service.parseScanpayCallback(realScanpaySuccessReq());

        assertThat(callback.payOrderNo()).isEqualTo("PO2026061200000030");
        assertThat(callback.channelMerchantNo()).isEqualTo("0002900F0370542");
        assertThat(callback.channelTradeNo()).isEqualTo("2026061223001429301459310001");
        assertThat(callback.channelStatus()).isEqualTo(PaymentOrderStatusEnum.SUCCESS.getCode());
        assertThat(callback.amount()).isEqualTo(1L);
        assertThat(callback.eventTime()).isEqualTo(LocalDateTime.of(2026, 6, 12, 17, 4, 44));
        assertThat(callback.channelReturnCode()).isEqualTo("000000");
        assertThat(callback.channelMessage()).isEqualTo("SUCCESS");
        assertThat(signService.verify(callback.fields(), SCANPAY_PUBLIC_KEY)).isTrue();
    }

    @Test
    @DisplayName("handle should verify real fuiou callback and submit standardized payment callback")
    void handle_realSuccessReq_submitsStandardPaymentCallback() {
        PaymentOrderEntity order = new PaymentOrderEntity();
        order.setPayOrderNo("PO2026061200000030");
        order.setTenantId(1L);
        order.setContractId(331009L);
        order.setChannelCode(PaymentChannelCode.FUIOU_PAY.name());
        when(paymentOrderMapper.selectByPayOrderNo("PO2026061200000030")).thenReturn(order);
        when(channelContractMapper.selectActiveConfigValuesJson(1L, 331009L)).thenReturn(configJson());

        PaymentChannelCallbackHandleResult result = service.handle(new PaymentChannelRawCallback(
                PaymentChannelCode.FUIOU_PAY.name(),
                "POST",
                "/api/payment/channel-callbacks/fuiou_pay",
                null,
                "application/x-www-form-urlencoded",
                "127.0.0.1",
                Map.of("req", realScanpaySuccessReq()),
                null,
                LocalDateTime.now()));

        assertThat(service.channelCode()).isEqualTo(PaymentChannelCode.FUIOU_PAY.name());
        assertThat(result.responseBody()).isEqualTo("1");
        ArgumentCaptor<PaymentChannelCallbackCommand> captor = ArgumentCaptor.forClass(PaymentChannelCallbackCommand.class);
        verify(callbackService).handle(captor.capture());
        PaymentChannelCallbackCommand command = captor.getValue();
        assertThat(command.getCallbackType()).isEqualTo("PAYMENT");
        assertThat(command.getChannelCode()).isEqualTo(PaymentChannelCode.FUIOU_PAY.name());
        assertThat(command.getPayOrderNo()).isEqualTo("PO2026061200000030");
        assertThat(command.getChannelTradeNo()).isEqualTo("2026061223001429301459310001");
        assertThat(command.getChannelMerchantNo()).isEqualTo("0002900F0370542");
        assertThat(command.getChannelStatus()).isEqualTo(PaymentOrderStatusEnum.SUCCESS.getCode());
        assertThat(command.getAmount()).isEqualTo(1L);
        assertThat(command.getEventTime()).isEqualTo(LocalDateTime.of(2026, 6, 12, 17, 4, 44));
        assertThat(command.getChannelReturnCode()).isEqualTo("000000");
        assertThat(command.getChannelMessage()).isEqualTo("SUCCESS");
    }

    private String realScanpaySuccessReq() {
        try {
            return new String(
                    getClass().getResourceAsStream("/payment/fuiou/scanpay-success-callback-urlencoded.txt").readAllBytes(),
                    StandardCharsets.UTF_8).trim();
        } catch (Exception ex) {
            throw new IllegalStateException("富友真实回调测试报文读取失败", ex);
        }
    }

    private String configJson() {
        return """
                {
                  "insCd": "08A9999999",
                  "merchantNo": "0002900F0370542",
                  "scanpayGatewayBaseUrl": "https://fundwx.payfuiouo2o.com",
                  "notifyUrl": "https://douxy.inner.yunxinbaokeji.com:1443/api/payment/channel-callbacks/fuiou_pay",
                  "privateKey": "merchant-private-key",
                  "fuiouPublicKey": "%s"
                }
                """.formatted(SCANPAY_PUBLIC_KEY);
    }
}
