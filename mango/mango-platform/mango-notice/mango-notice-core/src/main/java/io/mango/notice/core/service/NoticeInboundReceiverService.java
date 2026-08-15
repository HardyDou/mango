package io.mango.notice.core.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.Require;
import io.mango.file.api.IFileContentProvider;
import io.mango.file.api.command.SaveFileCommand;
import io.mango.file.api.vo.FileRecordVO;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.infra.event.api.DomainEvent;
import io.mango.infra.event.api.IDomainEventPublisher;
import io.mango.notice.api.InboundNoticeAttachmentRequest;
import io.mango.notice.api.InboundNoticeMessageRequest;
import io.mango.notice.api.InboundReceiveResultResponse;
import io.mango.notice.api.NoticeInboundReceiver;
import io.mango.notice.api.enums.NoticeCode;
import io.mango.notice.api.enums.NoticeInboundAttachmentStatus;
import io.mango.notice.api.enums.NoticeInboundMessageStatus;
import io.mango.notice.core.entity.NoticeInboundAttachmentEntity;
import io.mango.notice.core.entity.NoticeInboundMessageEntity;
import io.mango.notice.core.mapper.NoticeInboundAttachmentMapper;
import io.mango.notice.core.mapper.NoticeInboundMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/** Persists inbound messages, stores attachments in Mango File, and emits the stable event. */
@Service
@RequiredArgsConstructor(onConstructor_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "Spring-managed collaborators are injected and intentionally shared"))
@Slf4j
public class NoticeInboundReceiverService implements NoticeInboundReceiver, INoticeInboundReceiverService {
    public static final String EVENT_TYPE = "notice.message.received";
    private static final int MAX_ATTACHMENT_ATTEMPTS = 5;
    private static final int MAX_FAILURE_REASON_LENGTH = 1000;
    private static final int BROADCAST_RETRY_DELAY_SECONDS = 30;

    private final NoticeInboundMessageMapper messageMapper;
    private final NoticeInboundAttachmentMapper attachmentMapper;
    private final ObjectMapper objectMapper;
    private final IFileContentProvider fileContentProvider;
    private final IDomainEventPublisher domainEventPublisher;
    private final PlatformTransactionManager transactionManager;

    @Override
    public InboundReceiveResultResponse receive(InboundNoticeMessageRequest candidate) {
        InboundNoticeMessageRequest message = Require.nonNull(candidate, NoticeCode.NOTICE_BUSINESS_ERROR, "入站消息不能为空");
        Require.notBlank(message.sourceKey(), NoticeCode.NOTICE_BUSINESS_ERROR, "入站消息 sourceKey 不能为空");
        Require.notBlank(message.tenantId(), NoticeCode.NOTICE_BUSINESS_ERROR, "入站消息 tenantId 不能为空");
        Require.notNull(message.channelConfigId(), NoticeCode.NOTICE_BUSINESS_ERROR, "入站消息渠道配置不能为空");
        validateAttachments(message.attachments());
        try {
            return withTenant(message.tenantId(), () -> receiveInTenant(message));
        } finally {
            closeAttachments(message.attachments());
        }
    }

    private InboundReceiveResultResponse receiveInTenant(InboundNoticeMessageRequest message) {
        NoticeInboundMessageEntity entity = find(message.channelConfigId(), message.sourceKey());
        boolean duplicate = entity != null;
        if (entity == null) {
            entity = createOrReadWinner(message);
        }
        if (entity.getStatus() == NoticeInboundMessageStatus.BROADCASTED) {
            return result(entity, duplicate);
        }
        persistAttachmentMetadata(entity.getId(), message.attachments());
        saveAttachments(entity, message.attachments());
        publishWhenReady(entity.getId());
        NoticeInboundMessageEntity accepted = Require.nonNull(messageMapper.selectById(entity.getId()), NoticeCode.NOTICE_BUSINESS_ERROR,
                "入站消息保存后不存在");
        return result(accepted, duplicate);
    }

