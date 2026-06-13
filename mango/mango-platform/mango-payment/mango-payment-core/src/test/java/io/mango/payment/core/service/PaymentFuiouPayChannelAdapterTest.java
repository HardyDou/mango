package io.mango.payment.core.service;

import io.mango.common.exception.BizException;
import io.mango.payment.api.enums.PaymentOrderStatusEnum;
import io.mango.payment.api.enums.PaymentRefundOrderStatusEnum;
import io.mango.payment.api.vo.PaymentRefundOrderVO;
import io.mango.payment.core.entity.PaymentMethod;
import io.mango.payment.core.entity.PaymentOrderEntity;
import io.mango.payment.core.mapper.PaymentOrderMapper;
import io.mango.payment.core.mapper.PaymentRefundOrderMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentFuiouPayChannelAdapterTest {

    private final PaymentFuiouSignService signService = mock(PaymentFuiouSignService.class);
    private final PaymentFuiouGatewaySignService gatewaySignService = new PaymentFuiouGatewaySignService();
    private final PaymentOrderMapper paymentOrderMapper = mock(PaymentOrderMapper.class);
    private final PaymentRefundOrderMapper refundOrderMapper = mock(PaymentRefundOrderMapper.class);
    private final PaymentFuiouPayChannelAdapter adapter = new PaymentFuiouPayChannelAdapter(
            null,
            signService,
            gatewaySignService,
            null,
            null,
            null,
            paymentOrderMapper,
            refundOrderMapper);

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

    @Test
    @DisplayName("generateBill should not fabricate bill rows when local successful orders are empty")
    void generateBill_withoutSuccessfulOrders_rejects() {
        PaymentFuiouPayConfigParser configParser = mock(PaymentFuiouPayConfigParser.class);
        when(configParser.parse("{}")).thenReturn(config());
        when(paymentOrderMapper.selectSuccessfulChannelOrdersForBill(
                eq(1L),
                eq("FUIOU_PAY"),
                eq(331009L),
                eq(LocalDate.of(2026, 6, 12)),
                eq(LocalDate.of(2026, 6, 13))))
                .thenReturn(List.of());
        when(refundOrderMapper.selectSuccessfulChannelRefundsForBill(
                eq(1L),
                eq("FUIOU_PAY"),
                eq(331009L),
                eq(LocalDate.of(2026, 6, 12)),
                eq(LocalDate.of(2026, 6, 13))))
                .thenReturn(List.of());
        PaymentFuiouPayChannelAdapter billAdapter = new PaymentFuiouPayChannelAdapter(
                configParser,
                signService,
                gatewaySignService,
                null,
                tenantConfigMapper("{}"),
                null,
                paymentOrderMapper,
                refundOrderMapper);

        assertThatThrownBy(() -> billAdapter.generateBill(new IPaymentChannelAdapter.ChannelBillCommand(
                1L,
                "FUIOU_PAY",
                331009L,
                LocalDate.of(2026, 6, 12))))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("账单日期内没有富友确认成功的支付或退款订单");
    }

    @Test
    @DisplayName("generateBill should query historical scanpay orders through hisTradeQuery")
    void generateBill_oldScanpayPayment_usesHisTradeQuery() {
        PaymentFuiouPayConfigParser configParser = mock(PaymentFuiouPayConfigParser.class);
        PaymentFuiouHttpClient httpClient = mock(PaymentFuiouHttpClient.class);
        io.mango.payment.core.mapper.PaymentMethodMapper methodMapper =
                mock(io.mango.payment.core.mapper.PaymentMethodMapper.class);
        LocalDate billDate = LocalDate.now().minusDays(4);
        PaymentOrderEntity order = successfulPaymentOrder(billDate.atTime(10, 30));
        when(configParser.parse("{}")).thenReturn(config());
        when(paymentOrderMapper.selectSuccessfulChannelOrdersForBill(
                eq(1L), eq("FUIOU_PAY"), eq(331009L), eq(billDate), eq(billDate.plusDays(1))))
                .thenReturn(List.of(order));
        when(refundOrderMapper.selectSuccessfulChannelRefundsForBill(
                eq(1L), eq("FUIOU_PAY"), eq(331009L), eq(billDate), eq(billDate.plusDays(1))))
                .thenReturn(List.of());
        PaymentMethod method = new PaymentMethod();
        method.setId(320001L);
        method.setTenantId(1L);
        method.setMethodCode("PERSONAL_WECHAT_QR");
        method.setDelFlag(0);
        when(methodMapper.selectById(320001L)).thenReturn(method);
        when(signService.sign(any(), eq("merchant-private-key"))).thenReturn("signed-value");
        when(signService.verify(any(), eq("fuiou-public-key"))).thenReturn(true);
        when(httpClient.post(eq("https://fundwx.payfuiouo2o.com/hisTradeQuery"), any()))
                .thenReturn(Map.of(
                        "result_code", "000000",
                        "trans_stat", "SUCCESS",
                        "transaction_id", "4200000000000001",
                        "order_amt", "1",
                        "reserved_txn_fin_ts", billDate.format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE) + "103000"));
        PaymentFuiouPayChannelAdapter billAdapter = new PaymentFuiouPayChannelAdapter(
                configParser,
                signService,
                gatewaySignService,
                httpClient,
                tenantConfigMapper("{}"),
                methodMapper,
                paymentOrderMapper,
                refundOrderMapper);

        IPaymentChannelAdapter.ChannelBillResult result = billAdapter.generateBill(
                new IPaymentChannelAdapter.ChannelBillCommand(1L, "FUIOU_PAY", 331009L, billDate));

        assertThat(result.rows()).hasSize(1);
        verify(httpClient).post(eq("https://fundwx.payfuiouo2o.com/hisTradeQuery"), any());
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

    private PaymentOrderEntity successfulPaymentOrder(LocalDateTime payTime) {
        PaymentOrderEntity order = new PaymentOrderEntity();
        order.setId(1L);
        order.setTenantId(1L);
        order.setPayOrderNo("PO202606060001");
        order.setMethodId(320001L);
        order.setAmount(1L);
        order.setStatus(PaymentOrderStatusEnum.SUCCESS.getCode());
        order.setSuccessFlag(1);
        order.setPayTime(payTime);
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

    private io.mango.payment.core.mapper.PaymentChannelContractMapper tenantConfigMapper(String configValuesJson) {
        io.mango.payment.core.mapper.PaymentChannelContractMapper mapper =
                mock(io.mango.payment.core.mapper.PaymentChannelContractMapper.class);
        when(mapper.selectActiveConfigValuesJson(1L, 331009L)).thenReturn(configValuesJson);
        return mapper;
    }
}
