package io.mango.resource.starter.remote;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.R;
import io.mango.infra.feign.starter.ModuleTargetResolver;
import io.mango.infra.module.api.ModuleInfo;
import io.mango.resource.api.command.ExecuteResourceTargetCommand;
import io.mango.resource.api.vo.ResourceBatchEntryVO;
import io.mango.resource.api.vo.ResourceBatchResultVO;
import io.mango.resource.api.vo.ResourceSyncResultVO;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.support.model.ResourceSyncResult;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RemoteResourceTargetDispatcherTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void supports_whenTargetModuleUsesShortName_resolvesMangoModuleName() {
        RemoteResourceTargetDispatcher dispatcher = dispatcher(new RecordingTargetClient(), moduleName ->
                "mango-authorization".equals(moduleName)
                        ? Optional.of(new ModuleInfo(moduleName, "authorization-app", "", "/authorization", "test"))
                        : Optional.empty());

        assertThat(dispatcher.supports("authorization")).isTrue();
    }

    @Test
    void upsertBatch_routesJsonProtocolToResolvedTargetService() throws Exception {
        RecordingTargetClient client = new RecordingTargetClient();
        RemoteResourceTargetDispatcher dispatcher = dispatcher(client, moduleName ->
                Optional.of(new ModuleInfo(moduleName, "authorization-app", "/admin", "/authorization", "test")));
        ResourceDeclaration declaration = declaration("1", "authorization");

        Map<String, ResourceSyncResult> results = dispatcher.upsertBatch(List.of(declaration), List.of(declaration));

        assertThat(client.calls).hasSize(1);
        assertThat(client.calls.getFirst().targetUri()).isEqualTo(URI.create("http://authorization-app/admin"));
        ResourceDeclaration transmitted = objectMapper.readValue(
                client.calls.getFirst().command().getDeclarations(), ResourceDeclaration[].class)[0];
        assertThat(transmitted.getId()).isEqualTo("1");
        assertThat(results).containsOnlyKeys("1");
    }

    @Test
    void upsertBatch_splitsDeclarationsAndCompleteBatchByTargetModule() throws Exception {
        RecordingTargetClient client = new RecordingTargetClient();
        RemoteResourceTargetDispatcher dispatcher = dispatcher(client, moduleName ->
                Optional.of(new ModuleInfo(moduleName, moduleName + "-app", "", "/" + moduleName, "test")));
        ResourceDeclaration authorization = declaration("1", "authorization");
        ResourceDeclaration notice = declaration("2", "notice");

        Map<String, ResourceSyncResult> results = dispatcher.upsertBatch(
                List.of(authorization, notice), List.of(authorization, notice));

        assertThat(client.calls).hasSize(2);
        for (Call call : client.calls) {
            assertThat(objectMapper.readValue(call.command().getCompleteBatch(), ResourceDeclaration[].class))
                    .hasSize(1);
        }
        assertThat(results).containsOnlyKeys("1", "2");
    }

    private RemoteResourceTargetDispatcher dispatcher(RecordingTargetClient client,
                                                       io.mango.infra.module.api.ModuleInfoResolver resolver) {
        return new RemoteResourceTargetDispatcher(new ModuleTargetResolver(resolver), client, objectMapper);
    }

    private static ResourceDeclaration declaration(String id, String targetModule) {
        ResourceDeclaration declaration = new ResourceDeclaration();
        declaration.setId(id);
        declaration.setResourceType("AUTH_MENU");
        declaration.setTargetModule(targetModule);
        return declaration;
    }

    private static class RecordingTargetClient implements ResourceTargetClient {

        private final List<Call> calls = new ArrayList<>();

        @Override
        public R<ResourceBatchResultVO> upsertBatch(URI targetUri, ExecuteResourceTargetCommand command) {
            calls.add(new Call(targetUri, command));
            ResourceBatchEntryVO entry = new ResourceBatchEntryVO();
            entry.setResourceId(command.getDeclarations().contains("\"id\":\"1\"") ? "1" : "2");
            entry.setResult(result("ok"));
            ResourceBatchResultVO batch = new ResourceBatchResultVO();
            batch.setEntries(List.of(entry));
            return R.ok(batch);
        }

        @Override
        public R<ResourceSyncResultVO> disable(URI targetUri, ExecuteResourceTargetCommand command) {
            return R.ok(result("disabled"));
        }

        @Override
        public R<ResourceSyncResultVO> delete(URI targetUri, ExecuteResourceTargetCommand command) {
            return R.ok(result("deleted"));
        }

        private ResourceSyncResultVO result(String message) {
            ResourceSyncResultVO result = new ResourceSyncResultVO();
            result.setTargetId(100L);
            result.setTargetTable("authorization_app_module");
            result.setMessage(message);
            return result;
        }
    }

    private record Call(URI targetUri, ExecuteResourceTargetCommand command) {
    }
}