    private NoticeInboundMessageEntity createOrReadWinner(InboundNoticeMessageRequest message) {
        NoticeInboundMessageEntity entity = newMessage(message);
        try {
            transaction().executeWithoutResult(status -> messageMapper.insert(entity));
            return entity;
        } catch (DuplicateKeyException duplicate) {
            NoticeInboundMessageEntity winner = find(message.channelConfigId(), message.sourceKey());
            return Require.nonNull(winner, NoticeCode.NOTICE_BUSINESS_ERROR, "并发入站消息未找到幂等记录");
        }
    }

    private NoticeInboundMessageEntity newMessage(InboundNoticeMessageRequest message) {
        NoticeInboundMessageEntity entity = new NoticeInboundMessageEntity();
        entity.setId(IdWorker.getId());
        // The callback/poller enters the resolved tenant context before this method is called.
        // Set it explicitly as well so the persisted identity never depends on an insert-fill
        // interceptor and the event can be built from the in-memory winner safely.
        entity.setTenantId(message.tenantId());
        entity.setChannelConfigId(message.channelConfigId());
        entity.setChannelType(message.channelType());
        entity.setProviderCode(message.providerCode());
        entity.setSourceKey(message.sourceKey());
        entity.setMessageId(message.messageId());
        entity.setSubject(message.subject());
        entity.setFromAddress(message.fromAddress());
        entity.setToAddressesJson(writeJson(message.toAddresses()));
        entity.setBodyText(message.bodyText());
        entity.setBodyHtml(message.bodyHtml());
        entity.setRawHeadersJson(writeJson(message.headers()));
        entity.setStatus(NoticeInboundMessageStatus.RECEIVED);
        entity.setEventId(stableEventId(message));
        entity.setAttemptCount(0);
        entity.setReceivedAt(LocalDateTime.ofInstant(message.receivedAt(), java.time.ZoneId.systemDefault()));
        return entity;
    }

    private void persistAttachmentMetadata(Long messageId, List<InboundNoticeAttachmentRequest> attachments) {
        transaction().executeWithoutResult(status -> {
            for (InboundNoticeAttachmentRequest attachment : attachments) {
                NoticeInboundAttachmentEntity existing = findAttachment(messageId, attachment.index());
                if (existing != null) {
                    continue;
                }
                NoticeInboundAttachmentEntity entity = new NoticeInboundAttachmentEntity();
                entity.setId(IdWorker.getId());
                entity.setTenantId(messageTenantId(messageId));
                entity.setMessageId(messageId);
                entity.setAttachmentIndex(attachment.index());
                entity.setFileName(attachment.fileName());
                entity.setContentType(attachment.contentType());
                entity.setFileSize(attachment.fileSize());
                entity.setStatus(NoticeInboundAttachmentStatus.PENDING);
                entity.setAttemptCount(0);
                try {
                    attachmentMapper.insert(entity);
                } catch (DuplicateKeyException ignored) {
                    log.debug("Inbound attachment metadata already exists: messageId={}, index={}",
                            messageId, attachment.index());
                }
            }
        });
    }

