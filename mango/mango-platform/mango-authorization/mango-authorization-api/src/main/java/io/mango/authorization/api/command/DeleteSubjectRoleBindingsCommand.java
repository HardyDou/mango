package io.mango.authorization.api.command;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 删除主体角色绑定命令。
 */
@Data
public class DeleteSubjectRoleBindingsCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 租户 ID。 */
    private Long tenantId;

    /** 主体类型。 */
    private String subjectType;

    /** 主体 ID 列表。 */
    private List<Long> subjectIds;
}
