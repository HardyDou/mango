package io.mango.identity.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "成员组织关系存在性查询")
public class TenantMemberOrgExistsQuery {

    @NotNull(message = "租户ID不能为空")
    @Schema(description = "租户ID")
    private Long tenantId;

    @NotNull(message = "成员ID不能为空")
    @Schema(description = "成员ID")
    private Long memberId;

    @NotNull(message = "组织ID不能为空")
    @Schema(description = "组织ID")
    private Long orgId;
}
