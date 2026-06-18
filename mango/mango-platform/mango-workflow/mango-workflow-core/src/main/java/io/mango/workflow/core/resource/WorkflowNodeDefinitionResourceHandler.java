package io.mango.workflow.core.resource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.resource.api.ResourceHandler;
import io.mango.resource.api.ResourceTypes;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceHandlerSpec;
import io.mango.resource.api.model.ResourceSyncResult;
import io.mango.workflow.core.entity.WorkflowNodeDefinition;
import io.mango.workflow.core.mapper.WorkflowNodeDefinitionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 流程节点定义资源处理器。
 */
@Component
@RequiredArgsConstructor
public class WorkflowNodeDefinitionResourceHandler implements ResourceHandler {

    private static final String TARGET_TABLE = "workflow_node_definition";
    private static final Long DEFAULT_TENANT_ID = 1L;
    private static final int ENABLED = 1;
    private static final int DISABLED = 0;

    private final WorkflowNodeDefinitionMapper nodeDefinitionMapper;

    @Override
    public String resourceType() {
        return ResourceTypes.WORKFLOW_NODE_DEFINITION;
    }

    @Override
    public ResourceHandlerSpec spec() {
        return ResourceHandlerSpec.builder()
                .resourceType(resourceType())
                .requiredField("nodeDefinitionCode")
                .requiredField("nodeType")
                .requiredField("nodeName")
                .requiredField("categoryCode")
                .requiredField("categoryName")
                .requiredField("bpmnType")
                .requiredField("executionType")
                .fieldDescription("nodeDefinitionId", "节点定义稳定 ID，可选；不填时使用资源 ID。")
                .fieldDescription("tenantId", "租户 ID，默认 1。")
                .fieldDescription("nodeDefinitionCode", "节点定义编码，租户内唯一。")
                .fieldDescription("nodeType", "节点类型。")
                .fieldDescription("nodeName", "节点名称。")
                .fieldDescription("categoryCode", "节点分类编码。")
                .fieldDescription("categoryName", "节点分类名称。")
                .fieldDescription("description", "节点说明。")
                .fieldDescription("bpmnType", "底层 BPMN 类型。")
                .fieldDescription("executionType", "执行类型。")
                .fieldDescription("color", "节点颜色。")
                .fieldDescription("icon", "节点图标。")
                .fieldDescription("propertySchema", "属性配置 JSON Schema。")
                .fieldDescription("defaultProperties", "默认属性 JSON。")
                .fieldDescription("sort", "排序号，默认 0。")
                .fieldDescription("status", "状态：1 启用，0 停用。")
                .build();
    }

    @Override
    public ResourceSyncResult upsert(ResourceDeclaration resource) {
        Payload payload = Payload.from(resource);
        WorkflowNodeDefinition entity = find(payload.tenantId(), payload.nodeDefinitionCode());
        if (entity == null) {
            entity = new WorkflowNodeDefinition();
            entity.setId(payload.nodeDefinitionId());
            apply(entity, payload);
            nodeDefinitionMapper.insert(entity);
        } else {
            apply(entity, payload);
            nodeDefinitionMapper.updateById(entity);
        }
        return ResourceSyncResult.of(entity.getId(), TARGET_TABLE,
                "Workflow node definition synced: " + entity.getNodeDefinitionCode());
    }

    @Override
    public ResourceSyncResult disable(ResourceDeclaration resource) {
        WorkflowNodeDefinition entity = resolve(resource);
        if (entity == null) {
            return ResourceSyncResult.of(null, TARGET_TABLE, "Workflow node definition not found");
        }
        entity.setStatus(DISABLED);
        entity.setUpdatedTime(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        nodeDefinitionMapper.updateById(entity);
        return ResourceSyncResult.of(entity.getId(), TARGET_TABLE,
                "Workflow node definition disabled: " + entity.getNodeDefinitionCode());
    }

    @Override
    public ResourceSyncResult delete(ResourceDeclaration resource) {
        WorkflowNodeDefinition entity = resolve(resource);
        if (entity == null) {
            return ResourceSyncResult.of(null, TARGET_TABLE, "Workflow node definition not found");
        }
        nodeDefinitionMapper.deleteById(entity.getId());
        return ResourceSyncResult.of(entity.getId(), TARGET_TABLE,
                "Workflow node definition deleted: " + entity.getNodeDefinitionCode());
    }

    private void apply(WorkflowNodeDefinition entity, Payload payload) {
        LocalDateTime now = LocalDateTime.now();
        entity.setTenantId(payload.tenantId());
        entity.setNodeDefinitionCode(payload.nodeDefinitionCode());
        entity.setNodeType(payload.nodeType());
        entity.setNodeName(payload.nodeName());
        entity.setCategoryCode(payload.categoryCode());
        entity.setCategoryName(payload.categoryName());
        entity.setDescription(payload.description());
        entity.setBpmnType(payload.bpmnType());
        entity.setExecutionType(payload.executionType());
        entity.setColor(payload.color());
        entity.setIcon(payload.icon());
        entity.setPropertySchema(payload.propertySchema());
        entity.setDefaultProperties(payload.defaultProperties());
        entity.setSort(payload.sort());
        entity.setStatus(payload.status());
        if (entity.getCreatedTime() == null) {
            entity.setCreatedTime(now);
        }
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(now);
        }
        entity.setUpdatedTime(now);
        entity.setUpdatedAt(now);
    }

