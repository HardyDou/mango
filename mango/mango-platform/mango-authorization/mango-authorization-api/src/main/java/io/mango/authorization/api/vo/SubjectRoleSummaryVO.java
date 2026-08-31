package io.mango.authorization.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 成员直接角色摘要。
 */
@Data
@Schema(description = "成员直接角色摘要")
public class SubjectRoleSummaryVO {

    @Schema(description = "成员ID")
    private Long subjectId;

    @Schema(description = "成员直接分配的角色列表")
    private List<RoleVO> roles;

    public List<RoleVO> getRoles() {
        return roles == null ? null : List.copyOf(roles);
    }

    public void setRoles(List<RoleVO> roles) {
        this.roles = roles == null ? null : List.copyOf(roles);
    }
}
