package io.mango.captcha.api.dto;

import io.mango.captcha.api.constant.CaptchaType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 验证码校验请求
 *
 * @author Mango
 */
@Data
@Schema(description = "验证码校验请求")
public class CaptchaVerifyRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 验证码key
     */
    @Schema(description = "验证码键")
    @NotBlank(message = "验证码key不能为空")
    private String key;

    /**
     * 验证码类型（可空，由存储层推断）
     * 如果为null，服务端根据存储的验证码类型自动判断验证方式
     */
    @Schema(description = "验证码类型，可为空，由存储层推断")
    private CaptchaType type;

    /**
     * 用户输入的验证码（算术/短信/邮件）
     */
    @Schema(description = "用户输入的验证码")
    private String code;

    /**
     * 滑块验证参数（滑块验证码）
     */
    @Schema(description = "滑块验证参数")
    private String pointJson;
}
