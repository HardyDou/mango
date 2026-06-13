package io.mango.payment.core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.exception.BizException;
import io.mango.common.result.Require;
import io.mango.payment.api.PaymentCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentFuiouPayConfigParser {

    private static final String DEFAULT_SCANPAY_GATEWAY_BASE_URL = "https://fundwx.payfuiouo2o.com";
    private static final String DEFAULT_PC_GATEWAY_PAY_URL = "https://pay.fuioupay.com/smpGate.do";
    private static final String DEFAULT_PC_GATEWAY_QUERY_URL = "https://pay.fuioupay.com/smpQueryGate.do";
    private static final TypeReference<Map<String, Object>> CONFIG_VALUES_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final PaymentSensitiveValueService sensitiveValueService;

    public PaymentFuiouPayConfig parse(String configValuesJson) {
        Require.notBlank(configValuesJson, PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID.getCode(), "富友签约配置不能为空");
        Map<String, Object> values = parseJson(configValuesJson);
        String privateKey = sensitiveValueService.decrypt(trimToNull(values.get("privateKey")));
        String gatewayMerchantKey = sensitiveValueService.decrypt(trimToNull(values.get("gatewayMerchantKey")));
        PaymentFuiouPayConfig config = new PaymentFuiouPayConfig(
                trimToNull(values.get("insCd")),
                trimToNull(values.get("merchantNo")),
                valueOrDefault(values.get("scanpayGatewayBaseUrl"), valueOrDefault(values.get("gatewayBaseUrl"), DEFAULT_SCANPAY_GATEWAY_BASE_URL)),
                trimToNull(values.get("notifyUrl")),
                privateKey,
                trimToNull(values.get("fuiouPublicKey")),
                trimToNull(values.get("operatorId")),
                trimToNull(values.get("gatewayMerchantNo")),
                gatewayMerchantKey,
                valueOrDefault(values.get("gatewayPayUrl"), DEFAULT_PC_GATEWAY_PAY_URL),
                valueOrDefault(values.get("gatewayQueryUrl"), DEFAULT_PC_GATEWAY_QUERY_URL),
                trimToNull(values.get("gatewayPageNotifyUrl")),
                trimToNull(values.get("gatewayBackNotifyUrl")));
        validate(config);
        return config;
    }

    private Map<String, Object> parseJson(String configValuesJson) {
        try {
            Map<String, Object> values = objectMapper.readValue(configValuesJson, CONFIG_VALUES_TYPE);
            if (values == null) {
                return Map.of();
            }
            return values;
        } catch (JsonProcessingException ex) {
            throw new BizException(PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID.getCode(), "富友签约配置不是有效 JSON", ex);
        }
    }

    public void validateForScanpay(PaymentFuiouPayConfig config) {
        Require.notBlank(config.insCd(), PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID.getCode(), "富友签约配置缺少机构号");
        Require.notBlank(config.merchantNo(), PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID.getCode(), "富友签约配置缺少商户号");
        Require.notBlank(config.scanpayGatewayBaseUrl(), PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID.getCode(), "富友签约配置缺少扫码网关地址");
        Require.notBlank(config.notifyUrl(), PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID.getCode(), "富友签约配置缺少通知地址");
        Require.notBlank(config.privateKey(), PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID.getCode(), "富友签约配置缺少商户私钥");
        Require.notBlank(config.fuiouPublicKey(), PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID.getCode(), "富友签约配置缺少富友平台公钥");
    }

    public void validateForPcGateway(PaymentFuiouPayConfig config) {
        Require.notBlank(config.gatewayMerchantNo(), PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID.getCode(), "富友签约配置缺少网关商户号");
        Require.notBlank(config.gatewayMerchantKey(), PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID.getCode(), "富友签约配置缺少网关商户密钥");
        Require.notBlank(config.gatewayPayUrl(), PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID.getCode(), "富友签约配置缺少网关支付地址");
        Require.notBlank(config.gatewayQueryUrl(), PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID.getCode(), "富友签约配置缺少网关查单地址");
        Require.notBlank(config.gatewayPageNotifyUrl(), PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID.getCode(), "富友签约配置缺少页面跳转地址");
        Require.notBlank(config.gatewayBackNotifyUrl(), PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID.getCode(), "富友签约配置缺少后台通知地址");
    }

    private void validate(PaymentFuiouPayConfig config) {
        if (hasScanpayConfig(config)) {
            validateForScanpay(config);
        }
        if (hasPcGatewayConfig(config)) {
            validateForPcGateway(config);
        }
        Require.isTrue(hasScanpayConfig(config) || hasPcGatewayConfig(config),
                PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID.getCode(), "富友签约配置缺少已开通能力的商户资料");
    }

    private boolean hasScanpayConfig(PaymentFuiouPayConfig config) {
        return config.insCd() != null || config.merchantNo() != null || config.privateKey() != null || config.fuiouPublicKey() != null;
    }

    private boolean hasPcGatewayConfig(PaymentFuiouPayConfig config) {
        return config.gatewayMerchantNo() != null || config.gatewayMerchantKey() != null;
    }

    private String valueOrDefault(Object value, String defaultValue) {
        String text = trimToNull(value);
        if (text == null) {
            return defaultValue;
        }
        return text;
    }

    private String trimToNull(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return null;
        }
        return text;
    }
}
