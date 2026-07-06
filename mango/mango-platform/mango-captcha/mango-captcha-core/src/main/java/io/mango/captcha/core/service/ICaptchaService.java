package io.mango.captcha.core.service;

import io.mango.captcha.api.constant.CaptchaType;
import io.mango.captcha.api.dto.BehaviorCaptchaVerifyResult;
import io.mango.captcha.api.dto.CaptchaResponse;
import io.mango.captcha.api.dto.CaptchaSendRequest;
import io.mango.captcha.api.dto.CaptchaVerifyRequest;

import java.util.List;

/**
 * 验证码本地服务接口。
 */
public interface ICaptchaService {

    CaptchaResponse generate(CaptchaType type, String target);

    boolean verify(CaptchaVerifyRequest request);

    BehaviorCaptchaVerifyResult verifyBehavior(CaptchaVerifyRequest request);

    String sendSms(String mobile, String bizCode, long expire);

    String sendEmail(String email, String bizCode, long expire);

    String send(CaptchaSendRequest request);

    List<CaptchaType> getSupportedTypes();

    String getCurrentStorage();
}
