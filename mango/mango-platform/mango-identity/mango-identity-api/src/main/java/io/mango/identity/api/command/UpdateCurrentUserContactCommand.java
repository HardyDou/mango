package io.mango.identity.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
public class UpdateCurrentUserContactCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank
    @Pattern(regexp = "PHONE|EMAIL")
    @Schema(description = "联系方式类型，可选 PHONE 或 EMAIL")
    private String contactType;

    @NotBlank
    @Size(max = 100)
    @Schema(description = "新的手机号或邮箱")
    private String target;

    @NotBlank
    @Size(max = 200)
    @Schema(description = "当前 Mango 账号密码")
    private String currentPassword;

    @NotBlank
    @Size(max = 256)
    @Schema(description = "新联系方式验证码凭据")
    private String captchaKey;

    @NotBlank
    @Size(max = 256)
    @Schema(description = "新联系方式收到的验证码")
    private String captchaCode;
}
