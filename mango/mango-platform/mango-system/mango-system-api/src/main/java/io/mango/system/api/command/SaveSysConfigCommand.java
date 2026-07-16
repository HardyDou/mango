package io.mango.system.api.command;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Positive;
import io.mango.system.api.enums.ConfigOptionSourceEnum;
import io.mango.system.api.enums.ConfigTypeEnum;
import io.mango.system.api.enums.ConfigValueTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "保存系统配置命令")
public class SaveSysConfigCommand {
    @Positive(message = "主键 ID必须大于 0")
    @Schema(description = "主键 ID")
    private Long id;
    @NotBlank(message = "configKey不能为空")
    @Size(max = 100, message = "configKey长度不能超过100")
    @Schema(description = "配置键")
    private String configKey;
    @NotBlank(message = "configValue不能为空")
    @Schema(description = "配置值")
    private String configValue;
    @NotBlank(message = "configName不能为空")
    @Size(max = 100, message = "configName长度不能超过100")
    @Schema(description = "配置名称")
    private String configName;
    @NotNull(message = "type不能为空")
    @Schema(description = "配置类型")
    private ConfigTypeEnum type;
    @Size(max = 64, message = "domainCode长度不能超过64")
    @Schema(description = "业务域编码")
    private String domainCode;
    @Schema(description = "配置值类型")
    @NotNull(message = "配置值类型不能为空")
    private ConfigValueTypeEnum valueType;
    @Size(max = 64, message = "groupCode长度不能超过64")
    @Schema(description = "配置分组编码")
    private String groupCode;
    @Size(max = 100, message = "groupName长度不能超过100")
    @Schema(description = "配置分组名称")
    private String groupName;
    @Schema(description = "默认值")
    @Size(max = 65535, message = "默认值长度不正确")
    private String defaultValue;
    @Schema(description = "选项列表")
    @Size(max = 65535, message = "选项列表长度不正确")
    private String options;
    @Schema(description = "选项来源")
    @NotNull(message = "选项来源不能为空")
    private ConfigOptionSourceEnum optionSource;
    @Size(max = 50, message = "dictType长度不能超过50")
    @Schema(description = "字典类型编码")
    private String dictType;
    @Schema(description = "是否可编辑")
    @NotNull(message = "是否可编辑不能为空")
    private Boolean editable;
    @Size(max = 200, message = "editableReason长度不能超过200")
    @Schema(description = "不可编辑原因")
    private String editableReason;
    @Schema(description = "排序号")
    @PositiveOrZero(message = "排序号不能小于 0")
    private Integer sort;
    @Schema(description = "状态")
    @Max(value = Integer.MAX_VALUE, message = "状态不正确")
    private Integer status;
    @Schema(description = "备注")
    @Size(max = 500, message = "备注长度不正确")
    private String remark;
}
