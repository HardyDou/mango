package io.mango.home.api.vo;

import io.mango.home.api.enums.HomeTemplateAuthorizationSubjectType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "首页模板授权项")
public class HomeTemplateAuthorizationItem implements Serializable {

    @NotNull(message = "授权对象类型不能为空")
    @Schema(description = "授权对象类型：USER、ORG、ROLE")
    private HomeTemplateAuthorizationSubjectType subjectType;

    @Schema(description = "授权对象ID。用户和部门使用ID")
    private Long subjectId;

    @Schema(description = "授权对象编码。角色使用角色编码")
    private String subjectCode;

    @Schema(description = "授权对象名称")
    private String subjectName;

    @Schema(description = "是否默认首页候选")
    private Boolean defaultFlag;

    @Schema(description = "排序")
    private Integer sort;
}