    private void saveAttachments(NoticeInboundMessageEntity message, List<InboundNoticeAttachmentRequest> attachments) {
        if (attachments.isEmpty()) {
            // A duplicate delivery may not carry the original streams. Never turn a
            // partially processed message into a broadcastable one just because this
            // retry has no attachments; the persisted attachment rows remain the source
            // of truth and the source must retry with the streams when they are missing.
            if (!allAttachmentsSaved(message.getId())) {
                Require.rethrow(new InboundReceiveRetryableException("入站消息附件尚未全部提供"));
            }
            markReady(message.getId());
            return;
        }
        markMessageStatus(message.getId(), NoticeInboundMessageStatus.ATTACHMENT_PROCESSING, null, null);
        for (InboundNoticeAttachmentRequest attachment : attachments) {
            NoticeInboundAttachmentEntity entity = findAttachment(message.getId(), attachment.index());
            if (entity != null && entity.getStatus() == NoticeInboundAttachmentStatus.SAVED) {
                continue;
            }
            NoticeInboundAttachmentEntity checkedEntity = Require.nonNull(entity, NoticeCode.NOTICE_BUSINESS_ERROR, "入站附件元数据不存在");
            if (!claimAttachment(checkedEntity.getId())) {
                Require.rethrow(new InboundReceiveRetryableException("入站附件正在被其它请求处理"));
            }
            try {
                FileRecordVO file = fileContentProvider.save(fileCommand(message.getId(), attachment));
                Require.notNull(file, NoticeCode.NOTICE_BUSINESS_ERROR, "入站附件保存结果不能为空");
                Require.notNull(file.getId(), NoticeCode.NOTICE_BUSINESS_ERROR, "入站附件 fileId 不能为空");
                markAttachmentSaved(checkedEntity.getId(), file.getId());
            } catch (RuntimeException failure) {
                NoticeInboundAttachmentStatus attachmentStatus = markAttachmentFailed(checkedEntity.getId(), failure);
                markMessageStatus(message.getId(), attachmentStatus == NoticeInboundAttachmentStatus.DEAD_LETTER
                                ? NoticeInboundMessageStatus.DEAD_LETTER
                                : NoticeInboundMessageStatus.RETRYABLE_FAILED,
                        "NOTICE_INBOUND_ATTACHMENT_FAILED", failureMessage(failure));
                Require.rethrow(new InboundReceiveRetryableException("入站附件保存失败", failure));
            }
        }
        if (allAttachmentsSaved(message.getId())) {
            markReady(message.getId());
        }
    }

    private void publishWhenReady(Long messageId) {
        NoticeInboundMessageEntity entity = Require.nonNull(messageMapper.selectById(messageId), NoticeCode.NOTICE_BUSINESS_ERROR, "入站消息不存在");
        if (entity.getStatus() != NoticeInboundMessageStatus.READY_TO_BROADCAST) {
            Require.rethrow(new InboundReceiveRetryableException("入站消息附件尚未全部保存"));
        }
        try {
            domainEventPublisher.publish(event(entity));
            transaction().executeWithoutResult(status -> messageMapper.update(null,
                    new LambdaUpdateWrapper<NoticeInboundMessageEntity>()
                            .eq(NoticeInboundMessageEntity::getId, messageId)
                            .eq(NoticeInboundMessageEntity::getStatus,
                                    NoticeInboundMessageStatus.READY_TO_BROADCAST)
                            .set(NoticeInboundMessageEntity::getStatus,
                                    NoticeInboundMessageStatus.BROADCASTED)
                            .set(NoticeInboundMessageEntity::getFailureCode, null)
                            .set(NoticeInboundMessageEntity::getFailureReason, null)
                            .set(NoticeInboundMessageEntity::getNextRetryAt, null)
                            .set(NoticeInboundMessageEntity::getProcessedAt, LocalDateTime.now())));
        } catch (RuntimeException failure) {
            markBroadcastFailed(messageId, failure);
            Require.rethrow(new InboundReceiveRetryableException("入站消息广播暂未受理", failure));
        }
    }

    /** Retries a persisted message whose source payload is no longer needed. */
    public void retryBroadcast(String tenantId, Long messageId) {
        Require.notBlank(tenantId, NoticeCode.NOTICE_BUSINESS_ERROR, "租户不能为空");
        Require.notNull(messageId, NoticeCode.NOTICE_BUSINESS_ERROR, "入站消息 ID 不能为空");
        withTenant(tenantId, () -> {
            publishWhenReady(messageId);
            return null;
        });
    }

    public List<NoticeInboundMessageEntity> dueBroadcasts(int limit) {
        Require.isTrue(limit > 0, NoticeCode.NOTICE_BUSINESS_ERROR, "广播重试批次必须大于 0");
        return messageMapper.selectDueBroadcasts(limit);
    }

