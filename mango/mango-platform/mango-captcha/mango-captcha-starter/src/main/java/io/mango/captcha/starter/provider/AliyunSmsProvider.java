package io.mango.captcha.starter.provider;

import io.mango.captcha.api.spi.SmsProvider;
import io.mango.captcha.starter.properties.CaptchaProperties;

/**
 * 阿里云短信供应商。
 *
 * <p>凭证、签名与地域全部来自 {@code mango.captcha.sms.aliyun.*} 配置项，由部署环境通过
 * {@code ${ENV}} 占位符注入；模板编码由调用方传入（对应 {@code mango.captcha.sms.template-code}）。
 * 任何必需项缺失或发送失败统一返回 {@code CAPTCHA_SEND_FAILED}(2410)。</p>
 *
 * @author Mango
 */
public class AliyunSmsProvider implements SmsProvider {

    public static final String PROVIDER_NAME = "aliyun";
    public static final String TEMPLATE_CODE_PROPERTY = "mango.captcha.sms.template-code";
    public static final String ACCESS_KEY_ID_PROPERTY = "mango.captcha.sms.aliyun.access-key-id";
    public static final String ACCESS_KEY_SECRET_PROPERTY = "mango.captcha.sms.aliyun.access-key-secret";
    public static final String SIGN_NAME_PROPERTY = "mango.captcha.sms.aliyun.sign-name";
    private static final String DEFAULT_REGION = "cn-hangzhou";

    private final CaptchaProperties.Sms properties;
    private final AliyunSmsSender sender;

    public AliyunSmsProvider(CaptchaProperties.Sms properties) {
        this(properties, new AliyunSmsSender());
    }

    AliyunSmsProvider(CaptchaProperties.Sms properties, AliyunSmsSender sender) {
        this.properties = properties == null ? new CaptchaProperties.Sms() : properties;
        this.sender = sender;
    }

    @Override
    public boolean send(String mobile, String templateCode, String... params) {
        SmsProviderSupport.requireText(PROVIDER_NAME, "mobile", mobile);
        SmsProviderSupport.requireConfigured(PROVIDER_NAME, TEMPLATE_CODE_PROPERTY, templateCode);
        String code = SmsProviderSupport.requireTemplateParam(PROVIDER_NAME, params);

        CaptchaProperties.Aliyun aliyun = aliyun();
        String accessKeyId = aliyun.getAccessKeyId();
        String accessKeySecret = aliyun.getAccessKeySecret();
        String signName = aliyun.getSignName();
        SmsProviderSupport.requireConfigured(PROVIDER_NAME, ACCESS_KEY_ID_PROPERTY, accessKeyId);
        SmsProviderSupport.requireConfigured(PROVIDER_NAME, ACCESS_KEY_SECRET_PROPERTY, accessKeySecret);
        SmsProviderSupport.requireConfigured(PROVIDER_NAME, SIGN_NAME_PROPERTY, signName);

        AliyunSmsMessage message = new AliyunSmsMessage(mobile, signName, templateCode, code,
                SmsProviderSupport.configOrDefault(aliyun.getEndpoint(), AliyunSmsSender.DEFAULT_ENDPOINT),
                SmsProviderSupport.configOrDefault(aliyun.getRegion(), DEFAULT_REGION),
                accessKeyId, accessKeySecret);
        return sender.send(message);
    }

    @Override
    public String getName() {
        return PROVIDER_NAME;
    }

    private CaptchaProperties.Aliyun aliyun() {
        return properties.getAliyun() == null ? new CaptchaProperties.Aliyun() : properties.getAliyun();
    }
}
