package io.mango.gridlayout.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mango_user_grid_layout")
public class MangoUserGridLayoutEntity extends TenantEntity {

    private Long userId;

    private String pageCode;

    private Integer schemaVersion;

    private String layoutJson;
}
