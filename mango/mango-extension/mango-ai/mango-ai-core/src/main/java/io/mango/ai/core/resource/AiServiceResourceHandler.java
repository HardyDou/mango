package io.mango.ai.core.resource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.ai.api.enums.AiCapability;
import io.mango.ai.api.enums.AiServiceType;
import io.mango.ai.core.entity.AiPromptEntity;
import io.mango.ai.core.entity.AiServiceEntity;
import io.mango.ai.core.entity.AiSkillEntity;
import io.mango.ai.core.mapper.AiPromptMapper;
import io.mango.ai.core.mapper.AiServiceMapper;
import io.mango.ai.core.mapper.AiSkillMapper;
import io.mango.common.result.Require;
import io.mango.resource.support.ResourceHandler;
import io.mango.resource.support.ResourceTypes;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.support.model.ResourceHandlerSpec;
import io.mango.resource.support.model.ResourceSyncResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 首次初始化面向业务的 AI 服务，不覆盖租户后续配置。 */
@Component
@RequiredArgsConstructor
public class AiServiceResourceHandler implements ResourceHandler {

    private static final String TARGET_TABLE = "ai_service_definition";

    private final AiPromptMapper promptMapper;
    private final AiSkillMapper skillMapper;
    private final AiServiceMapper serviceMapper;

    @Override
    public String resourceType() {
        return ResourceTypes.AI_SERVICE;
    }

    @Override
    public List<String> dependsOnResourceTypes() {
        return List.of(ResourceTypes.AI_PROMPT, ResourceTypes.AI_SKILL);
    }

    @Override
    public String executionTenantField() {
        return "tenantId";
    }

    @Override
    public ResourceHandlerSpec spec() {
        return ResourceHandlerSpec.builder()
                .resourceType(resourceType())
                .requiredField("targetId")
                .requiredField("tenantId")
                .requiredField("code")
                .requiredField("name")
                .requiredField("serviceType")
                .requiredField("promptCode")
                .requiredField("inputSchemaJson")
                .requiredField("outputSchemaJson")
                .requiredField("enabled")
                .fieldDescription("targetId", "AI 服务稳定 ID。")
                .fieldDescription("tenantId", "AI 服务所属租户。")
                .fieldDescription("code", "租户内唯一的 AI 服务编码。")
                .fieldDescription("name", "AI 服务名称。")
                .fieldDescription("description", "AI 服务用途说明。")
                .fieldDescription("serviceType", "CHAT、EXTRACTION 或 CLASSIFICATION。")
                .fieldDescription("capability", "服务要求的模型能力。")
                .fieldDescription("promptCode", "已存在且已发布的 Prompt 编码。")
                .fieldDescription("skillCode", "可选的 Skill 编码。")
                .fieldDescription("inputSchemaJson", "输入 JSON Schema。")
                .fieldDescription("outputSchemaJson", "输出 JSON Schema。")
                .fieldDescription("enabled", "是否启用。")
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResourceSyncResult upsert(ResourceDeclaration resource) {
        String tenantId = AiResourceFields.requiredText(resource, "tenantId");
        String code = AiResourceFields.requiredText(resource, "code");
        AiServiceEntity existing = find(tenantId, code);
        if (existing != null) {
            return result(existing.getId(), "AI service preserved: " + code);
        }

        AiPromptEntity prompt = requirePrompt(
                tenantId, AiResourceFields.requiredText(resource, "promptCode"));
        AiSkillEntity skill = resolveSkill(
                tenantId, AiResourceFields.optionalText(resource, "skillCode"));
        AiServiceEntity service = new AiServiceEntity();
        service.setId(AiResourceFields.targetId(resource));
        service.setTenantId(tenantId);
        service.setCode(code);
        service.setName(AiResourceFields.requiredText(resource, "name"));
        service.setDescription(AiResourceFields.optionalText(resource, "description"));
        service.setServiceType(AiResourceFields.requiredEnum(
                resource, "serviceType", AiServiceType.class));
        service.setCapability(AiResourceFields.optionalEnum(
                resource, "capability", AiCapability.class));
        service.setPromptId(prompt.getId());
        service.setSkillId(skill == null ? null : skill.getId());
        service.setInputSchemaJson(AiResourceFields.requiredText(resource, "inputSchemaJson"));
        service.setOutputSchemaJson(AiResourceFields.requiredText(resource, "outputSchemaJson"));
        service.setEnabled(AiResourceFields.requiredBoolean(resource, "enabled"));
        Require.isTrue(serviceMapper.insert(service) > 0, "AI service initialization failed: " + code);
        return result(service.getId(), "AI service initialized: " + code);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResourceSyncResult disable(ResourceDeclaration resource) {
        String tenantId = AiResourceFields.requiredText(resource, "tenantId");
        String code = AiResourceFields.requiredText(resource, "code");
        AiServiceEntity service = find(tenantId, code);
        if (service == null) {
            return result(null, "AI service not found: " + code);
        }
        service.setEnabled(false);
        Require.isTrue(serviceMapper.updateById(service) > 0, "AI service disable failed: " + code);
        return result(service.getId(), "AI service disabled: " + code);
    }

    private AiPromptEntity requirePrompt(String tenantId, String promptCode) {
        return Require.nonNull(promptMapper.selectOne(new LambdaQueryWrapper<AiPromptEntity>()
                        .eq(AiPromptEntity::getTenantId, tenantId)
                        .eq(AiPromptEntity::getCode, promptCode)),
                "AI prompt is required before service initialization: " + promptCode);
    }

    private AiSkillEntity resolveSkill(String tenantId, String skillCode) {
        if (skillCode == null) {
            return null;
        }
        return Require.nonNull(skillMapper.selectOne(new LambdaQueryWrapper<AiSkillEntity>()
                        .eq(AiSkillEntity::getTenantId, tenantId)
                        .eq(AiSkillEntity::getCode, skillCode)),
                "AI skill is required before service initialization: " + skillCode);
    }

    private AiServiceEntity find(String tenantId, String code) {
        return serviceMapper.selectOne(new LambdaQueryWrapper<AiServiceEntity>()
                .eq(AiServiceEntity::getTenantId, tenantId)
                .eq(AiServiceEntity::getCode, code));
    }

    private ResourceSyncResult result(Long targetId, String message) {
        return ResourceSyncResult.of(targetId, TARGET_TABLE, message);
    }
}
