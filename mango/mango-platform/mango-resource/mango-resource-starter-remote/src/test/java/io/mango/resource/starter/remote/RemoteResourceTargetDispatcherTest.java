package io.mango.resource.starter.remote;

import io.mango.common.result.R;
import io.mango.infra.feign.starter.ModuleTargetResolver;
import io.mango.infra.module.api.ModuleInfo;
import io.mango.resource.api.command.ExecuteResourceTargetCommand;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceSyncResult;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class RemoteResourceTargetDispatcherTest {

    @Test
    void supports_whenTargetModuleUsesShortName_resolvesMangoModuleName() {
        RemoteResourceTargetDispatcher dispatcher = new RemoteResourceTargetDispatcher(
                new ModuleTargetResolver(moduleName -> "mango-authorization".equals(moduleName)
                        ? Optional.of(new ModuleInfo(moduleName, "authorization-app", "", "/authorization", "test"))
                        : Optional.empty()),
                new RecordingFeignClient());

        assertThat(dispatcher.supports("authorization")).isTrue();
    }

    @Test
    void upsertBatch_routesToResolvedTargetService() {
        RecordingFeignClient feignClient = new RecordingFeignClient();
        RemoteResourceTargetDispatcher dispatcher = new RemoteResourceTargetDispatcher(
                new ModuleTargetResolver(moduleName ->
                        Optional.of(new ModuleInfo(moduleName, "authorization-app", "", "/authorization", "test"))),
                feignClient);
        ResourceDeclaration declaration = declaration("1", "authorization");

        Map<String, ResourceSyncResult> results = dispatcher.upsertBatch(List.of(declaration), List.of(declaration));

        assertThat(feignClient.targetUri.get()).isEqualTo(URI.create("http://authorization-app"));
        assertThat(feignClient.command.get().getDeclarations()).containsExactly(declaration);
        assertThat(results).containsKey("1");
    }

    private static ResourceDeclaration declaration(String id, String targetModule) {
        ResourceDeclaration declaration = new ResourceDeclaration();
        declaration.setId(id);
        declaration.setResourceType("AUTH_MENU");
        declaration.setTargetModule(targetModule);
        return declaration;
    }

    private static class RecordingFeignClient implements ResourceTargetFeignClient {

        private final AtomicReference<URI> targetUri = new AtomicReference<>();
        private final AtomicReference<ExecuteResourceTargetCommand> command = new AtomicReference<>();

        @Override
        public R<Map<String, ResourceSyncResult>> upsertBatch(ExecuteResourceTargetCommand command) {
            return R.ok(Map.of());
        }

        @Override
        public R<Map<String, ResourceSyncResult>> upsertBatch(URI targetUri, ExecuteResourceTargetCommand command) {
            this.targetUri.set(targetUri);
            this.command.set(command);
            return R.ok(Map.of("1", ResourceSyncResult.of(100L, "authorization_app_module", "ok")));
        }

        @Override
        public R<ResourceSyncResult> disable(ExecuteResourceTargetCommand command) {
            return R.ok(ResourceSyncResult.of(100L, "authorization_app_module", "disabled"));
        }

        @Override
        public R<ResourceSyncResult> disable(URI targetUri, ExecuteResourceTargetCommand command) {
            return R.ok(ResourceSyncResult.of(100L, "authorization_app_module", "disabled"));
        }

        @Override
        public R<ResourceSyncResult> delete(ExecuteResourceTargetCommand command) {
            return R.ok(ResourceSyncResult.of(100L, "authorization_app_module", "deleted"));
        }

        @Override
        public R<ResourceSyncResult> delete(URI targetUri, ExecuteResourceTargetCommand command) {
            return R.ok(ResourceSyncResult.of(100L, "authorization_app_module", "deleted"));
        }
    }
}
