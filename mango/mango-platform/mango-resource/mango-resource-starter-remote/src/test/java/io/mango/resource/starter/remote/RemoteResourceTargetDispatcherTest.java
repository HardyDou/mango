package io.mango.resource.starter.remote;

import io.mango.common.result.R;
import io.mango.infra.feign.starter.ModuleTargetResolver;
import io.mango.infra.module.api.ModuleInfo;
import io.mango.resource.api.command.ExecuteResourceTargetCommand;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceSyncResult;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class RemoteResourceTargetDispatcherTest {

    @Test
    void targetFeignClient_uriOverloadsUseReverseResourceTargetPath() throws Exception {
        FeignClient feignClient = ResourceTargetFeignClient.class.getAnnotation(FeignClient.class);
        assertThat(feignClient.path()).isEmpty();
        assertThat(feignClient.url()).isEmpty();
        assertThat(postMapping("upsertBatch").value())
                .containsExactly("/_resource/targets/upsert-batch");
        assertThat(postMapping("disable").value())
                .containsExactly("/_resource/targets/disable");
        assertThat(postMapping("delete").value())
                .containsExactly("/_resource/targets/delete");
    }

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
                        Optional.of(new ModuleInfo(moduleName, "authorization-app", "/admin", "/authorization", "test"))),
                feignClient);
        ResourceDeclaration declaration = declaration("1", "authorization");

        Map<String, ResourceSyncResult> results = dispatcher.upsertBatch(List.of(declaration), List.of(declaration));

        assertThat(feignClient.targetUri.get()).isEqualTo(URI.create("http://authorization-app/admin"));
        assertThat(feignClient.command.get().getDeclarations()).containsExactly(declaration);
        assertThat(results).containsKey("1");
    }

    @Test
    void upsertBatch_splitsDeclarationsByTargetModule() {
        RecordingFeignClient feignClient = new RecordingFeignClient();
        RemoteResourceTargetDispatcher dispatcher = new RemoteResourceTargetDispatcher(
                new ModuleTargetResolver(moduleName ->
                        Optional.of(new ModuleInfo(moduleName, moduleName + "-app", "", "/" + moduleName, "test"))),
                feignClient);
        ResourceDeclaration authorization = declaration("1", "authorization");
        ResourceDeclaration notice = declaration("2", "notice");

        Map<String, ResourceSyncResult> results = dispatcher.upsertBatch(
                List.of(authorization, notice),
                List.of(authorization, notice));

        assertThat(feignClient.calls).hasSize(2);
        assertThat(feignClient.calls)
                .extracting(call -> call.targetUri)
                .containsExactlyInAnyOrder(
                        URI.create("http://authorization-app"),
                        URI.create("http://notice-app"));
        assertThat(feignClient.calls)
                .allSatisfy(call -> assertThat(call.command.getCompleteBatch()).hasSize(1));
        assertThat(results).containsOnlyKeys("1", "2");
    }

    private static ResourceDeclaration declaration(String id, String targetModule) {
        ResourceDeclaration declaration = new ResourceDeclaration();
        declaration.setId(id);
        declaration.setResourceType("AUTH_MENU");
        declaration.setTargetModule(targetModule);
        return declaration;
    }

    private static PostMapping postMapping(String methodName) throws NoSuchMethodException {
        Method method = ResourceTargetFeignClient.class.getMethod(
                methodName, URI.class, ExecuteResourceTargetCommand.class);
        return method.getAnnotation(PostMapping.class);
    }

    private static class RecordingFeignClient implements ResourceTargetFeignClient {

        private final AtomicReference<URI> targetUri = new AtomicReference<>();
        private final AtomicReference<ExecuteResourceTargetCommand> command = new AtomicReference<>();
        private final List<Call> calls = new ArrayList<>();

        @Override
        public R<Map<String, ResourceSyncResult>> upsertBatch(ExecuteResourceTargetCommand command) {
            return R.ok(Map.of());
        }

        @Override
        public R<Map<String, ResourceSyncResult>> upsertBatch(URI targetUri, ExecuteResourceTargetCommand command) {
            this.targetUri.set(targetUri);
            this.command.set(command);
            calls.add(new Call(targetUri, command));
            ResourceDeclaration declaration = command.getDeclarations().get(0);
            return R.ok(Map.of(declaration.getId(), ResourceSyncResult.of(100L, "authorization_app_module", "ok")));
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

    private record Call(URI targetUri, ExecuteResourceTargetCommand command) {
    }
}
