package io.mango.captcha.core.service;

import io.mango.captcha.api.dto.BehaviorCaptchaVerifyResult;
import io.mango.captcha.api.dto.CaptchaResponse;

/**
 * 无感行为验证服务。
 */
public interface BehaviorCaptchaService {

    /**
     * 创建无感行为验证 challenge。
     *
     * @return challenge 响应
     */
    CaptchaResponse generate();

    /**
     * 创建服务端保存的 challenge 内容。
     *
     * @param key 验证码键
     * @return challenge JSON
     */
    String createChallengeJson(String key);

    /**
     * 校验行为数据并输出评分结果。
     *
     * @param challengeJson 服务端保存的 challenge
     * @param payloadJson 前端提交的行为数据
     * @return 评分结果
     */
    BehaviorCaptchaVerifyResult verify(String challengeJson, String payloadJson);
}
