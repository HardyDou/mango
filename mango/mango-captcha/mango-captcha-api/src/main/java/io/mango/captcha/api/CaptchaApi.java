package io.mango.captcha.api;

import io.mango.captcha.api.constant.CaptchaType;
import io.mango.captcha.api.dto.CaptchaResponse;
import io.mango.captcha.api.dto.CaptchaSendRequest;
import io.mango.captcha.api.dto.CaptchaVerifyRequest;

import java.util.List;

/**
 * 验证码服务接口
 *
 * @author Mango
 */
public interface CaptchaApi {

    /**
     * 生成验证码
     *
     * @param type   验证码类型
     * @param target 目标（手机号/邮箱），可为null
     * @return 验证码响应
     */
    CaptchaResponse generate(CaptchaType type, String target);

    /**
     * 校验验证码
     *
     * @param request 校验请求
     * @return true-校验通过
     */
    boolean verify(CaptchaVerifyRequest request);

    /**
     * 发送短信验证码
     *
     * @param mobile   手机号
     * @param bizCode  业务标识（如 LOGIN, REGISTER, CHANGE_MOBILE）
     * @param expire   有效期（秒）
     * @return 验证码key
     */
    String sendSms(String mobile, String bizCode, long expire);

    /**
     * 发送邮件验证码
     *
     * @param email   邮箱
     * @param bizCode 业务标识（如 LOGIN, REGISTER, CHANGE_EMAIL）
     * @param expire  有效期（秒）
     * @return 验证码key
     */
    String sendEmail(String email, String bizCode, long expire);

    /**
     * 发送验证码（统一接口，支持SMS和EMAIL）
     *
     * @param request 发送请求
     * @return 验证码key
     */
    String send(CaptchaSendRequest request);

    /**
     * 获取支持的验证码类型
     *
     * @return 类型列表
     */
    List<CaptchaType> getSupportedTypes();

    /**
     * 获取当前存储策略
     *
     * @return 存储策略名称
     */
    String getCurrentStorage();
}
