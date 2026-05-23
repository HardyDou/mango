package io.mango.numgen.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "校验编号规则命令")
public class NumgenValidateRuleCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "规则键不能为空")
    @Schema(description = "规则键")
    private String genKey;

    @NotBlank(message = "规则名称不能为空")
    @Schema(description = "规则名称")
    private String ruleName;

    @Schema(description = "片段列表")
    private List<SaveNumgenRuleSegmentCommand> segments = new ArrayList<>();
}
