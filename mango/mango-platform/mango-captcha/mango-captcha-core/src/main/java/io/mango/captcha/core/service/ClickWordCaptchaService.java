package io.mango.captcha.core.service;

import io.mango.captcha.api.dto.CaptchaResponse;

/**
 * 点选文字验证码服务
 *
 * @author Mango
 */
public interface ClickWordCaptchaService {

    /**
     * 生成点选文字验证码
     *
     * @return 验证码响应，extra 字段为服务端校验用答案
     */
    CaptchaResponse generate();
}
