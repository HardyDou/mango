package io.mango.captcha.api;

import io.mango.captcha.api.constant.CaptchaDifficulty;
import io.mango.captcha.api.constant.GraphicCaptchaType;
import io.mango.captcha.api.dto.BehaviorCaptchaVerifyResponse;
import io.mango.captcha.api.dto.CaptchaResponse;
import io.mango.captcha.api.dto.CaptchaSendRequest;
import io.mango.captcha.api.dto.CaptchaTypesResponse;
import io.mango.captcha.api.dto.CaptchaVerifyRequest;
import io.mango.captcha.api.validation.OptionalEnumSelection;
import io.mango.common.result.R;
import io.mango.infra.web.api.Inner;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

/**
 * 验证码服务接口
 *
 * @author Mango
 */
@Validated
public interface CaptchaApi {

    /**
     * 查询当前服务支持的验证码类型以及实际启用的存储实现。
     *
     * @return 统一响应，包含验证码类型列表和存储实现名称
     */
    R<CaptchaTypesResponse> getTypes();

    /**
     * 生成供用户计算并填写答案的算术验证码。
     *
     * @return 统一响应，包含验证码键、图片和有效期
     */
    R<CaptchaResponse> generateArithmetic();

    /**
     * 生成按全局默认策略或会话参数决定题型和难度的图形验证码。
     *
     * @param type 会话题型，可为空；为空时使用全局默认题型
     * @param difficulty 会话难度，可为空；为空时使用全局默认难度
     * @return 统一响应，包含验证码键、图片、实际题型和实际难度
     */
    R<CaptchaResponse> generateGraphic(
            @NotNull(groups = OptionalEnumSelection.class) GraphicCaptchaType type,
            @NotNull(groups = OptionalEnumSelection.class) CaptchaDifficulty difficulty);

    /**
     * 生成供用户拖动并完成拼图的滑块验证码。
     *
     * @return 统一响应，包含验证码键、背景图、滑块图和有效期
     */
    R<CaptchaResponse> generateBlockPuzzle();

    /**
     * 生成要求用户按顺序点选指定文字的验证码。
     *
     * @return 统一响应，包含验证码键、图片、目标文字和有效期
     */
    R<CaptchaResponse> generateClickWord();

    /**
     * 生成用于采集和校验客户端操作轨迹的行为验证码。
     *
     * @return 统一响应，包含验证码键、行为挑战参数和有效期
     */
    R<CaptchaResponse> generateBehavior();

    /**
     * 根据客户端提交的行为轨迹校验挑战并计算风险评分。
     *
     * @param request 行为验证码校验请求
     * @return 统一响应，包含行为评分、风险等级和处置建议
     */
    R<BehaviorCaptchaVerifyResponse> verifyBehavior(@Valid CaptchaVerifyRequest request);

    /**
     * 校验算术、滑块、点选文字或者消息验证码的一次性答案。
     *
     * @param request 验证码校验请求
     * @return 统一响应，数据为是否校验通过
     */
    R<Boolean> verify(@Valid CaptchaVerifyRequest request);

    /**
     * 通过内部短信或邮件提供者发送业务验证码并保存校验答案。
     *
     * @param request 验证码发送请求
     * @return 统一响应，数据为后续校验使用的验证码键
     */
    @Inner
    R<String> send(@Valid CaptchaSendRequest request);
}
