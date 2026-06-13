package io.mango.payment.core.service;

import io.mango.common.exception.BizException;
import io.mango.common.result.Require;
import io.mango.payment.api.PaymentCode;
import io.mango.payment.api.enums.PaymentChannelCode;
import io.mango.payment.api.enums.PaymentOrderStatusEnum;
import io.mango.payment.api.enums.PaymentRefundOrderStatusEnum;
import io.mango.payment.api.vo.PaymentCashierPayMaterialVO;
import io.mango.payment.api.vo.PaymentRefundOrderVO;
import io.mango.payment.core.entity.PaymentOrderEntity;
import io.mango.payment.core.entity.PaymentRefundOrderEntity;
import io.mango.payment.core.entity.PaymentMethod;
import io.mango.payment.core.mapper.PaymentChannelContractMapper;
import io.mango.payment.core.mapper.PaymentMethodMapper;
import io.mango.payment.core.mapper.PaymentOrderMapper;
import io.mango.payment.core.mapper.PaymentRefundOrderMapper;
import io.mango.payment.core.model.PaymentChannelBillItemRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PaymentFuiouPayChannelAdapter implements IPaymentChannelAdapter {

    private static final String CHANNEL_CODE = PaymentChannelCode.FUIOU_PAY.name();
    private static final String RESULT_SUCCESS = "000000";
    private static final String ORDER_TYPE_ALIPAY = "ALIPAY";
    private static final String ORDER_TYPE_WECHAT = "WECHAT";
    private static final String METHOD_ALIPAY_QR = "PERSONAL_ALIPAY_QR";
    private static final String METHOD_WECHAT_QR = "PERSONAL_WECHAT_QR";
    private static final String METHOD_PERSONAL_EBANK = "PERSONAL_EBANK_REDIRECT";
    private static final String METHOD_CORPORATE_EBANK = "CORPORATE_EBANK_REDIRECT";
    private static final String MATERIAL_QR = "QR";
    private static final String MATERIAL_HTML_FORM = "HTML_FORM";
    private static final long DEFAULT_SCANPAY_EXPIRE_MINUTES = 120L;
    private static final String DEFAULT_SCANPAY_TERM_ID = "88888888";
    private static final String PC_GATEWAY_VERSION = "1.0.1";
    private static final String PC_GATEWAY_ORDER_PAY_TYPE_B2C = "B2C";
    private static final String PC_GATEWAY_ORDER_PAY_TYPE_B2B = "B2B";
    private static final String PC_GATEWAY_SUCCESS_PAY_CODE = "0000";
    private static final String PC_GATEWAY_STATUS_SUCCESS = "11";
    private static final String PC_GATEWAY_STATUS_CREATED = "00";
    private static final String PC_GATEWAY_STATUS_CONFIRMED = "04";
    private static final String PC_GATEWAY_STATUS_CANCELLED = "01";
    private static final String PC_GATEWAY_STATUS_EXPIRED = "03";
    private static final String PC_GATEWAY_STATUS_FAILED = "05";
    private static final int GOODS_DESCRIPTION_MAX_LENGTH = 128;
    private static final int RANDOM_STRING_LENGTH = 32;
    private static final long MAX_EXPIRE_MINUTES = 1440L;
    private static final long SCANPAY_REALTIME_QUERY_DAYS = 3L;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final char[] RANDOM_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final Pattern XML_FIELD_PATTERN = Pattern.compile("<([a-zA-Z0-9_]+)>(.*?)</\\1>", Pattern.DOTALL);

    private final PaymentFuiouPayConfigParser configParser;
    private final PaymentFuiouSignService signService;
    private final PaymentFuiouGatewaySignService gatewaySignService;
    private final PaymentFuiouHttpClient httpClient;
    private final PaymentChannelContractMapper channelContractMapper;
    private final PaymentMethodMapper methodMapper;
    private final PaymentOrderMapper paymentOrderMapper;
    private final PaymentRefundOrderMapper refundOrderMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String channelCode() {
        return CHANNEL_CODE;
    }

    @Override
    public PaymentApplyResult applyPayment(PaymentApplyCommand command) {
        validatePaymentCommand(command);
        PaymentFuiouPayConfig config = configParser.parse(command.contractConfigValuesJson());
        if (isPcGatewayMethod(command.methodCode())) {
            configParser.validateForPcGateway(config);
            return applyPcGatewayPayment(command, config);
        }
        configParser.validateForScanpay(config);
        Map<String, String> request = preCreateRequest(command, config);
        Map<String, String> response = call(config, "/preCreate", request);
        String resultCode = response.get("result_code");
        Require.isTrue(RESULT_SUCCESS.equals(resultCode), PaymentCode.PAYMENT_CASHIER_PAY_INVALID.getCode(),
                "富友下单失败：" + response.getOrDefault("result_msg", resultCode));
        Require.notBlank(response.get("qr_code"), PaymentCode.PAYMENT_CASHIER_PAY_INVALID.getCode(), "富友下单未返回二维码");
        PaymentCashierPayMaterialVO material = new PaymentCashierPayMaterialVO();
        material.setMaterialType(command.paymentMaterialType());
        material.setQrContent(response.get("qr_code"));
        material.setExpireTime(scanpayMaterialExpireTime(command.expireTime()));
        return new PaymentApplyResult(
                "FUIOU_PRE_CREATE",
                resultCode,
                "ASYNC_PROCESSING",
                PaymentOrderStatusEnum.PAYING.getCode(),
                valueOrDefault(response.get("reserved_fy_order_no"), command.payOrderNo()),
                material);
    }

    @Override
    public RefundApplyResult applyRefund(RefundApplyCommand command) {
        validateRefundCommand(command);
        Require.isTrue(isScanpayMethod(command.methodCode()), PaymentCode.PAYMENT_REFUND_ORDER_INVALID.getCode(),
                "富友网银退款接口资料未配置，当前仅开放微信扫码和支付宝扫码退款");
        PaymentFuiouPayConfig config = configParser.parse(requiredContractConfig(command.tenantId(), command.contractId()));
        configParser.validateForScanpay(config);
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
                valueOrDefault(response.get("refund_id"), command.refundOrderNo()));
    }

    @Override
    public ChannelBillResult generateBill(ChannelBillCommand command) {
        Require.notNull(command, PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "富友账单命令不能为空");
        Require.notNull(command.contractId(), PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID.getCode(), "富友账单缺少签约通道 ID");
        Require.notNull(command.billDate(), PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "富友账单日期不能为空");
        PaymentFuiouPayConfig config = configParser.parse(requiredContractConfig(command.tenantId(), command.contractId()));
        LocalDate nextBillDate = command.billDate().plusDays(1);
        List<PaymentChannelBillItemRow> rows = new ArrayList<>();
        appendPaymentBillRows(command, config, nextBillDate, rows);
        appendRefundBillRows(command, config, nextBillDate, rows);
        Require.notEmpty(rows, PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "账单日期内没有富友确认成功的支付或退款订单");
        return new ChannelBillResult(rows);
    }

    @Override
    public PaymentQueryResult queryPayment(PaymentQueryCommand command) {
        Require.notNull(command, PaymentCode.PAYMENT_ORDER_STATE_INVALID.getCode(), "富友支付查单命令不能为空");
        Require.notNull(command.order(), PaymentCode.PAYMENT_ORDER_NOT_FOUND);
        PaymentFuiouPayConfig config = configParser.parse(requiredContractConfig(command.tenantId(), command.order().getContractId()));
        String methodCode = requiredMethodCode(command.tenantId(), command.order().getMethodId());
        if (isPcGatewayMethod(methodCode)) {
            configParser.validateForPcGateway(config);
            return queryPcGatewayPayment(command, config);
        }
        configParser.validateForScanpay(config);
        Map<String, String> response = call(config, "/commonQuery", queryRequest(command, config));
        return mapPaymentQuery(response);
    }

    @Override
    public RefundQueryResult queryRefund(RefundQueryCommand command) {
        Require.notNull(command, PaymentCode.PAYMENT_REFUND_ORDER_INVALID.getCode(), "富友退款查单命令不能为空");
        Require.notNull(command.refundOrder(), PaymentCode.PAYMENT_REFUND_ORDER_NOT_FOUND);
        Require.isTrue(isScanpayMethod(command.refundOrder().getMethodCode()), PaymentCode.PAYMENT_REFUND_ORDER_INVALID.getCode(),
                "富友网银退款查单接口资料未配置，当前仅开放微信扫码和支付宝扫码退款查单");
        PaymentFuiouPayConfig config = configParser.parse(requiredContractConfig(command.tenantId(), command.refundOrder().getContractId()));
        configParser.validateForScanpay(config);
        Map<String, String> response = call(config, "/refundQuery", refundQueryRequest(command, config));
        String status = mapRefundQueryStatus(response);
        return new RefundQueryResult("FUIOU_REFUND_QUERY", response.get("result_code"), response.get("trans_stat"), status);
    }

    private void validatePaymentCommand(PaymentApplyCommand command) {
        Require.notNull(command, PaymentCode.PAYMENT_CASHIER_PAY_INVALID.getCode(), "富友支付命令不能为空");
        Require.isTrue(CHANNEL_CODE.equals(command.channelCode()), PaymentCode.PAYMENT_CHANNEL_INVALID.getCode(), "富友通道编码不正确");
        Require.notBlank(command.payOrderNo(), PaymentCode.PAYMENT_CASHIER_PAY_INVALID.getCode(), "支付订单号不能为空");
        Require.isTrue(isScanpayMethod(command.methodCode()) || isPcGatewayMethod(command.methodCode()),
                PaymentCode.PAYMENT_METHOD_INVALID.getCode(), "当前富友通道仅开放微信扫码、支付宝扫码、个人网银和企业网银能力");
        Require.isTrue((isScanpayMethod(command.methodCode()) && MATERIAL_QR.equals(command.paymentMaterialType()))
                        || (isPcGatewayMethod(command.methodCode()) && MATERIAL_HTML_FORM.equals(command.paymentMaterialType())),
                PaymentCode.PAYMENT_CASHIER_PAY_INVALID.getCode(), "富友支付方式与支付物料不匹配");
        Require.notNull(command.amount(), PaymentCode.PAYMENT_AMOUNT_INVALID.getCode(), "支付金额不能为空");
        Require.isTrue(command.amount() > 0, PaymentCode.PAYMENT_AMOUNT_INVALID.getCode(), "支付金额必须大于 0");
        if (isScanpayMethod(command.methodCode())) {
            Require.notBlank(command.clientIp(), PaymentCode.PAYMENT_CASHIER_PAY_INVALID.getCode(), "富友支付缺少付款人请求 IP");
        }
        if (isPcGatewayMethod(command.methodCode())) {
            Require.notBlank(command.payerBankCode(), PaymentCode.PAYMENT_CASHIER_PAY_INVALID.getCode(), "富友网银支付缺少银行编码");
        }
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

    Map<String, String> preCreateRequest(PaymentApplyCommand command, PaymentFuiouPayConfig config) {
        Map<String, String> fields = baseFields(config);
        fields.put("version", "1");
        fields.put("order_type", orderType(command.methodCode()));
        fields.put("goods_des", truncate(command.title(), GOODS_DESCRIPTION_MAX_LENGTH));
        fields.put("goods_detail", "");
        fields.put("goods_tag", "");
        fields.put("addn_inf", "");
        fields.put("mchnt_order_no", command.payOrderNo());
        fields.put("curr_type", command.currency());
        fields.put("order_amt", String.valueOf(command.amount()));
        fields.put("term_ip", command.clientIp());
        fields.put("txn_begin_ts", LocalDateTime.now().format(TIME_FORMATTER));
        fields.put("notify_url", config.notifyUrl());
        fields.put("reserved_expire_minute", expireMinutes(command.expireTime()));
        fields.put("reserved_sub_appid", "");
        fields.put("reserved_limit_pay", "");
        fields.put("sign", signService.sign(fields, config.privateKey()));
        return fields;
    }

    private Map<String, String> queryRequest(PaymentQueryCommand command, PaymentFuiouPayConfig config) {
        Map<String, String> fields = baseFields(config);
        fields.put("version", "1");
        fields.put("order_type", orderType(requiredMethodCode(command.tenantId(), command.order().getMethodId())));
        fields.put("mchnt_order_no", command.order().getPayOrderNo());
        fields.put("sign", signService.sign(fields, config.privateKey()));
        return fields;
    }

    private Map<String, String> refundRequest(RefundApplyCommand command, PaymentFuiouPayConfig config) {
        Map<String, String> fields = baseFields(config);
        fields.put("version", "1.0");
        fields.put("order_type", orderType(command.methodCode()));
        fields.put("mchnt_order_no", command.payOrderNo());
        fields.put("refund_order_no", command.refundOrderNo());
        fields.put("total_amt", String.valueOf(command.payAmount()));
        fields.put("refund_amt", String.valueOf(command.amount()));
        fields.put("operator_id", valueOrEmpty(config.operatorId()));
        fields.put("sign", signService.sign(fields, config.privateKey()));
        return fields;
    }

    private void appendPaymentBillRows(
            ChannelBillCommand command,
            PaymentFuiouPayConfig config,
            LocalDate nextBillDate,
            List<PaymentChannelBillItemRow> rows) {
        List<PaymentOrderEntity> orders = paymentOrderMapper.selectSuccessfulChannelOrdersForBill(
                command.tenantId(),
                CHANNEL_CODE,
                command.contractId(),
                command.billDate(),
                nextBillDate);
        for (PaymentOrderEntity order : orders) {
            String methodCode = requiredMethodCode(command.tenantId(), order.getMethodId());
            Map<String, String> response;
            PaymentQueryResult result;
            if (isPcGatewayMethod(methodCode)) {
                configParser.validateForPcGateway(config);
                response = queryPcGatewayPaymentResponse(order, config);
                result = mapPcGatewayPaymentQuery(response);
            } else {
                configParser.validateForScanpay(config);
                response = call(config, scanpayPaymentQueryPath(command.billDate()), queryRequest(order, methodCode, config, command.billDate()));
                result = mapPaymentQuery(response);
            }
            if (PaymentOrderStatusEnum.SUCCESS.getCode().equals(result.status())) {
                rows.add(paymentBillRow(order, response, methodCode));
            }
        }
    }

    private void appendRefundBillRows(
            ChannelBillCommand command,
            PaymentFuiouPayConfig config,
            LocalDate nextBillDate,
            List<PaymentChannelBillItemRow> rows) {
        List<PaymentRefundOrderEntity> refunds = refundOrderMapper.selectSuccessfulChannelRefundsForBill(
                command.tenantId(),
                CHANNEL_CODE,
                command.contractId(),
                command.billDate(),
                nextBillDate);
        for (PaymentRefundOrderEntity refund : refunds) {
            PaymentRefundOrderVO refundOrder = refundOrderMapper.selectByTenantAndRefundOrderNo(command.tenantId(), refund.getRefundOrderNo());
            Require.notNull(refundOrder, PaymentCode.PAYMENT_REFUND_ORDER_NOT_FOUND);
            Require.isTrue(isScanpayMethod(refundOrder.getMethodCode()), PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(),
                    "富友网银退款接口资料未配置，不能生成网银退款账单");
            configParser.validateForScanpay(config);
            Map<String, String> response = call(config, "/refundQuery", refundQueryRequest(refundOrder, config));
            String status = mapRefundQueryStatus(response);
            if (PaymentRefundOrderStatusEnum.SUCCESS.getCode().equals(status)) {
                rows.add(refundBillRow(refund, response));
            }
        }
    }

    private Map<String, String> queryRequest(
            PaymentOrderEntity order,
            String methodCode,
            PaymentFuiouPayConfig config,
            LocalDate billDate) {
        Map<String, String> fields = baseFields(config);
        fields.put("version", "1");
        fields.put("order_type", orderType(methodCode));
        fields.put("mchnt_order_no", order.getPayOrderNo());
        if (!isScanpayRealtimeQueryDate(billDate)) {
            fields.put("trade_dt", billDate.format(DateTimeFormatter.BASIC_ISO_DATE));
            fields.put("channel_order_id", "");
            fields.put("transaction_id", "");
        }
        fields.put("sign", signService.sign(fields, config.privateKey()));
        return fields;
    }

    private String scanpayPaymentQueryPath(LocalDate billDate) {
        return isScanpayRealtimeQueryDate(billDate) ? "/commonQuery" : "/hisTradeQuery";
    }

    private boolean isScanpayRealtimeQueryDate(LocalDate billDate) {
        return !billDate.isBefore(LocalDate.now().minusDays(SCANPAY_REALTIME_QUERY_DAYS));
    }

    private Map<String, String> refundQueryRequest(PaymentRefundOrderVO refundOrder, PaymentFuiouPayConfig config) {
        Map<String, String> fields = baseFields(config);
        fields.put("version", "1.0");
        fields.put("refund_order_no", refundOrder.getRefundOrderNo());
        fields.put("sign", signService.sign(fields, config.privateKey()));
        return fields;
    }

    private PaymentChannelBillItemRow paymentBillRow(
            PaymentOrderEntity order,
            Map<String, String> response,
            String methodCode) {
        PaymentChannelBillItemRow row = new PaymentChannelBillItemRow();
        row.setChannelTradeNo(paymentChannelTradeNo(order, response, methodCode));
        row.setTradeType("PAYMENT");
        row.setAmount(longValue(response.get("order_amt"), order.getAmount()));
        row.setFee(0L);
        row.setTradeTime(paymentBillTradeTime(order, response, methodCode));
        return row;
    }

    private PaymentChannelBillItemRow refundBillRow(PaymentRefundOrderEntity refund, Map<String, String> response) {
        PaymentChannelBillItemRow row = new PaymentChannelBillItemRow();
        row.setChannelTradeNo(valueOrDefault(response.get("refund_id"), valueOrDefault(refund.getChannelRefundNo(), refund.getRefundOrderNo())));
        row.setTradeType("REFUND");
        row.setAmount(longValue(response.get("reserved_refund_amt"), refund.getRefundAmount()));
        row.setFee(0L);
        row.setTradeTime(valueOrDefault(parseFuiouDate(response.get("reserved_fy_settle_dt")), refund.getRefundTime()));
        return row;
    }

    private String paymentChannelTradeNo(
            PaymentOrderEntity order,
            Map<String, String> response,
            String methodCode) {
        if (isPcGatewayMethod(methodCode)) {
            return valueOrDefault(response.get("fy_ssn"), valueOrDefault(order.getChannelTradeNo(), order.getPayOrderNo()));
        }
        return valueOrDefault(response.get("transaction_id"), valueOrDefault(order.getChannelTradeNo(), order.getPayOrderNo()));
    }

    private LocalDateTime paymentBillTradeTime(
            PaymentOrderEntity order,
            Map<String, String> response,
            String methodCode) {
        if (isPcGatewayMethod(methodCode)) {
            return valueOrDefault(parseFuiouDate(response.get("order_date")), order.getPayTime());
        }
        return valueOrDefault(parseFuiouDateTime(response.get("reserved_txn_fin_ts")),
                valueOrDefault(parseFuiouDate(response.get("reserved_fy_settle_dt")), order.getPayTime()));
    }

    Map<String, String> refundQueryRequest(RefundQueryCommand command, PaymentFuiouPayConfig config) {
        Map<String, String> fields = baseFields(config);
        fields.put("version", "1.0");
        fields.put("refund_order_no", command.refundOrder().getRefundOrderNo());
        fields.put("sign", signService.sign(fields, config.privateKey()));
        return fields;
    }

    private Map<String, String> baseFields(PaymentFuiouPayConfig config) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("ins_cd", config.insCd());
        fields.put("mchnt_cd", config.merchantNo());
        fields.put("term_id", DEFAULT_SCANPAY_TERM_ID);
        fields.put("random_str", randomString(RANDOM_STRING_LENGTH));
        return fields;
    }

    String orderType(String methodCode) {
        if (METHOD_WECHAT_QR.equals(methodCode)) {
            return ORDER_TYPE_WECHAT;
        }
        if (METHOD_ALIPAY_QR.equals(methodCode)) {
            return ORDER_TYPE_ALIPAY;
        }
        throw new BizException(PaymentCode.PAYMENT_METHOD_INVALID.getCode(), "当前富友通道仅开放微信扫码和支付宝扫码能力");
    }

    private boolean isScanpayMethod(String methodCode) {
        return METHOD_ALIPAY_QR.equals(methodCode) || METHOD_WECHAT_QR.equals(methodCode);
    }

    private boolean isPcGatewayMethod(String methodCode) {
        return METHOD_PERSONAL_EBANK.equals(methodCode) || METHOD_CORPORATE_EBANK.equals(methodCode);
    }

    private Map<String, String> call(PaymentFuiouPayConfig config, String path, Map<String, String> request) {
        Map<String, String> response = httpClient.post(config.scanpayGatewayBaseUrl() + path, request);
        Require.isTrue(signService.verify(response, config.fuiouPublicKey()),
                PaymentCode.PAYMENT_CHANNEL_INVALID.getCode(), "富友响应验签失败");
        return response;
    }

    private PaymentApplyResult applyPcGatewayPayment(PaymentApplyCommand command, PaymentFuiouPayConfig config) {
        Map<String, String> fields = pcGatewayPayRequest(command, config);
        PaymentCashierPayMaterialVO material = new PaymentCashierPayMaterialVO();
        material.setMaterialType(command.paymentMaterialType());
        material.setHtmlForm(htmlForm(config.gatewayPayUrl(), fields));
        material.setExpireTime(command.expireTime());
        return new PaymentApplyResult(
                "FUIOU_PC_GATEWAY_PAY",
                "FORM_CREATED",
                "ASYNC_PROCESSING",
                PaymentOrderStatusEnum.PAYING.getCode(),
                command.payOrderNo(),
                material);
    }

    Map<String, String> pcGatewayPayRequest(PaymentApplyCommand command, PaymentFuiouPayConfig config) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("mchnt_cd", config.gatewayMerchantNo());
        fields.put("order_id", command.payOrderNo());
        fields.put("order_amt", String.valueOf(command.amount()));
        fields.put("order_pay_type", pcGatewayPayType(command.methodCode()));
        fields.put("page_notify_url", config.gatewayPageNotifyUrl());
        fields.put("back_notify_url", config.gatewayBackNotifyUrl());
        fields.put("order_valid_time", pcGatewayExpire(command.expireTime()));
        fields.put("iss_ins_cd", command.payerBankCode());
        fields.put("goods_name", truncate(command.title(), 60));
        fields.put("goods_display_url", "");
        fields.put("rem", "");
        fields.put("ver", PC_GATEWAY_VERSION);
        fields.put("md5", gatewaySignService.signPay(fields, config.gatewayMerchantKey()));
        return fields;
    }

    private PaymentQueryResult queryPcGatewayPayment(PaymentQueryCommand command, PaymentFuiouPayConfig config) {
        Map<String, String> response = queryPcGatewayPaymentResponse(command.order(), config);
        return mapPcGatewayPaymentQuery(response);
    }

    private Map<String, String> queryPcGatewayPaymentResponse(PaymentOrderEntity order, PaymentFuiouPayConfig config) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("mchnt_cd", config.gatewayMerchantNo());
        fields.put("order_id", order.getPayOrderNo());
        fields.put("md5", gatewaySignService.signQuery(fields, config.gatewayMerchantKey()));
        String body = httpClient.postForm(config.gatewayQueryUrl(), fields);
        Map<String, String> response = parsePcGatewayQueryResponse(body);
        String plain = plainXml(body);
        String md5 = PaymentContextSupport.trimToNull(response.get("md5"));
        Require.isTrue(md5 != null && md5.equalsIgnoreCase(gatewaySignService.signPlain(plain, config.gatewayMerchantKey())),
                PaymentCode.PAYMENT_CHANNEL_INVALID.getCode(), "富友网关查单响应验签失败");
        return response;
    }

    PaymentQueryResult mapPcGatewayPaymentQuery(Map<String, String> response) {
        String payCode = response.get("order_pay_code");
        String orderStatus = response.get("order_st");
        if (PC_GATEWAY_SUCCESS_PAY_CODE.equals(payCode) && PC_GATEWAY_STATUS_SUCCESS.equals(orderStatus)) {
            return new PaymentQueryResult("FUIOU_PC_GATEWAY_QUERY", payCode, orderStatus, PaymentOrderStatusEnum.SUCCESS.getCode());
        }
        if (PC_GATEWAY_SUCCESS_PAY_CODE.equals(payCode)
                && (PC_GATEWAY_STATUS_CREATED.equals(orderStatus) || PC_GATEWAY_STATUS_CONFIRMED.equals(orderStatus))) {
            return new PaymentQueryResult("FUIOU_PC_GATEWAY_QUERY", payCode, orderStatus, PaymentOrderStatusEnum.PAYING.getCode());
        }
        if (PC_GATEWAY_STATUS_CANCELLED.equals(orderStatus) || PC_GATEWAY_STATUS_EXPIRED.equals(orderStatus)) {
            return new PaymentQueryResult("FUIOU_PC_GATEWAY_QUERY", payCode, orderStatus, PaymentOrderStatusEnum.CLOSED.getCode());
        }
        if (PC_GATEWAY_STATUS_FAILED.equals(orderStatus)) {
            return new PaymentQueryResult("FUIOU_PC_GATEWAY_QUERY", payCode, orderStatus, PaymentOrderStatusEnum.FAILED.getCode());
        }
        return new PaymentQueryResult("FUIOU_PC_GATEWAY_QUERY", payCode, orderStatus, PaymentOrderStatusEnum.PAYING.getCode());
    }

    private Map<String, String> parsePcGatewayQueryResponse(String body) {
        Require.notBlank(body, PaymentCode.PAYMENT_CHANNEL_INVALID.getCode(), "富友网关查单响应为空");
        Map<String, String> fields = new LinkedHashMap<>();
        Matcher matcher = XML_FIELD_PATTERN.matcher(body);
        while (matcher.find()) {
            fields.put(matcher.group(1), matcher.group(2));
        }
        Require.notBlank(fields.get("order_id"), PaymentCode.PAYMENT_CHANNEL_INVALID.getCode(), "富友网关查单响应缺少订单号");
        Require.notBlank(fields.get("md5"), PaymentCode.PAYMENT_CHANNEL_INVALID.getCode(), "富友网关查单响应缺少签名");
        return fields;
    }

    private String plainXml(String body) {
        int start = body.indexOf("<plain>");
        int end = body.indexOf("</plain>");
        Require.isTrue(start >= 0 && end > start, PaymentCode.PAYMENT_CHANNEL_INVALID.getCode(), "富友网关查单响应缺少 plain 报文");
        return body.substring(start + "<plain>".length(), end);
    }

    private String htmlForm(String action, Map<String, String> fields) {
        StringBuilder builder = new StringBuilder();
        builder.append("<form method=\"post\" action=\"").append(escapeHtml(action)).append("\">");
        fields.forEach((key, value) -> builder.append("<input type=\"hidden\" name=\"")
                .append(escapeHtml(key))
                .append("\" value=\"")
                .append(escapeHtml(value))
                .append("\" />"));
        builder.append("</form>");
        return builder.toString();
    }

    private String pcGatewayPayType(String methodCode) {
        if (METHOD_PERSONAL_EBANK.equals(methodCode)) {
            return PC_GATEWAY_ORDER_PAY_TYPE_B2C;
        }
        if (METHOD_CORPORATE_EBANK.equals(methodCode)) {
            return PC_GATEWAY_ORDER_PAY_TYPE_B2B;
        }
        throw new BizException(PaymentCode.PAYMENT_METHOD_INVALID.getCode(), "当前富友网关仅开放个人网银和企业网银能力");
    }

    private String pcGatewayExpire(LocalDateTime expireTime) {
        if (expireTime == null) {
            return "";
        }
        long minutes = java.time.Duration.between(LocalDateTime.now(), expireTime).toMinutes();
        if (minutes <= 0) {
            return "1m";
        }
        if (minutes < 60) {
            return minutes + "m";
        }
        long hours = minutes / 60;
        if (hours < 24) {
            return hours + "h";
        }
        long days = Math.min(hours / 24, 15);
        return days + "d";
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

    private String requiredMethodCode(Long tenantId, Long methodId) {
        Require.notNull(tenantId, PaymentCode.PAYMENT_METHOD_INVALID.getCode(), "富友查单缺少租户 ID");
        Require.notNull(methodId, PaymentCode.PAYMENT_METHOD_INVALID.getCode(), "富友查单缺少支付方式 ID");
        PaymentMethod method = methodMapper.selectById(methodId);
        Require.isTrue(method != null
                        && tenantId.equals(method.getTenantId())
                        && Integer.valueOf(0).equals(method.getDelFlag()),
                PaymentCode.PAYMENT_METHOD_NOT_FOUND.getCode(), "原支付方式不存在");
        return method.getMethodCode();
    }

    private String expireMinutes(LocalDateTime expireTime) {
        if (expireTime == null) {
            return String.valueOf(DEFAULT_SCANPAY_EXPIRE_MINUTES);
        }
        long minutes = java.time.Duration.between(LocalDateTime.now(), expireTime).toMinutes();
        if (minutes <= 0) {
            return "1";
        }
        return String.valueOf(Math.min(minutes, MAX_EXPIRE_MINUTES));
    }

    private LocalDateTime scanpayMaterialExpireTime(LocalDateTime expireTime) {
        if (expireTime != null) {
            return expireTime;
        }
        return LocalDateTime.now().plusMinutes(DEFAULT_SCANPAY_EXPIRE_MINUTES);
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

    private String valueOrDefault(String value, String defaultValue) {
        String normalized = PaymentContextSupport.trimToNull(value);
        if (normalized == null) {
            return defaultValue;
        }
        return normalized;
    }

    private Long longValue(String value, Long defaultValue) {
        String normalized = PaymentContextSupport.trimToNull(value);
        if (normalized == null) {
            return defaultValue;
        }
        try {
            return Long.valueOf(normalized);
        } catch (NumberFormatException ex) {
            throw new BizException(PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "富友账单金额格式不正确", ex);
        }
    }

    private LocalDateTime valueOrDefault(LocalDateTime value, LocalDateTime defaultValue) {
        return value == null ? defaultValue : value;
    }

    private LocalDateTime parseFuiouDateTime(String value) {
        String normalized = PaymentContextSupport.trimToNull(value);
        if (normalized == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(normalized, TIME_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw new BizException(PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "富友交易时间格式不正确", ex);
        }
    }

    private LocalDateTime parseFuiouDate(String value) {
        String normalized = PaymentContextSupport.trimToNull(value);
        if (normalized == null) {
            return null;
        }
        try {
            return LocalDate.parse(normalized, DateTimeFormatter.BASIC_ISO_DATE).atStartOfDay();
        } catch (DateTimeParseException ex) {
            throw new BizException(PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "富友清算日期格式不正确", ex);
        }
    }

    private String escapeHtml(String value) {
        return valueOrEmpty(value)
                .replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    String mapRefundQueryStatus(Map<String, String> response) {
        String resultCode = response.get("result_code");
        String transStat = response.get("trans_stat");
        if (RESULT_SUCCESS.equals(resultCode) && "SUCCESS".equals(transStat)) {
            return PaymentRefundOrderStatusEnum.SUCCESS.getCode();
        }
        if (RESULT_SUCCESS.equals(resultCode) && "PAYERROR".equals(transStat)) {
            return PaymentRefundOrderStatusEnum.FAILED.getCode();
        }
        if ((RESULT_SUCCESS.equals(resultCode) && ("USERPAYING".equals(transStat) || "PROCESSING".equals(transStat)))
                || "9999".equals(resultCode)) {
            return PaymentRefundOrderStatusEnum.REFUNDING.getCode();
        }
        if (!RESULT_SUCCESS.equals(resultCode)) {
            return PaymentRefundOrderStatusEnum.FAILED.getCode();
        }
        return PaymentRefundOrderStatusEnum.REFUNDING.getCode();
    }
}
