package io.mango.system.core.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.mango.system.api.enums.ConfigOptionSourceEnum;
import io.mango.system.api.enums.ConfigTypeEnum;
import io.mango.system.api.enums.ConfigValueTypeEnum;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_config")
public class SysConfig {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String configKey;
    private String configValue;
    private String configName;
    private ConfigTypeEnum type;
    private String domainCode;
    private ConfigValueTypeEnum valueType;
    private String groupCode;
    private String groupName;
    private String defaultValue;
    private String options;
    private ConfigOptionSourceEnum optionSource;
    private String dictType;
    private Boolean editable;
    private String editableReason;
    private Integer sort;
    private Integer status;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private String createBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
