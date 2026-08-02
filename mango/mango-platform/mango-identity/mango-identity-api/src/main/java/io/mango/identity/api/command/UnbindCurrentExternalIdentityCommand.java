package io.mango.identity.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
public class UnbindCurrentExternalIdentityCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull
    @Positive
    @Schema(description = "需要解绑的第三方身份绑定 ID")
    private Long bindingId;

    @NotBlank
    @Size(max = 200)
    @Schema(description = "当前 Mango 账号密码")
    private String currentPassword;
}
