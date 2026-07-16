package io.mango.system.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("sys_dict_type")
public class DictTypeEntity extends TenantEntity {
    private String dictType;
    private String dictName;
    private String domainCode;
    private Integer status;
    private String remark;
}
