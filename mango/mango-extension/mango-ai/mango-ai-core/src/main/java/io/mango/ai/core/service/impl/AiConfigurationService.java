package io.mango.ai.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.ai.api.command.CreateAiPromptCommand;
import io.mango.ai.api.command.CreateAiServiceCommand;
import io.mango.ai.api.command.CreateAiSkillCommand;
import io.mango.ai.api.command.CreateAiToolCommand;
import io.mango.ai.api.command.UpdateAiPromptCommand;
import io.mango.ai.api.command.UpdateAiServiceCommand;
import io.mango.ai.api.command.UpdateAiSkillCommand;
import io.mango.ai.api.command.UpdateAiToolCommand;
import io.mango.ai.api.enums.AiCode;
import io.mango.ai.api.enums.AiPromptStatus;
import io.mango.ai.api.vo.AiPromptVO;
import io.mango.ai.api.vo.AiServiceVO;
import io.mango.ai.api.vo.AiSkillVO;
import io.mango.ai.api.vo.AiToolVO;
import io.mango.ai.core.entity.AiPromptEntity;
import io.mango.ai.core.entity.AiServiceEntity;
import io.mango.ai.core.entity.AiSkillEntity;
import io.mango.ai.core.entity.AiToolEntity;
import io.mango.ai.core.mapper.AiPromptMapper;
import io.mango.ai.core.mapper.AiServiceMapper;
import io.mango.ai.core.mapper.AiSkillMapper;
import io.mango.ai.core.mapper.AiToolMapper;
import io.mango.ai.core.service.IAiConfigurationService;
import io.mango.common.result.Require;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** AI Prompt、Skill、工具和服务配置实现。 */
@Service
@RequiredArgsConstructor
public class AiConfigurationService implements IAiConfigurationService {
    private static final TypeReference<Set<Long>> TOOL_ID_TYPE = new TypeReference<>() { };

    private final AiPromptMapper promptMapper;
    private final AiSkillMapper skillMapper;
    private final AiToolMapper toolMapper;
    private final AiServiceMapper serviceMapper;
    private final ObjectMapper objectMapper;

