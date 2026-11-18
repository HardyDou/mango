package io.mango.captcha.starter.provider;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.dysmsapi20170525.models.SendSmsResponseBody;
import com.aliyun.teaopenapi.models.Config;
import io.mango.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

/**
 * 阿里云短信 SDK 调用封装。
 *
 * <p>把验证码发送参数映射为 {@code SendSms} 请求，响应编码为 {@code OK} 视为成功；
 * 其它响应码与 SDK 异常统一转换为 {@link io.mango.captcha.api.enums.CaptchaCode#CAPTCHA_SEND_FAILED}。
 * 日志不输出手机号明文与凭证值。</p>
 *
 * @author Mango
 */
@Slf4j
public class AliyunSmsSender {

    static final String SUCCESS_CODE = "OK";
    static final String DEFAULT_ENDPOINT = "dysmsapi.aliyuncs.com";
    private static final String PROVIDER_NAME = "aliyun";
    private static final String TEMPLATE_PARAM_CODE = "code";
    private static final int BUILDER_CAPACITY_MARGIN = 16;
    private static final char CONTROL_CHAR_LIMIT = 0x20;

    /**
     * 发送验证码短信。
     *
     * @param message 发送参数
     * @return 发送成功返回 {@code true}，失败抛出业务异常
     */
    public boolean send(AliyunSmsMessage message) {
        try {
            SendSmsResponse response = createClient(message).sendSms(buildRequest(message));
            SendSmsResponseBody body = response == null ? null : response.getBody();
            String responseCode = body == null ? null : body.getCode();
            if (SUCCESS_CODE.equalsIgnoreCase(responseCode)) {
                log.info("阿里云短信发送成功: provider={}, templateCode={}, requestId={}",
                        PROVIDER_NAME, message.templateCode(), body.getRequestId());
                return true;
            }
            log.error("阿里云短信发送失败: provider={}, templateCode={}, code={}, message={}, requestId={}",
                    PROVIDER_NAME, message.templateCode(), responseCode,
                    body == null ? null : body.getMessage(), body == null ? null : body.getRequestId());
        } catch (BizException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("阿里云短信接口调用异常: provider={}, templateCode={}, error={}", PROVIDER_NAME,
                    message.templateCode(), exception.getMessage(), exception);
        }
        throw SmsProviderSupport.sendFailed();
    }

    /**
     * 创建阿里云短信客户端。
     *
     * @param message 发送参数
     * @return 阿里云短信客户端
     * @throws Exception 客户端初始化失败
     */
    protected Client createClient(AliyunSmsMessage message) throws Exception {
        Config config = new Config()
                .setAccessKeyId(message.accessKeyId())
                .setAccessKeySecret(message.accessKeySecret())
                .setEndpoint(message.endpoint());
        if (StringUtils.hasText(message.region())) {
            config.setRegionId(message.region());
        }
        return new Client(config);
    }

    SendSmsRequest buildRequest(AliyunSmsMessage message) {
        return new SendSmsRequest()
                .setPhoneNumbers(message.mobile())
                .setSignName(message.signName())
                .setTemplateCode(message.templateCode())
                .setTemplateParam(buildTemplateParam(message.templateParam()));
    }

    /**
     * 构造模板参数 JSON（如 {@code {"code":"123456"}}）。
     *
     * <p>验证码只包含数字，这里仍对特殊字符做转义，避免参数拼接破坏 JSON 结构。</p>
     *
     * @param code 验证码
     * @return 模板参数 JSON
     */
    String buildTemplateParam(String code) {
        return "{\"" + TEMPLATE_PARAM_CODE + "\":\"" + escapeJson(code) + "\"}";
    }

    private static String escapeJson(String value) {
        StringBuilder builder = new StringBuilder(value.length() + BUILDER_CAPACITY_MARGIN);
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            switch (current) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (current < CONTROL_CHAR_LIMIT) {
                        builder.append(String.format("\\u%04x", (int) current));
                    } else {
                        builder.append(current);
                    }
                }
            }
        }
        return builder.toString();
    }
}
