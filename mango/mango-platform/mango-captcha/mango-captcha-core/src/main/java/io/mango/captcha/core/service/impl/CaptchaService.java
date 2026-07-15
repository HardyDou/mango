package io.mango.captcha.core.service.impl;

import cn.hutool.core.lang.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.captcha.api.constant.CaptchaType;
import io.mango.captcha.api.enums.CaptchaCode;
import io.mango.captcha.api.dto.BehaviorCaptchaVerifyResponse;
import io.mango.captcha.api.dto.CaptchaResponse;
import io.mango.captcha.api.dto.CaptchaSendRequest;
import io.mango.captcha.api.dto.CaptchaTypesResponse;
import io.mango.captcha.api.dto.CaptchaVerifyRequest;
import io.mango.captcha.core.generator.BehaviorCaptchaEngine;
import io.mango.captcha.api.spi.EmailProvider;
import io.mango.captcha.api.spi.SmsProvider;
import io.mango.captcha.core.generator.ArithmeticCaptchaGenerator;
import io.mango.captcha.core.generator.BlockPuzzleCaptchaGenerator;
import io.mango.captcha.core.generator.ClickWordCaptchaGenerator;
import io.mango.captcha.core.service.ICaptchaService;
import io.mango.common.result.Require;
import io.mango.infra.kv.api.IKvStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 验证码服务实现
 *
 * @author Mango
 */
@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("PMD.ServiceOrDaoClassShouldEndWithImplRule")
public class CaptchaService implements ICaptchaService {

    private static final String KEY_PREFIX = "captcha:";
    private static final int SLIDER_TOLERANCE = 5;
    private static final int DIGIT_BOUND = 10;
    private static final int DEFAULT_CLICK_TOLERANCE = 24;
    private static final int DEFAULT_CLICK_WIDTH = 320;
    private static final int DEFAULT_CLICK_HEIGHT = 180;
    private static final String MESSAGE_SUBJECT = "验证码";
    private static final String MESSAGE_PREFIX = "您的验证码是：";

    private final IKvStore kvStore;
    private final ArithmeticCaptchaGenerator arithmeticCaptchaService;
    private final BlockPuzzleCaptchaGenerator blockPuzzleCaptchaService;
    private final ClickWordCaptchaGenerator clickWordCaptchaService;
    private final BehaviorCaptchaEngine behaviorCaptchaService;
    private final List<SmsProvider> smsProviders;
    private final List<EmailProvider> emailProviders;
    private final ObjectMapper objectMapper;

    @Value("${mango.captcha.ttl:300}")
    private long defaultTtl;

    @Value("${mango.captcha.sms.length:6}")
    private int smsCodeLength;

    @Value("${mango.captcha.email.length:6}")
    private int emailCodeLength;

    private CaptchaResponse generate(CaptchaType type) {
        String key = UUID.randomUUID().toString(true);
        CaptchaResponse response = new CaptchaResponse();
        response.setKey(key);
        response.setType(type);

        switch (type) {
            case ARITHMETIC:
                CaptchaResponse arithmetic = arithmeticCaptchaService.generate();
                response.setImage(arithmetic.getImage());
                response.setExpireTime(defaultTtl);
                response.setExtra(arithmetic.getExtra());
                kvStore.set(KEY_PREFIX + key, arithmetic.getExtra(), defaultTtl);
                break;
            case BLOCK_PUZZLE:
                CaptchaResponse puzzle = blockPuzzleCaptchaService.generate();
                response.setBackgroundImage(puzzle.getBackgroundImage());
                response.setSliderImage(puzzle.getSliderImage());
                response.setX(puzzle.getX());
                response.setY(puzzle.getY());
                response.setExpireTime(defaultTtl);
                kvStore.set(KEY_PREFIX + key, String.valueOf(puzzle.getX()), defaultTtl);
                break;
            case CLICK_WORD:
                CaptchaResponse clickWord = clickWordCaptchaService.generate();
                response.setImage(clickWord.getImage());
                response.setTarget(clickWord.getTarget());
                response.setExpireTime(defaultTtl);
                response.setExtra(toClickWordPublicExtra(clickWord.getExtra()));
                kvStore.set(KEY_PREFIX + key, clickWord.getExtra(), defaultTtl);
                break;
            case BEHAVIOR:
                CaptchaResponse behavior = behaviorCaptchaService.generate();
                response.setExpireTime(behavior.getExpireTime());
                response.setExtra(behavior.getExtra());
                kvStore.set(KEY_PREFIX + key, createBehaviorChallenge(key), behavior.getExpireTime());
                break;
            default:
                Require.isTrue(false, CaptchaCode.CAPTCHA_INVALID, "不支持的验证码类型");
                break;
        }

        return response;
    }

    @Override
    public CaptchaTypesResponse getTypes() {
        CaptchaTypesResponse response = new CaptchaTypesResponse();
        response.setTypes(Arrays.asList(CaptchaType.values()));
        response.setCurrentStorage(kvStore.getClass().getSimpleName());
        return response;
    }

    @Override
    public CaptchaResponse generateArithmetic() {
        return generate(CaptchaType.ARITHMETIC);
    }

    @Override
    public CaptchaResponse generateBlockPuzzle() {
        return generate(CaptchaType.BLOCK_PUZZLE);
    }

    @Override
    public CaptchaResponse generateClickWord() {
        return generate(CaptchaType.CLICK_WORD);
    }

