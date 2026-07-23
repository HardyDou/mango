package io.mango.resource.core.diagnostic;

import io.mango.resource.api.ResourceAuthorizationRequirementsProvider;
import io.mango.resource.api.ResourceAuthorizationRequirementsProvider.ApiRequirement;
import io.mango.resource.api.ResourceAuthorizationRequirementsProvider.AuthorizationRequirements;
import io.mango.resource.api.ResourceAuthorizationRequirementsProvider.MenuRequirement;
import io.mango.resource.core.sync.ResourceContentHasher;
import io.mango.resource.core.sync.ResourceRegistryRepository.ResourceRegistrySnapshot;
import io.mango.resource.core.sync.ResourceRegistryRow;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.support.model.ResourceField;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Process-local status written only by the real Resource synchronization flow.
 */
public class ResourceModuleSyncStatusRegistry implements ResourceAuthorizationRequirementsProvider {

    private final Map<String, ResourceModuleSyncStatus> statuses = new ConcurrentHashMap<>();
    private final Map<String, ModuleObservation> currentObservations = new ConcurrentHashMap<>();
    private final ResourceContentHasher hasher;

    public ResourceModuleSyncStatusRegistry(ResourceContentHasher hasher) {
        this.hasher = hasher;
    }

    public Map<String, ModuleObservation> observations(Collection<ResourceDeclaration> declarations) {
        Map<String, List<ResourceDeclaration>> byModule = new LinkedHashMap<>();
        if (declarations != null) {
            declarations.stream()
                    .filter(declaration -> declaration.getModuleCode() != null
                            && !declaration.getModuleCode().isBlank())
                    .forEach(declaration -> byModule.computeIfAbsent(
                            declaration.getModuleCode().trim(), ignored -> new ArrayList<>()).add(declaration));
        }
        Map<String, ModuleObservation> observations = new LinkedHashMap<>();
        byModule.forEach((module, moduleDeclarations) -> observations.put(
                module,
                new ModuleObservation(
                        fingerprint(moduleDeclarations),
                        List.copyOf(moduleDeclarations),
                        pageRequirements(moduleDeclarations))));
        return Map.copyOf(observations);
    }

    public void running(Map<String, ModuleObservation> observations) {
        running(observations, observations.keySet());
    }

    public void running(Map<String, ModuleObservation> observations, Collection<String> managedModules) {
        if (managedModules != null) {
            managedModules.stream()
                    .filter(module -> module != null && !module.isBlank())
                    .map(String::trim)
                    .filter(module -> !observations.containsKey(module))
                    .forEach(module -> {
                        currentObservations.remove(module);
                        statuses.remove(module);
                    });
        }
        currentObservations.putAll(observations);
        observations.forEach((module, observation) -> statuses.put(module, status(
                module, ResourceModuleSyncState.RUNNING, observation, "RESOURCE_SYNC_RUNNING")));
    }

    public void complete(Map<String, ModuleObservation> observations, ResourceRegistrySnapshot snapshot) {
        complete(observations, snapshot, declaration -> true);
    }

    public void complete(
            Map<String, ModuleObservation> observations,
            ResourceRegistrySnapshot snapshot,
            Predicate<ResourceDeclaration> consumerResolver) {
        observations.forEach((module, observation) -> {
            int consumerResolvedCount = (int) observation.declarations().stream()
                    .filter(consumerResolver)
                    .count();
            int targetEvidenceCount = (int) observation.declarations().stream()
                    .filter(declaration -> hasTargetEvidence(snapshot.findByResourceId(declaration.getId())))
                    .count();
            boolean matches = observation.declarations().stream().allMatch(declaration -> registryMatches(
                    declaration,
                    snapshot.findByResourceId(declaration.getId()),
                    consumerResolver.test(declaration)));
            statuses.put(module, status(
                    module,
                    matches ? ResourceModuleSyncState.APPLIED : ResourceModuleSyncState.FAILED,
                    observation,
                    matches ? "CURRENT_DECLARATIONS_APPLIED" : "REGISTRY_CONSUMPTION_MISMATCH",
                    consumerResolvedCount,
                    targetEvidenceCount));
        });
    }

    public void failed(Map<String, ModuleObservation> observations, String reasonCode) {
        observations.forEach((module, observation) -> statuses.put(module, status(
                module, ResourceModuleSyncState.FAILED, observation, reasonCode)));
    }

