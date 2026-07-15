package io.mango.captcha.core.service;

import io.mango.captcha.api.dto.BehaviorCaptchaVerifyResponse;
import io.mango.captcha.api.dto.CaptchaResponse;
import io.mango.captcha.api.dto.CaptchaSendRequest;
import io.mango.captcha.api.dto.CaptchaTypesResponse;
import io.mango.captcha.api.dto.CaptchaVerifyRequest;

/**
 * 验证码业务服务。
 */
public interface ICaptchaService {

    /**
     * 查询验证码模块实际提供的类型和存储实现信息。
     *
     * @return 验证码能力信息
     */
    CaptchaTypesResponse getTypes();

    /**
     * 创建算术表达式图片并保存一次性答案。
     *
     * @return 算术验证码响应
     */
    CaptchaResponse generateArithmetic();

    /**
     * 创建滑块拼图图片并保存服务端坐标。
     *
     * @return 滑块验证码响应
     */
    CaptchaResponse generateBlockPuzzle();

    /**
     * 创建点选文字图片并保存服务端点位。
     *
     * @return 点选文字验证码响应
     */
    CaptchaResponse generateClickWord();

    /**
     * 创建无感行为挑战并保存挑战上下文。
     *
     * @return 行为验证码响应
     */
    CaptchaResponse generateBehavior();

    /**
     * 校验客户端行为轨迹并输出评分和处置建议。
     *
     * @param request 行为校验请求
     * @return 行为评分结果
     */
    BehaviorCaptchaVerifyResponse verifyBehavior(CaptchaVerifyRequest request);

    /**
     * 校验验证码答案，并在成功后删除一次性答案。
     *
     * @param request 验证码校验请求
     * @return 校验通过时返回 {@code true}
     */
    Boolean verify(CaptchaVerifyRequest request);

    /**
     * 通过短信或邮件提供者发送验证码并保存答案。
     *
     * @param request 验证码发送请求
     * @return 后续校验使用的验证码键
     */
    String send(CaptchaSendRequest request);
}
