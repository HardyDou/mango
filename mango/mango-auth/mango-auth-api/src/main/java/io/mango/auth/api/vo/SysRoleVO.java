package io.mango.auth.api.vo;

import io.mango.common.vo.BaseVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * System role VO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysRoleVO extends BaseVO {
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
