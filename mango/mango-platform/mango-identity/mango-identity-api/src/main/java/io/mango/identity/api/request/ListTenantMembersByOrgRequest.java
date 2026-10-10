package io.mango.identity.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.io.Serializable;

/**
 * 按组织范围查询启用租户成员请求。
 */
@Data
@Schema(description = "按组织范围查询启用租户成员请求")
public class ListTenantMembersByOrgRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull
    @Positive
    @Schema(description = "租户ID")
    private Long tenantId;

    @NotNull
    @Positive
    @Schema(description = "组织ID")
    private Long orgId;

    @NotNull
    @Schema(description = "是否包含下级组织")
    private Boolean includeDescendants;
}
