package io.mango.authorization.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 给主体分配角色命令。
 */
@Data
@Schema(description = "给主体分配角色命令")
public class AssignSubjectRolesCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主体ID，通常为用户ID")
    @NotNull(message = "主体ID不能为空")
    private Long subjectId;
    @Schema(description = "应用编码")
    private String appCode;
    @Schema(description = "登录域")
    private String realm;
    @Schema(description = "操作者类型")
    private String actorType;
    @Schema(description = "归属主体类型")
    private String partyType;
    @Schema(description = "归属主体ID")
    private Long partyId;
    @Schema(description = "角色ID列表")
    private List<Long> roleIds;
}
