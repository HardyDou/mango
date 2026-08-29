package io.mango.resource.support.execution;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.Require;
import io.mango.resource.api.command.ExecuteResourceTargetCommand;
import io.mango.resource.api.command.ResourceSyncContextCommand;
import io.mango.resource.api.enums.ResourceCode;
import io.mango.resource.api.vo.ResourceBatchEntryVO;
import io.mango.resource.api.vo.ResourceBatchResultVO;
import io.mango.resource.api.vo.ResourceSyncResultVO;
import io.mango.resource.support.ResourceHandler;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.support.model.ResourceSyncContext;
import io.mango.resource.support.model.ResourceSyncResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 资源目标执行端口的纯 Java 默认实现。
 */
public class DefaultResourceTargetExecutor implements ResourceTargetExecutor {

    private static final TypeReference<List<ResourceDeclaration>> DECLARATION_LIST_TYPE = new TypeReference<>() { };

    private final ObjectMapper objectMapper;
    private final Supplier<? extends Collection<ResourceHandler>> handlerSupplier;
    private final ResourceHandlerInvoker handlerInvoker = new ResourceHandlerInvoker();
    private volatile List<ResourceHandler> handlers;

    public DefaultResourceTargetExecutor(ObjectMapper objectMapper, Collection<ResourceHandler> handlers) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        List<ResourceHandler> resolvedHandlers = List.copyOf(handlers);
        this.handlerSupplier = () -> resolvedHandlers;
        this.handlers = resolvedHandlers;
    }

    /**
     * Creates an executor that resolves handlers only when a target operation first executes.
     *
     * @param objectMapper JSON protocol mapper
     * @param handlerSupplier ordered handler source
     */
    public DefaultResourceTargetExecutor(
            ObjectMapper objectMapper,
            Supplier<? extends Collection<ResourceHandler>> handlerSupplier) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.handlerSupplier = Objects.requireNonNull(handlerSupplier, "handlerSupplier");
    }

    @Override
    public ResourceBatchResultVO upsertBatch(ExecuteResourceTargetCommand command) {
        Require.notNull(command, ResourceCode.RESOURCE_INVALID, "资源目标端执行命令不能为空");
        List<ResourceDeclaration> declarations = parse(command.getDeclarations());
        List<ResourceDeclaration> completeBatch = parse(command.getCompleteBatch());
        Map<String, ResourceSyncContext> syncContexts = syncContexts(command.getSyncContexts());
        Map<String, List<ResourceDeclaration>> declarationsByType = groupByResourceType(declarations);
        List<ResourceBatchEntryVO> entries = new ArrayList<>();
        declarationsByType.forEach((resourceType, typedDeclarations) -> {
            ResourceHandler handler = findHandler(resourceType);
            List<ResourceDeclaration> typedCompleteBatch = completeBatch.stream()
                    .filter(declaration -> resourceType.equals(declaration.getResourceType()))
                    .toList();
            Map<String, ResourceSyncResult> handlerResults = handlerInvoker.upsertBatchWithContext(
                    handler, typedDeclarations, typedCompleteBatch, syncContexts);
            Require.notNull(handlerResults, ResourceCode.RESOURCE_SYNC_FAILED,
                    "资源处理器未返回批量同步结果: " + resourceType);
            handlerResults.forEach((resourceId, result) -> entries.add(toBatchEntry(resourceId, result)));
        });
        ResourceBatchResultVO result = new ResourceBatchResultVO();
        result.setEntries(entries);
        return result;
    }

    @Override
    public ResourceSyncResultVO disable(ExecuteResourceTargetCommand command) {
        Require.notNull(command, ResourceCode.RESOURCE_INVALID, "资源目标端执行命令不能为空");
        ResourceDeclaration declaration = singleDeclaration(command);
        ResourceHandler handler = findHandler(declaration.getResourceType());
        return toResultVO(handlerInvoker.disable(handler, declaration));
    }

    @Override
    public ResourceSyncResultVO delete(ExecuteResourceTargetCommand command) {
        Require.notNull(command, ResourceCode.RESOURCE_INVALID, "资源目标端执行命令不能为空");
        ResourceDeclaration declaration = singleDeclaration(command);
        ResourceHandler handler = findHandler(declaration.getResourceType());
        return toResultVO(handlerInvoker.delete(handler, declaration));
    }

    private Map<String, List<ResourceDeclaration>> groupByResourceType(List<ResourceDeclaration> declarations) {
        Map<String, List<ResourceDeclaration>> grouped = new LinkedHashMap<>();
        declarations.forEach(declaration -> grouped
                .computeIfAbsent(declaration.getResourceType(), ignored -> new ArrayList<>())
                .add(declaration));
        return grouped;
    }

    private ResourceHandler findHandler(String resourceType) {
        ResourceHandler handler = handlers().stream()
                .filter(candidate -> candidate.resourceType().equals(resourceType))
                .findFirst()
                .orElse(null);
        Require.notNull(handler, ResourceCode.RESOURCE_NOT_FOUND,
                "未找到资源处理器: " + resourceType);
        return handler;
    }

    private List<ResourceHandler> handlers() {
        List<ResourceHandler> resolvedHandlers = handlers;
        if (resolvedHandlers != null) {
            return resolvedHandlers;
        }
        synchronized (this) {
            resolvedHandlers = handlers;
            if (resolvedHandlers == null) {
                Collection<ResourceHandler> suppliedHandlers = Objects.requireNonNull(
                        handlerSupplier.get(), "handlerSupplier returned null");
                resolvedHandlers = List.copyOf(suppliedHandlers);
                handlers = resolvedHandlers;
            }
        }
        return resolvedHandlers;
    }

    private ResourceDeclaration singleDeclaration(ExecuteResourceTargetCommand command) {
        List<ResourceDeclaration> declarations = parse(command.getDeclarations());
        Require.isTrue(declarations.size() == 1, ResourceCode.RESOURCE_INVALID,
                "单资源操作必须且只能提交一条资源声明");
        return declarations.getFirst();
    }

    private List<ResourceDeclaration> parse(String json) {
        Require.notBlank(json, ResourceCode.RESOURCE_INVALID, "资源声明JSON不能为空");
        try {
            List<ResourceDeclaration> declarations = objectMapper.readValue(json, DECLARATION_LIST_TYPE);
            Require.notNull(declarations, ResourceCode.RESOURCE_INVALID, "资源声明JSON必须是数组");
            return declarations;
        } catch (JsonProcessingException exception) {
            Require.isTrue(false, ResourceCode.RESOURCE_INVALID, "资源声明JSON格式不正确");
            return List.of();
        }
    }

    private ResourceBatchEntryVO toBatchEntry(String resourceId, ResourceSyncResult result) {
        ResourceBatchEntryVO entry = new ResourceBatchEntryVO();
        entry.setResourceId(resourceId);
        entry.setResult(toResultVO(result));
        return entry;
    }

    private ResourceSyncResultVO toResultVO(ResourceSyncResult result) {
        Require.notNull(result, ResourceCode.RESOURCE_SYNC_FAILED, "资源处理器未返回同步结果");
        ResourceSyncResultVO vo = new ResourceSyncResultVO();
        vo.setTargetId(result.getTargetId());
        vo.setTargetTable(result.getTargetTable());
        vo.setMessage(result.getMessage());
        vo.setDisposition(result.getDisposition());
        vo.setSynchronizationTime(result.getSynchronizationTime());
        return vo;
    }

    private Map<String, ResourceSyncContext> syncContexts(List<ResourceSyncContextCommand> commands) {
        Map<String, ResourceSyncContext> contexts = new LinkedHashMap<>();
        for (ResourceSyncContextCommand command : commands) {
            Require.notBlank(command.getResourceId(), ResourceCode.RESOURCE_INVALID,
                    "资源同步上下文ID不能为空");
            Require.notNull(command.getSynchronizationTime(), ResourceCode.RESOURCE_INVALID,
                    "资源同步上下文时间不能为空: " + command.getResourceId());
            ResourceSyncContext previous = contexts.put(command.getResourceId(), ResourceSyncContext.of(
                    command.getResourceId(), command.getPreviousSyncTime(), command.getSynchronizationTime(),
                    command.getTargetId(), command.getTargetTable()));
            Require.isTrue(previous == null, ResourceCode.RESOURCE_CONFLICT,
                    "资源同步上下文ID重复: " + command.getResourceId());
        }
        return contexts;
    }
}
