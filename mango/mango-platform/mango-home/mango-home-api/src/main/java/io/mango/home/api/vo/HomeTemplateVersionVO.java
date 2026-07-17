package io.mango.home.api.vo;

import io.mango.home.api.enums.HomeTemplateVersionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "首页模板版本视图")
public class HomeTemplateVersionVO implements Serializable {

    @Schema(description = "版本ID")
    private Long id;

    @Schema(description = "模板ID")
    private Long templateId;

    @Schema(description = "版本号")
    private Integer versionNo;

    @Schema(description = "版本状态")
    private HomeTemplateVersionStatus status;

    @Schema(description = "布局JSON")
    private String layoutJson;

    @Schema(description = "来源版本ID")
    private Long sourceVersionId;

    @Schema(description = "发布人ID")
    private Long publishedBy;

    @Schema(description = "发布时间")
    private LocalDateTime publishedAt;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
