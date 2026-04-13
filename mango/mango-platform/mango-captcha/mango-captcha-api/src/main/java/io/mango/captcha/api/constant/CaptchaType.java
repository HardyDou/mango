package io.mango.captcha.api.constant;

/**
 * 验证码类型
 *
 * @author Mango
 */
public enum CaptchaType {

    /**
     * 算术验证码 - 简单计算题
     */
    ARITHMETIC("算术验证码"),

    /**
     * 滑块验证码 - 拖动滑块完成拼图
     */
    BLOCK_PUZZLE("滑块验证码"),

    /**
     * 短信验证码
     */
    SMS("短信验证码"),

    /**
     * 邮件验证码
     */
    EMAIL("邮件验证码");

    private final String description;

    CaptchaType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
