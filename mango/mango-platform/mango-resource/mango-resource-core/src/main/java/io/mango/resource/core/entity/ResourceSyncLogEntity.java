package io.mango.resource.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 资源同步日志。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("resource_sync_log")
public class ResourceSyncLogEntity extends TenantEntity {

    private Long resourceId;
    private String syncType;
    private String result;
    private String message;
}
