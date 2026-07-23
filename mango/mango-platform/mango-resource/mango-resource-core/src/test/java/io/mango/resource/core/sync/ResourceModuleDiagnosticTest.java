package io.mango.resource.core.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.infra.module.api.diagnostic.ModuleConditionStatus;
import io.mango.infra.module.api.diagnostic.ModuleDiagnosticCondition;
import io.mango.infra.module.api.diagnostic.ModuleDiagnosticProfile;
import io.mango.infra.module.api.diagnostic.ModuleDiagnosticRequest;
import io.mango.infra.module.api.diagnostic.ModuleInstallation;
import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.core.diagnostic.ResourceModuleDiagnosticContributor;
import io.mango.resource.core.diagnostic.ResourceModuleSyncState;
import io.mango.resource.core.diagnostic.ResourceModuleSyncStatusRegistry;
import io.mango.resource.core.diagnostic.ResourceModuleSyncStatusRegistry.ModuleObservation;
import io.mango.resource.support.config.ResourceRegistryProperties;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.support.model.ResourceField;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceModuleDiagnosticTest {

    private final ResourceContentHasher hasher = new ResourceContentHasher(new ObjectMapper());
    private final ResourceModuleSyncStatusRegistry registry = new ResourceModuleSyncStatusRegistry(hasher);

    @Test
    void currentFingerprintAndMatchingRegistryRowsProducePass() {
        ResourceDeclaration declaration = menuDeclaration();
        Map<String, ModuleObservation> observations = registry.observations(List.of(declaration));
        ResourceRegistryRow row = matchingRow(declaration);

        registry.running(observations);
        registry.complete(observations, ResourceRegistryRepository.ResourceRegistrySnapshot.of(
                List.of(row), List.of(row)));

        assertThat(registry.resolve("link")).get().satisfies(status -> {
            assertThat(status.state()).isEqualTo(ResourceModuleSyncState.APPLIED);
            assertThat(status.fingerprint()).hasSize(64);
            assertThat(status.pageRequirements()).containsExactly("link/categories/index", "link/items/index");
        });
        assertThat(condition(true).status()).isEqualTo(ModuleConditionStatus.PASS);
    }

    @Test
    void historicalRowWithWrongCurrentHashCannotProducePass() {
        ResourceDeclaration declaration = menuDeclaration();
        Map<String, ModuleObservation> observations = registry.observations(List.of(declaration));
        ResourceRegistryRow staleRow = matchingRow(declaration);
        staleRow.setSourceHash("stale-historical-hash");

        registry.complete(observations, ResourceRegistryRepository.ResourceRegistrySnapshot.of(
                List.of(staleRow), List.of(staleRow)));

        assertThat(registry.resolve("link")).get().satisfies(status -> {
            assertThat(status.state()).isEqualTo(ResourceModuleSyncState.FAILED);
            assertThat(status.reasonCode()).isEqualTo("REGISTRY_CONSUMPTION_MISMATCH");
        });
        assertThat(condition(true).status()).isEqualTo(ModuleConditionStatus.FAIL);
    }

    @Test
    void disabledOrUnobservedResourceSyncCannotProducePass() {
        assertThat(condition(true)).satisfies(condition -> {
            assertThat(condition.status()).isEqualTo(ModuleConditionStatus.UNKNOWN);
            assertThat(condition.reasonCode()).isEqualTo("CURRENT_SYNC_NOT_OBSERVED");
        });

        registry.running(registry.observations(List.of(menuDeclaration())));
        registry.invalidateObserved("RESOURCE_SYNC_LOCK_NOT_ACQUIRED");
        assertThat(condition(true)).satisfies(condition -> {
            assertThat(condition.status()).isEqualTo(ModuleConditionStatus.UNKNOWN);
            assertThat(condition.reasonCode()).isEqualTo("RESOURCE_SYNC_LOCK_NOT_ACQUIRED");
        });

        assertThat(condition(false)).satisfies(condition -> {
            assertThat(condition.status()).isEqualTo(ModuleConditionStatus.SKIPPED);
            assertThat(condition.reasonCode()).isEqualTo("RESOURCE_SYNC_DISABLED");
        });
    }

    @Test
    void authorizationRequirementsJoinMenuResourceModuleAndApiRuntimeModuleAcrossProviders() {
        ResourceDeclaration menu = authorizationMenuDeclaration();
        ResourceDeclaration api = apiDeclarationFromDifferentProvider();
        Map<String, ModuleObservation> observations = registry.observations(List.of(menu, api));

        registry.running(observations);
        registry.complete(observations, ResourceRegistryRepository.ResourceRegistrySnapshot.of(
                List.of(matchingRow(menu), matchingRow(api)),
                List.of(matchingRow(menu), matchingRow(api))));

        assertThat(registry.authorizationRequirements("link", "mango-link", "internal-admin"))
                .satisfies(requirements -> {
            assertThat(requirements.sourcesApplied()).isTrue();
            assertThat(requirements.menus()).singleElement().satisfies(menuRequirement -> {
                assertThat(menuRequirement.appCode()).isEqualTo("internal-admin");
                assertThat(menuRequirement.moduleCode()).isEqualTo("link");
                assertThat(menuRequirement.menuCode()).isEqualTo("link.items");
                assertThat(menuRequirement.component()).isEqualTo("link/items/index");
                assertThat(menuRequirement.apiCodes()).containsExactly("link:item:view");
            });
            assertThat(requirements.apis()).singleElement().satisfies(apiRequirement -> {
                assertThat(apiRequirement.moduleName()).isEqualTo("mango-link");
                assertThat(apiRequirement.httpMethod()).isEqualTo("GET");
                assertThat(apiRequirement.pathPattern()).isEqualTo("/api/link/items");
                assertThat(apiRequirement.resourceCode()).isEqualTo("GET:/api/link/items");
                assertThat(apiRequirement.permissionCode()).isEqualTo("link:item:view");
                assertThat(apiRequirement.accessMode()).isEqualTo("PERMISSION");
            });
        });
    }

    @Test
    void requirementsAreScopedToRequestedApp() {
        ResourceDeclaration menu = authorizationMenuDeclaration();
        ResourceDeclaration api = apiDeclarationFromDifferentProvider();
        Map<String, ModuleObservation> observations = registry.observations(List.of(menu, api));
        registry.running(observations);
        registry.complete(observations, ResourceRegistryRepository.ResourceRegistrySnapshot.of(
                List.of(matchingRow(menu), matchingRow(api)),
                List.of(matchingRow(menu), matchingRow(api))));

        assertThat(registry.authorizationRequirements("link", "mango-link", "customer-portal").menus())
                .isEmpty();
    }

    @Test
    void missingConsumerOrTargetCoordinatesCannotProducePass() {
        ResourceDeclaration declaration = menuDeclaration();
        Map<String, ModuleObservation> observations = registry.observations(List.of(declaration));
        ResourceRegistryRow row = matchingRow(declaration);

        registry.complete(observations, ResourceRegistryRepository.ResourceRegistrySnapshot.of(
                List.of(row), List.of(row)), ignored -> false);
        assertThat(registry.resolve("link")).get().satisfies(status -> {
            assertThat(status.state()).isEqualTo(ResourceModuleSyncState.FAILED);
            assertThat(status.consumerResolvedCount()).isZero();
        });

        row.setTargetId(null);
        registry.complete(observations, ResourceRegistryRepository.ResourceRegistrySnapshot.of(
                List.of(row), List.of(row)), ignored -> true);
        assertThat(registry.resolve("link")).get().satisfies(status -> {
            assertThat(status.state()).isEqualTo(ResourceModuleSyncState.FAILED);
            assertThat(status.targetEvidenceCount()).isZero();
        });
    }

    private ModuleDiagnosticCondition condition(boolean enabled) {
        ResourceRegistryProperties properties = new ResourceRegistryProperties();
        properties.setEnabled(enabled);
        ResourceModuleDiagnosticContributor contributor = new ResourceModuleDiagnosticContributor(registry, properties);
        ModuleDiagnosticRequest request = new ModuleDiagnosticRequest(
                "mango-link",
                "internal-admin",
                ModuleDiagnosticProfile.ADMIN_MODULE_RUNTIME_V1,
                Map.of(ModuleInstallation.RESOURCE_MODULE_ATTRIBUTE, "link"));
        return contributor.contribute(request).iterator().next();
    }

    private ResourceDeclaration menuDeclaration() {
        ResourceDeclaration declaration = new ResourceDeclaration();
        declaration.setId("1900000000000000001");
        declaration.setVersion(1);
        declaration.setResourceType("AUTH_MENU");
        declaration.setModuleCode("link");
        declaration.setBizKey("link.menu");
        declaration.setTargetModule("authorization");

        Map<String, Object> child = new LinkedHashMap<>();
        child.put("component", "link/items/index");
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("component", "link/categories/index");
        root.put("children", List.of(child));
        ResourceField menus = new ResourceField();
        menus.setType(ResourceFieldType.JSON);
        menus.setValue(List.of(root));
        declaration.putField("menus", menus);
        return declaration;
    }

    private ResourceDeclaration authorizationMenuDeclaration() {
        ResourceDeclaration declaration = new ResourceDeclaration();
        declaration.setId("1900000000000000011");
        declaration.setVersion(1);
        declaration.setResourceType("AUTH_MENU");
        declaration.setModuleCode("link");
        declaration.setBizKey("link.authorization.menu");
        declaration.setTargetModule("authorization");
        declaration.putField("appCode", textField("internal-admin"));
        declaration.putField("moduleCode", textField("link"));

        Map<String, Object> menu = new LinkedHashMap<>();
        menu.put("menuCode", "link.items");
        menu.put("component", "link/items/index");
        menu.put("apiCodes", List.of("link:item:view"));
        ResourceField menus = new ResourceField();
        menus.setType(ResourceFieldType.JSON);
        menus.setValue(List.of(menu));
        declaration.putField("menus", menus);
        return declaration;
    }

    private ResourceDeclaration apiDeclarationFromDifferentProvider() {
        ResourceDeclaration declaration = new ResourceDeclaration();
        declaration.setId("1900000000000000012");
        declaration.setVersion(1);
        declaration.setResourceType("API_RESOURCE");
        declaration.setModuleCode("link-api-provider");
        declaration.setBizKey("link.authorization.api");
        declaration.setTargetModule("authorization");
        declaration.putField("moduleName", textField("mango-link"));
        declaration.putField("httpMethod", textField("GET"));
        declaration.putField("pathPattern", textField("/api/link/items"));
        declaration.putField("resourceCode", textField("GET:/api/link/items"));
        declaration.putField("permissionCode", textField("link:item:view"));
        declaration.putField("accessMode", textField("PERMISSION"));
        return declaration;
    }

    private ResourceField textField(String value) {
        ResourceField field = new ResourceField();
        field.setType(ResourceFieldType.STRING);
        field.setValue(value);
        return field;
    }

    private ResourceRegistryRow matchingRow(ResourceDeclaration declaration) {
        ResourceRegistryRow row = new ResourceRegistryRow();
        row.setResourceId(declaration.getId());
        row.setResourceVersion(declaration.getVersion());
        row.setResourceType(declaration.getResourceType());
        row.setModuleCode(declaration.getModuleCode());
        row.setBizKey(declaration.getBizKey());
        row.setTargetModule(declaration.getTargetModule());
        row.setTargetId(9001L);
        row.setTargetTable("authorization_resource");
        row.setSourceHash(hasher.hash(declaration));
        row.setStatus(declaration.getStatus().name());
        return row;
    }
}
