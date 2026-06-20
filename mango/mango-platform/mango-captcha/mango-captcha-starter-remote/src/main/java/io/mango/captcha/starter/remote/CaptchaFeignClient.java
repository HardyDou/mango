package io.mango.captcha.starter.remote;

import io.mango.captcha.api.CaptchaApi;
import io.mango.captcha.api.constant.CaptchaType;
import io.mango.captcha.api.dto.BehaviorCaptchaVerifyResult;
import io.mango.captcha.api.dto.CaptchaResponse;
import io.mango.captcha.api.dto.CaptchaSendRequest;
import io.mango.captcha.api.dto.CaptchaVerifyRequest;
import io.mango.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * 验证码 Feign 适配器。
 */
@FeignClient(name = "mango-captcha", contextId = "captchaFeignClient", path = "/captcha")
public interface CaptchaFeignClient extends CaptchaApi {

    @Override
    default CaptchaResponse generate(CaptchaType type, String target) {
        return switch (type) {
            case ARITHMETIC -> unwrap(generateArithmetic());
            case BLOCK_PUZZLE -> unwrap(generateBlockPuzzle());
            case CLICK_WORD -> unwrap(generateClickWord());
            case BEHAVIOR -> unwrap(generateBehavior());
            case SMS, EMAIL -> sendAsGeneratedCaptcha(type, target);
        };
    }

    @GetMapping("/arithmetic")
    R<CaptchaResponse> generateArithmetic();

    @GetMapping("/block-puzzle")
    R<CaptchaResponse> generateBlockPuzzle();

    @GetMapping("/click-word")
    R<CaptchaResponse> generateClickWord();

    @GetMapping("/behavior")
    R<CaptchaResponse> generateBehavior();

    @Override
    @PostMapping("/verify")
    default boolean verify(@RequestBody CaptchaVerifyRequest request) {
        return Boolean.TRUE.equals(unwrap(verifyRemote(request)));
    }

    @PostMapping("/verify")
    R<Boolean> verifyRemote(@RequestBody CaptchaVerifyRequest request);

    @Override
    @PostMapping("/behavior/verify")
    default BehaviorCaptchaVerifyResult verifyBehavior(@RequestBody CaptchaVerifyRequest request) {
        return unwrap(verifyBehaviorRemote(request));
    }

    @PostMapping("/behavior/verify")
    R<BehaviorCaptchaVerifyResult> verifyBehaviorRemote(@RequestBody CaptchaVerifyRequest request);

    @Override
    @PostMapping("/send/sms")
    default String sendSms(@RequestParam("mobile") String mobile,
                           @RequestParam("bizCode") String bizCode,
                           @RequestParam("expire") long expire) {
        CaptchaSendRequest request = new CaptchaSendRequest();
        request.setType(CaptchaType.SMS);
        request.setTarget(mobile);
        request.setBusinessType(bizCode);
        request.setExpireSeconds(expire);
        return send(request);
    }

    @Override
    @PostMapping("/send/email")
    default String sendEmail(@RequestParam("email") String email,
                             @RequestParam("bizCode") String bizCode,
                             @RequestParam("expire") long expire) {
        CaptchaSendRequest request = new CaptchaSendRequest();
        request.setType(CaptchaType.EMAIL);
        request.setTarget(email);
        request.setBusinessType(bizCode);
        request.setExpireSeconds(expire);
        return send(request);
    }

    @Override
    @PostMapping("/send")
    default String send(@RequestBody CaptchaSendRequest request) {
        return unwrap(sendRemote(request));
    }

    @PostMapping("/send")
    R<String> sendRemote(@RequestBody CaptchaSendRequest request);

    @Override
    @GetMapping("/types")
    default List<CaptchaType> getSupportedTypes() {
        Object types = unwrap(types());
        if (types instanceof Map<?, ?> map && map.get("types") instanceof List<?> list) {
            return list.stream()
                    .filter(CaptchaType.class::isInstance)
                    .map(CaptchaType.class::cast)
                    .toList();
        }
        return List.of();
    }

    @Override
    @GetMapping("/types")
    default String getCurrentStorage() {
        Object types = unwrap(types());
        if (types instanceof Map<?, ?> map && map.get("currentStorage") instanceof String storage) {
            return storage;
        }
        return null;
    }

    @GetMapping("/types")
    R<Map<String, Object>> types();

    private CaptchaResponse sendAsGeneratedCaptcha(CaptchaType type, String target) {
        CaptchaSendRequest request = new CaptchaSendRequest();
        request.setType(type);
        request.setTarget(target);
        String key = send(request);
        CaptchaResponse response = new CaptchaResponse();
        response.setKey(key);
        response.setType(type);
        return response;
    }

    private static <T> T unwrap(R<T> response) {
        if (response == null || !response.isSuccess()) {
            return null;
        }
        return response.getData();
    }
}
