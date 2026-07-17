package io.mango.home.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("sys_user_home_preference")
public class UserHomePreferenceEntity extends TenantEntity {

    private Long userId;

    private Long defaultHomePageId;

    private String defaultHomeRef;

}
