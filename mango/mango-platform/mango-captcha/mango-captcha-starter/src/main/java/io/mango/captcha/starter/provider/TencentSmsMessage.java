package io.mango.captcha.starter.provider;

/**
 * 腾讯云短信发送参数。
 *
 * <p>由 {@link TencentSmsProvider} 从 {@code mango.captcha.sms.tencent.*} 配置项解析后构造，
 * 一经创建不可变；{@code toString} 不输出凭证与手机号。</p>
 *
 * @param mobile        手机号，原样透传给短信接口
 * @param countryCode   国家码，默认 {@code +86}
 * @param signName      短信签名
 * @param sdkAppId      短信应用 ID
 * @param templateCode  短信模板编码
 * @param templateParam 模板参数（验证码）
 * @param endpoint      接口地址
 * @param region        地域
 * @param secretId      访问密钥 ID
 * @param secretKey     访问密钥 Key
 * @author Mango
 */
public record TencentSmsMessage(String mobile, String countryCode, String signName,
                                String sdkAppId, String templateCode, String templateParam,
                                String endpoint, String region, String secretId, String secretKey) {

    @Override
    public String toString() {
        return "TencentSmsMessage[signName=" + signName + ", sdkAppId=" + sdkAppId
                + ", templateCode=" + templateCode + ", endpoint=" + endpoint + ", region=" + region + "]";
    }
}
