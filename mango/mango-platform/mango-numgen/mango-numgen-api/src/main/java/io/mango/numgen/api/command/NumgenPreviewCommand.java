package io.mango.numgen.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Data
@Schema(description = "编号预览命令")
public class NumgenPreviewCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "编号规则键不能为空")
    @Schema(description = "编号规则键")
    private String genKey;

    @Min(value = 1, message = "预览数量必须大于0")
    @Max(value = 20, message = "预览数量不能超过20")
    @Schema(description = "预览数量")
    private int count = 1;

    @Schema(description = "动态参数")
    private Map<String, Object> params = new HashMap<>();
}
