package io.mango.notice.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.common.vo.PageResult;
import io.mango.notice.api.command.NoticeAnnouncementTargetCommand;
import io.mango.notice.api.command.NoticeRecipientCommand;
import io.mango.notice.api.command.PublishNoticeAnnouncementCommand;
import io.mango.notice.api.command.SendNoticeCommand;
import io.mango.notice.api.enums.NoticeAnnouncementConfirmStatus;
import io.mango.notice.api.enums.NoticeAnnouncementStatus;
import io.mango.notice.api.enums.NoticeAnnouncementTargetType;
import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.api.enums.NoticeReadStatus;
import io.mango.notice.api.query.MyNoticeAnnouncementPageQuery;
import io.mango.notice.api.vo.NoticeAnnouncementVO;
import io.mango.notice.core.entity.NoticeAnnouncementEntity;
import io.mango.notice.core.entity.NoticeAnnouncementRecipientEntity;
import io.mango.notice.core.entity.NoticeAnnouncementTargetEntity;
import io.mango.notice.core.mapper.NoticeAnnouncementMapper;
import io.mango.notice.core.mapper.NoticeAnnouncementRecipientMapper;
import io.mango.notice.core.mapper.NoticeAnnouncementTargetMapper;
import io.mango.notice.core.service.INoticeService;
import io.mango.notice.core.service.NoticeRecipientResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static io.mango.notice.core.service.impl.NoticeAnnouncementService.ANNOUNCEMENT_PUBLISHED_BIZ_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class NoticeAnnouncementServiceTest {

    private NoticeAnnouncementMapper announcementMapper;
    private NoticeAnnouncementTargetMapper targetMapper;
    private NoticeAnnouncementRecipientMapper recipientMapper;
    private NoticeRecipientResolver recipientResolver;
    private INoticeService noticeService;
    private NoticeAnnouncementService announcementService;

    @BeforeEach
    void setUp() {
        announcementMapper = mock(NoticeAnnouncementMapper.class);
        targetMapper = mock(NoticeAnnouncementTargetMapper.class);
        recipientMapper = mock(NoticeAnnouncementRecipientMapper.class);
        recipientResolver = mock(NoticeRecipientResolver.class);
        noticeService = mock(INoticeService.class);
        announcementService = new NoticeAnnouncementService(
                announcementMapper,
                targetMapper,
                recipientMapper,
                recipientResolver,
                noticeService);
    }

    @Test
    void publishAnnouncement_fansOutAllUsersOnceAndSendsSiteReminder() {
        NoticeAnnouncementEntity draft = draftAnnouncement();
        draft.setConfirmRequired(true);
        draft.setSyncMessageEnabled(true);
        when(announcementMapper.selectById(100L)).thenReturn(draft);
        when(recipientResolver.listAllEnabledUsers()).thenReturn(List.of(
                recipient(1L, "张三"),
                recipient(1L, "张三重复"),
                recipient(2L, "李四")));

        PublishNoticeAnnouncementCommand command = new PublishNoticeAnnouncementCommand();
        command.setTargets(List.of(target(NoticeAnnouncementTargetType.ALL, null, "全员")));

        assertTrue(announcementService.publishAnnouncement(100L, command));

        ArgumentCaptor<NoticeAnnouncementTargetEntity> targetCaptor =
                ArgumentCaptor.forClass(NoticeAnnouncementTargetEntity.class);
        verify(targetMapper).insert(targetCaptor.capture());
        assertEquals(NoticeAnnouncementTargetType.ALL, targetCaptor.getValue().getTargetType());
        assertEquals(100L, targetCaptor.getValue().getAnnouncementId());

        ArgumentCaptor<NoticeAnnouncementRecipientEntity> recipientCaptor =
                ArgumentCaptor.forClass(NoticeAnnouncementRecipientEntity.class);
        verify(recipientMapper, times(2)).insert(recipientCaptor.capture());
        List<NoticeAnnouncementRecipientEntity> savedRecipients = recipientCaptor.getAllValues();
        assertEquals(List.of(1L, 2L), savedRecipients.stream().map(NoticeAnnouncementRecipientEntity::getUserId).toList());
        savedRecipients.forEach(recipient -> {
            assertEquals(100L, recipient.getAnnouncementId());
            assertEquals(NoticeReadStatus.UNREAD, recipient.getReadStatus());
            assertEquals(NoticeAnnouncementConfirmStatus.PENDING, recipient.getConfirmStatus());
        });

        ArgumentCaptor<NoticeAnnouncementEntity> announcementCaptor =
                ArgumentCaptor.forClass(NoticeAnnouncementEntity.class);
        verify(announcementMapper).updateById(announcementCaptor.capture());
        assertEquals(NoticeAnnouncementStatus.PUBLISHED, announcementCaptor.getValue().getStatus());
        assertNotNull(announcementCaptor.getValue().getPublishTime());

        ArgumentCaptor<SendNoticeCommand> sendCaptor = ArgumentCaptor.forClass(SendNoticeCommand.class);
        verify(noticeService).send(sendCaptor.capture());
        SendNoticeCommand reminder = sendCaptor.getValue();
        assertEquals(ANNOUNCEMENT_PUBLISHED_BIZ_TYPE, reminder.getBizType());
        assertEquals("100", reminder.getBizId());
        assertEquals(List.of(NoticeChannelType.SITE), reminder.getChannelTypes());
        assertEquals(ANNOUNCEMENT_PUBLISHED_BIZ_TYPE + ":100", reminder.getIdempotentKey());
        assertEquals(List.of(1L, 2L), reminder.getRecipients().stream().map(NoticeRecipientCommand::getUserId).toList());
    }

    @Test
    void publishAnnouncement_rejectsMixedAllTarget() {
        when(announcementMapper.selectById(100L)).thenReturn(draftAnnouncement());
        PublishNoticeAnnouncementCommand command = new PublishNoticeAnnouncementCommand();
        command.setTargets(List.of(
                target(NoticeAnnouncementTargetType.ALL, null, "全员"),
                target(NoticeAnnouncementTargetType.ROLE, 10L, "管理员")));

        assertThrows(RuntimeException.class, () -> announcementService.publishAnnouncement(100L, command));

        verifyNoInteractions(recipientResolver);
        verify(recipientMapper, never()).insert(any(NoticeAnnouncementRecipientEntity.class));
        verify(announcementMapper, never()).updateById(any(NoticeAnnouncementEntity.class));
        verifyNoInteractions(noticeService);
    }

    @Test
    void getMyAnnouncement_checksVisibilityBeforeMarkingRead() {
        NoticeAnnouncementRecipientEntity recipient = announcementRecipient(100L, 8L);
        recipient.setReadStatus(NoticeReadStatus.UNREAD);
        when(recipientMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(recipient);
        NoticeAnnouncementEntity offline = publishedAnnouncement();
        offline.setStatus(NoticeAnnouncementStatus.OFFLINE);
        when(announcementMapper.selectById(100L)).thenReturn(offline);

        assertThrows(RuntimeException.class, () -> announcementService.getMyAnnouncement(100L, 8L));

        verify(recipientMapper, never()).updateById(any(NoticeAnnouncementRecipientEntity.class));
    }

    @Test
    void getMyAnnouncement_marksUnreadRecipientAsRead() {
        NoticeAnnouncementRecipientEntity recipient = announcementRecipient(100L, 8L);
        recipient.setReadStatus(NoticeReadStatus.UNREAD);
        when(recipientMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(recipient);
        when(announcementMapper.selectById(100L)).thenReturn(publishedAnnouncement());

        NoticeAnnouncementVO vo = announcementService.getMyAnnouncement(100L, 8L);

        assertEquals(NoticeReadStatus.READ, vo.getReadStatus());
        assertNotNull(vo.getReadTime());
        assertSame(recipient.getReadTime(), vo.getReadTime());
        verify(recipientMapper).updateById(recipient);
    }

    @Test
    void confirmMyAnnouncement_marksReadAndConfirmed() {
        NoticeAnnouncementRecipientEntity recipient = announcementRecipient(100L, 8L);
        recipient.setReadStatus(NoticeReadStatus.UNREAD);
        recipient.setConfirmStatus(NoticeAnnouncementConfirmStatus.PENDING);
        when(recipientMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(recipient);
        NoticeAnnouncementEntity announcement = publishedAnnouncement();
        announcement.setConfirmRequired(true);
        when(announcementMapper.selectById(100L)).thenReturn(announcement);

        assertTrue(announcementService.confirmMyAnnouncement(100L, 8L));

        assertEquals(NoticeReadStatus.READ, recipient.getReadStatus());
        assertNotNull(recipient.getReadTime());
        assertEquals(NoticeAnnouncementConfirmStatus.CONFIRMED, recipient.getConfirmStatus());
        assertNotNull(recipient.getConfirmTime());
        verify(recipientMapper).updateById(recipient);
    }

    @Test
    void pageMyAnnouncements_usesJoinedMapperFiltersAndKeepsTotalFromDatabase() {
        MyNoticeAnnouncementPageQuery query = new MyNoticeAnnouncementPageQuery();
        query.setUnreadOnly(true);
        query.setPendingConfirmOnly(true);
        query.setKeyword("升级");
        Page<NoticeAnnouncementRecipientEntity> mapperPage = new Page<>(1, 10);
        mapperPage.setTotal(1);
        mapperPage.setRecords(List.of(announcementRecipient(100L, 8L)));
        when(recipientMapper.selectMyAnnouncementPage(any(Page.class), eq(8L), eq(true), eq(true), eq("升级"), any(LocalDateTime.class)))
                .thenReturn(mapperPage);
        when(announcementMapper.selectById(100L)).thenReturn(publishedAnnouncement());

        PageResult<NoticeAnnouncementVO> result = announcementService.pageMyAnnouncements(8L, query);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getList().size());
        verify(recipientMapper).selectMyAnnouncementPage(any(Page.class), eq(8L), eq(true), eq(true), eq("升级"),
                any(LocalDateTime.class));
    }

    private NoticeAnnouncementEntity draftAnnouncement() {
        NoticeAnnouncementEntity entity = new NoticeAnnouncementEntity();
        entity.setId(100L);
        entity.setTitle("系统升级公告");
        entity.setContent("今晚升级");
        entity.setStatus(NoticeAnnouncementStatus.DRAFT);
        entity.setPinned(false);
        entity.setConfirmRequired(false);
        entity.setSyncMessageEnabled(true);
        return entity;
    }

    private NoticeAnnouncementEntity publishedAnnouncement() {
        NoticeAnnouncementEntity entity = draftAnnouncement();
        entity.setStatus(NoticeAnnouncementStatus.PUBLISHED);
        entity.setPublishTime(LocalDateTime.now().minusHours(1));
        return entity;
    }

    private NoticeAnnouncementTargetCommand target(NoticeAnnouncementTargetType type, Long targetId, String targetName) {
        NoticeAnnouncementTargetCommand command = new NoticeAnnouncementTargetCommand();
        command.setTargetType(type);
        command.setTargetId(targetId);
        command.setTargetName(targetName);
        return command;
    }

    private NoticeRecipientCommand recipient(Long userId, String name) {
        NoticeRecipientCommand command = new NoticeRecipientCommand();
        command.setUserId(userId);
        command.setRecipientName(name);
        return command;
    }

    private NoticeAnnouncementRecipientEntity announcementRecipient(Long announcementId, Long userId) {
        NoticeAnnouncementRecipientEntity entity = new NoticeAnnouncementRecipientEntity();
        entity.setAnnouncementId(announcementId);
        entity.setUserId(userId);
        entity.setReadStatus(NoticeReadStatus.UNREAD);
        entity.setConfirmStatus(NoticeAnnouncementConfirmStatus.NOT_REQUIRED);
        return entity;
    }
}
