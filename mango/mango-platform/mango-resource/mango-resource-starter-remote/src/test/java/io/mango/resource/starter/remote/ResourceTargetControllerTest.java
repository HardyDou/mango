package io.mango.resource.starter.remote;

import io.mango.common.result.R;
import io.mango.resource.api.ResourceHandler;
import io.mango.resource.api.command.ExecuteResourceTargetCommand;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceHandlerSpec;
import io.mango.resource.api.model.ResourceSyncResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceTargetControllerTest {

    @Test
    void upsertBatch_whenHandlerRequiresCompleteBatch_passesCompleteBatchOnce() {
        BatchHandler handler = new BatchHandler();
        ResourceTargetController controller = new ResourceTargetController(new SingleHandlerProvider(handler));
        ExecuteResourceTargetCommand command = new ExecuteResourceTargetCommand();
        ResourceDeclaration changed = declaration("1");
        ResourceDeclaration unchanged = declaration("2");
        command.setDeclarations(List.of(changed));
        command.setCompleteBatch(List.of(changed, unchanged));

        R<Map<String, ResourceSyncResult>> response = controller.upsertBatch(command);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).containsOnlyKeys("1", "2");
        assertThat(handler.receivedBatch.get()).containsExactly(changed, unchanged);
    }

    private static ResourceDeclaration declaration(String id) {
        ResourceDeclaration declaration = new ResourceDeclaration();
        declaration.setId(id);
        declaration.setResourceType("AUTH_MENU");
        return declaration;
    }

    private static class BatchHandler implements ResourceHandler {

        private final AtomicReference<List<ResourceDeclaration>> receivedBatch = new AtomicReference<>();

        @Override
        public String resourceType() {
            return "AUTH_MENU";
        }

        @Override
        public ResourceHandlerSpec spec() {
            return ResourceHandler.super.spec();
        }

        @Override
        public ResourceSyncResult upsert(ResourceDeclaration resource) {
            return ResourceSyncResult.of(Long.valueOf(resource.getId()), "authorization_app_module", "ok");
        }

        @Override
        public boolean requiresCompleteBatch() {
            return true;
        }

        @Override
        public Map<String, ResourceSyncResult> upsertBatch(List<ResourceDeclaration> resources) {
            receivedBatch.set(resources);
            Map<String, ResourceSyncResult> results = new LinkedHashMap<>();
            for (ResourceDeclaration resource : resources) {
                results.put(resource.getId(), upsert(resource));
            }
            return results;
        }

        @Override
        public ResourceSyncResult disable(ResourceDeclaration resource) {
            return ResourceSyncResult.of(Long.valueOf(resource.getId()), "authorization_app_module", "disabled");
        }
    }

    private static class SingleHandlerProvider implements ObjectProvider<ResourceHandler> {

        private final ResourceHandler handler;

        SingleHandlerProvider(ResourceHandler handler) {
            this.handler = handler;
        }

        @Override
        public ResourceHandler getObject(Object... args) {
            return handler;
        }

        @Override
        public ResourceHandler getIfAvailable() {
            return handler;
        }

        @Override
        public ResourceHandler getIfUnique() {
            return handler;
        }

        @Override
        public ResourceHandler getObject() {
            return handler;
        }

        @Override
        public Iterator<ResourceHandler> iterator() {
            return List.of(handler).iterator();
        }

        @Override
        public Stream<ResourceHandler> stream() {
            return Stream.of(handler);
        }
    }
}
