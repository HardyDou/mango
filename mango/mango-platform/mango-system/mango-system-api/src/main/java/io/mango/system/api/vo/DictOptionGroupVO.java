package io.mango.system.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "单个字典类型的选项集合")
public class DictOptionGroupVO {

    @Schema(description = "字典类型编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String typeCode;

    @Schema(description = "该字典类型的平铺选项；字典类型不存在时为空数组")
    private List<DictOptionVO> options = new ArrayList<>();
}
