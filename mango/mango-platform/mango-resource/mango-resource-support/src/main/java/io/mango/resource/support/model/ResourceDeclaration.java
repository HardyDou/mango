package io.mango.resource.support.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.mango.resource.api.enums.ResourceStatus;
import io.mango.resource.api.enums.ResourceSyncMode;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 单个资源声明。
 */
@Data
@EqualsAndHashCode(doNotUseGetters = true)
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

    public Map<String, ResourceField> getFields() {
        return new LinkedHashMap<>(fields);
    }

    public void setFields(Map<String, ResourceField> fields) {
        if (fields == null) {
            this.fields = new LinkedHashMap<>();
            return;
        }
        this.fields = new LinkedHashMap<>(fields);
    }

    public void putField(String name, ResourceField field) {
        fields.put(name, field);
    }

    public void removeField(String name) {
        fields.remove(name);
    }

    public void clearFields() {
        fields.clear();
    }

    public ResourceDeclaration copy() {
        ResourceDeclaration copy = new ResourceDeclaration();
        copy.setId(id);
        copy.setVersion(version);
        copy.setAppCode(appCode);
        copy.setServiceCode(serviceCode);
        copy.setResourceType(resourceType);
        copy.setModuleCode(moduleCode);
        copy.setModuleName(moduleName);
        copy.setBizKey(bizKey);
        copy.setName(name);
        copy.setTargetModule(targetModule);
        copy.setSyncMode(syncMode);
        copy.setStatus(status);
        copy.setFields(fields);
        copy.setSource(source);
        return copy;
    }
}
