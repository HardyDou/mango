package io.mango.template.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 模板变量视图。
 */
@Data
@Schema(description = "模板变量视图")
public class TemplateVariableVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "变量名")
    private String name;
    @Schema(description = "展示标签")
    private String label;
    @Schema(description = "变量类型")
    private String type;
    @Schema(description = "是否必填")
    private Boolean required;
    @Schema(description = "示例值")
    private String example;
    @Schema(description = "变量说明")
    private String description;
    @Schema(description = "嵌套变量")
    private List<TemplateVariableVO> children = new ArrayList<>();

    public List<TemplateVariableVO> getChildren() {
        if (children == null) {
            return null;
        }
        return new ArrayList<>(children);
    }

    public void setChildren(List<TemplateVariableVO> children) {
        if (children == null) {
            this.children = null;
            return;
        }
        this.children = new ArrayList<>(children);
    }
}
