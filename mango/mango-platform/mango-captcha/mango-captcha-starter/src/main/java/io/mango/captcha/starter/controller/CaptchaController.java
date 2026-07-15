package io.mango.captcha.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.captcha.api.CaptchaApi;
import io.mango.captcha.api.dto.BehaviorCaptchaVerifyResponse;
import io.mango.captcha.api.dto.CaptchaResponse;
import io.mango.captcha.api.dto.CaptchaSendRequest;
import io.mango.captcha.api.dto.CaptchaTypesResponse;
import io.mango.captcha.api.dto.CaptchaVerifyRequest;
import io.mango.captcha.core.service.ICaptchaService;
import io.mango.common.result.R;
import io.mango.infra.web.api.Inner;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 验证码 HTTP 适配器。
 */
@Validated
@RestController
@RequestMapping("/captcha")
@RequiredArgsConstructor
@Tag(name = "验证码-公共", description = "验证码生成、校验公共接口")
@ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "验证码公共接口")
public class CaptchaController implements CaptchaApi {

    private final ICaptchaService captchaService;

    @Override
    @GetMapping("/types")
    @Operation(summary = "获取验证码类型", description = "获取当前支持的验证码类型列表和存储策略")
    public R<CaptchaTypesResponse> getTypes() {
        return R.ok(captchaService.getTypes());
    }

    @Override
    @GetMapping("/arithmetic")
    @Operation(summary = "生成算术验证码", description = "生成算术表达式验证码")
    public R<CaptchaResponse> generateArithmetic() {
        return R.ok(captchaService.generateArithmetic());
    }

    @Override
    @GetMapping("/block-puzzle")
    @Operation(summary = "生成滑块验证码", description = "生成滑块拼图验证码")
    public R<CaptchaResponse> generateBlockPuzzle() {
        return R.ok(captchaService.generateBlockPuzzle());
    }

    @Override
    @GetMapping("/click-word")
    @Operation(summary = "生成点选文字验证码", description = "生成按提示依次点击图片文字的验证码")
    public R<CaptchaResponse> generateClickWord() {
        return R.ok(captchaService.generateClickWord());
    }

    @Override
    @GetMapping("/behavior")
    @Operation(summary = "生成无感行为验证", description = "生成无感行为验证 challenge")
    public R<CaptchaResponse> generateBehavior() {
        return R.ok(captchaService.generateBehavior());
    }

    @Override
    @PostMapping("/behavior/verify")
    @Operation(summary = "校验无感行为验证", description = "校验前端行为数据并返回评分结果")
    public R<BehaviorCaptchaVerifyResponse> verifyBehavior(@RequestBody CaptchaVerifyRequest request) {
        return R.ok(captchaService.verifyBehavior(request));
    }

    @Override
    @PostMapping("/verify")
    @Operation(summary = "校验验证码", description = "校验验证码并在成功后使其失效")
    public R<Boolean> verify(@RequestBody CaptchaVerifyRequest request) {
        return R.ok(captchaService.verify(request));
    }

    @Override
    @Inner
    @ApiAccess(mode = ApiResourceAccessMode.INTERNAL, desc = "内部发送短信或邮件验证码")
    @PostMapping("/send")
    @Operation(summary = "发送验证码", description = "仅内部调用，发送短信或邮件验证码")
    public R<String> send(@RequestBody CaptchaSendRequest request) {
        return R.ok(captchaService.send(request));
    }
}
