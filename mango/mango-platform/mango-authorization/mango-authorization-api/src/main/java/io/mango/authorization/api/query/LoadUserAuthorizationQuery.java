package io.mango.authorization.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户授权快照查询条件。
 */
@Data
@Schema(description = "用户授权快照查询条件")
public class LoadUserAuthorizationQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主体ID，当前为机构成员ID")
    @NotNull(message = "主体ID不能为空")
    private Long subjectId;

    @Schema(description = "机构ID")
    private String tenantId;

    @Schema(description = "系统或应用编码")
    private String systemCode;

    @Schema(description = "登录域")
    private String realm;

    @Schema(description = "操作者类型")
    private String actorType;

    @Schema(description = "归属主体类型")
    private String partyType;

    @Schema(description = "归属主体ID")
    private Long partyId;
}
