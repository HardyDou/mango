package io.mango.resource.core.sync;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.resource.api.ResourceHandler;
import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.api.enums.ResourceStatus;
import io.mango.resource.api.enums.ResourceSyncMode;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceField;
import io.mango.resource.api.model.ResourceSyncResult;
import io.mango.resource.support.config.ResourceRegistryProperties;
import io.mango.resource.support.declaration.ResourceDeclarationCollector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.util.StringUtils;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 资源注册同步服务。
 */
@Slf4j
@RequiredArgsConstructor
public class ResourceRegistrySyncService {

    private final ResourceRegistryProperties properties;
    private final ResourceDeclarationCollector collector;
    private final ObjectProvider<ResourceHandler> handlers;
    private final ResourceContentHasher hasher;
    private final ResourceRegistryRepository repository;
    private final ResourceRegistryLock lock;
    private final ObjectMapper objectMapper;

    public void sync() {
        sync(false);
    }

    public void sync(boolean force) {
        if (!properties.isEnabled()) {
            log.info("Mango resource registry sync disabled");
            return;
        }
        String owner = resolveOwner();
        if (!lock.tryLock(owner, properties.getLockTtlSeconds())) {
            log.info("Mango resource registry sync skipped: lock is held by another instance");
            return;
        }
        try {
            doSync(force);
        } finally {
            lock.unlock(owner);
        }
    }

    public void syncRemote(List<ResourceDeclaration> declarations) {
        if (!properties.isEnabled()) {
            log.info("Mango resource registry remote sync disabled");
            return;
        }
        String owner = resolveOwner();
        if (!lock.tryLock(owner, properties.getLockTtlSeconds())) {
            log.info("Mango resource registry remote sync skipped: lock is held by another instance");
            return;
        }
        try {
            doSync(declarations, false);
        } finally {
            lock.unlock(owner);
        }
    }

    public void deleteResource(String resourceId, boolean physical) {
        requireText(resourceId, "Resource id is required");
        if (!properties.isEnabled()) {
            log.info("Mango resource registry delete skipped: registry disabled, resourceId={}", resourceId);
            return;
        }
        String owner = resolveOwner();
        if (!lock.tryLock(owner, properties.getLockTtlSeconds())) {
            log.info("Mango resource registry delete skipped: lock is held by another instance");
            return;
        }
        try {
            doDeleteResource(resourceId, physical);
        } finally {
            lock.unlock(owner);
        }
    }

    private void doSync(boolean force) {
        Map<String, ResourceHandler> handlerMap = loadHandlers();
        List<ResourceDeclaration> declarations = collector.collect();
        doSync(declarations, handlerMap, collector.managedModuleCodes(declarations), force);
    }

    private void doSync(List<ResourceDeclaration> declarations, boolean force) {
        Map<String, ResourceHandler> handlerMap = loadHandlers();
        doSync(declarations, handlerMap, declarationModuleCodes(declarations), force);
    }

    private void doSync(List<ResourceDeclaration> declarations, Map<String, ResourceHandler> handlerMap,
                        Set<String> managedModuleCodes, boolean force) {
        validateConflicts(declarations);
        Set<String> seenResourceIds = new HashSet<>();
        List<ResourceDeclaration> activeDeclarations = new ArrayList<>();
        for (ResourceDeclaration declaration : declarations) {
            seenResourceIds.add(declaration.getId());
            if (isDisabled(declaration)) {
                syncOne(declaration, handlerMap, force);
            } else {
                activeDeclarations.add(declaration);
            }
        }
        syncActiveBatch(activeDeclarations, handlerMap, force);
        disableMissing(managedModuleCodes, seenResourceIds, handlerMap);
        log.info("Mango resource registry sync complete: declarations={}", declarations.size());
    }

