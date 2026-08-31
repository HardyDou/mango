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

    private Long subjectId;

    private List<RoleVO> roles;
}
