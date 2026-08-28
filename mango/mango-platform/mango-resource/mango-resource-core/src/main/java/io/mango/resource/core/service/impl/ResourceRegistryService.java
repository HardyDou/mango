package io.mango.resource.core.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.Require;
import io.mango.infra.bootstrap.api.BootstrapGenerationFence;
import io.mango.infra.bootstrap.api.BootstrapWriteAuthority;
import io.mango.resource.api.command.RegisterResourceDeclarationsCommand;
import io.mango.resource.api.command.ResourceModuleManifestCommand;
import io.mango.resource.api.enums.ResourceApplyMode;
import io.mango.resource.api.enums.ResourceCode;
import io.mango.resource.support.ResourceHandler;
import io.mango.resource.support.ResourceTargetDispatcher;
import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.api.enums.ResourceStatus;
import io.mango.resource.api.enums.ResourceSyncMode;
import io.mango.resource.api.enums.ResourceSyncDisposition;
import io.mango.resource.api.enums.ResourceExecutionPhase;
import io.mango.resource.core.service.IResourceRegistryService;
import io.mango.resource.core.sync.ResourceContentHasher;
import io.mango.resource.core.sync.ResourceRegistryLock;
import io.mango.resource.core.sync.ResourceRegistryRepository;
import io.mango.resource.core.sync.ResourceRegistryRepository.ResourceRegistrySnapshot;
import io.mango.resource.core.sync.ResourceRegistryRow;
import io.mango.resource.core.sync.ResourceModuleReceiptRepository;
import io.mango.resource.core.diagnostic.ResourceModuleSyncStatusRegistry;
import io.mango.resource.core.diagnostic.ResourceModuleSyncStatusRegistry.ModuleObservation;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.support.model.ResourceField;
import io.mango.resource.support.model.ResourceSyncContext;
import io.mango.resource.support.model.ResourceSyncResult;
import io.mango.resource.support.config.ResourceRegistryProperties;
import io.mango.resource.support.declaration.ResourceDeclarationCollector;
import io.mango.resource.support.execution.ResourceHandlerInvoker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.lang.management.ManagementFactory;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 资源注册同步服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceRegistryService implements IResourceRegistryService, SmartLifecycle {

    private static final String LOCAL_APP_CODE = "local";
    private static final String LOCAL_SERVICE_CODE = "local";

    private final ResourceRegistryProperties properties;
    private final ResourceDeclarationCollector collector;
    private final ObjectProvider<ResourceHandler> handlers;
    private final ObjectProvider<ResourceTargetDispatcher> targetDispatchers;
    private final ResourceContentHasher hasher;
    private final ResourceRegistryRepository repository;
    private final ResourceModuleReceiptRepository moduleReceiptRepository;
    private final ResourceRegistryLock lock;
    private final ObjectMapper objectMapper;
    private final ResourceModuleSyncStatusRegistry moduleSyncStatusRegistry;
    private final ObjectProvider<BootstrapGenerationFence> generationFences;
    private final ResourceHandlerInvoker handlerInvoker = new ResourceHandlerInvoker();
    private final Object lifecycleMonitor = new Object();
    private volatile boolean running = true;
    private int inFlightOperations;

    private enum VisitState {
        VISITING,
        VISITED
    }

    @Override
    public void sync() {
        sync(false);
    }

    @Override
    public void sync(boolean force) {
        if (!beginOperation()) {
            log.info("Mango resource registry sync skipped: application is shutting down");
            return;
        }
        try {
            syncWhileRunning(force);
        } finally {
            endOperation();
        }
    }

    private void syncWhileRunning(boolean force) {
        invalidateDiagnosticStatus("RESOURCE_SYNC_ATTEMPT_STARTED");
        try {
            if (!properties.isEnabled()) {
                invalidateDiagnosticStatus("RESOURCE_SYNC_DISABLED");
                log.info("Mango resource registry sync disabled");
                return;
            }
            String owner = resolveOwner();
            ResourceRegistryLock.LeaseSession lease = lock.tryLock(
                    owner, properties.getLockTtlSeconds()).orElse(null);
            if (lease == null) {
                invalidateDiagnosticStatus("RESOURCE_SYNC_LOCK_NOT_ACQUIRED");
                log.info("Mango resource registry sync skipped: lock is held by another instance");
                return;
            }
            try {
                doSync(force);
            } finally {
                lease.close();
            }
        } catch (RuntimeException exception) {
            failObservedDiagnosticStatus("RESOURCE_SYNC_FAILED");
            Require.rethrow(exception);
        }
    }

    boolean syncRemote(List<ResourceDeclaration> declarations) {
        return syncRemote(LOCAL_APP_CODE, LOCAL_SERVICE_CODE, declarations);
    }

    boolean syncRemote(String appCode, String serviceCode, List<ResourceDeclaration> declarations) {
        return syncRemote(appCode, serviceCode, List.of(), declarations);
    }

    boolean syncRemote(String appCode, String serviceCode, List<String> managedModuleCodes,
                       List<ResourceDeclaration> declarations) {
        return syncRemote(appCode, serviceCode, managedModuleCodes, declarations, true);
    }

    boolean syncRemote(String appCode, String serviceCode, List<String> managedModuleCodes,
                       List<ResourceDeclaration> declarations, boolean disableMissingResources) {
        if (!beginOperation()) {
            log.info("Mango resource registry remote sync deferred: application is shutting down");
            return false;
        }
        try {
            return syncRemoteWhileRunning(
                    appCode, serviceCode, managedModuleCodes, declarations, disableMissingResources);
        } finally {
            endOperation();
        }
    }

    private boolean syncRemoteWhileRunning(String appCode, String serviceCode, List<String> managedModuleCodes,
                                           List<ResourceDeclaration> declarations,
                                           boolean disableMissingResources) {
        invalidateDiagnosticStatus("RESOURCE_SYNC_ATTEMPT_STARTED");
        try {
            requireText(appCode, "Resource remote appCode is required");
            requireText(serviceCode, "Resource remote serviceCode is required");
            if (!properties.isEnabled()) {
                invalidateDiagnosticStatus("RESOURCE_SYNC_DISABLED");
                log.info("Mango resource registry remote sync disabled");
                return true;
            }
            String owner = resolveOwner();
            ResourceRegistryLock.LeaseSession lease = lock.tryLock(
                    owner, properties.getLockTtlSeconds()).orElse(null);
            if (lease == null) {
                invalidateDiagnosticStatus("RESOURCE_SYNC_LOCK_NOT_ACQUIRED");
                log.info("Mango resource registry remote sync deferred: lock is held by another instance");
                return false;
            }
            try {
                doSync(appCode.trim(), serviceCode.trim(), declarations, managedModuleCodes, false,
                        disableMissingResources);
                return true;
            } finally {
                lease.close();
            }
        } catch (RuntimeException exception) {
            failObservedDiagnosticStatus("RESOURCE_SYNC_FAILED");
            return Require.rethrow(exception);
        }
    }

    @Override
    public Boolean registerDeclarations(RegisterResourceDeclarationsCommand command) {
        RegisterResourceDeclarationsCommand validatedCommand = Require.nonNull(
                command, ResourceCode.RESOURCE_INVALID, "资源声明注册命令不能为空");
        Require.notBlank(validatedCommand.getAppCode(), ResourceCode.RESOURCE_INVALID, "来源应用不能为空");
        Require.notBlank(validatedCommand.getServiceCode(), ResourceCode.RESOURCE_INVALID, "来源服务不能为空");
        Require.notBlank(validatedCommand.getEnvironmentKey(), ResourceCode.RESOURCE_INVALID,
                "Bootstrap 环境标识不能为空");
        long validatedGeneration = Require.nonNull(
                validatedCommand.getGeneration(), ResourceCode.RESOURCE_INVALID, "Release generation 不能为空");
        Require.isTrue(validatedGeneration > 0, ResourceCode.RESOURCE_INVALID, "Release generation 必须大于0");
        Require.notBlank(validatedCommand.getManifestFingerprint(), ResourceCode.RESOURCE_INVALID,
                "Manifest fingerprint 不能为空");
        long fencingToken = Require.nonNull(
                validatedCommand.getFencingToken(), ResourceCode.RESOURCE_INVALID, "Fencing token 不能为空");
        ResourceApplyMode validatedApplyMode = Require.nonNull(
                validatedCommand.getApplyMode(), ResourceCode.RESOURCE_INVALID, "Resource apply mode 不能为空");
        BootstrapGenerationFence generationFence = Require.nonNull(
                generationFences.getIfAvailable(), ResourceCode.RESOURCE_INVALID, "Bootstrap generation fence 未启用");
        BootstrapWriteAuthority writeAuthority = new BootstrapWriteAuthority(
                validatedCommand.getEnvironmentKey(), validatedGeneration, validatedCommand.getManifestFingerprint(),
                fencingToken);
        generationFence.assertAuthoritative(writeAuthority);
        if (!validatedCommand.getModuleManifests().isEmpty()
                && validatedApplyMode != ResourceApplyMode.EVENTUAL) {
            return registerModuleManifests(validatedCommand, validatedApplyMode, generationFence, writeAuthority);
        }
        List<ResourceDeclaration> declarations = parseDeclarations(validatedCommand.getDeclarations());
        List<String> moduleCodes = validatedCommand.getModuleCodes() == null
                ? List.of() : List.copyOf(validatedCommand.getModuleCodes());
        Require.isTrue(!declarations.isEmpty() || !moduleCodes.isEmpty(),
                ResourceCode.RESOURCE_INVALID, "资源声明和管理模块不能同时为空");
        List<ResourceDeclaration> selectedDeclarations = selectDeclarations(declarations, validatedApplyMode);
        boolean synchronizedNow = syncRemote(
                validatedCommand.getAppCode(), validatedCommand.getServiceCode(), moduleCodes, selectedDeclarations,
                validatedApplyMode == ResourceApplyMode.FINALIZE);
        if (!synchronizedNow) {
            return Boolean.FALSE;
        }
        log.info("Mango resource remote declarations registered: appCode={}, serviceCode={}, count={}",
                validatedCommand.getAppCode(), validatedCommand.getServiceCode(), declarations.size());
        return Boolean.TRUE;
    }

    private Boolean registerModuleManifests(RegisterResourceDeclarationsCommand command,
                                            ResourceApplyMode applyMode,
                                            BootstrapGenerationFence generationFence,
                                            BootstrapWriteAuthority writeAuthority) {
        if (!beginOperation()) {
            log.info("Mango resource module coordination deferred: application is shutting down");
            return Boolean.FALSE;
        }
        try {
            return registerModuleManifestsWhileRunning(command, applyMode, generationFence, writeAuthority);
        } finally {
            endOperation();
        }
    }

    private Boolean registerModuleManifestsWhileRunning(RegisterResourceDeclarationsCommand command,
                                                        ResourceApplyMode applyMode,
                                                        BootstrapGenerationFence generationFence,
                                                        BootstrapWriteAuthority writeAuthority) {
        invalidateDiagnosticStatus("RESOURCE_SYNC_ATTEMPT_STARTED");
        if (!properties.isEnabled()) {
            invalidateDiagnosticStatus("RESOURCE_SYNC_DISABLED");
            log.info("Mango resource module coordination disabled");
            return Boolean.TRUE;
        }
        List<ResourceModuleManifestCommand> orderedModules = orderModuleManifests(command.getModuleManifests());
        String owner = resolveOwner();
        ResourceRegistryLock.LeaseSession lease = lock.tryLock(
                owner, properties.getLockTtlSeconds()).orElse(null);
        if (lease == null) {
            invalidateDiagnosticStatus("RESOURCE_SYNC_LOCK_NOT_ACQUIRED");
            log.info("Mango resource module coordination deferred: lock is held by another instance");
            return Boolean.FALSE;
        }
        try {
            int skipped = 0;
            int applied = 0;
            long startedNanos = System.nanoTime();
            for (ResourceModuleManifestCommand module : orderedModules) {
                generationFence.assertAuthoritative(writeAuthority);
                long moduleStartedNanos = System.nanoTime();
                if (moduleReceiptRepository.isSatisfied(
                        command.getEnvironmentKey(), command.getAppCode(), command.getServiceCode(),
                        module.getModuleCode(), module.getModuleHash(), applyMode)) {
                    skipped++;
                    log.info("Mango resource module skipped: module={}, mode={}, hash={}, durationMs={}",
                            module.getModuleCode(), applyMode, module.getModuleHash(),
                            elapsedMillis(moduleStartedNanos));
                    continue;
                }
                List<ResourceDeclaration> declarations = parseDeclarations(module.getDeclarations());
                validateModuleManifest(module, declarations);
                List<ResourceDeclaration> selectedDeclarations = selectDeclarations(declarations, applyMode);
                doSync(command.getAppCode(), command.getServiceCode(), selectedDeclarations,
                        List.of(module.getModuleCode()), false, applyMode == ResourceApplyMode.FINALIZE);
                generationFence.assertAuthoritative(writeAuthority);
                moduleReceiptRepository.recordSuccess(
                        command.getEnvironmentKey(), command.getAppCode(), command.getServiceCode(),
                        module.getModuleCode(), module.getModuleHash(), command.getGeneration(),
                        command.getManifestFingerprint(), applyMode, declarations.size());
                applied++;
                log.info("Mango resource module applied: module={}, mode={}, declarations={}, hash={}, durationMs={}",
                        module.getModuleCode(), applyMode, declarations.size(), module.getModuleHash(),
                        elapsedMillis(moduleStartedNanos));
            }
            log.info("Mango resource module coordination complete: mode={}, modules={}, applied={}, skipped={}, durationMs={}",
                    applyMode, orderedModules.size(), applied, skipped, elapsedMillis(startedNanos));
            return Boolean.TRUE;
        } catch (RuntimeException exception) {
            failObservedDiagnosticStatus("RESOURCE_SYNC_FAILED");
            return Require.rethrow(exception);
        } finally {
            lease.close();
        }
    }

    private void validateModuleManifest(ResourceModuleManifestCommand module,
                                        List<ResourceDeclaration> declarations) {
        Require.isTrue(module.getDeclarationCount() == declarations.size(), ResourceCode.RESOURCE_INVALID,
                "Resource 模块声明数量不匹配: " + module.getModuleCode());
        Require.isTrue(declarations.stream().allMatch(
                        declaration -> module.getModuleCode().equals(declaration.getModuleCode())),
                ResourceCode.RESOURCE_INVALID, "Resource 声明不属于模块: " + module.getModuleCode());
        String actualHash = hasher.moduleHash(module.getModuleCode(), module.getDependencies(), declarations);
        Require.isTrue(actualHash.equals(module.getModuleHash()), ResourceCode.RESOURCE_INVALID,
                "Resource 模块 Hash 不匹配: " + module.getModuleCode());
    }

    private List<ResourceModuleManifestCommand> orderModuleManifests(
            List<ResourceModuleManifestCommand> manifests) {
        Map<String, ResourceModuleManifestCommand> byCode = new LinkedHashMap<>();
        for (ResourceModuleManifestCommand manifest : manifests) {
            Require.notNull(manifest, ResourceCode.RESOURCE_INVALID, "Resource 模块清单不能为空");
            requireText(manifest.getModuleCode(), "Resource moduleCode is required");
            Require.isTrue(manifest.getModuleHash() != null && manifest.getModuleHash().matches("[0-9a-f]{64}"),
                    ResourceCode.RESOURCE_INVALID, "Resource moduleHash is invalid: " + manifest.getModuleCode());
            Require.isTrue(manifest.getDeclarationCount() >= 0, ResourceCode.RESOURCE_INVALID,
                    "Resource declarationCount is invalid: " + manifest.getModuleCode());
            requireText(manifest.getDeclarations(), "Resource module declarations are required: "
                    + manifest.getModuleCode());
            ResourceModuleManifestCommand previous = byCode.put(manifest.getModuleCode(), manifest);
            Require.isTrue(previous == null, ResourceCode.RESOURCE_CONFLICT,
                    "Resource module manifest duplicated: " + manifest.getModuleCode());
        }
        List<ResourceModuleManifestCommand> ordered = new ArrayList<>();
        Map<String, VisitState> states = new HashMap<>();
        byCode.keySet().stream().sorted().forEach(moduleCode -> visitModule(
                moduleCode, byCode, states, ordered, new ArrayList<>()));
        return ordered;
    }

    private void visitModule(String moduleCode,
                             Map<String, ResourceModuleManifestCommand> byCode,
                             Map<String, VisitState> states,
                             List<ResourceModuleManifestCommand> ordered,
                             List<String> path) {
        VisitState state = states.get(moduleCode);
        if (state == VisitState.VISITED) {
            return;
        }
        if (state == VisitState.VISITING) {
            path.add(moduleCode);
            Require.isTrue(false, ResourceCode.RESOURCE_CONFLICT,
                    "Resource 模块依赖存在循环: " + String.join(" -> ", path));
        }
        states.put(moduleCode, VisitState.VISITING);
        path.add(moduleCode);
        ResourceModuleManifestCommand module = byCode.get(moduleCode);
        module.getDependencies().stream().distinct().sorted()
                .forEach(dependency -> {
                    Require.isTrue(byCode.containsKey(dependency), ResourceCode.RESOURCE_INVALID,
                            "Resource 模块依赖缺失: " + moduleCode + " -> " + dependency);
                    visitModule(dependency, byCode, states, ordered, new ArrayList<>(path));
                });
        states.put(moduleCode, VisitState.VISITED);
        ordered.add(module);
    }

    private static long elapsedMillis(long startedNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos);
    }

    private List<ResourceDeclaration> selectDeclarations(
            List<ResourceDeclaration> declarations, ResourceApplyMode applyMode) {
        return declarations.stream().filter(declaration -> switch (applyMode) {
            case EXPAND -> declaration.getExecutionPhase() == null
                    || declaration.getExecutionPhase() == ResourceExecutionPhase.BOOTSTRAP_REQUIRED;
            case EVENTUAL -> declaration.getExecutionPhase() == ResourceExecutionPhase.RUNTIME_EVENTUAL;
            case FINALIZE -> declaration.getExecutionPhase() != ResourceExecutionPhase.MANUAL;
        }).toList();
    }

    @Override
    public void deleteResource(String resourceId, boolean physical) {
        Require.notBlank(resourceId, ResourceCode.RESOURCE_INVALID, "Resource id is required");
        if (!beginOperation()) {
            log.info("Mango resource registry delete skipped: application is shutting down, resourceId={}",
                    resourceId);
            return;
        }
        try {
            deleteResourceWhileRunning(resourceId, physical);
        } finally {
            endOperation();
        }
    }

    private void deleteResourceWhileRunning(String resourceId, boolean physical) {
        Require.notBlank(resourceId, ResourceCode.RESOURCE_INVALID, "Resource id is required");
        if (!properties.isEnabled()) {
            log.info("Mango resource registry delete skipped: registry disabled, resourceId={}", resourceId);
            return;
        }
        String owner = resolveOwner();
        ResourceRegistryLock.LeaseSession lease = lock.tryLock(owner, properties.getLockTtlSeconds()).orElse(null);
        if (lease == null) {
            log.info("Mango resource registry delete skipped: lock is held by another instance");
            return;
        }
        try {
            doDeleteResource(resourceId, physical);
        } finally {
            lease.close();
        }
    }

    private void doSync(boolean force) {
        Map<String, ResourceHandler> handlerMap = loadHandlers();
        List<ResourceDeclaration> declarations = collector.collect();
        doSync(LOCAL_APP_CODE, LOCAL_SERVICE_CODE, declarations, handlerMap,
                collector.managedModuleCodes(declarations), force);
    }

    private void doSync(String appCode, String serviceCode, List<ResourceDeclaration> declarations,
                        List<String> managedModuleCodes, boolean force, boolean disableMissingResources) {
        Map<String, ResourceHandler> handlerMap = loadHandlers();
        List<ResourceDeclaration> safeDeclarations = declarations;
        if (safeDeclarations == null) {
            safeDeclarations = List.of();
        }
        Set<String> modules = new HashSet<>(declarationModuleCodes(safeDeclarations));
        if (managedModuleCodes != null) {
            managedModuleCodes.stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .forEach(modules::add);
        }
        doSync(appCode, serviceCode, safeDeclarations, handlerMap, modules, force, disableMissingResources);
    }

    private void doSync(String appCode, String serviceCode, List<ResourceDeclaration> declarations,
                        Map<String, ResourceHandler> handlerMap,
                        Set<String> managedModuleCodes, boolean force) {
        doSync(appCode, serviceCode, declarations, handlerMap, managedModuleCodes, force, true);
    }

    private void doSync(String appCode, String serviceCode, List<ResourceDeclaration> declarations,
                        Map<String, ResourceHandler> handlerMap,
                        Set<String> managedModuleCodes, boolean force, boolean disableMissingResources) {
        assertOperationCanContinue();
        declarations.forEach(declaration -> applySource(declaration, appCode, serviceCode));
        Map<String, ModuleObservation> observations = observeDeclarations(declarations);
        recordDiagnosticRunning(observations, managedModuleCodes);
        try {
            validateDeclarations(declarations);
            ResourceRegistrySnapshot registrySnapshot = repository.loadSnapshot(declarations);
            validateRegistryConflicts(declarations, registrySnapshot);
            Set<String> seenResourceIds = new HashSet<>();
            List<ResourceDeclaration> activeDeclarations = new ArrayList<>();
            for (ResourceDeclaration declaration : declarations) {
                assertOperationCanContinue();
                seenResourceIds.add(declaration.getId());
                if (isDeprecated(declaration)) {
                    syncDeprecated(declaration, force, registrySnapshot);
                } else if (isDisabled(declaration)) {
                    syncOne(declaration, handlerMap, force, registrySnapshot);
                } else {
                    activeDeclarations.add(declaration);
                }
            }
            syncActiveBatch(activeDeclarations, handlerMap, force, registrySnapshot);
            if (disableMissingResources) {
                disableMissing(appCode, serviceCode, managedModuleCodes, seenResourceIds, handlerMap);
            }
            observeDiagnosticCompletion(observations, declarations, handlerMap);
            log.info("Mango resource registry sync complete: declarations={}", declarations.size());
        } catch (RuntimeException exception) {
            recordDiagnosticFailure(observations, "RESOURCE_SYNC_FAILED");
            Require.rethrow(exception);
        }
    }

    private Map<String, ModuleObservation> observeDeclarations(List<ResourceDeclaration> declarations) {
        try {
            return moduleSyncStatusRegistry.observations(declarations);
        } catch (RuntimeException observationFailure) {
            log.warn("Mango resource synchronization diagnostic observation failed before synchronization",
                    observationFailure);
            return Map.of();
        }
    }

    private void observeDiagnosticCompletion(
            Map<String, ModuleObservation> observations,
            List<ResourceDeclaration> declarations,
            Map<String, ResourceHandler> handlerMap) {
        if (observations.isEmpty()) {
            return;
        }
        try {
            ResourceRegistrySnapshot completedSnapshot = repository.loadSnapshot(declarations);
            moduleSyncStatusRegistry.complete(
                    observations,
                    completedSnapshot,
                    declaration -> handlerMap.containsKey(declaration.getResourceType())
                            || targetDispatcher(declaration) != null);
        } catch (RuntimeException observationFailure) {
            recordDiagnosticUnknown(observations, "RESOURCE_SYNC_OBSERVATION_FAILED");
            log.warn("Mango resource synchronization succeeded but diagnostic observation failed",
                    observationFailure);
        }
    }

    private void invalidateDiagnosticStatus(String reasonCode) {
        try {
            moduleSyncStatusRegistry.invalidateObserved(reasonCode);
        } catch (RuntimeException observationFailure) {
            log.warn("Mango resource diagnostic invalidation failed: reasonCode={}",
                    reasonCode, observationFailure);
        }
    }

    private void recordDiagnosticRunning(
            Map<String, ModuleObservation> observations,
            Set<String> managedModuleCodes) {
        try {
            moduleSyncStatusRegistry.running(observations, managedModuleCodes);
        } catch (RuntimeException observationFailure) {
            log.warn("Mango resource diagnostic running state update failed", observationFailure);
        }
    }

    private void recordDiagnosticFailure(Map<String, ModuleObservation> observations, String reasonCode) {
        try {
            moduleSyncStatusRegistry.failed(observations, reasonCode);
        } catch (RuntimeException observationFailure) {
            log.warn("Mango resource diagnostic failure state update failed: reasonCode={}",
                    reasonCode, observationFailure);
        }
    }

    private void failObservedDiagnosticStatus(String reasonCode) {
        try {
            moduleSyncStatusRegistry.failedObserved(reasonCode);
        } catch (RuntimeException observationFailure) {
            log.warn("Mango resource observed diagnostic failure state update failed: reasonCode={}",
                    reasonCode, observationFailure);
        }
    }

    private void recordDiagnosticUnknown(Map<String, ModuleObservation> observations, String reasonCode) {
        try {
            moduleSyncStatusRegistry.unknown(observations, reasonCode);
        } catch (RuntimeException observationFailure) {
            log.warn("Mango resource diagnostic unknown state update failed: reasonCode={}",
                    reasonCode, observationFailure);
        }
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
            Require.isTrue(previous == null, ResourceCode.RESOURCE_CONFLICT,
                    "资源处理器重复: " + handler.resourceType());
        }
        return handlerMap;
    }

    private void validateDeclarations(List<ResourceDeclaration> declarations) {
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
        }
    }

    private void validateRegistryConflicts(List<ResourceDeclaration> declarations,
                                           ResourceRegistrySnapshot registrySnapshot) {
        for (ResourceDeclaration declaration : declarations) {
            ResourceRegistryRow rowByBizKey = registrySnapshot.findByTypeAndBizKey(
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
        Require.isTrue(declaration.getVersion() != null && declaration.getVersion() > 0,
                ResourceCode.RESOURCE_INVALID, "资源版本必须为正数: " + declaration.getId());
        Require.isTrue(declaration.getId().matches("\\d+"), ResourceCode.RESOURCE_INVALID,
                "资源ID必须是雪花算法数字字符串: " + declaration.getId());
    }

    private void requireText(String value, String message) {
        Require.notBlank(value, ResourceCode.RESOURCE_INVALID, message);
    }

    private void conflict(String message) {
        if (properties.isFailOnConflict()) {
            Require.isTrue(false, ResourceCode.RESOURCE_CONFLICT, message);
        }
        log.warn(message);
    }

    private void syncDeprecated(ResourceDeclaration declaration, boolean force,
                                ResourceRegistrySnapshot registrySnapshot) {
        String hash = hasher.hash(declaration);
        ResourceRegistryRow row = registrySnapshot.findByResourceId(declaration.getId());
        validateVersion(row, declaration);
        if (!force && isUnchanged(row, declaration, hash)) {
            return;
        }
        if (row != null && row.getSyncMode() != ResourceSyncMode.AUTO) {
            repository.insertSyncLog(row.getId(), "SKIP", "SKIPPED", "Resource sync mode is " + row.getSyncMode());
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

    private void syncOne(ResourceDeclaration declaration, Map<String, ResourceHandler> handlerMap, boolean force,
                         ResourceRegistrySnapshot registrySnapshot) {
        String hash = hasher.hash(declaration);
        ResourceRegistryRow row = registrySnapshot.findByResourceId(declaration.getId());
        validateVersion(row, declaration);
        if (!force && isUnchanged(row, declaration, hash)) {
            return;
        }
        if (row != null && row.getSyncMode() != ResourceSyncMode.AUTO) {
            repository.insertSyncLog(row.getId(), "SKIP", "SKIPPED", "Resource sync mode is " + row.getSyncMode());
            return;
        }
        ResourceSyncResult result;
        if (isDisabled(declaration)) {
            result = disableTarget(declaration, handlerMap);
        } else {
            result = upsertSingleTarget(declaration, handlerMap);
        }
        assertOperationCanContinue();
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
                                 boolean force, ResourceRegistrySnapshot registrySnapshot) {
        Map<String, List<ResourceDeclaration>> allDeclarationsByType = new LinkedHashMap<>();
        Map<String, List<ResourceDeclaration>> declarationsByType = new LinkedHashMap<>();
        for (ResourceDeclaration declaration : declarations) {
            prepareActiveDeclaration(
                    declaration, force, registrySnapshot, allDeclarationsByType, declarationsByType);
        }
        for (String resourceType : orderResourceTypesForSync(declarationsByType, handlerMap)) {
            assertOperationCanContinue();
            List<ResourceDeclaration> changedDeclarations = declarationsByType.get(resourceType);
            ResourceHandler handler = handlerMap.get(resourceType);
            Require.isTrue(handler != null || canDispatchAll(changedDeclarations),
                    ResourceCode.RESOURCE_NOT_FOUND, "未找到资源处理器: " + resourceType);
            List<ResourceDeclaration> completeBatch = allDeclarationsByType.get(resourceType);
            Map<String, ResourceSyncContext> syncContexts = syncContexts(
                    changedDeclarations, registrySnapshot);
            Map<String, ResourceSyncResult> results = syncActiveBatchByTarget(
                    handler,
                    changedDeclarations,
                    completeBatch,
                    syncContexts);
            for (ResourceDeclaration declaration : changedDeclarations) {
                assertOperationCanContinue();
                ResourceSyncResult result = results.get(declaration.getId());
                Require.notNull(result, ResourceCode.RESOURCE_SYNC_FAILED,
                        "资源处理器未返回同步结果: " + declaration.getId());
                saveActiveSyncResult(
                        declaration, result, syncContexts.get(declaration.getId()), registrySnapshot);
            }
        }
    }

    private void prepareActiveDeclaration(ResourceDeclaration declaration, boolean force,
                                          ResourceRegistrySnapshot registrySnapshot,
                                          Map<String, List<ResourceDeclaration>> allDeclarationsByType,
                                          Map<String, List<ResourceDeclaration>> declarationsByType) {
        assertOperationCanContinue();
        String hash = hasher.hash(declaration);
        ResourceRegistryRow row = registrySnapshot.findByResourceId(declaration.getId());
        validateVersion(row, declaration);
        ResourceDeclaration effectiveDeclaration = declaration;
        if (row != null) {
            effectiveDeclaration = withEffectiveSyncMode(declaration, row.getSyncMode());
        }
        allDeclarationsByType.computeIfAbsent(declaration.getResourceType(), key -> new ArrayList<>())
                .add(effectiveDeclaration);
        if (!force && isUnchanged(row, declaration, hash)) {
            return;
        }
        if (shouldPreserveInitOnlyTarget(row, declaration)) {
            skipInitOnlyTargetUpdate(row, declaration, hash);
            return;
        }
        if (row != null && row.getSyncMode() != ResourceSyncMode.AUTO) {
            repository.insertSyncLog(row.getId(), "SKIP", "SKIPPED",
                    "Resource sync mode is " + row.getSyncMode());
            return;
        }
        declarationsByType.computeIfAbsent(declaration.getResourceType(), key -> new ArrayList<>())
                .add(declaration);
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
        Require.isTrue(state != VisitState.VISITING, ResourceCode.RESOURCE_CONFLICT,
                "资源类型依赖存在循环: " + cyclePath(path, resourceType));
        states.put(resourceType, VisitState.VISITING);
        path.add(resourceType);
        ResourceHandler handler = handlerMap.get(resourceType);
        List<String> dependencyTypes = List.of();
        if (handler != null) {
            dependencyTypes = handler.dependsOnResourceTypes();
        }
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
        Require.isTrue(declaration.getVersion() >= row.getResourceVersion(), ResourceCode.RESOURCE_CONFLICT,
                "资源声明版本不允许回退: " + declaration.getId() + " current=" + row.getResourceVersion()
                        + ", incoming=" + declaration.getVersion());
    }

    private void doDeleteResource(String resourceId, boolean physical) {
        ResourceRegistryRow row = repository.findByResourceId(resourceId);
        Require.notNull(row, ResourceCode.RESOURCE_NOT_FOUND, "资源注册记录不存在: " + resourceId);
        Map<String, ResourceHandler> handlerMap = loadHandlers();
        ResourceDeclaration declaration = fromRow(row);
        ResourceSyncResult result;
        if (physical) {
            result = deleteTarget(declaration, handlerMap);
        } else {
            result = disableTarget(declaration, handlerMap);
        }
        assertOperationCanContinue();
        repository.updateStatus(row, ResourceStatus.REMOVED.name(), row.getSourceHash());
        String changeType = "DISABLE";
        if (physical) {
            changeType = "DELETE";
        }
        repository.insertSyncLog(row.getId(), changeType, "SUCCESS", result.getMessage());
        repository.insertChangeLog(row.getId(), changeType, toJson(row), toJson(declaration));
    }

    private void saveActiveSyncResult(ResourceDeclaration declaration, ResourceSyncResult result,
                                      ResourceSyncContext syncContext,
                                      ResourceRegistrySnapshot registrySnapshot) {
        String hash = hasher.hash(declaration);
        ResourceRegistryRow row = registrySnapshot.findByResourceId(declaration.getId());
        ResourceSyncDisposition disposition = result.getDisposition() == null
                ? ResourceSyncDisposition.APPLIED : result.getDisposition();
        if (disposition == ResourceSyncDisposition.PRESERVED || disposition == ResourceSyncDisposition.SKIPPED) {
            Require.notNull(row, ResourceCode.RESOURCE_SYNC_FAILED,
                    "新资源不能在未建立 Registry 记录时返回 " + disposition + ": " + declaration.getId());
            repository.insertSyncLog(row.getId(), disposition.name(), disposition.name(), result.getMessage());
            return;
        }
        Require.isTrue(disposition == ResourceSyncDisposition.APPLIED, ResourceCode.RESOURCE_SYNC_FAILED,
                "资源处理器不能返回失败结果，必须抛出异常: " + declaration.getId());
        LocalDateTime synchronizationTime = result.getSynchronizationTime();
        if (synchronizationTime != null) {
            Require.isTrue(syncContext != null
                            && synchronizationTime.equals(syncContext.getSynchronizationTime()),
                    ResourceCode.RESOURCE_SYNC_FAILED,
                    "资源处理器返回了非 Registry 分配的同步时间: " + declaration.getId());
        }
        if (row == null) {
            Long rowId = repository.insert(
                    declaration, hash, result.getTargetId(), result.getTargetTable(), synchronizationTime);
            repository.insertSyncLog(rowId, "CREATE", disposition.name(), result.getMessage());
            repository.insertChangeLog(rowId, "CREATE", null, toJson(declaration));
        } else {
            repository.update(
                    row, declaration, hash, result.getTargetId(), result.getTargetTable(), synchronizationTime);
            repository.insertSyncLog(row.getId(), "UPDATE", disposition.name(), result.getMessage());
            repository.insertChangeLog(row.getId(), "UPDATE", toJson(row), toJson(declaration));
        }
    }

    private Map<String, ResourceSyncContext> syncContexts(
            List<ResourceDeclaration> declarations,
            ResourceRegistrySnapshot registrySnapshot) {
        Map<String, ResourceSyncContext> contexts = new LinkedHashMap<>();
        for (ResourceDeclaration declaration : declarations) {
            ResourceRegistryRow row = registrySnapshot.findByResourceId(declaration.getId());
            LocalDateTime synchronizationTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
            contexts.put(declaration.getId(), ResourceSyncContext.of(
                    declaration.getId(),
                    row == null ? null : row.getLastSyncTime(),
                    synchronizationTime,
                    row == null ? null : row.getTargetId(),
                    row == null ? null : row.getTargetTable()));
        }
        return contexts;
    }

    private void skipInitOnlyTargetUpdate(ResourceRegistryRow row, ResourceDeclaration declaration, String hash) {
        if (isUnchanged(row, declaration, hash)) {
            return;
        }
        repository.update(row, declaration, hash, row.getTargetId(), row.getTargetTable());
        repository.insertSyncLog(row.getId(), "SKIP", "SKIPPED", "Resource sync mode is INIT_ONLY");
        repository.insertChangeLog(row.getId(), "UPDATE", toJson(row), toJson(declaration));
    }

    private boolean isUnchanged(ResourceRegistryRow row, ResourceDeclaration declaration, String hash) {
        return row != null
                && hash.equals(row.getSourceHash())
                && declaration.getStatus().name().equals(row.getStatus());
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
        List<String> managedModules = modules.stream().sorted().toList();
        for (ResourceRegistryRow row : repository.listBySourceAndModules(appCode, serviceCode, managedModules)) {
            assertOperationCanContinue();
            if (!seenResourceIds.contains(row.getResourceId()) && row.getSyncMode() == ResourceSyncMode.AUTO) {
                ResourceHandler handler = handlerMap.get(row.getResourceType());
                if (handler == null) {
                    ResourceDeclaration disabled = fromRow(row);
                    ResourceTargetDispatcher dispatcher = targetDispatcher(disabled);
                    Require.notNull(dispatcher, ResourceCode.RESOURCE_NOT_FOUND,
                            "缺失资源禁用时未找到处理器: " + row.getResourceType()
                                    + ", resourceId=" + row.getResourceId()
                                    + ", targetModule=" + row.getTargetModule());
                    ResourceSyncResult result = dispatcher.disable(disabled);
                    assertOperationCanContinue();
                    repository.updateStatus(row, ResourceStatus.REMOVED.name(), row.getSourceHash());
                    repository.insertSyncLog(row.getId(), "DISABLE", "SUCCESS", result.getMessage());
                    repository.insertChangeLog(row.getId(), "DISABLE", toJson(row), toJson(disabled));
                    continue;
                }
                ResourceDeclaration disabled = fromRow(row);
                ResourceSyncResult result = handlerInvoker.disable(handler, disabled);
                assertOperationCanContinue();
                repository.updateStatus(row, ResourceStatus.REMOVED.name(), row.getSourceHash());
                repository.insertSyncLog(row.getId(), "DISABLE", "SUCCESS", result.getMessage());
                repository.insertChangeLog(row.getId(), "DISABLE", toJson(row), toJson(disabled));
            }
        }
    }

    private Map<String, ResourceSyncResult> syncActiveBatchByTarget(
            ResourceHandler handler,
            List<ResourceDeclaration> changedDeclarations,
            List<ResourceDeclaration> completeBatch,
            Map<String, ResourceSyncContext> syncContexts) {
        Map<String, ResourceSyncResult> results = new HashMap<>();
        if (handler != null) {
            results.putAll(handlerInvoker.upsertBatchWithContext(
                    handler, changedDeclarations, completeBatch, syncContexts));
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
                results.putAll(dispatcher.upsertBatchWithContext(
                        declarations, completeBatch, syncContexts)));
        String missingResourceType = "unknown";
        if (!localDeclarations.isEmpty()) {
            missingResourceType = localDeclarations.getFirst().getResourceType();
        }
        Require.isTrue(localDeclarations.isEmpty(), ResourceCode.RESOURCE_NOT_FOUND,
                "未找到资源处理器: " + missingResourceType);
        return results;
    }

    private ResourceSyncResult upsertSingleTarget(ResourceDeclaration declaration, Map<String, ResourceHandler> handlerMap) {
        ResourceHandler handler = handlerMap.get(declaration.getResourceType());
        if (handler != null) {
            return handlerInvoker.upsert(handler, declaration);
        }
        ResourceTargetDispatcher dispatcher = targetDispatcher(declaration);
        Require.notNull(dispatcher, ResourceCode.RESOURCE_NOT_FOUND,
                "未找到资源处理器: " + declaration.getResourceType());
        Map<String, ResourceSyncResult> results = dispatcher.upsertBatch(List.of(declaration), List.of(declaration));
        ResourceSyncResult result = results.get(declaration.getId());
        Require.notNull(result, ResourceCode.RESOURCE_SYNC_FAILED,
                "资源目标调度器未返回同步结果: " + declaration.getId());
        return result;
    }

    private ResourceSyncResult disableTarget(ResourceDeclaration declaration, Map<String, ResourceHandler> handlerMap) {
        ResourceHandler handler = handlerMap.get(declaration.getResourceType());
        if (handler != null) {
            return handlerInvoker.disable(handler, declaration);
        }
        ResourceTargetDispatcher dispatcher = targetDispatcher(declaration);
        Require.notNull(dispatcher, ResourceCode.RESOURCE_NOT_FOUND,
                "未找到资源处理器: " + declaration.getResourceType());
        return dispatcher.disable(declaration);
    }

    private ResourceSyncResult deleteTarget(ResourceDeclaration declaration, Map<String, ResourceHandler> handlerMap) {
        ResourceHandler handler = handlerMap.get(declaration.getResourceType());
        if (handler != null) {
            return handlerInvoker.delete(handler, declaration);
        }
        ResourceTargetDispatcher dispatcher = targetDispatcher(declaration);
        Require.notNull(dispatcher, ResourceCode.RESOURCE_NOT_FOUND,
                "未找到资源处理器: " + declaration.getResourceType());
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
            declaration.putField("targetId", field(ResourceFieldType.LONG, row.getTargetId()));
        }
        if (StringUtils.hasText(row.getTargetTable())) {
            declaration.putField("targetTable", field(ResourceFieldType.STRING, row.getTargetTable()));
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
        } catch (JsonProcessingException exception) {
            log.error("Mango resource change log serialization failed", exception);
            Require.isTrue(false, ResourceCode.RESOURCE_SYNC_FAILED, "资源变更日志序列化失败");
            return "{}";
        }
    }

    private List<ResourceDeclaration> parseDeclarations(String declarations) {
        Require.notBlank(declarations, ResourceCode.RESOURCE_INVALID, "资源声明JSON不能为空");
        try {
            return objectMapper.readValue(declarations, new TypeReference<>() { });
        } catch (JsonProcessingException exception) {
            log.warn("Mango resource declarations JSON rejected: {}", exception.getOriginalMessage());
            Require.isTrue(false, ResourceCode.RESOURCE_INVALID, "资源声明JSON格式不正确");
            return List.of();
        }
    }

    private boolean beginOperation() {
        synchronized (lifecycleMonitor) {
            if (!running) {
                return false;
            }
            inFlightOperations++;
            return true;
        }
    }

    private void assertOperationCanContinue() {
        Require.isTrue(running, ResourceCode.RESOURCE_SYNC_FAILED,
                "Resource Registry operation stopped because the application is shutting down");
        lock.assertOwned();
    }

    private void endOperation() {
        synchronized (lifecycleMonitor) {
            inFlightOperations--;
            if (inFlightOperations == 0) {
                lifecycleMonitor.notifyAll();
            }
        }
    }

    @Override
    public void start() {
        synchronized (lifecycleMonitor) {
            running = true;
        }
    }

    @Override
    public void stop(Runnable callback) {
        synchronized (lifecycleMonitor) {
            running = false;
        }
        Thread shutdownWaiter = new Thread(
                () -> awaitInFlightOperations(callback), "mango-resource-shutdown-barrier");
        shutdownWaiter.setDaemon(true);
        shutdownWaiter.start();
    }

    @Override
    public void stop() {
        synchronized (lifecycleMonitor) {
            running = false;
        }
        awaitInFlightOperations(() -> { });
    }

    private void awaitInFlightOperations(Runnable callback) {
        long timeoutNanos = TimeUnit.SECONDS.toNanos(Math.max(1, properties.getShutdownWaitSeconds()));
        long deadline = System.nanoTime() + timeoutNanos;
        boolean interrupted = false;
        synchronized (lifecycleMonitor) {
            while (inFlightOperations > 0) {
                long remainingNanos = deadline - System.nanoTime();
                if (remainingNanos <= 0) {
                    log.warn("Mango resource shutdown barrier timed out: inFlight={}, waitSeconds={}",
                            inFlightOperations, properties.getShutdownWaitSeconds());
                    break;
                }
                try {
                    TimeUnit.NANOSECONDS.timedWait(lifecycleMonitor, remainingNanos);
                } catch (InterruptedException exception) {
                    interrupted = true;
                    log.warn("Mango resource shutdown barrier interrupted: inFlight={}", inFlightOperations);
                    break;
                }
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
        callback.run();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }

    private String resolveOwner() {
        if (StringUtils.hasText(properties.getInstanceId())) {
            return properties.getInstanceId();
        }
        return ManagementFactory.getRuntimeMXBean().getName();
    }
}
