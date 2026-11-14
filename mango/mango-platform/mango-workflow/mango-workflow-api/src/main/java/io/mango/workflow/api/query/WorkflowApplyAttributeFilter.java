package io.mango.workflow.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 按业务申请可筛属性过滤的一个条件。
 *
 * <p>属性名和属性值都由业务模块自定义，平台只把它当作键值对存储和匹配。多个条件之间是「且」，
 * 同一条件的多个值之间是「或」。属性名必须由字母开头，只允许字母、数字、下划线、点和短横线。
 */
@Data
@Schema(description = "业务申请可筛属性过滤条件，属性名和值由业务自定义")
public class WorkflowApplyAttributeFilter {

    @Schema(description = "属性名，字母开头，只允许字母、数字、下划线、点和短横线，最长64", example = "guaranteeTypeCode")
    @NotBlank(message = "可筛属性名不能为空")
    @Size(max = 64, message = "可筛属性名长度不能超过64")
    @Pattern(regexp = "[A-Za-z][A-Za-z0-9_.-]{0,63}",
            message = "可筛属性名只支持字母开头的字母、数字、下划线、点和短横线")
    private String key;

    @Schema(description = "属性值集合，同一条件的多个值之间是或关系，单个值不超过255")
    @NotEmpty(message = "可筛属性值不能为空")
    @Size(max = 50, message = "单个可筛属性的值数量不能超过50")
    private List<@NotBlank(message = "可筛属性值不能为空")
                 @Size(max = 255, message = "可筛属性值长度不能超过255") String> values;
}
