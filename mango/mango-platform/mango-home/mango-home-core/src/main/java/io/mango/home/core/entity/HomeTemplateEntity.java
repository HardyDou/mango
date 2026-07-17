package io.mango.home.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("sys_home_template")
public class HomeTemplateEntity extends TenantEntity {

    private String name;

    private Boolean enabled;

    private Long activeVersionId;

    private Integer activeVersionNo;

    private Integer sort;

}
