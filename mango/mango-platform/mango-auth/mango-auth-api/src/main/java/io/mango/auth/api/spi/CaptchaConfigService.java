package io.mango.auth.api.spi;

/**
 * 验证码配置 SPI。
 * 业务方可以实现该接口，按路径自定义验证码要求。
 */
public interface CaptchaConfigService {

    /**
     * 判断指定路径是否需要验证码。
     * @param path 请求路径
     * @return true 表示需要验证码，false 表示不需要验证码
     */
    boolean isCaptchaRequired(String path);

    /**
     * 获取指定路径需要的验证码类型。
     * @param path 请求路径
     * @return 验证码类型，返回 null 时使用默认类型
     */
    String getCaptchaType(String path);

    /**
     * 获取验证码有效期。
     * @param path 请求路径
     * @return 有效期，单位秒
     */
    long getCaptchaTtl(String path);
}
