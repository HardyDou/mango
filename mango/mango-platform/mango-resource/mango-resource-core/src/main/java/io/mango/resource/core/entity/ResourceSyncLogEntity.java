package io.mango.resource.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 资源同步日志。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("resource_sync_log")
public class ResourceSyncLogEntity extends BaseEntity {

    private Long resourceId;
    private String syncType;
    private String result;
    private String message;
    private LocalDateTime createdAt;
}