    private WorkflowNodeDefinition resolve(ResourceDeclaration resource) {
        Long tenantId = WorkflowResourceFields.longValue(resource, "tenantId", false, DEFAULT_TENANT_ID);
        String nodeDefinitionCode = WorkflowResourceFields.text(resource, "nodeDefinitionCode", false);
        if (StringUtils.hasText(nodeDefinitionCode)) {
            WorkflowNodeDefinition entity = find(tenantId, nodeDefinitionCode.trim());
            if (entity != null) {
                return entity;
            }
        }
        Long targetId = WorkflowResourceFields.longValue(resource, "targetId", false, null);
        return targetId == null ? null : nodeDefinitionMapper.selectById(targetId);
    }

    private WorkflowNodeDefinition find(Long tenantId, String nodeDefinitionCode) {
        return nodeDefinitionMapper.selectOne(new LambdaQueryWrapper<WorkflowNodeDefinition>()
                .eq(WorkflowNodeDefinition::getTenantId, tenantId)
                .eq(WorkflowNodeDefinition::getNodeDefinitionCode, nodeDefinitionCode)
                .last("limit 1"));
    }

    private record Payload(Long nodeDefinitionId, Long tenantId, String nodeDefinitionCode, String nodeType,
                           String nodeName, String categoryCode, String categoryName, String description,
                           String bpmnType, String executionType, String color, String icon, String propertySchema,
                           String defaultProperties, Integer sort, Integer status) {

        private static Payload from(ResourceDeclaration resource) {
            return new Payload(
                    WorkflowResourceFields.longValue(resource, "nodeDefinitionId", false,
                            Long.valueOf(resource.getId())),
                    WorkflowResourceFields.longValue(resource, "tenantId", false, DEFAULT_TENANT_ID),
                    WorkflowResourceFields.requiredText(resource, "nodeDefinitionCode",
                            ResourceTypes.WORKFLOW_NODE_DEFINITION),
                    WorkflowResourceFields.requiredText(resource, "nodeType", ResourceTypes.WORKFLOW_NODE_DEFINITION),
                    WorkflowResourceFields.requiredText(resource, "nodeName", ResourceTypes.WORKFLOW_NODE_DEFINITION),
                    WorkflowResourceFields.requiredText(resource, "categoryCode",
                            ResourceTypes.WORKFLOW_NODE_DEFINITION),
                    WorkflowResourceFields.requiredText(resource, "categoryName",
                            ResourceTypes.WORKFLOW_NODE_DEFINITION),
                    WorkflowResourceFields.text(resource, "description", false),
                    WorkflowResourceFields.requiredText(resource, "bpmnType", ResourceTypes.WORKFLOW_NODE_DEFINITION),
                    WorkflowResourceFields.requiredText(resource, "executionType",
                            ResourceTypes.WORKFLOW_NODE_DEFINITION),
                    WorkflowResourceFields.text(resource, "color", false),
                    WorkflowResourceFields.text(resource, "icon", false),
                    WorkflowResourceFields.text(resource, "propertySchema", false),
                    WorkflowResourceFields.defaultText(resource, "defaultProperties", "{}"),
                    WorkflowResourceFields.intValue(resource, "sort", false, 0),
                    WorkflowResourceFields.intValue(resource, "status", false, ENABLED)
            );
        }
    }
}
