package io.mango.captcha.core.service.impl;

import cn.hutool.core.lang.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.captcha.api.constant.CaptchaType;
import io.mango.captcha.api.dto.BehaviorCaptchaVerifyResult;
import io.mango.captcha.api.dto.CaptchaResponse;
import io.mango.captcha.api.dto.CaptchaSendRequest;
import io.mango.captcha.api.dto.CaptchaVerifyRequest;
import io.mango.captcha.core.service.BehaviorCaptchaService;
import io.mango.captcha.api.spi.EmailProvider;
import io.mango.captcha.api.spi.SmsProvider;
import io.mango.captcha.core.service.ArithmeticCaptchaService;
import io.mango.captcha.core.service.BlockPuzzleCaptchaService;
import io.mango.captcha.core.service.ClickWordCaptchaService;
import io.mango.captcha.core.service.ICaptchaService;
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
public class CaptchaServiceImpl implements ICaptchaService {

    private static final String KEY_PREFIX = "captcha:";

    private final IKvStore kvStore;
    private final ArithmeticCaptchaService arithmeticCaptchaService;
    private final BlockPuzzleCaptchaService blockPuzzleCaptchaService;
    private final ClickWordCaptchaService clickWordCaptchaService;
    private final BehaviorCaptchaService behaviorCaptchaService;
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
                response.setY(puzzle.getY());
                response.setExpireTime(defaultTtl);
                kvStore.set(KEY_PREFIX + key, String.valueOf(puzzle.getX()), defaultTtl);
            }
            case CLICK_WORD -> {
                CaptchaResponse clickWord = clickWordCaptchaService.generate();
                response.setImage(clickWord.getImage());
                response.setTarget(clickWord.getTarget());
                response.setExpireTime(defaultTtl);
                response.setExtra(toClickWordPublicExtra(clickWord.getExtra()));
                kvStore.set(KEY_PREFIX + key, clickWord.getExtra(), defaultTtl);
            }
            case BEHAVIOR -> {
                CaptchaResponse behavior = behaviorCaptchaService.generate();
                response.setExpireTime(behavior.getExpireTime());
                response.setExtra(behavior.getExtra());
                kvStore.set(KEY_PREFIX + key, createBehaviorChallenge(key), behavior.getExpireTime());
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
        if (request.getType() == CaptchaType.BEHAVIOR) {
            result = verifyBehavior(key, stored, request.getPointJson());
        } else if (request.getType() == CaptchaType.CLICK_WORD) {
            result = verifyClickWord(stored, request.getPointJson());
        } else if (request.getType() == CaptchaType.BLOCK_PUZZLE ||
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
    public BehaviorCaptchaVerifyResult verifyBehavior(CaptchaVerifyRequest request) {
        String key = request.getKey();
        String stored = kvStore.get(KEY_PREFIX + key);
        if (stored == null) {
            BehaviorCaptchaVerifyResult result = new BehaviorCaptchaVerifyResult();
            result.setKey(key);
            result.setScore(0.0D);
            result.setPassed(false);
            result.setRiskLevel("HIGH");
            result.setSuggestAction("DENY");
            result.setReason("CHALLENGE_NOT_FOUND");
            return result;
        }
        BehaviorCaptchaVerifyResult result = behaviorCaptchaService.verify(stored, request.getPointJson());
        result.setKey(key);
        if (result.isPassed()) {
            kvStore.delete(KEY_PREFIX + key);
        }
        return result;
    }

    @Override
    public String sendSms(String mobile, String bizCode, long expire) {
        String code = generateCode(smsCodeLength);
        String key = KEY_PREFIX + "sms:" + bizCode + ":" + mobile;

        log.info("短信验证码已生成: mobile={}, bizCode={}, code={}", mobile, bizCode, code);

        if (smsProviders.isEmpty() || !smsProviders.get(0).send(mobile, null, code)) {
            log.warn("短信验证码发送失败: mobile={}, bizCode={}", mobile, bizCode);
            return null;
        }

        kvStore.set(key, code, expire);
        return key;
    }

    @Override
    public String sendEmail(String email, String bizCode, long expire) {
        String code = generateCode(emailCodeLength);
        String key = KEY_PREFIX + "email:" + bizCode + ":" + email;

        log.info("邮件验证码已生成: email={}, bizCode={}, code={}", email, bizCode, code);

        if (emailProviders.isEmpty() || !emailProviders.get(0).send(email, "验证码", "您的验证码是：" + code)) {
            log.warn("邮件验证码发送失败: email={}, bizCode={}", email, bizCode);
            return null;
        }

        kvStore.set(key, code, expire);
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

        log.info("验证码已生成: type={}, target={}, businessType={}", type, target, businessType);

        boolean sent = switch (type) {
            case SMS -> !smsProviders.isEmpty() && smsProviders.get(0).send(target, null, code);
            case EMAIL -> !emailProviders.isEmpty() && emailProviders.get(0).send(target, "验证码", "您的验证码是：" + code);
            default -> false;
        };
        if (!sent) {
            log.warn("验证码发送失败: type={}, target={}, businessType={}", type, target, businessType);
            return null;
        }

        kvStore.set(key, code, expire);
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

    private String createBehaviorChallenge(String key) {
        return behaviorCaptchaService.createChallengeJson(key);
    }

    private boolean verifyBehavior(String key, String stored, String pointJson) {
        CaptchaVerifyRequest request = new CaptchaVerifyRequest();
        request.setKey(key);
        request.setType(CaptchaType.BEHAVIOR);
        request.setPointJson(pointJson);
        BehaviorCaptchaVerifyResult result = behaviorCaptchaService.verify(stored, request.getPointJson());
        return result.isPassed();
    }

    private boolean verifyClickWord(String stored, String pointJson) {
        if (pointJson == null || pointJson.isBlank()) {
            return false;
        }
        try {
            var answer = objectMapper.readTree(stored);
            var request = objectMapper.readTree(pointJson);
            var answerPoints = answer.get("points");
            var requestPoints = request.get("points");
            int tolerance = answer.path("tolerance").asInt(24);
            if (answerPoints == null || requestPoints == null || !answerPoints.isArray() || !requestPoints.isArray()) {
                return false;
            }
            if (answerPoints.size() != requestPoints.size()) {
                return false;
            }
            for (int i = 0; i < answerPoints.size(); i++) {
                int serverX = answerPoints.get(i).path("x").asInt();
                int serverY = answerPoints.get(i).path("y").asInt();
                int clientX = requestPoints.get(i).path("x").asInt();
                int clientY = requestPoints.get(i).path("y").asInt();
                if (Math.hypot(clientX - serverX, clientY - serverY) > tolerance) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            log.error("点选文字验证码解析失败: {}", e.getMessage());
            return false;
        }
    }

    private String toClickWordPublicExtra(String answerJson) {
        try {
            var answer = objectMapper.readTree(answerJson);
            var publicExtra = objectMapper.createObjectNode();
            publicExtra.put("width", answer.path("width").asInt(320));
            publicExtra.put("height", answer.path("height").asInt(180));
            publicExtra.put("pointCount", answer.path("points").size());
            return objectMapper.writeValueAsString(publicExtra);
        } catch (Exception e) {
            return "{\"width\":320,\"height\":180,\"pointCount\":3}";
        }
    }
}
