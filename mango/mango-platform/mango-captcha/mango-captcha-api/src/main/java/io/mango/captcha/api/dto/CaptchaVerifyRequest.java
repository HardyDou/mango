package io.mango.captcha.api.dto;

import io.mango.captcha.api.constant.CaptchaType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    /**
     * 显式要求验证码类型时使用的校验组；默认接口仍允许服务端推断类型。
     */
    public interface ExplicitType {
    }

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
    @NotNull(groups = ExplicitType.class, message = "验证码类型不能为空")
    private CaptchaType type;

    /**
     * 用户输入的验证码（算术/短信/邮件）
     */
    @Schema(description = "用户输入的验证码")
    @Size(max = 256, message = "验证码长度不能超过256")
    private String code;

    /**
     * 滑块验证参数（滑块验证码）
     */
    @Schema(description = "滑块验证参数")
    @Size(max = 16384, message = "验证参数长度不能超过16384")
    private String pointJson;
}
