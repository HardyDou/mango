package io.mango.infra.event.core.system;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.infra.event.api.command.ReconsumeSystemEventCommand;
import io.mango.infra.event.api.enums.EventCode;
import io.mango.infra.event.api.query.SystemEventPageQuery;
import io.mango.infra.event.api.vo.SystemEventVO;
import io.mango.infra.kv.api.IOutboxStore;
import io.mango.infra.kv.api.OutboxMessage;
import io.mango.infra.kv.api.OutboxMessageQuery;
import io.mango.infra.kv.api.OutboxStatus;
import io.mango.infra.kv.api.OutboxTopics;

import java.time.Clock;
import java.util.List;

/**
 * 系统事件运维服务。
 */
public class SystemEventService implements ISystemEventService {

    private final IOutboxStore outboxStore;
    private final Clock clock;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Thread-safe outbox SPI is intentionally retained as a shared service dependency")
    public SystemEventService(IOutboxStore outboxStore, Clock clock) {
        Require.notNull(outboxStore, EventCode.EVENT_BUSINESS_ERROR, "Outbox 存储不能为空");
        Require.notNull(clock, EventCode.EVENT_BUSINESS_ERROR, "时钟不能为空");
        this.outboxStore = outboxStore;
        this.clock = clock;
    }

    @Override
    public PageResult<SystemEventVO> page(SystemEventPageQuery query) {
        SystemEventPageQuery normalized = query;
        if (normalized == null) {
            normalized = new SystemEventPageQuery();
        }
        OutboxMessageQuery outboxQuery = toOutboxQuery(normalized);
        List<SystemEventVO> records = outboxStore.query(outboxQuery).stream()
                .map(this::toVO)
                .toList();
        return PageResult.of(records, outboxStore.count(outboxQuery), normalized.getPageNum(), normalized.getPageSize());
    }

    @Override
    public SystemEventVO detail(String messageId) {
        Require.notBlank(messageId, EventCode.EVENT_BUSINESS_ERROR, "消息 ID 不能为空");
        OutboxMessage message = outboxStore.findById(messageId);
        if (!isDomainEvent(message)) {
            return null;
        }
        return toVO(message);
    }

    @Override
    public boolean reconsume(ReconsumeSystemEventCommand command) {
        Require.notNull(command, EventCode.EVENT_BUSINESS_ERROR, "重新投递命令不能为空");
        Require.notBlank(command.getMessageId(), EventCode.EVENT_BUSINESS_ERROR, "消息 ID 不能为空");
        OutboxMessage message = outboxStore.findById(command.getMessageId());
        if (!isDomainEvent(message) || message.getStatus() == OutboxStatus.SUCCESS) {
            return false;
        }
        outboxStore.requeue(command.getMessageId(), clock.instant(), clock.instant());
        return true;
    }

    private OutboxMessageQuery toOutboxQuery(SystemEventPageQuery query) {
        OutboxMessageQuery outboxQuery = new OutboxMessageQuery();
        outboxQuery.setPageNum(query.getPageNum());
        outboxQuery.setPageSize(query.getPageSize());
        outboxQuery.setTopic(OutboxTopics.DOMAIN_EVENT);
        outboxQuery.setStatus(query.getStatus());
        outboxQuery.setEventType(query.getEventType());
        outboxQuery.setBusinessType(query.getBusinessType());
        outboxQuery.setBusinessKey(query.getBusinessKey());
        outboxQuery.setKeyword(query.getKeyword());
        outboxQuery.setAbnormalOnly(query.isAbnormalOnly());
        return outboxQuery;
    }

    private boolean isDomainEvent(OutboxMessage message) {
        if (message == null) {
            return false;
        }
        return OutboxTopics.DOMAIN_EVENT.equals(message.getTopic());
    }

    private SystemEventVO toVO(OutboxMessage message) {
        SystemEventVO vo = new SystemEventVO();
        vo.setMessageId(message.getMessageId());
        vo.setEventType(message.getEventType());
        vo.setBusinessType(message.getBusinessType());
        vo.setBusinessKey(message.getBusinessKey());
        vo.setAggregateId(message.getAggregateId());
        vo.setOccurredAt(message.getOccurredAt());
        vo.setStatus(message.getStatus());
        vo.setAttemptCount(message.getAttemptCount());
        vo.setNextAttemptAt(message.getNextAttemptAt());
        vo.setLockedAt(message.getLockedAt());
        vo.setLockedBy(message.getLockedBy());
        vo.setErrorMessage(message.getErrorMessage());
        vo.setPayload(message.getPayload());
        vo.setHeaders(message.getHeaders());
        return vo;
    }
}
