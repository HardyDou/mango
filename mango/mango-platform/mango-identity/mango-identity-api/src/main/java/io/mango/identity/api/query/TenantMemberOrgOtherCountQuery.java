package io.mango.identity.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
@Schema(description = "成员其它组织关系数量查询")
public class TenantMemberOrgOtherCountQuery {

    @NotNull(message = "租户ID不能为空")
    @Schema(description = "租户ID")
    private Long tenantId;

    @NotNull(message = "成员ID不能为空")
    @Schema(description = "成员ID")
    private Long memberId;

    @Positive(message = "排除关系ID必须大于0")
    @Schema(description = "需要排除的关系ID")
    private Long excludedRelationId;
}
