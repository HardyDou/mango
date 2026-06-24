package io.mango.system.api.vo;

import io.mango.system.api.enums.ConfigTypeEnum;
import io.mango.system.api.enums.ConfigOptionSourceEnum;
import io.mango.system.api.enums.ConfigValueTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "系统配置视图")
public class SysConfigVO {
    @Schema(description = "配置ID")
    private Long id;
    @Schema(description = "配置键")
    private String configKey;
    @Schema(description = "配置值")
    private String configValue;
    @Schema(description = "配置名称")
    private String configName;
    @Schema(description = "配置类型")
    private ConfigTypeEnum type;
    @Schema(description = "业务域编码")
    private String domainCode;
    @Schema(description = "配置值展示与编辑类型")
    private ConfigValueTypeEnum valueType;
    @Schema(description = "配置分组编码")
    private String groupCode;
    @Schema(description = "配置分组名称")
    private String groupName;
    @Schema(description = "默认值")
    private String defaultValue;
    @Schema(description = "选项列表，JSON字符串")
    private String options;
    @Schema(description = "选项来源：CUSTOM-自定义，DICT-字典")
    private ConfigOptionSourceEnum optionSource;
    @Schema(description = "绑定字典类型")
    private String dictType;
    @Schema(description = "是否可编辑")
    private Boolean editable;
    @Schema(description = "不可编辑原因")
    private String editableReason;
    @Schema(description = "排序号")
    private Integer sort;
    @Schema(description = "状态：0-禁用，1-启用")
    private Integer status;
}
