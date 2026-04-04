package io.mango.permission.core.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Public path configuration entity
 * Controls which paths are publicly accessible, require login, or need permissions
 *
 * @author Mango
 */
@Data
@TableName("sys_public_path")
@EqualsAndHashCode(callSuper = false)
public class SysPublicPath implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Path type: anonymous access
     */
    public static final int TYPE_ANONYMOUS = 1;

    /**
     * Path type: login required
     */
    public static final int TYPE_LOGIN = 2;

    /**
     * Path type: permission required
     */
    public static final int TYPE_PERMISSION = 3;

    /**
     * Path type: internal only (not accessible from external)
     */
    public static final int TYPE_INTERNAL = 4;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * Path pattern (supports wildcards like /public/**)
     */
    private String path;

    /**
     * Path type: 1=anonymous, 2=login, 3=permission, 4=internal
     */
    private Integer pathType;

    /**
     * Description
     */
    private String description;

    /**
     * Priority (higher number = higher priority)
     */
    private Integer priority;

    /**
     * Status: 0=disabled, 1=enabled
     */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private String creator;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updater;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
