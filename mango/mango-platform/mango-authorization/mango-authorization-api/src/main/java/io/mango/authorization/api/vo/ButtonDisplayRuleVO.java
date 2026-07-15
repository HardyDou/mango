package io.mango.authorization.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * Button display rule granted to the current subject.
 */
@Data
@Schema(description = "按钮显示规则")
public class ButtonDisplayRuleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "按钮权限编码")
    private String code;
    @Schema(description = "按钮类型")
    private String buttonType;
    @Schema(description = "按钮显示规则表达式")
    private String displayRule;
}
