package io.mango.identity.api.query;

import io.mango.identity.api.enums.IdentityUserTargetType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.Data;

import java.io.Serializable;

/**
 * 身份用户接收目标查询。
 */
@Data
@Schema(description = "身份用户接收目标查询")
public class IdentityUserTargetQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "目标类型不能为空")
    @Schema(description = "目标类型：USER、ORG、POST、ROLE")
    private IdentityUserTargetType targetType;

    @NotNull(message = "目标ID不能为空")
    @Schema(description = "目标ID")
    private Long targetId;

    @Schema(description = "成员状态：0-禁用，1-启用；空表示不限")
    @Min(value = 0, message = "成员状态只能为0或1")
    @Max(value = 1, message = "成员状态只能为0或1")
    private Integer status;
}
