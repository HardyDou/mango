package io.mango.home.api.vo;

import io.mango.home.api.enums.HomeTemplateAuthorizationSubjectType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "首页模板授权视图")
public class HomeTemplateAuthorizationVO implements Serializable {

    @Schema(description = "授权记录ID")
    private Long id;

    @Schema(description = "模板ID")
    private Long templateId;

    @Schema(description = "授权对象类型")
    private HomeTemplateAuthorizationSubjectType subjectType;

    @Schema(description = "授权对象ID")
    private Long subjectId;

    @Schema(description = "授权对象编码")
    private String subjectCode;

    @Schema(description = "授权对象名称")
    private String subjectName;

    @Schema(description = "是否默认首页候选")
    private Boolean defaultFlag;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
