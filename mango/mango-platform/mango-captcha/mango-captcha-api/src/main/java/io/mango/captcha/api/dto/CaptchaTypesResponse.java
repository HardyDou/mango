package io.mango.captcha.api.dto;

import io.mango.captcha.api.constant.CaptchaDifficulty;
import io.mango.captcha.api.constant.CaptchaType;
import io.mango.captcha.api.constant.GraphicCaptchaType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 验证码能力信息响应。
 */
@Data
@Schema(description = "验证码能力信息响应")
public class CaptchaTypesResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "当前支持的验证码类型")
    private List<CaptchaType> types;

    @Schema(description = "当前验证码存储实现")
    private String currentStorage;

    @Schema(description = "统一图形验证码默认题型")
    private GraphicCaptchaType defaultGraphicType;

    @Schema(description = "统一图形验证码默认难度")
    private CaptchaDifficulty defaultGraphicDifficulty;
}
