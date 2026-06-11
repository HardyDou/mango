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

    private static final String DEFAULT_GATEWAY_BASE_URL = "https://fundwx.fuiou.com";
    private static final String DEFAULT_TERM_ID = "88888888";
    private static final String DEFAULT_TERM_IP = "127.0.0.1";
    private static final TypeReference<Map<String, Object>> CONFIG_VALUES_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final PaymentSensitiveValueService sensitiveValueService;

    public PaymentFuiouPayConfig parse(String configValuesJson) {
        Require.notBlank(configValuesJson, PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID.getCode(), "富友签约配置不能为空");
        Map<String, Object> values = parseJson(configValuesJson);
        String privateKey = sensitiveValueService.decrypt(trimToNull(values.get("privateKey")));
        PaymentFuiouPayConfig config = new PaymentFuiouPayConfig(
                trimToNull(values.get("insCd")),
                trimToNull(values.get("merchantNo")),
                valueOrDefault(values.get("termId"), DEFAULT_TERM_ID),
                valueOrDefault(values.get("gatewayBaseUrl"), DEFAULT_GATEWAY_BASE_URL),
                trimToNull(values.get("notifyUrl")),
                privateKey,
                trimToNull(values.get("fuiouPublicKey")),
                valueOrDefault(values.get("termIp"), DEFAULT_TERM_IP),
                trimToNull(values.get("operatorId")));
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

    private void validate(PaymentFuiouPayConfig config) {
        Require.notBlank(config.insCd(), PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID.getCode(), "富友签约配置缺少机构号");
        Require.notBlank(config.merchantNo(), PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID.getCode(), "富友签约配置缺少商户号");
        Require.notBlank(config.termId(), PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID.getCode(), "富友签约配置缺少终端号");
        Require.notBlank(config.gatewayBaseUrl(), PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID.getCode(), "富友签约配置缺少网关地址");
        Require.notBlank(config.notifyUrl(), PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID.getCode(), "富友签约配置缺少通知地址");
        Require.notBlank(config.privateKey(), PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID.getCode(), "富友签约配置缺少商户私钥");
        Require.notBlank(config.fuiouPublicKey(), PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID.getCode(), "富友签约配置缺少富友公钥");
        Require.notBlank(config.termIp(), PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID.getCode(), "富友签约配置缺少终端 IP");
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