    private Map<String, ResourceHandler> loadHandlers() {
        Map<String, ResourceHandler> handlerMap = new HashMap<>();
        for (ResourceHandler handler : handlers) {
            ResourceHandler previous = handlerMap.put(handler.resourceType(), handler);
            if (previous != null) {
                throw new IllegalStateException("Duplicate resource handler: " + handler.resourceType());
            }
        }
        return handlerMap;
    }

    private void validateConflicts(List<ResourceDeclaration> declarations) {
        Set<String> ids = new HashSet<>();
        Set<String> bizKeys = new HashSet<>();
        for (ResourceDeclaration declaration : declarations) {
            validateRequired(declaration);
            if (!ids.add(declaration.getId())) {
                conflict("Duplicate resource id: " + declaration.getId());
            }
            String bizKey = declaration.getResourceType() + ":" + declaration.getBizKey();
            if (!bizKeys.add(bizKey)) {
                conflict("Duplicate resource type and bizKey: " + bizKey);
            }
            ResourceRegistryRow rowByBizKey = repository.findByTypeAndBizKey(
                    declaration.getResourceType(), declaration.getBizKey());
            if (rowByBizKey != null && !declaration.getId().equals(rowByBizKey.getResourceId())) {
                conflict("Resource bizKey already registered by another id: "
                        + declaration.getResourceType() + ":" + declaration.getBizKey());
            }
        }
    }

    private void validateRequired(ResourceDeclaration declaration) {
        requireText(declaration.getId(), "Resource id is required");
        requireText(declaration.getResourceType(), "Resource type is required: " + declaration.getId());
        requireText(declaration.getModuleCode(), "Resource moduleCode is required: " + declaration.getId());
        requireText(declaration.getBizKey(), "Resource bizKey is required: " + declaration.getId());
        requireText(declaration.getTargetModule(), "Resource targetModule is required: " + declaration.getId());
        if (declaration.getVersion() == null || declaration.getVersion() < 1) {
            throw new IllegalStateException("Resource version must be positive: " + declaration.getId());
        }
        if (!declaration.getId().matches("\\d+")) {
            throw new IllegalStateException("Resource id must be snowflake numeric string: " + declaration.getId());
        }
    }

