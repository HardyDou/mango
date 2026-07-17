package io.mango.home.api.command;

import io.mango.home.api.enums.HomeTemplateAuthorizationSubjectType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/** 首页模板授权项命令。 */
@Data
@Schema(description = "首页模板授权项命令")
public class HomeTemplateAuthorizationCommand implements Serializable {

    @NotNull(message = "授权对象类型不能为空")
    @Schema(description = "授权对象类型：USER、ORG、ROLE")
    private HomeTemplateAuthorizationSubjectType subjectType;

    @Min(value = 1, message = "授权对象ID必须大于0")
    @Schema(description = "授权对象ID。用户和部门使用ID")
    private Long subjectId;

    @Size(max = 128, message = "授权对象编码长度不能超过128")
    @Schema(description = "授权对象编码。角色使用角色编码")
    private String subjectCode;

    @Size(max = 128, message = "授权对象名称长度不能超过128")
    @Schema(description = "授权对象名称")
    private String subjectName;

    @NotNull(message = "是否默认首页候选不能为空")
    @Schema(description = "是否默认首页候选")
    private Boolean defaultFlag = false;

    @Min(value = 0, message = "排序不能小于0")
    @Max(value = 1000000, message = "排序不能超过1000000")
    @Schema(description = "排序")
    private Integer sort;

    /** 显式 null 与历史逻辑一致，按 false 处理。 */
    public void setDefaultFlag(Boolean defaultFlag) {
        this.defaultFlag = Boolean.TRUE.equals(defaultFlag);
    }
}
