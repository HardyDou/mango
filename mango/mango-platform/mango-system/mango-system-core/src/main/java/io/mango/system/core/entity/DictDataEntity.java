package io.mango.system.core.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("sys_dict_data")
public class DictDataEntity extends TenantEntity {
    private String dictType;
    private String dictLabel;
    private String dictValue;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String parentValue;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String dictGroups;
    private Integer sort;
    private Integer status;
    private String remark;
}
