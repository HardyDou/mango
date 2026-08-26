package io.mango.ai.core.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.ai.api.command.CreateAiPromptCommand;
import io.mango.ai.api.command.CreateAiSkillCommand;
import io.mango.ai.api.command.CreateAiToolCommand;
import io.mango.ai.api.command.UpdateAiPromptCommand;
import io.mango.ai.api.enums.AiPromptStatus;
import io.mango.ai.api.enums.AiToolType;
import io.mango.ai.core.entity.AiPromptEntity;
import io.mango.ai.core.entity.AiServiceEntity;
import io.mango.ai.core.entity.AiSkillEntity;
import io.mango.ai.core.entity.AiToolEntity;
import io.mango.ai.core.mapper.AiPromptMapper;
import io.mango.ai.core.mapper.AiServiceMapper;
import io.mango.ai.core.mapper.AiSkillMapper;
import io.mango.ai.core.mapper.AiToolMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiConfigurationServiceTest {
    private final AiPromptMapper promptMapper = mock(AiPromptMapper.class);
    private final AiSkillMapper skillMapper = mock(AiSkillMapper.class);
    private final AiToolMapper toolMapper = mock(AiToolMapper.class);
    private final AiServiceMapper serviceMapper = mock(AiServiceMapper.class);
    private final AiConfigurationService service = new AiConfigurationService(
            promptMapper, skillMapper, toolMapper, serviceMapper, new ObjectMapper());

    @Test
    void createsPromptAsDraftVersionOne() {
        CreateAiPromptCommand command = promptCommand();
        when(promptMapper.insert(any(AiPromptEntity.class))).thenReturn(1);

        Long id = service.createPrompt(command);

        ArgumentCaptor<AiPromptEntity> captor = ArgumentCaptor.forClass(AiPromptEntity.class);
        verify(promptMapper).insert(captor.capture());
        assertEquals(true, id > 0);
        assertEquals(AiPromptStatus.DRAFT, captor.getValue().getStatus());
        assertEquals(1, captor.getValue().getVersion());
    }

    @Test
    void updatingPromptIncrementsVersionAndReturnsToDraft() {
        AiPromptEntity entity = new AiPromptEntity();
        entity.setId(11L);
        entity.setVersion(3);
        entity.setStatus(AiPromptStatus.PUBLISHED);
        entity.setPublishedAt(LocalDateTime.now());
        when(promptMapper.selectById(11L)).thenReturn(entity);

        UpdateAiPromptCommand command = new UpdateAiPromptCommand();
        command.setId(11L);
        command.setCode("invoice");
        command.setName("Invoice");
        command.setTemplate("Extract invoice fields");
        when(promptMapper.updateById(entity)).thenReturn(1);

        assertEquals(true, service.updatePrompt(command));
        assertEquals(AiPromptStatus.DRAFT, entity.getStatus());
        assertEquals(4, entity.getVersion());
        assertNull(entity.getPublishedAt());
    }

    @Test
    void publishingPromptRequiresNonEmptyTemplate() {
        AiPromptEntity entity = new AiPromptEntity();
        entity.setId(12L);
        entity.setTemplate(" ");
        when(promptMapper.selectById(12L)).thenReturn(entity);

        assertThrows(RuntimeException.class, () -> service.publishPrompt(12L));
    }

    @Test
    void deletingReferencedPromptIsRejected() {
        AiPromptEntity entity = new AiPromptEntity();
        entity.setId(13L);
        when(promptMapper.selectById(13L)).thenReturn(entity);
        when(serviceMapper.selectCount(any())).thenReturn(1L);

        assertThrows(RuntimeException.class, () -> service.deletePrompt(13L));
    }

    @Test
    void skillReferencesMustResolveToExistingTools() {
        CreateAiSkillCommand command = new CreateAiSkillCommand();
        command.setCode("invoice");
        command.setName("Invoice Skill");
        command.setInstructions("Extract invoice data");
        command.setToolIds(Set.of(99L));
        command.setEnabled(true);
        when(toolMapper.selectCount(any())).thenReturn(0L);

        assertThrows(RuntimeException.class, () -> service.createSkill(command));
    }

    @Test
    void toolSchemaMustBeJsonObject() {
        CreateAiToolCommand command = new CreateAiToolCommand();
        command.setCode("invoice");
        command.setName("Invoice Tool");
        command.setToolType(AiToolType.HTTP);
        command.setEndpoint("https://example.test/invoice");
        command.setInputSchemaJson("[]");
        command.setOutputSchemaJson("{}");
        command.setEnabled(true);

        assertThrows(RuntimeException.class, () -> service.createTool(command));
    }

    private CreateAiPromptCommand promptCommand() {
        CreateAiPromptCommand command = new CreateAiPromptCommand();
        command.setCode("invoice");
        command.setName("Invoice");
        command.setTemplate("Extract invoice fields");
        return command;
    }
}
