package io.mango.captcha.api.dto;

import io.mango.captcha.api.constant.CaptchaType;
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
}
