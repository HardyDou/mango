package io.mango.workflow.core.resource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.R;
import io.mango.common.result.Require;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.resource.api.ResourceHandler;
import io.mango.resource.api.ResourceTypes;
import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceField;
import io.mango.resource.api.model.ResourceHandlerSpec;
import io.mango.resource.api.model.ResourceSyncResult;
import io.mango.workflow.api.WorkflowCode;
import io.mango.workflow.api.command.EnsureWorkflowDefinitionCommand;
import io.mango.workflow.api.enums.WorkflowDefinitionStatus;
import io.mango.workflow.api.vo.WorkflowDeployVO;
import io.mango.workflow.core.entity.WorkflowDefinition;
import io.mango.workflow.core.entity.WorkflowDefinitionVersion;
import io.mango.workflow.core.mapper.WorkflowDefinitionMapper;
import io.mango.workflow.core.mapper.WorkflowDefinitionVersionMapper;
import io.mango.workflow.core.service.IWorkflowDefinitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 流程定义资源处理器。
 */
@Component
@RequiredArgsConstructor
public class WorkflowDefinitionResourceHandler implements ResourceHandler {

    private static final String TARGET_TABLE = "workflow_definition";
    private static final Long DEFAULT_TENANT_ID = 1L;
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final IWorkflowDefinitionService workflowDefinitionService;
    private final WorkflowDefinitionMapper definitionMapper;
    private final WorkflowDefinitionVersionMapper versionMapper;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    @Override
    public String resourceType() {
        return ResourceTypes.WORKFLOW_DEFINITION;
    }

    @Override
    public List<String> dependsOnResourceTypes() {
        return List.of(ResourceTypes.WORKFLOW_CATEGORY, ResourceTypes.WORKFLOW_NODE_DEFINITION);
    }

    @Override
    public ResourceHandlerSpec spec() {
        return ResourceHandlerSpec.builder()
                .resourceType(resourceType())
                .requiredField("domainCode")
                .requiredField("categoryCode")
                .requiredField("categoryName")
                .requiredField("categorySort")
                .requiredField("definitionKey")
                .requiredField("definitionName")
                .requiredField("designerJson")
                .fieldDescription("tenantId", "租户 ID，默认 1。")
                .fieldDescription("domainCode", "业务域编码。")
                .fieldDescription("categoryCode", "流程分类编码。")
                .fieldDescription("categoryName", "流程分类名称。")
                .fieldDescription("categorySort", "流程分类排序。")
                .fieldDescription("categoryRemark", "流程分类备注。")
                .fieldDescription("orgId", "所属组织 ID。")
                .fieldDescription("adminUsers", "流程管理员用户名列表。")
                .fieldDescription("startEntryVisible", "启动入口是否可见；false 表示不出现在审批中心发起流程入口。")
                .fieldDescription("icon", "流程图标。")
                .fieldDescription("definitionKey", "流程编码。")
                .fieldDescription("definitionName", "流程名称。")
                .fieldDescription("designerJson", "设计器 JSON 内容，支持 inline JSON 或 FILE 字段。")
                .fieldDescription("formCode", "表单编码。")
                .fieldDescription("formJson", "动态表单 JSON 配置，支持 inline JSON 或 FILE 字段。")
                .fieldDescription("remark", "备注。")
                .build();
    }

    @Override
    public ResourceSyncResult upsert(ResourceDeclaration resource) {
        Payload payload = Payload.from(resource, objectMapper, resourceLoader);
        R<WorkflowDeployVO> result = withTenant(payload.tenantId(),
                () -> workflowDefinitionService.ensurePublished(payload.command()));
        Require.isTrue(result != null && result.isSuccess(), WorkflowCode.DEFINITION_INVALID.getCode(),
                result == null ? "流程定义资源同步失败" : result.getMsg());
        WorkflowDefinition definition = find(payload.tenantId(), payload.command().getDefinitionKey());
        return ResourceSyncResult.of(definition == null ? null : definition.getId(), TARGET_TABLE,
                "Workflow definition synced: " + payload.command().getDefinitionKey());
    }

