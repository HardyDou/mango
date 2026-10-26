package io.mango.captcha.core.generator;

import io.mango.captcha.api.constant.CaptchaDifficulty;
import io.mango.captcha.api.dto.CaptchaResponse;
import io.mango.captcha.api.constant.CaptchaType;

/**
 * 算术验证码服务
 *
 * @author Mango
 */
public interface ArithmeticCaptchaGenerator {

    /**
     * 生成算术验证码
     *
     * @return 验证码响应
     */
    CaptchaResponse generate();

    /**
     * 生成指定难度的算术验证码；存量实现默认回退到无参数生成方式。
     *
     * @param difficulty 难度级别
     * @return 验证码响应
     */
    default CaptchaResponse generate(CaptchaDifficulty difficulty) {
        return generate();
    }
}
