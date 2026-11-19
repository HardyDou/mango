package io.mango.system.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "批量字典选项查询条件")
public class DictOptionsBatchQuery {

    @Schema(description = "字典类型编码，最多 24 个，单个不超过 50 字符", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "order_status,after_sale")
    @NotEmpty(message = "字典类型编码不能为空")
    @Size(max = 24, message = "一次最多批量读取 24 个字典类型")
    private List<@NotBlank(message = "字典类型编码不能为空") @Size(max = 50, message = "字典类型编码长度不能超过 50 个字符")
            String> typeCodes;
}
