package io.mango.home.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("sys_user_home_page")
public class UserHomePageEntity extends TenantEntity {

    private Long userId;

    private String name;

    private String layoutJson;

    private Integer sort;

    private Boolean enabled;

}
