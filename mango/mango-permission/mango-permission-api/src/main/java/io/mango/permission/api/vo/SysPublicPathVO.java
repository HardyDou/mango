package io.mango.permission.api.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Public path VO
 *
 * @author Mango
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class SysPublicPathVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    /**
     * Path pattern
     */
    private String path;

    /**
     * Path type: 1=anonymous, 2=login, 3=permission
     */
    private Integer pathType;

    /**
     * Type name (for display)
     */
    private String pathTypeName;

    /**
     * Description
     */
    private String description;

    /**
     * Priority
     */
    private Integer priority;

    /**
     * Status: 0=disabled, 1=enabled
     */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
