package io.mango.notice.channel.sms;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.dysmsapi20170525.models.SendSmsResponseBody;
import com.aliyun.teaopenapi.models.Config;
import org.springframework.util.StringUtils;

final class AliyunSmsGateway implements SmsGateway {

    private static final String SUCCESS_CODE = "OK";

    @Override
    public SmsGatewayResponse send(SmsGatewayRequest request) {
        AliyunSmsConfig config = (AliyunSmsConfig) request.config();
        try {
            Client client = new Client(new Config()
                    .setAccessKeyId(config.accessKeyId())
                    .setAccessKeySecret(config.accessKeySecret())
                    .setEndpoint(config.endpoint()));
            SendSmsRequest sendSmsRequest = new SendSmsRequest()
                    .setPhoneNumbers(request.mobile())
                    .setSignName(request.signName())
                    .setTemplateCode(request.templateCode());
            if (StringUtils.hasText(request.templateParam())) {
                sendSmsRequest.setTemplateParam(request.templateParam());
            }
            SendSmsResponse response = client.sendSms(sendSmsRequest);
            SendSmsResponseBody body = response.getBody();
            String code = body.getCode();
            String snapshot = snapshot(body);
            if (SUCCESS_CODE.equalsIgnoreCase(code)) {
                return SmsGatewayResponse.success(body.getBizId(), snapshot);
            }
            return SmsGatewayResponse.failed("ALIYUN_SMS_" + nullToUnknown(code),
                    "阿里云短信发送失败：" + nullToDefault(body.getMessage(), code),
                    retryable(code), snapshot);
        } catch (SmsGatewayException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SmsGatewayException("ALIYUN_SMS_SEND_ERROR", "阿里云短信发送异常", true);
        }
    }

    private boolean retryable(String code) {
        if (!StringUtils.hasText(code)) {
            return true;
        }
        String upperCode = code.toUpperCase();
        return upperCode.contains("THROTTLING")
                || upperCode.contains("LIMIT")
                || upperCode.contains("SYSTEM")
                || upperCode.contains("INTERNAL")
                || upperCode.contains("UNAVAILABLE");
    }

    private String snapshot(SendSmsResponseBody body) {
        return "{\"provider\":\"ALIYUN\",\"code\":\"" + escape(body.getCode())
                + "\",\"message\":\"" + escape(body.getMessage())
                + "\",\"bizId\":\"" + escape(body.getBizId())
                + "\",\"requestId\":\"" + escape(body.getRequestId()) + "\"}";
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
