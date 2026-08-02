package io.mango.identity.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
public class SendContactCaptchaCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank
    @Pattern(regexp = "PHONE|EMAIL")
    @Schema(description = "联系方式类型，可选 PHONE 或 EMAIL")
    private String contactType;

    @NotBlank
    @Size(max = 100)
    @Schema(description = "接收验证码的新手机号或邮箱")
    private String target;
}
