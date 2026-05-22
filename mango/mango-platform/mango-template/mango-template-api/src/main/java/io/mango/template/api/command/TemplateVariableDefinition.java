package io.mango.template.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 模板变量定义。
 */
@Data
@Schema(description = "模板变量定义")
public class TemplateVariableDefinition implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "变量名，支持 object.field")
    private String name;

    @Schema(description = "展示标签")
    private String label;

    @Schema(description = "变量类型：STRING、NUMBER、BOOLEAN、OBJECT、ARRAY、DATE")
    private String type = "STRING";

    @Schema(description = "是否必填")
    private Boolean required = true;

    @Schema(description = "示例值")
    private String example;

    @Schema(description = "变量说明")
    private String description;

    @Schema(description = "嵌套变量定义")
    private List<TemplateVariableDefinition> children = new ArrayList<>();
}
