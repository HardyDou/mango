package io.mango.notice.channel.sms;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.api.enums.NoticeFailureCode;
import io.mango.notice.support.channel.NoticeChannelMessage;
import io.mango.notice.support.channel.ChannelSendResult;
import io.mango.notice.support.channel.NoticeChannelSender;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class SmsNoticeChannelSender implements NoticeChannelSender {

    private static final String PROVIDER_ALIYUN = "ALIYUN";
    private static final String PROVIDER_TENCENT = "TENCENT";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final SmsGateway aliyunGateway;
    private final SmsGateway tencentGateway;
    private final ObjectMapper objectMapper;

    public SmsNoticeChannelSender() {
        this(new AliyunSmsGateway(), new TencentSmsGateway(), new ObjectMapper());
    }

    SmsNoticeChannelSender(SmsGateway gateway) {
        this(gateway, gateway, new ObjectMapper());
    }

    SmsNoticeChannelSender(SmsGateway aliyunGateway, SmsGateway tencentGateway) {
        this(aliyunGateway, tencentGateway, new ObjectMapper());
    }

    SmsNoticeChannelSender(SmsGateway aliyunGateway, SmsGateway tencentGateway, ObjectMapper objectMapper) {
        this.aliyunGateway = aliyunGateway;
        this.tencentGateway = tencentGateway;
        this.objectMapper = objectMapper;
    }

    @Override
    public NoticeChannelType channelType() {
        return NoticeChannelType.SMS;
    }

    @Override
    public ChannelSendResult send(NoticeChannelMessage command) {
        if (!StringUtils.hasText(command.getMobile())) {
            return ChannelSendResult.failed(NoticeFailureCode.RECIPIENT_INVALID.name(), "手机号不能为空", false);
        }
        if (!StringUtils.hasText(command.getChannelConfigJson())) {
            return ChannelSendResult.failed(NoticeFailureCode.CHANNEL_CONFIG_INVALID.name(), "短信通道配置不能为空", false);
        }
        Map<String, Object> configMap;
        try {
            configMap = readMap(command.getChannelConfigJson());
        } catch (IllegalArgumentException ex) {
            return ChannelSendResult.failed(NoticeFailureCode.CHANNEL_CONFIG_INVALID.name(), ex.getMessage(), false);
        }
        String provider = firstText(command.getChannelProviderCode(), text(configMap, "providerCode"),
                text(configMap, "provider"));
        String normalizedProvider = normalizeProvider(provider);
        if (!PROVIDER_ALIYUN.equals(normalizedProvider) && !PROVIDER_TENCENT.equals(normalizedProvider)) {
            return ChannelSendResult.failed(NoticeFailureCode.CHANNEL_CONFIG_INVALID.name(),
                    "暂不支持短信供应商：" + provider, false);
        }
        SmsProviderConfig config;
        try {
            config = buildConfig(normalizedProvider, configMap, command.getChannelTemplateId());
        } catch (IllegalArgumentException ex) {
            return ChannelSendResult.failed(NoticeFailureCode.CHANNEL_CONFIG_INVALID.name(), ex.getMessage(), false);
        }
        String templateParam;
        try {
            templateParam = buildTemplateParam(command);
        } catch (IllegalArgumentException ex) {
            return ChannelSendResult.failed("SMS_TEMPLATE_PARAM_INVALID", ex.getMessage(), false);
        }
        try {
            SmsGatewayResult response = gateway(normalizedProvider).send(new SmsGatewayPayload(command.getMobile(),
                    config.signName(), config.templateCode(), templateParam, config));
            if (response.success()) {
                return ChannelSendResult.providerSuccess(
                        StringUtils.hasText(response.messageId()) ? response.messageId()
                                : normalizedProvider.toLowerCase() + "-" + command.getSendRecordId(),
                        response.responseSnapshot());
            }
            return ChannelSendResult.failed(response.failCode(), response.failReason(), response.retryable());
        } catch (SmsGatewayException ex) {
            return ChannelSendResult.failed(ex.failCode(), ex.failReason(), ex.retryable());
        } catch (RuntimeException ex) {
            return ChannelSendResult.failed(normalizedProvider + "_SMS_SEND_ERROR",
                    providerName(normalizedProvider) + "短信发送异常", true);
        }
    }

    private SmsProviderConfig buildConfig(String provider, Map<String, Object> configMap, String templateCode) {
        if (PROVIDER_TENCENT.equals(provider)) {
            return TencentSmsConfig.from(configMap, templateCode);
        }
        return AliyunSmsConfig.from(configMap, templateCode);
    }

    private SmsGateway gateway(String provider) {
        return PROVIDER_TENCENT.equals(provider) ? tencentGateway : aliyunGateway;
    }

    private String normalizeProvider(String provider) {
        if (!StringUtils.hasText(provider)) {
            return PROVIDER_ALIYUN;
        }
        String value = provider.trim().toUpperCase();
        if (PROVIDER_ALIYUN.equals(value) || "ALIYUN_SMS".equals(value)) {
            return PROVIDER_ALIYUN;
        }
        if (PROVIDER_TENCENT.equals(value) || "TENCENT_SMS".equals(value) || "TENCENT_CLOUD".equals(value)
                || "TENCENT_CLOUD_SMS".equals(value) || "QCLOUD_SMS".equals(value)) {
            return PROVIDER_TENCENT;
        }
        return value;
    }

    private String providerName(String provider) {
        return PROVIDER_TENCENT.equals(provider) ? "腾讯云" : "阿里云";
    }

    private Map<String, Object> readMap(String json) {
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("短信通道配置不是合法 JSON");
        }
    }

    private String buildTemplateParam(NoticeChannelMessage command) {
        Map<String, Object> params = command.getParams() == null ? Map.of() : command.getParams();
        Map<String, Object> templateParams = new LinkedHashMap<>();
        if (StringUtils.hasText(command.getVariableMapping())) {
            Map<String, Object> mapping;
            try {
                mapping = objectMapper.readValue(command.getVariableMapping(), MAP_TYPE);
            } catch (JsonProcessingException ex) {
                throw new IllegalArgumentException("短信模板变量映射不是合法 JSON");
            }
            for (Map.Entry<String, Object> entry : mapping.entrySet()) {
                String templateVariable = entry.getKey();
                String paramName = valueText(entry.getValue());
                if (!StringUtils.hasText(templateVariable) || !StringUtils.hasText(paramName)) {
                    continue;
                }
                if (!params.containsKey(paramName)) {
                    throw new IllegalArgumentException("短信模板参数缺失：" + paramName);
                }
                templateParams.put(templateVariable, params.get(paramName));
            }
        } else {
            templateParams.putAll(params);
        }
        return templateParams.isEmpty() ? null : writeJson(templateParams);
    }

    private String writeJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("短信模板参数序列化失败");
        }
    }

    private static String text(Map<String, Object> map, String key) {
        return valueText(map.get(key));
    }

    private static String valueText(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    static String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }
}
