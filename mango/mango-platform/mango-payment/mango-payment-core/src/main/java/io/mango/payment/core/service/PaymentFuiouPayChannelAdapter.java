package io.mango.payment.core.service;

import io.mango.common.exception.BizException;
import io.mango.common.result.Require;
import io.mango.payment.api.PaymentCode;
import io.mango.payment.api.enums.PaymentChannelCode;
import io.mango.payment.api.enums.PaymentOrderStatusEnum;
import io.mango.payment.api.enums.PaymentRefundOrderStatusEnum;
import io.mango.payment.api.vo.PaymentCashierPayMaterialVO;
import io.mango.payment.core.mapper.PaymentChannelContractMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentFuiouPayChannelAdapter implements IPaymentChannelAdapter {

    private static final String CHANNEL_CODE = PaymentChannelCode.FUIOU_PAY.name();
    private static final String RESULT_SUCCESS = "000000";
    private static final String ORDER_TYPE_ALIPAY = "ALIPAY";
    private static final String METHOD_ALIPAY_QR = "PERSONAL_ALIPAY_QR";
    private static final String MATERIAL_QR = "QR";
    private static final int GOODS_DESCRIPTION_MAX_LENGTH = 128;
    private static final int RANDOM_STRING_LENGTH = 32;
    private static final long MAX_EXPIRE_MINUTES = 1440L;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final char[] RANDOM_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();

    private final PaymentFuiouPayConfigParser configParser;
    private final PaymentFuiouSignService signService;
    private final PaymentFuiouHttpClient httpClient;
    private final PaymentChannelContractMapper channelContractMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String channelCode() {
        return CHANNEL_CODE;
    }

    @Override
    public PaymentApplyResult applyPayment(PaymentApplyCommand command) {
        validatePaymentCommand(command);
        PaymentFuiouPayConfig config = configParser.parse(command.contractConfigValuesJson());
        Map<String, String> request = preCreateRequest(command, config);
        Map<String, String> response = call(config, "/preCreate", request);
        String resultCode = response.get("result_code");
        Require.isTrue(RESULT_SUCCESS.equals(resultCode), PaymentCode.PAYMENT_CASHIER_PAY_INVALID.getCode(),
                "富友下单失败：" + response.getOrDefault("result_msg", resultCode));
        Require.notBlank(response.get("qr_code"), PaymentCode.PAYMENT_CASHIER_PAY_INVALID.getCode(), "富友下单未返回二维码");
        PaymentCashierPayMaterialVO material = new PaymentCashierPayMaterialVO();
        material.setMaterialType(command.paymentMaterialType());
        material.setQrContent(response.get("qr_code"));
        material.setExpireTime(command.expireTime());
        return new PaymentApplyResult(
                "FUIOU_PRE_CREATE",
                resultCode,
                "ASYNC_PROCESSING",
                PaymentOrderStatusEnum.PAYING.getCode(),
                response.getOrDefault("reserved_fy_order_no", command.payOrderNo()),
                material);
    }

    @Override
    public RefundApplyResult applyRefund(RefundApplyCommand command) {
        validateRefundCommand(command);
        PaymentFuiouPayConfig config = configParser.parse(requiredContractConfig(command.tenantId(), command.contractId()));
        Map<String, String> request = refundRequest(command, config);
        Map<String, String> response = call(config, "/commonRefund", request);
        String resultCode = response.get("result_code");
        Require.isTrue(RESULT_SUCCESS.equals(resultCode), PaymentCode.PAYMENT_REFUND_ORDER_INVALID.getCode(),
                "富友退款申请失败：" + response.getOrDefault("result_msg", resultCode));
        return new RefundApplyResult(
                "FUIOU_REFUND_APPLIED",
                resultCode,
                "ASYNC_PROCESSING",
                PaymentRefundOrderStatusEnum.REFUNDING.getCode(),
                response.getOrDefault("refund_id", command.refundOrderNo()));
    }

    @Override
    public ChannelBillResult generateBill(ChannelBillCommand command) {
        Require.notNull(command, PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "富友账单命令不能为空");
        throw new BizException(PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(),
                "富友账单需通过通道账单源配置的手动/FTP/FTPS/HTTP 获取入口导入，不能由本地支付订单生成");
    }

    @Override
    public PaymentQueryResult queryPayment(PaymentQueryCommand command) {
        Require.notNull(command, PaymentCode.PAYMENT_ORDER_STATE_INVALID.getCode(), "富友支付查单命令不能为空");
        Require.notNull(command.order(), PaymentCode.PAYMENT_ORDER_NOT_FOUND);
        PaymentFuiouPayConfig config = configParser.parse(requiredContractConfig(command.tenantId(), command.order().getContractId()));
        Map<String, String> response = call(config, "/commonQuery", queryRequest(command, config));
        return mapPaymentQuery(response);
    }

    @Override
    public RefundQueryResult queryRefund(RefundQueryCommand command) {
        Require.notNull(command, PaymentCode.PAYMENT_REFUND_ORDER_INVALID.getCode(), "富友退款查单命令不能为空");
        Require.notNull(command.refundOrder(), PaymentCode.PAYMENT_REFUND_ORDER_NOT_FOUND);
        PaymentFuiouPayConfig config = configParser.parse(requiredContractConfig(command.tenantId(), command.refundOrder().getContractId()));
        Map<String, String> response = call(config, "/commonQuery", refundQueryRequest(command, config));
        String status = mapRefundQueryStatus(response);
        return new RefundQueryResult("FUIOU_REFUND_QUERY", response.get("result_code"), response.get("trans_stat"), status);
    }

    private void validatePaymentCommand(PaymentApplyCommand command) {
        Require.notNull(command, PaymentCode.PAYMENT_CASHIER_PAY_INVALID.getCode(), "富友支付命令不能为空");
        Require.isTrue(CHANNEL_CODE.equals(command.channelCode()), PaymentCode.PAYMENT_CHANNEL_INVALID.getCode(), "富友通道编码不正确");
        Require.notBlank(command.payOrderNo(), PaymentCode.PAYMENT_CASHIER_PAY_INVALID.getCode(), "支付订单号不能为空");
        Require.isTrue(METHOD_ALIPAY_QR.equals(command.methodCode()), PaymentCode.PAYMENT_METHOD_INVALID.getCode(),
                "当前富友通道仅开放支付宝扫码能力");
        Require.isTrue(MATERIAL_QR.equals(command.paymentMaterialType()), PaymentCode.PAYMENT_CASHIER_PAY_INVALID.getCode(),
                "富友支付宝扫码必须使用二维码物料");
        Require.notNull(command.amount(), PaymentCode.PAYMENT_AMOUNT_INVALID.getCode(), "支付金额不能为空");
        Require.isTrue(command.amount() > 0, PaymentCode.PAYMENT_AMOUNT_INVALID.getCode(), "支付金额必须大于 0");
    }

    private void validateRefundCommand(RefundApplyCommand command) {
        Require.notNull(command, PaymentCode.PAYMENT_REFUND_ORDER_INVALID.getCode(), "富友退款命令不能为空");
        Require.isTrue(CHANNEL_CODE.equals(command.channelCode()), PaymentCode.PAYMENT_CHANNEL_INVALID.getCode(), "富友通道编码不正确");
        Require.notBlank(command.refundOrderNo(), PaymentCode.PAYMENT_REFUND_ORDER_INVALID.getCode(), "退款订单号不能为空");
        Require.notBlank(command.payOrderNo(), PaymentCode.PAYMENT_ORDER_NOT_FOUND.getCode(), "原支付订单号不能为空");
        Require.notNull(command.payAmount(), PaymentCode.PAYMENT_AMOUNT_INVALID.getCode(), "原支付金额不能为空");
        Require.notNull(command.amount(), PaymentCode.PAYMENT_AMOUNT_INVALID.getCode(), "退款金额不能为空");
        Require.isTrue(command.amount() > 0 && command.amount() <= command.payAmount(),
                PaymentCode.PAYMENT_REFUND_AMOUNT_EXCEEDED.getCode(), "退款金额必须大于 0 且不能超过原支付金额");
    }

    private Map<String, String> preCreateRequest(PaymentApplyCommand command, PaymentFuiouPayConfig config) {
        Map<String, String> fields = baseFields(config);
        fields.put("version", "1.0");
        fields.put("order_type", ORDER_TYPE_ALIPAY);
        fields.put("goods_des", truncate(command.title(), GOODS_DESCRIPTION_MAX_LENGTH));
        fields.put("goods_detail", "");
        fields.put("goods_tag", "");
        fields.put("addn_inf", command.bizOrderNo());
        fields.put("mchnt_order_no", command.payOrderNo());
        fields.put("curr_type", command.currency());
        fields.put("order_amt", String.valueOf(command.amount()));
        fields.put("term_ip", config.termIp());
        fields.put("txn_begin_ts", LocalDateTime.now().format(TIME_FORMATTER));
        fields.put("notify_url", config.notifyUrl());
        fields.put("reserved_expire_minute", expireMinutes(command.expireTime()));
        fields.put("sign", signService.sign(fields, config.privateKey()));
        return fields;
    }

    private Map<String, String> queryRequest(PaymentQueryCommand command, PaymentFuiouPayConfig config) {
        Map<String, String> fields = baseFields(config);
        fields.put("version", "1");
        fields.put("order_type", ORDER_TYPE_ALIPAY);
        fields.put("mchnt_order_no", command.order().getPayOrderNo());
        fields.put("sign", signService.sign(fields, config.privateKey()));
        return fields;
    }

    private Map<String, String> refundRequest(RefundApplyCommand command, PaymentFuiouPayConfig config) {
        Map<String, String> fields = baseFields(config);
        fields.put("version", "1.0");
        fields.put("order_type", ORDER_TYPE_ALIPAY);
        fields.put("mchnt_order_no", command.payOrderNo());
        fields.put("refund_order_no", command.refundOrderNo());
        fields.put("total_amt", String.valueOf(command.payAmount()));
        fields.put("refund_amt", String.valueOf(command.amount()));
        fields.put("operator_id", valueOrEmpty(config.operatorId()));
        fields.put("sign", signService.sign(fields, config.privateKey()));
        return fields;
    }

    private Map<String, String> refundQueryRequest(RefundQueryCommand command, PaymentFuiouPayConfig config) {
        Map<String, String> fields = baseFields(config);
        fields.put("version", "1");
        fields.put("order_type", ORDER_TYPE_ALIPAY);
        fields.put("mchnt_order_no", command.refundOrder().getPayOrderNo());
        fields.put("sign", signService.sign(fields, config.privateKey()));
        return fields;
    }

    private Map<String, String> baseFields(PaymentFuiouPayConfig config) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("ins_cd", config.insCd());
        fields.put("mchnt_cd", config.merchantNo());
        fields.put("term_id", config.termId());
        fields.put("random_str", randomString(RANDOM_STRING_LENGTH));
        return fields;
    }

    private Map<String, String> call(PaymentFuiouPayConfig config, String path, Map<String, String> request) {
        Map<String, String> response = httpClient.post(config.gatewayBaseUrl() + path, request);
        Require.isTrue(signService.verify(response, config.fuiouPublicKey()),
                PaymentCode.PAYMENT_CHANNEL_INVALID.getCode(), "富友响应验签失败");
        return response;
    }

    PaymentQueryResult mapPaymentQuery(Map<String, String> response) {
        String resultCode = response.get("result_code");
        String transStat = response.get("trans_stat");
        if (RESULT_SUCCESS.equals(resultCode) && "SUCCESS".equals(transStat)) {
            return new PaymentQueryResult("FUIOU_PAYMENT_QUERY", resultCode, transStat, PaymentOrderStatusEnum.SUCCESS.getCode());
        }
        if ((RESULT_SUCCESS.equals(resultCode) && ("USERPAYING".equals(transStat) || "NOTPAY".equals(transStat)))
                || "9999".equals(resultCode)) {
            return new PaymentQueryResult("FUIOU_PAYMENT_QUERY", resultCode, transStat, PaymentOrderStatusEnum.PAYING.getCode());
        }
        if (RESULT_SUCCESS.equals(resultCode) && ("CLOSED".equals(transStat) || "REVOKED".equals(transStat))) {
            return new PaymentQueryResult("FUIOU_PAYMENT_QUERY", resultCode, transStat, PaymentOrderStatusEnum.CLOSED.getCode());
        }
        return new PaymentQueryResult("FUIOU_PAYMENT_QUERY", resultCode, transStat, PaymentOrderStatusEnum.FAILED.getCode());
    }

    private String requiredContractConfig(Long tenantId, Long contractId) {
        Require.notNull(tenantId, PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID.getCode(), "富友租户 ID 不能为空");
        Require.notNull(contractId, PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID.getCode(), "富友签约 ID 不能为空");
        String configValuesJson = channelContractMapper.selectActiveConfigValuesJson(tenantId, contractId);
        Require.notBlank(configValuesJson, PaymentCode.PAYMENT_CHANNEL_CONTRACT_NOT_FOUND.getCode(), "富友签约配置不存在或未启用");
        return configValuesJson;
    }

    private String expireMinutes(LocalDateTime expireTime) {
        if (expireTime == null) {
            return "0";
        }
        long minutes = java.time.Duration.between(LocalDateTime.now(), expireTime).toMinutes();
        if (minutes <= 0) {
            return "1";
        }
        return String.valueOf(Math.min(minutes, MAX_EXPIRE_MINUTES));
    }

    private String randomString(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            builder.append(RANDOM_CHARS[secureRandom.nextInt(RANDOM_CHARS.length)]);
        }
        return builder.toString();
    }

    private String truncate(String value, int maxLength) {
        String normalized = PaymentContextSupport.trimToNull(value);
        if (normalized == null) {
            return "payment";
        }
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength);
    }

    private String valueOrEmpty(String value) {
        if (value == null) {
            return "";
        }
        return value;
    }

    private String mapRefundQueryStatus(Map<String, String> response) {
        if ("REFUND".equals(response.get("trans_stat"))) {
            return PaymentRefundOrderStatusEnum.SUCCESS.getCode();
        }
        return PaymentRefundOrderStatusEnum.REFUNDING.getCode();
    }
}
