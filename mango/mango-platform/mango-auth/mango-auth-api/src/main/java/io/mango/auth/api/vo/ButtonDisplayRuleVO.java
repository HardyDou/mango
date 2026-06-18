package io.mango.auth.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * Button display rule returned with login user info.
 */
@Data
@Schema(description = "按钮展示规则")
public class ButtonDisplayRuleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "按钮权限标识，对应菜单编码")
    private String code;

    @Schema(description = "按钮类型：TABLE-表格按钮，NON_TABLE-非表格按钮")
    private String buttonType;

    @Schema(description = "按钮展示规则表达式")
    private String displayRule;
}
