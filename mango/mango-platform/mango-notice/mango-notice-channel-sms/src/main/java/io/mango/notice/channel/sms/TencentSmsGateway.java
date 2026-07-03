package io.mango.notice.channel.sms;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.sms.v20210111.SmsClient;
import com.tencentcloudapi.sms.v20210111.models.SendSmsRequest;
import com.tencentcloudapi.sms.v20210111.models.SendSmsResponse;
import com.tencentcloudapi.sms.v20210111.models.SendStatus;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class TencentSmsGateway implements SmsGateway {

    private static final String SUCCESS_CODE = "OK";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    TencentSmsGateway() {
        this(new ObjectMapper());
    }

    TencentSmsGateway(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public SmsGatewayResponse send(SmsGatewayRequest request) {
        TencentSmsConfig config = (TencentSmsConfig) request.config();
        try {
            Credential credential = new Credential(config.secretId(), config.secretKey());
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint(config.endpoint());
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);
            SmsClient client = new SmsClient(credential, config.region(), clientProfile);

            SendSmsRequest sendSmsRequest = new SendSmsRequest();
            sendSmsRequest.setPhoneNumberSet(new String[]{formatMobile(request.mobile(), config.countryCode())});
            sendSmsRequest.setSmsSdkAppId(config.smsSdkAppId());
            sendSmsRequest.setSignName(request.signName());
            sendSmsRequest.setTemplateId(request.templateCode());
            String[] templateParamSet = parseTemplateParamSet(request.templateParam());
            if (templateParamSet.length > 0) {
                sendSmsRequest.setTemplateParamSet(templateParamSet);
            }

            SendSmsResponse response = client.SendSms(sendSmsRequest);
            SendStatus status = firstStatus(response.getSendStatusSet());
            String code = status == null ? null : status.getCode();
            String snapshot = snapshot(response, status);
            if (SUCCESS_CODE.equalsIgnoreCase(code)) {
                return SmsGatewayResponse.success(status.getSerialNo(), snapshot);
            }
            return SmsGatewayResponse.failed("TENCENT_SMS_" + nullToUnknown(code),
                    "腾讯云短信发送失败：" + nullToDefault(status == null ? null : status.getMessage(), code),
                    retryable(code), snapshot);
        } catch (SmsGatewayException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SmsGatewayException("TENCENT_SMS_SEND_ERROR", "腾讯云短信发送异常", true);
        }
    }

    private String[] parseTemplateParamSet(String templateParam) {
        if (!StringUtils.hasText(templateParam)) {
            return new String[0];
        }
        try {
            Map<String, Object> params = objectMapper.readValue(templateParam, MAP_TYPE);
            List<String> values = new ArrayList<>();
            for (Object value : params.values()) {
                values.add(value == null ? "" : String.valueOf(value));
            }
            return values.toArray(String[]::new);
        } catch (JsonProcessingException ex) {
            throw new SmsGatewayException("SMS_TEMPLATE_PARAM_INVALID", "短信模板参数不是合法 JSON", false);
        }
    }

    private String formatMobile(String mobile, String countryCode) {
        String trimmed = mobile.trim();
        if (trimmed.startsWith("+")) {
            return trimmed;
        }
        return countryCode + trimmed;
    }

    private SendStatus firstStatus(SendStatus[] statuses) {
        return statuses == null || statuses.length == 0 ? null : statuses[0];
    }

    private boolean retryable(String code) {
        if (!StringUtils.hasText(code)) {
            return true;
        }
        String upperCode = code.toUpperCase();
        return upperCode.contains("THROTTLE")
                || upperCode.contains("LIMIT")
                || upperCode.contains("INTERNAL")
                || upperCode.contains("UNAVAILABLE")
                || upperCode.contains("REQUESTLIMITEXCEEDED");
    }

    private String snapshot(SendSmsResponse response, SendStatus status) {
        return "{\"provider\":\"TENCENT\",\"requestId\":\"" + escape(response.getRequestId())
                + "\",\"code\":\"" + escape(status == null ? null : status.getCode())
                + "\",\"message\":\"" + escape(status == null ? null : status.getMessage())
                + "\",\"serialNo\":\"" + escape(status == null ? null : status.getSerialNo())
                + "\",\"phoneNumber\":\"" + escape(status == null ? null : status.getPhoneNumber()) + "\"}";
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String nullToUnknown(String value) {
        return StringUtils.hasText(value) ? value : "UNKNOWN";
    }

    private String nullToDefault(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : nullToUnknown(defaultValue);
    }
}
