package io.mango.captcha.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.captcha.api.CaptchaCode;
import io.mango.captcha.api.constant.CaptchaType;
import io.mango.captcha.api.dto.BehaviorCaptchaVerifyResult;
import io.mango.captcha.api.dto.CaptchaResponse;
import io.mango.captcha.api.dto.CaptchaVerifyRequest;
import io.mango.captcha.core.service.ICaptchaService;
import io.mango.common.result.R;
import io.mango.common.result.Require;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 验证码公共接口（无需认证）
 * 公共接口保持 /captcha/*，需要认证的接口使用 /auth/captcha/*。
 *
 * @author Mango
 */
@Slf4j
@RestController
@RequestMapping("/captcha")
@RequiredArgsConstructor
@Tag(name = "验证码-公共", description = "验证码生成、校验公共接口（无需认证）")
@ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "验证码公共接口")
public class CaptchaController {

    private final ICaptchaService captchaService;

    /**
     * 获取支持的验证码类型
     */
    @GetMapping("/types")
    @Operation(summary = "获取验证码类型", description = "获取当前支持的验证码类型列表和存储策略")
    public R<Map<String, Object>> getTypes() {
        Map<String, Object> result = new HashMap<>();
        result.put("types", captchaService.getSupportedTypes());
        result.put("currentStorage", captchaService.getCurrentStorage());
        return R.ok(result);
    }

    /**
     * 生成算术验证码
     */
    @GetMapping("/arithmetic")
    @Operation(summary = "生成算术验证码", description = "生成算术表达式验证码，答案在extra字段返回")
    public R<CaptchaResponse> generateArithmetic() {
        CaptchaResponse response = captchaService.generate(CaptchaType.ARITHMETIC, null);
        return R.ok(response);
    }

    /**
     * 生成滑块验证码
     */
    @GetMapping("/block-puzzle")
    @Operation(summary = "生成滑块验证码", description = "生成滑块拼图验证码")
    public R<CaptchaResponse> generateBlockPuzzle() {
        CaptchaResponse response = captchaService.generate(CaptchaType.BLOCK_PUZZLE, null);
        return R.ok(response);
    }

    /**
     * 生成点选文字验证码
     */
    @GetMapping("/click-word")
    @Operation(summary = "生成点选文字验证码", description = "生成按提示依次点击图片文字的验证码")
    public R<CaptchaResponse> generateClickWord() {
        CaptchaResponse response = captchaService.generate(CaptchaType.CLICK_WORD, null);
        return R.ok(response);
    }

    /**
     * 生成无感行为验证 challenge。
     */
    @GetMapping("/behavior")
    @Operation(summary = "生成无感行为验证", description = "生成无感行为验证 challenge，前端静默采集行为后提交 verify")
    public R<CaptchaResponse> generateBehavior() {
        CaptchaResponse response = captchaService.generate(CaptchaType.BEHAVIOR, null);
        return R.ok(response);
    }

    /**
     * 校验无感行为验证。
     */
    @PostMapping("/behavior/verify")
    @Operation(summary = "校验无感行为验证", description = "校验前端行为数据并返回 score、riskLevel 和 suggestAction")
    public R<BehaviorCaptchaVerifyResult> verifyBehavior(@Valid @RequestBody CaptchaVerifyRequest request) {
        request.setType(CaptchaType.BEHAVIOR);
        BehaviorCaptchaVerifyResult result = captchaService.verifyBehavior(request);
        return R.ok(result);
    }

    /**
     * 校验验证码
     */
    @PostMapping("/verify")
    @Operation(summary = "校验验证码", description = "校验验证码是否正确")
    public R<Boolean> verify(@Valid @RequestBody CaptchaVerifyRequest request) {
        boolean result = captchaService.verify(request);
        Require.isTrue(result, CaptchaCode.CAPTCHA_INVALID);
        return R.ok(true);
    }
}