    @Override
    public ResourceSyncResult disable(ResourceDeclaration resource) {
        WorkflowDefinition definition = resolve(resource);
        if (definition == null) {
            return ResourceSyncResult.of(null, TARGET_TABLE, "Workflow definition not found");
        }
        definition.setStatus(WorkflowDefinitionStatus.DISABLED.name());
        definition.setUpdatedTime(LocalDateTime.now());
        definition.setUpdatedAt(LocalDateTime.now());
        definitionMapper.updateById(definition);
        return ResourceSyncResult.of(definition.getId(), TARGET_TABLE,
                "Workflow definition disabled: " + definition.getDefinitionKey());
    }

    @Override
    public ResourceSyncResult delete(ResourceDeclaration resource) {
        WorkflowDefinition definition = resolve(resource);
        if (definition == null) {
            return ResourceSyncResult.of(null, TARGET_TABLE, "Workflow definition not found");
        }
        versionMapper.delete(new LambdaQueryWrapper<WorkflowDefinitionVersion>()
                .eq(WorkflowDefinitionVersion::getDefinitionId, definition.getId()));
        definitionMapper.deleteById(definition.getId());
        return ResourceSyncResult.of(definition.getId(), TARGET_TABLE,
                "Workflow definition deleted: " + definition.getDefinitionKey());
    }

    private WorkflowDefinition resolve(ResourceDeclaration resource) {
        Long tenantId = WorkflowResourceFields.longValue(resource, "tenantId", false, DEFAULT_TENANT_ID);
        String definitionKey = WorkflowResourceFields.text(resource, "definitionKey", false);
        if (StringUtils.hasText(definitionKey)) {
            WorkflowDefinition definition = find(tenantId, definitionKey.trim());
            if (definition != null) {
                return definition;
            }
        }
        Long targetId = WorkflowResourceFields.longValue(resource, "targetId", false, null);
        return targetId == null ? null : definitionMapper.selectById(targetId);
    }

    private WorkflowDefinition find(Long tenantId, String definitionKey) {
        return definitionMapper.selectOne(new LambdaQueryWrapper<WorkflowDefinition>()
                .eq(WorkflowDefinition::getTenantId, tenantId)
                .eq(WorkflowDefinition::getDefinitionKey, definitionKey)
                .last("limit 1"));
    }

    private <T> T withTenant(Long tenantId, TenantOperation<T> operation) {
        MangoContextSnapshot previous = MangoContextHolder.get();
        MangoContextHolder.set(previous.withTenantId(String.valueOf(tenantId)));
        try {
            return operation.execute();
        } finally {
            MangoContextHolder.set(previous);
        }
    }

    private interface TenantOperation<T> {
        T execute();
    }

    private record Payload(Long tenantId, EnsureWorkflowDefinitionCommand command) {

        private static Payload from(ResourceDeclaration resource, ObjectMapper objectMapper,
                                    ResourceLoader resourceLoader) {
            Long tenantId = WorkflowResourceFields.longValue(resource, "tenantId", false, DEFAULT_TENANT_ID);
            EnsureWorkflowDefinitionCommand command = new EnsureWorkflowDefinitionCommand();
            command.setDomainCode(WorkflowResourceFields.requiredText(resource, "domainCode",
                    ResourceTypes.WORKFLOW_DEFINITION));
            command.setCategoryCode(WorkflowResourceFields.requiredText(resource, "categoryCode",
                    ResourceTypes.WORKFLOW_DEFINITION));
            command.setCategoryName(WorkflowResourceFields.requiredText(resource, "categoryName",
                    ResourceTypes.WORKFLOW_DEFINITION));
            command.setCategorySort(WorkflowResourceFields.intValue(resource, "categorySort", true, null));
            command.setCategoryRemark(WorkflowResourceFields.text(resource, "categoryRemark", false));
            command.setOrgId(WorkflowResourceFields.longValue(resource, "orgId", false, null));
            command.setAdminUsers(stringList(resource, "adminUsers", objectMapper));
            command.setStartEntryVisible(booleanValue(resource, "startEntryVisible", false, null));
            command.setIcon(WorkflowResourceFields.text(resource, "icon", false));
            command.setDefinitionKey(WorkflowResourceFields.requiredText(resource, "definitionKey",
                    ResourceTypes.WORKFLOW_DEFINITION));
            command.setDefinitionName(WorkflowResourceFields.requiredText(resource, "definitionName",
                    ResourceTypes.WORKFLOW_DEFINITION));
            command.setDesignerJson(jsonText(resource, "designerJson", true, objectMapper, resourceLoader));
            command.setFormCode(WorkflowResourceFields.text(resource, "formCode", false));
            command.setFormJson(jsonText(resource, "formJson", false, objectMapper, resourceLoader));
            command.setRemark(WorkflowResourceFields.text(resource, "remark", false));
            return new Payload(tenantId, command);
        }

