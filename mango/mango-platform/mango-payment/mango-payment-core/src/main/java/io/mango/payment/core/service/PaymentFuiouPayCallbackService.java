package io.mango.payment.core.service;

import io.mango.common.result.Require;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.payment.api.PaymentCode;
import io.mango.payment.api.command.PaymentChannelCallbackCommand;
import io.mango.payment.api.enums.PaymentChannelCode;
import io.mango.payment.api.enums.PaymentOrderStatusEnum;
import io.mango.payment.core.entity.PaymentOrderEntity;
import io.mango.payment.core.mapper.PaymentChannelContractMapper;
import io.mango.payment.core.mapper.PaymentOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.net.URLDecoder;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentFuiouPayCallbackService implements IPaymentChannelCallbackHandler {

    private static final String SUCCESS_RESPONSE = "1";
    private static final String APP_CODE = "payment-callback";
    private static final String SYSTEM_PRINCIPAL = "fuiou-callback";
    private static final String RESULT_SUCCESS = "000000";
    private static final String PC_GATEWAY_SUCCESS_PAY_CODE = "0000";
    private static final String PC_GATEWAY_STATUS_SUCCESS = "11";
    private static final String PC_GATEWAY_STATUS_CANCELLED = "01";
    private static final String PC_GATEWAY_STATUS_EXPIRED = "03";
    private static final String PC_GATEWAY_STATUS_FAILED = "05";
    private static final int MAX_REQ_DECODE_COUNT = 2;
    private static final DateTimeFormatter FUIOU_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final PaymentFuiouXmlCodec xmlCodec;
    private final PaymentFuiouSignService signService;
    private final PaymentFuiouGatewaySignService gatewaySignService;
    private final PaymentFuiouPayConfigParser configParser;
    private final PaymentOrderMapper paymentOrderMapper;
    private final PaymentChannelContractMapper channelContractMapper;
    private final PaymentChannelCallbackService callbackService;

    @Override
    public String channelCode() {
        return PaymentChannelCode.FUIOU_PAY.name();
    }

    @Transactional(rollbackFor = Exception.class)
    public PaymentChannelCallbackHandleResult handle(PaymentChannelRawCallback callback) {
        Require.notNull(callback, PaymentCode.PAYMENT_ORDER_STATE_INVALID.getCode(), "富友回调不能为空");
        return PaymentChannelCallbackHandleResult.text(handle(callback.params()));
    }

    @Transactional(rollbackFor = Exception.class)
    String handle(Map<String, String> params) {
        Require.notNull(params, PaymentCode.PAYMENT_ORDER_STATE_INVALID.getCode(), "富友回调参数不能为空");
        String req = PaymentContextSupport.trimToNull(params.get("req"));
        if (req != null) {
            return handleScanpay(req);
        }
        return handlePcGateway(params);
    }

    String handle(String req) {
        return handleScanpay(req);
    }

    private String handleScanpay(String req) {
        Require.notBlank(req, PaymentCode.PAYMENT_ORDER_STATE_INVALID.getCode(), "富友回调 req 不能为空");
        FuiouScanpayCallback callback = parseScanpayCallback(req);
        String payOrderNo = callback.payOrderNo();
        Require.notBlank(payOrderNo, PaymentCode.PAYMENT_ORDER_NOT_FOUND.getCode(), "富友回调缺少商户订单号");
        PaymentOrderEntity order = paymentOrderMapper.selectByPayOrderNo(payOrderNo);
        Require.notNull(order, PaymentCode.PAYMENT_ORDER_NOT_FOUND);
        Require.isTrue(PaymentChannelCode.FUIOU_PAY.name().equals(order.getChannelCode()),
                PaymentCode.PAYMENT_CHANNEL_INVALID.getCode(), "富友回调订单通道不匹配");
        PaymentFuiouPayConfig config = configParser.parse(requiredContractConfig(order.getTenantId(), order.getContractId()));
        Require.isTrue(signService.verify(callback.fields(), config.fuiouPublicKey()),
                PaymentCode.PAYMENT_CHANNEL_INVALID.getCode(), "富友回调验签失败");

        MangoContextSnapshot previous = MangoContextHolder.get();
        try {
            bindContext(order.getTenantId());
            callbackService.handle(callbackCommand(callback, order));
            return SUCCESS_RESPONSE;
        } finally {
            MangoContextHolder.set(previous);
        }
    }

    FuiouScanpayCallback parseScanpayCallback(String req) {
        Require.notBlank(req, PaymentCode.PAYMENT_ORDER_STATE_INVALID.getCode(), "富友回调 req 不能为空");
        Map<String, String> fields = xmlCodec.decode(normalizeXmlReq(req));
        return new FuiouScanpayCallback(
                fields,
                PaymentContextSupport.trimToNull(fields.get("mchnt_order_no")),
                PaymentContextSupport.trimToNull(fields.get("transaction_id")),
                PaymentContextSupport.trimToNull(fields.get("mchnt_cd")),
                channelStatus(fields.get("result_code")),
                amount(fields.get("order_amt")),
                eventTime(fields.get("txn_fin_ts")),
                PaymentContextSupport.trimToNull(fields.get("result_code")),
                PaymentContextSupport.trimToNull(fields.get("result_msg")));
    }

    String normalizeXmlReq(String req) {
        String current = req.trim();
        for (int index = 0; index < MAX_REQ_DECODE_COUNT && !current.startsWith("<"); index++) {
            String decoded = URLDecoder.decode(current, PaymentFuiouSignService.FUIOU_CHARSET);
            if (decoded.equals(current)) {
                break;
            }
            current = decoded.trim();
        }
        return current;
    }

    private String handlePcGateway(Map<String, String> fields) {
        String payOrderNo = PaymentContextSupport.trimToNull(fields.get("order_id"));
        Require.notBlank(payOrderNo, PaymentCode.PAYMENT_ORDER_NOT_FOUND.getCode(), "富友网关回调缺少商户订单号");
        PaymentOrderEntity order = paymentOrderMapper.selectByPayOrderNo(payOrderNo);
        Require.notNull(order, PaymentCode.PAYMENT_ORDER_NOT_FOUND);
        Require.isTrue(PaymentChannelCode.FUIOU_PAY.name().equals(order.getChannelCode()),
                PaymentCode.PAYMENT_CHANNEL_INVALID.getCode(), "富友网关回调订单通道不匹配");
        PaymentFuiouPayConfig config = configParser.parse(requiredContractConfig(order.getTenantId(), order.getContractId()));
        configParser.validateForPcGateway(config);
        Require.isTrue(gatewaySignService.verifyCallback(fields, config.gatewayMerchantKey()),
                PaymentCode.PAYMENT_CHANNEL_INVALID.getCode(), "富友网关回调验签失败");

        MangoContextSnapshot previous = MangoContextHolder.get();
        try {
            bindContext(order.getTenantId());
            callbackService.handle(pcGatewayCallbackCommand(fields, order, config));
            return SUCCESS_RESPONSE;
        } finally {
            MangoContextHolder.set(previous);
        }
    }

    private PaymentChannelCallbackCommand callbackCommand(FuiouScanpayCallback callback, PaymentOrderEntity order) {
        PaymentChannelCallbackCommand command = new PaymentChannelCallbackCommand();
        command.setCallbackType("PAYMENT");
        command.setChannelCode(PaymentChannelCode.FUIOU_PAY.name());
        command.setPayOrderNo(order.getPayOrderNo());
        command.setChannelTradeNo(callback.channelTradeNo());
        command.setChannelMerchantNo(callback.channelMerchantNo());
        command.setChannelStatus(callback.channelStatus());
        command.setAmount(callback.amount());
        command.setEventTime(callback.eventTime());
        command.setChannelReturnCode(callback.channelReturnCode());
        command.setChannelMessage(callback.channelMessage());
        return command;
    }

    record FuiouScanpayCallback(
            Map<String, String> fields,
            String payOrderNo,
            String channelTradeNo,
            String channelMerchantNo,
            String channelStatus,
            Long amount,
            LocalDateTime eventTime,
            String channelReturnCode,
            String channelMessage) {
    }

    private PaymentChannelCallbackCommand pcGatewayCallbackCommand(
            Map<String, String> fields,
            PaymentOrderEntity order,
            PaymentFuiouPayConfig config) {
        PaymentChannelCallbackCommand command = new PaymentChannelCallbackCommand();
        command.setCallbackType("PAYMENT");
        command.setChannelCode(PaymentChannelCode.FUIOU_PAY.name());
        command.setPayOrderNo(order.getPayOrderNo());
        command.setChannelTradeNo(PaymentContextSupport.trimToNull(fields.get("fy_ssn")));
        command.setChannelMerchantNo(config.gatewayMerchantNo());
        command.setChannelStatus(pcGatewayChannelStatus(fields.get("order_pay_code"), fields.get("order_st")));
        command.setAmount(amount(fields.get("order_amt")));
        command.setEventTime(LocalDateTime.now());
        command.setChannelReturnCode(PaymentContextSupport.trimToNull(fields.get("order_pay_code")));
        command.setChannelMessage(PaymentContextSupport.trimToNull(fields.get("order_pay_error")));
        return command;
    }

    private String requiredContractConfig(Long tenantId, Long contractId) {
        Require.notNull(tenantId, PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID.getCode(), "富友回调缺少租户 ID");
        Require.notNull(contractId, PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID.getCode(), "富友回调缺少签约 ID");
        String configValuesJson = channelContractMapper.selectActiveConfigValuesJson(tenantId, contractId);
        Require.notBlank(configValuesJson, PaymentCode.PAYMENT_CHANNEL_CONTRACT_NOT_FOUND.getCode(), "富友签约配置不存在或未启用");
        return configValuesJson;
    }

    private String channelStatus(String resultCode) {
        if (RESULT_SUCCESS.equals(PaymentContextSupport.trimToNull(resultCode))) {
            return PaymentOrderStatusEnum.SUCCESS.getCode();
        }
        return PaymentOrderStatusEnum.FAILED.getCode();
    }

    private String pcGatewayChannelStatus(String payCode, String orderStatus) {
        if (PC_GATEWAY_SUCCESS_PAY_CODE.equals(PaymentContextSupport.trimToNull(payCode))
                && PC_GATEWAY_STATUS_SUCCESS.equals(PaymentContextSupport.trimToNull(orderStatus))) {
            return PaymentOrderStatusEnum.SUCCESS.getCode();
        }
        if (PC_GATEWAY_STATUS_CANCELLED.equals(orderStatus) || PC_GATEWAY_STATUS_EXPIRED.equals(orderStatus)) {
            return PaymentOrderStatusEnum.CLOSED.getCode();
        }
        if (PC_GATEWAY_STATUS_FAILED.equals(orderStatus)) {
            return PaymentOrderStatusEnum.FAILED.getCode();
        }
        return PaymentOrderStatusEnum.PAYING.getCode();
    }

    private Long amount(String value) {
        String amount = PaymentContextSupport.trimToNull(value);
        Require.notBlank(amount, PaymentCode.PAYMENT_AMOUNT_INVALID.getCode(), "富友回调缺少订单金额");
        try {
            return Long.valueOf(amount);
        } catch (NumberFormatException ex) {
            return Require.fail(PaymentCode.PAYMENT_AMOUNT_INVALID.getCode(), "富友回调订单金额非法: " + amount);
        }
    }

    private LocalDateTime eventTime(String value) {
        String text = PaymentContextSupport.trimToNull(value);
        if (text == null) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(text, FUIOU_TIME_FORMATTER);
        } catch (DateTimeParseException ex) {
            return Require.fail(PaymentCode.PAYMENT_ORDER_STATE_INVALID.getCode(), "富友回调支付完成时间非法: " + text);
        }
    }

    private void bindContext(Long tenantId) {
        MangoContextSnapshot current = MangoContextHolder.get();
        MangoContextHolder.set(current.withSecurity(null, String.valueOf(tenantId), SYSTEM_PRINCIPAL,
                null, "SYSTEM", "SYSTEM", null, APP_CODE));
    }
}
