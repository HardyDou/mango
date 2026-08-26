package io.mango.ai.api;

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
import io.mango.common.result.R;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

/** AI Prompt、Skill、工具和服务配置契约。 */
public interface AiConfigurationApi {
    R<List<AiPromptVO>> prompts();
    R<Long> createPrompt(@Valid CreateAiPromptCommand command);
    R<Boolean> updatePrompt(@Valid UpdateAiPromptCommand command);
    R<Boolean> deletePrompt(@NotNull @Positive Long id);
    R<Boolean> publishPrompt(@NotNull @Positive Long id);

    R<List<AiSkillVO>> skills();
    R<Long> createSkill(@Valid CreateAiSkillCommand command);
    R<Boolean> updateSkill(@Valid UpdateAiSkillCommand command);
    R<Boolean> deleteSkill(@NotNull @Positive Long id);

    R<List<AiToolVO>> tools();
    R<Long> createTool(@Valid CreateAiToolCommand command);
    R<Boolean> updateTool(@Valid UpdateAiToolCommand command);
    R<Boolean> deleteTool(@NotNull @Positive Long id);

    R<List<AiServiceVO>> services();
    R<Long> createService(@Valid CreateAiServiceCommand command);
    R<Boolean> updateService(@Valid UpdateAiServiceCommand command);
    R<Boolean> deleteService(@NotNull @Positive Long id);
}
