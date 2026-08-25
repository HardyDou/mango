package io.mango.ai.core.resource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.ai.api.enums.AiApiProtocol;
import io.mango.ai.core.entity.AiModelEntity;
import io.mango.ai.core.entity.AiProviderConnectionEntity;
import io.mango.ai.core.mapper.AiModelMapper;
import io.mango.ai.core.mapper.AiProviderConnectionMapper;
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

/** 首次初始化供应商模型目录，不覆盖租户后续配置。 */
@Component
@RequiredArgsConstructor
public class AiModelResourceHandler implements ResourceHandler {

    private static final String TARGET_TABLE = "ai_model";

    private final AiProviderConnectionMapper providerMapper;
    private final AiModelMapper modelMapper;

    @Override
    public String resourceType() {
        return ResourceTypes.AI_MODEL;
    }

    @Override
    public List<String> dependsOnResourceTypes() {
        return List.of(ResourceTypes.AI_PROVIDER_CONNECTION);
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
                .requiredField("providerCode")
                .requiredField("modelName")
                .requiredField("displayName")
                .requiredField("apiProtocol")
                .requiredField("capabilitiesJson")
                .requiredField("inputModalitiesJson")
                .requiredField("outputModalitiesJson")
                .fieldDescription("targetId", "模型稳定 ID。")
                .fieldDescription("tenantId", "模型所属租户。")
                .fieldDescription("providerCode", "所属供应商连接编码。")
                .fieldDescription("modelName", "供应商侧模型名称或端点 ID。")
                .fieldDescription("displayName", "模型显示名称。")
                .fieldDescription("platformAlias", "平台内模型别名。")
                .fieldDescription("apiProtocol", "CHAT_COMPLETIONS 或 RESPONSES。")
                .fieldDescription("capabilitiesJson", "模型能力 JSON 数组。")
                .fieldDescription("inputModalitiesJson", "输入模态 JSON 数组。")
                .fieldDescription("outputModalitiesJson", "输出模态 JSON 数组。")
                .fieldDescription("parameterJson", "模型默认参数 JSON 对象。")
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResourceSyncResult upsert(ResourceDeclaration resource) {
        String tenantId = AiResourceFields.requiredText(resource, "tenantId");
        String providerCode = AiResourceFields.requiredText(resource, "providerCode");
        String modelName = AiResourceFields.requiredText(resource, "modelName");
        AiProviderConnectionEntity provider = requireProvider(tenantId, providerCode);
        AiModelEntity existing = find(tenantId, provider.getId(), modelName);
        if (existing != null) {
            return result(existing.getId(), "AI model preserved: " + modelName);
        }

        AiModelEntity model = new AiModelEntity();
        model.setId(AiResourceFields.targetId(resource));
        model.setTenantId(tenantId);
        model.setProviderConnectionId(provider.getId());
        model.setModelName(modelName);
        model.setDisplayName(AiResourceFields.requiredText(resource, "displayName"));
        model.setPlatformAlias(AiResourceFields.optionalText(resource, "platformAlias"));
        model.setApiProtocol(AiResourceFields.requiredEnum(
                resource, "apiProtocol", AiApiProtocol.class));
        model.setCapabilitiesJson(AiResourceFields.requiredText(resource, "capabilitiesJson"));
        model.setInputModalitiesJson(AiResourceFields.requiredText(resource, "inputModalitiesJson"));
        model.setOutputModalitiesJson(AiResourceFields.requiredText(resource, "outputModalitiesJson"));
        model.setParameterJson(AiResourceFields.optionalText(resource, "parameterJson"));
        model.setEnabled(false);
        Require.isTrue(modelMapper.insert(model) > 0, "AI model initialization failed: " + modelName);
        return result(model.getId(), "AI model initialized: " + modelName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResourceSyncResult disable(ResourceDeclaration resource) {
        String tenantId = AiResourceFields.requiredText(resource, "tenantId");
        String providerCode = AiResourceFields.requiredText(resource, "providerCode");
        String modelName = AiResourceFields.requiredText(resource, "modelName");
        AiProviderConnectionEntity provider = requireProvider(tenantId, providerCode);
        AiModelEntity model = find(tenantId, provider.getId(), modelName);
        if (model == null) {
            return result(null, "AI model not found: " + modelName);
        }
        model.setEnabled(false);
        Require.isTrue(modelMapper.updateById(model) > 0, "AI model disable failed: " + modelName);
        return result(model.getId(), "AI model disabled: " + modelName);
    }

    private AiProviderConnectionEntity requireProvider(String tenantId, String providerCode) {
        return Require.nonNull(providerMapper.selectOne(
                        new LambdaQueryWrapper<AiProviderConnectionEntity>()
                                .eq(AiProviderConnectionEntity::getTenantId, tenantId)
                                .eq(AiProviderConnectionEntity::getCode, providerCode)),
                "AI provider is required before model initialization: " + providerCode);
    }

    private AiModelEntity find(String tenantId, Long providerId, String modelName) {
        return modelMapper.selectOne(new LambdaQueryWrapper<AiModelEntity>()
                .eq(AiModelEntity::getTenantId, tenantId)
                .eq(AiModelEntity::getProviderConnectionId, providerId)
                .eq(AiModelEntity::getModelName, modelName));
    }

    private ResourceSyncResult result(Long targetId, String message) {
        return ResourceSyncResult.of(targetId, TARGET_TABLE, message);
    }
}
