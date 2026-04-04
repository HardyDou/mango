package io.mango.message.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.common.result.R;
import io.mango.message.api.MessageApi;
import io.mango.message.api.enums.MessageType;
import io.mango.message.api.po.SysMessagePo;
import io.mango.message.api.vo.SysMessageVO;
import io.mango.message.core.channel.impl.SseChannel;
import io.mango.message.core.channel.impl.WebSocketChannel;
import io.mango.message.core.entity.SysMessage;
import io.mango.message.core.mapper.SysMessageMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MessageServiceImpl
 *
 * @author Mango
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MessageServiceImpl Tests")
class MessageServiceImplTest {

    @Mock
    private SysMessageMapper messageMapper;

    @Mock
    private WebSocketChannel wsChannel;

    @Mock
    private SseChannel sseChannel;

    private MessageServiceImpl messageService;

    @BeforeEach
    void setUp() {
        messageService = new MessageServiceImpl(messageMapper, wsChannel, sseChannel);
    }

    @Test
    @DisplayName("send should insert message and send via channels")
    void send_validPo_insertsAndSends() {
        SysMessagePo po = createMessagePo(MessageType.SYSTEM, "Test Title", "Test Content");
        po.setUserId(1L);
        when(messageMapper.insert(any(SysMessage.class))).thenAnswer(invocation -> {
            SysMessage msg = invocation.getArgument(0);
            msg.setId(1L);
            return 1;
        });

        R<Long> result = messageService.send(po);

        assertTrue(result.isSuccess());
        verify(messageMapper).insert(any(SysMessage.class));
        verify(wsChannel).sendToUser(eq(1L), anyString());
        verify(sseChannel).sendToUser(eq(1L), anyString());
    }

    @Test
    @DisplayName("broadcast should insert message and broadcast via channels")
    void broadcast_validPo_insertsAndBroadcasts() {
        SysMessagePo po = createMessagePo(MessageType.SYSTEM, "Broadcast Title", "Broadcast Content");
        when(messageMapper.insert(any(SysMessage.class))).thenAnswer(invocation -> {
            SysMessage msg = invocation.getArgument(0);
            msg.setId(1L);
            return 1;
        });

        R<Long> result = messageService.broadcast(po);

        assertTrue(result.isSuccess());
        verify(messageMapper).insert(any(SysMessage.class));
        verify(wsChannel).broadcast(anyString());
        verify(sseChannel).broadcast(anyString());
    }

    @Test
    @DisplayName("listByUser should return messages for user")
    void listByUser_existingUser_returnsMessages() {
        SysMessage message = createMessage(1L, MessageType.SYSTEM, "Test Message");
        when(messageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(message));

        R<List<SysMessageVO>> result = messageService.listByUser(1L);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().size());
    }

    @Test
    @DisplayName("listByUser should return empty list when no messages")
    void listByUser_noMessages_returnsEmptyList() {
        when(messageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        R<List<SysMessageVO>> result = messageService.listByUser(999L);

        assertTrue(result.isSuccess());
        assertTrue(result.getData().isEmpty());
    }

    @Test
    @DisplayName("markRead should update message read status")
    void markRead_existingMessage_updatesReadStatus() {
        SysMessage message = createMessage(1L, MessageType.SYSTEM, "Test Message");
        when(messageMapper.selectById(1L)).thenReturn(message);
        when(messageMapper.updateById(any(SysMessage.class))).thenReturn(1);

        R<Boolean> result = messageService.markRead(1L);

        assertTrue(result.isSuccess());
        verify(messageMapper).updateById(any(SysMessage.class));
    }

    @Test
    @DisplayName("markRead should return true even when message not found")
    void markRead_nonExistingMessage_returnsTrue() {
        when(messageMapper.selectById(999L)).thenReturn(null);

        R<Boolean> result = messageService.markRead(999L);

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("MessageServiceImpl implements MessageApi")
    void implementsMessageApi() {
        assertTrue(messageService instanceof MessageApi);
    }

    private SysMessagePo createMessagePo(MessageType type, String title, String content) {
        SysMessagePo po = new SysMessagePo();
        po.setMessageType(type);
        po.setTitle(title);
        po.setContent(content);
        po.setPriority(1);
        return po;
    }

    private SysMessage createMessage(Long id, MessageType type, String title) {
        SysMessage message = new SysMessage();
        message.setId(id);
        message.setMessageType(type);
        message.setTitle(title);
        message.setContent("Test content");
        message.setUserId(1L);
        message.setPriority(1);
        message.setReadStatus(0);
        message.setCreateTime(LocalDateTime.now());
        return message;
    }
}
