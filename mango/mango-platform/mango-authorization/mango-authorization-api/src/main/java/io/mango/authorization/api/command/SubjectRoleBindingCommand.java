package io.mango.authorization.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 主体角色绑定命令。
 */
@Data
@Schema(description = "主体角色绑定命令")
public class SubjectRoleBindingCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull
    @Positive
    @Schema(description = "租户ID")
    private Long tenantId;

    @NotNull
    @Positive
    @Schema(description = "主体ID")
    private Long subjectId;

    @NotBlank
    @Size(max = 32)
    @Schema(description = "主体类型")
    private String subjectType;

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

    @Size(max = 32)
    @Schema(description = "归属主体类型")
    private String partyType;

    @Positive
    @Schema(description = "归属主体ID")
    private Long partyId;

    @NotNull
    @Positive
    @Schema(description = "角色ID")
    private Long roleId;
}