    public void deadLetterBroadcast(NoticeInboundBroadcastDeadLetterCommand command) {
        Require.notNull(command, NoticeCode.NOTICE_BUSINESS_ERROR, "入站广播死信参数不能为空");
        Require.notBlank(command.tenantId(), NoticeCode.NOTICE_BUSINESS_ERROR, "租户不能为空");
        Require.notNull(command.messageId(), NoticeCode.NOTICE_BUSINESS_ERROR, "入站消息 ID 不能为空");
        Require.notBlank(command.reason(), NoticeCode.NOTICE_BUSINESS_ERROR, "死信原因不能为空");
        String tenantId = command.tenantId();
        Long messageId = command.messageId();
        String reason = command.reason();
        withTenant(tenantId, () -> {
            markMessageStatus(messageId, NoticeInboundMessageStatus.DEAD_LETTER,
                    "NOTICE_INBOUND_BROADCAST_EXHAUSTED", reason);
            return null;
        });
    }

    private DomainEvent event(NoticeInboundMessageEntity entity) {
        return DomainEvent.builder()
                .eventId(entity.getEventId())
                .eventType(EVENT_TYPE)
                .businessType("NOTICE_INBOUND_MESSAGE")
                .businessKey(entity.getSourceKey())
                .aggregateId(String.valueOf(entity.getId()))
                .header("tenantId", entity.getTenantId())
                .header("idempotencyKey", entity.getEventId())
                .payload("messageId", entity.getId())
                .payload("eventId", entity.getEventId())
                .payload("channelType", entity.getChannelType().name())
                .payload("providerCode", entity.getProviderCode())
                .payload("sourceMessageId", entity.getMessageId())
                .payload("status", NoticeInboundMessageStatus.BROADCASTED.name())
                .build();
    }

    private boolean claimAttachment(Long attachmentId) {
        Integer updated = transaction().execute(status -> attachmentMapper.update(null,
                new LambdaUpdateWrapper<NoticeInboundAttachmentEntity>()
                        .eq(NoticeInboundAttachmentEntity::getId, attachmentId)
                        .in(NoticeInboundAttachmentEntity::getStatus,
                                NoticeInboundAttachmentStatus.PENDING,
                                NoticeInboundAttachmentStatus.RETRYABLE_FAILED)
                        .set(NoticeInboundAttachmentEntity::getStatus, NoticeInboundAttachmentStatus.PROCESSING)
                        .setSql("attempt_count = attempt_count + 1")));
        return Require.nonNull(updated, NoticeCode.NOTICE_BUSINESS_ERROR, "入站附件申领结果不能为空") == 1;
    }

    private void markAttachmentSaved(Long attachmentId, Long fileId) {
        transaction().executeWithoutResult(status -> attachmentMapper.update(null,
                new LambdaUpdateWrapper<NoticeInboundAttachmentEntity>()
                        .eq(NoticeInboundAttachmentEntity::getId, attachmentId)
                        .eq(NoticeInboundAttachmentEntity::getStatus, NoticeInboundAttachmentStatus.PROCESSING)
                        .set(NoticeInboundAttachmentEntity::getFileId, fileId)
                        .set(NoticeInboundAttachmentEntity::getStatus, NoticeInboundAttachmentStatus.SAVED)
                        .set(NoticeInboundAttachmentEntity::getFailureCode, null)
                        .set(NoticeInboundAttachmentEntity::getFailureReason, null)));
    }

    private NoticeInboundAttachmentStatus markAttachmentFailed(Long attachmentId, RuntimeException failure) {
        NoticeInboundAttachmentEntity entity = Require.nonNull(
                attachmentMapper.selectById(attachmentId), NoticeCode.NOTICE_BUSINESS_ERROR, "入站附件不存在");
        NoticeInboundAttachmentStatus status = entity.getAttemptCount() != null
                && entity.getAttemptCount() >= MAX_ATTACHMENT_ATTEMPTS
                ? NoticeInboundAttachmentStatus.DEAD_LETTER
                : NoticeInboundAttachmentStatus.RETRYABLE_FAILED;
        transaction().executeWithoutResult(transaction -> attachmentMapper.update(null,
                new LambdaUpdateWrapper<NoticeInboundAttachmentEntity>()
                        .eq(NoticeInboundAttachmentEntity::getId, attachmentId)
                        .set(NoticeInboundAttachmentEntity::getStatus, status)
                        .set(NoticeInboundAttachmentEntity::getFailureCode, failure.getClass().getSimpleName())
                        .set(NoticeInboundAttachmentEntity::getFailureReason, failureMessage(failure))));
        return status;
    }

