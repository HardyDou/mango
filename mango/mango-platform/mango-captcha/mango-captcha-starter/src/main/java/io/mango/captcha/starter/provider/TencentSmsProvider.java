package io.mango.captcha.starter.provider;

import io.mango.captcha.api.spi.SmsProvider;
import io.mango.captcha.starter.properties.CaptchaProperties;

/**
 * 腾讯云短信供应商。
 * <p>凭证、签名、应用 ID 与地域全部来自 {@code mango.captcha.sms.tencent.*} 配置项，由部署环境通过
 * {@code ${ENV}} 占位符注入；模板编码由调用方传入（对应 {@code mango.captcha.sms.template-code}）。
 * 手机号按 {@code country-code} 规范化后拼接。任何必需项缺失或发送失败统一返回
 * {@code CAPTCHA_SEND_FAILED}(2410)。</p>
 *
 * @author Mango
 */
public class TencentSmsProvider implements SmsProvider {

    public static final String PROVIDER_NAME = "tencent";
    public static final String TEMPLATE_CODE_PROPERTY = "mango.captcha.sms.template-code";
    public static final String SECRET_ID_PROPERTY = "mango.captcha.sms.tencent.secret-id";
    public static final String SECRET_KEY_PROPERTY = "mango.captcha.sms.tencent.secret-key";
    public static final String SIGN_NAME_PROPERTY = "mango.captcha.sms.tencent.sign-name";
    public static final String SDK_APP_ID_PROPERTY = "mango.captcha.sms.tencent.sdk-app-id";
    private static final String DEFAULT_REGION = "ap-guangzhou";
    private static final String DEFAULT_COUNTRY_CODE = "+86";

    private final CaptchaProperties.Sms properties;
    private final TencentSmsSender sender;

    public TencentSmsProvider(CaptchaProperties.Sms properties) {
        this(properties, new TencentSmsSender());
    }

    TencentSmsProvider(CaptchaProperties.Sms properties, TencentSmsSender sender) {
        this.properties = properties == null ? new CaptchaProperties.Sms() : properties;
        this.sender = sender;
    }

    @Override
    public boolean send(String mobile, String templateCode, String... params) {
        SmsProviderSupport.requireText(PROVIDER_NAME, "mobile", mobile);
        SmsProviderSupport.requireConfigured(PROVIDER_NAME, TEMPLATE_CODE_PROPERTY, templateCode);
        String code = SmsProviderSupport.requireTemplateParam(PROVIDER_NAME, params);

        CaptchaProperties.Tencent tencent = tencent();
        String secretId = tencent.getSecretId();
        String secretKey = tencent.getSecretKey();
        String signName = tencent.getSignName();
        String sdkAppId = tencent.getSdkAppId();
        SmsProviderSupport.requireConfigured(PROVIDER_NAME, SECRET_ID_PROPERTY, secretId);
        SmsProviderSupport.requireConfigured(PROVIDER_NAME, SECRET_KEY_PROPERTY, secretKey);
        SmsProviderSupport.requireConfigured(PROVIDER_NAME, SIGN_NAME_PROPERTY, signName);
        SmsProviderSupport.requireConfigured(PROVIDER_NAME, SDK_APP_ID_PROPERTY, sdkAppId);

        String countryCode = SmsProviderSupport.normalizeCountryCode(PROVIDER_NAME,
                SmsProviderSupport.configOrDefault(tencent.getCountryCode(), DEFAULT_COUNTRY_CODE));
        TencentSmsMessage message = new TencentSmsMessage(mobile, countryCode, signName, sdkAppId,
                templateCode, code,
                SmsProviderSupport.configOrDefault(tencent.getEndpoint(), TencentSmsSender.DEFAULT_ENDPOINT),
                SmsProviderSupport.configOrDefault(tencent.getRegion(), DEFAULT_REGION),
                secretId, secretKey);
        return sender.send(message);
    }

    @Override
    public String getName() {
        return PROVIDER_NAME;
    }

    private CaptchaProperties.Tencent tencent() {
        return properties.getTencent() == null ? new CaptchaProperties.Tencent() : properties.getTencent();
    }
}
