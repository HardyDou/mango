package io.mango.auth.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
public class CompleteProviderAuthorizationCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank
    @Size(max = 256)
    @Schema(description = "第三方授权状态凭据")
    private String state;

    @NotBlank
    @Size(max = 1024)
    @Schema(description = "第三方平台返回的授权码")
    private String code;
}
