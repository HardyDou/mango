package io.mango.resource.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.AuditableEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 资源注册记录。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("resource_registry")
public class ResourceRegistryEntity extends AuditableEntity {

    private String resourceId;
    private Integer resourceVersion;
    private String appCode;
    private String serviceCode;
    private String resourceType;
    private String moduleCode;
    private String bizKey;
    private String name;
    private String targetModule;
    private String targetTable;
    private Long targetId;
    private String sourceHash;
    private String syncMode;
    private String status;
    private LocalDateTime lastSyncTime;
}
