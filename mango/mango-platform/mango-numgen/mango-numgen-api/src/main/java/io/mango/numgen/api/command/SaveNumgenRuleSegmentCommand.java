package io.mango.numgen.api.command;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "保存编号规则片段命令")
public class SaveNumgenRuleSegmentCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "片段 ID。新增时为空，修改时必填")
    private Long id;

    @NotNull(message = "规则 ID 不能为空")
    @Schema(description = "规则 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long ruleId;

    @NotNull(message = "排序不能为空")
    @Min(value = 1, message = "排序必须大于0")
    @Schema(description = "排序", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer sortOrder;

    @NotBlank(message = "片段类型不能为空")
    @Size(max = 32, message = "片段类型不能超过32个字符")
    @Schema(description = "片段类型：TEXT/DATE/PARAM/SEQ/EXPR", requiredMode = Schema.RequiredMode.REQUIRED)
    private String segmentType;

    @NotBlank(message = "片段名称不能为空")
    @Size(max = 128, message = "片段名称不能超过128个字符")
    @Schema(description = "片段名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String segmentName;

    @Size(max = 128, message = "字符串不能超过128个字符")
    @Schema(description = "字符串内容，支持 ${参数ID} 占位符")
    private String literalValue;

    @Size(max = 128, message = "变量键不能超过128个字符")
    @Schema(description = "变量键")
    private String variableKey;

    @Size(max = 64, message = "日期格式不能超过64个字符")
    @Schema(description = "日期格式")
    private String dateFormat;

    @Min(value = 1, message = "流水位数必须大于0")
    @Max(value = 20, message = "流水位数不能超过20")
    @Schema(description = "流水位数")
    private Integer seqWidth;

    @Size(max = 1, message = "补齐字符只能是单个字符")
    @Schema(description = "补齐字符")
    private String padChar;
}
