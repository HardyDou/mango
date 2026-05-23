package io.mango.numgen.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Data
@Schema(description = "生成单个编号命令")
public class NumgenNextCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "编号规则键不能为空")
    @Schema(description = "编号规则键")
    private String genKey;

    @Schema(description = "动态参数")
    private Map<String, Object> params = new HashMap<>();
}
