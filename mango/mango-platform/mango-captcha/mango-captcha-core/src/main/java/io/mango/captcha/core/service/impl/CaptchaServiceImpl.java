package io.mango.captcha.core.service.impl;

import cn.hutool.core.lang.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.captcha.api.CaptchaApi;
import io.mango.captcha.api.constant.CaptchaType;
import io.mango.captcha.api.dto.CaptchaResponse;
import io.mango.captcha.api.dto.CaptchaSendRequest;
import io.mango.captcha.api.dto.CaptchaVerifyRequest;
import io.mango.captcha.api.spi.EmailProvider;
import io.mango.captcha.api.spi.SmsProvider;
import io.mango.captcha.core.service.ArithmeticCaptchaService;
import io.mango.captcha.core.service.BlockPuzzleCaptchaService;
import io.mango.infra.kv.api.IKvStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * 验证码服务实现
 *
 * @author Mango
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaServiceImpl implements CaptchaApi {

    private static final String KEY_PREFIX = "captcha:";

    private final IKvStore kvStore;
    private final ArithmeticCaptchaService arithmeticCaptchaService;
    private final BlockPuzzleCaptchaService blockPuzzleCaptchaService;
    private final List<SmsProvider> smsProviders;
    private final List<EmailProvider> emailProviders;
    private final ObjectMapper objectMapper;

    @Value("${mango.captcha.ttl:300}")
    private long defaultTtl;

    @Value("${mango.captcha.sms.length:6}")
    private int smsCodeLength;

    @Value("${mango.captcha.email.length:6}")
    private int emailCodeLength;

    @Override
    public CaptchaResponse generate(CaptchaType type, String target) {
        String key = UUID.randomUUID().toString(true);
        CaptchaResponse response = new CaptchaResponse();
        response.setKey(key);
        response.setType(type);

        switch (type) {
            case ARITHMETIC -> {
                CaptchaResponse arithmetic = arithmeticCaptchaService.generate();
                response.setImage(arithmetic.getImage());
                response.setExpireTime(defaultTtl);
                response.setExtra(arithmetic.getExtra());
                kvStore.set(KEY_PREFIX + key, arithmetic.getExtra(), defaultTtl);
            }
            case BLOCK_PUZZLE -> {
                CaptchaResponse puzzle = blockPuzzleCaptchaService.generate();
                response.setBackgroundImage(puzzle.getBackgroundImage());
                response.setSliderImage(puzzle.getSliderImage());
                response.setX(puzzle.getX());
                response.setExpireTime(defaultTtl);
                kvStore.set(KEY_PREFIX + key, String.valueOf(puzzle.getX()), defaultTtl);
            }
            case SMS -> {
                // 短信验证码由sendSms生成
                response.setTarget(target);
                response.setExpireTime(defaultTtl);
            }
            case EMAIL -> {
                // 邮件验证码由sendEmail生成
                response.setTarget(target);
                response.setExpireTime(defaultTtl);
            }
        }

        return response;
    }

    @Override
    public boolean verify(CaptchaVerifyRequest request) {
        String key = request.getKey();
        String stored = kvStore.get(KEY_PREFIX + key);
        if (stored == null) {
            log.warn("验证码不存在或已过期: key={}", key);
            return false;
        }

        boolean result;
        // 如果type为空，根据验证参数推断类型
        // pointJson非空 → 滑块验证
        // 否则 → 算术/短信/邮件验证码
        if (request.getType() == CaptchaType.BLOCK_PUZZLE ||
            (request.getType() == null && request.getPointJson() != null)) {
            // 滑块验证：比较X坐标
            try {
                if (request.getPointJson() != null) {
                    var point = objectMapper.readTree(request.getPointJson());
                    int clientX = point.get("x").asInt();
                    int serverX = Integer.parseInt(stored);
                    // 容许5像素误差
                    result = Math.abs(clientX - serverX) <= 5;
                } else {
                    result = false;
                }
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                log.error("滑块验证JSON解析失败: {}", e.getMessage());
                result = false;
            } catch (NumberFormatException e) {
                log.error("滑块坐标格式错误: {}", e.getMessage());
                result = false;
            }
        } else {
            // 算术/短信/邮件验证码：直接比较答案
            result = stored.equalsIgnoreCase(request.getCode());
        }

        if (result) {
            kvStore.delete(KEY_PREFIX + key);
        }

        return result;
    }

    @Override
    public String sendSms(String mobile, String bizCode, long expire) {
        String code = generateCode(smsCodeLength);
        String key = KEY_PREFIX + "sms:" + bizCode + ":" + mobile;

        kvStore.set(key, code, expire);
        log.info("短信验证码已生成: mobile={}, bizCode={}, code={}", mobile, bizCode, code);

        // 如果配置了短信供应商，发送短信
        if (!smsProviders.isEmpty()) {
            smsProviders.get(0).send(mobile, null, code);
        }

        return key;
    }

    @Override
    public String sendEmail(String email, String bizCode, long expire) {
        String code = generateCode(emailCodeLength);
        String key = KEY_PREFIX + "email:" + bizCode + ":" + email;

        kvStore.set(key, code, expire);
        log.info("邮件验证码已生成: email={}, bizCode={}, code={}", email, bizCode, code);

        // 如果配置了邮件供应商，发送邮件
        if (!emailProviders.isEmpty()) {
            emailProviders.get(0).send(email, "验证码", "您的验证码是：" + code);
        }

        return key;
    }

    @Override
    public String send(CaptchaSendRequest request) {
        CaptchaType type = request.getType();
        String target = request.getTarget();
        String businessType = request.getBusinessType();
        long expire = request.getExpireSeconds() != null ? request.getExpireSeconds() : defaultTtl;

        String code = generateCode(type == CaptchaType.SMS ? smsCodeLength : emailCodeLength);
        String key = KEY_PREFIX + businessType + ":" + target;

        kvStore.set(key, code, expire);
        log.info("验证码已生成: type={}, target={}, businessType={}", type, target, businessType);

        // 发送通知
        if (type == CaptchaType.SMS && !smsProviders.isEmpty()) {
            smsProviders.get(0).send(target, null, code);
        } else if (type == CaptchaType.EMAIL && !emailProviders.isEmpty()) {
            emailProviders.get(0).send(target, "验证码", "您的验证码是：" + code);
        }

        return key;
    }

    @Override
    public List<CaptchaType> getSupportedTypes() {
        return Arrays.asList(CaptchaType.values());
    }

    @Override
    public String getCurrentStorage() {
        return kvStore.getClass().getSimpleName();
    }

    private String generateCode(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append((int) (Math.random() * 10));
        }
        return sb.toString();
    }
}