    private void markReady(Long messageId) {
        markMessageStatus(messageId, NoticeInboundMessageStatus.READY_TO_BROADCAST, null, null);
    }

    private void markMessageStatus(
            Long messageId, NoticeInboundMessageStatus status, String failureCode, String failureReason) {
        transaction().executeWithoutResult(transaction -> messageMapper.update(null,
                new LambdaUpdateWrapper<NoticeInboundMessageEntity>()
                        .eq(NoticeInboundMessageEntity::getId, messageId)
                        .ne(NoticeInboundMessageEntity::getStatus, NoticeInboundMessageStatus.BROADCASTED)
                        .set(NoticeInboundMessageEntity::getStatus, status)
                        .set(NoticeInboundMessageEntity::getFailureCode, failureCode)
                        .set(NoticeInboundMessageEntity::getFailureReason, failureReason)));
    }

    private void markBroadcastFailed(Long messageId, RuntimeException failure) {
        transaction().executeWithoutResult(transaction -> messageMapper.update(null,
                new LambdaUpdateWrapper<NoticeInboundMessageEntity>()
                        .eq(NoticeInboundMessageEntity::getId, messageId)
                        .ne(NoticeInboundMessageEntity::getStatus, NoticeInboundMessageStatus.BROADCASTED)
                        .set(NoticeInboundMessageEntity::getStatus, NoticeInboundMessageStatus.READY_TO_BROADCAST)
                        .set(NoticeInboundMessageEntity::getFailureCode, "NOTICE_INBOUND_BROADCAST_FAILED")
                        .set(NoticeInboundMessageEntity::getFailureReason, failureMessage(failure))
                        .set(NoticeInboundMessageEntity::getNextRetryAt,
                                LocalDateTime.now().plusSeconds(BROADCAST_RETRY_DELAY_SECONDS))
                        .setSql("attempt_count = attempt_count + 1")));
    }

    private boolean allAttachmentsSaved(Long messageId) {
        Long count = attachmentMapper.selectCount(new LambdaQueryWrapper<NoticeInboundAttachmentEntity>()
                .eq(NoticeInboundAttachmentEntity::getMessageId, messageId)
                .ne(NoticeInboundAttachmentEntity::getStatus, NoticeInboundAttachmentStatus.SAVED));
        return count == 0L;
    }

    private NoticeInboundMessageEntity find(Long channelConfigId, String sourceKey) {
        return messageMapper.selectOne(new LambdaQueryWrapper<NoticeInboundMessageEntity>()
                .eq(NoticeInboundMessageEntity::getChannelConfigId, channelConfigId)
                .eq(NoticeInboundMessageEntity::getSourceKey, sourceKey));
    }

    private NoticeInboundAttachmentEntity findAttachment(Long messageId, int index) {
        return attachmentMapper.selectOne(new LambdaQueryWrapper<NoticeInboundAttachmentEntity>()
                .eq(NoticeInboundAttachmentEntity::getMessageId, messageId)
                .eq(NoticeInboundAttachmentEntity::getAttachmentIndex, index));
    }

    private String messageTenantId(Long messageId) {
        String tenantId = Require.nonNull(
                messageMapper.selectById(messageId), NoticeCode.NOTICE_BUSINESS_ERROR, "入站消息不存在").getTenantId();
        Require.notBlank(tenantId, NoticeCode.NOTICE_BUSINESS_ERROR, "入站消息租户不存在");
        return tenantId;
    }

    private SaveFileCommand fileCommand(Long messageId, InboundNoticeAttachmentRequest attachment) {
        SaveFileCommand command = new SaveFileCommand();
        command.setInputStream(Require.nonNull(attachment.content(), NoticeCode.NOTICE_BUSINESS_ERROR, "入站附件流不能为空"));
        command.setFileName(Require.nonNull(attachment.fileName(), NoticeCode.NOTICE_BUSINESS_ERROR, "入站附件名不能为空"));
        command.setFileSize(attachment.fileSize());
        command.setContentType(attachment.contentType());
        command.setPurpose("notice-inbound-attachment");
        command.setAccessLevel("PRIVATE");
        command.setBizType("NOTICE_INBOUND_MESSAGE");
        command.setBizId(String.valueOf(messageId));
        return command;
    }

