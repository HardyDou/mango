package io.mango.infra.event.core.system;

import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.infra.event.api.command.ReconsumeSystemEventCommand;
import io.mango.infra.event.api.query.SystemEventPageQuery;
import io.mango.infra.event.api.vo.SystemEventVO;
import io.mango.infra.kv.api.IOutboxStore;
import io.mango.infra.kv.api.OutboxMessage;
import io.mango.infra.kv.api.OutboxMessageQuery;

import java.time.Clock;
import java.util.List;

/**
 * 系统事件运维服务。
 */
public class SystemEventService {

    private final IOutboxStore outboxStore;
    private final Clock clock;

    public SystemEventService(IOutboxStore outboxStore, Clock clock) {
        Require.notNull(outboxStore, "Outbox 存储不能为空");
        Require.notNull(clock, "时钟不能为空");
        this.outboxStore = outboxStore;
        this.clock = clock;
    }

    public PageResult<SystemEventVO> page(SystemEventPageQuery query) {
        SystemEventPageQuery normalized = query == null ? new SystemEventPageQuery() : query;
        OutboxMessageQuery outboxQuery = toOutboxQuery(normalized);
        List<SystemEventVO> records = outboxStore.query(outboxQuery).stream()
                .map(this::toVO)
                .toList();
        return PageResult.of(records, outboxStore.count(outboxQuery), normalized.getPageNum(), normalized.getPageSize());
    }

    public SystemEventVO detail(String messageId) {
        Require.notBlank(messageId, "消息 ID 不能为空");
        OutboxMessage message = outboxStore.findById(messageId);
        return message == null ? null : toVO(message);
    }

    public boolean reconsume(ReconsumeSystemEventCommand command) {
        Require.notNull(command, "重新投递命令不能为空");
        Require.notBlank(command.getMessageId(), "消息 ID 不能为空");
        if (outboxStore.findById(command.getMessageId()) == null) {
            return false;
        }
        outboxStore.requeue(command.getMessageId(), clock.instant(), clock.instant());
        return true;
    }

    private OutboxMessageQuery toOutboxQuery(SystemEventPageQuery query) {
        OutboxMessageQuery outboxQuery = new OutboxMessageQuery();
        outboxQuery.setPageNum(query.getPageNum());
        outboxQuery.setPageSize(query.getPageSize());
        outboxQuery.setStatus(query.getStatus());
        outboxQuery.setEventType(query.getEventType());
        outboxQuery.setBusinessType(query.getBusinessType());
        outboxQuery.setBusinessKey(query.getBusinessKey());
        outboxQuery.setKeyword(query.getKeyword());
        outboxQuery.setAbnormalOnly(query.isAbnormalOnly());
        return outboxQuery;
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
