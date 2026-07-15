package io.mango.authorization.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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

    @Schema(description = "主体ID，当前为机构成员ID")
    @NotNull(message = "主体ID不能为空")
    @Positive
    private Long subjectId;
    @Schema(description = "应用编码")
    @Size(max = 64)
    private String appCode;
    @Schema(description = "登录域")
    @Size(max = 32)
    private String realm;
    @Schema(description = "操作者类型")
    @Size(max = 32)
    private String actorType;
    @Schema(description = "归属主体类型")
    @Size(max = 32)
    private String partyType;
    @Schema(description = "归属主体ID")
    @Positive
    private Long partyId;
    @Schema(description = "角色ID列表")
    @NotNull
    @Size(max = 1000)
    private List<@Positive Long> roleIds;
}
