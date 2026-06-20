package io.mango.resource.api.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.mango.resource.api.enums.ResourceStatus;
import io.mango.resource.api.enums.ResourceSyncMode;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 单个资源声明。
 */
@Data
public class ResourceDeclaration {

    private String id;
    private Integer version;
    @JsonAlias("app-code")
    private String appCode;
    @JsonAlias("service-code")
    private String serviceCode;
    @JsonAlias("resource-type")
    private String resourceType;
    @JsonAlias("module-code")
    private String moduleCode;
    @JsonAlias("module-name")
    private String moduleName;
    @JsonAlias("biz-key")
    private String bizKey;
    private String name;
    @JsonAlias("target-module")
    private String targetModule;
    @JsonAlias("sync-mode")
    private ResourceSyncMode syncMode = ResourceSyncMode.AUTO;
    private ResourceStatus status = ResourceStatus.ACTIVE;
    private Map<String, ResourceField> fields = new LinkedHashMap<>();
    private String source;
}
