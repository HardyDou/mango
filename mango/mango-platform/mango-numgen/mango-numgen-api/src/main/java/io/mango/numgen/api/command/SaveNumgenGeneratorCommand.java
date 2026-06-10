package io.mango.numgen.api.command;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "保存编号生成器命令")
public class SaveNumgenGeneratorCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "编号生成器 ID。新增时为空，修改时必填")
    private Long id;

    @NotBlank(message = "业务 Key 不能为空")
    @Size(max = 128, message = "业务 Key 不能超过128个字符")
    @Schema(description = "业务 Key", requiredMode = Schema.RequiredMode.REQUIRED)
    private String genKey;

    @NotBlank(message = "名称不能为空")
    @Size(max = 128, message = "名称不能超过128个字符")
    @Schema(description = "名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String genName;

    @Size(max = 64, message = "业务域编码不能超过64个字符")
    @Schema(description = "业务域编码。不传默认 GENERAL")
    private String domainCode;

    @NotNull(message = "状态不能为空")
    @Schema(description = "状态：1-启用，0-停用", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;
}