    @Override
    public CaptchaResponse generateBehavior() {
        return generate(CaptchaType.BEHAVIOR);
    }

    @Override
    public Boolean verify(CaptchaVerifyRequest request) {
        Require.notNull(request, CaptchaCode.CAPTCHA_INVALID, "验证码校验请求不能为空");
        String key = request.getKey();
        String stored = kvStore.get(KEY_PREFIX + key);
        if (stored == null) {
            log.warn("验证码不存在或已过期: key={}", key);
            Require.isTrue(false, CaptchaCode.CAPTCHA_INVALID, "验证码不存在或已过期");
        }

        boolean result;
        // 如果type为空，根据验证参数推断类型
        // pointJson非空 → 滑块验证
        // 否则 → 算术/短信/邮件验证码
        if (request.getType() == CaptchaType.BEHAVIOR) {
            result = verifyBehavior(key, stored, request.getPointJson());
        } else if (request.getType() == CaptchaType.CLICK_WORD) {
            result = verifyClickWord(stored, request.getPointJson());
        } else if (isBlockPuzzleRequest(request)) {
            // 滑块验证：比较X坐标
            try {
                if (request.getPointJson() != null) {
                    var point = objectMapper.readTree(request.getPointJson());
                    int clientX = point.get("x").asInt();
                    int serverX = Integer.parseInt(stored);
                    // 容许5像素误差
                    result = Math.abs(clientX - serverX) <= SLIDER_TOLERANCE;
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

        Require.isTrue(result, CaptchaCode.CAPTCHA_INVALID, "验证码校验失败");
        return true;
    }

    @Override
    public BehaviorCaptchaVerifyResponse verifyBehavior(CaptchaVerifyRequest request) {
        Require.notNull(request, CaptchaCode.CAPTCHA_INVALID, "行为验证码校验请求不能为空");
        String key = request.getKey();
        String stored = kvStore.get(KEY_PREFIX + key);
        if (stored == null) {
            BehaviorCaptchaVerifyResponse result = new BehaviorCaptchaVerifyResponse();
            result.setKey(key);
            result.setScore(0.0D);
            result.setPassed(false);
            result.setRiskLevel("HIGH");
            result.setSuggestAction("DENY");
            result.setReason("CHALLENGE_NOT_FOUND");
            return result;
        }
        BehaviorCaptchaVerifyResponse result = behaviorCaptchaService.verify(stored, request.getPointJson());
        result.setKey(key);
        if (result.isPassed()) {
            kvStore.delete(KEY_PREFIX + key);
        }
        return result;
    }

    @Override
    public String send(CaptchaSendRequest request) {
        Require.notNull(request, CaptchaCode.CAPTCHA_INVALID, "验证码发送请求不能为空");
        CaptchaType type = request.getType();
        String target = request.getTarget();
        String businessType = request.getBusinessType();
        long expire = defaultTtl;
        if (request.getExpireSeconds() != null) {
            expire = request.getExpireSeconds();
        }

        int codeLength = emailCodeLength;
        if (type == CaptchaType.SMS) {
            codeLength = smsCodeLength;
        }
        String code = generateCode(codeLength);
        String key = KEY_PREFIX + businessType + ":" + target;

        log.info("验证码已生成: type={}, target={}, businessType={}", type, target, businessType);

        boolean sent = switch (type) {
            case SMS -> !smsProviders.isEmpty() && smsProviders.get(0).send(target, null, code);
            case EMAIL -> !emailProviders.isEmpty()
                    && emailProviders.get(0).send(target, MESSAGE_SUBJECT, MESSAGE_PREFIX + code);
            default -> false;
        };
        if (!sent) {
            log.warn("验证码发送失败: type={}, target={}, businessType={}", type, target, businessType);
            return null;
        }

        kvStore.set(key, code, expire);
        return key;
    }

    private String generateCode(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(ThreadLocalRandom.current().nextInt(DIGIT_BOUND));
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
        BehaviorCaptchaVerifyResponse result = behaviorCaptchaService.verify(stored, request.getPointJson());
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
            int tolerance = answer.path("tolerance").asInt(DEFAULT_CLICK_TOLERANCE);
            if (!validPointArrays(answerPoints, requestPoints)) {
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

    private boolean isBlockPuzzleRequest(CaptchaVerifyRequest request) {
        if (request.getType() == CaptchaType.BLOCK_PUZZLE) {
            return true;
        }
        return request.getType() == null && request.getPointJson() != null;
    }

    private boolean validPointArrays(com.fasterxml.jackson.databind.JsonNode answerPoints,
                                     com.fasterxml.jackson.databind.JsonNode requestPoints) {
        return answerPoints != null && requestPoints != null && answerPoints.isArray() && requestPoints.isArray();
    }

    private String toClickWordPublicExtra(String answerJson) {
        try {
            var answer = objectMapper.readTree(answerJson);
            var publicExtra = objectMapper.createObjectNode();
            publicExtra.put("width", answer.path("width").asInt(DEFAULT_CLICK_WIDTH));
            publicExtra.put("height", answer.path("height").asInt(DEFAULT_CLICK_HEIGHT));
            publicExtra.put("pointCount", answer.path("points").size());
            return objectMapper.writeValueAsString(publicExtra);
        } catch (Exception e) {
            return "{\"width\":320,\"height\":180,\"pointCount\":3}";
        }
    }
}