    public void failedObserved(String reasonCode) {
        statuses.replaceAll((module, current) -> new ResourceModuleSyncStatus(
                module,
                ResourceModuleSyncState.FAILED,
                current.fingerprint(),
                current.declarationCount(),
                current.consumerResolvedCount(),
                current.targetEvidenceCount(),
                current.pageRequirements(),
                Instant.now(),
                reasonCode));
    }

    public void unknown(Map<String, ModuleObservation> observations, String reasonCode) {
        observations.forEach((module, observation) -> statuses.put(module, status(
                module, ResourceModuleSyncState.UNKNOWN, observation, reasonCode)));
    }

    public void invalidateObserved(String reasonCode) {
        statuses.replaceAll((module, current) -> new ResourceModuleSyncStatus(
                module,
                ResourceModuleSyncState.UNKNOWN,
                current.fingerprint(),
                current.declarationCount(),
                current.consumerResolvedCount(),
                current.targetEvidenceCount(),
                current.pageRequirements(),
                Instant.now(),
                reasonCode));
    }

    public Optional<ResourceModuleSyncStatus> resolve(String moduleCode) {
        if (moduleCode == null || moduleCode.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(statuses.get(moduleCode.trim()));
    }

    /**
     * Resolves current declaration requirements for Authorization's internal materialization check.
     * API paths remain internal to the contributor and must not be copied into endpoint evidence.
     */
    @Override
    public AuthorizationRequirements authorizationRequirements(
            String resourceModule,
            String runtimeModule,
            String appCode) {
        if (resourceModule == null || resourceModule.isBlank()
                || runtimeModule == null || runtimeModule.isBlank()
                || appCode == null || appCode.isBlank()) {
            return AuthorizationRequirements.empty();
        }
        String normalizedResourceModule = resourceModule.trim();
        String normalizedRuntimeModule = runtimeModule.trim();
        String normalizedAppCode = appCode.trim();
        List<MenuRequirement> menus = new ArrayList<>();
        List<ApiRequirement> apis = new ArrayList<>();
        Set<String> sourceModules = ConcurrentHashMap.newKeySet();
        currentObservations.forEach((sourceModule, observation) -> {
            for (ResourceDeclaration declaration : observation.declarations()) {
                if ("AUTH_MENU".equals(declaration.getResourceType())
                        && normalizedResourceModule.equals(declaration.getModuleCode())
                        && normalizedAppCode.equals(stringField(declaration, "appCode"))) {
                    collectMenuRequirements(declaration, menus);
                    sourceModules.add(sourceModule);
                }
                if ("API_RESOURCE".equals(declaration.getResourceType())
                        && normalizedRuntimeModule.equals(stringField(declaration, "moduleName"))) {
                    collectApiRequirement(declaration, apis);
                    sourceModules.add(sourceModule);
                }
            }
        });
        boolean sourcesApplied = !sourceModules.isEmpty() && sourceModules.stream()
                .allMatch(module -> resolve(module)
                        .map(status -> status.state() == ResourceModuleSyncState.APPLIED)
                        .orElse(false));
        return new AuthorizationRequirements(
                menus.stream().distinct().sorted(Comparator.comparing(MenuRequirement::menuCode)).toList(),
                apis.stream().distinct().sorted(Comparator
                        .comparing(ApiRequirement::httpMethod)
                        .thenComparing(ApiRequirement::pathPattern)).toList(),
                sourcesApplied);
    }

    private ResourceModuleSyncStatus status(
            String module,
            ResourceModuleSyncState state,
            ModuleObservation observation,
            String reasonCode) {
        return status(module, state, observation, reasonCode, 0, 0);
    }

    private ResourceModuleSyncStatus status(
            String module,
            ResourceModuleSyncState state,
            ModuleObservation observation,
            String reasonCode,
            int consumerResolvedCount,
            int targetEvidenceCount) {
        return new ResourceModuleSyncStatus(
                module,
                state,
                observation.fingerprint(),
                observation.declarations().size(),
                consumerResolvedCount,
                targetEvidenceCount,
                observation.pageRequirements(),
                Instant.now(),
                reasonCode);
    }

    private boolean registryMatches(
            ResourceDeclaration declaration,
            ResourceRegistryRow row,
            boolean consumerResolved) {
        return row != null
                && consumerResolved
                && hasTargetEvidence(row)
                && hasher.hash(declaration).equals(row.getSourceHash())
                && declaration.getStatus().name().equals(row.getStatus())
                && declaration.getModuleCode().equals(row.getModuleCode())
                && declaration.getTargetModule().equals(row.getTargetModule());
    }

    private boolean hasTargetEvidence(ResourceRegistryRow row) {
        return row != null && row.getTargetId() != null
                && row.getTargetTable() != null && !row.getTargetTable().isBlank();
    }

    private String fingerprint(List<ResourceDeclaration> declarations) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            declarations.stream()
                    .sorted(Comparator.comparing(ResourceDeclaration::getId))
                    .map(hasher::hash)
                    .forEach(hash -> digest.update((hash + "\n").getBytes(StandardCharsets.UTF_8)));
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private List<String> pageRequirements(List<ResourceDeclaration> declarations) {
        List<String> requirements = new ArrayList<>();
        declarations.stream()
                .filter(declaration -> "AUTH_MENU".equals(declaration.getResourceType()))
                .map(ResourceDeclaration::getFields)
                .map(fields -> fields.get("menus"))
                .filter(java.util.Objects::nonNull)
                .map(ResourceField::getValue)
                .forEach(value -> collectComponents(value, requirements));
        return requirements.stream().distinct().sorted().toList();
    }

    private void collectComponents(Object value, List<String> requirements) {
        if (value instanceof Collection<?> collection) {
            collection.forEach(item -> collectComponents(item, requirements));
            return;
        }
        if (!(value instanceof Map<?, ?> map)) {
            return;
        }
        Object component = map.get("component");
        if (component instanceof String text && !text.isBlank()) {
            requirements.add(text.trim());
        }
        Object children = map.get("children");
        if (children != null) {
            collectComponents(children, requirements);
        }
    }

    private void collectMenuRequirements(ResourceDeclaration declaration, List<MenuRequirement> requirements) {
        String appCode = stringField(declaration, "appCode");
        String moduleCode = stringField(declaration, "moduleCode");
        if (moduleCode == null || moduleCode.isBlank()) {
            moduleCode = declaration.getModuleCode();
        }
        ResourceField menus = declaration.getFields().get("menus");
        if (appCode == null || appCode.isBlank() || moduleCode == null || moduleCode.isBlank() || menus == null) {
            return;
        }
        collectMenuRequirements(menus.getValue(), appCode, moduleCode, requirements);
    }

    private void collectMenuRequirements(
            Object value,
            String appCode,
            String moduleCode,
            List<MenuRequirement> requirements) {
        if (value instanceof Collection<?> collection) {
            collection.forEach(item -> collectMenuRequirements(item, appCode, moduleCode, requirements));
            return;
        }
        if (!(value instanceof Map<?, ?> map)) {
            return;
        }
        String menuCode = text(map.get("menuCode"));
        if (menuCode != null) {
            requirements.add(new MenuRequirement(
                    appCode,
                    moduleCode,
                    menuCode,
                    text(map.get("component")),
                    stringList(map.get("apiCodes")),
                    intValue(map.get("status"), 1)));
        }
        collectMenuRequirements(map.get("children"), appCode, moduleCode, requirements);
    }

    private void collectApiRequirement(ResourceDeclaration declaration, List<ApiRequirement> requirements) {
        String moduleName = stringField(declaration, "moduleName");
        String httpMethod = stringField(declaration, "httpMethod");
        String pathPattern = stringField(declaration, "pathPattern");
        if (moduleName == null || httpMethod == null || pathPattern == null) {
            return;
        }
        requirements.add(new ApiRequirement(
                moduleName,
                httpMethod,
                pathPattern,
                stringField(declaration, "resourceCode"),
                stringField(declaration, "permissionCode"),
                stringField(declaration, "accessMode"),
                declaration.getStatus() == io.mango.resource.api.enums.ResourceStatus.ACTIVE ? 1 : 0));
    }

    private List<String> stringList(Object value) {
        if (value instanceof Collection<?> collection) {
            return collection.stream()
                    .map(this::text)
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .sorted()
                    .toList();
        }
        String item = text(value);
        return item == null ? List.of() : List.of(item);
    }

    private String stringField(ResourceDeclaration declaration, String fieldName) {
        ResourceField field = declaration.getFields().get(fieldName);
        return field == null ? null : text(field.getValue());
    }

    private String text(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private int intValue(Object value, int defaultValue) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? defaultValue : Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    /** Current declarations and derived safe requirements for one module. */
    public record ModuleObservation(
            String fingerprint,
            List<ResourceDeclaration> declarations,
            List<String> pageRequirements) {
    }

}
