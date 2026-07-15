package io.mango.authorization.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 删除主体角色绑定命令。
 */
@Data
@Schema(description = "删除主体角色绑定命令")
public class DeleteSubjectRoleBindingsCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull
    @Positive
    @Schema(description = "租户ID")
    private Long tenantId;

    @NotBlank
    @Size(max = 32)
    @Schema(description = "主体类型")
    private String subjectType;

    @NotEmpty
    @Schema(description = "主体ID列表")
    private List<@Positive Long> subjectIds;
}
