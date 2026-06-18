package io.mango.resource.starter.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ResourceRegistryVO {

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
    private String syncMode;
    private String status;
    private LocalDateTime lastSyncTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
