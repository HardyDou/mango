package io.mango.home.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("sys_home_template_version")
public class HomeTemplateVersionEntity extends TenantEntity {

    private Long templateId;

    private Integer versionNo;

    private String status;

    private String layoutJson;

    private Long sourceVersionId;

    private Long publishedBy;

    private LocalDateTime publishedAt;

}
