package io.mango.resource.core.sync;

import io.mango.resource.api.enums.ResourceSyncMode;
import lombok.Data;

/**
 * resource_registry 行数据。
 */
@Data
class ResourceRegistryRow {

    private Long id;
    private String resourceId;
    private Integer resourceVersion;
    private String resourceType;
    private String moduleCode;
    private String bizKey;
    private String name;
    private String targetModule;
    private String targetTable;
    private Long targetId;
    private String sourceHash;
    private ResourceSyncMode syncMode;
    private String status;
}
