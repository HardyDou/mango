package io.mango.information.starter.resource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.exception.BizException;
import io.mango.common.result.Require;
import io.mango.information.api.enums.InformationCode;
import io.mango.information.api.enums.InformationFormStatus;
import io.mango.information.core.entity.InformationFormEntity;
import io.mango.information.core.service.impl.InformationFormService;
import io.mango.information.core.mapper.InformationFormMapper;
import io.mango.information.core.support.InformationDefinition;
import io.mango.resource.support.ResourceHandler;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.support.model.ResourceHandlerSpec;
import io.mango.resource.support.model.ResourceSyncResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 内置采集表单资源处理器。
 * <p>
 * 允许业务模块通过 Resource Registry 声明随代码发布的内置表单；
 * 同一租户下以 formCode 作为幂等键，重复同步只更新定义，不会生成重复表单。
 */
@Component
@RequiredArgsConstructor
public class InformationFormResourceHandler implements ResourceHandler {

    private static final String TARGET_TABLE = "information_form";
    private static final String DEFAULT_TENANT_ID = "1";

    private final InformationFormMapper formMapper;
    private final ObjectMapper objectMapper;

    @Override
    public String resourceType() {
        return InformationResourceTypes.FORM;
    }

    @Override
    public String executionTenantField() {
        return "tenantId";
    }

    @Override
    public ResourceHandlerSpec spec() {
        return ResourceHandlerSpec.builder()
                .resourceType(resourceType())
                .requiredField("formCode")
                .requiredField("formName")
                .requiredField("definitionJson")
                .fieldDescription("tenantId", "租户 ID，默认 1。")
                .fieldDescription("targetId", "表单主键稳定 ID，可选；不填使用资源声明 ID 或自动生成。")
                .fieldDescription("formCode", "表单编码，租户内唯一，托管地址按它访问。")
                .fieldDescription("formName", "表单名称。")
                .fieldDescription("description", "备注。")
                .fieldDescription("definitionJson", "表单定义，form-create 规则数组 JSON。")
                .fieldDescription("successMessage", "提交成功提示，默认“提交成功”。")
                .fieldDescription("status", "表单状态：PUBLISHED 启用，DISABLED 停用；默认 PUBLISHED。")
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResourceSyncResult upsert(ResourceDeclaration resource) {
        String tenantId = text(resource, "tenantId", DEFAULT_TENANT_ID);
        String formCode = requiredText(resource, "formCode");
        String definitionJson = requiredText(resource, "definitionJson");
        validateDefinition(definitionJson);

        InformationFormEntity entity = existing(tenantId, formCode);
        boolean created = entity == null;
        if (created) {
            entity = new InformationFormEntity();
            entity.setId(targetId(resource));
            entity.setTenantId(tenantId);
            entity.setFormCode(formCode);
            // 与后台创建保持一致：保存 SK 及其摘要，列不可为空
            entity.setServerSecret(newSecret());
            entity.setServerSecretDigest(InformationFormService.digest(entity.getServerSecret()));
        }
        entity.setFormName(requiredText(resource, "formName"));
        entity.setDescription(text(resource, "description", null));
        entity.setDefinitionJson(definitionJson);
        entity.setSuccessMessage(text(resource, "successMessage", "提交成功"));
        entity.setStatus(status(resource));
        if (created) {
            formMapper.insert(entity);
        } else {
            formMapper.updateById(entity);
        }
        return ResourceSyncResult.of(entity.getId(), TARGET_TABLE,
                "Information form synced: " + formCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResourceSyncResult disable(ResourceDeclaration resource) {
        InformationFormEntity entity = existing(text(resource, "tenantId", DEFAULT_TENANT_ID),
                requiredText(resource, "formCode"));
        if (entity == null) {
            return ResourceSyncResult.of(null, TARGET_TABLE, "Information form not found");
        }
        entity.setStatus(InformationFormStatus.DISABLED.name());
        formMapper.updateById(entity);
        return ResourceSyncResult.of(entity.getId(), TARGET_TABLE,
                "Information form disabled: " + entity.getFormCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResourceSyncResult delete(ResourceDeclaration resource) {
        InformationFormEntity entity = existing(text(resource, "tenantId", DEFAULT_TENANT_ID),
                requiredText(resource, "formCode"));
        if (entity == null) {
            return ResourceSyncResult.of(null, TARGET_TABLE, "Information form not found");
        }
        formMapper.deleteById(entity.getId());
        return ResourceSyncResult.of(entity.getId(), TARGET_TABLE,
                "Information form deleted: " + entity.getFormCode());
    }

    /** 内置表单以租户 + 表单编码为幂等键，避免重复声明产生多张同编码表单。 */
    private InformationFormEntity existing(String tenantId, String formCode) {
        return formMapper.selectOne(new LambdaQueryWrapper<InformationFormEntity>()
                .eq(InformationFormEntity::getTenantId, tenantId)
                .eq(InformationFormEntity::getFormCode, formCode)
                .last("LIMIT 1"));
    }

    private void validateDefinition(String definitionJson) {
        try {
            Require.isTrue(!InformationDefinition.fields(objectMapper.readTree(definitionJson)).isEmpty(),
                    InformationCode.INVALID_FORM_DEFINITION, "内置表单至少需要一个可提交采集项");
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(InformationCode.INVALID_FORM_DEFINITION.getCode(),
                    InformationCode.INVALID_FORM_DEFINITION.getMessage(), e);
        }
    }

    private Long targetId(ResourceDeclaration resource) {
        String targetId = text(resource, "targetId", null);
        if (StringUtils.hasText(targetId)) {
            Require.isTrue(targetId.matches("\\d+"), InformationCode.INVALID_FORM_DEFINITION, "targetId 必须为正整数");
            return Long.valueOf(targetId);
        }
        String resourceId = resource.getId();
        return resourceId != null && resourceId.matches("\\d+") ? Long.valueOf(resourceId) : IdWorker.getId();
    }

    private String status(ResourceDeclaration resource) {
        String status = text(resource, "status", InformationFormStatus.PUBLISHED.name());
        Require.isTrue(InformationFormStatus.PUBLISHED.name().equalsIgnoreCase(status)
                        || InformationFormStatus.DISABLED.name().equalsIgnoreCase(status)
                        || InformationFormStatus.DRAFT.name().equalsIgnoreCase(status),
                InformationCode.INVALID_FORM_DEFINITION, "status 仅支持 DRAFT、PUBLISHED、DISABLED");
        return status.toUpperCase(java.util.Locale.ROOT);
    }

    private String newSecret() {
        byte[] bytes = new byte[24];
        new java.security.SecureRandom().nextBytes(bytes);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String requiredText(ResourceDeclaration resource, String field) {
        String value = text(resource, field, null);
        Require.isTrue(StringUtils.hasText(value), InformationCode.INVALID_FORM_DEFINITION, field + " 不能为空");
        return value.trim();
    }

    private String text(ResourceDeclaration resource, String field, String fallback) {
        var declared = resource.getFields().get(field);
        if (declared == null) {
            return fallback;
        }
        Object value = declared.getValue();
        if (value == null || !StringUtils.hasText(String.valueOf(value))) {
            return fallback;
        }
        return String.valueOf(value);
    }

}
