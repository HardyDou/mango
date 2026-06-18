package io.mango.notice.starter.remote;

import io.mango.notice.api.NoticeApi;
import io.mango.notice.api.command.SendNoticeCommand;
import io.mango.notice.api.event.NoticeSendEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.LinkedHashMap;

/**
 * Sends notice events through the remote notice API after the owning business
 * transaction commits.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NoticeSendEventListener {

    private final NoticeApi noticeApi;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onNoticeSendEvent(NoticeSendEvent event) {
        try {
            noticeApi.send(toCommand(event));
        } catch (RuntimeException ex) {
            log.warn("Send remote notice event failed. bizType={}, bizId={}",
                    event.getBizType(), event.getBizId(), ex);
        }
    }

    private SendNoticeCommand toCommand(NoticeSendEvent event) {
        SendNoticeCommand command = new SendNoticeCommand();
        command.setBizType(event.getBizType());
        command.setBizId(event.getBizId());
        command.setUserId(event.getUserId());
        command.setUserIds(event.getUserIds());
        command.setRecipientRuleCode(event.getRecipientRuleCode());
        command.setChannelTypes(event.getChannelTypes());
        command.setParams(event.getParams() == null ? null : new LinkedHashMap<>(event.getParams()));
        command.setPriority(event.getPriority());
        command.setIdempotentKey(event.getIdempotentKey());
        return command;
    }
}
