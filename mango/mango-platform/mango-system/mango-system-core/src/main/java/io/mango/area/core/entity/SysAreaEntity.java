package io.mango.area.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("sys_area")
public class SysAreaEntity extends TenantEntity {
    private Long pid;
    private String name;
    private String letter;
    private Long adcode;
    private String location;
    private Integer areaSort;
    private String areaStatus;
    private String areaType;
    private String hot;
    private String cityCode;
}
