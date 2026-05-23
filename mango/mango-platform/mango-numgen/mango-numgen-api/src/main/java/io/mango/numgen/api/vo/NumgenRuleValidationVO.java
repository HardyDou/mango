package io.mango.numgen.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "编号规则校验结果")
public class NumgenRuleValidationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "是否有效")
    private boolean valid;

    @Schema(description = "错误信息")
    private List<String> errors = new ArrayList<>();
}
