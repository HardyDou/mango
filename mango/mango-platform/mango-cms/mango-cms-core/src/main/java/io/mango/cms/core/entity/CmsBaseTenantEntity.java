package io.mango.cms.core.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class CmsBaseTenantEntity extends TenantEntity {

    @TableLogic(value = "0", delval = "1")
    private Integer deleted;
}
