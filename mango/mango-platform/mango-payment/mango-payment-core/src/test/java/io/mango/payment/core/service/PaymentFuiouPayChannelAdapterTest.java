package io.mango.payment.core.service;

import io.mango.common.exception.BizException;
import io.mango.payment.api.enums.PaymentOrderStatusEnum;
import io.mango.payment.api.enums.PaymentRefundOrderStatusEnum;
import io.mango.payment.api.vo.PaymentRefundOrderVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaymentFuiouPayChannelAdapterTest {

    private final PaymentFuiouSignService signService = mock(PaymentFuiouSignService.class);
    private final PaymentFuiouGatewaySignService gatewaySignService = new PaymentFuiouGatewaySignService();
    private final PaymentFuiouPayChannelAdapter adapter = new PaymentFuiouPayChannelAdapter(
            null,
            signService,
            gatewaySignService,
            null,
            null,
            null);

    @Test
    @DisplayName("mapPaymentQuery should map fuiou success to payment success")
    void mapPaymentQuery_success() {
        IPaymentChannelAdapter.PaymentQueryResult result = adapter.mapPaymentQuery(Map.of(
                "result_code", "000000",
                "trans_stat", "SUCCESS"));

        assertThat(result.status()).isEqualTo(PaymentOrderStatusEnum.SUCCESS.getCode());
        assertThat(result.resultType()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("mapPaymentQuery should keep user paying as paying")
    void mapPaymentQuery_userPaying() {
        IPaymentChannelAdapter.PaymentQueryResult result = adapter.mapPaymentQuery(Map.of(
                "result_code", "000000",
                "trans_stat", "USERPAYING"));

        assertThat(result.status()).isEqualTo(PaymentOrderStatusEnum.PAYING.getCode());
    }

    @Test
    @DisplayName("mapPaymentQuery should map closed and revoked to closed")
    void mapPaymentQuery_closed() {
        IPaymentChannelAdapter.PaymentQueryResult result = adapter.mapPaymentQuery(Map.of(
                "result_code", "000000",
                "trans_stat", "CLOSED"));

        assertThat(result.status()).isEqualTo(PaymentOrderStatusEnum.CLOSED.getCode());
    }

    @Test
    @DisplayName("mapPaymentQuery should map failed response to failed")
    void mapPaymentQuery_failed() {
        IPaymentChannelAdapter.PaymentQueryResult result = adapter.mapPaymentQuery(Map.of(
                "result_code", "1010",
                "trans_stat", ""));

        assertThat(result.status()).isEqualTo(PaymentOrderStatusEnum.FAILED.getCode());
    }

    @Test
    @DisplayName("orderType should map cashier QR methods to fuiou scanpay order type")
    void orderType_mapsQrMethods() {
        assertThat(adapter.orderType("PERSONAL_WECHAT_QR")).isEqualTo("WECHAT");
        assertThat(adapter.orderType("PERSONAL_ALIPAY_QR")).isEqualTo("ALIPAY");
    }

    @Test
    @DisplayName("orderType should reject unsupported fuiou methods")
    void orderType_unsupportedMethod_rejects() {
        assertThatThrownBy(() -> adapter.orderType("PERSONAL_EBANK_REDIRECT"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("当前富友通道仅开放微信扫码和支付宝扫码能力");
    }

    @Test
    @DisplayName("preCreateRequest should send client IP and scanpay order type")
    void preCreateRequest_wechatQr_usesClientIpAndWechatOrderType() {
        when(signService.sign(any(), eq("merchant-private-key"))).thenReturn("signed-value");

        Map<String, String> request = adapter.preCreateRequest(
                paymentApplyCommand("PERSONAL_WECHAT_QR", "192.0.2.10"),
                config());

        assertThat(request)
                .containsEntry("version", "1")
                .containsEntry("order_type", "WECHAT")
                .containsEntry("term_id", "88888888")
                .containsEntry("term_ip", "192.0.2.10")
                .containsEntry("notify_url", "https://payment.example.com/callback")
                .containsEntry("reserved_expire_minute", "120")
                .containsEntry("reserved_sub_appid", "")
                .containsEntry("reserved_limit_pay", "")
                .containsEntry("sign", "signed-value");
    }

    @Test
    @DisplayName("preCreateRequest should use business order expire minutes when present")
    void preCreateRequest_withExpireTime_usesRemainingMinutes() {
        when(signService.sign(any(), eq("merchant-private-key"))).thenReturn("signed-value");

        Map<String, String> request = adapter.preCreateRequest(
                paymentApplyCommand("PERSONAL_ALIPAY_QR", "192.0.2.10", LocalDateTime.now().plusMinutes(36)),
                config());

        assertThat(Long.parseLong(request.get("reserved_expire_minute")))
                .isBetween(34L, 36L);
    }

    @Test
    @DisplayName("pcGatewayPayRequest should create B2C bank gateway form fields")
    void pcGatewayPayRequest_personalEbank_usesGatewayMerchantFields() {
        Map<String, String> request = adapter.pcGatewayPayRequest(
                ebankPaymentApplyCommand("PERSONAL_EBANK_REDIRECT"),
                config());

        assertThat(request)
                .containsEntry("mchnt_cd", "0001000F0040992")
                .containsEntry("order_id", "PO202606060001")
                .containsEntry("order_amt", "1")
                .containsEntry("order_pay_type", "B2C")
                .containsEntry("iss_ins_cd", "0803030000")
                .containsEntry("page_notify_url", "https://douxy.inner.yunxinbaokeji.com:1443/#/payment/gateway-result")
                .containsEntry("back_notify_url", "https://douxy.inner.yunxinbaokeji.com:1443/api/payment/channel-callbacks/fuiou_pay")
                .containsEntry("rem", "")
                .containsEntry("ver", "1.0.1")
                .containsKey("md5");
    }

    @Test
    @DisplayName("refundQueryRequest should query by refund order number only")
    void refundQueryRequest_usesRefundOrderNo() {
        when(signService.sign(any(), eq("merchant-private-key"))).thenReturn("signed-value");

        Map<String, String> request = adapter.refundQueryRequest(
                new IPaymentChannelAdapter.RefundQueryCommand(1L, refundOrder()),
                config());

        assertThat(request)
                .containsEntry("version", "1.0")
                .containsEntry("refund_order_no", "RO202606060001")
                .containsEntry("sign", "signed-value")
                .doesNotContainKeys("mchnt_order_no", "order_type");
    }

    @Test
    @DisplayName("mapRefundQueryStatus should map refund query states")
    void mapRefundQueryStatus_mapsStates() {
        assertThat(adapter.mapRefundQueryStatus(Map.of(
                "result_code", "000000",
                "trans_stat", "SUCCESS")))
                .isEqualTo(PaymentRefundOrderStatusEnum.SUCCESS.getCode());
        assertThat(adapter.mapRefundQueryStatus(Map.of(
                "result_code", "000000",
                "trans_stat", "PAYERROR")))
                .isEqualTo(PaymentRefundOrderStatusEnum.FAILED.getCode());
        assertThat(adapter.mapRefundQueryStatus(Map.of(
                "result_code", "9999",
                "trans_stat", "USERPAYING")))
                .isEqualTo(PaymentRefundOrderStatusEnum.REFUNDING.getCode());
    }

    @Test
    @DisplayName("mapPcGatewayPaymentQuery should map paid status to success")
    void mapPcGatewayPaymentQuery_success() {
        IPaymentChannelAdapter.PaymentQueryResult result = adapter.mapPcGatewayPaymentQuery(Map.of(
                "order_pay_code", "0000",
                "order_st", "11"));

        assertThat(result.status()).isEqualTo(PaymentOrderStatusEnum.SUCCESS.getCode());
    }

    @Test
    @DisplayName("applyPayment should reject scanpay request without client IP")
    void applyPayment_withoutClientIp_rejects() {
        assertThatThrownBy(() -> adapter.applyPayment(paymentApplyCommand("PERSONAL_ALIPAY_QR", null)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("富友支付缺少付款人请求 IP");
    }

    private IPaymentChannelAdapter.PaymentApplyCommand paymentApplyCommand(String methodCode, String clientIp) {
        return paymentApplyCommand(methodCode, clientIp, null);
    }

    private IPaymentChannelAdapter.PaymentApplyCommand paymentApplyCommand(String methodCode, String clientIp, LocalDateTime expireTime) {
        return new IPaymentChannelAdapter.PaymentApplyCommand(
                1L,
                "FUIOU_PAY",
                331009L,
                "{}",
                "PO202606060001",
                "BO202606060001",
                methodCode,
                "扫码支付",
                "QR",
                1L,
                "CNY",
                "测试订单",
                expireTime,
                320001L,
                "芒果科技有限公司",
                null,
                null,
                null,
                null,
                clientIp);
    }

    private IPaymentChannelAdapter.PaymentApplyCommand ebankPaymentApplyCommand(String methodCode) {
        return new IPaymentChannelAdapter.PaymentApplyCommand(
                1L,
                "FUIOU_PAY",
                331009L,
                "{}",
                "PO202606060001",
                "BO202606060001",
                methodCode,
                "个人网银",
                "HTML_FORM",
                1L,
                "CNY",
                "测试订单",
                LocalDateTime.of(2026, 6, 6, 10, 30),
                320001L,
                "芒果科技有限公司",
                "0803030000",
                "中国光大银行",
                "6222000000000000",
                "测试用户",
                "192.0.2.10");
    }

    private PaymentRefundOrderVO refundOrder() {
        PaymentRefundOrderVO order = new PaymentRefundOrderVO();
        order.setRefundOrderNo("RO202606060001");
        order.setPayOrderNo("PO202606060001");
        order.setMethodCode("PERSONAL_WECHAT_QR");
        return order;
    }

    private PaymentFuiouPayConfig config() {
        return new PaymentFuiouPayConfig(
                "08A9999999",
                "0002900F0370542",
                "https://fundwx.payfuiouo2o.com",
                "https://payment.example.com/callback",
                "merchant-private-key",
                "fuiou-public-key",
                "mango",
                "0001000F0040992",
                "vau6p7ldawpezyaugc0kopdrrwm4gkpu",
                "http://www-2.wg.fuiou.com:13195/smpGate.do",
                "http://www-2.wg.fuiou.com:13195/smpAQueryGate.do",
                "https://douxy.inner.yunxinbaokeji.com:1443/#/payment/gateway-result",
                "https://douxy.inner.yunxinbaokeji.com:1443/api/payment/channel-callbacks/fuiou_pay");
    }
}
