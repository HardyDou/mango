package io.mango.captcha.core.service;

import io.mango.captcha.api.dto.CaptchaResponse;
import io.mango.captcha.api.constant.CaptchaType;

/**
 * 算术验证码服务
 *
 * @author Mango
 */
public interface ArithmeticCaptchaService {

    /**
     * 生成算术验证码
     *
     * @return 验证码响应
     */
    CaptchaResponse generate();
}
