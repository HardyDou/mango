package io.mango.link.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("link_category")
public class LinkCategoryEntity extends TenantEntity {

    private String scope;

    private Long ownerUserId;

    private String name;

    private Integer sortNo;

    private String status;

    private String remark;

}
