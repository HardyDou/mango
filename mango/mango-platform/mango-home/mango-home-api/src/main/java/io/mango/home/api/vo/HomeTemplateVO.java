package io.mango.home.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "首页模板视图")
public class HomeTemplateVO implements Serializable {

    @Schema(description = "模板ID")
    private Long id;

    @Schema(description = "租户标识")
    private String tenantId;

    @Schema(description = "模板名称")
    private String name;

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "当前发布版本ID")
    private Long activeVersionId;

    @Schema(description = "当前发布版本号")
    private Integer activeVersionNo;

    @Schema(description = "当前发布布局JSON")
    private String activeLayoutJson;

    @Schema(description = "草稿版本ID")
    private Long draftVersionId;

    @Schema(description = "草稿布局JSON")
    private String draftLayoutJson;

    @Schema(description = "有效授权数量")
    private Integer authorizationCount;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
