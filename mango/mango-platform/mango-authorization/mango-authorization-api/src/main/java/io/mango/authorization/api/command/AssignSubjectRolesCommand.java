package io.mango.authorization.api.command;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 给主体分配角色命令。
 */
@Data
public class AssignSubjectRolesCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "主体ID不能为空")
    private Long subjectId;
    private String appCode;
    private String realm;
    private String actorType;
    private String partyType;
    private Long partyId;
    private List<Long> roleIds;
}
