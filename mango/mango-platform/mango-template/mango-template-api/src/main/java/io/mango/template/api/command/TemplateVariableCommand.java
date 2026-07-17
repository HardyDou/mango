package io.mango.template.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 模板变量定义。
 */
@Data
@Schema(description = "模板变量定义")
public class TemplateVariableCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "变量名不能为空")
    @Size(max = 128, message = "变量名不能超过128个字符")
    @Schema(description = "变量名，支持 object.field")
    private String name;

    @Size(max = 128, message = "展示标签不能超过128个字符")
    @Schema(description = "展示标签")
    private String label;

    @NotBlank(message = "变量类型不能为空")
    @Size(max = 16, message = "变量类型不能超过16个字符")
    @Schema(description = "变量类型：STRING、NUMBER、BOOLEAN、OBJECT、ARRAY、DATE")
    private String type = "STRING";

    @NotNull(message = "是否必填不能为空")
    @Schema(description = "是否必填")
    private Boolean required = true;

    @Size(max = 1000, message = "示例值不能超过1000个字符")
    @Schema(description = "示例值")
    private String example;

    @Size(max = 1000, message = "变量说明不能超过1000个字符")
    @Schema(description = "变量说明")
    private String description;

    @Valid
    @Size(max = 200, message = "嵌套变量不能超过200项")
    @Schema(description = "嵌套变量定义")
    private List<TemplateVariableCommand> children = new ArrayList<>();

    public List<TemplateVariableCommand> getChildren() {
        if (children == null) {
            return null;
        }
        return new ArrayList<>(children);
    }

    public void setChildren(List<TemplateVariableCommand> children) {
        if (children == null) {
            this.children = null;
            return;
        }
        this.children = new ArrayList<>(children);
    }
}
