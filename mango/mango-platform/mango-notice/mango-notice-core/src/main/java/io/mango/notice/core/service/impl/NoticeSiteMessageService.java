package io.mango.notice.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.event.api.DomainEvent;
import io.mango.infra.event.api.IDomainEventPublisher;
import io.mango.infra.realtime.api.RealtimeApi;
import io.mango.notice.api.command.CompleteNoticeSiteMessageActionCommand;
import io.mango.notice.api.command.ExecuteNoticeSiteMessageActionCommand;
import io.mango.notice.api.command.MarkNoticeReadCommand;
import io.mango.notice.api.enums.NoticeCode;
import io.mango.notice.api.enums.NoticeDeleteStatus;
import io.mango.notice.api.enums.NoticeReadStatus;
import io.mango.notice.api.enums.NoticeSiteMessageActionInteractionType;
import io.mango.notice.api.enums.NoticeSiteMessageActionRequestStatus;
import io.mango.notice.api.enums.NoticeSiteMessageActionStatus;
import io.mango.notice.api.enums.NoticeSiteMessageCategory;
import io.mango.notice.api.enums.NoticeSiteMessageTargetType;
import io.mango.notice.api.query.NoticeSiteMessagePageQuery;
import io.mango.notice.api.vo.NoticeJsonVO;
import io.mango.notice.api.vo.NoticeSiteMessageActionRequestVO;
import io.mango.notice.api.vo.NoticeSiteMessageActionVO;
import io.mango.notice.api.vo.NoticeSiteMessageSubjectVO;
import io.mango.notice.api.vo.NoticeSiteMessageTargetVO;
import io.mango.notice.api.vo.NoticeSiteMessageVO;
import io.mango.notice.api.vo.NoticeUnreadCountVO;
import io.mango.notice.api.vo.NoticeUnreadCategoryCountVO;
import io.mango.notice.api.vo.NoticeUnreadCategoryStatsVO;
import io.mango.notice.core.convert.NoticeSiteMessageConvert;
import io.mango.notice.core.entity.NoticeBusinessTypeEntity;
import io.mango.notice.core.entity.NoticeSiteMessageActionEntity;
import io.mango.notice.core.entity.NoticeSiteMessageActionRequestEntity;
import io.mango.notice.core.entity.NoticeSiteMessageEntity;
import io.mango.notice.core.mapper.NoticeBusinessTypeMapper;
import io.mango.notice.core.mapper.NoticeSiteMessageActionMapper;
import io.mango.notice.core.mapper.NoticeSiteMessageActionRequestMapper;
import io.mango.notice.core.mapper.NoticeSiteMessageMapper;
import io.mango.notice.core.service.INoticeSiteMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "Spring singleton collaborators are injected and intentionally shared")
public class NoticeSiteMessageService implements INoticeSiteMessageService {

    private static final String APPROVAL_BIZ_GROUP = "WORKFLOW";
    private static final Set<String> SYSTEM_BIZ_GROUPS = Set.of("AUTH", "IDENTITY", "JOB");

    private final NoticeSiteMessageMapper messageMapper;
    private final NoticeSiteMessageActionMapper messageActionMapper;
    private final NoticeSiteMessageActionRequestMapper messageActionRequestMapper;
    private final NoticeBusinessTypeMapper businessTypeMapper;
    private final ObjectMapper objectMapper;
    private final RealtimeApi realtimeApi;
    private final ObjectProvider<IDomainEventPublisher> domainEventPublisherProvider;

