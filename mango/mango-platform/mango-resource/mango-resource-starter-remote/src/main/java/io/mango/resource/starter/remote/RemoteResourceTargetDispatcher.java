package io.mango.resource.starter.remote;

import io.mango.common.result.R;
import io.mango.infra.feign.starter.ModuleTargetResolver;
import io.mango.resource.api.ResourceTargetDispatcher;
import io.mango.resource.api.command.ExecuteResourceTargetCommand;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceSyncResult;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 基于模块信息解析的资源目标远程调度器。
 */
@RequiredArgsConstructor
public class RemoteResourceTargetDispatcher implements ResourceTargetDispatcher {

    private final ModuleTargetResolver moduleTargetResolver;
    private final ResourceTargetFeignClient targetFeignClient;

    @Override
    public boolean supports(String targetModule) {
        return resolve(targetModule).isPresent();
    }

    @Override
    public Map<String, ResourceSyncResult> upsertBatch(List<ResourceDeclaration> declarations,
                                                       List<ResourceDeclaration> completeBatch) {
        Map<String, ResourceSyncResult> results = new HashMap<>();
        Map<String, List<ResourceDeclaration>> declarationsByTarget = declarations.stream()
                .collect(Collectors.groupingBy(ResourceDeclaration::getTargetModule));
        for (Map.Entry<String, List<ResourceDeclaration>> entry : declarationsByTarget.entrySet()) {
            ExecuteResourceTargetCommand command = new ExecuteResourceTargetCommand();
            command.setDeclarations(entry.getValue());
            command.setCompleteBatch(completeBatchForTarget(completeBatch, entry.getKey()));
            results.putAll(requireSuccess(targetFeignClient.upsertBatch(targetUri(entry.getKey()), command)));
        }
        return results;
    }

    @Override
    public ResourceSyncResult disable(ResourceDeclaration declaration) {
        return requireSuccess(targetFeignClient.disable(targetUri(declaration.getTargetModule()), command(declaration)));
    }

    @Override
    public ResourceSyncResult delete(ResourceDeclaration declaration) {
        return requireSuccess(targetFeignClient.delete(targetUri(declaration.getTargetModule()), command(declaration)));
    }

    private URI targetUri(String targetModule) {
        return resolve(targetModule)
                .orElseThrow(() -> new IllegalStateException("No module info found: " + targetModule));
    }

    private Optional<URI> resolve(String targetModule) {
        if (!StringUtils.hasText(targetModule)) {
            return Optional.empty();
        }
        String normalized = targetModule.trim();
        Optional<URI> targetUri = moduleTargetResolver.resolveModuleBaseUri(normalized);
        if (targetUri.isPresent() || normalized.startsWith("mango-")) {
            return targetUri;
        }
        return moduleTargetResolver.resolveModuleBaseUri("mango-" + normalized);
    }

    private List<ResourceDeclaration> completeBatchForTarget(List<ResourceDeclaration> completeBatch, String targetModule) {
        if (completeBatch == null || completeBatch.isEmpty()) {
            return List.of();
        }
        List<ResourceDeclaration> targetBatch = new ArrayList<>();
        for (ResourceDeclaration declaration : completeBatch) {
            if (targetModule.equals(declaration.getTargetModule())) {
                targetBatch.add(declaration);
            }
        }
        return targetBatch;
    }

    private ExecuteResourceTargetCommand command(ResourceDeclaration declaration) {
        ExecuteResourceTargetCommand command = new ExecuteResourceTargetCommand();
        command.setDeclarations(List.of(declaration));
        command.setCompleteBatch(List.of(declaration));
        return command;
    }

    private <T> T requireSuccess(R<T> response) {
        if (response == null || !response.isSuccess()) {
            String message = response == null ? "empty response" : response.getMsg();
            throw new IllegalStateException("Remote resource target execution failed: " + message);
        }
        return response.getData();
    }
}
