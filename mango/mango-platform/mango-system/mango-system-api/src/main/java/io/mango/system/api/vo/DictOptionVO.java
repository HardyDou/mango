package io.mango.system.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "字典选项")
public class DictOptionVO {
    @Schema(description = "选项标签")
    private String label;
    @Schema(description = "选项值")
    private String value;
}
