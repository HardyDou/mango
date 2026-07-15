package io.mango.authorization.api.vo;

import io.mango.authorization.api.enums.DataScopeMode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 角色数据权限配置。
 */
@Data
@Schema(description = "角色数据权限配置")
public class RoleDataScopeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "数据权限配置ID")
    private Long id;
    @Schema(description = "租户ID")
    private Long tenantId;
    @Schema(description = "应用编码")
    private String appCode;
    @Schema(description = "角色ID")
    private Long roleId;
    @Schema(description = "资源编码")
    private String resourceCode;
    @Schema(description = "数据范围模式")
    private DataScopeMode scopeMode;
    @Schema(description = "数据范围值集合")
    private List<String> scopeValues = new ArrayList<>();
    @Schema(description = "是否包含下级范围")
    private Boolean includeChildren;
    @Schema(description = "状态：0-禁用，1-启用")
    private Integer status;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
