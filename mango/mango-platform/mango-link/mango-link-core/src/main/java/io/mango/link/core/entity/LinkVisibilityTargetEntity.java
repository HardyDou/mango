package io.mango.link.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("link_visibility_target")
public class LinkVisibilityTargetEntity extends TenantEntity {

    private Long linkId;

    private String targetType;

    private Long targetId;

    private String targetName;

}
