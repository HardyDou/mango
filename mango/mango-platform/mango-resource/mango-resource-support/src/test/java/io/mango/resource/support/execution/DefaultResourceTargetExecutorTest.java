package io.mango.resource.support.execution;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.resource.api.command.ExecuteResourceTargetCommand;
import io.mango.resource.api.vo.ResourceBatchResultVO;
import io.mango.resource.api.vo.ResourceSyncResultVO;
import io.mango.resource.support.ResourceHandler;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.support.model.ResourceSyncResult;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultResourceTargetExecutorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void upsertBatch_whenHandlerNeedsCompleteBatch_usesSameTypeCompleteDeclarations() throws Exception {
        RecordingHandler handler = new RecordingHandler(true);
        ResourceDeclaration changed = declaration("1", "AUTH_MENU");
        ResourceDeclaration unchanged = declaration("2", "AUTH_MENU");
        ResourceDeclaration anotherType = declaration("3", "AUTH_ROLE");

        ResourceBatchResultVO result = executor(handler).upsertBatch(command(
                List.of(changed), List.of(changed, unchanged, anotherType)));

        assertThat(handler.receivedBatch.get()).containsExactly(changed, unchanged);
        assertThat(result.getEntries()).extracting("resourceId").containsExactly("1", "2");
    }

    @Test
    void upsertBatch_whenHandlerDoesNotNeedCompleteBatch_usesChangedDeclarationsOnly() throws Exception {
        RecordingHandler handler = new RecordingHandler(false);
        ResourceDeclaration changed = declaration("1", "AUTH_MENU");
        ResourceDeclaration unchanged = declaration("2", "AUTH_MENU");

        executor(handler).upsertBatch(command(List.of(changed), List.of(changed, unchanged)));

        assertThat(handler.receivedBatch.get()).containsExactly(changed);
    }

    @Test
    void disable_whenMultipleDeclarationsSubmitted_rejectsInvalidProtocol() throws Exception {
        ExecuteResourceTargetCommand command = command(
                List.of(declaration("1", "AUTH_MENU"), declaration("2", "AUTH_MENU")), List.of());

        assertThatThrownBy(() -> executor(new RecordingHandler(false)).disable(command))
                .hasMessageContaining("单资源操作必须且只能提交一条资源声明");
    }

    @Test
    void upsertBatch_whenHandlerDoesNotExist_reportsBusinessError() throws Exception {
        ExecuteResourceTargetCommand command = command(List.of(declaration("1", "UNKNOWN")), List.of());

        assertThatThrownBy(() -> executor(new RecordingHandler(false)).upsertBatch(command))
                .hasMessageContaining("未找到资源处理器");
    }

    @Test
    void upsertBatch_whenJsonIsInvalid_reportsProtocolError() {
        ExecuteResourceTargetCommand command = new ExecuteResourceTargetCommand();
        command.setDeclarations("not-json");
        command.setCompleteBatch("[]");

        assertThatThrownBy(() -> executor(new RecordingHandler(false)).upsertBatch(command))
                .hasMessageContaining("资源声明JSON格式不正确");
    }

    @Test
    void delete_delegatesToHandlerPhysicalDelete() throws Exception {
        RecordingHandler handler = new RecordingHandler(false);

        ResourceSyncResultVO result = executor(handler).delete(
                command(List.of(declaration("1", "AUTH_MENU")), List.of()));

        assertThat(handler.deleteCalled.get()).isTrue();
        assertThat(result.getMessage()).isEqualTo("deleted");
    }

    @Test
    void targetOperations_resolveLazyHandlersOnceOnFirstExecution() throws Exception {
        AtomicInteger resolutions = new AtomicInteger();
        RecordingHandler handler = new RecordingHandler(false);
        DefaultResourceTargetExecutor executor = new DefaultResourceTargetExecutor(objectMapper, () -> {
            resolutions.incrementAndGet();
            return List.of(handler);
        });

        assertThat(resolutions).hasValue(0);

        executor.upsertBatch(command(List.of(declaration("1", "AUTH_MENU")), List.of()));
        executor.disable(command(List.of(declaration("1", "AUTH_MENU")), List.of()));

        assertThat(resolutions).hasValue(1);
    }

    private DefaultResourceTargetExecutor executor(ResourceHandler handler) {
        return new DefaultResourceTargetExecutor(objectMapper, List.of(handler));
    }

    private ExecuteResourceTargetCommand command(List<ResourceDeclaration> declarations,
                                                  List<ResourceDeclaration> completeBatch) throws Exception {
        ExecuteResourceTargetCommand command = new ExecuteResourceTargetCommand();
        command.setDeclarations(objectMapper.writeValueAsString(declarations));
        command.setCompleteBatch(objectMapper.writeValueAsString(completeBatch));
        return command;
    }

    private ResourceDeclaration declaration(String id, String resourceType) {
        ResourceDeclaration declaration = new ResourceDeclaration();
        declaration.setId(id);
        declaration.setResourceType(resourceType);
        return declaration;
    }

    private static class RecordingHandler implements ResourceHandler {

        private final boolean completeBatchRequired;
        private final AtomicReference<List<ResourceDeclaration>> receivedBatch = new AtomicReference<>();
        private final AtomicBoolean deleteCalled = new AtomicBoolean();

        private RecordingHandler(boolean completeBatchRequired) {
            this.completeBatchRequired = completeBatchRequired;
        }

        @Override
        public String resourceType() {
            return "AUTH_MENU";
        }

        @Override
        public ResourceSyncResult upsert(ResourceDeclaration resource) {
            return ResourceSyncResult.of(Long.valueOf(resource.getId()), "authorization_app_module", "ok");
        }

        @Override
        public boolean requiresCompleteBatch() {
            return completeBatchRequired;
        }

        @Override
        public Map<String, ResourceSyncResult> upsertBatch(List<ResourceDeclaration> resources) {
            receivedBatch.set(resources);
            Map<String, ResourceSyncResult> results = new LinkedHashMap<>();
            resources.forEach(resource -> results.put(resource.getId(), upsert(resource)));
            return results;
        }

        @Override
        public ResourceSyncResult disable(ResourceDeclaration resource) {
            return ResourceSyncResult.of(Long.valueOf(resource.getId()), "authorization_app_module", "disabled");
        }

        @Override
        public ResourceSyncResult delete(ResourceDeclaration resource) {
            deleteCalled.set(true);
            return ResourceSyncResult.of(Long.valueOf(resource.getId()), "authorization_app_module", "deleted");
        }
    }
}
