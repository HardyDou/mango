package io.mango.resource.support.execution;

import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.support.ResourceHandler;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.support.model.ResourceField;
import io.mango.resource.support.model.ResourceSyncResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResourceHandlerInvokerTest {

    private final ResourceHandlerInvoker invoker = new ResourceHandlerInvoker();

    @BeforeEach
    void setUp() {
        MangoContextHolder.set(MangoContextSnapshot.empty().withTenantId("1"));
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    void upsert_tenantScopedHandler_switchesToDeclarationTenantAndRestoresPreviousContext() {
        RecordingHandler handler = new RecordingHandler();

        ResourceSyncResult result = invoker.upsert(handler, declaration("resource-a", "2"));

        assertThat(result.getTargetId()).isEqualTo(101L);
        assertThat(handler.invocations()).containsExactly("upsert:2:resource-a");
        assertThat(MangoContextHolder.tenantId()).isEqualTo("1");
    }

    @Test
    void upsert_handlerFails_restoresPreviousContextAndPropagatesFailure() {
        RecordingHandler handler = new RecordingHandler();
        handler.failOn("resource-failure");

        assertThatThrownBy(() -> invoker.upsert(handler, declaration("resource-failure", "2")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("handler failure: resource-failure");

        assertThat(handler.invocations()).containsExactly("upsert:2:resource-failure");
        assertThat(MangoContextHolder.tenantId()).isEqualTo("1");
    }

    @Test
    void upsertBatch_multipleTenants_groupsCallsAndScopesEachGroup() {
        RecordingHandler handler = new RecordingHandler();

        Map<String, ResourceSyncResult> results = invoker.upsertBatch(handler, List.of(
                declaration("resource-2a", "2"),
                declaration("resource-3a", "3"),
                declaration("resource-2b", "2")));

        assertThat(results.keySet()).containsExactly("resource-2a", "resource-2b", "resource-3a");
        assertThat(handler.invocations()).containsExactly(
                "batch:2:resource-2a,resource-2b",
                "batch:3:resource-3a");
        assertThat(MangoContextHolder.tenantId()).isEqualTo("1");
    }

    @Test
    void disableAndDelete_tenantScopedHandler_scopeBothOperationsAndRestoreContext() {
        RecordingHandler handler = new RecordingHandler();
        ResourceDeclaration declaration = declaration("resource-a", "2");

        invoker.disable(handler, declaration);
        invoker.delete(handler, declaration);

        assertThat(handler.invocations()).containsExactly(
                "disable:2:resource-a",
                "delete:2:resource-a");
        assertThat(MangoContextHolder.tenantId()).isEqualTo("1");
    }

    @Test
    void upsert_missingTenantField_rejectsDeclarationBeforeHandlerInvocation() {
        RecordingHandler handler = new RecordingHandler();
        ResourceDeclaration declaration = declaration("resource-a", "2");
        declaration.setFields(new LinkedHashMap<>());

        assertThatThrownBy(() -> invoker.upsert(handler, declaration))
                .hasMessageContaining("缺少字段 tenantId");

        assertThat(handler.invocations()).isEmpty();
        assertThat(MangoContextHolder.tenantId()).isEqualTo("1");
    }

    private ResourceDeclaration declaration(String id, String tenantId) {
        ResourceDeclaration declaration = new ResourceDeclaration();
        declaration.setId(id);
        declaration.setResourceType("TEST_TENANT_RESOURCE");
        declaration.setFields(new LinkedHashMap<>());
        ResourceField tenantField = new ResourceField();
        tenantField.setType(ResourceFieldType.STRING);
        tenantField.setValue(tenantId);
        declaration.putField("tenantId", tenantField);
        return declaration;
    }

    private static final class RecordingHandler implements ResourceHandler {

        private final List<String> invocations = new ArrayList<>();
        private String failingResourceId;

        @Override
        public String resourceType() {
            return "TEST_TENANT_RESOURCE";
        }

        @Override
        public String executionTenantField() {
            return "tenantId";
        }

        @Override
        public ResourceSyncResult upsert(ResourceDeclaration resource) {
            invocations.add("upsert:" + MangoContextHolder.tenantId() + ":" + resource.getId());
            if (resource.getId().equals(failingResourceId)) {
                throw new IllegalStateException("handler failure: " + resource.getId());
            }
            return ResourceSyncResult.of(101L, "test_resource", "upserted");
        }

        @Override
        public Map<String, ResourceSyncResult> upsertBatch(List<ResourceDeclaration> resources) {
            invocations.add("batch:" + MangoContextHolder.tenantId() + ":"
                    + resources.stream().map(ResourceDeclaration::getId).reduce((left, right) -> left + "," + right)
                    .orElse(""));
            Map<String, ResourceSyncResult> results = new LinkedHashMap<>();
            resources.forEach(resource -> results.put(resource.getId(),
                    ResourceSyncResult.of(101L, "test_resource", "upserted")));
            return results;
        }

        @Override
        public ResourceSyncResult disable(ResourceDeclaration resource) {
            invocations.add("disable:" + MangoContextHolder.tenantId() + ":" + resource.getId());
            return ResourceSyncResult.of(101L, "test_resource", "disabled");
        }

        @Override
        public ResourceSyncResult delete(ResourceDeclaration resource) {
            invocations.add("delete:" + MangoContextHolder.tenantId() + ":" + resource.getId());
            return ResourceSyncResult.of(101L, "test_resource", "deleted");
        }

        void failOn(String resourceId) {
            failingResourceId = resourceId;
        }

        List<String> invocations() {
            return List.copyOf(invocations);
        }
    }
}