    @Override
    public PageResult<NoticeSiteMessageVO> listSiteMessages(NoticeSiteMessagePageQuery query) {
        Long userId = currentUserId();
        LambdaQueryWrapper<NoticeSiteMessageEntity> wrapper = userVisibleWrapper(userId);
        if (Boolean.TRUE.equals(query.getUnreadOnly())) {
            wrapper.eq(NoticeSiteMessageEntity::getReadStatus, NoticeReadStatus.UNREAD);
        }
        if (query.getCategory() != null
                && !applyCategoryFilter(wrapper, query.getCategory(), loadCategoryBizTypes())) {
            return PageResult.of(List.of(), 0, query.getPageNum(), query.getPageSize());
        }
        if (StringUtils.hasText(query.getBizType())) {
            wrapper.eq(NoticeSiteMessageEntity::getBizType, query.getBizType());
        }
        if (query.getPriority() != null) {
            wrapper.eq(NoticeSiteMessageEntity::getPriority, query.getPriority());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(item -> item.like(NoticeSiteMessageEntity::getTitle, query.getKeyword())
                    .or()
                    .like(NoticeSiteMessageEntity::getContent, query.getKeyword()));
        }
        if (StringUtils.hasText(query.getBizId())) {
            wrapper.eq(NoticeSiteMessageEntity::getBizId, query.getBizId());
        }
        if (query.getStartTime() != null) {
            wrapper.ge(NoticeSiteMessageEntity::getCreatedAt, query.getStartTime());
        }
        if (query.getEndTime() != null) {
            wrapper.le(NoticeSiteMessageEntity::getCreatedAt, query.getEndTime());
        }
        if (StringUtils.hasText(query.getBizGroup())) {
            var bizTypes = businessTypeMapper.selectList(new LambdaQueryWrapper<NoticeBusinessTypeEntity>()
                            .eq(NoticeBusinessTypeEntity::getBizGroup, query.getBizGroup()))
                    .stream()
                    .map(NoticeBusinessTypeEntity::getBizType)
                    .collect(Collectors.toSet());
            if (bizTypes.isEmpty()) {
                return PageResult.of(List.of(), 0, query.getPageNum(), query.getPageSize());
            }
            wrapper.in(NoticeSiteMessageEntity::getBizType, bizTypes);
        }
        wrapper.orderByDesc(NoticeSiteMessageEntity::getTopStatus)
                .orderByDesc(NoticeSiteMessageEntity::getCreatedAt);
        Page<NoticeSiteMessageEntity> result = messageMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        return PageResult.of(result.getRecords().stream().map(this::toSiteMessageVO).toList(),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public NoticeSiteMessageVO getSiteMessage(Long id) {
        Long userId = currentUserId();
        NoticeSiteMessageEntity entity = messageMapper.selectOne(
                userVisibleWrapper(userId).eq(NoticeSiteMessageEntity::getId, id));
        Require.notNull(entity, NoticeCode.NOTICE_SITE_MESSAGE_NOT_FOUND, "系统消息不存在");
        return toSiteMessageVO(entity);
    }

    @Override
    public NoticeUnreadCountVO unreadCount() {
        Long userId = currentUserId();
        Long count = messageMapper.selectCount(userVisibleWrapper(userId)
                .eq(NoticeSiteMessageEntity::getReadStatus, NoticeReadStatus.UNREAD));
        return new NoticeUnreadCountVO(count);
    }

    @Override
    public NoticeUnreadCategoryStatsVO unreadCategoryStats() {
        Long userId = currentUserId();
        CategoryBizTypes categoryBizTypes = loadCategoryBizTypes();
        long approval = countUnreadByCategory(userId, NoticeSiteMessageCategory.APPROVAL, categoryBizTypes);
        long system = countUnreadByCategory(userId, NoticeSiteMessageCategory.SYSTEM, categoryBizTypes);
        long business = countUnreadByCategory(userId, NoticeSiteMessageCategory.BUSINESS, categoryBizTypes);
        return new NoticeUnreadCategoryStatsVO(
                approval + system + business,
                List.of(
                        new NoticeUnreadCategoryCountVO(NoticeSiteMessageCategory.APPROVAL, approval),
                        new NoticeUnreadCategoryCountVO(NoticeSiteMessageCategory.SYSTEM, system),
                        new NoticeUnreadCategoryCountVO(NoticeSiteMessageCategory.BUSINESS, business)));
    }

    @Override
    public boolean markSiteMessageRead(Long id) {
        Require.notNull(id, NoticeCode.NOTICE_BUSINESS_ERROR, "系统消息 ID 不能为空");
        Long userId = currentUserId();
        NoticeSiteMessageEntity entity = new NoticeSiteMessageEntity();
        entity.setReadStatus(NoticeReadStatus.READ);
        entity.setReadTime(LocalDateTime.now());
        int updated = messageMapper.update(entity, userVisibleWrapper(userId)
                .eq(NoticeSiteMessageEntity::getId, id)
                .eq(NoticeSiteMessageEntity::getReadStatus, NoticeReadStatus.UNREAD));
        if (updated > 0) {
            publishUnreadCount(userId);
        }
        return updated > 0;
    }

    @Override
    public boolean markSiteMessagesRead(MarkNoticeReadCommand command) {
        Require.notNull(command, NoticeCode.NOTICE_BUSINESS_ERROR, "消息已读命令不能为空");
        Long userId = currentUserId();
        NoticeSiteMessageEntity entity = new NoticeSiteMessageEntity();
        entity.setReadStatus(NoticeReadStatus.READ);
        entity.setReadTime(LocalDateTime.now());
        int updated = messageMapper.update(entity, userVisibleWrapper(userId)
                .in(NoticeSiteMessageEntity::getId, command.getIds())
                .eq(NoticeSiteMessageEntity::getReadStatus, NoticeReadStatus.UNREAD));
        if (updated > 0) {
            publishUnreadCount(userId);
        }
        return updated >= 0;
    }

    @Override
    public boolean markAllSiteMessagesRead() {
        Long userId = currentUserId();
        NoticeSiteMessageEntity entity = new NoticeSiteMessageEntity();
        entity.setReadStatus(NoticeReadStatus.READ);
        entity.setReadTime(LocalDateTime.now());
        int updated = messageMapper.update(entity, userVisibleWrapper(userId)
                .eq(NoticeSiteMessageEntity::getReadStatus, NoticeReadStatus.UNREAD));
        if (updated > 0) {
            publishUnreadCount(userId);
        }
        return updated >= 0;
    }

    @Override
    public boolean deleteSiteMessage(Long id) {
        Require.notNull(id, NoticeCode.NOTICE_BUSINESS_ERROR, "系统消息 ID 不能为空");
        Long userId = currentUserId();
        NoticeSiteMessageEntity existing = messageMapper.selectOne(
                userVisibleWrapper(userId).eq(NoticeSiteMessageEntity::getId, id));
        NoticeSiteMessageEntity entity = new NoticeSiteMessageEntity();
        entity.setDeleteStatus(NoticeDeleteStatus.DELETED);
        int updated = messageMapper.update(entity,
                userVisibleWrapper(userId).eq(NoticeSiteMessageEntity::getId, id));
        if (updated > 0 && existing != null && existing.getReadStatus() == NoticeReadStatus.UNREAD) {
            publishUnreadCount(userId);
        }
        return updated > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NoticeSiteMessageActionRequestVO executeSiteMessageAction(
            ExecuteNoticeSiteMessageActionCommand command) {
        Long userId = currentUserId();
        Require.notNull(command, NoticeCode.NOTICE_BUSINESS_ERROR, "系统消息动作命令不能为空");
        Long id = command.getMessageId();
        String actionCode = command.getActionCode();
        Require.notNull(id, NoticeCode.NOTICE_BUSINESS_ERROR, "系统消息 ID 不能为空");
        Require.notBlank(actionCode, NoticeCode.NOTICE_BUSINESS_ERROR, "系统消息动作编码不能为空");
        NoticeSiteMessageEntity message = messageMapper.selectOne(
                userVisibleWrapper(userId).eq(NoticeSiteMessageEntity::getId, id));
        Require.notNull(message, NoticeCode.NOTICE_BUSINESS_ERROR, "系统消息不存在");
        NoticeSiteMessageActionEntity action = findMessageAction(message.getId(), actionCode);
        Require.notNull(action, NoticeCode.NOTICE_BUSINESS_ERROR, "系统消息动作不存在");
        Require.isTrue(action.getInteractionType() == NoticeSiteMessageActionInteractionType.EVENT,
                NoticeCode.NOTICE_BUSINESS_ERROR, "该系统消息动作只能进入业务页面处理");
        Require.notBlank(action.getEventType(), NoticeCode.NOTICE_BUSINESS_ERROR,
                "系统消息动作事件类型不能为空");
        String requestId = actionRequestId(message.getId(), action.getActionCode(), userId);
        NoticeSiteMessageActionRequestEntity existing = findActionRequest(requestId);
        if (existing != null) {
            return toActionRequestVO(existing);
        }
        Require.isTrue(action.getStatus() == NoticeSiteMessageActionStatus.AVAILABLE,
                NoticeCode.NOTICE_BUSINESS_ERROR, "系统消息动作当前不可执行");
        if (isExpired(message.getExpireTime()) || isExpired(action.getExpireTime())) {
            markActionExpired(action);
            return Require.fail(NoticeCode.NOTICE_BUSINESS_ERROR, "系统消息动作已过期");
        }

        NoticeSiteMessageActionEntity processing = new NoticeSiteMessageActionEntity();
        processing.setStatus(NoticeSiteMessageActionStatus.PROCESSING);
        int updated = messageActionMapper.update(processing,
                new LambdaQueryWrapper<NoticeSiteMessageActionEntity>()
                        .eq(NoticeSiteMessageActionEntity::getId, action.getId())
                        .eq(NoticeSiteMessageActionEntity::getStatus,
                                NoticeSiteMessageActionStatus.AVAILABLE));
        if (updated == 0) {
            NoticeSiteMessageActionRequestEntity request = findActionRequest(requestId);
            if (request != null) {
                return toActionRequestVO(request);
            }
            return Require.fail(NoticeCode.NOTICE_BUSINESS_ERROR, "系统消息动作当前不可执行");
        }

        Map<String, Object> input = command.getInput() == null
                ? Collections.emptyMap() : command.getInput().toMap();
        NoticeSiteMessageActionRequestEntity request = new NoticeSiteMessageActionRequestEntity();
        request.setMessageId(message.getId());
        request.setActionId(action.getId());
        request.setActionCode(action.getActionCode());
        request.setActorUserId(userId);
        request.setRequestId(requestId);
        request.setInputJson(toJson(input));
        request.setStatus(NoticeSiteMessageActionRequestStatus.REQUESTED);
        request.setEventId(UUID.randomUUID().toString());
        request.setTenantId(currentTenantId());
        messageActionRequestMapper.insert(request);

        publishActionRequestedEvent(message, action, request, input);
        publishActionStatus(userId, message.getId(), action.getActionCode(),
                NoticeSiteMessageActionStatus.PROCESSING);
        return toActionRequestVO(request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NoticeSiteMessageActionRequestVO completeSiteMessageAction(
            CompleteNoticeSiteMessageActionCommand command) {
        Require.notNull(command, NoticeCode.NOTICE_BUSINESS_ERROR,
                "系统消息动作完成命令不能为空");
        Require.notBlank(command.getRequestId(), NoticeCode.NOTICE_BUSINESS_ERROR,
                "系统消息动作请求 ID 不能为空");
        Require.notNull(command.getStatus(), NoticeCode.NOTICE_BUSINESS_ERROR,
                "系统消息动作结果状态不能为空");
        Require.isTrue(command.getStatus() != NoticeSiteMessageActionRequestStatus.REQUESTED,
                NoticeCode.NOTICE_BUSINESS_ERROR, "系统消息动作结果状态非法");
        NoticeSiteMessageActionRequestEntity request = findActionRequest(command.getRequestId());
        Require.notNull(request, NoticeCode.NOTICE_BUSINESS_ERROR, "系统消息动作请求不存在");
        NoticeSiteMessageActionEntity action = messageActionMapper.selectById(request.getActionId());
        Require.notNull(action, NoticeCode.NOTICE_BUSINESS_ERROR, "系统消息动作不存在");

        request.setStatus(command.getStatus());
        request.setFailCode(command.getFailCode());
        request.setFailReason(command.getFailReason());
        request.setResultJson(toJson(command.getResult() == null
                ? Collections.emptyMap() : command.getResult().toMap()));
        request.setFinishedAt(LocalDateTime.now());
        messageActionRequestMapper.updateById(request);

        NoticeSiteMessageActionEntity update = new NoticeSiteMessageActionEntity();
        update.setId(action.getId());
        update.setStatus(command.getStatus() == NoticeSiteMessageActionRequestStatus.SUCCEEDED
                ? NoticeSiteMessageActionStatus.SUCCEEDED
                : NoticeSiteMessageActionStatus.FAILED);
        update.setFailureReason(command.getFailReason());
        messageActionMapper.updateById(update);
        publishActionStatus(request.getActorUserId(), request.getMessageId(),
                request.getActionCode(), update.getStatus());
        return toActionRequestVO(request);
    }

    private NoticeSiteMessageActionEntity findMessageAction(Long messageId, String actionCode) {
        return messageActionMapper.selectOne(new LambdaQueryWrapper<NoticeSiteMessageActionEntity>()
                .eq(NoticeSiteMessageActionEntity::getMessageId, messageId)
                .eq(NoticeSiteMessageActionEntity::getActionCode, actionCode)
                .last("LIMIT 1"));
    }

    private NoticeSiteMessageActionRequestEntity findActionRequest(String requestId) {
        return messageActionRequestMapper.selectOne(
                new LambdaQueryWrapper<NoticeSiteMessageActionRequestEntity>()
                        .eq(NoticeSiteMessageActionRequestEntity::getRequestId, requestId)
                        .last("LIMIT 1"));
    }

    private void markActionExpired(NoticeSiteMessageActionEntity action) {
        NoticeSiteMessageActionEntity update = new NoticeSiteMessageActionEntity();
        update.setStatus(NoticeSiteMessageActionStatus.EXPIRED);
        messageActionMapper.update(update, new LambdaQueryWrapper<NoticeSiteMessageActionEntity>()
                .eq(NoticeSiteMessageActionEntity::getId, action.getId())
                .eq(NoticeSiteMessageActionEntity::getStatus,
                        NoticeSiteMessageActionStatus.AVAILABLE));
    }

    private boolean isExpired(LocalDateTime expireTime) {
        return expireTime != null && expireTime.isBefore(LocalDateTime.now());
    }

    private String actionRequestId(Long messageId, String actionCode, Long userId) {
        return "NSMA:" + messageId + ":" + actionCode + ":" + userId;
    }

    private Long currentUserId() {
        Long userId = MangoContextHolder.userId();
        Require.notNull(userId, NoticeCode.NOTICE_BUSINESS_ERROR, "缺少当前用户上下文");
        return userId;
    }

    private String currentTenantId() {
        return StringUtils.hasText(MangoContextHolder.tenantId())
                ? MangoContextHolder.tenantId() : "default";
    }

    private void publishActionRequestedEvent(
            NoticeSiteMessageEntity message,
            NoticeSiteMessageActionEntity action,
            NoticeSiteMessageActionRequestEntity request,
            Map<String, Object> input) {
        IDomainEventPublisher publisher = Require.nonNull(
                domainEventPublisherProvider.getIfAvailable(),
                NoticeCode.NOTICE_BUSINESS_ERROR,
                "领域事件发布器未装配");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("messageId", message.getId());
        payload.put("actionCode", action.getActionCode());
        payload.put("actorUserId", request.getActorUserId());
        payload.put("requestId", request.getRequestId());
        payload.put("subjectType", message.getSubjectType());
        payload.put("subjectId", message.getSubjectId());
        payload.put("subjectName", message.getSubjectName());
        payload.put("input", input == null ? Collections.emptyMap() : input);
        payload.put("data", fromJson(message.getDataJson()));
        DomainEvent event = DomainEvent.builder()
                .eventId(request.getEventId())
                .eventType(action.getEventType())
                .businessType(message.getBizType())
                .businessKey(message.getBizId())
                .aggregateId(message.getSubjectId())
                .payload(payload)
                .header("tenantId", currentTenantId())
                .header("actorUserId", String.valueOf(request.getActorUserId()))
                .header("requestId", request.getRequestId())
                .header("messageId", String.valueOf(message.getId()))
                .header("actionCode", action.getActionCode())
                .build();
        publisher.publish(event);
    }

    private void publishActionStatus(
            Long userId, Long messageId, String actionCode, NoticeSiteMessageActionStatus status) {
        try {
            Map<String, Object> payload = Map.of(
                    "messageId", messageId,
                    "actionCode", actionCode,
                    "actionStatus", status.name());
            realtimeApi.publishToUser(userId, "notice-action", objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException | RuntimeException ex) {
            log.warn("Failed to publish notice action realtime message: userId={}, messageId={}, actionCode={}",
                    userId, messageId, actionCode, ex);
        }
    }

    private NoticeSiteMessageActionRequestVO toActionRequestVO(
            NoticeSiteMessageActionRequestEntity entity) {
        NoticeSiteMessageActionRequestVO vo = new NoticeSiteMessageActionRequestVO();
        vo.setRequestId(entity.getRequestId());
        vo.setMessageId(entity.getMessageId());
        vo.setActionCode(entity.getActionCode());
        vo.setEventId(entity.getEventId());
        vo.setStatus(entity.getStatus());
        vo.setFailCode(entity.getFailCode());
        vo.setFailReason(entity.getFailReason());
        vo.setResult(NoticeJsonVO.of(fromJson(entity.getResultJson())));
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setFinishedAt(entity.getFinishedAt());
        return vo;
    }

    private void publishUnreadCount(Long userId) {
        Long unreadCount = messageMapper.selectCount(userVisibleWrapper(userId)
                .eq(NoticeSiteMessageEntity::getReadStatus, NoticeReadStatus.UNREAD));
        try {
            Map<String, Object> payload = Map.of("unreadCount", Objects.requireNonNullElse(unreadCount, 0L));
            realtimeApi.publishToUser(userId, "notice", objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException | RuntimeException ex) {
            log.warn("Failed to publish notice unread count realtime message: userId={}", userId, ex);
        }
    }

    private NoticeSiteMessageVO toSiteMessageVO(NoticeSiteMessageEntity entity) {
        NoticeSiteMessageVO vo = NoticeSiteMessageConvert.toVO(entity);
        vo.setMessageScene(entity.getMessageScene());
        vo.setSubject(toSubjectVO(entity));
        vo.setTarget(toTargetVO(entity.getTargetType(), entity.getTargetKey(),
                entity.getTargetParamsJson(), entity.getTargetOpenMode()));
        vo.setData(NoticeJsonVO.of(fromJson(entity.getDataJson())));
        vo.setActions(messageActionMapper.selectList(new LambdaQueryWrapper<NoticeSiteMessageActionEntity>()
                        .eq(NoticeSiteMessageActionEntity::getMessageId, entity.getId())
                        .orderByAsc(NoticeSiteMessageActionEntity::getSortOrder)
                        .orderByAsc(NoticeSiteMessageActionEntity::getId))
                .stream()
                .map(this::toActionVO)
                .toList());
        vo.setExpireTime(entity.getExpireTime());
        if (entity.getBizType() == null) {
            return vo;
        }
        NoticeBusinessTypeEntity businessType = businessTypeMapper.selectOne(
                new LambdaQueryWrapper<NoticeBusinessTypeEntity>()
                        .eq(NoticeBusinessTypeEntity::getBizType, entity.getBizType())
                        .last("LIMIT 1"));
        if (businessType != null) {
            vo.setBizGroup(businessType.getBizGroup());
            vo.setBizName(businessType.getBizName());
        }
        return vo;
    }

    private NoticeSiteMessageSubjectVO toSubjectVO(NoticeSiteMessageEntity entity) {
        if (!StringUtils.hasText(entity.getSubjectType())
                && !StringUtils.hasText(entity.getSubjectId())
                && !StringUtils.hasText(entity.getSubjectName())) {
            return null;
        }
        NoticeSiteMessageSubjectVO subject = new NoticeSiteMessageSubjectVO();
        subject.setSubjectType(entity.getSubjectType());
        subject.setSubjectId(entity.getSubjectId());
        subject.setSubjectName(entity.getSubjectName());
        return subject;
    }

    private NoticeSiteMessageTargetVO toTargetVO(
            NoticeSiteMessageTargetType targetType, String targetKey, String paramsJson, String openMode) {
        if (targetType == null || targetType == NoticeSiteMessageTargetType.NONE) {
            return null;
        }
        NoticeSiteMessageTargetVO target = new NoticeSiteMessageTargetVO();
        target.setTargetType(targetType);
        target.setTargetKey(targetKey);
        target.setParams(NoticeJsonVO.of(fromJson(paramsJson)));
        target.setOpenMode(openMode);
        return target;
    }

    private NoticeSiteMessageActionVO toActionVO(NoticeSiteMessageActionEntity entity) {
        NoticeSiteMessageActionVO vo = new NoticeSiteMessageActionVO();
        vo.setId(entity.getId());
        vo.setActionCode(entity.getActionCode());
        vo.setActionLabel(entity.getActionLabel());
        vo.setInteractionType(entity.getInteractionType());
        vo.setEventType(entity.getEventType());
        vo.setTarget(toTargetVO(entity.getTargetType(), entity.getTargetKey(),
                entity.getTargetParamsJson(), entity.getTargetOpenMode()));
        vo.setConfirmRequired(entity.getConfirmRequired());
        vo.setInputSchema(entity.getInputSchema());
        vo.setStatus(displayActionStatus(entity));
        vo.setFailureReason(entity.getFailureReason());
        vo.setSortOrder(entity.getSortOrder());
        vo.setExpireTime(entity.getExpireTime());
        return vo;
    }

    private NoticeSiteMessageActionStatus displayActionStatus(NoticeSiteMessageActionEntity entity) {
        if (entity.getStatus() == NoticeSiteMessageActionStatus.AVAILABLE
                && isExpired(entity.getExpireTime())) {
            return NoticeSiteMessageActionStatus.EXPIRED;
        }
        return entity.getStatus();
    }

    private LambdaQueryWrapper<NoticeSiteMessageEntity> userVisibleWrapper(Long userId) {
        return new LambdaQueryWrapper<NoticeSiteMessageEntity>()
                .eq(NoticeSiteMessageEntity::getUserId, userId)
                .eq(NoticeSiteMessageEntity::getDeleteStatus, NoticeDeleteStatus.NORMAL);
    }

    private long countUnreadByCategory(
            Long userId, NoticeSiteMessageCategory category, CategoryBizTypes categoryBizTypes) {
        LambdaQueryWrapper<NoticeSiteMessageEntity> wrapper = userVisibleWrapper(userId)
                .eq(NoticeSiteMessageEntity::getReadStatus, NoticeReadStatus.UNREAD);
        if (!applyCategoryFilter(wrapper, category, categoryBizTypes)) {
            return 0L;
        }
        Long count = messageMapper.selectCount(wrapper);
        return count == null ? 0L : count;
    }

    private boolean applyCategoryFilter(
            LambdaQueryWrapper<NoticeSiteMessageEntity> wrapper,
            NoticeSiteMessageCategory category,
            CategoryBizTypes categoryBizTypes) {
        if (category == null) {
            return true;
        }
        Set<String> bizTypes = switch (category) {
            case APPROVAL -> categoryBizTypes.approval();
            case SYSTEM -> categoryBizTypes.system();
            case BUSINESS -> categoryBizTypes.excluded();
        };
        if (category == NoticeSiteMessageCategory.BUSINESS) {
            if (!bizTypes.isEmpty()) {
                wrapper.and(item -> item.isNull(NoticeSiteMessageEntity::getBizType)
                        .or()
                        .notIn(NoticeSiteMessageEntity::getBizType, bizTypes));
            }
            return true;
        }
        if (bizTypes.isEmpty()) {
            return false;
        }
        wrapper.in(NoticeSiteMessageEntity::getBizType, bizTypes);
        return true;
    }

    private CategoryBizTypes loadCategoryBizTypes() {
        List<NoticeBusinessTypeEntity> businessTypes = businessTypeMapper.selectList(
                new LambdaQueryWrapper<NoticeBusinessTypeEntity>()
                        .in(NoticeBusinessTypeEntity::getBizGroup,
                                Set.of(APPROVAL_BIZ_GROUP, "AUTH", "IDENTITY", "JOB")));
        Set<String> approval = businessTypes.stream()
                .filter(item -> APPROVAL_BIZ_GROUP.equals(item.getBizGroup()))
                .map(NoticeBusinessTypeEntity::getBizType)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        Set<String> system = businessTypes.stream()
                .filter(item -> SYSTEM_BIZ_GROUPS.contains(item.getBizGroup()))
                .map(NoticeBusinessTypeEntity::getBizType)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        return new CategoryBizTypes(approval, system);
    }

    private record CategoryBizTypes(Set<String> approval, Set<String> system) {
        private Set<String> excluded() {
            return java.util.stream.Stream.concat(approval.stream(), system.stream())
                    .collect(Collectors.toSet());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fromJson(String value) {
        if (!StringUtils.hasText(value)) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(value, Map.class);
        } catch (JsonProcessingException ex) {
            return Collections.emptyMap();
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }
}
