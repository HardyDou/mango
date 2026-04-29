package io.mango.authorization.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色 VO。
 */
@Data
@Schema(description = "角色视图对象")
public class RoleVO {

    @Schema(description = "角色ID")
    private Long roleId;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "应用编码")
    private String appCode;

    @Schema(description = "登录域")
    private String realm;

    @Schema(description = "操作者类型")
    private String actorType;

    @Schema(description = "角色编码")
    private String roleCode;

    @Schema(description = "角色名称")
    private String roleName;

    @Schema(description = "角色类型: 1-系统, 2-业务")
    private Integer roleType;

    @Schema(description = "状态: 0-禁用, 1-启用")
    private Integer status;

    @Schema(description = "排序号")
    private Integer sort;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "菜单ID列表")
    private List<Long> menuIds;

    @Schema(description = "权限ID列表")
    private List<Long> permissionIds;
}