        private static Boolean booleanValue(ResourceDeclaration resource, String name, boolean required,
                                            Boolean defaultValue) {
            Object value = WorkflowResourceFields.value(resource, name, required);
            if (value == null || !StringUtils.hasText(String.valueOf(value))) {
                return defaultValue;
            }
            if (value instanceof Boolean booleanValue) {
                return booleanValue;
            }
            return Boolean.valueOf(String.valueOf(value));
        }

        private static List<String> stringList(ResourceDeclaration resource, String name, ObjectMapper objectMapper) {
            Object value = WorkflowResourceFields.value(resource, name, false);
            if (value == null) {
                return null;
            }
            if (value instanceof Collection<?> collection) {
                return collection.stream()
                        .filter(item -> item != null && StringUtils.hasText(String.valueOf(item)))
                        .map(item -> String.valueOf(item).trim())
                        .distinct()
                        .toList();
            }
            String text = String.valueOf(value);
            if (!StringUtils.hasText(text)) {
                return null;
            }
            try {
                return objectMapper.readValue(text, STRING_LIST_TYPE);
            } catch (JsonProcessingException ignored) {
                return List.of(text.split("\\s*,\\s*")).stream()
                        .filter(StringUtils::hasText)
                        .map(String::trim)
                        .distinct()
                        .toList();
            }
        }

        private static String jsonText(ResourceDeclaration resource, String name, boolean required,
                                       ObjectMapper objectMapper, ResourceLoader resourceLoader) {
            ResourceField field = resource.getFields().get(name);
            if (field == null) {
                if (required) {
                    throw new IllegalStateException(resource.getResourceType() + " field is required: " + name);
                }
                return null;
            }
            String value = field.getType() == ResourceFieldType.FILE
                    ? fileContent(field, resourceLoader)
                    : valueContent(field, objectMapper);
            if (!StringUtils.hasText(value)) {
                if (required) {
                    throw new IllegalStateException(resource.getResourceType() + " field is required: " + name);
                }
                return null;
            }
            return value.trim();
        }

        private static String valueContent(ResourceField field, ObjectMapper objectMapper) {
            Object value = field.getValue();
            if (value == null) {
                return null;
            }
            if (value instanceof CharSequence) {
                return String.valueOf(value);
            }
            try {
                return objectMapper.writeValueAsString(value);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Serialize workflow definition resource field failed", e);
            }
        }

        private static String fileContent(ResourceField field, ResourceLoader resourceLoader) {
            if (!StringUtils.hasText(field.getLocation()) || !field.getLocation().startsWith("classpath:")) {
                throw new IllegalStateException("Workflow definition file field only supports classpath location: "
                        + field.getLocation());
            }
            Resource file = resourceLoader.getResource(field.getLocation());
            if (!file.exists() || !file.isReadable()) {
                throw new IllegalStateException("Workflow definition file is not readable: " + field.getLocation());
            }
            Charset charset = StringUtils.hasText(field.getEncoding())
                    ? Charset.forName(field.getEncoding())
                    : StandardCharsets.UTF_8;
            try {
                return file.getContentAsString(charset);
            } catch (IOException e) {
                throw new IllegalStateException("Read workflow definition file failed: " + field.getLocation(), e);
            }
        }
    }
}
