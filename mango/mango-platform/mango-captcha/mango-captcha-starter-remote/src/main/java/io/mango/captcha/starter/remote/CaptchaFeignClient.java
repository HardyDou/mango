package io.mango.captcha.starter.remote;

import io.mango.captcha.api.CaptchaApi;
import io.mango.captcha.api.dto.BehaviorCaptchaVerifyResponse;
import io.mango.captcha.api.dto.CaptchaResponse;
import io.mango.captcha.api.dto.CaptchaSendRequest;
import io.mango.captcha.api.dto.CaptchaTypesResponse;
import io.mango.captcha.api.dto.CaptchaVerifyRequest;
import io.mango.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 验证码 Feign 适配器。
 */
@FeignClient(name = "mango-captcha", contextId = "captchaFeignClient", path = "/captcha")
public interface CaptchaFeignClient extends CaptchaApi {

    /**
     * 通过远程验证码服务查询支持的验证码类型和存储实现。
     *
     * @return 统一响应，包含验证码类型列表和存储实现名称
     */
    @Override
    @GetMapping("/types")
    R<CaptchaTypesResponse> getTypes();

    /**
     * 通过远程验证码服务生成算术验证码。
     *
     * @return 统一响应，包含验证码键、图片和有效期
     */
    @Override
    @GetMapping("/arithmetic")
    R<CaptchaResponse> generateArithmetic();

    /**
     * 通过远程验证码服务生成滑块验证码。
     *
     * @return 统一响应，包含验证码键、背景图、滑块图和有效期
     */
    @Override
    @GetMapping("/block-puzzle")
    R<CaptchaResponse> generateBlockPuzzle();

    /**
     * 通过远程验证码服务生成点选文字验证码。
     *
     * @return 统一响应，包含验证码键、图片和目标文字
     */
    @Override
    @GetMapping("/click-word")
    R<CaptchaResponse> generateClickWord();

    /**
     * 通过远程验证码服务生成无感行为验证码。
     *
     * @return 统一响应，包含验证码键、挑战参数和有效期
     */
    @Override
    @GetMapping("/behavior")
    R<CaptchaResponse> generateBehavior();

    /**
     * 将行为轨迹提交给远程验证码服务进行风险评分。
     *
     * @param request 行为验证码校验请求
     * @return 统一响应，包含行为评分、风险等级和处置建议
     */
    @Override
    @PostMapping("/behavior/verify")
    R<BehaviorCaptchaVerifyResponse> verifyBehavior(@RequestBody CaptchaVerifyRequest request);

    /**
     * 将验证码答案提交给远程验证码服务进行一次性校验。
     *
     * @param request 验证码校验请求
     * @return 统一响应，数据为是否校验通过
     */
    @Override
    @PostMapping("/verify")
    R<Boolean> verify(@RequestBody CaptchaVerifyRequest request);

    /**
     * 调用远程验证码服务发送内部短信或邮件验证码。
     *
     * @param request 验证码发送请求
     * @return 统一响应，数据为后续校验使用的验证码键
     */
    @Override
    @PostMapping("/send")
    R<String> send(@RequestBody CaptchaSendRequest request);
}
