package io.mango.authorization.api.vo;

import io.mango.authorization.api.enums.DataScopeMode;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 角色数据权限配置。
 */
@Data
public class RoleDataScopeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long tenantId;
    private String appCode;
    private Long roleId;
    private String resourceCode;
    private DataScopeMode scopeMode;
    private List<String> scopeValues = new ArrayList<>();
    private Boolean includeChildren;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
