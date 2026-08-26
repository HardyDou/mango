package io.mango.ai.core.service;

import io.mango.ai.api.command.CreateAiPromptCommand;
import io.mango.ai.api.command.CreateAiServiceCommand;
import io.mango.ai.api.command.CreateAiSkillCommand;
import io.mango.ai.api.command.CreateAiToolCommand;
import io.mango.ai.api.command.UpdateAiPromptCommand;
import io.mango.ai.api.command.UpdateAiServiceCommand;
import io.mango.ai.api.command.UpdateAiSkillCommand;
import io.mango.ai.api.command.UpdateAiToolCommand;
import io.mango.ai.api.vo.AiPromptVO;
import io.mango.ai.api.vo.AiServiceVO;
import io.mango.ai.api.vo.AiSkillVO;
import io.mango.ai.api.vo.AiToolVO;

import java.util.List;

/** AI Prompt、Skill、工具和服务配置服务。 */
public interface IAiConfigurationService {
    List<AiPromptVO> prompts();
    Long createPrompt(CreateAiPromptCommand command);
    Boolean updatePrompt(UpdateAiPromptCommand command);
    Boolean deletePrompt(Long id);
    Boolean publishPrompt(Long id);

    List<AiSkillVO> skills();
    Long createSkill(CreateAiSkillCommand command);
    Boolean updateSkill(UpdateAiSkillCommand command);
    Boolean deleteSkill(Long id);

    List<AiToolVO> tools();
    Long createTool(CreateAiToolCommand command);
    Boolean updateTool(UpdateAiToolCommand command);
    Boolean deleteTool(Long id);

    List<AiServiceVO> services();
    Long createService(CreateAiServiceCommand command);
    Boolean updateService(UpdateAiServiceCommand command);
    Boolean deleteService(Long id);
}
