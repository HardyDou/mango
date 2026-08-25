package io.mango.ai.core.resource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.ai.api.enums.AiPromptStatus;
import io.mango.ai.core.entity.AiPromptEntity;
import io.mango.ai.core.mapper.AiPromptMapper;
import io.mango.common.result.Require;
import io.mango.resource.support.ResourceHandler;
import io.mango.resource.support.ResourceTypes;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.support.model.ResourceHandlerSpec;
import io.mango.resource.support.model.ResourceSyncResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** 首次初始化 Prompt 模板，不覆盖租户后续编辑和发布状态。 */
@Component
@RequiredArgsConstructor
public class AiPromptResourceHandler implements ResourceHandler {

    private static final String TARGET_TABLE = "ai_prompt_template";

    private final AiPromptMapper promptMapper;

    @Override
    public String resourceType() {
        return ResourceTypes.AI_PROMPT;
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
                .requiredField("template")
                .requiredField("variablesJson")
                .requiredField("status")
                .requiredField("templateVersion")
                .fieldDescription("targetId", "Prompt 模板稳定 ID。")
                .fieldDescription("tenantId", "Prompt 模板所属租户。")
                .fieldDescription("code", "租户内唯一的 Prompt 编码。")
                .fieldDescription("name", "Prompt 名称。")
                .fieldDescription("description", "Prompt 用途说明。")
                .fieldDescription("template", "Prompt 模板正文。")
                .fieldDescription("variablesJson", "模板变量 JSON 数组。")
                .fieldDescription("status", "DRAFT、PUBLISHED 或 ARCHIVED。")
                .fieldDescription("templateVersion", "Prompt 模板版本。")
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResourceSyncResult upsert(ResourceDeclaration resource) {
        String tenantId = AiResourceFields.requiredText(resource, "tenantId");
        String code = AiResourceFields.requiredText(resource, "code");
        AiPromptEntity existing = find(tenantId, code);
        if (existing != null) {
            return result(existing.getId(), "AI prompt preserved: " + code);
        }

        AiPromptStatus status = AiResourceFields.requiredEnum(
                resource, "status", AiPromptStatus.class);
        AiPromptEntity prompt = new AiPromptEntity();
        prompt.setId(AiResourceFields.targetId(resource));
        prompt.setTenantId(tenantId);
        prompt.setCode(code);
        prompt.setName(AiResourceFields.requiredText(resource, "name"));
        prompt.setDescription(AiResourceFields.optionalText(resource, "description"));
        prompt.setTemplate(AiResourceFields.requiredText(resource, "template"));
        prompt.setVariablesJson(AiResourceFields.requiredText(resource, "variablesJson"));
        prompt.setStatus(status);
        prompt.setVersion(AiResourceFields.requiredInt(resource, "templateVersion"));
        prompt.setPublishedAt(status == AiPromptStatus.PUBLISHED ? LocalDateTime.now() : null);
        Require.isTrue(promptMapper.insert(prompt) > 0, "AI prompt initialization failed: " + code);
        return result(prompt.getId(), "AI prompt initialized: " + code);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResourceSyncResult disable(ResourceDeclaration resource) {
        String tenantId = AiResourceFields.requiredText(resource, "tenantId");
        String code = AiResourceFields.requiredText(resource, "code");
        AiPromptEntity prompt = find(tenantId, code);
        if (prompt == null) {
            return result(null, "AI prompt not found: " + code);
        }
        prompt.setStatus(AiPromptStatus.ARCHIVED);
        Require.isTrue(promptMapper.updateById(prompt) > 0, "AI prompt archive failed: " + code);
        return result(prompt.getId(), "AI prompt archived: " + code);
    }

    private AiPromptEntity find(String tenantId, String code) {
        return promptMapper.selectOne(new LambdaQueryWrapper<AiPromptEntity>()
                .eq(AiPromptEntity::getTenantId, tenantId)
                .eq(AiPromptEntity::getCode, code));
    }

    private ResourceSyncResult result(Long targetId, String message) {
        return ResourceSyncResult.of(targetId, TARGET_TABLE, message);
    }
}