    @Override
    public List<AiPromptVO> prompts() {
        return promptMapper.selectList(new LambdaQueryWrapper<AiPromptEntity>()
                        .orderByDesc(AiPromptEntity::getUpdatedAt))
                .stream().map(this::toPrompt).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPrompt(CreateAiPromptCommand command) {
        Require.notNull(command, AiCode.CONFIG_INVALID);
        validateJson(command.getVariablesJson(), "Prompt 变量必须是 JSON 对象");
        AiPromptEntity entity = new AiPromptEntity();
        entity.setId(IdWorker.getId());
        applyPrompt(entity, command);
        entity.setStatus(AiPromptStatus.DRAFT);
        entity.setVersion(1);
        insertPrompt(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updatePrompt(UpdateAiPromptCommand command) {
        Require.notNull(command, AiCode.CONFIG_INVALID);
        validateJson(command.getVariablesJson(), "Prompt 变量必须是 JSON 对象");
        AiPromptEntity entity = Require.nonNull(promptMapper.selectById(command.getId()), AiCode.PROMPT_NOT_FOUND);
        applyPrompt(entity, command);
        entity.setStatus(AiPromptStatus.DRAFT);
        entity.setVersion(entity.getVersion() == null ? 1 : entity.getVersion() + 1);
        entity.setPublishedAt(null);
        try {
            Require.isTrue(promptMapper.updateById(entity) > 0, AiCode.PROMPT_NOT_FOUND);
        } catch (DuplicateKeyException exception) {
            Require.fail(AiCode.PROMPT_CONFLICT, AiCode.PROMPT_CONFLICT.getMessage(), exception);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deletePrompt(Long id) {
        AiPromptEntity entity = Require.nonNull(promptMapper.selectById(id), AiCode.PROMPT_NOT_FOUND);
        Require.isTrue(serviceMapper.selectCount(new LambdaQueryWrapper<AiServiceEntity>()
                .eq(AiServiceEntity::getPromptId, entity.getId())) == 0, AiCode.PROMPT_REFERENCED);
        return promptMapper.deleteById(entity.getId()) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean publishPrompt(Long id) {
        AiPromptEntity entity = Require.nonNull(promptMapper.selectById(id), AiCode.PROMPT_NOT_FOUND);
        Require.isTrue(StringUtils.hasText(entity.getTemplate()), AiCode.CONFIG_INVALID, "Prompt 模板不能为空");
        entity.setStatus(AiPromptStatus.PUBLISHED);
        entity.setPublishedAt(LocalDateTime.now());
        return promptMapper.updateById(entity) > 0;
    }

    @Override
    public List<AiSkillVO> skills() {
        return skillMapper.selectList(new LambdaQueryWrapper<AiSkillEntity>()
                        .orderByDesc(AiSkillEntity::getUpdatedAt))
                .stream().map(this::toSkill).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSkill(CreateAiSkillCommand command) {
        Require.notNull(command, AiCode.CONFIG_INVALID);
        validateToolIds(command.getToolIds());
        AiSkillEntity entity = new AiSkillEntity();
        entity.setId(IdWorker.getId());
        applySkill(entity, command);
        insertSkill(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateSkill(UpdateAiSkillCommand command) {
        Require.notNull(command, AiCode.CONFIG_INVALID);
        validateToolIds(command.getToolIds());
        AiSkillEntity entity = Require.nonNull(skillMapper.selectById(command.getId()), AiCode.SKILL_NOT_FOUND);
        applySkill(entity, command);
        try {
            Require.isTrue(skillMapper.updateById(entity) > 0, AiCode.SKILL_NOT_FOUND);
        } catch (DuplicateKeyException exception) {
            Require.fail(AiCode.SKILL_CONFLICT, AiCode.SKILL_CONFLICT.getMessage(), exception);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteSkill(Long id) {
        AiSkillEntity entity = Require.nonNull(skillMapper.selectById(id), AiCode.SKILL_NOT_FOUND);
        Require.isTrue(serviceMapper.selectCount(new LambdaQueryWrapper<AiServiceEntity>()
                .eq(AiServiceEntity::getSkillId, entity.getId())) == 0, AiCode.SKILL_REFERENCED);
        return skillMapper.deleteById(entity.getId()) > 0;
    }

    @Override
    public List<AiToolVO> tools() {
        return toolMapper.selectList(new LambdaQueryWrapper<AiToolEntity>()
                        .orderByDesc(AiToolEntity::getUpdatedAt))
                .stream().map(this::toTool).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTool(CreateAiToolCommand command) {
        Require.notNull(command, AiCode.CONFIG_INVALID);
        validateJson(command.getInputSchemaJson(), "工具输入 Schema 必须是 JSON 对象");
        validateJson(command.getOutputSchemaJson(), "工具输出 Schema 必须是 JSON 对象");
        AiToolEntity entity = new AiToolEntity();
        entity.setId(IdWorker.getId());
        applyTool(entity, command);
        insertTool(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateTool(UpdateAiToolCommand command) {
        Require.notNull(command, AiCode.CONFIG_INVALID);
        validateJson(command.getInputSchemaJson(), "工具输入 Schema 必须是 JSON 对象");
        validateJson(command.getOutputSchemaJson(), "工具输出 Schema 必须是 JSON 对象");
        AiToolEntity entity = Require.nonNull(toolMapper.selectById(command.getId()), AiCode.TOOL_NOT_FOUND);
        applyTool(entity, command);
        try {
            Require.isTrue(toolMapper.updateById(entity) > 0, AiCode.TOOL_NOT_FOUND);
        } catch (DuplicateKeyException exception) {
            Require.fail(AiCode.TOOL_CONFLICT, AiCode.TOOL_CONFLICT.getMessage(), exception);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteTool(Long id) {
        AiToolEntity entity = Require.nonNull(toolMapper.selectById(id), AiCode.TOOL_NOT_FOUND);
        boolean referenced = skillMapper.selectList(new LambdaQueryWrapper<AiSkillEntity>()).stream()
                .map(AiSkillEntity::getToolIdsJson)
                .filter(StringUtils::hasText)
                .map(this::readToolIds)
                .anyMatch(ids -> ids.contains(entity.getId()));
        Require.isFalse(referenced, AiCode.TOOL_REFERENCED);
        return toolMapper.deleteById(entity.getId()) > 0;
    }

    @Override
    public List<AiServiceVO> services() {
        List<AiPromptEntity> prompts = promptMapper.selectList(new LambdaQueryWrapper<AiPromptEntity>());
        List<AiSkillEntity> skills = skillMapper.selectList(new LambdaQueryWrapper<AiSkillEntity>());
        return serviceMapper.selectList(new LambdaQueryWrapper<AiServiceEntity>()
                        .orderByDesc(AiServiceEntity::getUpdatedAt))
                .stream().map(service -> toService(service, prompts, skills)).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createService(CreateAiServiceCommand command) {
        Require.notNull(command, AiCode.CONFIG_INVALID);
        validateReferences(command.getPromptId(), command.getSkillId());
        validateJson(command.getInputSchemaJson(), "服务输入 Schema 必须是 JSON 对象");
        validateJson(command.getOutputSchemaJson(), "服务输出 Schema 必须是 JSON 对象");
        AiServiceEntity entity = new AiServiceEntity();
        entity.setId(IdWorker.getId());
        applyService(entity, command);
        insertService(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateService(UpdateAiServiceCommand command) {
        Require.notNull(command, AiCode.CONFIG_INVALID);
        validateReferences(command.getPromptId(), command.getSkillId());
        validateJson(command.getInputSchemaJson(), "服务输入 Schema 必须是 JSON 对象");
        validateJson(command.getOutputSchemaJson(), "服务输出 Schema 必须是 JSON 对象");
        AiServiceEntity entity = Require.nonNull(serviceMapper.selectById(command.getId()), AiCode.SERVICE_NOT_FOUND);
        applyService(entity, command);
        try {
            Require.isTrue(serviceMapper.updateById(entity) > 0, AiCode.SERVICE_NOT_FOUND);
        } catch (DuplicateKeyException exception) {
            Require.fail(AiCode.SERVICE_CONFLICT, AiCode.SERVICE_CONFLICT.getMessage(), exception);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteService(Long id) {
        AiServiceEntity entity = Require.nonNull(serviceMapper.selectById(id), AiCode.SERVICE_NOT_FOUND);
        return serviceMapper.deleteById(entity.getId()) > 0;
    }

    private void applyPrompt(AiPromptEntity entity, CreateAiPromptCommand command) {
        entity.setCode(command.getCode().trim());
        entity.setName(command.getName().trim());
        entity.setDescription(trimToNull(command.getDescription()));
        entity.setTemplate(command.getTemplate().trim());
        entity.setVariablesJson(trimToNull(command.getVariablesJson()));
    }

    private void applySkill(AiSkillEntity entity, CreateAiSkillCommand command) {
        entity.setCode(command.getCode().trim());
        entity.setName(command.getName().trim());
        entity.setDescription(trimToNull(command.getDescription()));
        entity.setInstructions(command.getInstructions().trim());
        entity.setToolIdsJson(write(command.getToolIds() == null ? Set.of() : command.getToolIds()));
        entity.setEnabled(command.getEnabled());
    }

    private void applyTool(AiToolEntity entity, CreateAiToolCommand command) {
        entity.setCode(command.getCode().trim());
        entity.setName(command.getName().trim());
        entity.setDescription(trimToNull(command.getDescription()));
        entity.setToolType(command.getToolType());
        entity.setEndpoint(command.getEndpoint().trim());
        entity.setInputSchemaJson(command.getInputSchemaJson().trim());
        entity.setOutputSchemaJson(command.getOutputSchemaJson().trim());
        entity.setEnabled(command.getEnabled());
    }

    private void applyService(AiServiceEntity entity, CreateAiServiceCommand command) {
        entity.setCode(command.getCode().trim());
        entity.setName(command.getName().trim());
        entity.setDescription(trimToNull(command.getDescription()));
        entity.setServiceType(command.getServiceType());
        entity.setCapability(command.getCapability());
        entity.setPromptId(command.getPromptId());
        entity.setSkillId(command.getSkillId());
        entity.setInputSchemaJson(command.getInputSchemaJson().trim());
        entity.setOutputSchemaJson(command.getOutputSchemaJson().trim());
        entity.setEnabled(command.getEnabled());
    }

    private void validateToolIds(Set<Long> ids) {
        Require.isTrue(ids == null || ids.stream().noneMatch(id -> id == null || id <= 0),
                AiCode.CONFIG_INVALID, "Skill 工具引用不合法");
        if (ids == null || ids.isEmpty()) {
            return;
        }
        Require.isTrue(toolMapper.selectCount(new LambdaQueryWrapper<AiToolEntity>().in(AiToolEntity::getId, ids)) == ids.size(),
                AiCode.TOOL_NOT_FOUND);
    }

    private void validateReferences(Long promptId, Long skillId) {
        if (promptId != null) {
            Require.nonNull(promptMapper.selectById(promptId), AiCode.PROMPT_NOT_FOUND);
        }
        if (skillId != null) {
            Require.nonNull(skillMapper.selectById(skillId), AiCode.SKILL_NOT_FOUND);
        }
    }

    private void validateJson(String value, String message) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        try {
            JsonNode node = objectMapper.readTree(value);
            Require.isTrue(node != null && node.isObject(), AiCode.CONFIG_INVALID, message);
        } catch (JsonProcessingException exception) {
            Require.fail(AiCode.CONFIG_INVALID, message, exception);
        }
    }

    private void insertPrompt(AiPromptEntity entity) {
        try {
            Require.isTrue(promptMapper.insert(entity) > 0, AiCode.CONFIG_INVALID);
        } catch (DuplicateKeyException exception) {
            Require.fail(AiCode.PROMPT_CONFLICT, AiCode.PROMPT_CONFLICT.getMessage(), exception);
        }
    }

    private void insertSkill(AiSkillEntity entity) {
        try {
            Require.isTrue(skillMapper.insert(entity) > 0, AiCode.CONFIG_INVALID);
        } catch (DuplicateKeyException exception) {
            Require.fail(AiCode.SKILL_CONFLICT, AiCode.SKILL_CONFLICT.getMessage(), exception);
        }
    }

    private void insertTool(AiToolEntity entity) {
        try {
            Require.isTrue(toolMapper.insert(entity) > 0, AiCode.CONFIG_INVALID);
        } catch (DuplicateKeyException exception) {
            Require.fail(AiCode.TOOL_CONFLICT, AiCode.TOOL_CONFLICT.getMessage(), exception);
        }
    }

    private void insertService(AiServiceEntity entity) {
        try {
            Require.isTrue(serviceMapper.insert(entity) > 0, AiCode.CONFIG_INVALID);
        } catch (DuplicateKeyException exception) {
            Require.fail(AiCode.SERVICE_CONFLICT, AiCode.SERVICE_CONFLICT.getMessage(), exception);
        }
    }

    private AiPromptVO toPrompt(AiPromptEntity entity) {
        AiPromptVO vo = new AiPromptVO();
        vo.setId(entity.getId()); vo.setCode(entity.getCode()); vo.setName(entity.getName());
        vo.setDescription(entity.getDescription()); vo.setTemplate(entity.getTemplate());
        vo.setVariablesJson(entity.getVariablesJson()); vo.setStatus(entity.getStatus());
        vo.setVersion(entity.getVersion()); vo.setPublishedAt(entity.getPublishedAt()); vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private AiSkillVO toSkill(AiSkillEntity entity) {
        AiSkillVO vo = new AiSkillVO();
        vo.setId(entity.getId()); vo.setCode(entity.getCode()); vo.setName(entity.getName());
        vo.setDescription(entity.getDescription()); vo.setInstructions(entity.getInstructions());
        vo.setToolIds(readToolIds(entity.getToolIdsJson())); vo.setEnabled(entity.getEnabled()); vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private AiToolVO toTool(AiToolEntity entity) {
        AiToolVO vo = new AiToolVO();
        vo.setId(entity.getId()); vo.setCode(entity.getCode()); vo.setName(entity.getName());
        vo.setDescription(entity.getDescription()); vo.setToolType(entity.getToolType()); vo.setEndpoint(entity.getEndpoint());
        vo.setInputSchemaJson(entity.getInputSchemaJson()); vo.setOutputSchemaJson(entity.getOutputSchemaJson());
        vo.setEnabled(entity.getEnabled()); vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private AiServiceVO toService(AiServiceEntity entity, List<AiPromptEntity> prompts, List<AiSkillEntity> skills) {
        AiServiceVO vo = new AiServiceVO();
        vo.setId(entity.getId()); vo.setCode(entity.getCode()); vo.setName(entity.getName());
        vo.setDescription(entity.getDescription()); vo.setServiceType(entity.getServiceType()); vo.setCapability(entity.getCapability());
        vo.setPromptId(entity.getPromptId()); vo.setSkillId(entity.getSkillId()); vo.setInputSchemaJson(entity.getInputSchemaJson());
        vo.setOutputSchemaJson(entity.getOutputSchemaJson()); vo.setEnabled(entity.getEnabled()); vo.setUpdatedAt(entity.getUpdatedAt());
        prompts.stream().filter(prompt -> prompt.getId().equals(entity.getPromptId())).findFirst().ifPresent(prompt -> vo.setPromptName(prompt.getName()));
        skills.stream().filter(skill -> skill.getId().equals(entity.getSkillId())).findFirst().ifPresent(skill -> vo.setSkillName(skill.getName()));
        return vo;
    }

    private Set<Long> readToolIds(String value) {
        if (!StringUtils.hasText(value)) {
            return Set.of();
        }
        try {
            return new LinkedHashSet<>(objectMapper.readValue(value, TOOL_ID_TYPE));
        } catch (JsonProcessingException exception) {
            Require.fail(AiCode.CONFIG_INVALID, "Skill 工具引用数据损坏", exception);
            return Set.of();
        }
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            Require.fail(AiCode.CONFIG_INVALID, "AI 配置序列化失败", exception);
            return "";
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
