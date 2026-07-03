package io.mango.resource.core.sync;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.resource.api.ResourceHandler;
import io.mango.resource.api.ResourceTargetDispatcher;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 资源注册同步服务。
 */
@Slf4j
@RequiredArgsConstructor
public class ResourceRegistrySyncService {

    private static final String LOCAL_APP_CODE = "local";
    private static final String LOCAL_SERVICE_CODE = "local";

    private final ResourceRegistryProperties properties;
    private final ResourceDeclarationCollector collector;
    private final ObjectProvider<ResourceHandler> handlers;
    private final ObjectProvider<ResourceTargetDispatcher> targetDispatchers;
    private final ResourceContentHasher hasher;
    private final ResourceRegistryRepository repository;
    private final ResourceRegistryLock lock;
    private final ObjectMapper objectMapper;

    private enum VisitState {
        VISITING,
        VISITED
    }

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
        syncRemote(LOCAL_APP_CODE, LOCAL_SERVICE_CODE, declarations);
    }

    public void syncRemote(String appCode, String serviceCode, List<ResourceDeclaration> declarations) {
        syncRemote(appCode, serviceCode, List.of(), declarations);
    }

    public void syncRemote(String appCode, String serviceCode, List<String> managedModuleCodes,
                           List<ResourceDeclaration> declarations) {
        requireText(appCode, "Resource remote appCode is required");
        requireText(serviceCode, "Resource remote serviceCode is required");
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
            doSync(appCode.trim(), serviceCode.trim(), declarations, managedModuleCodes, false);
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
        doSync(LOCAL_APP_CODE, LOCAL_SERVICE_CODE, declarations, handlerMap,
                collector.managedModuleCodes(declarations), force);
    }

    private void doSync(String appCode, String serviceCode, List<ResourceDeclaration> declarations,
                        List<String> managedModuleCodes, boolean force) {
        Map<String, ResourceHandler> handlerMap = loadHandlers();
        List<ResourceDeclaration> safeDeclarations = declarations == null ? List.of() : declarations;
        Set<String> modules = new HashSet<>(declarationModuleCodes(safeDeclarations));
        if (managedModuleCodes != null) {
            managedModuleCodes.stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .forEach(modules::add);
        }
        doSync(appCode, serviceCode, safeDeclarations, handlerMap, modules, force);
    }

    private void doSync(String appCode, String serviceCode, List<ResourceDeclaration> declarations,
                        Map<String, ResourceHandler> handlerMap,
                        Set<String> managedModuleCodes, boolean force) {
        declarations.forEach(declaration -> applySource(declaration, appCode, serviceCode));
        validateConflicts(declarations);
        Set<String> seenResourceIds = new HashSet<>();
        List<ResourceDeclaration> activeDeclarations = new ArrayList<>();
        for (ResourceDeclaration declaration : declarations) {
            seenResourceIds.add(declaration.getId());
            if (isDeprecated(declaration)) {
                syncDeprecated(declaration, force);
            } else if (isDisabled(declaration)) {
                syncOne(declaration, handlerMap, force);
            } else {
                activeDeclarations.add(declaration);
            }
        }
        syncActiveBatch(activeDeclarations, handlerMap, force);
        disableMissing(appCode, serviceCode, managedModuleCodes, seenResourceIds, handlerMap);
        log.info("Mango resource registry sync complete: declarations={}", declarations.size());
    }

    private void applySource(ResourceDeclaration declaration, String appCode, String serviceCode) {
        if (!StringUtils.hasText(declaration.getAppCode())) {
            declaration.setAppCode(appCode);
        }
        if (!StringUtils.hasText(declaration.getServiceCode())) {
            declaration.setServiceCode(serviceCode);
        }
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
        requireText(declaration.getAppCode(), "Resource appCode is required: " + declaration.getId());
        requireText(declaration.getServiceCode(), "Resource serviceCode is required: " + declaration.getId());
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

    private void syncDeprecated(ResourceDeclaration declaration, boolean force) {
        String hash = hasher.hash(declaration);
        ResourceRegistryRow row = repository.findByResourceId(declaration.getId());
        validateVersion(row, declaration);
        if (row != null && row.getSyncMode() != ResourceSyncMode.AUTO) {
            repository.insertSyncLog(row.getId(), "SKIP", "SKIPPED", "Resource sync mode is " + row.getSyncMode());
            return;
        }
        if (!force && row != null && hash.equals(row.getSourceHash()) && declaration.getStatus().name().equals(row.getStatus())) {
            repository.insertSyncLog(row.getId(), "SKIP", "SKIPPED", "Resource declaration is unchanged");
            return;
        }
        if (row == null) {
            Long rowId = repository.insert(declaration, hash, null, null);
            repository.insertSyncLog(rowId, "CREATE", "SUCCESS", "Resource declaration is deprecated");
            repository.insertChangeLog(rowId, "CREATE", null, toJson(declaration));
        } else {
            repository.update(row, declaration, hash, row.getTargetId(), row.getTargetTable());
            repository.insertSyncLog(row.getId(), "UPDATE", "SUCCESS", "Resource declaration is deprecated");
            repository.insertChangeLog(row.getId(), "UPDATE", toJson(row), toJson(declaration));
        }
    }

    private void syncOne(ResourceDeclaration declaration, Map<String, ResourceHandler> handlerMap, boolean force) {
        String hash = hasher.hash(declaration);
        ResourceRegistryRow row = repository.findByResourceId(declaration.getId());
        validateVersion(row, declaration);
        if (row != null && row.getSyncMode() != ResourceSyncMode.AUTO) {
            repository.insertSyncLog(row.getId(), "SKIP", "SKIPPED", "Resource sync mode is " + row.getSyncMode());
            return;
        }
        if (!force && row != null && hash.equals(row.getSourceHash()) && declaration.getStatus().name().equals(row.getStatus())) {
            repository.insertSyncLog(row.getId(), "SKIP", "SKIPPED", "Resource declaration is unchanged");
            return;
        }
        ResourceSyncResult result = isDisabled(declaration)
                ? disableTarget(declaration, handlerMap)
                : upsertSingleTarget(declaration, handlerMap);
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
        Map<String, List<ResourceDeclaration>> allDeclarationsByType = new LinkedHashMap<>();
        Map<String, List<ResourceDeclaration>> declarationsByType = new LinkedHashMap<>();
        for (ResourceDeclaration declaration : declarations) {
            String hash = hasher.hash(declaration);
            ResourceRegistryRow row = repository.findByResourceId(declaration.getId());
            validateVersion(row, declaration);
            ResourceDeclaration effectiveDeclaration = row == null
                    ? declaration
                    : withEffectiveSyncMode(declaration, row.getSyncMode());
            allDeclarationsByType.computeIfAbsent(declaration.getResourceType(), key -> new ArrayList<>())
                    .add(effectiveDeclaration);
            if (shouldPreserveInitOnlyTarget(row, declaration)) {
                skipInitOnlyTargetUpdate(row, declaration, hash);
                continue;
            }
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
        for (String resourceType : orderResourceTypesForSync(declarationsByType, handlerMap)) {
            List<ResourceDeclaration> changedDeclarations = declarationsByType.get(resourceType);
            ResourceHandler handler = handlerMap.get(resourceType);
            if (handler == null && !canDispatchAll(changedDeclarations)) {
                throw new IllegalStateException("No resource handler found: " + resourceType);
            }
            List<ResourceDeclaration> completeBatch = allDeclarationsByType.get(resourceType);
            Map<String, ResourceSyncResult> results = syncActiveBatchByTarget(
                    handler,
                    changedDeclarations,
                    completeBatch);
            for (ResourceDeclaration declaration : changedDeclarations) {
                ResourceSyncResult result = results.get(declaration.getId());
                if (result == null) {
                    throw new IllegalStateException("Resource handler did not return sync result: " + declaration.getId());
                }
                saveActiveSyncResult(declaration, result);
            }
        }
    }

    private List<String> orderResourceTypesForSync(Map<String, List<ResourceDeclaration>> declarationsByType,
                                                   Map<String, ResourceHandler> handlerMap) {
        Set<String> activeTypes = declarationsByType.keySet();
        Map<String, VisitState> states = new HashMap<>();
        List<String> orderedTypes = new ArrayList<>();
        List<String> path = new ArrayList<>();
        for (String resourceType : declarationsByType.keySet()) {
            visitResourceType(resourceType, activeTypes, handlerMap, states, path, orderedTypes);
        }
        return orderedTypes;
    }

    private void visitResourceType(String resourceType, Set<String> activeTypes,
                                   Map<String, ResourceHandler> handlerMap,
                                   Map<String, VisitState> states,
                                   List<String> path,
                                   List<String> orderedTypes) {
        VisitState state = states.get(resourceType);
        if (state == VisitState.VISITED) {
            return;
        }
        if (state == VisitState.VISITING) {
            throw new IllegalStateException("Resource type dependency cycle detected: "
                    + cyclePath(path, resourceType));
        }
        states.put(resourceType, VisitState.VISITING);
        path.add(resourceType);
        ResourceHandler handler = handlerMap.get(resourceType);
        List<String> dependencyTypes = handler == null ? List.of() : handler.dependsOnResourceTypes();
        if (dependencyTypes != null) {
            for (String dependencyType : dependencyTypes) {
                if (StringUtils.hasText(dependencyType) && activeTypes.contains(dependencyType.trim())) {
                    visitResourceType(dependencyType.trim(), activeTypes, handlerMap, states, path, orderedTypes);
                }
            }
        }
        path.remove(path.size() - 1);
        states.put(resourceType, VisitState.VISITED);
        orderedTypes.add(resourceType);
    }

    private String cyclePath(List<String> path, String repeatedType) {
        int start = path.indexOf(repeatedType);
        List<String> cycle = new ArrayList<>(path.subList(Math.max(start, 0), path.size()));
        cycle.add(repeatedType);
        return String.join(" -> ", cycle);
    }

    private void validateVersion(ResourceRegistryRow row, ResourceDeclaration declaration) {
        if (row == null || declaration.getVersion() == null || row.getResourceVersion() == null) {
            return;
        }
        if (declaration.getVersion() < row.getResourceVersion()) {
            throw new IllegalStateException("Resource declaration version rollback is not allowed: "
                    + declaration.getId() + " current=" + row.getResourceVersion()
                    + ", incoming=" + declaration.getVersion());
        }
    }

    private void doDeleteResource(String resourceId, boolean physical) {
        ResourceRegistryRow row = repository.findByResourceId(resourceId);
        if (row == null) {
            throw new IllegalStateException("Resource registry not found: " + resourceId);
        }
        Map<String, ResourceHandler> handlerMap = loadHandlers();
        ResourceDeclaration declaration = fromRow(row);
        ResourceSyncResult result = physical
                ? deleteTarget(declaration, handlerMap)
                : disableTarget(declaration, handlerMap);
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

    private void skipInitOnlyTargetUpdate(ResourceRegistryRow row, ResourceDeclaration declaration, String hash) {
        if (hash.equals(row.getSourceHash()) && declaration.getStatus().name().equals(row.getStatus())) {
            repository.insertSyncLog(row.getId(), "SKIP", "SKIPPED", "Resource declaration is unchanged");
            return;
        }
        repository.update(row, declaration, hash, row.getTargetId(), row.getTargetTable());
        repository.insertSyncLog(row.getId(), "SKIP", "SKIPPED", "Resource sync mode is INIT_ONLY");
        repository.insertChangeLog(row.getId(), "UPDATE", toJson(row), toJson(declaration));
    }

    private boolean shouldPreserveInitOnlyTarget(ResourceRegistryRow row, ResourceDeclaration declaration) {
        if (row == null || declaration.getSyncMode() != ResourceSyncMode.INIT_ONLY) {
            return false;
        }
        return row.getSyncMode() == ResourceSyncMode.AUTO || row.getSyncMode() == ResourceSyncMode.INIT_ONLY;
    }

    private ResourceDeclaration withEffectiveSyncMode(ResourceDeclaration declaration, ResourceSyncMode syncMode) {
        ResourceDeclaration copy = new ResourceDeclaration();
        copy.setId(declaration.getId());
        copy.setVersion(declaration.getVersion());
        copy.setAppCode(declaration.getAppCode());
        copy.setServiceCode(declaration.getServiceCode());
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

    private void disableMissing(String appCode, String serviceCode, Set<String> modules, Set<String> seenResourceIds,
                                Map<String, ResourceHandler> handlerMap) {
        for (String module : modules) {
            for (ResourceRegistryRow row : repository.listBySourceAndModule(appCode, serviceCode, module)) {
                if (!seenResourceIds.contains(row.getResourceId()) && row.getSyncMode() == ResourceSyncMode.AUTO) {
                    ResourceHandler handler = handlerMap.get(row.getResourceType());
                    if (handler == null) {
                        ResourceDeclaration disabled = fromRow(row);
                        ResourceTargetDispatcher dispatcher = targetDispatcher(disabled);
                        if (dispatcher == null) {
                            throw new IllegalStateException("No resource handler found for missing resource disable: "
                                    + row.getResourceType() + ", resourceId=" + row.getResourceId()
                                    + ", targetModule=" + row.getTargetModule());
                        }
                        ResourceSyncResult result = dispatcher.disable(disabled);
                        repository.updateStatus(row, ResourceStatus.REMOVED.name(), row.getSourceHash());
                        repository.insertSyncLog(row.getId(), "DISABLE", "SUCCESS", result.getMessage());
                        repository.insertChangeLog(row.getId(), "DISABLE", toJson(row), toJson(disabled));
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

    private Map<String, ResourceSyncResult> syncActiveBatchByTarget(
            ResourceHandler handler,
            List<ResourceDeclaration> changedDeclarations,
            List<ResourceDeclaration> completeBatch) {
        Map<String, ResourceSyncResult> results = new HashMap<>();
        if (handler != null) {
            List<ResourceDeclaration> handlerDeclarations = handler.requiresCompleteBatch()
                    ? completeBatch
                    : changedDeclarations;
            results.putAll(handler.upsertBatch(handlerDeclarations));
            return results;
        }
        List<ResourceDeclaration> localDeclarations = new ArrayList<>();
        Map<ResourceTargetDispatcher, List<ResourceDeclaration>> remoteDeclarations = new HashMap<>();
        for (ResourceDeclaration declaration : changedDeclarations) {
            ResourceTargetDispatcher dispatcher = targetDispatcher(declaration);
            if (dispatcher == null) {
                localDeclarations.add(declaration);
            } else {
                remoteDeclarations.computeIfAbsent(dispatcher, ignored -> new ArrayList<>()).add(declaration);
            }
        }
        remoteDeclarations.forEach((dispatcher, declarations) ->
                results.putAll(dispatcher.upsertBatch(declarations, completeBatch)));
        if (!localDeclarations.isEmpty()) {
            throw new IllegalStateException("No resource handler found: " + localDeclarations.get(0).getResourceType());
        }
        return results;
    }

    private ResourceSyncResult upsertSingleTarget(ResourceDeclaration declaration, Map<String, ResourceHandler> handlerMap) {
        ResourceHandler handler = handlerMap.get(declaration.getResourceType());
        if (handler != null) {
            return handler.upsert(declaration);
        }
        ResourceTargetDispatcher dispatcher = targetDispatcher(declaration);
        if (dispatcher == null) {
            throw new IllegalStateException("No resource handler found: " + declaration.getResourceType());
        }
        Map<String, ResourceSyncResult> results = dispatcher.upsertBatch(List.of(declaration), List.of(declaration));
        ResourceSyncResult result = results.get(declaration.getId());
        if (result == null) {
            throw new IllegalStateException("Resource target dispatcher did not return sync result: " + declaration.getId());
        }
        return result;
    }

    private ResourceSyncResult disableTarget(ResourceDeclaration declaration, Map<String, ResourceHandler> handlerMap) {
        ResourceHandler handler = handlerMap.get(declaration.getResourceType());
        if (handler != null) {
            return handler.disable(declaration);
        }
        ResourceTargetDispatcher dispatcher = targetDispatcher(declaration);
        if (dispatcher == null) {
            throw new IllegalStateException("No resource handler found: " + declaration.getResourceType());
        }
        return dispatcher.disable(declaration);
    }

    private ResourceSyncResult deleteTarget(ResourceDeclaration declaration, Map<String, ResourceHandler> handlerMap) {
        ResourceHandler handler = handlerMap.get(declaration.getResourceType());
        if (handler != null) {
            return handler.delete(declaration);
        }
        ResourceTargetDispatcher dispatcher = targetDispatcher(declaration);
        if (dispatcher == null) {
            throw new IllegalStateException("No resource handler found: " + declaration.getResourceType());
        }
        return dispatcher.delete(declaration);
    }

    private boolean canDispatchAll(List<ResourceDeclaration> declarations) {
        return declarations.stream().allMatch(declaration -> targetDispatcher(declaration) != null);
    }

    private ResourceTargetDispatcher targetDispatcher(ResourceDeclaration declaration) {
        for (ResourceTargetDispatcher dispatcher : targetDispatchers) {
            if (dispatcher.supports(declaration.getTargetModule())) {
                return dispatcher;
            }
        }
        return null;
    }

    private boolean isDeprecated(ResourceDeclaration declaration) {
        return declaration.getStatus() == ResourceStatus.DEPRECATED;
    }

    private boolean isDisabled(ResourceDeclaration declaration) {
        return declaration.getStatus() == ResourceStatus.DISABLED
                || declaration.getStatus() == ResourceStatus.REMOVED;
    }

    private ResourceDeclaration fromRow(ResourceRegistryRow row) {
        ResourceDeclaration declaration = new ResourceDeclaration();
        declaration.setId(row.getResourceId());
        declaration.setVersion(row.getResourceVersion());
        declaration.setAppCode(row.getAppCode());
        declaration.setServiceCode(row.getServiceCode());
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
