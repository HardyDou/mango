package io.mango.system.api.po;

import io.mango.system.api.enums.ConfigTypeEnum;
import io.mango.system.api.enums.ConfigOptionSourceEnum;
import io.mango.system.api.enums.ConfigValueTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "系统配置")
public class SysConfigPo {
    @Schema(description = "配置ID")
    private Long id;

    @Schema(description = "配置键")
    @NotBlank(message = "configKey不能为空")
    @Size(max = 100, message = "configKey长度不能超过100")
    private String configKey;

    @Schema(description = "配置值")
    @NotBlank(message = "configValue不能为空")
    private String configValue;

    @Schema(description = "配置名称")
    @NotBlank(message = "configName不能为空")
    @Size(max = 100, message = "configName长度不能超过100")
    private String configName;

    @Schema(description = "配置类型")
    @NotNull(message = "type不能为空")
    private ConfigTypeEnum type;

    @Schema(description = "业务域编码")
    @Size(max = 64, message = "domainCode长度不能超过64")
    private String domainCode;

    @Schema(description = "配置值展示与编辑类型")
    private ConfigValueTypeEnum valueType;

    @Schema(description = "配置分组编码")
    @Size(max = 64, message = "groupCode长度不能超过64")
    private String groupCode;

    @Schema(description = "配置分组名称")
    @Size(max = 100, message = "groupName长度不能超过100")
    private String groupName;

    @Schema(description = "默认值")
    private String defaultValue;

    @Schema(description = "选项列表，JSON字符串")
    private String options;

    @Schema(description = "选项来源：CUSTOM-自定义，DICT-字典")
    private ConfigOptionSourceEnum optionSource;

    @Schema(description = "绑定字典类型")
    @Size(max = 50, message = "dictType长度不能超过50")
    private String dictType;

    @Schema(description = "是否可编辑")
    private Boolean editable;

    @Schema(description = "不可编辑原因")
    @Size(max = 200, message = "editableReason长度不能超过200")
    private String editableReason;

    @Schema(description = "排序号")
    private Integer sort;
    @Schema(description = "状态：0-禁用，1-启用")
    private Integer status;
    @Schema(description = "备注")
    private String remark;
}
