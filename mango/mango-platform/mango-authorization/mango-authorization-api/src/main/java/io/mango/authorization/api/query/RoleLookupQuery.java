package io.mango.authorization.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 角色业务条件查询。
 */
@Data
@Schema(description = "角色业务条件查询")
public class RoleLookupQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull
    @Positive
    @Schema(description = "租户ID")
    private Long tenantId;

    @NotBlank
    @Size(max = 64)
    @Schema(description = "应用编码")
    private String appCode;

    @NotBlank
    @Size(max = 32)
    @Schema(description = "登录域")
    private String realm;

    @Size(max = 32)
    @Schema(description = "操作者类型")
    private String actorType;

    @NotBlank
    @Size(max = 50)
    @Schema(description = "角色编码")
    private String roleCode;
}
