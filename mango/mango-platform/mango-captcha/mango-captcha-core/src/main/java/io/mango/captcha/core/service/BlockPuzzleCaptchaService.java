package io.mango.captcha.core.service;

import io.mango.captcha.api.dto.CaptchaResponse;

/**
 * 滑块验证码服务
 *
 * @author Mango
 */
public interface BlockPuzzleCaptchaService {

    /**
     * 生成滑块验证码
     *
     * @return 验证码响应
     */
    CaptchaResponse generate();
}
