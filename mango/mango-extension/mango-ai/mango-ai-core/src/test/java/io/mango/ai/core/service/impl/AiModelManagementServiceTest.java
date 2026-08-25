package io.mango.ai.core.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.ai.api.command.CreateAiProviderConnectionCommand;
import io.mango.ai.api.command.SetAiCapabilityRouteCommand;
import io.mango.ai.api.enums.AiApiProtocol;
import io.mango.ai.api.enums.AiCapability;
import io.mango.ai.core.entity.AiModelEntity;
import io.mango.ai.core.entity.AiCapabilityRouteEntity;
import io.mango.ai.api.enums.AiProviderType;
import io.mango.ai.api.enums.AiModality;
import io.mango.ai.core.mapper.AiCapabilityRouteMapper;
import io.mango.ai.core.mapper.AiModelMapper;
import io.mango.ai.core.mapper.AiProviderConnectionMapper;
import io.mango.ai.core.service.impl.provider.OpenAiCompatibleEndpoint;
import io.mango.ai.core.service.impl.provider.OpenAiResponsesChatModel;
import io.mango.ai.core.entity.AiProviderConnectionEntity;
import io.mango.infra.crypto.impl.ICryptoService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AiModelManagementServiceTest {
    private final AiProviderConnectionMapper providerMapper = mock(AiProviderConnectionMapper.class);
    private final AiModelMapper modelMapper = mock(AiModelMapper.class);
    private final AiCapabilityRouteMapper routeMapper = mock(AiCapabilityRouteMapper.class);
    private final ICryptoService cryptoService = mock(ICryptoService.class);
    private final AiModelManagementService service = new AiModelManagementService(
            providerMapper, modelMapper, routeMapper, cryptoService, new ObjectMapper());

    @Test
    void exposesAllSupportedProviderTypes() {
        assertEquals(8, service.providerTypes().size());
        assertEquals(AiProviderType.OLLAMA, service.providerTypes().get(7).getCode());
    }

    @Test
    void ollamaConnectionMayOmitApiKey() {
        CreateAiProviderConnectionCommand command = new CreateAiProviderConnectionCommand();
        command.setCode("local-ollama");
        command.setDisplayName("本地 Ollama");
        command.setProviderType(AiProviderType.OLLAMA);
        command.setBaseUrl("http://localhost:11434");
        command.setApiKey("");
        command.setEnabled(true);

        Long id = service.createProvider(command);

        verify(providerMapper).insert(any(AiProviderConnectionEntity.class));
        assertEquals(true, id > 0);
    }

    @Test
    void resolvesOllamaChatModelWithoutDecryptingApiKey() {
        AiModelEntity model = new AiModelEntity();
        model.setId(13L);
        model.setProviderConnectionId(14L);
        model.setModelName("llama3.2");
        model.setApiProtocol(AiApiProtocol.CHAT_COMPLETIONS);
        model.setCapabilitiesJson("[\"CHAT\"]");
        model.setEnabled(true);
        AiProviderConnectionEntity provider = new AiProviderConnectionEntity();
        provider.setId(14L);
        provider.setCode("local-ollama");
        provider.setProviderType(AiProviderType.OLLAMA);
        provider.setBaseUrl("http://localhost:11434");
        provider.setApiKeyCiphertext("");
        provider.setEnabled(true);
        when(modelMapper.selectById(13L)).thenReturn(model);
        when(providerMapper.selectById(14L)).thenReturn(provider);

        OpenAiChatModel chatModel = assertInstanceOf(
                OpenAiChatModel.class,
                service.resolveChatModel(13L).getChatModel());
        OpenAiChatOptions options = assertInstanceOf(OpenAiChatOptions.class, chatModel.getDefaultOptions());

        assertEquals("llama3.2", options.getModel());
        verifyNoInteractions(cryptoService);
    }

    @Test
    void createsNewCapabilityRoute() {
        AiModelEntity model = new AiModelEntity();
        model.setId(11L);
        model.setProviderConnectionId(12L);
        model.setEnabled(true);
        model.setApiProtocol(AiApiProtocol.CHAT_COMPLETIONS);
        model.setCapabilitiesJson("[\"CHAT\"]");
        AiProviderConnectionEntity provider = new AiProviderConnectionEntity();
        provider.setId(12L);
        provider.setEnabled(true);
        provider.setProviderType(AiProviderType.OPENAI_COMPATIBLE);
        when(modelMapper.selectById(11L)).thenReturn(model);
        when(providerMapper.selectById(12L)).thenReturn(provider);
        when(routeMapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(null);

        SetAiCapabilityRouteCommand command = new SetAiCapabilityRouteCommand();
        command.setModelId(11L);
        command.setCapability(AiCapability.CHAT);

        assertEquals(true, service.setRoute(command));
        verify(routeMapper).insert(org.mockito.ArgumentMatchers.any(AiCapabilityRouteEntity.class));
    }

    @Test
    void normalizesOpenAiCompatibleBaseUrlForSpringAi() {
        assertEquals("https://example.test", OpenAiCompatibleEndpoint.apiBaseUrl("https://example.test/v1/"));
        assertEquals("https://example.test", OpenAiCompatibleEndpoint.apiBaseUrl("https://example.test"));
        assertEquals("https://example.test/v1/responses",
                OpenAiCompatibleEndpoint.responsesUrl("https://example.test/v1/"));
    }

    @Test
    void requestsUsageForOpenAiCompatibleStreamingChat() {
        AiCapabilityRouteEntity route = new AiCapabilityRouteEntity();
        route.setModelId(21L);
        AiModelEntity model = new AiModelEntity();
        model.setId(21L);
        model.setProviderConnectionId(22L);
        model.setModelName("chat-model");
        model.setApiProtocol(AiApiProtocol.CHAT_COMPLETIONS);
        model.setCapabilitiesJson("[\"CHAT\"]");
        model.setEnabled(true);
        AiProviderConnectionEntity provider = new AiProviderConnectionEntity();
        provider.setId(22L);
        provider.setCode("openai-compatible");
        provider.setProviderType(AiProviderType.OPENAI_COMPATIBLE);
        provider.setBaseUrl("https://example.test/v1");
        provider.setApiKeyCiphertext("encrypted-secret");
        provider.setEnabled(true);
        when(routeMapper.selectOne(any())).thenReturn(route);
        when(modelMapper.selectById(21L)).thenReturn(model);
        when(providerMapper.selectById(22L)).thenReturn(provider);
        when(cryptoService.decrypt("encrypted-secret")).thenReturn("test-secret");

        OpenAiChatModel chatModel = assertInstanceOf(
                OpenAiChatModel.class,
                service.resolveChatModel(21L).getChatModel());
        OpenAiChatOptions options = assertInstanceOf(OpenAiChatOptions.class, chatModel.getDefaultOptions());

        assertEquals(true, options.getStreamUsage());
    }

    @Test
    void resolvesResponsesProtocolAsSpringAiChatModel() {
        AiCapabilityRouteEntity route = new AiCapabilityRouteEntity();
        route.setModelId(31L);
        AiModelEntity model = new AiModelEntity();
        model.setId(31L);
        model.setProviderConnectionId(32L);
        model.setModelName("responses-model");
        model.setApiProtocol(AiApiProtocol.RESPONSES);
        model.setCapabilitiesJson("[\"CHAT\"]");
        model.setInputModalitiesJson("[\"TEXT\",\"IMAGE\",\"AUDIO\",\"VIDEO\",\"FILE\"]");
        model.setEnabled(true);
        AiProviderConnectionEntity provider = new AiProviderConnectionEntity();
        provider.setId(32L);
        provider.setCode("openai-compatible");
        provider.setProviderType(AiProviderType.OPENAI_COMPATIBLE);
        provider.setBaseUrl("https://example.test/v1");
        provider.setApiKeyCiphertext("encrypted-secret");
        provider.setEnabled(true);
        when(routeMapper.selectOne(any())).thenReturn(route);
        when(modelMapper.selectById(31L)).thenReturn(model);
        when(providerMapper.selectById(32L)).thenReturn(provider);
        when(cryptoService.decrypt("encrypted-secret")).thenReturn("test-secret");

        var resolution = service.resolveChatModel(31L);
        assertInstanceOf(
                OpenAiResponsesChatModel.class,
                resolution.getChatModel());
        assertEquals(java.util.Set.of(AiModality.TEXT, AiModality.IMAGE, AiModality.FILE),
                resolution.getInputModalities());
    }

    @Test
    void chatAdapter_只暴露真实支持的输入输出模态() {
        AiModelEntity model = new AiModelEntity();
        model.setId(41L);
        model.setProviderConnectionId(42L);
        model.setModelName("multimodal-model");
        model.setApiProtocol(AiApiProtocol.CHAT_COMPLETIONS);
        model.setCapabilitiesJson("[\"CHAT\"]");
        model.setInputModalitiesJson("[\"TEXT\",\"IMAGE\",\"VIDEO\"]");
        model.setOutputModalitiesJson("[\"TEXT\",\"VIDEO\"]");
        model.setEnabled(true);
        AiProviderConnectionEntity provider = new AiProviderConnectionEntity();
        provider.setId(42L);
        provider.setCode("openai-compatible");
        provider.setProviderType(AiProviderType.OPENAI_COMPATIBLE);
        provider.setBaseUrl("https://example.test/v1");
        provider.setApiKeyCiphertext("encrypted-secret");
        provider.setEnabled(true);
        when(modelMapper.selectById(41L)).thenReturn(model);
        when(providerMapper.selectById(42L)).thenReturn(provider);
        when(cryptoService.decrypt("encrypted-secret")).thenReturn("test-secret");

        var resolution = service.resolveChatModel(41L);

        assertTrue(resolution.getInputModalities().contains(AiModality.IMAGE));
        assertFalse(resolution.getInputModalities().contains(AiModality.VIDEO));
        assertEquals(java.util.Set.of(AiModality.TEXT), resolution.getOutputModalities());
    }
}
