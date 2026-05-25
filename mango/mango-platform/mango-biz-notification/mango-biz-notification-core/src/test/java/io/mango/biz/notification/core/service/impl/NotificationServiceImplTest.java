package io.mango.biz.notification.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.R;
import io.mango.infra.realtime.api.RealtimeApi;
import io.mango.biz.notification.api.NotificationApi;
import io.mango.biz.notification.api.enums.NotificationType;
import io.mango.biz.notification.api.po.SysNotificationPo;
import io.mango.biz.notification.api.vo.SysNotificationVO;
import io.mango.biz.notification.core.entity.SysNotification;
import io.mango.biz.notification.core.mapper.SysNotificationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NotificationServiceImpl
 *
 * @author Mango
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationServiceImpl Tests")
class NotificationServiceImplTest {

    @Mock
    private SysNotificationMapper messageMapper;

    @Mock
    private RealtimeApi realtimeApi;

    private NotificationServiceImpl messageService;

    @BeforeEach
    void setUp() {
        messageService = new NotificationServiceImpl(messageMapper, realtimeApi, new ObjectMapper());
    }

    @Test
    @DisplayName("send should insert message and send via channels")
    void send_validPo_insertsAndSends() {
        SysNotificationPo po = createMessagePo(NotificationType.SYSTEM, "Test Title", "Test Content");
        po.setUserId(1L);
        when(messageMapper.insert(any(SysNotification.class))).thenAnswer(invocation -> {
            SysNotification msg = invocation.getArgument(0);
            msg.setId(1L);
            return 1;
        });

        R<Long> result = messageService.send(po);

        assertTrue(result.isSuccess());
        verify(messageMapper).insert(any(SysNotification.class));
        verify(realtimeApi).publishToUser(eq(1L), eq("message"), anyString());
    }

    @Test
    @DisplayName("broadcast should insert message and broadcast via channels")
    void broadcast_validPo_insertsAndBroadcasts() {
        SysNotificationPo po = createMessagePo(NotificationType.SYSTEM, "Broadcast Title", "Broadcast Content");
        when(messageMapper.insert(any(SysNotification.class))).thenAnswer(invocation -> {
            SysNotification msg = invocation.getArgument(0);
            msg.setId(1L);
            return 1;
        });

        R<Long> result = messageService.broadcast(po);

        assertTrue(result.isSuccess());
        verify(messageMapper).insert(any(SysNotification.class));
        verify(realtimeApi).broadcast(eq("message"), anyString());
    }

    @Test
    @DisplayName("listByUser should return messages for user")
    void listByUser_existingUser_returnsMessages() {
        SysNotification message = createMessage(1L, NotificationType.SYSTEM, "Test Message");
        when(messageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(message));

        R<List<SysNotificationVO>> result = messageService.listByUser(1L);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().size());
    }

    @Test
    @DisplayName("listByUser should return empty list when no messages")
    void listByUser_noMessages_returnsEmptyList() {
        when(messageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        R<List<SysNotificationVO>> result = messageService.listByUser(999L);

        assertTrue(result.isSuccess());
        assertTrue(result.getData().isEmpty());
    }

    @Test
    @DisplayName("markRead should update message read status")
    void markRead_existingMessage_updatesReadStatus() {
        SysNotification message = createMessage(1L, NotificationType.SYSTEM, "Test Message");
        when(messageMapper.selectById(1L)).thenReturn(message);
        when(messageMapper.updateById(any(SysNotification.class))).thenReturn(1);

        R<Boolean> result = messageService.markRead(1L);

        assertTrue(result.isSuccess());
        verify(messageMapper).updateById(any(SysNotification.class));
    }

    @Test
    @DisplayName("markRead should return true even when message not found")
    void markRead_nonExistingMessage_returnsTrue() {
        when(messageMapper.selectById(999L)).thenReturn(null);

        R<Boolean> result = messageService.markRead(999L);

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("get should return null when message does not exist")
    void get_nonExistingMessage_returnsNull() {
        when(messageMapper.selectById(999L)).thenReturn(null);

        SysNotificationVO result = messageService.get(999L);

        assertNull(result);
    }

    @Test
    @DisplayName("markReadForUser should update only current user's message")
    void markReadForUser_existingMessage_updatesWithUserScope() {
        when(messageMapper.update(any(SysNotification.class), any(LambdaQueryWrapper.class))).thenReturn(1);

        R<Boolean> result = messageService.markReadForUser(1L, 100L);

        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(messageMapper).update(any(SysNotification.class), any(LambdaQueryWrapper.class));
        verify(messageMapper, never()).selectById(anyLong());
        verify(messageMapper, never()).updateById(any(SysNotification.class));
    }

    @Test
    @DisplayName("markReadForUser should return false when message belongs to another user")
    void markReadForUser_otherUserMessage_returnsFalse() {
        when(messageMapper.update(any(SysNotification.class), any(LambdaQueryWrapper.class))).thenReturn(0);

        R<Boolean> result = messageService.markReadForUser(1L, 100L);

        assertTrue(result.isSuccess());
        assertFalse(result.getData());
    }

    @Test
    @DisplayName("deleteForUser should delete only current user's message")
    void deleteForUser_existingMessage_deletesWithUserScope() {
        when(messageMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);

        boolean result = messageService.deleteForUser(1L, 100L);

        assertTrue(result);
        verify(messageMapper).delete(any(LambdaQueryWrapper.class));
        verify(messageMapper, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("NotificationServiceImpl implements NotificationApi")
    void implementsNotificationApi() {
        assertTrue(messageService instanceof NotificationApi);
    }

    private SysNotificationPo createMessagePo(NotificationType type, String title, String content) {
        SysNotificationPo po = new SysNotificationPo();
        po.setNotificationType(type);
        po.setTitle(title);
        po.setContent(content);
        po.setPriority(1);
        return po;
    }

    private SysNotification createMessage(Long id, NotificationType type, String title) {
        SysNotification message = new SysNotification();
        message.setId(id);
        message.setNotificationType(type);
        message.setTitle(title);
        message.setContent("Test content");
        message.setUserId(1L);
        message.setPriority(1);
        message.setReadStatus(0);
        message.setCreateTime(LocalDateTime.now());
        return message;
    }
}
