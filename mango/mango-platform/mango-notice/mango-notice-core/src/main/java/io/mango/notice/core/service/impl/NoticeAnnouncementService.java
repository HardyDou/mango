package io.mango.notice.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.notice.api.command.NoticeAnnouncementTargetCommand;
import io.mango.notice.api.command.NoticeRecipientCommand;
import io.mango.notice.api.command.NoticeRecipientTargetCommand;
import io.mango.notice.api.command.PublishNoticeAnnouncementCommand;
import io.mango.notice.api.command.SaveNoticeAnnouncementCommand;
import io.mango.notice.api.command.SendNoticeCommand;
import io.mango.notice.api.enums.NoticeAnnouncementConfirmStatus;
import io.mango.notice.api.enums.NoticeAnnouncementStatus;
import io.mango.notice.api.enums.NoticeAnnouncementTargetType;
import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.api.enums.NoticePriority;
import io.mango.notice.api.enums.NoticeReadStatus;
import io.mango.notice.api.enums.NoticeRecipientTargetType;
import io.mango.notice.api.query.MyNoticeAnnouncementPageQuery;
import io.mango.notice.api.query.NoticeAnnouncementPageQuery;
import io.mango.notice.api.vo.NoticeAnnouncementStatsVO;
import io.mango.notice.api.vo.NoticeAnnouncementTargetVO;
import io.mango.notice.api.vo.NoticeAnnouncementVO;
import io.mango.notice.core.convert.NoticeAnnouncementConvert;
import io.mango.notice.core.entity.NoticeAnnouncementEntity;
import io.mango.notice.core.entity.NoticeAnnouncementRecipientEntity;
import io.mango.notice.core.entity.NoticeAnnouncementTargetEntity;
import io.mango.notice.core.mapper.NoticeAnnouncementMapper;
import io.mango.notice.core.mapper.NoticeAnnouncementRecipientMapper;
import io.mango.notice.core.mapper.NoticeAnnouncementTargetMapper;
import io.mango.notice.core.service.INoticeAnnouncementService;
import io.mango.notice.core.service.INoticeService;
import io.mango.notice.core.service.NoticeRecipientResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NoticeAnnouncementService implements INoticeAnnouncementService {

    public static final String ANNOUNCEMENT_PUBLISHED_BIZ_TYPE = "notice.announcement.published";

    private final NoticeAnnouncementMapper announcementMapper;
    private final NoticeAnnouncementTargetMapper targetMapper;
    private final NoticeAnnouncementRecipientMapper recipientMapper;
    private final NoticeRecipientResolver recipientResolver;
    private final INoticeService noticeService;

    @Override
    public PageResult<NoticeAnnouncementVO> pageAnnouncements(NoticeAnnouncementPageQuery query) {
        LambdaQueryWrapper<NoticeAnnouncementEntity> wrapper = new LambdaQueryWrapper<>();
        if (query.getStatus() != null) {
            wrapper.eq(NoticeAnnouncementEntity::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(condition -> condition
                    .like(NoticeAnnouncementEntity::getTitle, query.getKeyword())
                    .or()
                    .like(NoticeAnnouncementEntity::getContent, query.getKeyword()));
        }
        wrapper.orderByDesc(NoticeAnnouncementEntity::getPinned)
                .orderByDesc(NoticeAnnouncementEntity::getPublishTime)
                .orderByDesc(NoticeAnnouncementEntity::getCreatedAt);
        Page<NoticeAnnouncementEntity> result = announcementMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        List<NoticeAnnouncementVO> rows = result.getRecords().stream()
                .map(entity -> NoticeAnnouncementConvert.toVO(entity, targets(entity.getId()), getAnnouncementStats(entity.getId())))
                .toList();
        return PageResult.of(rows, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public NoticeAnnouncementVO getAnnouncement(Long id) {
        NoticeAnnouncementEntity entity = requireAnnouncement(id);
        return NoticeAnnouncementConvert.toVO(entity, targets(id), getAnnouncementStats(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NoticeAnnouncementVO createAnnouncement(SaveNoticeAnnouncementCommand command) {
        NoticeAnnouncementEntity entity = new NoticeAnnouncementEntity();
        applySaveCommand(entity, command);
        entity.setStatus(NoticeAnnouncementStatus.DRAFT);
        announcementMapper.insert(entity);
        saveTargets(entity.getId(), command.getTargets());
        return getAnnouncement(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NoticeAnnouncementVO updateAnnouncement(Long id, SaveNoticeAnnouncementCommand command) {
        NoticeAnnouncementEntity entity = requireAnnouncement(id);
        Require.isTrue(entity.getStatus() == NoticeAnnouncementStatus.DRAFT, "只有草稿公告允许编辑");
        applySaveCommand(entity, command);
        announcementMapper.updateById(entity);
        saveTargets(id, command.getTargets());
        return getAnnouncement(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean publishAnnouncement(Long id, PublishNoticeAnnouncementCommand command) {
        NoticeAnnouncementEntity entity = requireAnnouncement(id);
        Require.isTrue(entity.getStatus() == NoticeAnnouncementStatus.DRAFT, "只有草稿公告允许发布");
        applyPublishCommand(entity, command);
        validateValidTime(entity);
        List<NoticeAnnouncementTargetCommand> targets = publishTargets(id, command);
        validateTargets(targets);
        List<NoticeRecipientCommand> recipients = deduplicateRecipients(resolveRecipients(targets));
        Require.isTrue(!recipients.isEmpty(), "公告发布对象未解析到可接收用户");
        saveTargets(id, targets);
        saveRecipients(id, recipients, Boolean.TRUE.equals(entity.getConfirmRequired()));
        entity.setStatus(NoticeAnnouncementStatus.PUBLISHED);
        entity.setPublishTime(LocalDateTime.now());
        announcementMapper.updateById(entity);
        if (Boolean.TRUE.equals(entity.getSyncMessageEnabled())) {
            sendAnnouncementReminder(entity, recipients);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean offlineAnnouncement(Long id) {
        NoticeAnnouncementEntity entity = requireAnnouncement(id);
        Require.isTrue(entity.getStatus() == NoticeAnnouncementStatus.PUBLISHED, "只有已发布公告允许下线");
        entity.setStatus(NoticeAnnouncementStatus.OFFLINE);
        announcementMapper.updateById(entity);
        return true;
    }

    @Override
    public NoticeAnnouncementStatsVO getAnnouncementStats(Long id) {
        NoticeAnnouncementStatsVO vo = new NoticeAnnouncementStatsVO();
        vo.setAnnouncementId(id);
        vo.setRecipientCount(nullToZero(recipientMapper.countByAnnouncement(id)));
        vo.setReadCount(nullToZero(recipientMapper.countReadByAnnouncement(id)));
        vo.setPendingConfirmCount(nullToZero(recipientMapper.countPendingConfirmByAnnouncement(id)));
        vo.setConfirmedCount(nullToZero(recipientMapper.countConfirmedByAnnouncement(id)));
        return vo;
    }

    @Override
    public PageResult<NoticeAnnouncementVO> pageMyAnnouncements(Long userId, MyNoticeAnnouncementPageQuery query) {
        Require.notNull(userId, "当前用户不能为空");
        Page<NoticeAnnouncementRecipientEntity> page = recipientMapper.selectMyAnnouncementPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                userId,
                query.getUnreadOnly(),
                query.getPendingConfirmOnly(),
                query.getKeyword(),
                LocalDateTime.now());
        List<NoticeAnnouncementVO> rows = page.getRecords().stream()
                .map(this::myAnnouncementVO)
                .filter(vo -> vo != null)
                .toList();
        return PageResult.of(rows, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NoticeAnnouncementVO getMyAnnouncement(Long id, Long userId) {
        NoticeAnnouncementRecipientEntity recipient = requireUserRecipient(id, userId);
        NoticeAnnouncementEntity entity = requireVisiblePublishedAnnouncement(id);
        if (recipient.getReadStatus() != NoticeReadStatus.READ) {
            recipient.setReadStatus(NoticeReadStatus.READ);
            recipient.setReadTime(LocalDateTime.now());
            recipientMapper.updateById(recipient);
        }
        return NoticeAnnouncementConvert.toMyVO(entity, recipient);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean confirmMyAnnouncement(Long id, Long userId) {
        NoticeAnnouncementRecipientEntity recipient = requireUserRecipient(id, userId);
        NoticeAnnouncementEntity entity = requireVisiblePublishedAnnouncement(id);
        Require.isTrue(Boolean.TRUE.equals(entity.getConfirmRequired()), "公告无需确认");
        Require.isTrue(recipient.getConfirmStatus() == NoticeAnnouncementConfirmStatus.PENDING, "公告已确认或无需确认");
        recipient.setReadStatus(NoticeReadStatus.READ);
        if (recipient.getReadTime() == null) {
            recipient.setReadTime(LocalDateTime.now());
        }
        recipient.setConfirmStatus(NoticeAnnouncementConfirmStatus.CONFIRMED);
        recipient.setConfirmTime(LocalDateTime.now());
        recipientMapper.updateById(recipient);
        return true;
    }

    private NoticeAnnouncementVO myAnnouncementVO(NoticeAnnouncementRecipientEntity recipient) {
        NoticeAnnouncementEntity entity = announcementMapper.selectById(recipient.getAnnouncementId());
        if (!isVisiblePublished(entity)) {
            return null;
        }
        return NoticeAnnouncementConvert.toMyVO(entity, recipient);
    }

    private void applySaveCommand(NoticeAnnouncementEntity entity, SaveNoticeAnnouncementCommand command) {
        entity.setTitle(command.getTitle().trim());
        entity.setContent(command.getContent());
        entity.setValidStartTime(command.getValidStartTime());
        entity.setValidEndTime(command.getValidEndTime());
        entity.setPinned(Boolean.TRUE.equals(command.getPinned()));
        entity.setConfirmRequired(Boolean.TRUE.equals(command.getConfirmRequired()));
        entity.setSyncMessageEnabled(command.getSyncMessageEnabled() == null || Boolean.TRUE.equals(command.getSyncMessageEnabled()));
        validateValidTime(entity);
    }

    private void applyPublishCommand(NoticeAnnouncementEntity entity, PublishNoticeAnnouncementCommand command) {
        if (command == null) {
            return;
        }
        if (command.getValidStartTime() != null) {
            entity.setValidStartTime(command.getValidStartTime());
        }
        if (command.getValidEndTime() != null) {
            entity.setValidEndTime(command.getValidEndTime());
        }
        if (command.getPinned() != null) {
            entity.setPinned(command.getPinned());
        }
        if (command.getConfirmRequired() != null) {
            entity.setConfirmRequired(command.getConfirmRequired());
        }
        if (command.getSyncMessageEnabled() != null) {
            entity.setSyncMessageEnabled(command.getSyncMessageEnabled());
        }
    }

    private void validateValidTime(NoticeAnnouncementEntity entity) {
        if (entity.getValidStartTime() != null && entity.getValidEndTime() != null) {
            Require.isTrue(entity.getValidStartTime().isBefore(entity.getValidEndTime()), "公告有效开始时间必须早于结束时间");
        }
    }

    private List<NoticeAnnouncementTargetCommand> publishTargets(Long id, PublishNoticeAnnouncementCommand command) {
        if (command != null && command.getTargets() != null) {
            return command.getTargets();
        }
        return targetMapper.selectList(new LambdaQueryWrapper<NoticeAnnouncementTargetEntity>()
                        .eq(NoticeAnnouncementTargetEntity::getAnnouncementId, id))
                .stream()
                .map(this::toCommand)
                .toList();
    }

    private void validateTargets(List<NoticeAnnouncementTargetCommand> targets) {
        Require.isTrue(targets != null && !targets.isEmpty(), "公告发布对象不能为空");
        long allCount = targets.stream().filter(target -> target.getTargetType() == NoticeAnnouncementTargetType.ALL).count();
        Require.isTrue(allCount <= 1, "全员发布对象只能选择一次");
        Require.isTrue(allCount == 0 || targets.size() == 1, "全员发布不能和组织、角色、用户混用");
        targets.forEach(target -> {
            Require.notNull(target.getTargetType(), "公告发布对象类型不能为空");
            if (target.getTargetType() != NoticeAnnouncementTargetType.ALL) {
                Require.notNull(target.getTargetId(), "公告发布对象ID不能为空");
            }
        });
    }

    private List<NoticeRecipientCommand> resolveRecipients(List<NoticeAnnouncementTargetCommand> targets) {
        if (targets.size() == 1 && targets.get(0).getTargetType() == NoticeAnnouncementTargetType.ALL) {
            return recipientResolver.listAllEnabledUsers();
        }
        List<NoticeRecipientTargetCommand> recipientTargets = targets.stream()
                .map(this::toRecipientTarget)
                .toList();
        return recipientResolver.resolveRecipientTargets(recipientTargets);
    }

    private NoticeRecipientTargetCommand toRecipientTarget(NoticeAnnouncementTargetCommand target) {
        NoticeRecipientTargetCommand command = new NoticeRecipientTargetCommand();
        command.setTargetType(NoticeRecipientTargetType.valueOf(target.getTargetType().name()));
        command.setTargetId(target.getTargetId());
        command.setTargetName(target.getTargetName());
        return command;
    }

    private NoticeAnnouncementTargetCommand toCommand(NoticeAnnouncementTargetEntity entity) {
        NoticeAnnouncementTargetCommand command = new NoticeAnnouncementTargetCommand();
        command.setTargetType(entity.getTargetType());
        command.setTargetId(entity.getTargetId());
        command.setTargetName(entity.getTargetName());
        command.setIncludeChildren(entity.getIncludeChildren());
        return command;
    }

    private void saveTargets(Long announcementId, List<NoticeAnnouncementTargetCommand> targets) {
        targetMapper.delete(new LambdaQueryWrapper<NoticeAnnouncementTargetEntity>()
                .eq(NoticeAnnouncementTargetEntity::getAnnouncementId, announcementId));
        if (targets == null || targets.isEmpty()) {
            return;
        }
        targets.forEach(target -> {
            NoticeAnnouncementTargetEntity entity = new NoticeAnnouncementTargetEntity();
            entity.setAnnouncementId(announcementId);
            entity.setTargetType(target.getTargetType());
            entity.setTargetId(target.getTargetId());
            entity.setTargetName(target.getTargetName());
            entity.setIncludeChildren(Boolean.TRUE.equals(target.getIncludeChildren()));
            targetMapper.insert(entity);
        });
    }

    private void saveRecipients(Long announcementId, List<NoticeRecipientCommand> recipients, boolean confirmRequired) {
        recipientMapper.delete(new LambdaQueryWrapper<NoticeAnnouncementRecipientEntity>()
                .eq(NoticeAnnouncementRecipientEntity::getAnnouncementId, announcementId));
        deduplicateRecipients(recipients).forEach(recipient -> {
            NoticeAnnouncementRecipientEntity entity = new NoticeAnnouncementRecipientEntity();
            entity.setAnnouncementId(announcementId);
            entity.setUserId(recipient.getUserId());
            entity.setReadStatus(NoticeReadStatus.UNREAD);
            entity.setConfirmStatus(confirmRequired
                    ? NoticeAnnouncementConfirmStatus.PENDING
                    : NoticeAnnouncementConfirmStatus.NOT_REQUIRED);
            recipientMapper.insert(entity);
        });
    }

    private List<NoticeRecipientCommand> deduplicateRecipients(List<NoticeRecipientCommand> recipients) {
        Map<Long, NoticeRecipientCommand> dedup = new LinkedHashMap<>();
        recipients.stream()
                .filter(recipient -> recipient.getUserId() != null)
                .forEach(recipient -> dedup.putIfAbsent(recipient.getUserId(), recipient));
        return new ArrayList<>(dedup.values());
    }

    private void sendAnnouncementReminder(NoticeAnnouncementEntity entity, List<NoticeRecipientCommand> recipients) {
        SendNoticeCommand command = new SendNoticeCommand();
        command.setBizType(ANNOUNCEMENT_PUBLISHED_BIZ_TYPE);
        command.setBizId(String.valueOf(entity.getId()));
        command.setChannelTypes(List.of(NoticeChannelType.SITE));
        command.setTitle(entity.getTitle());
        command.setContent("你有一条新公告，请前往公告详情查看。");
        command.setPriority(NoticePriority.NORMAL);
        command.setRecipients(new ArrayList<>(recipients));
        command.setIdempotentKey(ANNOUNCEMENT_PUBLISHED_BIZ_TYPE + ":" + entity.getId());
        noticeService.send(command);
    }

    private List<NoticeAnnouncementTargetVO> targets(Long announcementId) {
        return targetMapper.selectList(new LambdaQueryWrapper<NoticeAnnouncementTargetEntity>()
                        .eq(NoticeAnnouncementTargetEntity::getAnnouncementId, announcementId))
                .stream()
                .map(NoticeAnnouncementConvert::toTargetVO)
                .toList();
    }

    private NoticeAnnouncementEntity requireAnnouncement(Long id) {
        Require.notNull(id, "公告ID不能为空");
        NoticeAnnouncementEntity entity = announcementMapper.selectById(id);
        Require.notNull(entity, "公告不存在");
        return entity;
    }

    private NoticeAnnouncementEntity requireVisiblePublishedAnnouncement(Long id) {
        NoticeAnnouncementEntity entity = requireAnnouncement(id);
        Require.isTrue(isVisiblePublished(entity), "公告不存在或不可访问");
        return entity;
    }

    private NoticeAnnouncementRecipientEntity requireUserRecipient(Long id, Long userId) {
        Require.notNull(userId, "当前用户不能为空");
        NoticeAnnouncementRecipientEntity recipient = recipientMapper.selectOne(new LambdaQueryWrapper<NoticeAnnouncementRecipientEntity>()
                .eq(NoticeAnnouncementRecipientEntity::getAnnouncementId, id)
                .eq(NoticeAnnouncementRecipientEntity::getUserId, userId)
                .last("limit 1"));
        Require.notNull(recipient, "公告不存在或不可访问");
        return recipient;
    }

    private boolean isVisiblePublished(NoticeAnnouncementEntity entity) {
        if (entity == null || entity.getStatus() != NoticeAnnouncementStatus.PUBLISHED) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return (entity.getValidStartTime() == null || !entity.getValidStartTime().isAfter(now))
                && (entity.getValidEndTime() == null || entity.getValidEndTime().isAfter(now));
    }

    private long nullToZero(Long value) {
        return value == null ? 0 : value;
    }
}
