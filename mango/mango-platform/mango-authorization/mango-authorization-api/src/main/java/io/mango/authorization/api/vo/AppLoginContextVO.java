package io.mango.authorization.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 应用登录上下文 VO。
 */
@Data
@Schema(description = "应用登录上下文")
public class AppLoginContextVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "登录上下文ID")
    private Long contextId;
    @Schema(description = "应用ID")
    private Long appId;
    @Schema(description = "应用编码")
    private String appCode;
    @Schema(description = "登录域")
    private String realm;
    @Schema(description = "操作者类型")
    private String actorType;
    @Schema(description = "是否默认上下文")
    private Integer defaultFlag;
    @Schema(description = "状态：0-禁用，1-启用")
    private Integer status;
    @Schema(description = "排序号")
    private Integer sort;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
