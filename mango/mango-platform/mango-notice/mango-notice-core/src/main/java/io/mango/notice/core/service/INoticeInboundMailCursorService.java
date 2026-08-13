package io.mango.notice.core.service;

import io.mango.notice.api.enums.NoticeInboundProtocol;
import io.mango.notice.core.entity.NoticeInboundReceiveCursorEntity;

import java.time.LocalDateTime;

/** Persists mailbox polling cursors. */
public interface INoticeInboundMailCursorService {

    NoticeInboundReceiveCursorEntity find(Long channelConfigId);

    void advance(NoticeInboundMailCursorAdvanceCommand command);

    void recordPoll(NoticeInboundMailCursorPollCommand command);

    void recordFailure(NoticeInboundMailCursorFailureCommand command);
}
