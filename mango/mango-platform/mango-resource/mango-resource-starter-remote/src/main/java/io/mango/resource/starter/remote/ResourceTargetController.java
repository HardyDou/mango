package io.mango.resource.starter.remote;

import io.mango.common.result.R;
import io.mango.resource.api.ResourceHandler;
import io.mango.resource.api.ResourceTargetApi;
import io.mango.resource.api.command.ExecuteResourceTargetCommand;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceSyncResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 资源目标模块反向执行入口。
 */
@RestController
@RequestMapping("/_resource/targets")
@RequiredArgsConstructor
public class ResourceTargetController implements ResourceTargetApi {

    private final ObjectProvider<ResourceHandler> handlers;

    @Override
    @PostMapping("/upsert-batch")
    public R<Map<String, ResourceSyncResult>> upsertBatch(
            @Valid @RequestBody ExecuteResourceTargetCommand command) {
        Map<String, ResourceSyncResult> results = new HashMap<>();
        Map<String, List<ResourceDeclaration>> declarationsByType = command.getDeclarations().stream()
                .collect(java.util.stream.Collectors.groupingBy(ResourceDeclaration::getResourceType));
        for (Map.Entry<String, List<ResourceDeclaration>> entry : declarationsByType.entrySet()) {
            ResourceHandler handler = findHandler(entry.getKey());
            List<ResourceDeclaration> handlerDeclarations = handler.requiresCompleteBatch()
                    ? command.getCompleteBatch()
                    : entry.getValue();
            results.putAll(handler.upsertBatch(handlerDeclarations));
        }
        return R.ok(results);
    }

    @Override
    @PostMapping("/disable")
    public R<ResourceSyncResult> disable(@Valid @RequestBody ExecuteResourceTargetCommand command) {
        ResourceDeclaration declaration = singleDeclaration(command);
        return R.ok(findHandler(declaration.getResourceType()).disable(declaration));
    }

    @Override
    @PostMapping("/delete")
    public R<ResourceSyncResult> delete(@Valid @RequestBody ExecuteResourceTargetCommand command) {
        ResourceDeclaration declaration = singleDeclaration(command);
        return R.ok(findHandler(declaration.getResourceType()).delete(declaration));
    }

    private ResourceHandler findHandler(String resourceType) {
        for (ResourceHandler handler : handlers) {
            if (handler.resourceType().equals(resourceType)) {
                return handler;
            }
        }
        throw new IllegalStateException("No resource handler found: " + resourceType);
    }

    private ResourceDeclaration singleDeclaration(ExecuteResourceTargetCommand command) {
        if (command.getDeclarations().size() != 1) {
            throw new IllegalStateException("Exactly one resource declaration is required");
        }
        return command.getDeclarations().get(0);
    }
}