    private void requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(message);
        }
    }

    private void conflict(String message) {
        if (properties.isFailOnConflict()) {
            throw new IllegalStateException(message);
        }
        log.warn(message);
    }

    private void syncOne(ResourceDeclaration declaration, Map<String, ResourceHandler> handlerMap, boolean force) {
        ResourceHandler handler = handlerMap.get(declaration.getResourceType());
        if (handler == null) {
            throw new IllegalStateException("No resource handler found: " + declaration.getResourceType());
        }
        String hash = hasher.hash(declaration);
        ResourceRegistryRow row = repository.findByResourceId(declaration.getId());
        if (row != null && row.getSyncMode() != ResourceSyncMode.AUTO) {
            repository.insertSyncLog(row.getId(), "SKIP", "SKIPPED", "Resource sync mode is " + row.getSyncMode());
            return;
        }
        if (!force && row != null && hash.equals(row.getSourceHash()) && declaration.getStatus().name().equals(row.getStatus())) {
            repository.insertSyncLog(row.getId(), "SKIP", "SKIPPED", "Resource declaration is unchanged");
            return;
        }
        ResourceSyncResult result = isDisabled(declaration)
                ? handler.disable(declaration)
                : handler.upsert(declaration);
        if (row == null) {
            Long rowId = repository.insert(declaration, hash, result.getTargetId(), result.getTargetTable());
            repository.insertSyncLog(rowId, "CREATE", "SUCCESS", result.getMessage());
            repository.insertChangeLog(rowId, "CREATE", null, toJson(declaration));
        } else {
            repository.update(row, declaration, hash, result.getTargetId(), result.getTargetTable());
            repository.insertSyncLog(row.getId(), "UPDATE", "SUCCESS", result.getMessage());
            repository.insertChangeLog(row.getId(), "UPDATE", toJson(row), toJson(declaration));
        }
    }

    private void syncActiveBatch(List<ResourceDeclaration> declarations, Map<String, ResourceHandler> handlerMap,
                                 boolean force) {
        Map<String, List<ResourceDeclaration>> allDeclarationsByType = new HashMap<>();
        Map<String, List<ResourceDeclaration>> declarationsByType = new HashMap<>();
        for (ResourceDeclaration declaration : declarations) {
            String hash = hasher.hash(declaration);
            ResourceRegistryRow row = repository.findByResourceId(declaration.getId());
            ResourceDeclaration effectiveDeclaration = row == null
                    ? declaration
                    : withEffectiveSyncMode(declaration, row.getSyncMode());
            allDeclarationsByType.computeIfAbsent(declaration.getResourceType(), key -> new ArrayList<>())
                    .add(effectiveDeclaration);
            if (row != null && row.getSyncMode() != ResourceSyncMode.AUTO) {
                repository.insertSyncLog(row.getId(), "SKIP", "SKIPPED", "Resource sync mode is " + row.getSyncMode());
                continue;
            }
            if (!force && row != null && hash.equals(row.getSourceHash()) && declaration.getStatus().name().equals(row.getStatus())) {
                repository.insertSyncLog(row.getId(), "SKIP", "SKIPPED", "Resource declaration is unchanged");
                continue;
            }
            declarationsByType.computeIfAbsent(declaration.getResourceType(), key -> new ArrayList<>()).add(declaration);
        }
        for (Map.Entry<String, List<ResourceDeclaration>> entry : declarationsByType.entrySet()) {
            ResourceHandler handler = handlerMap.get(entry.getKey());
            if (handler == null) {
                throw new IllegalStateException("No resource handler found: " + entry.getKey());
            }
            List<ResourceDeclaration> handlerDeclarations = handler.requiresCompleteBatch()
                    ? allDeclarationsByType.get(entry.getKey())
                    : entry.getValue();
            Map<String, ResourceSyncResult> results = handler.upsertBatch(handlerDeclarations);
            for (ResourceDeclaration declaration : entry.getValue()) {
                ResourceSyncResult result = results.get(declaration.getId());
                if (result == null) {
                    throw new IllegalStateException("Resource handler did not return sync result: " + declaration.getId());
                }
                saveActiveSyncResult(declaration, result);
            }
        }
    }

    private void doDeleteResource(String resourceId, boolean physical) {
        ResourceRegistryRow row = repository.findByResourceId(resourceId);
        if (row == null) {
            throw new IllegalStateException("Resource registry not found: " + resourceId);
        }
        ResourceHandler handler = loadHandlers().get(row.getResourceType());
        if (handler == null) {
            throw new IllegalStateException("No resource handler found: " + row.getResourceType());
        }
        ResourceDeclaration declaration = fromRow(row);
        ResourceSyncResult result = physical ? handler.delete(declaration) : handler.disable(declaration);
        repository.updateStatus(row, ResourceStatus.REMOVED.name(), row.getSourceHash());
        String changeType = physical ? "DELETE" : "DISABLE";
        repository.insertSyncLog(row.getId(), changeType, "SUCCESS", result.getMessage());
        repository.insertChangeLog(row.getId(), changeType, toJson(row), toJson(declaration));
    }

    private void saveActiveSyncResult(ResourceDeclaration declaration, ResourceSyncResult result) {
        String hash = hasher.hash(declaration);
        ResourceRegistryRow row = repository.findByResourceId(declaration.getId());
        if (row == null) {
            Long rowId = repository.insert(declaration, hash, result.getTargetId(), result.getTargetTable());
            repository.insertSyncLog(rowId, "CREATE", "SUCCESS", result.getMessage());
            repository.insertChangeLog(rowId, "CREATE", null, toJson(declaration));
        } else {
            repository.update(row, declaration, hash, result.getTargetId(), result.getTargetTable());
            repository.insertSyncLog(row.getId(), "UPDATE", "SUCCESS", result.getMessage());
            repository.insertChangeLog(row.getId(), "UPDATE", toJson(row), toJson(declaration));
        }
    }

    private ResourceDeclaration withEffectiveSyncMode(ResourceDeclaration declaration, ResourceSyncMode syncMode) {
        ResourceDeclaration copy = new ResourceDeclaration();
        copy.setId(declaration.getId());
        copy.setVersion(declaration.getVersion());
        copy.setResourceType(declaration.getResourceType());
        copy.setModuleCode(declaration.getModuleCode());
        copy.setModuleName(declaration.getModuleName());
        copy.setBizKey(declaration.getBizKey());
        copy.setName(declaration.getName());
        copy.setTargetModule(declaration.getTargetModule());
        copy.setSyncMode(syncMode);
        copy.setStatus(declaration.getStatus());
        copy.setFields(declaration.getFields());
        copy.setSource(declaration.getSource());
        return copy;
    }

    private Set<String> declarationModuleCodes(List<ResourceDeclaration> declarations) {
        Set<String> moduleCodes = new HashSet<>();
        for (ResourceDeclaration declaration : declarations) {
            if (StringUtils.hasText(declaration.getModuleCode())) {
                moduleCodes.add(declaration.getModuleCode());
            }
        }
        return moduleCodes;
    }

    private void disableMissing(Set<String> modules, Set<String> seenResourceIds,
                                Map<String, ResourceHandler> handlerMap) {
        for (String module : modules) {
            for (ResourceRegistryRow row : repository.listByModule(module)) {
                if (!seenResourceIds.contains(row.getResourceId()) && row.getSyncMode() == ResourceSyncMode.AUTO) {
                    ResourceHandler handler = handlerMap.get(row.getResourceType());
                    if (handler == null) {
                        continue;
                    }
                    ResourceDeclaration disabled = fromRow(row);
                    ResourceSyncResult result = handler.disable(disabled);
                    repository.updateStatus(row, ResourceStatus.REMOVED.name(), row.getSourceHash());
                    repository.insertSyncLog(row.getId(), "DISABLE", "SUCCESS", result.getMessage());
                    repository.insertChangeLog(row.getId(), "DISABLE", toJson(row), toJson(disabled));
                }
            }
        }
    }

    private boolean isDisabled(ResourceDeclaration declaration) {
        return declaration.getStatus() == ResourceStatus.DISABLED
                || declaration.getStatus() == ResourceStatus.DEPRECATED
                || declaration.getStatus() == ResourceStatus.REMOVED;
    }

    private ResourceDeclaration fromRow(ResourceRegistryRow row) {
        ResourceDeclaration declaration = new ResourceDeclaration();
        declaration.setId(row.getResourceId());
        declaration.setVersion(row.getResourceVersion());
        declaration.setResourceType(row.getResourceType());
        declaration.setModuleCode(row.getModuleCode());
        declaration.setBizKey(row.getBizKey());
        declaration.setTargetModule(row.getTargetModule());
        declaration.setStatus(ResourceStatus.REMOVED);
        if (row.getTargetId() != null) {
            declaration.getFields().put("targetId", field(ResourceFieldType.LONG, row.getTargetId()));
        }
        if (StringUtils.hasText(row.getTargetTable())) {
            declaration.getFields().put("targetTable", field(ResourceFieldType.STRING, row.getTargetTable()));
        }
        return declaration;
    }

    private ResourceField field(ResourceFieldType type, Object value) {
        ResourceField field = new ResourceField();
        field.setType(type);
        field.setValue(value);
        return field;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Serialize resource change log failed", e);
        }
    }

    private String resolveOwner() {
        if (StringUtils.hasText(properties.getInstanceId())) {
            return properties.getInstanceId();
        }
        return ManagementFactory.getRuntimeMXBean().getName();
    }
}
