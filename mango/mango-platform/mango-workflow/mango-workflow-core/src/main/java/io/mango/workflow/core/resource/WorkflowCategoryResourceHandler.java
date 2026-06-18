package io.mango.workflow.core.resource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.resource.api.ResourceHandler;
import io.mango.resource.api.ResourceTypes;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceHandlerSpec;
import io.mango.resource.api.model.ResourceSyncResult;
import io.mango.workflow.core.entity.WorkflowCategory;
import io.mango.workflow.core.mapper.WorkflowCategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 流程分类资源处理器。
 */
@Component
@RequiredArgsConstructor
public class WorkflowCategoryResourceHandler implements ResourceHandler {

    private static final String TARGET_TABLE = "workflow_category";
    private static final Long DEFAULT_TENANT_ID = 1L;
    private static final int ENABLED = 1;
    private static final int DISABLED = 0;

    private final WorkflowCategoryMapper categoryMapper;

    @Override
    public String resourceType() {
        return ResourceTypes.WORKFLOW_CATEGORY;
    }

    @Override
    public ResourceHandlerSpec spec() {
        return ResourceHandlerSpec.builder()
                .resourceType(resourceType())
                .requiredField("categoryCode")
                .requiredField("categoryName")
                .fieldDescription("categoryId", "流程分类稳定 ID，可选；不填时使用资源 ID。")
                .fieldDescription("tenantId", "租户 ID，默认 1。")
                .fieldDescription("categoryCode", "流程分类编码，租户内唯一。")
                .fieldDescription("categoryName", "流程分类名称。")
                .fieldDescription("domainCode", "业务域编码，默认 COMMON。")
                .fieldDescription("sort", "排序号，默认 0。")
                .fieldDescription("status", "状态：1 启用，0 停用。")
                .fieldDescription("remark", "备注。")
                .build();
    }

    @Override
    public ResourceSyncResult upsert(ResourceDeclaration resource) {
        Payload payload = Payload.from(resource);
        WorkflowCategory entity = find(payload.tenantId(), payload.categoryCode());
        if (entity == null) {
            entity = new WorkflowCategory();
            entity.setId(payload.categoryId());
            apply(entity, payload);
            categoryMapper.insert(entity);
        } else {
            apply(entity, payload);
            categoryMapper.updateById(entity);
        }
        return ResourceSyncResult.of(entity.getId(), TARGET_TABLE,
                "Workflow category synced: " + entity.getCategoryCode());
    }

    @Override
    public ResourceSyncResult disable(ResourceDeclaration resource) {
        WorkflowCategory entity = resolve(resource);
        if (entity == null) {
            return ResourceSyncResult.of(null, TARGET_TABLE, "Workflow category not found");
        }
        entity.setStatus(DISABLED);
        entity.setUpdatedTime(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        categoryMapper.updateById(entity);
        return ResourceSyncResult.of(entity.getId(), TARGET_TABLE,
                "Workflow category disabled: " + entity.getCategoryCode());
    }

    @Override
    public ResourceSyncResult delete(ResourceDeclaration resource) {
        WorkflowCategory entity = resolve(resource);
        if (entity == null) {
            return ResourceSyncResult.of(null, TARGET_TABLE, "Workflow category not found");
        }
        categoryMapper.deleteById(entity.getId());
        return ResourceSyncResult.of(entity.getId(), TARGET_TABLE,
                "Workflow category deleted: " + entity.getCategoryCode());
    }

    private void apply(WorkflowCategory entity, Payload payload) {
        LocalDateTime now = LocalDateTime.now();
        entity.setTenantId(payload.tenantId());
        entity.setCategoryCode(payload.categoryCode());
        entity.setCategoryName(payload.categoryName());
        entity.setDomainCode(payload.domainCode());
        entity.setSort(payload.sort());
        entity.setStatus(payload.status());
        entity.setRemark(payload.remark());
        if (entity.getCreatedTime() == null) {
            entity.setCreatedTime(now);
        }
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(now);
        }
        entity.setUpdatedTime(now);
        entity.setUpdatedAt(now);
    }

    private WorkflowCategory resolve(ResourceDeclaration resource) {
        Long tenantId = WorkflowResourceFields.longValue(resource, "tenantId", false, DEFAULT_TENANT_ID);
        String categoryCode = WorkflowResourceFields.text(resource, "categoryCode", false);
        if (StringUtils.hasText(categoryCode)) {
            WorkflowCategory entity = find(tenantId, categoryCode.trim());
            if (entity != null) {
                return entity;
            }
        }
        Long targetId = WorkflowResourceFields.longValue(resource, "targetId", false, null);
        return targetId == null ? null : categoryMapper.selectById(targetId);
    }

    private WorkflowCategory find(Long tenantId, String categoryCode) {
        return categoryMapper.selectOne(new LambdaQueryWrapper<WorkflowCategory>()
                .eq(WorkflowCategory::getTenantId, tenantId)
                .eq(WorkflowCategory::getCategoryCode, categoryCode)
                .last("limit 1"));
    }

    private record Payload(Long categoryId, Long tenantId, String categoryCode, String categoryName,
                           String domainCode, Integer sort, Integer status, String remark) {

        private static Payload from(ResourceDeclaration resource) {
            return new Payload(
                    WorkflowResourceFields.longValue(resource, "categoryId", false, Long.valueOf(resource.getId())),
                    WorkflowResourceFields.longValue(resource, "tenantId", false, DEFAULT_TENANT_ID),
                    WorkflowResourceFields.requiredText(resource, "categoryCode", ResourceTypes.WORKFLOW_CATEGORY),
                    WorkflowResourceFields.requiredText(resource, "categoryName", ResourceTypes.WORKFLOW_CATEGORY),
                    WorkflowResourceFields.defaultText(resource, "domainCode", "COMMON"),
                    WorkflowResourceFields.intValue(resource, "sort", false, 0),
                    WorkflowResourceFields.intValue(resource, "status", false, ENABLED),
                    WorkflowResourceFields.text(resource, "remark", false)
            );
        }
    }
}
