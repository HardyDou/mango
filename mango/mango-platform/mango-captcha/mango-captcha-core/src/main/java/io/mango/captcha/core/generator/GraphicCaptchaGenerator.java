package io.mango.captcha.core.generator;

import io.mango.captcha.api.constant.CaptchaDifficulty;
import io.mango.captcha.api.dto.CaptchaResponse;

/**
 * 字母数字图形验证码生成器。
 */
public interface GraphicCaptchaGenerator {

    /**
     * 生成指定难度的字母数字图形验证码。
     *
     * @param difficulty 难度级别
     * @return 验证码响应，答案仅供服务端保存
     */
    CaptchaResponse generate(CaptchaDifficulty difficulty);
}
