package io.mango.link.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("link_item")
public class LinkItemEntity extends TenantEntity {

    private Long categoryId;

    private String name;

    private String url;

    private String summary;

    private String iconUrl;

    private String tags;

    private String visibilityScope;

    private Long ownerUserId;

    private String openMode;

    private Boolean recommended;

    private Integer sortNo;

    private String status;

    private String remark;

}
