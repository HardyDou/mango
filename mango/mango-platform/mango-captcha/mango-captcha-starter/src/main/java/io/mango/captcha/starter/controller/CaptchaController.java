package io.mango.captcha.starter.controller;

import io.mango.captcha.api.CaptchaApi;
import io.mango.captcha.api.constant.CaptchaType;
import io.mango.captcha.api.dto.CaptchaResponse;
import io.mango.captcha.api.dto.CaptchaVerifyRequest;
import io.mango.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 验证码公共接口（无需认证）
 * Issue B Fix: 公共接口保持 /captcha/*，需要认证的接口使用 /auth/captcha/*
 *
 * @author Mango
 */
@Slf4j
@RestController
@RequestMapping("/captcha")
@RequiredArgsConstructor
@Tag(name = "验证码-公共", description = "验证码生成、校验公共接口（无需认证）")
public class CaptchaController {

    private final CaptchaApi captchaApi;

    /**
     * 获取支持的验证码类型
     */
    @GetMapping("/types")
    @Operation(summary = "获取验证码类型", description = "获取当前支持的验证码类型列表和存储策略")
    public R<Map<String, Object>> getTypes() {
        Map<String, Object> result = new HashMap<>();
        result.put("types", captchaApi.getSupportedTypes());
        result.put("currentStorage", captchaApi.getCurrentStorage());
        return R.ok(result);
    }

    /**
     * 生成算术验证码
     */
    @GetMapping("/arithmetic")
    @Operation(summary = "生成算术验证码", description = "生成算术表达式验证码，答案在extra字段返回")
    public R<CaptchaResponse> generateArithmetic() {
        CaptchaResponse response = captchaApi.generate(CaptchaType.ARITHMETIC, null);
        return R.ok(response);
    }

    /**
     * 生成滑块验证码
     */
    @GetMapping("/block-puzzle")
    @Operation(summary = "生成滑块验证码", description = "生成滑块拼图验证码")
    public R<CaptchaResponse> generateBlockPuzzle() {
        CaptchaResponse response = captchaApi.generate(CaptchaType.BLOCK_PUZZLE, null);
        return R.ok(response);
    }

    /**
     * 校验验证码
     */
    @PostMapping("/verify")
    @Operation(summary = "校验验证码", description = "校验验证码是否正确")
    public R<Boolean> verify(@Valid @RequestBody CaptchaVerifyRequest request) {
        boolean result = captchaApi.verify(request);
        if (!result) {
            return R.fail(400, "验证码校验失败");
        }
        return R.ok(true);
    }
}
