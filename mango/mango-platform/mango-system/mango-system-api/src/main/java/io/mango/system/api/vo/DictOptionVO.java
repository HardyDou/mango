package io.mango.system.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "字典选项")
public class DictOptionVO {
    @Schema(description = "选项标签")
    private String label;
    @Schema(description = "选项值")
    private String value;
    @Schema(description = "分组标记，空列表表示未分组")
    private List<String> groups;
}
