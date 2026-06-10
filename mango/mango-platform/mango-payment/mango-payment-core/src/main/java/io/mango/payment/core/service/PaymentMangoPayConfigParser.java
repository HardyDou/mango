package io.mango.payment.core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.exception.BizException;
import io.mango.payment.api.PaymentCode;

import java.util.LinkedHashMap;
import java.util.Map;

final class PaymentMangoPayConfigParser {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private PaymentMangoPayConfigParser() {
    }

    static Map<String, String> parse(String value) {
        String normalized = PaymentContextSupport.trimToNull(value);
        if (normalized == null) {
            return Map.of();
        }
        try {
            Map<String, Object> parsed = OBJECT_MAPPER.readValue(
                    normalized,
                    new TypeReference<LinkedHashMap<String, Object>>() {
                    });
            Map<String, String> result = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : parsed.entrySet()) {
                if (entry.getValue() != null) {
                    result.put(entry.getKey(), String.valueOf(entry.getValue()));
                }
            }
            return result;
        } catch (JsonProcessingException ex) {
            throw new BizException(PaymentCode.PAYMENT_CHANNEL_CONTRACT_VALUE_INVALID.getCode(), "芒果支付场景配置不是有效 JSON", ex);
        }
    }
}