    private InboundReceiveResultResponse result(NoticeInboundMessageEntity entity, boolean duplicate) {
        return new InboundReceiveResultResponse(entity.getId(), entity.getEventId(), duplicate,
                entity.getStatus() == NoticeInboundMessageStatus.BROADCASTED);
    }

    private String stableEventId(InboundNoticeMessageRequest message) {
        String identity = message.tenantId() + "|" + message.channelConfigId() + "|" + message.sourceKey();
        return UUID.nameUUIDFromBytes(identity.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private void validateAttachments(List<InboundNoticeAttachmentRequest> attachments) {
        HashSet<Integer> indexes = new HashSet<>();
        for (InboundNoticeAttachmentRequest attachment : attachments) {
            InboundNoticeAttachmentRequest checked = Require.nonNull(attachment, NoticeCode.NOTICE_BUSINESS_ERROR, "入站附件不能为空");
            Require.isTrue(checked.index() >= 0, NoticeCode.NOTICE_BUSINESS_ERROR, "入站附件序号不能小于 0");
            Require.isTrue(indexes.add(checked.index()), NoticeCode.NOTICE_BUSINESS_ERROR, "入站附件序号不能重复");
            Require.notBlank(checked.fileName(), NoticeCode.NOTICE_BUSINESS_ERROR, "入站附件名不能为空");
            Require.isTrue(checked.fileSize() > 0L, NoticeCode.NOTICE_BUSINESS_ERROR, "入站附件大小必须大于 0");
            Require.notNull(checked.content(), NoticeCode.NOTICE_BUSINESS_ERROR, "入站附件流不能为空");
        }
    }

    private void closeAttachments(List<InboundNoticeAttachmentRequest> attachments) {
        List<IOException> failures = new ArrayList<>();
        for (InboundNoticeAttachmentRequest attachment : attachments) {
            if (attachment == null) {
                continue;
            }
            try {
                attachment.close();
            } catch (IOException failure) {
                failures.add(failure);
            }
        }
        if (!failures.isEmpty()) {
            log.warn("Inbound attachment stream close failed: count={}", failures.size());
        }
    }

    private <T> T withTenant(String tenantId, Supplier<T> action) {
        MangoContextSnapshot previous = MangoContextHolder.get();
        try {
            // withTenantId deliberately keeps an existing tenant; a public callback may
            // arrive with a default/anonymous request context, while the persisted channel
            // configuration is the authoritative tenant mapping.
            MangoContextHolder.set(snapshotWithTenant(previous, tenantId));
            return action.get();
        } finally {
            MangoContextHolder.set(previous);
        }
    }

    private MangoContextSnapshot snapshotWithTenant(MangoContextSnapshot previous, String tenantId) {
        return new MangoContextSnapshot(
                previous.requestId(), previous.traceId(), tenantId, previous.userId(), previous.memberId(),
                previous.principalName(), previous.realm(), previous.actorType(), previous.partyType(),
                previous.partyId(), previous.appCode(), previous.clientIp());
    }

    private TransactionTemplate transaction() {
        return new TransactionTemplate(transactionManager);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException failure) {
            return Require.rethrow(new IllegalArgumentException("入站消息 JSON 无法序列化", failure));
        }
    }

    private String failureMessage(RuntimeException failure) {
        String message = failure.getMessage();
        if (message == null || message.isBlank()) {
            return failure.getClass().getSimpleName();
        }
        return message.length() <= MAX_FAILURE_REASON_LENGTH
                ? message : message.substring(0, MAX_FAILURE_REASON_LENGTH);
    }

    /** Signals the source to retry without acknowledging an incomplete receive. */
    public static final class InboundReceiveRetryableException extends RuntimeException {
        public InboundReceiveRetryableException(String message) {
            super(message);
        }

        public InboundReceiveRetryableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
