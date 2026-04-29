package io.mango.authorization.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 公共路径视图对象。
 *
 * @author Mango
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "公共路径视图对象")
public class PublicPathVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "公共路径ID")
    private Long id;

    /**
     * 路径模式。
     */
    @Schema(description = "路径模式")
    private String path;

    /**
     * 路径类型：1-匿名，2-登录，3-权限，4-内部。
     */
    @Schema(description = "路径类型: 1-匿名, 2-登录, 3-权限, 4-内部")
    private Integer pathType;

    /**
     * 路径类型名称。
     */
    @Schema(description = "路径类型名称")
    private String pathTypeName;

    /**
     * 描述。
     */
    @Schema(description = "描述")
    private String description;

    /**
     * 优先级。
     */
    @Schema(description = "优先级")
    private Integer priority;

    /**
     * 状态：0-禁用，1-启用。
     */
    @Schema(description = "状态: 0-禁用, 1-启用")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
