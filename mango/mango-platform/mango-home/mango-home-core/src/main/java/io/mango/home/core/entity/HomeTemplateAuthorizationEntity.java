package io.mango.home.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("sys_home_template_authorization")
public class HomeTemplateAuthorizationEntity extends TenantEntity {

    private Long templateId;

    private String subjectType;

    private Long subjectId;

    private String subjectCode;

    private String subjectName;

    private Boolean defaultFlag;

    private Integer sort;

    private Boolean enabled;

}
