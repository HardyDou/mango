package io.mango.link.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("link_favorite")
public class LinkFavoriteEntity extends TenantEntity {

    private Long userId;

    private Long linkId;

}
