package io.mango.captcha.core.generator;

import io.mango.captcha.api.dto.CaptchaResponse;

/**
 * 滑块验证码服务
 *
 * @author Mango
 */
public interface BlockPuzzleCaptchaGenerator {

    /**
     * 生成滑块验证码
     *
     * @return 验证码响应
     */
    CaptchaResponse generate();
}
