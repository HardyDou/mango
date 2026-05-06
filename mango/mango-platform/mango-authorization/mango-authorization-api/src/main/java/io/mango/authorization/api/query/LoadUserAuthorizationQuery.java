package io.mango.authorization.api.query;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户授权快照查询条件。
 */
@Data
public class LoadUserAuthorizationQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "主体ID不能为空")
    private Long subjectId;

    private String tenantId;

    private String systemCode;

    private String realm;

    private String actorType;

    private String partyType;

    private Long partyId;
}
