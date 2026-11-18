package io.mango.captcha.starter.provider;

/**
 * 阿里云短信发送参数。
 *
 * <p>由 {@link AliyunSmsProvider} 从 {@code mango.captcha.sms.aliyun.*} 配置项解析后构造，
 * 一经创建不可变；{@code toString} 不输出凭证与手机号。</p>
 *
 * @param mobile          手机号，原样透传给短信接口
 * @param signName        短信签名
 * @param templateCode    短信模板编码
 * @param templateParam   模板参数（验证码）
 * @param endpoint        接口地址
 * @param region          地域
 * @param accessKeyId     访问密钥 ID
 * @param accessKeySecret 访问密钥 Secret
 * @author Mango
 */
public record AliyunSmsMessage(String mobile, String signName, String templateCode,
                               String templateParam, String endpoint, String region,
                               String accessKeyId, String accessKeySecret) {

    @Override
    public String toString() {
        return "AliyunSmsMessage[signName=" + signName + ", templateCode=" + templateCode
                + ", endpoint=" + endpoint + ", region=" + region + "]";
    }
}
