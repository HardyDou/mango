package io.mango.authorization.api.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * System role VO
 *
 * @author Mango
 */
@Data
public class SysRoleVO {

    private Long roleId;

    private Long tenantId;

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
