package io.mango.information.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/** 信息采集表单保存命令。 */
@Data
@Schema(description = "信息采集表单保存命令")
public class SaveInformationFormCommand implements Serializable {
    private Long id;

    @NotBlank(message = "表单编码不能为空")
    @Size(max = 64, message = "表单编码不能超过64个字符")
    private String formCode;

    @NotBlank(message = "表单名称不能为空")
    @Size(max = 128, message = "表单名称不能超过128个字符")
    private String formName;

    @Size(max = 1000, message = "表单说明不能超过1000个字符")
    private String description;

    @NotBlank(message = "字段定义不能为空")
    @Size(max = 1048576, message = "字段定义不能超过1MB")
    @Schema(description = "JSON 字段定义，数组元素包含 fieldCode、fieldName、fieldType、required、sort")
    private String definitionJson;

    @Size(max = 255, message = "成功提示不能超过255个字符")
    private String successMessage;

    private Boolean published;
}
