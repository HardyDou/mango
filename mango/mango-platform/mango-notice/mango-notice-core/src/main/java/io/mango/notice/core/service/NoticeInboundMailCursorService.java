package io.mango.notice.core.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import io.mango.common.result.Require;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.notice.api.enums.NoticeCode;
import io.mango.notice.api.enums.NoticeInboundProtocol;
import io.mango.notice.core.entity.NoticeInboundReceiveCursorEntity;
import io.mango.notice.core.mapper.NoticeInboundReceiveCursorMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** Transactional cursor access; callers enter the account tenant before invoking it. */
@Service
@RequiredArgsConstructor
public class NoticeInboundMailCursorService implements INoticeInboundMailCursorService {

    private static final int MAX_FAILURE_REASON_LENGTH = 1000;

    private final NoticeInboundReceiveCursorMapper cursorMapper;

    public NoticeInboundReceiveCursorEntity find(Long channelConfigId) {
        Require.notNull(channelConfigId, NoticeCode.NOTICE_BUSINESS_ERROR, "邮箱渠道配置 ID 不能为空");
        return cursorMapper.selectOne(new LambdaQueryWrapper<NoticeInboundReceiveCursorEntity>()
                .eq(NoticeInboundReceiveCursorEntity::getChannelConfigId, channelConfigId));
    }

    @Transactional
    public void advance(NoticeInboundMailCursorAdvanceCommand command) {
        Require.notNull(command, NoticeCode.NOTICE_BUSINESS_ERROR, "邮箱游标推进参数不能为空");
        Long channelConfigId = command.channelConfigId();
        NoticeInboundProtocol protocol = command.protocol();
        String cursorValue = command.cursorValue();
        String cursorVersion = command.cursorVersion();
        LocalDateTime nextPollAt = command.nextPollAt();
        NoticeInboundReceiveCursorEntity entity = find(channelConfigId);
        if (entity == null) {
            entity = new NoticeInboundReceiveCursorEntity();
            entity.setId(IdWorker.getId());
            entity.setChannelConfigId(channelConfigId);
            entity.setTenantId(currentTenantId());
            entity.setProtocol(protocol);
            entity.setCursorValue(cursorValue);
            entity.setCursorVersion(cursorVersion);
            entity.setLastPolledAt(LocalDateTime.now());
            entity.setNextPollAt(nextPollAt);
            cursorMapper.insert(entity);
            return;
        }
        entity.setProtocol(protocol);
        entity.setCursorValue(cursorValue);
        entity.setCursorVersion(cursorVersion);
        entity.setLastPolledAt(LocalDateTime.now());
        entity.setNextPollAt(nextPollAt);
        cursorMapper.update(null, successfulPollUpdate(entity)
                .set(NoticeInboundReceiveCursorEntity::getCursorValue, cursorValue)
                .set(NoticeInboundReceiveCursorEntity::getCursorVersion, cursorVersion));
    }

    @Transactional
    public void recordPoll(NoticeInboundMailCursorPollCommand command) {
        Require.notNull(command, NoticeCode.NOTICE_BUSINESS_ERROR, "邮箱轮询游标参数不能为空");
        Long channelConfigId = command.channelConfigId();
        NoticeInboundProtocol protocol = command.protocol();
        LocalDateTime nextPollAt = command.nextPollAt();
        NoticeInboundReceiveCursorEntity entity = find(channelConfigId);
        if (entity == null) {
            advance(new NoticeInboundMailCursorAdvanceCommand(channelConfigId, protocol, null, null, nextPollAt));
            return;
        }
        entity.setProtocol(protocol);
        entity.setLastPolledAt(LocalDateTime.now());
        entity.setNextPollAt(nextPollAt);
        cursorMapper.update(null, successfulPollUpdate(entity));
    }

    @Transactional
    public void recordFailure(NoticeInboundMailCursorFailureCommand command) {
        Require.notNull(command, NoticeCode.NOTICE_BUSINESS_ERROR, "邮箱游标失败参数不能为空");
        Long channelConfigId = command.channelConfigId();
        NoticeInboundProtocol protocol = command.protocol();
        String failureCode = command.failureCode();
        String failureReason = command.failureReason();
        LocalDateTime nextPollAt = command.nextPollAt();
        NoticeInboundReceiveCursorEntity entity = find(channelConfigId);
        if (entity == null) {
            entity = new NoticeInboundReceiveCursorEntity();
            entity.setId(IdWorker.getId());
            entity.setChannelConfigId(channelConfigId);
            entity.setProtocol(protocol);
            entity.setTenantId(currentTenantId());
            cursorMapper.insert(entity);
        }
        cursorMapper.update(null, new LambdaUpdateWrapper<NoticeInboundReceiveCursorEntity>()
                .eq(NoticeInboundReceiveCursorEntity::getId, entity.getId())
                .set(NoticeInboundReceiveCursorEntity::getProtocol, protocol)
                .set(NoticeInboundReceiveCursorEntity::getLastPolledAt, LocalDateTime.now())
                .set(NoticeInboundReceiveCursorEntity::getNextPollAt, nextPollAt)
                .set(NoticeInboundReceiveCursorEntity::getLastFailureCode, failureCode)
                .set(NoticeInboundReceiveCursorEntity::getLastFailureReason, limit(failureReason)));
    }

    private LambdaUpdateWrapper<NoticeInboundReceiveCursorEntity> successfulPollUpdate(
            NoticeInboundReceiveCursorEntity entity) {
        return new LambdaUpdateWrapper<NoticeInboundReceiveCursorEntity>()
                .eq(NoticeInboundReceiveCursorEntity::getId, entity.getId())
                .set(NoticeInboundReceiveCursorEntity::getProtocol, entity.getProtocol())
                .set(NoticeInboundReceiveCursorEntity::getLastPolledAt, entity.getLastPolledAt())
                .set(NoticeInboundReceiveCursorEntity::getNextPollAt, entity.getNextPollAt())
                .set(NoticeInboundReceiveCursorEntity::getLastFailureCode, null)
                .set(NoticeInboundReceiveCursorEntity::getLastFailureReason, null);
    }

    private String currentTenantId() {
        String tenantId = MangoContextHolder.tenantId();
        return tenantId == null || tenantId.isBlank() ? "default" : tenantId;
    }

    private String limit(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.length() <= MAX_FAILURE_REASON_LENGTH
                ? value : value.substring(0, MAX_FAILURE_REASON_LENGTH);
    }
}
