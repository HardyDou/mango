package io.mango.biz.notification.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.R;
import io.mango.infra.realtime.api.RealtimePublisher;
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
    private RealtimePublisher messagePublisher;

    private NotificationServiceImpl messageService;

    @BeforeEach
    void setUp() {
        messageService = new NotificationServiceImpl(messageMapper, messagePublisher, new ObjectMapper());
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
        verify(messagePublisher).publishToUser(eq(1L), eq("message"), anyString());
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
        verify(messagePublisher).broadcast(eq("message"), anyString());
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
