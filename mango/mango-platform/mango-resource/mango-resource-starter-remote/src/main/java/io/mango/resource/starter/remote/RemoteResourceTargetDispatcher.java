package io.mango.resource.starter.remote;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.R;
import io.mango.common.result.Require;
import io.mango.infra.feign.starter.ModuleTargetResolver;
import io.mango.resource.api.command.ExecuteResourceTargetCommand;
import io.mango.resource.api.enums.ResourceCode;
import io.mango.resource.api.vo.ResourceBatchResultVO;
import io.mango.resource.api.vo.ResourceSyncResultVO;
import io.mango.resource.support.ResourceTargetDispatcher;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.support.model.ResourceSyncResult;
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
    private final ResourceTargetClient targetClient;
    private final ObjectMapper objectMapper;

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
            command.setDeclarations(toJson(entry.getValue()));
            command.setCompleteBatch(toJson(completeBatchForTarget(completeBatch, entry.getKey())));
            ResourceBatchResultVO response = requireSuccess(
                    targetClient.upsertBatch(targetUri(entry.getKey()), command));
            response.getEntries().forEach(result ->
                    results.put(result.getResourceId(), toResult(result.getResult())));
        }
        return results;
    }

    @Override
    public ResourceSyncResult disable(ResourceDeclaration declaration) {
        return toResult(requireSuccess(targetClient.disable(
                targetUri(declaration.getTargetModule()), command(declaration))));
    }

    @Override
    public ResourceSyncResult delete(ResourceDeclaration declaration) {
        return toResult(requireSuccess(targetClient.delete(
                targetUri(declaration.getTargetModule()), command(declaration))));
    }

    private URI targetUri(String targetModule) {
        URI targetUri = resolve(targetModule).orElse(null);
        Require.notNull(targetUri, ResourceCode.RESOURCE_NOT_FOUND,
                "未找到目标模块地址: " + targetModule);
        return targetUri;
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
        command.setDeclarations(toJson(List.of(declaration)));
        command.setCompleteBatch(toJson(List.of(declaration)));
        return command;
    }

    private <T> T requireSuccess(R<T> response) {
        if (response == null) {
            Require.isTrue(false, ResourceCode.RESOURCE_SYNC_FAILED,
                    "远程资源目标执行失败: empty response");
            return null;
        }
        if (!response.isSuccess()) {
            Require.isTrue(false, ResourceCode.RESOURCE_SYNC_FAILED,
                    "远程资源目标执行失败: " + response.getMsg());
        }
        return response.getData();
    }

    private String toJson(List<ResourceDeclaration> declarations) {
        try {
            return objectMapper.writeValueAsString(declarations);
        } catch (JsonProcessingException exception) {
            Require.isTrue(false, ResourceCode.RESOURCE_INVALID, "资源声明序列化失败");
            return "[]";
        }
    }

    private ResourceSyncResult toResult(ResourceSyncResultVO result) {
        Require.notNull(result, ResourceCode.RESOURCE_SYNC_FAILED, "远程资源目标未返回同步结果");
        return ResourceSyncResult.of(result.getTargetId(), result.getTargetTable(), result.getMessage());
    }
}
