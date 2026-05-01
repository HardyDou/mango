package io.mango.authorization.api.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色 VO。
 */
@Data
public class RoleVO {
    private Long roleId;
    private Long tenantId;
    private String appCode;
    private String realm;
    private String actorType;
    private String roleCode;
    private String roleName;
    private Integer roleType;
    private Integer status;
    private Integer sort;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<Long> menuIds;
    private List<Long> permissionIds;
}
