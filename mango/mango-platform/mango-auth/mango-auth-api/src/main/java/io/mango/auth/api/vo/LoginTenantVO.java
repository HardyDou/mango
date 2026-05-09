package io.mango.auth.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 登录机构选项。
 */
@Data
@Schema(description = "登录机构选项")
public class LoginTenantVO {

    @Schema(description = "机构ID。底层对应 tenantId")
    private String tenantId;

    @Schema(description = "机构编码")
    private String tenantCode;

    @Schema(description = "机构名称")
    private String tenantName;

    @Schema(description = "当前账号在机构下的成员ID")
    private Long memberId;

    @Schema(description = "成员显示名称")
    private String memberName;

    @Schema(description = "成员类型")
    private String memberType;
}
