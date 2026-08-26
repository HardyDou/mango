package io.mango.ai.core.resource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.ai.api.enums.AiProviderType;
import io.mango.ai.core.entity.AiProviderConnectionEntity;
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

/** 首次初始化 AI 供应商连接，不覆盖租户后续配置。 */
@Component
@RequiredArgsConstructor
public class AiProviderConnectionResourceHandler implements ResourceHandler {

    private static final String TARGET_TABLE = "ai_provider_connection";

    private final AiProviderConnectionMapper providerMapper;

    @Override
    public String resourceType() {
        return ResourceTypes.AI_PROVIDER_CONNECTION;
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
                .requiredField("displayName")
                .requiredField("providerType")
                .requiredField("baseUrl")
                .fieldDescription("targetId", "供应商连接稳定 ID。")
                .fieldDescription("tenantId", "供应商连接所属租户。")
                .fieldDescription("code", "租户内唯一的供应商连接编码。")
                .fieldDescription("displayName", "供应商显示名称。")
                .fieldDescription("providerType", "AI 供应商类型。")
                .fieldDescription("baseUrl", "供应商 API 基础地址。")
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResourceSyncResult upsert(ResourceDeclaration resource) {
        String tenantId = AiResourceFields.requiredText(resource, "tenantId");
        String code = AiResourceFields.requiredText(resource, "code");
        AiProviderConnectionEntity existing = find(tenantId, code);
        if (existing != null) {
            return result(existing.getId(), "AI provider preserved: " + code);
        }

        AiProviderConnectionEntity provider = new AiProviderConnectionEntity();
        provider.setId(AiResourceFields.targetId(resource));
        provider.setTenantId(tenantId);
        provider.setCode(code);
        provider.setDisplayName(AiResourceFields.requiredText(resource, "displayName"));
        provider.setProviderType(AiResourceFields.requiredEnum(
                resource, "providerType", AiProviderType.class));
        provider.setBaseUrl(AiResourceFields.requiredText(resource, "baseUrl"));
        provider.setApiKeyCiphertext("");
        provider.setApiKeyHint("");
        provider.setEnabled(false);
        Require.isTrue(providerMapper.insert(provider) > 0, "AI provider initialization failed: " + code);
        return result(provider.getId(), "AI provider initialized: " + code);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResourceSyncResult disable(ResourceDeclaration resource) {
        String tenantId = AiResourceFields.requiredText(resource, "tenantId");
        String code = AiResourceFields.requiredText(resource, "code");
        AiProviderConnectionEntity provider = find(tenantId, code);
        if (provider == null) {
            return result(null, "AI provider not found: " + code);
        }
        provider.setEnabled(false);
        Require.isTrue(providerMapper.updateById(provider) > 0, "AI provider disable failed: " + code);
        return result(provider.getId(), "AI provider disabled: " + code);
    }

    private AiProviderConnectionEntity find(String tenantId, String code) {
        return providerMapper.selectOne(new LambdaQueryWrapper<AiProviderConnectionEntity>()
                .eq(AiProviderConnectionEntity::getTenantId, tenantId)
                .eq(AiProviderConnectionEntity::getCode, code));
    }

    private ResourceSyncResult result(Long targetId, String message) {
        return ResourceSyncResult.of(targetId, TARGET_TABLE, message);
    }
}
