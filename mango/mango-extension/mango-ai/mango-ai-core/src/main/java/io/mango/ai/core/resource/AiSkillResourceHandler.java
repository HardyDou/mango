package io.mango.ai.core.resource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.ai.core.entity.AiSkillEntity;
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

/** 首次初始化 AI Skill，不覆盖租户后续编辑。 */
@Component
@RequiredArgsConstructor
public class AiSkillResourceHandler implements ResourceHandler {

    private static final String TARGET_TABLE = "ai_skill";

    private final AiSkillMapper skillMapper;

    @Override
    public String resourceType() {
        return ResourceTypes.AI_SKILL;
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
                .requiredField("instructions")
                .requiredField("toolIdsJson")
                .requiredField("enabled")
                .fieldDescription("targetId", "Skill 稳定 ID。")
                .fieldDescription("tenantId", "Skill 所属租户。")
                .fieldDescription("code", "租户内唯一的 Skill 编码。")
                .fieldDescription("name", "Skill 名称。")
                .fieldDescription("description", "Skill 用途说明。")
                .fieldDescription("instructions", "Skill 执行指令。")
                .fieldDescription("toolIdsJson", "Skill 绑定工具 ID JSON 数组。")
                .fieldDescription("enabled", "是否启用。")
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResourceSyncResult upsert(ResourceDeclaration resource) {
        String tenantId = AiResourceFields.requiredText(resource, "tenantId");
        String code = AiResourceFields.requiredText(resource, "code");
        AiSkillEntity existing = find(tenantId, code);
        if (existing != null) {
            return result(existing.getId(), "AI skill preserved: " + code);
        }

        AiSkillEntity skill = new AiSkillEntity();
        skill.setId(AiResourceFields.targetId(resource));
        skill.setTenantId(tenantId);
        skill.setCode(code);
        skill.setName(AiResourceFields.requiredText(resource, "name"));
        skill.setDescription(AiResourceFields.optionalText(resource, "description"));
        skill.setInstructions(AiResourceFields.requiredText(resource, "instructions"));
        skill.setToolIdsJson(AiResourceFields.requiredText(resource, "toolIdsJson"));
        skill.setEnabled(AiResourceFields.requiredBoolean(resource, "enabled"));
        Require.isTrue(skillMapper.insert(skill) > 0, "AI skill initialization failed: " + code);
        return result(skill.getId(), "AI skill initialized: " + code);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResourceSyncResult disable(ResourceDeclaration resource) {
        String tenantId = AiResourceFields.requiredText(resource, "tenantId");
        String code = AiResourceFields.requiredText(resource, "code");
        AiSkillEntity skill = find(tenantId, code);
        if (skill == null) {
            return result(null, "AI skill not found: " + code);
        }
        skill.setEnabled(false);
        Require.isTrue(skillMapper.updateById(skill) > 0, "AI skill disable failed: " + code);
        return result(skill.getId(), "AI skill disabled: " + code);
    }

    private AiSkillEntity find(String tenantId, String code) {
        return skillMapper.selectOne(new LambdaQueryWrapper<AiSkillEntity>()
                .eq(AiSkillEntity::getTenantId, tenantId)
                .eq(AiSkillEntity::getCode, code));
    }

    private ResourceSyncResult result(Long targetId, String message) {
        return ResourceSyncResult.of(targetId, TARGET_TABLE, message);
    }
}
