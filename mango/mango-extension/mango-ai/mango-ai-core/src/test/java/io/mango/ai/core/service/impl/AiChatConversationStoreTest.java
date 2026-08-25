package io.mango.ai.core.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import io.mango.ai.api.enums.AiApiProtocol;
import io.mango.ai.api.enums.AiMessageContentType;
import io.mango.ai.api.enums.AiModality;
import io.mango.ai.api.vo.AiMessageContentPartVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.ai.core.entity.AiChatConversationEntity;
import io.mango.ai.core.entity.AiChatMessageEntity;
import io.mango.ai.core.mapper.AiChatConversationMapper;
import io.mango.ai.core.mapper.AiChatMessageMapper;
import io.mango.ai.core.service.AiConversationExchange;
import io.mango.ai.core.service.AiConversationScope;
import io.mango.ai.core.service.AiModelResolution;
import io.mango.common.exception.BizException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

class AiChatConversationStoreTest {

    private final AiChatConversationMapper conversationMapper = mock(AiChatConversationMapper.class);
    private final AiChatMessageMapper messageMapper = mock(AiChatMessageMapper.class);
    private final AiChatConversationStore store = new AiChatConversationStore(
            conversationMapper, messageMapper, new ObjectMapper());

    @BeforeAll
    static void initializeTableMetadata() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                AiChatConversationEntity.class);
    }

    @Test
    void rejectsConcurrentAppendWhenMessageCountChanged() {
        AiChatConversationEntity conversation = new AiChatConversationEntity();
        conversation.setId(10L);
        conversation.setTenantId("tenant-1");
        conversation.setUserId(20L);
        conversation.setServiceCode("assistant.general");
        conversation.setSessionId("session-1");
        conversation.setLastModelId(30L);
        conversation.setLastThinkingEnabled(false);
        conversation.setMessageCount(2);
        when(conversationMapper.selectOne(any())).thenReturn(conversation);
        when(conversationMapper.update(any(), any())).thenReturn(0);
        AiModelResolution resolution = new AiModelResolution(
                30L,
                mock(ChatModel.class),
                "provider-1",
                "model-1",
                AiApiProtocol.CHAT_COMPLETIONS,
                false,
                Set.of(AiModality.TEXT),
                Set.of(AiModality.TEXT));

        assertThrows(BizException.class, () -> store.saveExchange(new AiConversationExchange(
                new AiConversationScope("tenant-1", 20L, "assistant.general", "session-1"),
                textParts(AiMessageContentType.TEXT, "下一条问题"),
                textParts(AiMessageContentType.RICH_TEXT, "下一条回答"),
                false,
                resolution)));

        verify(messageMapper, never()).insert(any(AiChatMessageEntity.class));
    }

    @Test
    void appendsTurnWithDifferentModelAndStoresMetadataOnlyOnAssistantMessage() {
        AiChatConversationEntity conversation = new AiChatConversationEntity();
        conversation.setId(10L);
        conversation.setTenantId("tenant-1");
        conversation.setUserId(20L);
        conversation.setServiceCode("assistant.general");
        conversation.setSessionId("session-1");
        conversation.setLastModelId(30L);
        conversation.setLastThinkingEnabled(false);
        conversation.setMessageCount(2);
        when(conversationMapper.selectOne(any())).thenReturn(conversation);
        when(conversationMapper.update(any(), any())).thenReturn(1);
        when(messageMapper.insert(any(AiChatMessageEntity.class))).thenReturn(1);
        AiModelResolution resolution = new AiModelResolution(
                40L,
                mock(ChatModel.class),
                "provider-2",
                "model-2",
                AiApiProtocol.CHAT_COMPLETIONS,
                true,
                Set.of(AiModality.TEXT),
                Set.of(AiModality.TEXT));

        store.saveExchange(new AiConversationExchange(
                new AiConversationScope("tenant-1", 20L, "assistant.general", "session-1"),
                textParts(AiMessageContentType.TEXT, "下一条问题"),
                textParts(AiMessageContentType.RICH_TEXT, "下一条回答"),
                true,
                resolution));

        org.mockito.ArgumentCaptor<AiChatMessageEntity> captor =
                org.mockito.ArgumentCaptor.forClass(AiChatMessageEntity.class);
        verify(messageMapper, org.mockito.Mockito.times(2)).insert(captor.capture());
        AiChatMessageEntity user = captor.getAllValues().getFirst();
        AiChatMessageEntity assistant = captor.getAllValues().getLast();
        assertNull(user.getModelId());
        assertEquals(40L, assistant.getModelId());
        assertEquals("model-2", assistant.getModelName());
        assertEquals("provider-2", assistant.getProviderCode());
        assertTrue(Boolean.TRUE.equals(assistant.getThinkingEnabled()));
    }

    private List<AiMessageContentPartVO> textParts(AiMessageContentType type, String text) {
        AiMessageContentPartVO part = new AiMessageContentPartVO();
        part.setType(type);
        part.setText(text);
        return List.of(part);
    }
}
